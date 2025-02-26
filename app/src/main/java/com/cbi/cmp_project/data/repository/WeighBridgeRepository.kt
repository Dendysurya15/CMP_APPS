package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.api.ApiService
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.network.Constants
import com.cbi.cmp_project.data.network.StagingApiClient
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.markertph.data.model.TPHNewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

class WeighBridgeRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val tphDao = database.tphDao()
    private val karyawanDao = database.karyawanDao()
    private val espbDao = database.espbDao()

    suspend fun getMill(millId: Int): List<MillModel> {
        return millDao.getMillById(millId)
    }

    suspend fun getTransporter(transporterId: Int): List<TransporterModel> {
        return transporterDao.getTransporterById(transporterId)
    }

    suspend fun deleteESPBByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        espbDao.deleteByListID(ids)
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

    suspend fun getActiveESPB(): List<ESPBEntity> = withContext(Dispatchers.IO) {
        espbDao.getAllActive()
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

    private suspend fun updateUploadStatus(id: Int, statusUploadPpro:Int, uploaderInfo:String, uploaderAt:String, uploadedById:Int ) {
        espbDao.updateUploadStatus(id, statusUploadPpro, uploaderInfo, uploaderAt, uploadedById)
    }

    // Create a data class to hold error information
    data class UploadError(
        val itemId: Int,
        val errorMessage: String,
        val errorType: String
    )

    suspend fun uploadESPBStagingKraniTimbang(
        dataList: List<Map<String, Any>>,
        onProgressUpdate: (Int, Int, Boolean, String?) -> Unit // itemId, progress, isSuccess, errorMsg
    ): Result<String>? {
        return try {
            withContext(Dispatchers.IO) {
                val results = mutableMapOf<Int, Boolean>()
                val errors = mutableListOf<UploadError>()

                AppLogger.d(dataList.toString())
                for (item in dataList) {
                    val itemId = item["id"] as Int
                    val uploaderInfo = item["uploader_info"] as String
                    val uploadedAt = item["uploaded_at"] as String
                    val uploadedById = item["uploaded_by_id"] as Int

                    AppLogger.d(uploaderInfo)
                    AppLogger.d(uploadedAt)
                    AppLogger.d(uploadedById.toString())
                    var errorMessage: String? = null

                    try {
                        onProgressUpdate(itemId, 10, false, null)
                        delay(500)

                        val data = try {
                            ApiService.dataUploadEspbKraniTimbang(
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

                        onProgressUpdate(itemId, 50, false, null)

                        try {
                            withTimeout(Constants.NETWORK_TIMEOUT_MS) {
                                try {
                                    val response = StagingApiClient.instance.insertESPBKraniTimbang(data)

                                    if (response.isSuccessful) {
                                        val responseBody = response.body()

                                        if (responseBody != null && responseBody.status == 1) {
                                            results[itemId] = true
                                            onProgressUpdate(itemId, 100, true, null)

                                            try {
                                                updateUploadStatus(
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

                                            AppLogger.e("APIError Item ID: $itemId - $rawErrorMessage")
                                            errors.add(UploadError(itemId, extractedMessage, "API_ERROR"))
                                            results[itemId] = false
                                            onProgressUpdate(itemId, 100, false, extractedMessage)
                                        }
                                    } else {
                                        errorMessage = response.errorBody()?.string() ?: "Server error: ${response.code()}"
                                        AppLogger.e("ServerError Item ID: $itemId - $errorMessage")
                                        errors.add(UploadError(itemId, errorMessage!!, "SERVER_ERROR"))
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
                    } catch (e: Exception) {
                        errorMessage = "Unknown error: ${e.message}"
                        AppLogger.e("UnknownError Item ID: $itemId - $errorMessage")
                        errors.add(UploadError(itemId, errorMessage!!, "UNKNOWN_ERROR"))
                        results[itemId] = false
                        onProgressUpdate(itemId, 100, false, errorMessage)
                    }
                }

                val allSucceeded = results.values.all { it }
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



}


