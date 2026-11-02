package com.alma.app.domain

import java.time.YearMonth

data class MonthHistory(val month: YearMonth, val books: List<Book>)

data class YearHistory(val year: Int, val months: List<MonthHistory>) {
    val count: Int get() = months.sumOf { it.books.size }
}

data class ReadingStats(
    val readCount: Int,
    /** Libros terminados por año y mes, de lo más reciente a lo más antiguo. */
    val history: List<YearHistory>,
    /** Libros leídos sin fecha de fin, que solo pueden venir de una copia importada. */
    val undatedBooks: List<Book>,
    /** Nota media en estrellas, sin contar los libros no puntuados; nula si no hay ninguno. */
    val averageStars: Double?,
    /** Número de libros por puntuación, de 2 a 10 medias estrellas. Incluye las puntuaciones a cero. */
    val ratingDistribution: List<Pair<Int, Int>>,
)

fun computeReadingStats(books: List<Book>): ReadingStats {
    val read = books.filter { it.status == ReadingStatus.READ }
    val ratings = read.mapNotNull { it.ratingHalves }
    return ReadingStats(
        readCount = read.size,
        history = readingHistory(read),
        undatedBooks = read.filter { it.finishedOn == null },
        averageStars = if (ratings.isEmpty()) null else ratings.average() / 2.0,
        ratingDistribution = (MIN_RATING_HALVES..MAX_RATING_HALVES).map { halves ->
            halves to ratings.count { it == halves }
        },
    )
}

private fun readingHistory(read: List<Book>): List<YearHistory> =
    read.filter { it.finishedOn != null }
        .sortedByDescending { it.finishedOn }
        .groupBy { YearMonth.from(it.finishedOn) }
        .map { (month, books) -> MonthHistory(month, books) }
        .groupBy { it.month.year }
        .map { (year, months) -> YearHistory(year, months) }
