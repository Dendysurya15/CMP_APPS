package com.cbi.mobile_plantation.data.network

import android.annotation.SuppressLint
import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object Constants {
    const val NETWORK_TIMEOUT_MS = 20_000L // 20 seconds timeout
}

@SuppressLint("StaticFieldLeak")
object StagingApiClient {
    private const val DEFAULT_BASE_URL = "http://192.168.1.76:3000/"
    private const val API_KEY = "9f2c5c6b8f8e7d6a5c4b3a2b1a0f9e8d7c6b5a4b3c2d1e0f9a8b7c6d5e4f3a2b1"

    private var baseUrl = DEFAULT_BASE_URL
    private lateinit var context: Context
    private var apiService: ApiService? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    // Method to update the base URL
    fun updateBaseUrl(newUrl: String) {
        baseUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        // Reset the API service so it will be recreated with the new URL
        apiService = null
    }

    val instance: ApiService
        get() {
            // Return existing instance if available and URL hasn't changed
            apiService?.let { return it }

            // Create new instance with current baseUrl
            val gson = GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .create()

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("x-api-key", API_KEY)
                        .method(original.method, original.body)
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
                .also { apiService = it }  // Store the instance
        }
}


