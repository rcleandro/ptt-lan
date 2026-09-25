package com.pttlan.core.designsystem

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Resolves [resource] with [args] outside composition: the effects the components send (snackbars) carry the
 * resource and its arguments, never a built text.
 */
@Suppress("SpreadOperator") // getString takes varargs; the list holds a couple of values
suspend fun resolveString(
    resource: StringResource,
    args: List<Any> = emptyList(),
): String = getString(resource, *args.toTypedArray())
