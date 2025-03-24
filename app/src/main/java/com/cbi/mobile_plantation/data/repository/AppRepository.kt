package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val panenDao = database.panenDao()
    private val espbDao = database.espbDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val transporterDao = database.transporterDao()

    sealed class SaveResultPanen {
        object Success : SaveResultPanen()
        data class Error(val exception: Exception) : SaveResultPanen()
    }

    suspend fun saveDataPanen(data: PanenEntity) {
        panenDao.insert(data)
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return karyawanDao.getPemuatByIdList(idPemuat)
    }

    suspend fun getKemandoranById(idKemandoran: List<String>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranById(idKemandoran)
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
                            kemandoran_id = "",
                            karyawan_nik = "",
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
                            scan_status = 1,
                            username = tphData.username
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

    suspend fun loadESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null): List<PanenEntityWithRelations> {
        return try {
            panenDao.loadESPB(archive, statusEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun countESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null): Int {
        return try {
            panenDao.countESPB(archive, statusEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            0  // Return 0 if there's an error
        }
    }

    suspend fun updateDataIsZippedPanen(ids: List<Int>,status:Int) {
        panenDao.updateDataIsZippedPanen(ids, status)
    }

    suspend fun getCompanyAbbrByTphId(id: Int): String? = withContext(Dispatchers.IO) {
        tphDao.geCompanyAbbrByTphId(id)
    }

    suspend fun getPanenCount(): Int {
        return panenDao.getCount()
    }

    suspend fun loadCountTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String?): Int {
        return try {
            panenDao.getCountTPHESPB(archive, statusEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading TPH ESPB count: ${e.message}")
            0  // Return 0 if an error occurs
        }
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

    suspend fun getAllPanenWhereESPB(no_esp: String): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllPanenWhereESPB(no_esp)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun getAllTPHHasBeenSelected(): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = panenDao.getAllTPHHasBeenSelected()
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

    suspend fun updateESPBStatusForMultipleIds(idsList: List<Int>, status: Int, noESPB: String): Int {
        return database.withTransaction {
            panenDao.updateESPBStatusByIds(idsList, status, noESPB)
        }
    }

//    suspend fun loadHistoryESPB(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
//        try {
//            val data = espbDao.getAllESPBS()
//            Result.success(data)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    suspend fun loadHistoryESPB(date: String? = null): List<ESPBEntity> {
        return try {
            espbDao.getAllESPBS(date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB history: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    fun getBlokById( listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
    }

    suspend fun getTransporterNameById(id: Int): String? {
        return transporterDao.getTransporterNameById(id)
    }

    suspend fun getMillNameById(id: Int): String? {
        return millDao.getMillNameById(id)
    }

    // Add to AppRepository.kt
    suspend fun insertESPBAndGetId(espbEntity: ESPBEntity): Long {
        return espbDao.insertAndGetId(espbEntity)
    }

}