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
import com.pttlan.core.designsystem.generated.resources.ptt_hint_reconnecting
import com.pttlan.core.designsystem.generated.resources.ptt_hint_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_hint_transmitting
import com.pttlan.core.designsystem.generated.resources.ptt_in_channel
import com.pttlan.core.designsystem.generated.resources.ptt_leave
import com.pttlan.core.designsystem.generated.resources.ptt_someone
import com.pttlan.core.designsystem.generated.resources.ptt_status_free
import com.pttlan.core.designsystem.generated.resources.ptt_status_free_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_status_receiving_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_reconnecting
import com.pttlan.core.designsystem.generated.resources.ptt_status_reconnecting_headline
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
internal fun StatusBlock(state: PttState) {
    val buttonState = state.buttonState()
    val (eyebrow, color: Color, headline) = statusCopy(state.statusCopy(), state.currentSpeakerName)

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
    copy: StatusCopy,
    speakerName: String?,
): Triple<String, Color, String> {
    val color =
        when (copy.tone) {
            StatusTone.Free -> PttTheme.customColors.statusOnline
            StatusTone.Own -> PttTheme.customColors.accentTx
            StatusTone.Receiving -> MaterialTheme.colorScheme.primary
            StatusTone.Offline -> PttTheme.customColors.statusIdle
        }
    val headline =
        if (copy.namesSpeaker) {
            stringResource(copy.headline, speakerName ?: stringResource(Res.string.ptt_someone))
        } else {
            stringResource(copy.headline)
        }
    return Triple(stringResource(copy.eyebrow), color, headline)
}

/** Color the status speaks in: green for a free channel, amber for the user's own turn, blue for someone else's. */
enum class StatusTone { Free, Own, Receiving, Offline }

/**
 * What the status above the button says in each state, shared by the Compose and SwiftUI screens (32.4).
 * When [namesSpeaker], [headline] takes the speaker's name, or "Alguém" when the server did not send one.
 */
data class StatusCopy(
    val eyebrow: StringResource,
    val headline: StringResource,
    val tone: StatusTone,
    val namesSpeaker: Boolean = false,
)

fun PttButtonState.statusCopy(): StatusCopy =
    when (this) {
        PttButtonState.Idle -> {
            StatusCopy(Res.string.ptt_status_free, Res.string.ptt_status_free_headline, StatusTone.Free)
        }

        PttButtonState.Requesting -> {
            StatusCopy(Res.string.ptt_status_requesting, Res.string.ptt_status_requesting_headline, StatusTone.Own)
        }

        PttButtonState.Transmitting -> {
            StatusCopy(Res.string.ptt_status_transmitting, Res.string.ptt_status_transmitting_headline, StatusTone.Own)
        }

        PttButtonState.Receiving -> {
            StatusCopy(Res.string.ptt_status_receiving, Res.string.ptt_status_receiving_headline, StatusTone.Receiving, namesSpeaker = true)
        }
    }

/** While the connection is down nothing can be said or heard: the status says so instead of "free". */
private val ReconnectingCopy =
    StatusCopy(Res.string.ptt_status_reconnecting, Res.string.ptt_status_reconnecting_headline, StatusTone.Offline)

/** The status above the button for the whole screen: the button's state, or reconnecting while offline. */
fun PttState.statusCopy(): StatusCopy = if (isOnline) buttonState().statusCopy() else ReconnectingCopy

/** The line under the button for the whole screen: the button's hint, or what happens while offline. */
fun PttState.hint(): StringResource = if (isOnline) buttonState().hint() else Res.string.ptt_hint_reconnecting

/** Line under the button: what holding or releasing it does now. */
fun PttButtonState.hint(): StringResource =
    when (this) {
        PttButtonState.Idle -> Res.string.ptt_hint_idle
        PttButtonState.Requesting -> Res.string.ptt_hint_requesting
        PttButtonState.Transmitting -> Res.string.ptt_hint_transmitting
        PttButtonState.Receiving -> Res.string.ptt_hint_receiving
    }
