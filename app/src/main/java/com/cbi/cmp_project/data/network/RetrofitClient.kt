package com.cbi.cmp_project.data.network

import com.cbi.cmp_project.data.api.ApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://auth.srs-ssms.com/api/"
    private const val TIMEOUT_SECONDS = 10L

    val instance: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS) // Connection timeout
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)    // Read timeout
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)   // Write timeout
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
