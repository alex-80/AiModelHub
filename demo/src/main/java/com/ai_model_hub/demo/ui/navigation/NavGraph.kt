package com.ai_model_hub.demo.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import com.ai_model_hub.demo.ui.chat.ChatScreen
import com.ai_model_hub.demo.ui.home.HomeScreen
import com.ai_model_hub.demo.ui.translate.TranslateScreen

object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val TRANSLATE = "translate"
}

@Composable
fun AiHubDemoNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onChatClick = { navController.navigate(Routes.CHAT) },
                onTranslateClick = { navController.navigate(Routes.TRANSLATE) },
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TRANSLATE) {
            TranslateScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}
