package com.alma.app.domain

import com.alma.app.T0
import com.alma.app.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatusTransitionsTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val earlier = LocalDate.of(2026, 10, 1)
    private val now = T0.plusSeconds(60)

    @Test
    fun startingSetsTheStartDateOnlyIfEmpty() {
        val started = book().withStatus(ReadingStatus.READING, today, now)
        assertEquals(today, started.startedOn)
        assertEquals(now, started.updatedAt)

        val kept = book().copy(startedOn = earlier).withStatus(ReadingStatus.READING, today, now)
        assertEquals(earlier, kept.startedOn)
    }

    @Test
    fun finishingSetsTheFinishDateOnlyIfEmpty() {
        val finished = book(status = ReadingStatus.READING).copy(startedOn = earlier)
            .withStatus(ReadingStatus.READ, today, now)
        assertEquals(today, finished.finishedOn)
        assertEquals(earlier, finished.startedOn)
    }

    @Test
    fun leavingReadClearsTheFinishDate() {
        val read = book(status = ReadingStatus.READ).copy(startedOn = earlier, finishedOn = today)
        val reading = read.withStatus(ReadingStatus.READING, today, now)
        assertNull(reading.finishedOn)
        assertEquals(earlier, reading.startedOn)
    }

    @Test
    fun returningToPendingClearsBothDates() {
        val read = book(status = ReadingStatus.READ).copy(startedOn = earlier, finishedOn = today)
        val pending = read.withStatus(ReadingStatus.PENDING, today, now)
        assertNull(pending.startedOn)
        assertNull(pending.finishedOn)
    }

    @Test
    fun ratingAndCommentSurviveStatusChanges() {
        val read = book(status = ReadingStatus.READ).copy(ratingHalves = 7, comment = "Muy bueno")
        val back = read.withStatus(ReadingStatus.PENDING, today, now).withStatus(ReadingStatus.READ, today, now)
        assertEquals(7, back.ratingHalves)
        assertEquals("Muy bueno", back.comment)
    }

    @Test
    fun sameStatusChangesNothing() {
        val reading = book(status = ReadingStatus.READING)
        assertSame(reading, reading.withStatus(ReadingStatus.READING, today, now))
    }

    @Test
    fun finishDateCannotBeBeforeStartDate() {
        assertTrue(areValidDates(earlier, today))
        assertTrue(areValidDates(today, today))
        assertTrue(areValidDates(null, today))
        assertTrue(areValidDates(earlier, null))
        assertFalse(areValidDates(today, earlier))
    }
}
