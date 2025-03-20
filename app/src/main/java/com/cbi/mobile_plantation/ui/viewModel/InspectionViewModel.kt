package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.PathWithInspectionRelations
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _savedInspectionList = MutableLiveData<List<PathWithInspectionRelations>>()
    val savedInspectionList: LiveData<List<PathWithInspectionRelations>> = _savedInspectionList

    private val _uploadedCount = MutableLiveData<Int>()
    val uploadedCount: LiveData<Int> = _uploadedCount

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadSavedInspection() {
        viewModelScope.launch {
            repository.getSavedInspection()
                .onSuccess { listData ->
                    _savedInspectionList.postValue(listData)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadInspectionCountUploaded() = viewModelScope.launch {
        try {
            val count = repository.getInspectionCountUploaded()
            _uploadedCount.value = count
        } catch (e: Exception) {
            AppLogger.e("Error loading archive count: ${e.message}")
            _uploadedCount.value = 0
        }
    }

    suspend fun insertPathDataSync(pathData: InspectionPathModel): String? {
        return withContext(Dispatchers.IO) {
            try {
                val result = repository.addPathDataInspection(pathData)
                result.getOrNull()?.toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun insertInspectionDataSync(inspectionList: List<InspectionModel>): List<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val result = repository.addDataInspection(inspectionList)
                result.getOrElse { emptyList() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun deleteInspectionAndPath(id: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = repository.deleteInspectionAndPathById(id)
                if (result.isSuccess) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to delete data"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    class InspectionViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InspectionViewModel::class.java)) {
                return InspectionViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}