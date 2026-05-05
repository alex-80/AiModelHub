package com.ai_model_hub.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.ModelAllowlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemoUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedModel: String = MODELS.first(),
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val inputText: String = "",
    val response: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String = "",
)

val MODELS = ModelAllowlist.models.map { it.name }

class DemoViewModel(app: Application) : AndroidViewModel(app) {

    val client = AiHubClient()

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            client.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    errorMessage = if (state is ConnectionState.Error) state.message else "",
                    isModelLoaded = if (state !is ConnectionState.Connected) false
                    else _uiState.value.isModelLoaded,
                )
            }
        }
    }

    fun connect() = client.connect()

    fun disconnect() {
        client.disconnect()
        _uiState.value = _uiState.value.copy(isModelLoaded = false, response = "")
    }

    fun selectModel(name: String) {
        _uiState.value =
            _uiState.value.copy(selectedModel = name, isModelLoaded = false, response = "")
    }

    fun loadModel() {
        val model = _uiState.value.selectedModel
        _uiState.value = _uiState.value.copy(isLoadingModel = true, errorMessage = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.loadModel(model) { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingModel = false,
                        isModelLoaded = error.isEmpty(),
                        errorMessage = error
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingModel = false,
                    errorMessage = e.message ?: "Load failed"
                )
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.unloadModel(_uiState.value.selectedModel)
                _uiState.value = _uiState.value.copy(isModelLoaded = false, response = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Unload failed")
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val model = _uiState.value.selectedModel
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
                client.sendMessage(model, message).collect { token ->
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
        client.stopGeneration(_uiState.value.selectedModel)
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
