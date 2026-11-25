package com.hamzaapps.hifzpath.domain

import com.hamzaapps.hifzpath.data.quran.Quran
import kotlin.math.ceil

object PlanGenerator {

    fun pageOrder(direction: Direction): List<Int> = when (direction) {
        Direction.FROM_START -> Quran.allPages.toList()
        Direction.FROM_END -> Quran.allPages.reversed().toList()
    }

    /**
     * Builds today's plan. Work already done today stays in the plan, marked as done,
     * so ticking items off never makes the list jump around.
     */
    fun generate(
        states: Map<Int, PageState>,
        logsToday: List<LogEntry>,
        settings: PlanSettings,
        today: Long,
    ): DailyPlan {
        val order = pageOrder(settings.direction)
        val orderIndex = order.withIndex().associate { it.value to it.index }
        val byOrder = compareBy<Int> { orderIndex[it] ?: Int.MAX_VALUE }

        val newToday = logsToday.filter { it.kind == LogKind.NEW }.map { it.page }.distinct()
            .filter { states[it]?.status == PageStatus.MEMORIZED }
        val reviewedToday = logsToday.filter { it.kind == LogKind.REVIEW }.map { it.page }.toSet()

        // --- Sabaq: new pages ---
        val pagesTarget = ceil(settings.newQuartersPerDay.coerceAtLeast(1) / 4.0).toInt()
        val remainingNew = (pagesTarget - newToday.size).coerceAtLeast(0)
        val learning = order.filter { states[it]?.status == PageStatus.LEARNING }
        val notStarted = order.filter { states[it] == null || states[it]?.status == PageStatus.NOT_STARTED }
        val nextNew = (learning + notStarted).take(remainingNew)
        val sabaq = newToday.sortedWith(byOrder).map { PlanItem(it, done = true) } +
            nextNew.map { PlanItem(it, done = false) }

        // --- Sabqi: recently memorized pages, revised daily ---
        val memorized = states.values.filter { it.status == PageStatus.MEMORIZED }
        val recent = memorized.filter { s ->
            val day = s.memorizedDay ?: return@filter false
            day != today && today - day <= settings.recentDays
        }
        val recentPages = recent.map { it.page }.toSet()
        val sabqi = recent.map { it.page }.sortedWith(byOrder)
            .map { PlanItem(it, done = it in reviewedToday) }

        // --- Manzil: older pages on a rotating cycle, weakest and most overdue first ---
        val old = memorized.filter { it.page !in recentPages && it.memorizedDay != today }
        val quota = if (old.isEmpty()) 0 else ceil(old.size / settings.cycleDays.coerceAtLeast(1).toDouble())
            .toInt().coerceAtLeast(1)
        val doneOld = old.filter { it.page in reviewedToday }.map { it.page }.sortedWith(byOrder)
        val candidates = old.filter { it.page !in reviewedToday }
            .sortedWith(
                compareByDescending<PageState> { priority(it, today, settings.cycleDays) }
                    .thenBy { orderIndex[it.page] ?: Int.MAX_VALUE },
            )
            .take((quota - doneOld.size).coerceAtLeast(0))
            .map { it.page }
        val manzil = doneOld.map { PlanItem(it, done = true) } + candidates.map { PlanItem(it, done = false) }

        return DailyPlan(
            sabaq = sabaq,
            sabqi = sabqi,
            manzil = manzil,
            newQuartersTarget = settings.newQuartersPerDay,
            quranComplete = learning.isEmpty() && notStarted.isEmpty(),
        )
    }

    /** Higher means the page needs revision sooner. */
    internal fun priority(state: PageState, today: Long, cycleDays: Int): Double {
        val lastSeen = state.lastReviewedDay ?: state.memorizedDay ?: (today - cycleDays)
        val daysSince = (today - lastSeen).coerceAtLeast(0).toDouble()
        val weight = when (state.strength) {
            Strength.WEAK -> 3.0
            Strength.GOOD -> 2.0
            Strength.STRONG -> 1.0
        }
        return (daysSince + 1) * weight
    }
}
