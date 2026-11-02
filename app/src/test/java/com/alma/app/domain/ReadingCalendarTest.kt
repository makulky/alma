package com.alma.app.domain

import com.alma.app.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReadingCalendarTest {
    @Test
    fun gridStartsOnMonday() {
        // Noviembre de 2026 empieza en domingo: seis huecos antes del día 1.
        val grid = monthGrid(YearMonth.of(2026, 11))
        assertTrue(grid.take(6).all { it == null })
        assertEquals(LocalDate.of(2026, 11, 1), grid[6])
    }

    @Test
    fun monthStartingOnMondayHasNoLeadingGaps() {
        // Junio de 2026 empieza en lunes.
        assertEquals(LocalDate.of(2026, 6, 1), monthGrid(YearMonth.of(2026, 6)).first())
    }

    @Test
    fun gridIsCompleteWeeksWithEveryDay() {
        listOf(YearMonth.of(2026, 2), YearMonth.of(2024, 2), YearMonth.of(2026, 11)).forEach { month ->
            val grid = monthGrid(month)
            assertEquals(0, grid.size % 7)
            assertEquals(month.lengthOfMonth(), grid.count { it != null })
        }
    }

    @Test
    fun eventsAreGroupedByDayInsideTheMonth() {
        val month = YearMonth.of(2026, 11)
        val day = LocalDate.of(2026, 11, 10)
        val books = listOf(
            book(title = "A").copy(startedOn = day, finishedOn = LocalDate.of(2026, 12, 1)),
            book(title = "B").copy(startedOn = LocalDate.of(2026, 10, 1), finishedOn = day),
            book(title = "C"),
        )
        val events = calendarEvents(books, month)
        assertEquals(setOf(day), events.keys)
        val onDay = events.getValue(day)
        assertEquals(CalendarEventType.STARTED, onDay.first { it.book.title == "A" }.type)
        assertEquals(CalendarEventType.FINISHED, onDay.first { it.book.title == "B" }.type)
        assertNull(onDay.firstOrNull { it.book.title == "C" })
    }

    @Test
    fun bookStartedAndFinishedTheSameDayHasTwoEvents() {
        val day = LocalDate.of(2026, 11, 10)
        val events = calendarEvents(listOf(book().copy(startedOn = day, finishedOn = day)), YearMonth.of(2026, 11))
        assertEquals(2, events.getValue(day).size)
    }
}
