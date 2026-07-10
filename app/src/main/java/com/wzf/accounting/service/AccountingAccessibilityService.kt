package com.wzf.accounting.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.wzf.accounting.data.local.NotificationStore
import com.wzf.accounting.data.model.AutoAccountingNotification

class AccountingAccessibilityService : AccessibilityService() {

    private val store by lazy { NotificationStore(applicationContext) }

    override fun onServiceConnected() {
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return

        runCatching {
            val packageName = event.packageName?.toString() ?: return

            val selectedPackages = store.getSelectedPackages()
            if (selectedPackages.isNotEmpty() && packageName !in selectedPackages) return

            val textFromEvent = event.text?.filter { !it.isNullOrBlank() }
                ?.joinToString(" ")
                ?.trim()
                .orEmpty()

            val textFromTree = extractTextFromNodeTree(rootInActiveWindow)

            val combinedText = listOf(textFromEvent, textFromTree)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")

            if (combinedText.isBlank()) {
                Log.d(TAG, "Empty notification text from accessibility, package=$packageName")
                return
            }

            val searchable = combinedText
            if (!NotificationFilter.isLikelyFinancialNotification(searchable)) {
                Log.d(TAG, "Filtered non-financial notification via accessibility: package=$packageName")
                return
            }

            val parts = searchable.split("\\s+".toRegex(), limit = 3)
            val title = parts.getOrElse(0) { "" }
            val content = parts.drop(1).joinToString(" ").ifBlank { searchable }

            val appName = resolveAppName(packageName)

            val now = System.currentTimeMillis()
            store.saveNotification(
                AutoAccountingNotification(
                    id = "a11y:$packageName:${now}",
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    content = content,
                    postedAt = now,
                    capturedAt = now,
                    fromActiveSnapshot = false
                )
            )
            Log.d(TAG, "Captured financial notification via accessibility: package=$packageName, title=$title")
        }.onFailure { Log.e(TAG, "Failed to handle accessibility event", it) }
    }

    @Suppress("DEPRECATION")
    private fun extractTextFromNodeTree(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val texts = mutableListOf<String>()
        collectTexts(root, texts)
        root.recycle()
        return texts.joinToString(" ")
    }

    @Suppress("DEPRECATION")
    private fun collectTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, texts)
            child.recycle()
        }
    }

    private fun resolveAppName(packageName: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AcctAccessibility"
    }
}
