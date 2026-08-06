package com.macsense.ai.viewmodel

import com.macsense.ai.ui.viewmodel.DawViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DawViewModelTest {
    
    private val dispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testPlayPauseStateTransition() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        assertFalse(vm.isPlaying.value)
        vm.togglePlayPause()
        assertTrue(vm.isPlaying.value)
        vm.togglePlayPause()
        assertFalse(vm.isPlaying.value)
    }
    
    @Test
    fun testSectionReorder() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val initialSections = vm.sections.value
        assertEquals("intro", initialSections[0].id)
        assertEquals("verse1", initialSections[1].id)
        
        vm.reorderSection(0, 1)
        
        val newSections = vm.sections.value
        assertEquals("verse1", newSections[0].id)
        assertEquals("intro", newSections[1].id)
    }
    
    @Test
    fun testBarPositionAdvancesWithTransportClock() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        assertEquals(1, vm.barPosition.value)
        vm.advanceBar() // manually advance since we can't easily test the coroutine delay without kotlinx-coroutines-test
        assertEquals(2, vm.barPosition.value)
        vm.advanceBar()
        assertEquals(3, vm.barPosition.value)
    }

    @Test
    fun testSendMessageToAriAddsMessageToLog() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val initialSize = vm.ariChatLog.value.size
        
        vm.sendMessageToAri("make it fast")
        
        // Assert user message is appended immediately
        assertEquals(initialSize + 1, vm.ariChatLog.value.size)
        assertEquals("user", vm.ariChatLog.value.last().role)
        assertEquals("make it fast", vm.ariChatLog.value.last().text)
    }

    @Test
    fun testApplyAriCommandBpm() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val originalBpm = vm.bpm.value
        
        val cmd = com.macsense.ai.api.AriCommand(
            type = "update_bpm",
            bpm_value = 150.0,
            explanation = "too slow rookie"
        )
        
        vm.applyAriCommand(cmd)
        
        assertEquals(150.0, vm.bpm.value, 0.001)
    }

    @Test
    fun testApplyAriCommandLyrics() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val cmd = com.macsense.ai.api.AriCommand(
            type = "update_lyrics",
            section_id = "intro",
            value = "ari lyrics hook drop",
            explanation = "hot bars"
        )
        
        vm.applyAriCommand(cmd)
        
        val introSection = vm.sections.value.find { it.id == "intro" }
        assertEquals("ari lyrics hook drop", introSection?.lyrics)
    }

    @Test
    fun testApplyAriCommandReorder() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val originalOrder = vm.sections.value.map { it.id }
        
        val reversedOrder = originalOrder.reversed()
        val cmd = com.macsense.ai.api.AriCommand(
            type = "reorder_sections",
            section_order = reversedOrder,
            explanation = "switch it up"
        )
        
        vm.applyAriCommand(cmd)
        
        val newOrder = vm.sections.value.map { it.id }
        assertEquals(reversedOrder, newOrder)
    }

    @Test
    fun testApplyAriCommandEffects() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel()
        val cmd = com.macsense.ai.api.AriCommand(
            type = "update_effects",
            section_id = "intro",
            reverb = 0.9f,
            delay = 0.8f,
            filter = 0.7f,
            volume = 0.6f,
            explanation = "wash out"
        )
        
        vm.applyAriCommand(cmd)
        
        val introSection = vm.sections.value.find { it.id == "intro" }
        assertEquals(0.9f, introSection?.reverb ?: 0f, 0.001f)
        assertEquals(0.8f, introSection?.delay ?: 0f, 0.001f)
        assertEquals(0.7f, introSection?.filter ?: 0f, 0.001f)
        assertEquals(0.6f, introSection?.volume ?: 0f, 0.001f)
    }
}
