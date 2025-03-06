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
    suspend fun insertESPBData(espbData: ESPBEntity) {
        espbDao.insertESPBData(espbData)
    }

    sealed class SaveResultESPBKrani {
        object Success : SaveResultESPBKrani()
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
                    val itemId = item["id"] as Int
                    val uploaderInfo = item["uploader_info"] as String
                    val uploadedAt = item["uploaded_at"] as String
                    val uploadedById = item["uploaded_by_id"] as Int
                    AppLogger.d("Processing item ID: $itemId, Endpoint: $endpoint")

                    var errorMessage: String? = null

                    try {
                        onProgressUpdate(itemId, 10, false, null)
                        delay(500)

                        // Handle PPRO data upload
                        if (endpoint == "PPRO") {
                            idsESPB.add(itemId)
                            val data = try {
                                ApiService.dataUploadEspbKraniTimbangPPRO(
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
                            } catch (e: Exception) {
                                errorMessage = "Data error: ${e.message}"
                                AppLogger.e("DataError Item ID: $itemId - $errorMessage")
                                errors.add(UploadError(itemId, errorMessage, "DATA_ERROR"))
                                results[itemId] = false
                                onProgressUpdate(itemId, -1, false, errorMessage)
                                continue
                            }

                            AppLogger.d("Data prepared for item ID: $itemId -> $data")
                            onProgressUpdate(itemId, 50, false, null)

                            try {
                                withTimeout(Constants.NETWORK_TIMEOUT_MS) {
                                    try {
                                        val response =
                                            StagingApiClient.instance.insertESPBKraniTimbangPPRO(data)

                                        if (response.isSuccessful) {
                                            val responseBody = response.body()

                                            if (responseBody != null && responseBody.status == 1) {
                                                results[itemId] = true
                                                onProgressUpdate(itemId, 100, true, null)

                                                try {
                                                    updateUploadStatusPPRO(
                                                        itemId,
                                                        1,
                                                        uploaderInfo,
                                                        uploadedAt,
                                                        uploadedById
                                                    )
                                                    AppLogger.d("espb table dengan id $itemId has been updated")
                                                } catch (e: Exception) {
                                                    AppLogger.e("Failed to update espb table for Item ID: $itemId - ${e.message}")
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

                                                AppLogger.e("APIError Item ID: $itemId - $rawErrorMessage")
                                                errors.add(
                                                    UploadError(
                                                        itemId,
                                                        extractedMessage,
                                                        "API_ERROR"
                                                    )
                                                )
                                                results[itemId] = false
                                                onProgressUpdate(itemId, 100, false, extractedMessage)
                                            }
                                        } else {
                                            errorMessage = response.errorBody()?.string()
                                                ?: "Server error: ${response.code()}"
                                            AppLogger.e("ServerError Item ID: $itemId - $errorMessage")
                                            errors.add(
                                                UploadError(
                                                    itemId,
                                                    errorMessage!!,
                                                    "SERVER_ERROR"
                                                )
                                            )
                                            results[itemId] = false
                                            onProgressUpdate(itemId, 100, false, errorMessage)
                                        }
                                    } catch (e: IOException) {
                                        errorMessage = "Network error: ${e.message}"
                                        AppLogger.e("NetworkError Item ID: $itemId - $errorMessage")
                                        errors.add(UploadError(itemId, errorMessage!!, "NETWORK_ERROR"))
                                        results[itemId] = false
                                        onProgressUpdate(itemId, 100, false, errorMessage)
                                    } catch (e: Exception) {
                                        errorMessage = "API error: ${e.message}"
                                        AppLogger.e("APIError Item ID: $itemId - $errorMessage")
                                        errors.add(UploadError(itemId, errorMessage!!, "API_ERROR"))
                                        results[itemId] = false
                                        onProgressUpdate(itemId, 100, false, errorMessage)
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                errorMessage = "Request timed out"
                                AppLogger.e("TimeoutError Item ID: $itemId - $errorMessage")
                                errors.add(UploadError(itemId, errorMessage!!, "TIMEOUT_ERROR"))
                                results[itemId] = false
                                onProgressUpdate(itemId, 100, false, errorMessage)
                            }

                        }
                        // Handle CMP (ZIP file) upload
                        else if (endpoint == "CMP") {
                            val filePath = item["file"] as? String
                            if (filePath.isNullOrEmpty()) {
                                errorMessage = "File path is missing for item ID: $itemId"
                                AppLogger.e(errorMessage!!)
                                errors.add(UploadError(itemId, errorMessage!!, "FILE_ERROR"))
                                results[itemId] = false
                                onProgressUpdate(itemId, 100, false, errorMessage)
                                continue
                            }

                            val file = File(filePath)
                            if (!file.exists() || !file.isFile) {
                                errorMessage = "File not found or invalid: $filePath"
                                AppLogger.e(errorMessage!!)
                                errors.add(UploadError(itemId, errorMessage!!, "INVALID_FILE"))
                                results[itemId] = false
                                onProgressUpdate(itemId, 100, false, errorMessage)
                                continue
                            }

                            try {

                                val progressRequestBody = AppUtils.ProgressRequestBody(
                                    file,
                                    "application/zip"
                                ) { progress ->
                                    AppLogger.d("Upload progress: $progress%")
                                    onProgressUpdate(progress, 50, true, null)
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


                                    results[itemId] = true
                                    onProgressUpdate(itemId, 100, true, null)
                                }

                                else {
                                    errorMessage = "ZIP upload failed: ${response.message()}"
                                    AppLogger.e("ZipUploadError Item ID: $itemId - $errorMessage")
                                    errors.add(
                                        UploadError(
                                            itemId,
                                            errorMessage!!,
                                            "ZIP_UPLOAD_ERROR"
                                        )
                                    )
                                    results[itemId] = false
                                    onProgressUpdate(itemId, 100, false, errorMessage)
                                }
                            } catch (e: Exception) {
                                errorMessage = "ZIP upload error: ${e.message}"
                                AppLogger.e("ZipUploadError Item ID: $itemId - $errorMessage")
                                errors.add(UploadError(itemId, errorMessage!!, "ZIP_UPLOAD_ERROR"))
                                results[itemId] = false
                                onProgressUpdate(itemId, 100, false, errorMessage)
                                continue
                            }
                        }
                        // Unknown endpoint
                        else {
                            errorMessage = "Unknown endpoint for item ID: $itemId -> $endpoint"
                            AppLogger.e(errorMessage!!)
                            errors.add(UploadError(itemId, errorMessage!!, "UNKNOWN_ENDPOINT"))
                            results[itemId] = false
                            onProgressUpdate(itemId, 100, false, errorMessage)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Unknown error: ${e.message}"
                        AppLogger.e("UnknownError Item ID: $itemId - $errorMessage")
                        errors.add(UploadError(itemId, errorMessage!!, "UNKNOWN_ERROR"))
                        results[itemId] = false
                        onProgressUpdate(itemId, 100, false, errorMessage)
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

