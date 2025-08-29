package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String

class MutuBuahViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

    private val _activeMutuBuahList = MutableLiveData<List<MutuBuahEntity>>()
    val activeMutuBuahList: LiveData<List<MutuBuahEntity>> get() = _activeMutuBuahList

    private val _countMutuBuahUnuploaded = MutableLiveData<Int>()
    val countMutuBuahUnuploaded: LiveData<Int> = _countMutuBuahUnuploaded

    private val _countMutuBuahUploaded = MutableLiveData<Int>()
    val countMutuBuahUploaded: LiveData<Int> = _countMutuBuahUploaded

    private val _mutuBuahList = MutableLiveData<List<MutuBuahEntity>>()
    val mutuBuahList: LiveData<List<MutuBuahEntity>> = _mutuBuahList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    suspend fun loadMutuBuahToday(): Int {
        val count = try {
            repository.getMBCountCreatedToday()
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
        }
        return count
    }

    suspend fun saveDataMutuBuah(
        tph_id: String,
        date_created: String,
        created_by: Int,
        foto: String,
        foto_selfie: String,
        komentar: String,
        lat: Double,
        lon: Double,
        info: String,
        nomorPemanenInput: Int,
        jjgPanen: Int,
        jjgMasak: Int,
        jjgMentah: Int,
        jjgLewatMasak: Int,
        jjgKosong: Int,
        jjgAbnormal: Int,
        jjgSeranganTikus: Int,
        jjgPanjang: Int,
        jjgTidakVcut: Int,
        jjgBayar: Int,
        jjgKirim: Int,
        createdName: String
    ): AppRepository.SaveResultMutuBuah {
        val tphData = repository.getTPHById(tph_id.toInt())

        return try {
            val mutuBuahEntity = MutuBuahEntity(
                tanggal = date_created.split(" ")[0],
                regional = tphData.regional!!,
                wilayah = tphData.wilayah!!,
                company = tphData.company!!,
                companyAbbr = tphData.company_abbr!!,
                companyNama = tphData.company_nama!!,
                dept = tphData.dept!!,
                deptPpro = tphData.dept_ppro!!,
                deptAbbr = tphData.dept_abbr!!,
                deptNama = tphData.dept_nama!!,
                divisi = tphData.divisi!!,
                divisiPpro = tphData.divisi_ppro!!,
                divisiAbbr = tphData.divisi_abbr!!,
                divisiNama = tphData.divisi_nama!!,
                blok = tphData.blok!!,
                blokPpro = tphData.blok_ppro!!,
                blokKode = tphData.blok_kode!!,
                blokNama = tphData.blok_nama!!,
                tph = tph_id,
                tphNomor = tphData.nomor!!,
                nomorPemanen = nomorPemanenInput,
                jjgPanen = jjgPanen,
                jjgMasak = jjgMasak,
                jjgMentah = jjgMentah,
                jjgLewatMasak = jjgLewatMasak,
                jjgKosong = jjgKosong,
                jjgAbnormal = jjgAbnormal,
                jjgSeranganTikus = jjgSeranganTikus,
                jjgPanjang = jjgPanjang,
                jjgTidakVcut = jjgTidakVcut,
                jjgBayar = jjgBayar,
                jjgKirim = jjgKirim,
                createdBy = created_by,
                createdName = createdName,
                createdDate = date_created,
                foto = foto,
                foto_selfie = foto_selfie,
                komentar = komentar,
                appVersion = info,
                lat = lat,
                lon = lon
            )
            repository.saveMutuBuah(mutuBuahEntity)
            AppRepository.SaveResultMutuBuah.Success
        } catch (e: Exception) {
            AppRepository.SaveResultMutuBuah.Error(e)
        }
    }

    fun loadMutuBuahAll() {
        viewModelScope.launch {
            repository.getMutuBuahAll()
                .onSuccess { mutuBuahList ->
                    _mutuBuahList.value = mutuBuahList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load MutuBuah data")
                }
        }
    }

    fun updateDataIsZippedMutuBuah(ids: List<Int>, status:Int) {
        viewModelScope.launch {
            try {
                repository.updateDataIsZippedMutuBuah(ids,status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
    }

    fun updateStatusUploadMutuBuah(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadMutuBuah(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating status_upload: ${e.message}")
            }
        }
    }


    fun loadMBUnuploaded(statusUpload: Int, date: String? = null) = viewModelScope.launch {
        try {
            val list = repository.loadMutuBuah(statusUpload, date)
            _activeMutuBuahList.value = list
        } catch (e: Exception) {
            AppLogger.e("Error loading MutuBuah: ${e.message}")
            _activeMutuBuahList.value = emptyList()  // Return empty list if there's an error
        }
    }

    fun countMBUnuploaded(date: String? = null) = viewModelScope.launch {
        try {
            val int = repository.countMutuBuah(0, date)
            _countMutuBuahUnuploaded.value = int
        } catch (e: Exception) {
            AppLogger.e("Error loading MutuBuah count unuploaded: ${e.message}")
            _countMutuBuahUnuploaded.value = 0
        }
    }

    fun countMBUploaded(date: String? = null) = viewModelScope.launch {
        try {
            val int = repository.countMutuBuah(3, date)
            _countMutuBuahUploaded.value = int
        } catch (e: Exception) {
            AppLogger.e("Error loading MutuBuah count uploaded: ${e.message}")
            _countMutuBuahUploaded.value = 0
        }
    }

    class MutuBuahViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MutuBuahViewModel::class.java)) {
                return MutuBuahViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}

