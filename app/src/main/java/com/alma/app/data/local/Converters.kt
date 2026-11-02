package com.alma.app.data.local

import androidx.room.TypeConverter
import com.alma.app.domain.ReadingStatus
import java.time.Instant
import java.time.LocalDate

class Converters {
    // Las fechas se guardan en ISO-8601, que se ordena bien como texto y permite consultas por rango.
    @TypeConverter fun localDateToText(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun textToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun instantToMillis(value: Instant): Long = value.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter fun statusToText(value: ReadingStatus): String = value.name
    @TypeConverter fun textToStatus(value: String): ReadingStatus = ReadingStatus.valueOf(value)
}
