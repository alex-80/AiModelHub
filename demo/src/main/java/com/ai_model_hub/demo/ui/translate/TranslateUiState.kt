package com.ai_model_hub.demo.ui.translate

import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.Model
import com.ai_model_hub.sdk.functional.TranslateAvailableLanguage

data class TranslateUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedModel: Model? = null,
    val sourceLanguage: String = "",
    val targetLanguage: String = TranslateAvailableLanguage.ENGLISH,
    val inputText: String = "",
    val result: String = "",
    val isTranslating: Boolean = false,
    val errorMessage: String = "",
    val availableModels: List<Model> = emptyList(),
)
