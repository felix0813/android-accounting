package com.wzf.accounting.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wzf.accounting.data.local.NotificationStore
import com.wzf.accounting.data.model.AutoAccountingNotification

class AccountingNotificationListenerService : NotificationListenerService() {
    private val numberRegex = Regex("\\d")
    private val store by lazy { NotificationStore(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        activeNotifications?.forEach { handleNotification(it, fromActiveSnapshot = true) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotification(sbn, fromActiveSnapshot = false)
    }

    private fun handleNotification(sbn: StatusBarNotification, fromActiveSnapshot: Boolean) {
        runCatching {
            val selectedPackages = store.getSelectedPackages()
            if (selectedPackages.isNotEmpty() && sbn.packageName !in selectedPackages) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString().orEmpty()
            val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
            val content = listOf(text, bigText).filter { it.isNotBlank() }.distinct().joinToString("\n")
            val appName = runCatching {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(sbn.packageName)
            val searchable = "$title $content"
            if (!numberRegex.containsMatchIn(searchable)) {
                Log.d(TAG, "Ignored notification without number: package=${sbn.packageName}")
                return
            }

            store.saveNotification(
                AutoAccountingNotification(
                    id = "${sbn.packageName}:${sbn.id}:${sbn.postTime}",
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    content = content,
                    postedAt = sbn.postTime,
                    capturedAt = System.currentTimeMillis(),
                    fromActiveSnapshot = fromActiveSnapshot
                )
            )
        }.onFailure { Log.e(TAG, "Failed to handle notification from ${sbn.packageName}", it) }
    }

    companion object {
        private const val TAG = "AcctNotificationSvc"
    }
}
