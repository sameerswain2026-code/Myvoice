package com.myvoice.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myvoice.app.AppContainer
import com.myvoice.app.ui.detail.DetailScreen
import com.myvoice.app.ui.detail.detailViewModel
import com.myvoice.app.ui.docs.DocDetailScreen
import com.myvoice.app.ui.docs.DocsScreen
import com.myvoice.app.ui.docs.docDetailViewModel
import com.myvoice.app.ui.docs.docsViewModel
import com.myvoice.app.ui.history.HistoryScreen
import com.myvoice.app.ui.history.historyViewModel
import com.myvoice.app.ui.home.HomeScreen
import com.myvoice.app.ui.home.homeViewModel
import com.myvoice.app.ui.live.LiveScreen
import com.myvoice.app.ui.settings.SettingsScreen
import com.myvoice.app.ui.settings.settingsViewModel

const val ROUTE_HOME = "home"
const val ROUTE_LIVE = "live"
const val ROUTE_HISTORY = "history"
const val ROUTE_DOCS = "docs"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_DETAIL = "detail/{id}"
const val ROUTE_DOC_DETAIL = "doc/{id}"

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topDestinations = listOf(
        TopDestination(ROUTE_HOME, "Talk", Icons.Filled.Mic),
        TopDestination(ROUTE_LIVE, "Live", Icons.Filled.GraphicEq),
        TopDestination(ROUTE_HISTORY, "History", Icons.Outlined.History),
        TopDestination(ROUTE_DOCS, "Docs", Icons.Outlined.Description),
        TopDestination(ROUTE_SETTINGS, "Settings", Icons.Outlined.Settings)
    )
    val showBottomBar = currentRoute in topDestinations.map { it.route }

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
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) }
                        )
                    }
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
                    onOpenSettings = { navigateTopLevel(ROUTE_SETTINGS) },
                    onOpenThought = { navController.navigate("detail/$it") }
                )
            }
            composable(ROUTE_LIVE) {
                LiveScreen(engine = container.liveEngine)
            }
            composable(ROUTE_HISTORY) {
                HistoryScreen(
                    vm = viewModel(factory = historyViewModel(container)),
                    onOpenThought = { navController.navigate("detail/$it") }
                )
            }
            composable(ROUTE_DOCS) {
                DocsScreen(
                    vm = viewModel(factory = docsViewModel(container)),
                    onOpenDoc = { navController.navigate("doc/$it") }
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
            composable(ROUTE_DOC_DETAIL) { entry ->
                val docId = entry.arguments?.getString("id").orEmpty()
                DocDetailScreen(
                    vm = viewModel(
                        key = "doc-$docId",
                        factory = docDetailViewModel(container, docId)
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
