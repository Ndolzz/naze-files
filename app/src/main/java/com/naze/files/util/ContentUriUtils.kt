package com.naze.files.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object ContentUriUtils {

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return File(uri.path ?: "").name
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            } ?: uri.lastPathSegment ?: "file"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "file"
        }
    }

    /** Streams the content behind [uri] into [destFile], overwriting it if present. */
    fun copyToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output, bufferSize = 256 * 1024) }
            } != null
        } catch (e: Exception) {
            false
        }
    }

    /** For viewing an incoming file: copies into the app cache so the existing File-based viewers can open it. */
    fun copyToCacheForViewing(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            val direct = uri.path?.let { File(it) }
            if (direct != null && direct.exists()) return direct
        }
        val name = displayName(context, uri)
        val dir = File(context.cacheDir, "incoming").apply { mkdirs() }
        val dest = File(dir, name)
        return if (copyToFile(context, uri, dest)) dest else null
    }
}
