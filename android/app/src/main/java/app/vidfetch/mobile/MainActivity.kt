package app.vidfetch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vidfetch.mobile.data.ThemeMode
import app.vidfetch.mobile.ui.DownloadsScreen
import app.vidfetch.mobile.ui.HistoryScreen
import app.vidfetch.mobile.ui.HomeScreen
import app.vidfetch.mobile.ui.SettingsScreen
import app.vidfetch.mobile.ui.SettingsViewModel
import app.vidfetch.mobile.ui.theme.VidFetchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VidFetchRoot()
        }
    }
}

data class Dest(val route: String, val label: String, val icon: ImageVector)

val destinations = listOf(
    Dest("home", "Главная", Icons.Filled.Home),
    Dest("downloads", "Загрузки", Icons.Filled.Download),
    Dest("history", "История", Icons.Filled.History),
    Dest("settings", "Настройки", Icons.Filled.Settings),
)

@Composable
fun VidFetchBottomBar(current: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        destinations.forEach { d ->
            NavigationBarItem(
                selected = current == d.route,
                onClick = { onSelect(d.route) },
                icon = { Icon(d.icon, contentDescription = d.label) },
                label = { Text(d.label) },
            )
        }
    }
}

@Composable
fun VidFetchRoot(vm: SettingsViewModel = viewModel()) {
    val themeMode by vm.theme.collectAsState()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    VidFetchTheme(darkTheme = dark) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination?.route

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    VidFetchBottomBar(current) { route ->
                        if (current != route) {
                            nav.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                            }
                        }
                    }
                },
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    modifier = Modifier.padding(inner),
                ) {
                    composable("home") { HomeScreen() }
                    composable("downloads") { DownloadsScreen() }
                    composable("history") { HistoryScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}
