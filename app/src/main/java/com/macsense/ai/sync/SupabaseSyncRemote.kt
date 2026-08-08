package com.macsense.ai.sync

import com.macsense.ai.data.local.ProjectEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * P8 (issue #43): cloud mirror of a project row as stored in the Supabase `projects` table.
 * Room stays the source of truth; this is the durable backup shape.
 */
@Serializable
data class CloudProject(
    /** Supabase row id (uuid). Null when uploading a project for the first time. */
    @SerialName("id") val id: String? = null,
    @SerialName("local_id") val localId: String,
    @SerialName("name") val name: String,
    @SerialName("bpm") val bpm: Double,
    @SerialName("created_at_ms") val createdAtMs: Long,
    @SerialName("updated_at_ms") val updatedAtMs: Long,
) {
    companion object {
        fun fromEntity(e: ProjectEntity) = CloudProject(
            id = e.cloudId,
            localId = e.id,
            name = e.name,
            bpm = e.bpm,
            createdAtMs = e.createdAt,
            updatedAtMs = e.updatedAt,
        )
    }
}

/**
 * Remote boundary for project sync. Implemented against Supabase's PostgREST API in
 * production and by fakes in tests, keeping the offline-first engine fully unit-testable.
 */
interface SupabaseSyncRemote {
    /** Upserts a project row; returns the stored row (with cloud id). Throws on failure. */
    suspend fun upsertProject(project: CloudProject): CloudProject

    /** Fetches the cloud row for a local project id, or null when it doesn't exist. */
    suspend fun fetchProject(localId: String): CloudProject?
}

/**
 * Validated client configuration for the Supabase project mirror.
 *
 * Only a public client key is accepted here. A service-role or secret key must never be shipped
 * in an Android package because it bypasses the database's row-level security boundary.
 */
data class ValidatedSupabaseConfig(
    val baseUrl: String,
    val anonKey: String,
    val userAccessToken: String,
)

object SupabaseSyncConfiguration {
    private val placeholders = setOf(
        "",
        "MY_SUPABASE_URL",
        "MY_SUPABASE_ANON_KEY",
        "MY_SUPABASE_ACCESS_TOKEN",
        "SUPABASE_URL",
        "SUPABASE_ANON_KEY",
        "SUPABASE_ACCESS_TOKEN",
        "unspecified",
    )

    data class Validation(
        val config: ValidatedSupabaseConfig?,
        val message: String,
    ) {
        val isConfigured: Boolean
            get() = config != null
    }

    fun validate(baseUrl: String, anonKey: String, userAccessToken: String): Validation {
        val candidateUrl = baseUrl.trim()
        val candidateKey = anonKey.trim()
        val candidateToken = userAccessToken.trim()
        if (candidateUrl in placeholders || candidateKey in placeholders || candidateToken in placeholders) {
            return Validation(
                null,
                "Supabase URL, public client key, or authenticated user access token is blank or a placeholder.",
            )
        }

        val parsedUrl = candidateUrl.toHttpUrlOrNull()
            ?: return Validation(null, "Supabase URL is not a valid URL.")
        if (parsedUrl.scheme != "https") {
            return Validation(null, "Supabase URL must use HTTPS.")
        }
        if (parsedUrl.host.isBlank() || parsedUrl.encodedPath != "/" ||
            parsedUrl.query != null || parsedUrl.fragment != null
        ) {
            return Validation(null, "Supabase URL must be the project origin without a path or query.")
        }

        val normalizedKey = candidateKey.lowercase()
        if (normalizedKey.startsWith("service_role") ||
            normalizedKey.startsWith("sb_secret_") ||
            normalizedKey.contains("service-role")
        ) {
            return Validation(null, "A privileged Supabase service or secret key is not allowed in the Android client.")
        }

        val normalizedUrl = parsedUrl.newBuilder()
            .encodedPath("/")
            .build()
            .toString()
            .removeSuffix("/")
        return Validation(
            ValidatedSupabaseConfig(normalizedUrl, candidateKey, candidateToken),
            "Supabase project URL, public client key, and authenticated user token are configured.",
        )
    }
}

/**
 * PostgREST implementation. `baseUrl` is the Supabase project URL
 * (https://<ref>.supabase.co), `apiKey` the public anon/publishable key, and
 * `userAccessToken` an authenticated user's short-lived JWT. Validation is performed by
 * [SupabaseSyncConfiguration] before this class is constructed.
 */
class SupabasePostgrestRemote(
    private val baseUrl: String,
    private val apiKey: String,
    private val userAccessToken: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SupabaseSyncRemote {

    private val mediaType = "application/json".toMediaType()

    override suspend fun upsertProject(project: CloudProject): CloudProject {
        val body = json.encodeToString(CloudProject.serializer(), project)
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegments("rest/v1/projects")
            ?.addQueryParameter("on_conflict", "local_id")
            ?.build()
            ?: error("Supabase base URL is invalid")
        val request = Request.Builder()
            .url(url)
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $userAccessToken")
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .post(body.toRequestBody(mediaType))
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "Supabase upsert failed: HTTP ${resp.code} $text" }
            val rows = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(CloudProject.serializer()), text
            )
            return rows.firstOrNull() ?: error("Supabase upsert returned no rows")
        }
    }

    override suspend fun fetchProject(localId: String): CloudProject? {
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegments("rest/v1/projects")
            ?.addQueryParameter("local_id", "eq.$localId")
            ?.addQueryParameter("select", "*")
            ?.build()
            ?: error("Supabase base URL is invalid")
        val request = Request.Builder()
            .url(url)
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $userAccessToken")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "Supabase fetch failed: HTTP ${resp.code} $text" }
            val rows = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(CloudProject.serializer()), text
            )
            return rows.firstOrNull()
        }
    }
}
