package com.ai_model_hub.demo.ui.chat

import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.Model

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long? = null, // generation elapsed time, null while loading or for user messages
)

data class ChatUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedModel: Model? = null,
    val sessionId: String = "",
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String = "",
    val availableModels: List<Model> = emptyList(),
)
