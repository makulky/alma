package com.alma.app.data.remote

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

/** Quita los repetidos y pone primero los libros cuyo título mejor coincide con la búsqueda. */
fun rankSearchResults(query: String, results: List<SearchResult>): List<SearchResult> {
    val normalizedQuery = normalize(query)
    return results
        .distinctBy { it.sourceId }
        .sortedWith(
            compareBy<SearchResult> { titleMatchRank(normalize(it.title), normalizedQuery) }
                .thenBy { it.coverUri == null },
        )
}

private fun titleMatchRank(title: String, query: String): Int = when {
    title == query -> 0
    title.startsWith(query) -> 1
    title.contains(query) -> 2
    else -> 3
}

private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(WHITESPACE, " ")
        .trim()
        .lowercase()
