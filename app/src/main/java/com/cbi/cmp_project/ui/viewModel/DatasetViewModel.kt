package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.repository.DatasetRepository
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.WilayahModel
import kotlinx.coroutines.launch

class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)

    fun updateOrInsertRegional(regionals: List<RegionalModel>) = viewModelScope.launch {
        repository.updateOrInsertRegional(regionals)
    }

    // New methods for other models
    fun updateOrInsertWilayah(wilayah: List<WilayahModel>) = viewModelScope.launch {
        repository.updateOrInsertWilayah(wilayah)
    }

    fun updateOrInsertDept(dept: List<DeptModel>) = viewModelScope.launch {
        repository.updateOrInsertDept(dept)
    }

    fun updateOrInsertDivisi(divisions: List<DivisiModel>) = viewModelScope.launch {
        repository.updateOrInsertDivisi(divisions)
    }

    fun updateOrInsertBlok(blok: List<BlokModel>) = viewModelScope.launch {
        repository.updateOrInsertBlok(blok)
    }


    fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) = viewModelScope.launch {
        repository.updateOrInsertKemandoran(kemandoran)
    }


    fun updateOrInsertKemandoranDetail(kemandoran_detail: List<KemandoranDetailModel>) = viewModelScope.launch {
        repository.updateOrInsertKemandoranDetail(kemandoran_detail)
    }


    fun updateOrInsertKaryawan(karyawan: List<KaryawanModel>) = viewModelScope.launch {
        repository.updateOrInsertKaryawan(karyawan)
    }


    class DatasetViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DatasetViewModel::class.java)) {
                return DatasetViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}