package com.wzf.accounting.ui.screens

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzf.accounting.data.model.AutoAccountingNotification
import com.wzf.accounting.ui.viewmodel.AccountingViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAccountingScreen(viewModel: AccountingViewModel) {
    val notifications by viewModel.filteredNotifications.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current
    var activeNotification by remember { mutableStateOf<AutoAccountingNotification?>(null) }
    var showNotificationSources by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 左侧图标逻辑：根据当前显示的页面决定是“返回”还是“列表切换”
                IconButton(onClick = {
                    showNotificationSources = if (showNotificationSources) {
                        // 如果当前在通知来源页面，点击返回主页面
                        false
                    } else {
                        // 如果在主页面，点击切换显示通知来源
                        true
                    }
                }) {
                    Icon(
                        // 这里使用了系统默认的导航返回图标，如果项目中没有，可以继续使用 List 图标，或者导入 NavigationIcon
                        imageVector = if (showNotificationSources) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.List,
                        contentDescription = if (showNotificationSources) "返回" else "通知来源",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. 中间标题
                Text(
                    text = if (showNotificationSources) "通知来源" else "自动记账",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )

                // 3. 右侧图标保持不变（刷新和设置）
                IconButton(onClick = { viewModel.refreshNotificationData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = {
                    try {
                        context.startActivity(viewModel.notificationSettingsIntent())
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "无法打开通知访问设置", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "通知权限", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (showNotificationSources) {
            NotificationSourcesPage(viewModel = viewModel)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("待处理通知（${notifications.size}）", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                items(notifications) { notification ->
                    NotificationCard(notification, onRecord = { activeNotification = notification }, onDelete = { viewModel.deleteStoredNotification(notification.id) })
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    activeNotification?.let { notification ->
        RecordNotificationDialog(
            notification = notification,
            categories = categories,
            onDismiss = { activeNotification = null },
            onConfirm = { amount, category, note, date ->
                viewModel.addExpenseFromNotification(notification.id, amount, category, note, date)
                activeNotification = null
            }
        )
    }
}

@Composable
private fun NotificationSourcesPage(viewModel: AccountingViewModel) {
    val apps by viewModel.selectableApps.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("通知来源", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("未勾选任何应用时会截取所有应用通知；勾选后仅保留指定应用中标题或内容包含数字的通知。", fontSize = 13.sp, color = Color.Gray)
                    OutlinedButton(onClick = { viewModel.captureAllNotificationApps() }, modifier = Modifier.fillMaxWidth()) { Text("截取所有应用") }
                }
            }
        }
        items(apps) { app ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Checkbox(checked = app.isSelected, onCheckedChange = { viewModel.toggleNotificationApp(app.packageName) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, fontWeight = FontWeight.SemiBold)
                        Text(app.packageName, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun NotificationCard(notification: AutoAccountingNotification, onRecord: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (notification.isRecorded) Color(0xFFE8F5E9) else Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notification.appName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(notification.postedAt)), fontSize = 12.sp, color = Color.Gray)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除") }
            }
            if (notification.title.isNotBlank()) Text(notification.title, fontWeight = FontWeight.SemiBold)
            Text(notification.content.ifBlank { "（无内容）" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRecord, enabled = !notification.isRecorded) { Text(if (notification.isRecorded) "已记账" else "提取金额并记账") }
            }
        }
    }
}

@Composable
private fun RecordNotificationDialog(
    notification: AutoAccountingNotification,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String) -> Unit
) {
    var amount by remember { mutableStateOf(extractAmount(notification.title + " " + notification.content)?.toString() ?: "") }
    var category by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    val note = listOf(notification.appName, notification.title, notification.content).filter { it.isNotBlank() }.joinToString(" - ").take(200)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从通知记账") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, singleLine = true)
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(category.ifBlank { "选择类别" }) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) }
                }
                Text(note, fontSize = 13.sp, color = Color.Gray)
            }
        },
        confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { if (category.isNotBlank()) onConfirm(it, category, note, LocalDate.now().toString()) } }) { Text("记账") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun extractAmount(text: String): Double? = Regex("\\d+(?:\\.\\d+)?").find(text)?.value?.toDoubleOrNull()
