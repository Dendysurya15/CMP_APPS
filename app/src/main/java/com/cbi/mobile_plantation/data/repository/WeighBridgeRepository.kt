package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.Constants
import com.cbi.mobile_plantation.data.network.StagingApiClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

@Suppress("UNREACHABLE_CODE")
class WeighBridgeRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val tphDao = database.tphDao()
    private val karyawanDao = database.karyawanDao()
    private val espbDao = database.espbDao()
    private val uploadCMPDao = database.uploadCMPDao()
    private val blokDao = database.blokDao()

    suspend fun getMill(millId: Int): List<MillModel> {
        return millDao.getMillById(millId)
    }

    suspend fun getTransporter(transporterId: Int): List<TransporterModel> {
        return transporterDao.getTransporterById(transporterId)
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
    }

    suspend fun getDataByIdInBlok(listBlokId: List<Int>): List<BlokModel> {
        return blokDao.getDataByIdInBlok(listBlokId)
    }


    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return karyawanDao.getPemuatByIdList(idPemuat)
    }

    suspend fun coundESPBUploaded(): Int {
        return espbDao.countESPBUploaded()
    }

    suspend fun getActiveESPB(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
        try {
            val data = espbDao.getAllActive()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveESPBAll(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
        try {
            val data = espbDao.getAllActiveESPB()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTPHByBlockId(blockId: Int): Result<TPHNewModel?> = withContext(Dispatchers.IO) {
        try {
            val tphData = tphDao.getTPHByBlockId(blockId)
            Result.success(tphData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadHistoryESPB(date: String? = null): List<ESPBEntity> {
        return try {
            espbDao.getAllESPBS(date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB history: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun getCountCreatedToday(): Int {
        return try {
            espbDao.getCountCreatedToday()
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
        }
    }


    suspend fun getActiveESPBByIds(ids: List<Int>): Result<List<ESPBEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val data = espbDao.getActiveESPBByIds(ids)
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun deleteESPBByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        espbDao.deleteByListID(ids)
    }

    suspend fun loadHistoryUploadeSPB(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
        try {
            val data = espbDao.getAllESPBUploaded()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatusUploadEspbCmpSp(ids: List<Int>, statusUpload: Int) {
        espbDao.updateStatusUploadEspbCmpSp(ids, statusUpload)
    }

    // Function to check if noESPB exists
    suspend fun isNoESPBExists(noESPB: String): Boolean {
        return espbDao.isNoESPBExists(noESPB) > 0
    }

    // Function to insert data into the database
    suspend fun insertESPBDataAndGetId(espbData: ESPBEntity): Int {
        return espbDao.insertAndGetId(espbData).toInt()
    }

    sealed class SaveResultESPBKrani {
        data class Success(val id: Int) : SaveResultESPBKrani()
        object AlreadyExists : SaveResultESPBKrani()
        data class Error(val exception: Exception) : SaveResultESPBKrani()
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

    suspend fun updateDataIsZippedESPB(ids: List<Int>, statusArchive: Int) {
        espbDao.updateDataIsZippedESPB(ids, statusArchive)
    }

    // Create a data class to hold error information
    data class UploadError(
        val itemId: Int,
        val errorMessage: String,
        val errorType: String
    )

    suspend fun uploadESPBKraniTimbang(
        dataList: List<Map<String, Any>>,
        globalIdESPB: List<Int>,
        onProgressUpdate: (Int, Int, Boolean, String?) -> Unit // itemId, progress, isSuccess, errorMsg
    ): Result<String>? {
        return try {
            withContext(Dispatchers.IO) {
                val results = mutableMapOf<Int, Boolean>()
                val errors = mutableListOf<UploadError>()
                val idsESPB = mutableListOf<Int>() // ✅ Define list outside loop
                AppLogger.d("Starting upload for ${dataList.size} items")
                AppLogger.d(globalIdESPB.toString())
                AppLogger.d(dataList.toString())

                for (item in dataList) {


                    val endpoint = item["endpoint"] as String
                    val num = item["num"] as Int
                    val ipMill = item["ip"] as String
                    val itemId = item["id"] as Int
                    val uploaderInfo = item["uploader_info"] as String
                    val uploadedAt = item["uploaded_at"] as String
                    val uploadedById = item["uploaded_by_id"] as Int
                    AppLogger.d("Processing item ID: $num, Endpoint: $endpoint")

                    var errorMessage: String? = null

                    try {


                        if (endpoint == "PPRO") {
                            try {
                                onProgressUpdate(num, 10, false, null)
                                AppLogger.d("PPRO: Adding ID $num to idsESPB list")
                                idsESPB.add(itemId)

                                AppLogger.d("PPRO: Preparing data for API call")
                                // Replace the problematic line in your code
                                val data = try {
                                    val result = ApiService.dataUploadEspbKraniTimbangPPRO(
                                        dept_ppro = (item["dept_ppro"] ?: "0").toString(),
                                        divisi_ppro = (item["divisi_ppro"] ?: "0").toString(),
                                        commodity = (item["commodity"]
                                            ?: "2").toString(), // Added null check
                                        blok_jjg = (item["blok_jjg"] ?: "").toString(),
                                        nopol = (item["nopol"] ?: "").toString(),
                                        driver = (item["driver"] ?: "").toString(),
                                        pemuat_id = (item["pemuat_id"] ?: "").toString(),
                                        transporter_id = (item["transporter_id"] ?: "0").toString(),
                                        mill_id = (item["mill_id"] ?: "0").toString(),
                                        created_by_id = (item["created_by_id"] ?: "0").toString(),
                                        created_at = (item["created_at"] ?: "").toString(),
                                        no_espb = (item["no_espb"] ?: "").toString()
                                    )
                                    AppLogger.d("PPRO: Data prepared successfully")
                                    result
                                } catch (e: Exception) {
                                    errorMessage = "Data error: ${e.message}"
                                    AppLogger.e("PPRO: DataError Item ID: $num - $errorMessage")
                                    errors.add(UploadError(num, errorMessage, "DATA_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                    continue
                                }

                                AppLogger.d("PPRO: Data prepared for item ID: $num -> $data")
                                onProgressUpdate(num, 50, false, null)

                                try {
                                    AppLogger.d("PPRO: Making API call to StagingApiClient.insertESPBKraniTimbangPPRO")
                                    StagingApiClient.updateBaseUrl("http://$ipMill:3000")

                                    val response =
                                        StagingApiClient.instance.insertESPBKraniTimbangPPRO(data)
                                    AppLogger.d("PPRO: API call completed, isSuccessful=${response.isSuccessful}, code=${response.code()}")

                                    if (response.isSuccessful) {
                                        val responseBody = response.body()
                                        AppLogger.d("PPRO: Response body received, status=${responseBody?.status}")

                                        if (responseBody != null && responseBody.status == 1) {
                                            AppLogger.d("PPRO: Upload successful")
                                            results[num] = true
                                            onProgressUpdate(num, 100, true, null)

                                            try {
                                                AppLogger.d("PPRO: Updating local database status")
                                                updateUploadStatusPPRO(
                                                    itemId,
                                                    1,
                                                    uploaderInfo,
                                                    uploadedAt,
                                                    uploadedById,
                                                    ""
                                                )
                                                AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                            } catch (e: Exception) {
                                                AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                            }
                                        } else {
                                            val rawErrorMessage = responseBody?.message?.toString()
                                                ?: "No message provided"
                                            val extractedMessage =
                                                if (rawErrorMessage.contains("message=")) {
                                                    try {
                                                        // Extract the actual error message between "message=" and the next comma or period
                                                        val startIndex =
                                                            rawErrorMessage.indexOf("message=") + "message=".length
                                                        val endIndex =
                                                            rawErrorMessage.indexOf(",", startIndex)
                                                                .takeIf { it > 0 }
                                                                ?: rawErrorMessage.indexOf(
                                                                    ".",
                                                                    startIndex
                                                                ).takeIf { it > 0 }
                                                                ?: rawErrorMessage.length

                                                        rawErrorMessage.substring(
                                                            startIndex,
                                                            endIndex
                                                        ).trim()
                                                    } catch (e: Exception) {
                                                        // If parsing fails, use the original error
                                                        "API Error: ${rawErrorMessage.take(100)}"
                                                    }
                                                } else {
                                                    "API Error: ${rawErrorMessage.take(100)}"
                                                }

                                            try {
                                                AppLogger.d("PPRO: Updating local database status")
                                                updateUploadStatusPPRO(
                                                    itemId,
                                                    0,
                                                    uploaderInfo,
                                                    uploadedAt,
                                                    uploadedById,
                                                    "${extractedMessage.take(1000)}..."
                                                )
                                                AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                            } catch (e: Exception) {
                                                AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                            }

                                            AppLogger.e("PPRO: APIError Item ID: $itemId - $rawErrorMessage")
                                            errors.add(
                                                UploadError(
                                                    num,
                                                    extractedMessage,
                                                    "API_ERROR"
                                                )
                                            )
                                            results[num] = false
                                            onProgressUpdate(num, 100, false, extractedMessage)
                                        }
                                    } else {
                                        errorMessage = response.errorBody()?.string()
                                            ?: "Server error: ${response.code()}"

                                        try {
                                            AppLogger.d("PPRO: Updating local database status")
                                            updateUploadStatusPPRO(
                                                itemId,
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "${errorMessage.take(1000)}..."
                                            )
                                            AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                        } catch (e: Exception) {
                                            AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                        }
                                        AppLogger.e("PPRO: ServerError Item ID: $num - $errorMessage")
                                        errors.add(UploadError(num, errorMessage!!, "SERVER_ERROR"))
                                        results[num] = false
                                        onProgressUpdate(num, 100, false, errorMessage)
                                    }
                                } catch (e: IOException) {
                                    AppLogger.e("PPRO: Network error: ${e.message}")
                                    AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                    errorMessage = "Network error: ${e.message}"

                                    try {
                                        AppLogger.d("PPRO: Updating local database status")
                                        updateUploadStatusPPRO(
                                            itemId,
                                            0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${errorMessage.take(1000)}..."
                                        )
                                        AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                    }
                                    AppLogger.e("PPRO: NetworkError Item ID: $num - $errorMessage")
                                    errors.add(UploadError(num, errorMessage!!, "NETWORK_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                } catch (e: Exception) {
                                    AppLogger.e("PPRO: Exception during API call: ${e.javaClass.simpleName} - ${e.message}")
                                    AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                    errorMessage = "API error: ${e.message}"
                                    try {
                                        AppLogger.d("PPRO: Updating local database status")
                                        updateUploadStatusPPRO(
                                            itemId,
                                            0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${errorMessage.take(1000)}..."
                                        )
                                        AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                    } catch (e: Exception) {
                                        AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                    }
                                    AppLogger.e("PPRO: APIError Item ID: $num - $errorMessage")
                                    errors.add(UploadError(num, errorMessage!!, "API_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                }
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Top-level exception in PPRO block: ${e.javaClass.simpleName} - ${e.message}")
                                AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                errorMessage = "Fatal error in PPRO upload: ${e.message}"
                                try {
                                    AppLogger.d("PPRO: Updating local database status")
                                    updateUploadStatusPPRO(
                                        itemId,
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMessage.take(1000)}..."
                                    )
                                    AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                } catch (e: Exception) {
                                    AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                }
                                errors.add(UploadError(num, errorMessage!!, "FATAL_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                            }
                        }
                        else if (endpoint == "CMP") {
                            idsESPB.add(itemId)
                            onProgressUpdate(num, 10, false, null)
                            val data = item["data"] as? String
                            val fileName = item["no_espb"] as? String

                            if (data.isNullOrEmpty()) {
                                errorMessage = "JSON data is empty or missing"
                                AppLogger.e(errorMessage)
                                for (id in idsESPB) {
                                    try {
                                        withContext(Dispatchers.IO) { // Ensures it runs in background & waits
                                            updateUploadStatusCMP(
                                                id, // ✅ Replace itemId with id from idsESPB
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "${errorMessage!!.take(1000)}..."
                                            )
                                        }
                                        AppLogger.d("ESPB table dengan id $id has been updated")
                                    } catch (e: Exception) {
                                        AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                    }
                                }
                                errors.add(UploadError(num, errorMessage!!, "DATA_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                                continue
                            }

                            try {
                                // Check if data is blank
                                if (data.isBlank()) {
                                    val errorMsg = "JSON data is empty for $fileName"
                                    AppLogger.e(errorMsg)
                                    for (id in idsESPB) {
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
                                            AppLogger.d("ESPB table dengan id $id has been updated")
                                        } catch (e: Exception) {
                                            AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                        }
                                    }
                                    errors.add(UploadError(num, errorMsg, "EMPTY_DATA"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMsg)
                                    continue
                                }

                                // Create the JSON request body
                                val jsonRequestBody = RequestBody.create(
                                    "application/json".toMediaTypeOrNull(),
                                    data
                                )

                                // Log before API call
                                AppLogger.d("CMP Upload - Starting upload for data with size: ${data.length} characters")

                                try {
                                    // Use the direct method for uploading JSON data
                                    val response = CMPApiClient.instance.uploadJsonV3Raw(
                                        jsonData = jsonRequestBody
                                    )

                                    val responseBody = response.body()
                                    val httpStatusCode = response.code()

                                    AppLogger.d("CMP Upload - Response received: HTTP $httpStatusCode")
                                    AppLogger.d("CMP Upload - Response body: $responseBody")

                                    if (response.isSuccessful) {
                                        AppLogger.d(responseBody.toString())

                                        // Check if response body exists
                                        if (responseBody == null) {
                                            errorMessage = "Response body is null despite successful response"
                                            AppLogger.e(errorMessage!!)

                                            for (id in idsESPB) {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        updateUploadStatusCMP(
                                                            id,
                                                            httpStatusCode,
                                                            uploaderInfo,
                                                            uploadedAt,
                                                            uploadedById,
                                                            "${errorMessage!!.take(1000)}..."
                                                        )
                                                    }
                                                    AppLogger.d("ESPB table dengan id $id has been updated")
                                                } catch (e: Exception) {
                                                    AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                                }
                                            }

                                            errors.add(UploadError(num, errorMessage!!, "NULL_RESPONSE"))
                                            results[num] = false
                                            onProgressUpdate(num, 100, false, errorMessage)
                                            continue
                                        }

                                        // Process the response body
                                        responseBody.let {
                                            val jsonResultTableIds =
                                                createJsonTableNameMapping(globalIdESPB) // Pass globalIdESPB

                                            val uploadData = UploadCMPModel(
                                                tracking_id = it.trackingId.toString(), // Convert Int to String if needed
                                                nama_file = it.nama_file,
                                                status = it.status,
                                                tanggal_upload = it.tanggal_upload,
                                                table_ids = jsonResultTableIds
                                            )

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

                                            delay(100) // Small delay before the next operation

                                            // Check if status is between 1 and 3 (inclusive)
                                            val isStatusValid = it.status in 1..3
                                            val resultMessage = if (isStatusValid) {
                                                "Success Uploading to CMP"
                                            } else {
                                                "Uploaded with status: ${it.status}. Message: ${it.message ?: "No message"}"
                                            }

                                            // update espb id untuk status_cmp_upload
                                            for (id in idsESPB) {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        updateUploadStatusCMP(
                                                            id,
                                                            it.status ?: 0,  // Use response status instead of HTTP status code
                                                            uploaderInfo,
                                                            uploadedAt,
                                                            uploadedById,
                                                            resultMessage
                                                        )
                                                    }
                                                    AppLogger.d("ESPB table dengan id $id has been updated")
                                                } catch (e: Exception) {
                                                    AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                                }
                                            }

                                            // Set the result based on the status check
                                            results[num] = isStatusValid
                                            onProgressUpdate(num, 100, isStatusValid, if (!isStatusValid) resultMessage else null)
                                        }
                                    } else {
                                        // Get more detailed error information
                                        val errorBodyString = response.errorBody()?.string() ?: "No error body"
                                        errorMessage = "JSON upload failed: HTTP $httpStatusCode - ${response.message()}"
                                        AppLogger.e("JSON UploadError Item ID: $num - $errorMessage")
                                        AppLogger.e("JSON Error Response Body: $errorBodyString")

                                        errors.add(
                                            UploadError(
                                                num,
                                                errorMessage!!,
                                                "JSON_UPLOAD_ERROR"
                                            )
                                        )

                                        for (id in idsESPB) {
                                            try {
                                                withContext(Dispatchers.IO) { // Ensures it runs in background & waits
                                                    updateUploadStatusCMP(
                                                        id, // ✅ Replace itemId with id from idsESPB
                                                        responseBody!!.status,
                                                        uploaderInfo,
                                                        uploadedAt,
                                                        uploadedById,
                                                        "${errorMessage!!.take(1000)}..."
                                                    )
                                                }
                                                AppLogger.d("ESPB table dengan id $id has been updated")
                                            } catch (e: Exception) {
                                                AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                            }
                                        }
                                        results[num] = false
                                        onProgressUpdate(num, 100, false, errorMessage)
                                    }
                                } catch (e: Exception) {
                                    // Get detailed exception info
                                    val exceptionType = e.javaClass.simpleName
                                    val exceptionStackTrace = Log.getStackTraceString(e)

                                    errorMessage = "JSON upload error: [$exceptionType] ${e.message ?: "Unknown error"}"
                                    AppLogger.e("JSON UploadError Item ID: $num - $errorMessage")
                                    AppLogger.e("Exception stack trace: $exceptionStackTrace")

                                    // Check for specific error types
                                    when (e) {
                                        is IOException -> AppLogger.e("JSON Upload - Network error: Possible connectivity issue")
                                        is SocketTimeoutException -> AppLogger.e("JSON Upload - Timeout error: Server took too long to respond")
                                        is IllegalStateException -> AppLogger.e("JSON Upload - State error: Retrofit/OkHttp issue")
                                        is NullPointerException -> AppLogger.e("JSON Upload - Null error: A null value was unexpectedly encountered")
                                    }

                                    for (id in idsESPB) {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                updateUploadStatusCMP(
                                                    id,
                                                    0,
                                                    uploaderInfo,
                                                    uploadedAt,
                                                    uploadedById,
                                                    "${errorMessage!!.take(1000)}..."
                                                )
                                            }
                                            AppLogger.d("ESPB table dengan id $id has been updated")
                                        } catch (e: Exception) {
                                            AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                        }
                                    }
                                    errors.add(UploadError(num, errorMessage!!, "JSON_UPLOAD_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                    continue
                                }
                            } catch (e: Exception) {
                                // This is the outer try-catch for general data handling errors
                                val exceptionType = e.javaClass.simpleName
                                val exceptionStackTrace = Log.getStackTraceString(e)

                                errorMessage = "Data preparation error: [$exceptionType] ${e.message ?: "Unknown error"}"
                                AppLogger.e("JSON Data Error Item ID: $num - $errorMessage")
                                AppLogger.e("Data exception stack trace: $exceptionStackTrace")

                                for (id in idsESPB) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusCMP(
                                                id,
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "${errorMessage!!.take(1000)}..."
                                            )
                                        }
                                        AppLogger.d("ESPB table dengan id $id has been updated")
                                    } catch (e: Exception) {
                                        AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                    }
                                }
                                errors.add(UploadError(num, errorMessage!!, "JSON_DATA_PREPARATION_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                                continue
                            }
                        }
                        // Unknown endpoint
                        else {
                            errorMessage = "Unknown endpoint for item ID: $itemId -> $endpoint"
                            AppLogger.e(errorMessage!!)
                            for (id in idsESPB) {
                                try {
                                    withContext(Dispatchers.IO) { // Ensures it runs in background & waits
                                        updateUploadStatusCMP(
                                            id, // ✅ Replace itemId with id from idsESPB
                                            0,
                                            uploaderInfo,
                                            uploadedAt,
                                            uploadedById,
                                            "${errorMessage!!.take(1000)}..."
                                        )
                                    }
                                    AppLogger.d("ESPB table dengan id $id has been updated")
                                } catch (e: Exception) {
                                    AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                }
                            }

                            try {
                                AppLogger.d("PPRO: Updating local database status")
                                updateUploadStatusPPRO(
                                    itemId,
                                    0,
                                    uploaderInfo,
                                    uploadedAt,
                                    uploadedById,
                                    "${errorMessage.take(1000)}..."
                                )
                                AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                            }
                            errors.add(UploadError(num, errorMessage!!, "UNKNOWN_ENDPOINT"))
                            results[num] = false
                            onProgressUpdate(num, 100, false, errorMessage)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Unknown error: ${e.message}"
                        AppLogger.e("UnknownError Item ID: $num - $errorMessage")

                        for (id in idsESPB) {
                            try {
                                withContext(Dispatchers.IO) { // Ensures it runs in background & waits
                                    updateUploadStatusCMP(
                                        id, // ✅ Replace itemId with id from idsESPB
                                        0,
                                        uploaderInfo,
                                        uploadedAt,
                                        uploadedById,
                                        "${errorMessage!!.take(1000)}..."
                                    )
                                }
                                AppLogger.d("ESPB table dengan id $id has been updated")
                            } catch (e: Exception) {
                                AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                            }
                        }
                        errors.add(UploadError(num, errorMessage!!, "UNKNOWN_ERROR"))
                        results[num] = false
                        onProgressUpdate(num, 100, false, errorMessage)
                    }
                }

                val allSucceeded = results.values.all { it }

                AppLogger.d(allSucceeded.toString())
                if (allSucceeded) {
                    AppLogger.d("UploadResult All data uploaded successfully.")
                    Result.success("All data uploaded successfully.")
                } else {
                    val successCount = results.values.count { it }
                    val failCount = results.size - successCount
                    val errorMessage = "$failCount out of ${results.size} uploads failed."
                    AppLogger.e("UploadResult $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            AppLogger.e("RepositoryError Error: ${e.message}")
            Result.failure(Exception("Repository error: ${e.message}"))
        }
    }


    fun createJsonTableNameMapping(globalIdESPB: List<Int>): String {
        val tableMap = mapOf(
            AppUtils.DatabaseTables.ESPB to globalIdESPB // Use the passed parameter
        )
        return Gson().toJson(tableMap) // Convert to JSON string
    }


}

// Fetch TPH by ID

