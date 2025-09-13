package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.PhotoResult
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network._root_ide_package_.com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.StagingApiClient
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    private val inspeksiDao = database.inspectionDao()
    private val mutuBuahDao = database.mutuBuahDao()


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


    data class ImageFileInfo(
        val file: File,
        val imageName: String,
        val tableId: String,
        val basePath: String,
        val databaseTable: String,
        val anotherDatabaseTable: String
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

                        // Calculate progress steps - each image gets equal weight
                        val progressPerImage =
                            90.0 / imageList.size // Reserve 10% for initial setup
                        var currentProgress = 10.0  // Start at 10%

                        // Validate all files exist first
                        val validImageFiles = mutableListOf<ImageFileInfo>()
                        val failedFiles = mutableListOf<String>()

                        AppLogger.d("====== CHECKING FILE EXISTENCE ======")


                        AppLogger.d("imageList $imageList")

                        imageList.forEachIndexed { index, imageData ->
                            val imagePath = imageData["path"] ?: ""
                            val imageName = imageData["name"] ?: ""
                            val tableId = imageData["table_ids"]?.toIntOrNull() ?: -1
                            val table = imageData["table"]?.toString()
                            val basePathImage = imageData["base_path"]
                            val databaseTable = imageData["database"]
                            AppLogger.d("Checking file ${index + 1}/${imageList.size}: $imageName - Table ID: $tableId")

                            val file = File(imagePath)
                            if (file.exists()) {
                                validImageFiles.add(
                                    ImageFileInfo(
                                        file, imageName, tableId.toString(),
                                        basePathImage!!, databaseTable!!, table ?: ""
                                    )
                                )
                                AppLogger.d("✓ File exists: $imageName")
                            } else {
                                failedFiles.add(imageName)
                                AppLogger.e("✗ File not found: $imageName")

                                // Update progress for missing file
                                currentProgress += progressPerImage
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(
                                        currentProgress.toInt(),
                                        false,
                                        "✗ File not found: $imageName"
                                    )
                                }

                                // Update status to error for file not found
                                if (tableId != -1) {
                                    val errorJson = JsonObject().apply {
                                        add("error", JsonArray().apply {
                                            add(imageName)
                                        })
                                    }.toString()

                                    // Check if table field exists, otherwise use databaseTable
                                    val targetTable =
                                        if (!table.isNullOrEmpty()) table else databaseTable

                                    when (targetTable) {
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

                                        AppUtils.DatabaseTables.MUTU_BUAH -> {
                                            mutuBuahDao.updateStatusUploadedImage(
                                                listOf(tableId),
                                                errorJson
                                            )
                                        }

                                        "${AppUtils.DatabaseTables.MUTU_BUAH}_selfie" -> {
                                            mutuBuahDao.updateStatusUploadedImageSelfie(
                                                listOf(tableId),
                                                errorJson
                                            )
                                        }

                                        AppUtils.DatabaseTables.INSPEKSI, AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {
                                            // Handle inspeksi watermark cases
                                            when (databaseTable) {
                                                AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> {
                                                    inspeksiDao.updateStatusUploadedImageFotoUser(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> {
                                                    inspeksiDao.updateStatusUploadedImageFotoUserPemulihan(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> {
                                                    inspeksiDao.updateStatusUploadedImageInspeksi(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> {
                                                    inspeksiDao.updateStatusUploadedImageInspeksi(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> {
                                                    inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> {
                                                    inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                        listOf(tableId),
                                                        errorJson
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    AppLogger.d("Updated status_uploaded_image for table ID $tableId in table $targetTable with error: $errorJson")
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

                            try {

//                                if (index == 0) {
//                                    throw SocketTimeoutException("Connection timeout - simulated error")
//                                }

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
                                    AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok
                                    AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH
                                    AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser
                                    AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser
                                    AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH
                                    AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok
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


                                AppLogger.d("photoPart $photoPart")
                                AppLogger.d("datasetTypeRequestBody $datasetTypeRequestBody")
                                // Make the API call for single image
                                val response = CMPApiClient.instance.uploadPhotos(
                                    photos = listOf(photoPart),
                                    datasetType = datasetTypeRequestBody,
                                    path = basePathRequestBody
                                )

                                AppLogger.d("kfjalksjdfl asdfklj asldfk")

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

                                        AppLogger.d("tableIdInt $tableIdInt")
                                        if (tableIdInt != -1) {
                                            val targetTable =
                                                if (!fileInfo.anotherDatabaseTable.isNullOrEmpty()) fileInfo.anotherDatabaseTable else fileInfo.databaseTable
                                            AppLogger.d("Target table resolved to: $targetTable")
                                            AppLogger.d("Database table: ${fileInfo.databaseTable}")
                                            AppLogger.d("Another database table: ${fileInfo.anotherDatabaseTable}")

                                            when (targetTable) {
                                                AppUtils.DatabaseTables.PANEN -> {
                                                    AppLogger.d("Updating PANEN status_uploaded_image to 200 for ID $tableIdInt")
                                                    panenDao.updateStatusUploadedImage(
                                                        listOf(tableIdInt),
                                                        "200"
                                                    )
                                                }

                                                AppUtils.DatabaseTables.ABSENSI -> {
                                                    AppLogger.d("Updating ABSENSI status_uploaded_image to 200 for ID $tableIdInt")
                                                    absensiDao.updateStatusUploadedImage(
                                                        listOf(tableIdInt),
                                                        "200"
                                                    )
                                                }

                                                AppUtils.DatabaseTables.MUTU_BUAH -> {
                                                    mutuBuahDao.updateStatusUploadedImage(
                                                        listOf(tableIdInt),
                                                        "200"
                                                    )
                                                }

                                                "${AppUtils.DatabaseTables.MUTU_BUAH}_selfie" -> {
                                                    mutuBuahDao.updateStatusUploadedImageSelfie(
                                                        listOf(tableIdInt),
                                                        "200"
                                                    )
                                                }

                                                AppUtils.DatabaseTables.INSPEKSI, AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {
                                                    AppLogger.d("INSPEKSI or INSPEKSI_DETAIL table, determining watermark type: ${databaseTable}")
                                                    when (fileInfo.databaseTable) {
                                                        AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> {
                                                            AppLogger.d("Updating FotoUser for WMBuktiInspeksiUser ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImageFotoUser(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> {
                                                            AppLogger.d("Updating FotoUserPemulihan for WMBuktiFUInspeksiUser ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImageFotoUserPemulihan(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> {
                                                            AppLogger.d("Updating Inspeksi for WMInspeksiTPH ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImageInspeksi(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> {
                                                            AppLogger.d("Updating Inspeksi for WMInspeksiPokok ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImageInspeksi(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> {
                                                            AppLogger.d("Updating PemulihanInspeksi for WMFUInspeksiTPH ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> {
                                                            AppLogger.d("Updating PemulihanInspeksi for WMFUInspeksiPokok ID $tableIdInt")
                                                            inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                                listOf(tableIdInt),
                                                                "200"
                                                            )
                                                        }

                                                        else -> {
                                                            AppLogger.e("Unknown databaseTable inside INSPEKSI: $databaseTable")
                                                        }
                                                    }
                                                }

                                                else -> {
                                                    AppLogger.e("Unknown targetTable: $targetTable")
                                                }
                                            }
                                        }


                                        // Update progress for successful upload
                                        currentProgress += progressPerImage
                                        withContext(Dispatchers.Main) {
                                            onProgressUpdate(
                                                currentProgress.toInt(),
                                                false,
                                                "✓ ${fileInfo.imageName} uploaded successfully"
                                            )
                                        }
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
                                            val targetTable =
                                                if (!fileInfo.anotherDatabaseTable.isNullOrEmpty()) fileInfo.anotherDatabaseTable else fileInfo.databaseTable

                                            when (targetTable) {
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

                                                AppUtils.DatabaseTables.MUTU_BUAH -> {
                                                    mutuBuahDao.updateStatusUploadedImage(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }
                                                "${AppUtils.DatabaseTables.MUTU_BUAH}_selfie" -> {
                                                    mutuBuahDao.updateStatusUploadedImageSelfie(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.DatabaseTables.INSPEKSI, AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {

                                                    when (databaseTable) {
                                                        AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> {
                                                            inspeksiDao.updateStatusUploadedImageFotoUser(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> {
                                                            inspeksiDao.updateStatusUploadedImageFotoUserPemulihan(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> {
                                                            inspeksiDao.updateStatusUploadedImageInspeksi(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> {
                                                            inspeksiDao.updateStatusUploadedImageInspeksi(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> {
                                                            inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }

                                                        AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> {
                                                            inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                                listOf(tableIdInt),
                                                                errorJson
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        failedImagePaths.add(fileInfo.file.absolutePath)
                                        failedImageNames.add(fileInfo.imageName)

                                        // Update progress for failed upload
                                        currentProgress += progressPerImage
                                        withContext(Dispatchers.Main) {
                                            onProgressUpdate(
                                                currentProgress.toInt(),
                                                false,
                                                "✗ ${fileInfo.imageName} failed - null response"
                                            )
                                        }
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
                                        val targetTable =
                                            if (!fileInfo.anotherDatabaseTable.isNullOrEmpty()) fileInfo.anotherDatabaseTable else fileInfo.databaseTable

                                        when (targetTable) {
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

                                            AppUtils.DatabaseTables.MUTU_BUAH -> {
                                                mutuBuahDao.updateStatusUploadedImage(
                                                    listOf(tableIdInt),
                                                    errorJson
                                                )
                                            }
                                            "${AppUtils.DatabaseTables.MUTU_BUAH}_selfie" -> {
                                                mutuBuahDao.updateStatusUploadedImageSelfie(
                                                    listOf(tableIdInt),
                                                    errorJson
                                                )
                                            }

                                            AppUtils.DatabaseTables.INSPEKSI, AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {

                                                when (databaseTable) {
                                                    AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> {
                                                        inspeksiDao.updateStatusUploadedImageFotoUser(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }

                                                    AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> {
                                                        inspeksiDao.updateStatusUploadedImageFotoUserPemulihan(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }

                                                    AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> {
                                                        inspeksiDao.updateStatusUploadedImageInspeksi(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }

                                                    AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> {
                                                        inspeksiDao.updateStatusUploadedImageInspeksi(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }

                                                    AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> {
                                                        inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }

                                                    AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> {
                                                        inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                            listOf(tableIdInt),
                                                            errorJson
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    failedImagePaths.add(fileInfo.file.absolutePath)
                                    failedImageNames.add(fileInfo.imageName)

                                    // Update progress for failed upload
                                    currentProgress += progressPerImage

                                    AppLogger.d("currentProgress $currentProgress")
                                    withContext(Dispatchers.Main) {
                                        onProgressUpdate(
                                            currentProgress.toInt(),
                                            false,
                                            "✗ ${fileInfo.imageName} failed - ${response.code()}"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Exception uploading ${fileInfo.imageName}: ${e.message}")
                                failureCount++

                                // Update status with exception
                                val tableIdInt = fileInfo.tableId.toIntOrNull() ?: -1
                                if (tableIdInt != -1) {
                                    val errorJson = JsonObject().apply {
                                        add("error", JsonArray().apply {
                                            add(fileInfo.imageName)
                                        })
                                    }.toString()
                                    val targetTable =
                                        if (!fileInfo.anotherDatabaseTable.isNullOrEmpty()) fileInfo.anotherDatabaseTable else fileInfo.databaseTable

                                    when (targetTable) {
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

                                        AppUtils.DatabaseTables.MUTU_BUAH -> {
                                            mutuBuahDao.updateStatusUploadedImage(
                                                listOf(tableIdInt),
                                                errorJson
                                            )
                                        }
                                        "${AppUtils.DatabaseTables.MUTU_BUAH}_selfie" -> {
                                            mutuBuahDao.updateStatusUploadedImageSelfie(
                                                listOf(tableIdInt),
                                                errorJson
                                            )
                                        }
                                        AppUtils.DatabaseTables.INSPEKSI, AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {

                                            when (databaseTable) {
                                                AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser -> {
                                                    inspeksiDao.updateStatusUploadedImageFotoUser(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser -> {
                                                    inspeksiDao.updateStatusUploadedImageFotoUserPemulihan(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH -> {
                                                    inspeksiDao.updateStatusUploadedImageInspeksi(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok -> {
                                                    inspeksiDao.updateStatusUploadedImageInspeksi(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH -> {
                                                    inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }

                                                AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok -> {
                                                    inspeksiDao.updateStatusUploadedImagePemulihanInspeksi(
                                                        listOf(tableIdInt),
                                                        errorJson
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                failedImagePaths.add(fileInfo.file.absolutePath)
                                failedImageNames.add(fileInfo.imageName)

                                // Update progress for exception
                                currentProgress += progressPerImage
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(
                                        currentProgress.toInt(),
                                        false,
                                        "✗ ${fileInfo.imageName} error - ${e.message}"
                                    )
                                }
                            }
                        }

                        // Final update with complete status - only called once after ALL uploads are done
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

                        // Final progress update to 100% - only when ALL images are processed
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
                        AppLogger.d("kajdfjslkdaflk sadflkds fksd")
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

                    AppLogger.d("databaseTable $databaseTable")
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

                        val response = CMPApiClient.instance.uploadJsonV3Raw(
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

                                // Check for special mutu_buah case with skipped records
                                val isMutuBuahWithSkipped = databaseTable == "mutu_buah" &&
                                        responseBody.results?.skipped != null &&
                                        responseBody.results.skipped > 0

                                // Filter table_ids for mutu_buah if there are skipped records
                                val filteredTableIds = if (isMutuBuahWithSkipped && tableIds != null) {
                                    try {
                                        // Parse the tableIds JSON string
                                        val originalTableIdsJson = JSONObject(tableIds)

                                        // Get the skipped record IDs from skipErrorDetails
                                        val skippedIds = mutableSetOf<Int>()
                                        responseBody.results?.skipErrorDetails?.forEach { skipDetail ->
                                            skipDetail.data?.let { data ->
                                                // Extract the ID from the data object
                                                val dataJson = JSONObject(data.toString())
                                                if (dataJson.has("id")) {
                                                    skippedIds.add(dataJson.getInt("id"))
                                                }
                                            }
                                        }

                                        // Filter mutu_buah IDs by removing skipped ones
                                        if (originalTableIdsJson.has("mutu_buah")) {
                                            val originalMutuBuahIdsArray = originalTableIdsJson.getJSONArray("mutu_buah")
                                            val originalMutuBuahIds = (0 until originalMutuBuahIdsArray.length()).map {
                                                originalMutuBuahIdsArray.getInt(it)
                                            }

                                            val filteredMutuBuahIds = originalMutuBuahIds.filter { id -> !skippedIds.contains(id) }

                                            AppLogger.d("Original mutu_buah IDs: $originalMutuBuahIds")
                                            AppLogger.d("Skipped IDs: $skippedIds")
                                            AppLogger.d("Filtered mutu_buah IDs: $filteredMutuBuahIds")

                                            // Create new JSON with filtered IDs
                                            val filteredTableIdsJson = JSONObject(tableIds) // Copy original
                                            val filteredIdsArray = JSONArray()
                                            filteredMutuBuahIds.forEach { filteredIdsArray.put(it) }
                                            filteredTableIdsJson.put("mutu_buah", filteredIdsArray)

                                            filteredTableIdsJson.toString()
                                        } else {
                                            tableIds
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error filtering table_ids: ${e.message}")
                                        tableIds
                                    }
                                } else {
                                    tableIds
                                }

                                // Determine final message, status, and success
                                val finalMessage = if (isMutuBuahWithSkipped) {
                                    "Data tidak dapat di-upload, Mohon hubungi Kerani Panen untuk upload data panen"
                                } else {
                                    responseBody.message
                                }

                                val finalStatus = responseBody.status
                                val finalSuccess = if (isMutuBuahWithSkipped) false else responseBody.success


                                AppLogger.d("filteredTableIds $filteredTableIds")
                                val jsonResponse = UploadV3Response(
                                    success = finalSuccess,
                                    trackingId = responseBody.trackingId,
                                    message = finalMessage,
                                    status = finalStatus,
                                    tanggal_upload = responseBody.tanggal_upload,
                                    nama_file = responseBody.nama_file,
                                    results = responseBody.results,
                                    type = "json",
                                    table_ids = filteredTableIds // Use filtered table_ids
                                )

                                AppLogger.d("====== JSON RESPONSE DETAILS ======")
                                AppLogger.d("Response Success: ${jsonResponse.success}")
                                AppLogger.d("Response Tracking ID: ${jsonResponse.trackingId}")
                                AppLogger.d("Response Message: ${jsonResponse.message}")
                                AppLogger.d("Response Status: ${jsonResponse.status}")
                                AppLogger.d("Response Upload Date: ${jsonResponse.tanggal_upload}")
                                AppLogger.d("Response File Name: ${jsonResponse.nama_file}")
                                AppLogger.d("Response Type: ${jsonResponse.type}")
                                AppLogger.d("Response Table IDs: ${jsonResponse.table_ids}")
                                AppLogger.d("Response Results: ${jsonResponse.results}")

                                // ✅ ADD THIS: Update progress to 100% on success
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(100, true, finalMessage) // true = success
                                }

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