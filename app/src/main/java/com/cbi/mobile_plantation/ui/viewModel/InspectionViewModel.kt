package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.cmp_project.data.model.InspectionModel
import com.cbi.cmp_project.data.model.InspectionPathModel
import com.cbi.mobile_plantation.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

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