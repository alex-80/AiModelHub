package com.ai_model_hub.ui.navigation

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
import com.ai_model_hub.ui.chat.ChatScreen
import com.ai_model_hub.ui.modelmanager.ModelManagerScreen

object Routes {
    const val MODEL_MANAGER = "model_manager"
    const val CHAT = "chat/{modelName}"
    fun chat(modelName: String) = "chat/$modelName"
}

@Composable
fun AiModelHubNavGraph() {
    val navController = rememberNavController()
    var currentChatModel by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isChatActive = navBackStackEntry?.destination?.route?.startsWith("chat/") == true

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
                        currentChatModel?.let { model ->
                            navController.navigate(Routes.chat(model)) {
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
                    enabled = currentChatModel != null,
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
        ) {
            composable(Routes.MODEL_MANAGER) {
                ModelManagerScreen(
                    onOpenChat = { modelName ->
                        currentChatModel = modelName
                        navController.navigate(Routes.chat(modelName))
                    }
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
        }
    }
}
