package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.repository.WeighBridgeRepository
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.launch

sealed class SaveDataESPBKraniTimbangState {
    object Loading : SaveDataESPBKraniTimbangState()
    data class Success(val id: Long) : SaveDataESPBKraniTimbangState()
    data class Error(val message: String) : SaveDataESPBKraniTimbangState()
}

class WeighBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeighBridgeRepository = WeighBridgeRepository(application)

    private val _savedESPBByKrani = MutableLiveData<List<ESPBEntity>>()
    val savedESPBByKrani: LiveData<List<ESPBEntity>> = _savedESPBByKrani

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _deleteItemsResult = MutableLiveData<Boolean>()
    val deleteItemsResult: LiveData<Boolean> = _deleteItemsResult

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    data class UploadItemInfo(
        val status: String,
        val endpoint: String
    )

    private val _uploadStatusEndpointMap = MutableLiveData<Map<Int, UploadItemInfo>>()
    val uploadStatusEndpointMap: LiveData<Map<Int, UploadItemInfo>> = _uploadStatusEndpointMap


    private val _uploadProgress = MutableLiveData<Map<Int, Int>>() // Tracks each item's progress
    val uploadProgress: LiveData<Map<Int, Int>> get() = _uploadProgress

    private val _uploadResult = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = _uploadResult

    // Add a status map
    private val _uploadStatusMap = MutableLiveData<Map<Int, String>>()
    val uploadStatusMap: LiveData<Map<Int, String>> get() = _uploadStatusMap

    private val _uploadErrorMap = MutableLiveData<Map<Int, String>>()
    val uploadErrorMap: LiveData<Map<Int, String>> get() = _uploadErrorMap

    private val _activeESPBByIds = MutableLiveData<List<ESPBEntity>>()
    val activeESPBByIds: LiveData<List<ESPBEntity>> = _activeESPBByIds

    fun updateStatusUploadEspbCmpSp(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadEspbCmpSp(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating ESPB status_upload: ${e.message}")
            }
        }
    }

    fun uploadESPBKraniTimbang(selectedItems: List<Map<String, Any>>, globalIdEspb: List<Int>) {
        viewModelScope.launch {
            val progressMap = mutableMapOf<Int, Int>()
            val statusMap = mutableMapOf<Int, String>()
            val statusEndpointMap = mutableMapOf<Int, UploadItemInfo>()
            val errorMap = mutableMapOf<Int, String>()

            selectedItems.forEach { item ->
                val itemId = item["num"] as Int
                val endpoint = item["endpoint"] as String
                progressMap[itemId] = 0
                statusMap[itemId] = "Waiting"
                statusEndpointMap[itemId] = UploadItemInfo("Waiting", endpoint)
            }

            _uploadProgress.value = progressMap
            _uploadStatusMap.value = statusMap
            _uploadStatusEndpointMap.value = statusEndpointMap
            _uploadErrorMap.value = errorMap

            val result = repository.uploadESPBKraniTimbang(
                selectedItems,
                globalIdEspb
            ) { itemId, progress, isSuccess, errorMsg ->
                progressMap[itemId] = progress

                val newStatus = when {
                    !isSuccess && !errorMsg.isNullOrEmpty() -> "Failed"
                    isSuccess -> "Success"
                    progress > 0 && progress < 100 -> "Uploading"
                    else -> "Waiting"
                }

                statusMap[itemId] = newStatus

                // Update the endpoint info map with new status but keep the endpoint
                statusEndpointMap[itemId]?.let { info ->
                    statusEndpointMap[itemId] = info.copy(status = newStatus)
                }

                // Store error message if any
                if (!errorMsg.isNullOrEmpty()) {
                    errorMap[itemId] = errorMsg
                }

                _uploadProgress.postValue(progressMap)
                _uploadStatusMap.postValue(statusMap)
                _uploadStatusEndpointMap.postValue(statusEndpointMap)
                _uploadErrorMap.postValue(errorMap)
            }

            _uploadResult.value = result ?: Result.failure(Exception("Unknown error occurred"))
        }
    }


    suspend fun getMillName(millId: Int): List<MillModel> {
        return repository.getMill(millId)
    }

    suspend fun getTransporterName(transporterId: Int): List<TransporterModel> {
        return repository.getTransporter(transporterId)
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return repository.getBlokById(listBlokId)
    }

    suspend fun getDataByIdInBlok(listBlokId: List<Int>): List<BlokModel> {
        return repository.getDataByIdInBlok(listBlokId)
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return repository.getPemuatByIdList(idPemuat)
    }


    private val _tphData = MutableLiveData<TPHNewModel?>()
    val tphData: LiveData<TPHNewModel?> = _tphData


    // Function to fetch TPH data by block ID
    fun fetchTPHByBlockId(blockId: Int) {
        viewModelScope.launch {
            repository.getTPHByBlockId(blockId)
                .onSuccess { tph ->
                    _tphData.postValue(tph)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load TPH data")
                }
        }
    }


    private val _activeESPBUploadCMP = MutableLiveData<List<ESPBEntity>>()
    val activeESPBUploadCMP: LiveData<List<ESPBEntity>> get() = _activeESPBUploadCMP

    fun fetchActiveESPB() {
        viewModelScope.launch {
            repository.getActiveESPB()
                .onSuccess { espbList ->
                    _activeESPBUploadCMP.postValue(espbList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load ESPB data")
                }
        }
    }

    fun fetchActiveESPBAll() {
        viewModelScope.launch {
            repository.getActiveESPBAll()
                .onSuccess { espbList ->
                    _activeESPBUploadCMP.postValue(espbList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load ESPB data")
                }
        }
    }


    fun fetchActiveESPBByIds(ids: List<Int>) {
        viewModelScope.launch {
            repository.getActiveESPBByIds(ids)
                .onSuccess { espbList ->
                    _activeESPBByIds.postValue(espbList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load ESPB data")
                }
        }
    }


    fun loadHistoryUploadeSPB() {
        viewModelScope.launch {
            repository.loadHistoryUploadeSPB()
                .onSuccess { listData ->
                    _savedESPBByKrani.postValue(listData)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun deleteMultipleItems(items: List<Map<String, Any>>) {
        viewModelScope.launch {
            try {
                // Extract IDs from the items
                val ids = items.mapNotNull { item ->
                    (item["id"] as? Number)?.toInt()
                }

                if (ids.isEmpty()) {
                    _deleteItemsResult.postValue(false)
                    _error.postValue("No valid IDs found to delete")
                    return@launch
                }

                val result = repository.deleteESPBByIds(ids)

                // Check if the number of deleted items matches the number of IDs
                val isSuccess = result == ids.size
                _deleteItemsResult.postValue(isSuccess)

                if (!isSuccess) {
                    _error.postValue("Failed to delete all items")
                }

            } catch (e: Exception) {
                _deleteItemsResult.postValue(false)
                _error.postValue("Error deleting items: ${e.message}")
            }
        }
    }

    fun loadHistoryESPB(date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.loadHistoryESPB(date)
            _savedESPBByKrani.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB history: ${e.message}")
            _savedESPBByKrani.value = emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun getCountCreatedToday():Int{
        val count = try {
            repository.getCountCreatedToday()
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
        }
        return count
    }

    suspend fun saveDataLocalKraniTimbangESPB(
        blok_jjg: String,
        created_by_id: Int,
        created_at: String,
        nopol: String,
        driver: String,
        transporter_id: Int,
        pemuat_id: String,
        kemandoran_id: String,
        pemuat_nik: String,
        mill_id: Int,
        archive: Int,
        tph0: String,
        tph1: String,
        update_info_sp: String? = null,
        uploaded_by_id_wb: Int,
        uploaded_at_wb: String,
        status_upload_cmp_wb: Int,
        status_upload_ppro_wb: Int,
        creator_info: String,
        uploader_info_wb: String,
        noESPB: String,
        scan_status: Int = 1,
    ): WeighBridgeRepository.SaveResultESPBKrani {
        return try {
            val exists = repository.isNoESPBExists(noESPB)
            if (!exists) {
                val espbData = ESPBEntity(
                    blok_jjg = blok_jjg,
                    created_by_id = created_by_id,
                    created_at = created_at,
                    nopol = nopol,
                    driver = driver,
                    transporter_id = transporter_id,
                    pemuat_id = pemuat_id,
                    kemandoran_id = kemandoran_id,
                    pemuat_nik = pemuat_nik,
                    mill_id = mill_id,
                    archive = archive,
                    tph0 = tph0,
                    tph1 = tph1,
                    update_info_sp = update_info_sp ?: "NULL",
                    uploaded_by_id_wb = uploaded_by_id_wb,
                    uploaded_at_wb = uploaded_at_wb,
                    uploaded_by_id_sp = 0,
                    uploaded_at_sp = "NULL",
                    status_upload_cmp_sp = 0,
                    status_upload_cmp_wb = status_upload_cmp_wb,
                    status_upload_ppro_wb = status_upload_ppro_wb,
                    status_draft = 0,
                    status_mekanisasi = 0,
                    creator_info = creator_info,
                    uploader_info_sp = "NULL",
                    uploader_info_wb = uploader_info_wb,
                    noESPB = noESPB,
                    scan_status = scan_status,
                    dataIsZipped = 0
                )
                // Insert and get the ID
                val insertedId = repository.insertESPBDataAndGetId(espbData)
                WeighBridgeRepository.SaveResultESPBKrani.Success(insertedId)
            } else {
                WeighBridgeRepository.SaveResultESPBKrani.AlreadyExists
            }
        } catch (e: Exception) {
            WeighBridgeRepository.SaveResultESPBKrani.Error(e)
        }
    }


    fun updateDataIsZippedESPB(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateDataIsZippedESPB(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    class WeightBridgeViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeighBridgeViewModel::class.java)) {
                return WeighBridgeViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}