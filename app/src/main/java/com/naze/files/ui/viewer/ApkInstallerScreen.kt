package com.naze.files.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.naze.files.data.model.FileItem
import com.naze.files.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ApkPreview(
    val label: String,
    val versionName: String?,
    val packageName: String?,
    val icon: Bitmap?,
)

private suspend fun loadApkPreview(context: android.content.Context, item: FileItem): ApkPreview =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info = try {
            pm.getPackageArchiveInfo(item.absolutePath, 0)
        } catch (e: Exception) {
            null
        }
        val appInfo = info?.applicationInfo
        if (appInfo != null) {
            // getPackageArchiveInfo doesn't set these, but getApplicationLabel/
            // getApplicationIcon need them to find the archive's own resources.
            appInfo.sourceDir = item.absolutePath
            appInfo.publicSourceDir = item.absolutePath
        }
        val label = appInfo?.let {
            try {
                pm.getApplicationLabel(it).toString()
            } catch (e: Exception) {
                null
            }
        } ?: item.name
        val icon = appInfo?.let {
            try {
                pm.getApplicationIcon(it).toBitmap()
            } catch (e: Exception) {
                null
            }
        }
        ApkPreview(
            label = label,
            versionName = info?.versionName,
            packageName = info?.packageName,
            icon = icon,
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInstallerScreen(
    item: FileItem,
    onInstall: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var preview by remember(item.absolutePath) { mutableStateOf<ApkPreview?>(null) }

    LaunchedEffect(item.absolutePath) {
        preview = loadApkPreview(context, item)
    }

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
                val current = preview
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = current?.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Text(
                    text = current?.label ?: item.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp),
                    textAlign = TextAlign.Center,
                )
                val subtitle = when {
                    current == null -> "Reading package…"
                    current.versionName != null -> "Version ${current.versionName} • ${formatFileSize(item.sizeBytes)}"
                    else -> formatFileSize(item.sizeBytes)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (current != null && current.packageName == null) {
                    Text(
                        text = "This file doesn't look like a valid APK, but you can still try opening it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = onInstall,
                    enabled = current != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                ) {
                    if (current == null) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.GetApp, contentDescription = null)
                        Text(" Install", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                OutlinedButton(
                    onClick = onOpenWith,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("Open with…")
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
