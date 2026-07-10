package com.plantia.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

data class MoonInfo(
    val dateIso: String,
    val phaseName: String,
    val emoji: String,
    val illuminationPct: Int,
)

object MoonCalculator {
    private const val SYNODIC_MONTH = 29.530588853
    private const val REF_NEW_MOON_EPOCH_MS = 947182440_000L // 2000-01-06 18:14 UTC

    fun moonInfo(today: LocalDate = LocalDate.now()): MoonInfo {
        val noon = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        val age = moonAgeDays(noon)
        val (phaseName, emoji) = phaseNameAndEmoji(age)
        val illum = illuminationFromAge(age)
        return MoonInfo(
            dateIso = today.toString(),
            phaseName = phaseName,
            emoji = emoji,
            illuminationPct = illum,
        )
    }

    private fun moonAgeDays(instant: Instant): Double {
        val deltaDays = (instant.toEpochMilli() - REF_NEW_MOON_EPOCH_MS) / 86_400_000.0
        return deltaDays % SYNODIC_MONTH
    }

    private fun illuminationFromAge(age: Double): Int {
        val x = abs(age - (SYNODIC_MONTH / 2)) / (SYNODIC_MONTH / 2)
        val illum = ((1 - x) * 100).roundToInt()
        return illum.coerceIn(0, 100)
    }

    private fun phaseNameAndEmoji(age: Double): Pair<String, String> {
        val idx = (floor((age / SYNODIC_MONTH) * 8).toInt()) % 8
        val phases = listOf(
            "Luna nueva" to "🌑",
            "Luna creciente" to "🌒",
            "Cuarto creciente" to "🌓",
            "Gibosa creciente" to "🌔",
            "Luna llena" to "🌕",
            "Gibosa menguante" to "🌖",
            "Cuarto menguante" to "🌗",
            "Luna menguante" to "🌘",
        )
        return phases[idx]
    }
}

fun formatJournalDate(date: LocalDate = LocalDate.now()): String {
    val month = date.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
    return "${date.dayOfMonth} de $month"
}

fun parseJournalLines(notas: String): List<String> =
    notas.trim().lines().map { it.trim() }.filter { it.isNotBlank() }
