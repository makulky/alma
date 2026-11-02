package com.alma.app.data

import com.alma.app.data.local.BookDao
import com.alma.app.data.local.toDomain
import com.alma.app.data.local.toEntity
import com.alma.app.data.remote.OpenLibraryApi
import com.alma.app.data.remote.SearchResult
import com.alma.app.data.remote.rankSearchResults
import com.alma.app.data.remote.retryOnServerError
import com.alma.app.data.remote.synopsis
import com.alma.app.data.remote.toSearchResult
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.domain.areValidDates
import com.alma.app.domain.isValidRating
import com.alma.app.domain.withStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface SearchOutcome {
    data class Success(val results: List<SearchResult>) : SearchOutcome
    data object Offline : SearchOutcome

    /** Open Library rechaza la petición: normalmente por el límite de uso. */
    data object Rejected : SearchOutcome
    data object Failed : SearchOutcome
}

data class ManualBook(
    val title: String,
    val authors: String,
    val coverUri: String? = null,
    val description: String? = null,
    val isbn13: String? = null,
    val publisher: String? = null,
    val pageCount: Int? = null,
    val publishedYear: Int? = null,
)

class BookRepository(
    private val dao: BookDao,
    private val api: OpenLibraryApi,
    private val covers: CoverStore,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    val books: Flow<List<Book>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun book(id: Long): Flow<Book?> = dao.observeById(id).map { it?.toDomain() }

    fun booksWithDatesBetween(from: LocalDate, to: LocalDate): Flow<List<Book>> =
        dao.observeWithDatesBetween(from.toString(), to.toString()).map { list -> list.map { it.toDomain() } }

    suspend fun search(query: String): SearchOutcome = try {
        val response = retryOnServerError { api.search(query = query) }
        SearchOutcome.Success(rankSearchResults(query, response.docs.mapNotNull { it.toSearchResult() }))
    } catch (e: CancellationException) {
        throw e
    } catch (_: SocketTimeoutException) {
        // Hay conexión, pero Open Library no ha respondido a tiempo.
        SearchOutcome.Failed
    } catch (_: IOException) {
        SearchOutcome.Offline
    } catch (e: HttpException) {
        if (e.code() in 400..499) SearchOutcome.Rejected else SearchOutcome.Failed
    } catch (_: SerializationException) {
        SearchOutcome.Failed
    }

    /** La búsqueda no incluye la sinopsis. Si no se puede obtener, el libro se guarda sin ella. */
    private suspend fun fetchSynopsis(workId: String): String? = try {
        api.work(workId).synopsis()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        null
    } catch (_: HttpException) {
        null
    } catch (_: SerializationException) {
        null
    }

    /** Guarda un resultado de búsqueda. Si el libro ya estaba, solo cambia su estado. */
    suspend fun saveFromSearch(result: SearchResult, status: ReadingStatus): Long {
        val existing = dao.findBySourceId(result.sourceId) ?: result.isbn13?.let { dao.findByIsbn(it) }
        if (existing != null) {
            setStatus(existing.id, status)
            return existing.id
        }
        val synopsis = fetchSynopsis(result.workId)
        val now = now()
        val book = Book(
            sourceId = result.sourceId,
            title = result.title,
            authors = result.authors,
            coverUri = result.coverUri,
            description = synopsis,
            isbn13 = result.isbn13,
            publisher = result.publisher,
            pageCount = result.pageCount,
            publishedYear = result.publishedYear,
            status = ReadingStatus.PENDING,
            addedAt = now,
            updatedAt = now,
        ).withStatus(status, today(), now)
        return dao.insert(book.toEntity())
    }

    suspend fun findIdByIsbn(isbn13: String): Long? = dao.findByIsbn(isbn13)?.id

    suspend fun addManual(manual: ManualBook, status: ReadingStatus): Long {
        val now = now()
        val book = Book(
            title = manual.title,
            authors = manual.authors,
            coverUri = manual.coverUri,
            description = manual.description,
            isbn13 = manual.isbn13,
            publisher = manual.publisher,
            pageCount = manual.pageCount,
            publishedYear = manual.publishedYear,
            status = ReadingStatus.PENDING,
            addedAt = now,
            updatedAt = now,
        ).withStatus(status, today(), now)
        return dao.insert(book.toEntity())
    }

    suspend fun setStatus(id: Long, status: ReadingStatus) = edit(id) { it.withStatus(status, today(), now()) }

    suspend fun setRating(id: Long, halves: Int?) {
        require(halves == null || isValidRating(halves))
        edit(id) { it.copy(ratingHalves = halves, updatedAt = now()) }
    }

    suspend fun setComment(id: Long, comment: String) =
        edit(id) { it.copy(comment = comment.takeIf(String::isNotBlank), updatedAt = now()) }

    /** Devuelve false, sin guardar, si la fecha de fin es anterior a la de inicio. */
    suspend fun setDates(id: Long, startedOn: LocalDate?, finishedOn: LocalDate?): Boolean {
        if (!areValidDates(startedOn, finishedOn)) return false
        edit(id) { it.copy(startedOn = startedOn, finishedOn = finishedOn, updatedAt = now()) }
        return true
    }

    suspend fun delete(id: Long) {
        val coverUri = dao.getById(id)?.coverUri
        dao.deleteById(id)
        covers.delete(coverUri)
    }

    private suspend fun edit(id: Long, change: (Book) -> Book) {
        val book = dao.getById(id)?.toDomain() ?: return
        dao.update(change(book).toEntity())
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    // Room guarda milisegundos; se trunca aquí para que el valor en memoria coincida con el guardado.
    private fun now(): Instant = clock.instant().truncatedTo(ChronoUnit.MILLIS)
}
