package com.macsense.ai.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiContractTest {
    @Test
    fun `each model tier uses a documented dynamic model path`() {
        ModelTier.entries.forEach { tier ->
            assertTrue(tier.modelName.startsWith("gemini-"))
            assertEquals(
                "v1beta/models/${tier.modelName}:generateContent",
                tier.endpointUrl,
            )
            assertFalseLikeLegacyModel(tier.modelName)
        }
    }

    private fun assertFalseLikeLegacyModel(modelName: String) {
        assertTrue("legacy, non-existent 3.5 model cannot be used", !modelName.startsWith("gemini-3.5"))
    }
}