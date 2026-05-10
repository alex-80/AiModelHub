package com.ai_model_hub.demo.ui.chat

import com.ai_model_hub.sdk.ConnectionState

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ChatUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedModel: String = "",
    val sessionId: String = "",
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String = "",
    val availableModels: List<String> = emptyList(),
)
