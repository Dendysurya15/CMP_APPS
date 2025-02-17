package com.cbi.cmp_project.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.DatasetRepository
import kotlinx.coroutines.launch

class ESPBViewModel(private val repository: AppRepository) : ViewModel() {

    private val _espbList = MutableLiveData<List<ESPBEntity>>()
    val espbList: LiveData<List<ESPBEntity>> get() = _espbList

    private val _archivedESPBList = MutableLiveData<List<ESPBEntity>>()
    val archivedESPBList: LiveData<List<ESPBEntity>> get() = _archivedESPBList

    private val _activeESPBList = MutableLiveData<List<ESPBEntity>>()
    val activeESPBList: LiveData<List<ESPBEntity>> get() = _activeESPBList

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
}
