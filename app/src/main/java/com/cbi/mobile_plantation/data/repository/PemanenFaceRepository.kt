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
import com.cbi.mobile_plantation.utils.PrefManager
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
    private val prefManager = PrefManager(appContext)
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

    suspend fun markUploaded(karyawanIds: List<Int>) = withContext(Dispatchers.IO) {
        if (karyawanIds.isNotEmpty()) {
            pemanenFaceDao.updateStatusUpload(karyawanIds, 1)
        }
    }

    private fun resolveDeptId(): Int? {
        val raw = prefManager.estateIdUserLogin?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return raw.split(",").firstOrNull()?.trim()?.toIntOrNull()
    }

    private fun resolveCompanyId(): Int? {
        return prefManager.companyIdUserLogin?.trim()?.toIntOrNull()
    }

    private fun toUploadRequest(entity: PemanenFaceEntity): FaceUploadRequest {
        return FaceUploadRequest(
            karyawanId = entity.karyawan_id,
            nik = entity.nik,
            nama = entity.nama,
            kemandoranNama = entity.kemandoran_nama,
            embedding = entity.embedding,
            updatedAt = entity.updated_at,
            dept = resolveDeptId(),
            company = resolveCompanyId()
        )
    }

    suspend fun uploadSingle(entity: PemanenFaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.uploadPemanenFace(toUploadRequest(entity))
            if (response.isSuccessful && response.body()?.success == true) {
                pemanenFaceDao.updateStatusUpload(listOf(entity.karyawan_id), 1)
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

    suspend fun uploadPendingBatch(
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit = { _, _, _ -> }
    ): Result<UploadV3Response> = withContext(Dispatchers.IO) {
        uploadBatchFromEntities(getPendingUpload(), onProgressUpdate)
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

    private suspend fun uploadBatchFromEntities(
        entities: List<PemanenFaceEntity>,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> {
        if (entities.isEmpty()) {
            return Result.failure(Exception("Tidak ada data wajah yang perlu diupload"))
        }
        return uploadBatchRequests(entities.map { toUploadRequest(it) }, onProgressUpdate)
    }

    private suspend fun uploadBatchRequests(
        faces: List<FaceUploadRequest>,
        onProgressUpdate: (progress: Int, isSuccess: Boolean, errorMsg: String?) -> Unit
    ): Result<UploadV3Response> {
        withContext(Dispatchers.Main) { onProgressUpdate(10, false, null) }

        val uploadedIds = mutableListOf<Int>()
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
                if (item.status == "success" && item.karyawanId != null) {
                    uploadedIds.add(item.karyawanId)
                }
            }
        }

        if (uploadedIds.isNotEmpty()) {
            pemanenFaceDao.updateStatusUpload(uploadedIds.distinct(), 1)
        }

        val isSuccess = totalFailed == 0 && totalSaved > 0
        val message = if (isSuccess) {
            "Upload wajah berhasil: $totalSaved data"
        } else {
            "Upload wajah selesai: $totalSaved berhasil, $totalFailed gagal"
        }

        withContext(Dispatchers.Main) { onProgressUpdate(100, isSuccess, message) }

        val tableIdsJson = gson.toJson(
            mapOf(AppUtils.DatabaseTables.PEMANEN_FACE to uploadedIds.distinct())
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

    suspend fun syncFromServer(dept: Int? = resolveDeptId()): SyncResult = withContext(Dispatchers.IO) {
        try {
            var offset = 0
            val limit = 5000
            var downloaded = 0
            var updated = 0
            var skipped = 0

            while (true) {
                val response = apiService.downloadPemanenFaces(dept = dept, limit = limit, offset = offset)
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
        val local = pemanenFaceDao.getByKaryawanId(face.karyawanId)
        val serverUpdatedAt = parseDate(face.updatedAt)
        val localUpdatedAt = local?.updated_at?.let { parseDate(it) }

        if (local != null && local.status_upload == 0) {
            val localIsNewer = localUpdatedAt != null && serverUpdatedAt != null && localUpdatedAt.after(serverUpdatedAt)
            if (localIsNewer) return false
        } else if (local != null && localUpdatedAt != null && serverUpdatedAt != null && !serverUpdatedAt.after(localUpdatedAt)) {
            return false
        }

        val entity = PemanenFaceEntity(
            karyawan_id = face.karyawanId,
            nik = face.nik,
            nama = face.nama,
            kemandoran_nama = face.kemandoranNama.orEmpty(),
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
