package com.hamzaapps.hifzpath

import com.hamzaapps.hifzpath.domain.LogEntry
import com.hamzaapps.hifzpath.domain.LogKind
import com.hamzaapps.hifzpath.domain.PageState
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.domain.StatsCalculator
import com.hamzaapps.hifzpath.domain.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsCalculatorTest {
    private val today = 20_000L

    @Test fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(3, StatsCalculator.streak(setOf(today, today - 1, today - 2, today - 4), today))
        assertEquals(2, StatsCalculator.streak(setOf(today - 1, today - 2), today))
        assertEquals(0, StatsCalculator.streak(setOf(today - 2), today))
    }

    @Test fun juzAmmaComplete() {
        val states = (582..604).associateWith { PageState(it, PageStatus.MEMORIZED, Strength.GOOD) }
        val stats = StatsCalculator.compute(states, emptyList(), today)
        assertEquals(23, stats.memorizedPages)
        assertEquals(1, stats.juzComplete)
        // An-Naba (78) .. An-Nas (114) are all inside juz 30.
        assertEquals(37, stats.surahsComplete)
        assertNull(stats.estimatedDaysLeft)
    }

    @Test fun estimateUsesLast30Days() {
        val states = (1..30).associateWith { PageState(it, PageStatus.MEMORIZED, Strength.GOOD, memorizedDay = today - it + 1) }
        val logs = (1..30).map { LogEntry(it, today - it + 1, LogKind.NEW, Strength.GOOD) }
        val stats = StatsCalculator.compute(states, logs, today)
        assertEquals(30, stats.newPagesLast30Days)
        assertEquals(574, stats.estimatedDaysLeft)
        assertEquals(7, stats.last7Days.size)
        assertEquals(1, stats.last7Days.last())
        assertEquals(84, stats.last84Days.size)
    }
}
