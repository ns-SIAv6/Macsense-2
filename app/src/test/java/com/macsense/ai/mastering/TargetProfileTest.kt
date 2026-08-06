package com.macsense.ai.mastering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class TargetProfileTest {

    @Test
    fun testDefaultProfiles() {
        val pop = TargetProfile.POP
        assertEquals("pop", pop.id)
        assertEquals(-10.0, pop.targetLufs, 0.001)
        assertEquals(-1.0, pop.ceilingDbtp, 0.001)
        assertEquals(0.15, pop.targetLowMidRatio, 0.001)
        assertEquals(0.08, pop.targetHighRatio, 0.001)
        assertEquals(9.0, pop.dynamicRange, 0.001)
    }

    @Test
    fun testImportFromReferenceTrack() {
        val sampleRate = 44100
        val size = sampleRate // 1 second
        val left = DoubleArray(size)
        val right = DoubleArray(size)

        var phase = 0.0
        for (i in 0 until size) {
            left[i] = sin(phase) * 0.5
            right[i] = sin(phase) * 0.45
            phase += 2.0 * Math.PI * 440.0 / sampleRate
        }
        val channels = arrayOf(left, right)

        val profile = TargetProfile.importFromReferenceTrack(channels, sampleRate, "My Reference")

        assertEquals("My Reference", profile.name)
        assertTrue(profile.id.startsWith("imported_"))
        assertTrue(profile.targetLufs in -24.0..-6.0)
        assertTrue(profile.ceilingDbtp in -6.0..0.0)
        assertTrue(profile.targetLowMidRatio >= 0.0)
        assertTrue(profile.targetHighRatio >= 0.0)
        assertTrue(profile.dynamicRange >= 1.0)
    }
}
