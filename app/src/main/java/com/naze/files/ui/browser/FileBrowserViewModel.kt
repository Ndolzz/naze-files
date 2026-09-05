package com.naze.files.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.files.data.archive.ArchiveRepository
import com.naze.files.data.favorites.FavoritesRepository
import com.naze.files.data.model.FileItem
import com.naze.files.data.model.SortOrder
import com.naze.files.data.model.ViewMode
import com.naze.files.data.model.sortedWith
import com.naze.files.data.operations.ConflictResolution
import com.naze.files.data.operations.ConflictResolver
import com.naze.files.data.operations.FileOperationsRepository
import com.naze.files.data.operations.OperationProgress
import com.naze.files.data.repository.FileRepository
import com.naze.files.data.settings.SettingsRepository
import com.naze.files.data.trash.TrashRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FileBrowserViewModel(
    private val repository: FileRepository,
    private val operationsRepository: FileOperationsRepository,
    private val trashRepository: TrashRepository,
    private val favoritesRepository: FavoritesRepository,
    private val archiveRepository: ArchiveRepository,
    private val settingsRepository: SettingsRepository,
    private val rootPath: String,
    private val rootLabel: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState(currentPath = rootPath))
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var pendingConflictDeferred: CompletableDeferred<ConflictResolution>? = null

    private val conflictResolver = ConflictResolver { fileName ->
        val deferred = CompletableDeferred<ConflictResolution>()
        pendingConflictDeferred = deferred
        _uiState.update { it.copy(pendingConflictFileName = fileName) }
        val result = deferred.await()
        _uiState.update { it.copy(pendingConflictFileName = null) }
        result
    }

    init {
        viewModelScope.launch {
            val initial = runCatching { settingsRepository.settings.first() }.getOrNull()
            if (initial != null) {
                _uiState.update {
                    it.copy(
                        sortPreference = it.sortPreference.copy(order = initial.defaultSortOrder, foldersFirst = initial.foldersFirst),
                        viewMode = initial.defaultViewMode,
                        showHiddenFiles = initial.showHiddenFiles,
                        showFileExtensions = initial.showFileExtensions,
                    )
                }
            }
            openFolder(rootPath)
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update { it.copy(showFileExtensions = s.showFileExtensions, confirmDeleteEnabled = s.confirmDelete) }
            }
        }
        viewModelScope.launch {
            favoritesRepository.favoritePaths.collect { paths ->
                _uiState.update { it.copy(favoritePaths = paths) }
            }
        }
    }

    // ---- navigation ----

    fun openFolder(path: String) {
        _uiState.update {
            it.copy(
                currentPath = path,
                breadcrumbs = buildBreadcrumbs(path),
                isLoading = true,
                errorMessage = null,
                searchQuery = "",
                selectedPaths = emptySet(),
            )
        }
        load(path)
    }

    fun navigateUp(): Boolean {
        val current = _uiState.value.currentPath
        if (current == rootPath) return false
        val parent = File(current).parent ?: return false
        if (!parent.startsWith(rootPath)) return false
        openFolder(parent)
        return true
    }

    fun refresh() = load(_uiState.value.currentPath)

    private fun load(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val children = repository.listChildren(path, _uiState.value.showHiddenFiles)
                _uiState.update {
                    it.copy(items = children.sortedWith(it.sortPreference), isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(items = emptyList(), isLoading = false, errorMessage = e.message ?: "Unable to load this folder.")
                }
            }
        }
    }

    // ---- sort / view / search ----

    fun setSortOrder(order: SortOrder) {
        _uiState.update {
            val newPref = it.sortPreference.copy(order = order)
            it.copy(sortPreference = newPref, items = it.items.sortedWith(newPref))
        }
        viewModelScope.launch { settingsRepository.setDefaultSortOrder(order) }
    }

    fun toggleFoldersFirst() {
        val newValue = !_uiState.value.sortPreference.foldersFirst
        _uiState.update {
            val newPref = it.sortPreference.copy(foldersFirst = newValue)
            it.copy(sortPreference = newPref, items = it.items.sortedWith(newPref))
        }
        viewModelScope.launch { settingsRepository.setFoldersFirst(newValue) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        viewModelScope.launch { settingsRepository.setDefaultViewMode(mode) }
    }

    fun toggleShowHidden() {
        val newValue = !_uiState.value.showHiddenFiles
        _uiState.update { it.copy(showHiddenFiles = newValue) }
        viewModelScope.launch { settingsRepository.setShowHiddenFiles(newValue) }
        refresh()
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    // ---- selection ----

    fun toggleSelect(path: String) {
        _uiState.update {
            val current = it.selectedPaths
            it.copy(selectedPaths = if (path in current) current - path else current + path)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedPaths = emptySet()) }

    fun selectAll() {
        _uiState.update { it.copy(selectedPaths = it.visibleItems.map { i -> i.absolutePath }.toSet()) }
    }

    // ---- context menu ----

    fun openContextMenu(item: FileItem) = _uiState.update { it.copy(contextMenuTarget = item) }

    fun closeContextMenu() = _uiState.update { it.copy(contextMenuTarget = null) }

    // ---- clipboard (copy/cut/paste) ----

    fun copySelectedToClipboard() {
        val items = _uiState.value.selectedItems
        _uiState.update { it.copy(clipboard = ClipboardState(items, ClipboardOperation.COPY), selectedPaths = emptySet()) }
    }

    fun cutSelectedToClipboard() {
        val items = _uiState.value.selectedItems
        _uiState.update { it.copy(clipboard = ClipboardState(items, ClipboardOperation.CUT), selectedPaths = emptySet()) }
    }

    fun copySingleToClipboard(item: FileItem) {
        _uiState.update { it.copy(clipboard = ClipboardState(listOf(item), ClipboardOperation.COPY), contextMenuTarget = null) }
    }

    fun cutSingleToClipboard(item: FileItem) {
        _uiState.update { it.copy(clipboard = ClipboardState(listOf(item), ClipboardOperation.CUT), contextMenuTarget = null) }
    }

    fun clearClipboard() = _uiState.update { it.copy(clipboard = null) }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboard ?: return
        val destDir = File(_uiState.value.currentPath)
        val sources = clip.items.map { File(it.absolutePath) }
        val label = if (clip.operation == ClipboardOperation.CUT) "Moving" else "Copying"

        _uiState.update { it.copy(activeOperation = ActiveOperation(label = label)) }

        activeJob = viewModelScope.launch {
            try {
                val onProgress: suspend (OperationProgress) -> Unit = { p ->
                    _uiState.update { it.copy(activeOperation = ActiveOperation(label = label, progress = p)) }
                }
                val result = if (clip.operation == ClipboardOperation.CUT) {
                    operationsRepository.move(sources, destDir, conflictResolver, onProgress)
                } else {
                    operationsRepository.copy(sources, destDir, conflictResolver, onProgress)
                }
                result.onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Operation failed") } }
                result.onSuccess {
                    if (clip.operation == ClipboardOperation.CUT) {
                        _uiState.update { it.copy(clipboard = null) }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled by the user - fall through to the finally block below.
            } finally {
                _uiState.update { it.copy(activeOperation = null) }
                refresh()
            }
        }
    }

    fun cancelActiveOperation() {
        activeJob?.cancel()
        pendingConflictDeferred?.complete(ConflictResolution.Skip)
    }

    fun resolveConflict(resolution: ConflictResolution) {
        pendingConflictDeferred?.complete(resolution)
        pendingConflictDeferred = null
    }

    // ---- delete / trash ----

    fun requestDeleteSelected() {
        val items = _uiState.value.selectedItems
        if (items.isEmpty()) return
        if (_uiState.value.confirmDeleteEnabled) {
            _uiState.update { it.copy(deleteConfirmationTargets = items) }
        } else {
            _uiState.update { it.copy(deleteConfirmationTargets = items) }
            confirmDelete()
        }
    }

    fun requestDeleteSingle(item: FileItem) {
        val targets = listOf(item)
        if (_uiState.value.confirmDeleteEnabled) {
            _uiState.update { it.copy(deleteConfirmationTargets = targets, contextMenuTarget = null) }
        } else {
            _uiState.update { it.copy(deleteConfirmationTargets = targets, contextMenuTarget = null) }
            confirmDelete()
        }
    }

    fun cancelDelete() = _uiState.update { it.copy(deleteConfirmationTargets = null) }

    fun confirmDelete() {
        val targets = _uiState.value.deleteConfirmationTargets ?: return
        _uiState.update { it.copy(deleteConfirmationTargets = null, selectedPaths = emptySet(), activeOperation = ActiveOperation("Deleting")) }

        viewModelScope.launch {
            var trashFailures = 0
            for (item in targets) {
                val result = trashRepository.moveToTrash(File(item.absolutePath))
                if (result.isFailure) {
                    // Trash unavailable here (e.g. read-only volume) - fall back
                    // to a real, compatible permanent delete rather than failing silently.
                    val fallback = operationsRepository.deletePermanently(listOf(File(item.absolutePath)))
                    if (fallback.isFailure) trashFailures++
                }
            }
            _uiState.update {
                it.copy(
                    activeOperation = null,
                    message = if (trashFailures > 0) {
                        "Deleted, but $trashFailures item(s) could not use trash on this storage and were removed permanently."
                    } else {
                        "Moved to trash"
                    },
                )
            }
            refresh()
        }
    }

    // ---- rename ----

    fun requestRename(item: FileItem) = _uiState.update { it.copy(renameTarget = item, contextMenuTarget = null) }

    fun cancelRename() = _uiState.update { it.copy(renameTarget = null) }

    // ---- information ----

    fun requestInfo(item: FileItem) = _uiState.update { it.copy(infoTarget = item, contextMenuTarget = null) }

    fun dismissInfo() = _uiState.update { it.copy(infoTarget = null) }

    fun confirmRename(newName: String) {
        val target = _uiState.value.renameTarget ?: return
        viewModelScope.launch {
            val result = operationsRepository.rename(File(target.absolutePath), newName)
            _uiState.update {
                it.copy(
                    renameTarget = null,
                    message = result.exceptionOrNull()?.message,
                )
            }
            refresh()
        }
    }

    // ---- create folder/file ----

    fun requestCreate(request: CreateRequest) = _uiState.update { it.copy(activeCreateRequest = request) }

    fun cancelCreate() = _uiState.update { it.copy(activeCreateRequest = null) }

    fun confirmCreate(name: String) {
        val request = _uiState.value.activeCreateRequest ?: return
        val parent = File(_uiState.value.currentPath)
        viewModelScope.launch {
            val result = when (request) {
                is CreateRequest.Folder -> operationsRepository.createFolder(parent, name)
                is CreateRequest.FileOfType -> operationsRepository.createFile(parent, name)
            }
            _uiState.update {
                it.copy(
                    activeCreateRequest = null,
                    message = result.exceptionOrNull()?.message,
                )
            }
            refresh()
        }
    }

    // ---- compress ----

    fun requestCompressSelected() {
        val items = _uiState.value.selectedItems
        if (items.isNotEmpty()) {
            val suggested = if (items.size == 1) "${items[0].displayNameWithoutExtension}.zip" else "Archive.zip"
            _uiState.update { it.copy(compressTargets = items, compressSuggestedName = suggested, selectedPaths = emptySet()) }
        }
    }

    fun requestCompressSingle(item: FileItem) {
        _uiState.update {
            it.copy(
                compressTargets = listOf(item),
                compressSuggestedName = "${item.displayNameWithoutExtension}.zip",
                contextMenuTarget = null,
            )
        }
    }

    fun cancelCompress() = _uiState.update { it.copy(compressTargets = null) }

    fun confirmCompress(archiveName: String, compressionLevel: Int) {
        val targets = _uiState.value.compressTargets ?: return
        val destDir = File(_uiState.value.currentPath)
        val destZip = File(destDir, archiveName)
        _uiState.update { it.copy(compressTargets = null, activeOperation = ActiveOperation("Compressing")) }

        viewModelScope.launch {
            val sources = targets.map { File(it.absolutePath) }
            val result = archiveRepository.createZip(sources, destZip, compressionLevel) { name, processed, total ->
                _uiState.update {
                    it.copy(
                        activeOperation = ActiveOperation(
                            "Compressing",
                            com.naze.files.data.operations.OperationProgress(name, processed.toLong(), total.toLong(), processed, total),
                        ),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    activeOperation = null,
                    message = if (result.isSuccess) "Created \"$archiveName\"" else result.exceptionOrNull()?.message,
                )
            }
            refresh()
        }
    }

    // ---- favorites ----

    fun toggleFavorite(item: FileItem) {
        viewModelScope.launch { favoritesRepository.toggle(item.absolutePath) }
        _uiState.update { it.copy(contextMenuTarget = null) }
    }

    // ---- share ----

    fun requestShareSelected() {
        val items = _uiState.value.selectedItems
        if (items.isNotEmpty()) _uiState.update { it.copy(pendingShare = items, selectedPaths = emptySet()) }
    }

    fun requestShareSingle(item: FileItem) {
        _uiState.update { it.copy(pendingShare = listOf(item), contextMenuTarget = null) }
    }

    fun consumeShareRequest() = _uiState.update { it.copy(pendingShare = null) }

    // ---- one-shot messages ----

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    // ---- breadcrumbs ----

    private fun buildBreadcrumbs(path: String): List<Breadcrumb> {
        val crumbs = mutableListOf(Breadcrumb(rootLabel, rootPath))
        if (path == rootPath) return crumbs

        val relative = path.removePrefix(rootPath).trim('/')
        if (relative.isEmpty()) return crumbs

        var accumulated = rootPath
        relative.split("/").forEach { segment ->
            accumulated = "$accumulated/$segment"
            crumbs += Breadcrumb(segment, accumulated)
        }
        return crumbs
    }

    class Factory(
        private val repository: FileRepository,
        private val operationsRepository: FileOperationsRepository,
        private val trashRepository: TrashRepository,
        private val favoritesRepository: FavoritesRepository,
        private val archiveRepository: ArchiveRepository,
        private val settingsRepository: SettingsRepository,
        private val rootPath: String,
        private val rootLabel: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileBrowserViewModel(
                repository, operationsRepository, trashRepository, favoritesRepository,
                archiveRepository, settingsRepository, rootPath, rootLabel,
            ) as T
        }
    }
}
