package com.alma.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "books",
    indices = [Index(value = ["sourceId"], unique = true), Index("isbn13")],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String?,
    val title: String,
    val authors: String,
    val coverUri: String?,
    val description: String?,
    val isbn13: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedYear: Int?,
    val status: ReadingStatus,
    val ratingHalves: Int?,
    val comment: String?,
    val startedOn: LocalDate?,
    val finishedOn: LocalDate?,
    val addedAt: Instant,
    val updatedAt: Instant,
)

fun BookEntity.toDomain() = Book(
    id = id,
    sourceId = sourceId,
    title = title,
    authors = authors,
    coverUri = coverUri,
    description = description,
    isbn13 = isbn13,
    publisher = publisher,
    pageCount = pageCount,
    publishedYear = publishedYear,
    status = status,
    ratingHalves = ratingHalves,
    comment = comment,
    startedOn = startedOn,
    finishedOn = finishedOn,
    addedAt = addedAt,
    updatedAt = updatedAt,
)

fun Book.toEntity() = BookEntity(
    id = id,
    sourceId = sourceId,
    title = title,
    authors = authors,
    coverUri = coverUri,
    description = description,
    isbn13 = isbn13,
    publisher = publisher,
    pageCount = pageCount,
    publishedYear = publishedYear,
    status = status,
    ratingHalves = ratingHalves,
    comment = comment,
    startedOn = startedOn,
    finishedOn = finishedOn,
    addedAt = addedAt,
    updatedAt = updatedAt,
)
