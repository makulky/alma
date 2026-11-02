package com.alma.app.domain

import java.time.LocalDate
import java.time.YearMonth

enum class CalendarEventType { STARTED, FINISHED }

data class CalendarEvent(val date: LocalDate, val type: CalendarEventType, val book: Book)

/**
 * Celdas de un mes con la semana empezando en lunes: los huecos antes del día 1 son nulos y la
 * lista se completa hasta un múltiplo de 7.
 */
fun monthGrid(month: YearMonth): List<LocalDate?> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailing = (7 - (leading + days.size) % 7) % 7
    return List(leading) { null } + days + List(trailing) { null }
}

/** Eventos de inicio y fin de lectura dentro de un mes, agrupados por día. */
fun calendarEvents(books: List<Book>, month: YearMonth): Map<LocalDate, List<CalendarEvent>> =
    books.flatMap { book ->
        listOfNotNull(
            book.startedOn?.let { CalendarEvent(it, CalendarEventType.STARTED, book) },
            book.finishedOn?.let { CalendarEvent(it, CalendarEventType.FINISHED, book) },
        )
    }.filter { YearMonth.from(it.date) == month }.groupBy { it.date }
