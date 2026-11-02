package com.alma.app.domain

const val STAR_COUNT = 5
const val MIN_RATING_HALVES = 2
const val MAX_RATING_HALVES = STAR_COUNT * 2

fun isValidRating(halves: Int): Boolean = halves in MIN_RATING_HALVES..MAX_RATING_HALVES

/**
 * Convierte una posición horizontal sobre las estrellas en medias estrellas: la mitad izquierda de
 * una estrella da media y la derecha la entera. El mínimo es 1 ★, también en la primera estrella.
 */
fun ratingFromPosition(x: Float, totalWidth: Float): Int {
    if (totalWidth <= 0f) return MIN_RATING_HALVES
    val halfWidth = totalWidth / MAX_RATING_HALVES
    val halves = (x / halfWidth).toInt() + 1
    return halves.coerceIn(MIN_RATING_HALVES, MAX_RATING_HALVES)
}

/** Texto de la puntuación con coma decimal: 7 medias estrellas → "3,5". */
fun formatRating(halves: Int): String =
    if (halves % 2 == 0) "${halves / 2}" else "${halves / 2},5"
