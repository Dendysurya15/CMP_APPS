package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate


sealed class SaveDataPanenState {
    data class Success(val id: Long) : SaveDataPanenState()
    data class Error(val message: String) : SaveDataPanenState()
}

class PanenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _allKaryawanList = MutableLiveData<List<KaryawanModel>>()
    val allKaryawanList: LiveData<List<KaryawanModel>> = _allKaryawanList

    private val _panenCountTPHESPB = MutableLiveData<Int>()
    val panenCountTPHESPB: LiveData<Int> get() = _panenCountTPHESPB

    private val _panenList = MutableLiveData<List<PanenEntity>>()
    val panenList: LiveData<List<PanenEntity>> get() = _panenList

    private val _archivedPanenList = MutableLiveData<List<PanenEntityWithRelations>>()
    val archivedPanenList: LiveData<List<PanenEntityWithRelations>>  = _archivedPanenList

    private val _activePanenList = MutableLiveData<List<PanenEntityWithRelations>>()
    val activePanenList: LiveData<List<PanenEntityWithRelations>> get() = _activePanenList

    private val _detailESPB = MutableLiveData<List<ESPBEntity>>()
    val detailESPb: LiveData<List<ESPBEntity>> get() = _detailESPB

    private val _deleteItemsResult = MutableLiveData<Boolean>()
    val deleteItemsResult: LiveData<Boolean> = _deleteItemsResult

    private val _panenCount = MutableStateFlow(0)
    val panenCount: StateFlow<Int> = _panenCount.asStateFlow()

    private val _archivedCount = MutableLiveData<Int>()
    val archivedCount: LiveData<Int> = _archivedCount

    private val _panenCountApproval = MutableStateFlow(0)
    val panenCountApproval: StateFlow<Int> = _panenCountApproval.asStateFlow()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    private val _panenCountActive = MutableLiveData<Int>()
    val panenCountActive: LiveData<Int> = _panenCountActive

    private val _panenCountHasBeenESPB = MutableLiveData<Int>()
    val panenCountHasBeenESPB: LiveData<Int> = _panenCountHasBeenESPB

    private val _panenCountArchived = MutableLiveData<Int>()
    val panenCountArchived: LiveData<Int> = _panenCountArchived


    // ViewModel.kt
    private val _jenisTPHList = MutableLiveData<List<JenisTPHModel>>()
    val jenisTPHList: LiveData<List<JenisTPHModel>> = _jenisTPHList


    fun loadAllPanen() {
        viewModelScope.launch {
            _panenList.value = repository.getAllPanen()
        }
    }

    fun loadPanenCountArchive() = viewModelScope.launch {
        try {
            val count = repository.getPanenCountArchive()
            _archivedCount.value = count
        } catch (e: Exception) {
            AppLogger.e("Error loading archive count: ${e.message}")
            _archivedCount.value = 0  // Set to 0 if there's an error
        }
    }

    fun loadTPHNonESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.loadESPB(archive, statusEspb, scanStatus, date)
            _activePanenList.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            _activePanenList.value = emptyList()  // Return empty list if there's an error
        }
    }

    fun countTPHNonESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val count = repository.countESPB(archive, statusEspb, scanStatus, date)
            _panenCountActive.value = count
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            _panenCountActive.value = 0
        }
    }

    fun countHasBeenESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val count = repository.countESPB(archive, statusEspb, scanStatus, date)
            _panenCountHasBeenESPB.value = count
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            _panenCountHasBeenESPB.value = 0
        }
    }


    fun loadTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.loadESPB(archive, statusEspb, scanStatus, date)
            _archivedPanenList.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading ESPB: ${e.message}")
            _archivedPanenList.value = emptyList()  // Return empty list if there's an error
        }
    }

    fun countTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val count = repository.countESPB(archive, statusEspb, scanStatus, date)
            _panenCountArchived.value = count
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB: ${e.message}")
            _panenCountArchived.value = 0
        }
    }

    suspend fun loadPanenCount(): Int {
        val count = repository.getPanenCount()
        _panenCount.value = count
        return count
    }

    suspend fun getCountScanMPanen(status_scan_mpanen: Int = 0): Int{
        val count = repository.getCountScanMPanen(status_scan_mpanen)
        _panenCount.value = count
        return count
    }

    fun loadCountTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String? = null) = viewModelScope.launch {
        try {
            val formattedDate = date?.take(10) // Ensures only YYYY-MM-DD is passed
            val count = repository.loadCountTPHESPB(archive, statusEspb, scanStatus, formattedDate)
            _panenCountTPHESPB.value = count
        } catch (e: Exception) {
            AppLogger.e("Error loading TPH ESPB count: ${e.message}")
            _panenCountTPHESPB.value = 0
        }
    }


    suspend fun loadPanenCountApproval(): Int {
        val count = repository.getPanenCountApproval()
        _panenCount.value = count
        return count
    }

    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return repository.getPemuatByIdList(idPemuat)
    }

    suspend fun getKemandoranById(idKemandoran: List<String>): List<KemandoranModel> {
        return withContext(Dispatchers.IO) {  // Run on background thread
            repository.getKemandoranById(idKemandoran)
        }
    }

    fun updateStatusUploadPanen(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadPanen(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating status_upload: ${e.message}")
            }
        }
    }

    fun loadActivePanen() {
        viewModelScope.launch {
            repository.getActivePanen()
                .onSuccess { panenList ->
                    _activePanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadActivePanenESPB() {
        viewModelScope.launch {
            repository.getActivePanenESPB()
                .onSuccess { panenList ->
                    _activePanenList.value = panenList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadActivePanenESPBAll() {
        viewModelScope.launch {
            repository.getActivePanenESPBAll()
                .onSuccess { panenList ->
                    _activePanenList.value = panenList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun getAllTPHHasBeenSelected() {
        viewModelScope.launch {
            repository.getAllTPHHasBeenSelected()
                .onSuccess { panenList ->
                    _activePanenList.value = panenList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun getAllJenisTPH() {
        viewModelScope.launch {
            repository.getAllJenisTPH()
                .onSuccess { jenisTPHModels ->
                    _jenisTPHList.value = jenisTPHModels
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load JenisTPH data")
                }
        }
    }


//    fun getAllScanMPanenByDate(status_mpanen: Int, date: String) {
//        viewModelScope.launch {
//            repository.getAllScanMPanenByDate(status_mpanen, date)
//                .onSuccess { panenList ->
//                    _activePanenList.value = panenList
//                }
//                .onFailure { exception ->
//                    _error.postValue(exception.message ?: "Failed to load data")
//                }
//        }
//    }

    fun getAllScanMPanenByDate(archiveMpanen: Int, date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.getAllScanMPanenByDate(archiveMpanen, date)
            _activePanenList.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading getAllScanMPanenByDate: ${e.message}")
            _activePanenList.value = emptyList()  // Return empty list if there's an error
        }
    }

    fun updateDataIsZippedPanen(ids: List<Int>, status:Int) {
        viewModelScope.launch {
            try {
                repository.updateDataIsZippedPanen(ids,status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun getAllKaryawan() {
        viewModelScope.launch {
            repository.getAllKaryawan()
                .onSuccess { karyawanList ->
                    _allKaryawanList.postValue(karyawanList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load karyawan data")
                }
        }
    }

    fun getAllPanenWhereESPB(no_espb: String) {
        viewModelScope.launch {
            repository.getAllPanenWhereESPB(no_espb)
                .onSuccess { panenList ->
                    _detailESPB.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    suspend fun getTPHAndBlokInfo(id: Int): TPHBlokInfo? {
        return repository.getTPHAndBlokInfo(id)
    }

    fun loadActivePanenRestan(status: Int = 0) {
        viewModelScope.launch {
            repository.getActivePanenRestan(status)
                .onSuccess { panenList ->
                    _activePanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    fun loadArchivedPanen() {
        viewModelScope.launch {
            repository.getArchivedPanen()
                .onSuccess { panenList ->
                    _archivedPanenList.postValue(panenList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load archived data")
                }
        }
    }



    fun deleteMultipleItems(items: List<Map<String, Any>>) {
        viewModelScope.launch {
            try {
                // Extract IDs from the items
                val ids = items.mapNotNull { item ->
                    (item["id"] as? Number)?.toInt()
                }

                if (ids.isEmpty()) {
                    _deleteItemsResult.postValue(false)
                    _error.postValue("No valid IDs found to delete")
                    return@launch
                }

                val result = repository.deletePanenByIds(ids)

                // Check if the number of deleted items matches the number of IDs
                val isSuccess = result == ids.size
                _deleteItemsResult.postValue(isSuccess)

                if (!isSuccess) {
                    _error.postValue("Failed to delete all items")
                }

            } catch (e: Exception) {
                _deleteItemsResult.postValue(false)
                _error.postValue("Error deleting items: ${e.message}")
            }
        }
    }

    fun archivePanenById(id: Int) {
        viewModelScope.launch {
            repository.archivePanenById(id)
            loadAllPanen() // Refresh the data
        }
    }

    fun archiveMpanenByID(id: Int) {
        viewModelScope.launch {
            repository.archiveMpanenByID(id)
            loadAllPanen() // Refresh the data
        }
    }

    suspend fun getBlokKodeByTphId(tphId: Int): String {
        return try {
            repository.getBlokKodeByTphId(tphId)
        } catch (e: Exception) {
            AppLogger.e("Error loading blok kode: ${e.message}")
            ""
        }.toString()
    }

    suspend fun getNamaByNik(nik: String): String {
        return try {
            repository.getNamaByNik(nik)
        } catch (e: Exception) {
            AppLogger.e("Error loading blok kode: ${e.message}")
            ""
        }.toString()
    }

    suspend fun getNomorTPHbyId(tphId: Int): String {
        return try {
            repository.getNomorTPHbyId(tphId)
        } catch (e: Exception) {
            AppLogger.e("Error loading nomor TPH: ${e.message}")
            ""
        }.toString()
    }

    suspend fun saveDataPanen(
        tph_id: String,
        date_created: String,
        created_by: Int,
        karyawan_id: String,
        kemandoran_id: String,
        karyawan_nik: String,
        karyawan_nama: String,
        jjg_json: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        jenis_panen: Int,
        ancakInput: Int,
        info:String,
        blokBanjir : Int,
        archive: Int,
    ): AppRepository.SaveResultPanen {
        return try {
            val panenData = PanenEntity(
                tph_id = tph_id,
                date_created = date_created,
                created_by = created_by,
                karyawan_id = karyawan_id,
                kemandoran_id = kemandoran_id,
                karyawan_nik = karyawan_nik,
                karyawan_nama = karyawan_nama,
                jjg_json = jjg_json,
                foto = foto,
                komentar = komentar,
                asistensi = asistensi,
                lat = lat,
                lon = lon,
                jenis_panen = jenis_panen,
                ancak = ancakInput,
                info = info,
                status_banjir = blokBanjir,
                archive = archive
            )
            repository.saveDataPanen(panenData)
            AppRepository.SaveResultPanen.Success
        } catch (e: Exception) {
            AppRepository.SaveResultPanen.Error(e)

        }
    }

    class PanenViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PanenViewModel::class.java)) {
                return PanenViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
