package com.naze.files.ui.browser.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.util.iconFor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    showExtension: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                FileThumbnail(
                    item = item,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd),
                )
            }
        }
        Text(
            text = if (showExtension || item.isDirectory) item.name else item.displayNameWithoutExtension,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
