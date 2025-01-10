package com.cbi.cmp_project.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    @Streaming
    @GET("downloadDatasetCompanyJson")
    suspend fun downloadDatasetCompany(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetBUnitJson")
    suspend fun downloadDatasetBUnit(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetDivisionJson")
    suspend fun downloadDatasetDivision(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetTPHJson")
    suspend fun downloadDatasetTPH(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetFieldJson")
    suspend fun downloadDatasetField(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetWorkerInGroupJson")
    suspend fun downloadDatasetWorkerInGroup(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetWorkerGroupJson")
    suspend fun downloadDatasetWorkerGroup(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetWorkerJson")
    suspend fun downloadDatasetWorker(): Response<ResponseBody>
}