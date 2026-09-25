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
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.toRelativeDisplay
import kotlin.time.Instant

@Composable
fun HistoryScreen(component: HistoryComponent) {
    val messages by component.messages.collectAsState()
    val playingMessageId by component.playingMessageId.collectAsState()
    val isPaused by component.isPaused.collectAsState()
    val playbackPosition by component.playbackPosition.collectAsState()
    val queue by component.queue.collectAsState()

    HistoryScreenContent(
        messages = messages,
        playingMessageId = playingMessageId,
        isPaused = isPaused,
        playbackPosition = playbackPosition,
        queue = queue,
        onPlayClick = component::playMessage,
        onPlayChannelClick = component::playChannel,
        onPrevious = component::playPrevious,
        onNext = component::playNext,
        onSeek = component::seekTo,
        onClearCacheClick = component::clearAllMessages,
        onDeleteMessage = component::deleteMessage,
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
    queue: List<VoiceMessage> = emptyList(),
    onPlayClick: (VoiceMessage) -> Unit,
    onPlayChannelClick: (String) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onClearCacheClick: () -> Unit,
    onDeleteMessage: (VoiceMessage) -> Unit,
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
            intensity = 0.26f,
            modifier = Modifier.size(460.dp).align(Alignment.BottomStart).offset((-160).dp, 120.dp),
        )

        if (messages.isEmpty()) {
            EmptyHistory(modifier = Modifier.align(Alignment.Center))
        } else {
            MessageList(
                messages = messages,
                playingMessageId = playingMessageId,
                isPaused = isPaused,
                onPlayClick = onPlayClick,
                onDeleteMessage = { messageToDelete = it },
                channelActions = ChannelActions(onPlay = onPlayChannelClick, onDelete = { channelToDelete = it }),
            )
        }

        PttTopBar(
            modifier = Modifier.readableWidth(),
            navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack) },
            actions = {
                if (messages.isNotEmpty()) {
                    GlassIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Limpar histórico",
                        onClick = { showClearDialog = true },
                        tint = PttTheme.customColors.statusOffline,
                    )
                }
            },
        )

        if (playingMessage != null) {
            MiniPlayer(
                message = playingMessage,
                isPaused = isPaused,
                queue =
                    if (queueIndex >= 0) {
                        QueueControls("${queueIndex + 1} de ${queue.size}", queueIndex < queue.lastIndex, onPrevious, onNext)
                    } else {
                        null
                    },
                position = playbackPosition?.takeIf { it.messageId == playingMessage.id },
                actions = PlayerActions(onPlayPause = { onPlayClick(playingMessage) }, onSeek = onSeek),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    messageToDelete?.let { message ->
        ConfirmDialog(
            title = "Apagar áudio",
            text = "Tem certeza que deseja apagar este áudio?",
            confirmLabel = "Apagar",
            onConfirm = { onDeleteMessage(message) },
            onDismiss = { messageToDelete = null },
        )
    }
    channelToDelete?.let { channelId ->
        ConfirmDialog(
            title = "Apagar canal",
            text = "Tem certeza que deseja apagar todos os áudios do canal #$channelId?",
            confirmLabel = "Apagar",
            onConfirm = { onDeleteChannelClick(channelId) },
            onDismiss = { channelToDelete = null },
        )
    }
    if (showClearDialog) {
        ConfirmDialog(
            title = "Limpar histórico",
            text = "Tem certeza que deseja apagar todos os áudios gravados? Esta ação não pode ser desfeita.",
            confirmLabel = "Limpar",
            onConfirm = onClearCacheClick,
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PttTheme.customColors.textTertiary,
        )
        Text(
            text = "Nenhum áudio salvo ainda.",
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
