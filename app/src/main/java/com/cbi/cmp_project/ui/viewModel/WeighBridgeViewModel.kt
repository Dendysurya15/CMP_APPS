package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.repository.WeighBridgeRepository
import com.cbi.markertph.data.model.TPHNewModel
import kotlinx.coroutines.launch


class WeighBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeighBridgeRepository = WeighBridgeRepository(application)

    private val _uploadedESPB = MutableLiveData<List<ESPBEntity>>()
    val uploadedESPB: LiveData<List<ESPBEntity>> = _uploadedESPB

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    suspend fun getMillName(millId: Int): List<MillModel> {
        return repository.getMill(millId)
    }

    suspend fun getTransporterName(transporterId: Int): List<TransporterModel> {
        return repository.getTransporter(transporterId)
    }

    suspend fun getBlokById(listBlokId: List<Int>): List<TPHNewModel> {
        return repository.getBlokById(listBlokId)
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return repository.getPemuatByIdList(idPemuat)
    }

    suspend fun coundESPBUploaded(): Int {
        val count = repository.coundESPBUploaded()
        return count
    }

    fun loadHistoryUploadeSPB() {
        viewModelScope.launch {
            repository.loadHistoryUploadeSPB()
                .onSuccess { listData ->
                    _uploadedESPB.postValue(listData)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
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