package com.macsense.ai.viewmodel

import com.macsense.ai.ui.viewmodel.DawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DawViewModelLyricsTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `section lyrics stay linked to DAW sections`() {
        val vm = DawViewModel()
        vm.updateSectionLyrics("hook", "Feel so free\nCarrying me")
        val lines = vm.sectionLyricsLines()
        assertTrue("Feel so free" in lines)
        assertTrue("Carrying me" in lines)
    }

    @Test
    fun `end rhyme analysis runs over section-linked lyrics`() {
        val vm = DawViewModel()
        vm.sections.value.forEach { vm.updateSectionLyrics(it.id, "") }
        vm.updateSectionLyrics("hook", "Feel so free\nCarrying me")
        val groups = vm.analyzeEndRhymes()
        assertEquals(1, groups.size)
    }

    @Test
    fun `lrc export uses project bpm timing`() {
        val vm = DawViewModel()
        vm.sections.value.forEach { vm.updateSectionLyrics(it.id, "") }
        vm.updateSectionLyrics("hook", "First line\nSecond line")
        val lrc = vm.exportLyricsAsLrc(title = "T")
        assertTrue(lrc.startsWith("[ti:T]"))
        assertTrue(lrc.contains("[00:00.00]First line"))
        // default 120 bpm -> 2s per bar
        assertTrue(lrc.contains("[00:02.00]Second line"))
        val srt = vm.exportLyricsAsSrt()
        assertTrue(srt.contains("00:00:00,000 --> 00:00:02,000"))
    }

    @Test
    fun `syllable alignment flags nothing for sparse lines`() {
        val vm = DawViewModel()
        vm.sections.value.forEach { vm.updateSectionLyrics(it.id, "") }
        vm.updateSectionLyrics("hook", "yeah")
        assertTrue(vm.analyzeSyllableAlignment().none { it.offGrid })
    }
}
