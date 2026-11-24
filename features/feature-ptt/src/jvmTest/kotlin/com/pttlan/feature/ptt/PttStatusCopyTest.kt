package com.pttlan.feature.ptt

import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.accessibilityLabel
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_button_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_hint_reconnecting
import com.pttlan.core.designsystem.generated.resources.ptt_hint_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_status_free
import com.pttlan.core.designsystem.generated.resources.ptt_status_receiving_headline
import com.pttlan.core.designsystem.generated.resources.ptt_status_reconnecting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The status, hint and label the Compose and SwiftUI PTT screens both show (32.4). */
class PttStatusCopyTest {
    @Test
    fun theUsersOwnTurnIsAmberWhetherAskingOrOnTheAir() {
        assertEquals(StatusTone.Own, PttButtonState.Requesting.statusCopy().tone)
        assertEquals(StatusTone.Own, PttButtonState.Transmitting.statusCopy().tone)
        assertEquals(StatusTone.Free, PttButtonState.Idle.statusCopy().tone)
        assertEquals(StatusTone.Receiving, PttButtonState.Receiving.statusCopy().tone)
    }

    @Test
    fun onlyTheReceivingHeadlineNamesTheSpeaker() {
        val receiving = PttButtonState.Receiving.statusCopy()
        assertTrue(receiving.namesSpeaker)
        assertEquals(Res.string.ptt_status_receiving_headline, receiving.headline)
        PttButtonState.entries.filter { it != PttButtonState.Receiving }.forEach { assertFalse(it.statusCopy().namesSpeaker, "$it") }
        assertEquals(Res.string.ptt_status_free, PttButtonState.Idle.statusCopy().eyebrow)
    }

    @Test
    fun hintAndLabelFollowTheButtonState() {
        assertEquals(Res.string.ptt_hint_requesting, PttButtonState.Requesting.hint())
        assertEquals(Res.string.ptt_button_receiving, PttButtonState.Receiving.accessibilityLabel())
    }

    @Test
    fun offlineTheStatusSaysReconnectingWhateverTheButton() {
        val offline = PttState(isOnline = false, currentSpeakerId = "user-2")
        assertEquals(Res.string.ptt_status_reconnecting, offline.statusCopy().eyebrow)
        assertEquals(StatusTone.Offline, offline.statusCopy().tone)
        assertEquals(Res.string.ptt_hint_reconnecting, offline.hint())
        val online = offline.copy(isOnline = true)
        assertEquals(PttButtonState.Receiving.statusCopy(), online.statusCopy())
        assertEquals(PttButtonState.Receiving.hint(), online.hint())
    }
}
