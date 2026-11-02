package com.alma.app.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RetryOnServerErrorTest {
    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    @Test
    fun retriesServerErrorsUntilItWorks() = runTest {
        var calls = 0
        val result = retryOnServerError {
            calls++
            if (calls < 3) throw httpError(503)
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, calls)
    }

    @Test
    fun givesUpAfterTheLastAttempt() = runTest {
        var calls = 0
        val error = runCatching {
            retryOnServerError<Unit>(delaysMs = listOf(1L, 2L, 3L)) {
                calls++
                throw httpError(503)
            }
        }.exceptionOrNull() as HttpException
        assertEquals(503, error.code())
        assertEquals(4, calls)
    }

    @Test
    fun doesNotRetryClientErrors() = runTest {
        var calls = 0
        val error = runCatching {
            retryOnServerError<Unit> {
                calls++
                throw httpError(403)
            }
        }.exceptionOrNull() as HttpException
        assertEquals(403, error.code())
        assertEquals(1, calls)
    }

    @Test
    fun doesNotRetryNetworkErrors() = runTest {
        var calls = 0
        val error = runCatching {
            retryOnServerError<Unit> {
                calls++
                throw IOException("sin red")
            }
        }.exceptionOrNull()
        assertEquals(IOException::class.java, error?.javaClass)
        assertEquals(1, calls)
    }
}
