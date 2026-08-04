package com.macsense.ai.ui.viewmodel

import com.macsense.ai.audio.LiveMeterEngine
import com.macsense.ai.audio.NativePlaybackEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Native playback is unavailable in the JVM unit test environment (no Android runtime
 * for the native lib), so these tests focus on verifying DawViewModel degrades safely --
 * every native-playback-adjacent call must be a harmless no-op rather than throwing, and
 * state flags must accurately reflect that no take is actually loaded.
 */
class DawViewModelPlaybackTest {

    private fun newViewModel() = DawViewModel(
        meterEngine = LiveMeterEngine(),
        nativePlayback = NativePlaybackEngine()
    )

    @Test
    fun `loadTake reports unavailable when native lib is not loaded`() {
        val viewModel = newViewModel()
        viewModel.loadTake(DoubleArray(1000) { 0.2 }, sampleRate = 44100)

        assertFalse(viewModel.hasLoadedTake.value)
        assertFalse(viewModel.isNativePlaybackAvailable)
    }

    @Test
    fun `play pause stop and seek do not throw without a loaded take`() {
        val viewModel = newViewModel()

        viewModel.play()
        viewModel.seekTakeTo(1.5)
        viewModel.pause()
        viewModel.stopTakePlayback()

        assertEquals(0.0, viewModel.nativePlaybackPositionSeconds, 0.0001)
    }

    @Test
    fun `play pause stop and seek do not throw after a failed loadTake`() {
        val viewModel = newViewModel()
        viewModel.loadTake(DoubleArray(500) { 0.0 })

        viewModel.play()
        viewModel.seekTakeTo(0.5)
        viewModel.pause()
        viewModel.stopTakePlayback()

        assertFalse(viewModel.isNativePlaybackAvailable)
    }
}
