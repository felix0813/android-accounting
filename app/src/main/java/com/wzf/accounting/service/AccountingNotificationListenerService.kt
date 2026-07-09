package com.wzf.accounting.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wzf.accounting.data.local.NotificationStore
import com.wzf.accounting.data.model.AutoAccountingNotification

class AccountingNotificationListenerService : NotificationListenerService() {
    private val amountPattern = Regex("\\d+(?:\\.\\d{1,2})?")
    private val decimalPattern = Regex("\\d+\\.\\d+")
    private val financialKeywords = listOf(
        "元", "¥", "￥", "$", "€", "£",
        "支付", "付款", "收款", "转账", "消费", "扣款", "到账", "入账", "充值",
        "红包", "话费", "余额", "提现", "还款", "账单", "交易", "订单",
        "payment", "pay", "transfer", "balance", "refund", "charge"
    )
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

            if (!isLikelyFinancialNotification(searchable)) {
                Log.d(TAG, "Filtered non-financial notification: package=${sbn.packageName}, title=$title")
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

    private fun isLikelyFinancialNotification(text: String): Boolean {
        if (!amountPattern.containsMatchIn(text)) return false

        val lowerText = text.lowercase()
        if (financialKeywords.any { kw -> lowerText.contains(kw.lowercase()) }) return true

        if (decimalPattern.containsMatchIn(text)) return true

        return false
    }

    companion object {
        private const val TAG = "AcctNotificationSvc"
    }
}
