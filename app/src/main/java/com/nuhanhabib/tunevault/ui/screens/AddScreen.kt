package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.ScreenTitle
import com.nuhanhabib.tunevault.ui.components.TrackRow
import com.nuhanhabib.tunevault.ui.components.cleanSongName
import com.nuhanhabib.tunevault.ui.components.formatDuration
import com.nuhanhabib.tunevault.ui.theme.AppIcons

@Composable
fun AddScreen(vm: MainViewModel, input: String, onInputChange: (String) -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val known by vm.knownIds.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val isLink = input.trim().startsWith("http")

    fun submit() {
        focus.clearFocus()
        vm.submit(input)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Add music", "Songs and playlists from YouTube")

        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Search or paste a link") },
                singleLine = true,
                leadingIcon = { Icon(if (isLink) AppIcons.Link else Icons.Default.Search, null) },
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { onInputChange("") }) { Icon(Icons.Default.Clear, "Clear") }
                    } else {
                        IconButton(onClick = { clipboard.getText()?.text?.let(onInputChange) }) {
                            Icon(AppIcons.Paste, "Paste")
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = if (isLink) ImeAction.Go else ImeAction.Search),
                keyboardActions = KeyboardActions(onGo = { submit() }, onSearch = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            if (busy) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            } else {
                FilledTonalIconButton(
                    onClick = { submit() },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.size(56.dp),
                ) { Icon(if (isLink) AppIcons.Download else Icons.Default.Search, if (isLink) "Download" else "Search") }
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }

        if (results.isEmpty()) {
            HowItWorks(onPaste = { clipboard.getText()?.text?.let { onInputChange(it); vm.submit(it) } })
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                items(results, key = { it.id }) { track ->
                    val have = track.id in known
                    val name = cleanSongName(track.title, track.artist)
                    TrackRow(
                        title = name.title,
                        subtitle = listOf(name.artist, formatDuration(track.durationSec))
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        artwork = track.thumbnail,
                        onClick = { if (!have) vm.download(track) },
                    ) {
                        if (have) {
                            Icon(
                                Icons.Default.CheckCircle, "In your library",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp),
                            )
                        } else {
                            IconButton(onClick = { vm.download(track) }) {
                                Icon(AppIcons.Download, "Download", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorks(onPaste: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Got a link copied?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Paste a song or playlist link and it starts downloading right away.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Button(onClick = onPaste) {
                    Icon(AppIcons.Paste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Paste & download")
                }
            }
        }
        Tip(Icons.Default.Search, "Search", "Type a song or artist name and tap a result to save it.")
        Tip(Icons.Default.Share, "Share from YouTube", "In the YouTube app tap Share → TuneVault.")
        Tip(AppIcons.Queue, "Whole playlists", "Paste a playlist link to save every song in it.")
    }
}

@Composable
private fun Tip(icon: ImageVector, title: String, body: String) {
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
