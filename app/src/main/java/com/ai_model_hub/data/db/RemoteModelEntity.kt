package com.ai_model_hub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai_model_hub.data.remote.DefaultConfig
import com.ai_model_hub.data.remote.RemoteModel

@Entity(tableName = "remote_models")
data class RemoteModelEntity(
    @PrimaryKey val modelId: String,
    val name: String,
    val displayName: String,
    val description: String,
    val sizeInBytes: Long,
    val modelFile: String,
    val commitHash: String,
    val maxTokens: Int,
    val accelerators: String,
    val updateInfo: String,
)

fun RemoteModelEntity.toRemoteModel(): RemoteModel = RemoteModel(
    name = name,
    displayName = displayName,
    description = description,
    sizeInBytes = sizeInBytes,
    modelFile = modelFile,
    commitHash = commitHash,
    modelId = modelId,
    defaultConfig = DefaultConfig(
        topK = 0,
        topP = 0f,
        temperature = 0f,
        maxContextLength = 0,
        maxTokens = maxTokens,
        accelerators = accelerators,
    ),
    updateInfo = updateInfo,
)

fun RemoteModel.toEntity(): RemoteModelEntity = RemoteModelEntity(
    name = name,
    displayName = displayName.ifBlank { name },
    description = description,
    sizeInBytes = sizeInBytes,
    modelFile = modelFile,
    commitHash = commitHash,
    modelId = modelId,
    maxTokens = defaultConfig.maxTokens,
    accelerators = defaultConfig.accelerators,
    updateInfo = updateInfo,
)
