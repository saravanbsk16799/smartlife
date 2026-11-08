package com.stark.smartlife.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.stark.smartlife.data.AppDatabase
import com.stark.smartlife.data.Reading
import com.stark.smartlife.data.ReadingType
import com.stark.smartlife.data.Source
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

enum class HcAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

/**
 * Read-only Health Connect integration. Data appears here only if some app (for example the
 * official Fastrack app) has written it to Health Connect; this app never writes to it.
 */
class HealthConnectRepo(private val context: Context, private val db: AppDatabase) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    fun availability(): HcAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HcAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcAvailability.UPDATE_REQUIRED
        else -> HcAvailability.UNAVAILABLE
    }

    private fun client() = HealthConnectClient.getOrCreate(context)

    suspend fun hasAllPermissions(): Boolean =
        availability() == HcAvailability.AVAILABLE &&
            client().permissionController.getGrantedPermissions().containsAll(permissions)

    /** Replaces previously imported Health Connect readings in the window with fresh ones. */
    suspend fun importDays(days: Int = 7): Int {
        val end = Instant.now()
        val start = end.minus(Duration.ofDays(days.toLong()))
        val filter = TimeRangeFilter.between(start, end)
        val from = start.toEpochMilli()
        val out = mutableListOf<Reading>()

        fun add(type: String, value: Double, at: Instant) {
            out += Reading(type = type, value = value, time = at.toEpochMilli(), source = Source.HEALTH_CONNECT)
        }

        readAll(StepsRecord::class, filter).forEach { add(ReadingType.STEPS, it.count.toDouble(), it.endTime) }
        readAll(DistanceRecord::class, filter).forEach { add(ReadingType.DISTANCE_M, it.distance.inMeters, it.endTime) }
        readAll(ActiveCaloriesBurnedRecord::class, filter).forEach { add(ReadingType.CALORIES, it.energy.inKilocalories, it.endTime) }
        readAll(OxygenSaturationRecord::class, filter).forEach { add(ReadingType.SPO2, it.percentage.value, it.time) }
        readAll(SleepSessionRecord::class, filter).forEach {
            add(ReadingType.SLEEP_MIN, Duration.between(it.startTime, it.endTime).toMinutes().toDouble(), it.endTime)
        }
        readAll(HeartRateRecord::class, filter).forEach { rec ->
            // One averaged point per record keeps storage small and the chart readable.
            if (rec.samples.isNotEmpty()) {
                add(ReadingType.HEART_RATE, rec.samples.map { it.beatsPerMinute }.average(), rec.endTime)
            }
        }

        listOf(
            ReadingType.STEPS, ReadingType.DISTANCE_M, ReadingType.CALORIES,
            ReadingType.SPO2, ReadingType.SLEEP_MIN, ReadingType.HEART_RATE,
        ).forEach { db.readings().deleteSince(Source.HEALTH_CONNECT, it, from) }
        db.readings().insertAll(out)
        return out.size
    }

    private suspend fun <T : Record> readAll(type: KClass<T>, filter: TimeRangeFilter): List<T> {
        val all = mutableListOf<T>()
        var token: String? = null
        do {
            val resp = client().readRecords(ReadRecordsRequest(type, filter, pageSize = 1000, pageToken = token))
            all += resp.records
            token = resp.pageToken
        } while (token != null)
        return all
    }
}
