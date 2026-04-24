package com.ai_model_hub.data

import android.content.Context
import java.io.File

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
    var normalizedName: String = "",
    var instance: Any? = null,
    var initializing: Boolean = false,
    var configValues: Map<String, Any> = mapOf(),
) {
    init {
        normalizedName = NORMALIZE_NAME_REGEX.replace(name, "_")
    }

    fun getModelFilePath(context: Context): String {
        val baseDir = listOf(
            context.getExternalFilesDir(null)?.absolutePath ?: "",
            normalizedName,
            version
        ).joinToString(File.separator)
        return listOf(baseDir, downloadFileName).joinToString(File.separator)
    }

    fun getModelDir(context: Context): String {
        return listOf(
            context.getExternalFilesDir(null)?.absolutePath ?: "",
            normalizedName,
            version
        ).joinToString(File.separator)
    }
}

enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0L,
    val receivedBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val remainingMs: Long = 0L,
    val errorMessage: String = "",
)

// Keys for WorkManager data passing
const val KEY_MODEL_NAME = "modelName"
const val KEY_MODEL_URL = "modelUrl"
const val KEY_MODEL_DOWNLOAD_FILE_NAME = "downloadFileName"
const val KEY_MODEL_DOWNLOAD_MODEL_DIR = "modelDir"
const val KEY_MODEL_COMMIT_HASH = "commitHash"
const val KEY_MODEL_TOTAL_BYTES = "totalBytes"
const val KEY_MODEL_DOWNLOAD_RECEIVED_BYTES = "receivedBytes"
const val KEY_MODEL_DOWNLOAD_RATE = "downloadRate"
const val KEY_MODEL_DOWNLOAD_REMAINING_MS = "remainingMs"
const val KEY_MODEL_DOWNLOAD_ERROR_MESSAGE = "errorMessage"
const val KEY_MODEL_DOWNLOAD_ACCESS_TOKEN = "accessToken"
const val TMP_FILE_EXT = "tmp"
