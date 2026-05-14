package com.ai_model_hub.demo.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    val client = AiHubClient()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
                    errorMessage = if (state is ConnectionState.Error) state.message else "",
                    isModelLoaded = if (state !is ConnectionState.Connected) false
                    else _uiState.value.isModelLoaded,
                    availableModels = availableModels,
                    selectedModel = if (state !is ConnectionState.Connected || availableModels.isEmpty()) null
                    else availableModels.first(),
                )
            }
        }
    }

    fun connect() = client.connect()

    fun selectAndLoad(model: Model) {
        selectModel(model)
        createSession()
    }

    fun disconnect() {
        client.disconnect()
        _uiState.value = _uiState.value.copy(
            isModelLoaded = false,
            sessionId = "",
            messages = emptyList(),
        )
    }

    fun selectModel(model: Model) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            sessionId = "",
            isModelLoaded = false,
            messages = emptyList(),
        )
    }

    fun createSession() {
        val model = _uiState.value.selectedModel ?: return
        _uiState.value = _uiState.value.copy(isLoadingModel = true, errorMessage = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionId = client.createSession(model = model)
                _uiState.value = _uiState.value.copy(
                    isLoadingModel = false,
                    isModelLoaded = true,
                    sessionId = sessionId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingModel = false,
                    isModelLoaded = false,
                    errorMessage = e.message ?: "Load failed",
                )
            }
        }
    }

    fun closeSession() {
        val sessionId = _uiState.value.sessionId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.closeSession(sessionId)
                _uiState.value = _uiState.value.copy(
                    isModelLoaded = false,
                    sessionId = "",
                    messages = emptyList(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Unload failed")
            }
        }
    }

    fun sendMessage(message: String) {
        val sessionId = _uiState.value.sessionId
        if (message.isBlank()) return

        val userMsg = ChatMessage(role = "user", content = message)
        val loadingMsg = ChatMessage(role = "assistant", content = "", isLoading = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            isGenerating = true,
            errorMessage = "",
        )

        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                client.sendMessage(sessionId, message).collect { token ->
                    sb.append(token)
                    val msgs = _uiState.value.messages.dropLast(1) +
                            ChatMessage(role = "assistant", content = sb.toString())
                    _uiState.value = _uiState.value.copy(messages = msgs)
                }
                val durationMs = System.currentTimeMillis() - startTime
                val msgs = _uiState.value.messages.dropLast(1) +
                        _uiState.value.messages.last().copy(durationMs = durationMs)
                _uiState.value = _uiState.value.copy(messages = msgs, isGenerating = false)
            } catch (e: Exception) {
                val durationMs = System.currentTimeMillis() - startTime
                val msgs = _uiState.value.messages.dropLast(1) +
                        ChatMessage(
                            role = "assistant",
                            content = e.message ?: "Generation failed",
                            isError = true,
                            durationMs = durationMs
                        )
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    isGenerating = false,
                    errorMessage = e.message ?: "Generation failed",
                )
            }
        }
    }

    fun stopGeneration() {
        client.stopGeneration(_uiState.value.sessionId)
        val msgs = _uiState.value.messages
        val updatedMsgs = if (msgs.isNotEmpty() && msgs.last().isLoading) {
            msgs.dropLast(1) + msgs.last().copy(isLoading = false)
        } else msgs
        _uiState.value = _uiState.value.copy(messages = updatedMsgs, isGenerating = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
        disconnect()
    }
}
