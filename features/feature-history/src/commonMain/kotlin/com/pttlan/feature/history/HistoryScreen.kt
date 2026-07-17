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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.toRelativeDisplay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

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
        onClearCacheClick = {
            component.clearAllMessages()
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
    onClearCacheClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
        if (messages.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
            var collapsedChannels by remember { mutableStateOf(setOf<String>()) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val groupedMessages = messages.groupBy { it.channelId }
                groupedMessages.forEach { (channelId, channelMessages) ->
                    val isCollapsed = collapsedChannels.contains(channelId)
                    item(key = "header_$channelId") {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                    .clickable {
                                        collapsedChannels =
                                            if (isCollapsed) {
                                                collapsedChannels - channelId
                                            } else {
                                                collapsedChannels + channelId
                                            }
                                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Canal: #$channelId",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Expandir" else "Recolher",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (!isCollapsed) {
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

        var showClearDialog by remember { mutableStateOf(false) }

        FloatingActionButton(
            onClick = { showClearDialog = true },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Limpar Cache",
            )
        }

        if (showClearDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Limpar Histórico") },
                text = { Text("Tem certeza que deseja apagar todos os áudios gravados? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showClearDialog = false
                            onClearCacheClick()
                        },
                    ) {
                        Text("Limpar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancelar")
                    }
                },
            )
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
            val recordedAt = Instant.fromEpochMilliseconds(message.recordedAt)
            val durationSec = (message.durationMs / 1000).coerceAtLeast(1)

            Text(
                text = "${recordedAt.toRelativeDisplay()} • $durationSec sec",
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
                        VoiceMessage(
                            "3",
                            "channel1",
                            "Maria",
                            "/path3",
                            3000,
                            (Clock.System.now() - Duration.parse("PT2H15M")).toEpochMilliseconds(),
                        ),
                    ),
                playingMessageId = "1",
                isPaused = false,
                onPlayClick = {},
                onClearCacheClick = {},
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
                onClearCacheClick = {},
            )
        }
    }
}
