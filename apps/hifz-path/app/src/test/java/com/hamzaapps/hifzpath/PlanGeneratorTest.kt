package com.hamzaapps.hifzpath

import com.hamzaapps.hifzpath.domain.Direction
import com.hamzaapps.hifzpath.domain.LogEntry
import com.hamzaapps.hifzpath.domain.LogKind
import com.hamzaapps.hifzpath.domain.PageState
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.domain.PlanGenerator
import com.hamzaapps.hifzpath.domain.PlanItem
import com.hamzaapps.hifzpath.domain.PlanSettings
import com.hamzaapps.hifzpath.domain.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGeneratorTest {
    private val today = 20_000L
    private val backward = PlanSettings(Direction.FROM_END, newQuartersPerDay = 4, recentDays = 7, cycleDays = 30)

    private fun memorized(page: Int, day: Long?, strength: Strength = Strength.GOOD, last: Long? = day) =
        page to PageState(page, PageStatus.MEMORIZED, strength, day, last)

    @Test fun emptyStudentStartsAtAnNasWhenBackward() {
        val plan = PlanGenerator.generate(emptyMap(), emptyList(), backward, today)
        assertEquals(listOf(PlanItem(604, false)), plan.sabaq)
        assertTrue(plan.sabqi.isEmpty())
        assertTrue(plan.manzil.isEmpty())
    }

    @Test fun forwardStartsAtPageOneAndTwoPagesTarget() {
        val plan = PlanGenerator.generate(emptyMap(), emptyList(), backward.copy(direction = Direction.FROM_START, newQuartersPerDay = 8), today)
        assertEquals(listOf(1, 2), plan.sabaq.map { it.page })
    }

    @Test fun halfPageTargetStillShowsOnePage() {
        val plan = PlanGenerator.generate(emptyMap(), emptyList(), backward.copy(newQuartersPerDay = 2), today)
        assertEquals(1, plan.sabaq.size)
    }

    @Test fun learningPagesComeFirst() {
        val states = mapOf(300 to PageState(300, PageStatus.LEARNING))
        val plan = PlanGenerator.generate(states, emptyList(), backward, today)
        assertEquals(300, plan.sabaq.single().page)
    }

    @Test fun memorizedTodayIsDoneAndNotDuplicated() {
        val states = mapOf(memorized(604, today))
        val logs = listOf(LogEntry(604, today, LogKind.NEW, Strength.GOOD))
        val plan = PlanGenerator.generate(states, logs, backward, today)
        assertEquals(listOf(PlanItem(604, true)), plan.sabaq)
        assertTrue(plan.sabqi.isEmpty())
        assertTrue(plan.manzil.isEmpty())
        assertEquals(1, plan.doneItems)
    }

    @Test fun recentPagesGoToSabqi() {
        val states = mapOf(memorized(604, today - 1), memorized(603, today - 7), memorized(602, today - 8))
        val plan = PlanGenerator.generate(states, emptyList(), backward, today)
        assertEquals(listOf(604, 603), plan.sabqi.map { it.page })
        assertEquals(listOf(602), plan.manzil.map { it.page })
        assertEquals(601, plan.sabaq.single().page)
    }

    @Test fun manzilQuotaFollowsCycleAndPrefersWeakAndOverdue() {
        // 60 old pages, 30-day cycle -> 2 pages a day.
        val states = (1..60).associate { memorized(it, null, Strength.STRONG, last = today - 5) }.toMutableMap()
        states[40] = PageState(40, PageStatus.MEMORIZED, Strength.WEAK, null, today - 5)
        states[10] = PageState(10, PageStatus.MEMORIZED, Strength.STRONG, null, today - 25)
        val plan = PlanGenerator.generate(states, emptyList(), backward, today)
        assertEquals(2, plan.manzil.size)
        assertEquals(setOf(10, 40), plan.manzil.map { it.page }.toSet())
    }

    @Test fun reviewedManzilStaysDoneAndQuotaIsNotRefilled() {
        val states = (1..60).associate { memorized(it, null, last = today - 10) }.toMutableMap()
        states[5] = PageState(5, PageStatus.MEMORIZED, Strength.GOOD, null, today)
        states[6] = PageState(6, PageStatus.MEMORIZED, Strength.GOOD, null, today)
        val logs = listOf(LogEntry(5, today, LogKind.REVIEW, Strength.GOOD), LogEntry(6, today, LogKind.REVIEW, Strength.GOOD))
        val plan = PlanGenerator.generate(states, logs, backward, today)
        assertEquals(listOf(PlanItem(6, true), PlanItem(5, true)), plan.manzil)
    }

    @Test fun completeQuranHasNoNewLesson() {
        val states = (1..604).associate { memorized(it, null) }
        val plan = PlanGenerator.generate(states, emptyList(), backward, today)
        assertTrue(plan.sabaq.isEmpty())
        assertTrue(plan.quranComplete)
        assertEquals(21, plan.manzil.size) // ceil(604 / 30)
    }
}
