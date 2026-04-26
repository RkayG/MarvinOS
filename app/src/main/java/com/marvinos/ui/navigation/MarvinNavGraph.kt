package com.marvinos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marvinos.ui.ChatScreen
import com.marvinos.ui.ChatViewModel
import com.marvinos.ui.OnboardingScreen

@Composable
fun MarvinNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = viewModel,
                onNavigateToOnboarding = {
                    navController.navigate("onboarding")
                }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
