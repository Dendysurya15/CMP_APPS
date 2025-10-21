package com.cbi.mobile_plantation.data.repository

import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.model.MissingPhotosResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger

class CheckPhotoRepository(
    private val ApiService: ApiService = ApiProvider.currentApiService
) {

    suspend fun getMissingPhotos(
        tanggal: String,
        deptAbbr: String,
        createdBy: Int
    ): Result<MissingPhotosResponse> {
        return try {
            val response = ApiService.getMissingPhotos(tanggal, deptAbbr, createdBy)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error fetching missing photos: ${e.message}")
            Result.failure(e)
        }
    }
}