package com.alma.app.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alma.app.R
import com.alma.app.ui.addbook.AddBookScreen
import com.alma.app.ui.calendar.CalendarScreen
import com.alma.app.ui.detail.BookDetailScreen
import com.alma.app.ui.library.LibraryScreen
import com.alma.app.ui.search.SearchScreen
import com.alma.app.ui.settings.SettingsScreen
import com.alma.app.ui.stats.StatsScreen

private object Routes {
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ARG_BOOK_ID = "bookId"
    const val BOOK = "book/{$ARG_BOOK_ID}"
    const val ARG_TITLE = "title"
    const val ADD = "add?$ARG_TITLE={$ARG_TITLE}"

    fun book(id: Long) = "book/$id"
    fun add(title: String = "") = "add?$ARG_TITLE=${Uri.encode(title)}"
}

private enum class TopLevel(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    LIBRARY(Routes.LIBRARY, R.string.nav_library, Icons.AutoMirrored.Filled.MenuBook),
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search),
    CALENDAR(Routes.CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    STATS(Routes.STATS, R.string.nav_stats, Icons.Filled.BarChart),
}

@Composable
fun AlmaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Cada pantalla tiene su Scaffold; este solo añade la barra inferior y deja el resto de márgenes a ellas.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (TopLevel.entries.any { it.route == currentRoute }) {
                AlmaBottomBar(navController, currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onBookClick = { navController.navigate(Routes.book(it)) },
                    onAddClick = { navController.navigate(Routes.add()) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBookClick = { navController.navigate(Routes.book(it)) },
                    onAddManually = { navController.navigate(Routes.add(it)) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onBookClick = { navController.navigate(Routes.book(it)) })
            }
            composable(Routes.STATS) {
                StatsScreen(onBookClick = { navController.navigate(Routes.book(it)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = navController::popBackStack)
            }
            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument(Routes.ARG_BOOK_ID) { type = NavType.LongType }),
            ) {
                BookDetailScreen(onBack = navController::popBackStack)
            }
            composable(
                route = Routes.ADD,
                arguments = listOf(
                    navArgument(Routes.ARG_TITLE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                AddBookScreen(
                    onBack = navController::popBackStack,
                    onSaved = { id ->
                        navController.navigate(Routes.book(id)) {
                            popUpTo(Routes.ADD) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AlmaBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        TopLevel.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
}
