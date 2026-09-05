package com.naze.files.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.util.formatFileSize
import com.naze.files.util.formatModifiedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

private data class FolderStats(val itemCount: Int, val totalBytes: Long)

@Composable
fun FileInfoDialog(item: FileItem, onDismiss: () -> Unit) {
    var folderStats by remember(item.absolutePath) { mutableStateOf<FolderStats?>(null) }
    var computing by remember(item.absolutePath) { mutableStateOf(item.isDirectory) }
    var createdLabel by remember(item.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.absolutePath) {
        val file = File(item.absolutePath)
        createdLabel = withContext(Dispatchers.IO) { readCreationTime(file) }
        if (item.isDirectory) {
            folderStats = withContext(Dispatchers.IO) { computeFolderStats(file) }
            computing = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Information") },
        text = {
            Column {
                InfoRow("Name", item.name)
                InfoRow("Type", if (item.isDirectory) "Folder" else item.category.name.lowercase().replaceFirstChar { it.uppercase() })
                if (!item.isDirectory) InfoRow("MIME type", item.mimeType ?: "Unknown")
                InfoRow(
                    "Size",
                    if (item.isDirectory) {
                        folderStats?.let { formatFileSize(it.totalBytes) } ?: if (computing) "Calculating…" else "Unavailable"
                    } else {
                        formatFileSize(item.sizeBytes)
                    },
                )
                if (item.isDirectory) {
                    InfoRow("Items", folderStats?.itemCount?.toString() ?: if (computing) "Calculating…" else "Unavailable")
                }
                InfoRow("Location", File(item.absolutePath).parent ?: "—")
                InfoRow("Created", createdLabel ?: "Not available")
                InfoRow("Modified", formatModifiedDate(item.lastModifiedMillis))
                InfoRow("Readable", if (item.canRead) "Yes" else "No")
                InfoRow("Writable", if (item.canWrite) "Yes" else "No")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f),
        )
    }
}

private fun computeFolderStats(dir: File): FolderStats {
    var count = 0
    var bytes = 0L
    fun walk(f: File) {
        val children = f.listFiles() ?: return
        for (c in children) {
            count++
            if (c.isDirectory) walk(c) else bytes += c.length()
        }
    }
    walk(dir)
    return FolderStats(count, bytes)
}

private fun readCreationTime(file: File): String? {
    return try {
        val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        val millis = attrs.creationTime().toMillis()
        if (millis > 0) formatModifiedDate(millis) else null
    } catch (e: Exception) {
        null
    }
}
