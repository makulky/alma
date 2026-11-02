package com.alma.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alma.app.domain.ReadingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BookDaoTest {
    private lateinit var database: AlmaDatabase
    private lateinit var dao: BookDao

    private fun entity(
        title: String = "T",
        sourceId: String? = null,
        isbn: String? = null,
        startedOn: LocalDate? = null,
        finishedOn: LocalDate? = null,
    ) = BookEntity(
        sourceId = sourceId,
        title = title,
        authors = "A",
        coverUri = null,
        description = null,
        isbn13 = isbn,
        publisher = null,
        pageCount = null,
        publishedYear = null,
        status = ReadingStatus.READ,
        ratingHalves = 7,
        comment = null,
        startedOn = startedOn,
        finishedOn = finishedOn,
        addedAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000),
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AlmaDatabase::class.java).build()
        dao = database.bookDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun storesAndReadsEveryConvertedType() = runTest {
        val id = dao.insert(entity(startedOn = LocalDate.of(2026, 1, 2), finishedOn = LocalDate.of(2026, 2, 3)))
        val stored = dao.getById(id)!!
        assertEquals(ReadingStatus.READ, stored.status)
        assertEquals(LocalDate.of(2026, 1, 2), stored.startedOn)
        assertEquals(LocalDate.of(2026, 2, 3), stored.finishedOn)
        assertEquals(Instant.ofEpochMilli(2_000), stored.updatedAt)
    }

    @Test
    fun monthRangeIncludesBothEndsAndEitherDate() = runTest {
        dao.insert(entity("first day", startedOn = LocalDate.of(2026, 11, 1)))
        dao.insert(entity("last day", finishedOn = LocalDate.of(2026, 11, 30)))
        dao.insert(entity("started before, finished inside", startedOn = LocalDate.of(2026, 10, 1), finishedOn = LocalDate.of(2026, 11, 15)))
        dao.insert(entity("before", startedOn = LocalDate.of(2026, 10, 31)))
        dao.insert(entity("after", finishedOn = LocalDate.of(2026, 12, 1)))
        dao.insert(entity("no dates"))

        val titles = dao.observeWithDatesBetween("2026-11-01", "2026-11-30").first().map { it.title }.toSet()

        assertEquals(setOf("first day", "last day", "started before, finished inside"), titles)
    }

    @Test
    fun sourceIdIsUniqueButManualBooksCanCoexist() = runTest {
        dao.insert(entity(sourceId = "vol"))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.insert(entity(sourceId = "vol")) }
        }
        dao.insert(entity(sourceId = null))
        dao.insert(entity(sourceId = null))
        assertEquals(3, dao.getAll().size)
    }

    @Test
    fun findsDuplicatesByVolumeAndIsbn() = runTest {
        val id = dao.insert(entity(sourceId = "vol", isbn = "978"))
        assertEquals(id, dao.findBySourceId("vol")?.id)
        assertEquals(id, dao.findByIsbn("978")?.id)
        assertNull(dao.findBySourceId("otro"))
        assertNull(dao.findByIsbn("otro"))
    }

    @Test
    fun failedImportLeavesTheDatabaseUnchanged() = runTest {
        val id = dao.insert(entity("original", sourceId = "vol"))
        val updated = dao.getById(id)!!.copy(title = "cambiado")

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                dao.applyImport(listOf(entity("nuevo"), entity("repetido", sourceId = "vol")), listOf(updated))
            }
        }

        val all = dao.getAll()
        assertEquals(listOf("original"), all.map { it.title })
    }

    @Test
    fun importInsertsAndUpdatesTogether() = runTest {
        val id = dao.insert(entity("original"))
        val updated = dao.getById(id)!!.copy(title = "cambiado")

        dao.applyImport(listOf(entity("nuevo")), listOf(updated))

        assertTrue(dao.getAll().map { it.title }.containsAll(listOf("cambiado", "nuevo")))
    }
}
