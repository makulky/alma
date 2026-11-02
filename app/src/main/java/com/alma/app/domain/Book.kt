package com.alma.app.domain

import java.time.Instant
import java.time.LocalDate

enum class ReadingStatus { PENDING, READING, READ }

data class Book(
    val id: Long = 0,
    val sourceId: String? = null,
    val title: String,
    val authors: String,
    val coverUri: String? = null,
    val description: String? = null,
    val isbn13: String? = null,
    val publisher: String? = null,
    val pageCount: Int? = null,
    val publishedYear: Int? = null,
    val status: ReadingStatus,
    /** Puntuación en medias estrellas: de 2 (1 ★) a 10 (5 ★). */
    val ratingHalves: Int? = null,
    val comment: String? = null,
    val startedOn: LocalDate? = null,
    val finishedOn: LocalDate? = null,
    val addedAt: Instant,
    val updatedAt: Instant,
)
