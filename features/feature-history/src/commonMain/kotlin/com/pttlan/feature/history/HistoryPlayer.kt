package com.pttlan.feature.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val PROGRESS_TRACK_ALPHA = 0.18f
private const val PROGRESS_LABEL_ALPHA = 0.7f

/** How far the rewind and forward buttons jump. */
private const val SEEK_STEP_MS = 5_000L

/** What the mini player's buttons and progress bar do. */
internal class PlayerActions(
    val onPlayPause: () -> Unit,
    val onSeek: (positionMs: Long) -> Unit,
)

/** The mini player's controls while a room plays in sequence. */
internal class QueueControls(
    val label: String,
    val hasNext: Boolean,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

@Composable
internal fun MiniPlayer(
    message: VoiceMessage,
    isPaused: Boolean,
    queue: QueueControls?,
    position: PlaybackPosition?,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .readableWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SenderAvatar(message.senderNickname)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.senderNickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // In a queue its position takes the state's place: the play button already shows it
                val state = queue?.label ?: if (isPaused) "pausado" else "tocando"
                SectionLabel(text = "# ${message.channelId} · $state")
            }
            PlayButton(isPlaying = !isPaused, isActive = true, onClick = actions.onPlayPause, size = 52)
        }
        PlaybackControls(position, fallbackDurationMs = message.durationMs, queue = queue, onSeek = actions.onSeek)
    }
}

@Composable
private fun SenderAvatar(nickname: String) {
    Box(
        modifier =
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PttTheme.customColors.primaryGlow),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * A draggable progress bar over the transport row: previous, back 5 s, "elapsed / total", forward 5 s and next,
 * the skip buttons only while a room plays in sequence.
 */
@Composable
private fun PlaybackControls(
    position: PlaybackPosition?,
    fallbackDurationMs: Long,
    queue: QueueControls?,
    onSeek: (Long) -> Unit,
) {
    val totalMs = position?.durationMs ?: fallbackDurationMs
    val playedMs = position?.positionMs ?: 0L
    val fraction by animateFloatAsState(position?.fraction ?: 0f, label = "playbackProgress")
    // Held while dragging, so the reported position does not pull the thumb back from under the finger
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    SeekBar(
        fraction = dragFraction ?: fraction,
        onDrag = { dragFraction = it },
        onDragEnd = {
            dragFraction?.let { onSeek((it * totalMs).toLong()) }
            dragFraction = null
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (queue != null) {
            IconButton(onClick = queue.onPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior") }
        }
        IconButton(onClick = { onSeek((playedMs - SEEK_STEP_MS).coerceAtLeast(0)) }) {
            Icon(Icons.Default.Replay5, contentDescription = "Voltar 5 segundos")
        }
        Text(
            text = "${formatPosition(dragFraction?.let { (it * totalMs).toLong() } ?: playedMs)} / ${formatDuration(totalMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = PROGRESS_LABEL_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onSeek(playedMs + SEEK_STEP_MS) }) {
            Icon(Icons.Default.Forward5, contentDescription = "Avançar 5 segundos")
        }
        if (queue != null) {
            IconButton(onClick = queue.onNext, enabled = queue.hasNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Próxima")
            }
        }
    }
}

/** Material's slider, drawn as the thin bar the player always had plus a small thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    fraction: Float,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors =
        SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = PROGRESS_TRACK_ALPHA),
        )
    Slider(
        value = fraction,
        onValueChange = onDrag,
        onValueChangeFinished = onDragEnd,
        modifier = Modifier.fillMaxWidth().height(24.dp),
        colors = colors,
        thumb = {
            Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(4.dp),
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}

internal fun formatDuration(durationMs: Long): String = formatPosition((durationMs / MS_PER_SECOND).coerceAtLeast(1) * MS_PER_SECOND)

/** Like [formatDuration], but an elapsed time of zero shows as `0:00` instead of being rounded up. */
private fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs / MS_PER_SECOND
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "${totalSeconds / SECONDS_PER_MINUTE}:$seconds"
}
