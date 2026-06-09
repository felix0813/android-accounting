package com.wzf.accounting.data.api

import com.wzf.accounting.data.model.CategoriesResponse
import com.wzf.accounting.data.model.ExpenseInput
import com.wzf.accounting.data.model.ExpenseResponse
import com.wzf.accounting.data.model.ExpensesResponse
import com.wzf.accounting.data.model.StatsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AccountingApi {
    @GET("api/categories")
    suspend fun getCategories(): CategoriesResponse

    @PUT("api/categories")
    suspend fun updateCategories(@Body body: CategoriesResponse): CategoriesResponse

    @GET("api/expenses")
    suspend fun getExpenses(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("category") category: String? = null,
        @Query("q") q: String? = null,
        @Query("limit") limit: Int? = null
    ): ExpensesResponse

    @POST("api/expenses")
    suspend fun createExpense(@Body expense: ExpenseInput): ExpenseResponse

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: Int, @Body expense: ExpenseInput): ExpenseResponse

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Map<String, Boolean>

    @GET("api/stats")
    suspend fun getStats(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): StatsResponse
}
