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

/**
 * Builds ONE recursive index of a storage root and shares it across every
 * screen that needs "all files under this root" — category browsing, the
 * storage analyzer, etc.
 *
 * Before this existed, each category screen ran its own recursive
 * [File.listFiles] walk from scratch every time it opened. That meant:
 *  - Images, Videos, Audio, Docs, Archives, APKs, Code and Other all paid
 *    the cost of a full-storage walk on every single open.
 *  - A single unreadable directory (scoped-storage-restricted folders like
 *    Android/data, a permission error, a broken symlink) threw an
 *    exception that was never caught, so the screen's "results" state
 *    stayed null forever — an infinite spinner, for every category, since
 *    they all shared the same unguarded walk function.
 *
 * This repository fixes both: one shared, cached, in-memory index; and a
 * walk where a bad file or folder is skipped, never fatal, so the scan
 * always finishes with either a real result or a real (catchable) error.
 */
object FileIndexRepository {

    private const val CACHE_TTL_MILLIS = 60_000L

    data class IndexResult(val files: List<FileItem>, val builtAtMillis: Long)

    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, IndexResult>()

    /**
     * Whatever is currently cached for [rootPath], however old — used to
     * paint a screen instantly with last-known-good data while a refresh
     * runs in the background. Never touches disk, never throws.
     */
    fun peekCache(rootPath: String): List<FileItem>? = cache[rootPath]?.files

    /**
     * Returns the shared index for [rootPath]. A cache hit younger than
     * [CACHE_TTL_MILLIS] is returned with no disk access at all. Concurrent
     * callers (e.g. two category screens opened back to back) are
     * serialized on [mutex] rather than each kicking off their own scan, so
     * the second caller just picks up the first caller's fresh result.
     *
     * Throws [IOException] if the root cannot be scanned at all (e.g. the
     * root path itself is missing or unreadable) — callers should catch
     * this and show a real error + retry, never leave the caller waiting
     * forever.
     */
    suspend fun getIndex(rootPath: String, forceRefresh: Boolean = false): IndexResult {
        if (!forceRefresh) {
            cache[rootPath]?.let { cached ->
                if (System.currentTimeMillis() - cached.builtAtMillis < CACHE_TTL_MILLIS) return cached
            }
        }
        return mutex.withLock {
            // Another caller may have refreshed it while we were waiting on the lock.
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

    /** Forces the next [getIndex] call to re-scan instead of using the cache. */
    fun invalidate(rootPath: String? = null) {
        if (rootPath == null) cache.clear() else cache.remove(rootPath)
    }

    private suspend fun scan(rootPath: String): IndexResult {
        val root = File(rootPath)
        if (!root.exists()) throw IOException("Storage root not found: $rootPath")
        if (!root.canRead()) throw IOException("Permission denied: $rootPath")

        val found = mutableListOf<FileItem>()
        // Canonical paths already visited, so a symlink that loops back to
        // an ancestor directory can't spin the walk forever.
        val visitedDirs = HashSet<String>()

        suspend fun walk(dir: File) {
            currentCoroutineContext().ensureActive()
            if (dir.name == ".naze_trash") return

            val canonical = try {
                dir.canonicalPath
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return // Unresolvable path (dangling symlink, race with deletion) — skip it.
            }
            if (!visitedDirs.add(canonical)) return

            val children = try {
                dir.listFiles()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                null // Restricted directory (e.g. scoped-storage-blocked) — skip it, don't fail the scan.
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
                    // One corrupt/unreadable entry must never take down the whole scan.
                    continue
                }
            }
        }

        walk(root)
        return IndexResult(files = found, builtAtMillis = System.currentTimeMillis())
    }
}
