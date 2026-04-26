package com.ai_model_hub.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.sdk.ModelAllowlist
import com.ai_model_hub.runtime.LiteRtLmHelper
import com.ai_model_hub.sdk.Model
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var currentModelName: String? = null
    private var loadedModel: Model? = null

    fun initialize(modelName: String) {
        if (currentModelName == modelName && loadedModel?.instance != null) return
        currentModelName = modelName

        val modelSpec = ModelAllowlist.findByName(modelName) ?: run {
            _uiState.value = _uiState.value.copy(modelError = "Model not found: $modelName")
            return
        }
        val model = modelSpec.copy()
        loadedModel = model

        _uiState.value = _uiState.value.copy(isModelLoading = true, modelError = "")
        viewModelScope.launch(Dispatchers.Default) {
            LiteRtLmHelper.initialize(
                context = context,
                model = model,
                onDone = { error ->
                    if (error.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isModelLoading = false)
                    } else {
                        loadedModel = null
                        _uiState.value = _uiState.value.copy(
                            isModelLoading = false,
                            modelError = error,
                        )
                    }
                }
            )
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return
        val model = loadedModel ?: return

        val userMsg = ChatMessage(role = "user", content = userInput)
        val loadingMsg = ChatMessage(role = "assistant", content = "", isLoading = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            isGenerating = true,
        )

        val sb = StringBuilder()
        LiteRtLmHelper.runInference(
            model = model,
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
        loadedModel?.let { LiteRtLmHelper.stopGeneration(it) }
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearSession() {
        loadedModel?.let { LiteRtLmHelper.resetConversation(it) }
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        loadedModel?.let { LiteRtLmHelper.cleanUp(it) }
        loadedModel = null
    }
}
