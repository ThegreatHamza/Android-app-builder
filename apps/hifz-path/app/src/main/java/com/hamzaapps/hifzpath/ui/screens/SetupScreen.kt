package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.ui.components.PlanEditor

const val MAX_NAME_LENGTH = 40

@Composable
fun SetupScreen(onCreate: (name: String, direction: Int, quarters: Int, cycleDays: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableIntStateOf(1) }
    var quarters by rememberSaveable { mutableIntStateOf(2) }
    var cycle by rememberSaveable { mutableIntStateOf(30) }
    var showError by rememberSaveable { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.setup_welcome), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(MAX_NAME_LENGTH)
                    showError = false
                },
                label = { Text(stringResource(R.string.student_name)) },
                placeholder = { Text(stringResource(R.string.student_name_hint)) },
                isError = showError,
                supportingText = { if (showError) Text(stringResource(R.string.error_name_required)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_name"),
            )
            Spacer(Modifier.height(16.dp))
            PlanEditor(direction, quarters, cycle, { direction = it }, { quarters = it }, { cycle = it })
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isBlank()) showError = true else onCreate(name.trim(), direction, quarters, cycle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_start"),
            ) { Text(stringResource(R.string.setup_start)) }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.setup_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
