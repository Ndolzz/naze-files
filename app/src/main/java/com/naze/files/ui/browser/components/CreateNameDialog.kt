package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.naze.files.ui.browser.CreateRequest
import com.naze.files.util.FileNameValidator
import com.naze.files.util.NameValidation

@Composable
fun CreateNameDialog(
    request: CreateRequest,
    existingNames: Set<String>,
    onConfirm: (finalName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var baseName by remember(request) { mutableStateOf("") }

    val extension = (request as? CreateRequest.FileOfType)?.extension
    val finalName = when {
        request is CreateRequest.Folder -> baseName.trim()
        extension != null -> "${baseName.trim()}.$extension"
        else -> baseName.trim() // "Other" / custom: user types the full filename incl. extension
    }
    val validation = FileNameValidator.validate(finalName, existingNames)

    val title = when (request) {
        is CreateRequest.Folder -> "Create folder"
        is CreateRequest.FileOfType -> "Create file"
    }
    val label = when {
        request is CreateRequest.Folder -> "Folder name"
        extension != null -> "File name (.${extension} will be added)"
        else -> "File name (include extension)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = baseName,
                    onValueChange = { baseName = it },
                    label = { Text(label) },
                    singleLine = true,
                    isError = baseName.isNotEmpty() && validation is NameValidation.Invalid,
                )
                if (baseName.isNotEmpty() && validation is NameValidation.Invalid) {
                    Text(validation.reason, color = MaterialTheme.colorScheme.error)
                }
                if (extension != null) {
                    Text(
                        text = "Will be created as \"$finalName\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(finalName) },
                enabled = validation is NameValidation.Valid,
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
