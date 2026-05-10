package com.ai_model_hub.demo.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_model_hub.demo.ui.chat.widget.ChatBubble
import com.ai_model_hub.demo.ui.chat.widget.ChatInputBar
import com.ai_model_hub.demo.ui.theme.AiHubDemoTheme

@Composable
fun ChatActiveScreen(
    state: ChatUiState,
    onClearMessages: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ChatTopBar(
            modelName = state.selectedModel,
            onClearMessages = onClearMessages,
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            MessageList(messages = state.messages, listState = listState)
            ChatInputBar(
                input = inputText,
                onInputChange = { inputText = it },
                isGenerating = state.isGenerating,
                onSend = { onSend(inputText); inputText = "" },
                onStop = onStop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    modelName: String,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Powered by AiModelHub",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = onClearMessages) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear messages",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Start a conversation!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatActiveScreenPreview() {
    AiHubDemoTheme {
        ChatActiveScreen(
            state = ChatUiState(
                selectedModel = "gemma-4b",
                isModelLoaded = true,
                messages = listOf(
                    ChatMessage(role = "user", content = "Hello!"),
                    ChatMessage(role = "assistant", content = "Hi! How can I help you?"),
                ),
            ),
            onClearMessages = {},
            onSend = {},
            onStop = {},
            onBack = {},
        )
    }
}
