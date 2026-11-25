package com.hamzaapps.hifzpath.domain

import com.hamzaapps.hifzpath.data.quran.Quran
import kotlin.math.ceil

data class JuzProgress(val juz: Int, val memorized: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else memorized.toFloat() / total
}

data class Stats(
    val memorizedPages: Int,
    val learningPages: Int,
    val weakPages: Int,
    val percent: Float,
    val juzComplete: Int,
    val surahsComplete: Int,
    val streakDays: Int,
    /** Activity counts for the last 7 days, oldest first; the last value is today. */
    val last7Days: List<Int>,
    /** Activity counts for the last 84 days (12 weeks), oldest first. */
    val last84Days: List<Int>,
    val newPagesLast30Days: Int,
    /** Estimated days to finish the whole Quran at the last 30 days' pace, or null if unknown. */
    val estimatedDaysLeft: Int?,
    val juz: List<JuzProgress>,
)

object StatsCalculator {

    fun compute(states: Map<Int, PageState>, logs: List<LogEntry>, today: Long): Stats {
        val memorized = states.values.filter { it.status == PageStatus.MEMORIZED }
        val memorizedPages = memorized.map { it.page }.toSet()

        val juz = (1..Quran.JUZ_COUNT).map { j ->
            val pages = Quran.pagesOfJuz(j)
            JuzProgress(j, pages.count { it in memorizedPages }, pages.count())
        }
        val surahsComplete = Quran.surahs.count { s -> s.pages.all { it in memorizedPages } }

        val countsByDay = logs.groupingBy { it.day }.eachCount()
        val last7 = (6 downTo 0).map { countsByDay[today - it] ?: 0 }
        val last84 = (83 downTo 0).map { countsByDay[today - it] ?: 0 }

        val newLast30 = logs.filter { it.kind == LogKind.NEW && today - it.day in 0..29 }
            .map { it.page }.distinct().count { it in memorizedPages }
        val remaining = Quran.PAGE_COUNT - memorizedPages.size
        val estimate = when {
            remaining == 0 -> 0
            newLast30 == 0 -> null
            else -> ceil(remaining / (newLast30 / 30.0)).toInt()
        }

        return Stats(
            memorizedPages = memorizedPages.size,
            learningPages = states.values.count { it.status == PageStatus.LEARNING },
            weakPages = memorized.count { it.strength == Strength.WEAK },
            percent = memorizedPages.size * 100f / Quran.PAGE_COUNT,
            juzComplete = juz.count { it.memorized == it.total },
            surahsComplete = surahsComplete,
            streakDays = streak(countsByDay.keys, today),
            last7Days = last7,
            last84Days = last84,
            newPagesLast30Days = newLast30,
            estimatedDaysLeft = estimate,
            juz = juz,
        )
    }

    /** Consecutive active days ending today (or yesterday, so the streak survives until tonight). */
    internal fun streak(activeDays: Set<Long>, today: Long): Int {
        var day = when {
            today in activeDays -> today
            today - 1 in activeDays -> today - 1
            else -> return 0
        }
        var count = 0
        while (day in activeDays) {
            count++
            day--
        }
        return count
    }
}
