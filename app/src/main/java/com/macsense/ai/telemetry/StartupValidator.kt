package com.macsense.ai.telemetry

import com.macsense.ai.BuildConfig
import com.macsense.ai.sync.SupabaseSyncConfiguration

/**
 * Validates required environment/config at process startup so misconfiguration surfaces
 * as one clear log line instead of a deep Retrofit/JSON stack trace the first time a user
 * talks to Ari. This does not crash the app: Macsense can keep working with deterministic local
 * automation when no key is configured, so validation failures are logged as warnings, not fatal
 * errors. Local automation must not be represented as a cloud AI response.
 */
object StartupValidator {
    private val placeholderKeys = setOf("", "MY_GEMINI_API_KEY", "unspecified")

    data class Result(val isGeminiKeyConfigured: Boolean, val message: String)
    data class SupabaseResult(
        val isConfigured: Boolean,
        val message: String,
        val baseUrl: String? = null,
        val anonKey: String? = null,
        val userAccessToken: String? = null,
    )

    fun validateGeminiKey(key: String): Result {
        val isPlaceholder = key.isEmpty() || key in placeholderKeys
        return if (isPlaceholder) {
            val message = "GEMINI_API_KEY is not configured (using placeholder/blank value). " +
                "Ari will use deterministic local automation, not cloud AI. Set a real key in .env to enable cloud responses."
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

    fun validateSupabase(
        baseUrl: String,
        anonKey: String,
        userAccessToken: String,
    ): SupabaseResult {
        val validation = SupabaseSyncConfiguration.validate(baseUrl, anonKey, userAccessToken)
        if (!validation.isConfigured) {
            AppLogger.w("StartupValidator", "Cloud sync unavailable: ${validation.message}")
            return SupabaseResult(false, validation.message)
        }
        AppLogger.i("StartupValidator", "Cloud sync configured with a validated public client key.")
        return SupabaseResult(
            isConfigured = true,
            message = validation.message,
            baseUrl = validation.config?.baseUrl,
            anonKey = validation.config?.anonKey,
            userAccessToken = validation.config?.userAccessToken,
        )
    }
}
