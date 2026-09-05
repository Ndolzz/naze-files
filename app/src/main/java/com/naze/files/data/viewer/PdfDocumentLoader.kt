package com.naze.files.data.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class PdfPageSize(val width: Int, val height: Int)

/**
 * android.graphics.pdf.PdfRenderer is built into Android (API 21+) - no
 * third-party PDF library needed. It only allows one page open at a time,
 * so every render call is serialized through [mutex].
 */
class PdfDocumentLoader(private val file: File) {

    private val mutex = Mutex()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    var pageCount: Int = 0
        private set

    suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = descriptor
            val r = PdfRenderer(descriptor)
            renderer = r
            pageCount = r.pageCount
        }
    }

    suspend fun pageSize(index: Int): PdfPageSize = mutex.withLock {
        withContext(Dispatchers.IO) {
            val r = renderer ?: throw IllegalStateException("Document not open")
            r.openPage(index).use { page -> PdfPageSize(page.width, page.height) }
        }
    }

    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            val r = renderer ?: throw IllegalStateException("Document not open")
            r.openPage(index).use { page ->
                val safeWidth = targetWidthPx.coerceAtLeast(1)
                val scale = safeWidth.toFloat() / page.width
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(safeWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    fun close() {
        try {
            renderer?.close()
            pfd?.close()
        } catch (e: Exception) {
            // Already closed or the file changed underneath us - nothing more to do.
        }
    }
}
