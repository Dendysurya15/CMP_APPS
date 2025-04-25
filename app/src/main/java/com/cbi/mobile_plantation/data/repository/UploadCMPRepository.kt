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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream


sealed class SaveResultNewUploadDataCMP {
    object Success : SaveResultNewUploadDataCMP()
    data class Error(val exception: Exception) : SaveResultNewUploadDataCMP()
}

class UploadCMPRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val uploadCMPDao = database.uploadCMPDao()


    suspend fun UpdateOrInsertDataUpload(data: UploadCMPModel) {
        val existingCount = uploadCMPDao.getTrackingIdCount(data.tracking_id!!,data.nama_file!!)

        if (existingCount > 0) {
            uploadCMPDao.updateStatus(data.tracking_id,data.nama_file!!,  data.status!!)
        } else {
            uploadCMPDao.insertNewData(data)
        }
    }

    suspend fun getAllData(): List<UploadCMPModel> {
        return uploadCMPDao.getAllData() // Calls the DAO function
    }
    class ProgressRequestBody(
        private val file: File,
        private val contentType: String,
        private val onProgressUpdate: (progress: Int, bytesUploaded: Long, totalBytes: Long, done: Boolean) -> Unit
    ) : RequestBody() {

        override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = file.length()
            var bytesWritten = 0L

            try {
                file.inputStream().use { inputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        // Update before writing in case writing takes time or fails
                        bytesWritten += read
                        val progress = if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0

                        // Report progress
                        onProgressUpdate(progress, bytesWritten, totalBytes, false)

                        // Write to sink
                        sink.write(buffer, 0, read)
                    }
                }
                // Report completion
                onProgressUpdate(100, totalBytes, totalBytes, true)

            } catch (e: Exception) {
                // Report error but still send the accurate bytes count
                AppLogger.e("Error during file upload: ${e.message}")
                onProgressUpdate(
                    if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0,
                    bytesWritten,
                    totalBytes,
                    true
                )
                throw e
            }
        }

        companion object {
            private const val BUFFER_SIZE = 8192 // 8 KB buffer for efficiency
        }
    }

    suspend fun uploadZipToServerV2(
        fileZipPath: String,
        batchUuid: String,
        partNumber: Int,
        totalParts: Int,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadCMPResponse> {
        return try {
            withContext(Dispatchers.IO) {


                val file = File(fileZipPath)

                onProgressUpdate(0, false, null)
//
//test failure sengaja
//                if (partNumber == 5) {
//                    val errorMsg = "Simulated failure for part 5"
//                    AppLogger.d(errorMsg)
//                    onProgressUpdate(100, false, errorMsg)
//                    return@withContext Result.failure(Exception(errorMsg))
//                }
                // Check if file exists
                if (!file.exists()) {
                    val errorMsg = "File does not exist: $fileZipPath"
                    AppLogger.d(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Check if file is readable
                if (!file.canRead()) {
                    val errorMsg = "File exists but is not readable: $fileZipPath"
                    AppLogger.d(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Check if file has valid size
                if (file.length() <= 0) {
                    val errorMsg = "File exists but is empty (0 bytes): $fileZipPath"
                    AppLogger.d(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }


                // Check if file has valid ZIP signature (optional, more thorough validation)
                try {
                    val inputStream = FileInputStream(file)
                    val signature = ByteArray(4)
                    val bytesRead = inputStream.read(signature)
                    inputStream.close()

                    if (bytesRead != 4 ||
                        signature[0] != 0x50.toByte() || // 'P'
                        signature[1] != 0x4B.toByte() || // 'K'
                        signature[2] != 0x03.toByte() ||
                        signature[3] != 0x04.toByte()) {
                        val errorMsg = "File exists but does not appear to be a valid ZIP file: $fileZipPath"
                        AppLogger.d(errorMsg)
                        onProgressUpdate(100, false, errorMsg)
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error validating ZIP file signature: ${e.message}"
                    AppLogger.d(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val fileSize = file.length()
                AppLogger.d("Starting file upload: ${file.name}, Size: ${AppUtils.formatFileSize(fileSize)}")
                val progressRequestBody = ProgressRequestBody(file, "application/zip") { progress, bytesUploaded, totalBytes, done ->
                    AppLogger.d("Upload progress: $progress% (${AppUtils.formatFileSize(bytesUploaded)}/${AppUtils.formatFileSize(totalBytes)})")

                    // Only update progress during active upload
                    if (!done) {
                        onProgressUpdate(progress, false, null)
                    }
                }

                // Create the parts for the multipart request
                val filePart = MultipartBody.Part.createFormData("zipFile", file.name, progressRequestBody)

                // Create RequestBody objects for the new parameters
                val uuidPart = RequestBody.create("text/plain".toMediaTypeOrNull(), batchUuid)
                val partPart = RequestBody.create("text/plain".toMediaTypeOrNull(), partNumber.toString())
                val totalPart = RequestBody.create("text/plain".toMediaTypeOrNull(), totalParts.toString())

                AppLogger.d("Sending upload request with UUID: $batchUuid, Part: $partNumber, Total: $totalParts")

                try {
                    val response = CMPApiClient.instance.uploadZipV2(filePart, uuidPart, partPart, totalPart)

                    AppLogger.d("response $response")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            AppLogger.d("Upload successful: ${file.name}")
                            AppLogger.d("Response Code: ${response.code()}")
                            AppLogger.d("Response Headers: ${response.headers()}")
                            AppLogger.d("Response Body: ${responseBody}")

                            // Mark as success with 100% progress
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
                        AppLogger.d("Response Code: ${response.code()}")
                        AppLogger.d("Response Message: ${response.message()}")
                        AppLogger.d("Response Headers: ${response.headers()}")
                        AppLogger.d("Response Body: $errorBody")

                        onProgressUpdate(100, false, errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    // Handle network errors consistently for all files
                    val errorMsg = "Network error: ${e.message}"
                    AppLogger.d(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    Result.failure(Exception(errorMsg))  // Return failure directly, don't rethrow
                }
            }
        } catch (e: Exception) {
            // This outer catch should now only be hit for errors in the withContext setup
            // or other unexpected exceptions, not for network errors
            val errorMsg = "Error preparing upload: ${e.message}"
            AppLogger.d(errorMsg)
            onProgressUpdate(100, false, errorMsg)
            Result.failure(Exception(errorMsg))
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

                    val fileSize = file.length()
                    AppLogger.d("Starting file upload: ${file.name}, Size: ${AppUtils.formatFileSize(fileSize)}")
                    val progressRequestBody = ProgressRequestBody(file, "application/zip") { progress, bytesUploaded, totalBytes, done ->
                        AppLogger.d("Upload progress: $progress% (${AppUtils.formatFileSize(bytesUploaded)}/${AppUtils.formatFileSize(totalBytes)})")

                        // Still call the original callback to maintain compatibility
                        onProgressUpdate(progress, false, null)

                        // Additional tracking can be done here, but we're preserving the original callback signature
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