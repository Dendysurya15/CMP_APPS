package com.cbi.cmp_project.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.repository.AppRepository
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

    private val _janjangByBlockString = MutableLiveData<String>()
    val janjangByBlockString: LiveData<String> = _janjangByBlockString

    /**
     * Process TPH data and format janjang sums as a string
     */
    fun processTPHDataAsString(tphData: String) {
        viewModelScope.launch {
            val result = repository.getJanjangSumByBlockString(tphData)
            _janjangByBlockString.postValue(result)
        }
    }
}
