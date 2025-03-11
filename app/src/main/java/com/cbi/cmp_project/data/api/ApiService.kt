package com.cbi.cmp_project.data.api

import android.graphics.Region
import androidx.room.Query
import com.cbi.cmp_project.data.model.LoginResponse
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.model.uploadCMP.UploadCMPResponse
import com.cbi.cmp_project.data.model.weighBridge.UploadStagingResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @POST("api/insert")
    suspend fun insertESPBKraniTimbangPPRO(@Body request: dataUploadEspbKraniTimbangPPRO): Response<UploadStagingResponse>

    data class dataUploadEspbKraniTimbangPPRO(
        @SerializedName("dept_ppro") val dept_ppro: String,
        @SerializedName("divisi_ppro") val divisi_ppro: String,
        @SerializedName("commodity") val commodity: String,
        @SerializedName("blok_jjg") val blok_jjg: String,
        @SerializedName("nopol") val nopol: String,
        @SerializedName("driver") val driver: String,
        @SerializedName("pemuat_id") val pemuat_id: String,
        @SerializedName("transporter_id") val transporter_id: String,
        @SerializedName("mill_id") val mill_id: String,
        @SerializedName("created_by_id") val created_by_id: String,
        @SerializedName("created_at") val created_at: String,
        @SerializedName("no_espb") val no_espb: String,
    )

    @Multipart
    @POST("cmpmain/upload")
    suspend fun uploadZip(
        @Part zipFile: MultipartBody.Part
    ): Response<UploadCMPResponse>

    @FormUrlEncoded
    @POST("cmpmain/status")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/x-www-form-urlencoded"
    )
    suspend fun checkStatusUploadCMP(
        @Field("idData") ids: String // Send list as a comma-separated string
    ):  Response<ResponseBody>

}