package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _inspectionList = MutableLiveData<List<InspectionModel>>()
    val inspectionList: LiveData<List<InspectionModel>> get() = _inspectionList

    private val _inspectionWithDetails = MutableLiveData<List<InspectionWithDetailRelations>>()
    val inspectionWithDetails: LiveData<List<InspectionWithDetailRelations>> =
        _inspectionWithDetails

    data class InspectionParameterItem(
        val id: Int,
        val nama: String,
        val status_ppro: Int,
        val undivided: String,
        val temuan_pokok: Int
    )

    sealed class SaveDataInspectionState {
        data class Success(val inspectionId: Long) : SaveDataInspectionState()
        data class Error(val message: String) : SaveDataInspectionState()
    }

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    fun loadInspectionById(inspectionId: String) {
        viewModelScope.launch {
            _inspectionWithDetails.value = repository.getInspectionById(inspectionId)
        }
    }

    suspend fun getKemandoranByNik(nikList: List<String>): List<KaryawanModel> {
        return withContext(Dispatchers.IO) {  // Run on background thread
            repository.getKemandoranByNik(nikList)
        }
    }

    suspend fun getAfdelingName(afdelingId: Int): String? {
        return repository.getAfdelingName(afdelingId)
    }

    suspend fun getParameterInspeksiJson(): List<InspectionParameterItem> {
        val jsonString = repository.getParameterInspeksiJson()
        AppLogger.d("Repository returned: $jsonString")

        if (jsonString.isNullOrEmpty()) {
            throw Exception("Parameter inspeksi JSON is null or empty")
        }

        val gson = Gson()
        val type = object : TypeToken<List<InspectionParameterItem>>() {}.type
        val result = gson.fromJson<List<InspectionParameterItem>>(jsonString, type)
        AppLogger.d("Parsed result: $result")
        return result ?: throw Exception("Failed to parse parameter inspeksi JSON")
    }

    suspend fun loadInspectionCount(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ): Int {
        return repository.getInspectionCount(datetime, isPushedToServer)
    }

    fun updateDataIsZippedHP(ids: List<Int>, status: Int) {

        AppLogger.d(ids.toString())
        AppLogger.d("masuk gak sih ")
        viewModelScope.launch {
            try {
                repository.updateDataInspeksiIsZippedHP(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun loadInspectionPaths(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ) {
        viewModelScope.launch {
            _inspectionWithDetails.value = repository.getInspectionData(datetime, isPushedToServer)
        }
    }

    fun updateStatusUploadInspeksiPanen(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadInspeksiPanen(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating status_upload: ${e.message}")
            }
        }
    }

    fun updateStatusUploadInspeksiDetailPanen(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadInspeksiDetailPanen(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating status_upload: ${e.message}")
            }
        }
    }


    suspend fun saveDataInspection(
        created_date_start: String,
        created_by: String,
        created_name: String,
        tph_id: Int,
        id_panen: String,
        date_panen: String,
        jalur_masuk: String,
        jenis_kondisi: Int,
        baris: String,
        foto_user:String,
        inspeksi_putaran: Int? = 1,
        jml_pkk_inspeksi: Int,
        tracking_path: String,
        app_version: String,
        status_upload: String,
        status_uploaded_image: String,
        // New parameters for follow-up
        isFollowUp: Boolean = false,
        existingInspectionId: Int? = null,
        tracking_path_pemulihan: String? = null,
        updated_date_start: String? = null,
        updated_date_end: String? = null,
        updated_by: String? = null,
        updated_name: String? = null,
        app_version_pemulihan: String? = null
    ): SaveDataInspectionState {
        return try {
            if (isFollowUp && existingInspectionId != null) {
                // Update existing inspection for follow-up
                val success = repository.updateInspectionForFollowUp(
                    inspectionId = existingInspectionId,
                    tracking_path_pemulihan = tracking_path_pemulihan,
                    inspeksi_putaran = 2,
                    updated_date_start = updated_date_start ?: "",
                    updated_date_end = updated_date_end ?: "",
                    updated_by = updated_by ?: "",
                    updated_name = updated_name ?: "",
                    app_version_pemulihan = app_version_pemulihan ?: ""
                )

                if (success) {
                    SaveDataInspectionState.Success(existingInspectionId.toLong())
                } else {
                    SaveDataInspectionState.Error("Failed to update inspection")
                }
            } else {

                val inspectionData = InspectionModel(
                    created_date = created_date_start,
                    created_by = created_by,
                    created_name = created_name,
                    tph_id = tph_id,
                    id_panen = id_panen,
                    date_panen = date_panen,
                    jalur_masuk = jalur_masuk,
                    jenis_kondisi = jenis_kondisi,
                    baris = baris,
                    foto_user = foto_user,
                    inspeksi_putaran = inspeksi_putaran,
                    jml_pkk_inspeksi = jml_pkk_inspeksi,
                    tracking_path = tracking_path,
                    app_version = app_version,
                    status_upload = status_upload,
                    status_uploaded_image = status_uploaded_image
                )

                val inspectionId = repository.insertInspectionData(inspectionData)
                SaveDataInspectionState.Success(inspectionId)
            }
        } catch (e: Exception) {
            SaveDataInspectionState.Error(e.toString())
        }
    }

    suspend fun updateDataInspectionDetailsForFollowUp(
        detailInspeksiList: List<InspectionDetailModel>,
        formData: Map<Int, FormAncakViewModel.PageData>,
        jumBrdTglPath: Int,
        jumBuahTglPath: Int,
        parameterInspeksi: List<InspectionParameterItem>,
        createdDateStart: String,
        createdName: String,
        createdBy: String
    ): SaveDataInspectionDetailsState {
        return try {
            if (parameterInspeksi.isEmpty()) {
                AppLogger.w("No parameter inspeksi found, cannot update inspection details")
                return SaveDataInspectionDetailsState.Error("No parameter inspeksi data found")
            }

            // Create mapping dynamically from database parameter
            data class InspectionMapping(
                val kodeInspeksi: Int,
                val getValue: (FormAncakViewModel.PageData, Int, Int) -> Int,
                val statusPpro: Int,
                val nama: String,
                val temuanPokok: Int,
                val undivided: String
            )

            // Regular pokok mappings (exclude 5, 6, 9, and 10 - we'll handle pruning separately)
            val regularPokokMappings = listOf(
                InspectionMapping(
                    1, { pageData, _, _ -> pageData.brdKtpGawangan },
                    parameterInspeksi.find { it.id == 1 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 1 }?.nama ?: AppUtils.kodeInspeksi.brondolanDigawangan,
                    parameterInspeksi.find { it.id == 1 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 1 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    2, { pageData, _, _ -> pageData.brdKtpPiringanPikulKetiak },
                    parameterInspeksi.find { it.id == 2 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 2 }?.nama ?: AppUtils.kodeInspeksi.brondolanTidakDikutip,
                    parameterInspeksi.find { it.id == 2 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 2 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    3, { pageData, _, _ -> pageData.buahMasakTdkDipotong },
                    parameterInspeksi.find { it.id == 3 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 3 }?.nama ?: AppUtils.kodeInspeksi.buahMasakTidakDipotong,
                    parameterInspeksi.find { it.id == 3 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 3 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    4, { pageData, _, _ -> pageData.btPiringanGawangan },
                    parameterInspeksi.find { it.id == 4 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 4 }?.nama ?: AppUtils.kodeInspeksi.buahTertinggalPiringan,
                    parameterInspeksi.find { it.id == 4 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 4 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    7, { pageData, _, _ -> if (pageData.neatPelepah == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 7 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 7 }?.nama ?: AppUtils.kodeInspeksi.susunanPelepahTidakSesuai,
                    parameterInspeksi.find { it.id == 7 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 7 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    8, { pageData, _, _ -> if (pageData.pelepahSengkleh == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 8 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 8 }?.nama ?: AppUtils.kodeInspeksi.terdapatPelepahSengkleh,
                    parameterInspeksi.find { it.id == 8 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 8 }?.undivided ?: "True"
                )
            )

            // Add pruning mappings (codes 9 and 10)
            val pruningMappings = listOf(
                InspectionMapping(
                    9, { pageData, _, _ -> if (pageData.kondisiPruning == 2) 1 else 0 }, // Over Pruning
                    parameterInspeksi.find { it.id == 9 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 9 }?.nama ?: AppUtils.kodeInspeksi.overPruning,
                    parameterInspeksi.find { it.id == 9 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 9 }?.undivided ?: "True"
                ),
                InspectionMapping(
                    10, { pageData, _, _ -> if (pageData.kondisiPruning == 3) 1 else 0 }, // Under Pruning
                    parameterInspeksi.find { it.id == 10 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 10 }?.nama ?: AppUtils.kodeInspeksi.underPruning,
                    parameterInspeksi.find { it.id == 10 }?.temuan_pokok ?: 1,
                    parameterInspeksi.find { it.id == 10 }?.undivided ?: "True"
                )
            )

            // Combine all mappings
            val allMappings = regularPokokMappings + pruningMappings

            var updatedCount = 0

            // Log all pageData before processing
            AppLogger.d("=== LOGGING ALL PAGE DATA BEFORE UPDATE ===")
            formData.forEach { (pageNumber, pageData) ->
                AppLogger.d("Page $pageNumber: $pageData")
            }
            AppLogger.d("=== END PAGE DATA LOGGING ===")

            // Also create a lookup for counting unique people per no_pokok
            val uniquePeoplePerNoPokok = detailInspeksiList
                .groupBy { it.no_pokok }
                .mapValues { (_, details) ->
                    details.map { "${it.nama}_${it.nik}" }.distinct().size
                }

            AppLogger.d("=== GROUPING INFO ===")
            uniquePeoplePerNoPokok.forEach { (noPokok, peopleCount) ->
                AppLogger.d("no_pokok $noPokok has $peopleCount unique people")
            }
            AppLogger.d("=== END GROUPING INFO ===")

            // Main loop through each detail inspection
            detailInspeksiList.forEach { detail ->
                AppLogger.d("Processing detail ID: ${detail.id}, no_pokok: ${detail.no_pokok}, kode_inspeksi: ${detail.kode_inspeksi}, nama: ${detail.nama}, nik: ${detail.nik}")

                // Handle special case for no_pokok = 0 (codes 5 and 6)
                if (detail.no_pokok == 0) {
                    when (detail.kode_inspeksi) {
                        5 -> {
                            // Find matching pageData with pokokNumber = 0
                            val matchingPageData = formData.values.find { it.pokokNumber == 0 && it.emptyTree == 1 }

                            val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                                inspectionDetailId = detail.id,
                                temuanInspeksi = jumBuahTglPath.toDouble(),
                                fotoPemulihan = matchingPageData?.photo,
                                komentarPemulihan = matchingPageData?.comment,
                                latPemulihan = matchingPageData?.latIssue ?: 0.0,
                                lonPemulihan = matchingPageData?.lonIssue ?: 0.0,
                                updatedDate = createdDateStart,
                                statusPemulihan = 0,
                                updatedName = createdName,
                                updatedBy = createdBy
                            )

                            if (updateSuccess) {
                                updatedCount++
                                AppLogger.d("Updated detail ID: ${detail.id}, no_pokok 0, Code 5, Value ${jumBuahTglPath.toDouble()}")
                            }
                        }
                        6 -> {
                            // Find matching pageData with pokokNumber = 0
                            val matchingPageData = formData.values.find { it.pokokNumber == 0 && it.emptyTree == 1 }

                            val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                                inspectionDetailId = detail.id,
                                temuanInspeksi = jumBrdTglPath.toDouble(),
                                fotoPemulihan = matchingPageData?.photo,
                                komentarPemulihan = matchingPageData?.comment,
                                latPemulihan = matchingPageData?.latIssue ?: 0.0,
                                lonPemulihan = matchingPageData?.lonIssue ?: 0.0,
                                updatedDate = createdDateStart,
                                statusPemulihan = 0,
                                updatedName = createdName,
                                updatedBy = createdBy
                            )

                            if (updateSuccess) {
                                updatedCount++
                                AppLogger.d("Updated detail ID: ${detail.id}, no_pokok 0, Code 6, Value ${jumBrdTglPath.toDouble()}")
                            }
                        }
                        else -> {
                            AppLogger.d("Skipping detail ID: ${detail.id} - no_pokok 0 but kode_inspeksi ${detail.kode_inspeksi} is not 5 or 6")
                        }
                    }
                } else {
                    // Handle regular pokok (no_pokok > 0)
                    // Find matching pageData with pokokNumber = detail.no_pokok
                    val matchingPageData = formData.values.find {
                        it.pokokNumber == detail.no_pokok && it.emptyTree == 1
                    }

                    if (matchingPageData == null) {
                        AppLogger.d("No matching pageData found for no_pokok: ${detail.no_pokok} or emptyTree != 1")
                        return@forEach
                    }

                    // Find the mapping for this kode_inspeksi
                    val mapping = allMappings.find { it.kodeInspeksi == detail.kode_inspeksi }

                    if (mapping == null) {
                        AppLogger.d("No mapping found for kode_inspeksi: ${detail.kode_inspeksi}")
                        return@forEach
                    }

                    // Get the raw value from pageData
                    val rawValue = mapping.getValue(matchingPageData, jumBrdTglPath, jumBuahTglPath)

                    // For pruning codes (9 and 10), log the kondisiPruning value
                    if (detail.kode_inspeksi == 9 || detail.kode_inspeksi == 10) {
                        AppLogger.d("Pruning check - Page ${detail.no_pokok}, kondisiPruning: ${matchingPageData.kondisiPruning}, kode_inspeksi: ${detail.kode_inspeksi}, rawValue: $rawValue")
                    }

                    // Skip if the value is 0 (this handles the kondisiPruning = 1 (Normal) case)
                    if (rawValue == 0) {
                        AppLogger.d("Skipping detail ID: ${detail.id} - rawValue is 0")
                        return@forEach
                    }

                    // Calculate the final value based on undivided setting
                    val finalValue = if (mapping.undivided == "True") {
                        // Get the count of unique people in this no_pokok
                        val uniquePeopleCount = uniquePeoplePerNoPokok[detail.no_pokok] ?: 1
                        rawValue.toDouble() / uniquePeopleCount
                    } else {
                        // Give full value to each person
                        rawValue.toDouble()
                    }

                    // Update the inspection detail record
                    val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                        inspectionDetailId = detail.id,
                        temuanInspeksi = finalValue,
                        fotoPemulihan = matchingPageData.photo,
                        komentarPemulihan = matchingPageData.comment,
                        latPemulihan = matchingPageData.latIssue ?: 0.0,
                        lonPemulihan = matchingPageData.lonIssue ?: 0.0,
                        statusPemulihan = 1,
                        updatedDate = matchingPageData.createdDate ?: createdDateStart,
                        updatedName = matchingPageData.createdName ?: createdName,
                        updatedBy = matchingPageData.createdBy?.toString() ?: createdBy
                    )

                    if (updateSuccess) {
                        updatedCount++
                    }
                }
            }

            AppLogger.d("Total inspection details updated: $updatedCount")
            SaveDataInspectionDetailsState.Success(updatedCount)

        } catch (e: Exception) {
            AppLogger.e("Error updating inspection details for follow-up: ${e.message}")
            SaveDataInspectionDetailsState.Error(e.toString())
        }
    }

    suspend fun saveDataInspectionDetails(
        inspectionId: String,
        formData: Map<Int, FormAncakViewModel.PageData>,
        totalPages: Int,
        selectedKaryawanList: List<FormInspectionActivity.KaryawanInfo>,
        jumBrdTglPath: Int,
        jumBuahTglPath: Int,
        parameterInspeksi: List<InspectionParameterItem>,
        createdDate: String,
        createdName: String,
        createdBy: String,
        latTPH: Double,
        lonTPH: Double,
        foto: String? = null,
        komentar: String,
    ): SaveDataInspectionDetailsState {
        return try {
            val inspectionDetailList = mutableListOf<InspectionDetailModel>()

            // Get karyawan count for division calculations
            val karyawanCount = selectedKaryawanList.size
            AppLogger.d("Karyawan count for division: $karyawanCount")

            if (karyawanCount == 0) {
                AppLogger.w("No karyawan found, cannot save inspection details")
                return SaveDataInspectionDetailsState.Error("No karyawan data found")
            }

            if (parameterInspeksi.isEmpty()) {
                AppLogger.w("No parameter inspeksi found, cannot save inspection details")
                return SaveDataInspectionDetailsState.Error("No parameter inspeksi data found")
            }

            // Create mapping dynamically from database parameter
            data class InspectionMapping(
                val kodeInspeksi: Int,
                val getValue: (FormAncakViewModel.PageData, Int, Int) -> Int,
                val statusPpro: Int,
                val nama: String,
                val temuanPokok: Int
            )

            // Regular pokok mappings (exclude 5, 6, 9, and 10 - we'll handle pruning separately)
            val regularPokokMappings = listOf(
                InspectionMapping(
                    1, { pageData, _, _ -> pageData.brdKtpGawangan },
                    parameterInspeksi.find { it.id == 1 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 1 }?.nama
                        ?: AppUtils.kodeInspeksi.brondolanDigawangan,
                    parameterInspeksi.find { it.id == 1 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    2, { pageData, _, _ -> pageData.brdKtpPiringanPikulKetiak },
                    parameterInspeksi.find { it.id == 2 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 2 }?.nama
                        ?: AppUtils.kodeInspeksi.brondolanTidakDikutip,
                    parameterInspeksi.find { it.id == 2 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    3, { pageData, _, _ -> pageData.buahMasakTdkDipotong },
                    parameterInspeksi.find { it.id == 3 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 3 }?.nama
                        ?: AppUtils.kodeInspeksi.buahMasakTidakDipotong,
                    parameterInspeksi.find { it.id == 3 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    4, { pageData, _, _ -> pageData.btPiringanGawangan },
                    parameterInspeksi.find { it.id == 4 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 4 }?.nama
                        ?: AppUtils.kodeInspeksi.buahTertinggalPiringan,
                    parameterInspeksi.find { it.id == 4 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    7, { pageData, _, _ -> if (pageData.neatPelepah == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 7 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 7 }?.nama
                        ?: AppUtils.kodeInspeksi.susunanPelepahTidakSesuai,
                    parameterInspeksi.find { it.id == 7 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    8, { pageData, _, _ -> if (pageData.pelepahSengkleh == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 8 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 8 }?.nama
                        ?: AppUtils.kodeInspeksi.terdapatPelepahSengkleh,
                    parameterInspeksi.find { it.id == 8 }?.temuan_pokok ?: 1
                )
            )

            // Add pruning mappings (codes 9 and 10)
            val pruningMappings = listOf(
                InspectionMapping(
                    9, { pageData, _, _ -> if (pageData.kondisiPruning == 2) 1 else 0 }, // Over Pruning
                    parameterInspeksi.find { it.id == 9 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 9 }?.nama ?: AppUtils.kodeInspeksi.overPruning,
                    parameterInspeksi.find { it.id == 9 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    10, { pageData, _, _ -> if (pageData.kondisiPruning == 3) 1 else 0 }, // Under Pruning
                    parameterInspeksi.find { it.id == 10 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 10 }?.nama
                        ?: AppUtils.kodeInspeksi.underPruning,
                    parameterInspeksi.find { it.id == 10 }?.temuan_pokok ?: 1
                )
            )

            // Combine all mappings
            val allMappings = regularPokokMappings + pruningMappings

            AppLogger.d("Created ${allMappings.size} inspection mappings from database parameters (including pruning)")

            // More efficient approach: Loop through formData instead of 1..totalPages
            formData.forEach { (pageNumber, pageData) ->
                val emptyTreeValue = pageData.emptyTree

                if (emptyTreeValue != 1) {
                    AppLogger.d("Skipping page $pageNumber - emptyTree is not 1 (value: $emptyTreeValue)")
                    return@forEach
                }

                AppLogger.d("Processing page $pageNumber with ${selectedKaryawanList.size} karyawan")

                selectedKaryawanList.forEach { karyawan ->
                    allMappings.forEach { mapping ->
                        val rawValue = mapping.getValue(pageData, jumBrdTglPath, jumBuahTglPath)

                        AppLogger.d("DEBUG: Page $pageNumber, Code ${mapping.kodeInspeksi} (${mapping.nama}): rawValue = $rawValue")

                        // For pruning codes, log the kondisiPruning value
                        if (mapping.kodeInspeksi == 9 || mapping.kodeInspeksi == 10) {
                            AppLogger.d("Pruning check - Page $pageNumber, kondisiPruning: ${pageData.kondisiPruning}, kode_inspeksi: ${mapping.kodeInspeksi}, rawValue: $rawValue")
                        }

                        val undivided = parameterInspeksi.find { it.id == mapping.kodeInspeksi }?.undivided ?: "True"
                        val dividedValue = if (undivided == "False") {
                            rawValue.toDouble()
                        } else {
                            rawValue.toDouble() / karyawanCount.toDouble()
                        }

                        // Skip if temuan_inspeksi value is 0 (this handles kondisiPruning = 1 (Normal) case)
                        if (dividedValue == 0.0) {
                            AppLogger.d("Skipping save for page $pageNumber, karyawan ${karyawan.nama}, code ${mapping.kodeInspeksi} - temuan_inspeksi is 0")
                            return@forEach
                        }

                        AppLogger.d("Page $pageNumber, Karyawan ${karyawan.nama}, Code ${mapping.kodeInspeksi} (${mapping.nama}): $rawValue / $karyawanCount = $dividedValue")

                        val inspectionDetail = InspectionDetailModel(
                            id_inspeksi = inspectionId,
                            created_date = pageData.createdDate ?: "",
                            created_name = pageData.createdName ?: "",
                            created_by = pageData.createdBy.toString(),
                            nik = karyawan.nik,
                            nama = karyawan.nama,
                            no_pokok = pageNumber,
                            pokok_panen = pageData.harvestTree,
                            kode_inspeksi = mapping.kodeInspeksi,
                            temuan_inspeksi = dividedValue,
                            status_pemulihan = pageData.status_pemulihan ?: 0,
                            foto = pageData.photo,
                            foto_pemulihan = null,
                            komentar = pageData.comment,
                            latIssue = pageData.latIssue ?: 0.0,
                            lonIssue = pageData.lonIssue ?: 0.0,
                            status_upload = "0",
                            status_uploaded_image = "0"
                        )

                        inspectionDetailList.add(inspectionDetail)
                        AppLogger.d("Added inspection detail: Page $pageNumber, Karyawan ${karyawan.nama}, Code ${mapping.kodeInspeksi}, Value $dividedValue")
                    }
                }
            }

            // Handle special case for no_pokok = 0 (kode_inspeksi 5 and 6 only)
            if (jumBuahTglPath != 0) {
                val inspectionDetail = InspectionDetailModel(
                    id_inspeksi = inspectionId,
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    nik = "",
                    nama = "",
                    no_pokok = 0,
                    pokok_panen = null,
                    kode_inspeksi = 5,
                    temuan_inspeksi = jumBuahTglPath.toDouble(),
                    status_pemulihan = 0,
                    foto = foto,
                    komentar = komentar,
                    latIssue = latTPH,
                    lonIssue = lonTPH,
                    status_upload = "0",
                    status_uploaded_image = "0"
                )
                inspectionDetailList.add(inspectionDetail)
                AppLogger.d("Added inspection detail: no_pokok 0, Code 5, Value ${jumBuahTglPath.toDouble()}")
            }

            if (jumBrdTglPath != 0) {
                val inspectionDetail = InspectionDetailModel(
                    id_inspeksi = inspectionId,
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    nik = "",
                    nama = "",
                    no_pokok = 0,
                    pokok_panen = null,
                    kode_inspeksi = 6,
                    temuan_inspeksi = jumBrdTglPath.toDouble(),
                    status_pemulihan = 0,
                    foto = foto,
                    komentar = komentar,
                    latIssue = latTPH,
                    lonIssue = lonTPH,
                    status_upload = "0",
                    status_uploaded_image = "0"
                )
                inspectionDetailList.add(inspectionDetail)
                AppLogger.d("Added inspection detail: no_pokok 0, Code 6, Value ${jumBrdTglPath.toDouble()}")
            }

            AppLogger.d("Total inspection details to save: ${inspectionDetailList.size}")

            // Only insert if we have data to insert
            if (inspectionDetailList.isNotEmpty()) {
                repository.insertInspectionDetails(inspectionDetailList)
                SaveDataInspectionDetailsState.Success(inspectionDetailList.size)
            } else {
                SaveDataInspectionDetailsState.Success(0)
            }

        } catch (e: Exception) {
            AppLogger.e("Error saving inspection details: ${e.message}")
            SaveDataInspectionDetailsState.Error(e.toString())
        }
    }


    // ===== STATE CLASS =====
    sealed class SaveDataInspectionDetailsState {
        data class Success(val insertedCount: Int) : SaveDataInspectionDetailsState()
        data class Error(val message: String) : SaveDataInspectionDetailsState()
    }

    class InspectionViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InspectionViewModel::class.java)) {
                return InspectionViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}