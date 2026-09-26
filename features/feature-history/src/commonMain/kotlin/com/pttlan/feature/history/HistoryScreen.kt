package com.pttlan.feature.history

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.history_back
import com.pttlan.core.designsystem.generated.resources.history_clear
import com.pttlan.core.designsystem.generated.resources.history_clear_confirm
import com.pttlan.core.designsystem.generated.resources.history_clear_text
import com.pttlan.core.designsystem.generated.resources.history_delete_channel_text
import com.pttlan.core.designsystem.generated.resources.history_delete_channel_title
import com.pttlan.core.designsystem.generated.resources.history_delete_confirm
import com.pttlan.core.designsystem.generated.resources.history_delete_text
import com.pttlan.core.designsystem.generated.resources.history_delete_title
import com.pttlan.core.designsystem.generated.resources.history_empty
import com.pttlan.core.designsystem.generated.resources.history_queue_position
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private const val GLOW_INTENSITY = 0.26f
private val GlowSize = 460.dp
private val GlowOffsetX = (-160).dp
private val GlowOffsetY = 120.dp
private val EmptyIconSize = 64.dp

@Composable
fun HistoryScreen(component: HistoryComponent) {
    val playback = component.playback
    val messages by component.messages.collectAsState()
    val playingMessageId by playback.playingMessageId.collectAsState()
    val isPaused by playback.isPaused.collectAsState()
    val playbackPosition by playback.playbackPosition.collectAsState()
    val playbackSpeed by playback.playbackSpeed.collectAsState()
    val queue by playback.queue.collectAsState()

    HistoryScreenContent(
        messages = messages,
        playingMessageId = playingMessageId,
        isPaused = isPaused,
        playbackPosition = playbackPosition,
        playbackSpeed = playbackSpeed,
        queue = queue,
        onPlayClick = playback::playMessage,
        onPlayChannelClick = playback::playChannel,
        onPlayUnheardClick = playback::playUnheard,
        onPrevious = playback::playPrevious,
        onNext = playback::playNext,
        onSeek = playback::seekTo,
        onCycleSpeed = playback::cycleSpeed,
        onClearCacheClick = component::clearAllMessages,
        onDeleteMessage = component::deleteMessage,
        onShareMessage = component::shareMessage,
        onDeleteChannelClick = component::deleteChannelMessages,
        onBack = component::onBack,
    )
}

@Composable
fun HistoryScreenContent(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    isPaused: Boolean,
    playbackPosition: PlaybackPosition? = null,
    playbackSpeed: Float = 1f,
    queue: List<VoiceMessage> = emptyList(),
    onPlayClick: (VoiceMessage) -> Unit,
    onPlayChannelClick: (String) -> Unit = {},
    onPlayUnheardClick: (String) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onClearCacheClick: () -> Unit,
    onDeleteMessage: (VoiceMessage) -> Unit,
    onShareMessage: (VoiceMessage) -> Unit = {},
    onDeleteChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<VoiceMessage?>(null) }
    var channelToDelete by remember { mutableStateOf<String?>(null) }
    val playingMessage = messages.find { it.id == playingMessageId }
    val queueIndex = queue.indexOfFirst { it.id == playingMessageId }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            intensity = GLOW_INTENSITY,
            modifier = Modifier.size(GlowSize).align(Alignment.BottomStart).offset(GlowOffsetX, GlowOffsetY),
        )

        if (messages.isEmpty()) {
            EmptyHistory(modifier = Modifier.align(Alignment.Center))
        } else {
            MessageList(
                messages = messages,
                playingMessageId = playingMessageId,
                isPaused = isPaused,
                messageActions = MessageActions(onPlay = onPlayClick, onShare = onShareMessage, onDelete = { messageToDelete = it }),
                channelActions =
                    ChannelActions(
                        onPlay = onPlayChannelClick,
                        onPlayUnheard = onPlayUnheardClick,
                        onDelete = { channelToDelete = it },
                    ),
            )
        }

        PttTopBar(
            modifier = Modifier.readableWidth(),
            navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.history_back), onBack) },
            actions = {
                if (messages.isNotEmpty()) {
                    GlassIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.history_clear),
                        onClick = { showClearDialog = true },
                        tint = PttTheme.customColors.statusOffline,
                    )
                }
            },
        )

        if (playingMessage != null) {
            MiniPlayer(
                message = playingMessage,
                state = PlayerState(isPaused, playbackPosition?.takeIf { it.messageId == playingMessage.id }, playbackSpeed),
                queue =
                    if (queueIndex >= 0) {
                        QueueControls(
                            stringResource(Res.string.history_queue_position, queueIndex + 1, queue.size),
                            queueIndex < queue.lastIndex,
                            onPrevious,
                            onNext,
                        )
                    } else {
                        null
                    },
                actions = PlayerActions(onPlayPause = { onPlayClick(playingMessage) }, onSeek = onSeek, onCycleSpeed = onCycleSpeed),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    messageToDelete?.let { message ->
        ConfirmDialog(
            title = stringResource(Res.string.history_delete_title),
            text = stringResource(Res.string.history_delete_text),
            confirmLabel = stringResource(Res.string.history_delete_confirm),
            onConfirm = { onDeleteMessage(message) },
            onDismiss = { messageToDelete = null },
        )
    }
    channelToDelete?.let { channelId ->
        ConfirmDialog(
            title = stringResource(Res.string.history_delete_channel_title),
            text = stringResource(Res.string.history_delete_channel_text, channelId),
            confirmLabel = stringResource(Res.string.history_delete_confirm),
            onConfirm = { onDeleteChannelClick(channelId) },
            onDismiss = { channelToDelete = null },
        )
    }
    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.history_clear),
            text = stringResource(Res.string.history_clear_text),
            confirmLabel = stringResource(Res.string.history_clear_confirm),
            onConfirm = onClearCacheClick,
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Dimens.Space4xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXl),
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(EmptyIconSize),
            tint = PttTheme.customColors.textTertiary,
        )
        Text(
            text = stringResource(Res.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val previewMessages =
    listOf(
        VoiceMessage("1", "Geral", "Marcos", "/path1", 12_000, 1721151600000L),
        VoiceMessage("2", "Geral", "Júlia", "/path2", 6_000, 1721151660000L),
        VoiceMessage("3", "Obra Alameda", "Rafael", "/path3", 9_000, 1721151700000L),
    )

@Preview
@Composable
private fun HistoryScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            HistoryScreenContent(
                messages = previewMessages,
                playingMessageId = "1",
                isPaused = false,
                onPlayClick = {},
                onClearCacheClick = {},
                onDeleteMessage = {},
                onDeleteChannelClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun HistoryScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            HistoryScreenContent(
                messages = previewMessages,
                playingMessageId = null,
                isPaused = false,
                onPlayClick = {},
                onClearCacheClick = {},
                onDeleteMessage = {},
                onDeleteChannelClick = {},
            )
        }
    }
}
