package com.cbi.cmp_project.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {
    @Streaming  // Add this annotation
    @GET("downloadEncryptedDatasetTPHJson")
    suspend fun downloadDataset(): Response<ResponseBody>
}