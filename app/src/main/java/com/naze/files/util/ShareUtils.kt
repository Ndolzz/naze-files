package com.naze.files.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun buildShareIntent(context: Context, files: List<File>): Intent {
    val authority = "${context.packageName}.fileprovider"
    val uris = files.map { FileProvider.getUriForFile(context, authority, it) }

    return if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uris[0]) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.let { Intent.createChooser(it, if (files.size == 1) files[0].name else "${files.size} files") }
}
