package com.pttlan.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.common.MIN_ROOM_PIN_LENGTH
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.LabeledTextField
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PttTextField
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.StatusDot
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.common_history
import com.pttlan.core.designsystem.generated.resources.common_settings
import com.pttlan.core.designsystem.generated.resources.connection_connect
import com.pttlan.core.designsystem.generated.resources.connection_connecting
import com.pttlan.core.designsystem.generated.resources.connection_host
import com.pttlan.core.designsystem.generated.resources.connection_host_description
import com.pttlan.core.designsystem.generated.resources.connection_host_title
import com.pttlan.core.designsystem.generated.resources.connection_manual
import com.pttlan.core.designsystem.generated.resources.connection_manual_hint
import com.pttlan.core.designsystem.generated.resources.connection_nickname
import com.pttlan.core.designsystem.generated.resources.connection_on_network
import com.pttlan.core.designsystem.generated.resources.connection_pin
import com.pttlan.core.designsystem.generated.resources.connection_search_again
import com.pttlan.core.designsystem.generated.resources.connection_searching
import com.pttlan.core.designsystem.generated.resources.connection_server_address
import com.pttlan.core.designsystem.generated.resources.connection_server_lan
import com.pttlan.core.designsystem.generated.resources.connection_server_web
import com.pttlan.core.designsystem.generated.resources.connection_subtitle
import com.pttlan.core.designsystem.generated.resources.connection_title
import com.pttlan.core.designsystem.resolveString
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NicknameField(
    nickname: String,
    onIntent: (ConnectionIntent) -> Unit,
) {
    LabeledTextField(
        label = stringResource(Res.string.connection_nickname),
        value = nickname,
        onValueChange = { onIntent(ConnectionIntent.UpdateNickname(it)) },
    )
}

@Composable
internal fun PinField(
    pin: String,
    onIntent: (ConnectionIntent) -> Unit,
) {
    LabeledTextField(
        label = stringResource(Res.string.connection_pin),
        value = pin,
        onValueChange = { onIntent(ConnectionIntent.UpdatePin(it)) },
    )
}

@Composable
internal fun HostCard(onHost: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().contentCard().padding(Dimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.connection_host_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.connection_host_description, MIN_ROOM_PIN_LENGTH),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PillButton(text = stringResource(Res.string.connection_host), onClick = onHost)
    }
}
