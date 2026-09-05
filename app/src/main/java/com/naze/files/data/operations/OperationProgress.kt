package com.naze.files.data.operations

data class OperationProgress(
    val currentFileName: String,
    val processedBytes: Long,
    val totalBytes: Long,
    val processedItems: Int,
    val totalItems: Int,
) {
    val percent: Int
        get() = if (totalBytes <= 0) 0 else ((processedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
}
