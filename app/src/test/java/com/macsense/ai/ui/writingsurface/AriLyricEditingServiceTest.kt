package com.macsense.ai.ui.writingsurface

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AriLyricEditingServiceTest {

    private val service = AriLyricEditingService()

    @Test
    fun testRequestLyricEdit_offlineFallbackRewrite() = runTest {
        val result = service.requestLyricEdit(
            selectedText = "beat so hard",
            action = "Rewrite",
            artistIdentity = "Aggressive Trap"
        )
        assertEquals("drippin gold, MACSENSE roll, we never fold", result)
    }

    @Test
    fun testRequestLyricEdit_offlineFallbackMakeMoreAggressive() = runTest {
        val result = service.requestLyricEdit(
            selectedText = "soft rain falling",
            action = "Make more aggressive",
            artistIdentity = "Melodic R&B"
        )
        assertEquals("heavy bass beating hard inside my bruised chest", result)
    }

    @Test
    fun testRequestLyricEdit_offlineFallbackImproveRhyme() = runTest {
        val result = service.requestLyricEdit(
            selectedText = "faded dreams",
            action = "Improve rhyme",
            artistIdentity = "Poetic Folk"
        )
        assertEquals("carving out a path of stone, in the bone, far from home", result)
    }

    @Test
    fun testRequestLyricEdit_emptyTextReturnsError() = runTest {
        val result = service.requestLyricEdit(
            selectedText = "  ",
            action = "Rewrite",
            artistIdentity = "Aggressive Trap"
        )
        assertTrue(result.contains("empty bars"))
    }
}
