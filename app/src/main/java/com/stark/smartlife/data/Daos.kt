package com.stark.smartlife.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DayTotal(val day: String, val total: Double)
data class DayAvg(val day: String, val avg: Double)

@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(r: Reading)

    @Insert
    suspend fun insertAll(r: List<Reading>)

    @Query("DELETE FROM readings WHERE source = :source AND type = :type AND time >= :from")
    suspend fun deleteSince(source: String, type: String, from: Long)

    @Query("SELECT * FROM readings WHERE type = :type ORDER BY time DESC LIMIT 1")
    fun latest(type: String): Flow<Reading?>

    @Query("SELECT * FROM readings WHERE type = :type AND time >= :from ORDER BY time ASC")
    fun since(type: String, from: Long): Flow<List<Reading>>

    @Query("SELECT COALESCE(SUM(value), 0) FROM readings WHERE type = :type AND time >= :from")
    fun sumSince(type: String, from: Long): Flow<Double>

    @Query(
        "SELECT strftime('%Y-%m-%d', time/1000, 'unixepoch', 'localtime') AS day, SUM(value) AS total " +
            "FROM readings WHERE type = :type AND time >= :from GROUP BY day ORDER BY day ASC"
    )
    fun dailyTotals(type: String, from: Long): Flow<List<DayTotal>>

    @Query(
        "SELECT strftime('%Y-%m-%d', time/1000, 'unixepoch', 'localtime') AS day, AVG(value) AS avg " +
            "FROM readings WHERE type = :type AND time >= :from GROUP BY day ORDER BY day ASC"
    )
    fun dailyAverages(type: String, from: Long): Flow<List<DayAvg>>

    @Query("DELETE FROM readings")
    suspend fun clear()
}

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(w: Workout): Long

    @Query("SELECT * FROM workouts ORDER BY startTime DESC LIMIT 50")
    fun recent(): Flow<List<Workout>>

    @Query("DELETE FROM workouts")
    suspend fun clear()
}

@Dao
interface AlarmDao {
    @Insert
    suspend fun insert(a: Alarm): Long

    @Update
    suspend fun update(a: Alarm)

    @Delete
    suspend fun delete(a: Alarm)

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun all(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    suspend fun allNow(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun byId(id: Long): Alarm?

    @Query("DELETE FROM alarms")
    suspend fun clear()
}

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(a: Alert)

    @Query("SELECT * FROM alerts ORDER BY time DESC LIMIT 100")
    fun recent(): Flow<List<Alert>>

    @Query("DELETE FROM alerts")
    suspend fun clear()
}
