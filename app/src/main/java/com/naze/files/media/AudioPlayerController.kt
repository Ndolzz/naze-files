package com.naze.files.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.naze.files.data.model.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTitle: String = "",
    val currentIndex: Int = 0,
    val playlistSize: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/**
 * Owns one long-lived MediaController connection to [PlaybackService].
 * Created once (see MainActivity) and reused for every audio file opened,
 * so playback keeps going - with a real notification and lock-screen
 * controls - when the person navigates elsewhere in the app.
 */
class AudioPlayerController(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private var playlist: List<FileItem> = emptyList()

    private val _state = MutableStateFlow(AudioPlayerUiState())
    val state: StateFlow<AudioPlayerUiState> = _state.asStateFlow()

    private suspend fun ensureConnected() {
        if (controller != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val newController = MediaController.Builder(appContext, token).buildAsync().await()
        newController.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()
            override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        })
        controller = newController
    }

    fun play(newPlaylist: List<FileItem>, startIndex: Int) {
        scope.launch {
            ensureConnected()
            val c = controller ?: return@launch
            playlist = newPlaylist
            val items = newPlaylist.map { toMediaItem(it) }
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
            refresh()
            startTicker()
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun next() = controller?.seekToNextMediaItem()

    fun previous() = controller?.seekToPreviousMediaItem()

    /** Releases the client connection only - playback in the service is untouched. */
    fun releaseController() {
        tickerJob?.cancel()
        controller?.release()
        controller = null
        _state.value = AudioPlayerUiState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                refresh()
                delay(500)
            }
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        _state.value = AudioPlayerUiState(
            isConnected = true,
            isPlaying = c.isPlaying,
            currentTitle = playlist.getOrNull(index)?.name ?: "",
            currentIndex = index,
            playlistSize = playlist.size,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
        )
    }

    private fun toMediaItem(item: FileItem): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.fromFile(File(item.absolutePath)))
            .setMediaId(item.absolutePath)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(item.name).build())
            .build()
    }
}
