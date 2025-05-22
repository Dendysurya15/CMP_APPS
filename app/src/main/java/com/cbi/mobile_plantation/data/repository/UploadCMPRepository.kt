package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.PhotoResult
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadResults
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.StagingApiClient
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
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.IOException


sealed class SaveResultNewUploadDataCMP {
    object Success : SaveResultNewUploadDataCMP()
    data class Error(val exception: Exception) : SaveResultNewUploadDataCMP()
}

class UploadCMPRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val uploadCMPDao = database.uploadCMPDao()
    private val panenDao = database.panenDao()
    private val absensiDao = database.absensiDao()
    private val espbDao = database.espbDao()


    suspend fun UpdateOrInsertDataUpload(data: UploadCMPModel) {
        val existingCount = uploadCMPDao.getTrackingIdCount(data.tracking_id!!, data.nama_file!!)

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
                        val progress =
                            if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0

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
                        signature[3] != 0x04.toByte()
                    ) {
                        val errorMsg =
                            "File exists but does not appear to be a valid ZIP file: $fileZipPath"
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
                AppLogger.d(
                    "Starting file upload: ${file.name}, Size: ${
                        AppUtils.formatFileSize(
                            fileSize
                        )
                    }"
                )
                val progressRequestBody = ProgressRequestBody(
                    file,
                    "application/zip"
                ) { progress, bytesUploaded, totalBytes, done ->
                    AppLogger.d(
                        "Upload progress: $progress% (${
                            AppUtils.formatFileSize(
                                bytesUploaded
                            )
                        }/${AppUtils.formatFileSize(totalBytes)})"
                    )

                    // Only update progress during active upload
                    if (!done) {
                        onProgressUpdate(progress, false, null)
                    }
                }

                // Create the parts for the multipart request
                val filePart =
                    MultipartBody.Part.createFormData("zipFile", file.name, progressRequestBody)

                // Create RequestBody objects for the new parameters
                val uuidPart = RequestBody.create("text/plain".toMediaTypeOrNull(), batchUuid)
                val partPart =
                    RequestBody.create("text/plain".toMediaTypeOrNull(), partNumber.toString())
                val totalPart =
                    RequestBody.create("text/plain".toMediaTypeOrNull(), totalParts.toString())

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

    data class ImageFileInfo(
        val file: File,
        val imageName: String,
        val tableId: String,
        val basePath: String,
        val databaseTable: String
    )

    suspend fun uploadJsonToServerV3(
        jsonFilePath: String,
        filename: String,
        data: String,
        type: String,
        tableIds: String? = null,
        databaseTable: String,
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
                        val imageList = Gson().fromJson(
                            data,
                            object : TypeToken<List<Map<String, Any>>>() {}.type
                        ) as List<Map<String, String>>

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
                        val validImageFiles = mutableListOf<ImageFileInfo>()
                        val failedFiles = mutableListOf<String>()

                        AppLogger.d("====== CHECKING FILE EXISTENCE ======")

                        imageList.forEachIndexed { index, imageData ->
                            val imagePath = imageData["path"] ?: ""
                            val imageName = imageData["name"] ?: ""
                            val tableId = imageData["table_ids"]?.toIntOrNull() ?: -1
                            val basePathImage = imageData["base_path"]
                            val databaseTable = imageData["database"]
                            AppLogger.d("Checking file ${index + 1}/${imageList.size}: $imageName - Table ID: $tableId")

                            val file = File(imagePath)
                            if (file.exists()) {
                                validImageFiles.add(
                                    ImageFileInfo(
                                        file, imageName, tableId.toString(),
                                        basePathImage!!, databaseTable!!
                                    )
                                )
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

                                    when (databaseTable) {
                                        AppUtils.DatabaseTables.PANEN -> {
                                            panenDao.updateStatusUploadedImage(
                                                listOf(tableId),
                                                errorJson
                                            )
                                        }

                                        AppUtils.DatabaseTables.ABSENSI -> {
                                            absensiDao.updateStatusUploadedImage(
                                                listOf(tableId),
                                                errorJson
                                            )
                                        }
                                    }
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

                        validImageFiles.forEachIndexed { index, fileInfo ->
                            AppLogger.d("====== UPLOADING IMAGE ${index + 1}/${validImageFiles.size} ======")
                            AppLogger.d("Image name: ${fileInfo.imageName}")
                            AppLogger.d("Image path: ${fileInfo.file.absolutePath}")
                            AppLogger.d("File size: ${fileInfo.file.length()} bytes")
                            AppLogger.d("Table ID: ${fileInfo.tableId}")

//                            withContext(Dispatchers.Main) {
//                                onProgressUpdate(currentProgress, false, "Uploading ${imageName} (${index + 1}/${validImageFiles.size})")
//                            }

                            try {


                                // Create the multipart data for single image
                                val photoRequestBody = RequestBody.create(
                                    "image/*".toMediaTypeOrNull(),
                                    fileInfo.file
                                )
                                val photoPart = MultipartBody.Part.createFormData(
                                    "photos",
                                    fileInfo.imageName,
                                    photoRequestBody
                                )

                                val datasetType = when (fileInfo.databaseTable) {
                                    AppUtils.DatabaseTables.ABSENSI -> AppUtils.DatabaseTables.ABSENSI
                                    else -> AppUtils.DatabaseTables.PANEN
                                }

                                // Create RequestBody once
                                val datasetTypeRequestBody = RequestBody.create(
                                    "text/plain".toMediaTypeOrNull(),
                                    datasetType
                                )


                                val basePathRequestBody = RequestBody.create(
                                    "text/plain".toMediaTypeOrNull(),
                                    fileInfo.basePath,
                                )

                                // Make the API call for single image
                                val response = CMPApiClient.instance.uploadPhotos(
                                    photos = listOf(photoPart),
                                    datasetType = datasetTypeRequestBody,
                                    path = basePathRequestBody
                                )

                                AppLogger.d("====== RESPONSE FOR IMAGE ${index + 1} ======")
                                AppLogger.d("Response successful: ${response.isSuccessful}")
                                AppLogger.d("Response code: ${response.code()}")

                                val tableIdInt = fileInfo.tableId.toIntOrNull() ?: -1

                                if (response.isSuccessful) {
                                    val responseBody = response.body()
                                    if (responseBody != null) {
                                        AppLogger.d("Success uploading: ${fileInfo.imageName}")
                                        AppLogger.d("Response: ${responseBody.message}")

                                        // Add result to our list
                                        responseBody.data.results.forEach { result ->
                                            uploadResults.add(result)
                                        }

                                        successCount += responseBody.data.successful
                                        failureCount += responseBody.data.failed

                                        // Update status to 200 (success)
                                        if (tableIdInt != -1) {
                                            when (fileInfo.databaseTable) {
                                                AppUtils.DatabaseTables.PANEN -> {
                                                    panenDao.updateStatusUploadedImage(
                                                        listOf(
                                                            tableIdInt
                                                        ), "200"
                                                    )
                                                }

                                                AppUtils.DatabaseTables.ABSENSI -> {
                                                    absensiDao.updateStatusUploadedImage(
                                                        listOf(
                                                            tableIdInt
                                                        ), "200"
                                                    )
                                                }
                                            }
//                                            AppLogger.d("Updated status_uploaded_image for table ID $tableIdInt to 200")
                                        }

                                        currentProgress += progressPerImage
//                                        withContext(Dispatchers.Main) {
//                                            onProgressUpdate(currentProgress, false, "✓ ${imageName} uploaded successfully")
//                                        }
                                    } else {
                                        AppLogger.e("Null response body for: ${fileInfo.imageName}")
                                        failureCount++

                                        // Update status with error
                                        if (tableIdInt != -1) {
                                            val errorJson = JsonObject().apply {
                                                add("error", JsonArray().apply {
                                                    add(fileInfo.imageName)
                                                })
                                            }.toString()
                                            when (fileInfo.databaseTable) {
                                                AppUtils.DatabaseTables.PANEN -> {
                                                    panenDao.updateStatusUploadedImage(
                                                        listOf(
                                                            tableIdInt
                                                        ), errorJson
                                                    )
                                                }

                                                AppUtils.DatabaseTables.ABSENSI -> {
                                                    absensiDao.updateStatusUploadedImage(
                                                        listOf(
                                                            tableIdInt
                                                        ), errorJson
                                                    )
                                                }
                                            }
                                        }

                                        currentProgress += progressPerImage
//                                        withContext(Dispatchers.Main) {
//                                            onProgressUpdate(currentProgress, false, "✗ ${imageName} failed - null response")
//                                        }
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    AppLogger.e("Failed to upload: ${fileInfo.imageName} - Code: ${response.code()}, Error: $errorBody")
                                    failureCount++

                                    // Update status with error code
                                    if (tableIdInt != -1) {
                                        val errorJson = JsonObject().apply {
                                            add("error", JsonArray().apply {
                                                add(fileInfo.imageName)
                                            })
                                        }.toString()
                                        when (fileInfo.databaseTable) {
                                            AppUtils.DatabaseTables.PANEN -> {
                                                panenDao.updateStatusUploadedImage(
                                                    listOf(tableIdInt),
                                                    errorJson
                                                )
                                            }

                                            AppUtils.DatabaseTables.ABSENSI -> {
                                                absensiDao.updateStatusUploadedImage(
                                                    listOf(
                                                        tableIdInt
                                                    ), errorJson
                                                )
                                            }
                                            // Add other cases if needed
                                        }
                                    }

                                    currentProgress += progressPerImage
                                    withContext(Dispatchers.Main) {
                                        onProgressUpdate(
                                            currentProgress,
                                            false,
                                            "✗ ${fileInfo.imageName} failed - ${response.code()}"
                                        )
                                    }

                                    failedImagePaths.add(fileInfo.file.absolutePath)
                                    failedImageNames.add(fileInfo.imageName)
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Exception uploading${fileInfo.imageName}: ${e.message}")
                                failureCount++

                                // Update status with exception
                                val tableIdInt = fileInfo.tableId.toIntOrNull() ?: -1
                                if (tableIdInt != -1) {
                                    val errorJson = JsonObject().apply {
                                        add("error", JsonArray().apply {
                                            add(fileInfo.imageName)
                                        })
                                    }.toString()
                                    when (fileInfo.databaseTable) {
                                        AppUtils.DatabaseTables.PANEN -> {
                                            panenDao.updateStatusUploadedImage(
                                                listOf(tableIdInt),
                                                errorJson
                                            )
                                        }

                                        AppUtils.DatabaseTables.ABSENSI -> {
                                            absensiDao.updateStatusUploadedImage(
                                                listOf(tableIdInt),
                                                errorJson
                                            )
                                        }
                                        // Add other cases if needed
                                    }
                                }

                                currentProgress += progressPerImage
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(
                                        currentProgress,
                                        false,
                                        "✗ ${fileInfo.imageName} error - ${e.message}"
                                    )
                                }
                                failedImagePaths.add(fileInfo.file.absolutePath)
                                failedImageNames.add(fileInfo.imageName)
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
                }
                else if (type == AppUtils.DatabaseServer.CMP) {

                    val dataMap = if (data.isNotEmpty()) {
                        try {
                            Gson().fromJson(data, Map::class.java)
                        } catch (e: Exception) {
                            AppLogger.e("Error parsing data JSON: ${e.message}")
                            null
                        }
                    } else {
                        null
                    }

                    // Extract the espb_json field which contains the actual data to upload
                    val espbJson = dataMap?.get("espb_json")?.toString() ?: ""


                    val espbIds = (dataMap?.get("espb_ids") as? List<*>)?.mapNotNull {
                        (it as? Double)?.toInt() ?: (it as? Int)
                    } ?: emptyList()

                    // Extract the additional parameters
                    val uploaderInfo = dataMap?.get("uploader_info")?.toString() ?: ""
                    val uploadedAt = dataMap?.get("uploaded_at")?.toString() ?: ""
                    val uploadedById = when (val id = dataMap?.get("uploaded_by_id")) {
                        is Double -> id.toInt()
                        is Int -> id
                        else -> 0
                    }


                    try {
                        // Check if JSON data is empty
                        if (espbJson.isBlank()) {
                            val errorMsg = "JSON data is empty for $filename"
                            AppLogger.e(errorMsg)
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }
                            return@withContext Result.failure(Exception(errorMsg))
                        }

// Create the request body from the JSON string
                        val jsonRequestBody = RequestBody.create(
                            "application/json".toMediaTypeOrNull(),
                            espbJson
                        )

                        withContext(Dispatchers.Main) {
                            onProgressUpdate(50, false, null)
                        }

                        AppLogger.d("CMP: Making API call to upload JSON file")
                        val response = CMPApiClient.instance.uploadJsonV3Raw(
                            jsonData = jsonRequestBody
                        )

                        val responseBody = response.body()
                        val httpStatusCode = response.code()

                        AppLogger.d("CMP: Response received, HTTP code: $httpStatusCode")

                        if (response.isSuccessful && responseBody != null) {
                            AppLogger.d("CMP: Upload successful, response: $responseBody")

                            // Check if the status is between 1 and 3 (inclusive)
                            val isStatusValid = responseBody.status in 1..3
                            val resultMessage = if (isStatusValid) {
                                "Success Uploading to CMP"
                            } else {
                                "Upload completed but with invalid status: ${responseBody.status}. Message: ${responseBody.message ?: "No message"}"
                            }

                            AppLogger.d("CMP: Status check - isStatusValid: $isStatusValid, status: ${responseBody.status}, message: $resultMessage")

                            // Update local database with response data
                            responseBody.let {
                                // Create table name mapping for database update
                                val jsonResultTableIds = createJsonTableNameMapping(espbIds)

                                // Create upload model for database
                                val uploadData = UploadCMPModel(
                                    tracking_id = it.trackingId.toString(),
                                    nama_file = it.nama_file,
                                    status = it.status,
                                    tanggal_upload = it.tanggal_upload,
                                    table_ids = jsonResultTableIds
                                )

                                // Update or insert into database
                                withContext(Dispatchers.IO) {
                                    val existingCount = uploadCMPDao.getTrackingIdCount(
                                        uploadData.tracking_id!!,
                                        uploadData.nama_file!!
                                    )

                                    if (existingCount > 0) {
                                        uploadCMPDao.updateStatus(
                                            uploadData.tracking_id,
                                            uploadData.status!!
                                        )
                                    } else {
                                        uploadCMPDao.insertNewData(uploadData)
                                    }
                                }

                                delay(100) // Small delay before next operation

                                // Update status for all related ESPB IDs
                                for (id in espbIds) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusCMP(
                                                id,
                                                responseBody.status,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                resultMessage
                                            )
                                        }
                                        AppLogger.d("ESPB table dengan id $id has been updated with status message: $resultMessage")
                                    } catch (e: Exception) {
                                        AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                    }
                                }
                            }

                            // Report progress based on the status check
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(
                                    100,
                                    isStatusValid,
                                    if (!isStatusValid) responseBody.message else null
                                )
                            }

                            return@withContext Result.success(responseBody)
                        } else {
                            // Get error details
                            val errorBodyString = response.errorBody()?.string() ?: "No error body"
                            val errorMsg =
                                "JSON upload failed: HTTP $httpStatusCode - ${response.message()}"
                            AppLogger.e("CMP: Error: $errorMsg")
                            AppLogger.e("CMP: Error response body: $errorBodyString")

                            // Update status for all related ESPB IDs with error
                            for (id in espbIds) {
                                try {
                                    withContext(Dispatchers.IO) {
                                        updateUploadStatusCMP(
                                            id,
                                            responseBody?.status ?: 0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${errorMsg.take(1000)}..."
                                        )
                                    }
                                    AppLogger.d("ESPB table dengan id $id has been updated with error status")
                                } catch (e: Exception) {
                                    AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                }
                            }

                            // Report 100% progress with error
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }

                            // Create error response
                            val errorResponse = UploadV3Response(
                                success = false,
                                trackingId = 0,
                                message = errorMsg,
                                status = responseBody?.status ?: 0,
                                tanggal_upload = "",
                                nama_file = filename,
                                results = null,
                                type = AppUtils.DatabaseServer.CMP,
                                imageFullPath = emptyList(),
                                imageName = emptyList()
                            )

                            return@withContext Result.success(errorResponse)
                        }
                    } catch (e: Exception) {
                        val exceptionType = e.javaClass.simpleName
                        val errorMsg =
                            "CMP upload error: [$exceptionType] ${e.message ?: "Unknown error"}"
                        AppLogger.e(errorMsg)
                        AppLogger.e("Stack trace: ${Log.getStackTraceString(e)}")

                        // Update status for all related ESPB IDs with exception error
                        for (id in espbIds) {
                            try {
                                withContext(Dispatchers.IO) {
                                    updateUploadStatusCMP(
                                        id,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMsg.take(1000)}..."
                                    )
                                }
                                AppLogger.d("ESPB table dengan id $id has been updated with exception error")
                            } catch (e: Exception) {
                                AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                            }
                        }

                        // Report 100% progress with error
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, false, errorMsg)
                        }

                        val errorResponse = UploadV3Response(
                            success = false,
                            trackingId = 0,
                            message = errorMsg,
                            status = 0,
                            tanggal_upload = "",
                            nama_file = filename,
                            results = null,
                            type = AppUtils.DatabaseServer.CMP,
                            imageFullPath = emptyList(),
                            imageName = emptyList()
                        )

                        return@withContext Result.success(errorResponse)
                    }
                }
                else if (type == AppUtils.DatabaseServer.PPRO) {
                    // Handle PPRO upload
                    try {
                        AppLogger.d("PPRO: Processing data payload")

                        // Parse the JSON data
                        val jsonData = Gson().fromJson(data, Map::class.java)
                        AppLogger.d("PPRO: Parsed JSON data")

                        // Extract the item ID for database update
                        val itemId =
                            (jsonData["id"] as? Double)?.toInt() ?: (jsonData["id"] as? Int) ?: 0
                        val ipMill =
                            (jsonData["ip"] as? Double)?.toInt() ?: (jsonData["ip"] as? Int) ?: 0

                        // Extract the uploader info for database update
                        val uploaderInfo = jsonData["uploader_info"]?.toString() ?: ""
                        val uploadedAt = jsonData["uploaded_at"]?.toString() ?: ""
                        val uploadedById = when (val id = jsonData["uploaded_by_id"]) {
                            is Double -> id.toInt()
                            is Int -> id
                            else -> 0
                        }

                        AppLogger.d("PPRO: Item ID: $itemId")
                        AppLogger.d("PPRO: Uploader info: $uploaderInfo")
                        AppLogger.d("PPRO: Uploaded at: $uploadedAt")
                        AppLogger.d("PPRO: Uploaded by ID: $uploadedById")

                        // Safe update of progress on main thread
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(10, false, null)
                        }

                        // Extract data for API call
                        val apiData = try {
                            val result = ApiService.dataUploadEspbKraniTimbangPPRO(
                                dept_ppro = (jsonData["dept_ppro"] ?: "0").toString(),
                                divisi_ppro = (jsonData["divisi_ppro"] ?: "0").toString(),
                                commodity = (jsonData["commodity"] ?: "2").toString(),
                                blok_jjg = (jsonData["blok_jjg"] ?: "").toString(),
                                nopol = (jsonData["nopol"] ?: "").toString(),
                                driver = (jsonData["driver"] ?: "").toString(),
                                pemuat_id = (jsonData["pemuat_id"] ?: "").toString(),
                                transporter_id = (jsonData["transporter_id"] ?: "0").toString(),
                                mill_id = (jsonData["mill_id"] ?: "0").toString(),
                                created_by_id = (jsonData["created_by_id"] ?: "0").toString(),
                                created_at = (jsonData["created_at"] ?: "").toString(),
                                no_espb = (jsonData["no_espb"] ?: "").toString()
                            )
                            AppLogger.d("PPRO: Data prepared successfully")
                            result
                        } catch (e: Exception) {
                            val errorMsg = "Data error: ${e.message}"
                            AppLogger.e("PPRO: Data preparation error - $errorMsg")

                            // Update database with error
                            try {
                                withContext(Dispatchers.IO) {
                                    updateUploadStatusPPRO(
                                        itemId,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMsg.take(1000)}..."
                                    )
                                }
                                AppLogger.d("PPRO: Item ID $itemId has been updated with data preparation error")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                            }

                            // Safe update of progress on main thread
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }

                            val errorResponse = UploadV3Response(
                                success = false,
                                trackingId = 0,
                                message = errorMsg,
                                status = 0,
                                tanggal_upload = "",
                                nama_file = filename,
                                results = null,
                                type = AppUtils.DatabaseServer.PPRO,
                                imageFullPath = emptyList(),
                                imageName = emptyList()
                            )

                            return@withContext Result.success(errorResponse)
                        }

                        AppLogger.d("PPRO: Data prepared: $apiData")

                        // Safe update of progress on main thread
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(50, false, null)
                        }

                        try {
                            AppLogger.d("PPRO: Making API call to StagingApiClient.insertESPBKraniTimbangPPRO")
                            StagingApiClient.updateBaseUrl("http://$ipMill:3000")

                            val response =
                                StagingApiClient.instance.insertESPBKraniTimbangPPRO(apiData)
                            AppLogger.d("PPRO: API call completed, isSuccessful=${response.isSuccessful}, code=${response.code()}")

                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                AppLogger.d("PPRO: Response body received, status=${responseBody?.status}")

                                if (responseBody != null && responseBody.status == 1) {
                                    AppLogger.d("PPRO: Upload successful")

                                    // Update database with success
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusPPRO(
                                                itemId,
                                                1,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "Success Uploading to PPRO"
                                            )
                                        }
                                        AppLogger.d("PPRO: Item ID $itemId has been updated with success status")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                    }

                                    // Safe update of progress on main thread
                                    withContext(Dispatchers.Main) {
                                        onProgressUpdate(100, true, null)
                                    }

                                    val successResponse = UploadV3Response(
                                        success = true,
                                        trackingId = 0,
                                        message = "Upload successful",
                                        status = responseBody.status,
                                        tanggal_upload = "",
                                        nama_file = filename,
                                        results = null,
                                        type = AppUtils.DatabaseServer.PPRO,
                                        imageFullPath = emptyList(),
                                        imageName = emptyList()
                                    )

                                    return@withContext Result.success(successResponse)
                                } else {
                                    // Extract error message
                                    val rawErrorMessage =
                                        responseBody?.message?.toString() ?: "No message provided"
                                    val extractedMessage =
                                        if (rawErrorMessage.contains("message=")) {
                                            try {
                                                // Extract the actual error message
                                                val startIndex =
                                                    rawErrorMessage.indexOf("message=") + "message=".length
                                                val endIndex =
                                                    rawErrorMessage.indexOf(",", startIndex)
                                                        .takeIf { it > 0 }
                                                        ?: rawErrorMessage.indexOf(".", startIndex)
                                                            .takeIf { it > 0 }
                                                        ?: rawErrorMessage.length

                                                rawErrorMessage.substring(startIndex, endIndex)
                                                    .trim()
                                            } catch (e: Exception) {
                                                "API Error: ${rawErrorMessage.take(100)}"
                                            }
                                        } else {
                                            "API Error: ${rawErrorMessage.take(100)}"
                                        }

                                    AppLogger.e("PPRO: API Error - $extractedMessage")

                                    // Update database with error
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusPPRO(
                                                itemId,
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "${extractedMessage.take(1000)}..."
                                            )
                                        }
                                        AppLogger.d("PPRO: Item ID $itemId has been updated with API error")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                    }

                                    // Safe update of progress on main thread
                                    withContext(Dispatchers.Main) {
                                        onProgressUpdate(100, false, extractedMessage)
                                    }

                                    val errorResponse = UploadV3Response(
                                        success = false,
                                        trackingId = 0,
                                        message = extractedMessage,
                                        status = 0,
                                        tanggal_upload = "",
                                        nama_file = filename,
                                        results = null,
                                        type = AppUtils.DatabaseServer.PPRO,
                                        imageFullPath = emptyList(),
                                        imageName = emptyList()
                                    )

                                    return@withContext Result.success(errorResponse)
                                }
                            } else {
                                val errorMessage = response.errorBody()?.string()
                                    ?: "Server error: ${response.code()}"
                                AppLogger.e("PPRO: Server Error - $errorMessage")

                                // Update database with error
                                try {
                                    withContext(Dispatchers.IO) {
                                        updateUploadStatusPPRO(
                                            itemId,
                                            0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${errorMessage.take(1000)}..."
                                        )
                                    }
                                    AppLogger.d("PPRO: Item ID $itemId has been updated with server error")
                                } catch (e: Exception) {
                                    AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                }

                                // Safe update of progress on main thread
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(100, false, errorMessage)
                                }

                                val errorResponse = UploadV3Response(
                                    success = false,
                                    trackingId = 0,
                                    message = errorMessage,
                                    status = response.code(),
                                    tanggal_upload = "",
                                    nama_file = filename,
                                    results = null,
                                    type = AppUtils.DatabaseServer.PPRO,
                                    imageFullPath = emptyList(),
                                    imageName = emptyList()
                                )

                                return@withContext Result.success(errorResponse)
                            }
                        } catch (e: IOException) {
                            val errorMessage = "Network error: ${e.message}"
                            AppLogger.e("PPRO: Network error - $errorMessage")
                            AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                            // Update database with error
                            try {
                                withContext(Dispatchers.IO) {
                                    updateUploadStatusPPRO(
                                        itemId,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMessage.take(1000)}..."
                                    )
                                }
                                AppLogger.d("PPRO: Item ID $itemId has been updated with network error")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                            }

                            // Safe update of progress on main thread
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMessage)
                            }

                            val errorResponse = UploadV3Response(
                                success = false,
                                trackingId = 0,
                                message = errorMessage,
                                status = 0,
                                tanggal_upload = "",
                                nama_file = filename,
                                results = null,
                                type = AppUtils.DatabaseServer.PPRO,
                                imageFullPath = emptyList(),
                                imageName = emptyList()
                            )

                            return@withContext Result.success(errorResponse)
                        } catch (e: Exception) {
                            val errorMessage = "API error: ${e.message}"
                            AppLogger.e("PPRO: Exception during API call - ${e.javaClass.simpleName} - $errorMessage")
                            AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                            // Update database with error
                            try {
                                withContext(Dispatchers.IO) {
                                    updateUploadStatusPPRO(
                                        itemId,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMessage.take(1000)}..."
                                    )
                                }
                                AppLogger.d("PPRO: Item ID $itemId has been updated with API exception")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                            }

                            // Safe update of progress on main thread
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMessage)
                            }

                            val errorResponse = UploadV3Response(
                                success = false,
                                trackingId = 0,
                                message = errorMessage,
                                status = 0,
                                tanggal_upload = "",
                                nama_file = filename,
                                results = null,
                                type = AppUtils.DatabaseServer.PPRO,
                                imageFullPath = emptyList(),
                                imageName = emptyList()
                            )

                            return@withContext Result.success(errorResponse)
                        }
                    } catch (e: Exception) {
                        val errorMessage = "Fatal error in PPRO upload: ${e.message}"
                        AppLogger.e("PPRO: Top-level exception - $errorMessage")
                        AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                        // Safe update of progress on main thread
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(100, false, errorMessage)
                        }

                        val errorResponse = UploadV3Response(
                            success = false,
                            trackingId = 0,
                            message = errorMessage,
                            status = 0,
                            tanggal_upload = "",
                            nama_file = filename,
                            results = null,
                            type = AppUtils.DatabaseServer.PPRO,
                            imageFullPath = emptyList(),
                            imageName = emptyList()
                        )

                        return@withContext Result.success(errorResponse)
                    }
                }
                else {
                    AppLogger.d("Starting JSON data upload for: $filename")
                    try {
                        // Validate that we have data
                        if (data.isBlank()) {
                            val errorMsg = "JSON data is empty for $filename"
                            AppLogger.e(errorMsg)
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(100, false, errorMsg)
                            }
                            return@withContext Result.failure(Exception(errorMsg))
                        }

                        // Create the request body from the JSON string
                        val jsonRequestBody = RequestBody.create(
                            "application/json".toMediaTypeOrNull(),
                            data
                        )

                        AppLogger.d("====== REQUEST DATA ======")
                        AppLogger.d("Filename: $filename (for reference only)")
                        AppLogger.d("JSON Data length: ${data.length} characters")

                        // Simulate progress
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(25, false, null)
                        }

                        // Make the API call with raw JSON
                        AppLogger.d("====== MAKING API CALL ======")
                        AppLogger.d("Using raw JSON body")

                        val response = TestingAPIClient.instance.uploadJsonV3Raw(
                            jsonData = jsonRequestBody
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

                                // Check if status is between 1 and 3
                                val statusInt = responseBody.status.toInt()
                                val isSuccess = statusInt in 1..3

                                // Update progress based on status
                                withContext(Dispatchers.Main) {
                                    if (isSuccess) {
                                        // Status is valid, mark as success
                                        onProgressUpdate(100, true, null)
                                        AppLogger.d("Upload marked as SUCCESS with status: $statusInt")
                                    } else {
                                        // Status is outside valid range, mark as failure
                                        val errorMessage = responseBody.message
                                            ?: "Upload failed with status: $statusInt"
                                        onProgressUpdate(100, false, errorMessage)
                                        AppLogger.d("Upload marked as FAILURE: $errorMessage")
                                    }
                                }

                                val jsonResponse = UploadV3Response(
                                    success = responseBody.success,
                                    trackingId = responseBody.trackingId,
                                    message = responseBody.message,
                                    status = responseBody.status,
                                    tanggal_upload = responseBody.tanggal_upload,
                                    nama_file = responseBody.nama_file,
                                    results = responseBody.results,
                                    type = "json",
                                    table_ids = tableIds
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
                            val errorMsg =
                                "Upload failed - Code: ${response.code()}, Error: $errorBody"

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

    fun createJsonTableNameMapping(globalIdESPB: List<Int>): String {
        val tableMap = mapOf(
            AppUtils.DatabaseTables.ESPB to globalIdESPB // Use the passed parameter
        )
        return Gson().toJson(tableMap) // Convert to JSON string
    }

    private suspend fun updateUploadStatusCMP(
        id: Int,
        statusUploadCMP: Int,
        uploaderInfo: String,
        uploaderAt: String,
        uploadedById: Int,
        message: String? = null
    ) {
        espbDao.updateUploadStatusCMP(
            id,
            statusUploadCMP,
            uploaderInfo,
            uploaderAt,
            uploadedById,
            message
        )
    }

    private suspend fun updateUploadStatusPPRO(
        id: Int,
        statusUploadPpro: Int,
        uploaderInfo: String,
        uploaderAt: String,
        uploadedById: Int,
        message: String? = null
    ) {
        espbDao.updateUploadStatusPPRO(
            id,
            statusUploadPpro,
            uploaderInfo,
            uploaderAt,
            uploadedById,
            message
        )
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
                AppLogger.d(
                    "Starting file upload: ${file.name}, Size: ${
                        AppUtils.formatFileSize(
                            fileSize
                        )
                    }"
                )
                val progressRequestBody = ProgressRequestBody(
                    file,
                    "application/zip"
                ) { progress, bytesUploaded, totalBytes, done ->
                    AppLogger.d(
                        "Upload progress: $progress% (${
                            AppUtils.formatFileSize(
                                bytesUploaded
                            )
                        }/${AppUtils.formatFileSize(totalBytes)})"
                    )

                    // Still call the original callback to maintain compatibility
                    onProgressUpdate(progress, false, null)

                    // Additional tracking can be done here, but we're preserving the original callback signature
                }
                val filePart =
                    MultipartBody.Part.createFormData("zipFile", file.name, progressRequestBody)

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