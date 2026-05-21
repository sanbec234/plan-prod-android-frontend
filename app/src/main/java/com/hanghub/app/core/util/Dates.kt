package com.hanghub.app.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ISO-8601 parsing/formatting helpers. The backend emits timestamps both with
 * and without fractional seconds, mirroring the dual-formatter logic in the
 * iOS APIClient.
 */
object Dates {

    private val parsers: List<SimpleDateFormat> = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    private val iso8601Out =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

    /** Parse an ISO-8601 string to epoch millis, or null if unparseable. */
    fun parseMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        for (p in parsers) {
            try {
                return p.parse(iso)?.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    /** Current time as an ISO-8601 UTC string. */
    fun nowIso(): String = iso8601Out.format(Date())

    /** Format epoch millis as an ISO-8601 UTC string. */
    fun toIso(millis: Long): String = iso8601Out.format(Date(millis))

    /** e.g. "Today · 7:05 PM", "Tomorrow · 9:00 AM", "Sun · 9:00 AM". */
    fun relativeLabel(millis: Long?): String {
        if (millis == null) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val time = SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))
        return when {
            isSameDay(cal, now) -> "Today · $time"
            isSameDay(cal, (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }) ->
                "Tomorrow · $time"
            else -> SimpleDateFormat("EEE · h:mm a", Locale.US).format(Date(millis))
        }
    }

    /** e.g. "now", "2m", "1h", "3d". */
    fun shortLabel(millis: Long?): String {
        if (millis == null) return ""
        val secs = (System.currentTimeMillis() - millis) / 1000
        return when {
            secs < 60 -> "now"
            secs < 3600 -> "${secs / 60}m"
            secs < 86_400 -> "${secs / 3600}h"
            else -> "${secs / 86_400}d"
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
