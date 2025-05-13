package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.repository.AbsensiRepository
import com.cbi.mobile_plantation.data.repository.PanenTBSRepository
import com.cbi.mobile_plantation.data.repository.WeighBridgeRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SaveDataAbsensiState{
    object Loading: SaveDataAbsensiState()
    object Success: SaveDataAbsensiState()
    data class Error(val message: String): SaveDataAbsensiState()
}

sealed class UpdateKaryawanAbsensiState{
    object Loading: UpdateKaryawanAbsensiState()
    data class Success(val id: Long): UpdateKaryawanAbsensiState()
    data class Error(val message: String): UpdateKaryawanAbsensiState()
}
sealed class UpdateKemandoranAbsensiState{
    object Loading: UpdateKemandoranAbsensiState()
    data class Success(val id: Long): UpdateKemandoranAbsensiState()
    data class Error(val message: String): UpdateKemandoranAbsensiState()
}
class AbsensiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AbsensiRepository = AbsensiRepository(application)

    private val _savedDataAbsensiList = MutableLiveData<List<AbsensiKemandoranRelations>>()
    val savedDataAbsensiList: LiveData<List<AbsensiKemandoranRelations>> = _savedDataAbsensiList

    private val _activeAbsensiList = MutableLiveData<List<AbsensiKemandoranRelations>>()
    val activeAbsensiList: LiveData<List<AbsensiKemandoranRelations>> = _activeAbsensiList

    private val _archivedAbsensiList = MutableLiveData<List<AbsensiKemandoranRelations>>()
    val archivedAbsensiList: LiveData<List<AbsensiKemandoranRelations>> = _archivedAbsensiList

    private val _saveDataAbsensiState = MutableStateFlow<SaveDataAbsensiState>(SaveDataAbsensiState.Loading)
    val saveDataAbsensiState: StateFlow<SaveDataAbsensiState> get() = _saveDataAbsensiState.asStateFlow()

    private val _updateKaryawanAbsensiState = MutableStateFlow<UpdateKaryawanAbsensiState>(UpdateKaryawanAbsensiState.Loading)
    val updateKaryawanAbsensiState: StateFlow<UpdateKaryawanAbsensiState> get() = _updateKaryawanAbsensiState.asStateFlow()

    private val _updateKemandoranbsensiState = MutableStateFlow<UpdateKemandoranAbsensiState>(UpdateKemandoranAbsensiState.Loading)
    val updateKemandoranbsensiState: StateFlow<UpdateKemandoranAbsensiState> get() = _updateKemandoranbsensiState.asStateFlow()

    fun isAbsensiExist(dateAbsen: String, karyawanMskIds: List<String>): Boolean {
        return repository.isAbsensiExist(dateAbsen, karyawanMskIds)
    }

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _archivedCount = MutableLiveData<Int>()
    val archivedCount: LiveData<Int> = _archivedCount

    private val _absensiCount = MutableStateFlow(0)
    val absensiCount: StateFlow<Int> = _absensiCount.asStateFlow()

    suspend fun loadAbsensiCount(): Int {
        val count = repository.getAbsensiCount()
        _absensiCount.value = count
        return count
    }

    fun archiveAbsensiById(id: Int) {
        viewModelScope.launch {
            repository.archiveAbsensiById(id)
            getAllDataAbsensi(0) // Refresh the data
        }
    }

    fun loadActiveAbsensi() {
        viewModelScope.launch {
            repository.getActiveAbsensi()
                .onSuccess { panenList ->
                    _activeAbsensiList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadArchivedAbsensi() {
        viewModelScope.launch {
            repository.getArchivedAbsensi()
                .onSuccess { absensiList ->
                    _archivedAbsensiList.postValue(absensiList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load archived data")
                }
        }
    }

    fun loadAbsensiCountArchive(load_status_scan: Int) = viewModelScope.launch {
        try {
            val count = repository.getAbsensiCountArhive(load_status_scan)
            _archivedCount.value = count
        } catch (e: Exception) {
            AppLogger.e("Error loading archive count: ${e.message}")
            _archivedCount.value = 0  // Set to 0 if there's an error
        }
    }

    fun getAllDataAbsensi(status_scan: Int) {
        viewModelScope.launch {
            repository.getAllDataAbsensi(status_scan)
                .onSuccess { listData ->
                    _savedDataAbsensiList.postValue(listData)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    suspend fun getKemandoranById(idKemandoran: List<String>): List<KemandoranModel> {
        return withContext(Dispatchers.IO) {  // Run on background thread
            repository.getKemandoranById(idKemandoran)
        }
    }



    suspend fun saveDataAbsensi(
        kemandoran_id: String,
        date_absen: String,
        created_by: Int,
        karyawan_msk_id: String,
        karyawan_tdk_msk_id: String,
        karyawan_msk_nik: String,
        karyawan_tdk_msk_nik: String,
        karyawan_msk_nama: String,
        karyawan_tdk_msk_nama: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        info: String,
        status_scan : Int = 0,
        archive: Int
    ): SaveDataAbsensiState {
        return try {
            val absensiData = AbsensiModel(
                kemandoran_id = kemandoran_id,
                date_absen = date_absen,
                created_by = created_by,
                karyawan_msk_id = karyawan_msk_id,
                karyawan_tdk_msk_id = karyawan_tdk_msk_id,
                karyawan_msk_nik = karyawan_msk_nik,
                karyawan_tdk_msk_nik = karyawan_tdk_msk_nik,
                karyawan_msk_nama = karyawan_msk_nama,
                karyawan_tdk_msk_nama = karyawan_tdk_msk_nama,
                foto = foto,
                komentar = komentar,
                asistensi = asistensi,
                lat = lat,
                lon = lon,
                info = info,
                status_scan = status_scan,
                archive = archive
            )
            repository.insertAbsensiData(absensiData)
            SaveDataAbsensiState.Success

        } catch (e: Exception) {
            SaveDataAbsensiState.Error(e.toString())
        }
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return repository.getPemuatByIdList(idPemuat)
    }

//    suspend fun saveDataLokalAbsensi(
//        kemandoran_id: String,
//        date_absen: String,
//        created_by: Int,
//        karyawan_msk_id: String,
//        asistensi: Int
//    ): SaveDataAbsensiState {
//        return try {
//            val absensiDataLokal = AbsensiModelScan(
//                kemandoran_id = kemandoran_id,
//                date_absen = date_absen,
//                created_by = created_by,
//                karyawan_msk_id = karyawan_msk_id,
//                asistensi = asistensi
//            )
//            repository.insertAbsensiDataLokal(absensiDataLokal)
//            SaveDataAbsensiState.Success
//
//        } catch (e: Exception) {
//            SaveDataAbsensiState.Error(e.toString())
//        }
//    }

    suspend fun updateKaryawanAbsensi(
        date_absen: String,
        status_absen: String,
        karyawan_msk_id: List<String>,
    ) {
        _updateKaryawanAbsensiState.value = UpdateKaryawanAbsensiState.Loading

        viewModelScope.launch {
            try {
                val rowsUpdated: Long = repository.updateKaryawanAbsensi(date_absen, status_absen, karyawan_msk_id).toLong()
                if (rowsUpdated > 0) {
                    _updateKaryawanAbsensiState.value = UpdateKaryawanAbsensiState.Success(rowsUpdated)
                } else {
                    _updateKaryawanAbsensiState.value = UpdateKaryawanAbsensiState.Error("No records updated")
                }
            } catch (e: Exception) {
                _updateKaryawanAbsensiState.value = UpdateKaryawanAbsensiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    suspend fun updateKemandoranAbsensi(
        date_absen: String,
        status_absen: String,
        kemandoran_id: String,
    ) {
        _updateKemandoranbsensiState.value = UpdateKemandoranAbsensiState.Loading

        viewModelScope.launch {
            try {
                val rowsUpdated: Long = repository.updateKemandoranAbsensi(date_absen, status_absen, kemandoran_id).toLong()
                if (rowsUpdated > 0) {
                    _updateKemandoranbsensiState.value = UpdateKemandoranAbsensiState.Success(rowsUpdated)
                } else {
                    _updateKemandoranbsensiState.value = UpdateKemandoranAbsensiState.Error("No records updated")
                }
            } catch (e: Exception) {
                _updateKemandoranbsensiState.value = UpdateKemandoranAbsensiState.Error(
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
