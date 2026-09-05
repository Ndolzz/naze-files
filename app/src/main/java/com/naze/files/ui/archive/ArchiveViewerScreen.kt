package com.naze.files.ui.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.archive.ArchiveEntry
import com.naze.files.data.archive.ArchiveRepository
import com.naze.files.data.model.FileItem
import com.naze.files.data.operations.OperationProgress
import com.naze.files.data.repository.FileRepository
import com.naze.files.ui.browser.ActiveOperation
import com.naze.files.ui.browser.Breadcrumb
import com.naze.files.ui.browser.components.BreadcrumbBar
import com.naze.files.ui.browser.components.DeleteConfirmDialog
import com.naze.files.ui.browser.components.ProgressOverlay
import com.naze.files.util.formatFileSize
import kotlinx.coroutines.launch
import java.io.File

private enum class PickerMode { ExtractAll, ExtractSelected }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    item: FileItem,
    archiveRepository: ArchiveRepository,
    fileRepository: FileRepository,
    storageRootPath: String,
    storageRootLabel: String,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val zipFile = remember(item.absolutePath) { File(item.absolutePath) }

    var allEntries by remember(item.absolutePath) { mutableStateOf<List<ArchiveEntry>?>(null) }
    var loadError by remember(item.absolutePath) { mutableStateOf<String?>(null) }
    var currentVirtualPath by remember { mutableStateOf("") }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var activeOperation by remember { mutableStateOf<ActiveOperation?>(null) }
    var pendingDeletePaths by remember { mutableStateOf<Set<String>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    LaunchedEffect(item.absolutePath) {
        val result = archiveRepository.listEntries(zipFile)
        result.onSuccess { allEntries = it }
        result.onFailure { loadError = it.message ?: "This archive could not be opened." }
    }

    val entries = allEntries
    val visibleChildren = remember(entries, currentVirtualPath) {
        entries?.let { childrenOf(it, currentVirtualPath) } ?: emptyList()
    }
    val breadcrumbs = remember(currentVirtualPath, item.name) {
        buildVirtualBreadcrumbs(item.name, currentVirtualPath)
    }

    fun runExtraction(paths: Set<String>?, destination: File) {
        activeOperation = ActiveOperation(label = "Extracting")
        scope.launch {
            val result = archiveRepository.extract(zipFile, paths, destination) { name, processed, total ->
                activeOperation = ActiveOperation(
                    label = "Extracting",
                    progress = OperationProgress(name, processed.toLong(), total.toLong(), processed, total),
                )
            }
            activeOperation = null
            result.onSuccess { message = "Extracted to ${destination.name}" }
            result.onFailure { message = it.message ?: "Extraction failed" }
            selectedPaths = emptySet()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedPaths.isNotEmpty()) {
                            Text("${selectedPaths.size} selected")
                        } else {
                            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                when {
                                    selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
                                    currentVirtualPath.isNotEmpty() -> currentVirtualPath = currentVirtualPath.substringBeforeLast('/', "")
                                    else -> onNavigateBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selectedPaths.isNotEmpty()) Icons.Filled.Close else Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        if (selectedPaths.isNotEmpty()) {
                            IconButton(onClick = { pickerMode = PickerMode.ExtractSelected }) {
                                Icon(imageVector = Icons.Filled.UnfoldMore, contentDescription = "Extract selected")
                            }
                            IconButton(onClick = { pendingDeletePaths = selectedPaths }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete from archive")
                            }
                        } else {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Extract all") },
                                    onClick = { menuExpanded = false; pickerMode = PickerMode.ExtractAll },
                                )
                                DropdownMenuItem(
                                    text = { Text("Information") },
                                    onClick = { menuExpanded = false; onShowInfo() },
                                )
                            }
                        }
                    },
                )
                BreadcrumbBar(breadcrumbs = breadcrumbs, onCrumbClick = { currentVirtualPath = it.path })
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
                entries == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                visibleChildren.isEmpty() -> Text(
                    text = "This folder is empty",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleChildren, key = { it.path }) { entry ->
                        ArchiveEntryRow(
                            entry = entry,
                            isSelected = entry.path in selectedPaths,
                            onClick = {
                                if (selectedPaths.isNotEmpty()) {
                                    selectedPaths = toggle(selectedPaths, entry.path)
                                } else if (entry.isDirectory) {
                                    currentVirtualPath = entry.path
                                }
                            },
                            onLongClick = { selectedPaths = toggle(selectedPaths, entry.path) },
                        )
                    }
                }
            }
        }
    }

    pickerMode?.let { mode ->
        FolderPickerDialog(
            repository = fileRepository,
            rootPath = storageRootPath,
            rootLabel = storageRootLabel,
            initialPath = zipFile.parent ?: storageRootPath,
            confirmLabel = "Extract here",
            onFolderSelected = { destinationPath ->
                pickerMode = null
                val destination = File(destinationPath, zipFile.nameWithoutExtension)
                runExtraction(if (mode == PickerMode.ExtractSelected) selectedPaths else null, destination)
            },
            onDismiss = { pickerMode = null },
        )
    }

    pendingDeletePaths?.let { paths ->
        val targetsAsFileItems = paths.mapNotNull { p -> allEntries?.firstOrNull { it.path == p } }
            .map { FileItem(it.name, it.path, it.isDirectory, it.sizeBytes, it.lastModifiedMillis, false, null, true, true) }
        DeleteConfirmDialog(
            targets = targetsAsFileItems,
            onConfirm = {
                pendingDeletePaths = null
                scope.launch {
                    val result = archiveRepository.deleteEntries(zipFile, paths)
                    result.onSuccess {
                        val reload = archiveRepository.listEntries(zipFile)
                        reload.onSuccess { allEntries = it }
                        selectedPaths = emptySet()
                        message = "Removed from archive"
                    }
                    result.onFailure { message = it.message ?: "Could not update archive" }
                }
            },
            onDismiss = { pendingDeletePaths = null },
        )
    }

    activeOperation?.let { op ->
        ProgressOverlay(operation = op, onCancel = { })
    }
}

private fun toggle(set: Set<String>, value: String): Set<String> =
    if (value in set) set - value else set + value

private fun childrenOf(entries: List<ArchiveEntry>, virtualPath: String): List<ArchiveEntry> {
    val prefix = if (virtualPath.isEmpty()) "" else "$virtualPath/"
    return entries.filter { entry ->
        if (!entry.path.startsWith(prefix)) return@filter false
        val remainder = entry.path.removePrefix(prefix)
        remainder.isNotEmpty() && !remainder.contains('/')
    }.sortedWith(compareByDescending<ArchiveEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
}

private fun buildVirtualBreadcrumbs(archiveName: String, virtualPath: String): List<Breadcrumb> {
    val crumbs = mutableListOf(Breadcrumb(archiveName, ""))
    if (virtualPath.isEmpty()) return crumbs
    var accumulated = ""
    virtualPath.split("/").forEach { segment ->
        accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
        crumbs += Breadcrumb(segment, accumulated)
    }
    return crumbs
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
            Icon(
                imageVector = when {
                    isSelected -> Icons.Filled.CheckCircle
                    entry.isDirectory -> Icons.Filled.Folder
                    else -> Icons.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Box(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!entry.isDirectory) {
                Text(
                    text = formatFileSize(entry.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
