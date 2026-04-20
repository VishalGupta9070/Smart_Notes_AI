package com.example.smartnotesai.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartnotesai.ui.screens.home.HomeScreen
import com.example.smartnotesai.ui.screens.notes.AddNotesScreen

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private object MainRoute {
    const val HOME = "home"
    const val ADD_NOTES = "add_notes"
}

@Composable
fun MainScreen(
    onNavigateToTaskList: (String) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val destinations = listOf(
        BottomDestination(
            route = MainRoute.HOME,
            label = "Home",
            icon = Icons.Default.Home
        ),
        BottomDestination(
            route = MainRoute.ADD_NOTES,
            label = "Add Notes",
            icon = Icons.Default.AddCircle
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(MainRoute.HOME) {
                HomeScreen(
                    onNavigateToTaskList = onNavigateToTaskList,
                    onLogout = onLogout
                )
            }
            composable(MainRoute.ADD_NOTES) {
                AddNotesScreen(onOpenHome = {
                    navController.navigate(MainRoute.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        }
    }
}
