package com.ai_model_hub.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.data.AppRepository
import com.ai_model_hub.sdk.ModelAllowlist
import com.ai_model_hub.runtime.LiteRtLmHelper
import com.ai_model_hub.runtime.LlmSession
import com.ai_model_hub.runtime.createSession
import com.ai_model_hub.runtime.sendMessage
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.Model
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isModelLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val modelError: String = "",
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var session: LlmSession? = null
    private val loadedModel get() = session?.model
    private val currentModelName get() = loadedModel?.name

    fun initialize(modelName: String) {
        if (currentModelName == modelName && loadedModel != null) return

        _uiState.value = _uiState.value.copy(isModelLoading = true, modelError = "")
        viewModelScope.launch(Dispatchers.Default) {
            val backendPreference = appRepository.backendPreference.first()
            try {
                session = LiteRtLmHelper.createSession(
                    context = context,
                    modelName = modelName,
                    backendPreference = backendPreference,
                )
                _uiState.value = _uiState.value.copy(isModelLoading = false)
            } catch (e: Exception) {
                session = null
                _uiState.value = _uiState.value.copy(
                    isModelLoading = false,
                    modelError = e.message ?: "Unknown error while loading model",
                )
                return@launch
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return
        val activeSession = session ?: return

        val userMsg = ChatMessage(role = "user", content = userInput)
        val loadingMsg = ChatMessage(role = "assistant", content = "", isLoading = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            isGenerating = true,
        )

        val sb = StringBuilder()
        LiteRtLmHelper.sendMessage(
            session = activeSession,
            input = userInput,
            onToken = { token, done ->
                if (!done) {
                    sb.append(token)
                    val msgs = _uiState.value.messages.dropLast(1) +
                            ChatMessage(
                                role = "assistant",
                                content = sb.toString(),
                                isLoading = false
                            )
                    _uiState.value = _uiState.value.copy(messages = msgs)
                } else {
                    val msgs = _uiState.value.messages.dropLast(1) +
                            ChatMessage(
                                role = "assistant",
                                content = sb.toString(),
                                isLoading = false
                            )
                    _uiState.value = _uiState.value.copy(messages = msgs, isGenerating = false)
                }
            },
            onError = { error ->
                val msgs = _uiState.value.messages.dropLast(1) +
                        ChatMessage(role = "assistant", content = error, isError = true)
                _uiState.value = _uiState.value.copy(messages = msgs, isGenerating = false)
            },
            coroutineScope = viewModelScope,
        )
    }

    fun stopGeneration() {
        session?.let { LiteRtLmHelper.stopGeneration(it) }
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearSession() {
        session?.let { LiteRtLmHelper.resetSession(it) }
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        session?.let { LiteRtLmHelper.cleanUp(it) }
        session = null
    }
}
