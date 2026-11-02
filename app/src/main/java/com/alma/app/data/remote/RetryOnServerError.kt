package com.alma.app.data.remote

import kotlinx.coroutines.delay
import retrofit2.HttpException

private val RETRY_DELAYS_MS = listOf(500L, 1_000L, 2_000L)

/**
 * Repite la llamada si el servidor responde 5xx, porque suelen ser fallos pasajeros. Los errores
 * 4xx y de red se propagan sin repetir.
 */
suspend fun <T> retryOnServerError(
    delaysMs: List<Long> = RETRY_DELAYS_MS,
    call: suspend () -> T,
): T {
    for (delayMs in delaysMs) {
        try {
            return call()
        } catch (e: HttpException) {
            if (e.code() !in 500..599) throw e
            delay(delayMs)
        }
    }
    return call()
}
