package com.naze.files.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.util.iconFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsupportedViewerScreen(
    item: FileItem,
    reason: String,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(item.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Text(
                    text = "Can't preview this file yet",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onOpenWith,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                ) {
                    Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = null)
                    Text(" Open with…", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    Text(" Share", modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = onShowInfo, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                    Text(" Information", modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(" Delete", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
