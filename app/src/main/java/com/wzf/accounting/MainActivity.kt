package com.wzf.accounting

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wzf.accounting.service.AccountingAccessibilityService
import com.wzf.accounting.service.AccountingNotificationListenerService
import com.wzf.accounting.service.NotificationKeepAliveService
import com.wzf.accounting.ui.screens.AutoAccountingScreen
import com.wzf.accounting.ui.screens.MainScreen
import com.wzf.accounting.ui.screens.StatsScreen
import com.wzf.accounting.ui.theme.AccountingTheme
import com.wzf.accounting.ui.viewmodel.AccountingViewModel
import androidx.core.net.toUri

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS permission ${if (granted) "granted" else "denied"}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ensureNotificationListenerPermission()
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
        startKeepAliveService()

        setContent {
            AccountingTheme {
                val viewModel: AccountingViewModel = viewModel()
                val navItems = remember {
                    listOf(
                        BottomNavItem("账单", Icons.AutoMirrored.Filled.List),
                        BottomNavItem("自动", Icons.Default.Star),
                        BottomNavItem("统计", Icons.Default.Star)
                    )
                }
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            navItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            MainScreen(viewModel = viewModel)
                        }
                        1 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            AutoAccountingScreen(viewModel = viewModel)
                        }
                        2 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            StatsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val cn = ComponentName(this, AccountingNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!NotificationKeepAliveService.isNotificationListenerEnabled(cn, flat)) {
            Log.w(TAG, "Notification listener not enabled on resume, prompting settings")
            ensureNotificationListenerPermission()
        }
        val a11yCn = ComponentName(this, AccountingAccessibilityService::class.java)
        if (!NotificationKeepAliveService.isAccessibilityServiceEnabled(this, a11yCn)) {
            Log.d(TAG, "Accessibility service not enabled (user can enable from the auto-accounting screen)")
        }
        startKeepAliveService()
    }

    private fun ensureNotificationListenerPermission() {
        val cn = ComponentName(this, AccountingNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!NotificationKeepAliveService.isNotificationListenerEnabled(cn, flat)) {
            Log.w(TAG, "Notification listener not enabled, opening settings")
            try {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification listener settings", e)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:$packageName".toUri()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request battery optimization exemption", e)
            }
        }
    }

    private fun startKeepAliveService() {
        val intent = Intent(this, NotificationKeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
