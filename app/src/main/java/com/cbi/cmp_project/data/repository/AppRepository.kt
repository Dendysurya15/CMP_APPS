package com.cbi.cmp_project.data.repository

import android.content.Context
import android.util.Log
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.model.TPHBlokInfo
import com.cbi.cmp_project.data.model.TphRvData
import com.cbi.markertph.data.model.TPHNewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val panenDao = database.panenDao()
    private val espbDao = database.espbDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()

    sealed class SaveResultPanen {
        object Success : SaveResultPanen()
        data class Error(val exception: Exception) : SaveResultPanen()
    }

    suspend fun saveDataPanen(data: PanenEntity) {
        panenDao.insert(data)
    }


    suspend fun saveTPHDataList(tphDataList: List<TphRvData>): Result<List<Long>> =
        withContext(Dispatchers.IO) {
            try {
                // Check for duplicates first
                val duplicates = tphDataList.filter { tphData ->
                    panenDao.exists(tphData.namaBlok, tphData.time)
                }

                if (duplicates.isNotEmpty()) {
                    val duplicateInfo = duplicates.joinToString("\n") {
                        "TPH ID: ${it.namaBlok}, Date: ${it.time}"
                    }
                    return@withContext Result.failure(
                        Exception("Duplicate data found:\n$duplicateInfo")
                    )
                }

                // If no duplicates, proceed with saving
                val results = tphDataList.map { tphData ->
                    panenDao.insertWithTransaction(
                        PanenEntity(
                            tph_id = tphData.namaBlok,
                            date_created = tphData.time,
                            created_by = 0,
                            karyawan_id = "",
                            jjg_json = "{\"KP\": ${tphData.jjg}}",
                            foto = "",
                            komentar = "",
                            asistensi = 0,
                            lat = 0.0,
                            lon = 0.0,
                            jenis_panen = 0,
                            ancak = 0,
                            info = "",
                            archive = 0,
                            status_espb = 0,
                            status_restan = 0,
                            scan_status = 1
                        )
                    )
                }

                // Collect all successful results or throw the first error
                val savedIds = results.map { result ->
                    result.getOrThrow()
                }

                Result.success(savedIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    suspend fun updatePanen(panen: List<PanenEntity>) = withContext(Dispatchers.IO) {
        panenDao.update(panen)
    }

    suspend fun deleteAllPanen(panen: List<PanenEntity>) = withContext(Dispatchers.IO) {
        panenDao.deleteAll(panen)
    }

    suspend fun getPanenById(id: Int): PanenEntity? = withContext(Dispatchers.IO) {
        panenDao.getById(id)
    }

    suspend fun getDivisiAbbrByTphId(id: Int): String? = withContext(Dispatchers.IO) {
        tphDao.getDivisiAbbrByTphId(id)
    }

    suspend fun updatePanenArchive(ids: List<Int>,statusArchive:Int) {
        panenDao.updatePanenArchive(ids, statusArchive)
    }

    suspend fun getCompanyAbbrByTphId(id: Int): String? = withContext(Dispatchers.IO) {
        tphDao.geCompanyAbbrByTphId(id)
    }

    suspend fun getPanenCount(): Int {
        return panenDao.getCount()
    }

    suspend fun getCountDraftESPB(): Int {
        return espbDao.getCountDraft()
    }

    suspend fun getPanenCountArchive(): Int {
        return panenDao.getCountArchive()
    }

    suspend fun getPanenCountApproval(): Int {
        return panenDao.getCountApproval()
    }

    suspend fun getTPHAndBlokInfo(id: Int): TPHBlokInfo? = withContext(Dispatchers.IO) {
        try {
            tphDao.getTPHAndBlokInfo(id)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error getting TPH and Blok info", e)
            null
        }
    }

    suspend fun getAllPanen(): List<PanenEntity> = withContext(Dispatchers.IO) {
        panenDao.getAll()
    }

    suspend fun getActivePanen(): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllActiveWithRelations()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivePanenESPB(): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllActivePanenESPBWithRelations()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivePanenRestan(status: Int = 0): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllAPanenRestan(status)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArchivedPanen(): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllArchivedWithRelations()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePanenById(id: Int) = withContext(Dispatchers.IO) {
        panenDao.deleteByID(id)
    }

    suspend fun deletePanenByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        panenDao.deleteByListID(ids)
    }

    suspend fun archivePanenById(id: Int) = withContext(Dispatchers.IO) {
        panenDao.archiveByID(id)
    }

    suspend fun archivePanenByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        panenDao.archiveByListID(ids)
    }


    // ESPBEntity Methods
    suspend fun insertESPB(espb: List<ESPBEntity>) = withContext(Dispatchers.IO) {
        espbDao.insert(espb)
    }

    suspend fun updateESPB(espb: List<ESPBEntity>) = withContext(Dispatchers.IO) {
        espbDao.update(espb)
    }

    suspend fun deleteAllESPB(espb: List<ESPBEntity>) = withContext(Dispatchers.IO) {
        espbDao.deleteAll(espb)
    }

    suspend fun getESPBById(id: Int): ESPBEntity? = withContext(Dispatchers.IO) {
        espbDao.getById(id)
    }

    suspend fun getAllESPB(): List<ESPBEntity> = withContext(Dispatchers.IO) {
        espbDao.getAll()
    }

    suspend fun getActiveESPB(): List<ESPBEntity> = withContext(Dispatchers.IO) {
        espbDao.getAllActive()
    }

    suspend fun getArchivedESPB(): List<ESPBEntity> = withContext(Dispatchers.IO) {
        espbDao.getAllArchived()
    }

    suspend fun deleteESPBById(id: Int) = withContext(Dispatchers.IO) {
        espbDao.deleteByID(id)
    }

    suspend fun deleteESPBByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        espbDao.deleteByListID(ids)
    }

    suspend fun archiveESPBById(id: Int) = withContext(Dispatchers.IO) {
        espbDao.archiveByID(id)
    }

    suspend fun archiveESPBByIds(ids: List<Int>) = withContext(Dispatchers.IO) {
        espbDao.archiveByListID(ids)
    }

    suspend fun updateOrInsertESPB(espb: List<ESPBEntity>) = withContext(Dispatchers.IO) {
        espbDao.updateOrInsert(espb)
    }

    suspend fun getMillList() = withContext(Dispatchers.IO) {
        millDao.getAll()
    }

    private fun transformTphDataToMap(inputData: String): Map<Int, Int> {
        val records = inputData.split(";")

        return records.mapNotNull {
            val parts = it.split(",")
            if (parts.size >= 3) {
                try {
                    parts[0].toInt() to parts[2].toInt()
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                null
            }
        }.toMap()
    }

    suspend fun getJanjangSumByBlock(tphData: String): Map<Int, Int> = withContext(Dispatchers.IO) {
        try {
            // Parse the TPH data to get ID-to-janjang mapping
            val tphJanjangMap = transformTphDataToMap(tphData)

            // Get the TPH IDs from the map
            val tphIds = tphJanjangMap.keys.toList()

            // Retrieve the TPH models for these IDs
            val tphModels = tphDao.getTPHsByIds(tphIds)

            // Group by block and sum janjang values
            tphModels
                .filter { it.id != null && it.blok != null }
                .groupBy { it.blok!! }
                .mapValues { (_, tphsInBlock) ->
                    // Sum janjang values for each TPH in this block
                    tphsInBlock
                        .mapNotNull { tph ->
                            tph.id?.let { id -> tphJanjangMap[id] ?: 0 }
                        }
                        .sum()
                }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error calculating janjang sum by block", e)
            emptyMap()
        }
    }

    suspend fun getJanjangSumByBlockString(tphData: String): String = withContext(Dispatchers.IO) {
        try {
            val janjangByBlockMap = getJanjangSumByBlock(tphData)
            convertJanjangMapToString(janjangByBlockMap)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error formatting janjang sums", e)
            ""
        }
    }

    fun convertJanjangMapToString(janjangByBlock: Map<Int, Int>): String {
        return janjangByBlock.entries
            .joinToString(";") { (blockId, janjangSum) ->
                "$blockId,$janjangSum"
            }
    }

    // Add this to your AppRepository
    suspend fun updatePanenESPBStatus(ids: List<Int>, status: Int) = withContext(Dispatchers.IO) {
        panenDao.updateESPBStatusByIds(ids, status)
    }

    suspend fun loadHistoryESPB(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
        try {
            val data = espbDao.getAllESPBS()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getBlokById( listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
    }

}