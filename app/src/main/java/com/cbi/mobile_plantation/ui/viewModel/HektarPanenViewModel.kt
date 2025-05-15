package com.cbi.mobile_plantation.ui.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HektarPanenViewModel(private val repository: AppRepository) : ViewModel() {

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    suspend fun countWhereLuasPanenIsZeroAndDateToday(): Int {
        val count = repository.countWhereLuasPanenIsZeroAndDateToday()
        return count
    }

    suspend fun countWhereLuasPanenIsZeroAndDateAndBlok(blok: Int, date: String?): Int {
        val count = repository.countWhereLuasPanenIsZeroAndDateAndBlok(blok, date)
        return count
    }

    suspend fun getSumLuasPanen(blok: Int, date: String):Float {
        return repository.getSumLuasPanen(blok, date)
    }

    suspend fun updateLuasPanen(id:Int, luasPanen: Float):Int {
        return repository.updateLuasPanen(id,luasPanen)
    }

    suspend fun getLuasBlokByBlok(blok: Int):Float {
        return repository.getLuasBlokByBlok(blok)
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

    suspend fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(date: String?, blok: Int?): List<HektarPanenEntity> {
        return repository.getNikLuasPanenLuasBlokDibayarByDateAndBlok(date!!, blok!!)
    }

    fun updateStatusUploadHektarPanen(ids: List<Int>, status: Int) {
        viewModelScope.launch {
            try {
                repository.updateStatusUploadHektarPanen(ids, status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
                AppLogger.e("Error updating status_upload: ${e.message}")
            }
        }
    }

    fun updateDataIsZippedHP(ids: List<Int>, status:Int) {
        viewModelScope.launch {
            try {
                repository.updateDataIsZippedHP(ids,status)
                _updateStatus.postValue(true)
            } catch (e: Exception) {
                _updateStatus.postValue(false)
            }
        }
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

    fun loadHektarPanenData(date: String? = null, blok: Int? = null) = viewModelScope.launch(Dispatchers.IO) {
        try {
            Log.d("HektarPanenVM", "Loading data with date: $date, blok: $blok")
            val list = repository.getNikLuasPanenLuasBlokDibayarByDateAndBlok(date, blok)
            Log.d("HektarPanenVM", "Loaded ${list.size} records")
            _historyHektarPanen.postValue(list)
        } catch (e: Exception) {
            Log.e("HektarPanenVM", "Error loading data: ${e.message}", e)
            _historyHektarPanen.postValue(emptyList())
        }
    }

    // Get item by ID
    suspend fun getItemById(id: Int): HektarPanenEntity? {
        return repository.getHektarPanenById(id)
    }

    // Get total luas_panen for a specific blok and date (excluding a specific item)
    suspend fun getTotalLuasPanenForBlokAndDate(blokId: Int, dateOnly: String, excludeId: Int): Float {
        return repository.getTotalLuasPanenForBlokAndDate(blokId, dateOnly, excludeId)
    }

    // Update luas_panen for an item
    suspend fun updateLuasPanenBaru(id: Int, newValue: Float): Int {
        return repository.updateLuasPanenBaru(id, newValue)
    }


}