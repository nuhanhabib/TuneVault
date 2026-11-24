package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.EmptyState
import com.nuhanhabib.tunevault.ui.components.TrackRow

@Composable
fun DownloadsScreen(vm: MainViewModel) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val active by vm.activeDownload.collectAsStateWithLifecycle()
    val failed = queue.count { it.status == TrackStatus.FAILED }
    val waiting = queue.size - failed

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Downloads", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$waiting in queue" + if (failed > 0) " • $failed failed" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (failed > 0) {
                FilledTonalButton(onClick = vm::retryFailed) { Text("Retry failed") }
            } else if (waiting > 0 && active == null) {
                FilledTonalButton(onClick = vm::resumeDownloads) { Text("Resume") }
            }
        }
        if (queue.isEmpty()) {
            EmptyState("Nothing to download. Everything is ready to play offline.")
            return
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(queue, key = { it.id }) { track ->
                val isActive = active?.trackId == track.id
                Column {
                    TrackRow(
                        title = track.title,
                        subtitle = when {
                            isActive -> "Downloading… ${((active?.progress ?: 0f) * 100).toInt()}%"
                            track.status == TrackStatus.FAILED -> "Failed: ${track.error.orEmpty()}"
                            else -> "Waiting"
                        },
                        artwork = track.remoteThumbnail,
                    ) {
                        IconButton(onClick = { vm.deleteTrack(track) }) { Icon(Icons.Default.Close, "Remove") }
                    }
                    if (isActive) {
                        LinearProgressIndicator(
                            progress = { active?.progress ?: 0f },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}
