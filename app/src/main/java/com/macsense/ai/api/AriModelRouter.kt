package com.macsense.ai.api

enum class ModelTier(val modelName: String, val endpointUrl: String) {
    FAST(
        modelName = "gemini-3.5-flash",
        endpointUrl = "v1beta/models/gemini-3.5-flash:generateContent"
    ),
    CREATIVE(
        modelName = "gemini-3.5-pro",
        endpointUrl = "v1beta/models/gemini-3.5-pro:generateContent"
    )
}

object AriModelRouter {

    fun routeTier(commandType: String?): ModelTier {
        if (commandType == null) return ModelTier.FAST
        return when (commandType.lowercase().trim()) {
            "update_bpm", "update_effects", "apply_preset" -> ModelTier.FAST
            "update_lyrics", "reorder_sections", "breed_sounds", "resurrect_sound", "creative_writing" -> ModelTier.CREATIVE
            else -> ModelTier.FAST
        }
    }

    fun routeTier(command: AriCommand?): ModelTier {
        return routeTier(command?.type)
    }

    fun getEndpointUrl(commandType: String?): String {
        return routeTier(commandType).endpointUrl
    }

    fun getEndpointUrl(command: AriCommand?): String {
        return getEndpointUrl(command?.type)
    }

    fun getEndpointUrl(tier: ModelTier): String {
        return tier.endpointUrl
    }
}
