package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.UploadCMPModel
import com.cbi.cmp_project.data.model.uploadCMP.UploadCMPResponse
import com.cbi.cmp_project.data.repository.SaveResultNewUploadDataCMP
import com.cbi.cmp_project.data.repository.UploadCMPRepository
import com.cbi.cmp_project.data.repository.WeighBridgeRepository
import com.cbi.cmp_project.utils.AppUtils
import kotlinx.coroutines.launch

class UploadCMPViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UploadCMPRepository = UploadCMPRepository(application)

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus



    private val _uploadProgressCMP = MutableLiveData<Int>()
    val uploadProgressCMP: LiveData<Int> get() = _uploadProgressCMP

    private val _uploadStatusCMP = MutableLiveData<String>()
    val uploadStatusCMP: LiveData<String> get() = _uploadStatusCMP

    private val _uploadErrorCMP = MutableLiveData<String?>()
    val uploadErrorCMP: LiveData<String?> get() = _uploadErrorCMP

    private val _uploadResult = MutableLiveData<Result<UploadCMPResponse>>()
    val uploadResult: LiveData<Result<UploadCMPResponse>> get() = _uploadResult

    private val _uploadResponseCMP = MutableLiveData<UploadCMPResponse?>()
    val uploadResponseCMP: LiveData<UploadCMPResponse?> get() = _uploadResponseCMP


    fun UpdateOrInsertDataUpload(
        tracking_id: Int,
        nama_file: String,
        status: Int,
        tanggal_upload: String,
        table_ids: String
    ) {
        viewModelScope.launch {
            try {
                repository.UpdateOrInsertDataUpload(
                    UploadCMPModel(
                        tracking_id = tracking_id,
                        nama_file = nama_file,
                        status = status,
                        tanggal_upload = tanggal_upload,
                        table_ids = table_ids
                    )
                )
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun uploadZipToServer(fileZip: String) {
        viewModelScope.launch {
            // Reset progress values (assuming single Int/Status LiveData)
            _uploadProgressCMP.value = 0
            _uploadStatusCMP.value = AppUtils.UploadStatusUtils.WAITING
            _uploadErrorCMP.value = null
            _uploadResponseCMP.value = null // Reset previous response

            val result = repository.uploadZipToServer(fileZip) { progress, isSuccess, error ->
                // Update LiveData
                _uploadProgressCMP.postValue(progress)
                _uploadStatusCMP.postValue(
                    when {
                        !isSuccess && !error.isNullOrEmpty() -> AppUtils.UploadStatusUtils.FAILED
                        isSuccess -> AppUtils.UploadStatusUtils.SUCCESS
                        progress in 1..99 -> AppUtils.UploadStatusUtils.UPLOADING
                        else -> AppUtils.UploadStatusUtils.WAITING
                    }
                )
                _uploadErrorCMP.postValue(error)
            }

            result?.let {
                if (it.isSuccess) {
                    _uploadResponseCMP.postValue(it.getOrNull()) // Store response
                }
            }
        }
    }





    class UploadCMPViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UploadCMPViewModel::class.java)) {
                return UploadCMPViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}