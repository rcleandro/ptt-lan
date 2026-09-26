package com.pttlan.feature.ptt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ConnectionStatus
import com.pttlan.core.designsystem.components.ConnectionStatusBadge
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.LocalTabletopFold
import com.pttlan.core.designsystem.components.ParticipantAvatar
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PillButtonStyle
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.StatusDot
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_channel_busy
import com.pttlan.core.designsystem.generated.resources.ptt_hint_idle
import com.pttlan.core.designsystem.generated.resources.ptt_hint_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_hint_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_hint_transmitting
import com.pttlan.core.designsystem.generated.resources.ptt_in_channel
import com.pttlan.core.designsystem.generated.resources.ptt_leave
import com.pttlan.core.designsystem.generated.resources.ptt_someone
import com.pttlan.core.designsystem.generated.resources.ptt_status_free
import com.pttlan.core.designsystem.generated.resources.ptt_status_free_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_status_receiving_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_status_requesting_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_transmitting
import com.pttlan.core.designsystem.generated.resources.ptt_status_transmitting_headline
import com.pttlan.core.designsystem.resolveString
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.model.ParticipantDomain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatusBlock(
    buttonState: PttButtonState,
    speakerName: String?,
) {
    val (eyebrow, color: Color, headline) = statusCopy(buttonState, speakerName)

    Column(
        modifier = Modifier.padding(horizontal = Dimens.Space3xl).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = color, halo = buttonState != PttButtonState.Idle)
            SectionLabel(text = eyebrow, color = color)
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Eyebrow, its color and the headline above the button, for each state. */
@Composable
private fun statusCopy(
    buttonState: PttButtonState,
    speakerName: String?,
): Triple<String, Color, String> {
    val colors = PttTheme.customColors
    return when (buttonState) {
        PttButtonState.Idle -> {
            Triple(stringResource(Res.string.ptt_status_free), colors.statusOnline, stringResource(Res.string.ptt_status_free_headline))
        }

        PttButtonState.Requesting -> {
            Triple(
                stringResource(Res.string.ptt_status_requesting),
                colors.accentTx,
                stringResource(Res.string.ptt_status_requesting_headline),
            )
        }

        PttButtonState.Transmitting -> {
            Triple(
                stringResource(Res.string.ptt_status_transmitting),
                colors.accentTx,
                stringResource(Res.string.ptt_status_transmitting_headline),
            )
        }

        PttButtonState.Receiving -> {
            Triple(
                stringResource(Res.string.ptt_status_receiving),
                MaterialTheme.colorScheme.primary,
                stringResource(Res.string.ptt_status_receiving_headline, speakerName ?: stringResource(Res.string.ptt_someone)),
            )
        }
    }
}

internal fun PttButtonState.hint(): StringResource =
    when (this) {
        PttButtonState.Idle -> Res.string.ptt_hint_idle
        PttButtonState.Requesting -> Res.string.ptt_hint_requesting
        PttButtonState.Transmitting -> Res.string.ptt_hint_transmitting
        PttButtonState.Receiving -> Res.string.ptt_hint_receiving
    }
