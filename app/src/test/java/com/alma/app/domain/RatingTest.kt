package com.alma.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingTest {
    private val width = 500f // Cada estrella ocupa 100.

    @Test
    fun leftHalfGivesHalfStarAndRightHalfGivesWholeStar() {
        assertEquals(3, ratingFromPosition(120f, width)) // 1,5 ★
        assertEquals(4, ratingFromPosition(180f, width)) // 2 ★
        assertEquals(7, ratingFromPosition(320f, width)) // 3,5 ★
        assertEquals(10, ratingFromPosition(480f, width)) // 5 ★
    }

    @Test
    fun firstStarAlwaysGivesAtLeastOneStar() {
        assertEquals(2, ratingFromPosition(10f, width))
        assertEquals(2, ratingFromPosition(80f, width))
    }

    @Test
    fun positionsOutsideTheStarsAreClamped() {
        assertEquals(2, ratingFromPosition(-50f, width))
        assertEquals(10, ratingFromPosition(900f, width))
        assertEquals(10, ratingFromPosition(width, width))
    }

    @Test
    fun zeroWidthDoesNotFail() {
        assertEquals(2, ratingFromPosition(10f, 0f))
    }

    @Test
    fun validRatingsGoFromOneToFiveStars() {
        assertFalse(isValidRating(1))
        assertTrue(isValidRating(2))
        assertTrue(isValidRating(10))
        assertFalse(isValidRating(11))
    }

    @Test
    fun formatsWithDecimalComma() {
        assertEquals("1", formatRating(2))
        assertEquals("3,5", formatRating(7))
        assertEquals("5", formatRating(10))
    }
}
