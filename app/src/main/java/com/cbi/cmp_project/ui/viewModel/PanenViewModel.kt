package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.DatasetRepository
import com.cbi.cmp_project.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed class SaveDataPanenState {
    object Loading : SaveDataPanenState()
    data class Success(val id: Long) : SaveDataPanenState()
    data class Error(val message: String) : SaveDataPanenState()
}

class PanenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _panenList = MutableLiveData<List<PanenEntity>>()
    val panenList: LiveData<List<PanenEntity>> get() = _panenList

    private val _archivedPanenList = MutableLiveData<List<PanenEntityWithRelations>>()
    val archivedPanenList: LiveData<List<PanenEntityWithRelations>> = _archivedPanenList

    private val _activePanenList = MutableLiveData<List<PanenEntityWithRelations>>()
    val activePanenList: LiveData<List<PanenEntityWithRelations>> = _activePanenList

    private val _deleteItemsResult = MutableLiveData<Boolean>()
    val deleteItemsResult: LiveData<Boolean> = _deleteItemsResult

    private val _panenCount = MutableStateFlow(0)
    val panenCount: StateFlow<Int> = _panenCount.asStateFlow()

    private val _archivedCount = MutableLiveData<Int>()
    val archivedCount: LiveData<Int> = _archivedCount

    private val _panenCountApproval = MutableStateFlow(0)
    val panenCountApproval: StateFlow<Int> = _panenCountApproval.asStateFlow()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus


    private val _saveDataPanenState = MutableStateFlow<SaveDataPanenState>(SaveDataPanenState.Loading)
    val saveDataPanenState = _saveDataPanenState.asStateFlow()

    fun loadAllPanen() {
        viewModelScope.launch {
            _panenList.value = repository.getAllPanen()
        }
    }

    fun loadPanenCountArchive() = viewModelScope.launch {
        try {
            val count = repository.getPanenCountArchive()
            _archivedCount.value = count
        } catch (e: Exception) {
            AppLogger.e("Error loading archive count: ${e.message}")
            _archivedCount.value = 0  // Set to 0 if there's an error
        }
    }

    suspend fun loadPanenCount(): Int {
        val count = repository.getPanenCount()
        _panenCount.value = count
        return count
    }

    suspend fun loadPanenCountApproval(): Int {
        val count = repository.getPanenCountApproval()
        _panenCount.value = count
        return count
    }

    fun loadActivePanen() {
        viewModelScope.launch {
            repository.getActivePanen()
                .onSuccess { panenList ->
                    _activePanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadActivePanenESPB() {
        viewModelScope.launch {
            repository.getActivePanenESPB()
                .onSuccess { panenList ->
                    _activePanenList.value = panenList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun updateArchivePanen(ids: List<Int>, statusArchive:Int) {
        viewModelScope.launch {
            try {
                repository.updatePanenArchive(ids,statusArchive)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun loadActivePanenRestan() {
        viewModelScope.launch {
            repository.getActivePanenRestan()
                .onSuccess { panenList ->
                    _activePanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadArchivedPanen() {
        viewModelScope.launch {
            repository.getArchivedPanen()
                .onSuccess { panenList ->
                    _archivedPanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load archived data")
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

                val result = repository.deletePanenByIds(ids)

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


    fun archivePanenById(id: Int) {
        viewModelScope.launch {
            repository.archivePanenById(id)
            loadAllPanen() // Refresh the data
        }
    }

    suspend fun saveDataPanen(
        tph_id: String,
        date_created: String,
        created_by: Int,
        karyawan_id: String,
        jjg_json: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        jenis_panen: Int,
        ancakInput: String,
        info:String,
        archive: Int,
    ) {
        _saveDataPanenState.value = SaveDataPanenState.Loading

        viewModelScope.launch {
            try {
                val result = repository.saveDataPanen(
                    tph_id,
                    date_created,
                    created_by,
                    karyawan_id,
                    jjg_json,
                    foto,
                    komentar,
                    asistensi,
                    lat,
                    lon,
                    jenis_panen,
                    ancakInput,
                    info,
                    archive
                )

                result.fold(
                    onSuccess = { id ->
                        _saveDataPanenState.value = SaveDataPanenState.Success(id)
                    },
                    onFailure = { exception ->
                        _saveDataPanenState.value = SaveDataPanenState.Error(
                            exception.message ?: "Unknown error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                _saveDataPanenState.value = SaveDataPanenState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    class PanenViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PanenViewModel::class.java)) {
                return PanenViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
