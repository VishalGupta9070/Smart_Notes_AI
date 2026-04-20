package com.example.smartnotesai.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartnotesai.data.repository.AuthRepository
import com.example.smartnotesai.ui.screens.home.TaskListScreen
import com.example.smartnotesai.ui.screens.login.LoginScreen
import com.example.smartnotesai.ui.screens.main.MainScreen

private object AppRoute {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val TASK_LIST = "taskList"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepository = remember(context) { AuthRepository(context) }
    val startDestination = if (authRepository.isLoggedIn()) AppRoute.MAIN else AppRoute.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoute.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.MAIN) {
                        popUpTo(AppRoute.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.MAIN) {
            MainScreen(
                onNavigateToTaskList = { date ->
                    navController.navigate("${AppRoute.TASK_LIST}/${Uri.encode(date)}")
                },
                onLogout = {
                    authRepository.logout()
                    navController.navigate(AppRoute.LOGIN) {
                        popUpTo(AppRoute.MAIN) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "${AppRoute.TASK_LIST}/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { entry ->
            val encodedDate = entry.arguments?.getString("date").orEmpty()
            TaskListScreen(
                date = Uri.decode(encodedDate),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
