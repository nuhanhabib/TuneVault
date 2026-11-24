package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.PlaylistSummary
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.EmptyState
import com.nuhanhabib.tunevault.ui.components.PlaylistNameDialog
import com.nuhanhabib.tunevault.ui.components.ScreenTitle
import com.nuhanhabib.tunevault.ui.components.SongActions
import com.nuhanhabib.tunevault.ui.components.SongRow
import com.nuhanhabib.tunevault.ui.components.cleanSongName
import com.nuhanhabib.tunevault.ui.theme.AppIcons

@Composable
fun LibraryScreen(vm: MainViewModel, onOpenPlaylist: (String) -> Unit, onGoToAdd: () -> Unit) {
    var showPlaylists by rememberSaveable { mutableStateOf(false) }
    val songs by vm.songs.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Library", "${songs.size} songs · ${playlists.size} playlists")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showPlaylists,
                onClick = { showPlaylists = false },
                label = { Text("Songs") },
                shape = RoundedCornerShape(50),
            )
            FilterChip(
                selected = showPlaylists,
                onClick = { showPlaylists = true },
                label = { Text("Playlists") },
                shape = RoundedCornerShape(50),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (showPlaylists) PlaylistsGrid(vm, playlists, onOpenPlaylist)
        else SongsList(vm, songs, onGoToAdd)
    }
}

@Composable
private fun SongsList(vm: MainViewModel, songs: List<TrackEntity>, onGoToAdd: () -> Unit) {
    val player by vm.player.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<TrackEntity?>(null) }
    val shown = remember(songs, filter) {
        if (filter.isBlank()) songs
        else songs.filter {
            val n = cleanSongName(it.title, it.artist)
            n.title.contains(filter, true) || n.artist.contains(filter, true) || it.title.contains(filter, true)
        }
    }

    if (songs.isEmpty()) {
        EmptyState(
            icon = AppIcons.LibraryMusic,
            title = "Your library is empty",
            body = "Paste a YouTube link or search for a song. It will be saved here to play offline.",
            actionLabel = "Add music",
            onAction = onGoToAdd,
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            SearchField(filter, { filter = it }, "Search your songs")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { vm.play(shown) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play all")
                }
                FilledTonalButton(onClick = { vm.shuffle(shown) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(AppIcons.Shuffle, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    "No songs match \"$filter\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        itemsIndexed(shown, key = { _, t -> t.id }) { index, track ->
            SongRow(
                track = track,
                playing = track.id == player.trackId,
                isPlaying = player.isPlaying,
                onClick = { vm.play(shown, index) },
                onMore = { menuFor = track },
            )
        }
    }

    menuFor?.let { SongActions(vm, it, onDismiss = { menuFor = null }) }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, "Clear") }
        },
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlaylistsGrid(vm: MainViewModel, playlists: List<PlaylistSummary>, onOpen: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "new") { NewPlaylistCard { creating = true } }
        items(playlists, key = { it.playlist.id }) { p -> PlaylistCard(p) { onOpen(p.playlist.id) } }
    }

    if (creating) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { vm.createPlaylist(it); creating = false },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun NewPlaylistCard(onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(
            "New playlist",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text("Make your own", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaylistCard(p: PlaylistSummary, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Box {
            Artwork(
                p.coverPath ?: p.playlist.remoteThumbnail,
                size = 150.dp,
                corner = 16.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            if (p.total > 0) {
                Text(
                    "${p.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            p.playlist.title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            when {
                p.downloaded < p.total -> "${p.downloaded}/${p.total} downloaded"
                p.playlist.isLocal -> "Your playlist · ${p.total} songs"
                else -> "YouTube · ${p.total} songs"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

