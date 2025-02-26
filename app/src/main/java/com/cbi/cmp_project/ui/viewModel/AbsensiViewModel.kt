package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.repository.AbsensiRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SaveDataAbsensiState{
    object Loading: SaveDataAbsensiState()
    data class Success(val id: Long): SaveDataAbsensiState()
    data class Error(val message: String): SaveDataAbsensiState()
}

class AbsensiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AbsensiRepository = AbsensiRepository(application)

    private val _saveDataAbsensiState = MutableStateFlow<SaveDataAbsensiState>(SaveDataAbsensiState.Loading)
    val saveDataAbsensiState: StateFlow<SaveDataAbsensiState> get() = _saveDataAbsensiState.asStateFlow()

    suspend fun saveDataAbsensi(
        kemandoran_id: String,
        date_created: String,
        created_by: Int,
        karyawan_msk_id: String,
        karyawan_tdk_msk_id: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        info:String,
        archive: Int,
    ) {
        _saveDataAbsensiState.value = SaveDataAbsensiState.Loading

        viewModelScope.launch {
            try {
                val result = repository.saveDataAbsensi(
                    kemandoran_id,
                    date_created,
                    created_by,
                    karyawan_msk_id,
                    karyawan_tdk_msk_id,
                    foto,
                    komentar,
                    asistensi,
                    lat,
                    lon,
                    info,
                    archive
                )

                result.fold(
                    onSuccess = { id ->
                        _saveDataAbsensiState.value = SaveDataAbsensiState.Success(id)
                    },
                    onFailure = { exception ->
                        _saveDataAbsensiState.value = SaveDataAbsensiState.Error(
                            exception.message ?: "Unknown error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                _saveDataAbsensiState.value = SaveDataAbsensiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    class AbsensiViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AbsensiViewModel::class.java)) {
                return AbsensiViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
