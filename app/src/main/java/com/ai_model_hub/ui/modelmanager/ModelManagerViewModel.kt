package com.ai_model_hub.ui.modelmanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.data.AppRepository
import com.ai_model_hub.data.DownloadRepository
import com.ai_model_hub.data.Model
import com.ai_model_hub.data.ModelAllowlist
import com.ai_model_hub.data.ModelDownloadStatus
import com.ai_model_hub.data.ModelDownloadStatusType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModelUiState(
    val model: Model,
    val downloadStatus: ModelDownloadStatus = ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
    val isDownloaded: Boolean = false,
    val isEnabled: Boolean = false,
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _downloadStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())

    val modelUiStates: StateFlow<List<ModelUiState>> = combine(
        _downloadStatuses,
        appRepository.downloadedModels,
        appRepository.enabledModels,
    ) { statuses, downloadedSet, enabledSet ->
        ModelAllowlist.models.map { model ->
            val status = statuses[model.name]
                ?: if (model.name in downloadedSet) ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED)
                else ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
            ModelUiState(
                model = model,
                downloadStatus = status,
                isDownloaded = model.name in downloadedSet || status.status == ModelDownloadStatusType.SUCCEEDED,
                isEnabled = model.name in enabledSet,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun downloadModel(model: Model) {
        Log.d("ModelManagerVM", "Downloading: ${model.name}")
        downloadRepository.downloadModel(model) { m, status ->
            updateStatus(m.name, status)
            if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                viewModelScope.launch { appRepository.markModelDownloaded(m.name) }
            }
        }
    }

    fun cancelDownload(model: Model) {
        downloadRepository.cancelDownload(model)
        updateStatus(
            model.name,
            ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
        )
    }

    fun deleteModel(model: Model) {
        val dir =
            File(context.getExternalFilesDir(null), "${model.normalizedName}/${model.version}")
        if (dir.exists()) dir.deleteRecursively()
        viewModelScope.launch { appRepository.markModelDeleted(model.name) }
        updateStatus(
            model.name,
            ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
        )
    }

    fun toggleEnabled(model: Model) {
        val isCurrentlyEnabled =
            modelUiStates.value.find { it.model.name == model.name }?.isEnabled ?: false
        viewModelScope.launch { appRepository.setModelEnabled(model.name, !isCurrentlyEnabled) }
    }

    private fun updateStatus(modelName: String, status: ModelDownloadStatus) {
        _downloadStatuses.value = _downloadStatuses.value + (modelName to status)
    }
}
