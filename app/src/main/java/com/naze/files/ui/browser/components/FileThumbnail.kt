package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.naze.files.data.model.FileCategory
import com.naze.files.data.model.FileItem
import com.naze.files.util.iconFor
import java.io.File

@Composable
fun FileThumbnail(item: FileItem, tint: Color, modifier: Modifier = Modifier) {
    val showsRealThumbnail = !item.isDirectory && (item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO)

    if (showsRealThumbnail) {
        SubcomposeAsyncImage(
            model = File(item.absolutePath),
            contentDescription = item.name,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { Icon(imageVector = iconFor(item.category), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp)) },
            error = { Icon(imageVector = iconFor(item.category), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp)) },
        )
    } else {
        Icon(imageVector = iconFor(item.category), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
    }
}
