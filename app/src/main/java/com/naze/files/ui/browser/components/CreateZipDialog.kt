package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.files.util.FileNameValidator
import com.naze.files.util.NameValidation
import java.util.zip.Deflater

private data class CompressionOption(val label: String, val level: Int)

private val compressionOptions = listOf(
    CompressionOption("Fast", Deflater.BEST_SPEED),
    CompressionOption("Normal", Deflater.DEFAULT_COMPRESSION),
    CompressionOption("Maximum", Deflater.BEST_COMPRESSION),
)

@Composable
fun CreateZipDialog(
    suggestedName: String,
    existingNames: Set<String>,
    onConfirm: (name: String, compressionLevel: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(suggestedName) }
    var selectedLevel by remember { mutableStateOf(compressionOptions[1]) }
    val validation = FileNameValidator.validate(name, existingNames)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Archive name") },
                    singleLine = true,
                    isError = validation is NameValidation.Invalid,
                )
                if (validation is NameValidation.Invalid) {
                    Text(validation.reason, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    text = "Compression level",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                compressionOptions.forEach { option ->
                    Row(
                        modifier = Modifier.selectable(
                            selected = option == selectedLevel,
                            onClick = { selectedLevel = option },
                        ),
                    ) {
                        RadioButton(selected = option == selectedLevel, onClick = { selectedLevel = option })
                        Text(option.label, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedLevel.level) },
                enabled = validation is NameValidation.Valid,
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
