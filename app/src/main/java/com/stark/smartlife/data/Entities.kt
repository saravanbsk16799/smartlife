package com.stark.smartlife.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Kinds of readings stored in [Reading]. */
object ReadingType {
    const val STEPS = "steps"          // value = steps in the interval ending at `time`
    const val HEART_RATE = "hr"        // bpm
    const val SPO2 = "spo2"            // percent
    const val SLEEP_MIN = "sleep_min"  // minutes slept, time = end of session
    const val DISTANCE_M = "distance_m"
    const val CALORIES = "kcal"
    const val BATTERY = "battery"      // percent
}

/** Where a reading came from. Never fabricated: only "watch_ble" and "health_connect". */
object Source {
    const val WATCH_BLE = "watch_ble"
    const val HEALTH_CONNECT = "health_connect"
}

@Entity(tableName = "readings", indices = [Index(value = ["type", "time"])])
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val time: Long,
    val source: String,
)

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sport: String,
    val startTime: Long,
    val endTime: Long,
)

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
)

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val app: String,
    val title: String,
    val isCall: Boolean,
    val time: Long,
)
