package com.macsense.ai.export

import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenomeArtifactCodecTest {

    private fun entry(takeId: String, genome: SoundGenome?) = SoundArchive.Entry(
        takeId = takeId,
        state = SoundArchive.State.LIVING,
        tags = setOf("dark", "808"),
        genome = genome,
        originTakeId = null,
    )

    private fun genome(sourceId: String, parents: List<String> = emptyList()) = SoundGenome(
        sourceId = sourceId,
        transient = 0.7,
        harmonicity = 0.4,
        brightness = 0.9,
        dynamics = 0.55,
        stereoWidth = 0.3,
        confidence = 0.87,
        parents = parents,
    )

    @Test
    fun `export-import round trip preserves genome and lineage across the project boundary`() {
        val g = genome("take-orig", parents = listOf("mom", "dad"))
        val raw = GenomeArtifactCodec.export(entry("take-orig", g), "Night Drive", "sia", exportedAt = 42L)

        assertTrue(raw.contains(GenomeShareableTrack.MAGIC))

        val imported = GenomeArtifactCodec.import(raw, newTakeId = "local-import-1")
        assertEquals("local-import-1", imported.takeId)
        assertEquals(SoundArchive.State.LIVING, imported.state)
        assertTrue(GenomeArtifactCodec.IMPORTED_TAG in imported.tags)
        assertTrue("dark" in imported.tags)
        // #61 lineage integrity: ancestry references survive export/import untouched.
        assertEquals(g, imported.genome)
        assertEquals(listOf("mom", "dad"), imported.genome!!.parents)
        assertEquals("take-orig", imported.genome!!.sourceId)
        assertEquals(0.87, imported.genome!!.confidence, 1e-9)
    }

    @Test
    fun `imported genome breeds against a local genome with cross-project ancestry recorded`() {
        val raw = GenomeArtifactCodec.export(
            entry("remote-take", genome("remote-take")), "Remote", "friend", exportedAt = 1L
        )
        val imported = GenomeArtifactCodec.import(raw, "local-1")
        val local = genome("local-take")

        val child = local.breed(imported.genome!!, setOf(SoundGenome.Trait.BRIGHTNESS))
        assertTrue("local-take" in child.parents)
        assertTrue("remote-take" in child.parents)
        assertEquals(imported.genome!!.brightness, child.brightness, 1e-9)
        assertEquals(local.transient, child.transient, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `exporting a take without a genome fails loudly`() {
        GenomeArtifactCodec.export(entry("no-genome", null), "X", "Y", exportedAt = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importing garbage fails loudly`() {
        GenomeArtifactCodec.import("this is not sound dna", "x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importing an empty artifact fails loudly`() {
        GenomeArtifactCodec.import("   ", "x")
    }
}
