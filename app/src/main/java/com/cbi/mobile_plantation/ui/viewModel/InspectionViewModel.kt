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
        jjg_panen : Int,
        foto_user_pemulihan:String?= null,
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
                    foto_user_pemulihan = foto_user_pemulihan ?: "",
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
                    jjg_panen = jjg_panen,
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

    // Add this helper function first
    private fun processFormData(formData: Map<Int, FormAncakViewModel.PageData>): Pair<Map<Int, FormAncakViewModel.PageData>, Map<Int, FormAncakViewModel.PageData>> {
        val processedFormData = formData.mapValues { (_, pageData) ->
            if (pageData.emptyTree != 1) {
                // Reset values to 0 or null if emptyTree != 1
                pageData.copy(
                    harvestTree = 0,
                    neatPelepah = 0,
                    pelepahSengkleh = 0,
                    kondisiPruning = 0,
                    buahMasakTdkDipotong = 0,
                    btPiringanGawangan = 0,
                    brdKtpGawangan = 0,
                    brdKtpPiringanPikulKetiak = 0,
                    photo = null,
                    comment = null,
                    latIssue = null,
                    lonIssue = null,
                    foto_pemulihan = null,
                    komentar_pemulihan = null,
                    status_pemulihan = null
                )
            } else {
                // Keep original data if emptyTree == 1
                pageData
            }
        }

        // Return both processed data and filtered valid data
        val validFormData = processedFormData.filter { (_, pageData) -> pageData.emptyTree == 1 }

        return Pair(processedFormData, validFormData)
    }

    suspend fun updateDataInspectionDetailsForFollowUp(
        detailInspeksiList: List<InspectionDetailModel>,
        formData: Map<Int, FormAncakViewModel.PageData>,
        latTPH: Double,
        lonTPH: Double,
        photoTPHFollowUp : String,
        komentarTPHFollowUp : String,
        createdDateStart: String,
        createdName: String,
        createdBy: String
    ): SaveDataInspectionDetailsState {
        return try {

            val (processedFormData, validFormData) = processFormData(formData)

            var updatedCount = 0

            detailInspeksiList.forEach { detail ->
                AppLogger.d("Processing detail ID: ${detail.id}, no_pokok: ${detail.no_pokok}, kode_inspeksi: ${detail.kode_inspeksi}, nama: ${detail.nama}, nik: ${detail.nik}")

                // Handle special case for no_pokok = 0 (codes 5 and 6)
                if (detail.no_pokok == 0) {
                    when (detail.kode_inspeksi) {
                        5 -> {
                            val matchingPageData = validFormData.values.find { it.pokokNumber == 0 }

                            val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                                inspectionDetailId = detail.id,
                                temuanInspeksi = detail.temuan_inspeksi, // Keep original value
                                fotoPemulihan = if (photoTPHFollowUp.isNotEmpty()) photoTPHFollowUp else "",
                                komentarPemulihan = if (photoTPHFollowUp.isNotEmpty()) komentarTPHFollowUp else "",
                                latPemulihan = latTPH ?: 0.0,
                                lonPemulihan = lonTPH ?: 0.0,
                                updatedDate = createdDateStart,
                                statusPemulihan = if (photoTPHFollowUp.isNotEmpty()) 1 else 0,
                                updatedName = createdName,
                                updatedBy = createdBy
                            )

                            if (updateSuccess) {
                                updatedCount++
                                AppLogger.d("Updated detail ID: ${detail.id}, no_pokok 0, Code 5")
                            }
                        }
                        6 -> {
                            val matchingPageData = validFormData.values.find { it.pokokNumber == 0 }

                            val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                                inspectionDetailId = detail.id,
                                temuanInspeksi = detail.temuan_inspeksi, // Keep original value
                                fotoPemulihan = if (photoTPHFollowUp.isNotEmpty()) photoTPHFollowUp else "",
                                komentarPemulihan = if (photoTPHFollowUp.isNotEmpty()) komentarTPHFollowUp else "",
                                latPemulihan = latTPH ?: 0.0,
                                lonPemulihan = lonTPH ?: 0.0,
                                updatedDate = createdDateStart,
                                statusPemulihan = if (photoTPHFollowUp.isNotEmpty()) 1 else 0,
                                updatedName = createdName,
                                updatedBy = createdBy
                            )

                            if (updateSuccess) {
                                updatedCount++
                                AppLogger.d("Updated detail ID: ${detail.id}, no_pokok 0, Code 6")
                            }
                        }
                        else -> {
                            AppLogger.d("Skipping detail ID: ${detail.id} - no_pokok 0 but kode_inspeksi ${detail.kode_inspeksi} is not 5 or 6")
                        }
                    }
                } else {
                    val matchingPageData = validFormData.values.find {
                        it.pokokNumber == detail.no_pokok
                    }

                    if (matchingPageData == null) {
                        AppLogger.d("No valid pageData found for no_pokok: ${detail.no_pokok}")
                        return@forEach
                    }

                    // Update the inspection detail record (keep original temuan_inspeksi)
                    val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                        inspectionDetailId = detail.id,
                        temuanInspeksi = detail.temuan_inspeksi, // Keep original value
                        fotoPemulihan = matchingPageData.foto_pemulihan,
                        komentarPemulihan = matchingPageData.komentar_pemulihan,
                        latPemulihan = matchingPageData.latIssue ?: 0.0,
                        lonPemulihan = matchingPageData.lonIssue ?: 0.0,
                        statusPemulihan = if (!matchingPageData.foto_pemulihan.isNullOrEmpty()) 1 else 0,
                        updatedDate = matchingPageData.createdDate ?: createdDateStart,
                        updatedName = matchingPageData.createdName ?: createdName,
                        updatedBy = matchingPageData.createdBy?.toString() ?: createdBy
                    )

                    if (updateSuccess) {
                        updatedCount++
                        AppLogger.d("Updated detail ID: ${detail.id}, no_pokok: ${detail.no_pokok}, kode_inspeksi: ${detail.kode_inspeksi}")
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
        foto_pemulihan_tph: String,
        komentar_pemulihan_tph: String,
    ): SaveDataInspectionDetailsState {
        return try {
            // PROCESS THE DATA FIRST - Reset values where emptyTree != 1, then filter valid data
            val (processedFormData, validFormData) = processFormData(formData)

            AppLogger.d("Processed ${processedFormData.size} total pages, ${validFormData.size} valid pages for inspection")

            // Log the processing results
            processedFormData.forEach { (pageNumber, pageData) ->
                if (pageData.emptyTree != 1) {
                    AppLogger.d("Page $pageNumber: emptyTree=${pageData.emptyTree} - Values reset to 0/null")
                } else {
                    AppLogger.d("Page $pageNumber: emptyTree=${pageData.emptyTree} - Using original values")
                }
            }

            val inspectionDetailList = mutableListOf<InspectionDetailModel>()

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

            validFormData.forEach { (pageNumber, pageData) ->
                // Skip pages with empty pemanen
                if (pageData.pemanen.isEmpty()) {
                    AppLogger.d("Skipping page $pageNumber - no pemanen data")
                    return@forEach
                }

                val karyawanCount = pageData.pemanen.size
                AppLogger.d("Processing page $pageNumber with $karyawanCount pemanen")

                // Loop through each pemanen in this page
                pageData.pemanen.forEach { (nik, nama) ->
                    AppLogger.d("Processing pemanen: $nik - $nama")

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
                            AppLogger.d("Skipping save for page $pageNumber, pemanen $nama, code ${mapping.kodeInspeksi} - temuan_inspeksi is 0")
                            return@forEach
                        }

                        AppLogger.d("Page $pageNumber, Pemanen $nama ($nik), Code ${mapping.kodeInspeksi} (${mapping.nama}): $rawValue / $karyawanCount = $dividedValue")

                        // Check if status_pemulihan is 1 and foto_pemulihan is not null
                        val isPemulihanComplete = pageData.status_pemulihan == 1 && pageData.foto_pemulihan != null

                        val inspectionDetail = InspectionDetailModel(
                            id_inspeksi = inspectionId,
                            created_date = pageData.createdDate ?: "",
                            created_name = pageData.createdName ?: "",
                            created_by = pageData.createdBy.toString(),
                            // Set updated fields only if pemulihan is complete
                            updated_date = if (isPemulihanComplete) pageData.createdDate ?: "" else null,
                            updated_name = if (isPemulihanComplete) pageData.createdName ?: "" else null,
                            updated_by = if (isPemulihanComplete) pageData.createdBy.toString() else null,
                            nik = nik, // Use NIK from pemanen map
                            nama = nama, // Use nama from pemanen map
                            no_pokok = pageNumber,
                            pokok_panen = pageData.harvestTree,
                            kode_inspeksi = mapping.kodeInspeksi,
                            temuan_inspeksi = dividedValue,
                            status_pemulihan = pageData.status_pemulihan ?: 0,
                            foto = pageData.photo,
                            foto_pemulihan = pageData.foto_pemulihan,
                            komentar = pageData.comment,
                            komentar_pemulihan = if (isPemulihanComplete) pageData.komentar_pemulihan else null,
                            latIssue = pageData.latIssue ?: 0.0,
                            lonIssue = pageData.lonIssue ?: 0.0,
                            // Set lat/lonPemulihan only if pemulihan is complete
                            latPemulihan = if (isPemulihanComplete) pageData.latIssue ?: 0.0 else null,
                            lonPemulihan = if (isPemulihanComplete) pageData.lonIssue ?: 0.0 else null,
                            status_upload = "0",
                            status_uploaded_image = "0"
                        )

                        inspectionDetailList.add(inspectionDetail)
                        AppLogger.d("Added inspection detail: Page $pageNumber, Pemanen $nama ($nik), Code ${mapping.kodeInspeksi}, Value $dividedValue")

                        if (isPemulihanComplete) {
                            AppLogger.d("Pemulihan data saved for page $pageNumber, pemanen $nama: lat=${pageData.latIssue}, lon=${pageData.lonIssue}")
                        }
                    }
                }
            }

            // Handle special case for no_pokok = 0 (kode_inspeksi 5 and 6 only)
            if (jumBuahTglPath != 0) {
                val isPemulihanTphComplete = !foto_pemulihan_tph.isNullOrEmpty()
                val inspectionDetail = InspectionDetailModel(
                    id_inspeksi = inspectionId,
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    updated_date = if (isPemulihanTphComplete) createdDate else null,
                    updated_name = if (isPemulihanTphComplete) createdName else null,
                    updated_by = if (isPemulihanTphComplete) createdBy else null,
                    nik = "",
                    nama = "",
                    no_pokok = 0,
                    pokok_panen = null,
                    kode_inspeksi = 5,
                    temuan_inspeksi = jumBuahTglPath.toDouble(),
                    status_pemulihan = if (isPemulihanTphComplete) 1 else 0,
                    foto = foto,
                    komentar = komentar,
                    foto_pemulihan = if (isPemulihanTphComplete) foto_pemulihan_tph else null,
                    komentar_pemulihan = if (isPemulihanTphComplete) komentar_pemulihan_tph else null,
                    latIssue = latTPH,
                    lonIssue = lonTPH,
                    latPemulihan = if (isPemulihanTphComplete) latTPH else null,
                    lonPemulihan = if (isPemulihanTphComplete) lonTPH else null,
                    status_upload = "0",
                    status_uploaded_image = "0"
                )
                inspectionDetailList.add(inspectionDetail)
                AppLogger.d("Added inspection detail: no_pokok 0, Code 5, Value ${jumBuahTglPath.toDouble()}")
            }

            if (jumBrdTglPath != 0) {
                val isPemulihanTphComplete = !foto_pemulihan_tph.isNullOrEmpty()
                val inspectionDetail = InspectionDetailModel(
                    id_inspeksi = inspectionId,
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    updated_date = if (isPemulihanTphComplete) createdDate else null,
                    updated_name = if (isPemulihanTphComplete) createdName else null,
                    updated_by = if (isPemulihanTphComplete) createdBy else null,
                    nik = "",
                    nama = "",
                    no_pokok = 0,
                    pokok_panen = null,
                    kode_inspeksi = 6,
                    temuan_inspeksi = jumBrdTglPath.toDouble(),
                    status_pemulihan = if (isPemulihanTphComplete) 1 else 0,
                    foto = foto,
                    komentar = komentar,
                    latIssue = latTPH,
                    lonIssue = lonTPH,
                    latPemulihan = if (isPemulihanTphComplete) latTPH else null,
                    lonPemulihan = if (isPemulihanTphComplete) lonTPH else null,
                    foto_pemulihan = if (isPemulihanTphComplete) foto_pemulihan_tph else null,
                    komentar_pemulihan = if (isPemulihanTphComplete) komentar_pemulihan_tph else null,
                    status_upload = "0",
                    status_uploaded_image = "0"
                )
                inspectionDetailList.add(inspectionDetail)
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