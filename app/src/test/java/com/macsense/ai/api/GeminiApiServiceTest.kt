package com.macsense.ai.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the Gemini API layer against a local MockWebServer instead of the real
 * network, verifying request shape (header-based auth, no key in the URL) and response
 * parsing without depending on network access or a real API key.
 */
class GeminiApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: GeminiApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = retrofit.create(GeminiApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends api key as header not query parameter`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"hi"}]}}]}"""
            ).setResponseCode(200)
        )
        service.generateContent("gemini-2.0-flash", "secret-key-value", GenerateContentRequest(contents = emptyList()))
        val recorded = server.takeRequest()
        assertEquals("secret-key-value", recorded.getHeader("x-goog-api-key"))
        assertTrue(!recorded.path!!.contains("secret-key-value"))
        assertTrue(!recorded.path!!.contains("key="))
        assertEquals("/v1beta/models/gemini-2.0-flash:generateContent", recorded.path)
    }

    @Test
    fun `parses successful response into candidates`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"vision applied"}]}}]}"""
            ).setResponseCode(200)
        )
        val response = service.generateContent("gemini-2.0-flash", "k", GenerateContentRequest(contents = emptyList()))
        assertNotNull(response.candidates)
        assertEquals("vision applied", response.candidates!!.first().content?.parts?.firstOrNull()?.text)
    }

    @Test
    fun `handles response with empty candidates list`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"candidates":[]}""").setResponseCode(200))
        val response = service.generateContent("gemini-2.0-flash", "k", GenerateContentRequest(contents = emptyList()))
        assertNotNull(response.candidates)
        assertTrue(response.candidates!!.isEmpty())
    }

    @Test
    fun `serializes request body with system instruction and contents`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"candidates":[]}""").setResponseCode(200))
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = "yo ari")))),
            systemInstruction = Content(parts = listOf(Part(text = "system prompt")))
        )
        service.generateContent("gemini-2.0-flash", "k", request)
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("yo ari"))
        assertTrue(body.contains("system prompt"))
    }
}
