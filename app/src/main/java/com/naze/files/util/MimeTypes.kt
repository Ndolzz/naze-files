package com.naze.files.util

import android.webkit.MimeTypeMap

/**
 * Stock [MimeTypeMap] is missing a handful of extensions that matter for a
 * file manager - most importantly "apk": on stock Android,
 * `MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk")` returns
 * `null`, because .apk was never added to the system's MIME table.
 *
 * The null value can otherwise fall back to a wildcard MIME type when the app
 * builds an ACTION_VIEW/install intent, preventing Android from routing the
 * intent to the Package Installer.
 *
 * Every place in the app that resolves a MIME type from a file extension
 * goes through this one function so a fix here fixes it everywhere at once.
 */
fun resolveMimeType(extension: String): String? {
    val ext = extension.lowercase()
    knownExtensionOverrides[ext]?.let { return it }
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
}

private val knownExtensionOverrides = mapOf(
    "apk" to "application/vnd.android.package-archive",
)
