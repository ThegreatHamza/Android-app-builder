package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.quran.Quran
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.domain.PlanItem
import com.hamzaapps.hifzpath.ui.HifzUiState
import com.hamzaapps.hifzpath.ui.HifzViewModel
import com.hamzaapps.hifzpath.ui.components.RateDialog
import com.hamzaapps.hifzpath.ui.components.SectionTitle
import com.hamzaapps.hifzpath.ui.components.pageSubtitle
import com.hamzaapps.hifzpath.ui.components.quartersLabel

private enum class Part { SABAQ, SABQI, MANZIL }

@Composable
fun TodayScreen(state: HifzUiState, vm: HifzViewModel) {
    val plan = state.plan ?: return
    val stats = state.stats ?: return
    var rating by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) } // part ordinal to page

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.testTag("today_list"),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        stringResource(R.string.today_progress, plan.doneItems, plan.totalItems),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.padding(4.dp))
                    LinearProgressIndicator(
                        progress = { if (plan.totalItems == 0) 0f else plan.doneItems.toFloat() / plan.totalItems },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(4.dp))
                        Text(pluralStringResource(R.plurals.streak_days, stats.streakDays, stats.streakDays))
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.memorized_of_total, stats.memorizedPages, Quran.PAGE_COUNT))
                    }
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.part_sabaq))
            Text(
                stringResource(R.string.part_sabaq_help, stringResource(quartersLabel(plan.newQuartersTarget))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plan.quranComplete && plan.sabaq.isEmpty()) {
            item { EmptyText(stringResource(R.string.quran_complete)) }
        }
        planItems(plan.sabaq, state, Part.SABAQ, onDo = { rating = Part.SABAQ.ordinal to it }, onUndo = vm::undoMemorized)

        item {
            SectionTitle(stringResource(R.string.part_sabqi))
            Text(
                stringResource(R.string.part_sabqi_help, state.student?.recentDays ?: 7),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plan.sabqi.isEmpty()) item { EmptyText(stringResource(R.string.empty_sabqi)) }
        planItems(plan.sabqi, state, Part.SABQI, onDo = { rating = Part.SABQI.ordinal to it }, onUndo = vm::undoReview)

        item {
            SectionTitle(stringResource(R.string.part_manzil))
            Text(
                stringResource(R.string.part_manzil_help, state.student?.cycleDays ?: 30),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plan.manzil.isEmpty()) item { EmptyText(stringResource(R.string.empty_manzil)) }
        planItems(plan.manzil, state, Part.MANZIL, onDo = { rating = Part.MANZIL.ordinal to it }, onUndo = vm::undoReview)
    }

    rating?.let { (partIndex, page) ->
        val part = Part.entries[partIndex]
        RateDialog(
            title = stringResource(R.string.page_n, page),
            onRate = { strength ->
                if (part == Part.SABAQ) vm.markMemorized(page, strength) else vm.review(page, strength)
                rating = null
            },
            onDismiss = { rating = null },
        )
    }
}

private fun LazyListScope.planItems(
    items: List<PlanItem>,
    state: HifzUiState,
    part: Part,
    onDo: (Int) -> Unit,
    onUndo: (Int) -> Unit,
) {
    items(items, key = { "${part.name}-${it.page}" }) { item ->
        val learning = state.pages[item.page]?.status == PageStatus.LEARNING
        ListItem(
            headlineContent = { Text(stringResource(R.string.page_n, item.page)) },
            supportingContent = {
                Column {
                    Text(pageSubtitle(item.page))
                    if (learning && !item.done) {
                        Text(stringResource(R.string.status_learning), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            leadingContent = {
                if (item.done) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.done), tint = MaterialTheme.colorScheme.primary)
                }
            },
            trailingContent = {
                if (item.done) {
                    TextButton(onClick = { onUndo(item.page) }) { Text(stringResource(R.string.undo)) }
                } else {
                    FilledTonalButton(
                        onClick = { onDo(item.page) },
                        modifier = Modifier.testTag("do_${part.name}_${item.page}"),
                    ) {
                        Text(stringResource(if (part == Part.SABAQ) R.string.action_memorized else R.string.action_revised))
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

