package com.cbi.mobile_plantation.data.network

import android.annotation.SuppressLint
import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.utils.PrefManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object TestingAPIClient {
    private const val BASE_URL = "http://10.9.116.150:3005/"
//    private const val BASE_URL = "http://192.168.1.34:4005/" // pc ho

    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    val instance: ApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            if (!this::context.isInitialized) {
                throw IllegalStateException("TestingAPIClient not initialized. Call init(context) before using the API client.")
            }

            val original = chain.request()
            val prefManager = PrefManager(context)
            val token = prefManager.token ?: ""

            val request = original.newBuilder()
                .header("Authorization", "Bearer $token") // Add Bearer token
                .method(original.method, original.body)
                .build()

            chain.proceed(request)
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
