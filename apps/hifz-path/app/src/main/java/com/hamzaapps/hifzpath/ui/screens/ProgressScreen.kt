package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.quran.Quran
import com.hamzaapps.hifzpath.ui.HifzUiState
import com.hamzaapps.hifzpath.ui.HifzViewModel
import com.hamzaapps.hifzpath.ui.components.SectionTitle
import com.hamzaapps.hifzpath.util.DayClock

@Composable
fun ProgressScreen(state: HifzUiState, vm: HifzViewModel, onOpenSettings: () -> Unit) {
    val stats = state.stats ?: return
    val isPro by vm.isPro.collectAsStateWithLifecycle()

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("progress_screen"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_memorized), stats.memorizedPages.toString(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_percent), stringResource(R.string.percent_value, stats.percent), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_streak), stats.streakDays.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_juz), "${stats.juzComplete}/${Quran.JUZ_COUNT}", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_surahs), "${stats.surahsComplete}/114", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_weak), stats.weakPages.toString(), Modifier.weight(1f))
        }

        SectionTitle(stringResource(R.string.estimate_title))
        Text(
            when (val days = stats.estimatedDaysLeft) {
                null -> stringResource(R.string.estimate_unknown)
                0 -> stringResource(R.string.quran_complete)
                else -> stringResource(
                    R.string.estimate_value,
                    stats.newPagesLast30Days,
                    pluralStringResource(R.plurals.days_count, days, days),
                    DayClock.format(state.today + days),
                )
            },
        )

        SectionTitle(stringResource(R.string.last_7_days))
        BarChart(stats.last7Days)

        SectionTitle(stringResource(R.string.activity_12_weeks))
        if (isPro) {
            Heatmap(stats.last84Days)
        } else {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.pro_heatmap_locked), Modifier.weight(1f))
                }
                FilledTonalButton(onClick = onOpenSettings, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                    Text(stringResource(R.string.go_pro))
                }
            }
        }

        SectionTitle(stringResource(R.string.juz_progress))
        stats.juz.forEach { j ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.juz_n, j.juz), Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { j.fraction }, modifier = Modifier.weight(1f))
                Text("${j.memorized}/${j.total}", Modifier.width(56.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarChart(values: List<Int>) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val description = stringResource(R.string.chart_description, values.joinToString(", "))
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("bar_chart"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v.toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((80 * v / max).coerceAtLeast(2).dp)
                        .background(
                            if (i == values.lastIndex) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
                )
            }
        }
    }
    Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Heatmap(values: List<Int>) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    // 12 columns (weeks) x 7 rows (days), oldest at top-left.
    Row(Modifier.fillMaxWidth().testTag("heatmap"), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        values.chunked(7).forEach { week ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { v ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(
                                if (v == 0) empty else base.copy(alpha = 0.25f + 0.75f * v / max),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
    }
}
