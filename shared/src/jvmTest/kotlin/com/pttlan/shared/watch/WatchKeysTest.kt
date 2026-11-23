package com.pttlan.shared.watch

import com.pttlan.core.designsystem.components.PttButtonState
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The watch runs no Kotlin, so it repeats the message keys and the button states in Swift (33.2). A key that differs
 * on one side is a watch that silently shows nothing: this fails the build instead.
 */
class WatchKeysTest {
    private val watchKeys = File("../iosApp/watchApp/WatchKeys.swift").readText()

    @Test
    fun theWatchUsesTheSameMessageKeysAsKotlin() {
        val kotlin =
            WatchKeys::class.java.declaredFields
                .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
                .map { it.get(null) as String }
                .toSet()
        val swift = Regex("""static let \w+ = "(\w+)"""").findAll(watchKeys).map { it.groupValues[1] }.toSet()

        assertEquals(kotlin, swift)
    }

    @Test
    fun theWatchKnowsEveryButtonStateByItsKotlinName() {
        val swift = Regex("""case \w+ = "(\w+)"""").findAll(watchKeys).map { it.groupValues[1] }.toSet()

        assertEquals(PttButtonState.entries.map { it.name }.toSet(), swift)
    }
}
