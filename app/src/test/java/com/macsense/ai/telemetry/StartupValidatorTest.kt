package com.macsense.ai.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupValidatorTest {

    @Test
    fun `blank key is treated as not configured`() {
        val result = StartupValidator.validateGeminiKey("")
        assertFalse(result.isGeminiKeyConfigured)
    }

    @Test
    fun `known placeholder key is treated as not configured`() {
        val result = StartupValidator.validateGeminiKey("MY_GEMINI_API_KEY")
        assertFalse(result.isGeminiKeyConfigured)
    }

    @Test
    fun `unspecified placeholder key is treated as not configured`() {
        val result = StartupValidator.validateGeminiKey("unspecified")
        assertFalse(result.isGeminiKeyConfigured)
    }

    @Test
    fun `real looking key is treated as configured`() {
        val result = StartupValidator.validateGeminiKey("AIzaSyRealLookingKeyValue1234567890")
        assertTrue(result.isGeminiKeyConfigured)
    }

    @Test
    fun `configured result message does not contain the raw key value`() {
        val secret = "AIzaSyRealLookingKeyValue1234567890"
        val result = StartupValidator.validateGeminiKey(secret)
        assertFalse(result.message.contains(secret))
    }
}
