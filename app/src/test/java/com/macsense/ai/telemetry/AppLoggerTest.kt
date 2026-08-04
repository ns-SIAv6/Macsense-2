package com.macsense.ai.telemetry

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLoggerTest {

    private class RecordingSink : AppLogger.Sink {
        val records = mutableListOf<Triple<AppLogger.Level, String, String>>()
        override fun log(level: AppLogger.Level, tag: String, message: String, throwable: Throwable?) {
            records.add(Triple(level, tag, message))
        }
    }

    @After
    fun tearDown() {
        // Reset to no-op sink installed so other tests are not affected by ordering.
        AppLogger.install(object : AppLogger.Sink {
            override fun log(level: AppLogger.Level, tag: String, message: String, throwable: Throwable?) {}
        })
    }

    @Test
    fun `installed sink receives info logs`() {
        val sink = RecordingSink()
        AppLogger.install(sink)
        AppLogger.i("Tag", "hello")
        assertEquals(1, sink.records.size)
        assertEquals(AppLogger.Level.INFO, sink.records[0].first)
        assertEquals("hello", sink.records[0].third)
    }

    @Test
    fun `installed sink receives warn and error logs distinctly`() {
        val sink = RecordingSink()
        AppLogger.install(sink)
        AppLogger.w("Tag", "warn msg")
        AppLogger.e("Tag", "err msg")
        assertEquals(2, sink.records.size)
        assertEquals(AppLogger.Level.WARN, sink.records[0].first)
        assertEquals(AppLogger.Level.ERROR, sink.records[1].first)
    }

    @Test
    fun `debug logs are recorded when sink installed`() {
        val sink = RecordingSink()
        AppLogger.install(sink)
        AppLogger.d("Tag", "debug msg")
        assertTrue(sink.records.any { it.first == AppLogger.Level.DEBUG })
    }
}
