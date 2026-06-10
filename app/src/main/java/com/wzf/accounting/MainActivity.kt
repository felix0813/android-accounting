package com.wzf.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wzf.accounting.ui.screens.MainScreen
import com.wzf.accounting.ui.screens.StatsScreen
import com.wzf.accounting.ui.theme.AccountingTheme
import com.wzf.accounting.ui.viewmodel.AccountingViewModel

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountingTheme {
                val viewModel: AccountingViewModel = viewModel()
                val navItems = remember {
                    listOf(
                        BottomNavItem("账单", Icons.AutoMirrored.Filled.List),
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
                        1 -> StatsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
