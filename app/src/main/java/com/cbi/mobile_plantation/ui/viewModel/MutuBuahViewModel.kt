package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.database.TPHDao
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String

class MutuBuahViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = AppRepository(application)

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

