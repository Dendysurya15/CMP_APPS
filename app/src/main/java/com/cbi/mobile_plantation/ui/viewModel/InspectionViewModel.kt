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
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _inspectionList = MutableLiveData<List<InspectionModel>>()
    val inspectionList: LiveData<List<InspectionModel>> get() = _inspectionList

    private val _inspectionWithDetails = MutableLiveData<List<InspectionWithDetailRelations>>()
    val inspectionWithDetails: LiveData<List<InspectionWithDetailRelations>> = _inspectionWithDetails

    sealed class SaveDataInspectionState {
        data class Success(val inspectionId: Long) : SaveDataInspectionState()
        data class Error(val message: String) : SaveDataInspectionState()
    }

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun getTPHHasBeenInspect() {
        viewModelScope.launch {
            repository.getTPHHasBeenInspect()
                .onSuccess { inspecationList ->
                    _inspectionList.value = inspecationList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    suspend fun getAfdelingName(afdelingId: Int): String? {
        return repository.getAfdelingName(afdelingId)
    }

    suspend fun loadInspectionCount(datetime: String? = null): Int {
        val count = repository.getInspectionCount(datetime)
        return count
    }

    fun updateDataIsZippedHP(ids: List<Int>, status:Int) {

        AppLogger.d(ids.toString())
        AppLogger.d("masuk gak sih ")
        viewModelScope.launch {
            try {
                repository.updateDataInspeksiIsZippedHP(ids,status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun loadInspectionPaths(datetime: String? = null) {
        viewModelScope.launch {
            _inspectionWithDetails.value = repository.getInspectionData(datetime)
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
        id_panen:Int,
        date_panen: String,
        jalur_masuk: String,
        brd_tinggal: Int,
        buah_tinggal: Int,
        jenis_kondisi: Int,
        baris1: Int,
        baris2: Int?,
        jml_pkk_inspeksi: Int,
        tracking_path: String,
        foto: String? = null,
        komentar: String? = null,
        latTPH: Double,
        lonTPH: Double,
        app_version: String,
        status_upload: String,
        status_uploaded_image: String,
    ): SaveDataInspectionState {
        return try {
            val inspectionData = InspectionModel(
                created_date_start = created_date_start,
                created_date_end = created_date_end,
                created_by = created_by,
                tph_id = tph_id,
                id_panen = id_panen,
                date_panen = date_panen,
                jalur_masuk = jalur_masuk,
                brd_tinggal = brd_tinggal,
                buah_tinggal = buah_tinggal,
                jenis_kondisi = jenis_kondisi,
                baris1 = baris1,
                baris2 = baris2,
                jml_pkk_inspeksi = jml_pkk_inspeksi,
                tracking_path = tracking_path,
                foto = foto,
                komentar = komentar,
                latTPH = latTPH,
                lonTPH = lonTPH,
                app_version = app_version,
                status_upload = status_upload,
                status_uploaded_image =  status_uploaded_image
            )

            val inspectionId = repository.insertInspectionData(inspectionData)
            SaveDataInspectionState.Success(inspectionId)

        } catch (e: Exception) {
            SaveDataInspectionState.Error(e.toString())
        }
    }

    suspend fun saveDataInspectionDetails(
        inspectionId: String,
        formData: Map<Int, FormAncakViewModel.PageData>,
        totalPages: Int,
    ): SaveDataInspectionDetailsState {
        return try {
            val inspectionDetailList = mutableListOf<InspectionDetailModel>()

            for (page in 1..totalPages) {
                val pageData = formData[page]
                val emptyTreeValue = pageData?.emptyTree ?: 0

                // Skip
                if (emptyTreeValue == 2 || emptyTreeValue == 3 || emptyTreeValue == 0) {
                    continue
                }

                val inspectionDetail = InspectionDetailModel(
                    id_inspeksi = inspectionId,
                    no_pokok = page,
                    prioritas = pageData?.priority ?: 0,
                    pokok_panen = pageData?.harvestTree ?: 0,
                    susunan_pelepah = pageData?.neatPelepah ?: 0,
                    pelepah_sengkleh = pageData?.pelepahSengkleh ?: 0,
                    kondisi_pruning = pageData?.pruning ?: 0,
                    ripe = pageData?.ripe  ?: 0,
                    buahm1 = pageData?.buahM1  ?: 0,
                    buahm2 = pageData?.buahM2  ?: 0,
                    brd_tidak_dikutip = pageData?.brdKtp ?: 0,
                    foto = pageData?.photo,
                    komentar = pageData?.comment,
                    latIssue = pageData?.latIssue ?: 0.0,
                    lonIssue = pageData?.lonIssue ?: 0.0,
                    status_upload = "0",
                    status_uploaded_image = "0"
                )
                inspectionDetailList.add(inspectionDetail)
            }

            // Only insert if we have data to insert
            if (inspectionDetailList.isNotEmpty()) {
                repository.insertInspectionDetails(inspectionDetailList)
                SaveDataInspectionDetailsState.Success(inspectionDetailList.size)
            } else {
                SaveDataInspectionDetailsState.Success(0)
            }

        } catch (e: Exception) {
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