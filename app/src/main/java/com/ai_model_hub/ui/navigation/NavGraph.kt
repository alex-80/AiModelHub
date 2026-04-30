package com.ai_model_hub.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ai_model_hub.ui.chat.ChatEmptyScreen
import com.ai_model_hub.ui.chat.ChatScreen
import com.ai_model_hub.ui.modelmanager.ModelManagerScreen
import com.ai_model_hub.ui.settings.SettingsScreen

object Routes {
    const val MODEL_MANAGER = "model_manager"
    const val CHAT_EMPTY = "chat"
    const val CHAT = "chat/{modelName}"
    const val SETTINGS = "settings"
    fun chat(modelName: String) = "chat/$modelName"
}

private fun routeToTabIndex(route: String?): Int? = when {
    route == Routes.MODEL_MANAGER -> 0
    route?.startsWith("chat") == true -> 1
    else -> null
}

@Composable
fun AiModelHubNavGraph() {
    val navController = rememberNavController()
    var currentChatModel by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isChatActive = navBackStackEntry?.destination?.route?.startsWith("chat") == true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                NavigationBarItem(
                    selected = !isChatActive,
                    onClick = {
                        navController.navigate(Routes.MODEL_MANAGER) {
                            popUpTo(Routes.MODEL_MANAGER) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Memory, contentDescription = "Models") },
                    label = { Text("Models") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                NavigationBarItem(
                    selected = isChatActive,
                    onClick = {
                        val model = currentChatModel
                        if (model != null) {
                            navController.navigate(Routes.chat(model)) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(Routes.CHAT_EMPTY) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.MODEL_MANAGER,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            enterTransition = {
                val fromIndex = routeToTabIndex(initialState.destination.route)
                val toIndex = routeToTabIndex(targetState.destination.route)
                if (fromIndex != null && toIndex != null) {
                    if (toIndex < fromIndex) slideInHorizontally { -it } else slideInHorizontally { it }
                } else {
                    slideInHorizontally { it }
                }
            },
            exitTransition = {
                val fromIndex = routeToTabIndex(initialState.destination.route)
                val toIndex = routeToTabIndex(targetState.destination.route)
                if (fromIndex != null && toIndex != null) {
                    if (toIndex < fromIndex) slideOutHorizontally { it } else slideOutHorizontally { -it }
                } else {
                    slideOutHorizontally { -it }
                }
            },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            composable(Routes.CHAT_EMPTY) {
                ChatEmptyScreen(
                    onModelSelected = { modelName ->
                        currentChatModel = modelName
                        navController.navigate(Routes.chat(modelName)) {
                            popUpTo(Routes.CHAT_EMPTY) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.MODEL_MANAGER) {
                ModelManagerScreen(
                    onOpenChat = { modelName ->
                        currentChatModel = modelName
                        navController.navigate(Routes.chat(modelName))
                    },
                    onOpenSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                )
            }
            composable(Routes.CHAT) { backStackEntry ->
                val modelName =
                    backStackEntry.arguments?.getString("modelName") ?: return@composable
                ChatScreen(
                    modelName = modelName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
