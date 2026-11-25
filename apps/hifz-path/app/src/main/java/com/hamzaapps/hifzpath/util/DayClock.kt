package com.hamzaapps.hifzpath.util

import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/** Calendar-day helpers that work on API 24 without java.time desugaring. */
object DayClock {
    private const val MILLIS_PER_DAY = 86_400_000L

    /** Today's local date as days since 1970-01-01. */
    fun today(nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(nowMillis + zone.getOffset(nowMillis), MILLIS_PER_DAY)

    /** Formats an epoch day as a medium-length local date, e.g. "Mar 4, 2027". */
    fun format(epochDay: Long): String {
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(epochDay * MILLIS_PER_DAY))
    }
}
