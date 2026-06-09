package com.wzf.accounting.data.api

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val TAG = "RetrofitInstance"
    private const val BASE_URL = "https://wzfly.top/accounting/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val logging = HttpLoggingInterceptor { message ->
        Log.d(TAG, "OkHttp: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Accounting-Android/1.0")
                .header("Accept", "application/json")
                .build()
            
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                // 清洗错误消息中的 IP/Port
                val cleanMsg = e.message?.replace(Regex("/[0-9a-fA-F:.]+\\s*\\(port\\s*\\d+\\)", RegexOption.IGNORE_CASE), "")
                    ?.replace(Regex("/[0-9a-fA-F:.]+", RegexOption.IGNORE_CASE), "")
                    ?.trim()
                throw Exception(cleanMsg ?: e.message)
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val api: AccountingApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AccountingApi::class.java)
    }
}
