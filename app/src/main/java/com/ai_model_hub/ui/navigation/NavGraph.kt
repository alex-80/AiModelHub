package com.ai_model_hub.ui.navigation

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import com.ai_model_hub.ui.modelmanager.ModelManagerScreen
import com.ai_model_hub.ui.settings.SettingsScreen

object Routes {
    const val MODEL_MANAGER = "model_manager"
    const val SETTINGS = "settings"
}

@Composable
fun AiModelHubNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MODEL_MANAGER) {
        composable(Routes.MODEL_MANAGER) {
            ModelManagerScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
