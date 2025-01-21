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
    @GET("downloadDatasetKaryawanJson")
    suspend fun downloadDatasetKaryawanJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetKemandoranJson")
    suspend fun downloadDatasetKemandoranJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetKemandoranDetailJson")
    suspend fun downloadDatasetKemandoranDetailJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetTPHNewJson")
    suspend fun downloadDatasetTPHNewJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetBlokJson")
    suspend fun downloadDatasetBlokJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetDivisiJson")
    suspend fun downloadDatasetDivisiJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetDeptJson")
    suspend fun downloadDatasetDeptJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetWilayahJson")
    suspend fun downloadDatasetWilayahJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetRegionalJson")
    suspend fun downloadDatasetRegionalJson(): Response<ResponseBody>


    @GET("getTablesLatestModified")
    suspend fun getTablesLatestModified(): Response<TablesModifiedResponse>

    data class TablesModifiedResponse(
        val statusCode: Int,
        val message: String,
        val data: Map<String, String?>
    )
}