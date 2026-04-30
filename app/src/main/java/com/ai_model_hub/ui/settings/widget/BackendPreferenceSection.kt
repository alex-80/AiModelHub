package com.ai_model_hub.ui.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ai_model_hub.sdk.BackendPreference

@Composable
internal fun BackendPreferenceSection(
    selected: BackendPreference,
    onSelect: (BackendPreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,

            ) {
            Text(
                text = "Inference Backend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(3f)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(2f)) {
                BackendPreference.entries.forEachIndexed { index, pref ->
                    SegmentedButton(
                        modifier = Modifier.heightIn(min = 24.dp, max = 32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        selected = selected == pref,
                        onClick = { onSelect(pref) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BackendPreference.entries.size,
                        ),
                        label = {
                            Text(
                                text = when (pref) {
                                    BackendPreference.CPU -> "CPU"
                                    BackendPreference.GPU -> "GPU"
                                },
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Choose the preferred compute backend for model inference. " +
                    "Takes effect the next time a model is loaded. " +
                    "GPU is only used when the model declares GPU support.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun BackendPreferenceSectionPreview() {
    MaterialTheme {
        BackendPreferenceSection(
            selected = BackendPreference.CPU,
            onSelect = {},
        )
    }
}