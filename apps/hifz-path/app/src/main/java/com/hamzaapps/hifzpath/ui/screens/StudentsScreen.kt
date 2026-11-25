package com.hamzaapps.hifzpath.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.data.db.Student
import com.hamzaapps.hifzpath.ui.HifzUiState
import com.hamzaapps.hifzpath.ui.HifzViewModel
import com.hamzaapps.hifzpath.ui.components.ConfirmDialog

@Composable
fun StudentsScreen(state: HifzUiState, vm: HifzViewModel, onOpenSettings: () -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var showProPrompt by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }

    Column {
        Text(
            stringResource(R.string.students_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.students, key = { it.id }) { s ->
                val active = s.id == state.student?.id
                ListItem(
                    modifier = Modifier.clickable { vm.switchStudent(s) },
                    leadingContent = {
                        if (active) Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.active), tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = { Text(s.name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renaming = s.id }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename)) }
                            if (state.students.size > 1) {
                                IconButton(onClick = { deleting = s.id }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
        Button(
            onClick = { if (vm.canAddStudent()) adding = true else showProPrompt = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_student"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.add_student), Modifier.padding(start = 8.dp))
        }
    }

    if (adding) {
        NameDialog(stringResource(R.string.add_student), "", onSave = { name ->
            val current = state.student
            vm.createStudent(name, current?.direction ?: 1, current?.newQuartersPerDay ?: 2, current?.cycleDays ?: 30)
            adding = false
        }, onDismiss = { adding = false })
    }
    renaming?.let { id ->
        val s = state.students.firstOrNull { it.id == id }
        if (s != null) {
            NameDialog(stringResource(R.string.rename), s.name, onSave = {
                vm.renameStudent(s, it)
                renaming = null
            }, onDismiss = { renaming = null })
        }
    }
    deleting?.let { id ->
        val s: Student? = state.students.firstOrNull { it.id == id }
        if (s != null) {
            ConfirmDialog(
                title = stringResource(R.string.delete),
                message = stringResource(R.string.confirm_delete_student, s.name),
                confirmLabel = stringResource(R.string.delete),
                onConfirm = { vm.deleteStudent(s) },
                onDismiss = { deleting = null },
            )
        }
    }
    if (showProPrompt) {
        ConfirmDialog(
            title = stringResource(R.string.pro_title),
            message = stringResource(R.string.pro_students_locked),
            confirmLabel = stringResource(R.string.go_pro),
            onConfirm = onOpenSettings,
            onDismiss = { showProPrompt = false },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                label = { Text(stringResource(R.string.student_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.testTag("name_field"),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
