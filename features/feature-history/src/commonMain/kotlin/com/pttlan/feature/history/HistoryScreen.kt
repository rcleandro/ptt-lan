package com.pttlan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(component: HistoryComponent) {
    val messages by component.messages.collectAsState()
    val playingMessageId by component.playingMessageId.collectAsState()

    HistoryScreenContent(
        messages = messages,
        playingMessageId = playingMessageId,
        onPlayClick = {
            if (it.id == playingMessageId) {
                component.stopPlaying()
            } else {
                component.playMessage(it)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    onPlayClick: (VoiceMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("No messages in this channel yet.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(modifier),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                VoiceMessageItem(
                    message = message,
                    isPlaying = message.id == playingMessageId,
                    onPlayClick = { onPlayClick(message) },
                )
            }
        }
    }
}

@Composable
fun VoiceMessageItem(
    message: VoiceMessage,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isPlaying) {
                        PttTheme.customColors.primaryGlow
                    } else {
                        PttTheme.customColors.surface2
                    },
                ).border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                .clickable(onClick = onPlayClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val time =
                kotlin.time.Instant
                    .fromEpochMilliseconds(message.recordedAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            val durationSec = (message.durationMs / 1000).coerceAtLeast(1)

            Text(
                text = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')} • $durationSec sec",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    PttTheme {
        Surface {
            HistoryScreenContent(
                messages =
                    listOf(
                        VoiceMessage("1", "channel1", "Leandro", "/path1", 2500, 1721151600000L),
                        VoiceMessage("2", "channel1", "João", "/path2", 5000, 1721151660000L),
                    ),
                playingMessageId = "1",
                onPlayClick = {},
            )
        }
    }
}
