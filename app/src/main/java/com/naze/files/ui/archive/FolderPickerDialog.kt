package com.naze.files.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.data.repository.FileRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    repository: FileRepository,
    rootPath: String,
    rootLabel: String,
    initialPath: String = rootPath,
    confirmLabel: String = "Use this folder",
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(currentPath) {
        loading = true
        folders = try {
            repository.listChildren(currentPath, includeHidden = false)
                .filter { it.isDirectory }
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(File(currentPath).name.ifEmpty { rootLabel }) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (currentPath == rootPath) {
                                        onDismiss()
                                    } else {
                                        currentPath = File(currentPath).parent ?: rootPath
                                    }
                                },
                            ) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
                bottomBar = {
                    Button(
                        onClick = { onFolderSelected(currentPath) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) { Text(confirmLabel) }
                },
            ) { padding ->
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)) {
                        items(folders, key = { it.absolutePath }) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.name) },
                                leadingContent = { Icon(imageVector = Icons.Filled.Folder, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPath = folder.absolutePath },
                            )
                        }
                    }
                }
            }
        }
    }
}
