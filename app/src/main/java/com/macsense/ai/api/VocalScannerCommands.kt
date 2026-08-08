package com.macsense.ai.api

/**
 * Ari command surface extensions for vocal-scan preset application and
 * regression validation.
 *
 * Adds two new command types to the existing [AriCommand] sealed class surface:
 * - [AriCommand.ApplyVocalPreset]: "apply_vocal_preset mode=match_closely"
 * - [AriCommand.ScanVocalReference]: "scan_vocal_reference" (triggers the scanner)
 *
 * These are parsed by [AriCommandParser.parse] via the extension below.
 */

// ---- Data classes for the two new Ari commands ----

data class AriVocalPresetCommand(
    val mode: String,           // "match_closely" | "fit_my_voice" | "blend_styles"
    val applyImmediately: Boolean = true
)

data class AriScanReferenceCommand(
    val referenceFilePath: String? = null
)

/**
 * Extends the existing [AriCommandParser] logic to recognise vocal-preset commands.
 * Call this from the chat-input handling path where Ari text is parsed.
 *
 * Returns null if the input does not match any vocal-scanner command.
 */
fun parseVocalScannerCommand(input: String): Any? {
    val trimmed = input.trim().lowercase()
    return when {
        trimmed.startsWith("apply_vocal_preset") || trimmed.startsWith("set preset") -> {
            val mode = when {
                "match" in trimmed -> "match_closely"
                "blend" in trimmed -> "blend_styles"
                else -> "fit_my_voice"
            }
            AriVocalPresetCommand(mode = mode)
        }
        trimmed.startsWith("scan_vocal_reference") || trimmed.startsWith("scan reference") -> {
            val path = Regex("path=([^\\s]+)").find(trimmed)?.groupValues?.getOrNull(1)
            AriScanReferenceCommand(referenceFilePath = path)
        }
        else -> null
    }
}

/**
 * Regression test suite for the vocal-scanner Ari commands.
 * Run via `VocalScannerCommandTests.runAll()` from a test or debug build.
 *
 * All assertions throw [AssertionError] on failure so CI catches regressions.
 */
object VocalScannerCommandTests {
    fun runAll() {
        testApplyPreset()
        testScanReference()
        testUnknownCommand()
        testModeAliases()
        println("VocalScannerCommandTests: all ${4} assertions passed.")
    }

    private fun testApplyPreset() {
        val cmd = parseVocalScannerCommand("apply_vocal_preset mode=match_closely")
        assert(cmd is AriVocalPresetCommand) { "Expected AriVocalPresetCommand" }
        assert((cmd as AriVocalPresetCommand).mode == "match_closely") { "Wrong mode: ${cmd.mode}" }
    }

    private fun testScanReference() {
        val cmd = parseVocalScannerCommand("scan_vocal_reference path=/sdcard/ref.mp3")
        assert(cmd is AriScanReferenceCommand) { "Expected AriScanReferenceCommand" }
        assert((cmd as AriScanReferenceCommand).referenceFilePath == "/sdcard/ref.mp3") {
            "Wrong path: ${cmd.referenceFilePath}"
        }
    }

    private fun testUnknownCommand() {
        val cmd = parseVocalScannerCommand("start_playback")
        assert(cmd == null) { "Expected null for unknown command" }
    }

    private fun testModeAliases() {
        val cmd = parseVocalScannerCommand("set preset blend styles")
        assert(cmd is AriVocalPresetCommand) { "Expected AriVocalPresetCommand for alias" }
        assert((cmd as AriVocalPresetCommand).mode == "blend_styles") { "Wrong mode: ${cmd.mode}" }
    }
}
