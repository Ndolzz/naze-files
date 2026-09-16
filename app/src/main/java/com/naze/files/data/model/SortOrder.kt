package com.naze.files.data.model

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST, TYPE
}

data class SortPreference(
    val order: SortOrder = SortOrder.NAME_ASC,
    val foldersFirst: Boolean = true,
)

/**
 * Applies the user's sort preference to a list of [FileItem]s. Folder-first
 * grouping is layered on top of whichever comparator the order implies, so
 * "Folders first" behaves consistently no matter which column is sorted.
 *
 * Note: explicit <FileItem, String> type arguments are required on the
 * compareBy calls whose result is chained (.reversed() / .thenBy()) —
 * otherwise the compiler cannot infer T through the call chain.
 */
fun List<FileItem>.sortedWith(preference: SortPreference): List<FileItem> {
    val nameComparator: Comparator<FileItem> =
        compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }

    val baseComparator: Comparator<FileItem> = when (preference.order) {
        SortOrder.NAME_ASC -> nameComparator
        SortOrder.NAME_DESC -> nameComparator.reversed()
        SortOrder.DATE_NEWEST -> compareByDescending { it.lastModifiedMillis }
        SortOrder.DATE_OLDEST -> compareBy { it.lastModifiedMillis }
        SortOrder.SIZE_LARGEST -> compareByDescending { it.sizeBytes }
        SortOrder.SIZE_SMALLEST -> compareBy { it.sizeBytes }
        SortOrder.TYPE -> compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) { it.extension }
            .thenBy { it.name }
    }

    val comparator = if (preference.foldersFirst) {
        compareByDescending<FileItem> { it.isDirectory }.then(baseComparator)
    } else {
        baseComparator
    }

    return sortedWith(comparator)
}
