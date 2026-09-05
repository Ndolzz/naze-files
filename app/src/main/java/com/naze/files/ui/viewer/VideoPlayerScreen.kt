package com.naze.files.ui.viewer

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.naze.files.data.model.FileItem
import java.io.File

private val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    item: FileItem,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var chromeVisible by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(item.absolutePath))))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(item.absolutePath) {
        onDispose { player.release() }
    }

    Scaffold(
        topBar = {
            if (chromeVisible) {
                TopAppBar(
                    title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { speedMenuExpanded = true }) {
                            Icon(imageVector = Icons.Filled.Speed, contentDescription = "Playback speed")
                        }
                        DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    leadingIcon = {
                                        if (playbackSpeed == speed) Icon(Icons.Filled.Check, contentDescription = null)
                                    },
                                    onClick = {
                                        playbackSpeed = speed
                                        player.setPlaybackSpeed(speed)
                                        speedMenuExpanded = false
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = { menuExpanded = false; onShare() },
                            )
                            DropdownMenuItem(
                                text = { Text("Information") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = { menuExpanded = false; onShowInfo() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuExpanded = false; onDelete() },
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(if (chromeVisible) padding else PaddingValues(0.dp)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setControllerVisibilityListener(
                            PlayerView.ControllerVisibilityListener { visibility ->
                                chromeVisible = visibility == android.view.View.VISIBLE
                            },
                        )
                    }
                },
            )
        }
    }
}
