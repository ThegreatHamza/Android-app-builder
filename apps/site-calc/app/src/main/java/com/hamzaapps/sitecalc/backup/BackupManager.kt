package com.hamzaapps.sitecalc.backup

import androidx.room.withTransaction
import com.hamzaapps.sitecalc.data.db.Project
import com.hamzaapps.sitecalc.data.db.SavedCalc
import com.hamzaapps.sitecalc.data.db.SiteDatabase
import com.hamzaapps.sitecalc.domain.CalcType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = BackupFormat.APP_ID,
    val version: Int = 1,
    val projects: List<ProjectDto>,
    val items: List<ItemDto>,
)

@Serializable
data class ProjectDto(val id: Long, val name: String, val createdAt: Long)

@Serializable
data class ItemDto(
    val projectId: Long,
    val type: String,
    val title: String,
    val inputsJson: String,
    val optionsJson: String,
    val summary: String,
    val cost: Double,
    val createdAt: Long,
)

class InvalidBackupException(message: String) : Exception(message)

object BackupFormat {
    const val APP_ID = "site-calc"
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
        val ids = file.projects.map { it.id }.toSet()
        val types = CalcType.entries.map { it.name }.toSet()
        if (file.items.any { it.projectId !in ids || it.type !in types }) {
            throw InvalidBackupException("Backup contains invalid data")
        }
        return file
    }
}

class BackupManager(private val db: SiteDatabase) {
    suspend fun exportJson(): String {
        val file = BackupFile(
            projects = db.projects().getAll().map { ProjectDto(it.id, it.name, it.createdAt) },
            items = db.items().getAll().map {
                ItemDto(it.projectId, it.type, it.title, it.inputsJson, it.optionsJson, it.summary, it.cost, it.createdAt)
            },
        )
        return BackupFormat.json.encodeToString(BackupFile.serializer(), file)
    }

    /** Replaces all projects with the backup. Returns the number of projects restored. */
    suspend fun importJson(text: String): Int {
        val file = BackupFormat.parse(text)
        db.withTransaction {
            db.projects().deleteAll()
            db.projects().insertAll(file.projects.map { Project(it.id, it.name, it.createdAt) })
            db.items().insertAll(
                file.items.map {
                    SavedCalc(0, it.projectId, it.type, it.title, it.inputsJson, it.optionsJson, it.summary, it.cost, it.createdAt)
                },
            )
        }
        return file.projects.size
    }
}
