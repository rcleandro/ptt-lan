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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.generated.resources.history_collapse
import com.pttlan.core.designsystem.generated.resources.history_delete
import com.pttlan.core.designsystem.generated.resources.history_expand
import com.pttlan.core.designsystem.generated.resources.history_pause
import com.pttlan.core.designsystem.generated.resources.history_play
import com.pttlan.core.designsystem.generated.resources.history_play_room
import com.pttlan.core.designsystem.generated.resources.history_share
import com.pttlan.core.designsystem.generated.resources.history_status_paused
import com.pttlan.core.designsystem.generated.resources.history_status_playing
import com.pttlan.core.designsystem.generated.resources.history_status_recorded
import com.pttlan.core.designsystem.generated.resources.history_subtitle
import com.pttlan.core.designsystem.generated.resources.history_title
import com.pttlan.core.designsystem.generated.resources.history_unheard
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.relativeTimeText
import com.pttlan.feature.history.util.toRelativeTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private val TopBarClearance = 72.dp

/** The mini player's height above the navigation bar: 16 + 10 padding on each side, 52 + 24 + 16 + 48 of rows. */
private val PlayerClearance = 200.dp

/** The divider between messages starts after the play button. */
private val DividerInset = 64.dp
private val MessagePaddingVertical = 10.dp
private val MessagePlayButtonSize = 40.dp
private val MessageActionIconSize = 20.dp

/** What a room header does: play the room or its unheard messages in sequence, or (long press) delete it. */
internal class ChannelActions(
    val onPlay: (String) -> Unit,
    val onPlayUnheard: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

/** What a message row does: play or pause it, share it as a `.wav`, or delete it. */
internal class MessageActions(
    val onPlay: (VoiceMessage) -> Unit,
    val onShare: (VoiceMessage) -> Unit,
    val onDelete: (VoiceMessage) -> Unit,
)

@Composable
internal fun MessageList(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    isPaused: Boolean,
    messageActions: MessageActions,
    channelActions: ChannelActions,
) {
    var collapsedChannels by remember { mutableStateOf(setOf<String>()) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The mini player sits above the navigation bar, so the last message has to clear both
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize().readableWidth(),
        contentPadding =
            PaddingValues(
                start = Dimens.Space2xl,
                end = Dimens.Space2xl,
                top = topInset + TopBarClearance,
                bottom =
                    bottomInset + PlayerClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = Dimens.SpaceMd)) {
                Text(
                    text = stringResource(Res.string.history_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(Res.string.history_subtitle),
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
                    unheardCount = channelMessages.count { it.playedAt == null },
                    isCollapsed = isCollapsed,
                    onToggle = {
                        collapsedChannels = if (isCollapsed) collapsedChannels - channelId else collapsedChannels + channelId
                    },
                    actions = channelActions,
                )
            }
            if (!isCollapsed) {
                item(key = "group_$channelId") {
                    Column(modifier = Modifier.fillMaxWidth().contentCard()) {
                        channelMessages.forEachIndexed { index, message ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = DividerInset),
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            VoiceMessageItem(
                                message = message,
                                isPlaying = message.id == playingMessageId,
                                isPaused = message.id == playingMessageId && isPaused,
                                actions = messageActions,
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
    unheardCount: Int,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    actions: ChannelActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .combinedClickable(onClick = onToggle, onLongClick = { actions.onDelete(channelId) })
                .padding(start = Dimens.SpaceXl, end = Dimens.SpaceMd, top = Dimens.SpaceMd, bottom = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = stringResource(Res.string.channel_name, channelId), modifier = Modifier.weight(1f))
        if (unheardCount > 0) {
            TextButton(onClick = { actions.onPlayUnheard(channelId) }) {
                Text(
                    pluralStringResource(Res.plurals.history_unheard, unheardCount, unheardCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        IconButton(onClick = { actions.onPlay(channelId) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = stringResource(Res.string.history_play_room),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(if (isCollapsed) Res.string.history_expand else Res.string.history_collapse),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun VoiceMessageItem(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
    actions: MessageActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceLg, vertical = MessagePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        PlayButton(
            isPlaying = isPlaying && !isPaused,
            isActive = isPlaying,
            onClick = { actions.onPlay(message) },
            size = MessagePlayButtonSize,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = messageStatus(message, isPlaying, isPaused),
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MessageActionButton(Icons.Default.Share, stringResource(Res.string.history_share)) { actions.onShare(message) }
        MessageActionButton(Icons.Default.Delete, stringResource(Res.string.history_delete)) { actions.onDelete(message) }
    }
}

@Composable
private fun MessageActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PttTheme.customColors.textTertiary,
            modifier = Modifier.size(MessageActionIconSize),
        )
    }
}

/** "tocando · 0:12", "pausado · 0:12", or when it was recorded and how long it is. */
@Composable
private fun messageStatus(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
): String {
    val duration = formatDuration(message.durationMs)
    return when {
        isPlaying && isPaused -> {
            stringResource(Res.string.history_status_paused, duration)
        }

        isPlaying -> {
            stringResource(Res.string.history_status_playing, duration)
        }

        else -> {
            stringResource(
                Res.string.history_status_recorded,
                relativeTimeText(Instant.fromEpochMilliseconds(message.recordedAt).toRelativeTime()),
                duration,
            )
        }
    }
}

@Composable
internal fun PlayButton(
    isPlaying: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val background = if (isActive) MaterialTheme.colorScheme.primary else PttTheme.customColors.surface3
    val tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .border(Dimens.Hairline, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = stringResource(if (isPlaying) Res.string.history_pause else Res.string.history_play),
            tint = tint,
        )
    }
}
