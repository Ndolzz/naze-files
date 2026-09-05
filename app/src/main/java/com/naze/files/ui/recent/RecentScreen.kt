package com.naze.files.ui.recent

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
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.naze.files.util.formatModifiedDate
import com.naze.files.util.iconFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    viewModel: RecentViewModel,
    onOpenItem: (FileItem) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear recent")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                entries.isEmpty() -> Text(
                    text = "No recently opened files yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.path }) { entry ->
                        val item = entry.item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item != null) { item?.let(onOpenItem) }
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
                                    text = if (item == null) "No longer available" else formatModifiedDate(item.lastModifiedMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
