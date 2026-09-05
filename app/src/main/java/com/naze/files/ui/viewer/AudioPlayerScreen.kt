package com.naze.files.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.FileItem
import com.naze.files.media.AudioPlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    controller: AudioPlayerController,
    playlist: List<FileItem>,
    startIndex: Int,
    isFavorite: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by controller.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var userSeekPosition by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(playlist, startIndex) {
        controller.play(playlist, startIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                        )
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
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
            }

            Text(
                text = state.currentTitle.ifBlank { "Loading…" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
            if (state.playlistSize > 1) {
                Text(
                    text = "${state.currentIndex + 1} of ${state.playlistSize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Slider(
                value = userSeekPosition ?: positionFraction(state.positionMs, state.durationMs),
                onValueChange = { userSeekPosition = it },
                onValueChangeFinished = {
                    val fraction = userSeekPosition
                    if (fraction != null) {
                        controller.seekTo((fraction * state.durationMs).toLong())
                    }
                    userSeekPosition = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(state.positionMs), style = MaterialTheme.typography.bodyMedium)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = controller::previous, enabled = state.hasPrevious) {
                    Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = controller::togglePlayPause,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = controller::next, enabled = state.hasNext) {
                    Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

private fun positionFraction(position: Long, duration: Long): Float {
    if (duration <= 0) return 0f
    return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
