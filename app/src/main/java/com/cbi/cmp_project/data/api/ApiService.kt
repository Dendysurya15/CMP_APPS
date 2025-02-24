package com.cbi.cmp_project.data.api

import android.graphics.Region
import androidx.room.Query
import com.cbi.cmp_project.data.model.LoginResponse
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
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


    data class LoginRequest(
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String
    )


    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Streaming
    @POST("dataset/check-and-download")
    @Headers("Accept: application/json")
    suspend fun downloadDataset(@Body request: DatasetRequest): Response<ResponseBody>

    @POST("org/fetch-mill")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun downloadSmallDataset(@Body body: Map<String, Int>): Response<ResponseBody>

}