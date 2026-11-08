package com.stark.smartlife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Reading::class, Workout::class, Alarm::class, Alert::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readings(): ReadingDao
    abstract fun workouts(): WorkoutDao
    abstract fun alarms(): AlarmDao
    abstract fun alerts(): AlertDao

    suspend fun wipe() {
        readings().clear()
        workouts().clear()
        alarms().clear()
        alerts().clear()
    }

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "smartlife.db")
                .build()
    }
}
