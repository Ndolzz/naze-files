package com.naze.files.ui.browser.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.naze.files.data.model.FileItem

@Composable
fun DeleteConfirmDialog(
    targets: List<FileItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = if (targets.size == 1) {
        "Move \"${targets[0].name}\" to trash?"
    } else {
        "Move ${targets.size} items to trash?"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
