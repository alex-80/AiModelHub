package com.ai_model_hub.ui.modelmanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.data.AllowlistRepository
import com.ai_model_hub.data.AppRepository
import com.ai_model_hub.data.DownloadRepository
import com.ai_model_hub.data.ModelDownloadStatus
import com.ai_model_hub.data.ModelDownloadStatusType
import com.ai_model_hub.data.db.toEntity
import com.ai_model_hub.data.db.toRemoteModel
import com.ai_model_hub.data.getModelDir
import com.ai_model_hub.data.remote.RemoteModel
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

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val appRepository: AppRepository,
    private val allowlistRepository: AllowlistRepository,
) : ViewModel() {

    private val _downloadStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())

    val modelUiStates: StateFlow<List<ModelUiState>> = combine(
        allowlistRepository.modelsFlow,
        allowlistRepository.updatableModelsFlow,
        _downloadStatuses,
        appRepository.downloadedModels,
        appRepository.enabledModels,
    ) { entities, updatableEntities, statuses, downloadedSet, enabledSet ->
        entities.map { entity ->
            val remoteModel = entity.toRemoteModel()
            val remoteUpdatableModel =
                updatableEntities.find { it.modelId == entity.modelId }?.toRemoteModel()

            val isDownloaded =
                remoteModel.name in downloadedSet || remoteModel.modelId in downloadedSet

            val status = statuses[remoteModel.modelId] ?: statuses[remoteModel.name]
            ?: if (isDownloaded) ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED)
            else ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)

            val hasUpdate = isDownloaded && remoteUpdatableModel != null &&
                    remoteUpdatableModel.commitHash.isNotEmpty() &&
                    remoteUpdatableModel.commitHash != entity.commitHash

            ModelUiState(
                model = remoteModel,
                updatableModel = remoteUpdatableModel,
                downloadStatus = status,
                isDownloaded = isDownloaded || status.status == ModelDownloadStatusType.SUCCEEDED,
                isEnabled = remoteModel.modelId in enabledSet || remoteModel.name in enabledSet,
                hasUpdate = hasUpdate,
                updateInfo = if (hasUpdate) entity.updateInfo else "",
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allowlistRepository.maybeInitFromAssets()
        }
        viewModelScope.launch {
            allowlistRepository.loadFromRemote()
        }
    }

    fun downloadModel(remoteModel: RemoteModel) {
        Log.d("ModelManagerVM", "Downloading: ${remoteModel.name}")
        downloadRepository.downloadModel(remoteModel) { rm, status ->
            updateStatus(rm, status)
            if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                viewModelScope.launch {
                    appRepository.markModelDownloaded(rm.modelId)
                    allowlistRepository.updateInstalled(
                        remoteModel.toEntity(),
                    )
                }
            }
        }
    }

    fun cancelDownload(
        remoteModel: RemoteModel,
        status: ModelDownloadStatusType = ModelDownloadStatusType.NOT_DOWNLOADED
    ) {
        downloadRepository.cancelDownload(remoteModel)
        updateStatus(
            remoteModel,
            ModelDownloadStatus(status = status)
        )
    }

    fun deleteModel(remoteModel: RemoteModel) {
        val dir = File(remoteModel.getModelDir(context))
        if (dir.exists()) dir.deleteRecursively()
        viewModelScope.launch { appRepository.markModelDeleted(remoteModel.modelId) }
        updateStatus(
            remoteModel,
            ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
        )
    }

    fun updateModel(state: ModelUiState) {
        val updatableModel = state.updatableModel ?: return
        Log.d(
            "ModelManagerVM",
            "Updating ${updatableModel.name} to commitHash=${updatableModel.commitHash}"
        )
        downloadRepository.downloadModel(updatableModel) { rm, status ->
            if (status.status == ModelDownloadStatusType.CANCELLED) {
                updateStatus(
                    updatableModel,
                    ModelDownloadStatus(status = ModelDownloadStatusType.UPDATE_CANCELLED)
                )
                return@downloadModel
            }
            if (status.status == ModelDownloadStatusType.FAILED) {
                updateStatus(
                    updatableModel,
                    ModelDownloadStatus(
                        status = ModelDownloadStatusType.UPDATE_FAILED,
                        errorMessage = status.errorMessage
                    )
                )
                return@downloadModel
            }
            updateStatus(rm, status)
            if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                viewModelScope.launch {
                    val oldCommitHash = state.model.commitHash
                    if (oldCommitHash.isNotEmpty()) {
                        val oldDir = File(state.model.getModelDir(context))
                        if (oldDir.exists()) oldDir.deleteRecursively()
                    }
                    appRepository.markModelDownloaded(rm.modelId)
                    allowlistRepository.updateInstalled(updatableModel.toEntity())
                }
            }
        }
    }

    fun toggleEnabled(remoteModel: RemoteModel) {
        val isCurrentlyEnabled =
            modelUiStates.value.find { it.model.modelId == remoteModel.modelId || it.model.name == remoteModel.name }?.isEnabled
                ?: false
        viewModelScope.launch {
            appRepository.setModelEnabled(
                remoteModel.modelId,
                !isCurrentlyEnabled
            )
            appRepository.setModelEnabled(
                remoteModel.name,
                !isCurrentlyEnabled
            )
        }
    }

    private fun updateStatus(id: String, status: ModelDownloadStatus) {
        _downloadStatuses.value += (id to status)
    }

    private fun updateStatus(remoteModel: RemoteModel, status: ModelDownloadStatus) {
        updateStatus(remoteModel.modelId, status)
    }

}
