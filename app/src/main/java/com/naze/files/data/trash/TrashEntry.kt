package com.naze.files.data.trash

data class TrashEntry(
    val id: String,
    val originalPath: String,
    val originalName: String,
    val trashPath: String,
    val trashedAtMillis: Long,
    val isDirectory: Boolean,
)
