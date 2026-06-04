package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadHarvestResponse
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.StagingApiClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.gson.Gson
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
import java.io.IOException


sealed class SaveResultNewUploadDataCMPAdmTimbang {
    object Success : SaveResultNewUploadDataCMPAdmTimbang()
    data class Error(val exception: Exception) : SaveResultNewUploadDataCMPAdmTimbang()
}

class UploadCMPRepositoryAdmTimbang(context: Context) {

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

    fun getIntString(jsonData: Map<String, Any?>, key: String, default: Int = 0): String {
        return when (val value = jsonData[key]) {
            is Double -> value.toInt().toString()
            is Float -> value.toInt().toString()
            is Int -> value.toString()
            is Long -> value.toString()
            is String -> value
            else -> default.toString()
        }
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
        } as Result<UploadWBCMPResponse>
    }

    data class ImageFileInfo(
        val file: File,
        val imageName: String,
        val tableId: String,
        val basePath: String,
        val databaseTable: String
    )


    sealed class UploadCMPResult {
        abstract val isSuccess: Boolean
        abstract val message: String

        data class V3Result(
            val response: UploadV3Response,
            override val isSuccess: Boolean = response.success,
            override val message: String = response.message
        ) : UploadCMPResult()

        data class HarvestResult(
            val response: UploadHarvestResponse,
            override val isSuccess: Boolean = response.status.equals("success", ignoreCase = true),
            override val message: String = response.message
        ) : UploadCMPResult()
    }

    suspend fun uploadJsonToServerV4(
        jsonFilePath: String,
        filename: String,
        data: String,
        type: String,
        tableIds: String? = null,
        databaseTable: String,
        endpoint: String = "v3",
        ipMill: String? = null,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadCMPResult> {
        return try {
            AppLogger.d("uploadJsonToServerV4 called with endpoint: $endpoint")

            // Make API call based on endpoint type
            val result = when (endpoint) {
                "harvest" -> {
                    AppLogger.d("🌾 Calling HARVEST endpoint...")
                    onProgressUpdate(10, false, null)

                    try {
                        // ✅ Parse the outer JSON to extract all fields
                        AppLogger.d("🌾 Parsing data wrapper...")
                        val dataWrapper = Gson().fromJson(data, Map::class.java) as Map<String, Any?>

                        // Extract metadata
                        val espbJson = dataWrapper["espb_json"]?.toString() ?: ""
                        val espbIds = (dataWrapper["espb_ids"] as? List<*>)?.mapNotNull {
                            (it as? Double)?.toInt() ?: (it as? Int)
                        } ?: emptyList()
                        val ipFromData = dataWrapper["ip"]?.toString() ?: ipMill ?: ""
                        val uploaderInfo = dataWrapper["uploader_info"]?.toString() ?: ""
                        val uploadedAt = dataWrapper["uploaded_at"]?.toString() ?: ""
                        val uploadedById = when (val id = dataWrapper["uploaded_by_id"]) {
                            is Double -> id.toInt()
                            is Int -> id
                            else -> 0
                        }

                        // ✅ Check if espb_json is empty
                        if (espbJson.isBlank()) {
                            val errorMsg = "espb_json is empty or missing"
                            AppLogger.e("🌾 $errorMsg")
                            onProgressUpdate(100, false, errorMsg)
                            return Result.failure(Exception(errorMsg))
                        }

                        // ✅ Parse the espb_json string as JSON
                        AppLogger.d("🌾 Unwrapping espb_json...")
                        val originalJson = JSONObject(espbJson)
                        val espbTableArray = originalJson.optJSONArray("espb_table")

                        if (espbTableArray == null || espbTableArray.length() == 0) {
                            val errorMsg = "espb_table array is missing or empty in espb_json"
                            AppLogger.e("🌾 $errorMsg")
                            onProgressUpdate(100, false, errorMsg)
                            return Result.failure(Exception(errorMsg))
                        }

                        // ✅ Get the first object from the espb_table array
                        val unwrappedJson = espbTableArray.getJSONObject(0)
                        
                        // Extract the specific local ID for this record to avoid overwriting other records in the batch
                        val currentRecordId = unwrappedJson.optInt("id", 0)
                        val actualIdsToUpdate = if (currentRecordId != 0) listOf(currentRecordId) else espbIds

                        // ✅ CLEAN DOUBLE-ESCAPED JSON FIELDS
                        val fieldsToClean = listOf("creator_info", "app_version", "update_info_sp")
                        for (field in fieldsToClean) {
                            if (unwrappedJson.has(field)) {
                                try {
                                    val originalValue = unwrappedJson.getString(field)
                                    val cleanedValue = cleanDoubleEscapedJson(originalValue)
                                    unwrappedJson.put(field, cleanedValue)
                                } catch (e: Exception) {
                                    AppLogger.e("⚠️ Could not clean field $field: ${e.message}")
                                }
                            }
                        }

                        val finalJsonString = unwrappedJson.toString()

                        AppLogger.d("🌾 JSON unwrapped successfully")
                        AppLogger.d("🌾 Final JSON preview (first 500 chars):")
                        AppLogger.d(finalJsonString.take(500))

                        onProgressUpdate(20, false, null)

                        // ✅ Create RequestBody with unwrapped JSON
                        val jsonRequestBody = RequestBody.create(
                            "application/json".toMediaTypeOrNull(),
                            finalJsonString
                        )

                        // ✅ Update base URL
                        StagingApiClient.updateBaseUrl("http://192.168.1.34:37891")
//                        StagingApiClient.updateBaseUrl("http://$ipFromData:37891")

                        onProgressUpdate(40, false, null)

                        // ✅ Make API call
                        AppLogger.d("🌾 Calling uploadHarvest API...")
                        val response = StagingApiClient.instance.uploadHarvest(
                            jsonData = jsonRequestBody
                        )

                        val httpStatusCode = response.code()

                        // ✅ LOG THE RESPONSE
                        AppLogger.d("🌾 HARVEST Response received:")
                        AppLogger.d("  ├─ URL: ${response.raw().request.url}")
                        AppLogger.d("  ├─ HTTP Status: $httpStatusCode")
                        AppLogger.d("  ├─ isSuccessful: ${response.isSuccessful}")
                        AppLogger.d("  └─ Body: ${response.body()}")

                        onProgressUpdate(70, false, null)

                        if (response.isSuccessful && response.body() != null) {
                            val harvestResponse = response.body()!!

                            // ✅ LOG ALL RESPONSE DETAILS
                            AppLogger.d("🌾 HARVEST Response Details:")
                            AppLogger.d("  ├─ status: ${harvestResponse.status}")
                            AppLogger.d("  ├─ message: ${harvestResponse.message}")
                            AppLogger.d("  ├─ id: ${harvestResponse.id}")
                            AppLogger.d("  └─ noESPB: ${harvestResponse.noESPB}")

                            val isSuccess = harvestResponse.status.equals("success", ignoreCase = true)
                            val finalMessage = if (isSuccess) {
                                "Success - ID: ${harvestResponse.id}, No ESPB: ${harvestResponse.noESPB}"
                            } else {
                                harvestResponse.message
                            }

                            // ✅ Update database for the specific ESPB IDs
                            if (isSuccess) {
                                AppLogger.d("🌾 Updating database for ${actualIdsToUpdate.size} ESPB IDs...")

                                // Generate random tracking ID
                                val randomTrackingId = (100000000..999999999).random().toString()
                                val jsonResultTableIds = createJsonTableNameMapping(actualIdsToUpdate)

                                val uploadData = UploadCMPModel(
                                    tracking_id = randomTrackingId,
                                    nama_file = "",
                                    status = 3,
                                    tanggal_upload = uploadedAt,
                                    table_ids = jsonResultTableIds
                                )

                                AppLogger.d("🌾 UploadCMPModel created:")
                                AppLogger.d("  ├─ tracking_id: ${uploadData.tracking_id}")
                                AppLogger.d("  ├─ status: ${uploadData.status}")
                                AppLogger.d("  └─ tanggal_upload: ${uploadData.tanggal_upload}")

                                try {
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
                                            AppLogger.d("🌾 Updated existing upload record")
                                        } else {
                                            uploadCMPDao.insertNewData(uploadData)
                                            AppLogger.d("🌾 Inserted new upload record")
                                        }
                                    }

                                    delay(100)

                                    // Update status for the specific ESPB IDs
                                    for (id in actualIdsToUpdate) {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                updateUploadStatusCMP(
                                                    id,
                                                    3,
                                                    uploaderInfo,
                                                    uploadedAt,
                                                    uploadedById,
                                                    finalMessage
                                                )
                                            }
                                            AppLogger.d("🌾 Updated ESPB ID $id with success status")
                                        } catch (e: Exception) {
                                            AppLogger.e("🌾 Failed to update ESPB ID $id: ${e.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("🌾 Failed to update database: ${e.message}")
                                }
                            } else {
                                // Update with error status for the specific ESPB IDs
                                AppLogger.d("🌾 Updating database with error status for ${actualIdsToUpdate.size} ESPB IDs...")
                                for (id in actualIdsToUpdate) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusCMP(
                                                id,
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                finalMessage
                                            )
                                        }
                                        AppLogger.d("🌾 Updated ESPB ID $id with error status")
                                    } catch (e: Exception) {
                                        AppLogger.e("🌾 Failed to update ESPB ID $id: ${e.message}")
                                    }
                                }
                            }

                            onProgressUpdate(100, isSuccess, if (!isSuccess) finalMessage else null)

                            Result.success(
                                UploadCMPResult.HarvestResult(
                                    response = harvestResponse
                                )
                            )
                        } else {
                            // ✅ Extract actual error message from JSON
                            val errorBodyString = response.errorBody()?.string() ?: "No error body"
                            AppLogger.e("🌾 HARVEST Error Response Body: $errorBodyString")

                            val errorMsg = try {
                                val errorJson = JSONObject(errorBodyString)
                                errorJson.optString("message", null)?.takeIf { it.isNotEmpty() }
                                    ?: errorJson.optString("error", null)?.takeIf { it.isNotEmpty() }
                                    ?: "Upload failed with HTTP $httpStatusCode"
                            } catch (e: Exception) {
                                AppLogger.e("🌾 Failed to parse error JSON: ${e.message}")
                                if (errorBodyString != "No error body" && errorBodyString.length < 200) {
                                    errorBodyString
                                } else {
                                    "Upload failed with HTTP $httpStatusCode"
                                }
                            }

                            AppLogger.e("🌾 HARVEST Upload failed: $errorMsg")

                            // Update database with error for the specific ESPB IDs
                            AppLogger.d("🌾 Updating database with error for ${actualIdsToUpdate.size} ESPB IDs...")
                            for (id in actualIdsToUpdate) {
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
                                    AppLogger.d("🌾 Updated ESPB ID $id with error")
                                } catch (e: Exception) {
                                    AppLogger.e("🌾 Failed to update ESPB ID $id: ${e.message}")
                                }
                            }

                            onProgressUpdate(100, false, errorMsg)
                            Result.failure(Exception(errorMsg))
                        }
                    } catch (e: Exception) {
                        AppLogger.e("🌾 HARVEST Exception: ${e.javaClass.simpleName} - ${e.message}")
                        AppLogger.e("🌾 Stack trace: ${Log.getStackTraceString(e)}")
                        val errorMsg = "Harvest upload error: ${e.message}"
                        onProgressUpdate(100, false, errorMsg)
                        Result.failure(e)
                    }
                }


                "v3" -> {
                    // ✅ V3 ENDPOINT = PPRO LOGIC
                    AppLogger.d("📤 Calling V3 (PPRO) endpoint...")
                    onProgressUpdate(10, false, null)

                    try {
                        AppLogger.d("PPRO: Processing data payload")

                        // Parse the JSON data
                        val jsonData = Gson().fromJson(data, Map::class.java)
                        AppLogger.d("PPRO: Parsed JSON data")

                        // Extract the item ID for database update
                        val itemId = (jsonData["id"] as? Double)?.toInt() ?: (jsonData["id"] as? Int) ?: 0
                        val ipMillFromData = jsonData["ip"]?.toString() ?: ipMill ?: ""

                        // Extract the uploader info for database update
                        val uploaderInfo = jsonData["uploader_info"]?.toString() ?: ""
                        val uploadedAt = jsonData["uploaded_at"]?.toString() ?: ""
                        val uploadedById = when (val id = jsonData["uploaded_by_id"]) {
                            is Double -> id.toInt()
                            is Int -> id
                            else -> 0
                        }

                        AppLogger.d("PPRO: IP Mill: $ipMillFromData")
                        AppLogger.d("PPRO: Item ID: $itemId")
                        AppLogger.d("PPRO: Uploader info: $uploaderInfo")
                        AppLogger.d("PPRO: Uploaded at: $uploadedAt")
                        AppLogger.d("PPRO: Uploaded by ID: $uploadedById")

                        onProgressUpdate(20, false, null)

                        // Prepare API data
                        val jsonMap = jsonData as Map<String, Any?>
                        val apiData = try {
                            val result = ApiService.dataUploadEspbKraniTimbangPPRO(
                                dept_ppro = getIntString(jsonMap, "dept_ppro"),
                                divisi_ppro = getIntString(jsonMap, "divisi_ppro"),
                                commodity = getIntString(jsonMap, "commodity", 2),
                                blok_jjg = (jsonMap["blok_jjg"] ?: "").toString(),
                                nopol = (jsonMap["nopol"] ?: "").toString(),
                                driver = (jsonMap["driver"] ?: "").toString(),
                                pemuat_id = (jsonMap["pemuat_id"] ?: "").toString(),
                                transporter_id = getIntString(jsonMap, "transporter_id"),
                                mill_id = getIntString(jsonMap, "mill_id"),
                                created_by_id = getIntString(jsonMap, "created_by_id"),
                                created_at = (jsonMap["created_at"] ?: "").toString(),
                                no_espb = (jsonMap["no_espb"] ?: "").toString()
                            )
                            AppLogger.d("PPRO: Data prepared successfully")
                            result
                        } catch (e: Exception) {
                            val errorMsg = "Data preparation error: ${e.message}"
                            AppLogger.e("PPRO: $errorMsg")

                            // Update database with error
                            try {
                                updateUploadStatusPPRO(
                                    itemId,
                                    0,
                                    uploaderInfo,
                                    uploadedAt,
                                    uploadedById,
                                    "${errorMsg.take(1000)}..."
                                )
                                AppLogger.d("PPRO: Item ID $itemId updated with data preparation error")
                            } catch (dbError: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${dbError.message}")
                            }

                            onProgressUpdate(100, false, errorMsg)

                            // Return failure wrapped in V3Result
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

                            return Result.success(
                                UploadCMPResult.V3Result(response = errorResponse)
                            )
                            }

                        AppLogger.d("PPRO: Data prepared: $apiData")
                        onProgressUpdate(50, false, null)

                        try {
                            AppLogger.d("PPRO: Making API call to StagingApiClient.insertESPBKraniTimbangPPRO")
                            StagingApiClient.updateBaseUrl("http://192.168.1.34:37891")
//                            StagingApiClient.updateBaseUrl("http://$ipMillFromData:3000")

                            val response = StagingApiClient.instance.insertESPBKraniTimbangPPRO(apiData)

                            val httpStatusCode = response.code()

                            AppLogger.d("📤 PPRO Response received:")
                            AppLogger.d("  ├─ URL: ${response.raw().request.url}")
                            AppLogger.d("  ├─ HTTP Status: $httpStatusCode")
                            AppLogger.d("  ├─ isSuccessful: ${response.isSuccessful}")
                            AppLogger.d("  └─ Status: ${response.body()?.status}")

                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                AppLogger.d("PPRO: Response body received, status=${responseBody?.status}")

                                if (responseBody != null && responseBody.status == 1) {
                                    AppLogger.d("PPRO: Upload successful")

                                    // Update database with success
                                    try {
                                        updateUploadStatusPPRO(
                                            itemId,
                                            1,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "Success Uploading to PPRO"
                                        )
                                        AppLogger.d("PPRO: Item ID $itemId updated with success status")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                    }

                                    onProgressUpdate(100, true, null)

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

                                    Result.success(
                                        UploadCMPResult.V3Result(response = successResponse)
                                    )
                                } else {
                                    // Extract error message
                                    val rawErrorMessage = responseBody?.message?.toString() ?: "No message provided"
                                    val extractedMessage = if (rawErrorMessage.contains("message=")) {
                                        try {
                                            val startIndex = rawErrorMessage.indexOf("message=") + "message=".length
                                            val endIndex = rawErrorMessage.indexOf(",", startIndex).takeIf { it > 0 }
                                                ?: rawErrorMessage.indexOf(".", startIndex).takeIf { it > 0 }
                                                ?: rawErrorMessage.length

                                            rawErrorMessage.substring(startIndex, endIndex).trim()
                                        } catch (e: Exception) {
                                            "API Error: ${rawErrorMessage.take(100)}"
                                        }
                                    } else {
                                        "API Error: ${rawErrorMessage.take(100)}"
                                    }

                                    AppLogger.e("PPRO: API Error - $extractedMessage")

                                    // Update database with error
                                    try {
                                        updateUploadStatusPPRO(
                                            itemId,
                                            0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${extractedMessage.take(1000)}..."
                                        )
                                        AppLogger.d("PPRO: Item ID $itemId updated with API error")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                    }

                                    onProgressUpdate(100, false, extractedMessage)

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

                                    Result.success(
                                        UploadCMPResult.V3Result(response = errorResponse)
                                    )
                                }
                            } else {
                                // ✅ Extract actual error message from JSON error body
                                val errorBodyString = response.errorBody()?.string() ?: "No error body"
                                AppLogger.e("📤 PPRO Error Response Body: $errorBodyString")

                                val errorMessage = try {
                                    val errorJson = JSONObject(errorBodyString)
                                    errorJson.optString("message", null)?.takeIf { it.isNotEmpty() }
                                        ?: errorJson.optString("error", null)?.takeIf { it.isNotEmpty() }
                                        ?: "Server error: ${response.code()}"
                                } catch (e: Exception) {
                                    if (errorBodyString != "No error body" && errorBodyString.length < 200) {
                                        errorBodyString
                                    } else {
                                        "Server error: ${response.code()}"
                                    }
                                }

                                AppLogger.e("PPRO: Server Error - $errorMessage")

                                // Update database with error
                                try {
                                    updateUploadStatusPPRO(
                                        itemId,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMessage.take(1000)}..."
                                    )
                                    AppLogger.d("PPRO: Item ID $itemId updated with server error")
                                } catch (e: Exception) {
                                    AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                                }

                                onProgressUpdate(100, false, errorMessage)

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

                                Result.success(
                                    UploadCMPResult.V3Result(response = errorResponse)
                                )
                            }
                        } catch (e: IOException) {
                            val errorMessage = "Network error: ${e.message}"
                            AppLogger.e("PPRO: Network error - $errorMessage")
                            AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                            // Update database with error
                            try {
                                updateUploadStatusPPRO(
                                    itemId,
                                    0,
                                    uploaderInfo,
                                    uploadedAt,
                                    uploadedById,
                                    "${errorMessage.take(1000)}..."
                                )
                                AppLogger.d("PPRO: Item ID $itemId updated with network error")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                            }

                            onProgressUpdate(100, false, errorMessage)

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

                            Result.success(
                                UploadCMPResult.V3Result(response = errorResponse)
                            )
                        } catch (e: Exception) {
                            val errorMessage = "API error: ${e.message}"
                            AppLogger.e("PPRO: Exception during API call - ${e.javaClass.simpleName} - $errorMessage")
                            AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                            // Update database with error
                            try {
                                updateUploadStatusPPRO(
                                    itemId,
                                    0,
                                    uploaderInfo,
                                    uploadedAt,
                                    uploadedById,
                                    "${errorMessage.take(1000)}..."
                                )
                                AppLogger.d("PPRO: Item ID $itemId updated with API exception")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update database for Item ID: $itemId - ${e.message}")
                            }

                            onProgressUpdate(100, false, errorMessage)

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

                            Result.success(
                                UploadCMPResult.V3Result(response = errorResponse)
                            )
                        }
                    } catch (e: Exception) {
                        val errorMessage = "Fatal error in PPRO upload: ${e.message}"
                        AppLogger.e("PPRO: Top-level exception - $errorMessage")
                        AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")

                        onProgressUpdate(100, false, errorMessage)

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

                        Result.success(
                            UploadCMPResult.V3Result(response = errorResponse)
                        )
                    }
                }

                else -> {
                    val errorMsg = "Unknown endpoint: $endpoint"
                    AppLogger.e(errorMsg)
                    onProgressUpdate(100, false, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            }

            result

        } catch (e: Exception) {
            AppLogger.e("uploadJsonToServerV4 - Fatal error: ${e.message}")
            AppLogger.e("Stack trace: ${Log.getStackTraceString(e)}")
            onProgressUpdate(100, false, e.message)
            Result.failure(e)
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
        } as Result<UploadWBCMPResponse>
    }

    private fun cleanDoubleEscapedJson(jsonString: String): String {
        var cleaned = jsonString
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }
        cleaned = cleaned.replace("\\\"", "\"")
        cleaned = cleaned.replace("\\\\", "\\")
        return cleaned
    }


}