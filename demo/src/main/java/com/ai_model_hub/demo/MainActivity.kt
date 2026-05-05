package com.ai_model_hub.demo

import android.content.Intent
import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai_model_hub.sdk.ConnectionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DemoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(vm: DemoViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AiModelHub Demo") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Connection ──────────────────────────────────────────
            ConnectionCard(state, onConnect = vm::connect, onDisconnect = vm::disconnect)

            // ── Model selection & load ───────────────────────────────
            if (state.connectionState is ConnectionState.Connected) {
                ModelCard(
                    state,
                    onSelectModel = vm::selectModel,
                    onLoad = vm::loadModel,
                    onUnload = vm::unloadModel
                )
            }

            // ── Chat ─────────────────────────────────────────────────
            if (state.isModelLoaded) {
                ChatCard(
                    state,
                    onInputChange = vm::updateInput,
                    onSend = vm::sendMessage,
                    onStop = vm::stopGeneration
                )
            }

            // ── Error ────────────────────────────────────────────────
            if (state.errorMessage.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Feature navigation ────────────────────────────────────
            HorizontalDivider()
            val context = LocalContext.current
            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, TranslateActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Translation Demo")
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectionCard(state: DemoUiState, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Service Connection", style = MaterialTheme.typography.titleMedium)
            val (statusText, statusColor) = when (state.connectionState) {
                ConnectionState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.outline
                ConnectionState.Connecting -> "Connecting…" to MaterialTheme.colorScheme.tertiary
                is ConnectionState.Connected -> "Connected ✓" to MaterialTheme.colorScheme.primary
                is ConnectionState.Error -> "Error" to MaterialTheme.colorScheme.error
            }
            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnect,
                    enabled = state.connectionState is ConnectionState.Disconnected || state.connectionState is ConnectionState.Error,
                ) { Text("Connect") }
                OutlinedButton(
                    onClick = onDisconnect,
                    enabled = state.connectionState !is ConnectionState.Disconnected,
                ) { Text("Disconnect") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelCard(
    state: DemoUiState,
    onSelectModel: (String) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Model", style = MaterialTheme.typography.titleMedium)

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = state.selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = { onSelectModel(model); expanded = false },
                        )
                    }
                }
            }

            if (state.isModelLoaded) {
                Text(
                    "Model loaded ✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.isLoadingModel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Loading model (this may take a while)…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLoad, enabled = !state.isModelLoaded && !state.isLoadingModel) {
                    Text("Load Model")
                }
                OutlinedButton(onClick = onUnload, enabled = state.isModelLoaded) {
                    Text("Unload")
                }
            }
        }
    }
}

@Composable
private fun ChatCard(
    state: DemoUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Chat", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInputChange,
                label = { Text("Your message") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                enabled = !state.isGenerating,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSend,
                    enabled = state.inputText.isNotBlank() && !state.isGenerating
                ) {
                    Text("Send")
                }
                if (state.isGenerating) {
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
            }
            if (state.response.isNotEmpty() || state.isGenerating) {
                HorizontalDivider()
                Text("Response:", style = MaterialTheme.typography.labelMedium)
                if (state.isGenerating && state.response.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = state.response,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
