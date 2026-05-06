package com.ai_model_hub.sdk

private val NORMALIZE_NAME_REGEX = Regex("[^a-zA-Z0-9]")

data class Model(
    val name: String,
    val displayName: String = "",
    val description: String = "",
    val url: String = "",
    val sizeInBytes: Long = 0L,
    val downloadFileName: String = "_",
    val version: String = "_",
    val huggingFaceRepo: String = "",
    val maxTokens: Int = 1024,
    val supportedBackends: List<BackendPreference> = listOf(BackendPreference.CPU),
    var normalizedName: String = "",
    var initializing: Boolean = false,
    var configValues: Map<String, Any> = mapOf(),
) {
    init {
        normalizedName = NORMALIZE_NAME_REGEX.replace(name, "_")
    }

}

