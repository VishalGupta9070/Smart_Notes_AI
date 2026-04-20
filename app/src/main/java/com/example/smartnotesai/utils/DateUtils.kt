package com.example.smartnotesai.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object TaskDateFormatter {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun today(): String = LocalDate.now().format(displayFormatter)

    fun format(date: LocalDate): String = date.format(displayFormatter)

    fun fromDatePickerMillis(utcMillis: Long): String {
        return Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(displayFormatter)
    }

    fun toDatePickerMillis(dateLabel: String): Long? {
        return runCatching {
            LocalDate.parse(dateLabel, displayFormatter)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    fun toSortableEpochDay(dateLabel: String): Long {
        return runCatching {
            LocalDate.parse(dateLabel, displayFormatter).toEpochDay()
        }.getOrDefault(Long.MIN_VALUE)
    }

    fun isToday(dateLabel: String): Boolean = dateLabel == today()
}
