package com.myvoice.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myvoice.app.AppContainer
import com.myvoice.app.ui.detail.DetailScreen
import com.myvoice.app.ui.detail.detailViewModel
import com.myvoice.app.ui.history.HistoryScreen
import com.myvoice.app.ui.history.historyViewModel
import com.myvoice.app.ui.home.HomeScreen
import com.myvoice.app.ui.home.homeViewModel
import com.myvoice.app.ui.settings.SettingsScreen
import com.myvoice.app.ui.settings.settingsViewModel

const val ROUTE_HOME = "home"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_DETAIL = "detail/{id}"

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(ROUTE_HOME, ROUTE_HISTORY, ROUTE_SETTINGS)

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_HOME,
                        onClick = { navigateTopLevel(ROUTE_HOME) },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        label = { Text("Talk") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_HISTORY,
                        onClick = { navigateTopLevel(ROUTE_HISTORY) },
                        icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_SETTINGS,
                        onClick = { navigateTopLevel(ROUTE_SETTINGS) },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    vm = viewModel(factory = homeViewModel(container)),
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onOpenThought = { navController.navigate("detail/$it") }
                )
            }
            composable(ROUTE_HISTORY) {
                HistoryScreen(
                    vm = viewModel(factory = historyViewModel(container)),
                    onOpenThought = { navController.navigate("detail/$it") }
                )
            }
            composable(ROUTE_DETAIL) { entry ->
                val thoughtId = entry.arguments?.getString("id").orEmpty()
                DetailScreen(
                    vm = viewModel(
                        key = "detail-$thoughtId",
                        factory = detailViewModel(container, thoughtId)
                    ),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    vm = viewModel(factory = settingsViewModel(container))
                )
            }
        }
    }
}
