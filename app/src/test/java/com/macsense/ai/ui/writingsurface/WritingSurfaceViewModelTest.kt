package com.macsense.ai.ui.writingsurface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WritingSurfaceViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: WritingSurfaceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        viewModel = WritingSurfaceViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitializationStats() {
        val stats = viewModel.stats.value
        assertTrue(stats.wordCount > 0)
        assertTrue(stats.syllableCount > 0)
        assertTrue(stats.rhymeDensityPercent in 0..100)
    }

    @Test
    fun testUpdateLyricsRecalculatesStats() {
        val newLyrics = "faded dreams in neon streams"
        viewModel.updateLyrics(newLyrics)

        assertEquals(newLyrics, viewModel.lyricsText.value)
        assertEquals(5, viewModel.stats.value.wordCount)
    }

    @Test
    fun testSelectTextRangeSetsSpan() {
        val text = viewModel.lyricsText.value // "Yeah, double cup spilling..."
        val part = "double cup"
        val start = text.indexOf(part)
        val end = start + part.length

        viewModel.selectTextRange(start, end)
        val span = viewModel.selectedTextSpan.value

        assertNotNull(span)
        assertEquals(part, span?.text)
        assertEquals(start, span?.start)
        assertEquals(end, span?.end)

        viewModel.clearSelection()
        assertNull(viewModel.selectedTextSpan.value)
    }

    @Test
    fun testSetTabAndIdentity() {
        viewModel.setTab(1)
        assertEquals(1, viewModel.activeTab.value)

        viewModel.setArtistIdentity("Melodic R&B")
        assertEquals("Melodic R&B", viewModel.artistIdentity.value)
    }

    @Test
    fun testTriggerLyricEditActionAndAcceptDiff() = kotlinx.coroutines.runBlocking {
        val text = "Yeah, double cup spilling on the MPC"
        viewModel.updateLyrics(text)
        val target = "double cup"
        val start = text.indexOf(target)
        val end = start + target.length

        viewModel.selectTextRange(start, end)
        viewModel.setArtistIdentity("Aggressive Trap")
        viewModel.triggerLyricEditAction("Rewrite")

        // Wait for coroutine to complete offline edit mock
        var attempts = 0
        while (viewModel.isDiffVisible.value == false && attempts < 20) {
            kotlinx.coroutines.delay(100)
            attempts++
        }

        assertTrue(viewModel.isDiffVisible.value)
        assertEquals("drippin gold, MACSENSE roll, we never fold", viewModel.diffSuggested.value)

        viewModel.acceptDiff()

        assertFalse(viewModel.isDiffVisible.value)
        assertNull(viewModel.selectedTextSpan.value)
        val expectedText = "Yeah, drippin gold, MACSENSE roll, we never fold spilling on the MPC"
        assertEquals(expectedText, viewModel.lyricsText.value)
    }

    @Test
    fun testTriggerLyricEditActionAndRejectDiff() = kotlinx.coroutines.runBlocking {
        val text = "Yeah, double cup spilling"
        viewModel.updateLyrics(text)
        val target = "double cup"
        val start = text.indexOf(target)
        val end = start + target.length

        viewModel.selectTextRange(start, end)
        viewModel.triggerLyricEditAction("Better cadence")

        var attempts = 0
        while (viewModel.isDiffVisible.value == false && attempts < 20) {
            kotlinx.coroutines.delay(100)
            attempts++
        }

        viewModel.rejectDiff()

        assertFalse(viewModel.isDiffVisible.value)
        assertEquals(text, viewModel.lyricsText.value) // original remains unchanged
    }

    @Test
    fun testSendMessageToAriAddsToChatLog() = kotlinx.coroutines.runBlocking {
        viewModel.sendMessageToAri("feedback")

        // Wait for offline response coroutine on Dispatchers.IO to finish and call Dispatchers.Main
        var attempts = 0
        while (viewModel.chatLog.value.size < 3 && attempts < 20) {
            kotlinx.coroutines.delay(100)
            attempts++
        }
        kotlinx.coroutines.delay(100) // Settle time

        val chat = viewModel.chatLog.value
        assertEquals(3, chat.size)
        assertEquals("user", chat[1].role)
        assertEquals("feedback", chat[1].text)
        assertEquals("assistant", chat[2].role)
        assertTrue(chat[2].text.contains("flow") || chat[2].text.contains("cadence"))
    }
}
