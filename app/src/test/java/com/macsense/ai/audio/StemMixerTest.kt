package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StemMixerTest {

    private fun tracks() = StemType.values().map { StemTrack(id = it.name.lowercase(), type = it) }

    @Test
    fun `unity gain by default and all stems audible`() {
        val all = tracks()
        val gains = StemMixer.effectiveGains(all)
        assertEquals(all.size, gains.size)
        gains.values.forEach { assertEquals(1f, it, 1e-4f) }
    }

    @Test
    fun `muted stem is silent`() {
        val all = tracks().map { if (it.type == StemType.BASS) it.copy(muted = true) else it }
        val gains = StemMixer.effectiveGains(all)
        assertEquals(0f, gains["bass"]!!, 0f)
        assertEquals(1f, gains["drums"]!!, 1e-4f)
    }

    @Test
    fun `solo silences everything else`() {
        val all = tracks().map { if (it.type == StemType.VOCALS) it.copy(soloed = true) else it }
        val gains = StemMixer.effectiveGains(all)
        assertEquals(1f, gains["vocals"]!!, 1e-4f)
        all.filter { it.type != StemType.VOCALS }.forEach { assertEquals(0f, gains[it.id]!!, 0f) }
    }

    @Test
    fun `mute wins over solo on the same stem`() {
        val all = tracks().map {
            if (it.type == StemType.FX) it.copy(soloed = true, muted = true) else it
        }
        val gains = StemMixer.effectiveGains(all)
        assertEquals(0f, gains["fx"]!!, 0f)
        // FX is still "a soloed stem", so others stay silent too.
        assertEquals(0f, gains["drums"]!!, 0f)
    }

    @Test
    fun `two soloed stems are both audible`() {
        val all = tracks().map {
            if (it.type == StemType.DRUMS || it.type == StemType.BASS) it.copy(soloed = true) else it
        }
        val gains = StemMixer.effectiveGains(all)
        assertTrue(gains["drums"]!! > 0f)
        assertTrue(gains["bass"]!! > 0f)
        assertEquals(0f, gains["vocals"]!!, 0f)
    }

    @Test
    fun `gain dB converts to linear and clamps`() {
        assertEquals(1f, StemMixer.linearGain(0f), 1e-4f)
        assertEquals(2f, StemMixer.linearGain(6.0206f), 1e-3f)
        assertEquals(0.5f, StemMixer.linearGain(-6.0206f), 1e-3f)
        // Beyond the fader floor => hard zero.
        assertEquals(0f, StemMixer.linearGain(-120f), 0f)
        // Clamped at +12 dB.
        assertEquals(StemMixer.linearGain(12f), StemMixer.linearGain(40f), 1e-5f)
        assertEquals(12f, StemMixer.clampGainDb(99f), 0f)
        assertEquals(-60f, StemMixer.clampGainDb(-99f), 0f)
    }

    @Test
    fun `every instrument grid lane maps to a stem type`() {
        val lanes = listOf(
            "808/Bass", "Kick", "Snare", "Hi-Hat", "Clap", "Percussion",
            "Riser", "Crash", "Bass Synth", "Lead", "Pads", "Vocal/Adlib"
        )
        val mapped = lanes.map { StemType.fromLane(it) }.toSet()
        assertTrue(mapped.contains(StemType.DRUMS))
        assertTrue(mapped.contains(StemType.BASS))
        assertTrue(mapped.contains(StemType.VOCALS))
        assertTrue(mapped.contains(StemType.CHORDS))
        assertTrue(mapped.contains(StemType.FX))
        // Unknown lanes fall back to atmosphere, never crash.
        assertEquals(StemType.ATMOSPHERE, StemType.fromLane("Theremin"))
        assertFalse(mapped.contains(StemType.ATMOSPHERE))
    }
}
