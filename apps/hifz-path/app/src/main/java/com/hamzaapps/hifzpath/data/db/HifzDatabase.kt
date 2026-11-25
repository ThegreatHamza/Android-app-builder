package com.hamzaapps.hifzpath.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Student::class, PageProgressEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HifzDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun progressDao(): ProgressDao
    abstract fun logDao(): LogDao

    companion object {
        fun build(context: Context): HifzDatabase =
            Room.databaseBuilder(context, HifzDatabase::class.java, "hifz.db").build()
    }
}
