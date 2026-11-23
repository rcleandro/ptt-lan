package com.pttlan.feature.settings

import com.pttlan.core.common.storage.STORAGE_EXTERNAL
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.settings_storage_external
import com.pttlan.core.designsystem.generated.resources.settings_storage_internal
import com.pttlan.core.designsystem.generated.resources.size_bytes
import com.pttlan.core.designsystem.generated.resources.size_gb
import com.pttlan.core.designsystem.generated.resources.size_kb
import com.pttlan.core.designsystem.generated.resources.size_mb
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToLong

// Rules of the settings screen, shared by the Compose and SwiftUI screens (32.4): the screens only draw them

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB
private const val BYTES_PER_GB = BYTES_PER_MB * BYTES_PER_KB

/** Smallest and largest history limit the slider offers, and its step, in MB. */
const val CACHE_SIZE_MIN_MB = 100
const val CACHE_SIZE_MAX_MB = 2000
const val CACHE_SIZE_STEP_MB = 100

/** A size in the largest unit that keeps it at least 1: [resource] takes [value] as its only argument. */
data class SizeText(
    val resource: StringResource,
    val value: Long,
)

fun sizeText(bytes: Long): SizeText =
    when {
        bytes >= BYTES_PER_GB -> SizeText(Res.string.size_gb, (bytes.toDouble() / BYTES_PER_GB).roundToLong())
        bytes >= BYTES_PER_MB -> SizeText(Res.string.size_mb, (bytes.toDouble() / BYTES_PER_MB).roundToLong())
        bytes >= BYTES_PER_KB -> SizeText(Res.string.size_kb, (bytes.toDouble() / BYTES_PER_KB).roundToLong())
        else -> SizeText(Res.string.size_bytes, bytes)
    }

/** The name of a storage place, from the id saved in settings. */
fun storageName(id: String): StringResource =
    if (id == STORAGE_EXTERNAL) Res.string.settings_storage_external else Res.string.settings_storage_internal

/** How full the history is, from 0 to 1, for the usage bar. */
fun SettingsState.cacheUsage(): Float = if (maxCacheSizeMb == 0) 0f else (currentCacheUsageMb.toFloat() / maxCacheSizeMb).coerceIn(0f, 1f)

/** The device takes an SD card, but none is in: the location picker says so instead of just missing the option. */
val SettingsState.isSdCardMissing: Boolean
    get() = isExternalStorageSupported && storageOptions.size == 1
