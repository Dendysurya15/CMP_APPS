package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.data.repository.PanenTBSRepository

class PanenTBSViewModel(application: Application, private val repository: PanenTBSRepository) : AndroidViewModel(application){


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