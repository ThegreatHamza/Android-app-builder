package com.hamzaapps.hifzpath.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.ui.screens.MushafScreen
import com.hamzaapps.hifzpath.ui.screens.ProgressScreen
import com.hamzaapps.hifzpath.ui.screens.SettingsScreen
import com.hamzaapps.hifzpath.ui.screens.SetupScreen
import com.hamzaapps.hifzpath.ui.screens.StudentsScreen
import com.hamzaapps.hifzpath.ui.screens.SurahsScreen
import com.hamzaapps.hifzpath.ui.screens.TodayScreen

private enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    TODAY("today", R.string.tab_today, Icons.Filled.Today),
    MUSHAF("mushaf", R.string.tab_mushaf, Icons.Filled.GridView),
    SURAHS("surahs", R.string.tab_surahs, Icons.AutoMirrored.Filled.List),
    PROGRESS("progress", R.string.tab_progress, Icons.Filled.Insights),
}

private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_STUDENTS = "students"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzRoot(viewModel: HifzViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            val text = if (msg.arg != null) resources.getString(msg.text, msg.arg) else resources.getString(msg.text)
            snackbar.showSnackbar(text)
        }
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val student = state.student
    if (student == null) {
        SetupScreen(onCreate = viewModel::createStudent)
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (route) {
                            ROUTE_SETTINGS -> stringResource(R.string.settings)
                            ROUTE_STUDENTS -> stringResource(R.string.students)
                            else -> stringResource(R.string.title_with_student, stringResource(currentTab?.label ?: R.string.app_name), student.name)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (currentTab == null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (currentTab != null) {
                        IconButton(onClick = { nav.navigate(ROUTE_STUDENTS) }) {
                            Icon(Icons.Filled.People, contentDescription = stringResource(R.string.students))
                        }
                        IconButton(onClick = { nav.navigate(ROUTE_SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding)) {
            composable(Tab.TODAY.route) { TodayScreen(state, viewModel) }
            composable(Tab.MUSHAF.route) { MushafScreen(state, viewModel) }
            composable(Tab.SURAHS.route) { SurahsScreen(state, viewModel) }
            composable(Tab.PROGRESS.route) {
                ProgressScreen(state, viewModel, onOpenSettings = { nav.navigate(ROUTE_SETTINGS) })
            }
            composable(ROUTE_SETTINGS) { SettingsScreen(state, viewModel) }
            composable(ROUTE_STUDENTS) {
                StudentsScreen(state, viewModel, onOpenSettings = { nav.navigate(ROUTE_SETTINGS) })
            }
        }
    }
}
