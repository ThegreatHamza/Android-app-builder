package com.hamzaapps.hifzpath.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY id")
    fun observeAll(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY id")
    suspend fun getAll(): List<Student>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun count(): Int

    @Insert
    suspend fun insert(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>)

    @Update
    suspend fun update(student: Student)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM page_progress WHERE studentId = :studentId")
    fun observe(studentId: Long): Flow<List<PageProgressEntity>>

    @Query("SELECT * FROM page_progress")
    suspend fun getAll(): List<PageProgressEntity>

    @Query("SELECT * FROM page_progress WHERE studentId = :studentId AND page = :page")
    suspend fun get(studentId: Long, page: Int): PageProgressEntity?

    @Upsert
    suspend fun upsert(entity: PageProgressEntity)

    @Upsert
    suspend fun upsertAll(entities: List<PageProgressEntity>)

    @Query("DELETE FROM page_progress WHERE studentId = :studentId AND page IN (:pages)")
    suspend fun delete(studentId: Long, pages: List<Int>)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM review_log WHERE studentId = :studentId ORDER BY epochDay DESC, id DESC")
    fun observe(studentId: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_log")
    suspend fun getAll(): List<ReviewLogEntity>

    @Query("SELECT * FROM review_log WHERE studentId = :studentId AND page = :page AND kind = 1")
    suspend fun reviewsForPage(studentId: Long, page: Int): List<ReviewLogEntity>

    @Insert
    suspend fun insert(entity: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReviewLogEntity>)

    @Query(
        "DELETE FROM review_log WHERE studentId = :studentId AND page = :page " +
            "AND epochDay = :epochDay AND kind = :kind",
    )
    suspend fun deleteFor(studentId: Long, page: Int, epochDay: Long, kind: Int)
}
