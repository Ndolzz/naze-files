package com.naze.files.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.data.viewer.PdfDocumentLoader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    item: FileItem,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    var loader by remember(item.absolutePath) { mutableStateOf<PdfDocumentLoader?>(null) }
    var loadError by remember(item.absolutePath) { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var menuExpanded by remember { mutableStateOf(false) }
    val listState = remember { LazyListState() }

    LaunchedEffect(item.absolutePath) {
        val newLoader = PdfDocumentLoader(File(item.absolutePath))
        val result = newLoader.open()
        if (result.isSuccess) {
            loader = newLoader
        } else {
            loadError = result.exceptionOrNull()?.message ?: "This PDF could not be opened."
        }
    }

    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.5f) }) {
                            Icon(imageVector = Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                        }
                        IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(3f) }) {
                            Icon(imageVector = Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = { menuExpanded = false; onShare() },
                            )
                            DropdownMenuItem(
                                text = { Text("Open with…") },
                                leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenWith() },
                            )
                            DropdownMenuItem(
                                text = { Text("Information") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = { menuExpanded = false; onShowInfo() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuExpanded = false; onDelete() },
                            )
                        }
                    },
                )
                loader?.let { l ->
                    if (l.pageCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Page $currentPage of ${l.pageCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                loadError != null -> Text(
                    text = loadError ?: "",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                loader == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                else -> {
                    val l = loader!!
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(l.pageCount, key = { it }) { index ->
                            PdfPageItem(loader = l, index = index, targetWidthPx = (screenWidthPx * zoom).toInt())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageItem(loader: PdfDocumentLoader, index: Int, targetWidthPx: Int) {
    var bitmap by remember(index, targetWidthPx) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(index, targetWidthPx) {
        bitmap = runCatching { loader.renderPage(index, targetWidthPx) }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val current = bitmap
        if (current != null) {
            Image(bitmap = current.asImageBitmap(), contentDescription = "Page ${index + 1}")
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
