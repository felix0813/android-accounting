package com.wzf.accounting.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wzf.accounting.data.model.AutoAccountingNotification
import kotlinx.serialization.json.Json
import androidx.core.content.edit

class NotificationStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getNotifications(): List<AutoAccountingNotification> = runCatching {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        json.decodeFromString<List<AutoAccountingNotification>>(raw)
    }.onFailure { Log.e(TAG, "Failed to read stored notifications", it) }.getOrDefault(emptyList())

    fun saveNotification(notification: AutoAccountingNotification) {
        val current = getNotifications().filterNot { it.id == notification.id }.toMutableList()

        val isRecentDuplicate = current.any { existing ->
            existing.packageName == notification.packageName &&
                normalize(existing.title + existing.content) == normalize(notification.title + notification.content) &&
                kotlin.math.abs(existing.capturedAt - notification.capturedAt) < DEDUP_WINDOW_MS
        }
        if (isRecentDuplicate) {
            Log.d(TAG, "Skipped duplicate notification from ${notification.packageName}")
            return
        }

        current.add(0, notification)
        persist(current.take(MAX_NOTIFICATIONS))
        Log.d(TAG, "Stored filtered notification from ${notification.packageName}, total=${current.size.coerceAtMost(MAX_NOTIFICATIONS)}")
    }

    private fun normalize(text: String): String =
        text.replace("\\s+".toRegex(), " ").trim()

    fun deleteNotification(id: String) {
        val updated = getNotifications().filterNot { it.id == id }
        persist(updated)
        Log.d(TAG, "Deleted stored notification id=$id")
    }

    fun markRecorded(id: String) {
        val updated = getNotifications().map { if (it.id == id) it.copy(isRecorded = true) else it }
        persist(updated)
        Log.d(TAG, "Marked notification as recorded id=$id")
    }

    fun getSelectedPackages(): Set<String> = prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()).orEmpty()

    fun setSelectedPackages(packages: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_PACKAGES, packages) }
        Log.d(TAG, "Updated selected notification packages: ${packages.size}")
    }

    private fun persist(notifications: List<AutoAccountingNotification>) {
        runCatching {
            prefs.edit { putString(KEY_NOTIFICATIONS, json.encodeToString(notifications)) }
        }.onFailure { Log.e(TAG, "Failed to persist filtered notifications", it) }
    }

    companion object {
        private const val TAG = "NotificationStore"
        private const val PREFS_NAME = "auto_accounting_notifications"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
        private const val MAX_NOTIFICATIONS = 300
        private const val DEDUP_WINDOW_MS = 5_000L
    }
}
