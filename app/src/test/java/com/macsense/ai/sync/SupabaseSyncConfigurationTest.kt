package com.macsense.ai.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncConfigurationTest {

    @Test
    fun `valid project origin and public key are accepted and normalized`() {
        val result = SupabaseSyncConfiguration.validate(
            " https://example.supabase.co/ ",
            " public-anon-key ",
            " user-access-token ",
        )

        assertTrue(result.isConfigured)
        assertEquals("https://example.supabase.co", result.config?.baseUrl)
        assertEquals("public-anon-key", result.config?.anonKey)
        assertEquals("user-access-token", result.config?.userAccessToken)
    }

    @Test
    fun `blank and placeholder values stay local only`() {
        listOf(
            Triple("", "public-anon-key", "user-token"),
            Triple("MY_SUPABASE_URL", "public-anon-key", "user-token"),
            Triple("https://example.supabase.co", "", "user-token"),
            Triple("https://example.supabase.co", "MY_SUPABASE_ANON_KEY", "user-token"),
            Triple("https://example.supabase.co", "public-anon-key", ""),
            Triple("https://example.supabase.co", "public-anon-key", "MY_SUPABASE_ACCESS_TOKEN"),
        ).forEach { (url, key, token) ->
            val result = SupabaseSyncConfiguration.validate(url, key, token)

            assertFalse("$url / $key / $token", result.isConfigured)
            assertNotNull(result.message)
        }
    }

    @Test
    fun `non HTTPS project origins are rejected`() {
        val result = SupabaseSyncConfiguration.validate(
            "http://example.supabase.co",
            "public-anon-key",
            "user-token",
        )

        assertFalse(result.isConfigured)
        assertTrue(result.message.contains("HTTPS"))
    }

    @Test
    fun `paths queries and fragments are rejected`() {
        listOf(
            "https://example.supabase.co/rest",
            "https://example.supabase.co?project=one",
            "https://example.supabase.co#project",
        ).forEach { url ->
            assertFalse(
                url,
                SupabaseSyncConfiguration.validate(url, "public-anon-key", "user-token").isConfigured,
            )
        }
    }

    @Test
    fun `privileged Supabase keys are rejected from the Android client`() {
        listOf(
            "service_role_secret",
            "sb_secret_123",
            "my-service-role-key",
        ).forEach { key ->
            val result = SupabaseSyncConfiguration.validate(
                "https://example.supabase.co",
                key,
                "user-token",
            )

            assertFalse(key, result.isConfigured)
            assertTrue(result.message.contains("privileged"))
        }
    }
}