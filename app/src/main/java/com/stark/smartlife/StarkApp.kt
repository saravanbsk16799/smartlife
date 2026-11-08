package com.stark.smartlife

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.stark.smartlife.alarms.AlarmScheduler
import com.stark.smartlife.ble.BleRepository
import com.stark.smartlife.data.AppDatabase
import com.stark.smartlife.data.SecureStore
import com.stark.smartlife.data.SettingsStore
import com.stark.smartlife.health.HealthConnectRepo

class StarkApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var secure: SecureStore
        private set
    lateinit var ble: BleRepository
        private set
    lateinit var health: HealthConnectRepo
        private set
    lateinit var alarms: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.create(this)
        settings = SettingsStore(this)
        secure = SecureStore(this)
        ble = BleRepository(this, db, secure)
        health = HealthConnectRepo(this, db)
        alarms = AlarmScheduler(this, db)
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_CONNECTION, getString(R.string.device_title), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARMS, getString(R.string.alarms_title), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val CHANNEL_CONNECTION = "connection"
        const val CHANNEL_ALARMS = "alarms"
    }
}

val android.content.Context.starkApp: StarkApp
    get() = applicationContext as StarkApp
