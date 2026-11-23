package com.pttlan.shared.watch

import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.feature.channellist.ChannelListState
import com.pttlan.feature.ptt.PttState
import com.pttlan.feature.ptt.buttonState

/** A channel in the watch's list: its name and how many people are in it. */
data class WatchChannel(
    val id: String,
    val people: Int,
)

/**
 * What the watch shows (33.2), decided on the iPhone from its screens: the watch only draws it, so the rules stay in
 * Kotlin. [button] uses the iPhone's own rule and is null outside a channel; [speaker] is who else is talking, when
 * the server named them.
 */
data class WatchState(
    val connected: Boolean,
    val channels: List<WatchChannel> = emptyList(),
    val channel: String? = null,
    val people: Int = 0,
    val button: PttButtonState? = null,
    val speaker: String? = null,
    val busy: Boolean = false,
)

/** What the watch asks the iPhone to do. */
sealed interface WatchCommand {
    data object Press : WatchCommand

    data object Release : WatchCommand

    data object Leave : WatchCommand

    data class Join(
        val channelId: String,
    ) : WatchCommand
}

/** Keys and values of the WatchConnectivity messages; the watch's `WatchKeys` must use the same (`WatchKeysTest`). */
object WatchKeys {
    const val CONNECTED = "connected"
    const val CHANNELS = "channels"
    const val ID = "id"
    const val PEOPLE = "people"
    const val CHANNEL = "channel"
    const val BUTTON = "button"
    const val SPEAKER = "speaker"
    const val BUSY = "busy"
    const val COMMAND = "command"
    const val PRESS = "press"
    const val RELEASE = "release"
    const val JOIN = "join"
    const val LEAVE = "leave"
}

/** The watch's view of the iPhone: connected while the client is (or is reconnecting), with the open channel. */
fun watchState(
    status: ConnectionStatus,
    list: ChannelListState?,
    ptt: PttState?,
): WatchState {
    val connected = list != null && (status == ConnectionStatus.Connected || status == ConnectionStatus.Reconnecting)
    if (!connected) return WatchState(connected = false)
    val button = ptt?.buttonState()
    return WatchState(
        connected = true,
        channels = list.activeChannels.map { WatchChannel(it.id, it.participantCount) },
        channel = ptt?.channelId,
        people = ptt?.participants?.size ?: 0,
        button = button,
        speaker = ptt?.currentSpeakerName?.takeIf { button == PttButtonState.Receiving },
        busy = ptt?.floorBlocked ?: false,
    )
}

/** As a WatchConnectivity message: property-list values only, so absent values are left out instead of null. */
fun WatchState.toMessage(): Map<String, Any> =
    buildMap {
        put(WatchKeys.CONNECTED, connected)
        put(WatchKeys.CHANNELS, channels.map { mapOf(WatchKeys.ID to it.id, WatchKeys.PEOPLE to it.people) })
        channel?.let { put(WatchKeys.CHANNEL, it) }
        put(WatchKeys.PEOPLE, people)
        button?.let { put(WatchKeys.BUTTON, it.name) }
        speaker?.let { put(WatchKeys.SPEAKER, it) }
        put(WatchKeys.BUSY, busy)
    }

/** The command in a message from the watch, or null when it is not one this version knows. */
fun watchCommand(message: Map<*, *>): WatchCommand? =
    when (message[WatchKeys.COMMAND]) {
        WatchKeys.PRESS -> WatchCommand.Press
        WatchKeys.RELEASE -> WatchCommand.Release
        WatchKeys.LEAVE -> WatchCommand.Leave
        WatchKeys.JOIN -> (message[WatchKeys.CHANNEL] as? String)?.takeIf { it.isNotBlank() }?.let(WatchCommand::Join)
        else -> null
    }
