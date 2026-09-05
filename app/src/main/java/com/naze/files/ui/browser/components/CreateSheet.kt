package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.files.ui.browser.CreateRequest

private data class FileTypeOption(val label: String, val extension: String?)

private val fileTypeOptions = listOf(
    FileTypeOption("HTML", "html"),
    FileTypeOption("CSS", "css"),
    FileTypeOption("JavaScript", "js"),
    FileTypeOption("JSON", "json"),
    FileTypeOption("XML", "xml"),
    FileTypeOption("TXT", "txt"),
    FileTypeOption("Markdown", "md"),
    FileTypeOption("Kotlin", "kt"),
    FileTypeOption("Java", "java"),
    FileTypeOption("Python", "py"),
    FileTypeOption("C", "c"),
    FileTypeOption("C++", "cpp"),
    FileTypeOption("YAML", "yml"),
    FileTypeOption("CSV", "csv"),
    FileTypeOption("Other (custom name)", null),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntryPointSheet(
    onDismiss: () -> Unit,
    onPickFolder: () -> Unit,
    onPickFile: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text("Folder") },
            leadingContent = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickableRow(onPickFolder),
        )
        ListItem(
            headlineContent = { Text("File") },
            leadingContent = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickableRow(onPickFile),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTypePickerSheet(
    onDismiss: () -> Unit,
    onTypeSelected: (CreateRequest.FileOfType) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Choose type",
            modifier = Modifier.padding(16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            items(fileTypeOptions) { option ->
                AssistChip(
                    onClick = { onTypeSelected(CreateRequest.FileOfType(option.label, option.extension)) },
                    label = { Text(option.label) },
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
