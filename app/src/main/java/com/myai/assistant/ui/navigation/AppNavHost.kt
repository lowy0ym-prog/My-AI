package com.myai.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myai.assistant.ui.chat.ChatScreen
import com.myai.assistant.ui.models.ModelManagerScreen
import com.myai.assistant.ui.settings.SettingsScreen
import com.myai.assistant.ui.sidebar.ConversationListScreen

object Routes {
    const val CHAT = "chat/{conversationId}"
    const val CONVERSATIONS = "conversations"
    const val MODELS = "models"
    const val SETTINGS = "settings"

    fun chat(conversationId: Long) = "chat/$conversationId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.CONVERSATIONS) {
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                onOpenConversation = { id -> navController.navigate(Routes.chat(id)) },
                onOpenModels = { navController.navigate(Routes.MODELS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CHAT) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("conversationId")?.toLongOrNull() ?: -1L
            ChatScreen(conversationId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.MODELS) {
            ModelManagerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
