package com.hamzaapps.sitecalc.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class SavedCalc(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    /** [com.hamzaapps.sitecalc.domain.CalcType] name. */
    val type: String,
    val title: String,
    /** JSON map of raw text inputs. */
    val inputsJson: String,
    /** JSON map of option values. */
    val optionsJson: String,
    /** Human readable key results at save time. */
    val summary: String,
    val cost: Double,
    val createdAt: Long,
)

data class ProjectSummary(
    @Embedded val project: Project,
    @ColumnInfo(name = "itemCount") val itemCount: Int,
    @ColumnInfo(name = "totalCost") val totalCost: Double,
)

@Dao
interface ProjectDao {
    @Query(
        "SELECT p.*, COUNT(i.id) AS itemCount, COALESCE(SUM(i.cost), 0) AS totalCost " +
            "FROM projects p LEFT JOIN items i ON i.projectId = p.id GROUP BY p.id ORDER BY p.createdAt DESC",
    )
    fun observeSummaries(): Flow<List<ProjectSummary>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects ORDER BY createdAt")
    suspend fun getAll(): List<Project>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Insert
    suspend fun insert(project: Project): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<Project>)

    @Update
    suspend fun update(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE projectId = :projectId ORDER BY createdAt")
    fun observeForProject(projectId: Long): Flow<List<SavedCalc>>

    @Query("SELECT * FROM items WHERE projectId = :projectId ORDER BY createdAt")
    suspend fun getForProject(projectId: Long): List<SavedCalc>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): SavedCalc?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<SavedCalc>

    @Insert
    suspend fun insert(item: SavedCalc): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedCalc>)

    @Update
    suspend fun update(item: SavedCalc)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [Project::class, SavedCalc::class], version = 1, exportSchema = true)
abstract class SiteDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao
    abstract fun items(): ItemDao

    companion object {
        fun build(context: Context): SiteDatabase =
            Room.databaseBuilder(context, SiteDatabase::class.java, "sitecalc.db").build()
    }
}
