package com.anothersshclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anothersshclient.AnotherSshClientApplication
import com.anothersshclient.data.ThemeMode
import com.anothersshclient.ui.screens.HostListScreen
import com.anothersshclient.ui.screens.SessionScreen

private object Routes {
    const val Hosts = "hosts"
    const val Sessions = "sessions"
}

@Composable
fun AnotherSshClientApp(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as AnotherSshClientApplication }
    val sessionManager = app.sessionManager
    val openSessions by sessionManager.sessions.collectAsStateWithLifecycle()

    val hostListViewModel: HostListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HostListViewModel(app) as T
            }
        },
    )

    fun goToSessions() {
        navController.navigate(Routes.Sessions) {
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.Hosts) {
        composable(Routes.Hosts) {
            HostListScreen(
                viewModel = hostListViewModel,
                openSessionCount = openSessions.size,
                themeMode = themeMode,
                onCycleThemeMode = onCycleThemeMode,
                onConnect = { profile ->
                    sessionManager.queueOpen(profile)
                    goToSessions()
                },
                onOpenSessions = { goToSessions() },
            )
        }
        composable(Routes.Sessions) {
            SessionScreen(
                sessionManager = sessionManager,
                loadPassword = { id -> hostListViewModel.passwordFor(id) },
                onLeaveToHosts = {
                    if (!navController.popBackStack(Routes.Hosts, inclusive = false)) {
                        navController.navigate(Routes.Hosts) {
                            popUpTo(Routes.Sessions) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}
