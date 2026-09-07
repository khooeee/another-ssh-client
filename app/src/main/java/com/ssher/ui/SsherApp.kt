package com.ssher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ssher.data.HostProfile
import com.ssher.ui.screens.HostListScreen
import com.ssher.ui.screens.SessionScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private object Routes {
    const val Hosts = "hosts"
    const val Session = "session/{name}/{host}/{port}/{username}"

    fun session(profile: HostProfile): String {
        val enc = { value: String -> URLEncoder.encode(value, StandardCharsets.UTF_8.name()) }
        return "session/${enc(profile.name)}/${enc(profile.host)}/${profile.port}/${enc(profile.username)}"
    }
}

@Composable
fun SsherApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as android.app.Application }
    val hostListViewModel: HostListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HostListViewModel(app) as T
            }
        },
    )

    NavHost(navController = navController, startDestination = Routes.Hosts) {
        composable(Routes.Hosts) {
            HostListScreen(
                viewModel = hostListViewModel,
                onConnect = { profile ->
                    navController.navigate(Routes.session(profile))
                },
            )
        }
        composable(
            route = Routes.Session,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("host") { type = NavType.StringType },
                navArgument("port") { type = NavType.IntType },
                navArgument("username") { type = NavType.StringType },
            ),
        ) { entry ->
            val decode = { key: String ->
                URLDecoder.decode(entry.arguments?.getString(key).orEmpty(), StandardCharsets.UTF_8.name())
            }
            SessionScreen(
                name = decode("name"),
                host = decode("host"),
                port = entry.arguments?.getInt("port") ?: 22,
                username = decode("username"),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
