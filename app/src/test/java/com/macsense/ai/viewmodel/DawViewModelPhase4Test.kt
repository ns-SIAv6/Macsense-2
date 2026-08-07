package com.macsense.ai.viewmodel

import com.macsense.ai.audio.StemType
import com.macsense.ai.ui.viewmodel.DawViewModel
import com.macsense.ai.ui.viewmodel.SectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DawViewModelPhase4Test {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stem tracks cover all six stem types with unity defaults`() {
        val vm = DawViewModel()
        val stems = vm.stemTracks.value
        assertEquals(StemType.values().size, stems.size)
        assertEquals(StemType.values().toSet(), stems.map { it.type }.toSet())
        vm.stemEffectiveGains.values.forEach { assertEquals(1f, it, 1e-4f) }
    }

    @Test
    fun `solo and mute drive effective gains`() {
        val vm = DawViewModel()
        vm.toggleStemSolo("vocals")
        assertEquals(0f, vm.stemEffectiveGains["drums"]!!, 0f)
        assertTrue(vm.stemEffectiveGains["vocals"]!! > 0f)
        vm.toggleStemSolo("vocals") // un-solo
        vm.toggleStemMute("drums")
        assertEquals(0f, vm.stemEffectiveGains["drums"]!!, 0f)
        assertTrue(vm.stemEffectiveGains["bass"]!! > 0f)
    }

    @Test
    fun `stem gain is clamped to the fader range`() {
        val vm = DawViewModel()
        vm.setStemGain("bass", 99f)
        assertEquals(12f, vm.stemTracks.value.first { it.id == "bass" }.gainDb, 0f)
    }

    @Test
    fun `sections carry semantic labels Ari can target`() {
        val vm = DawViewModel()
        assertEquals(1, vm.sectionsWithLabel(SectionLabel.HOOK).size)
        vm.updateSectionLabel("bridge", SectionLabel.HOOK)
        assertEquals(2, vm.sectionsWithLabel(SectionLabel.HOOK).size)
    }

    @Test
    fun `section prompt memory is stored on the section`() {
        val vm = DawViewModel()
        vm.updateSectionAriPrompt("hook", "dark 808 hook, halftime")
        assertEquals(
            "dark 808 hook, halftime",
            vm.sections.value.first { it.id == "hook" }.ariPrompt
        )
    }

    @Test
    fun `fork creates a version and switching validates ids`() {
        val vm = DawViewModel()
        val rootId = vm.forkCurrentVersion(now = 1L)
        val childId = vm.forkCurrentVersion(now = 2L)
        assertEquals(childId, vm.currentVersionId.value)
        assertEquals(listOf(rootId, childId), vm.versionTree.value.pathTo(childId).map { it.id })

        vm.switchToVersion(rootId)
        assertEquals(rootId, vm.currentVersionId.value)
        vm.switchToVersion("ghost") // ignored, logged
        assertEquals(rootId, vm.currentVersionId.value)
    }

    @Test
    fun `loop region state validates input`() {
        val vm = DawViewModel()
        vm.setLoopRegion(4, 2) // invalid — rejected
        assertNull(vm.loopRegion.value)
        vm.setLoopRegion(2, 4)
        assertNotNull(vm.loopRegion.value)
        assertEquals(2 to 4, vm.loopRegion.value)
        vm.clearLoopRegion()
        assertNull(vm.loopRegion.value)
    }
}
