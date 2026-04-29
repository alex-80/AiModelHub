package com.ai_model_hub

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ai_model_hub.service.AiModelHubService
import com.ai_model_hub.ui.navigation.AiModelHubNavGraph
import com.ai_model_hub.ui.theme.AiManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // Keep AiModelHubService alive as a foreground service so that third-party
        // apps can always bind without triggering a background cold-start, which
        // OEM ROM managers (e.g. OPLUS OplusAppStartupManager) typically block.
        startForegroundService(Intent(this, AiModelHubService::class.java))
        setContent {
            AiManagerTheme {
                AiModelHubNavGraph()
            }
        }
    }
}
