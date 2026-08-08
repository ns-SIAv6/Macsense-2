package com.macsense.ai.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AriModelRouterTest {

    @Test
    fun `fast command types route to FAST model tier`() {
        val fastTypes = listOf("update_bpm", "update_effects", "apply_preset")
        for (type in fastTypes) {
            val tier = AriModelRouter.routeTier(type)
            assertEquals("Expected $type to route to FAST tier", ModelTier.FAST, tier)
            val url = AriModelRouter.getEndpointUrl(type)
            assertTrue("Expected URL for $type to contain flash", url.contains("flash"))
        }
    }

    @Test
    fun `creative command types route to CREATIVE model tier`() {
        val creativeTypes = listOf("update_lyrics", "reorder_sections", "breed_sounds", "resurrect_sound")
        for (type in creativeTypes) {
            val tier = AriModelRouter.routeTier(type)
            assertEquals("Expected $type to route to CREATIVE tier", ModelTier.CREATIVE, tier)
            val url = AriModelRouter.getEndpointUrl(type)
            assertTrue("Expected URL for $type to contain pro", url.contains("pro"))
        }
    }

    @Test
    fun `routes AriCommand objects correctly`() {
        val fastCmd = AriCommand(type = "update_bpm", bpm_value = 120.0, explanation = "tempo")
        val creativeCmd = AriCommand(type = "update_lyrics", value = "new verse", explanation = "lyrics")

        assertEquals(ModelTier.FAST, AriModelRouter.routeTier(fastCmd))
        assertEquals(ModelTier.CREATIVE, AriModelRouter.routeTier(creativeCmd))

        assertTrue(AriModelRouter.getEndpointUrl(fastCmd).contains("flash"))
        assertTrue(AriModelRouter.getEndpointUrl(creativeCmd).contains("pro"))
    }

    @Test
    fun `handles null and unknown command types gracefully`() {
        assertEquals(ModelTier.FAST, AriModelRouter.routeTier(null as String?))
        assertEquals(ModelTier.FAST, AriModelRouter.routeTier("unknown_custom_action"))

        val nullCmd: AriCommand? = null
        assertEquals(ModelTier.FAST, AriModelRouter.routeTier(nullCmd))
    }

    @Test
    fun `handles uppercase or whitespace command types`() {
        assertEquals(ModelTier.FAST, AriModelRouter.routeTier(" UPDATE_BPM "))
        assertEquals(ModelTier.CREATIVE, AriModelRouter.routeTier("UPDATE_LYRICS"))
    }

    @Test
    fun `routes creative prompts before a response command exists`() {
        assertEquals(ModelTier.CREATIVE, AriModelRouter.routePrompt("rewrite my chorus with more tension"))
        assertEquals(ModelTier.CREATIVE, AriModelRouter.routePrompt("breed these two sound genomes"))
        assertEquals(ModelTier.CREATIVE, AriModelRouter.routePrompt("reorder the song structure"))
        assertEquals(ModelTier.FAST, AriModelRouter.routePrompt("raise the BPM to 132"))
        assertEquals(ModelTier.FAST, AriModelRouter.routePrompt(null))
    }

    @Test
    fun `model tier enum provides correct endpoint url`() {
        assertTrue(ModelTier.FAST.endpointUrl.contains("flash"))
        assertTrue(ModelTier.CREATIVE.endpointUrl.contains("pro"))
        assertEquals(ModelTier.FAST.endpointUrl, AriModelRouter.getEndpointUrl(ModelTier.FAST))
        assertEquals(ModelTier.CREATIVE.endpointUrl, AriModelRouter.getEndpointUrl(ModelTier.CREATIVE))
    }
}
