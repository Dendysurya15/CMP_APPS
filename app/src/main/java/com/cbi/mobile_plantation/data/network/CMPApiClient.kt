package com.cbi.mobile_plantation.data.network

import android.annotation.SuppressLint
import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object CMPApiClient {
    private const val BASE_URL = "https://cmp.citraborneo.co.id/api/"
    val instance: ApiService by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val prefManager = PrefManager(context)

                // Check if it's an auth-related endpoint
                val request = if (original.url.encodedPath.contains("/auth/")) {
                    original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .method(original.method, original.body)
                        .build()
                } else {
                    original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer ${prefManager.token}")
                        .method(original.method, original.body)
                        .build()
                }

                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    private lateinit var context: Context
    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
