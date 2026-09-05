package com.naze.files.data.operations

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Performs real file operations against java.io.File — every byte copied is
 * streamed (never fully buffered in memory), every operation is cooperatively
 * cancellable, and every naming conflict is resolved by asking the caller's
 * [ConflictResolver] rather than overwriting silently.
 */
class FileOperationsRepository {

    suspend fun copy(
        sources: List<File>,
        destinationDir: File,
        conflictResolver: ConflictResolver,
        onProgress: suspend (OperationProgress) -> Unit,
    ): Result<Unit> = ioResult {
        val tracker = ProgressTracker(
            totalBytes = sources.sumOf { sizeOf(it) },
            totalItems = sources.sumOf { countItems(it) },
            onProgress = onProgress,
        )
        for (src in sources) {
            currentCoroutineContext().ensureActive()
            val dest = resolveTarget(destinationDir, src.name, conflictResolver) ?: continue
            copyRecursive(src, dest, conflictResolver, tracker)
        }
    }

    suspend fun move(
        sources: List<File>,
        destinationDir: File,
        conflictResolver: ConflictResolver,
        onProgress: suspend (OperationProgress) -> Unit,
    ): Result<Unit> = ioResult {
        val tracker = ProgressTracker(
            totalBytes = sources.sumOf { sizeOf(it) },
            totalItems = sources.sumOf { countItems(it) },
            onProgress = onProgress,
        )
        for (src in sources) {
            currentCoroutineContext().ensureActive()
            val dest = resolveTarget(destinationDir, src.name, conflictResolver) ?: continue
            moveRecursive(src, dest, conflictResolver, tracker)
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = ioResult {
        val target = File(file.parentFile, newName)
        if (target.exists()) throw IOException("\"$newName\" already exists here")
        if (!file.renameTo(target)) throw IOException("Could not rename \"${file.name}\"")
        target
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = ioResult {
        val target = File(parent, name)
        if (target.exists()) throw IOException("\"$name\" already exists here")
        if (!target.mkdir()) throw IOException("Could not create folder \"$name\"")
        target
    }

    suspend fun createFile(parent: File, name: String): Result<File> = ioResult {
        val target = File(parent, name)
        if (target.exists()) throw IOException("\"$name\" already exists here")
        if (!target.createNewFile()) throw IOException("Could not create file \"$name\"")
        target
    }

    suspend fun deletePermanently(files: List<File>): Result<Unit> = ioResult {
        files.forEach { f ->
            if (f.exists() && !f.deleteRecursively()) {
                throw IOException("Could not delete \"${f.name}\"")
            }
        }
    }

    // ---- internals ----

    private suspend fun resolveTarget(
        destinationDir: File,
        desiredName: String,
        conflictResolver: ConflictResolver,
    ): File? {
        var name = desiredName
        while (true) {
            val candidate = File(destinationDir, name)
            if (!candidate.exists()) return candidate
            when (val resolution = conflictResolver.resolve(name)) {
                is ConflictResolution.Replace -> {
                    if (!candidate.deleteRecursively()) {
                        throw IOException("Could not replace \"$name\"")
                    }
                    return candidate
                }
                is ConflictResolution.Skip -> return null
                is ConflictResolution.Rename -> name = resolution.newName
            }
        }
    }

    private suspend fun copyRecursive(
        src: File,
        dest: File,
        conflictResolver: ConflictResolver,
        tracker: ProgressTracker,
    ) {
        currentCoroutineContext().ensureActive()
        if (src.isDirectory) {
            if (!dest.exists() && !dest.mkdirs()) {
                throw IOException("Could not create folder \"${dest.name}\"")
            }
            val children = src.listFiles() ?: emptyArray()
            for (child in children) {
                val childDest = resolveTarget(dest, child.name, conflictResolver) ?: continue
                copyRecursive(child, childDest, conflictResolver, tracker)
            }
            tracker.itemDone(src.name)
        } else {
            copyFileStreaming(src, dest, tracker)
            tracker.itemDone(src.name)
        }
    }

    private suspend fun moveRecursive(
        src: File,
        dest: File,
        conflictResolver: ConflictResolver,
        tracker: ProgressTracker,
    ) {
        currentCoroutineContext().ensureActive()
        // Fast path: same-filesystem rename is atomic and typically instant.
        if (src.renameTo(dest)) {
            tracker.completeSubtree(sizeOf(dest), countItems(dest), src.name)
            return
        }
        // Cross-filesystem fallback (e.g. internal storage -> SD card): copy then delete.
        copyRecursive(src, dest, conflictResolver, tracker)
        if (!src.deleteRecursively()) {
            throw IOException("Copied \"${src.name}\" but could not remove the original")
        }
    }

    private suspend fun copyFileStreaming(src: File, dest: File, tracker: ProgressTracker) {
        FileInputStream(src).use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(256 * 1024)
                var read = input.read(buffer)
                while (read >= 0) {
                    currentCoroutineContext().ensureActive()
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        tracker.addBytes(read.toLong(), src.name)
                    }
                    read = input.read(buffer)
                }
            }
        }
        dest.setLastModified(src.lastModified())
    }

    private fun sizeOf(file: File): Long = if (file.isDirectory) {
        (file.listFiles() ?: emptyArray()).sumOf { sizeOf(it) }
    } else {
        file.length()
    }

    private fun countItems(file: File): Int = if (file.isDirectory) {
        1 + (file.listFiles() ?: emptyArray()).sumOf { countItems(it) }
    } else {
        1
    }

    private suspend fun <T> ioResult(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e // never swallow cancellation - let structured concurrency propagate it
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private class ProgressTracker(
    private val totalBytes: Long,
    private val totalItems: Int,
    private val onProgress: suspend (OperationProgress) -> Unit,
) {
    private var processedBytes = 0L
    private var processedItems = 0
    private var bytesSinceLastReport = 0L
    private val reportThresholdBytes = 512L * 1024

    suspend fun addBytes(bytes: Long, fileName: String) {
        processedBytes += bytes
        bytesSinceLastReport += bytes
        if (bytesSinceLastReport >= reportThresholdBytes) {
            bytesSinceLastReport = 0
            report(fileName)
        }
    }

    suspend fun itemDone(fileName: String) {
        processedItems += 1
        report(fileName)
    }

    suspend fun completeSubtree(bytes: Long, items: Int, fileName: String) {
        processedBytes += bytes
        processedItems += items
        report(fileName)
    }

    private suspend fun report(fileName: String) {
        onProgress(OperationProgress(fileName, processedBytes, totalBytes, processedItems, totalItems))
    }
}
