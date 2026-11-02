package com.alma.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankingTest {
    private fun result(id: String, title: String = id, cover: String? = "https://x/$id.jpg") = SearchResult(
        sourceId = id,
        workId = id,
        title = title,
        authors = "",
        coverUri = cover,
        isbn13 = null,
        publisher = null,
        pageCount = null,
        publishedYear = null,
    )

    private fun ids(results: List<SearchResult>) = results.map { it.sourceId }

    @Test
    fun exactTitleGoesFirstThenPrefixThenContainsThenRest() {
        val ranked = rankSearchResults(
            "la asistenta",
            listOf(
                result("otro", "Novelas de misterio"),
                result("contiene", "Manual de la asistenta"),
                result("empieza", "La asistenta te vigila"),
                result("exacto", "La asistenta"),
            ),
        )
        assertEquals(listOf("exacto", "empieza", "contiene", "otro"), ids(ranked))
    }

    @Test
    fun ignoresCaseAccentsAndExtraSpaces() {
        val ranked = rankSearchResults(
            "  el  ultimo   CATON ",
            listOf(result("otro", "Otro libro"), result("exacto", "El último catón")),
        )
        assertEquals("exacto", ranked.first().sourceId)
    }

    @Test
    fun booksWithCoverGoFirstWithinTheSameMatch() {
        val ranked = rankSearchResults(
            "dune",
            listOf(result("sin", "Dune", cover = null), result("con", "Dune")),
        )
        assertEquals(listOf("con", "sin"), ids(ranked))
    }

    @Test
    fun keepsSourceOrderWhenEverythingElseIsEqual() {
        val ranked = rankSearchResults("zzz", listOf(result("b"), result("a"), result("c")))
        assertEquals(listOf("b", "a", "c"), ids(ranked))
    }

    @Test
    fun removesRepeatedBooks() {
        val ranked = rankSearchResults("dune", listOf(result("a", "Dune"), result("a", "Dune"), result("b", "Dune")))
        assertEquals(listOf("a", "b"), ids(ranked))
    }
}
