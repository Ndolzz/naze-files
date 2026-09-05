package com.naze.files.data.model

/**
 * A single file or folder entry backed by a real path on device storage.
 * This is never synthesized — every instance corresponds to an actual
 * java.io.File that exists at the time it was read.
 */
data class FileItem(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val isHidden: Boolean,
    val mimeType: String?,
    val canRead: Boolean,
    val canWrite: Boolean,
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', missingDelimiterValue = "")

    val displayNameWithoutExtension: String
        get() = if (isDirectory || extension.isEmpty()) name else name.removeSuffix(".$extension")

    val category: FileCategory
        get() = FileCategory.fromItem(this)
}

enum class FileCategory {
    FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, CODE, OTHER;

    companion object {
        private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg")
        private val videoExt = setOf("mp4", "mkv", "webm", "3gp", "avi", "mov")
        private val audioExt = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus")
        private val documentExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt")
        private val archiveExt = setOf("zip", "7z", "tar", "gz", "rar", "bz2", "xz")
        private val codeExt = setOf(
            "html", "htm", "css", "js", "ts", "json", "xml", "kt", "java", "py",
            "c", "cpp", "h", "yaml", "yml", "csv", "md", "txt", "log", "ini", "conf", "sh", "gradle"
        )

        fun fromItem(item: FileItem): FileCategory {
            if (item.isDirectory) return FOLDER
            return when (item.extension.lowercase()) {
                in imageExt -> IMAGE
                in videoExt -> VIDEO
                in audioExt -> AUDIO
                in documentExt -> DOCUMENT
                in archiveExt -> ARCHIVE
                "apk" -> APK
                in codeExt -> CODE
                else -> OTHER
            }
        }
    }
}
