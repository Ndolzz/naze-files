package com.naze.files.ui.storage

import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileCategory
import com.naze.files.data.model.FileItem
import com.naze.files.ui.theme.NazeBlue
import com.naze.files.ui.theme.NazePurple
import com.naze.files.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

private data class CategoryBreakdown(val category: FileCategory, val bytes: Long)

private val categoryColors = mapOf(
    FileCategory.IMAGE to NazeBlue,
    FileCategory.VIDEO to NazePurple,
    FileCategory.AUDIO to Color(0xFF3DDC97),
    FileCategory.DOCUMENT to Color(0xFFFFB020),
    FileCategory.ARCHIVE to Color(0xFFFF5C6C),
    FileCategory.APK to Color(0xFF8D8FB8),
    FileCategory.CODE to Color(0xFF5BC8FF),
    FileCategory.OTHER to Color(0xFF6F7196),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    rootPath: String,
    onNavigateBack: () -> Unit,
) {
    var totalBytes by remember { mutableStateOf(0L) }
    var freeBytes by remember { mutableStateOf(0L) }
    var breakdown by remember { mutableStateOf<List<CategoryBreakdown>?>(null) }

    LaunchedEffect(rootPath) {
        val stat = StatFs(rootPath)
        totalBytes = stat.totalBytes
        freeBytes = stat.availableBytes

        val totals = mutableMapOf<FileCategory, Long>()
        withContext(Dispatchers.IO) {
            suspend fun walk(dir: File) {
                if (dir.name == ".naze_trash") return
                val children = dir.listFiles() ?: return
                for (child in children) {
                    currentCoroutineContext().ensureActive()
                    if (child.isDirectory) {
                        walk(child)
                    } else {
                        val syntheticItem = FileItem(
                            child.name, child.absolutePath, false, child.length(), child.lastModified(), false, null, true, true,
                        )
                        val category = FileCategory.fromItem(syntheticItem)
                        totals[category] = (totals[category] ?: 0L) + child.length()
                    }
                }
            }
            walk(File(rootPath))
        }
        breakdown = totals.entries.map { CategoryBreakdown(it.key, it.value) }.sortedByDescending { it.bytes }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0)
            Text("Used ${formatFileSize(usedBytes)} of ${formatFileSize(totalBytes)}", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Text(
                text = "By category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            val currentBreakdown = breakdown
            if (currentBreakdown == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val maxBytes = currentBreakdown.maxOfOrNull { it.bytes } ?: 1L
                currentBreakdown.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val fraction = if (maxBytes > 0) (entry.bytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f) else 0.02f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(categoryColors[entry.category] ?: MaterialTheme.colorScheme.primary),
                            )
                        }
                        Text(
                            text = entry.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(0.7f),
                        )
                        Text(
                            text = formatFileSize(entry.bytes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
