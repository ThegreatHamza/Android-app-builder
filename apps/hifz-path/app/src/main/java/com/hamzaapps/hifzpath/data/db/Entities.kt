package com.hamzaapps.hifzpath.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 0 = start from Al-Fatiha (page 1), 1 = start from An-Nas (page 604). */
    val direction: Int,
    /** New memorization target per day, in quarter pages (1 = 1/4 page, 4 = 1 page). */
    val newQuartersPerDay: Int,
    /** Number of days in which every older memorized page should be revised once. */
    val cycleDays: Int,
    /** Pages memorized within this many days are revised every day (sabqi). */
    val recentDays: Int = 7,
    val createdEpochDay: Long,
)

@Entity(
    tableName = "page_progress",
    primaryKeys = ["studentId", "page"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId")],
)
data class PageProgressEntity(
    val studentId: Long,
    val page: Int,
    /** 1 = learning, 2 = memorized. Pages that are not started have no row. */
    val status: Int,
    /** 1 = weak, 2 = good, 3 = strong. */
    val strength: Int,
    /** Day the page was memorized, or null if it was already known before using the app. */
    val memorizedEpochDay: Long?,
    val lastReviewedEpochDay: Long?,
    val reviewCount: Int,
)

@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId"), Index(value = ["studentId", "epochDay"])],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val page: Int,
    val epochDay: Long,
    /** 0 = new page memorized, 1 = revision. */
    val kind: Int,
    /** 1 = weak, 2 = good, 3 = strong. */
    val quality: Int,
)
