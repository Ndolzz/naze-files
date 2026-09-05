package com.naze.files.ui.home

import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileCategory
import com.naze.files.util.formatFileSize
import java.io.File

private data class QuickAccessItem(val label: String, val icon: ImageVector, val path: String? = null, val action: QuickAction? = null)
private enum class QuickAction { RECENT, FAVORITES }

@Composable
fun HomeScreen(
    rootLabel: String,
    rootPath: String,
    onOpenBrowser: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenRecent: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStorageAnalyzer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var usedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(rootPath) {
        val stat = StatFs(rootPath)
        totalBytes = stat.totalBytes
        usedBytes = (stat.totalBytes - stat.availableBytes).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Naze Files",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenStorageAnalyzer) {
                Icon(imageVector = Icons.Filled.PieChart, contentDescription = "Storage Analyzer")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        Surface(
            onClick = onOpenBrowser,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Search files",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        Surface(
            onClick = onOpenBrowser,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = rootLabel,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                    )
                }
                Text(
                    text = "Used ${formatFileSize(usedBytes)} of ${formatFileSize(totalBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }

        SectionLabel("Categories")
        val categories = listOf(
            FileCategory.IMAGE to Icons.Filled.Image,
            FileCategory.VIDEO to Icons.Filled.Movie,
            FileCategory.AUDIO to Icons.Filled.MusicNote,
            FileCategory.DOCUMENT to Icons.Filled.Description,
            FileCategory.ARCHIVE to Icons.Filled.Archive,
            FileCategory.APK to Icons.Filled.Android,
            FileCategory.CODE to Icons.Filled.Code,
            FileCategory.OTHER to Icons.Filled.InsertDriveFile,
        )
        categories.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                rowItems.forEach { (category, icon) ->
                    CategoryTile(
                        label = categoryDisplayName(category),
                        icon = icon,
                        onClick = { onOpenCategory(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowItems.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }

        SectionLabel("Quick Access")
        val quickAccessItems = listOf(
            QuickAccessItem("Recent", Icons.Filled.History, action = QuickAction.RECENT),
            QuickAccessItem("Favorites", Icons.Filled.Star, action = QuickAction.FAVORITES),
            QuickAccessItem("Downloads", Icons.Filled.Download, path = "$rootPath/Download"),
            QuickAccessItem("Documents", Icons.Filled.Description, path = "$rootPath/Documents"),
            QuickAccessItem("Pictures", Icons.Filled.Image, path = "$rootPath/Pictures"),
            QuickAccessItem("Music", Icons.Filled.MusicNote, path = "$rootPath/Music"),
            QuickAccessItem("Movies", Icons.Filled.Movie, path = "$rootPath/Movies"),
        )
        quickAccessItems.forEach { qa ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (qa.action) {
                            QuickAction.RECENT -> onOpenRecent()
                            QuickAction.FAVORITES -> onOpenFavorites()
                            null -> qa.path?.let { path ->
                                if (File(path).exists()) onOpenFolder(path) else onOpenBrowser()
                            }
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = qa.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = qa.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

private fun categoryDisplayName(category: FileCategory): String = when (category) {
    FileCategory.IMAGE -> "Images"
    FileCategory.VIDEO -> "Videos"
    FileCategory.AUDIO -> "Audio"
    FileCategory.DOCUMENT -> "Docs"
    FileCategory.ARCHIVE -> "Archives"
    FileCategory.APK -> "APKs"
    FileCategory.CODE -> "Code"
    FileCategory.OTHER -> "Other"
    FileCategory.FOLDER -> "Folders"
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun CategoryTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .aspectRatio(1f)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
