package com.ai_model_hub.sdk.functional

import android.util.Log
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

object TranslateAvailableLanguage {
    const val ARABIC = "Arabic"
    const val CHINESE = "Chinese"
    const val CZECH = "Czech"
    const val DANISH = "Danish"
    const val DUTCH = "Dutch"
    const val ENGLISH = "English"
    const val FINNISH = "Finnish"
    const val FRENCH = "French"
    const val GERMAN = "German"
    const val HINDI = "Hindi"
    const val HUNGARIAN = "Hungarian"
    const val INDONESIAN = "Indonesian"
    const val ITALIAN = "Italian"
    const val JAPANESE = "Japanese"
    const val KOREAN = "Korean"
    const val NORWEGIAN = "Norwegian"
    const val POLISH = "Polish"
    const val PORTUGUESE = "Portuguese"
    const val ROMANIAN = "Romanian"
    const val RUSSIAN = "Russian"
    const val SPANISH = "Spanish"
    const val SWEDISH = "Swedish"
    const val THAI = "Thai"
    const val TURKISH = "Turkish"
    const val UKRAINIAN = "Ukrainian"
    const val VIETNAMESE = "Vietnamese"

    /** All languages in display order. */
    val all: List<String> = listOf(
        ARABIC, CHINESE, CZECH, DANISH, DUTCH, ENGLISH, FINNISH, FRENCH, GERMAN,
        HINDI, HUNGARIAN, INDONESIAN, ITALIAN, JAPANESE, KOREAN, NORWEGIAN, POLISH,
        PORTUGUESE, ROMANIAN, RUSSIAN, SPANISH, SWEDISH, THAI, TURKISH, UKRAINIAN,
        VIETNAMESE,
    )
}

private const val DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L

enum class TranslationErrorCause {
    CONNECTION_TIMEOUT,
    MODEL_LOAD_FAILED,
    GENERATION_FAILED,
}

class TranslationException(message: String, val causeType: TranslationErrorCause) :
    Exception(message)

private const val TAG = "TranslateFunction"

/**
 * Translate [text] to [targetLanguage] using [modelName] and emit incremental tokens.
 *
 * @param modelName          Name of the model to use (e.g. "Gemma 4 E2B").
 * @param text               The text to translate.
 * @param targetLanguage     Target language name (use [TranslateAvailableLanguage] constants).
 * @param sourceLanguage     Source language name. Leave empty to let the model auto-detect.
 * @param connectionTimeoutMs How long to wait for the service to connect before failing.
 *
 * @throws TranslationException if connecting, loading, or generating fails.
 */
fun translateStream(
    modelName: String,
    text: String,
    targetLanguage: String,
    sourceLanguage: String = "",
    connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS,
): Flow<String> = flow {
    val client = AiHubClient().also {
        it.connect()
    }
    // 1. Wait for service connection
    val connected = withTimeoutOrNull(connectionTimeoutMs) {
        client.connectionState.first { it is ConnectionState.Connected }
    } ?: throw TranslationException(
        "Could not connect to AiModelHub service within ${connectionTimeoutMs}ms. " +
                "Is the AiModelHub app installed and a model enabled?",
        TranslationErrorCause.CONNECTION_TIMEOUT,
    )
    check(connected is ConnectionState.Connected)

    // 2. Load model and get session ID
    val sessionId =  client.createSession(modelName)
    Log.d(TAG, "Model \"$modelName\" loaded with session ID: $sessionId")

    // 3. Build prompt and stream tokens
    val prompt = buildPrompt(text, targetLanguage, sourceLanguage)
    Log.d(TAG, "Sending prompt to model \"$modelName\" (session: $sessionId): ${prompt.take(100)}")
    client.sendMessage(sessionId, prompt).collect { token -> emit(token) }

    // 4. close session after generation completes
    Log.d(TAG, "Translation completed for session $sessionId, closing session.")
    client.closeSession(sessionId)
}

/**
 * Translate [text] to [targetLanguage] and return the complete translation.
 *
 * Convenience wrapper around [translateStream] that collects all tokens.
 *
 * @throws TranslationException on any failure.
 */
suspend fun translate(
    modelName: String,
    text: String,
    targetLanguage: String,
    sourceLanguage: String = "",
    connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS,
): String {
    val result = StringBuilder()
    translateStream(modelName, text, targetLanguage, sourceLanguage, connectionTimeoutMs)
        .collect { token -> result.append(token) }
    return result.toString().trim()
}

private fun buildPrompt(text: String, targetLanguage: String, sourceLanguage: String): String =
    buildString {
        append("Translate the following text")
        if (sourceLanguage.isNotBlank()) append(" from $sourceLanguage")
        append(" to $targetLanguage")
        appendLine(". Output only the translation, nothing else.")
        appendLine()
        append(text)
    }


