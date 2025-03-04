package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.api.ApiService
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.UploadCMPModel
import com.cbi.cmp_project.data.model.uploadCMP.UploadCMPResponse
import com.cbi.cmp_project.data.network.CMPApiClient
import com.cbi.cmp_project.data.network.Constants
import com.cbi.cmp_project.data.network.StagingApiClient
import com.cbi.cmp_project.data.network.TestingAPIClient
import com.cbi.cmp_project.data.repository.WeighBridgeRepository.UploadError
import com.cbi.cmp_project.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale


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

    class ProgressRequestBody(
        private val file: File,
        private val contentTypeString: String,
        private val callback: (progress: Int) -> Unit
    ) : RequestBody() {

        override fun contentType(): MediaType? = contentTypeString.toMediaTypeOrNull()

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val length = contentLength()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val fileInputStream = FileInputStream(file)
            var uploaded: Long = 0
            fileInputStream.use { inputStream ->
                var read = inputStream.read(buffer)
                while (read != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val progress = (uploaded * 100 / length).toInt()
                    callback(progress) // report progress
                    read = inputStream.read(buffer)
                }
            }
        }

        companion object {
            private const val DEFAULT_BUFFER_SIZE = 2048
        }
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

                val progressRequestBody = ProgressRequestBody(file, "application/zip") { progress ->
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