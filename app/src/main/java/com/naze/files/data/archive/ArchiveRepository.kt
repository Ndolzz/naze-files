package com.naze.files.data.archive

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveRepository {

    suspend fun listEntries(zipFile: File): Result<List<ArchiveEntry>> = ioResult {
        val entries = mutableListOf<ArchiveEntry>()
        val seenDirs = mutableSetOf<String>()

        ZipFile(zipFile).use { zf ->
            val enumeration = zf.entries()
            while (enumeration.hasMoreElements()) {
                val e = enumeration.nextElement()
                val normalized = e.name.trimEnd('/')
                if (normalized.isEmpty()) continue

                if (e.isDirectory) {
                    if (seenDirs.add(normalized)) {
                        entries += ArchiveEntry(normalized, normalized.substringAfterLast('/'), true, 0, 0, e.time)
                    }
                } else {
                    entries += ArchiveEntry(normalized, normalized.substringAfterLast('/'), false, e.size, e.compressedSize, e.time)
                }

                // Synthesize any parent folders the zip didn't list explicitly.
                var parent = normalized.substringBeforeLast('/', "")
                while (parent.isNotEmpty()) {
                    if (!seenDirs.add(parent)) break
                    entries += ArchiveEntry(parent, parent.substringAfterLast('/'), true, 0, 0, e.time)
                    parent = parent.substringBeforeLast('/', "")
                }
            }
        }
        entries
    }

    /** [entryPaths] null means extract everything. Every output path is verified to stay inside [destinationDir]. */
    suspend fun extract(
        zipFile: File,
        entryPaths: Set<String>?,
        destinationDir: File,
        onProgress: suspend (currentName: String, processed: Int, total: Int) -> Unit,
    ): Result<Unit> = ioResult {
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw IOException("Could not create \"${destinationDir.name}\"")
        }
        val destCanonical = destinationDir.canonicalFile

        ZipFile(zipFile).use { zf ->
            val toExtract = mutableListOf<ZipEntry>()
            val enumeration = zf.entries()
            while (enumeration.hasMoreElements()) {
                val e = enumeration.nextElement()
                val normalized = e.name.trimEnd('/')
                val matches = entryPaths == null ||
                    normalized in entryPaths ||
                    entryPaths.any { normalized.startsWith("$it/") }
                if (matches) toExtract += e
            }

            val total = toExtract.size
            toExtract.forEachIndexed { index, entry ->
                currentCoroutineContext().ensureActive()
                val outFile = File(destinationDir, entry.name)
                val outCanonical = outFile.canonicalFile
                if (outCanonical != destCanonical && !outCanonical.path.startsWith(destCanonical.path + File.separator)) {
                    throw SecurityException("Refusing to extract \"${entry.name}\" - it would write outside the destination folder")
                }
                if (entry.isDirectory) {
                    outCanonical.mkdirs()
                } else {
                    outCanonical.parentFile?.mkdirs()
                    zf.getInputStream(entry).use { input ->
                        FileOutputStream(outCanonical).use { output ->
                            input.copyTo(output, bufferSize = 256 * 1024)
                        }
                    }
                }
                onProgress(entry.name, index + 1, total)
            }
        }
    }

    suspend fun createZip(
        sources: List<File>,
        destinationZip: File,
        compressionLevel: Int,
        onProgress: suspend (currentName: String, processed: Int, total: Int) -> Unit,
    ): Result<Unit> = ioResult {
        data class Task(val file: File, val entryName: String, val isDir: Boolean)

        val tasks = mutableListOf<Task>()
        fun collect(file: File, relativePath: String) {
            if (file.isDirectory) {
                val children = file.listFiles()
                if (children.isNullOrEmpty()) {
                    tasks += Task(file, "$relativePath/", true)
                } else {
                    children.forEach { child -> collect(child, "$relativePath/${child.name}") }
                }
            } else {
                tasks += Task(file, relativePath, false)
            }
        }
        sources.forEach { collect(it, it.name) }

        ZipOutputStream(FileOutputStream(destinationZip)).use { zos ->
            zos.setLevel(compressionLevel)
            val total = tasks.size
            tasks.forEachIndexed { index, task ->
                currentCoroutineContext().ensureActive()
                zos.putNextEntry(ZipEntry(task.entryName))
                if (!task.isDir) {
                    FileInputStream(task.file).use { input -> input.copyTo(zos, bufferSize = 256 * 1024) }
                }
                zos.closeEntry()
                onProgress(task.file.name, index + 1, total)
            }
        }
    }

    suspend fun deleteEntries(zipFile: File, entryPathsToDelete: Set<String>): Result<Unit> = ioResult {
        val tempFile = File(zipFile.parentFile, "${zipFile.nameWithoutExtension}_naze_tmp_${System.currentTimeMillis()}.zip")
        ZipFile(zipFile).use { zf ->
            ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                val enumeration = zf.entries()
                while (enumeration.hasMoreElements()) {
                    currentCoroutineContext().ensureActive()
                    val entry = enumeration.nextElement()
                    val normalized = entry.name.trimEnd('/')
                    val shouldDelete = normalized in entryPathsToDelete ||
                        entryPathsToDelete.any { normalized.startsWith("$it/") }
                    if (shouldDelete) continue

                    zos.putNextEntry(ZipEntry(entry.name))
                    if (!entry.isDirectory) {
                        zf.getInputStream(entry).use { it.copyTo(zos, bufferSize = 256 * 1024) }
                    }
                    zos.closeEntry()
                }
            }
        }
        if (!zipFile.delete() || !tempFile.renameTo(zipFile)) {
            tempFile.delete()
            throw IOException("Could not update the archive")
        }
    }

    suspend fun addFiles(zipFile: File, filesToAdd: List<File>): Result<Unit> = ioResult {
        val tempFile = File(zipFile.parentFile, "${zipFile.nameWithoutExtension}_naze_tmp_${System.currentTimeMillis()}.zip")
        val existingNames = mutableSetOf<String>()

        ZipFile(zipFile).use { zf ->
            ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                val enumeration = zf.entries()
                while (enumeration.hasMoreElements()) {
                    currentCoroutineContext().ensureActive()
                    val entry = enumeration.nextElement()
                    existingNames += entry.name.trimEnd('/')
                    zos.putNextEntry(ZipEntry(entry.name))
                    if (!entry.isDirectory) {
                        zf.getInputStream(entry).use { it.copyTo(zos, bufferSize = 256 * 1024) }
                    }
                    zos.closeEntry()
                }

                fun addRecursive(file: File, relativePath: String) {
                    if (file.isDirectory) {
                        (file.listFiles() ?: emptyArray()).forEach { addRecursive(it, "$relativePath/${it.name}") }
                    } else {
                        var name = relativePath
                        var counter = 1
                        while (name in existingNames) {
                            val dot = relativePath.lastIndexOf('.')
                            name = if (dot > 0) "${relativePath.substring(0, dot)} ($counter)${relativePath.substring(dot)}" else "$relativePath ($counter)"
                            counter++
                        }
                        zos.putNextEntry(ZipEntry(name))
                        FileInputStream(file).use { it.copyTo(zos, bufferSize = 256 * 1024) }
                        zos.closeEntry()
                        existingNames += name
                    }
                }
                filesToAdd.forEach { addRecursive(it, it.name) }
            }
        }
        if (!zipFile.delete() || !tempFile.renameTo(zipFile)) {
            tempFile.delete()
            throw IOException("Could not update the archive")
        }
    }

    private suspend fun <T> ioResult(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
