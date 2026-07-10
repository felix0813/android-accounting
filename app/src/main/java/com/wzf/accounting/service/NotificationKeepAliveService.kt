package com.wzf.accounting.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wzf.accounting.MainActivity
import com.wzf.accounting.R

class NotificationKeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val healthCheckRunnable = object : Runnable {
        override fun run() {
            checkNotificationListenerHealth()
            checkAccessibilityHealth()
            handler.postDelayed(this, HEALTH_CHECK_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        handler.removeCallbacks(healthCheckRunnable)
        handler.post(healthCheckRunnable)

        Log.d(TAG, "Keep-alive foreground service started")
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(healthCheckRunnable)
        Log.d(TAG, "Keep-alive foreground service destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "自动记账服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持自动记账通知监听功能在后台持续运行"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("自动记账运行中")
        .setContentText("正在监听支付通知，点击打开应用")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun checkNotificationListenerHealth() {
        val cn = ComponentName(this, AccountingNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = flat?.contains(cn.flattenToString()) == true

        if (!isEnabled) {
            Log.w(TAG, "Notification listener was disabled, requesting re-enable...")
            try {
                startActivity(
                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification listener settings", e)
            }
        } else {
            Log.d(TAG, "Notification listener is healthy")
        }
    }

    private fun checkAccessibilityHealth() {
        val cn = ComponentName(this, AccountingAccessibilityService::class.java)
        val enabled = isAccessibilityServiceEnabled(cn)
        if (!enabled) {
            Log.w(TAG, "Accessibility service is disabled")
        } else {
            Log.d(TAG, "Accessibility service is healthy")
        }
    }

    private fun isAccessibilityServiceEnabled(componentName: ComponentName): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = componentName.flattenToString()
        return enabledServices.split(":").any { it.equals(expected, ignoreCase = true) }
    }

    companion object {
        private const val TAG = "KeepAliveSvc"
        private const val FOREGROUND_CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 1001
        private const val HEALTH_CHECK_INTERVAL_MS = 60_000L

        fun isNotificationListenerEnabled(componentName: ComponentName, flat: String?): Boolean {
            return flat?.contains(componentName.flattenToString()) == true
        }

        fun isAccessibilityServiceEnabled(context: android.content.Context, componentName: ComponentName): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = componentName.flattenToString()
            return enabledServices.split(":").any { it.equals(expected, ignoreCase = true) }
        }
    }
}
