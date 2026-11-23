package com.pttlan.core.designsystem

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A user-facing text described from the resources, not built: a pure function decides it, so it is tested on its
 * own, and each UI resolves it (Compose with [asString], SwiftUI through [resolve], 32.4). An argument can be
 * another [UiText], e.g. a plural inside a sentence.
 */
sealed interface UiText {
    data class Res(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Plural(
        val resource: PluralStringResource,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /** Pieces written one after the other, e.g. a number with the decimal separator of the language. */
    data class Concat(
        val parts: List<Any>,
    ) : UiText
}

@Suppress("SpreadOperator") // the resource functions take varargs; the lists hold a couple of values
@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Res -> stringResource(resource, *args.map { it.asArgument() }.toTypedArray())
        is UiText.Plural -> pluralStringResource(resource, quantity, *args.map { it.asArgument() }.toTypedArray())
        is UiText.Concat -> parts.map { it.asArgument() }.joinToString("")
    }

@Composable
private fun Any.asArgument(): Any = if (this is UiText) asString() else this

/** Resolves this outside composition, for the SwiftUI screens and for effects. */
@Suppress("SpreadOperator")
suspend fun UiText.resolve(): String =
    when (this) {
        is UiText.Res -> getString(resource, *args.map { it.resolveArgument() }.toTypedArray())
        is UiText.Plural -> getPluralString(resource, quantity, *args.map { it.resolveArgument() }.toTypedArray())
        is UiText.Concat -> parts.map { it.resolveArgument() }.joinToString("")
    }

private suspend fun Any.resolveArgument(): Any = if (this is UiText) resolve() else this
