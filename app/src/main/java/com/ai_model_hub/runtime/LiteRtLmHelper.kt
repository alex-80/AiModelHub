package com.ai_model_hub.runtime

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.ai_model_hub.data.getModelFilePath
import com.ai_model_hub.sdk.BackendPreference
import com.ai_model_hub.sdk.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.Role
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LiteRtLmHelper"

typealias TokenListener = (token: String, done: Boolean) -> Unit

@OptIn(ExperimentalApi::class)
object LiteRtLmHelper {

    internal val sessions: MutableList<LlmSession> = Collections.synchronizedList(mutableListOf())

    /** One EngineHolder per model name — engines are never loaded twice for the same model. */
    private val engineHolders: MutableMap<String, EngineHolder> =
        Collections.synchronizedMap(mutableMapOf())

    private fun initialize(
        context: Context,
        model: Model,
        backendPreference: BackendPreference = BackendPreference.CPU,
        enableSpeculativeDecoding: Boolean = false,
    ): Engine {
        val modelPath = model.getModelFilePath(context)
        Log.d(TAG, "Initializing model at: $modelPath")

        // Prefer external storage for the XNNPack cache — it has far more headroom than
        // the internal /data partition, which may have only tens of MB free.
        val xnnpackCacheDir = (context.getExternalFilesDir("xnnpack_cache") ?: context.cacheDir)
            .apply { mkdirs() }

        // XNNPack calls abort() (uncatchable SIGABRT) if it cannot write the cache file.
        // Pre-check space to give a friendly error instead of a hard crash.
        val freeBytes = StatFs(xnnpackCacheDir.absolutePath).availableBytes
        val requiredBytes = 1_000L * 1024 * 1024 // 1 GB conservative estimate
        val modelFileName = File(modelPath).name
        val cacheAlreadyExists =
            xnnpackCacheDir.listFiles()?.any { it.name.startsWith(modelFileName) } == true
        if (!cacheAlreadyExists && freeBytes < requiredBytes) {
            val msg = "空间不足：XNNPack 权重缓存需要至少 1 GB 可用空间，" +
                    "当前仅剩 ${freeBytes / 1_048_576} MB。请清理存储后重试。"
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        Log.d(
            TAG,
            "XNNPack cache dir: ${xnnpackCacheDir.absolutePath} (free: ${freeBytes / 1_048_576} MB)"
        )

        try {
            val useGpu = backendPreference == BackendPreference.GPU &&
                    BackendPreference.GPU in model.supportedBackends
            Log.d(TAG, "Using backend: ${if (useGpu) "GPU" else "CPU"} for model: ${model.name}")

            // Enable MTP via speculative decoding
            @OptIn(ExperimentalApi::class)
            ExperimentalFlags.enableSpeculativeDecoding = enableSpeculativeDecoding

            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                maxNumTokens = model.maxTokens,
                cacheDir = xnnpackCacheDir.absolutePath,
            )
            val engine = Engine(engineConfig)
            engine.initialize()
            return engine
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            throw e
        }
    }

    fun createSession(
        context: Context,
        model: Model,
        conversationConfig: ConversationConfig = ConversationConfig(
            samplerConfig = SamplerConfig(
                topK = 40,
                topP = 0.95,
                temperature = 0.8,
            )
        ),
        backendPreference: BackendPreference = BackendPreference.CPU,
        enableSpeculativeDecoding: Boolean = false,
    ): LlmSession {
        // One engine per model — create only if not yet loaded.
        val holder = engineHolders.getOrPut(model.name) {
            EngineHolder(
                engine = initialize(
                    context = context,
                    model = model,
                    backendPreference = backendPreference,
                    enableSpeculativeDecoding = enableSpeculativeDecoding
                ),
                model = model,
            )
        }

        // Conversation is created lazily on the first sendMessage call.
        val session = LlmSession(
            engineHolder = holder,
            id = UUID.randomUUID().toString(),
            model = model,
            conversationConfig = conversationConfig,
        )
        sessions.add(session)
        Log.d(TAG, "Session created: ${session.id} for model: ${model.name}")
        return session
    }

    fun sendMessage(
        session: LlmSession,
        input: Contents,
        userText: String,
        onToken: TokenListener,
        onError: (String) -> Unit,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            val holder = session.engineHolder
            try {
                holder.mutex.withLock {
                    // Switch to this session if a different session is currently active.
                    if (holder.activeSessionId != session.id) {
                        holder.activeConversation?.close()
                        val initialMessages = session.buildInitialMessages()
                        holder.activeConversation = holder.engine.createConversation(
                            session.conversationConfig.copy(
                                initialMessages = initialMessages,
                                systemInstruction = session.conversationConfig.systemInstruction
                            )
                        )
                        holder.activeSessionId = session.id
                        Log.d(TAG, "Switched active session to: ${session.id}")
                    }

                    val conversation = holder.activeConversation!!
                    val assistantSb = StringBuilder()

                    suspendCancellableCoroutine { cont ->
                        conversation.sendMessageAsync(
                            input,
                            object : MessageCallback {
                                override fun onMessage(message: Message) {
                                    val token = message.toString()
                                    assistantSb.append(token)
                                    onToken(token, false)
                                }

                                override fun onDone() {
                                    onToken("", true)
                                    cont.resume(Unit)
                                }

                                override fun onError(throwable: Throwable) {
                                    if (throwable is CancellationException) {
                                        onToken("", true)
                                        cont.resume(Unit)
                                    } else {
                                        Log.e(TAG, "Inference error", throwable)
                                        cont.resumeWithException(throwable)
                                    }
                                }
                            },
                            emptyMap()
                        )
                        // Cancel inference when the coroutine is cancelled (e.g. stopGeneration).
                        cont.invokeOnCancellation {
                            holder.activeConversation?.cancelProcess()
                        }
                    }

                    // Persist completed exchange to history for future context restoration.
                    if (assistantSb.isNotEmpty()) {
                        session.history.add(HistoryEntry(role = Role.USER, text = userText))
                        session.history.add(
                            HistoryEntry(
                                role = Role.MODEL,
                                text = assistantSb.toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage error", e)
                onError(e.message ?: "Error")
            }
        }
    }

    fun isSessionAlive(session: LlmSession): Boolean {
        // A virtual session is "alive" as long as it hasn't been cleaned up.
        return sessions.contains(session)
    }

    fun stopGeneration(session: LlmSession) {
        val holder = session.engineHolder
        if (holder.activeSessionId == session.id) {
            holder.activeConversation?.cancelProcess()
        }
    }

    fun resetSession(session: LlmSession) {
        val holder = session.engineHolder
        if (holder.activeSessionId == session.id) {
            holder.activeConversation?.close()
            holder.activeConversation = null
            holder.activeSessionId = null
        }
        session.history.clear()
        Log.d(TAG, "Session reset: ${session.id}")
    }

    fun cleanUp(session: LlmSession) {
        val holder = session.engineHolder
        if (holder.activeSessionId == session.id) {
            try {
                holder.activeConversation?.close()
            } catch (e: Exception) {
                Log.e(TAG, "close conversation", e)
            }
            holder.activeConversation = null
            holder.activeSessionId = null
        }
        sessions.remove(session)

        // Close the engine only when no other session is still using this holder.
        val holderStillInUse = sessions.any { it.engineHolder === holder }
        if (!holderStillInUse) {
            engineHolders.remove(holder.model.name)
            try {
                holder.engine.close()
            } catch (e: Exception) {
                Log.e(TAG, "close engine", e)
            }
        }
        Log.d(TAG, "Cleaned up session: ${session.id}")
    }
}
