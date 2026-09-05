package com.naze.files.data.trash

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Android has no OS-level trash for arbitrary files (MediaStore's trash only
 * covers media items). Naze Files implements its own: deleted items move
 * into a hidden ".naze_trash" folder at the root of the same storage volume,
 * alongside a JSON index recording where each one came from. If that folder
 * can't be created (e.g. a read-only or exotic volume), callers should fall
 * back to [com.naze.files.data.operations.FileOperationsRepository.deletePermanently]
 * and tell the person plainly that trash wasn't available for that location.
 */
class TrashRepository(rootPath: String) {

    private val trashDir = File(rootPath, ".naze_trash")
    private val indexFile = File(trashDir, "index.json")

    suspend fun moveToTrash(file: File): Result<Unit> = ioResult {
        if (!trashDir.exists() && !trashDir.mkdirs()) {
            throw IOException("Trash is not available on this storage")
        }
        val id = UUID.randomUUID().toString()
        val target = File(trashDir, "${id}_${file.name}")
        if (!file.renameTo(target)) {
            copyThenDelete(file, target)
        }
        val entries = readIndex().toMutableList()
        entries += TrashEntry(
            id = id,
            originalPath = file.absolutePath,
            originalName = file.name,
            trashPath = target.absolutePath,
            trashedAtMillis = System.currentTimeMillis(),
            isDirectory = target.isDirectory,
        )
        writeIndex(entries)
    }

    suspend fun listEntries(): List<TrashEntry> = withContext(Dispatchers.IO) { readIndex() }

    suspend fun restore(entry: TrashEntry): Result<Unit> = ioResult {
        val parent = File(entry.originalPath).parentFile
            ?: throw IOException("Original location is no longer available")
        if (!parent.exists()) throw IOException("Original folder no longer exists")
        val dest = File(entry.originalPath)
        if (dest.exists()) throw IOException("A file already exists at the original location")
        val src = File(entry.trashPath)
        if (!src.exists()) throw IOException("This item is no longer in the trash")
        if (!src.renameTo(dest)) {
            copyThenDelete(src, dest)
        }
        writeIndex(readIndex().filterNot { it.id == entry.id })
    }

    suspend fun deleteForever(entry: TrashEntry): Result<Unit> = ioResult {
        val target = File(entry.trashPath)
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Could not permanently delete \"${entry.originalName}\"")
        }
        writeIndex(readIndex().filterNot { it.id == entry.id })
    }

    suspend fun emptyTrash(): Result<Unit> = ioResult {
        if (trashDir.exists() && !trashDir.deleteRecursively()) {
            throw IOException("Could not empty trash")
        }
    }

    private fun copyThenDelete(src: File, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            (src.listFiles() ?: emptyArray()).forEach { child ->
                copyThenDelete(child, File(dest, child.name))
            }
        } else {
            src.copyTo(dest, overwrite = true)
        }
        if (src.isFile) src.delete()
    }

    private fun readIndex(): List<TrashEntry> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val array = JSONArray(indexFile.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                TrashEntry(
                    id = o.getString("id"),
                    originalPath = o.getString("originalPath"),
                    originalName = o.getString("originalName"),
                    trashPath = o.getString("trashPath"),
                    trashedAtMillis = o.getLong("trashedAtMillis"),
                    isDirectory = o.getBoolean("isDirectory"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeIndex(entries: List<TrashEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("originalPath", entry.originalPath)
                    put("originalName", entry.originalName)
                    put("trashPath", entry.trashPath)
                    put("trashedAtMillis", entry.trashedAtMillis)
                    put("isDirectory", entry.isDirectory)
                },
            )
        }
        trashDir.mkdirs()
        indexFile.writeText(array.toString())
    }

    private suspend fun <T> ioResult(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
