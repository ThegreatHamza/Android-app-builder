package com.hamzaapps.sitecalc.ui

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.ui.screens.CalculatorScreen
import com.hamzaapps.sitecalc.ui.screens.HomeScreen
import com.hamzaapps.sitecalc.ui.screens.ProjectDetailScreen
import com.hamzaapps.sitecalc.ui.screens.ProjectsScreen
import com.hamzaapps.sitecalc.ui.screens.SettingsScreen

private enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    CALCULATORS("home", R.string.tab_calculators, Icons.Filled.Calculate),
    PROJECTS("projects", R.string.tab_projects, Icons.Filled.Folder),
}

object Routes {
    const val SETTINGS = "settings"
    fun calc(type: CalcType, itemId: Long? = null) = "calc/${type.name}" + (itemId?.let { "?item=$it" } ?: "")
    fun project(id: Long) = "project/$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteCalcRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            snackbar.showSnackbar(if (m.arg != null) resources.getString(m.text, m.arg) else resources.getString(m.text))
        }
    }
    LaunchedEffect(Unit) {
        vm.share.collect { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, null))
        }
    }

    val title = when {
        tab != null -> stringResource(R.string.app_name)
        route == Routes.SETTINGS -> stringResource(R.string.settings)
        route?.startsWith("calc/") == true ->
            CalcType.entries.firstOrNull { it.name == backStack?.arguments?.getString("type") }?.let { stringResource(it.title) } ?: ""
        else -> stringResource(R.string.project)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (tab == null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (tab != null) {
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (tab != null) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(nav, startDestination = Tab.CALCULATORS.route, modifier = Modifier.padding(padding)) {
            composable(Tab.CALCULATORS.route) { HomeScreen(onOpen = { nav.navigate(Routes.calc(it)) }) }
            composable(Tab.PROJECTS.route) { ProjectsScreen(vm, onOpen = { nav.navigate(Routes.project(it)) }) }
            composable(
                "calc/{type}?item={item}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("item") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val type = CalcType.valueOf(entry.arguments?.getString("type") ?: CalcType.CONCRETE.name)
                val item = entry.arguments?.getLong("item")?.takeIf { it > 0 }
                CalculatorScreen(vm, type, item, onSavedOpenProject = { pid ->
                    nav.navigate(Routes.project(pid)) { popUpTo(Tab.CALCULATORS.route) }
                })
            }
            composable("project/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ProjectDetailScreen(
                    vm,
                    projectId = entry.arguments?.getLong("id") ?: 0,
                    onEditItem = { type, itemId -> nav.navigate(Routes.calc(type, itemId)) },
                    onDeleted = { nav.popBackStack() },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
        }
    }
}
