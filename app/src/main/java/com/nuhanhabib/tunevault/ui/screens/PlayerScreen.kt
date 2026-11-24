package com.nuhanhabib.tunevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.media3.common.Player
import com.nuhanhabib.tunevault.playback.PlayerConnection
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.formatDuration
import kotlinx.coroutines.delay

@Composable
private fun rememberPosition(player: PlayerConnection, isPlaying: Boolean, trackId: String?): Long {
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, trackId) {
        do {
            position = player.positionMs
            delay(500)
        } while (isPlaying)
    }
    return position
}

@Composable
fun MiniPlayer(player: PlayerConnection, onExpand: () -> Unit) {
    val state by player.state.collectAsStateWithLifecycle()
    if (state.trackId == null) return
    val position = rememberPosition(player, state.isPlaying, state.trackId)

    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                Modifier.clickable(onClick = onExpand).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(state.artwork, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = player::togglePlay) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause")
                }
                IconButton(onClick = player::next) { Icon(Icons.Default.SkipNext, "Next") }
            }
        }
    }
}

@Composable
fun PlayerScreen(player: PlayerConnection, onCollapse: () -> Unit) {
    val state by player.state.collectAsStateWithLifecycle()
    val position = rememberPosition(player, state.isPlaying, state.trackId)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.trackId) { if (state.trackId == null) onCollapse() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onCollapse) { Icon(Icons.Default.KeyboardArrowDown, "Close player") }
        }
        Spacer(Modifier.weight(0.5f))
        Artwork(
            state.artwork,
            size = 320.dp,
            corner = 20.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            state.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(state.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(24.dp))

        val duration = state.durationMs.coerceAtLeast(1L)
        Slider(
            value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                player.seekTo((dragValue * duration).toLong())
                dragging = false
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = if (dragging) (dragValue * duration).toLong() else position
            Text(formatDuration(shown / 1000).ifEmpty { "0:00" }, style = MaterialTheme.typography.bodySmall)
            Text(formatDuration(state.durationMs / 1000), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val active = MaterialTheme.colorScheme.primary
            val inactive = MaterialTheme.colorScheme.onSurfaceVariant
            IconButton(onClick = player::toggleShuffle) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffle) active else inactive)
            }
            IconButton(onClick = player::previous) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
            FilledIconButton(onClick = player::togglePlay, modifier = Modifier.size(72.dp)) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play/Pause",
                    Modifier.size(40.dp),
                )
            }
            IconButton(onClick = player::next) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
            IconButton(onClick = player::cycleRepeat) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) inactive else active,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
