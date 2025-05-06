package com.cbi.mobile_plantation.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.repository.AppRepository

class HektarPanenViewModel(private val repository: AppRepository) : ViewModel() {

    suspend fun countWhereLuasPanenIsZeroAndDateToday(): Int {
        val count = repository.countWhereLuasPanenIsZeroAndDateToday()
        return count
    }

    suspend fun getKaryawanByNik(nik: String): String {
        val nama = repository.getNamaByNik(nik)
        return nama!!
    }

    private val _historyHektarPanen = MutableLiveData<List<HektarPanenEntity>>()
    val historyHektarPanen : LiveData<List<HektarPanenEntity>> = _historyHektarPanen

    suspend fun getDistinctBlokByDate(date: String): List<Int> {
        return repository.getDistinctBlokByDate(date)
    }

    class HektarPanenViewModelFactory(
        private val application: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HektarPanenViewModel::class.java)) {
                return HektarPanenViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}