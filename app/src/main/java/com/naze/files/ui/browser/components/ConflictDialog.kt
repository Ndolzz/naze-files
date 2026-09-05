package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import com.naze.files.data.operations.ConflictResolution

@Composable
fun ConflictDialog(
    fileName: String,
    onResolve: (ConflictResolution) -> Unit,
) {
    var renaming by remember(fileName) { mutableStateOf(false) }
    var newName by remember(fileName) { mutableStateOf(suggestRenamedName(fileName)) }

    AlertDialog(
        onDismissRequest = { /* must choose one of the options below */ },
        title = { Text("File already exists") },
        text = {
            Column {
                Text("\"$fileName\" already exists at the destination.")
                if (renaming) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (renaming) {
                Button(onClick = { onResolve(ConflictResolution.Rename(newName.trim())) }) { Text("Save as new name") }
            } else {
                Button(onClick = { onResolve(ConflictResolution.Replace) }) { Text("Replace") }
            }
        },
        dismissButton = {
            Column {
                if (!renaming) {
                    OutlinedButton(onClick = { renaming = true }) { Text("Rename") }
                }
                TextButton(onClick = { onResolve(ConflictResolution.Skip) }) { Text("Skip") }
            }
        },
    )
}

private fun suggestRenamedName(name: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot <= 0) "$name (1)" else "${name.substring(0, dot)} (1)${name.substring(dot)}"
}
