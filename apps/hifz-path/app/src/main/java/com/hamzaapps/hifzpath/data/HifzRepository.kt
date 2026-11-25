package com.hamzaapps.hifzpath.data

import androidx.room.withTransaction
import com.hamzaapps.hifzpath.data.db.HifzDatabase
import com.hamzaapps.hifzpath.data.db.PageProgressEntity
import com.hamzaapps.hifzpath.data.db.ReviewLogEntity
import com.hamzaapps.hifzpath.data.db.Student
import com.hamzaapps.hifzpath.domain.LogEntry
import com.hamzaapps.hifzpath.domain.LogKind
import com.hamzaapps.hifzpath.domain.PageState
import com.hamzaapps.hifzpath.domain.PageStatus
import com.hamzaapps.hifzpath.domain.Strength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HifzRepository(
    private val db: HifzDatabase,
    private val settings: SettingsRepository,
) {
    private val students = db.studentDao()
    private val progress = db.progressDao()
    private val logs = db.logDao()

    val allStudents: Flow<List<Student>> = students.observeAll()

    fun pageStates(studentId: Long): Flow<Map<Int, PageState>> =
        progress.observe(studentId).map { list -> list.associate { it.page to it.toState() } }

    fun logEntries(studentId: Long): Flow<List<LogEntry>> =
        logs.observe(studentId).map { list -> list.map { it.toEntry() } }

    suspend fun createStudent(student: Student): Long {
        val id = students.insert(student.copy(id = 0, name = student.name.trim()))
        settings.setActiveStudent(id)
        return id
    }

    suspend fun updateStudent(student: Student) = students.update(student.copy(name = student.name.trim()))

    suspend fun deleteStudent(id: Long) = students.delete(id)

    suspend fun studentCount(): Int = students.count()

    /** Marks a page as memorized today and logs it as new work. */
    suspend fun markMemorized(studentId: Long, page: Int, strength: Strength, today: Long) = db.withTransaction {
        val current = progress.get(studentId, page)
        if (current?.status == STATUS_MEMORIZED) {
            progress.upsert(current.copy(strength = strength.value))
            return@withTransaction
        }
        progress.upsert(
            PageProgressEntity(
                studentId = studentId,
                page = page,
                status = STATUS_MEMORIZED,
                strength = strength.value,
                memorizedEpochDay = today,
                lastReviewedEpochDay = today,
                reviewCount = current?.reviewCount ?: 0,
            ),
        )
        logs.insert(ReviewLogEntity(0, studentId, page, today, LogKind.NEW.value, strength.value))
    }

    suspend fun markLearning(studentId: Long, page: Int) {
        val current = progress.get(studentId, page)
        progress.upsert(
            PageProgressEntity(
                studentId = studentId,
                page = page,
                status = STATUS_LEARNING,
                strength = current?.strength ?: Strength.GOOD.value,
                memorizedEpochDay = null,
                lastReviewedEpochDay = current?.lastReviewedEpochDay,
                reviewCount = current?.reviewCount ?: 0,
            ),
        )
    }

    suspend fun setStrength(studentId: Long, page: Int, strength: Strength) {
        val current = progress.get(studentId, page) ?: return
        progress.upsert(current.copy(strength = strength.value))
    }

    /** Records a revision of an already memorized page. */
    suspend fun recordReview(studentId: Long, page: Int, quality: Strength, today: Long) = db.withTransaction {
        val current = progress.get(studentId, page) ?: return@withTransaction
        progress.upsert(
            current.copy(
                strength = quality.value,
                lastReviewedEpochDay = today,
                reviewCount = current.reviewCount + 1,
            ),
        )
        logs.insert(ReviewLogEntity(0, studentId, page, today, LogKind.REVIEW.value, quality.value))
    }

    /** Undoes today's revision of a page. */
    suspend fun undoReview(studentId: Long, page: Int, today: Long) = db.withTransaction {
        logs.deleteFor(studentId, page, today, LogKind.REVIEW.value)
        val current = progress.get(studentId, page) ?: return@withTransaction
        val remaining = logs.reviewsForPage(studentId, page)
        val last = remaining.maxOfOrNull { it.epochDay } ?: current.memorizedEpochDay
        progress.upsert(
            current.copy(
                lastReviewedEpochDay = last,
                reviewCount = remaining.size,
                strength = remaining.maxByOrNull { it.epochDay * 1_000_000 + it.id }?.quality ?: current.strength,
            ),
        )
    }

    /** Undoes today's "memorized" mark; the page goes back to learning. */
    suspend fun undoMemorized(studentId: Long, page: Int, today: Long) = db.withTransaction {
        logs.deleteFor(studentId, page, today, LogKind.NEW.value)
        val current = progress.get(studentId, page) ?: return@withTransaction
        progress.upsert(current.copy(status = STATUS_LEARNING, memorizedEpochDay = null))
    }

    /** Marks pages the student already knew before using the app. They go straight into manzil. */
    suspend fun markKnown(studentId: Long, pages: Iterable<Int>) = db.withTransaction {
        val updates = pages.map { page ->
            val current = progress.get(studentId, page)
            if (current?.status == STATUS_MEMORIZED) {
                current
            } else {
                PageProgressEntity(
                    studentId = studentId,
                    page = page,
                    status = STATUS_MEMORIZED,
                    strength = current?.strength ?: Strength.GOOD.value,
                    memorizedEpochDay = null,
                    lastReviewedEpochDay = current?.lastReviewedEpochDay,
                    reviewCount = current?.reviewCount ?: 0,
                )
            }
        }
        progress.upsertAll(updates)
    }

    suspend fun markLearningAll(studentId: Long, pages: Iterable<Int>) = db.withTransaction {
        pages.forEach { markLearning(studentId, it) }
    }

    suspend fun clearPages(studentId: Long, pages: Iterable<Int>) {
        progress.delete(studentId, pages.toList())
    }

    private fun PageProgressEntity.toState() = PageState(
        page = page,
        status = when (status) {
            STATUS_MEMORIZED -> PageStatus.MEMORIZED
            STATUS_LEARNING -> PageStatus.LEARNING
            else -> PageStatus.NOT_STARTED
        },
        strength = Strength.of(strength),
        memorizedDay = memorizedEpochDay,
        lastReviewedDay = lastReviewedEpochDay,
        reviewCount = reviewCount,
    )

    private fun ReviewLogEntity.toEntry() = LogEntry(
        page = page,
        day = epochDay,
        kind = if (kind == LogKind.NEW.value) LogKind.NEW else LogKind.REVIEW,
        quality = Strength.of(quality),
    )

    companion object {
        const val STATUS_LEARNING = 1
        const val STATUS_MEMORIZED = 2
    }
}
