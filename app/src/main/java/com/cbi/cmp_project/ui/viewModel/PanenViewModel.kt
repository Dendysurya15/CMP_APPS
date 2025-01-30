package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.DatasetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _archivedPanenList = MutableLiveData<List<PanenEntity>>()
    val archivedPanenList: LiveData<List<PanenEntity>> get() = _archivedPanenList

    private val _activePanenList = MutableLiveData<List<PanenEntity>>()
    val activePanenList: LiveData<List<PanenEntity>> get() = _activePanenList

    private val _panenCount = MutableStateFlow(0)
    val panenCount: StateFlow<Int> = _panenCount.asStateFlow()


    private val _saveDataPanenState = MutableStateFlow<SaveDataPanenState>(SaveDataPanenState.Loading)
    val saveDataPanenState = _saveDataPanenState.asStateFlow()

    fun loadAllPanen() {
        viewModelScope.launch {
            _panenList.value = repository.getAllPanen()
        }
    }

    suspend fun loadPanenCount(): Int {
        val count = repository.getPanenCount()
        _panenCount.value = count
        return count
    }

    fun loadActivePanen() {
        viewModelScope.launch {
            _activePanenList.value = repository.getActivePanen()
        }
    }

    fun loadArchivedPanen() {
        viewModelScope.launch {
            _archivedPanenList.value = repository.getArchivedPanen()
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
