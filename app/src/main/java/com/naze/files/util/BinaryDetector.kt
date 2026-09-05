package com.naze.files.util

import java.io.File
import java.io.FileInputStream

/**
 * Naze Files never opens a binary file in the text/code viewer. This does a
 * cheap heuristic check on the first few KB — a NUL byte, or a high enough
 * ratio of non-printable bytes, marks the file as binary — rather than
 * trusting the extension alone.
 */
object BinaryDetector {

    private const val SNIFF_BYTES = 8000

    fun isLikelyBinary(file: File): Boolean {
        if (!file.exists() || !file.isFile) return true
        return try {
            FileInputStream(file).use { stream ->
                val buffer = ByteArray(minOf(SNIFF_BYTES, file.length().toInt().coerceAtLeast(0)))
                val read = stream.read(buffer)
                if (read <= 0) return false // empty file is fine to view as text

                var suspicious = 0
                for (i in 0 until read) {
                    val b = buffer[i].toInt() and 0xFF
                    if (b == 0) return true // NUL byte - definitely binary
                    val isPrintableOrWhitespace = b in 0x09..0x0D || b in 0x20..0x7E || b >= 0x80
                    if (!isPrintableOrWhitespace) suspicious++
                }
                (suspicious.toDouble() / read) > 0.05
            }
        } catch (e: Exception) {
            true // can't verify safely - treat as binary and let Information/Open with handle it
        }
    }
}
