package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction

import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.database.HektarPanenDao
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.MathFun
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Add this class to represent the different outcomes
sealed class SaveTPHResult {
    data class AllSuccess(val savedIds: List<Long>) : SaveTPHResult()
    data class PartialSuccess(
        val savedIds: List<Long>,
        val duplicateCount: Int,
        val duplicateInfo: String
    ) : SaveTPHResult()

    data class AllDuplicate(
        val duplicateCount: Int,
        val duplicateInfo: String
    ) : SaveTPHResult()
}

class AppRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val panenDao = database.panenDao()
    private val espbDao = database.espbDao()
    private val blokDao = database.blokDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val transporterDao = database.transporterDao()
    private val inspectionDao = database.inspectionDao()
    private val inspectionDetailDao = database.inspectionDetailDao()
    private val kendaraanDao = database.kendaraanDao()
    private val hektarPanenDao = database.hektarPanenDao()
    private val jenisTPHDao = database.jenisTPHDao()
    private val mutuBuahDao = database.mutuBuahDao()

    private val _tphData = MutableLiveData<TPHNewModel?>()
    val tphData: LiveData<TPHNewModel?> = _tphData

    suspend fun loadTPH(tphId: Int) {
        withContext(Dispatchers.IO) {
            val result = tphDao.getTPHById(tphId)
            _tphData.postValue(result)
        }
    }
    private val afdelingDao = database.afdelingDao()
    private val parameterDao = database.parameterDao()


    sealed class SaveResultPanen {
        object Success : SaveResultPanen()
        data class Error(val exception: Exception) : SaveResultPanen()
    }

    sealed class SaveResultMutuBuah {
        object Success : SaveResultMutuBuah()
        data class Error(val exception: Exception) : SaveResultMutuBuah()
    }

    suspend fun saveDataPanen(data: PanenEntity) {
        panenDao.insert(data)
    }

    suspend fun insertInspectionData(inspectionData: InspectionModel): Long {
        return inspectionDao.insertInspection(inspectionData)
    }

    suspend fun updateInspectionForFollowUp(
        inspectionId: Int,
        tracking_path_pemulihan: String?,
        inspeksi_putaran: Int,
        updated_date_start: String,
        updated_date_end: String,
        updated_by: String,
        foto_user_pemulihan: String,
        updated_name: String,
        app_version_pemulihan: String,
    ): Boolean {
        return try {
            inspectionDao.updateInspectionForFollowUp(
                inspectionId = inspectionId,
                tracking_path_pemulihan = tracking_path_pemulihan,
                inspeksi_putaran = inspeksi_putaran,
                updated_date_start = updated_date_start,
                updated_date_end = updated_date_end,
                updated_by = updated_by,
                foto_user_pemulihan = foto_user_pemulihan,
                updated_name = updated_name,
                app_version_pemulihan = app_version_pemulihan,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateInspectionDetailForFollowUpById(
        inspectionDetailId: Int,
        temuanInspeksi: Double,
        fotoPemulihan: String?,
        komentarPemulihan: String?,
        latPemulihan: Double,
        lonPemulihan: Double,
        updatedDate: String,
        statusPemulihan: Int,
        updatedName: String,
        updatedBy: String
    ): Boolean {
        return try {
            inspectionDetailDao.updateInspectionDetailForFollowUpById(
                inspectionDetailId = inspectionDetailId,
                temuanInspeksi = temuanInspeksi,
                fotoPemulihan = fotoPemulihan,
                komentarPemulihan = komentarPemulihan,
                latPemulihan = latPemulihan,
                statusPemulihan = statusPemulihan,
                lonPemulihan = lonPemulihan,
                updatedDate = updatedDate,
                updatedName = updatedName,
                updatedBy = updatedBy
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun insertInspectionDetails(inspectionDetailList: List<InspectionDetailModel>) {
        inspectionDao.insertInspectionDetails(inspectionDetailList)
    }

    suspend fun getAfdelingName(afdelingId: Int): String? {
        return afdelingDao.getAfdelingNameById(afdelingId)
    }

    suspend fun getKemandoranByNik(nikList: List<String>): List<KaryawanModel> {
        return karyawanDao.getKaryawanByNikList(nikList)
    }

    suspend fun getParameterInspeksiJson(): String? {
        return parameterDao.getParameterInspeksiJson()
    }

    suspend fun getMillByAbbr(abbr: String): MillModel? {
        return millDao.getMillByAbbr(abbr)
    }

    suspend fun updateDataIsZippedMutuBuah(ids: List<Int>, status: Int) {
        mutuBuahDao.updateDataIsZippedMutuBuah(ids, status)
    }

    suspend fun getInspectionData(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ): List<InspectionWithDetailRelations> {
        return try {
            inspectionDao.getInspectionData(datetime, isPushedToServer)
        } catch (e: Exception) {
            AppLogger.e("Error loading inspection paths: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun getInspectionCount(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ): Int {
        return try {
            inspectionDao.getInspectionCount(datetime, isPushedToServer)
        } catch (e: Exception) {
            AppLogger.e("Error loading inspection count: ${e.message}")
            0
        }
    }


    suspend fun getInspectionById(inspectionId: String): List<InspectionWithDetailRelations> {
        return try {
            inspectionDao.getInspectionById(inspectionId)
        } catch (e: Exception) {
            AppLogger.e("Error loading inspection by ID: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPanenForTransferInspeksi(
        date: String? = null,
        archive_transfer_inspeksi: Int? = null
    ): List<PanenEntityWithRelations> {
        return try {
            panenDao.getPanenForTransferInspeksi(date, archive_transfer_inspeksi)
        } catch (e: Exception) {
            AppLogger.e("Error loading inspection by ID: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCountPanenForTransferInspeksi(
        datetime: String? = null,
        archive_transfer_inspeksi: Int
    ): Int {
        return panenDao.getCountPanenForTransferInspeksi(datetime, archive_transfer_inspeksi)
    }

    suspend fun resetEspbStatus(
        tphId: String,
        dateCreated: String,
        kpJson: String,
        nomorPemanen: String
    ): Int {
        return panenDao.resetEspbStatus(tphId, dateCreated, kpJson, nomorPemanen)
    }

    suspend fun setEspbStatus(
        tphId: String,
        dateCreated: String,
        kpJson: String,
        nomorPemanen: String,
        noEspb: String
    ): Int {
        return panenDao.setEspbStatus(tphId, dateCreated, kpJson, nomorPemanen, noEspb)
    }


    suspend fun saveMutuBuah(data: MutuBuahEntity) {
        mutuBuahDao.insert(data)
    }

    suspend fun saveScanMPanen(
        tphDataList: List<PanenEntity>,
        createdBy: String? = null,
        creatorInfo: String? = null,
        context: Context
    ): Result<SaveTPHResult> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // Keep track of successes and failures
                val savedIds = mutableListOf<Long>()
                val duplicates = mutableListOf<PanenEntity>()
                val updated = mutableListOf<PanenEntity>()
                val hektarPanenDao = database.hektarPanenDao()

                val kemandoranId = tphDataList.first().kemandoran_id
                val kemandoranNama = kemandoranDao.getKemandoranByTheId(kemandoranId.toInt())!!.nama
                val kemandoranKode = kemandoranDao.getKemandoranByTheId(kemandoranId.toInt())!!.kode

                // Step 1: Process each PanenEntity record
                // Step 1: Process each PanenEntity record
                for (tphData in tphDataList) {
                    // Check if this specific item exists in local database
                    val existingRecord = panenDao.existsModel(tphData.tph_id, tphData.date_created)

                    if (existingRecord == null) {
                        // Record doesn't exist -> INSERT
                        val result = panenDao.insertWithTransaction(tphData)
                        result.fold(
                            onSuccess = { id -> savedIds.add(id) },
                            onFailure = { throw it }
                        )
                    } else {
                        if (existingRecord.panen.status_scan_mpanen == 1) {
                            duplicates.add(tphData)
                            Log.d(
                                "AppRepository",
                                "Already scanned duplicate: TPH=${tphData.tph_id}, Date=${tphData.date_created}"
                            )
                        } else {
                            if (existingRecord.panen.isPushedToServer == 1) {
                                // Server record exists, update specific fields only
                                val updatedRecord = existingRecord.copy(
                                    panen = existingRecord.panen.copy(
                                        karyawan_id = if ((existingRecord.panen.karyawan_id.isNullOrEmpty() ||
                                                    existingRecord.panen.karyawan_id == "NULL") &&
                                            tphData.karyawan_id.isNotEmpty() &&
                                            tphData.karyawan_id != "NULL"
                                        ) {
                                            tphData.karyawan_id
                                        } else {
                                            existingRecord.panen.karyawan_id
                                        },

                                        karyawan_nama = if ((existingRecord.panen.karyawan_nama.isNullOrEmpty() ||
                                                    existingRecord.panen.karyawan_nama == "NULL") &&
                                            tphData.karyawan_nama.isNotEmpty() &&
                                            tphData.karyawan_nama != "NULL"
                                        ) {
                                            tphData.karyawan_nama
                                        } else {
                                            existingRecord.panen.karyawan_nama
                                        },

                                        jjg_json = if (tphData.jjg_json.isNotEmpty() &&
                                            tphData.jjg_json != "NULL"
                                        ) {
                                            tphData.jjg_json
                                        } else {
                                            existingRecord.panen.jjg_json
                                        },

                                        status_scan_mpanen = tphData.status_scan_mpanen
                                    )
                                )



                                panenDao.update(listOf(updatedRecord.panen))
                                updated.add(updatedRecord.panen)

                                Log.d(
                                    "AppRepository",
                                    "Updated existing server record: TPH=${tphData.tph_id}, Date=${tphData.date_created}"
                                )

                            }
                            else {
                                if (existingRecord.panen.status_scan_mpanen == 1) {
                                    duplicates.add(tphData)
                                    Log.d(
                                        "AppRepository",
                                        "Already scanned duplicate: TPH=${tphData.tph_id}, Date=${tphData.date_created}"
                                    )
                                }else {
                                    if (existingRecord.panen.status_scan_mpanen == 1) {
                                        duplicates.add(tphData)
                                        Log.d(
                                            "AppRepository",
                                            "Already scanned duplicate: TPH=${tphData.tph_id}, Date=${tphData.date_created}"
                                        )
                                    } else {
                                        if (existingRecord.panen.isPushedToServer == 1) {
                                            // Server record exists, update specific fields only
                                            val updatedRecord = existingRecord.copy(
                                                panen = existingRecord.panen.copy(
                                                    jjg_json = if (tphData.jjg_json.isNotEmpty() &&
                                                        tphData.jjg_json != "NULL"
                                                    ) {
                                                        tphData.jjg_json
                                                    } else {
                                                        existingRecord.panen.jjg_json
                                                    },
                                                    status_scan_mpanen = tphData.status_scan_mpanen,
                                                    jumlah_pemanen = tphData.jumlah_pemanen
                                                )
                                            )

                                            panenDao.update(listOf(updatedRecord.panen))
                                            updated.add(updatedRecord.panen)

                                            Log.d(
                                                "AppRepository",
                                                "Updated existing server record: TPH=${tphData.tph_id}, Date=${tphData.date_created}"
                                            )

                                        } else {
                                            // Local record only (not pushed to server) -> Check for exact duplicate
                                            Log.d("AppRepository", "🔍 COMPARISON DETAILS for TPH=${tphData.tph_id}:")
                                            Log.d("AppRepository", "   tph_id: '${tphData.tph_id}' vs '${existingRecord.panen.tph_id}' → ${tphData.tph_id == existingRecord.panen.tph_id}")
                                            Log.d("AppRepository", "   date_created: '${tphData.date_created}' vs '${existingRecord.panen.date_created}' → ${tphData.date_created == existingRecord.panen.date_created}")
                                            Log.d("AppRepository", "   kemandoran_id: '${tphData.kemandoran_id}' vs '${existingRecord.panen.kemandoran_id}' → ${tphData.kemandoran_id == existingRecord.panen.kemandoran_id}")
                                            Log.d("AppRepository", "   karyawan_nik: '${tphData.karyawan_nik}' vs '${existingRecord.panen.karyawan_nik}' → ${tphData.karyawan_nik == existingRecord.panen.karyawan_nik}")

                                            // Helper function to check if a value should be ignored
                                            fun shouldIgnoreValue(value: String?): Boolean {
                                                return value == null || value.isEmpty() || value == "NULL"
                                            }

                                            // Only compare jjg_json if incoming value is not NULL/empty
                                            val jjgJsonMatches = if (shouldIgnoreValue(tphData.jjg_json)) {
                                                true // Skip comparison if incoming is NULL/empty
                                            } else {
                                                tphData.jjg_json == existingRecord.panen.jjg_json
                                            }
                                            Log.d("AppRepository", "   jjg_json: '${tphData.jjg_json}' vs '${existingRecord.panen.jjg_json}' → $jjgJsonMatches ${if (shouldIgnoreValue(tphData.jjg_json)) "(skipped - incoming NULL/empty)" else ""}")

                                            // Compare status_scan_mpanen
                                            val statusScanMpanenMatches = tphData.status_scan_mpanen == existingRecord.panen.status_scan_mpanen
                                            Log.d("AppRepository", "   status_scan_mpanen: '${tphData.status_scan_mpanen}' vs '${existingRecord.panen.status_scan_mpanen}' → $statusScanMpanenMatches")

                                            // Compare jumlah_pemanen
                                            val jumlahPemanenMatches = tphData.jumlah_pemanen == existingRecord.panen.jumlah_pemanen
                                            Log.d("AppRepository", "   jumlah_pemanen: '${tphData.jumlah_pemanen}' vs '${existingRecord.panen.jumlah_pemanen}' → $jumlahPemanenMatches")

                                            val isExactDuplicate = (
                                                    tphData.tph_id == existingRecord.panen.tph_id &&
                                                            tphData.date_created == existingRecord.panen.date_created &&
                                                            tphData.kemandoran_id == existingRecord.panen.kemandoran_id &&
                                                            tphData.karyawan_nik == existingRecord.panen.karyawan_nik &&
                                                            jjgJsonMatches &&
                                                            statusScanMpanenMatches &&
                                                            jumlahPemanenMatches
                                                    )

                                            if (isExactDuplicate) {
                                                // Add to duplicates list
                                                duplicates.add(tphData)
                                                Log.d("AppRepository", "⚠️ Exact duplicate found: TPH=${tphData.tph_id}, Date=${tphData.date_created}")
                                            } else {
                                                // Not duplicate, update the existing record
                                                Log.d("AppRepository", "🔄 Data is different, updating existing local record")
                                                val updatedRecord = existingRecord.copy(
                                                    panen = existingRecord.panen.copy(
                                                        jjg_json = if (!shouldIgnoreValue(tphData.jjg_json))
                                                            tphData.jjg_json else existingRecord.panen.jjg_json,
                                                        status_scan_mpanen = tphData.status_scan_mpanen,
                                                        jumlah_pemanen = tphData.jumlah_pemanen
                                                    )
                                                )

                                                panenDao.update(listOf(updatedRecord.panen))
                                                updated.add(updatedRecord.panen)
                                                Log.d("AppRepository", "✅ Successfully updated local record: TPH=${tphData.tph_id}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Step 2: Group by unique (NIK, Block) combination
                // Use records that were either saved or updated (not duplicates)
                val recordsToProcess = tphDataList.filter { tphData ->
                    !duplicates.contains(tphData)
                }

                val groupedByNikAndBlock =
                    mutableMapOf<Pair<String, String>, MutableList<PanenEntity>>()

                // Add debug logging for tphDataList
                Log.d(
                    "AppRepository",
                    "Processing ${recordsToProcess.size} TPH entries for HektarPanen, with ${duplicates.size} duplicates, ${updated.size} updated"
                )

                for (tphData in recordsToProcess) {
                    try {
                        val tphId = tphData.tph_id.toIntOrNull()
                        if (tphId == null) {
                            Log.e("AppRepository", "Invalid TPH ID: ${tphData.tph_id}")
                            continue
                        }

                        val blokIdFromTPHid = tphDao.getBlokIdbyIhTph(tphId)
                        if (blokIdFromTPHid == null) {
                            Log.e(
                                "AppRepository",
                                "Could not find block ID for TPH ID: ${tphData.tph_id}"
                            )
                            continue
                        }
                        if (tphData.karyawan_nik.contains(",")) {
                            val nikArr = tphData.karyawan_nik.split(",")
                            for (nik in nikArr) {
                                val key = Pair(
                                    nik,
                                    "${blokIdFromTPHid}$${tphData.date_created.split(" ")[0]}"
                                )
                                // Add logging for grouping
                                Log.d(
                                    "AppRepository",
                                    "Grouping TPH: ${blokIdFromTPHid}$${tphData.date_created.split(" ")[0]}, NIK: ${nik}, Block: $blokIdFromTPHid"
                                )
                                // Initialize list for this key if it doesn't exist
                                if (!groupedByNikAndBlock.containsKey(key)) {
                                    groupedByNikAndBlock[key] = mutableListOf()
                                }
                                // Add this entity to the group
                                groupedByNikAndBlock[key]!!.add(tphData)
                            }
                        } else {
                            // Create a key with NIK and Block (Pair<String, Int>)
                            val key = Pair(
                                tphData.karyawan_nik,
                                "${blokIdFromTPHid}$${tphData.date_created.split(" ")[0]}"
                            )

                            // Add logging for grouping
                            Log.d(
                                "AppRepository",
                                "Grouping TPH: ${blokIdFromTPHid}$${tphData.date_created.split(" ")[0]}, NIK: ${tphData.karyawan_nik}, Block: $blokIdFromTPHid"
                            )

                            // Initialize list for this key if it doesn't exist
                            if (!groupedByNikAndBlock.containsKey(key)) {
                                groupedByNikAndBlock[key] = mutableListOf()
                            }

                            // Add this entity to the group
                            groupedByNikAndBlock[key]!!.add(tphData)
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing TPH data: ${e.message}")
                    }
                }

                // Log the number of unique (NIK, Block) combinations
                Log.d(
                    "AppRepository",
                    "Found ${groupedByNikAndBlock.size} unique (NIK, Block) combinations"
                )

                // Log each unique combination
                groupedByNikAndBlock.keys.forEach { (nik, blokIdDate) ->
                    Log.d("AppRepository", "Unique combination - NIK: $nik, BlockDate: $blokIdDate")
                }

                // Step 3: Process each group to create/update HektarPanen records
                for ((key, entities) in groupedByNikAndBlock) {
                    val (nik, blokIdDate) = key

                    try {
                        val blokId = try {
                            blokIdDate.split("$")[0].toInt()
                        } catch (e: Exception) {
                            Toasty.error(context, "Error parsing blokId: ${e.message}").show()
                            0
                        }

                        val date = try {
                            blokIdDate.split("$")[1]
                        } catch (e: Exception) {
                            Toasty.error(context, "Error parsing date: ${e.message}").show()
                            ""
                        }

                        // Check if a record already exists for this (NIK, Block) combination
                        var hektarPanen = hektarPanenDao.getByNikAndBlokDate(nik, blokId, date)

                        // Prepare the arrays to store values
                        val totalJjg = mutableListOf<String>()
                        val unripe = mutableListOf<String>()
                        val overripe = mutableListOf<String>()
                        val emptyBunch = mutableListOf<String>()
                        val abnormal = mutableListOf<String>()
                        val ripe = mutableListOf<String>()
                        val kirimPabrik = mutableListOf<String>()
                        val dibayar = mutableListOf<String>()
                        val tphIds = mutableListOf<String>()
                        val dateCreatedPanen = mutableListOf<String>()

                        // Process all entities in this group
                        for (entity in entities) {
                            try {
                                val jjgJson = JSONObject(entity.jjg_json)
                                totalJjg.add(
                                    MathFun().round(
                                        jjgJson.optInt("TO", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                unripe.add(
                                    MathFun().round(
                                        jjgJson.optInt("UN", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                overripe.add(
                                    MathFun().round(
                                        jjgJson.optInt("OV", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                emptyBunch.add(
                                    MathFun().round(
                                        jjgJson.optInt("EM", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                abnormal.add(
                                    MathFun().round(
                                        jjgJson.optInt("AB", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                ripe.add(
                                    MathFun().round(
                                        jjgJson.optInt("RI", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                kirimPabrik.add(
                                    MathFun().round(
                                        jjgJson.optInt("KP", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                dibayar.add(
                                    MathFun().round(
                                        jjgJson.optInt("PA", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat(), 2
                                    ).toString()
                                )
                                tphIds.add(entity.tph_id)
                                dateCreatedPanen.add(entity.date_created)
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error parsing jjg_json: ${e.message}")
                            }
                        }

                        // Log the values for debugging
                        Log.d(
                            "AppRepository",
                            "For NIK: $nik, Block: $blokIdDate - TotalJJG: ${totalJjg.joinToString(";")}"
                        )

                        if (hektarPanen == null) {
                            // Get the TPH model
                            val tphModel = tphDao.getTPHByBlockId(blokId)

                            // Extract luas_area with error handling
                            val luasArea = try {
                                val rawValue = tphModel!!.luas_area!!.toFloat()
                                BigDecimal(rawValue.toDouble()).setScale(2, RoundingMode.HALF_UP)
                                    .toFloat()
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting luas area: ${e.message}")
                                0f
                            }

                            // Extract regional with error handling
                            val regional = try {
                                tphModel!!.regional
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting regional: ${e.message}")
                                "NULL"
                            }

                            // Extract company with error handling
                            val company = try {
                                tphModel!!.company
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting company: ${e.message}")
                                0
                            }

                            // Extract company_abbr with error handling
                            val companyAbbr = try {
                                tphModel!!.company_abbr
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting company_abbr: ${e.message}")
                                "NULL"
                            }

                            // Extract company_nama with error handling
                            val companyNama = try {
                                tphModel!!.company_nama
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting company_nama: ${e.message}")
                                "NULL"
                            }

                            // Extract wilayah with error handling
                            val wilayah = try {
                                tphModel!!.wilayah
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting wilayah: ${e.message}")
                                "NULL"
                            }

                            // Extract dept with error handling
                            val dept = try {
                                tphModel!!.dept
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting dept: ${e.message}")
                                0
                            }

                            // Extract dept_ppro with error handling
                            val deptPpro = try {
                                tphModel!!.dept_ppro
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting dept_ppro: ${e.message}")
                                0
                            }

                            // Extract dept_abbr with error handling
                            val deptAbbr = try {
                                tphModel!!.dept_abbr
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting dept_abbr: ${e.message}")
                                "NULL"
                            }

                            // Extract dept_nama with error handling
                            val deptNama = try {
                                tphModel!!.dept_nama
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting dept_nama: ${e.message}")
                                "NULL"
                            }

                            // Extract divisi with error handling
                            val divisi = try {
                                tphModel!!.divisi
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting divisi: ${e.message}")
                                0
                            }

                            // Extract divisi_ppro with error handling
                            val divisiPpro = try {
                                tphModel!!.divisi_ppro
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting divisi_ppro: ${e.message}")
                                0
                            }

                            // Extract divisi_abbr with error handling
                            val divisiAbbr = try {
                                tphModel!!.divisi_abbr
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting divisi_abbr: ${e.message}")
                                "NULL"
                            }

                            // Extract divisi_nama with error handling
                            val divisiNama = try {
                                tphModel!!.divisi_nama
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting divisi_nama: ${e.message}")
                                "NULL"
                            }

                            // Extract blok with error handling
                            val blok = try {
                                tphModel!!.blok
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok: ${e.message}")
                                0
                            }

                            // Extract blok_ppro with error handling
                            val blokPpro = try {
                                tphModel!!.blok_ppro
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok_ppro: ${e.message}")
                                0
                            }

                            // Extract blok_kode with error handling
                            val blokKode = try {
                                tphModel!!.blok_kode
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok_kode: ${e.message}")
                                "NULL"
                            }

                            // Extract blok_nama with error handling
                            val blokNama = try {
                                tphModel!!.blok_nama
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok_nama: ${e.message}")
                                "NULL"
                            }

                            // Get employee details from KaryawanDao
                            val pemanen = try {
                                karyawanDao.getNamaByNik(nik)
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting pemanen_nama: ${e.message}")
                                "NULL"
                            }

                            // Extract kemandoran details
                            val kemandoranId = try {
                                recordsToProcess.first().kemandoran_id
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting kemandoran_id: ${e.message}")
                                "0"
                            }

                            // Create the HektarPanenEntity
                            hektarPanen = HektarPanenEntity(
                                id = null,
                                nik = nik,
                                pemanen_nama = pemanen!!,
                                kemandoran_id = kemandoranId,
                                kemandoran_nama = kemandoranNama!!,
                                kemandoran_kode = kemandoranKode!!,
                                blok = blokId,
                                luas_blok = luasArea,
                                luas_panen = 0f,
                                date_created = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date()),
                                created_by = createdBy ?: "Unknown",
                                creator_info = creatorInfo ?: "{}",
                                total_jjg_arr = totalJjg.joinToString(";"),
                                unripe_arr = unripe.joinToString(";"),
                                overripe_arr = overripe.joinToString(";"),
                                empty_bunch_arr = emptyBunch.joinToString(";"),
                                abnormal_arr = abnormal.joinToString(";"),
                                ripe_arr = ripe.joinToString(";"),
                                kirim_pabrik_arr = kirimPabrik.joinToString(";"),
                                dibayar_arr = dibayar.joinToString(";"),
                                tph_ids = tphIds.joinToString(";"),
                                date_created_panen = dateCreatedPanen.joinToString(";"),
                                regional = regional!!,
                                wilayah = wilayah!!,
                                company = company,
                                company_abbr = companyAbbr,
                                company_nama = companyNama,
                                dept = dept,
                                dept_ppro = deptPpro,
                                dept_abbr = deptAbbr,
                                dept_nama = deptNama,
                                divisi = divisi,
                                divisi_ppro = divisiPpro,
                                divisi_abbr = divisiAbbr,
                                divisi_nama = divisiNama,
                                blok_ppro = blokPpro,
                                blok_kode = blokKode,
                                blok_nama = blokNama
                            )

                            // Log the new entity
                            Log.d(
                                "AppRepository",
                                "Creating new HektarPanen: NIK=$nik, Block=$blokIdDate"
                            )

                            // Insert the new record
                            hektarPanenDao.insert(hektarPanen)
                        } else {
                            // Log the existing entity
                            Log.d(
                                "AppRepository",
                                "Updating existing HektarPanen: NIK=$nik, Block=$blokIdDate"
                            )

                            // Handle null/empty arrays safely
                            val existingTotalJjg =
                                hektarPanen.total_jjg_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingUnripe =
                                hektarPanen.unripe_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingOverripe =
                                hektarPanen.overripe_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingEmptyBunch =
                                hektarPanen.empty_bunch_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingAbnormal =
                                hektarPanen.abnormal_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingRipe =
                                hektarPanen.ripe_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingKirimPabrik =
                                hektarPanen.kirim_pabrik_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingDibayar =
                                hektarPanen.dibayar_arr.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingTphIds =
                                hektarPanen.tph_ids.takeUnless { it.isNullOrEmpty() } ?: ""
                            val existingDateCreated =
                                hektarPanen.date_created_panen.takeUnless { it.isNullOrEmpty() }
                                    ?: ""

                            // Append new values to existing ones
                            val updatedTotalJjg =
                                if (existingTotalJjg.isEmpty()) totalJjg.joinToString(";") else existingTotalJjg + ";" + totalJjg.joinToString(
                                    ";"
                                )
                            val updatedUnripe =
                                if (existingUnripe.isEmpty()) unripe.joinToString(";") else existingUnripe + ";" + unripe.joinToString(
                                    ";"
                                )
                            val updatedOverripe =
                                if (existingOverripe.isEmpty()) overripe.joinToString(";") else existingOverripe + ";" + overripe.joinToString(
                                    ";"
                                )
                            val updatedEmptyBunch =
                                if (existingEmptyBunch.isEmpty()) emptyBunch.joinToString(";") else existingEmptyBunch + ";" + emptyBunch.joinToString(
                                    ";"
                                )
                            val updatedAbnormal =
                                if (existingAbnormal.isEmpty()) abnormal.joinToString(";") else existingAbnormal + ";" + abnormal.joinToString(
                                    ";"
                                )
                            val updatedRipe =
                                if (existingRipe.isEmpty()) ripe.joinToString(";") else existingRipe + ";" + ripe.joinToString(
                                    ";"
                                )
                            val updatedKirimPabrik =
                                if (existingKirimPabrik.isEmpty()) kirimPabrik.joinToString(";") else existingKirimPabrik + ";" + kirimPabrik.joinToString(
                                    ";"
                                )
                            val updatedDibayar =
                                if (existingDibayar.isEmpty()) dibayar.joinToString(";") else existingDibayar + ";" + dibayar.joinToString(
                                    ";"
                                )
                            val updatedTphIds =
                                if (existingTphIds.isEmpty()) tphIds.joinToString(";") else existingTphIds + ";" + tphIds.joinToString(
                                    ";"
                                )
                            val updatedDateCreated =
                                if (existingDateCreated.isEmpty()) dateCreatedPanen.joinToString(";") else existingDateCreated + ";" + dateCreatedPanen.joinToString(
                                    ";"
                                )

                            // Create updated entity
                            val updatedHektarPanen = hektarPanen.copy(
                                total_jjg_arr = updatedTotalJjg,
                                unripe_arr = updatedUnripe,
                                overripe_arr = updatedOverripe,
                                empty_bunch_arr = updatedEmptyBunch,
                                abnormal_arr = updatedAbnormal,
                                ripe_arr = updatedRipe,
                                kirim_pabrik_arr = updatedKirimPabrik,
                                dibayar_arr = updatedDibayar,
                                tph_ids = updatedTphIds,
                                date_created_panen = updatedDateCreated
                            )

                            // Update the record
                            hektarPanenDao.update(updatedHektarPanen)
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "AppRepository",
                            "Error processing group for NIK: $nik, Block: $blokIdDate - ${e.message}"
                        )
                    }
                }

                // Return appropriate result based on success/failure
                when {
                    duplicates.isEmpty() -> {
                        Result.success(SaveTPHResult.AllSuccess(savedIds + updated.map { it.id.toLong() }))
                    }

                    savedIds.isEmpty() && updated.isEmpty() -> {
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.tph_id}, Date: ${it.date_created}"
                        }
                        Result.failure(Exception("All data is duplicate:\n$duplicateInfo"))
                    }

                    else -> {
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.tph_id}, Date: ${it.date_created}"
                        }
                        Result.success(
                            SaveTPHResult.PartialSuccess(
                                savedIds = savedIds + updated.map { it.id.toLong() },
                                duplicateCount = duplicates.size,
                                duplicateInfo = duplicateInfo
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error in saveScanMPanen: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Repository.kt
    suspend fun getAllJenisTPH(): Result<List<JenisTPHModel>> =
        withContext(Dispatchers.IO) {
            try {
                val data = jenisTPHDao.getAllJenisTPH()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateStatusUploadPanen(ids: List<Int>, statusUpload: Int) {
        panenDao.updateStatusUploadPanen(ids, statusUpload)
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return karyawanDao.getPemuatByIdList(idPemuat)
    }

    suspend fun getKemandoranById(idKemandoran: List<String>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranById(idKemandoran)
    }

    suspend fun getAllKaryawan(): Result<List<KaryawanModel>> = withContext(Dispatchers.IO) {
        try {
            val data = karyawanDao.getAllKaryawan()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatusUploadHektarPanen(ids: List<Int>, statusUpload: Int) {
        hektarPanenDao.updateStatusUploadHektarPanen(ids, statusUpload)
    }

    suspend fun saveTransferInspeksi(
        transferInspeksiList: List<PanenEntity>,
        createdBy: String,
        creatorInfo: String,
        context: Context
    ): Result<SaveTPHResult> = withContext(Dispatchers.IO) {
        try {
            val processedIds = mutableListOf<Long>()

            Log.d(
                "TransferInspeksi",
                "Starting saveTransferInspeksi with ${transferInspeksiList.size} items"
            )

            // Check each item individually
            for ((index, transferInspeksi) in transferInspeksiList.withIndex()) {
                Log.d(
                    "TransferInspeksi",
                    "Processing item $index: tph_id=${transferInspeksi.tph_id}, date_created=${transferInspeksi.date_created}"
                )

                // Check if this specific item exists based on tph_id and date_created
                val existingEntity = panenDao.findByTphAndDate(
                    transferInspeksi.tph_id,
                    transferInspeksi.date_created
                )

                if (existingEntity != null) {
                    // EXISTS: Update fields if they are empty/null and set status_scan_inspeksi = 1
                    Log.d(
                        "TransferInspeksi",
                        "EXISTING RECORD FOUND - ID: ${existingEntity.id}, updating fields and status_scan_inspeksi to 1"
                    )

                    val updatedRecord = existingEntity.copy(
                        // Always update status_scan_inspeksi to 1
                        status_scan_inspeksi = 1,

                        // Update kemandoran_id if existing is null/empty
                        kemandoran_id = if (existingEntity.kemandoran_id.isNullOrEmpty() || existingEntity.kemandoran_id == "NULL") {
                            transferInspeksi.kemandoran_id
                        } else {
                            existingEntity.kemandoran_id
                        },

                        // Update karyawan_nik if existing is null/empty
                        karyawan_nik = if (existingEntity.karyawan_nik.isNullOrEmpty() || existingEntity.karyawan_nik == "NULL") {
                            transferInspeksi.karyawan_nik
                        } else {
                            existingEntity.karyawan_nik
                        },

                        // Update karyawan_nama if existing is null/empty
                        karyawan_nama = if (existingEntity.karyawan_nama.isNullOrEmpty() || existingEntity.karyawan_nama == "NULL") {
                            transferInspeksi.karyawan_nama
                        } else {
                            existingEntity.karyawan_nama
                        },

                        // Update jenis_panen if existing is 0 (default/empty)
                        jenis_panen = if (existingEntity.jenis_panen == 0) {
                            transferInspeksi.jenis_panen
                        } else {
                            existingEntity.jenis_panen
                        },

                        // Update ancak if existing is 0 (default/empty)
                        ancak = if (existingEntity.ancak == 0) {
                            transferInspeksi.ancak
                        } else {
                            existingEntity.ancak
                        },

                        // Also update karyawan_id if existing is empty
                        karyawan_id = if (existingEntity.karyawan_id.isNullOrEmpty()) {
                            transferInspeksi.karyawan_id
                        } else {
                            existingEntity.karyawan_id
                        }
                    )

                    // Use the general update method instead of specific status update
                    panenDao.update(listOf(updatedRecord))
                    processedIds.add(existingEntity.id.toLong())

                    Log.d(
                        "TransferInspeksi",
                        "Successfully updated existing record ID: ${existingEntity.id}"
                    )
                    Log.d(
                        "TransferInspeksi",
                        "Updated fields - kemandoran_id: ${updatedRecord.kemandoran_id}, karyawan_nik: ${updatedRecord.karyawan_nik}, karyawan_nama: ${updatedRecord.karyawan_nama}, jenis_panen: ${updatedRecord.jenis_panen}, ancak: ${updatedRecord.ancak}"
                    )

                } else {
                    // DOESN'T EXIST: Insert new record
                    Log.d(
                        "TransferInspeksi",
                        "NEW RECORD - No existing record found, inserting new record"
                    )

                    val entityToSave = transferInspeksi.copy(
                        created_by = createdBy.toIntOrNull() ?: 0,
                        info = creatorInfo
                    )

                    val result = panenDao.insertWithTransaction(entityToSave)

                    result.fold(
                        onSuccess = { id ->
                            processedIds.add(id)
                            Log.d(
                                "TransferInspeksi",
                                "Successfully inserted new record with ID: $id"
                            )
                        },
                        onFailure = {
                            Log.e("TransferInspeksi", "Failed to insert new record: ${it.message}")
                            throw it
                        }
                    )
                }
            }

            Log.d(
                "TransferInspeksi",
                "Completed processing. Total processed IDs: ${processedIds.size}"
            )

            // Always return success
            Result.success(SaveTPHResult.AllSuccess(processedIds))

        } catch (e: Exception) {
            Log.e("TransferInspeksi", "Error in saveTransferInspeksi: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveTPHDataList(tphDataList: List<TphRvData>): Result<SaveTPHResult> =
        withContext(Dispatchers.IO) {
            try {
                AppLogger.d("=== SAVE TPH DATA LIST START ===")
                AppLogger.d("Total items to process: ${tphDataList.size}")

                // Keep track of successes and failures
                val savedIds = mutableListOf<Long>()
                val updatedIds = mutableListOf<Long>()
                val duplicates = mutableListOf<TphRvData>()

                // Check each item individually
                for ((index, tphData) in tphDataList.withIndex()) {
                    AppLogger.d("Processing item ${index + 1}/${tphDataList.size}: TPH=${tphData.namaBlok}, Date=${tphData.time}, JJG=${tphData.jjg}, User=${tphData.username}")

                    val existingRecord = panenDao.existsModel(tphData.namaBlok, tphData.time)

                    if (existingRecord != null) {
                        AppLogger.d("🔍 COMPARISON DETAILS for TPH=${tphData.namaBlok}:")
                        AppLogger.d("   namaBlok: '${tphData.namaBlok}' vs '${existingRecord.tph?.id}' → ${tphData.namaBlok == existingRecord.tph?.id.toString()}")
                        AppLogger.d("   time: '${tphData.time}' vs '${existingRecord.panen.date_created}' → ${tphData.time == existingRecord.panen.date_created}")
                        AppLogger.d("   nomor_pemanen: '${tphData.nomor_pemanen}' vs '${existingRecord.panen.nomor_pemanen}' → ${tphData.nomor_pemanen == existingRecord.panen.nomor_pemanen}")

                        // Helper function to check if a value should be ignored
                        fun shouldIgnoreValue(value: String?): Boolean {
                            return value == null || value.isEmpty() || value == "NULL"
                        }

                        // Only compare username if incoming value is not NULL/empty
                        val usernameMatches = if (shouldIgnoreValue(tphData.username)) {
                            true // Skip comparison if incoming is NULL/empty
                        } else {
                            tphData.username == existingRecord.panen.username
                        }
                        AppLogger.d("   username: '${tphData.username}' vs '${existingRecord.panen.username}' → $usernameMatches ${if (shouldIgnoreValue(tphData.username)) "(skipped - incoming NULL/empty)" else ""}")

                        val existingJJG = extractJJGFromJson(existingRecord.panen.jjg_json)
                        val jjgMatches = if (shouldIgnoreValue(tphData.jjg)) {
                            true // Skip comparison if incoming is NULL/empty
                        } else {
                            tphData.jjg == existingJJG
                        }
                        AppLogger.d("   jjg: '${tphData.jjg}' vs '$existingJJG' → $jjgMatches ${if (shouldIgnoreValue(tphData.jjg)) "(skipped - incoming NULL/empty)" else ""}")

                        val incomingTipePanen = if (shouldIgnoreValue(tphData.tipePanen)) null else tphData.tipePanen.toIntOrNull()
                        val tipePanenMatches = if (incomingTipePanen == null) {
                            true // Skip comparison if incoming is NULL/empty
                        } else {
                            incomingTipePanen == existingRecord.panen.jenis_panen
                        }
                        AppLogger.d("   tipePanen: '${tphData.tipePanen}' vs '${existingRecord.panen.jenis_panen}' → $tipePanenMatches ${if (incomingTipePanen == null) "(skipped - incoming NULL/empty)" else ""}")

                        val incomingAncak = if (shouldIgnoreValue(tphData.ancak)) null else tphData.ancak.toIntOrNull()
                        val ancakMatches = if (incomingAncak == null) {
                            true // Skip comparison if incoming is NULL/empty
                        } else {
                            incomingAncak == existingRecord.panen.ancak
                        }
                        AppLogger.d("   ancak: '${tphData.ancak}' vs '${existingRecord.panen.ancak}' → $ancakMatches ${if (incomingAncak == null) "(skipped - incoming NULL/empty)" else ""}")

                        val isExactDuplicate = (
                                tphData.namaBlok == existingRecord.tph?.id.toString() &&
                                        tphData.time == existingRecord.panen.date_created &&
                                        tphData.nomor_pemanen == existingRecord.panen.nomor_pemanen &&
                                        usernameMatches &&
                                        jjgMatches &&
                                        tipePanenMatches &&
                                        ancakMatches
                                )

                        if (isExactDuplicate) {
                            // Add to duplicates list
                            duplicates.add(tphData)
                            AppLogger.w("⚠️ Duplicate found: TPH=${tphData.namaBlok}, Date=${tphData.time}")
                        } else {
                            // Not duplicate, update the existing record (only update non-NULL values)
                            AppLogger.d("🔄 Data is different, updating existing record")
                            val updatedEntity = existingRecord.copy(
                                panen = existingRecord.panen.copy(
                                    nomor_pemanen = if (tphData.nomor_pemanen != 0)
                                        tphData.nomor_pemanen else existingRecord.panen.nomor_pemanen,
                                    username = if (!shouldIgnoreValue(tphData.username))
                                        tphData.username else existingRecord.panen.username,
                                    jjg_json = if (!shouldIgnoreValue(tphData.jjg))
                                        "{\"KP\": ${tphData.jjg}}" else existingRecord.panen.jjg_json,
                                    jenis_panen = if (incomingTipePanen != null) {
                                        incomingTipePanen
                                    } else existingRecord.panen.jenis_panen,
                                    ancak = if (incomingAncak != null) {
                                        incomingAncak
                                    } else existingRecord.panen.ancak,
                                    scan_status = 1
                                )
                            )

                            // Update record
                            panenDao.update(listOf(updatedEntity.panen))
                            updatedIds.add(existingRecord.panen.id.toLong())
                            AppLogger.d("✅ Successfully updated: TPH=${tphData.namaBlok}")
                        }
                    }else {
                        AppLogger.d("Creating new record for TPH=${tphData.namaBlok}, Date=${tphData.time}")

                        // Save non-duplicate
                        val result = panenDao.insertWithTransaction(
                            PanenEntity(
                                tph_id = tphData.namaBlok,
                                date_created = tphData.time,
                                created_by = 0,
                                karyawan_id = "",
                                kemandoran_id = "",
                                karyawan_nik = "",
                                karyawan_nama = "",
                                jjg_json = "{\"KP\": ${tphData.jjg}}",
                                foto = "",
                                komentar = "",
                                asistensi = 0,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = 0,
                                nomor_pemanen = tphData.nomor_pemanen,
                                ancak = 0,
                                info = "",
                                archive = 0,
                                status_espb = 0,
                                status_restan = 0,
                                scan_status = 1,
                                username = tphData.username
                            )
                        )

                        result.fold(
                            onSuccess = { id ->
                                savedIds.add(id)
                                AppLogger.d("✅ Successfully saved: TPH=${tphData.namaBlok}, ID=$id")
                            },
                            onFailure = {
                                AppLogger.e("❌ Failed to save: TPH=${tphData.namaBlok}, Error: ${it.message}")
                                throw it
                            }
                        )
                    }
                }

                AppLogger.d("=== PROCESSING SUMMARY ===")
                AppLogger.d("Total processed: ${tphDataList.size}")
                AppLogger.d("Successfully saved: ${savedIds.size}")
                AppLogger.d("Duplicates found: ${duplicates.size}")
                AppLogger.d("==========================")

                // Create result based on what happened
                // In your saveTPHDataList function, change the result handling:

                when {
                    duplicates.isEmpty() && updatedIds.isEmpty() -> {
                        // All items were saved as new records
                        AppLogger.d("All ${savedIds.size} items saved successfully!")
                        Result.success(SaveTPHResult.AllSuccess(savedIds))
                    }

                    duplicates.isEmpty() && savedIds.isEmpty() -> {
                        // All items were updates of existing records
                        AppLogger.d("All ${updatedIds.size} items were updates!")
                        Result.success(SaveTPHResult.AllSuccess(updatedIds)) // or create AllUpdated if you prefer
                    }

                    savedIds.isEmpty() && updatedIds.isEmpty() -> {
                        // Everything was a duplicate
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.namaBlok}, Date: ${it.time}"
                        }
                        AppLogger.w("All data is duplicate - returning as success to show alert")
                        AppLogger.w("Duplicate details:\n$duplicateInfo")
                        Result.success(
                            SaveTPHResult.AllDuplicate(
                                duplicateCount = duplicates.size,
                                duplicateInfo = duplicateInfo
                            )
                        )
                    }

                    else -> {
                        // Mixed results: saves, updates, and/or duplicates
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.namaBlok}, Date: ${it.time}"
                        }
                        AppLogger.w("Partial success: ${savedIds.size} saved, ${updatedIds.size} updated, ${duplicates.size} duplicates")
                        AppLogger.d("Saved IDs: $savedIds")
                        AppLogger.d("Updated IDs: $updatedIds")
                        AppLogger.w("Duplicate details:\n$duplicateInfo")
                        Result.success(
                            SaveTPHResult.PartialSuccess(
                                savedIds = savedIds + updatedIds, // combine both lists
                                duplicateCount = duplicates.size,
                                duplicateInfo = duplicateInfo
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("💥 Error saving TPH data: ${e.message}")
                AppLogger.e("Exception details: ${e.stackTraceToString()}")
                Result.failure(e)
            } finally {
                AppLogger.d("=== SAVE TPH DATA LIST END ===")
            }
        }

    private fun extractJJGFromJson(jjgJson: String): String {
        return try {
            val jsonObj = JSONObject(jjgJson)
            jsonObj.getString("KP")
        } catch (e: Exception) {
            ""
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

    suspend fun loadESPB(
        archive: Int,
        statusTransferRestan: Int,
        hasNoEspb: Boolean,
        scanStatus: Int,
        date: String? = null
    ): List<PanenEntityWithRelations> {
        return try {
            panenDao.loadESPB(archive, statusTransferRestan, hasNoEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun loadMutuBuah(
        statusUpload: Int,
        date: String? = null
    ): List<MutuBuahEntity> {
        return try {
            mutuBuahDao.loadMutuBuahByStatusUploadAndDate(statusUpload, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading MutuBuah: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun countMutuBuah(
        statusUpload: Int,
        date: String? = null
    ): Int {
        return try {
            mutuBuahDao.countMutuBuahByStatusUploadAndDate(statusUpload, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading MutuBuah: ${e.message}")
            0
        }
    }

    suspend fun countESPB(
        archive: Int,
        statusTransferRestan: Int,
        hasNoEspb: Boolean,
        scanStatus: Int,
        date: String? = null
    ): Int {
        return try {
            panenDao.countESPB(archive, statusTransferRestan, hasNoEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            0  // Return 0 if there's an error
        }
    }


    suspend fun updateStatusUploadInspeksiPanen(ids: List<Int>, statusUpload: Int) {
        inspectionDao.updateStatusUploadInspeksiPanen(ids, statusUpload)
    }

    suspend fun updateStatusUploadInspeksiDetailPanen(ids: List<Int>, statusUpload: Int) {
        inspectionDao.updateStatusUploadInspeksiDetailPanen(ids, statusUpload)
    }

    suspend fun updateStatusUploadMutuBuah(ids: List<Int>, statusUpload: Int) {
        mutuBuahDao.updateStatusUploadMutuBuah(ids, statusUpload)
    }

    suspend fun getAllHektarPanen(): Result<List<HektarPanenEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val data = hektarPanenDao.getAll()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateDataIsZippedPanen(ids: List<Int>, status: Int) {
        panenDao.updateDataIsZippedPanen(ids, status)
    }

    suspend fun getCompanyAbbrByTphId(id: Int): String? = withContext(Dispatchers.IO) {
        tphDao.geCompanyAbbrByTphId(id)
    }

    suspend fun updateDataIsZippedHP(ids: List<Int>, status: Int) {
        hektarPanenDao.updateDataIsZippedHP(ids, status)
    }

    suspend fun updateDataInspeksiIsZippedHP(ids: List<Int>, status: Int) {
        inspectionDao.updateDataIsZippedHP(ids, status)
    }

    suspend fun getBlokKodeByTphId(tphId: Int): String? = withContext(Dispatchers.IO) {
        tphDao.getBlokKodeByTphId(tphId)
    }

    suspend fun getTPHById(tphId: Int): TPHNewModel = withContext(Dispatchers.IO) {
        tphDao.getTPHById(tphId)
    }

    suspend fun getNamaByNik(nik: String): String? = withContext(Dispatchers.IO) {
        karyawanDao.getNamaByNik(nik)
    }

    suspend fun getNomorTPHbyId(tphId: Int): String? = withContext(Dispatchers.IO) {
        tphDao.getNomorTPHbyId(tphId)
    }

    suspend fun getPanenCount(): Int {
        return panenDao.getCount()
    }

    suspend fun getPanenCountForTransferInspeksi(): Int {
        return panenDao.getCountForTransferInspeksi()
    }

    suspend fun countWhereLuasPanenIsZeroAndDateToday(): Int {
        return hektarPanenDao.countWhereLuasPanenIsZeroAndDate()
    }

    suspend fun countWhereLuasPanenIsZeroAndDateAndBlok(blok: Int, date: String?): Int {
        return hektarPanenDao.countWhereLuasPanenIsZeroAndDateAndBlok(blok, date)
    }

    suspend fun getSumLuasPanen(blok: Int, date: String): Float {
        return hektarPanenDao.getSumLuasPanen(blok, date)
    }

    suspend fun updateLuasPanen(id: Int, luasPanen: Float): Int {
        return hektarPanenDao.updateLuasPanen(id, luasPanen)
    }

    suspend fun getLuasBlokByBlok(blok: Int): Float {
        return hektarPanenDao.getLuasBlokByBlok(blok)
    }

    suspend fun getDistinctBlokParamsByDate(date: String): List<HektarPanenDao.BlokParams> {
        return hektarPanenDao.getDistinctBlokParamsByDate(date)
    }

    suspend fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(
        date: String?,
        blok: Int?
    ): List<HektarPanenEntity> {
        return if (blok == null && date != null) {
            hektarPanenDao.getNikLuasPanenLuasBlokDibayarByDateAndBlok(date)
        } else if (date == null && blok != null) {
            hektarPanenDao.getNikLuasPanenLuasBlokDibayarByDateAndBlok(blok)
        } else if (date != null && blok != null) {
            hektarPanenDao.getNikLuasPanenLuasBlokDibayarByDateAndBlok(date, blok)
        } else {
            hektarPanenDao.getNikLuasPanenLuasBlokDibayarByDateAndBlok()
        }
    }

    suspend fun loadCountTPHESPB(
        archive: Int,
        statusEspb: Int,
        scanStatus: Int,
        date: String?
    ): Int {
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

    suspend fun getAllPanenWhereESPB(no_esp: String): Result<List<ESPBEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val data = espbDao.getAllPanenWhereESPB(no_esp)
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getActivePanen(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllActiveWithRelations()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getActivePanenESPB(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllActivePanenESPBWithRelations()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getActivePanenESPBAll(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllActivePanenESPBAll()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAllTPHHasBeenSelected(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllTPHHasBeenSelected()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun getAllTPHinWeek(estateId: Int): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllTPHinWeek(estateId)
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getTPHHasBeenInspect(): Result<List<InspectionModel>> =
        withContext(Dispatchers.IO) {
            try {
                val data = inspectionDao.getTPHHasBeenInspect()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAllPanenForInspection(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllPanenForInspection()
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getActivePanenRestan(status: Int = 0): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllAPanenRestan(status)
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun getArchivedPanen(): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
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

    suspend fun changeStatusTransferRestan(id: Int) = withContext(Dispatchers.IO) {
        panenDao.changeStatusTransferRestan(id)
    }

    suspend fun changeStatusTransferInspeksiPanen(id: Int) = withContext(Dispatchers.IO) {
        panenDao.changeStatusTransferInspeksiPanen(id)
    }

    suspend fun archiveMpanenByID(id: Int) = withContext(Dispatchers.IO) {
        panenDao.archiveMpanenByID(id)
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

    suspend fun updateTPH1AndBlokJjg(noespb: String, newTph1: String, newBlokJjg: String) =
        withContext(Dispatchers.IO) {
            espbDao.updateTPH1AndBlokJjg(noespb, newTph1, newBlokJjg)
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

    suspend fun getNopolList() = withContext(Dispatchers.IO) {
        kendaraanDao.getAll()
    }

    private fun transformTphDataToMap(inputData: String): Map<Int, Int> {
        val records = inputData.split(";")
        val result = mutableMapOf<Int, Int>()

        records.forEach { record ->
            val parts = record.split(",")
            if (parts.size >= 3) {
                try {
                    val tphId = parts[0].toInt()
                    val janjangCount = parts[2].toInt()

                    // If the TPH ID already exists, add to its janjang count
                    result[tphId] = result.getOrDefault(tphId, 0) + janjangCount
                } catch (e: NumberFormatException) {
                    // Ignore parsing errors
                }
            }
        }

        return result
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
                .filter { it.id != null && it.blok_ppro != null }
                .groupBy { it.blok_ppro!! }
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

    suspend fun updateESPBStatusForMultipleIds(
        idsList: List<Int>,
        status: Int,
        noESPB: String
    ): Int {
        return database.withTransaction {
            panenDao.updateESPBStatusByIds(idsList, status, noESPB)
        }
    }

    suspend fun panenUpdateStatusAngkut(idsList: List<Int>, status: Int): Int {
        return database.withTransaction {
            panenDao.panenUpdateStatusAngkut(idsList, status)
        }
    }

//    suspend fun getAllScanMPanenByDate(status_scan_mpanen: Int, date: String): Result<List<PanenEntityWithRelations>> = withContext(Dispatchers.IO) {
//        try {
//            val data = panenDao.getAllScanMPanenByDate(status_scan_mpanen, date)
//            Result.success(data)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    suspend fun getAllScanMPanenByDate(
        archiveMpanen: Int,
        date: String? = null
    ): List<PanenEntityWithRelations> = withContext(Dispatchers.IO) {
        try {
            panenDao.getAllScanMPanenByDate(archiveMpanen, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun getCountScanMPanen(status_scan_mpanen: Int = 0): Int {
        return try {
            panenDao.getCountScanMPanen(status_scan_mpanen)
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
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

    suspend fun getMBCountCreatedToday(): Int {
        return try {
            mutuBuahDao.getCountCreatedToday()
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
        }
    }

    // In your Repository class
    suspend fun getMutuBuahAll(): Result<List<MutuBuahEntity>> {
        return try {
            val data = mutuBuahDao.getAllMutuBuah() // You'll need to add this DAO method
            Result.success(data)
        } catch (e: Exception) {
            AppLogger.e("Error getting all MutuBuah: ${e.message}")
            Result.failure(e)
        }
    }

    fun getBlokById(listBlokId: List<Int>): List<BlokModel> {
        return blokDao.getDataByIdInBlok(listBlokId)
    }

    suspend fun fetchBlokbyParams(
        blockId: Int,
        blokPpro: Int?,
        dept: String?,
        divisi: String?
    ): Result<BlokModel?> = withContext(Dispatchers.IO) {
        try {
            var blokData: BlokModel? = null

            // Try with blok_ppro first if available
            if (blokPpro != null && dept != null && divisi != null) {
                blokData = blokDao.getBlokByEstAfdKode(blokPpro.toString(), dept, divisi)
                if (blokData != null) {
                    AppLogger.d("Blok found using blok_ppro: $blokPpro, dept: $dept, divisi: $divisi")
                }
            }

            // If not found and we have blockId, try with blockId
            if (blokData == null && dept != null && divisi != null) {
                blokData = blokDao.getBlokByIdEstAfd(blockId, dept, divisi)
                if (blokData != null) {
                    AppLogger.d("Blok found using blockId: $blockId, dept: $dept, divisi: $divisi")
                }
            }

            if (blokData == null) {
                AppLogger.d("No blok found with any parameter combination")
            }

            Result.success(blokData)
        } catch (e: Exception) {
            AppLogger.e("Error in fetchBlokbyParams: ${e.message}")
            Result.failure(e)
        }
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

    suspend fun addDataInspection(data: List<InspectionModel>): Result<List<Long>> {
        return try {
            val insertedIds = inspectionDao.insertAll(data)
            Result.success(insertedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun addPathDataInspection(data: InspectionDetailModel): Result<Long> {
//        return try {
//            val insertedId = inspectionPathDao.insert(data)
//            if (insertedId != -1L) {
//                Result.success(insertedId)
//            } else {
//                Result.failure(Exception("Insert failed"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

//    fun deleteInspectionDatas(ids: List<String>): Result<Unit> {
//        return try {
//            val deletedPath = inspectionPathDao.deleteByID(ids)
//            if (deletedPath > 0) {
//                Result.success(Unit)
//            } else {
//                Result.failure(Exception("Failed to delete one or both records"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

//    suspend fun getInspectionPathWithTphAndCount(pathId: String): PathWithInspectionTphRelations {
//        return inspectionPathDao.getInspectionPathWithTphAndCount(pathId)
//    }
//    suspend fun getInspectionCountCard(archive: Int): Int {
//        return inspectionDao.countCard(archive)
//    }

//    suspend fun getInspectionPathsWithTphAndCount(archive: Int): List<PathWithInspectionTphRelations> {
//        return inspectionPathDao.getInspectionPathsWithTphAndCount(archive)
//    }
//
//    suspend fun getInspectionPathWithTphAndCount(pathId: String): PathWithInspectionTphRelations {
//        return inspectionPathDao.getInspectionPathWithTphAndCount(pathId)
//    }

}