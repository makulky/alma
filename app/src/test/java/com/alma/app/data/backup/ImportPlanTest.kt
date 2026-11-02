package com.alma.app.data.backup

import com.alma.app.T0
import com.alma.app.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPlanTest {
    private val later = T0.plusSeconds(60)

    @Test
    fun newBooksAreInsertedWithoutReusingIds() {
        val plan = planImport(emptyList(), listOf(book().copy(id = 42)))
        assertEquals(0, plan.toInsert.single().id)
        assertTrue(plan.toUpdate.isEmpty())
    }

    @Test
    fun booksMissingFromTheBackupAreNeverTouched() {
        val existing = book(title = "Solo en el móvil").copy(id = 1)
        val plan = planImport(listOf(existing), listOf(book(title = "Solo en la copia")))
        assertEquals(1, plan.toInsert.size)
        assertTrue(plan.toUpdate.isEmpty())
    }

    @Test
    fun newerBackupVersionWinsAndKeepsTheLocalId() {
        val existing = book().copy(id = 7, sourceId = "vol", comment = "antiguo")
        val incoming = book(updatedAt = later).copy(sourceId = "vol", comment = "nuevo")
        val plan = planImport(listOf(existing), listOf(incoming))
        assertTrue(plan.toInsert.isEmpty())
        assertEquals(7, plan.toUpdate.single().id)
        assertEquals("nuevo", plan.toUpdate.single().comment)
    }

    @Test
    fun olderOrEqualBackupVersionChangesNothing() {
        val existing = book(updatedAt = later).copy(id = 7, sourceId = "vol", comment = "reciente")
        val older = book().copy(sourceId = "vol", comment = "viejo")
        val same = book(updatedAt = later).copy(sourceId = "vol", comment = "igual")

        listOf(older, same).forEach { incoming ->
            val plan = planImport(listOf(existing), listOf(incoming))
            assertTrue(plan.toInsert.isEmpty())
            assertTrue(plan.toUpdate.isEmpty())
            assertEquals(1, plan.unchanged)
        }
    }

    @Test
    fun matchesByIsbnWhenVolumeDiffers() {
        val existing = book(title = "Edición A").copy(id = 1, sourceId = "a", isbn13 = "978")
        val incoming = book(title = "Edición B", updatedAt = later).copy(sourceId = "b", isbn13 = "978")
        val plan = planImport(listOf(existing), listOf(incoming))
        assertTrue(plan.toInsert.isEmpty())
        assertEquals(1, plan.toUpdate.single().id)
    }

    @Test
    fun volumeMatchHasPriorityOverIsbn() {
        val byIsbn = book(title = "Por ISBN").copy(id = 1, sourceId = "otro", isbn13 = "978")
        val byVolume = book(title = "Por volumen").copy(id = 2, sourceId = "vol")
        val incoming = book(updatedAt = later).copy(sourceId = "vol", isbn13 = "978")
        val plan = planImport(listOf(byIsbn, byVolume), listOf(incoming))
        assertEquals(2, plan.toUpdate.single().id)
    }

    @Test
    fun manualBooksMatchByTitleAndAuthorIgnoringCaseAndSpaces() {
        val existing = book(title = "Mi Libro", authors = "Ana").copy(id = 3)
        val incoming = book(title = " mi libro ", authors = "ANA", updatedAt = later)
        val plan = planImport(listOf(existing), listOf(incoming))
        assertTrue(plan.toInsert.isEmpty())
        assertEquals(3, plan.toUpdate.single().id)
    }

    @Test
    fun sameTitleAndAuthorDoesNotMatchBooksFromGoogle() {
        val existing = book().copy(id = 3, sourceId = "vol")
        val plan = planImport(listOf(existing), listOf(book(updatedAt = later)))
        assertEquals(1, plan.toInsert.size)
        assertTrue(plan.toUpdate.isEmpty())
    }

    @Test
    fun updateKeepsIdentifiersTheBackupLacks() {
        val existing = book().copy(id = 3, isbn13 = "978", sourceId = "vol")
        val incoming = book(updatedAt = later).copy(isbn13 = "978")
        assertEquals("vol", planImport(listOf(existing), listOf(incoming)).toUpdate.single().sourceId)
    }

    @Test
    fun duplicatesInsideTheBackupAreInsertedOnceWithTheNewestVersion() {
        val old = book().copy(sourceId = "vol", comment = "viejo")
        val new = book(updatedAt = later).copy(sourceId = "vol", comment = "nuevo")
        val plan = planImport(emptyList(), listOf(old, new))
        assertEquals("nuevo", plan.toInsert.single().comment)
    }

    @Test
    fun importingTheSameBackupTwiceChangesNothing() {
        val books = listOf(book(title = "A").copy(sourceId = "a"), book(title = "B"))
        val first = planImport(emptyList(), books)
        val stored = first.toInsert.mapIndexed { index, book -> book.copy(id = index + 1L) }
        val second = planImport(stored, books)
        assertTrue(second.toInsert.isEmpty())
        assertTrue(second.toUpdate.isEmpty())
        assertEquals(2, second.unchanged)
    }
}
