package com.ai_model_hub.demo.ui.chat

import com.ai_model_hub.sdk.ConnectionState

data class ChatUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedModel: String = "",
    val sessionId: String = "",
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val inputText: String = "",
    val response: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String = "",
    val availableModels: List<String> = emptyList(),
)
