package com.alma.app.data.backup

import com.alma.app.data.isValidCoverName
import com.alma.app.data.localCoverName
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.domain.areValidDates
import com.alma.app.domain.isValidRating
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.time.Instant
import java.time.LocalDate

const val BACKUP_VERSION = 2
const val BACKUP_JSON_ENTRY = "backup.json"
const val BACKUP_COVERS_PREFIX = "covers/"

internal val backupJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

enum class BackupError { CORRUPT, NEWER_VERSION, INVALID_PATH }

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(error.name, cause)

/** Solo la versión: se lee primero para rechazar formatos más nuevos antes de interpretar el resto. */
@Serializable
internal data class BackupHeader(val version: Int)

@Serializable
internal data class BackupFile(
    val version: Int,
    val exportedAt: String,
    val books: List<BackupBook>,
)

@Serializable
internal data class BackupBook(
    // Las copias de la versión 1 lo llamaban googleVolumeId.
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("googleVolumeId")
    val sourceId: String? = null,
    val title: String,
    val authors: String,
    val coverUri: String? = null,
    val description: String? = null,
    val isbn13: String? = null,
    val publisher: String? = null,
    val pageCount: Int? = null,
    val publishedYear: Int? = null,
    val status: String,
    val ratingHalves: Int? = null,
    val comment: String? = null,
    val startedOn: String? = null,
    val finishedOn: String? = null,
    val addedAt: String,
    val updatedAt: String,
)

internal fun Book.toBackup() = BackupBook(
    sourceId = sourceId,
    title = title,
    authors = authors,
    coverUri = coverUri,
    description = description,
    isbn13 = isbn13,
    publisher = publisher,
    pageCount = pageCount,
    publishedYear = publishedYear,
    status = status.name,
    ratingHalves = ratingHalves,
    comment = comment,
    startedOn = startedOn?.toString(),
    finishedOn = finishedOn?.toString(),
    addedAt = addedAt.toString(),
    updatedAt = updatedAt.toString(),
)

/** Valida el libro por completo y lanza [BackupException] si hay algún dato imposible. */
internal fun BackupBook.toBookOrThrow(): Book {
    validateCoverUri(coverUri)
    val book = try {
        Book(
            sourceId = sourceId,
            title = title,
            authors = authors,
            coverUri = coverUri,
            description = description,
            isbn13 = isbn13,
            publisher = publisher,
            pageCount = pageCount,
            publishedYear = publishedYear,
            status = ReadingStatus.valueOf(status),
            ratingHalves = ratingHalves,
            comment = comment,
            startedOn = startedOn?.let(LocalDate::parse),
            finishedOn = finishedOn?.let(LocalDate::parse),
            addedAt = Instant.parse(addedAt),
            updatedAt = Instant.parse(updatedAt),
        )
    } catch (e: RuntimeException) {
        // IllegalArgumentException (estado) o DateTimeParseException (fechas).
        throw BackupException(BackupError.CORRUPT, e)
    }
    val isValid = book.title.isNotBlank() &&
        (book.ratingHalves == null || isValidRating(book.ratingHalves)) &&
        areValidDates(book.startedOn, book.finishedOn)
    if (!isValid) throw BackupException(BackupError.CORRUPT)
    return book
}

private fun validateCoverUri(coverUri: String?) {
    if (coverUri == null) return
    val localName = localCoverName(coverUri)
    val isAllowed = if (localName != null) {
        isValidCoverName(localName)
    } else {
        // Nada de file:// ni content://: una copia no debe poder apuntar a otros archivos.
        coverUri.startsWith("https://") || coverUri.startsWith("http://")
    }
    if (!isAllowed) throw BackupException(BackupError.INVALID_PATH)
}
