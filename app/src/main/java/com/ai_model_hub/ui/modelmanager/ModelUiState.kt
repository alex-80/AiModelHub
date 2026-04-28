package com.ai_model_hub.ui.modelmanager

import com.ai_model_hub.data.ModelDownloadStatus
import com.ai_model_hub.data.ModelDownloadStatusType
import com.ai_model_hub.sdk.Model

data class ModelUiState(
    val model: Model,
    val downloadStatus: ModelDownloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
    val isDownloaded: Boolean = false,
    val isEnabled: Boolean = false,
)