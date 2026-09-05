package com.naze.files.data.repository

import android.webkit.MimeTypeMap
import com.naze.files.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FileRepositoryImpl : FileRepository {

    override suspend fun listChildren(
        directoryPath: String,
        includeHidden: Boolean,
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)

        if (!dir.exists()) {
            throw IOException("Folder not found: $directoryPath")
        }
        if (!dir.isDirectory) {
            throw IOException("Not a folder: $directoryPath")
        }
        if (!dir.canRead()) {
            throw IOException("Permission denied: $directoryPath")
        }

        // listFiles() returns null on I/O error even when isDirectory was
        // true a moment ago (e.g. an SD card was unmounted mid-read).
        val children = dir.listFiles() ?: throw IOException("Unable to read folder: $directoryPath")

        children
            .asSequence()
            .filter { includeHidden || !it.name.startsWith(".") }
            .map { it.toFileItem() }
            .toList()
    }

    override suspend fun stat(path: String): FileItem? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) null else file.toFileItem()
    }

    private fun File.toFileItem(): FileItem {
        val mime = if (isDirectory) {
            null
        } else {
            val ext = name.substringAfterLast('.', "").lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
        return FileItem(
            name = name,
            absolutePath = absolutePath,
            isDirectory = isDirectory,
            sizeBytes = if (isDirectory) 0L else length(),
            lastModifiedMillis = lastModified(),
            isHidden = name.startsWith("."),
            mimeType = mime,
            canRead = canRead(),
            canWrite = canWrite(),
        )
    }
}
