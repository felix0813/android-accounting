package com.wzf.accounting.data.api

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier

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

    // 信任所有证书（仅开发环境使用）
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Accounting-Android/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
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
