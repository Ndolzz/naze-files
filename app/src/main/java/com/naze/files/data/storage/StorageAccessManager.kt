package com.naze.files.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

/**
 * Central authority on storage permission state and available storage roots.
 *
 * Naze Files is a file-manager-class app, so it requests broad filesystem
 * access (MANAGE_EXTERNAL_STORAGE on API 30+) rather than pretending to be a
 * media-only app — but it always does so behind an explicit rationale screen
 * (see ui/permission), and falls back to SAF tree access for a specific
 * folder if the user declines, per the app's storage-access rules.
 */
class StorageAccessManager(private val context: Context) {

    /** True if the app can read/write across shared storage right now. */
    fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // On API 26-29 the legacy READ/WRITE_EXTERNAL_STORAGE runtime
            // permissions (declared in the manifest) cover full access;
            // MainActivity is responsible for requesting them at runtime.
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /** Intent that opens the system's "All files access" screen for this app. */
    fun allFilesAccessSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /** Intent to let the user grant SAF access to one folder (e.g. an SD card). */
    fun openDocumentTreeIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }
    }

    /**
     * Real, currently-mounted storage roots — internal storage plus any
     * mounted SD card / USB volume the OS reports. Never fabricated.
     */
    fun listStorageRoots(): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()
        val primary = Environment.getExternalStorageDirectory()
        if (primary.exists()) {
            roots += StorageRoot(
                label = "Internal Storage",
                rootFile = primary,
                isPrimary = true,
                isRemovable = false,
            )
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            storageManager.storageVolumes.forEach { volume ->
                if (volume.isPrimary) return@forEach // already added above
                val dir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    volume.directory
                } else {
                    // No public accessor before API 30; this reflective call
                    // mirrors what AOSP's own Files app used pre-R and simply
                    // returns null (skipping the volume) if it ever changes.
                    try {
                        volume.javaClass.getMethod("getPathFile").invoke(volume) as? File
                    } catch (e: Exception) {
                        null
                    }
                }
                if (dir != null && dir.exists()) {
                    roots += StorageRoot(
                        label = volume.getDescription(context) ?: "SD Card",
                        rootFile = dir,
                        isPrimary = false,
                        isRemovable = true,
                    )
                }
            }
        }

        return roots
    }
}

data class StorageRoot(
    val label: String,
    val rootFile: File,
    val isPrimary: Boolean,
    val isRemovable: Boolean,
)
