package com.ai_model_hub.ui.modelmanager

import com.ai_model_hub.data.ModelDownloadStatus
import com.ai_model_hub.data.ModelDownloadStatusType
import com.ai_model_hub.data.remote.RemoteModel

data class ModelUiState(
    val model: RemoteModel,
    val updatableModel: RemoteModel? = null,
    val downloadStatus: ModelDownloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
    val isDownloaded: Boolean = false,
    val isEnabled: Boolean = false,
    val hasUpdate: Boolean = false,
    val updateInfo: String = "",
)