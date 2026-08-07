package com.macsense.ai.telemetry

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingTest {

    private class FakeReporter : CrashReporter {
        val exceptions = mutableListOf<Triple<Throwable, String, String>>()
        val logs = mutableListOf<String>()

        override fun recordException(throwable: Throwable, tag: String, message: String) {
            exceptions += Triple(throwable, tag, message)
        }

        override fun log(message: String) {
            logs += message
        }
    }

    @After
    fun tearDown() {
        // AppLogger holds a static sink; reset it so tests don't leak state into each other.
        AppLogger.install(object : AppLogger.Sink {
            override fun log(level: AppLogger.Level, tag: String, message: String, throwable: Throwable?) {}
        })
    }

    @Test
    fun `sink forwards error with throwable to reporter as exception`() {
        val reporter = FakeReporter()
        val sink = CrashReportingLogSink(reporter, enabled = true)
        val error = IllegalStateException("boom")

        sink.log(AppLogger.Level.ERROR, "Ari", "Gemini call failed", error)

        assertEquals(1, reporter.exceptions.size)
        assertEquals(error, reporter.exceptions[0].first)
        assertEquals("Ari", reporter.exceptions[0].second)
        assertTrue(reporter.logs.isEmpty())
    }

    @Test
    fun `sink forwards error without throwable as log`() {
        val reporter = FakeReporter()
        val sink = CrashReportingLogSink(reporter, enabled = true)

        sink.log(AppLogger.Level.ERROR, "Ari", "Unknown failure", null)

        assertEquals(1, reporter.logs.size)
        assertTrue(reporter.logs[0].contains("Unknown failure"))
        assertTrue(reporter.exceptions.isEmpty())
    }

    @Test
    fun `sink ignores debug and info levels`() {
        val reporter = FakeReporter()
        val sink = CrashReportingLogSink(reporter, enabled = true)

        sink.log(AppLogger.Level.DEBUG, "Ari", "debug msg", null)
        sink.log(AppLogger.Level.INFO, "Ari", "info msg", null)

        assertTrue(reporter.logs.isEmpty())
        assertTrue(reporter.exceptions.isEmpty())
    }

    @Test
    fun `no-op reporter never throws and records nothing observable`() {
        val reporter = NoOpCrashReporter()
        // Should not throw for any input.
        reporter.recordException(RuntimeException("x"), "tag", "msg")
        reporter.log("msg")
    }
}
