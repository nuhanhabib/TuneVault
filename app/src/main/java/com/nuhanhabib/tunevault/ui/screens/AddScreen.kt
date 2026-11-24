package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.TrackRow
import com.nuhanhabib.tunevault.ui.components.formatDuration

@Composable
fun AddScreen(vm: MainViewModel, input: String, onInputChange: (String) -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val known by vm.knownIds.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current

    fun submit() {
        focus.clearFocus()
        vm.submit(input)
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Add music", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Paste a YouTube / YouTube Music link (song or playlist), or type a song name to search. " +
                    "You can also tap Share → TuneVault inside the YouTube app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Link or song name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let(onInputChange) }) {
                        Icon(Icons.Default.ContentPaste, "Paste")
                    }
                },
            )
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { submit() }, enabled = !busy && input.isNotBlank()) {
                    Text(if (input.trim().startsWith("http")) "Download" else "Search")
                }
                if (busy) {
                    Spacer(Modifier.width(16.dp))
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { track ->
                val have = track.id in known
                TrackRow(
                    title = track.title,
                    subtitle = listOf(track.artist, formatDuration(track.durationSec))
                        .filter { it.isNotBlank() }.joinToString(" • "),
                    artwork = track.thumbnail,
                    onClick = { if (!have) vm.download(track) },
                ) {
                    if (have) {
                        Icon(Icons.Default.CheckCircle, "In library", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp))
                    } else {
                        IconButton(onClick = { vm.download(track) }) { Icon(Icons.Default.Download, "Download") }
                    }
                }
            }
        }
    }
}
