package com.stark.smartlife.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.stark.smartlife.data.Alert
import com.stark.smartlife.starkApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Records incoming call and app notifications locally (title and app only, never the body text).
 * Forwarding to the watch is intentionally not implemented because the watch's notification
 * command is not verified; see README.
 */
class StarkNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val isCall = n.category == Notification.CATEGORY_CALL
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        scope.launch {
            starkApp.db.alerts().insert(Alert(app = label, title = title, isCall = isCall, time = sbn.postTime))
        }
    }
}
