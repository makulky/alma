package com.alma.app.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.alma.app.data.CoverStore
import com.alma.app.data.local.BookDao
import com.alma.app.data.local.toDomain
import com.alma.app.data.localCoverName
import com.alma.app.domain.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe la copia en formato ZIP. Si falta el archivo de alguna portada, ese libro se exporta sin
 * portada para que la copia siga siendo válida.
 */
fun writeBackup(out: OutputStream, books: List<Book>, coverFile: (String?) -> File?, exportedAt: Instant) {
    ZipOutputStream(out).use { zip ->
        val written = mutableSetOf<String>()
        val exported = books.map { book ->
            val name = localCoverName(book.coverUri) ?: return@map book
            val file = coverFile(book.coverUri)?.takeIf(File::isFile) ?: return@map book.copy(coverUri = null)
            if (written.add(name)) {
                zip.putNextEntry(ZipEntry(BACKUP_COVERS_PREFIX + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            book
        }
        val backup = BackupFile(BACKUP_VERSION, exportedAt.toString(), exported.map { it.toBackup() })
        zip.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
        zip.write(backupJson.encodeToString(backup).toByteArray())
        zip.closeEntry()
    }
}

class BackupExporter(
    private val context: Context,
    private val dao: BookDao,
    private val covers: CoverStore,
) {
    /** Devuelve el número de libros exportados. Si falla, borra el archivo incompleto. */
    suspend fun export(destination: Uri): Int = withContext(Dispatchers.IO) {
        val books = dao.getAll().map { it.toDomain() }
        try {
            val out = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("No se puede abrir el destino")
            out.use { writeBackup(it, books, covers::fileFor, Instant.now()) }
            books.size
        } catch (e: CancellationException) {
            deleteQuietly(destination)
            throw e
        } catch (e: IOException) {
            deleteQuietly(destination)
            throw e
        }
    }

    private fun deleteQuietly(destination: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
    }
}
