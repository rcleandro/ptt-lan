package com.pttlan.shared

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.UiText
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.accessibilityLabel
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.allPluralStringResources
import com.pttlan.core.designsystem.generated.resources.allStringResources
import com.pttlan.core.designsystem.resolve
import com.pttlan.core.designsystem.resolveString
import com.pttlan.core.navigation.RootComponent
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.messageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import kotlin.coroutines.CoroutineContext

/**
 * Lets Swift watch [flow] (a component's `state` or `effects`): [onEach] runs on the main thread, in order, until the
 * returned [Observation] is cancelled. A hand-written bridge instead of SKIE, so Kotlin upgrades never wait on it.
 */
fun observe(
    flow: Flow<Any?>,
    onEach: (Any?) -> Unit,
): Observation = observe(flow, Dispatchers.Main, onEach)

internal fun observe(
    flow: Flow<Any?>,
    context: CoroutineContext,
    onEach: (Any?) -> Unit,
): Observation = Observation(CoroutineScope(context).launch { flow.collect { onEach(it) } })

/** Text of the string named [key] in `strings.xml`: SwiftUI screens read the same resources as the Compose ones. */
suspend fun text(
    key: String,
    args: List<Any>,
): String = text(Res.allStringResources.getValue(key), args)

/** Text of the plural named [key] in `strings.xml` for [quantity], e.g. "3 pessoas". */
suspend fun pluralText(
    key: String,
    quantity: Int,
    args: List<Any>,
): String = getPluralString(Res.allPluralStringResources.getValue(key), quantity, *args.toTypedArray())

/** Text of [resource], e.g. the message of a component's error effect. */
suspend fun text(
    resource: StringResource,
    args: List<Any>,
): String = resolveString(resource, args)

/** The app's navigation root, shared by the SwiftUI screens and the Compose ones not migrated yet. */
fun createRootComponent(): RootComponent = RootComponent(componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()))

/** What a screen reader says for the PTT button in [state], the same as on the Compose button. */
fun pttButtonLabel(state: PttButtonState): StringResource = state.accessibilityLabel()

/** Text of a [UiText] a rule decided, e.g. a history message's status with its nested plural. */
suspend fun text(uiText: UiText): String = uiText.resolve()

/** [messageStatus] at the current time and time zone: Swift does not see Kotlin's default arguments. */
fun historyMessageStatus(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
): UiText = messageStatus(message, isPlaying, isPaused)
