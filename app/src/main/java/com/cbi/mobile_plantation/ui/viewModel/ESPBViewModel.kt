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
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ESPBViewModel(private val repository: AppRepository) : ViewModel() {

    private val _espbList = MutableLiveData<List<ESPBEntity>>()
    val espbList: LiveData<List<ESPBEntity>> get() = _espbList

    private val _espbEntity = MutableLiveData<ESPBEntity>()
    val espbEntity: LiveData<ESPBEntity> get() = _espbEntity

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

    private val _nopolList = MutableLiveData<List<KendaraanModel>>()
    val nopolList: LiveData<List<KendaraanModel>> = _nopolList

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

    private fun loadNopol() {
        viewModelScope.launch {
            try {
                val nopol = repository.getNopolList()
                _nopolList.postValue(nopol)
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

    fun getESPBById(int: Int){
        viewModelScope.launch {
            _espbEntity.value = repository.getESPBById(int)
        }
    }

    fun deleteESPBById(int: Int): Int{
        var code = 0
        viewModelScope.launch {
            code = repository.deleteESPBById(int)
        }
        return code
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

    // Add to ESPBViewModel.kt
    suspend fun insertESPBAndGetId(espbEntity: ESPBEntity): Long {
        return repository.insertESPBAndGetId(espbEntity)
    }

    suspend fun updateESPBStatus(idsList: List<Int>, status: Int, noESPB: String): Int {
        return repository.updateESPBStatusForMultipleIds(idsList, status, noESPB)
    }

    suspend fun panenUpdateStatusAngkut(idsList: List<Int>, status: Int): Int {
        return repository.panenUpdateStatusAngkut(idsList, status)
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

    fun loadHistoryESPBNonScan(date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.loadHistoryESPB(date)
            _historyEPSB.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB history: ${e.message}")
            _historyEPSB.value = emptyList()  // Return empty list if there's an error
        }
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return repository.getBlokById(listBlokId)
    }

    suspend fun getTransporterNameById(transporterId: Int): String? {
        // Implement this method to retrieve the transporter name from your repository or database
        return repository.getTransporterNameById(transporterId)
    }

    suspend fun getMillNameById(millId: Int): String? {
        // Implement this method to retrieve the mill name from your repository or database
        return repository.getMillNameById(millId)
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

}
