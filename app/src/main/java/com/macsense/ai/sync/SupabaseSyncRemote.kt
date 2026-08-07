package com.macsense.ai.sync

import com.macsense.ai.data.local.ProjectEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
 * PostgREST implementation. `baseUrl` is the Supabase project URL
 * (https://<ref>.supabase.co) and `apiKey` the anon/service key — both injected from
 * BuildConfig/.env, never hardcoded.
 */
class SupabasePostgrestRemote(
    private val baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SupabaseSyncRemote {

    private val mediaType = "application/json".toMediaType()

    override suspend fun upsertProject(project: CloudProject): CloudProject {
        val body = json.encodeToString(CloudProject.serializer(), project)
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/projects?on_conflict=local_id")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
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
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/projects?local_id=eq.$localId&select=*")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
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
