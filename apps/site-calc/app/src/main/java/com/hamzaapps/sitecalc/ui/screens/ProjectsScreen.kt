package com.hamzaapps.sitecalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.ui.MainViewModel
import com.hamzaapps.sitecalc.ui.components.ConfirmDialog
import com.hamzaapps.sitecalc.ui.components.NameDialog
import com.hamzaapps.sitecalc.util.Fmt

@Composable
fun ProjectsScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    var proPrompt by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        val list = projects
        when {
            list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            list.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.projects_empty_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(
                    stringResource(R.string.projects_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.testTag("project_list")) {
                items(list, key = { it.project.id }) { s ->
                    ListItem(
                        modifier = Modifier.clickable { onOpen(s.project.id) },
                        leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        headlineContent = { Text(s.project.name) },
                        supportingContent = { Text(pluralStringResource(R.plurals.items_count, s.itemCount, s.itemCount)) },
                        trailingContent = { if (s.totalCost > 0) Text(Fmt.money(s.totalCost, currency)) },
                    )
                    HorizontalDivider()
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { if (vm.canCreateProject()) creating = true else proPrompt = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_project)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_new_project"),
        )
    }

    if (creating) {
        NameDialog(
            title = stringResource(R.string.new_project),
            initial = "",
            label = stringResource(R.string.project_name),
            onSave = {
                vm.createProject(it, onOpen)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    if (proPrompt) {
        ConfirmDialog(
            title = stringResource(R.string.pro_title),
            message = stringResource(R.string.pro_projects_locked),
            confirmLabel = stringResource(R.string.ok),
            onConfirm = {},
            onDismiss = { proPrompt = false },
        )
    }
}
