package com.ai_model_hub.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.ai_model_hub.runtime.LiteRtLmHelper
import com.ai_model_hub.sdk.Model
import com.ai_model_hub.sdk.ModelAllowlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

private const val TAG = "AiModelHubService"

class AiModelHubService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Named _models to avoid clash with Kotlin's property synthesis of AIDL's getLoadedModels()
    private val _models: MutableMap<String, Model> = mutableMapOf()

    private val binder = object : IAiModelHubService.Stub() {

        override fun getLoadedModels(): List<String> {
            return _models.keys.toList()
        }

        override fun loadModel(modelName: String, callback: ILoadModelCallback) {
            Log.d(TAG, "loadModel: $modelName")
            val modelSpec = ModelAllowlist.findByName(modelName) ?: run {
                Log.w(TAG, "Model not found in allowlist: $modelName")
                callback.onError("Model not found in allowlist: $modelName")
                return
            }
            if (_models.containsKey(modelName)) {
                Log.d(TAG, "Model already loaded: $modelName")
                callback.onSuccess()
                return
            }
            val model = modelSpec.copy()
            LiteRtLmHelper.initialize(
                context = applicationContext,
                model = model,
                onDone = { error ->
                    if (error.isEmpty()) {
                        _models[modelName] = model
                        Log.d(TAG, "Model loaded successfully: $modelName")
                        callback.onSuccess()
                    } else {
                        Log.e(TAG, "Failed to load model: $error")
                        callback.onError(error)
                    }
                }
            )
        }

        override fun unloadModel(modelName: String) {
            Log.d(TAG, "unloadModel: $modelName")
            _models.remove(modelName)?.let { model ->
                LiteRtLmHelper.cleanUp(model)
            }
        }

        override fun isModelLoaded(modelName: String): Boolean {
            return _models.containsKey(modelName) && _models[modelName]?.instance != null
        }

        override fun sendMessage(
            modelName: String,
            message: String,
            callback: IAiResponseCallback
        ) {
            Log.d(TAG, "sendMessage to $modelName: ${message.take(50)}")
            val model = _models[modelName] ?: run {
                callback.onError("Model '$modelName' is not loaded")
                return
            }
            val sb = StringBuilder()
            LiteRtLmHelper.runInference(
                model = model,
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

        override fun stopGeneration(modelName: String) {
            _models[modelName]?.let { LiteRtLmHelper.stopGeneration(it) }
        }

        override fun resetSession(modelName: String) {
            _models[modelName]?.let { LiteRtLmHelper.resetConversation(it) }
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
        _models.values.forEach { LiteRtLmHelper.cleanUp(it) }
        _models.clear()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
