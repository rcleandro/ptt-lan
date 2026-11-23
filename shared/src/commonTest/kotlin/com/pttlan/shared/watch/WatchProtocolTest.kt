package com.pttlan.shared.watch

import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.feature.channellist.ChannelListState
import com.pttlan.feature.ptt.PttState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WatchProtocolTest {
    private val list = ChannelListState(activeChannels = listOf(ActiveChannelDomain("Geral", 3), ActiveChannelDomain("Obra", 1)))
    private val me = ParticipantDomain(userId = "me", nickname = "Eu", isSpeaking = false)
    private val ana = ParticipantDomain(userId = "ana", nickname = "Ana", isSpeaking = true)
    private val inChannel = PttState(channelId = "Geral", localUserId = "me", participants = listOf(me, ana))

    @Test
    fun withoutTheChannelListOrAConnectionTheWatchShowsDisconnected() {
        assertEquals(WatchState(connected = false), watchState(ConnectionStatus.Connected, list = null, ptt = null))
        assertEquals(WatchState(connected = false), watchState(ConnectionStatus.Disconnected, list, ptt = null))
        assertEquals(WatchState(connected = false), watchState(ConnectionStatus.Connecting, list, ptt = null))
    }

    @Test
    fun reconnectingKeepsTheChannelsLikeTheIphoneScreens() {
        assertEquals(true, watchState(ConnectionStatus.Reconnecting, list, ptt = null).connected)
    }

    @Test
    fun outsideAChannelTheWatchListsTheActiveChannelsWithNoButton() {
        val state = watchState(ConnectionStatus.Connected, list, ptt = null)

        assertEquals(listOf(WatchChannel("Geral", 3), WatchChannel("Obra", 1)), state.channels)
        assertNull(state.channel)
        assertNull(state.button)
    }

    @Test
    fun inAChannelTheButtonFollowsTheIphoneRuleAndNamesWhoElseIsTalking() {
        val receiving = inChannel.copy(currentSpeakerId = "ana", currentSpeakerName = "Ana")

        val state = watchState(ConnectionStatus.Connected, list, receiving)

        assertEquals("Geral", state.channel)
        assertEquals(2, state.people)
        assertEquals(PttButtonState.Receiving, state.button)
        assertEquals("Ana", state.speaker)
    }

    @Test
    fun theUsersOwnTurnNamesNoSpeakerAndABusyChannelIsFlagged() {
        val talking = inChannel.copy(isTransmitting = true, isFloorGranted = true, currentSpeakerId = "me", currentSpeakerName = "Eu")
        val blocked = inChannel.copy(floorBlocked = true)

        val own = watchState(ConnectionStatus.Connected, list, talking)

        assertEquals(PttButtonState.Transmitting, own.button)
        assertNull(own.speaker)
        assertEquals(true, watchState(ConnectionStatus.Connected, list, blocked).busy)
    }

    @Test
    fun theMessageCarriesOnlyPropertyListValuesAndLeavesAbsentOnesOut() {
        val outside = watchState(ConnectionStatus.Connected, list, ptt = null).toMessage()
        val anaTalking = inChannel.copy(currentSpeakerId = "ana", currentSpeakerName = "Ana")
        val receiving = watchState(ConnectionStatus.Connected, list, anaTalking).toMessage()
        val channels = listOf(mapOf(WatchKeys.ID to "Geral", WatchKeys.PEOPLE to 3), mapOf(WatchKeys.ID to "Obra", WatchKeys.PEOPLE to 1))

        assertEquals(true, outside[WatchKeys.CONNECTED])
        assertEquals(channels, outside[WatchKeys.CHANNELS])
        assertFalse(WatchKeys.CHANNEL in outside)
        assertFalse(WatchKeys.BUTTON in outside)
        assertFalse(WatchKeys.SPEAKER in outside)
        assertEquals("Geral", receiving[WatchKeys.CHANNEL])
        assertEquals("Receiving", receiving[WatchKeys.BUTTON])
        assertEquals("Ana", receiving[WatchKeys.SPEAKER])
        assertEquals(2, receiving[WatchKeys.PEOPLE])
    }

    @Test
    fun theWatchsCommandsAreReadAndUnknownOrIncompleteOnesIgnored() {
        assertEquals(WatchCommand.Press, watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.PRESS)))
        assertEquals(WatchCommand.Release, watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.RELEASE)))
        assertEquals(WatchCommand.Leave, watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.LEAVE)))
        assertEquals(WatchCommand.Join("Obra"), watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.JOIN, WatchKeys.CHANNEL to "Obra")))
        assertNull(watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.JOIN)), "joining needs a channel")
        assertNull(watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.JOIN, WatchKeys.CHANNEL to " ")))
        assertNull(watchCommand(mapOf(WatchKeys.COMMAND to "dance")))
        assertNull(watchCommand(mapOf(WatchKeys.COMMAND to WatchKeys.SYNC)), "sync only asks for the state")
        assertNull(watchCommand(emptyMap<String, Any>()))
    }
}
