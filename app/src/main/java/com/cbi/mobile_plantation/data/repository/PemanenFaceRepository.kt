package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.PemanenFaceEntity
import com.cbi.mobile_plantation.data.model.pemanenFace.FaceSyncItem
import com.cbi.mobile_plantation.data.model.pemanenFace.FaceUploadBatchRequest
import com.cbi.mobile_plantation.data.model.pemanenFace.FaceUploadRequest
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PemanenFaceRepository(
    context: Context,
    private val apiService: ApiService = ApiProvider.currentApiService
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val pemanenFaceDao = database.pemanenFaceDao()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val downloaded: Int = 0,
        val updated: Int = 0,
        val skipped: Int = 0
    )

    suspend fun getPendingUpload(): List<PemanenFaceEntity> = withContext(Dispatchers.IO) {
        pemanenFaceDao.getPendingUpload()
    }

    suspend fun markUploaded(niks: List<String>) = withContext(Dispatchers.IO) {
        if (niks.isNotEmpty()) {
            pemanenFaceDao.updateStatusUpload(niks, 1)
        }
    }

    private fun toUploadRequest(entity: PemanenFaceEntity): FaceUploadRequest {
        return FaceUploadRequest(
            nik = entity.nik,
            nama = entity.nama,
            embedding = entity.embedding,
            updatedAt = entity.updated_at
        )
    }

    suspend fun uploadSingle(entity: PemanenFaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.uploadPemanenFace(toUploadRequest(entity))
            if (response.isSuccessful && response.body()?.success == true) {
                pemanenFaceDao.updateStatusUpload(listOf(entity.nik), 1)
                Result.success(Unit)
            } else {
                val message = response.body()?.message ?: response.errorBody()?.string() ?: "Upload gagal"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            AppLogger.e("Face upload single error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadBatchFromJson(
        jsonData: String,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<Map<String, List<FaceUploadRequest>>>() {}.type
            val parsed = gson.fromJson<Map<String, List<FaceUploadRequest>>>(jsonData, type)
            val faces = parsed[AppUtils.DatabaseTables.PEMANEN_FACE]
                ?: parsed["faces"]
                ?: emptyList()

            if (faces.isEmpty()) {
                return@withContext Result.failure(Exception("Tidak ada data wajah untuk diupload"))
            }

            uploadBatchRequests(faces, onProgressUpdate)
        } catch (e: Exception) {
            AppLogger.e("Face upload parse error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun uploadBatchRequests(
        faces: List<FaceUploadRequest>,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> {
        withContext(Dispatchers.Main) { onProgressUpdate(10, false, null) }

        val uploadedNiks = mutableListOf<String>()
        var totalSaved = 0
        var totalFailed = 0

        faces.chunked(500).forEachIndexed { index, chunk ->
            val progress = 10 + ((index + 1) * 80 / ((faces.size + 499) / 500))
            withContext(Dispatchers.Main) { onProgressUpdate(progress.coerceAtMost(90), false, null) }

            val response = apiService.uploadPemanenFaceBatch(FaceUploadBatchRequest(chunk))
            if (!response.isSuccessful || response.body()?.success != true) {
                val message = response.body()?.message ?: response.errorBody()?.string() ?: "Upload batch gagal"
                withContext(Dispatchers.Main) { onProgressUpdate(100, false, message) }
                return Result.failure(Exception(message))
            }

            val summary = response.body()?.data
            totalSaved += summary?.saved ?: 0
            totalFailed += summary?.failed ?: 0

            summary?.results?.forEach { item ->
                if (item.status == "success" && !item.nik.isNullOrBlank()) {
                    uploadedNiks.add(item.nik)
                }
            }
        }

        if (uploadedNiks.isNotEmpty()) {
            pemanenFaceDao.updateStatusUpload(uploadedNiks.distinct(), 1)
        }

        val isSuccess = totalFailed == 0 && totalSaved > 0
        val message = if (isSuccess) {
            "Upload wajah berhasil: $totalSaved data"
        } else {
            "Upload wajah selesai: $totalSaved berhasil, $totalFailed gagal"
        }

        withContext(Dispatchers.Main) { onProgressUpdate(100, isSuccess, message) }

        val tableIdsJson = gson.toJson(
            mapOf(AppUtils.DatabaseTables.PEMANEN_FACE to uploadedNiks.distinct())
        )

        return Result.success(
            UploadV3Response(
                success = isSuccess,
                message = message,
                trackingId = 0,
                status = if (isSuccess) 1 else 0,
                tanggal_upload = dateFormat.format(Date()),
                nama_file = "Data Wajah Pemanen",
                results = null,
                type = "face",
                table_ids = tableIdsJson
            )
        )
    }

    suspend fun syncFromServer(): SyncResult = withContext(Dispatchers.IO) {
        try {
            var offset = 0
            val limit = 5000
            var downloaded = 0
            var updated = 0
            var skipped = 0

            while (true) {
                val response = apiService.downloadPemanenFaces(limit = limit, offset = offset)
                if (!response.isSuccessful || response.body()?.success != true) {
                    val message = response.body()?.message ?: response.errorBody()?.string() ?: "Gagal sinkron data wajah"
                    return@withContext SyncResult(false, message)
                }

                val faces = response.body()?.data?.faces.orEmpty()
                if (faces.isEmpty()) break

                for (face in faces) {
                    if (!face.embedding.startsWith("mfn:")) {
                        skipped++
                        continue
                    }

                    val merged = mergeServerFace(face)
                    if (merged) updated++ else skipped++
                    downloaded++
                }

                if (faces.size < limit) break
                offset += limit
            }

            SyncResult(
                success = true,
                message = "Sinkron wajah selesai: $updated diperbarui, $skipped dilewati",
                downloaded = downloaded,
                updated = updated,
                skipped = skipped
            )
        } catch (e: Exception) {
            AppLogger.e("Face sync error: ${e.message}")
            SyncResult(false, e.message ?: "Gagal sinkron data wajah")
        }
    }

    private suspend fun mergeServerFace(face: FaceSyncItem): Boolean {
        val local = pemanenFaceDao.getByNik(face.nik)
        val serverUpdatedAt = parseDate(face.updatedAt)
        val localUpdatedAt = local?.updated_at?.let { parseDate(it) }

        if (local != null && local.status_upload == 0) {
            val localIsNewer = localUpdatedAt != null && serverUpdatedAt != null && localUpdatedAt.after(serverUpdatedAt)
            if (localIsNewer) return false
        } else if (local != null && localUpdatedAt != null && serverUpdatedAt != null && !serverUpdatedAt.after(localUpdatedAt)) {
            return false
        }

        val entity = PemanenFaceEntity(
            nik = face.nik,
            nama = face.nama,
            embedding = face.embedding,
            updated_at = normalizeDate(face.updatedAt),
            status_upload = 1
        )
        pemanenFaceDao.insertOrUpdate(entity)
        return true
    }

    private fun parseDate(value: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                return SimpleDateFormat(pattern, Locale.getDefault()).parse(value)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun normalizeDate(value: String): String {
        return parseDate(value)?.let { dateFormat.format(it) } ?: value
    }

    fun buildUploadJson(entities: List<PemanenFaceEntity>): String {
        val faces = entities.map { toUploadRequest(it) }
        return gson.toJson(mapOf(AppUtils.DatabaseTables.PEMANEN_FACE to faces))
    }
}
