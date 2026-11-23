package com.pttlan.shared.watch

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.pttlan.core.navigation.RootComponent
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListIntent
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The messages to send the watch as the iPhone's screens change (33.2), one per change. */
@OptIn(ExperimentalCoroutinesApi::class)
fun RootComponent.watchMessages(): Flow<Map<String, Any>> =
    childStack
        .asFlow()
        .flatMapLatest { stack ->
            // The channel stays open under the history the user may have opened on top of it
            val list = stack.listComponent()
            val ptt = stack.pttComponent()
            combine(connectionStatus, list?.state ?: flowOf(null), ptt?.state ?: flowOf(null), ::watchState)
        }.distinctUntilChanged()
        .map { it.toMessage() }

/**
 * The iPhone end of the watch remote (33.2), one for the app: runs what the watch asks through the same intents as
 * the iPhone's own screens, and remembers whether the watch's button is down, so a watch that goes out of reach
 * mid-press (33.4) does not leave the microphone on.
 */
class WatchRemote(
    private val root: RootComponent,
) {
    private var holding = false

    /**
     * Does what [message] asks and answers with the iPhone's state right now. The answer keeps the watch honest about
     * an iPhone app that was closed and launched again (the system wakes it for the message).
     */
    fun onMessage(message: Map<*, *>): Map<String, Any> {
        val command = watchCommand(message)
        holding = holding.after(command)
        val stack = root.childStack.value
        when (command) {
            WatchCommand.Press -> root.handlePttKey(isPressed = true)
            WatchCommand.Release -> root.handlePttKey(isPressed = false)
            WatchCommand.Leave -> stack.pttComponent()?.onIntent(PttIntent.LeaveChannel)
            is WatchCommand.Join -> stack.listComponent()?.onIntent(ChannelListIntent.JoinChannel(command.channelId, command.channelId))
            null -> Unit
        }
        return watchState(root.connectionStatus.value, stack.listComponent()?.state?.value, stack.pttComponent()?.state?.value).toMessage()
    }

    /** The watch went out of reach, or its app closed: with its button down, the release would never arrive. */
    fun onUnreachable() {
        if (holding) {
            holding = false
            root.handlePttKey(isPressed = false)
        }
    }
}

/** Whether the watch's button is down after [command]: leaving the channel lets go of it too. */
internal fun Boolean.after(command: WatchCommand?): Boolean =
    when (command) {
        WatchCommand.Press -> true
        WatchCommand.Release, WatchCommand.Leave -> false
        is WatchCommand.Join, null -> this
    }

private inline fun <reified T : RootComponent.Child> ChildStack<*, RootComponent.Child>.component(): T? =
    items.firstNotNullOfOrNull { it.instance as? T }

private fun ChildStack<*, RootComponent.Child>.listComponent(): ChannelListComponent? =
    component<RootComponent.Child.ChannelListChild>()?.component

private fun ChildStack<*, RootComponent.Child>.pttComponent(): PttComponent? = component<RootComponent.Child.PttChild>()?.component

private fun <T : Any> Value<T>.asFlow(): Flow<T> =
    callbackFlow {
        val cancellation = subscribe { trySend(it) }
        awaitClose { cancellation.cancel() }
    }
