package com.wzf.accounting.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wzf.accounting.data.api.RetrofitInstance
import com.wzf.accounting.data.local.NotificationStore
import com.wzf.accounting.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AccountingViewModel"
    private val api = RetrofitInstance.api
    private val notificationStore = NotificationStore(application)

    private val _filteredNotifications = MutableStateFlow<List<AutoAccountingNotification>>(emptyList())
    val filteredNotifications: StateFlow<List<AutoAccountingNotification>> = _filteredNotifications.asStateFlow()

    private val _selectableApps = MutableStateFlow<List<SelectableApp>>(emptyList())
    val selectableApps: StateFlow<List<SelectableApp>> = _selectableApps.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Filter state - 默认当前月份
    var fromDate = MutableStateFlow<String?>(monthStart())
    var toDate = MutableStateFlow<String?>(monthEnd())
    var selectedCategory = MutableStateFlow<String?>(null)
    var searchQuery = MutableStateFlow("")

    private fun monthStart(): String = "${LocalDate.now().year}-${String.format("%02d", LocalDate.now().monthValue)}-01"
    private fun monthEnd(): String {
        val now = LocalDate.now()
        val lastDay = now.withDayOfMonth(now.lengthOfMonth())
        return lastDay.toString()
    }

    init {
        Log.d(TAG, "Initializing AccountingViewModel (default: ${fromDate.value} ~ ${toDate.value})")
        refreshAll()
        refreshNotificationData()
    }

    private fun cleanMsg(msg: String?): String = msg
        ?.replace(Regex("/[0-9a-fA-F:.]+\\s*\\(port\\s*\\d+\\)"), "")
        ?.replace(Regex("/[0-9a-fA-F:.]+"), "")
        ?.trim() ?: "Unknown error"

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                fetchCategories()
                fetchExpenses()
                fetchStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error in refreshAll: ${e.message}")
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshStatsOnly() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                fetchStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing stats: ${e.message}")
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchCategories() {
        val response = api.getCategories()
        _categories.value = response.categories
    }

    private suspend fun fetchExpenses() {
        val cat = if (selectedCategory.value == "全部类别") null else selectedCategory.value
        val q = if (searchQuery.value.isBlank()) null else searchQuery.value
        val response = api.getExpenses(
            from = fromDate.value,
            to = toDate.value,
            category = cat,
            q = q
        )
        _expenses.value = response.expenses
    }

    private suspend fun fetchStats() {
        val response = api.getStats(
            from = fromDate.value,
            to = toDate.value
        )
        _stats.value = response
    }

    fun refreshNotificationData() {
        viewModelScope.launch {
            _filteredNotifications.value = notificationStore.getNotifications()
            loadSelectableApps()
        }
    }

    fun toggleNotificationApp(packageName: String) {
        val selected = notificationStore.getSelectedPackages().toMutableSet()
        if (!selected.add(packageName)) selected.remove(packageName)
        notificationStore.setSelectedPackages(selected)
        refreshNotificationData()
    }

    fun captureAllNotificationApps() {
        notificationStore.setSelectedPackages(emptySet())
        refreshNotificationData()
    }

    fun deleteStoredNotification(id: String) {
        notificationStore.deleteNotification(id)
        refreshNotificationData()
    }

    fun addExpenseFromNotification(notificationId: String, amount: Double, category: String, note: String, spentAt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.createExpense(ExpenseInput(amount, category, note, spentAt))
                notificationStore.markRecorded(notificationId)
                refreshNotificationData()
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "addExpenseFromNotification failed: ${e.message}", e)
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun notificationSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    private fun loadSelectableApps() {
        val selected = notificationStore.getSelectedPackages()
        val pm = getApplication<Application>().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                SelectableApp(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    isSelected = selected.contains(app.packageName)
                )
            }
            .sortedWith(compareByDescending<SelectableApp> { it.isSelected }.thenBy { it.appName.lowercase() })
        _selectableApps.value = apps
    }

    fun addExpense(amount: Double, category: String, note: String, spentAt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.createExpense(ExpenseInput(amount, category, note, spentAt))
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "addExpense failed: ${e.message}")
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateExpense(id: Int, amount: Double, category: String, note: String, spentAt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.updateExpense(id, ExpenseInput(amount, category, note, spentAt))
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "updateExpense failed: ${e.message}")
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.deleteExpense(id)
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "deleteExpense failed: ${e.message}")
                _error.value = cleanMsg(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun resetFilters() {
        fromDate.value = monthStart()
        toDate.value = monthEnd()
        selectedCategory.value = null
        searchQuery.value = ""
        refreshAll()
    }
}
