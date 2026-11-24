package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val version by vm.ytDlpVersion.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    var storage by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) { vm.loadYtDlpVersion() }
    LaunchedEffect(songs.size) { storage = withContext(Dispatchers.IO) { vm.storageUsed() } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Library", style = MaterialTheme.typography.titleMedium)
                Text("${songs.size} songs • ${formatBytes(storage)} used")
                Text(
                    "Songs are stored inside the app's private folder. Uninstalling the app deletes them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Card(Modifier.padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Download engine (yt-dlp)", style = MaterialTheme.typography.titleMedium)
                Text("Version: ${version ?: "loading…"}")
                Text(
                    "If downloads start failing, update the engine. It also updates itself automatically once a day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Button(onClick = vm::updateYtDlp, enabled = !busy) { Text("Update now") }
            }
        }

        Card(Modifier.padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Background playback tip", style = MaterialTheme.typography.titleMedium)
                Text(
                    "On Samsung phones open Settings → Apps → TuneVault → Battery and choose \"Unrestricted\" " +
                        "so music and downloads are never stopped while the screen is off.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
