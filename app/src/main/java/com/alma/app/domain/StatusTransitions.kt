package com.alma.app.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Aplica un cambio de estado y ajusta las fechas de lectura. La puntuación y el comentario se
 * conservan siempre, para que reaparezcan si el libro vuelve a Leído.
 */
fun Book.withStatus(newStatus: ReadingStatus, today: LocalDate, now: Instant): Book {
    if (newStatus == status) return this
    return when (newStatus) {
        ReadingStatus.PENDING -> copy(startedOn = null, finishedOn = null)
        ReadingStatus.READING -> copy(startedOn = startedOn ?: today, finishedOn = null)
        ReadingStatus.READ -> copy(finishedOn = finishedOn ?: today)
    }.copy(status = newStatus, updatedAt = now)
}

fun areValidDates(startedOn: LocalDate?, finishedOn: LocalDate?): Boolean =
    startedOn == null || finishedOn == null || !finishedOn.isBefore(startedOn)
