package com.naze.files.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.data.model.ViewMode
import com.naze.files.ui.browser.components.BreadcrumbBar
import com.naze.files.ui.browser.components.ClipboardBar
import com.naze.files.ui.browser.components.ConflictDialog
import com.naze.files.ui.browser.components.CreateEntryPointSheet
import com.naze.files.ui.browser.components.CreateNameDialog
import com.naze.files.ui.browser.components.CreateZipDialog
import com.naze.files.ui.browser.components.DeleteConfirmDialog
import com.naze.files.ui.browser.components.FileContextMenuSheet
import com.naze.files.ui.browser.components.FileGridItem
import com.naze.files.ui.browser.components.FileListItem
import com.naze.files.ui.browser.components.FileTypePickerSheet
import com.naze.files.ui.browser.components.ProgressOverlay
import com.naze.files.ui.browser.components.RenameDialog
import com.naze.files.ui.browser.components.SelectionToolbar
import com.naze.files.ui.browser.components.SortMenu
import com.naze.files.ui.browser.components.ViewModeMenu
import com.naze.files.ui.viewer.FileInfoDialog
import com.naze.files.util.buildOpenWithIntent
import java.io.File

private enum class CreateStep { None, EntryPoint, FileTypePicker }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onOpenFile: (FileItem) -> Unit,
    onShareFiles: (List<FileItem>) -> Unit,
    onOpenFavorites: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchActive by remember { mutableStateOf(false) }
    var createStep by remember { mutableStateOf(CreateStep.None) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.pendingShare) {
        state.pendingShare?.let {
            onShareFiles(it)
            viewModel.consumeShareRequest()
        }
    }

    BackHandler(enabled = true) {
        when {
            state.isSelectionMode -> viewModel.clearSelection()
            searchActive -> {
                searchActive = false
                viewModel.setSearchQuery("")
            }
            !viewModel.navigateUp() -> onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                placeholder = { Text("Search") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (state.isSelectionMode) {
                            Text("${state.selectedPaths.size} selected")
                        } else {
                            Text(state.breadcrumbs.lastOrNull()?.label ?: "Naze Files")
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (state.isSelectionMode) {
                                    viewModel.clearSelection()
                                } else if (searchActive) {
                                    searchActive = false
                                    viewModel.setSearchQuery("")
                                } else if (!viewModel.navigateUp()) {
                                    onNavigateBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (state.isSelectionMode || searchActive) Icons.Filled.Close else Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        if (!state.isSelectionMode) {
                            if (!searchActive) {
                                IconButton(onClick = { searchActive = true }) {
                                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = onOpenFavorites) {
                                    Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorites")
                                }
                            }
                            ViewModeMenu(current = state.viewMode, onSelected = viewModel::setViewMode)
                            SortMenu(
                                current = state.sortPreference,
                                onOrderSelected = viewModel::setSortOrder,
                                onToggleFoldersFirst = viewModel::toggleFoldersFirst,
                            )
                        }
                    },
                )
                if (!searchActive) {
                    BreadcrumbBar(
                        breadcrumbs = state.breadcrumbs,
                        onCrumbClick = { viewModel.openFolder(it.path) },
                    )
                }
            }
        },
        bottomBar = {
            when {
                state.isSelectionMode -> SelectionToolbar(
                    onCopy = viewModel::copySelectedToClipboard,
                    onCut = viewModel::cutSelectedToClipboard,
                    onShare = viewModel::requestShareSelected,
                    onCompress = viewModel::requestCompressSelected,
                    onDelete = viewModel::requestDeleteSelected,
                )
                state.clipboard != null -> ClipboardBar(
                    clipboard = state.clipboard!!,
                    onPaste = viewModel::pasteClipboard,
                    onCancel = viewModel::clearClipboard,
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode && !searchActive) {
                FloatingActionButton(onClick = { createStep = CreateStep.EntryPoint }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> ErrorState(message = state.errorMessage!!, onRetry = viewModel::refresh)
                state.visibleItems.isEmpty() -> EmptyState(isSearching = state.searchQuery.isNotBlank())
                else -> FileItemsContent(
                    items = state.visibleItems,
                    viewMode = state.viewMode,
                    selectedPaths = state.selectedPaths,
                    favoritePaths = state.favoritePaths,
                    isSelectionMode = state.isSelectionMode,
                    showExtension = state.showFileExtensions,
                    onItemClick = { item ->
                        when {
                            state.isSelectionMode -> viewModel.toggleSelect(item.absolutePath)
                            item.isDirectory -> viewModel.openFolder(item.absolutePath)
                            else -> onOpenFile(item)
                        }
                    },
                    onItemLongClick = { viewModel.toggleSelect(it.absolutePath) },
                    onMoreClick = { viewModel.openContextMenu(it) },
                )
            }
        }
    }

    // ---- create flow ----
    if (createStep == CreateStep.EntryPoint) {
        CreateEntryPointSheet(
            onDismiss = { createStep = CreateStep.None },
            onPickFolder = {
                createStep = CreateStep.None
                viewModel.requestCreate(CreateRequest.Folder)
            },
            onPickFile = { createStep = CreateStep.FileTypePicker },
        )
    }
    if (createStep == CreateStep.FileTypePicker) {
        FileTypePickerSheet(
            onDismiss = { createStep = CreateStep.None },
            onTypeSelected = {
                createStep = CreateStep.None
                viewModel.requestCreate(it)
            },
        )
    }
    state.activeCreateRequest?.let { request ->
        CreateNameDialog(
            request = request,
            existingNames = state.items.map { it.name }.toSet(),
            onConfirm = { viewModel.confirmCreate(it) },
            onDismiss = { viewModel.cancelCreate() },
        )
    }

    // ---- context menu ----
    state.contextMenuTarget?.let { item ->
        FileContextMenuSheet(
            item = item,
            isFavorite = item.absolutePath in state.favoritePaths,
            onDismiss = { viewModel.closeContextMenu() },
            onOpen = {
                viewModel.closeContextMenu()
                if (item.isDirectory) viewModel.openFolder(item.absolutePath) else onOpenFile(item)
            },
            onOpenWith = {
                viewModel.closeContextMenu()
                context.startActivity(buildOpenWithIntent(context, File(item.absolutePath), item.mimeType))
            },
            onShare = { viewModel.requestShareSingle(item) },
            onCopy = { viewModel.copySingleToClipboard(item) },
            onCut = { viewModel.cutSingleToClipboard(item) },
            onRename = { viewModel.requestRename(item) },
            onCompress = { viewModel.requestCompressSingle(item) },
            onDelete = { viewModel.requestDeleteSingle(item) },
            onToggleFavorite = { viewModel.toggleFavorite(item) },
            onShowInfo = { viewModel.requestInfo(item) },
        )
    }

    // ---- other dialogs ----
    state.renameTarget?.let { target ->
        RenameDialog(
            target = target,
            existingNames = state.items.map { it.name }.toSet(),
            onConfirm = { viewModel.confirmRename(it) },
            onDismiss = { viewModel.cancelRename() },
        )
    }
    state.deleteConfirmationTargets?.let { targets ->
        DeleteConfirmDialog(
            targets = targets,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() },
        )
    }
    state.pendingConflictFileName?.let { fileName ->
        ConflictDialog(fileName = fileName, onResolve = { viewModel.resolveConflict(it) })
    }
    state.activeOperation?.let { operation ->
        ProgressOverlay(operation = operation, onCancel = { viewModel.cancelActiveOperation() })
    }
    state.infoTarget?.let { target ->
        FileInfoDialog(item = target, onDismiss = { viewModel.dismissInfo() })
    }
    state.compressTargets?.let { targets ->
        CreateZipDialog(
            suggestedName = state.compressSuggestedName,
            existingNames = state.items.map { it.name }.toSet(),
            onConfirm = { name, level -> viewModel.confirmCompress(name, level) },
            onDismiss = { viewModel.cancelCompress() },
        )
    }
}

@Composable
private fun FileItemsContent(
    items: List<FileItem>,
    viewMode: ViewMode,
    selectedPaths: Set<String>,
    favoritePaths: Set<String>,
    isSelectionMode: Boolean,
    showExtension: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onMoreClick: (FileItem) -> Unit,
) {
    when (viewMode) {
        ViewMode.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems(items, key = { it.absolutePath }) { item ->
                FileGridItem(
                    item = item,
                    isSelected = item.absolutePath in selectedPaths,
                    isFavorite = item.absolutePath in favoritePaths,
                    showExtension = showExtension,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
        ViewMode.LIST, ViewMode.COMPACT -> LazyColumn {
            items(items, key = { it.absolutePath }) { item ->
                FileListItem(
                    item = item,
                    isSelected = item.absolutePath in selectedPaths,
                    isFavorite = item.absolutePath in favoritePaths,
                    selectionModeActive = isSelectionMode,
                    showExtension = showExtension,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    onMoreClick = { onMoreClick(item) },
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (isSearching) "No matching files" else "This folder is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Unable to open this folder.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        IconButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text("Retry")
        }
    }
}
