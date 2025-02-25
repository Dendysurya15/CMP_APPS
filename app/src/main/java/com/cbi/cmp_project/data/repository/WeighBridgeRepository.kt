package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.api.ApiService
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.model.weighBridge.UploadStagingResponse
import com.cbi.cmp_project.data.network.CMPApiClient
import com.cbi.cmp_project.data.network.Constants
import com.cbi.cmp_project.data.network.StagingApiClient
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.markertph.data.model.TPHNewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

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


    suspend fun uploadESPBStagingKraniTimbang(dataList: List<Map<String, Any>>): Result<String>? {
        return try {
            withContext(Dispatchers.IO) {
                val uploadData = dataList.map { item ->
                    ApiService.dataUploadEspbKraniTimbang(
                        dept_ppro = item["dept_ppro"].toString(),
                        divisi_ppro = item["divisi_ppro"].toString(),
                        commodity = item["commodity"].toString(),
                        blok_jjg = item["blok_jjg"].toString(),
                        nopol = item["nopol"].toString(),
                        driver = item["driver"].toString(),
                        pemuat_id = item["pemuat_id"].toString(),
                        transporter_id = item["transporter_id"].toString(),
                        mill_id = item["mill_id"].toString(),
                        created_by_id = item["created_by_id"].toString(),
                        created_at = item["created_at"].toString(),
                        no_espb = item["no_espb"].toString()
                    )
                }

                AppLogger.d("uploadData $uploadData")

                for (data in uploadData) {
                    try {
                        withTimeout(Constants.NETWORK_TIMEOUT_MS) {
                            AppLogger.d("Request JSON: ${Gson().toJson(data)}")
                            val response = StagingApiClient.instance.insertESPBKraniTimbang(data)
                            AppLogger.d("Upload Request Body: ${Gson().toJson(data)}")
                            AppLogger.d("Raw Response: $response")

                            if (response.isSuccessful) {
                                val body = response.body()

                                if (body != null) {
                                    // The body is already an object, so we can safely cast it
                                    val uploadResponse = body as UploadStagingResponse
                                    AppLogger.d("Parsed Response: $uploadResponse")

                                    if (uploadResponse.status == 1) {
                                        val messageStr = uploadResponse.message.toString()
                                        AppLogger.d("Upload Success: $messageStr")
                                    } else {
                                        val messageStr = uploadResponse.message.toString()
                                        AppLogger.e("Upload Failed: $messageStr")

                                        // Extract the actual error message if possible
                                        var detailedError = messageStr
                                        if (uploadResponse.message is Map<*, *>) {
                                            val messageMap = uploadResponse.message as Map<*, *>
                                            if (messageMap.containsKey("originalError")) {
                                                val originalError = messageMap["originalError"] as? Map<*, *>
                                                if (originalError?.containsKey("info") == true) {
                                                    val info = originalError["info"] as? Map<*, *>
                                                    if (info?.containsKey("message") == true) {
                                                        detailedError = info["message"].toString()
                                                    }
                                                }
                                            }
                                        }

                                        throw Exception("Upload failed: $detailedError")
                                    }
                                } else {
                                    AppLogger.e("Upload Failed: Response body is null")
                                    throw Exception("Upload failed: Null response body")
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                AppLogger.e("Upload Error: Code ${response.code()} - Body: $errorBody")
                                throw Exception("HTTP Error: ${response.code()} - $errorBody")
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        AppLogger.e("Upload Error: Timeout on data ${Gson().toJson(data)}")
                        throw Exception("Timeout occurred for data: ${Gson().toJson(data)}")
                    } catch (e: Exception) {
                        AppLogger.e("Upload Error: Unexpected issue - ${e.message}")
                        throw e // Propagate the exception to be caught by the ViewModel
                    }
                }

                Result.success("All data uploaded successfully.")
            }
        } catch (e: Exception) {
            AppLogger.e("Upload Error: Unexpected issue - ${e.message}")
            Result.failure(e)
        }
    }

}


