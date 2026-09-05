package com.naze.files.ui.favorites

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.util.formatFileSize
import com.naze.files.util.formatModifiedDate
import com.naze.files.util.iconFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenItem: (FileItem) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                entries.isEmpty() -> Text(
                    text = "No favorites yet. Add files or folders from their context menu.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
                else -> LazyColumn {
                    items(entries, key = { it.path }) { entry ->
                        FavoriteRow(
                            entry = entry,
                            onClick = { entry.item?.let(onOpenItem) },
                            onRemove = { viewModel.removeFavorite(entry.path) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(entry: FavoriteEntry, onClick: () -> Unit, onRemove: () -> Unit) {
    val item = entry.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item != null, onClick = onClick)
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
            if (item == null) {
                Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            } else {
                Icon(imageVector = iconFor(item.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Box(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item?.name ?: entry.path.substringAfterLast('/'),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    item == null -> "No longer available"
                    item.isDirectory -> formatModifiedDate(item.lastModifiedMillis)
                    else -> "${formatFileSize(item.sizeBytes)} • ${formatModifiedDate(item.lastModifiedMillis)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (item == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Filled.StarBorder, contentDescription = "Remove from favorites")
        }
    }
}
