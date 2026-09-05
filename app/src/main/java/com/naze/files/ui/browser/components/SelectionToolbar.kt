package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToolbarAction(icon = Icons.Filled.ContentCopy, label = "Copy", onClick = onCopy)
            ToolbarAction(icon = Icons.Filled.ContentCut, label = "Cut", onClick = onCut)
            ToolbarAction(icon = Icons.Filled.Share, label = "Share", onClick = onShare)
            ToolbarAction(icon = Icons.Filled.Archive, label = "Compress", onClick = onCompress)
            ToolbarAction(icon = Icons.Filled.Delete, label = "Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ToolbarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
