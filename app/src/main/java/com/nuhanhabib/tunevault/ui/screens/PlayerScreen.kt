package com.nuhanhabib.tunevault.ui.screens

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.playback.PlayerConnection
import com.nuhanhabib.tunevault.ui.MainViewModel
import com.nuhanhabib.tunevault.ui.components.Artwork
import com.nuhanhabib.tunevault.ui.components.SongActions
import com.nuhanhabib.tunevault.ui.components.formatDuration
import com.nuhanhabib.tunevault.ui.theme.AppIcons
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
    val progress = if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(Modifier.clickable(onClick = onExpand)) {
            Row(
                Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(state.artwork, 46.dp, corner = 12.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = player::togglePlay) {
                    Icon(
                        if (state.isPlaying) AppIcons.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(onClick = player::next) {
                    Icon(AppIcons.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(vm: MainViewModel, onCollapse: () -> Unit) {
    val player = vm.player
    val state by player.state.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val position = rememberPosition(player, state.isPlaying, state.trackId)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var sheetFor by remember { mutableStateOf<TrackEntity?>(null) }
    val currentTrack = songs.firstOrNull { it.id == state.trackId }
    val artScale by animateFloatAsState(if (state.isPlaying) 1f else 0.88f, label = "art")

    LaunchedEffect(state.trackId) { if (state.trackId == null) onCollapse() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Blurred cover as a soft background.
        if (state.artwork != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = state.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier.fillMaxSize().blur(80.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), MaterialTheme.colorScheme.surface)
                )
            )
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) { Icon(Icons.Default.KeyboardArrowDown, "Close player", Modifier.size(30.dp)) }
                Text(
                    "Now playing",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { sheetFor = currentTrack }, enabled = currentTrack != null) {
                    Icon(AppIcons.PlaylistAdd, "Add to playlist")
                }
            }

            Spacer(Modifier.weight(0.6f))
            Artwork(
                state.artwork,
                size = 320.dp,
                corner = 24.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).scale(artScale),
            )
            Spacer(Modifier.weight(0.6f))

            Column(Modifier.fillMaxWidth()) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(20.dp))

            val duration = state.durationMs.coerceAtLeast(1L)
            Slider(
                value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    player.seekTo((dragValue * duration).toLong())
                    dragging = false
                },
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val shown = if (dragging) (dragValue * duration).toLong() else position
                Text(
                    formatDuration(shown / 1000).ifEmpty { "0:00" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatDuration(state.durationMs / 1000),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val active = MaterialTheme.colorScheme.primary
                val inactive = MaterialTheme.colorScheme.onSurfaceVariant
                IconButton(onClick = player::toggleShuffle) {
                    Icon(AppIcons.Shuffle, "Shuffle", tint = if (state.shuffle) active else inactive)
                }
                IconButton(onClick = player::previous, modifier = Modifier.size(56.dp)) {
                    Icon(AppIcons.SkipPrevious, "Previous", Modifier.size(38.dp))
                }
                FilledIconButton(onClick = player::togglePlay, modifier = Modifier.size(80.dp)) {
                    Icon(
                        if (state.isPlaying) AppIcons.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        Modifier.size(42.dp),
                    )
                }
                IconButton(onClick = player::next, modifier = Modifier.size(56.dp)) {
                    Icon(AppIcons.SkipNext, "Next", Modifier.size(38.dp))
                }
                IconButton(onClick = player::cycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) AppIcons.RepeatOne else AppIcons.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) inactive else active,
                    )
                }
            }
            Spacer(Modifier.weight(0.5f))
        }
    }

    sheetFor?.let { SongActions(vm, it, onDismiss = { sheetFor = null }, startWithPlaylists = true) }
}
