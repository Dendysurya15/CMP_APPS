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

    suspend fun loadInspectionCount(datetime: String? = null): Int {
        val count = repository.getInspectionCount(datetime)
        return count
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
        created_date_end: String,
        created_by: String,
        tph_id: Int,
        id_panen: String,
        date_panen: String,
        jalur_masuk: String,
        jenis_kondisi: Int,
        baris: String,
        inspeksi_putaran: Int? = 1,
        jml_pkk_inspeksi: Int,
        jml_pkk_diperiksa: Int,
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
        app_version_pemulihan: String? = null
    ): SaveDataInspectionState {
        return try {
            if (isFollowUp && existingInspectionId != null) {
                // Update existing inspection for follow-up
                val success = repository.updateInspectionForFollowUp(
                    inspectionId = existingInspectionId,
                    tracking_path_pemulihan = tracking_path_pemulihan,
                    inspeksi_putaran = 2,
                    updated_date_start = updated_date_start ?: created_date_start,
                    updated_date_end = updated_date_end ?: created_date_end,
                    updated_by = updated_by ?: created_by,
                    app_version_pemulihan = app_version_pemulihan ?: ""
                )

                if (success) {
                    SaveDataInspectionState.Success(existingInspectionId.toLong())
                } else {
                    SaveDataInspectionState.Error("Failed to update inspection")
                }
            } else {
                // Create new inspection
                val inspectionData = InspectionModel(
                    created_date_start = created_date_start,
                    created_date_end = created_date_end,
                    created_by = created_by,
                    tph_id = tph_id,
                    id_panen = id_panen,
                    date_panen = date_panen,
                    jalur_masuk = jalur_masuk,
                    jenis_kondisi = jenis_kondisi,
                    baris = baris,
                    inspeksi_putaran = inspeksi_putaran,
                    jml_pkk_inspeksi = jml_pkk_inspeksi,
                    jml_pkk_diperiksa = jml_pkk_diperiksa,
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
        totalPages: Int,
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
                val temuanPokok: Int
            )

            // Regular pokok mappings (exclude 5 and 6)
            val regularPokokMappings = listOf(
                InspectionMapping(
                    1, { pageData, _, _ -> pageData.brdKtpGawangan },
                    parameterInspeksi.find { it.id == 1 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 1 }?.nama ?: AppUtils.kodeInspeksi.brondolanDigawangan,
                    parameterInspeksi.find { it.id == 1 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    2, { pageData, _, _ -> pageData.brdKtpPiringanPikulKetiak },
                    parameterInspeksi.find { it.id == 2 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 2 }?.nama ?: AppUtils.kodeInspeksi.brondolanTidakDikutip,
                    parameterInspeksi.find { it.id == 2 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    3, { pageData, _, _ -> pageData.buahMasakTdkDipotong },
                    parameterInspeksi.find { it.id == 3 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 3 }?.nama ?: AppUtils.kodeInspeksi.buahMasakTidakDipotong,
                    parameterInspeksi.find { it.id == 3 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    4, { pageData, _, _ -> pageData.btPiringanGawangan },
                    parameterInspeksi.find { it.id == 4 }?.status_ppro ?: 1,
                    parameterInspeksi.find { it.id == 4 }?.nama ?: AppUtils.kodeInspeksi.buahTertinggalPiringan,
                    parameterInspeksi.find { it.id == 4 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    7, { pageData, _, _ -> if (pageData.neatPelepah == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 7 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 7 }?.nama ?: AppUtils.kodeInspeksi.susunanPelepahTidakSesuai,
                    parameterInspeksi.find { it.id == 7 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    8, { pageData, _, _ -> if (pageData.pelepahSengkleh == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 8 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 8 }?.nama ?: AppUtils.kodeInspeksi.terdapatPelepahSengkleh,
                    parameterInspeksi.find { it.id == 8 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    9, { pageData, _, _ -> if (pageData.overPruning == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 9 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 9 }?.nama ?: AppUtils.kodeInspeksi.overPruning,
                    parameterInspeksi.find { it.id == 9 }?.temuan_pokok ?: 1
                ),
                InspectionMapping(
                    10, { pageData, _, _ -> if (pageData.underPruning == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 10 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 10 }?.nama ?: AppUtils.kodeInspeksi.underPruning,
                    parameterInspeksi.find { it.id == 10 }?.temuan_pokok ?: 1
                )
            )

            var updatedCount = 0

            // Handle regular pokok (no_pokok 1 to totalPages)
            for (page in 1..totalPages) {
                val pageData = formData[page]
                val emptyTreeValue = pageData?.emptyTree ?: 0

                if (emptyTreeValue != 1) {
                    AppLogger.d("Skipping page $page - emptyTree is not 1 (value: $emptyTreeValue)")
                    continue
                }

                AppLogger.d("Processing follow-up update for page $page")

                regularPokokMappings.forEach { mapping ->
                    val rawValue = mapping.getValue(pageData!!, jumBrdTglPath, jumBuahTglPath)

                    val undivided = parameterInspeksi.find { it.id == mapping.kodeInspeksi }?.undivided ?: "True"
                    val dividedValue = if (undivided == "False") {
                        rawValue.toDouble()
                    } else {
                        rawValue.toDouble()
                    }

                    // Skip if temuan_inspeksi value is 0
                    if (dividedValue == 0.0) {
                        AppLogger.d("Skipping update for page $page, code ${mapping.kodeInspeksi} - temuan_inspeksi is 0")
                        return@forEach
                    }

                    // Find the existing inspection detail record by no_pokok and kode_inspeksi
                    val existingDetail = detailInspeksiList.find {
                        it.no_pokok == page && it.kode_inspeksi == mapping.kodeInspeksi
                    }

                    existingDetail?.let { detail ->
                        // Update the inspection detail record using its ID
                        val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                            inspectionDetailId = detail.id,
                            temuanInspeksi = dividedValue,
                            fotoPemulihan = pageData.photo,
                            komentarPemulihan = pageData.comment,
                            latPemulihan = pageData.latIssue ?: 0.0,
                            lonPemulihan = pageData.lonIssue ?: 0.0,
                            updatedDate = pageData.createdDate ?: "",
                            updatedName = pageData.createdName ?: "",
                            updatedBy = pageData.createdBy.toString()
                        )

                        if (updateSuccess) {
                            updatedCount++
                            AppLogger.d("Updated inspection detail ID: ${detail.id}, Page $page, Code ${mapping.kodeInspeksi}, Value $dividedValue")
                        }
                    }
                }
            }

            // Handle special case for no_pokok = 0 (kode_inspeksi 5 and 6)
            if (jumBuahTglPath != 0) {
                val existingDetail = detailInspeksiList.find {
                    it.no_pokok == 0 && it.kode_inspeksi == 5
                }

                existingDetail?.let { detail ->
                    val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                        inspectionDetailId = detail.id,
                        temuanInspeksi = jumBuahTglPath.toDouble(),
                        fotoPemulihan = null,
                        komentarPemulihan = null,
                        latPemulihan = 0.0,
                        lonPemulihan = 0.0,
                        updatedDate = "",
                        updatedName = "",
                        updatedBy = ""
                    )

                    if (updateSuccess) {
                        updatedCount++
                        AppLogger.d("Updated inspection detail ID: ${detail.id}, no_pokok 0, Code 5, Value ${jumBuahTglPath.toDouble()}")
                    }
                }
            }

            if (jumBrdTglPath != 0) {
                val existingDetail = detailInspeksiList.find {
                    it.no_pokok == 0 && it.kode_inspeksi == 6
                }

                existingDetail?.let { detail ->
                    val updateSuccess = repository.updateInspectionDetailForFollowUpById(
                        inspectionDetailId = detail.id,
                        temuanInspeksi = jumBrdTglPath.toDouble(),
                        fotoPemulihan = null,
                        komentarPemulihan = null,
                        latPemulihan = 0.0,
                        lonPemulihan = 0.0,
                        updatedDate = createdDateStart,
                        updatedName = createdName,
                        updatedBy = createdBy
                    )

                    if (updateSuccess) {
                        updatedCount++
                        AppLogger.d("Updated inspection detail ID: ${detail.id}, no_pokok 0, Code 6, Value ${jumBrdTglPath.toDouble()}")
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

            // Regular pokok mappings (exclude 5 and 6)
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
                ),

                InspectionMapping(
                    9, { pageData, _, _ -> if (pageData.overPruning == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 9 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 9 }?.nama ?: AppUtils.kodeInspeksi.overPruning,
                    parameterInspeksi.find { it.id == 9 }?.temuan_pokok ?: 1
                ),

                InspectionMapping(
                    10, { pageData, _, _ -> if (pageData.underPruning == 1) 1 else 0 },
                    parameterInspeksi.find { it.id == 10 }?.status_ppro ?: 0,
                    parameterInspeksi.find { it.id == 10 }?.nama
                        ?: AppUtils.kodeInspeksi.underPruning,
                    parameterInspeksi.find { it.id == 10 }?.temuan_pokok ?: 1
                )
            )

            AppLogger.d("Created ${regularPokokMappings.size} regular pokok inspection mappings from database parameters")

            // Handle regular pokok (no_pokok 1 to totalPages)
            for (page in 1..totalPages) {
                val pageData = formData[page]
                val emptyTreeValue = pageData?.emptyTree ?: 0

                if (emptyTreeValue != 1) {
                    AppLogger.d("Skipping page $page - emptyTree is not 1 (value: $emptyTreeValue)")
                    continue
                }

                AppLogger.d("Processing page $page with ${selectedKaryawanList.size} karyawan")

                selectedKaryawanList.forEach { karyawan ->
                    regularPokokMappings.forEach { mapping ->
                        val rawValue = mapping.getValue(pageData!!, jumBrdTglPath, jumBuahTglPath)

                        AppLogger.d("DEBUG: Page $page, Code ${mapping.kodeInspeksi} (${mapping.nama}): rawValue = $rawValue")

                        val undivided =
                            parameterInspeksi.find { it.id == mapping.kodeInspeksi }?.undivided
                                ?: "True"
                        val dividedValue = if (undivided == "False") {
                            rawValue.toDouble()
                        } else {
                            rawValue.toDouble() / karyawanCount.toDouble()
                        }

                        // Skip if temuan_inspeksi value is 0
                        if (dividedValue == 0.0) {
                            AppLogger.d("Skipping save for page $page, karyawan ${karyawan.nama}, code ${mapping.kodeInspeksi} - temuan_inspeksi is 0")
                            return@forEach
                        }

                        AppLogger.d("Page $page, Karyawan ${karyawan.nama}, Code ${mapping.kodeInspeksi} (${mapping.nama}): $rawValue / $karyawanCount = $dividedValue")

                        val inspectionDetail = InspectionDetailModel(
                            id_inspeksi = inspectionId,
                            created_date = pageData.createdDate ?: "",
                            created_name = pageData.createdName ?: "",
                            created_by = pageData.createdBy.toString(),
                            nik = karyawan.nik,
                            nama = karyawan.nama,
                            no_pokok = page,
                            pokok_panen = pageData.harvestTree,
                            kode_inspeksi = mapping.kodeInspeksi,
                            temuan_inspeksi = dividedValue,
                            status_pemulihan = 0.0,
                            foto = pageData.photo,
                            foto_pemulihan = null,
                            komentar = pageData.comment,
                            latIssue = pageData.latIssue ?: 0.0,
                            lonIssue = pageData.lonIssue ?: 0.0,
                            status_upload = "0",
                            status_uploaded_image = "0"
                        )

                        inspectionDetailList.add(inspectionDetail)
                        AppLogger.d("Added inspection detail: Page $page, Karyawan ${karyawan.nama}, Code ${mapping.kodeInspeksi}, Value $dividedValue")
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
                    status_pemulihan = 0.0,
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
                    status_pemulihan = 0.0,
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