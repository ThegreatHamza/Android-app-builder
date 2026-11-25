package com.hamzaapps.sitecalc.data

import com.hamzaapps.sitecalc.data.db.Project
import com.hamzaapps.sitecalc.data.db.ProjectSummary
import com.hamzaapps.sitecalc.data.db.SavedCalc
import com.hamzaapps.sitecalc.data.db.SiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ProjectRepository(private val db: SiteDatabase) {
    val summaries: Flow<List<ProjectSummary>> = db.projects().observeSummaries()

    fun project(id: Long): Flow<Project?> = db.projects().observe(id)
    fun items(projectId: Long): Flow<List<SavedCalc>> = db.items().observeForProject(projectId)

    suspend fun projectCount(): Int = db.projects().count()
    suspend fun allProjects(): List<Project> = db.projects().getAll()
    suspend fun itemsOf(projectId: Long): List<SavedCalc> = db.items().getForProject(projectId)
    suspend fun item(id: Long): SavedCalc? = db.items().get(id)

    suspend fun createProject(name: String): Long =
        db.projects().insert(Project(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun renameProject(project: Project, name: String) = db.projects().update(project.copy(name = name.trim()))
    suspend fun deleteProject(id: Long) = db.projects().delete(id)

    suspend fun saveItem(item: SavedCalc): Long =
        if (item.id == 0L) db.items().insert(item) else item.id.also { db.items().update(item) }

    suspend fun deleteItem(id: Long) = db.items().delete(id)

    companion object {
        private val mapStr = MapSerializer(String.serializer(), String.serializer())
        private val mapDouble = MapSerializer(String.serializer(), Double.serializer())

        fun encodeInputs(map: Map<String, String>): String = Json.encodeToString(mapStr, map)
        fun decodeInputs(json: String): Map<String, String> = runCatching { Json.decodeFromString(mapStr, json) }.getOrDefault(emptyMap())
        fun encodeOptions(map: Map<String, Double>): String = Json.encodeToString(mapDouble, map)
        fun decodeOptions(json: String): Map<String, Double> = runCatching { Json.decodeFromString(mapDouble, json) }.getOrDefault(emptyMap())
    }
}
