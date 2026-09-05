package com.naze.files.data.viewer

import com.naze.files.data.model.FileCategory
import com.naze.files.data.model.FileItem
import com.naze.files.util.BinaryDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class ViewerRoute {
    data class Image(val item: FileItem) : ViewerRoute()
    data class TextCode(val item: FileItem) : ViewerRoute()
    data class Pdf(val item: FileItem) : ViewerRoute()
    data class Audio(val item: FileItem) : ViewerRoute()
    data class Video(val item: FileItem) : ViewerRoute()
    data class Archive(val item: FileItem) : ViewerRoute()
    data class Unsupported(val item: FileItem, val reason: String) : ViewerRoute()
}

/**
 * Decides which viewer opens for a file. Never falls through to displaying
 * a binary file as text - unknown or not-yet-implemented types land on
 * [ViewerRoute.Unsupported], which the UI backs with File Information and
 * Open With, exactly as the spec's mapping table requires.
 */
object ViewerRouter {

    suspend fun route(item: FileItem): ViewerRoute {
        if (item.isDirectory) {
            return ViewerRoute.Unsupported(item, "This is a folder")
        }
        return when (item.category) {
            FileCategory.IMAGE -> ViewerRoute.Image(item)
            FileCategory.AUDIO -> ViewerRoute.Audio(item)
            FileCategory.VIDEO -> ViewerRoute.Video(item)

            FileCategory.CODE -> withContext(Dispatchers.IO) {
                if (BinaryDetector.isLikelyBinary(File(item.absolutePath))) {
                    ViewerRoute.Unsupported(item, "This file appears to contain binary data and can't be shown as text.")
                } else {
                    ViewerRoute.TextCode(item)
                }
            }

            FileCategory.OTHER -> withContext(Dispatchers.IO) {
                if (item.sizeBytes == 0L || !BinaryDetector.isLikelyBinary(File(item.absolutePath))) {
                    ViewerRoute.TextCode(item)
                } else {
                    ViewerRoute.Unsupported(item, "No viewer is available yet for this file type.")
                }
            }

            FileCategory.DOCUMENT -> if (item.extension.equals("pdf", ignoreCase = true)) {
                ViewerRoute.Pdf(item)
            } else {
                ViewerRoute.Unsupported(item, "Viewing Office documents (${item.extension.uppercase()}) isn't available yet - use Open With.")
            }

            FileCategory.ARCHIVE -> if (item.extension.equals("zip", ignoreCase = true)) {
                ViewerRoute.Archive(item)
            } else {
                ViewerRoute.Unsupported(item, "Only ZIP archives can be browsed in-app right now - use Open With for other archive formats.")
            }
            FileCategory.APK -> ViewerRoute.Unsupported(item, "APK details aren't available yet.")
            FileCategory.FOLDER -> ViewerRoute.Unsupported(item, "This is a folder")
        }
    }
}
