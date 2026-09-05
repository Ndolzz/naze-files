package com.naze.files.ui.incoming

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.naze.files.data.repository.FileRepository
import com.naze.files.ui.archive.FolderPickerDialog
import com.naze.files.util.ContentUriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveIncomingScreen(
    uris: List<Uri>,
    fileRepository: FileRepository,
    storageRootPath: String,
    storageRootLabel: String,
    defaultFolder: String,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val names = remember { uris.map { ContentUriUtils.displayName(context, it) } }
    var showFolderPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }

    fun startSave(destDir: File) {
        isSaving = true
        savedCount = 0
        scope.launch(Dispatchers.IO) {
            if (!destDir.exists()) destDir.mkdirs()
            for (uri in uris) {
                val name = ContentUriUtils.displayName(context, uri)
                var target = File(destDir, name)
                var counter = 1
                while (target.exists()) {
                    val dot = name.lastIndexOf('.')
                    val newName = if (dot > 0) "${name.substring(0, dot)} ($counter)${name.substring(dot)}" else "$name ($counter)"
                    target = File(destDir, newName)
                    counter++
                }
                ContentUriUtils.copyToFile(context, uri, target)
                withContext(Dispatchers.Main) { savedCount += 1 }
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save to Naze Files") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = "${uris.size} item(s) to save",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(names) { name ->
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(imageVector = Icons.Filled.InsertDriveFile, contentDescription = null) },
                    )
                }
            }
            if (isSaving) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text("Saving $savedCount / ${uris.size}", modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextButton(onClick = { showFolderPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose folder…")
                    }
                    Button(
                        onClick = { startSave(File(defaultFolder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text("Save to ${File(defaultFolder).name}") }
                }
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            repository = fileRepository,
            rootPath = storageRootPath,
            rootLabel = storageRootLabel,
            initialPath = defaultFolder,
            confirmLabel = "Save here",
            onFolderSelected = { folder ->
                showFolderPicker = false
                startSave(File(folder))
            },
            onDismiss = { showFolderPicker = false },
        )
    }
}
