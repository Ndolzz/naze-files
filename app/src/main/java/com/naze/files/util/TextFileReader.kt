package com.naze.files.util

import java.io.File
import java.io.FileInputStream

data class TextFileContent(val text: String, val truncated: Boolean)

object TextFileReader {

    /** Above this, syntax highlighting is skipped to keep scrolling smooth. */
    const val HIGHLIGHT_SIZE_LIMIT = 300 * 1024L

    /** Above this, only the first chunk is read into memory at all. */
    private const val MAX_READ_BYTES = 5 * 1024 * 1024

    fun read(file: File): TextFileContent {
        val length = file.length()
        val readLimit = minOf(length, MAX_READ_BYTES.toLong()).toInt()
        val buffer = ByteArray(readLimit)
        FileInputStream(file).use { it.read(buffer) }
        val text = String(buffer, Charsets.UTF_8)
        return TextFileContent(text = text, truncated = length > MAX_READ_BYTES)
    }

    fun write(file: File, text: String) {
        file.writeText(text, Charsets.UTF_8)
    }
}
