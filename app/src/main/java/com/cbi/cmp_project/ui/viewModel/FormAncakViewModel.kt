package com.cbi.cmp_project.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cbi.cmp_project.R
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils

class FormAncakViewModel : ViewModel() {
    data class PageData(
        val emptyTree: Int = 0,
        val priority: Int = 2,
        val harvestTree: Int = 0,
        val ratAttack: Int = 2,
        val ganoderma: Int = 2,
        val neatPelepah: Int = 1,
        val pelepahSengkleh: Int = 2,
        val pruning: Int = 1,
        val kentosan: Int = 2,
        val ripe: Int = 0,
        val buahM1: Int = 0,
        val buahM2: Int = 0,
        val buahM3: Int = 0,
        val brdKtp: Int = 0,
        val brdIn: Int = 0,
        val brdOut: Int = 0,
        val pasarPikul: Int = 0,
        val ketiak: Int = 0,
        val parit: Int = 0,
        val brdSegar: Int = 0,
        val brdBusuk: Int = 0,
        val photo: String = "",
        val comment: String = ""
    )

    data class ValidationResult(
        val isValid: Boolean,
        val fieldId: Int? = null,
        val errorMessage: String? = null
    )

    private val _currentPage = MutableLiveData<Int>(1)
    val currentPage: LiveData<Int> = _currentPage

    private val _totalPages = MutableLiveData<Int>(AppUtils.TOTAL_MAX_TREES_INSPECTION)
    val totalPages: LiveData<Int> = _totalPages

    private val _estName = MutableLiveData<String>("-")
    val estName: LiveData<String> = _estName

    private val _formData = MutableLiveData<MutableMap<Int, PageData>>(mutableMapOf())
    val formData: LiveData<MutableMap<Int, PageData>> = _formData

    private val _fieldValidationError = MutableLiveData<Map<Int, String>>(emptyMap())
    val fieldValidationError: LiveData<Map<Int, String>> = _fieldValidationError

    private fun ensurePageDataExists(pageNumber: Int) {
        val currentData = _formData.value ?: mutableMapOf()
        if (!currentData.containsKey(pageNumber)) {
            currentData[pageNumber] = PageData()
            _formData.value = currentData
        }
    }

    fun nextPage() {
        _currentPage.value = (_currentPage.value ?: 1) + 1
    }

    fun previousPage() {
        _currentPage.value = (_currentPage.value ?: 1) - 1
    }

    fun getPageData(pageNumber: Int): PageData? {
        return _formData.value?.get(pageNumber)
    }

    fun savePageData(pageNumber: Int, data: PageData) {
        val currentData = _formData.value ?: mutableMapOf()
        currentData[pageNumber] = data
        _formData.value = currentData
    }

    fun updateEstName(estate: String) {
        _estName.value = estate
    }

    fun validateCurrentPage(): ValidationResult {
        val pageNumber = _currentPage.value ?: 1
        ensurePageDataExists(pageNumber)

        val data = _formData.value?.get(pageNumber)

        if (data?.emptyTree == 0) {
            val errorMessage = "Titik kosong wajib diisi!"

            val errorMap = mapOf(R.id.lyExistsTreeInspect to errorMessage)
            _fieldValidationError.value = errorMap

            return ValidationResult(false, R.id.lyExistsTreeInspect, errorMessage)
        }

        _fieldValidationError.value = emptyMap()
        return ValidationResult(true)
    }
}