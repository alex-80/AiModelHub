package com.ai_model_hub.demo.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    if (state.isModelLoaded) {
        ChatActiveScreen(
            state = state,
            onClearMessages = vm::clearMessages,
            onSend = vm::sendMessage,
            onStop = vm::stopGeneration,
            onBack = onBack,
        )
    } else {
        ChatSetupScreen(
            state = state,
            onSelectModel = vm::selectAndLoad,
            onRetry = vm::connect,
            onBack = onBack,
        )
    }
}
