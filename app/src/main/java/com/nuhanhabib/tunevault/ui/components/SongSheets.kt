package com.nuhanhabib.tunevault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.PlaylistEntity
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.theme.AppIcons

private enum class Step { Actions, Playlists, NewPlaylist, ConfirmDelete }

/**
 * Everything you can do with a song: play next, queue, add to a playlist,
 * remove from the current playlist, or delete it from the phone.
 *
 * [fromPlaylist] is set when the sheet is opened inside one of your own playlists.
 */
@Composable
fun SongActions(
    vm: MainViewModel,
    track: TrackEntity,
    onDismiss: () -> Unit,
    fromPlaylist: PlaylistEntity? = null,
    startWithPlaylists: Boolean = false,
) {
    var step by remember { mutableStateOf(if (startWithPlaylists) Step.Playlists else Step.Actions) }
    val name = remember(track) { cleanSongName(track.title, track.artist) }

    when (step) {
        Step.Actions -> AppSheet(onDismiss) {
            SheetSongHeader(track, name)
            SheetAction(AppIcons.PlayNext, "Play next") { vm.playNext(track); onDismiss() }
            SheetAction(AppIcons.Queue, "Add to queue") { vm.addToQueue(track); onDismiss() }
            SheetAction(AppIcons.PlaylistAdd, "Add to playlist") { step = Step.Playlists }
            if (fromPlaylist?.isLocal == true) {
                SheetAction(AppIcons.PlaylistRemove, "Remove from this playlist") {
                    vm.removeFromPlaylist(fromPlaylist.id, track); onDismiss()
                }
            }
            SheetAction(Icons.Default.Delete, "Delete from phone", danger = true) { step = Step.ConfirmDelete }
        }

        Step.Playlists -> AddToPlaylistSheet(
            vm = vm,
            track = track,
            onNew = { step = Step.NewPlaylist },
            onDismiss = onDismiss,
        )

        Step.NewPlaylist -> PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { vm.createPlaylist(it, track); onDismiss() },
            onDismiss = onDismiss,
        )

        Step.ConfirmDelete -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete this song?") },
            text = { Text("\"${name.title}\" will be removed from your phone and from all playlists.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTrack(track); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddToPlaylistSheet(
    vm: MainViewModel,
    track: TrackEntity,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val playlists by vm.localPlaylists.collectAsStateWithLifecycle()
    val containing by remember(track.id) { vm.playlistIdsWith(track.id) }.collectAsState(initial = emptyList())

    AppSheet(onDismiss) {
        Text(
            "Add to playlist",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SheetAction(Icons.Default.Add, "New playlist", accent = true, onClick = onNew)
        if (playlists.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 4.dp))
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                val isIn = playlist.id in containing
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.toggleInPlaylist(playlist, track, isIn) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Queue, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(20.dp))
                    Text(playlist.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isIn) Icon(Icons.Default.CheckCircle, "Added", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (playlists.isEmpty()) {
            Text(
                "You don't have any playlists yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) { content() }
    }
}

@Composable
private fun SheetSongHeader(track: TrackEntity, name: SongName) {
    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track.thumbPath ?: track.remoteThumbnail, 56.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(name.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
    HorizontalDivider(Modifier.padding(bottom = 4.dp))
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val color = when {
        danger -> MaterialTheme.colorScheme.error
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger || accent) color else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Dialog with one text field, used to create or rename a playlist. */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val valid = value.text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.text.length <= 60) value = it },
                placeholder = { Text("Playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(value.text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }, enabled = valid) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

