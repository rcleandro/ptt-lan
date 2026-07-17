package com.pttlan.feature.history.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Formata um [Instant] como texto relativo ("há 5 minutos", "há 2 horas e 15 minutos")
 * quando recente, ou como data/hora completa quando ultrapassa [recentThreshold].
 *
 * Pensado para listas como o histórico de mensagens (HistoryScreen), onde itens recentes
 * se beneficiam de um texto relativo e itens antigos precisam de contexto de data completa.
 */
fun Instant.toRelativeDisplay(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    recentThreshold: Duration = 24.hours,
): String {
    val duration = now - this

    if (duration.isNegative()) {
        return formatFullDateTime(timeZone)
    }

    return when {
        duration < 1.minutes -> "agora mesmo"
        duration < 1.hours -> {
            val minutes = duration.inWholeMinutes
            "há ${pluralize(minutes, "minuto", "minutos")}"
        }
        duration < recentThreshold -> {
            val hours = duration.inWholeHours
            val minutesPart = (duration - hours.hours).inWholeMinutes
            val horasTexto = pluralize(hours, "hora", "horas")
            if (minutesPart == 0L) {
                "há $horasTexto"
            } else {
                "há $horasTexto e ${pluralize(minutesPart, "minuto", "minutos")}"
            }
        }
        else -> formatFullDateTime(timeZone)
    }
}

private fun Instant.formatFullDateTime(timeZone: TimeZone): String {
    val dt = toLocalDateTime(timeZone)
    val day = dt.day.toString().padStart(2, '0')
    val month =
        dt.month.number
            .toString()
            .padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "$day/$month/${dt.year} às $hour:$minute"
}

private fun pluralize(
    value: Long,
    singular: String,
    plural: String,
): String = "$value ${if (value == 1L) singular else plural}"
