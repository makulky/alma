package com.alma.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlmaDatabase::class.java,
        listOf(RenameVolumeIdToSourceId()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun keepsBooksWhenRenamingTheIdentifier() {
        helper.createDatabase(databaseName, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO books (id, googleVolumeId, title, authors, status, ratingHalves, comment,
                    startedOn, finishedOn, addedAt, updatedAt)
                VALUES (7, 'vol', 'Rayuela', 'Julio Cortázar', 'READ', 7, 'Me ha encantado',
                    '2026-11-01', '2026-11-02', 1000, 2000)
                """,
            )
        }

        helper.runMigrationsAndValidate(databaseName, 2, true).use { database ->
            database.query("SELECT id, sourceId, title, ratingHalves, comment, finishedOn FROM books").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(7L, cursor.getLong(0))
                assertEquals("vol", cursor.getString(1))
                assertEquals("Rayuela", cursor.getString(2))
                assertEquals(7, cursor.getInt(3))
                assertEquals("Me ha encantado", cursor.getString(4))
                assertEquals("2026-11-02", cursor.getString(5))
            }
        }
    }
}
