package com.hamzaapps.hifzpath.backup

import androidx.room.withTransaction
import com.hamzaapps.hifzpath.data.db.HifzDatabase
import com.hamzaapps.hifzpath.data.db.PageProgressEntity
import com.hamzaapps.hifzpath.data.db.ReviewLogEntity
import com.hamzaapps.hifzpath.data.db.Student
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val version: Int = 1,
    val students: List<StudentDto>,
    val progress: List<ProgressDto>,
    val logs: List<LogDto>,
)

@Serializable
data class StudentDto(
    val id: Long,
    val name: String,
    val direction: Int,
    val newQuartersPerDay: Int,
    val cycleDays: Int,
    val recentDays: Int,
    val createdEpochDay: Long,
)

@Serializable
data class ProgressDto(
    val studentId: Long,
    val page: Int,
    val status: Int,
    val strength: Int,
    val memorizedEpochDay: Long? = null,
    val lastReviewedEpochDay: Long? = null,
    val reviewCount: Int = 0,
)

@Serializable
data class LogDto(
    val studentId: Long,
    val page: Int,
    val epochDay: Long,
    val kind: Int,
    val quality: Int,
)

const val APP_ID = "hifz-path"

class InvalidBackupException(message: String) : Exception(message)

/** Exports and imports all app data as a single JSON file. */
class BackupManager(private val db: HifzDatabase) {
    private val json = BackupFormat.json

    suspend fun exportJson(): String {
        val file = BackupFile(
            students = db.studentDao().getAll().map {
                StudentDto(it.id, it.name, it.direction, it.newQuartersPerDay, it.cycleDays, it.recentDays, it.createdEpochDay)
            },
            progress = db.progressDao().getAll().map {
                ProgressDto(it.studentId, it.page, it.status, it.strength, it.memorizedEpochDay, it.lastReviewedEpochDay, it.reviewCount)
            },
            logs = db.logDao().getAll().map { LogDto(it.studentId, it.page, it.epochDay, it.kind, it.quality) },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Replaces all data with the backup. Returns the number of students restored. */
    suspend fun importJson(text: String): Int {
        val file = parse(text)
        db.withTransaction {
            db.studentDao().deleteAll()
            db.studentDao().insertAll(
                file.students.map {
                    Student(it.id, it.name, it.direction, it.newQuartersPerDay, it.cycleDays, it.recentDays, it.createdEpochDay)
                },
            )
            db.progressDao().upsertAll(
                file.progress.map {
                    PageProgressEntity(it.studentId, it.page, it.status, it.strength, it.memorizedEpochDay, it.lastReviewedEpochDay, it.reviewCount)
                },
            )
            db.logDao().insertAll(file.logs.map { ReviewLogEntity(0, it.studentId, it.page, it.epochDay, it.kind, it.quality) })
        }
        return file.students.size
    }

    fun parse(text: String): BackupFile = BackupFormat.parse(text)
}

/** JSON format and validation, independent of the database. */
object BackupFormat {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun parse(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not a valid backup file")
        }
        if (file.app != APP_ID) throw InvalidBackupException("Backup is from a different app")
        if (file.students.isEmpty()) throw InvalidBackupException("Backup has no students")
        val ids = file.students.map { it.id }.toSet()
        if (file.progress.any { it.studentId !in ids || it.page !in 1..604 } ||
            file.logs.any { it.studentId !in ids || it.page !in 1..604 }
        ) {
            throw InvalidBackupException("Backup contains invalid data")
        }
        return file
    }
}
