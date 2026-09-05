package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.ui.browser.ActiveOperation
import com.naze.files.util.formatFileSize

@Composable
fun ProgressOverlay(
    operation: ActiveOperation,
    onCancel: () -> Unit,
) {
    val progress = operation.progress
    AlertDialog(
        onDismissRequest = { /* cancel via explicit button only */ },
        title = { Text(operation.label) },
        text = {
            Column {
                if (progress != null) {
                    Text(
                        text = progress.currentFileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { progress.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    )
                    Text(
                        text = "${formatFileSize(progress.processedBytes)} / ${formatFileSize(progress.totalBytes)} " +
                            "(${progress.processedItems}/${progress.totalItems} items)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}
