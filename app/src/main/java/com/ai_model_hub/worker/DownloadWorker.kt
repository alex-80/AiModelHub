package com.ai_model_hub.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ai_model_hub.MainActivity
import com.ai_model_hub.R
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_ERROR_MESSAGE
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_FILE_NAME
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_MODEL_DIR
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_RATE
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES
import com.ai_model_hub.data.KEY_MODEL_DOWNLOAD_REMAINING_MS
import com.ai_model_hub.data.KEY_MODEL_NAME
import com.ai_model_hub.data.KEY_MODEL_TOTAL_BYTES
import com.ai_model_hub.data.KEY_MODEL_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DownloadWorker"
private const val CHANNEL_ID = "model_download_channel"
private const val TMP_FILE_EXT = "tmp"

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationId: Int = params.id.hashCode()

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_download),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_download_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUrl = inputData.getString(KEY_MODEL_URL) ?: return@withContext Result.failure()
        val fileName =
            inputData.getString(KEY_MODEL_DOWNLOAD_FILE_NAME) ?: return@withContext Result.failure()
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        val modelDir =
            inputData.getString(KEY_MODEL_DOWNLOAD_MODEL_DIR) ?: return@withContext Result.failure()
        val totalBytes = inputData.getLong(KEY_MODEL_TOTAL_BYTES, 0L)

        setForeground(createForegroundInfo(0, modelName))

        try {
            val outputDir = File(modelDir)
            if (!outputDir.exists()) outputDir.mkdirs()

            val tmpFile = File(outputDir, "$fileName.$TMP_FILE_EXT")
            val existingBytes = tmpFile.length()

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
                connection.setRequestProperty("Accept-Encoding", "identity")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK &&
                connection.responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                throw IOException("HTTP error: ${connection.responseCode}")
            }

            var downloadedBytes = existingBytes
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tmpFile, existingBytes > 0)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            var lastReportTs = 0L
            val bytesBuffer = mutableListOf<Long>()
            val latencyBuffer = mutableListOf<Long>()
            var deltaBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                deltaBytes += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportTs > 200) {
                    if (lastReportTs != 0L) {
                        if (bytesBuffer.size == 5) bytesBuffer.removeAt(0)
                        bytesBuffer.add(deltaBytes)
                        if (latencyBuffer.size == 5) latencyBuffer.removeAt(0)
                        latencyBuffer.add(now - lastReportTs)
                        deltaBytes = 0L
                    }
                    val bytesPerMs = if (bytesBuffer.isNotEmpty() && latencyBuffer.sum() > 0)
                        bytesBuffer.sum().toFloat() / latencyBuffer.sum() else 0f
                    val remainingMs = if (bytesPerMs > 0 && totalBytes > 0)
                        ((totalBytes - downloadedBytes) / bytesPerMs).toLong() else 0L

                    setProgress(
                        Data.Builder()
                            .putLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, downloadedBytes)
                            .putLong(KEY_MODEL_DOWNLOAD_RATE, (bytesPerMs * 1000).toLong())
                            .putLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, remainingMs)
                            .build()
                    )

                    val progress =
                        if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                    setForeground(createForegroundInfo(progress, modelName))
                    lastReportTs = now
                }
            }
            outputStream.close()
            inputStream.close()

            // Rename tmp to final
            val finalFile = File(outputDir, fileName)
            if (finalFile.exists()) finalFile.delete()
            tmpFile.renameTo(finalFile)

            Log.d(TAG, "Download complete: $fileName")
            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "Download error", e)
            Result.failure(
                Data.Builder().putString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE, e.message).build()
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(0)

    private fun createForegroundInfo(progress: Int, modelName: String? = null): ForegroundInfo {
        val title = if (modelName != null)
            applicationContext.getString(R.string.notification_download_in_progress, modelName)
        else
            applicationContext.getString(R.string.notification_download_title)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(applicationContext.getString(R.string.notification_download_progress, progress))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()
        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
