package com.alma.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLibraryMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(text: String) = json.decodeFromString<WorkDocDto>(text)

    @Test
    fun usesTheSpanishEditionReturnedInsideTheWork() {
        val result = parse(
            """
            {
              "key": "/works/OL27729743W",
              "title": "The Housemaid",
              "author_name": ["Freida McFadden", "Otra Autora"],
              "cover_i": 111,
              "first_publish_year": 2022,
              "number_of_pages_median": 336,
              "desconocido": true,
              "editions": {"docs": [{
                "key": "/books/OL48293412M",
                "title": "La empleada",
                "cover_i": 222,
                "isbn": ["1644739070", "9781644739075"],
                "publisher": ["Vintage Español"],
                "publish_date": ["2023"]
              }]}
            }
            """,
        ).toSearchResult()!!
        assertEquals("OL48293412M", result.sourceId)
        assertEquals("OL27729743W", result.workId)
        assertEquals("La empleada", result.title)
        assertEquals("Freida McFadden, Otra Autora", result.authors)
        assertEquals("https://covers.openlibrary.org/b/id/222-M.jpg", result.coverUri)
        assertEquals("9781644739075", result.isbn13)
        assertEquals("Vintage Español", result.publisher)
        assertEquals(2023, result.publishedYear)
        assertEquals(336, result.pageCount)
    }

    @Test
    fun fallsBackToTheWorkWithoutEdition() {
        val result = parse(
            """{"key": "/works/OL1W", "title": "Obra", "cover_i": 111, "first_publish_year": 1963}""",
        ).toSearchResult()!!
        assertEquals("OL1W", result.sourceId)
        assertEquals("Obra", result.title)
        assertEquals("https://covers.openlibrary.org/b/id/111-M.jpg", result.coverUri)
        assertEquals(1963, result.publishedYear)
    }

    @Test
    fun toleratesMissingFields() {
        val result = parse("""{"key": "/works/OL1W", "title": "Solo título"}""").toSearchResult()!!
        assertEquals("", result.authors)
        assertNull(result.coverUri)
        assertNull(result.isbn13)
        assertNull(result.publisher)
        assertNull(result.pageCount)
        assertNull(result.publishedYear)
    }

    @Test
    fun acceptsTextInsteadOfListsAndVariedDates() {
        val dates = mapOf("05/10/2023" to 2023, "May 1979" to 1979, "January 1, 1995" to 1995)
        dates.forEach { (date, year) ->
            val result = parse(
                """
                {"key": "/works/OL1W", "title": "T", "editions": {"docs": [
                  {"key": "/books/OL2M", "publisher": "SUMA", "publish_date": "$date", "isbn": "9788491294283"}
                ]}}
                """,
            ).toSearchResult()!!
            assertEquals(year, result.publishedYear)
            assertEquals("SUMA", result.publisher)
            assertEquals("9788491294283", result.isbn13)
        }
    }

    @Test
    fun ignoresIsbn10AndDatesWithoutYear() {
        val result = parse(
            """
            {"key": "/works/OL1W", "title": "T", "first_publish_year": 1990, "editions": {"docs": [
              {"key": "/books/OL2M", "isbn": ["842051912X"], "publish_date": ["desconocida"]}
            ]}}
            """,
        ).toSearchResult()!!
        assertNull(result.isbn13)
        assertEquals(1990, result.publishedYear)
    }

    @Test
    fun discardsWorksThatCannotBeSaved() {
        assertNull(parse("""{"title": "Sin clave"}""").toSearchResult())
        assertNull(parse("""{"key": "/works/OL1W"}""").toSearchResult())
        assertNull(parse("""{"key": "/works/OL1W", "title": " "}""").toSearchResult())
    }

    @Test
    fun emptySearchHasNoDocs() {
        val response = json.decodeFromString<SearchResponse>("""{"numFound": 0}""")
        assertEquals(emptyList<WorkDocDto>(), response.docs)
    }

    @Test
    fun readsSynopsisAsTextOrObjectWithoutMarks() {
        val asText = json.decodeFromString<WorkDto>("""{"description": "*Rayuela* es una novela."}""")
        val asObject = json.decodeFromString<WorkDto>(
            """{"description": {"type": "/type/text", "value": "Una **gran** novela."}}""",
        )
        assertEquals("Rayuela es una novela.", asText.synopsis())
        assertEquals("Una gran novela.", asObject.synopsis())
        assertNull(json.decodeFromString<WorkDto>("""{"title": "Sin sinopsis"}""").synopsis())
    }
}
