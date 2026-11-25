package com.hamzaapps.sitecalc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.ads.BannerAd
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.domain.Calculators
import com.hamzaapps.sitecalc.domain.materials
import com.hamzaapps.sitecalc.ui.CalcOutcome
import com.hamzaapps.sitecalc.ui.CalculatorViewModel
import com.hamzaapps.sitecalc.ui.MainViewModel
import com.hamzaapps.sitecalc.ui.components.MAX_NAME_LENGTH
import com.hamzaapps.sitecalc.ui.components.SectionTitle
import com.hamzaapps.sitecalc.util.Fmt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(vm: MainViewModel, type: CalcType, itemId: Long?, onSavedOpenProject: (Long) -> Unit) {
    val calc: CalculatorViewModel = viewModel(key = "calc-${type.name}-${itemId ?: 0}") {
        CalculatorViewModel(vm.c, type, itemId)
    }
    val spec = Calculators.spec(type)
    val inputs by calc.inputs.collectAsStateWithLifecycle()
    val options by calc.options.collectAsStateWithLifecycle()
    val prices by calc.priceText.collectAsStateWithLifecycle()
    val outcome by calc.outcome.collectAsStateWithLifecycle()
    val editing by calc.editing.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val canAds by vm.canRequestAds.collectAsStateWithLifecycle()
    val launches by vm.launchCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPrices by rememberSaveable { mutableStateOf(false) }
    var showSave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(calc) {
        calc.saved.collect { pid ->
            context.findActivity()?.let { vm.c.ads.maybeShowInterstitial(it, isPro, launches) }
            onSavedOpenProject(pid)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .testTag("calc_screen"),
        ) {
            Text(
                stringResource(type.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            spec.options.forEach { opt ->
                Text(stringResource(opt.label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    opt.choices.forEach { choice ->
                        FilterChip(
                            selected = options[opt.key] == choice.value,
                            onClick = { calc.setOption(opt.key, choice.value) },
                            label = { Text(stringResource(choice.label, Fmt.number(choice.value))) },
                        )
                    }
                }
            }
            SectionTitle(stringResource(R.string.dimensions))
            val invalidKey = (outcome as? CalcOutcome.Invalid)?.fieldKey
            spec.fields(options).forEach { f ->
                OutlinedTextField(
                    value = inputs[f.key] ?: "",
                    onValueChange = { calc.setInput(f.key, it) },
                    label = { Text(stringResource(f.label)) },
                    suffix = { Text(stringResource(f.unit.label)) },
                    isError = invalidKey == f.key && (inputs[f.key]?.isNotBlank() == true || f.default != null),
                    supportingText = if (invalidKey == f.key) {
                        { Text(stringResource(R.string.error_invalid_number)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("input_${f.key}"),
                )
            }

            // Optional prices
            TextButton(onClick = { showPrices = !showPrices }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(if (showPrices) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                Text(stringResource(R.string.prices_optional), Modifier.padding(start = 8.dp))
            }
            if (showPrices) {
                Text(stringResource(R.string.prices_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                type.materials().forEach { m ->
                    OutlinedTextField(
                        value = prices[m] ?: "",
                        onValueChange = { calc.setPrice(m, it) },
                        label = { Text(stringResource(R.string.price_per, stringResource(m.label))) },
                        prefix = if (currency.isNotBlank()) {
                            { Text("$currency ") }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("price_${m.name}"),
                    )
                }
            }

            SectionTitle(stringResource(R.string.results))
            when (val o = outcome) {
                CalcOutcome.Empty -> Text(stringResource(R.string.results_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                is CalcOutcome.Invalid -> Text(stringResource(R.string.results_invalid), color = MaterialTheme.colorScheme.error)
                is CalcOutcome.Ok -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("results_card"),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        o.result.lines.forEach { line ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(line.label),
                                    Modifier.weight(1f),
                                    fontWeight = if (line.key) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(
                                    "${Fmt.number(line.shownValue, line.whole)} ${stringResource(line.unit.label)}".trim(),
                                    fontWeight = if (line.key) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                        if (o.result.cost > 0) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.estimated_cost), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Text(Fmt.money(o.result.cost, currency), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.results_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { showSave = true },
                enabled = outcome is CalcOutcome.Ok,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("save_calc"),
            ) { Text(stringResource(if (editing != null) R.string.update_in_project else R.string.save_to_project)) }
        }
        BannerAd(show = !isPro && canAds)
    }

    if (showSave) {
        SaveDialog(
            vm = vm,
            defaultTitle = editing?.title ?: stringResource(type.title),
            preselectProject = editing?.projectId,
            onSave = { projectId, newName, title ->
                calc.save(projectId, newName, title)
                showSave = false
            },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun SaveDialog(
    vm: MainViewModel,
    defaultTitle: String,
    preselectProject: Long?,
    onSave: (projectId: Long?, newName: String?, title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val list = projects.orEmpty()
    var title by rememberSaveable { mutableStateOf(defaultTitle) }
    var selected by rememberSaveable { mutableLongStateOf(preselectProject ?: list.firstOrNull()?.project?.id ?: NEW_PROJECT) }
    var newName by rememberSaveable { mutableStateOf("") }
    val canCreate = vm.canCreateProject()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_to_project)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.item_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("save_title"),
                )
                Text(stringResource(R.string.choose_project), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Column(Modifier.selectableGroup()) {
                    list.forEach { p ->
                        RadioRow(p.project.name, selected == p.project.id) { selected = p.project.id }
                    }
                    if (canCreate) {
                        RadioRow(stringResource(R.string.new_project), selected == NEW_PROJECT, Modifier.testTag("radio_new_project")) {
                            selected = NEW_PROJECT
                        }
                    } else {
                        Text(
                            stringResource(R.string.pro_projects_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (selected == NEW_PROJECT && canCreate) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(MAX_NAME_LENGTH) },
                        label = { Text(stringResource(R.string.project_name)) },
                        placeholder = { Text(stringResource(R.string.project_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_project_name"),
                    )
                }
            }
        },
        confirmButton = {
            val creatingNew = selected == NEW_PROJECT
            TextButton(
                onClick = { onSave(if (creatingNew) null else selected, if (creatingNew) newName.trim() else null, title) },
                enabled = title.isNotBlank() && (!creatingNew || (canCreate && newName.isNotBlank())),
                modifier = Modifier.testTag("confirm_save"),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val NEW_PROJECT = -1L

@Composable
private fun RadioRow(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}
