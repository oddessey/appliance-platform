package com.smartmove.mobile.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimes {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDateTime(value: String?): String {
        val parsed = parse(value) ?: return value.orEmpty()
        return parsed.atZoneSameInstant(ZoneId.systemDefault()).format(dateTimeFormatter)
    }

    fun formatDate(value: String?): String {
        val parsed = parse(value) ?: return value.orEmpty()
        return parsed.atZoneSameInstant(ZoneId.systemDefault()).format(dateFormatter)
    }

    fun formatTime(value: String?): String {
        val parsed = parse(value) ?: return value.orEmpty()
        return parsed.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatter)
    }

    fun formatSchedule(startAt: String?, endAt: String?): String =
        "${formatDateTime(startAt)} ~ ${formatTime(endAt)}"

    fun formatSchedule(startDate: LocalDate, startTime: LocalTime, endDate: LocalDate, endTime: LocalTime): String =
        "${startDate.format(dateFormatter)} ${startTime.format(timeFormatter)} ~ " +
            "${endDate.format(dateFormatter)} ${endTime.format(timeFormatter)}"

    fun formatCurrency(amount: Double?): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA).apply {
            maximumFractionDigits = 0
        }
        return formatter.format(amount ?: 0.0)
    }

    fun toIso(date: LocalDate, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()): String =
        ZonedDateTime.of(date, time, zoneId).toOffsetDateTime().toString()

    fun defaultStart(): Pair<LocalDate, LocalTime> =
        LocalDate.now().plusDays(1) to LocalTime.of(9, 0)

    fun defaultEnd(): Pair<LocalDate, LocalTime> =
        LocalDate.now().plusDays(1) to LocalTime.of(13, 0)

    fun parseDate(value: String?): LocalDate? = parse(value)
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.toLocalDate()

    fun parseTime(value: String?): LocalTime? = parse(value)
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.toLocalTime()

    fun isFuture(value: String?): Boolean {
        val parsed = parse(value) ?: return false
        return parsed.isAfter(OffsetDateTime.now())
    }

    fun parseLocalTime(text: String): LocalTime? {
        val normalized = text.trim()
        return runCatching {
            when {
                normalized.count { it == ':' } == 1 -> LocalTime.parse(normalized)
                else -> LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HH:mm:ss"))
            }
        }.getOrNull()
    }

    private fun parse(value: String?): OffsetDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value) }.getOrNull()
    }
}
