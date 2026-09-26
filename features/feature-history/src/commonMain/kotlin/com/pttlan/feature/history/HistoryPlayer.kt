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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.generated.resources.decimal_separator
import com.pttlan.core.designsystem.generated.resources.history_back_5
import com.pttlan.core.designsystem.generated.resources.history_forward_5
import com.pttlan.core.designsystem.generated.resources.history_next
import com.pttlan.core.designsystem.generated.resources.history_paused
import com.pttlan.core.designsystem.generated.resources.history_player_subtitle
import com.pttlan.core.designsystem.generated.resources.history_playing
import com.pttlan.core.designsystem.generated.resources.history_previous
import com.pttlan.core.designsystem.generated.resources.history_speed
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import org.jetbrains.compose.resources.stringResource

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val PROGRESS_TRACK_ALPHA = 0.18f
private const val PROGRESS_LABEL_ALPHA = 0.7f

/** How far the rewind and forward buttons jump. */
private const val SEEK_STEP_MS = 5_000L

/** The sender's avatar and the play button of the mini player. */
private val PlayerControlSize = 52.dp
private val PlayerPadding = 10.dp
private val SeekBarHeight = 24.dp
private val SeekThumbSize = 12.dp
private val SeekTrackHeight = 4.dp

/** What the mini player shows about the message playing. */
internal class PlayerState(
    val isPaused: Boolean,
    val position: PlaybackPosition?,
    val speed: Float,
)

/** What the mini player's buttons and progress bar do. */
internal class PlayerActions(
    val onPlayPause: () -> Unit,
    val onSeek: (positionMs: Long) -> Unit,
    val onCycleSpeed: () -> Unit,
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
    state: PlayerState,
    queue: QueueControls?,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .readableWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceXl)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(PlayerPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
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
                val status = queue?.label ?: stringResource(if (state.isPaused) Res.string.history_paused else Res.string.history_playing)
                SectionLabel(
                    text =
                        stringResource(
                            Res.string.history_player_subtitle,
                            stringResource(Res.string.channel_name, message.channelId),
                            status,
                        ),
                )
            }
            PlayButton(isPlaying = !state.isPaused, isActive = true, onClick = actions.onPlayPause, size = PlayerControlSize)
        }
        PlaybackControls(state, fallbackDurationMs = message.durationMs, queue = queue, actions = actions)
    }
}

@Composable
private fun SenderAvatar(nickname: String) {
    Box(
        modifier =
            Modifier
                .size(PlayerControlSize)
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

/** A draggable progress bar, elapsed and total time at its ends, and the transport row below. */
@Composable
private fun PlaybackControls(
    state: PlayerState,
    fallbackDurationMs: Long,
    queue: QueueControls?,
    actions: PlayerActions,
) {
    val totalMs = state.position?.durationMs ?: fallbackDurationMs
    val fraction by animateFloatAsState(state.position?.fraction ?: 0f, label = "playbackProgress")
    // Held while dragging, so the reported position does not pull the thumb back from under the finger
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    SeekBar(
        fraction = dragFraction ?: fraction,
        onDrag = { dragFraction = it },
        onDragEnd = {
            dragFraction?.let { actions.onSeek((it * totalMs).toLong()) }
            dragFraction = null
        },
    )
    val shownMs = dragFraction?.let { (it * totalMs).toLong() } ?: state.position?.positionMs ?: 0L
    Row {
        TimeLabel(formatPosition(shownMs), Modifier.weight(1f))
        TimeLabel(formatDuration(totalMs))
    }
    TransportRow(shownMs = shownMs, speed = state.speed, queue = queue, actions = actions)
}

@Composable
private fun TimeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = PROGRESS_LABEL_ALPHA),
        modifier = modifier,
    )
}

/** Previous, back 5 s, the speed, forward 5 s and next; the skips only while a room plays in sequence. */
@Composable
private fun TransportRow(
    shownMs: Long,
    speed: Float,
    queue: QueueControls?,
    actions: PlayerActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (queue != null) {
            IconButton(onClick = queue.onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(Res.string.history_previous))
            }
        }
        IconButton(onClick = { actions.onSeek((shownMs - SEEK_STEP_MS).coerceAtLeast(0)) }) {
            Icon(Icons.Default.Replay5, contentDescription = stringResource(Res.string.history_back_5))
        }
        TextButton(onClick = actions.onCycleSpeed) {
            Text(formatSpeed(speed), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        IconButton(onClick = { actions.onSeek(shownMs + SEEK_STEP_MS) }) {
            Icon(Icons.Default.Forward5, contentDescription = stringResource(Res.string.history_forward_5))
        }
        if (queue != null) {
            IconButton(onClick = queue.onNext, enabled = queue.hasNext) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(Res.string.history_next))
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
        modifier = Modifier.fillMaxWidth().height(SeekBarHeight),
        colors = colors,
        thumb = {
            Box(Modifier.size(SeekThumbSize).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(SeekTrackHeight),
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}

/** "1×", "1,5×", "2×". */
@Composable
private fun formatSpeed(speed: Float): String {
    val number =
        if (speed % 1f ==
            0f
        ) {
            speed.toInt().toString()
        } else {
            speed.toString().replace(".", stringResource(Res.string.decimal_separator))
        }
    return stringResource(Res.string.history_speed, number)
}

internal fun formatDuration(durationMs: Long): String = formatPosition((durationMs / MS_PER_SECOND).coerceAtLeast(1) * MS_PER_SECOND)

/** Like [formatDuration], but an elapsed time of zero shows as `0:00` instead of being rounded up. */
private fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs / MS_PER_SECOND
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "${totalSeconds / SECONDS_PER_MINUTE}:$seconds"
}
