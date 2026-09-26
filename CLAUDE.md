# CLAUDE.md

Guidance for Claude Code in the PTT-LAN repository. Project context lives in [docs/CONTEXTO.md](docs/CONTEXTO.md) and
the work plan in [docs/ROADMAP_MELHORIAS.md](docs/ROADMAP_MELHORIAS.md).

## No hardcoded values

Keep the code free of values written straight into it, so it stays easy to maintain, to test and, later, to translate.
The app is Compose Multiplatform, so the Android idioms (`strings.xml`, `dimens.xml`) map as below.

### User-facing strings go to resources

No text the user sees (titles, messages, errors, button labels, content descriptions, notifications) lives in Kotlin.

- **Screens (all platforms):** `core/core-designsystem/src/commonMain/composeResources/values/strings.xml`, grouped by
  screen, with `common_*` for texts shared across screens. Read them with `stringResource(Res.string.x)` /
  `pluralStringResource(Res.plurals.x, count, count)` in composables. `Res` is
  `com.pttlan.core.designsystem.generated.resources.Res`, and each key is imported from that package.
- **Components and view models** never build a text: effects carry the `StringResource` and its arguments
  (`ShowError(message = Res.string.x, args = listOf(...))`), and the screen resolves them with
  `resolveString(resource, args)` from `core-designsystem`. Tests compare resources, not texts.
- **Android code outside Compose** (foreground services, notifications, the launcher label): the app module's
  `res/values/strings.xml` and `getString(R.string.x)`.
- Placeholders are positional (`%1$s`, `%1$d`); line breaks inside a string are `\n` in the XML.
- A pure decision that ends up as text (e.g. how long ago something happened) returns a value (a sealed type or an
  enum), tested on its own, and a composable turns it into text from the resources.

**Not user-facing, so they stay in code:** ids and keys saved in settings or sent over the network (channel ids,
`STORAGE_INTERNAL`), log messages (in English), exception messages meant for logs, MIME types, and the sample data of
`@Preview`s and tests. **Known exception:** the texts the server sends (close reasons such as "Nome já em uso",
`FloorDenied` reasons) are protocol; the app shows them as they come until the protocol carries reason codes.

### Magic numbers become named constants

No loose number in logic, validation, delays, calculations or layout: name it in `SNAKE_CASE`
(`private const val MIN_PASSWORD_LENGTH = 8`, `private val FLOOR_TIMEOUT = 30.seconds`). A value shared by several
classes or modules goes to a common place, e.g. `core-common`'s `ChannelLimits.kt` (`DEFAULT_SERVER_PORT`,
`MIN_ROOM_PIN_LENGTH`, `MAX_CHANNELS`), which the server uses too.

### Dimensions come from tokens

- **Spacing** (margins, paddings, gaps, borders): `Dimens` in `core-designsystem` (`Dimens.SpaceXl`,
  `Dimens.Hairline`); **shared control sizes** too (`Dimens.TouchTarget`, `Dimens.GlassControl`).
- **A size that belongs to one component** (an icon, an avatar, a glow): a named `private val` at the top of that file
  (`private val AvatarSize = 48.dp`), not an inline `48.dp`.

### Enforcement

detekt's `MagicNumber` runs with `ignoreNamedArgument: false` and `ignoreExtensionFunctions: false`, so loose numbers,
named arguments like `alpha = 0.3f` and `16.dp` fail the build; `@Preview`s and `Previews.kt` are exempt. There is no
rule for strings: grep for quoted text in `commonMain` before committing a screen.

## Checks before a commit

The pre-commit hook runs `./gradlew jvmTest :server-core:test detekt ktlintCheck`. It does **not** compile the apps nor
the iOS targets: after touching a shared module, also run `./gradlew :androidApp:assembleDebug :wearApp:assembleDebug
:desktopApp:compileKotlinJvm iosSimulatorArm64Test`, and `xcodebuild` for the iOS app when iOS code changed.

Design system snapshots (`:core:core-designsystem:verifyRoborazziAndroidHostTest`) are recorded on Linux by the
"Record snapshots" workflow; images recorded on a Mac never match the CI runner.
