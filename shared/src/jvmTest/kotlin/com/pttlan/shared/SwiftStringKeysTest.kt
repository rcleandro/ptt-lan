package com.pttlan.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The SwiftUI screens name their texts by the key in `strings.xml` (32.1). A key that does not exist is a crash on
 * iOS, found only on that screen: this fails the build instead.
 */
class SwiftStringKeysTest {
    private val repo = File("..")
    private val stringsXml = repo.resolve("core/core-designsystem/src/commonMain/composeResources/values/strings.xml")
    private val swiftSources = repo.resolve("iosApp/iosApp")

    @Test
    fun everyStringKeyUsedInSwiftExists() {
        val keys = Regex("""<(?:string|plurals) name="([a-z0-9_]+)"""").findAll(stringsXml.readText()).map { it.groupValues[1] }.toSet()
        // Snake-case literals are string keys: identifiers, images and settings keys in Swift are camelCase or dotted
        val usedInSwift =
            swiftSources
                .walk()
                .filter { it.extension == "swift" }
                .flatMap { file -> Regex(""""([a-z]+(?:_[a-z0-9]+)+)"""").findAll(file.readText()).map { file.name to it.groupValues[1] } }
                .toList()

        assertTrue(usedInSwift.isNotEmpty(), "found no string keys in ${swiftSources.absolutePath}")
        val missing = usedInSwift.filter { (_, key) -> key !in keys }
        assertTrue(missing.isEmpty(), "keys missing from strings.xml: $missing")
    }
}
