package com.cbi.mobile_plantation.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.markertph.data.model.TPHNewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ESPBViewModel(private val repository: AppRepository) : ViewModel() {

    private val _espbList = MutableLiveData<List<ESPBEntity>>()
    val espbList: LiveData<List<ESPBEntity>> get() = _espbList

    private val _archivedESPBList = MutableLiveData<List<ESPBEntity>>()
    val archivedESPBList: LiveData<List<ESPBEntity>> get() = _archivedESPBList

    private val _activeESPBList = MutableLiveData<List<ESPBEntity>>()
    val activeESPBList: LiveData<List<ESPBEntity>> get() = _activeESPBList

    private val _millList = MutableLiveData<List<MillModel>>()
    val millList: LiveData<List<MillModel>> = _millList

    private val _historyEPSB = MutableLiveData<List<ESPBEntity>>()
    val historyESPBNonScan: LiveData<List<ESPBEntity>> = _historyEPSB

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _espbDraftCount = MutableStateFlow(0)

    init {
        loadMills()
    }

    private fun loadMills() {
        viewModelScope.launch {
            try {
                val mills = repository.getMillList()
                _millList.postValue(mills)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadAllESPB() {
        viewModelScope.launch {
            _espbList.value = repository.getAllESPB()
        }
    }

    fun loadActiveESPB() {
        viewModelScope.launch {
            _activeESPBList.value = repository.getActiveESPB()
        }
    }


    suspend fun getCountDraftESPB(): Int {
        val count = repository.getCountDraftESPB()
        _espbDraftCount.value = count
        return count
    }

    fun loadArchivedESPB() {
        viewModelScope.launch {
            _archivedESPBList.value = repository.getArchivedESPB()
        }
    }

    fun insertESPB(espb: List<ESPBEntity>) {
        viewModelScope.launch {
            repository.insertESPB(espb)
            loadAllESPB() // Refresh the data
        }
    }

    fun archiveESPBById(id: Int) {
        viewModelScope.launch {
            repository.archiveESPBById(id)
            loadAllESPB() // Refresh the data
        }
    }

    //use getDivisiAbbrByTphId
    suspend fun getDivisiAbbrByTphId(tphId: Int): String {
        return repository.getDivisiAbbrByTphId(tphId)!!
    }

    //use getDivisiAbbrByTphId
    suspend fun getCompanyAbbrByTphId(tphId: Int): String {
        return repository.getCompanyAbbrByTphId(tphId)!!
    }

    private val _janjangByBlock = MutableLiveData<Map<Int, Int>>()
    val janjangByBlock: LiveData<Map<Int, Int>> = _janjangByBlock

    /**
     * Process TPH data and calculate janjang sum by block
     */
    fun processTPHData(tphData: String) {
        viewModelScope.launch {
            val result = repository.getJanjangSumByBlock(tphData)
            _janjangByBlock.postValue(result)
        }
    }

    // LiveData or StateFlow to track the update result if needed
    private val _updateResult = MutableLiveData<Result<Int>>()
    val updateResult: LiveData<Result<Int>> = _updateResult

    fun updateESPBStatus(idList: List<Int>, newStatus: Int) {
        viewModelScope.launch {
            try {
                val updatedCount = repository.updatePanenESPBStatus(idList, newStatus)
                _updateResult.postValue(Result.success(updatedCount))
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    class ESPBViewModelFactory(
        private val application: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ESPBViewModel::class.java)) {
                return ESPBViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun loadHistoryESPBNonScan() {
        viewModelScope.launch {
            repository.loadHistoryESPB()
                .onSuccess { listData ->
                    _historyEPSB.postValue(listData)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return repository.getBlokById(listBlokId)
    }
}
