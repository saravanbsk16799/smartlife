package com.stark.smartlife.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stark.smartlife.data.Alarm
import com.stark.smartlife.data.AppDatabase
import java.util.Calendar

/**
 * Phone-side alarms and reminders. Uses inexact-while-idle alarms, which need no
 * special permission. These alarms ring on the phone; they are not sent to the watch.
 */
class AlarmScheduler(private val context: Context, private val db: AppDatabase) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    private fun pending(id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun schedule(a: Alarm) {
        cancel(a)
        if (!a.enabled) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, a.hour)
            set(Calendar.MINUTE, a.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(a.id))
    }

    fun cancel(a: Alarm) = manager.cancel(pending(a.id))

    suspend fun rescheduleAll() {
        db.alarms().allNow().forEach { schedule(it) }
    }
}
