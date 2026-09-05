package com.naze.files.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileCategory
import com.naze.files.data.model.FileItem
import com.naze.files.ui.browser.components.FileThumbnail
import com.naze.files.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

private fun categoryLabel(category: FileCategory): String = when (category) {
    FileCategory.IMAGE -> "Images"
    FileCategory.VIDEO -> "Videos"
    FileCategory.AUDIO -> "Audio"
    FileCategory.DOCUMENT -> "Documents"
    FileCategory.ARCHIVE -> "Archives"
    FileCategory.APK -> "APKs"
    FileCategory.CODE -> "Code"
    FileCategory.OTHER -> "Other"
    FileCategory.FOLDER -> "Folders"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: FileCategory,
    rootPath: String,
    onOpenItem: (FileItem) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var results by remember(category) { mutableStateOf<List<FileItem>?>(null) }

    LaunchedEffect(category, rootPath) {
        val found = mutableListOf<FileItem>()
        withContext(Dispatchers.IO) {
            suspend fun walk(dir: File) {
                if (dir.name == ".naze_trash") return
                val children = dir.listFiles() ?: return
                for (child in children) {
                    currentCoroutineContext().ensureActive()
                    if (child.isDirectory) {
                        walk(child)
                    } else {
                        val item = FileItem(
                            child.name, child.absolutePath, false, child.length(), child.lastModified(),
                            child.name.startsWith("."), null, child.canRead(), child.canWrite(),
                        )
                        if (item.category == category) found += item
                    }
                }
            }
            walk(File(rootPath))
        }
        results = found.sortedByDescending { it.lastModifiedMillis }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryLabel(category)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val current = results
            when {
                current == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                current.isEmpty() -> Text(
                    text = "No ${categoryLabel(category).lowercase()} found",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(current, key = { it.absolutePath }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenItem(item) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                FileThumbnail(item = item, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize())
                            }
                            Box(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${formatFileSize(item.sizeBytes)} • ${item.absolutePath.substringBeforeLast('/')}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
