package com.alma.app.data.backup

import com.alma.app.T0
import com.alma.app.book
import com.alma.app.data.localCoverRef
import com.alma.app.domain.ReadingStatus
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRoundTripTest {
    @get:Rule val temp = TemporaryFolder()

    private val coverName = "portada-1.jpg"
    private val coverBytes = byteArrayOf(1, 2, 3, 4)

    private val complete = book(status = ReadingStatus.READ).copy(
        sourceId = "vol",
        coverUri = "https://x/t.jpg",
        description = "Sinopsis con acentos: áéíóú ñ",
        isbn13 = "9788437604572",
        publisher = "Cátedra",
        pageCount = 600,
        publishedYear = 1963,
        ratingHalves = 7,
        comment = "Me ha encantado",
        startedOn = LocalDate.of(2026, 1, 2),
        finishedOn = LocalDate.of(2026, 2, 3),
    )

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun backupJson(body: String) = """{"version": $BACKUP_VERSION, "exportedAt": "$T0", "books": [$body]}"""

    private fun bookJson(extra: String = "") =
        """{"title": "T", "authors": "A", "status": "READ", "addedAt": "$T0", "updatedAt": "$T0"$extra}"""

    private fun read(bytes: ByteArray, staging: File = temp.newFolder()) =
        readBackup(ByteArrayInputStream(bytes), staging)

    private fun assertRejected(expected: BackupError, bytes: ByteArray) {
        val error = assertThrows(BackupException::class.java) { read(bytes) }
        assertEquals(expected, error.error)
    }

    @Test
    fun exportAndImportKeepEverything() {
        val cover = temp.newFile(coverName).apply { writeBytes(coverBytes) }
        val manual = book(title = "Manual").copy(coverUri = localCoverRef(coverName))
        val out = ByteArrayOutputStream()

        writeBackup(out, listOf(complete, manual), { if (it == manual.coverUri) cover else null }, T0)

        val staging = temp.newFolder()
        val parsed = read(out.toByteArray(), staging)
        assertEquals(listOf(complete, manual), parsed.books)
        assertArrayEquals(coverBytes, File(staging, coverName).readBytes())
    }

    @Test
    fun missingCoverFileIsExportedWithoutCover() {
        val manual = book().copy(coverUri = localCoverRef(coverName))
        val out = ByteArrayOutputStream()

        writeBackup(out, listOf(manual), { File(temp.root, "no-existe.jpg") }, T0)

        assertNull(read(out.toByteArray()).books.single().coverUri)
    }

    @Test
    fun rejectsFilesThatAreNotZips() {
        assertRejected(BackupError.CORRUPT, "esto no es un zip".toByteArray())
        assertRejected(BackupError.CORRUPT, ByteArray(0))
    }

    @Test
    fun rejectsTruncatedFiles() {
        val out = ByteArrayOutputStream()
        writeBackup(out, listOf(complete), { null }, T0)
        val truncated = out.toByteArray().copyOf(out.size() / 2)
        assertRejected(BackupError.CORRUPT, truncated)
    }

    @Test
    fun rejectsZipWithoutBackupJson() {
        assertRejected(BackupError.CORRUPT, zipOf("otra-cosa.txt" to byteArrayOf(1)))
    }

    @Test
    fun rejectsMalformedJson() {
        assertRejected(BackupError.CORRUPT, zipOf(BACKUP_JSON_ENTRY to "{ no es json".toByteArray()))
    }

    @Test
    fun rejectsNewerVersionEvenIfTheRestIsUnknown() {
        val json = """{"version": ${BACKUP_VERSION + 1}, "formatoNuevo": true}"""
        assertRejected(BackupError.NEWER_VERSION, zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
    }

    @Test
    fun rejectsImpossibleData() {
        listOf(
            """, "ratingHalves": 11""",
            """, "ratingHalves": 1""",
            """, "startedOn": "2026-02-01", "finishedOn": "2026-01-01"""",
            """, "startedOn": "no-es-fecha"""",
        ).forEach { extra ->
            val json = backupJson(bookJson(extra))
            assertRejected(BackupError.CORRUPT, zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
        }
    }

    @Test
    fun rejectsUnknownStatus() {
        val json = backupJson("""{"title": "T", "authors": "A", "status": "OTRO", "addedAt": "$T0", "updatedAt": "$T0"}""")
        assertRejected(BackupError.CORRUPT, zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
    }

    @Test
    fun rejectsCoverReferencedButMissing() {
        val json = backupJson(bookJson(""", "coverUri": "local:$coverName""""))
        assertRejected(BackupError.CORRUPT, zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
    }

    @Test
    fun rejectsZipEntriesThatTryToLeaveTheCoversFolder() {
        val staging = temp.newFolder("parent", "staging")
        listOf("covers/../evil.jpg", "covers/../../evil.jpg", "covers/sub/evil.jpg", "covers/evil.exe").forEach { name ->
            val bytes = zipOf(name to byteArrayOf(1), BACKUP_JSON_ENTRY to backupJson("").toByteArray())
            val error = assertThrows(BackupException::class.java) { read(bytes, staging) }
            assertEquals(BackupError.INVALID_PATH, error.error)
        }
        assertFalse(File(staging.parentFile, "evil.jpg").exists())
        assertFalse(File(staging.parentFile!!.parentFile, "evil.jpg").exists())
    }

    @Test
    fun rejectsBooksPointingOutsideTheCoversFolder() {
        listOf("local:../../alma.db", "file:///data/data/com.alma.app/databases/alma.db", "content://x/y").forEach { uri ->
            val json = backupJson(bookJson(""", "coverUri": "$uri""""))
            assertRejected(BackupError.INVALID_PATH, zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
        }
    }

    @Test
    fun readsBackupsFromVersionOneWithTheOldIdentifierName() {
        val json = """{"version": 1, "exportedAt": "$T0", "books": [${bookJson(""", "googleVolumeId": "vol"""")}]}"""
        val parsed = read(zipOf(BACKUP_JSON_ENTRY to json.toByteArray()))
        assertEquals("vol", parsed.books.single().sourceId)
    }

    @Test
    fun ignoresUnexpectedEntries() {
        val json = backupJson(bookJson())
        val parsed = read(zipOf("../fuera.txt" to byteArrayOf(1), BACKUP_JSON_ENTRY to json.toByteArray()))
        assertEquals(1, parsed.books.size)
        assertFalse(File(temp.root, "fuera.txt").exists())
    }
}
