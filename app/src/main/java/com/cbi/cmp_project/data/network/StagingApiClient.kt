package com.cbi.cmp_project.data.network

import android.annotation.SuppressLint
import com.cbi.cmp_project.data.api.ApiService
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

object StagingApiClient {
    private const val BASE_URL = "http://192.168.1.76:3000/"
    private const val API_KEY = "9f2c5c6b8f8e7d6a5c4b3a2b1a0f9e8d7c6b5a4b3c2d1e0f9a8b7c6d5e4f3a2b1"

    val instance: ApiService by lazy {
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
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}


