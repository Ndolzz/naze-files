package com.naze.files.data.repository

import com.naze.files.data.model.FileItem

/**
 * Reads real entries from device storage. Implementations must never
 * synthesize data — every [FileItem] returned corresponds to an entry that
 * existed on disk at read time.
 */
interface FileRepository {

    /**
     * Lists the immediate children of [directoryPath]. Returns an empty list
     * for an empty folder; throws for a path that cannot be read (missing,
     * permission denied, not a directory) so the caller can show a precise
     * error instead of a silently-empty screen.
     */
    suspend fun listChildren(directoryPath: String, includeHidden: Boolean): List<FileItem>

    /** Reads metadata for a single path, or null if it no longer exists. */
    suspend fun stat(path: String): FileItem?
}
