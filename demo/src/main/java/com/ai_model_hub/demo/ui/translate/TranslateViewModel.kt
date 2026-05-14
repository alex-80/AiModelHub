package com.ai_model_hub.demo.ui.translate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.Model
import com.ai_model_hub.sdk.functional.TranslationException
import com.ai_model_hub.sdk.functional.translateStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslateViewModel(app: Application) : AndroidViewModel(app) {

    private val client = AiHubClient()

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null

    init {
        client.connect()
        viewModelScope.launch {
            client.connectionState.collect { state ->
                val availableModels = if (state is ConnectionState.Connected) {
                    try {
                        client.getAvailableModels()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    errorMessage = if (state is ConnectionState.Error) state.message
                    else _uiState.value.errorMessage,
                    availableModels = availableModels,
                    selectedModel = if (state !is ConnectionState.Connected || availableModels.isEmpty()) null
                    else availableModels.first(),
                )
            }
        }
    }

    fun selectModel(model: Model) {
        _uiState.value = _uiState.value.copy(selectedModel = model, result = "")
    }

    fun setSourceLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(sourceLanguage = lang)
    }

    fun setTargetLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(targetLanguage = lang)
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun translate() {
        val state = _uiState.value
        val model = state.selectedModel ?: return
        if (state.inputText.isBlank() || state.isTranslating) return

        translateJob?.cancel()
        _uiState.value = state.copy(result = "", isTranslating = true, errorMessage = "")

        translateJob = viewModelScope.launch {
            try {
                translateStream(
                    modelId = model.modelId,
                    text = state.inputText,
                    targetLanguage = state.targetLanguage,
                    sourceLanguage = state.sourceLanguage,
                ).collect { token ->
                    _uiState.value = _uiState.value.copy(result = _uiState.value.result + token)
                }
                _uiState.value = _uiState.value.copy(isTranslating = false)
            } catch (e: TranslationException) {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    errorMessage = e.message ?: "Translation failed",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    errorMessage = e.message ?: "Translation failed",
                )
            }
        }
    }

    fun stopTranslation() {
        translateJob?.cancel()
        _uiState.value = _uiState.value.copy(isTranslating = false)
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
