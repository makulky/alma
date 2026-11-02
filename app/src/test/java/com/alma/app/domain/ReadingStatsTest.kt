package com.alma.app.domain

import com.alma.app.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReadingStatsTest {
    private fun read(year: Int, halves: Int?) = book(status = ReadingStatus.READ)
        .copy(finishedOn = LocalDate.of(year, 5, 1), ratingHalves = halves)

    @Test
    fun onlyCountsReadBooks() {
        val stats = computeReadingStats(
            listOf(
                read(2026, 8),
                book(status = ReadingStatus.READING).copy(ratingHalves = 2),
                book(status = ReadingStatus.PENDING),
            ),
        )
        assertEquals(1, stats.readCount)
        assertEquals(4.0, stats.averageStars!!, 0.001)
    }

    @Test
    fun averageIgnoresUnratedBooks() {
        val stats = computeReadingStats(listOf(read(2026, 10), read(2026, 7), read(2026, null)))
        assertEquals(3, stats.readCount)
        assertEquals(4.25, stats.averageStars!!, 0.001)
    }

    @Test
    fun averageIsNullWithoutRatings() {
        assertNull(computeReadingStats(listOf(read(2026, null))).averageStars)
        assertNull(computeReadingStats(emptyList()).averageStars)
    }

    private fun finished(title: String, date: LocalDate) =
        book(title = title, status = ReadingStatus.READ).copy(finishedOn = date)

    @Test
    fun groupsByYearAndMonthNewestFirst() {
        val stats = computeReadingStats(
            listOf(
                finished("2024-marzo", LocalDate.of(2024, 3, 10)),
                finished("2026-enero", LocalDate.of(2026, 1, 5)),
                finished("2026-junio-pronto", LocalDate.of(2026, 6, 2)),
                finished("2026-junio-tarde", LocalDate.of(2026, 6, 20)),
            ),
        )

        assertEquals(listOf(2026, 2024), stats.history.map { it.year })
        val year2026 = stats.history.first()
        assertEquals(3, year2026.count)
        assertEquals(listOf(YearMonth.of(2026, 6), YearMonth.of(2026, 1)), year2026.months.map { it.month })
        assertEquals(listOf("2026-junio-tarde", "2026-junio-pronto"), year2026.months.first().books.map { it.title })
    }

    @Test
    fun sameMonthOfDifferentYearsIsNotMixed() {
        val stats = computeReadingStats(
            listOf(finished("A", LocalDate.of(2026, 11, 1)), finished("B", LocalDate.of(2025, 11, 1))),
        )
        assertEquals(listOf("A"), stats.history[0].months.single().books.map { it.title })
        assertEquals(listOf("B"), stats.history[1].months.single().books.map { it.title })
    }

    @Test
    fun booksWithoutFinishDateGoToTheirOwnGroup() {
        val undated = book(title = "Sin fecha", status = ReadingStatus.READ)
        val stats = computeReadingStats(listOf(undated, finished("Con fecha", LocalDate.of(2026, 1, 1))))
        assertEquals(listOf(undated), stats.undatedBooks)
        assertEquals(1, stats.history.single().count)
    }

    @Test
    fun historyOnlyHasReadBooks() {
        val stats = computeReadingStats(
            listOf(
                book(status = ReadingStatus.READING).copy(finishedOn = LocalDate.of(2026, 1, 1)),
                book(status = ReadingStatus.PENDING),
            ),
        )
        assertEquals(emptyList<YearHistory>(), stats.history)
        assertEquals(emptyList<Any>(), stats.undatedBooks)
    }

    @Test
    fun groupsAddUpToTheTotal() {
        val stats = computeReadingStats(
            listOf(
                finished("A", LocalDate.of(2026, 1, 1)),
                finished("B", LocalDate.of(2026, 2, 1)),
                finished("C", LocalDate.of(2025, 2, 1)),
                book(title = "D", status = ReadingStatus.READ),
            ),
        )
        assertEquals(stats.readCount, stats.history.sumOf { it.count } + stats.undatedBooks.size)
    }

    @Test
    fun distributionCoversEveryRating() {
        val stats = computeReadingStats(listOf(read(2026, 7), read(2026, 7), read(2026, 2)))
        assertEquals(9, stats.ratingDistribution.size)
        assertEquals(2 to 1, stats.ratingDistribution.first())
        assertEquals(10 to 0, stats.ratingDistribution.last())
        assertEquals(2, stats.ratingDistribution.first { it.first == 7 }.second)
    }
}
