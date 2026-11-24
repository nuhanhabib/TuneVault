package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.EmptyState
import com.nuhanhabib.tunevault.ui.components.TrackRow
import com.nuhanhabib.tunevault.ui.components.formatDuration

@Composable
fun LibraryScreen(vm: MainViewModel, onOpenPlaylist: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Songs") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Playlists") })
        }
        if (tab == 0) SongsTab(vm) else PlaylistsTab(vm, onOpenPlaylist)
    }
}

@Composable
private fun SongsTab(vm: MainViewModel) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("") }
    val shown = remember(songs, filter) {
        if (filter.isBlank()) songs
        else songs.filter { it.title.contains(filter, true) || it.artist.contains(filter, true) }
    }

    if (songs.isEmpty()) {
        EmptyState("No songs yet.\nGo to Add and paste a YouTube link or search for a song.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Filter ${songs.size} songs") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            PlayShuffleRow(onPlay = { vm.play(shown) }, onShuffle = { vm.shuffle(shown) })
        }
        itemsIndexed(shown, key = { _, t -> t.id }) { index, track ->
            SongItem(
                track = track,
                playing = track.id == player.trackId,
                onClick = { vm.play(shown, index) },
                onDelete = { vm.deleteTrack(track) },
            )
        }
    }
}

@Composable
fun SongItem(track: TrackEntity, playing: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    TrackRow(
        title = track.title,
        subtitle = listOf(track.artist, formatDuration(track.durationSec)).filter { it.isNotBlank() }.joinToString(" • "),
        artwork = track.thumbPath ?: track.remoteThumbnail,
        highlighted = playing,
        onClick = onClick,
    ) {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete from phone") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
fun PlayShuffleRow(onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text("Play")
        }
        FilledTonalButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Shuffle, null)
            Spacer(Modifier.width(6.dp))
            Text("Shuffle")
        }
    }
}

@Composable
private fun PlaylistsTab(vm: MainViewModel, onOpen: (String) -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    if (playlists.isEmpty()) {
        EmptyState("No playlists yet.\nPaste a YouTube playlist link in Add to download the whole playlist.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(playlists, key = { it.playlist.id }) { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(p.playlist.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(p.coverPath ?: p.playlist.remoteThumbnail, 64.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(
                        if (p.downloaded == p.total) "${p.total} songs"
                        else "${p.downloaded} of ${p.total} downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
