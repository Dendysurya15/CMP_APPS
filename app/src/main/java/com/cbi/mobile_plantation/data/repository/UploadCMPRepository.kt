package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.PhotoResult
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadResults
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val panenDao = database.panenDao()


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
        data: String,
        type: String,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> {
        return try {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    onProgressUpdate(0, false, null)
                }

                AppLogger.d("====== UPLOAD START ======")
                AppLogger.d("Starting upload for type: $type")

                if (type == "image") {
                    try {
                        // Parse the data as JSON array of image objects
                        AppLogger.d("====== PARSING IMAGE DATA ======")
                        val imageList = Gson().fromJson(data, object : TypeToken<List<Map<String, Any>>>() {}.type) as List<Map<String, String>>

                        if (imageList.isEmpty()) {
                            val errorMsg = "No images to upload"
                            AppLogger.e(errorMsg)
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }
                            return@withContext Result.failure(Exception(errorMsg))
                        }

                        AppLogger.d("Total images to upload: ${imageList.size}")

                        // Calculate progress steps
                        val progressPerImage = 90 / imageList.size // Reserve 10% for initial setup
                        var currentProgress = 10  // Start at 10%

//                        withContext(Dispatchers.Main) {
//                            onProgressUpdate(currentProgress, false, "Found ${imageList.size} images to process")
//                        }

                        // Validate all files exist first
                        val validImageFiles = mutableListOf<Triple<File, String, String>>()
                        val failedFiles = mutableListOf<String>()

                        AppLogger.d("====== CHECKING FILE EXISTENCE ======")

                        imageList.forEachIndexed { index, imageData ->
                            val imagePath = imageData["path"] ?: ""
                            val imageName = imageData["name"] ?: ""
                            val tableId = imageData["table_ids"]?.toIntOrNull() ?: -1

                            AppLogger.d("Checking file ${index + 1}/${imageList.size}: $imageName - Table ID: $tableId")

                            val file = File(imagePath)
                            if (file.exists()) {
                                validImageFiles.add(Triple(file, imageName, tableId.toString()))
                                AppLogger.d("✓ File exists: $imageName")
                            } else {
                                failedFiles.add(imageName)
                                AppLogger.e("✗ File not found: $imageName")

                                // Update status to error for file not found
                                if (tableId != -1) {
                                    val errorJson = JsonObject().apply {
                                        add("error", JsonArray().apply {
                                            add(imageName)
                                        })
                                    }.toString()

                                    panenDao.updateStatusUploadedImage(listOf(tableId), errorJson)
                                    AppLogger.d("Updated status_uploaded_image for table ID $tableId with error: $errorJson")
                                }
                            }
                        }

                        // Log the test results
                        AppLogger.d("====== FILE EXISTENCE TEST RESULTS ======")
                        AppLogger.d("Total files attempted: ${imageList.size}")
                        AppLogger.d("Valid files found: ${validImageFiles.size}")
                        AppLogger.d("Failed files: ${failedFiles.size}")
                        AppLogger.d("Failed file names: $failedFiles")

                        if (validImageFiles.isEmpty()) {
                            val errorMsg = "No valid images found to upload"
                            AppLogger.e(errorMsg)
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }
                            return@withContext Result.failure(Exception(errorMsg))
                        }

                        // Start uploading images one by one
                        var successCount = 0
                        var failureCount = failedFiles.size // Count files that weren't found
                        val uploadResults = mutableListOf<PhotoResult>()
                        val failedImagePaths = mutableListOf<String>()
                        val failedImageNames = mutableListOf<String>()

                        validImageFiles.forEachIndexed { index, (file, imageName, tableId) ->
                            AppLogger.d("====== UPLOADING IMAGE ${index + 1}/${validImageFiles.size} ======")
                            AppLogger.d("Image name: $imageName")
                            AppLogger.d("Image path: ${file.absolutePath}")
                            AppLogger.d("File size: ${file.length()} bytes")
                            AppLogger.d("Table ID: $tableId")

//                            withContext(Dispatchers.Main) {
//                                onProgressUpdate(currentProgress, false, "Uploading ${imageName} (${index + 1}/${validImageFiles.size})")
//                            }

                            try {
                                // TEST: Force error for second image (PANEN TPH_2_2025505_143611.jpg)
                                if (imageName.contains("_2_") && imageName.contains("143611")) {
                                    AppLogger.d("TEST ERROR: Forcing error for second image...")
                                    throw Exception("Simulated error for testing - second image failed to upload")
                                }

                                // Create the multipart data for single image
                                val photoRequestBody = RequestBody.create(
                                    "image/*".toMediaTypeOrNull(),
                                    file
                                )
                                val photoPart = MultipartBody.Part.createFormData("photos", imageName, photoRequestBody)

                                val datasetTypeRequestBody = RequestBody.create(
                                    "text/plain".toMediaTypeOrNull(),
                                    "panen_table"
                                )

                                // Make the API call for single image
                                val response = TestingAPIClient.instance.uploadPhotos(
                                    photos = listOf(photoPart),  // Send as single-item list
                                    datasetType = datasetTypeRequestBody
                                )

                                AppLogger.d("====== RESPONSE FOR IMAGE ${index + 1} ======")
                                AppLogger.d("Response successful: ${response.isSuccessful}")
                                AppLogger.d("Response code: ${response.code()}")

                                val tableIdInt = tableId.toIntOrNull() ?: -1

                                if (response.isSuccessful) {
                                    val responseBody = response.body()
                                    if (responseBody != null) {
                                        AppLogger.d("Success uploading: $imageName")
                                        AppLogger.d("Response: ${responseBody.message}")

                                        // Add result to our list
                                        responseBody.data.results.forEach { result ->
                                            uploadResults.add(result)
                                        }

                                        successCount += responseBody.data.successful
                                        failureCount += responseBody.data.failed

                                        // Update status to 200 (success)
                                        if (tableIdInt != -1) {
                                            panenDao.updateStatusUploadedImage(listOf(tableIdInt), "200")
                                            AppLogger.d("Updated status_uploaded_image for table ID $tableIdInt to 200")
                                        }

                                        currentProgress += progressPerImage
//                                        withContext(Dispatchers.Main) {
//                                            onProgressUpdate(currentProgress, false, "✓ ${imageName} uploaded successfully")
//                                        }
                                    } else {
                                        AppLogger.e("Null response body for: $imageName")
                                        failureCount++

                                        // Update status with error
                                        if (tableIdInt != -1) {
                                            val errorJson = JsonObject().apply {
                                                add("error", JsonArray().apply {
                                                    add(imageName)
                                                })
                                            }.toString()
                                            panenDao.updateStatusUploadedImage(listOf(tableIdInt), errorJson)
                                            AppLogger.d("Updated status_uploaded_image for table ID $tableIdInt with error: $errorJson")
                                        }

                                        currentProgress += progressPerImage
//                                        withContext(Dispatchers.Main) {
//                                            onProgressUpdate(currentProgress, false, "✗ ${imageName} failed - null response")
//                                        }
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    AppLogger.e("Failed to upload: $imageName - Code: ${response.code()}, Error: $errorBody")
                                    failureCount++

                                    // Update status with error code
                                    if (tableIdInt != -1) {
                                        val errorJson = JsonObject().apply {
                                            add("error", JsonArray().apply {
                                                add(imageName)
                                            })
                                        }.toString()
                                        panenDao.updateStatusUploadedImage(listOf(tableIdInt), errorJson)
                                        AppLogger.d("Updated status_uploaded_image for table ID $tableIdInt with error: $errorJson")
                                    }

                                    currentProgress += progressPerImage
                                    withContext(Dispatchers.Main) {
                                        onProgressUpdate(currentProgress, false, "✗ ${imageName} failed - ${response.code()}")
                                    }

                                    failedImagePaths.add(file.absolutePath)
                                    failedImageNames.add(imageName)
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Exception uploading $imageName: ${e.message}")
                                failureCount++

                                // Update status with exception
                                val tableIdInt = tableId.toIntOrNull() ?: -1
                                if (tableIdInt != -1) {
                                    val errorJson = JsonObject().apply {
                                        add("error", JsonArray().apply {
                                            add(imageName)
                                        })
                                    }.toString()
                                    panenDao.updateStatusUploadedImage(listOf(tableIdInt), errorJson)
                                    AppLogger.d("Updated status_uploaded_image for table ID $tableIdInt with error: $errorJson")
                                }

                                currentProgress += progressPerImage
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(currentProgress, false, "✗ ${imageName} error - ${e.message}")
                                }
                                failedImagePaths.add(file.absolutePath)
                                failedImageNames.add(imageName)
                            }
                        }

                        // Final update with complete status - THIS SHOULD BE CALLED ONCE AFTER THE LOOP
                        val isSuccess = failureCount == 0
                        val finalMessage = if (isSuccess) {
                            "All $successCount images uploaded successfully"
                        } else {
                            "Upload completed: $successCount successful, $failureCount failed (${failureCount - failedFiles.size} upload errors, ${failedFiles.size} files not found)"
                        }

                        AppLogger.d("====== COMPLETE UPLOAD RESULTS ======")
                        AppLogger.d("Summary: $finalMessage")
                        AppLogger.d("Uploaded files: ${uploadResults.size}")
                        AppLogger.d("isSuccess: $isSuccess")

// This should be called only once
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, isSuccess, if (isSuccess) null else finalMessage)
                        }

                        val uploadV3Response = UploadV3Response(
                            success = isSuccess,
                            trackingId = 0,
                            message = finalMessage,
                            status = 0,
                            tanggal_upload = "",
                            nama_file = "",
                            results = null,
                            type = "image",
                            imageFullPath = failedImagePaths,
                            imageName = failedImageNames
                        )

                        Result.success(uploadV3Response)


                    } catch (e: Exception) {
                        val errorMsg = "Error processing images: ${e.message}"
                        AppLogger.e(errorMsg)
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, false, errorMsg)
                        }
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    // Original JSON upload logic for non-image types
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
                                AppLogger.d("Processed: ${responseBody.results!!.processed}")
                                AppLogger.d("Created: ${responseBody.results!!.created}")
                                AppLogger.d("Updated: ${responseBody.results!!.updated}")
                                AppLogger.d("Errors: ${responseBody.results!!.errors}")
                                AppLogger.d("Skipped: ${responseBody.results!!.skipped}")

                                // Mark as success with 100% progress
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(100, true, null)
                                }

                                val jsonResponse = UploadV3Response(
                                    success = responseBody.success,
                                    trackingId = responseBody.trackingId,
                                    message = responseBody.message,
                                    status = responseBody.status,
                                    tanggal_upload = responseBody.tanggal_upload,
                                    nama_file = responseBody.nama_file,
                                    results = responseBody.results,
                                    type = "json"
                                )

                                Result.success(jsonResponse)
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