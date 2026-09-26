package com.pttlan.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SNAPSHOT_DIR = "src/androidHostTest/snapshots"

/**
 * One snapshot per design system component (23.5). They are the guardrail for the rules in ADR 0006:
 * amber only for the local user transmitting or requesting, blue for receiving, green for connected.
 */
@OptIn(ExperimentalResourceApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class DesignSystemSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshot(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            // Robolectric does not start the provider that hands the Android context to Compose resources
            CompositionLocalProvider(LocalInspectionMode provides true) { PreviewContextConfigurationEffect() }
            PttTheme(appTheme = AppTheme.DARK) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage("$SNAPSHOT_DIR/$name.png")
    }

    @Test
    fun pttButtonTransmitting() {
        snapshot("ptt_button_transmitting") {
            PttButton(state = PttButtonState.Transmitting, onPressStart = {}, onPressEnd = {})
        }
    }

    @Test
    fun pttButtonReceiving() {
        snapshot("ptt_button_receiving") {
            PttButton(state = PttButtonState.Receiving, onPressStart = {}, onPressEnd = {})
        }
    }

    @Test
    fun connectionStatusBadges() {
        snapshot("connection_status_reconnecting") {
            ConnectionStatusBadge(status = ConnectionStatus.Reconnecting)
        }
    }

    @Test
    fun channelCardWithParticipants() {
        snapshot("channel_card") {
            ChannelCard(name = "Obra Alameda", participantCount = 3, onClick = {})
        }
    }

    @Test
    fun participantAvatarSpeaking() {
        snapshot("participant_avatar_speaking") {
            ParticipantAvatar(name = "Leandro", isSpeaking = true, isSelf = true)
        }
    }
}
