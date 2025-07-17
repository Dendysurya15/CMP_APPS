package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormAncakViewModel : ViewModel() {
    data class PageData(
        val pokokNumber: Int = 0,
        val emptyTree: Int = 0,
        val harvestTree: Int = 0,
        val neatPelepah: Int = 0, // 7
        val pelepahSengkleh: Int = 0, // 8
        val overPruning: Int = 0, // 9
        val underPruning: Int = 0, // 10
        val buahMasakTdkDipotong: Int = 0, /// 3
        val btPiringanGawangan: Int = 0, // 4
        val brdKtpGawangan: Int = 0,   //1
        val brdKtpPiringanPikulKetiak: Int = 0, //2
        val photo: String? = null,
        val comment: String? = null,
        val latIssue: Double? = null,
        val lonIssue: Double? = null,
        val createdDate: String? = null,
        val createdBy: Int? = null,
        val createdName: String? = null
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

    fun nextPage() {
        _currentPage.value = (_currentPage.value ?: 1) + 1
    }

    fun previousPage() {
        _currentPage.value = (_currentPage.value ?: 1) - 1
    }

    fun updateTotalPages(totalPages: Int) {
        _totalPages.value = totalPages
        AppLogger.d("Total pages updated to: $totalPages")
    }

    fun getPageData(pageNumber: Int): PageData? {
        val currentData = _formData.value ?: mutableMapOf()
        if (!currentData.containsKey(pageNumber)) {
            currentData[pageNumber] = PageData(pokokNumber = pageNumber)
            _formData.value = currentData
            AppLogger.d("Created new PageData for page $pageNumber")
        }
        return currentData[pageNumber]
    }

    fun savePageData(pageNumber: Int, data: PageData) {
        val currentData = _formData.value ?: mutableMapOf()
        val updatedData = data.copy(pokokNumber = pageNumber)
        currentData[pageNumber] = updatedData
        _formData.value = currentData
        AppLogger.d("Saved PageData for page $pageNumber: $updatedData")
    }

    fun updateInfoFormAncak(estate: String, afdeling: String, blok: String) {
        _estName.value = estate
        _afdName.value = afdeling
        _blokName.value = blok
    }

    fun shouldSetLatLonIssue(pageData: PageData): Boolean {
        if (pageData.emptyTree != 1) {
            return false
        }

        val hasRipeFruit = pageData.buahMasakTdkDipotong > 0 || pageData.btPiringanGawangan > 0

        if (hasRipeFruit) {
            return true
        } else {
            return (pageData.brdKtpGawangan + pageData.brdKtpPiringanPikulKetiak) > 50
        }
    }

    fun setCurrentPage(pageNumber: Int) {
        _currentPage.value = pageNumber
        AppLogger.d("Current page set to: $pageNumber")
    }

    fun updatePokokDataWithLocationAndGetTrackingStatus(
        pokokNumber: Int,
        lat: Double?,
        lon: Double?,
        prefManager: PrefManager,
        context: Context
    ): Boolean {
        val currentData = getPageData(pokokNumber) ?: PageData()

        // Check if this pokok data is already saved (has location data)
        val isAlreadySaved = currentData.latIssue != null &&
                currentData.lonIssue != null &&
                !currentData.createdDate.isNullOrEmpty() &&
                currentData.createdBy != null &&
                !currentData.createdName.isNullOrEmpty()

        if (isAlreadySaved) {
            // Data is already saved, don't overwrite it - just return the tracking status
            AppLogger.d("Pokok $pokokNumber data already saved, skipping location update")

            // Return tracking status based on existing data WITHOUT saving again
            return shouldSetLatLonIssue(currentData)
        }

        // Data is not saved yet, proceed with normal save logic
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (shouldSetLatLonIssue(currentData)) {
            // Conditions are met: Set the lat/lon issue for this pokok
            val updatedData = currentData.copy(
                latIssue = lat,
                lonIssue = lon,
                createdDate = currentDate,
                createdBy = prefManager.idUserLogin,
                createdName = prefManager.nameUserLogin
            )
            savePageData(pokokNumber, updatedData)
            AppLogger.d("Saved new location data for pokok $pokokNumber")

            // Show success toast with saved location
            Toasty.success(context, "Lat:$lat Lon:$lon sudah tersimpan", Toast.LENGTH_SHORT, true).show()

            return true // Should track this location
        } else {
            // Conditions are NOT met: Save metadata but clear location
            val updatedData = currentData.copy(
                latIssue = null,
                lonIssue = null,
                createdDate = currentDate,
                createdBy = prefManager.idUserLogin,
                createdName = prefManager.nameUserLogin
            )
            savePageData(pokokNumber, updatedData)
            AppLogger.d("Saved metadata for pokok $pokokNumber without location (no issues)")

            return false // Should remove tracking for this location
        }
    }
    fun validateCurrentPage(inspectionType: Int? = null): ValidationResult {
        val pageNumber = _currentPage.value ?: 1
        val data = getPageData(pageNumber)
        val errors = mutableMapOf<Int, String>()

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

            if (data?.overPruning == 0) {
                errors[R.id.lyOverPruningInspect] = "Kondisi OverPruning wajib diisi!"
                AppLogger.d("VALIDATION FAILED: pruning == 0")
            }

            if (data?.underPruning == 0) {
                errors[R.id.lyUnderPruningInspect] = "Kondisi UnderPruning wajib diisi!"
                AppLogger.d("VALIDATION FAILED: pruning == 0")
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

    fun clearAllData() {
        _formData.value = mutableMapOf()
        _estName.value = "-"
        _afdName.value = "-"
        _blokName.value = "-"
        _isInspection.value = true
        _currentPage.value = 1
    }
}