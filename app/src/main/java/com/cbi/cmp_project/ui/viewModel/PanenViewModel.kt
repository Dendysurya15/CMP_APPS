package com.cbi.cmp_project.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.repository.AppRepository
import kotlinx.coroutines.launch

class PanenViewModel(private val repository: AppRepository) : ViewModel() {

    private val _panenList = MutableLiveData<List<PanenEntity>>()
    val panenList: LiveData<List<PanenEntity>> get() = _panenList

    private val _archivedPanenList = MutableLiveData<List<PanenEntity>>()
    val archivedPanenList: LiveData<List<PanenEntity>> get() = _archivedPanenList

    private val _activePanenList = MutableLiveData<List<PanenEntity>>()
    val activePanenList: LiveData<List<PanenEntity>> get() = _activePanenList

    fun loadAllPanen() {
        viewModelScope.launch {
            _panenList.value = repository.getAllPanen()
        }
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

    fun insertPanen(panen: List<PanenEntity>) {
        viewModelScope.launch {
            repository.insertPanen(panen)
            loadAllPanen() // Refresh the data
        }
    }

    fun archivePanenById(id: Int) {
        viewModelScope.launch {
            repository.archivePanenById(id)
            loadAllPanen() // Refresh the data
        }
    }
}
