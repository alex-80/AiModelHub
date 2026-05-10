package com.ai_model_hub.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ai_model_hub.demo.ui.navigation.AiHubDemoNavGraph
import com.ai_model_hub.demo.ui.theme.AiHubDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiHubDemoTheme {
                AiHubDemoNavGraph()
            }
        }
    }
}

