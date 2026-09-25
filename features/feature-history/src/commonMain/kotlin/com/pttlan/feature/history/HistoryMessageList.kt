package com.pttlan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.toRelativeDisplay
import kotlin.time.Instant

private val TopBarClearance = 72.dp
private val PlayerClearance = 110.dp

/** What a room header does: play the room in sequence, or (long press) delete it. */
internal class ChannelActions(
    val onPlay: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

@Composable
internal fun MessageList(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    isPaused: Boolean,
    onPlayClick: (VoiceMessage) -> Unit,
    onDeleteMessage: (VoiceMessage) -> Unit,
    channelActions: ChannelActions,
) {
    var collapsedChannels by remember { mutableStateOf(setOf<String>()) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize().readableWidth(),
        contentPadding =
            PaddingValues(start = 20.dp, end = 20.dp, top = topInset + TopBarClearance, bottom = PlayerClearance),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Áudios recebidos, salvos só neste aparelho",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        messages.groupBy { it.channelId }.forEach { (channelId, channelMessages) ->
            val isCollapsed = channelId in collapsedChannels
            item(key = "header_$channelId") {
                ChannelHeader(
                    channelId = channelId,
                    isCollapsed = isCollapsed,
                    onToggle = {
                        collapsedChannels = if (isCollapsed) collapsedChannels - channelId else collapsedChannels + channelId
                    },
                    onLongClick = { channelActions.onDelete(channelId) },
                    onPlayAll = { channelActions.onPlay(channelId) },
                )
            }
            if (!isCollapsed) {
                item(key = "group_$channelId") {
                    Column(modifier = Modifier.fillMaxWidth().contentCard()) {
                        channelMessages.forEachIndexed { index, message ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 64.dp),
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            VoiceMessageItem(
                                message = message,
                                isPlaying = message.id == playingMessageId,
                                isPaused = message.id == playingMessageId && isPaused,
                                onPlayClick = { onPlayClick(message) },
                                onDeleteClick = { onDeleteMessage(message) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    channelId: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .combinedClickable(onClick = onToggle, onLongClick = onLongClick)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = "# $channelId", modifier = Modifier.weight(1f))
        IconButton(onClick = onPlayAll) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = "Tocar a sala",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = if (isCollapsed) "Expandir" else "Recolher",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VoiceMessageItem(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayButton(isPlaying = isPlaying && !isPaused, isActive = isPlaying, onClick = onPlayClick, size = 40)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    when {
                        isPlaying && isPaused -> {
                            "pausado · ${formatDuration(message.durationMs)}"
                        }

                        isPlaying -> {
                            "tocando · ${formatDuration(message.durationMs)}"
                        }

                        else -> {
                            "${Instant.fromEpochMilliseconds(message.recordedAt).toRelativeDisplay()} · " +
                                formatDuration(message.durationMs)
                        }
                    },
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Apagar áudio",
                tint = PttTheme.customColors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun PlayButton(
    isPlaying: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    size: Int,
) {
    val background = if (isActive) MaterialTheme.colorScheme.primary else PttTheme.customColors.surface3
    val tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pausar" else "Ouvir",
            tint = tint,
        )
    }
}
