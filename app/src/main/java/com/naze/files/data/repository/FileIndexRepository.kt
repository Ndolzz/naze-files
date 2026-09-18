package com.naze.files.data.repository

import com.naze.files.data.model.FileItem
import com.naze.files.util.resolveMimeType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Shared, cached recursive index of a storage root. */
object FileIndexRepository {

    private const val CACHE_TTL_MILLIS = 60_000L

    data class IndexResult(val files: List<FileItem>, val builtAtMillis: Long)

    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, IndexResult>()

    fun peekCache(rootPath: String): List<FileItem>? = cache[rootPath]?.files

    suspend fun getIndex(rootPath: String, forceRefresh: Boolean = false): IndexResult {
        if (!forceRefresh) {
            cache[rootPath]?.let { cached ->
                if (System.currentTimeMillis() - cached.builtAtMillis < CACHE_TTL_MILLIS) return cached
            }
        }
        return mutex.withLock {
            if (!forceRefresh) {
                cache[rootPath]?.let { cached ->
                    if (System.currentTimeMillis() - cached.builtAtMillis < CACHE_TTL_MILLIS) return@withLock cached
                }
            }
            withContext(Dispatchers.IO) {
                val result = scan(rootPath)
                cache[rootPath] = result
                result
            }
        }
    }

    fun invalidate(rootPath: String? = null) {
        if (rootPath == null) cache.clear() else cache.remove(rootPath)
    }

    private suspend fun scan(rootPath: String): IndexResult {
        val root = File(rootPath)
        if (!root.exists()) throw IOException("Storage root not found: $rootPath")
        if (!root.canRead()) throw IOException("Permission denied: $rootPath")

        val found = mutableListOf<FileItem>()
        val visitedDirs = HashSet<String>()

        suspend fun walk(dir: File) {
            currentCoroutineContext().ensureActive()
            if (dir.name == ".naze_trash") return

            val canonical = try {
                dir.canonicalPath
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return
            }
            if (!visitedDirs.add(canonical)) return

            val children = try {
                dir.listFiles()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                null
            } ?: return

            for (child in children) {
                currentCoroutineContext().ensureActive()
                try {
                    if (child.isDirectory) {
                        walk(child)
                    } else {
                        found += FileItem(
                            name = child.name,
                            absolutePath = child.absolutePath,
                            isDirectory = false,
                            sizeBytes = child.length(),
                            lastModifiedMillis = child.lastModified(),
                            isHidden = child.name.startsWith("."),
                            mimeType = resolveMimeType(child.name.substringAfterLast('.', "")),
                            canRead = child.canRead(),
                            canWrite = child.canWrite(),
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    continue
                }
            }
        }

        walk(root)
        return IndexResult(files = found, builtAtMillis = System.currentTimeMillis())
    }
}
