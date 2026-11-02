package com.alma.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {
    /** Con [language], Open Library devuelve en cada obra su edición en ese idioma, si la tiene. */
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("lang") language: String = "es",
        @Query("limit") limit: Int = 20,
        @Query("fields") fields: String = SEARCH_FIELDS,
    ): SearchResponse

    @GET("works/{id}.json")
    suspend fun work(@Path("id") workId: String): WorkDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"

        private const val SEARCH_FIELDS =
            "key,title,author_name,cover_i,first_publish_year,number_of_pages_median," +
                "editions,editions.key,editions.title,editions.isbn,editions.cover_i," +
                "editions.publisher,editions.publish_date"
    }
}

/** Open Library pide que las apps se identifiquen. */
object OpenLibraryUserAgent : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", "Alma/0.1 (Android)").build())
}

@Serializable
data class SearchResponse(val docs: List<WorkDocDto> = emptyList())

@Serializable
data class WorkDocDto(
    val key: String? = null,
    val title: String? = null,
    @SerialName("author_name") val authorNames: List<String> = emptyList(),
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("number_of_pages_median") val pagesMedian: Int? = null,
    val editions: EditionsDto? = null,
)

@Serializable
data class EditionsDto(val docs: List<EditionDocDto> = emptyList())

// Estos campos pueden llegar como texto o como lista: se leen con JsonElement para aceptar ambos.
@Serializable
data class EditionDocDto(
    val key: String? = null,
    val title: String? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    val isbn: JsonElement? = null,
    val publisher: JsonElement? = null,
    @SerialName("publish_date") val publishDate: JsonElement? = null,
)

/** La descripción de una obra puede ser un texto o un objeto con el texto en "value". */
@Serializable
data class WorkDto(val description: JsonElement? = null)
