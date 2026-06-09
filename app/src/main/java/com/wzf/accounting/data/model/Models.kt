package com.wzf.accounting.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: Int,
    val amount: Double,
    val category: String,
    val note: String,
    val spentAt: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ExpenseInput(
    val amount: Double,
    val category: String,
    val note: String,
    val spentAt: String
)

@Serializable
data class CategoriesResponse(
    val categories: List<String>
)

@Serializable
data class ExpensesResponse(
    val expenses: List<Expense>
)

@Serializable
data class ExpenseResponse(
    val expense: Expense
)

@Serializable
data class StatsResponse(
    val summary: Summary,
    val byCategory: List<CategoryStat>,
    val byMonth: List<MonthStat>
)

@Serializable
data class Summary(
    val count: Int,
    val total: Double
)

@Serializable
data class CategoryStat(
    val category: String,
    val total: Double,
    val count: Int
)

@Serializable
data class MonthStat(
    val month: String,
    val total: Double,
    val count: Int
)
