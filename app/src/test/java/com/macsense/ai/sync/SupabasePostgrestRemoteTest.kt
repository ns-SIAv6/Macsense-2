package com.macsense.ai.sync

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabasePostgrestRemoteTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: SupabasePostgrestRemote

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        remote = SupabasePostgrestRemote(
            baseUrl = server.url("/").toString().removeSuffix("/"),
            apiKey = "public-anon-key",
            userAccessToken = "user-access-token",
            client = OkHttpClient(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `upsert uses PostgREST conflict target and public auth headers`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """[{"id":"cloud-1","local_id":"local-1","name":"Night Drive","bpm":120.0,"created_at_ms":1,"updated_at_ms":2}]"""
                ),
        )

        val stored = remote.upsertProject(
            CloudProject(
                localId = "local-1",
                name = "Night Drive",
                bpm = 120.0,
                createdAtMs = 1L,
                updatedAtMs = 2L,
            ),
        )
        val request = server.takeRequest()

        assertEquals("cloud-1", stored.id)
        assertEquals("/rest/v1/projects?on_conflict=local_id", request.path)
        assertEquals("public-anon-key", request.getHeader("apikey"))
        assertEquals("Bearer user-access-token", request.getHeader("Authorization"))
        assertTrue(request.getHeader("Prefer")!!.contains("resolution=merge-duplicates"))
        assertTrue(request.body.readUtf8().contains("\"local_id\":\"local-1\""))
    }

    @Test
    fun `fetch encodes local project ids and returns no row as null`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = remote.fetchProject("local/id with spaces")
        val request = server.takeRequest()

        assertEquals(null, result)
        assertEquals(
            "/rest/v1/projects?local_id=eq.local%2Fid%20with%20spaces&select=*",
            request.path,
        )
    }

    @Test
    fun `failed HTTP responses are surfaced instead of treated as sync success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        val error = runCatching {
            remote.fetchProject("local-1")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("HTTP 401"))
    }
}