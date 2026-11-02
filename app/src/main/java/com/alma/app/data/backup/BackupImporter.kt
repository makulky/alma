package com.alma.app.data.backup

import android.content.Context
import android.net.Uri
import com.alma.app.data.CoverStore
import com.alma.app.data.isValidCoverName
import com.alma.app.data.local.BookDao
import com.alma.app.data.local.toDomain
import com.alma.app.data.local.toEntity
import com.alma.app.data.localCoverName
import com.alma.app.domain.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

private const val MAX_JSON_BYTES = 64L * 1024 * 1024
private const val MAX_COVER_BYTES = 32L * 1024 * 1024

data class ParsedBackup(val books: List<Book>, val stagingDir: File)

/**
 * Lee y valida la copia entera, dejando las portadas en [stagingDir], sin tocar los datos de la app.
 * Lanza [BackupException] si el archivo está dañado, es de una versión más nueva o tiene rutas no
 * válidas. Solo se leen "backup.json" y "covers/<nombre>"; el resto de entradas se ignora.
 */
fun readBackup(input: InputStream, stagingDir: File): ParsedBackup {
    var json: String? = null
    try {
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.isDirectory -> Unit
                    entry.name == BACKUP_JSON_ENTRY -> {
                        val bytes = ByteArrayOutputStream()
                        zip.copyLimited(bytes, MAX_JSON_BYTES)
                        json = bytes.toString(Charsets.UTF_8.name())
                    }
                    entry.name.startsWith(BACKUP_COVERS_PREFIX) -> {
                        val name = entry.name.removePrefix(BACKUP_COVERS_PREFIX)
                        if (!isValidCoverName(name)) throw BackupException(BackupError.INVALID_PATH)
                        stagingDir.mkdirs()
                        File(stagingDir, name).outputStream().use { zip.copyLimited(it, MAX_COVER_BYTES) }
                    }
                }
            }
        }
    } catch (e: IOException) {
        throw BackupException(BackupError.CORRUPT, e)
    }
    val text = json ?: throw BackupException(BackupError.CORRUPT)
    return ParsedBackup(parseBooks(text, stagingDir), stagingDir)
}

private fun parseBooks(text: String, stagingDir: File): List<Book> {
    val file = try {
        val version = backupJson.decodeFromString<BackupHeader>(text).version
        if (version > BACKUP_VERSION) throw BackupException(BackupError.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupError.CORRUPT)
        backupJson.decodeFromString<BackupFile>(text)
    } catch (e: SerializationException) {
        throw BackupException(BackupError.CORRUPT, e)
    }
    val books = file.books.map { it.toBookOrThrow() }
    val missingCover = books.mapNotNull { localCoverName(it.coverUri) }.any { !File(stagingDir, it).isFile }
    if (missingCover) throw BackupException(BackupError.CORRUPT)
    return books
}

private fun InputStream.copyLimited(out: OutputStream, limit: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) return
        total += read
        if (total > limit) throw BackupException(BackupError.CORRUPT)
        out.write(buffer, 0, read)
    }
}

data class ImportPlan(
    val toInsert: List<Book>,
    val toUpdate: List<Book>,
    val unchanged: Int,
)

/**
 * Combina la copia con la biblioteca sin borrar nada. Los libros se identifican por el volumen de
 * Google, luego por ISBN y, en los añadidos a mano, por título y autor. Si un libro está en ambos
 * lados, gana el de [Book.updatedAt] más reciente.
 */
