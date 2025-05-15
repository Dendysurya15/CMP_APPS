package com.cbi.mobile_plantation.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.PathWithInspectionTphRelations
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.mobile_plantation.utils.AppLogger
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
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
}

class AppRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val panenDao = database.panenDao()
    private val espbDao = database.espbDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val transporterDao = database.transporterDao()
    private val inspectionDao = database.inspectionDao()
    private val inspectionPathDao = database.inspectionPathDao()
    private val kendaraanDao = database.kendaraanDao()
    private val hektarPanenDao = database.hektarPanenDao()
    private val jenisTPHDao = database.jenisTPHDao()


    sealed class SaveResultPanen {
        object Success : SaveResultPanen()
        data class Error(val exception: Exception) : SaveResultPanen()
    }

    suspend fun saveDataPanen(data: PanenEntity) {
        panenDao.insert(data)
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
                val hektarPanenDao = database.hektarPanenDao()

                // Step 1: First, save all PanenEntity records to the panen table
                for (tphData in tphDataList) {
                    // Check if this specific item is a duplicate
                    val isDuplicate = panenDao.exists(tphData.tph_id, tphData.date_created)

                    if (isDuplicate) {
                        // Add to duplicates list
                        duplicates.add(tphData)
                    } else {
                        // Save non-duplicate
                        val result = panenDao.insertWithTransaction(tphData)

                        result.fold(
                            onSuccess = { id -> savedIds.add(id) },
                            onFailure = { throw it }
                        )
                    }
                }

                // Step 2: Group by unique (NIK, Block) combination
                // Use a map of Pair<NIK, Block> -> List of PanenEntity
                val groupedByNikAndBlock =
                    mutableMapOf<Pair<String, String>, MutableList<PanenEntity>>()

                // Add debug logging for tphDataList
                Log.d(
                    "AppRepository",
                    "Processing ${tphDataList.size} TPH entries, with ${duplicates.size} duplicates"
                )

                for (tphData in tphDataList) {
                    if (duplicates.contains(tphData)) continue
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
                                    roundToOneDecimal(
                                        jjgJson.optInt("TO", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                unripe.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("UN", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                overripe.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("OV", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                emptyBunch.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("EM", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                abnormal.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("AB", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                ripe.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("RI", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                kirimPabrik.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("KP", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
                                )
                                dibayar.add(
                                    roundToOneDecimal(
                                        jjgJson.optInt("PA", 0)
                                            .toFloat() / entity.jumlah_pemanen.toFloat()
                                    )
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

                            // Get karyawan model to extract kemandoran details
                            val karyawanModel = try {
                                karyawanDao.getAllKaryawan().find { it.nik == nik }
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting karyawan model: ${e.message}")
                                null
                            }

                            // Extract kemandoran details
                            val kemandoranId = try {
                                karyawanModel?.kemandoran_id?.toString() ?: "0"
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting kemandoran_id: ${e.message}")
                                "0"
                            }

                            // Get karyawan model to extract kemandoran details
                            val kemandoranData = try {
                                kemandoranDao.getKemandoranByTheId(kemandoranId.toInt())
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting karyawan model: ${e.message}")
                                null
                            }

                            // Extract blok_nama with error handling
                            val kemandoranNama = try {
                                kemandoranData!!.nama
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok_nama: ${e.message}")
                                "NULL"
                            }

//                            // Extract blok_nama with error handling
//                            val kemandoranPpro = try {
//                                kemandoranData!!.nama
//                            } catch (e: Exception) {
//                                Log.e("AppRepository", "Error getting blok_nama: ${e.message}")
//                                "NULL"
//                            }

                            // Extract blok_nama with error handling
                            val kemandoranKode = try {
                                kemandoranData!!.kode
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error getting blok_nama: ${e.message}")
                                "NULL"
                            }

                            // Create the HektarPanenEntity
                            hektarPanen = HektarPanenEntity(
                                id = null,
                                nik = nik,
                                pemanen_nama = pemanen!!,
                                kemandoran_id = kemandoranId,
                                kemandoran_nama = kemandoranNama!!,
//                                kemandoran_ppro = kemandoranPpro!!,
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
                        Result.success(SaveTPHResult.AllSuccess(savedIds))
                    }

                    savedIds.isEmpty() -> {
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
                                savedIds = savedIds,
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

    suspend fun saveTPHDataList(tphDataList: List<TphRvData>): Result<SaveTPHResult> =
        withContext(Dispatchers.IO) {
            try {
                // Keep track of successes and failures
                val savedIds = mutableListOf<Long>()
                val duplicates = mutableListOf<TphRvData>()

                // Check each item individually
                for (tphData in tphDataList) {
                    // Check if this specific item is a duplicate
                    val isDuplicate = panenDao.exists(tphData.namaBlok, tphData.time)

                    if (isDuplicate) {
                        // Add to duplicates list
                        duplicates.add(tphData)
                    } else {
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
                                ancak = 0,
                                info = "",
                                archive = 0,
                                status_espb = 0,
                                status_restan = 0,
                                scan_status = 1,
//                                username = tphData.username
                            )
                        )

                        result.fold(
                            onSuccess = { id -> savedIds.add(id) },
                            onFailure = { throw it }
                        )
                    }
                }

                // Create result based on what happened
                when {
                    duplicates.isEmpty() -> {
                        // All items were saved successfully
                        Result.success(SaveTPHResult.AllSuccess(savedIds))
                    }

                    savedIds.isEmpty() -> {
                        // Everything was a duplicate
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.namaBlok}, Date: ${it.time}"
                        }
                        Result.failure(
                            Exception("All data is duplicate:\n$duplicateInfo")
                        )
                    }

                    else -> {
                        // We had partial success
                        val duplicateInfo = duplicates.joinToString("\n") {
                            "TPH ID: ${it.namaBlok}, Date: ${it.time}"
                        }
                        Result.success(
                            SaveTPHResult.PartialSuccess(
                                savedIds = savedIds,
                                duplicateCount = duplicates.size,
                                duplicateInfo = duplicateInfo
                            )
                        )
                    }
                }
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

    suspend fun loadESPB(
        archive: Int,
        statusEspb: Int,
        scanStatus: Int,
        date: String? = null
    ): List<PanenEntityWithRelations> {
        return try {
            panenDao.loadESPB(archive, statusEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun countESPB(
        archive: Int,
        statusEspb: Int,
        scanStatus: Int,
        date: String? = null
    ): Int {
        return try {
            panenDao.countESPB(archive, statusEspb, scanStatus, date)
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            0  // Return 0 if there's an error
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

    suspend fun getBlokKodeByTphId(tphId: Int): String? = withContext(Dispatchers.IO) {
        tphDao.getBlokKodeByTphId(tphId)
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

    suspend fun getDistinctBlokByDate(date: String): List<Int> {
        return hektarPanenDao.getDistinctBlokByDate(date)
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

    suspend fun getAllPanenWhereESPB(no_esp: String): Result<List<PanenEntityWithRelations>> =
        withContext(Dispatchers.IO) {
            try {
                val data = panenDao.getAllPanenWhereESPB(no_esp)
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

    fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
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

    suspend fun addDataInspection(data: List<InspectionModel>): Result<List<Long>> {
        return try {
            val insertedIds = inspectionDao.insertAll(data)
            Result.success(insertedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPathDataInspection(data: InspectionPathModel): Result<Long> {
        return try {
            val insertedId = inspectionPathDao.insert(data)
            if (insertedId != -1L) {
                Result.success(insertedId)
            } else {
                Result.failure(Exception("Insert failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteInspectionDatas(ids: List<String>): Result<Unit> {
        return try {
            val deletedPath = inspectionPathDao.deleteByID(ids)
            if (deletedPath > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete one or both records"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInspectionCountCard(archive: Int): Int {
        return inspectionDao.countCard(archive)
    }

    suspend fun getInspectionPathsWithTphAndCount(archive: Int): List<PathWithInspectionTphRelations> {
        return inspectionPathDao.getInspectionPathsWithTphAndCount(archive)
    }

    suspend fun getInspectionPathWithTphAndCount(pathId: String): PathWithInspectionTphRelations {
        return inspectionPathDao.getInspectionPathWithTphAndCount(pathId)
    }

    // Add this helper function at the top of your AppRepository class
    private fun roundToOneDecimal(value: Float): String {
        return BigDecimal(value.toDouble())
            .setScale(2, RoundingMode.HALF_EVEN)
            .toString()
    }

}