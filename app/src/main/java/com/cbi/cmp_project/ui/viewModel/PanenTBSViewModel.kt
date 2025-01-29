package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.PanenTBSModel
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import kotlinx.coroutines.launch

class PanenTBSViewModel(application: Application, private val repository: PanenTBSRepository) : AndroidViewModel(application){

    private val _insertDBPanenTBS = MutableLiveData<Boolean>()
    val insertDBPanenTBS: LiveData<Boolean> get() = _insertDBPanenTBS

//    fun insertPanenTBSVM(
//        id: Int? = 0,
//        user_id: Int,
//        tanggal: String,
//        name: String,
//        estate: String,
//        id_estate: Int,
//        afdeling: String,
//        id_afdeling: Int,
//        blok: String,
//        id_blok: Int,
//        tahun_tanam: Int,
//        id_tt: Int,
//        ancak: String,
//        id_ancak: Int,
//        tph: String,
//        id_tph: Int,
//        jenis_panen: String,
//        list_pemanen: String,
//        list_idpemanen: String,
//        tbs: Int,
//        tbs_mentah: Int,
//        tbs_lewatmasak: Int,
//        tks: Int,
//        abnormal: Int,
//        tikus: Int,
//        tangkai_panjang: Int,
//        vcut: Int,
//        tbs_masak: Int,
//        tbs_dibayar: Int,
//        tbs_kirim: Int,
//        latitude: String,
//        longitude: String,
//        foto: String
//    ) {
//        viewModelScope.launch {
//            try {
//
//                val dataSubmitPanen = PanenTBSModel(
//                    id!!,
//                    user_id,
//                    tanggal,
//                    name,
//                    estate,
//                    id_estate,
//                    afdeling,
//                    id_afdeling,
//                    blok,
//                    id_blok,
//                    tahun_tanam,
//                    id_tt,
//                    ancak,
//                    id_ancak,
//                    tph,
//                    id_tph,
//                    jenis_panen,
//                    list_pemanen,
//                    list_idpemanen,
//                    tbs,
//                    tbs_mentah,
//                    tbs_lewatmasak,
//                    tks,
//                    abnormal,
//                    tikus,
//                    tangkai_panjang,
//                    vcut,
//                    tbs_masak,
//                    tbs_dibayar,
//                    tbs_kirim,
//                    latitude,
//                    longitude,
//                    foto
//                )
//
//                // Insert data into the repository
//                val isInserted = repository.insertPanenTBSRepo(dataSubmitPanen)
//
//                // Update LiveData
//                _insertDBPanenTBS.postValue(isInserted)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                _insertDBPanenTBS.postValue(false)
//            }
//        }
//    }


    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application, private val repository: PanenTBSRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PanenTBSViewModel::class.java)) {
                return PanenTBSViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}