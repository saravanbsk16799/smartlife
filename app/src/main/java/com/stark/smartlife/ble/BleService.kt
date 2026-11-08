package com.stark.smartlife.ble

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stark.smartlife.R
import com.stark.smartlife.StarkApp
import com.stark.smartlife.starkApp

/**
 * Keeps the watch link alive while the user wants live sync. Started explicitly by the
 * "Sync now" button and stopped on disconnect, so it never runs in the background unasked.
 */
class BleService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            starkApp.ble.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        val n: Notification = NotificationCompat.Builder(this, StarkApp.CHANNEL_CONNECTION)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_connected))
            .setOngoing(true)
            .addAction(
                0, getString(R.string.disconnect),
                android.app.PendingIntent.getService(
                    this, 0, Intent(this, BleService::class.java).setAction(ACTION_STOP),
                    android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, 1, n, type)
        if (starkApp.ble.state.value.link == LinkState.DISCONNECTED) starkApp.ble.reconnectLast()
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_STOP = "stop"

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, BleService::class.java))

        fun stop(context: Context) =
            context.startService(Intent(context, BleService::class.java).setAction(ACTION_STOP))
    }
}
