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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SaveDataESPBKraniTimbangState {
    object Loading : SaveDataESPBKraniTimbangState()
    data class Success(val id: Long) : SaveDataESPBKraniTimbangState()
    data class Error(val message: String) : SaveDataESPBKraniTimbangState()
}

class WeighBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeighBridgeRepository = WeighBridgeRepository(application)

    private val _uploadedESPB = MutableLiveData<List<ESPBEntity>>()
    val uploadedESPB: LiveData<List<ESPBEntity>> = _uploadedESPB

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _saveDataESPBKraniTimbang = MutableStateFlow<SaveDataESPBKraniTimbangState>(SaveDataESPBKraniTimbangState.Loading)
    val saveDataESPBKraniTimbang = _saveDataESPBKraniTimbang.asStateFlow()

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


    suspend fun saveDataLocalKraniTimbangESPB(
        blok_jjg: String,
        created_by_id: Int,
        created_at: String,
        nopol: String,
        driver: String,
        transporter_id: Int,
        pemuat_id: String,
        mill_id: Int,
        archive: Int,
        tph0: String,
        tph1: String,
        update_info: String,
        uploaded_by_id: Int,
        uploaded_at: String,
        status_upload_cmp: Int,
        status_upload_ppro: Int,
        creator_info: String,
        uploader_info: String,
        noESPB: String
    ): WeighBridgeRepository.SaveResultESPBKrani {
        return try {
            val exists = repository.isNoESPBExists(noESPB)

            // Only insert if noESPB doesn't exist
            if (!exists) {
                val espbData = ESPBEntity(
                    blok_jjg = blok_jjg,
                    created_by_id = created_by_id,
                    created_at = created_at,
                    nopol = nopol,
                    driver = driver,
                    transporter_id = transporter_id,
                    pemuat_id = pemuat_id,
                    mill_id = mill_id,
                    archive = archive,
                    tph0 = tph0,
                    tph1 = tph1,
                    update_info = update_info,
                    uploaded_by_id = uploaded_by_id,
                    uploaded_at = uploaded_at,
                    status_upload_cmp = status_upload_cmp,
                    status_upload_ppro = status_upload_ppro,
                    creator_info = creator_info,
                    uploader_info = uploader_info,
                    noESPB = noESPB
                )
                repository.insertESPBData(espbData)
                WeighBridgeRepository.SaveResultESPBKrani.Success
            } else {
                WeighBridgeRepository.SaveResultESPBKrani.AlreadyExists
            }
        } catch (e: Exception) {
            WeighBridgeRepository.SaveResultESPBKrani.Error(e)
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