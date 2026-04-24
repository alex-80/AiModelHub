package com.ai_model_hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ai_model_hub.ui.navigation.AiModelHubNavGraph
import com.ai_model_hub.ui.theme.AiManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiManagerTheme {
                AiModelHubNavGraph()
            }
        }
    }
}
