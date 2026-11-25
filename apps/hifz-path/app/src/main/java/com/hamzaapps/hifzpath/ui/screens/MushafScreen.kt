package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.quran.Quran
import com.hamzaapps.hifzpath.domain.PageState
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.domain.Strength
import com.hamzaapps.hifzpath.ui.HifzUiState
import com.hamzaapps.hifzpath.ui.HifzViewModel
import com.hamzaapps.hifzpath.ui.components.ConfirmDialog
import com.hamzaapps.hifzpath.ui.components.RateDialog
import com.hamzaapps.hifzpath.ui.components.pageSubtitle
import com.hamzaapps.hifzpath.ui.components.strengthLabel

@Composable
fun pageColors(state: PageState?): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when {
        state == null || state.status == PageStatus.NOT_STARTED -> c.surfaceVariant to c.onSurfaceVariant
        state.status == PageStatus.LEARNING -> c.tertiaryContainer to c.onTertiaryContainer
        state.strength == Strength.WEAK -> c.errorContainer to c.onErrorContainer
        state.strength == Strength.GOOD -> c.primaryContainer to c.onPrimaryContainer
        else -> c.primary to c.onPrimary
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MushafScreen(state: HifzUiState, vm: HifzViewModel) {
    val defaultJuz = if (state.student?.direction == 1) Quran.JUZ_COUNT else 1
    var juz by rememberSaveable { mutableIntStateOf(defaultJuz) }
    var selectedPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    val pages = Quran.pagesOfJuz(juz)
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem((juz - 3).coerceAtLeast(0)) }

    Column {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Quran.JUZ_COUNT) { i ->
                val j = i + 1
                FilterChip(
                    selected = j == juz,
                    onClick = { juz = j },
                    label = { Text(stringResource(R.string.juz_n, j)) },
                    modifier = Modifier.testTag("juz_$j"),
                )
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val done = pages.count { state.pages[it]?.status == PageStatus.MEMORIZED }
            Text(
                stringResource(R.string.juz_summary, juz, done, pages.count()),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.juz_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.mark_juz_known)) }, onClick = {
                        menuOpen = false
                        confirm = "known"
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.clear_juz)) }, onClick = {
                        menuOpen = false
                        confirm = "clear"
                    })
                }
            }
        }
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend(stringResource(R.string.status_not_started), null)
            Legend(stringResource(R.string.status_learning), PageState(0, PageStatus.LEARNING))
            Strength.entries.forEach { s ->
                Legend(stringResource(strengthLabel(s)), PageState(0, PageStatus.MEMORIZED, s))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("page_grid"),
        ) {
            items(pages.toList(), key = { it }) { page ->
                val (bg, fg) = pageColors(state.pages[page])
                val label = stringResource(R.string.page_n, page)
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .background(bg, RoundedCornerShape(10.dp))
                        .clickable { selectedPage = page }
                        .semantics { contentDescription = label }
                        .testTag("page_$page"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(page.toString(), color = fg, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    selectedPage?.let { page ->
        PageSheet(page, state.pages[page], vm, onDismiss = { selectedPage = null })
    }

    when (confirm) {
        "known" -> ConfirmDialog(
            title = stringResource(R.string.mark_juz_known),
            message = stringResource(R.string.confirm_mark_known_juz, juz),
            confirmLabel = stringResource(R.string.confirm),
            onConfirm = { vm.markKnown(pages) },
            onDismiss = { confirm = null },
        )
        "clear" -> ConfirmDialog(
            title = stringResource(R.string.clear_juz),
            message = stringResource(R.string.confirm_clear_juz, juz),
            confirmLabel = stringResource(R.string.clear),
            onConfirm = { vm.clearPages(pages) },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun Legend(label: String, state: PageState?) {
    val (bg, _) = pageColors(state)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier
                .size(14.dp)
                .background(bg, RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSheet(page: Int, state: PageState?, vm: HifzViewModel, onDismiss: () -> Unit) {
    var reviewing by rememberSaveable { mutableStateOf(false) }
    val status = state?.status ?: PageStatus.NOT_STARTED
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.page_n, page), style = MaterialTheme.typography.headlineSmall)
            Text(pageSubtitle(page), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(8.dp))
            Text(stringResource(R.string.status), style = MaterialTheme.typography.labelLarge)
            val options = listOf(
                PageStatus.NOT_STARTED to R.string.status_not_started,
                PageStatus.LEARNING to R.string.status_learning,
                PageStatus.MEMORIZED to R.string.status_memorized,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = status == value,
                        onClick = {
                            when (value) {
                                PageStatus.NOT_STARTED -> vm.clearPage(page)
                                PageStatus.LEARNING -> vm.markLearning(page)
                                PageStatus.MEMORIZED -> if (status != PageStatus.MEMORIZED) vm.markKnown(listOf(page))
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.testTag("status_${value.name}"),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }
            if (status == PageStatus.MEMORIZED && state != null) {
                Spacer(Modifier.padding(8.dp))
                Text(stringResource(R.string.strength), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Strength.entries.forEachIndexed { index, s ->
                        SegmentedButton(
                            selected = state.strength == s,
                            onClick = { vm.setStrength(page, s) },
                            shape = SegmentedButtonDefaults.itemShape(index, Strength.entries.size),
                        ) { Text(stringResource(strengthLabel(s))) }
                    }
                }
                Spacer(Modifier.padding(4.dp))
                Text(
                    stringResource(R.string.review_count, state.reviewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { reviewing = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.log_revision_now))
                }
            }
        }
    }
    if (reviewing) {
        RateDialog(
            title = stringResource(R.string.page_n, page),
            onRate = {
                vm.review(page, it)
                reviewing = false
            },
            onDismiss = { reviewing = false },
        )
    }
}
