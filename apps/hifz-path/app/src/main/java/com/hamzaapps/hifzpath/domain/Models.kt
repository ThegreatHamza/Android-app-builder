package com.hamzaapps.hifzpath.domain

enum class PageStatus { NOT_STARTED, LEARNING, MEMORIZED }

/** How firmly a page is memorized. [value] is what is stored in the database. */
enum class Strength(val value: Int) {
    WEAK(1), GOOD(2), STRONG(3);

    companion object {
        fun of(value: Int): Strength = entries.firstOrNull { it.value == value } ?: GOOD
    }
}

enum class Direction { FROM_START, FROM_END }

enum class LogKind(val value: Int) { NEW(0), REVIEW(1) }

data class PageState(
    val page: Int,
    val status: PageStatus,
    val strength: Strength = Strength.GOOD,
    val memorizedDay: Long? = null,
    val lastReviewedDay: Long? = null,
    val reviewCount: Int = 0,
)

data class LogEntry(
    val page: Int,
    val day: Long,
    val kind: LogKind,
    val quality: Strength,
)

data class PlanSettings(
    val direction: Direction,
    /** New memorization per day in quarter pages. */
    val newQuartersPerDay: Int,
    val recentDays: Int,
    val cycleDays: Int,
)

data class PlanItem(val page: Int, val done: Boolean)

/**
 * A day's work, using the three classic parts of hifz:
 * sabaq (new lesson), sabqi (recent pages) and manzil (older pages).
 */
data class DailyPlan(
    val sabaq: List<PlanItem>,
    val sabqi: List<PlanItem>,
    val manzil: List<PlanItem>,
    val newQuartersTarget: Int,
    val quranComplete: Boolean,
) {
    val totalItems: Int get() = sabaq.size + sabqi.size + manzil.size
    val doneItems: Int get() = (sabaq + sabqi + manzil).count { it.done }
}
