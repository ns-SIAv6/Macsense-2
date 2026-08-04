package com.macsense.ai.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AriCommandParserTest {

    @Test
    fun `parses valid update_bpm command and strips tags from text`() {
        val raw = "lets speed this up.\n<ari_command>{\"type\":\"update_bpm\",\"bpm_value\":140.0,\"explanation\":\"faster\"}</ari_command>"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals("lets speed this up.", cleanText)
        assertNotNull(command)
        assertEquals("update_bpm", command!!.type)
        assertEquals(140.0, command.bpm_value)
        assertEquals("faster", command.explanation)
    }

    @Test
    fun `parses update_lyrics command with section_id and value`() {
        val raw = "<ari_command>{\"type\":\"update_lyrics\",\"section_id\":\"verse1\",\"value\":\"new bars\",\"explanation\":\"sharper\"}</ari_command>"
        val (_, command) = AriCommandParser.parse(raw)
        assertNotNull(command)
        assertEquals("verse1", command!!.section_id)
        assertEquals("new bars", command.value)
    }

    @Test
    fun `parses reorder_sections command with section_order list`() {
        val raw = "<ari_command>{\"type\":\"reorder_sections\",\"section_order\":[\"hook\",\"intro\"],\"explanation\":\"flip it\"}</ari_command>"
        val (_, command) = AriCommandParser.parse(raw)
        assertNotNull(command)
        assertEquals(listOf("hook", "intro"), command!!.section_order)
    }

    @Test
    fun `parses update_effects command with float fields`() {
        val raw = "<ari_command>{\"type\":\"update_effects\",\"section_id\":\"intro\",\"reverb\":0.5,\"delay\":0.3,\"filter\":0.6,\"volume\":0.7,\"explanation\":\"space it out\"}</ari_command>"
        val (_, command) = AriCommandParser.parse(raw)
        assertNotNull(command)
        assertEquals(0.5f, command!!.reverb)
        assertEquals(0.3f, command.delay)
        assertEquals(0.6f, command.filter)
        assertEquals(0.7f, command.volume)
    }

    @Test
    fun `returns original text and null command when no tags present`() {
        val raw = "just a plain chat reply with no command block."
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals(raw, cleanText)
        assertNull(command)
    }

    @Test
    fun `returns original text and null command when json is malformed`() {
        val raw = "broken cmd <ari_command>{not valid json at all}</ari_command>"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals(raw, cleanText)
        assertNull(command)
    }

    @Test
    fun `returns original text and null command when end tag missing`() {
        val raw = "<ari_command>{\"type\":\"update_bpm\",\"bpm_value\":140.0,\"explanation\":\"faster\"}"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals(raw, cleanText)
        assertNull(command)
    }

    @Test
    fun `returns original text and null command when start tag missing`() {
        val raw = "{\"type\":\"update_bpm\",\"bpm_value\":140.0,\"explanation\":\"faster\"}</ari_command>"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals(raw, cleanText)
        assertNull(command)
    }

    @Test
    fun `handles empty command block gracefully`() {
        val raw = "empty block <ari_command></ari_command> after"
        val (_, command) = AriCommandParser.parse(raw)
        assertNull(command)
    }

    @Test
    fun `handles missing required explanation field as parse failure`() {
        val raw = "<ari_command>{\"type\":\"update_bpm\",\"bpm_value\":140.0}</ari_command>"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertEquals(raw, cleanText)
        assertNull(command)
    }

    @Test
    fun `ignores unknown extra json keys without failing`() {
        val raw = "<ari_command>{\"type\":\"update_bpm\",\"bpm_value\":140.0,\"explanation\":\"faster\",\"unknown_field\":\"ignored\"}</ari_command>"
        val (_, command) = AriCommandParser.parse(raw)
        assertNotNull(command)
        assertEquals(140.0, command!!.bpm_value)
    }

    @Test
    fun `parses apply_preset command`() {
        val raw = "<ari_command>{\"type\":\"apply_preset\",\"section_id\":\"hook\",\"preset_name\":\"Trap 16ths\",\"explanation\":\"knock\"}</ari_command>"
        val (_, command) = AriCommandParser.parse(raw)
        assertNotNull(command)
        assertEquals("Trap 16ths", command!!.preset_name)
    }

    @Test
    fun `strips command block from middle of longer text correctly`() {
        val raw = "before text <ari_command>{\"type\":\"update_bpm\",\"bpm_value\":100.0,\"explanation\":\"x\"}</ari_command> after text"
        val (cleanText, command) = AriCommandParser.parse(raw)
        assertTrue(cleanText.contains("before text"))
        assertTrue(cleanText.contains("after text"))
        assertTrue(!cleanText.contains("ari_command"))
        assertNotNull(command)
    }
}
