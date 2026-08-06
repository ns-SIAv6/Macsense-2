package com.macsense.ai.api

import com.macsense.ai.telemetry.AppLogger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Request body sent to the Macsense backend gateway's `/v1/ari/chat` endpoint. Intentionally
 * mirrors [GenerateContentRequest] (minus [GenerationConfig]) so the gateway stays a thin,
 * low-risk pass-through to Gemini rather than a second place request-shaping logic can drift.
 */
@kotlinx.serialization.Serializable
data class AriChatGatewayRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

interface GatewayApiService {
    @POST("v1/ari/chat")
    suspend fun chat(
        @Header("Authorization") authorization: String?,
        @Body request: AriChatGatewayRequest
    ): GenerateContentResponse
}

/**
 * Retrofit client for the Macsense backend gateway (see `server/`). This is what the app
 * should use for all live Ari requests going forward: the gateway holds `GEMINI_API_KEY`
 * server-side, so the client never needs to know it. [RetrofitClient] (direct-to-Gemini)
 * is kept only for reference/tests and must not be wired into production chat flows.
 */
object GatewayClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Builds the Retrofit service for a given gateway base URL. The base URL is read from
     * [com.macsense.ai.BuildConfig.GATEWAY_BASE_URL] by callers rather than hardcoded here,
     * so debug builds can point at a local dev server and release builds at a deployed one.
     */
    fun create(baseUrl: String): GatewayApiService {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        AppLogger.i("GatewayClient", "Configured gateway base URL: $normalizedBaseUrl")
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(GatewayApiService::class.java)
    }
}
