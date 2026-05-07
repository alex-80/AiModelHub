package com.ai_model_hub.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.ai_model_hub.data.AppRepository
import com.ai_model_hub.runtime.LiteRtLmHelper
import com.ai_model_hub.runtime.cleanUp
import com.ai_model_hub.runtime.isSessionAlive
import com.ai_model_hub.runtime.resetSession
import com.ai_model_hub.runtime.sendMessage
import com.ai_model_hub.runtime.stopGeneration
import com.ai_model_hub.sdk.ModelAllowlist
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AiModelHubService"

@AndroidEntryPoint
class AiModelHubService : Service() {

    @Inject
    lateinit var appRepository: AppRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var _enabledModels: Set<String> = emptySet()

    private val binder = object : IAiModelHubService.Stub() {

        override fun getAvailableModels(): List<String> {
            return _enabledModels.toList()
        }

        override fun createSession(modelName: String, callback: ICreateSessionCallback) {
            serviceScope.launch {
                val backendPreference = appRepository.backendPreference.first()
                val enableSpeculativeDecoding = appRepository.speculativeDecoding.first()
                Log.d(TAG, "loadModel: $modelName, backend: $backendPreference, speculativeDecoding: $enableSpeculativeDecoding")
                val modelSpec = ModelAllowlist.findByName(modelName) ?: run {
                    Log.w(TAG, "Model not found in allowlist: $modelName")
                    callback.onError("Model not found in allowlist: $modelName")
                    return@launch
                }
                try {
                    val session = LiteRtLmHelper.createSession(
                        context = applicationContext,
                        model = modelSpec,
                        backendPreference = backendPreference,
                        enableSpeculativeDecoding = enableSpeculativeDecoding,
                    )
                    Log.d(TAG, "Model loaded successfully: $modelName")
                    callback.onSuccess(session.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load model: ${e.message}")
                    callback.onError(e.message)
                }
            }

        }

        override fun closeSession(sessionId: String) {
            Log.d(TAG, "unloadModel: $sessionId")
            LiteRtLmHelper.cleanUp(sessionId)
        }

        override fun isSessionAlive(sessionId: String): Boolean {
            return LiteRtLmHelper.isSessionAlive(sessionId)
        }

        override fun sendMessage(
            sessionId: String,
            message: String,
            callback: IAiResponseCallback
        ) {
            Log.d(TAG, "sendMessage to $sessionId: ${message.take(50)}")

            val sb = StringBuilder()
            LiteRtLmHelper.sendMessage(
                sessionId = sessionId,
                input = message,
                onToken = { token, done ->
                    if (!done && token.isNotEmpty()) {
                        sb.append(token)
                        callback.onToken(token)
                    } else if (done) {
                        callback.onComplete(sb.toString())
                    }
                },
                onError = { error ->
                    callback.onError(error)
                },
                coroutineScope = serviceScope,
            )
        }

        override fun stopGeneration(sessionId: String) {
            LiteRtLmHelper.stopGeneration(sessionId)
        }

        override fun resetSession(sessionId: String) {
            LiteRtLmHelper.resetSession(sessionId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            appRepository.enabledModels.collect { enabled ->
                _enabledModels = enabled
            }
        }
    }

    /**
     * Promote to a foreground service when started (via startForegroundService).
     * START_STICKY ensures the system restarts the service if it is killed, keeping
     * the process alive so cross-app bindService() calls never need a cold-start.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        // Also promote when a client binds directly (e.g. first launch before
        // startForegroundService has been called).
        promoteToForeground()
        return binder
    }

    /**
     * Return true so onRebind() is called when new clients bind after all previous
     * clients have unbound. The service intentionally stays running (foreground) even
     * after all clients disconnect — this keeps the process alive for the next bind.
     */
    override fun onUnbind(intent: Intent?): Boolean = true

    private fun promoteToForeground() {
        createServiceNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                buildServiceNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(this))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LiteRtLmHelper.cleanUp()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
