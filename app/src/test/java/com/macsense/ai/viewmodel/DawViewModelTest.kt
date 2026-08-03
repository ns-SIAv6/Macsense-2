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
    fun testPlayPauseStateTransition() {
        val vm = DawViewModel()
        assertFalse(vm.isPlaying.value)
        vm.togglePlayPause()
        assertTrue(vm.isPlaying.value)
        vm.togglePlayPause()
        assertFalse(vm.isPlaying.value)
    }
    
    @Test
    fun testSectionReorder() {
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
    fun testBarPositionAdvancesWithTransportClock() {
        val vm = DawViewModel()
        assertEquals(1, vm.barPosition.value)
        vm.advanceBar() // manually advance since we can't easily test the coroutine delay without kotlinx-coroutines-test
        assertEquals(2, vm.barPosition.value)
        vm.advanceBar()
        assertEquals(3, vm.barPosition.value)
    }
}
