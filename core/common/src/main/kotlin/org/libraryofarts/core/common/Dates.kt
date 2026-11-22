package org.libraryofarts.core.common

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

interface TimeProvider {
    fun nowMillis(): Long = java.lang.System.currentTimeMillis()
    fun todayEpochDay(): Long = nowMillis() / 86_400_000L
}

object SystemTimeProvider : TimeProvider

object DateFormats {
    fun yearLabel(year: String): String = year.ifBlank { "Unknown date" }

    fun epochDayLabel(epochDay: Long): String {
        val date = LocalDate.fromEpochDays(epochDay.toInt())
        return "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}, ${date.year}"
    }

    fun formatDate(epochMillis: Long, locale: Locale = Locale.getDefault()): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
            "${local.dayOfMonth}, ${local.year}"
    }

    fun relative(epochMillis: Long, now: Long = java.lang.System.currentTimeMillis()): String {
        val delta = (now - epochMillis).coerceAtLeast(0)
        val minutes = delta / 60_000
        val hours = delta / 3_600_000
        val days = delta / 86_400_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days < 7 -> "$days d ago"
            days < 30 -> "${days / 7} wk ago"
            else -> formatDate(epochMillis)
        }
    }

    fun monthKey(epochMillis: Long): String {
        val local = Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}"
    }

    fun startOfDay(epochMillis: Long): Long {
        val local = Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return local.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}
