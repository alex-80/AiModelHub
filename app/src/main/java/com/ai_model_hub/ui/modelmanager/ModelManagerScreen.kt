package com.ai_model_hub.ui.modelmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ai_model_hub.data.ModelDownloadStatusType
import com.ai_model_hub.ui.modelmanager.widget.DownloadedCard
import com.ai_model_hub.ui.modelmanager.widget.DownloadingCard
import com.ai_model_hub.ui.modelmanager.widget.NotDownloadedCard

@Composable
fun ModelManagerScreen(
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ModelManagerViewModel = hiltViewModel(),
) {
    val modelStates by viewModel.modelUiStates.collectAsState()
    val enabledCount = modelStates.count { it.isEnabled }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ModelManagerTopBar(onOpenSettings = onOpenSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Installed Models",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$enabledCount Models Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            items(modelStates, key = { it.model.name }) { state ->
                ModelCard(
                    state = state,
                    onDownload = { viewModel.downloadModel(state.model) },
                    onCancel = { viewModel.cancelDownload(state.model) },
                    onDelete = { viewModel.deleteModel(state.model) },
                    onChat = { onOpenChat(state.model.name) },
                    onToggleEnabled = { viewModel.toggleEnabled(state.model) },
                )
            }
        }
    }
}

@Composable
private fun ModelManagerTopBar(onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "AI Model Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ModelCard(
    state: ModelUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit,
    onToggleEnabled: () -> Unit,
) {
    val versionTag = state.model.displayName.split(" ").lastOrNull() ?: state.model.version
    when (state.downloadStatus.status) {
        ModelDownloadStatusType.NOT_DOWNLOADED,
        ModelDownloadStatusType.FAILED,
        ModelDownloadStatusType.CANCELLED -> NotDownloadedCard(
            model = state.model,
            errorMessage = if (state.downloadStatus.status == ModelDownloadStatusType.FAILED)
                state.downloadStatus.errorMessage else null,
            onDownload = onDownload,
        )

        ModelDownloadStatusType.IN_PROGRESS -> DownloadingCard(
            model = state.model,
            status = state.downloadStatus,
            onCancel = onCancel,
        )

        ModelDownloadStatusType.SUCCEEDED -> DownloadedCard(
            state = state,
            versionTag = versionTag,
            onDelete = onDelete,
            onChat = onChat,
            onToggleEnabled = onToggleEnabled,
        )
    }
}