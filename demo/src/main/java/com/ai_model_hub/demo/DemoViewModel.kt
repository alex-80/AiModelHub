package com.ai_model_hub.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemoUiState(
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

class DemoViewModel(app: Application) : AndroidViewModel(app) {

    val client = AiHubClient()

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    init {
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
                    errorMessage = if (state is ConnectionState.Error) state.message else "",
                    isModelLoaded = if (state !is ConnectionState.Connected) false
                    else _uiState.value.isModelLoaded,
                    availableModels = availableModels,
                    selectedModel = if (state !is ConnectionState.Connected || availableModels.isEmpty()) "" else availableModels.first(),
                )
            }
        }
    }

    fun connect() = client.connect()

    fun disconnect() {
        client.disconnect()
        _uiState.value = _uiState.value.copy(isModelLoaded = false, sessionId = "", response = "")
    }

    fun selectModel(name: String) {
        _uiState.value =
            _uiState.value.copy(
                selectedModel = name,
                sessionId = "",
                isModelLoaded = false,
                response = ""
            )
    }

    fun createSession() {
        val model = _uiState.value.selectedModel
        _uiState.value = _uiState.value.copy(isLoadingModel = true, errorMessage = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionId = client.createSession(modelName = model)
                _uiState.value = _uiState.value.copy(
                    isLoadingModel = false,
                    isModelLoaded = true,
                    sessionId = sessionId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingModel = false,
                    isModelLoaded = false,
                    errorMessage = e.message ?: "Load failed"
                )
            }
        }
    }

    fun closeSession() {
        val sessionId = _uiState.value.sessionId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.closeSession(sessionId)
                _uiState.value =
                    _uiState.value.copy(isModelLoaded = false, sessionId = "", response = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Unload failed")
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val sessionId = _uiState.value.sessionId
        val message = _uiState.value.inputText.trim()
        if (message.isBlank()) return
        _uiState.value = _uiState.value.copy(
            inputText = "",
            response = "",
            isGenerating = true,
            errorMessage = ""
        )
        viewModelScope.launch {
            try {
                client.sendMessage(sessionId, message).collect { token ->
                    _uiState.value = _uiState.value.copy(response = _uiState.value.response + token)
                }
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = e.message ?: "Generation failed"
                )
            }
        }
    }

    fun stopGeneration() {
        client.stopGeneration(_uiState.value.sessionId)
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
