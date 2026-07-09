package com.wzf.accounting.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.wzf.accounting.service.AccountingNotificationListenerService
import com.wzf.accounting.service.NotificationKeepAliveService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting keep-alive service")

            val cn = ComponentName(context, AccountingNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val isListenerEnabled = NotificationKeepAliveService.isNotificationListenerEnabled(cn, flat)

            if (isListenerEnabled) {
                val serviceIntent = Intent(context, NotificationKeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AcctBootReceiver"
    }
}
