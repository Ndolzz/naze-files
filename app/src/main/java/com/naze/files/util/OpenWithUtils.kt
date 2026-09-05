package com.naze.files.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun buildOpenWithIntent(context: Context, file: File, mimeType: String?): Intent {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(viewIntent, "Open with")
}
