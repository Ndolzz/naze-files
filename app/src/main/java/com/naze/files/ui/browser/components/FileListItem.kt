package com.naze.files.ui.browser.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.util.formatFileSize
import com.naze.files.util.formatModifiedDate
import com.naze.files.util.iconFor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    selectionModeActive: Boolean,
    showExtension: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                FileThumbnail(item = item, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize())
            }
        }

        Spacer()

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showExtension || item.isDirectory) item.name else item.displayNameWithoutExtension,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp).padding(start = 4.dp),
                    )
                }
            }
            val subtitle = if (item.isDirectory) {
                formatModifiedDate(item.lastModifiedMillis)
            } else {
                "${formatFileSize(item.sizeBytes)} • ${formatModifiedDate(item.lastModifiedMillis)}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!selectionModeActive) {
            IconButton(onClick = onMoreClick) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
private fun Spacer() {
    Box(modifier = Modifier.width(12.dp))
}
