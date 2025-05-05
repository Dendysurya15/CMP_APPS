package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
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
    ): Result<UploadWBCMPResponse> {
        return try {
            withContext(Dispatchers.IO) {


                val file = File(fileZipPath)

                onProgressUpdate(0, false, null)
//
//test failure sengaja
//                AppLogger.d("askljdlfkjasdf")
//                if (partNumber == 2 || partNumber == 4) {
//                    val errorMsg = "Simulated failure for part $partNumber"
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
//                    val response = CMPApiClient.instance.uploadZipV2(filePart, uuidPart, partPart, totalPart)
                    val response = CMPApiClient.instance.uploadZip(filePart)

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

    suspend fun uploadJsonToServerV3(
        jsonFilePath: String,
        filename: String,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> {
        return try {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    onProgressUpdate(0, false, null)
                }

                AppLogger.d("====== UPLOAD START ======")
                AppLogger.d("Starting JSON file upload for file: $jsonFilePath")

                try {
                    // Check if file exists
                    val file = File(jsonFilePath)
                    if (!file.exists()) {
                        val errorMsg = "JSON file not found: $jsonFilePath"
                        AppLogger.e(errorMsg)
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, false, errorMsg)
                        }
                        return@withContext Result.failure(Exception(errorMsg))
                    }

                    // Create the file part
                    val fileRequestBody = RequestBody.create(
                        "application/json".toMediaTypeOrNull(),
                        file
                    )
                    val filePart = MultipartBody.Part.createFormData("jsonFile", filename, fileRequestBody)

                    // Create the filename part
                    val filenameRequestBody = RequestBody.create(
                        "text/plain".toMediaTypeOrNull(),
                        filename
                    )

                    AppLogger.d("====== REQUEST DATA ======")
                    AppLogger.d("Filename: $filename")
                    AppLogger.d("JSON File Path: $jsonFilePath")
                    AppLogger.d("File size: ${file.length()} bytes")

                    // Simulate progress
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(25, false, null)
                    }

                    // Make the API call
                    AppLogger.d("====== MAKING API CALL ======")
                    AppLogger.d("Using MultipartBody.Part for jsonFile")

                    val response = TestingAPIClient.instance.uploadJsonV3(
                        jsonFile = filePart,
                        filename = filenameRequestBody
                    )

                    AppLogger.d("====== RAW RESPONSE ======")
                    AppLogger.d("Response successful: ${response.isSuccessful}")
                    AppLogger.d("Response code: ${response.code()}")
                    AppLogger.d("Response message: ${response.message()}")

                    val rawBody = response.body()
                    AppLogger.d("Raw response body: $rawBody")

                    val rawErrorBody = response.errorBody()?.string()
                    AppLogger.d("Raw error body: $rawErrorBody")

                    // Log all headers
                    AppLogger.d("====== RESPONSE HEADERS ======")
                    for (header in response.headers()) {
                        AppLogger.d("Header - ${header.first}: ${header.second}")
                    }

                    // Log network details
                    AppLogger.d("====== NETWORK DETAILS ======")
                    AppLogger.d("Request URL: ${response.raw().request.url}")
                    AppLogger.d("Request Method: ${response.raw().request.method}")

                    // Simulate more progress
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(75, false, null)
                    }

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            AppLogger.d("====== SUCCESS DETAILS ======")
                            AppLogger.d("Upload successful for: $filename")
                            AppLogger.d("Tracking ID: ${responseBody.trackingId}")
                            AppLogger.d("Message: ${responseBody.message}")
                            AppLogger.d("Status: ${responseBody.status}")
                            AppLogger.d("Upload Date: ${responseBody.tanggal_upload}")
                            AppLogger.d("File Name: ${responseBody.nama_file}")

                            // Log results details
                            AppLogger.d("====== RESULTS DETAILS ======")
                            AppLogger.d("Processed: ${responseBody.results.processed}")
                            AppLogger.d("Created: ${responseBody.results.created}")
                            AppLogger.d("Updated: ${responseBody.results.updated}")
                            AppLogger.d("Errors: ${responseBody.results.errors}")
                            AppLogger.d("Skipped: ${responseBody.results.skipped}")

                            // Mark as success with 100% progress
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, true, null)
                            }
                            Result.success(responseBody)
                        } else {
                            val errorMsg = "Upload successful but response body is null"
                            AppLogger.d("====== ERROR ======")
                            AppLogger.d(errorMsg)
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }
                            Result.failure(Exception(errorMsg))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = "Upload failed - Code: ${response.code()}, Error: $errorBody"

                        AppLogger.d("====== ERROR DETAILS ======")
                        AppLogger.d("Upload failed for: $filename")
                        AppLogger.d("Response Code: ${response.code()}")
                        AppLogger.d("Response Message: ${response.message()}")
                        AppLogger.d("Error Body: $errorBody")

                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, false, errorMsg)
                        }
                        Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    // Handle network errors
                    val errorMsg = "Network error: ${e.message}"
                    AppLogger.d("====== EXCEPTION ======")
                    AppLogger.d(errorMsg)
                    AppLogger.d("Exception type: ${e.javaClass.simpleName}")
                    AppLogger.d("Stack trace: ${e.stackTraceToString()}")

                    withContext(Dispatchers.Main) {
                        onProgressUpdate(100, false, errorMsg)
                    }
                    Result.failure(Exception(errorMsg))
                } finally {
                    AppLogger.d("====== UPLOAD END ======")
                }
            }
        } catch (e: Exception) {
            // This outer catch should only be hit for errors in the withContext setup
            val errorMsg = "Error preparing upload: ${e.message}"
            AppLogger.d("====== FATAL EXCEPTION ======")
            AppLogger.d(errorMsg)
            AppLogger.d("Exception type: ${e.javaClass.simpleName}")
            AppLogger.d("Stack trace: ${e.stackTraceToString()}")

            // Make sure we're on the main thread for this callback too
            withContext(Dispatchers.Main) {
                onProgressUpdate(100, false, errorMsg)
            }
            Result.failure(Exception(errorMsg))
        }
    }

        suspend fun uploadZipToServer(
            fileZipPath: String,
            onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
        ): Result<UploadWBCMPResponse> {
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

                    AppLogger.d("lkasjdflkjasdklfasdf")

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