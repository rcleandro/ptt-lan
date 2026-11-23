package com.pttlan.shared.watch

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The watch runs no Kotlin, so its string catalog repeats `strings.xml` under the same keys (33.3). This keeps one
 * source for the texts: a text changed in `strings.xml` fails the build until the watch follows, and a key the
 * watch's Swift asks for that the catalog lacks would show the key itself on the wrist.
 */
class WatchStringsTest {
    private val repo = File("..")
    private val stringsXml = repo.resolve("core/core-designsystem/src/commonMain/composeResources/values/strings.xml").readText()
    private val catalog = Json.parseToJsonElement(repo.resolve("iosApp/watchApp/Localizable.xcstrings").readText()).jsonObject
    private val strings = catalog.getValue("strings").jsonObject

    @Test
    fun everyWatchTextIsTheSameAsInStringsXml() {
        assertTrue(strings.isNotEmpty())
        strings.forEach { (key, entry) ->
            val localization =
                entry.jsonObject
                    .getValue("localizations")
                    .jsonObject
                    .getValue(LANGUAGE)
                    .jsonObject
            val plural = localization["variations"]?.jsonObject?.getValue("plural")?.jsonObject
            if (plural == null) {
                val xml = Regex("""<string name="$key">(.*?)</string>""").find(stringsXml)?.groupValues?.get(1)
                assertEquals(xml?.let(::swiftFormat), localization.value(), "$key differs from strings.xml")
            } else {
                assertEquals(xmlPlural(key), plural.mapValues { (_, unit) -> unit.jsonObject.value() }, "$key differs from strings.xml")
            }
        }
    }

    @Test
    fun everyKeyTheWatchAsksForIsInItsCatalog() {
        val used =
            repo
                .resolve("iosApp/watchApp")
                .walk()
                .filter { it.extension == "swift" }
                .flatMap { file -> Regex(""""([a-z]+(?:_[a-z0-9]+)+)"""").findAll(file.readText()).map { it.groupValues[1] } }
                .toSet()

        assertTrue(used.isNotEmpty())
        assertEquals(emptySet(), used - strings.keys, "keys missing from the watch's Localizable.xcstrings")
    }

    /** The quantities of a `<plurals>` in `strings.xml`, e.g. one → "%1$lld pessoa". */
    private fun xmlPlural(key: String): Map<String, String> {
        val items = Regex("""<plurals name="$key">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).find(stringsXml)?.groupValues?.get(1)
        return Regex("""<item quantity="(\w+)">(.*?)</item>""")
            .findAll(items.orEmpty())
            .associate { it.groupValues[1] to swiftFormat(it.groupValues[2]) }
    }

    private fun JsonObject.value(): String =
        getValue("stringUnit")
            .jsonObject
            .getValue("value")
            .jsonPrimitive.content

    /** `%1$d` and `%1$s` in `strings.xml` are `%1$lld` and `%1$@` for Swift's `String(format:)`. */
    private fun swiftFormat(text: String): String = text.replace("\$d", "\$lld").replace("\$s", "$@")

    private companion object {
        const val LANGUAGE = "pt-BR"
    }
}
