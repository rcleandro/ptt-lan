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

/** Does what [message] from the watch asks, through the same intents as the iPhone's own screens. */
fun RootComponent.onWatchMessage(message: Map<*, *>) {
    val stack = childStack.value
    when (val command = watchCommand(message)) {
        WatchCommand.Press -> handlePttKey(isPressed = true)
        WatchCommand.Release -> handlePttKey(isPressed = false)
        WatchCommand.Leave -> stack.pttComponent()?.onIntent(PttIntent.LeaveChannel)
        is WatchCommand.Join -> stack.listComponent()?.onIntent(ChannelListIntent.JoinChannel(command.channelId, command.channelId))
        null -> Unit
    }
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
