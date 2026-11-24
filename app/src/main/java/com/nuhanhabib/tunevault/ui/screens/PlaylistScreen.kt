package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.TrackRow

@Composable
fun PlaylistScreen(vm: MainViewModel, playlistId: String, onBack: () -> Unit) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }.collectAsState(initial = null)
    val tracks by remember(playlistId) { vm.playlistTracks(playlistId) }.collectAsState(initial = emptyList())
    val player by vm.player.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val playable = tracks.filter { it.status == TrackStatus.DONE }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(
                playlist?.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.syncPlaylist(playlistId) }) { Icon(Icons.Default.Sync, "Check for new songs") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete playlist") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(playable.firstOrNull()?.thumbPath ?: playlist?.remoteThumbnail, 96.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "${playable.size} of ${tracks.size} songs available offline",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PlayShuffleRow(onPlay = { vm.play(playable) }, onShuffle = { vm.shuffle(playable) })
            }
            itemsIndexed(tracks, key = { _, t -> t.id }) { _, track ->
                if (track.status == TrackStatus.DONE) {
                    SongItem(
                        track = track,
                        playing = track.id == player.trackId,
                        onClick = { vm.play(playable, playable.indexOf(track)) },
                        onDelete = { vm.deleteTrack(track) },
                    )
                } else {
                    TrackRow(
                        title = track.title,
                        subtitle = when (track.status) {
                            TrackStatus.FAILED -> "Failed: ${track.error.orEmpty()}"
                            TrackStatus.DOWNLOADING -> "Downloading…"
                            else -> "Waiting to download"
                        },
                        artwork = track.remoteThumbnail,
                        enabled = false,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        var withSongs by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withSongs, onCheckedChange = { withSongs = it })
                    Text("Also delete its songs from the phone (songs in other playlists are kept)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deletePlaylist(playlistId, withSongs)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
