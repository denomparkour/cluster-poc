package com.mhp.cluster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mhp.cluster.ui.screens.HomeScreen
import com.mhp.cluster.ui.screens.SearchScreen
import com.mhp.cluster.ui.screens.ProfileScreen
import com.mhp.cluster.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Search.route) { SearchScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
} 