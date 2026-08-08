package com.macsense.ai.export

import org.junit.Assert.*
import org.junit.Test

class DistributionModelsTest {

    @Test
    fun `DistributionMetadata generates valid ISRC code`() {
        val metadata = DistributionMetadata(
            trackTitle = "Test Track",
            artistName = "Test Artist",
            audioFileUri = "file:///test.wav"
        )
        val isrc = metadata.generateIsrc()
        assertTrue("ISRC should start with country code", isrc.startsWith("US"))
        assertTrue("ISRC should be 12 characters", isrc.length == 12)
    }

    @Test
    fun `DistributionStatus has all states`() {
        val states = DistributionStatus.DistributionState.values()
        assertTrue(states.contains(DistributionStatus.DistributionState.PREPARING))
        assertTrue(states.contains(DistributionStatus.DistributionState.UPLOADING))
        assertTrue(states.contains(DistributionStatus.DistributionState.LIVE))
        assertTrue(states.contains(DistributionStatus.DistributionState.REJECTED))
    }

    @Test
    fun `DistributionProvider has known providers`() {
        assertTrue(DistributionProvider.values().any { it.displayName == "DistroKit" })
        assertTrue(DistributionProvider.values().any { it.displayName == "TuneCore" })
    }

    @Test
    fun `DistributionMetadata has required fields`() {
        val metadata = DistributionMetadata(
            trackTitle = "Midnight",
            artistName = "MacSense",
            audioFileUri = "file:///midnight.wav",
            isrc = "USMAC2400001"
        )
        assertEquals("Midnight", metadata.trackTitle)
        assertEquals("MacSense", metadata.artistName)
        assertEquals("USMAC2400001", metadata.isrc)
        assertFalse(metadata.explicit)
    }
}
