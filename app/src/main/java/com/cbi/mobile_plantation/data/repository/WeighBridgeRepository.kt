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
import org.json.JSONObject
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

    suspend fun getEspbByNumber(noEspb: String): Result<ESPBEntity?> = withContext(Dispatchers.IO) {
        try {
            val espbData = espbDao.getEspbByNumber(noEspb)
            Result.success(espbData)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun getBlokByEstAfdBlokId(est: String, afd: String, blokId: String): Result<BlokModel?> = withContext(Dispatchers.IO) {
        try {
            val blokData = blokDao.getBlokByEstAfdKode(est, afd, blokId)
            Result.success(blokData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBlokbyParams(blockId: Int, est: String?, afd: String?): Result<BlokModel?> = withContext(Dispatchers.IO) {
        try {
            // First try to match with kode if available
            var blokData: BlokModel? = null

            if (!est.isNullOrEmpty() && !afd.isNullOrEmpty()) {
                // Convert blockId to String for the kode parameter
                blokData = blokDao.getBlokByEstAfdKode(est, afd, blockId.toString())
                if (blokData != null) {
                    AppLogger.d("Blok found using id_ppro search - est: $est, afd: $afd, kode: $blockId")
                    AppLogger.d("Found BlokModel: ${blokData.nama} (id_ppro: ${blokData.id_ppro})")
                } else {
                    AppLogger.d("No blok found using id_ppro search - est: $est, afd: $afd, kode: $blockId")
                }
            }

            if (blokData == null && !est.isNullOrEmpty() && !afd.isNullOrEmpty()) {
                blokData = blokDao.getBlokByIdEstAfd(blockId, est, afd)
                if (blokData != null) {
                    AppLogger.d("Blok found using ID search - blockId: $blockId, est: $est, afd: $afd")
                    AppLogger.d("Found BlokModel: ${blokData.nama} (id: ${blokData.id})")
                } else {
                    AppLogger.d("No blok found using ID search - blockId: $blockId, est: $est, afd: $afd")
                }
            }

            if (blokData == null) {
                AppLogger.d("No blok found with any search method")
            }

            Result.success(blokData)
        } catch (e: Exception) {
            AppLogger.e("Error in fetchBlokbyParams: ${e.message}")
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
                                                    "${extractedMessage.take(1500)}..."
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
                                                "${errorMessage.take(1500)}..."
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
                                            "${errorMessage.take(1500)}..."
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
                                            "${errorMessage.take(1500)}..."
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
                                        "${errorMessage.take(1500)}..."
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
                        // ✅ UPDATED STAGING_CMP SECTION
// Replace your existing STAGING_CMP section with this code

                        else if (endpoint == "STAGING_CMP") {
                            idsESPB.add(itemId)
                            onProgressUpdate(num, 10, false, null)
                            val data = item["data"] as? String
                            val ipMill = item["ip"] as? String
                            val fileName = item["no_espb"] as? String
                            val updatedDateWb = item["updated_date_wb"] as? String ?: "" // ✅ Get updated_date_wb from item

                            if (data.isNullOrEmpty()) {
                                errorMessage = "JSON data is empty or missing"
                                AppLogger.e(errorMessage)
                                for (id in idsESPB) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateUploadStatusCMP(
                                                id,
                                                0,
                                                uploaderInfo,
                                                uploadedAt,
                                                uploadedById,
                                                "${errorMessage!!.take(1500)}..."
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
                                                    "${errorMsg.take(1500)}..."
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
                                AppLogger.d("data bro $data")
                                val originalJson = JSONObject(data)
                                val espbTableArray = originalJson.optJSONArray("espb_table")

                                if (espbTableArray == null || espbTableArray.length() == 0) {
                                    val errorMsg = "espb_table array is missing or empty"
                                    AppLogger.e(errorMsg)
                                    errors.add(UploadError(num, errorMsg, "INVALID_JSON_STRUCTURE"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMsg)
                                    continue
                                }

// Get the first (and only) object from the array
                                val unwrappedJson = espbTableArray.getJSONObject(0)

// ✅ CLEAN THE JSON FIELDS
                                val cleanedJson = cleanJsonFields(unwrappedJson)

// Convert to string for RequestBody
                                val finalJsonString = cleanedJson.toString()

                                AppLogger.d("✅ JSON cleaned and unwrapped successfully")
//                                AppLogger.d("📋 Final JSON (first 500 chars):")
//                                AppLogger.d(finalJsonString.take(1000))

// Create the JSON request body with cleaned data
                                val jsonRequestBody = RequestBody.create(
                                    "application/json".toMediaTypeOrNull(),
                                    finalJsonString
                                )
                                try {

//                                    StagingApiClient.updateBaseUrl("http://10.9.116.125:37891")
                                    StagingApiClient.updateBaseUrl("http://$ipMill:37891")
                                    // ✅ Use uploadHarvest which returns UploadHarvestResponse
                                    val response = StagingApiClient.instance.uploadHarvest(
                                           jsonData = jsonRequestBody
                                    )

                                    val responseBody = response.body()
                                    val httpStatusCode = response.code()

                                    // ✅ ALWAYS LOG THE RESPONSE
                                    AppLogger.d("🌐 Response URL: ${response.raw().request.url}")
                                    AppLogger.d("CMP Upload - Response received: HTTP $httpStatusCode")
                                    AppLogger.d("CMP Upload - Response isSuccessful: ${response.isSuccessful}")
                                    AppLogger.d("CMP Upload - Response body: $responseBody")

//                                    if (responseBody != null) {
//                                        AppLogger.d("CMP Upload - Response Details:")
//                                        AppLogger.d("  ├─ status: ${responseBody.status}")
//                                        AppLogger.d("  ├─ message: ${responseBody.message}")
//                                        AppLogger.d("  ├─ id: ${responseBody.id}")
//                                        AppLogger.d("  └─ noESPB: ${responseBody.noESPB}")
//                                    } else {
//                                        AppLogger.d("CMP Upload - Response body is NULL")
//                                    }

                                    if (response.isSuccessful) {
                                        // Check if response body exists
                                        if (responseBody == null) {
                                            errorMessage = "Response body is null despite successful response"
                                            AppLogger.e(errorMessage!!)

                                            for (id in idsESPB) {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        updateUploadStatusCMP(
                                                            id,
                                                            0,
                                                            uploaderInfo,
                                                            uploadedAt,
                                                            uploadedById,
                                                            "${errorMessage!!.take(1500)}..."
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

                                        // ✅ Process the NEW UploadHarvestResponse
                                        responseBody.let {
                                            // ✅ Generate random 9-digit tracking ID
                                            val randomTrackingId = (100000000..999999999).random().toString()

                                            val jsonResultTableIds = createJsonTableNameMapping(globalIdESPB)

                                            val uploadData = UploadCMPModel(
                                                tracking_id = randomTrackingId, // ✅ Random 9-digit ID
                                                nama_file = "", // ✅ Empty as requested
                                                status = 3, // ✅ Status = 3
                                                tanggal_upload = updatedDateWb, // ✅ Use updated_date_wb from item
                                                table_ids = jsonResultTableIds
                                            )

                                            AppLogger.d("📦 UploadCMPModel created:")
                                            AppLogger.d("  ├─ tracking_id: ${uploadData.tracking_id}")
                                            AppLogger.d("  ├─ nama_file: '${uploadData.nama_file}'")
                                            AppLogger.d("  ├─ status: ${uploadData.status}")
                                            AppLogger.d("  ├─ tanggal_upload: ${uploadData.tanggal_upload}")
                                            AppLogger.d("  └─ table_ids: ${uploadData.table_ids}")

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
                                                    AppLogger.d("Updated existing upload record")
                                                } else {
                                                    uploadCMPDao.insertNewData(uploadData)
                                                    AppLogger.d("Inserted new upload record")
                                                }
                                            }

                                            delay(100)

                                            // ✅ Check if response status is "success" (case-insensitive)
                                            val isStatusValid = it.status.equals("success", ignoreCase = true)
                                            val resultMessage = if (isStatusValid) {
                                                "Success Uploading to CMP - ID: ${it.id}, No ESPB: ${it.noESPB}"
                                            } else {
                                                "Upload status: ${it.status}. Message: ${it.message}"
                                            }

                                            AppLogger.d("✅ Upload result: isValid=$isStatusValid, message='$resultMessage'")

                                            // Update espb table for all IDs
                                            for (id in idsESPB) {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        updateUploadStatusCMP(
                                                            id,
                                                            if (isStatusValid) 3 else 0, // ✅ Use 3 for success, 0 for failure
                                                            uploaderInfo,
                                                            uploadedAt,
                                                            uploadedById,
                                                            resultMessage
                                                        )
                                                    }
                                                    AppLogger.d("ESPB table dengan id $id has been updated with status ${if (isStatusValid) 3 else 0}")
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
                                        errorMessage = try {
                                            val errorJson = JSONObject(errorBodyString)
                                            errorJson.optString("message", null)?.takeIf { it.isNotEmpty() }
                                                ?: errorJson.optString("error", null)?.takeIf { it.isNotEmpty() }
                                                ?: "Upload failed: HTTP $httpStatusCode - ${response.message()}"
                                        } catch (e: Exception) {
                                            if (errorBodyString != "No error body" && errorBodyString.length < 200) {
                                                errorBodyString
                                            } else {
                                                "Upload failed: HTTP $httpStatusCode - ${response.message()}"
                                            }
                                        }
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
                                                withContext(Dispatchers.IO) {
                                                    updateUploadStatusCMP(
                                                        id,
                                                        0,
                                                        uploaderInfo,
                                                        uploadedAt,
                                                        uploadedById,
                                                        "${errorMessage!!.take(1500)}..."
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
                                                    "${errorMessage!!.take(1500)}..."
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
                                                "${errorMessage!!.take(1500)}..."
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
                                            "${errorMessage!!.take(1500)}..."
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
                                    "${errorMessage.take(1500)}..."
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
                                        "${errorMessage!!.take(1500)}..."
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

    fun cleanJsonFields(jsonObject: JSONObject): JSONObject {
        val cleanedJson = JSONObject()

        // Fields that might be double-escaped JSON strings
        val jsonStringFields = listOf("uploader_info_sp","uploader_info_wb")

        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)

            if (key in jsonStringFields && value is String) {
                // Try to parse and re-stringify to clean up escaping
                try {
                    // Remove extra quotes at start and end if present
                    var cleanedValue = value.trim()

                    // If it starts and ends with quotes, remove them
                    if (cleanedValue.startsWith("\"") && cleanedValue.endsWith("\"")) {
                        cleanedValue = cleanedValue.substring(1, cleanedValue.length - 1)
                    }

                    // Replace escaped quotes
                    cleanedValue = cleanedValue.replace("\\\"", "\"")

                    // Try to parse as JSON to validate
                    val parsedJson = try {
                        JSONObject(cleanedValue)
                    } catch (e: Exception) {
                        // If it's not valid JSON, just use the cleaned string
                        null
                    }

                    if (parsedJson != null) {
                        // If it's valid JSON, use the compact string representation
                        cleanedJson.put(key, parsedJson.toString())
                        AppLogger.d("✅ Cleaned field '$key': $cleanedValue")
                    } else {
                        // Not JSON, use as-is
                        cleanedJson.put(key, cleanedValue)
                    }
                } catch (e: Exception) {
                    AppLogger.e("❌ Error cleaning field '$key': ${e.message}")
                    cleanedJson.put(key, value)
                }
            } else {
                // Not a JSON string field, copy as-is
                cleanedJson.put(key, value)
            }
        }

        return cleanedJson
    }

    fun createJsonTableNameMapping(globalIdESPB: List<Int>): String {
        val tableMap = mapOf(
            AppUtils.DatabaseTables.ESPB to globalIdESPB // Use the passed parameter
        )
        return Gson().toJson(tableMap) // Convert to JSON string
    }


}

// Fetch TPH by ID

