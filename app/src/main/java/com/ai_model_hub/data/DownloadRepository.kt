package com.ai_model_hub.data

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ai_model_hub.sdk.Model
import com.ai_model_hub.worker.DownloadWorker
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepository"
const val KEY_MODEL_NAME = "modelName"
const val KEY_MODEL_URL = "modelUrl"
const val KEY_MODEL_DOWNLOAD_FILE_NAME = "downloadFileName"
const val KEY_MODEL_DOWNLOAD_MODEL_DIR = "modelDir"
const val KEY_MODEL_TOTAL_BYTES = "totalBytes"
const val KEY_MODEL_DOWNLOAD_RECEIVED_BYTES = "receivedBytes"
const val KEY_MODEL_DOWNLOAD_RATE = "downloadRate"
const val KEY_MODEL_DOWNLOAD_REMAINING_MS = "remainingMs"
const val KEY_MODEL_DOWNLOAD_ERROR_MESSAGE = "errorMessage"

interface DownloadRepository {
    fun downloadModel(
        model: Model,
        onStatusUpdated: (model: Model, status: ModelDownloadStatus) -> Unit,
    )

    fun cancelDownload(model: Model)
    fun cancelAll(onComplete: () -> Unit)
}

@Singleton
class DefaultDownloadRepository @Inject constructor(
    private val context: Context,
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun downloadModel(
        model: Model,
        onStatusUpdated: (model: Model, status: ModelDownloadStatus) -> Unit,
    ) {
        val inputData = Data.Builder()
            .putString(KEY_MODEL_NAME, model.name)
            .putString(KEY_MODEL_URL, model.url)
            .putString(KEY_MODEL_DOWNLOAD_MODEL_DIR, model.getModelDir(context))
            .putString(KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
            .putLong(KEY_MODEL_TOTAL_BYTES, model.sizeInBytes)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .addTag("$KEY_MODEL_NAME:${model.name}")
            .build()

        workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.REPLACE, request)
        observeProgress(workerId = request.id, model = model, onStatusUpdated = onStatusUpdated)
    }

    override fun cancelDownload(model: Model) {
        workManager.cancelAllWorkByTag("$KEY_MODEL_NAME:${model.name}")
    }

    override fun cancelAll(onComplete: () -> Unit) {
        workManager.cancelAllWork().result
            .addListener({ onComplete() }, Executors.newSingleThreadExecutor())
    }

    private fun observeProgress(
        workerId: UUID,
        model: Model,
        onStatusUpdated: (model: Model, status: ModelDownloadStatus) -> Unit,
    ) {
        workManager.getWorkInfoByIdLiveData(workerId).observeForever { workInfo ->
            if (workInfo == null) return@observeForever
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val receivedBytes =
                        workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L)
                    val rate = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L)
                    val remaining = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L)
                    if (receivedBytes > 0L) {
                        onStatusUpdated(
                            model, ModelDownloadStatus(
                                status = ModelDownloadStatusType.IN_PROGRESS,
                                totalBytes = model.sizeInBytes,
                                receivedBytes = receivedBytes,
                                bytesPerSecond = rate,
                                remainingMs = remaining,
                            )
                        )
                    }
                }

                WorkInfo.State.SUCCEEDED -> {
                    Log.d(TAG, "Download succeeded: ${model.name}")
                    onStatusUpdated(
                        model,
                        ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED)
                    )
                }

                WorkInfo.State.FAILED -> {
                    val error =
                        workInfo.outputData.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: ""
                    Log.e(TAG, "Download failed: $error")
                    onStatusUpdated(
                        model,
                        ModelDownloadStatus(
                            status = ModelDownloadStatusType.FAILED,
                            errorMessage = error
                        )
                    )
                }

                WorkInfo.State.CANCELLED -> {
                    onStatusUpdated(
                        model,
                        ModelDownloadStatus(status = ModelDownloadStatusType.CANCELLED)
                    )
                }

                else -> {}
            }
        }
    }
}
