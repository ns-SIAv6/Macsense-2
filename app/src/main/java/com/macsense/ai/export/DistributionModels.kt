package com.macsense.ai.export

import kotlinx.serialization.Serializable

/**
 * Metadata submitted to a distribution provider when releasing a track.
 * Maps to the standard fields required by DistroKit, TuneCore, DistroKid, etc.
 */
@Serializable
data class DistributionMetadata(
    val trackTitle: String,
    val artistName: String,
    val albumTitle: String? = null,
    val isrc: String? = null,
    val releaseDate: Long? = null,
    val genre: String? = null,
    val explicit: Boolean = false,
    val coverArtUri: String? = null,
    val audioFileUri: String,
    val format: ExportFormat = ExportFormat.FULL_MIX
) {
    /**
     * Generates a compliant ISRC code if none is provided.
     * Format: CCXXXYYSSSSS (country code + registrant + year + designation)
     */
    fun generateIsrc(countryCode: String = "US", registrantCode: String = "MAC"): String {
        val year = (System.currentTimeMillis() / 31536000000L % 100).toInt()
        val designation = (System.currentTimeMillis() % 100000).toString().padStart(5, '0')
        return "$countryCode$registrantCode${year.toString().padStart(2, '0')}$designation"
    }
}

/**
 * The status of a distribution submission to a streaming platform.
 */
@Serializable
data class DistributionStatus(
    val id: String,
    val projectId: String,
    val metadata: DistributionMetadata,
    val provider: DistributionProvider,
    val state: DistributionState,
    val submittedAt: Long,
    val updatedAt: Long,
    val providerTrackId: String? = null,
    val errorMessage: String? = null,
    val platformStatuses: Map<String, PlatformStatus> = emptyMap()
) {
    enum class DistributionState {
        PREPARING, UPLOADING, PROCESSING, PENDING_RELEASE, LIVE, REJECTED, TAKEN_DOWN
    }

    @Serializable
    data class PlatformStatus(
        val platform: String, // "spotify", "apple_music", "tiktok", etc.
        val state: String,    // "pending", "live", "processing", "rejected"
        val url: String? = null,
        val liveDate: Long? = null
    )
}

/**
 * Supported distribution providers.
 * The actual API integration requires vendor selection and OAuth credentials.
 */
enum class DistributionProvider(val displayName: String, val apiDocumentation: String) {
    DISTROKIT("DistroKit", "https://distrokit.com/api/docs"),
    TUNECORE("TuneCore", "https://developer.tunecore.com"),
    DISTROKID("DistroKid", "https://distrokid.com/api"),
    SYMPLIFY("Symplify", "https://www.symplify.amsterdam/api");
}

/**
 * Interface for distribution provider integrations.
 * Concrete implementations will make API calls to the chosen provider.
 */
interface DistributionClient {
    val provider: DistributionProvider
    
    /**
     * Submits a track for distribution with the given metadata.
     * Returns the provider's track ID for status tracking.
     */
    suspend fun submit(metadata: DistributionMetadata): Result<String>
    
    /**
     * Checks the current status of a submitted track.
     */
    suspend fun checkStatus(providerTrackId: String): Result<DistributionStatus>
    
    /**
     * Takes down a track from all platforms.
     */
    suspend fun takedown(providerTrackId: String): Result<Unit>
}
