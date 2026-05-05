package com.ai_model_hub.demo

import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai_model_hub.demo.ui.theme.AiHubDemoTheme
import com.ai_model_hub.sdk.functional.TranslateAvailableLanguage

class TranslateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiHubDemoTheme {
                TranslateScreen(onNavigateUp = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    vm: TranslateViewModel = viewModel(),
    onNavigateUp: () -> Unit = {},
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translation Demo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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

            // ── Model selection ──────────────────────────────────────
            ModelDropdown(
                label = "Model",
                options = TRANSLATE_MODELS,
                selected = state.selectedModel,
                onSelect = vm::selectModel,
            )

            // ── Language selection ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguageDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Source (auto)",
                    options = listOf("") + TranslateAvailableLanguage.all,
                    selected = state.sourceLanguage,
                    onSelect = vm::setSourceLanguage,
                    displayText = { it.ifEmpty { "Auto-detect" } },
                )
                LanguageDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Target",
                    options = TranslateAvailableLanguage.all,
                    selected = state.targetLanguage,
                    onSelect = vm::setTargetLanguage,
                    displayText = { it },
                )
            }

            // ── Input ────────────────────────────────────────────────
            OutlinedTextField(
                value = state.inputText,
                onValueChange = vm::updateInput,
                label = { Text("Text to translate") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 6,
            )

            // ── Action buttons ───────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::translate,
                    enabled = state.inputText.isNotBlank() && !state.isTranslating,
                ) { Text("Translate") }
                if (state.isTranslating) {
                    OutlinedButton(onClick = vm::stopTranslation) { Text("Stop") }
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterVertically),
                        strokeWidth = 2.dp
                    )
                }
            }

            // ── Result ───────────────────────────────────────────────
            if (state.result.isNotEmpty() || state.isTranslating) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = state.result.ifEmpty { "…" },
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // ── Error ────────────────────────────────────────────────
            if (state.errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    displayText: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayText(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
