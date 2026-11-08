package com.stark.smartlife.alarms

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stark.smartlife.R
import com.stark.smartlife.StarkApp
import com.stark.smartlife.starkApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.starkApp
                val alarm = app.db.alarms().byId(id) ?: return@launch
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
                ) {
                    val n = NotificationCompat.Builder(context, StarkApp.CHANNEL_ALARMS)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(alarm.label.ifBlank { context.getString(R.string.alarms_title) })
                        .setContentText("%02d:%02d".format(alarm.hour, alarm.minute))
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .build()
                    context.getSystemService(NotificationManager::class.java).notify(id.toInt(), n)
                }
                app.alarms.schedule(alarm) // repeat daily
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "alarm_id"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.starkApp.alarms.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