fun planImport(existing: List<Book>, incoming: List<Book>): ImportPlan {
    class Slot(var book: Book, val isNew: Boolean, var changed: Boolean = false)

    val slots = existing.map { Slot(it, isNew = false) }.toMutableList()
    var unchanged = 0
    for (book in incoming) {
        val slot = slots.firstOrNull { book.sourceId != null && it.book.sourceId == book.sourceId }
            ?: slots.firstOrNull { book.isbn13 != null && it.book.isbn13 == book.isbn13 }
            ?: slots.firstOrNull { it.book.isSameManualBookAs(book) }
        when {
            slot == null -> slots += Slot(book.copy(id = 0), isNew = true)
            book.updatedAt.isAfter(slot.book.updatedAt) -> {
                slot.book = book.copy(
                    id = slot.book.id,
                    sourceId = book.sourceId ?: slot.book.sourceId,
                    isbn13 = book.isbn13 ?: slot.book.isbn13,
                )
                slot.changed = true
            }
            else -> if (!slot.isNew && !slot.changed) unchanged++
        }
    }
    return ImportPlan(
        toInsert = slots.filter { it.isNew }.map { it.book },
        toUpdate = slots.filter { !it.isNew && it.changed }.map { it.book },
        unchanged = unchanged,
    )
}

private fun Book.isSameManualBookAs(other: Book): Boolean =
    sourceId == null && other.sourceId == null &&
        title.trim().equals(other.title.trim(), ignoreCase = true) &&
        authors.trim().equals(other.authors.trim(), ignoreCase = true)

class ImportPreview internal constructor(
    val newBooks: Int,
    val updatedBooks: Int,
    val unchangedBooks: Int,
    internal val parsed: ParsedBackup,
)

class BackupImporter(
    private val context: Context,
    private val dao: BookDao,
    private val covers: CoverStore,
) {
    private val stagingRoot get() = File(context.cacheDir, "backup-import")

    /** Lee y valida el archivo sin cambiar nada. Lanza [BackupException] si no es válido. */
    suspend fun prepare(source: Uri): ImportPreview = withContext(Dispatchers.IO) {
        stagingRoot.deleteRecursively()
        val stagingDir = File(stagingRoot, UUID.randomUUID().toString())
        try {
            val parsed = try {
                val input = context.contentResolver.openInputStream(source)
                    ?: throw BackupException(BackupError.CORRUPT)
                input.use { readBackup(it, stagingDir) }
            } catch (e: IOException) {
                throw BackupException(BackupError.CORRUPT, e)
            }
            val plan = planImport(dao.getAll().map { it.toDomain() }, parsed.books)
            ImportPreview(plan.toInsert.size, plan.toUpdate.size, plan.unchanged, parsed)
        } catch (e: BackupException) {
            stagingRoot.deleteRecursively()
            throw e
        }
    }

    /** Aplica la importación: o se guarda todo, o los datos quedan como estaban. */
    suspend fun confirm(preview: ImportPreview) = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getAll().map { it.toDomain() }
            val plan = planImport(existing, preview.parsed.books)
            val changed = plan.toInsert + plan.toUpdate
            val copiedCovers = copyCovers(changed, preview.parsed.stagingDir)
            try {
                dao.applyImport(plan.toInsert.map { it.toEntity() }, plan.toUpdate.map { it.toEntity() })
            } catch (e: Exception) {
                copiedCovers.forEach(File::delete)
                throw e
            }
            deleteReplacedCovers(existing, plan.toUpdate)
        } finally {
            stagingRoot.deleteRecursively()
        }
    }

    fun cancel() {
        stagingRoot.deleteRecursively()
    }

    private fun copyCovers(books: List<Book>, stagingDir: File): List<File> {
        covers.directory.mkdirs()
        val copied = mutableListOf<File>()
        try {
            books.mapNotNull { localCoverName(it.coverUri) }.distinct().forEach { name ->
                val target = File(covers.directory, name)
                if (!target.exists()) {
                    File(stagingDir, name).copyTo(target)
                    copied += target
                }
            }
        } catch (e: IOException) {
            copied.forEach(File::delete)
            throw e
        }
        return copied
    }

    private suspend fun deleteReplacedCovers(existing: List<Book>, updated: List<Book>) {
        val inUse = dao.getAll().mapNotNull { it.coverUri }.toSet()
        val updatedIds = updated.map { it.id }.toSet()
        existing.filter { it.id in updatedIds }
            .mapNotNull { it.coverUri }
            .filter { it !in inUse }
            .forEach { covers.delete(it) }
    }
}
