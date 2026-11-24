package com.nuhanhabib.tunevault.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.ui.components.cleanSongName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlayerUiState(
    val trackId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artwork: Uri? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

/** Connects the UI to [PlaybackService] through a MediaController. */
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private val future: ListenableFuture<MediaController> = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    init {
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            refresh(c)
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: pending.add(action)
    }

    private fun refresh(player: Player) {
        val meta = player.mediaMetadata
        _state.value = PlayerUiState(
            trackId = player.currentMediaItem?.mediaId,
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            artwork = meta.artworkUri,
            isPlaying = player.isPlaying,
            durationMs = player.duration.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    val positionMs: Long get() = controller?.currentPosition ?: 0L

    fun play(tracks: List<TrackEntity>, startIndex: Int = 0, shuffle: Boolean = false) {
        val items = tracks.filter { it.filePath != null }.map { it.toMediaItem() }
        if (items.isEmpty()) return
        val startId = tracks.getOrNull(startIndex)?.id
        val index = items.indexOfFirst { it.mediaId == startId }.coerceAtLeast(0)
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, if (shuffle) (items.indices).random() else index, 0L)
            c.prepare()
            c.play()
        }
    }

    /** Plays [track] right after the current song (or now, if nothing is playing). */
    fun playNext(track: TrackEntity) = withController { c ->
        if (track.filePath == null) return@withController
        if (c.mediaItemCount == 0) {
            c.setMediaItem(track.toMediaItem()); c.prepare(); c.play()
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
        }
    }

    /** Adds [track] to the end of the queue. */
    fun addToQueue(track: TrackEntity) = withController { c ->
        if (track.filePath == null) return@withController
        c.addMediaItem(track.toMediaItem())
        if (c.mediaItemCount == 1) { c.prepare(); c.play() }
    }

    fun togglePlay() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Removes a deleted song from the current queue. */
    fun removeFromQueue(trackId: String) = withController { c ->
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == trackId) c.removeMediaItem(i)
        }
    }

    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
    }

    private fun TrackEntity.toMediaItem(): MediaItem {
        val uri = Uri.fromFile(File(filePath!!))
        val name = cleanSongName(title, artist)
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name.title)
                    .setArtist(name.artist)
                    .setArtworkUri(thumbPath?.let { Uri.fromFile(File(it)) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }
}
