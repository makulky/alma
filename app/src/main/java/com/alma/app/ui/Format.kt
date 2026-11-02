package com.alma.app.ui

import androidx.annotation.StringRes
import com.alma.app.R
import com.alma.app.domain.ReadingStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", spanish)
private val monthNameFormatter = DateTimeFormatter.ofPattern("LLLL", spanish)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun formatMonth(month: YearMonth): String =
    month.format(monthFormatter).replaceFirstChar { it.titlecase(spanish) }

fun formatMonthName(month: YearMonth): String =
    month.format(monthNameFormatter).replaceFirstChar { it.titlecase(spanish) }

@StringRes
fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.PENDING -> R.string.status_pending
    ReadingStatus.READING -> R.string.status_reading
    ReadingStatus.READ -> R.string.status_read
}
