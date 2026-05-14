package com.ai_model_hub.data.remote

import com.ai_model_hub.sdk.Model

data class DefaultConfig(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val maxContextLength: Int,
    val maxTokens: Int,
    val accelerators: String = "cpu",
    val visionAccelerator: String = ""
)

data class RemoteModel(
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val sizeInBytes: Long = 0L,
    val modelFile: String = "",
    val commitHash: String = "",
    val modelId: String = "",
    val defaultConfig: DefaultConfig = DefaultConfig(0, 0f, 0f, 0, 0),
    val updateInfo: String = "",
) {
    val version: String
        get() = commitHash.take(7)
}

data class RemoteAllowlist(
    val models: List<RemoteModel> = listOf(),
)

fun RemoteModel.toModel(): Model = Model(
    name = name,
    displayName = displayName.ifBlank { name },
    description = description,
    modelId = modelId,
)
