package com.naze.files.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.graphics.vector.ImageVector
import com.naze.files.data.model.FileCategory

fun iconFor(category: FileCategory): ImageVector = when (category) {
    FileCategory.FOLDER -> Icons.Filled.Folder
    FileCategory.IMAGE -> Icons.Filled.Image
    FileCategory.VIDEO -> Icons.Filled.Movie
    FileCategory.AUDIO -> Icons.Filled.Audiotrack
    FileCategory.DOCUMENT -> Icons.Filled.Description
    FileCategory.ARCHIVE -> Icons.Filled.Archive
    FileCategory.APK -> Icons.Filled.Android
    FileCategory.CODE -> Icons.Filled.Code
    FileCategory.OTHER -> Icons.Filled.InsertDriveFile
}
