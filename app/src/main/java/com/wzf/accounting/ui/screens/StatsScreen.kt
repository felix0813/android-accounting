package com.wzf.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzf.accounting.data.model.CategoryStat
import com.wzf.accounting.data.model.MonthStat
import com.wzf.accounting.ui.viewmodel.AccountingViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: AccountingViewModel) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // 统计页独立的月份筛选状态
    var statsFrom by remember { mutableStateOf(viewModel.fromDate.value ?: monthStart()) }
    var statsTo by remember { mutableStateOf(viewModel.toDate.value ?: monthEnd()) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf<String?>(null) }
    var showCatMenu by remember { mutableStateOf(false) }

    // 从 ViewModel 获取带筛选的统计数据
    val currentStats = stats // local val to avoid smart-cast issue
    val filteredStats = remember(currentStats, selectedCat) {
        if (selectedCat == null) {
            currentStats
        } else {
            currentStats?.copy(
                byCategory = currentStats.byCategory.filter { it.category == selectedCat },
                byMonth = currentStats.byMonth
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计分析", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5E35B1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 日期筛选行
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("筛选统计范围", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateFieldWithPicker("开始日期", statsFrom, showFromPicker,
                                    onToggle = { showFromPicker = !showFromPicker },
                                    onDateSelected = {
                                        statsFrom = it
                                        showFromPicker = false
                                        viewModel.fromDate.value = it
                                        viewModel.refreshStatsOnly()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DateFieldWithPicker("结束日期", statsTo, showToPicker,
                                    onToggle = { showToPicker = !showToPicker },
                                    onDateSelected = {
                                        statsTo = it
                                        showToPicker = false
                                        viewModel.toDate.value = it
                                        viewModel.refreshStatsOnly()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 类别过滤
                            Box {
                                OutlinedTextField(
                                    value = selectedCat ?: "全部类别",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("按类别查看") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { showCatMenu = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = showCatMenu,
                                    onDismissRequest = { showCatMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("全部类别") },
                                        onClick = {
                                            selectedCat = null
                                            showCatMenu = false
                                        }
                                    )
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                selectedCat = cat
                                                showCatMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 总览卡片
                item {
                    currentStats?.summary?.let { summary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF5E35B1)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (selectedCat != null) "$selectedCat 总计" else "总计支出",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "¥${String.format("%.2f", if (selectedCat != null) filteredStats?.byCategory?.firstOrNull()?.total ?: 0.0 else summary.total)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Text(
                                        "记录数: ${if (selectedCat != null) filteredStats?.byCategory?.firstOrNull()?.count ?: 0 else summary.count}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                    if (selectedCat == null) {
                                        val avg = if (summary.count > 0) summary.total / summary.count else 0.0
                                        Text(
                                            "均笔: ¥${String.format("%.2f", avg)}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 按月统计
                item {
                    Text(
                        text = "按月统计",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val monthData = currentStats?.byMonth ?: emptyList()
                            if (monthData.isEmpty()) {
                                Text("暂无数据", color = Color.Gray)
                            } else {
                                val maxTotal = monthData.maxOf { it.total }.toFloat()
                                monthData.forEachIndexed { index, monthStat ->
                                    val progress = if (maxTotal > 0) monthStat.total.toFloat() / maxTotal else 0f
                                    MonthProgressItem(monthStat, progress)
                                    if (index < monthData.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = Color(0xFFF0F0F0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 按类别统计
                item {
                    Text(
                        text = "按类别统计",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val catData = currentStats?.byCategory ?: emptyList()
                            if (catData.isEmpty()) {
                                Text("暂无数据", color = Color.Gray)
                            } else {
                                val maxTotal = catData.maxOf { it.total }.toFloat()
                                catData.forEachIndexed { index, catStat ->
                                    val progress = if (maxTotal > 0) catStat.total.toFloat() / maxTotal else 0f
                                    CategoryProgressItem(catStat, progress)
                                    if (index < catData.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = Color(0xFFF0F0F0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun MonthProgressItem(month: MonthStat, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(month.month, fontWeight = FontWeight.Medium)
            Text(
                "¥${String.format("%.2f", month.total)}  (${month.count}笔)",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5E35B1)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .background(Color(0xFF5E35B1), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun CategoryProgressItem(cat: CategoryStat, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cat.category, fontWeight = FontWeight.Medium)
            Text(
                "¥${String.format("%.2f", cat.total)}  (${cat.count}笔)",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5E35B1)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .background(
                        when (cat.category) {
                            "餐饮" -> Color(0xFFFF7043)
                            "交通" -> Color(0xFF42A5F5)
                            "购物" -> Color(0xFFAB47BC)
                            "居住" -> Color(0xFF26A69A)
                            "娱乐" -> Color(0xFFEC407A)
                            "医疗" -> Color(0xFFEF5350)
                            "学习" -> Color(0xFF66BB6A)
                            else -> Color(0xFF5E35B1)
                        },
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// ---------- 日期选择字段 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFieldWithPicker(
    label: String,
    dateStr: String,
    showDialog: Boolean,
    onToggle: () -> Unit,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(dateStr)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) { null }
    )

    OutlinedTextField(
        value = dateStr,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.clickable { onToggle() },
        trailingIcon = {
            IconButton(onClick = { onToggle() }) {
                Icon(Icons.Default.DateRange, contentDescription = "选择日期")
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { onToggle() },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onDateSelected(d)
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { onToggle() }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun monthStart(): String =
    "${LocalDate.now().year}-${String.format("%02d", LocalDate.now().monthValue)}-01"

private fun monthEnd(): String {
    val now = LocalDate.now()
    return now.withDayOfMonth(now.lengthOfMonth()).toString()
}
