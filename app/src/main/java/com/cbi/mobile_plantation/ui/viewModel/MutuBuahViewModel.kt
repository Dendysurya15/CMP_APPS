package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.data.model.MutuBuahWithRelations
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
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

    private val _activeMBList = MutableLiveData<List<MutuBuahWithRelations>>()
    val activeMBList: LiveData<List<MutuBuahWithRelations>> get() = _activeMBList


    fun getAllTPHHasBeenSelected() {
        viewModelScope.launch {
            repository.getAllTPHHasBeenSelectedMB()
                .onSuccess { panenList ->
                    _activeMBList.value = panenList // ✅ Immediate emission like StateFlow
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load data")
                }
        }
    }

    suspend fun loadMutuBuahToday(): Int {
        val count = try {
            repository.getMBCountCreatedToday()
        } catch (e: Exception) {
            AppLogger.e("Error counting ESPB created today: ${e.message}")
            0
        }
        return count
    }

    suspend fun getMutuBuahById(id: Long): MutuBuahEntity? {
        return repository.getMutuBuahById(id)
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
        return try {
            // Validate tph_id first
            if (tph_id.isBlank() || tph_id == "null" || tph_id == "0") {
                AppLogger.e("Invalid tph_id: $tph_id")
                return AppRepository.SaveResultMutuBuah.Error(
                    IllegalArgumentException("TPH ID tidak valid: $tph_id")
                )
            }

            val tphIdInt = tph_id.toIntOrNull()
            if (tphIdInt == null) {
                AppLogger.e("Cannot convert tph_id to Int: $tph_id")
                return AppRepository.SaveResultMutuBuah.Error(
                    IllegalArgumentException("TPH ID harus berupa angka: $tph_id")
                )
            }

            // Get TPH data with null check
            val tphData = repository.getTPHById(tphIdInt)

            if (tphData == null) {
                AppLogger.e("TPH not found for id: $tphIdInt")
                return AppRepository.SaveResultMutuBuah.Error(
                    NoSuchElementException("TPH dengan ID $tphIdInt tidak ditemukan di database")
                )
            }

            var resolvedWilayah = tphData.wilayah
            var resolvedBlokPpro = tphData.blok_ppro


            AppLogger.d("tphData $tphData")
            AppLogger.d("resolvedWilayah $resolvedWilayah")
            AppLogger.d("resolvedBlokPpro $resolvedBlokPpro")
            if (resolvedWilayah == null || resolvedBlokPpro == null) {
                AppLogger.d("tphData.blok_ppro ${tphData.blok_ppro}")
                AppLogger.d("tphData.dept ${tphData.dept}")
                AppLogger.d("tphData.divisi ${tphData.divisi}")
                AppLogger.d("tphData.blok ${tphData.blok}")
                val blokResult = repository.fetchBlokbyParams(
                    blockId = tphData.blok!!,
                    blokPpro = tphData.blok_ppro,
                    dept = tphData.dept_abbr.toString(),
                    divisi = tphData.divisi_abbr.toString()
                )
                val blok = blokResult.getOrNull()


                AppLogger.d("blok $blok")
                if (blok != null) {
                    if (tphData.wilayah == null)     tphData.wilayah     = blok.wilayah.toString()
                    if (tphData.blok_ppro == null)   tphData.blok_ppro   = blok.id_ppro
                }
            }

            AppLogger.d("${tphData.wilayah}")
            AppLogger.d("${tphData.blok_ppro}")
            // Log to debug which field might be null
            AppLogger.d("TPH Data loaded: id=$tphIdInt")
            AppLogger.d("regional=${tphData.regional}, wilayah=${tphData.wilayah}")
            AppLogger.d("company=${tphData.company}, dept=${tphData.dept}")
            AppLogger.d("divisi=${tphData.divisi}, blok=${tphData.blok}")

            // Validate required fields
            val validationErrors = mutableListOf<String>()
            if (tphData.regional == null) validationErrors.add("regional")
            if (tphData.wilayah == null) validationErrors.add("wilayah")
            if (tphData.company == null) validationErrors.add("company")
            if (tphData.company_abbr == null) validationErrors.add("company_abbr")
            if (tphData.company_nama == null) validationErrors.add("company_nama")
            if (tphData.dept == null) validationErrors.add("dept")
            if (tphData.dept_ppro == null) validationErrors.add("dept_ppro")
            if (tphData.dept_abbr == null) validationErrors.add("dept_abbr")
            if (tphData.dept_nama == null) validationErrors.add("dept_nama")
            if (tphData.divisi == null) validationErrors.add("divisi")
            if (tphData.divisi_ppro == null) validationErrors.add("divisi_ppro")
            if (tphData.divisi_abbr == null) validationErrors.add("divisi_abbr")
            if (tphData.divisi_nama == null) validationErrors.add("divisi_nama")
            if (tphData.blok == null) validationErrors.add("blok")
            if (tphData.blok_ppro == null) validationErrors.add("blok_ppro")
            if (tphData.blok_kode == null) validationErrors.add("blok_kode")
            if (tphData.blok_nama == null) validationErrors.add("blok_nama")
            if (tphData.nomor == null) validationErrors.add("nomor")

            if (validationErrors.isNotEmpty()) {
                val errorMsg = "TPH data incomplete. Missing fields: ${validationErrors.joinToString(", ")}"
                AppLogger.e(errorMsg)
                return AppRepository.SaveResultMutuBuah.Error(
                    IllegalStateException("Data TPH tidak lengkap. Field yang kosong: ${validationErrors.joinToString(", ")}")
                )
            }

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

            AppLogger.d("Saving MutuBuah entity to database...")
            val insertedId = repository.saveMutuBuah(mutuBuahEntity)
            return AppRepository.SaveResultMutuBuah.Success(insertedId)

        } catch (e: Exception) {
            AppLogger.e("Error saving MutuBuah: ${e.message}", e.toString())
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

