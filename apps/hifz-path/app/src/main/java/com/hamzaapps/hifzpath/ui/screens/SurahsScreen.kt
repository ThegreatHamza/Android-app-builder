package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.quran.Quran
import com.hamzaapps.hifzpath.data.quran.Surah
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.ui.HifzUiState
import com.hamzaapps.hifzpath.ui.HifzViewModel

@Composable
fun SurahsScreen(state: HifzUiState, vm: HifzViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val filtered = Quran.surahs.filter { s ->
        query.isBlank() ||
            s.number.toString() == query.trim() ||
            s.transliteration.contains(query.trim(), ignoreCase = true) ||
            s.meaning.contains(query.trim(), ignoreCase = true) ||
            s.arabic.contains(query.trim())
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_surah)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("surah_search"),
        )
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.testTag("surah_list")) {
            items(filtered, key = { it.number }) { surah ->
                val done = surah.pages.count { state.pages[it]?.status == PageStatus.MEMORIZED }
                val fraction = done.toFloat() / surah.pages.count()
                ListItem(
                    modifier = Modifier.clickable { selected = surah.number },
                    leadingContent = {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(surah.number.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    headlineContent = { Text(surah.transliteration) },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.surah_meta, surah.meaning, surah.ayahCount, surah.startPage, surah.endPage))
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    trailingContent = { Text(surah.arabic, style = MaterialTheme.typography.titleMedium) },
                )
                HorizontalDivider()
            }
        }
    }

    selected?.let { number ->
        SurahDialog(Quran.surah(number), vm, onDismiss = { selected = null })
    }
}

@Composable
private fun SurahDialog(surah: Surah, vm: HifzViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.surah_title, surah.number, surah.transliteration)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.surah_page_note))
                OutlinedButton(onClick = { vm.markKnown(surah.pages); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mark_surah_known))
                }
                OutlinedButton(onClick = { vm.markLearningAll(surah.pages); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mark_surah_learning))
                }
                OutlinedButton(onClick = { vm.clearPages(surah.pages); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.clear_surah))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
