package com.macsense.ai.export

import com.macsense.ai.audio.SoundGenome
import org.junit.Assert.*
import org.junit.Test

class GenomeShareableTrackTest {

    private fun makeTestGenome() = SoundGenome(
        sourceId = "dna-test-001", transient = 0.75, harmonicity = 0.45,
        brightness = 0.65, dynamics = 0.55, stereoWidth = 0.3,
        confidence = 0.85, parents = listOf("ancestor-1", "ancestor-2")
    )

    @Test
    fun `Sound DNA round-trips through JSON serialization`() {
        val track = GenomeShareableTrack(
            genome = makeTestGenome(), trackName = "Midnight Bass Hook",
            creatorName = "TestArtist", exportedAt = 1723000000000L,
            tags = listOf("dark", "808", "trap"), lineageSummary = "2 ancestors, 3 generations"
        )
        val json = GenomeShareableTrack.toShareableJson(track)
        val decoded = GenomeShareableTrack.fromShareableJson(json)
        assertEquals(track.trackName, decoded.trackName)
        assertEquals(track.creatorName, decoded.creatorName)
        assertEquals(track.exportedAt, decoded.exportedAt)
        assertEquals(track.tags, decoded.tags)
        assertEquals(track.lineageSummary, decoded.lineageSummary)
        assertEquals(track.genome.sourceId, decoded.genome.sourceId)
        assertEquals(track.genome.transient, decoded.genome.transient, 0.0001)
        assertEquals(track.genome.parents, decoded.genome.parents)
    }

    @Test
    fun `Shareable JSON includes magic header and metadata comments`() {
        val track = GenomeShareableTrack(makeTestGenome(), "Test Track", "TestArtist", exportedAt = 1723000000000L)
        val json = GenomeShareableTrack.toShareableJson(track)
        assertTrue(json.startsWith("# MACSENSE_DNA_V1"))
        assertTrue(json.contains("# Track: Test Track"))
        assertTrue(json.contains("# Creator: TestArtist"))
        assertTrue(json.contains("# Breed this sound"))
    }

    @Test
    fun `fromShareableJson strips comment lines before parsing`() {
        val track = GenomeShareableTrack(makeTestGenome(), "Commented Track", "Artist", exportedAt = 1723000000000L)
        val json = "# Extra comment 1\n# Extra comment 2\n" + GenomeShareableTrack.toShareableJson(track)
        val decoded = GenomeShareableTrack.fromShareableJson(json)
        assertEquals(track.trackName, decoded.trackName)
        assertEquals(track.genome.sourceId, decoded.genome.sourceId)
    }

    @Test
    fun `Sound DNA genome can be bred after round-trip`() {
        val track = GenomeShareableTrack(makeTestGenome(), "Breedable DNA", "TestArtist", exportedAt = 1723000000000L)
        val decoded = GenomeShareableTrack.fromShareableJson(GenomeShareableTrack.toShareableJson(track))
        val partner = SoundGenome("partner-001", 0.3, 0.8, 0.2, 0.7, 0.5, 0.9)
        val offspring = decoded.genome.breed(partner, setOf(SoundGenome.Trait.TRANSIENT, SoundGenome.Trait.BRIGHTNESS))
        assertEquals(partner.transient, offspring.transient, 0.0001)
        assertEquals(partner.brightness, offspring.brightness, 0.0001)
        assertEquals(track.genome.harmonicity, offspring.harmonicity, 0.0001)
        assertEquals(track.genome.dynamics, offspring.dynamics, 0.0001)
    }

    @Test
    fun `Sound DNA preserves parents for lineage tracking`() {
        val track = GenomeShareableTrack(makeTestGenome(), "Lineage DNA", "TestArtist", exportedAt = 1723000000000L, lineageSummary = "2 parents")
        val decoded = GenomeShareableTrack.fromShareableJson(GenomeShareableTrack.toShareableJson(track))
        assertEquals(2, decoded.genome.parents.size)
        assertEquals("ancestor-1", decoded.genome.parents[0])
        assertEquals("ancestor-2", decoded.genome.parents[1])
    }
}
