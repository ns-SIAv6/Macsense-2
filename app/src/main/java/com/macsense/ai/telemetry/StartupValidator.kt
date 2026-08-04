package com.macsense.ai.telemetry

import com.macsense.ai.BuildConfig

/**
 * Validates required environment/config at process startup so misconfiguration surfaces
 * as one clear log line instead of a deep Retrofit/JSON stack trace the first time a user
 * talks to Ari. This does not crash the app: Macsense is designed to keep working via the
 * offline Ari fallback when no key is configured, so validation failures are logged as
 * warnings, not fatal errors.
 */
object StartupValidator {
    private val placeholderKeys = setOf("", "MY_GEMINI_API_KEY", "unspecified")

    data class Result(val isGeminiKeyConfigured: Boolean, val message: String)

    fun validateGeminiKey(key: String): Result {
        val isPlaceholder = key.isEmpty() || key in placeholderKeys
        return if (isPlaceholder) {
            val message = "GEMINI_API_KEY is not configured (using placeholder/blank value). " +
                "Ari will run in local offline mode. Set a real key in .env to enable cloud responses."
            AppLogger.w("StartupValidator", message)
            Result(isGeminiKeyConfigured = false, message = message)
        } else {
            val message = "GEMINI_API_KEY configured (length=${key.length}). Cloud Ari responses enabled."
            AppLogger.i("StartupValidator", message)
            Result(isGeminiKeyConfigured = true, message = message)
        }
    }

    fun runAll() {
        validateGeminiKey(BuildConfig.GEMINI_API_KEY)
    }
}
