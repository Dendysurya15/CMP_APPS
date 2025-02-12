package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import kotlinx.coroutines.launch

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