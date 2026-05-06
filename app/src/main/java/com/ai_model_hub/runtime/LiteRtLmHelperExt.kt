package com.ai_model_hub.runtime

import android.content.Context
import android.util.Log
import com.ai_model_hub.sdk.BackendPreference
import com.ai_model_hub.sdk.ModelAllowlist
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope

private const val TAG = "LiteRtLmHelperExt"

fun LiteRtLmHelper.createSession(
    context: Context,
    modelName: String,
    conversationConfig: ConversationConfig = ConversationConfig(
        samplerConfig = SamplerConfig(
            topK = 40,
            topP = 0.95,
            temperature = 0.8,
        )
    ),
    backendPreference: BackendPreference = BackendPreference.CPU
): LlmSession {
    val model = ModelAllowlist.findByName(modelName) ?: run {
        throw IllegalArgumentException("Model not found: $modelName")
    }
    return createSession(
        context = context,
        model = model,
        conversationConfig = conversationConfig,
        backendPreference = backendPreference
    )
}

/** Convenience overload: accepts a plain [String] and stores it in session history. */
fun LiteRtLmHelper.sendMessage(
    session: LlmSession,
    input: String,
    onToken: TokenListener,
    onError: (String) -> Unit,
    coroutineScope: CoroutineScope,
) {
    sendMessage(
        session = session,
        input = Contents.of(listOf(Content.Text(input))),
        userText = input,
        onToken = onToken,
        onError = onError,
        coroutineScope = coroutineScope,
    )
}

fun LiteRtLmHelper.sendMessage(
    sessionId: String,
    input: String,
    onToken: TokenListener,
    onError: (String) -> Unit,
    coroutineScope: CoroutineScope,
) {
    val session = sessions.find { it.id == sessionId } ?: run {
        Log.e(TAG, "Session not found: $sessionId")
        onError("Session not found: $sessionId")
        return
    }
    sendMessage(
        session = session,
        input = Contents.of(listOf(Content.Text(input))),
        userText = input,
        onToken = onToken,
        onError = onError,
        coroutineScope = coroutineScope,
    )
}

fun LiteRtLmHelper.isSessionAlive(sessionId: String): Boolean {
    val session = sessions.find { it.id == sessionId } ?: run {
        Log.e(TAG, "Session not found: $sessionId")
        return false
    }
    return isSessionAlive(session)
}

fun LiteRtLmHelper.resetSession(sessionId: String) {
    val session = sessions.find { it.id == sessionId } ?: run {
        Log.e(TAG, "Session not found: $sessionId")
        return
    }
    resetSession(session)
}

fun LiteRtLmHelper.stopGeneration(sessionId: String) {
    val session = sessions.find { it.id == sessionId } ?: run {
        Log.e(TAG, "Session not found: $sessionId")
        return
    }
    stopGeneration(session)
}

fun LiteRtLmHelper.cleanUp(sessionId: String) {
    val session = sessions.find { it.id == sessionId } ?: run {
        Log.e(TAG, "Session not found: $sessionId")
        return
    }
    cleanUp(session)
}

fun LiteRtLmHelper.cleanUp() {
    // Snapshot to avoid ConcurrentModificationException as cleanUp(session) mutates the list.
    sessions.toList().forEach { cleanUp(it) }
}