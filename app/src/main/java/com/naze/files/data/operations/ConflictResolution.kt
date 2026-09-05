package com.naze.files.data.operations

/** How to handle a destination name that already exists. */
sealed class ConflictResolution {
    data object Replace : ConflictResolution()
    data object Skip : ConflictResolution()
    data class Rename(val newName: String) : ConflictResolution()
}

/**
 * Asked once per naming conflict encountered during a copy/move. The engine
 * suspends until this returns, so the UI has time to show a dialog and wait
 * for the person to choose — nothing is ever overwritten silently.
 */
fun interface ConflictResolver {
    suspend fun resolve(fileName: String): ConflictResolution
}
