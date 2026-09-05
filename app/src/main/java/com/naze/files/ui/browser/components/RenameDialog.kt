package com.naze.files.ui.browser.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.naze.files.data.model.FileItem
import com.naze.files.util.FileNameValidator
import com.naze.files.util.NameValidation

@Composable
fun RenameDialog(
    target: FileItem,
    existingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(target.absolutePath) {
        mutableStateOf(TextFieldValue(target.name, selection = androidx.compose.ui.text.TextRange(0, target.displayNameWithoutExtension.length)))
    }
    val otherNames = remember(existingNames, target.name) { existingNames - target.name }
    val validation = FileNameValidator.validate(text.text, otherNames)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            androidx.compose.foundation.layout.Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = validation is NameValidation.Invalid,
                )
                if (validation is NameValidation.Invalid) {
                    Text(validation.reason, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.text.trim()) },
                enabled = validation is NameValidation.Valid,
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
