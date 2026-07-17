package com.pttlan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextAlign
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
    val isPaused by component.isPaused.collectAsState()

    HistoryScreenContent(
        messages = messages,
        playingMessageId = playingMessageId,
        isPaused = isPaused,
        onPlayClick = {
            component.playMessage(it)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    isPaused: Boolean,
    onPlayClick: (VoiceMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhum áudio salvo ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
            val groupedMessages = messages.groupBy { it.channelId }
            groupedMessages.forEach { (channelId, channelMessages) ->
                item(key = "header_$channelId") {
                    Text(
                        text = "Canal: #$channelId",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(channelMessages, key = { it.id }) { message ->
                    VoiceMessageItem(
                        message = message,
                        isPlaying = message.id == playingMessageId,
                        isPaused = message.id == playingMessageId && isPaused,
                        onPlayClick = { onPlayClick(message) },
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMessageItem(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
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

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (isPlaying && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying && !isPaused) "Pause" else "Play",
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun HistoryScreenPreviewDark() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.DARK) {
        Surface {
            HistoryScreenContent(
                messages =
                    listOf(
                        VoiceMessage("1", "channel1", "Leandro", "/path1", 2500, 1721151600000L),
                        VoiceMessage("2", "channel1", "João", "/path2", 5000, 1721151660000L),
                    ),
                playingMessageId = "1",
                isPaused = false,
                onPlayClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun HistoryScreenPreviewLight() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.LIGHT) {
        Surface {
            HistoryScreenContent(
                messages =
                    listOf(
                        VoiceMessage("1", "channel1", "Leandro", "/path1", 2500, 1721151600000L),
                        VoiceMessage("2", "channel1", "João", "/path2", 5000, 1721151660000L),
                    ),
                playingMessageId = "1",
                isPaused = false,
                onPlayClick = {},
            )
        }
    }
}
