package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.PlaylistEntity
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.AppSheet
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.EmptyState
import com.nuhanhabib.tunevault.ui.components.PlaylistNameDialog
import com.nuhanhabib.tunevault.ui.components.SongActions
import com.nuhanhabib.tunevault.ui.components.SongRow
import com.nuhanhabib.tunevault.ui.components.TrackRow
import com.nuhanhabib.tunevault.ui.components.cleanSongName
import com.nuhanhabib.tunevault.ui.components.formatTotalDuration
import com.nuhanhabib.tunevault.ui.theme.AppIcons

@Composable
fun PlaylistScreen(vm: MainViewModel, playlistId: String, onBack: () -> Unit) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }.collectAsState(initial = null)
    val tracks by remember(playlistId) { vm.playlistTracks(playlistId) }.collectAsState(initial = emptyList())
    val player by vm.player.state.collectAsStateWithLifecycle()
    val playable = tracks.filter { it.status == TrackStatus.DONE }
    val isLocal = playlist?.isLocal == true

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addingSongs by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<TrackEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Spacer(Modifier.weight(1f))
            if (!isLocal && playlist != null) {
                IconButton(onClick = { vm.syncPlaylist(playlistId) }) { Icon(AppIcons.Sync, "Check YouTube for new songs") }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, "More")
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (isLocal) {
                        DropdownMenuItem(
                            text = { Text("Add songs") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = { menuOpen = false; addingSongs = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; renaming = true },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete playlist") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        playable.firstOrNull()?.thumbPath ?: playlist?.remoteThumbnail,
                        size = 200.dp,
                        corner = 20.dp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        playlist?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(
                            if (isLocal) "Your playlist" else "From YouTube",
                            "${tracks.size} songs",
                            formatTotalDuration(playable.sumOf { it.durationSec }),
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (playable.size < tracks.size) {
                        Text(
                            "${tracks.size - playable.size} still downloading",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isLocal) {
                            FilledTonalIconButton(onClick = { addingSongs = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Add, "Add songs")
                            }
                        }
                        Button(
                            onClick = { vm.play(playable) },
                            enabled = playable.isNotEmpty(),
                            modifier = Modifier.height(48.dp).width(130.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        FilledTonalButton(
                            onClick = { vm.shuffle(playable) },
                            enabled = playable.isNotEmpty(),
                            modifier = Modifier.height(48.dp).width(130.dp),
                        ) {
                            Icon(AppIcons.Shuffle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (tracks.isEmpty() && playlist != null) {
                item {
                    EmptyState(
                        icon = AppIcons.PlaylistAdd,
                        title = "No songs yet",
                        body = "Add songs from your library, or tap ⋮ on any song and choose \"Add to playlist\".",
                        actionLabel = if (isLocal) "Add songs" else null,
                        onAction = { addingSongs = true },
                    )
                }
            }

            items(tracks, key = { it.id }) { track ->
                if (track.status == TrackStatus.DONE) {
                    SongRow(
                        track = track,
                        playing = track.id == player.trackId,
                        isPlaying = player.isPlaying,
                        onClick = { vm.play(playable, playable.indexOf(track)) },
                        onMore = { menuFor = track },
                    )
                } else {
                    val name = cleanSongName(track.title, track.artist)
                    TrackRow(
                        title = name.title,
                        subtitle = when (track.status) {
                            TrackStatus.FAILED -> "Download failed"
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

    menuFor?.let { SongActions(vm, it, onDismiss = { menuFor = null }, fromPlaylist = playlist) }

    if (addingSongs && playlist != null) {
        AddSongsSheet(vm, playlist!!, inPlaylist = tracks.map { it.id }.toSet(), onDismiss = { addingSongs = false })
    }

    if (renaming && playlist != null) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initial = playlist!!.title,
            onConfirm = { vm.renamePlaylist(playlistId, it); renaming = false },
            onDismiss = { renaming = false },
        )
    }

    if (confirmDelete) {
        var withSongs by remember { mutableStateOf(!isLocal) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete \"${playlist?.title.orEmpty()}\"?") },
            text = {
                Row(
                    Modifier.clickable { withSongs = !withSongs },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = withSongs, onCheckedChange = { withSongs = it })
                    Text("Also delete its songs from the phone. Songs in other playlists are kept.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deletePlaylist(playlistId, withSongs)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Pick songs from the library to add to (or remove from) one of your playlists. */
@Composable
private fun AddSongsSheet(vm: MainViewModel, playlist: PlaylistEntity, inPlaylist: Set<String>, onDismiss: () -> Unit) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("") }
    val shown = remember(songs, filter) {
        if (filter.isBlank()) songs
        else songs.filter { it.title.contains(filter, true) || it.artist.contains(filter, true) }
    }

    AppSheet(onDismiss) {
        Row(Modifier.padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Add songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    playlist.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        SearchField(filter, { filter = it }, "Search your songs")
        if (songs.isEmpty()) {
            Text(
                "Download some songs first, then add them here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(shown, key = { it.id }) { track ->
                val isIn = track.id in inPlaylist
                val name = remember(track) { cleanSongName(track.title, track.artist) }
                TrackRow(
                    title = name.title,
                    subtitle = name.artist,
                    artwork = track.thumbPath ?: track.remoteThumbnail,
                    onClick = { vm.toggleInPlaylist(playlist, track, isIn, notify = false) },
                ) {
                    Checkbox(checked = isIn, onCheckedChange = { vm.toggleInPlaylist(playlist, track, isIn, notify = false) })
                }
            }
        }
    }
}
