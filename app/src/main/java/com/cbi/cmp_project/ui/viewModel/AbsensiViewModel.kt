package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.cmp_project.data.repository.AbsensiRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository

class AbsensiViewModel(application: Application, private val repository: AbsensiRepository) : AndroidViewModel(application) {

    private val _insertDBAbsensi = MutableLiveData<Boolean>()
    val insertDBAbsnesi: LiveData<Boolean> get() = _insertDBAbsensi

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application, private val repository: AbsensiRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AbsensiViewModel::class.java)) {
                return AbsensiViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
