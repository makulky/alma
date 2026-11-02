package com.alma.app.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val COVER_URL = "https://covers.openlibrary.org/b/id/%d-M.jpg"
private const val ISBN13_LENGTH = 13
private val YEAR = Regex("\\b(\\d{4})\\b")

/** Libro encontrado en Open Library, todavía sin guardar en la biblioteca. */
data class SearchResult(
    /** Identifica la edición, o la obra si no hay datos de edición. */
    val sourceId: String,
    val workId: String,
    val title: String,
    val authors: String,
    val coverUri: String?,
    val isbn13: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedYear: Int?,
)

/** Devuelve null si faltan el identificador o el título, sin los que el libro no se puede guardar. */
fun WorkDocDto.toSearchResult(): SearchResult? {
    val workId = key?.toOpenLibraryId() ?: return null
    val edition = editions?.docs?.firstOrNull()
    val bookTitle = edition?.title?.takeIf { it.isNotBlank() } ?: title?.takeIf { it.isNotBlank() } ?: return null
    return SearchResult(
        sourceId = edition?.key?.toOpenLibraryId() ?: workId,
        workId = workId,
        title = bookTitle,
        authors = authorNames.joinToString(", "),
        coverUri = (edition?.coverId ?: coverId)?.let { COVER_URL.format(it) },
        isbn13 = edition?.isbn.texts().firstOrNull { it.length == ISBN13_LENGTH },
        publisher = edition?.publisher.texts().firstOrNull { it.isNotBlank() },
        pageCount = pagesMedian?.takeIf { it > 0 },
        publishedYear = edition?.publishDate.texts().firstNotNullOfOrNull { yearIn(it) } ?: firstPublishYear,
    )
}

/** Texto de la sinopsis, sin las marcas de cursiva y negrita que usa Open Library. */
fun WorkDto.synopsis(): String? {
    val text = when (val value = description) {
        is JsonPrimitive -> value.content
        is JsonObject -> (value["value"] as? JsonPrimitive)?.content
        else -> null
    }
    return text?.replace("*", "")?.trim()?.takeIf { it.isNotEmpty() }
}

private fun String.toOpenLibraryId(): String? = substringAfterLast('/').takeIf { it.isNotBlank() }

private fun yearIn(date: String): Int? = YEAR.find(date)?.value?.toInt()

private fun JsonElement?.texts(): List<String> = when (this) {
    is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.content }
    is JsonPrimitive -> listOf(content)
    else -> emptyList()
}
