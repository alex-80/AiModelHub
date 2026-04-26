package com.ai_model_hub.runtime

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.ai_model_hub.data.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CancellationException

private const val TAG = "LiteRtLmHelper"

data class LlmInstance(val engine: Engine, var conversation: Conversation)

typealias TokenListener = (token: String, done: Boolean) -> Unit

@OptIn(ExperimentalApi::class)
object LiteRtLmHelper {

    fun initialize(
        context: Context,
        model: Model,
        maxTokens: Int = 1024,
        topK: Int = 40,
        topP: Float = 0.95f,
        temperature: Float = 0.8f,
        onDone: (errorMsg: String) -> Unit,
    ) {
        val modelPath = model.getModelFilePath(context)
        Log.d(TAG, "Initializing model at: $modelPath")

        // Prefer external storage for the XNNPack cache — it has far more headroom than
        // the internal /data partition, which may have only tens of MB free.
        val xnnpackCacheDir = (context.getExternalFilesDir("xnnpack_cache") ?: context.cacheDir)
            .apply { mkdirs() }

        // XNNPack calls abort() (uncatchable SIGABRT) if it cannot write the cache file.
        // Pre-check space to give a friendly error instead of a hard crash.
        val freeBytes = StatFs(xnnpackCacheDir.absolutePath).availableBytes
        val requiredBytes = 1_500L * 1024 * 1024 // 1.5 GB conservative estimate
        val modelFileName = File(modelPath).name
        val cacheAlreadyExists =
            xnnpackCacheDir.listFiles()?.any { it.name.startsWith(modelFileName) } == true
        if (!cacheAlreadyExists && freeBytes < requiredBytes) {
            val msg = "空间不足：XNNPack 权重缓存需要至少 1.5 GB 可用空间，" +
                    "当前仅剩 ${freeBytes / 1_048_576} MB。请清理存储后重试。"
            Log.e(TAG, msg)
            onDone(msg)
            return
        }
        Log.d(
            TAG,
            "XNNPack cache dir: ${xnnpackCacheDir.absolutePath} (free: ${freeBytes / 1_048_576} MB)"
        )

        try {
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                maxNumTokens = maxTokens,
                cacheDir = xnnpackCacheDir.absolutePath,
            )
            val engine = Engine(engineConfig)
            engine.initialize()

            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble(),
                    )
                )
            )
            model.instance = LlmInstance(engine = engine, conversation = conversation)
            Log.d(TAG, "Model initialized: ${model.name}")
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            onDone(e.message ?: "Unknown error")
        }
    }

    fun resetConversation(
        model: Model,
        topK: Int = 40,
        topP: Float = 0.95f,
        temperature: Float = 0.8f,
    ) {
        val instance = model.instance as? LlmInstance ?: return
        try {
            instance.conversation.close()
            val newConversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble(),
                    )
                )
            )
            instance.conversation = newConversation
            Log.d(TAG, "Conversation reset for: ${model.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset conversation", e)
        }
    }

    fun runInference(
        model: Model,
        input: String,
        onToken: TokenListener,
        onError: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        val instance = model.instance as? LlmInstance
        if (instance == null) {
            onError("Model not initialized")
            return
        }

        coroutineScope.launch(Dispatchers.Default) {
            try {
                instance.conversation.sendMessageAsync(
                    Contents.of(listOf(Content.Text(input))),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            onToken(message.toString(), false)
                        }

                        override fun onDone() {
                            onToken("", true)
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                onToken("", true)
                            } else {
                                Log.e(TAG, "Inference error", throwable)
                                onError(throwable.message ?: "Inference error")
                            }
                        }
                    },
                    emptyMap()
                )
            } catch (e: Exception) {
                Log.e(TAG, "runInference error", e)
                onError(e.message ?: "Error")
            }
        }
    }

    fun stopGeneration(model: Model) {
        (model.instance as? LlmInstance)?.conversation?.cancelProcess()
    }

    fun cleanUp(model: Model) {
        val instance = model.instance as? LlmInstance ?: return
        try {
            instance.conversation.close()
        } catch (e: Exception) {
            Log.e(TAG, "close conversation", e)
        }
        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "close engine", e)
        }
        model.instance = null
        Log.d(TAG, "Cleaned up: ${model.name}")
    }
}
