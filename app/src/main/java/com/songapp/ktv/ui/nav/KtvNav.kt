package com.songapp.ktv.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.songapp.ktv.ui.components.AuroraBackground
import com.songapp.ktv.ui.home.HomeScreen
import com.songapp.ktv.ui.library.LibraryScreen
import com.songapp.ktv.ui.online.OnlineSearchScreen
import com.songapp.ktv.ui.player.PlayerScreen
import com.songapp.ktv.ui.queue.QueueScreen
import com.songapp.ktv.ui.settings.SettingsScreen
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet

private sealed class Tab(val route: String, val label: String) {
    data object Home : Tab("home", "开唱")
    data object Library : Tab("library", "曲库")
    data object Online : Tab("online", "歌源")
    data object Queue : Tab("queue", "点歌")
    data object Settings : Tab("settings", "我的")
}

@Composable
fun KtvRoot() {
    val nav = rememberNavController()
    var selected by rememberSaveable { mutableStateOf(0) }
    val tabs = remember { listOf(Tab.Home, Tab.Library, Tab.Online, Tab.Queue, Tab.Settings) }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.30f),
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = selected == i,
                            onClick = {
                                if (selected != i) {
                                    selected = i
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Home -> Icons.Filled.Home
                                        Tab.Library -> Icons.Filled.LibraryMusic
                                        Tab.Online -> Icons.Filled.Public
                                        Tab.Queue -> Icons.Filled.QueueMusic
                                        Tab.Settings -> Icons.Filled.Settings
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPink,
                                selectedTextColor = NeonPink,
                                indicatorColor = NeonViolet.copy(alpha = 0.18f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Tab.Home.route,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                composable(Tab.Home.route) {
                    HomeScreen(
                        onOpenLibrary = {
                            selected = tabs.indexOf(Tab.Library)
                            nav.navigate(Tab.Library.route) { launchSingleTop = true }
                        },
                        onOpenSources = {
                            selected = tabs.indexOf(Tab.Online)
                            nav.navigate(Tab.Online.route) { launchSingleTop = true }
                        },
                        onOpenQueue = {
                            selected = tabs.indexOf(Tab.Queue)
                            nav.navigate(Tab.Queue.route) { launchSingleTop = true }
                        },
                        onOpenPlayer = { nav.navigate("player") },
                        onOpenSettings = {
                            selected = tabs.indexOf(Tab.Settings)
                            nav.navigate(Tab.Settings.route) { launchSingleTop = true }
                        }
                    )
                }
                composable(Tab.Library.route) {
                    LibraryScreen(onOpenPlayer = { nav.navigate("player") })
                }
                composable(Tab.Online.route) {
                    OnlineSearchScreen(onOpenPlayer = { nav.navigate("player") })
                }
                composable(Tab.Queue.route) {
                    QueueScreen(onOpenPlayer = { nav.navigate("player") })
                }
                composable(Tab.Settings.route) { SettingsScreen() }
                composable("player") {
                    PlayerScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
