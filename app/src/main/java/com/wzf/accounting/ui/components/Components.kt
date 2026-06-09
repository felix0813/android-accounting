package com.wzf.accounting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzf.accounting.ui.theme.PrimaryPurple
import com.wzf.accounting.ui.theme.SecondaryPurple

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryPurple, SecondaryPurple)
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(
    category: String,
    note: String,
    amount: Double,
    date: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (note.isNotEmpty()) {
                    Text(
                        text = note,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = date,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${String.format("%.2f", amount)}",
                    color = PrimaryPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row {
                    TextButton(onClick = onEdit) {
                        Text("编辑", color = PrimaryPurple)
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除", color = Color.Red)
                    }
                }
            }
        }
    }
}
