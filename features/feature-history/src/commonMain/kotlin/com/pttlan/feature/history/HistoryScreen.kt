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
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.toRelativeDisplay
import kotlin.time.Instant

private val TopBarClearance = 72.dp
private val PlayerClearance = 110.dp
private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L

@Composable
fun HistoryScreen(component: HistoryComponent) {
    val messages by component.messages.collectAsState()
    val playingMessageId by component.playingMessageId.collectAsState()
    val isPaused by component.isPaused.collectAsState()

    HistoryScreenContent(
        messages = messages,
        playingMessageId = playingMessageId,
        isPaused = isPaused,
        onPlayClick = component::playMessage,
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
    onPlayClick: (VoiceMessage) -> Unit,
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
                onDeleteChannel = { channelToDelete = it },
            )
        }

        PttTopBar(
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
                onPlayPause = { onPlayClick(playingMessage) },
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

@Composable
private fun MessageList(
    messages: List<VoiceMessage>,
    playingMessageId: String?,
    isPaused: Boolean,
    onPlayClick: (VoiceMessage) -> Unit,
    onDeleteMessage: (VoiceMessage) -> Unit,
    onDeleteChannel: (String) -> Unit,
) {
    var collapsedChannels by remember { mutableStateOf(setOf<String>()) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    onLongClick = { onDeleteChannel(channelId) },
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
private fun PlayButton(
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

@Composable
private fun MiniPlayer(
    message: VoiceMessage,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PttTheme.customColors.primaryGlow),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.senderNickname.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderNickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SectionLabel(text = "# ${message.channelId} · ${if (isPaused) "pausado" else "tocando"}")
        }
        PlayButton(isPlaying = !isPaused, isActive = true, onClick = onPlayPause, size = 52)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(confirmLabel, color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / MS_PER_SECOND).coerceAtLeast(1)
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "${totalSeconds / SECONDS_PER_MINUTE}:$seconds"
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
