package com.naze.files.ui.browser

import com.naze.files.data.model.FileItem
import com.naze.files.data.model.SortPreference
import com.naze.files.data.model.ViewMode
import com.naze.files.data.operations.OperationProgress

/** One crumb in the breadcrumb bar, e.g. "Internal Storage / Download". */
data class Breadcrumb(val label: String, val path: String)

enum class ClipboardOperation { COPY, CUT }

data class ClipboardState(val items: List<FileItem>, val operation: ClipboardOperation)

/** What kind of new item the create sheet is currently building. */
sealed class CreateRequest {
    data object Folder : CreateRequest()
    data class FileOfType(val label: String, val extension: String?) : CreateRequest()
}

data class ActiveOperation(
    val label: String,
    val progress: OperationProgress? = null,
)

data class FileBrowserUiState(
    val currentPath: String = "",
    val breadcrumbs: List<Breadcrumb> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sortPreference: SortPreference = SortPreference(),
    val viewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val searchQuery: String = "",
    val selectedPaths: Set<String> = emptySet(),
    val favoritePaths: Set<String> = emptySet(),
    val clipboard: ClipboardState? = null,
    val contextMenuTarget: FileItem? = null,
    val renameTarget: FileItem? = null,
    val infoTarget: FileItem? = null,
    val deleteConfirmationTargets: List<FileItem>? = null,
    val pendingConflictFileName: String? = null,
    val activeOperation: ActiveOperation? = null,
    val activeCreateRequest: CreateRequest? = null,
    val compressTargets: List<FileItem>? = null,
    val compressSuggestedName: String = "",
    val confirmDeleteEnabled: Boolean = true,
    val pendingShare: List<FileItem>? = null,
    val message: String? = null,
) {
    val isSelectionMode: Boolean get() = selectedPaths.isNotEmpty()

    val visibleItems: List<FileItem>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    val selectedItems: List<FileItem>
        get() = items.filter { it.absolutePath in selectedPaths }
}
