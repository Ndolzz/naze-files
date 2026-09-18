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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.naze.files.data.repository.FileIndexRepository
import com.naze.files.ui.browser.components.FileThumbnail
import com.naze.files.util.formatFileSize
import kotlinx.coroutines.CancellationException
import java.io.IOException

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

/** What the screen actually shows. Loading never lingers forever — every
 *  path out of [FileIndexRepository.getIndex] leads to either [Loaded] or
 *  [Error], so the UI can never get stuck spinning. */
private sealed interface CategoryUiState {
    data object Loading : CategoryUiState
    data class Loaded(val items: List<FileItem>, val isRefreshing: Boolean) : CategoryUiState
    data class Error(val message: String) : CategoryUiState
}

private fun List<FileItem>.filterAndSort(category: FileCategory): List<FileItem> =
    filter { it.category == category }.sortedByDescending { it.lastModifiedMillis }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: FileCategory,
    rootPath: String,
    onOpenItem: (FileItem) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Paint instantly from whatever is already cached (even if stale) so
    // re-opening a category never shows a blank spinner while the shared
    // index refreshes underneath it.
    var state by remember(category, rootPath) {
        val cached = FileIndexRepository.peekCache(rootPath)
        mutableStateOf<CategoryUiState>(
            if (cached != null) CategoryUiState.Loaded(cached.filterAndSort(category), isRefreshing = true)
            else CategoryUiState.Loading,
        )
    }
    var retryTick by remember(category, rootPath) { mutableIntStateOf(0) }

    LaunchedEffect(category, rootPath, retryTick) {
        val forceRefresh = retryTick > 0
        if (state !is CategoryUiState.Loaded) state = CategoryUiState.Loading
        try {
            val index = FileIndexRepository.getIndex(rootPath, forceRefresh = forceRefresh)
            state = CategoryUiState.Loaded(index.files.filterAndSort(category), isRefreshing = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            state = CategoryUiState.Error(e.message ?: "Unable to load files")
        } catch (e: Exception) {
            state = CategoryUiState.Error(e.message ?: "Unable to load files")
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(categoryLabel(category)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                if ((state as? CategoryUiState.Loaded)?.isRefreshing == true) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val current = state) {
                is CategoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is CategoryUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Unable to load files",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    Button(onClick = { retryTick++ }) { Text("Retry") }
                }

                is CategoryUiState.Loaded -> if (current.items.isEmpty() && !current.isRefreshing) {
                    Text(
                        text = "No ${categoryLabel(category).lowercase()} found",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(current.items, key = { it.absolutePath }) { item ->
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
}
