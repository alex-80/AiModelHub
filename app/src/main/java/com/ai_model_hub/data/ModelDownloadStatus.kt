package com.ai_model_hub.data

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0L,
    val receivedBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val remainingMs: Long = 0L,
    val errorMessage: String = "",
)