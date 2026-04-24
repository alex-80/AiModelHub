package com.ai_model_hub.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ai_model_hub.service.IAiModelHubService
import com.ai_model_hub.service.IAiResponseCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

private const val HOST_PACKAGE = "com.ai_model_hub"
private const val SERVICE_CLASS = "com.ai_model_hub.service.AiModelHubService"

/**
 * SDK entry point for binding to AiModelHub service.
 *
 * Usage:
 * ```
 * val client = AiHubClient(context)
 * client.connect()
 * // observe connectionState until Connected
 * client.loadModel("Gemma 4 E2B")
 * client.sendMessage("Gemma 4 E2B", "Hello").collect { token -> ... }
 * client.disconnect()
 * ```
 */
class AiHubClient(private val context: Context) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /** Observe binding state changes. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = IAiModelHubService.Stub.asInterface(binder)
            _connectionState.value = ConnectionState.Connected(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun onBindingDied(name: ComponentName) {
            _connectionState.value =
                ConnectionState.Error("Binding died — AiModelHub may have crashed")
        }

        override fun onNullBinding(name: ComponentName) {
            _connectionState.value = ConnectionState.Error("Service returned null binder")
        }
    }

    /** Bind to AiModelHub service. Safe to call multiple times. */
    fun connect() {
        if (_connectionState.value !is ConnectionState.Disconnected) return
        _connectionState.value = ConnectionState.Connecting
        val intent = Intent().apply {
            component = ComponentName(HOST_PACKAGE, SERVICE_CLASS)
        }
        val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            _connectionState.value = ConnectionState.Error(
                "bindService failed — is AiModelHub installed? (package: $HOST_PACKAGE)"
            )
        }
    }

    /** Unbind from the service. */
    fun disconnect() {
        if (_connectionState.value is ConnectionState.Disconnected) return
        try {
            context.unbindService(serviceConnection)
        } catch (_: IllegalArgumentException) { /* not bound */
        }
        _connectionState.value = ConnectionState.Disconnected
    }

    // ---- Convenience helpers that throw if not connected ----

    private fun requireService(): IAiModelHubService {
        return (_connectionState.value as? ConnectionState.Connected)?.service
            ?: error("Not connected to AiModelHub service. Call connect() and wait for Connected state.")
    }

    /** Load a model by name (e.g. "Gemma 4 E2B"). Blocking binder call — run on IO thread. */
    fun loadModel(modelName: String) = requireService().loadModel(modelName)

    /** Unload a previously loaded model. */
    fun unloadModel(modelName: String) = requireService().unloadModel(modelName)

    /** Returns true if the model is currently loaded and ready. */
    fun isModelLoaded(modelName: String): Boolean = requireService().isModelLoaded(modelName)

    /** Returns names of all currently loaded models. */
    fun getLoadedModels(): List<String> = requireService().getLoadedModels()

    /** Stop an ongoing generation for the given model. */
    fun stopGeneration(modelName: String) = requireService().stopGeneration(modelName)

    /** Reset conversation history for the given model. */
    fun resetSession(modelName: String) = requireService().resetSession(modelName)

    /**
     * Send a message and receive streaming tokens as a Flow.
     *
     * Each emitted [String] is one incremental token. The flow completes when
     * generation finishes, or throws an exception on error.
     *
     * Example:
     * ```
     * client.sendMessage("Gemma 4 E2B", "Tell me a joke")
     *     .collect { token -> print(token) }
     * ```
     */
    fun sendMessage(modelName: String, message: String): Flow<String> = callbackFlow {
        val service = requireService()
        service.sendMessage(modelName, message, object : IAiResponseCallback.Stub() {
            override fun onToken(token: String) {
                trySend(token)
            }

            override fun onComplete(fullText: String) {
                close()
            }

            override fun onError(errorMessage: String) {
                close(RuntimeException(errorMessage))
            }
        })
        awaitClose {
            // Cancel generation if the collector is cancelled mid-stream
            try {
                service.stopGeneration(modelName)
            } catch (_: Exception) {
            }
        }
    }
}
