package cn.mine.minestars.core.tavern.macro

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TemplateMacroTimeSupport {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun resolve(name: String, args: List<String>): String? {
        val now = LocalDateTime.now(zoneId)
        return when (name.lowercase()) {
            "time" -> formatTime(now.toLocalTime(), args.firstOrNull())
            "date" -> formatDate(now.toLocalDate(), args.firstOrNull())
            "datetime" -> formatDateTime(now, args.firstOrNull())
            "weekday" -> now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
            "timestamp" -> Instant.now().epochSecond.toString()
            "isotime" -> formatTime(now.toLocalTime(), "HH:mm")
            "isodate" -> formatDate(now.toLocalDate(), "yyyy-MM-dd")
            "datetimeformat" -> formatDateTime(now, args.firstOrNull())
            "timediff" -> resolveTimeDiff(args)
            else -> null
        }
    }

    private fun resolveTimeDiff(args: List<String>): String {
        val t1 = args.getOrNull(0).orEmpty()
        val t2 = args.getOrNull(1).orEmpty()
        if (t1.isBlank() || t2.isBlank()) return ""
        return try {
            val time1 = java.time.Instant.parse(t1).atZone(zoneId).toLocalDateTime()
            val time2 = java.time.Instant.parse(t2).atZone(zoneId).toLocalDateTime()
            val duration = java.time.Duration.between(time2, time1)
            val seconds = duration.seconds
            when {
                seconds < 60 -> "a few seconds"
                seconds < 120 -> "a minute"
                seconds < 3600 -> "${seconds / 60} minutes"
                seconds < 7200 -> "an hour"
                seconds < 86400 -> "${seconds / 3600} hours"
                seconds < 172800 -> "a day"
                else -> "${seconds / 86400} days"
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun resolveTimeUTC(offset: String): String {
        val offsetMinutes = try {
            val sign = if (offset.startsWith('-')) -1 else 1
            val num = offset.removePrefix("+").removePrefix("-").toIntOrNull() ?: 0
            sign * num * 60
        } catch (_: Exception) { 0 }
        val utc = LocalDateTime.now(java.time.ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val adjusted = utc.plusMinutes(offsetMinutes.toLong())
        return adjusted.format(formatter)
    }

    fun resolveIdleDuration(lastTimestampEpochMillis: Long): String {
        if (lastTimestampEpochMillis <= 0L) return "just now"
        val now = System.currentTimeMillis()
        val diffMs = now - lastTimestampEpochMillis
        val seconds = diffMs / 1000
        return when {
            seconds < 30 -> "just now"
            seconds < 60 -> "a few seconds"
            seconds < 120 -> "a minute"
            seconds < 3600 -> "${seconds / 60} minutes"
            seconds < 7200 -> "an hour"
            seconds < 86400 -> "${seconds / 3600} hours"
            seconds < 172800 -> "a day"
            else -> "${seconds / 86400} days"
        }
    }

    private fun formatTime(time: LocalTime, pattern: String?): String {
        val formatter = formatterOrNull(pattern) ?: DateTimeFormatter.ofPattern("HH:mm:ss")
        return time.format(formatter)
    }

    private fun formatDate(date: LocalDate, pattern: String?): String {
        val formatter = formatterOrNull(pattern) ?: DateTimeFormatter.ISO_LOCAL_DATE
        return date.format(formatter)
    }

    private fun formatDateTime(dateTime: LocalDateTime, pattern: String?): String {
        val formatter = formatterOrNull(pattern) ?: DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return dateTime.format(formatter)
    }

    private fun formatterOrNull(pattern: String?): DateTimeFormatter? {
        val normalized = pattern?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return runCatching { DateTimeFormatter.ofPattern(normalized) }.getOrNull()
    }
}
