package com.cbi.mobile_plantation.data.repository

import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.model.DownloadMapProgressResponse
import com.cbi.mobile_plantation.data.model.DownloadMapResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import okhttp3.ResponseBody
import retrofit2.Response

class DownloadIDMapRepository(
    private val ApiService: ApiService = ApiProvider.currentApiService
) {

    suspend fun getDownloadMapList(): Result<DownloadMapResponse> {
        return try {
            val response = ApiService.getDownloadMapList()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error fetching download map list: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getDownloadMapProgress(downloadId: String): Result<DownloadMapProgressResponse> {
        return try {
            val response = ApiService.getDownloadMapProgress(downloadId)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error fetching download map progress: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadMapChunk(downloadId: String, chunkIndex: Int): Response<ResponseBody> {
        return try {
            ApiService.downloadMapChunk(downloadId, chunkIndex)
        } catch (e: Exception) {
            AppLogger.e("Error downloading map chunk $chunkIndex: ${e.message}")
            throw e
        }
    }
}