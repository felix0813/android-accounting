package com.wzf.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wzf.accounting.ui.screens.MainScreen
import com.wzf.accounting.ui.theme.AccountingTheme
import com.wzf.accounting.ui.viewmodel.AccountingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountingTheme {
                val viewModel: AccountingViewModel = viewModel()
                MainScreen(viewModel = viewModel)
            }
        }
    }
}