package com.alma.app

import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import java.time.Instant

val T0: Instant = Instant.parse("2026-01-01T10:00:00.000Z")

fun book(
    title: String = "Rayuela",
    authors: String = "Julio Cortázar",
    status: ReadingStatus = ReadingStatus.PENDING,
    updatedAt: Instant = T0,
) = Book(title = title, authors = authors, status = status, addedAt = T0, updatedAt = updatedAt)
