package com.naze.files.data.archive

data class ArchiveEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val compressedSizeBytes: Long,
    val lastModifiedMillis: Long,
)
