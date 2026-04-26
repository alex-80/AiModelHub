package com.ai_model_hub.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ai_model_hub.sdk.Model
import com.ai_model_hub.sdk.ModelAllowlist
import com.ai_model_hub.runtime.LiteRtLmHelper
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

        override fun loadModel(modelName: String) {
            Log.d(TAG, "loadModel: $modelName")
            val modelSpec = ModelAllowlist.findByName(modelName) ?: run {
                Log.w(TAG, "Model not found in allowlist: $modelName")
                return
            }
            if (_models.containsKey(modelName)) {
                Log.d(TAG, "Model already loaded: $modelName")
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
                    } else {
                        Log.e(TAG, "Failed to load model: $error")
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

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        _models.values.forEach { LiteRtLmHelper.cleanUp(it) }
        _models.clear()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
