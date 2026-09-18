package com.naze.files.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/**
 * Whether this app is currently allowed to trigger an install. Always true
 * below API 26 (the per-source toggle didn't exist yet); on API 26+ it
 * reflects the "Install unknown apps" switch for Naze Files specifically,
 * under Settings > Apps > Special access > Install unknown apps. Without
 * [android.Manifest.permission.REQUEST_INSTALL_PACKAGES] declared in the
 * manifest, that switch never appears at all, and this always reports false.
 */
fun canInstallPackages(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

/** Sends the user straight to the per-app toggle mentioned above, pre-scoped to this app. */
fun buildInstallUnknownAppsSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

/**
 * The actual install intent. Uses [APK_MIME_TYPE] explicitly rather than
 * relying on [resolveMimeType] at the call site, since this is the one place
 * in the app where that exact type genuinely matters (the OS routes
 * ACTION_VIEW to the Package Installer based on it).
 */
fun buildInstallApkIntent(context: Context, file: File): Intent {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, APK_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
