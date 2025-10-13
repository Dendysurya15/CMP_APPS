package com.cbi.mobile_plantation.data.repository

import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.model.DownloadMapResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger

class DownloadIDMapRepository(
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance
) {

    suspend fun getDownloadMapList(): Result<DownloadMapResponse> {
        return try {
            val response = apiService.getDownloadMapList()
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
}