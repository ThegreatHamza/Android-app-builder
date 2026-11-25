package com.hamzaapps.hifzpath.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.quran.Quran
import com.hamzaapps.hifzpath.domain.Strength

val QUARTER_OPTIONS = listOf(1, 2, 4, 8)
val CYCLE_OPTIONS = listOf(7, 14, 20, 30, 40, 60)

@StringRes
fun quartersLabel(quarters: Int): Int = when (quarters) {
    1 -> R.string.amount_quarter
    2 -> R.string.amount_half
    3 -> R.string.amount_three_quarters
    4 -> R.string.amount_one
    8 -> R.string.amount_two
    else -> R.string.amount_one
}

@StringRes
fun strengthLabel(strength: Strength): Int = when (strength) {
    Strength.WEAK -> R.string.strength_weak
    Strength.GOOD -> R.string.strength_good
    Strength.STRONG -> R.string.strength_strong
}

/** "An-Naba · Juz 30" style subtitle for a page. */
@Composable
fun pageSubtitle(page: Int): String {
    val names = Quran.surahsOnPage(page).joinToString(", ") { it.transliteration }
    return stringResource(R.string.page_subtitle, names, Quran.juzOfPage(page))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Asks how well a page was recited. */
@Composable
fun RateDialog(title: String, onRate: (Strength) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.rate_question))
                Strength.entries.reversed().forEach { s ->
                    OutlinedButton(onClick = { onRate(s) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(strengthLabel(s)))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Editor for direction, daily amount and revision cycle; used by setup and settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEditor(
    direction: Int,
    quarters: Int,
    cycleDays: Int,
    onDirection: (Int) -> Unit,
    onQuarters: (Int) -> Unit,
    onCycle: (Int) -> Unit,
) {
    Column {
        Text(stringResource(R.string.plan_direction), style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            listOf(0 to R.string.direction_start, 1 to R.string.direction_end).forEach { (value, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = direction == value, onClick = { onDirection(value) }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = direction == value, onClick = null)
                    Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Text(
            stringResource(R.string.plan_daily_new),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUARTER_OPTIONS.forEach { q ->
                FilterChip(selected = q == quarters, onClick = { onQuarters(q) }, label = { Text(stringResource(quartersLabel(q))) })
            }
        }
        Text(
            stringResource(R.string.plan_cycle),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.plan_cycle_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CYCLE_OPTIONS.forEach { d ->
                FilterChip(selected = d == cycleDays, onClick = { onCycle(d) }, label = { Text(stringResource(R.string.days_n, d)) })
            }
        }
    }
}
