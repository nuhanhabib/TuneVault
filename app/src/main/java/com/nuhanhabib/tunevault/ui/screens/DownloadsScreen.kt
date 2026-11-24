package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.EmptyState
import com.nuhanhabib.tunevault.ui.components.ScreenTitle
import com.nuhanhabib.tunevault.ui.components.TrackRow
import com.nuhanhabib.tunevault.ui.components.cleanSongName
import com.nuhanhabib.tunevault.ui.theme.AppIcons

@Composable
fun DownloadsScreen(vm: MainViewModel) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val active by vm.activeDownload.collectAsStateWithLifecycle()
    val failed = queue.count { it.status == TrackStatus.FAILED }
    val waiting = queue.size - failed

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            "Downloads",
            when {
                queue.isEmpty() -> "All caught up"
                failed > 0 -> "$waiting in queue · $failed failed"
                else -> "$waiting in queue"
            },
        ) {
            if (failed > 0) {
                FilledTonalButton(onClick = vm::retryFailed, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Refresh, null)
                    Text("Retry", Modifier.padding(start = 6.dp))
                }
            } else if (waiting > 0 && active == null) {
                FilledTonalButton(onClick = vm::resumeDownloads, modifier = Modifier.padding(end = 8.dp)) { Text("Resume") }
            }
        }

        if (queue.isEmpty()) {
            EmptyState(
                icon = AppIcons.Download,
                title = "Nothing downloading",
                body = "Everything you added is ready to play offline.",
            )
            return
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            active?.let { now ->
                item(key = "active") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Downloading now",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(now.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text(
                                cleanSongName(now.title, "").title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                            LinearProgressIndicator(
                                progress = { now.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            )
                        }
                    }
                }
            }
            items(queue.filter { it.id != active?.trackId }, key = { it.id }) { track ->
                val name = cleanSongName(track.title, track.artist)
                val isFailed = track.status == TrackStatus.FAILED
                TrackRow(
                    title = name.title,
                    subtitle = if (isFailed) track.error ?: "Download failed" else "Waiting · ${name.artist}",
                    artwork = track.remoteThumbnail,
                ) {
                    if (isFailed) {
                        Icon(Icons.Default.Warning, "Failed", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { vm.deleteTrack(track) }) { Icon(Icons.Default.Close, "Remove") }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
