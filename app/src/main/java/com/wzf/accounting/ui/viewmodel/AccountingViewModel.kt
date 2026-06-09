package com.wzf.accounting.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzf.accounting.data.api.RetrofitInstance
import com.wzf.accounting.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountingViewModel : ViewModel() {
    private val TAG = "AccountingViewModel"
    private val api = RetrofitInstance.api 

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

    // Filter state
    var fromDate = MutableStateFlow<String?>(null)
    var toDate = MutableStateFlow<String?>(null)
    var selectedCategory = MutableStateFlow<String?>(null)
    var searchQuery = MutableStateFlow("")

    init {
        Log.d(TAG, "Initializing AccountingViewModel")
        refreshAll()
    }

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
                _error.value = e.message
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

    fun addExpense(amount: Double, category: String, note: String, spentAt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.createExpense(ExpenseInput(amount, category, note, spentAt))
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "addExpense failed: ${e.message}")
                _error.value = e.message
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
                _error.value = e.message
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
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun resetFilters() {
        fromDate.value = null
        toDate.value = null
        selectedCategory.value = null
        searchQuery.value = ""
        refreshAll()
    }
}
