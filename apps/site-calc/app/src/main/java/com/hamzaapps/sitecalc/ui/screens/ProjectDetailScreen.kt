package com.hamzaapps.sitecalc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.ads.BannerAd
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.ui.MainViewModel
import com.hamzaapps.sitecalc.ui.components.ConfirmDialog
import com.hamzaapps.sitecalc.ui.components.NameDialog
import com.hamzaapps.sitecalc.util.Fmt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    vm: MainViewModel,
    projectId: Long,
    onEditItem: (CalcType, Long) -> Unit,
    onDeleted: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val project by remember(projectId) { vm.project(projectId) }.collectAsStateWithLifecycle(null)
    val items by remember(projectId) { vm.items(projectId) }.collectAsStateWithLifecycle(emptyList())
    val currency by vm.currency.collectAsStateWithLifecycle()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val canAds by vm.canRequestAds.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var deletingItem by rememberSaveable { mutableStateOf<Long?>(null) }
    var proPrompt by rememberSaveable { mutableStateOf(false) }

    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(projectId, uri)
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.exportPdf(projectId, uri)
    }

    val p = project ?: return
    val fileBase = p.name.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40).ifBlank { "project" }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menu = false; renaming = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_project)) }, onClick = { menu = false; deleting = true })
                }
            }
        }
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { vm.shareProject(projectId) },
                enabled = items.isNotEmpty(),
                label = { Text(stringResource(R.string.share)) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            )
            AssistChip(
                onClick = { csv.launch("$fileBase.csv") },
                enabled = items.isNotEmpty(),
                label = { Text(stringResource(R.string.export_csv)) },
                leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = null) },
            )
            AssistChip(
                onClick = { if (isPro) pdf.launch("$fileBase.pdf") else proPrompt = true },
                enabled = items.isNotEmpty(),
                label = { Text(stringResource(R.string.export_pdf)) },
                leadingIcon = { Icon(if (isPro) Icons.Filled.PictureAsPdf else Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier.testTag("export_pdf"),
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.total_cost), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(
                    if (items.any { it.cost > 0 }) Fmt.money(items.sumOf { it.cost }, currency) else stringResource(R.string.no_prices),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("project_total"),
                )
            }
        }
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.project_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.weight(1f).testTag("item_list"), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items, key = { it.id }) { item ->
                val type = CalcType.entries.firstOrNull { it.name == item.type }
                ListItem(
                    modifier = Modifier.clickable(enabled = type != null) { type?.let { onEditItem(it, item.id) } },
                    headlineContent = { Text(item.title) },
                    overlineContent = { type?.let { Text(stringResource(it.title)) } },
                    supportingContent = {
                        Column {
                            Text(item.summary)
                            if (item.cost > 0) Text(Fmt.money(item.cost, currency), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { deletingItem = item.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                )
                HorizontalDivider()
            }
        }
        BannerAd(show = !isPro && canAds)
    }

    if (renaming) {
        NameDialog(stringResource(R.string.rename), p.name, stringResource(R.string.project_name), onSave = {
            vm.renameProject(p, it)
            renaming = false
        }, onDismiss = { renaming = false })
    }
    if (deleting) {
        ConfirmDialog(
            title = stringResource(R.string.delete_project),
            message = stringResource(R.string.confirm_delete_project, p.name),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deleteProject(p.id)
                onDeleted()
            },
            onDismiss = { deleting = false },
        )
    }
    deletingItem?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.confirm_delete_item),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.deleteItem(id) },
            onDismiss = { deletingItem = null },
        )
    }
    if (proPrompt) {
        ConfirmDialog(
            title = stringResource(R.string.pro_title),
            message = stringResource(R.string.pro_pdf_locked),
            confirmLabel = stringResource(R.string.go_pro),
            onConfirm = onOpenSettings,
            onDismiss = { proPrompt = false },
        )
    }
}
