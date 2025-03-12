package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.File


sealed class SaveResultNewUploadDataCMP {
    object Success : SaveResultNewUploadDataCMP()
    data class Error(val exception: Exception) : SaveResultNewUploadDataCMP()
}

class UploadCMPRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val uploadCMPDao = database.uploadCMPDao()


    suspend fun UpdateOrInsertDataUpload(data: UploadCMPModel) {
        val existingCount = uploadCMPDao.getTrackingIdCount(data.tracking_id!!)

        if (existingCount > 0) {
            uploadCMPDao.updateStatus(data.tracking_id, data.status!!)
        } else {
            uploadCMPDao.insertNewData(data)
        }
    }

    suspend fun getAllData(): List<UploadCMPModel> {
        return uploadCMPDao.getAllData() // Calls the DAO function
    }


    suspend fun uploadZipToServer(
        fileZipPath: String,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadCMPResponse> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(fileZipPath)
                if (!file.exists()) {
                    val errorMsg = "File does not exist: $fileZipPath"
                    AppLogger.d(errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                AppLogger.d("Starting file upload: ${file.name}")

                val progressRequestBody = AppUtils.ProgressRequestBody(file, "application/zip") { progress ->
                    AppLogger.d("Upload progress: $progress%")
                    onProgressUpdate(progress, false, null)
                }
                val filePart = MultipartBody.Part.createFormData("zipFile", file.name, progressRequestBody)

                AppLogger.d("Sending upload request...")

                val response = CMPApiClient.instance.uploadZip(filePart)

                AppLogger.d(response.toString())
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    return@withContext if (responseBody != null) {
                        AppLogger.d("Upload successful: ${file.name}")
                        AppLogger.d("Response Code: ${response.code()}")
                        AppLogger.d("Response Headers: ${response.headers()}")
                        AppLogger.d("Response Body: ${responseBody}")

                        onProgressUpdate(100, true, null)
                        Result.success(responseBody)
                    } else {
                        val errorMsg = "Upload successful but response body is null"
                        AppLogger.d(errorMsg)
                        onProgressUpdate(100, false, errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = "Upload failed - Code: ${response.code()}, Error: $errorBody"

                    AppLogger.d("Upload failed: $errorMsg")
                    AppLogger.d("Response Headers: ${response.headers()}")

                    onProgressUpdate(100, false, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error uploading file: ${e.message}"
            onProgressUpdate(100, false, errorMsg)
            Result.failure(Exception(errorMsg))
        }
    }





}