package com.cbi.cmp_project.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.repository.EspbRepository
import kotlinx.coroutines.launch

class PanenViewModel(private val repository: EspbRepository) : ViewModel() {

    fun insert(user: ESPBEntity) = viewModelScope.launch {
        repository.insert(user)
    }

    fun update(user: ESPBEntity) = viewModelScope.launch {
        repository.update(user)
    }

    fun delete(user: ESPBEntity) = viewModelScope.launch {
        repository.delete(user)
    }

    fun deleteById(id: Int) = viewModelScope.launch {
        repository.deleteById(id)
    }

    fun getAllEntries(onResult: (List<ESPBEntity>) -> Unit) = viewModelScope.launch {
        val entries = repository.getAllEntries()
        onResult(entries)
    }

    fun getEntryById(id: Int, onResult: (ESPBEntity?) -> Unit) = viewModelScope.launch {
        val entry = repository.getEntryById(id)
        onResult(entry)
    }
}
