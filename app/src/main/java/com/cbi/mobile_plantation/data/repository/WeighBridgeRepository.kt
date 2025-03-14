package com.cbi.mobile_plantation.data.repository

import android.content.Context
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
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException

@Suppress("UNREACHABLE_CODE")
class WeighBridgeRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val tphDao = database.tphDao()
    private val karyawanDao = database.karyawanDao()
    private val espbDao = database.espbDao()
    private val uploadCMPDao = database.uploadCMPDao()

    suspend fun getMill(millId: Int): List<MillModel> {
        return millDao.getMillById(millId)
    }

    suspend fun getTransporter(transporterId: Int): List<TransporterModel> {
        return transporterDao.getTransporterById(transporterId)
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
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

    suspend fun getActiveESPBByIds(ids: List<Int>): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
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
        uploadedById: Int
    ) {
        espbDao.updateUploadStatusPPRO(id, statusUploadPpro, uploaderInfo, uploaderAt, uploadedById)
    }

    private suspend fun updateUploadStatusCMP(
        id: Int,
        statusUploadCMP: Int,
        uploaderInfo: String,
        uploaderAt: String,
        uploadedById: Int
    ) {
        espbDao.updateUploadStatusCMP(id, statusUploadCMP, uploaderInfo, uploaderAt, uploadedById)
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
        globalIdESPB : List<Int>,
        onProgressUpdate: (Int, Int, Boolean, String?) -> Unit // itemId, progress, isSuccess, errorMsg
    ): Result<String>? {
        return try {
            withContext(Dispatchers.IO) {
                val results = mutableMapOf<Int, Boolean>()
                val errors = mutableListOf<UploadError>()
                val idsESPB = mutableListOf<Int>() // ✅ Define list outside loop
                AppLogger.d("Starting upload for ${dataList.size} items")
                AppLogger.d(globalIdESPB.toString())
                AppLogger.d("Starting upload for ${dataList.size} items")
                AppLogger.d(dataList.toString())

                for (item in dataList) {
                    val endpoint = item["endpoint"] as String
                    val num = item["num"] as Int
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
                                val data = try {
                                    val result = ApiService.dataUploadEspbKraniTimbangPPRO(
                                        dept_ppro = (item["dept_ppro"] ?: "0").toString(),
                                        divisi_ppro = (item["divisi_ppro"] ?: "0").toString(),
                                        commodity = (item["commodity"] ?: "0").toString(),
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
                                    onProgressUpdate(num, -1, false, errorMessage)
                                    continue
                                }

                                AppLogger.d("PPRO: Data prepared for item ID: $num -> $data")
                                onProgressUpdate(num, 50, false, null)

                                try {
                                    AppLogger.d("PPRO: Making API call to StagingApiClient.insertESPBKraniTimbangPPRO")
                                    val response = StagingApiClient.instance.insertESPBKraniTimbangPPRO(data)
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
                                                    uploadedById
                                                )
                                                AppLogger.d("PPRO: espb table dengan id $itemId has been updated")
                                            } catch (e: Exception) {
                                                AppLogger.e("PPRO: Failed to update espb table for Item ID: $itemId - ${e.message}")
                                            }
                                        } else {
                                            val rawErrorMessage = responseBody?.message?.toString() ?: "No message provided"
                                            val extractedMessage = if (rawErrorMessage.contains("message=")) {
                                                try {
                                                    // Extract the actual error message between "message=" and the next comma or period
                                                    val startIndex = rawErrorMessage.indexOf("message=") + "message=".length
                                                    val endIndex = rawErrorMessage.indexOf(",", startIndex).takeIf { it > 0 }
                                                        ?: rawErrorMessage.indexOf(".", startIndex).takeIf { it > 0 }
                                                        ?: rawErrorMessage.length

                                                    rawErrorMessage.substring(startIndex, endIndex).trim()
                                                } catch (e: Exception) {
                                                    // If parsing fails, use the original error
                                                    "API Error: ${rawErrorMessage.take(100)}"
                                                }
                                            } else {
                                                "API Error: ${rawErrorMessage.take(100)}"
                                            }

                                            AppLogger.e("PPRO: APIError Item ID: $itemId - $rawErrorMessage")
                                            errors.add(UploadError(num, extractedMessage, "API_ERROR"))
                                            results[num] = false
                                            onProgressUpdate(num, 100, false, extractedMessage)
                                        }
                                    } else {
                                        errorMessage = response.errorBody()?.string() ?: "Server error: ${response.code()}"
                                        AppLogger.e("PPRO: ServerError Item ID: $num - $errorMessage")
                                        errors.add(UploadError(num, errorMessage!!, "SERVER_ERROR"))
                                        results[num] = false
                                        onProgressUpdate(num, 100, false, errorMessage)
                                    }
                                } catch (e: IOException) {
                                    AppLogger.e("PPRO: Network error: ${e.message}")
                                    AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                    errorMessage = "Network error: ${e.message}"
                                    AppLogger.e("PPRO: NetworkError Item ID: $num - $errorMessage")
                                    errors.add(UploadError(num, errorMessage!!, "NETWORK_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                } catch (e: Exception) {
                                    AppLogger.e("PPRO: Exception during API call: ${e.javaClass.simpleName} - ${e.message}")
                                    AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                    errorMessage = "API error: ${e.message}"
                                    AppLogger.e("PPRO: APIError Item ID: $num - $errorMessage")
                                    errors.add(UploadError(num, errorMessage!!, "API_ERROR"))
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                }
                            } catch (e: Exception) {
                                AppLogger.e("PPRO: Top-level exception in PPRO block: ${e.javaClass.simpleName} - ${e.message}")
                                AppLogger.e("PPRO: Stack trace: ${e.stackTraceToString()}")
                                errorMessage = "Fatal error in PPRO upload: ${e.message}"
                                errors.add(UploadError(num, errorMessage!!, "FATAL_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                            }
                        }
                        // Handle CMP (ZIP file) upload
                        else if (endpoint == "CMP") {

                            onProgressUpdate(num, 10, false, null)
                            val filePath = item["file"] as? String
                            if (filePath.isNullOrEmpty()) {
                                errorMessage = "File .zip is missing or error when generating .zip"
                                AppLogger.e(errorMessage!!)
                                errors.add(UploadError(num, errorMessage!!, "FILE_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                                continue
                            }

                            val file = File(filePath)
                            if (!file.exists() || !file.isFile) {
                                errorMessage = "File not found or invalid: $filePath"
                                AppLogger.e(errorMessage!!)
                                errors.add(UploadError(num, errorMessage!!, "INVALID_FILE"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                                continue
                            }

                            try {

                                val progressRequestBody = AppUtils.ProgressRequestBody(
                                    file,
                                    "application/zip"
                                ) { progress ->
                                    AppLogger.d("Upload progress: $progress%")
                                    onProgressUpdate(num, progress, false, null)  // Use itemId as first param, progress as second
                                }
                                val filePart = MultipartBody.Part.createFormData(
                                    "zipFile",
                                    file.name,
                                    progressRequestBody
                                )

                                val response = CMPApiClient.instance.uploadZip(filePart)

                                if (response.isSuccessful) {
                                    val responseBody = response.body()


                                    responseBody?.let {
                                        val jsonResultTableIds = createJsonTableNameMapping(globalIdESPB) // Pass globalIdESPB

                                        val uploadData = UploadCMPModel(
                                            tracking_id = it.trackingId,
                                            nama_file = it.nama_file,
                                            status = it.status,
                                            tanggal_upload = it.tanggal_upload,
                                            table_ids = jsonResultTableIds
                                        )

                                        withContext(Dispatchers.IO) {
                                            val existingCount = uploadCMPDao.getTrackingIdCount(uploadData.tracking_id!!)

                                            if (existingCount > 0) {
                                                uploadCMPDao.updateStatus(uploadData.tracking_id, uploadData.status!!)
                                            } else {
                                                uploadCMPDao.insertNewData(uploadData)
                                            }
                                        }

                                        delay(100) // Small delay before the next operation
                                    }

                                    // update espb id untuk status_cmp_upload = 1
                                    for (id in idsESPB) {
                                        try {
                                            withContext(Dispatchers.IO) { // Ensures it runs in background & waits
                                                updateUploadStatusCMP(
                                                    id, // ✅ Replace itemId with id from idsESPB
                                                    1,
                                                    uploaderInfo,
                                                    uploadedAt,
                                                    uploadedById
                                                )
                                            }
                                            AppLogger.d("ESPB table dengan id $id has been updated")
                                        } catch (e: Exception) {
                                            AppLogger.e("Failed to update ESPB table for Item ID: $id - ${e.message}")
                                        }
                                    }


                                    results[num] = true
                                    onProgressUpdate(num, 100, true, null)
                                }

                                else {
                                    errorMessage = "ZIP upload failed: ${response.message()}"
                                    AppLogger.e("ZipUploadError Item ID: $num - $errorMessage")
                                    errors.add(
                                        UploadError(
                                            num,
                                            errorMessage!!,
                                            "ZIP_UPLOAD_ERROR"
                                        )
                                    )
                                    results[num] = false
                                    onProgressUpdate(num, 100, false, errorMessage)
                                }
                            } catch (e: Exception) {
                                errorMessage = "ZIP upload error: ${e.message}"
                                AppLogger.e("ZipUploadError Item ID: $num - $errorMessage")
                                errors.add(UploadError(num, errorMessage!!, "ZIP_UPLOAD_ERROR"))
                                results[num] = false
                                onProgressUpdate(num, 100, false, errorMessage)
                                continue
                            }
                        }
                        // Unknown endpoint
                        else {
                            errorMessage = "Unknown endpoint for item ID: $itemId -> $endpoint"
                            AppLogger.e(errorMessage!!)
                            errors.add(UploadError(num, errorMessage!!, "UNKNOWN_ENDPOINT"))
                            results[num] = false
                            onProgressUpdate(itemId, 100, false, errorMessage)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Unknown error: ${e.message}"
                        AppLogger.e("UnknownError Item ID: $num - $errorMessage")
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

