package com.cbi.mobile_plantation.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils

class FormAncakViewModel : ViewModel() {
    data class PageData(
        val emptyTree: Int = 0,
        val jjgAkp: Int = 0,
        val priority: Int = 0,
        val harvestTree: Int = 0,
        val ratAttack: Int = 0,
        val ganoderma: Int = 0,
        val neatPelepah: Int = 0,
        val pelepahSengkleh: Int = 0,
        val pruning: Int = 0,
        val kentosan: Int = 0,
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
        val photo: String? = null,
        val comment: String? = null,
        val latIssue: Double? = null,
        val lonIssue: Double? = null
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

    private val _afdName = MutableLiveData<String>("-")
    val afdName: LiveData<String> = _afdName

    private val _blokName = MutableLiveData<String>("-")
    val blokName: LiveData<String> = _blokName

    private val _isInspection = MutableLiveData<Boolean>(true)
    val isInspection: LiveData<Boolean> = _isInspection

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

    fun updateInfoFormAncak(estate: String, afdeling: String, blok: String) {
        _estName.value = estate
        _afdName.value = afdeling
        _blokName.value = blok
    }

    fun updateTypeInspection(newValue: Boolean) {
        _isInspection.value = newValue
    }

    fun validateCurrentPage(inspectionType: Int? = null): ValidationResult {
        val pageNumber = _currentPage.value ?: 1
        ensurePageDataExists(pageNumber)

        val data = _formData.value?.get(pageNumber)
        val errors = mutableMapOf<Int, String>()

        AppLogger.d("=== VIEWMODEL VALIDATION ===")
        AppLogger.d("inspectionType: $inspectionType")
        AppLogger.d("pageNumber: $pageNumber")
        AppLogger.d("data: $data")
        AppLogger.d("emptyTree: ${data?.emptyTree}")

        // STEP 1: Check if emptyTree is selected
        if (data?.emptyTree == 0) {
            val nameMessage = if (inspectionType == 1) "Temuan" else "Pokok dipanen"
            errors[R.id.lyExistsTreeInspect] = "$nameMessage wajib diisi!"
            AppLogger.d("VALIDATION FAILED: emptyTree == 0")

            _fieldValidationError.value = errors
            return ValidationResult(false, R.id.lyExistsTreeInspect, "$nameMessage wajib diisi!")
        }

        // STEP 2: Only validate other fields if emptyTree == 1 (Ya/Ada Pohon)
        if (data?.emptyTree == 1) {
            AppLogger.d("emptyTree == 1, validating other fields...")

            if (data?.priority == 0) {
                errors[R.id.lyPrioritasInspect] = "Prioritas wajib diisi!"
                AppLogger.d("VALIDATION FAILED: priority == 0")
            }

            if (data?.harvestTree == 0) {
                errors[R.id.lyHarvestTreeInspect] = "Pokok dipanen wajib diisi!"
                AppLogger.d("VALIDATION FAILED: harvestTree == 0")
            }

            if (data?.neatPelepah == 0) {
                errors[R.id.lyNeatPelepahInspect] = "Susunan pelepah wajib diisi!"
                AppLogger.d("VALIDATION FAILED: neatPelepah == 0")
            }

            if (data?.pelepahSengkleh == 0) {
                errors[R.id.lyPelepahSengklehInspect] = "Pelepah sengkleh wajib diisi!"
                AppLogger.d("VALIDATION FAILED: pelepahSengkleh == 0")
            }

            if (data?.pruning == 0) {
                errors[R.id.lyPruningInspect] = "Kondisi pruning wajib diisi!"
                AppLogger.d("VALIDATION FAILED: pruning == 0")
            }

            if (inspectionType == 2 && data.jjgAkp <= 0) {
                errors[R.id.lyJjgPanenAKPInspect] = "Janjang panen harus lebih dari 0!"
                AppLogger.d("VALIDATION FAILED: jjgAkp <= 0")
            }
        } else {
            AppLogger.d("emptyTree == ${data?.emptyTree} (Tidak/Titik Kosong), skipping field validation")
        }

        AppLogger.d("Total errors found: ${errors.size}")
        AppLogger.d("Errors: $errors")

        return if (errors.isEmpty()) {
            _fieldValidationError.value = emptyMap()
            AppLogger.d("VALIDATION SUCCESS: No errors")
            ValidationResult(true)
        } else {
            _fieldValidationError.value = errors
            val first = errors.entries.first()
            AppLogger.d("VALIDATION FAILED: ${first.value}")
            ValidationResult(false, first.key, first.value)
        }
    }


    fun clearValidation() {
        _fieldValidationError.value = emptyMap()
    }
}