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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
internal fun MiniPlayer(
    message: VoiceMessage,
    isPaused: Boolean,
    queueLabel: String?,
    position: PlaybackPosition?,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .readableWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SenderAvatar(message.senderNickname)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val state = if (isPaused) "pausado" else "tocando"
            SectionLabel(text = listOfNotNull("# ${message.channelId}", queueLabel, state).joinToString(" · "))
            PlaybackProgress(position = position, fallbackDurationMs = message.durationMs)
        }
        PlayButton(isPlaying = !isPaused, isActive = true, onClick = onPlayPause, size = 52)
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

/** Progress bar plus "elapsed / total", both driven by the position the repository reports. */
@Composable
private fun PlaybackProgress(
    position: PlaybackPosition?,
    fallbackDurationMs: Long,
) {
    val totalMs = position?.durationMs ?: fallbackDurationMs
    val fraction by animateFloatAsState(position?.fraction ?: 0f, label = "playbackProgress")

    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = PROGRESS_TRACK_ALPHA)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = "${formatPosition(position?.positionMs ?: 0L)} / ${formatDuration(totalMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = PROGRESS_LABEL_ALPHA),
        )
    }
}

internal fun formatDuration(durationMs: Long): String = formatPosition((durationMs / MS_PER_SECOND).coerceAtLeast(1) * MS_PER_SECOND)

/** Like [formatDuration], but an elapsed time of zero shows as `0:00` instead of being rounded up. */
private fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs / MS_PER_SECOND
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "${totalSeconds / SECONDS_PER_MINUTE}:$seconds"
}
