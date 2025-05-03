package com.cbi.mobile_plantation.ui.viewModel

import androidx.lifecycle.ViewModel
import com.cbi.mobile_plantation.data.repository.AppRepository

class HektarPanenViewModel(private val repository: AppRepository) : ViewModel() {

    suspend fun countWhereLuasPanenIsZeroAndDateToday(): Int {
        val count = repository.countWhereLuasPanenIsZeroAndDateToday()
        return count
    }

}