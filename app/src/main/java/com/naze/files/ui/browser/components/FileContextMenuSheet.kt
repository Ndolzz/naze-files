package com.naze.files.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContextMenuSheet(
    item: FileItem,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )

        ContextMenuRow(Icons.Filled.OpenInNew, "Open", onOpen)
        if (!item.isDirectory) {
            ContextMenuRow(Icons.Filled.OpenInNew, "Open with…", onOpenWith)
            ContextMenuRow(Icons.Filled.Share, "Share", onShare)
        }
        ContextMenuRow(Icons.Filled.ContentCopy, "Copy", onCopy)
        ContextMenuRow(Icons.Filled.ContentCut, "Cut", onCut)
        ContextMenuRow(Icons.Filled.DriveFileRenameOutline, "Rename", onRename)
        ContextMenuRow(Icons.Filled.Archive, "Compress", onCompress)
        ContextMenuRow(
            icon = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            onClick = onToggleFavorite,
        )
        ContextMenuRow(Icons.Filled.Info, "Information", onShowInfo)
        ContextMenuRow(Icons.Filled.Delete, "Delete", onDelete)
    }
}

@Composable
private fun ContextMenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
