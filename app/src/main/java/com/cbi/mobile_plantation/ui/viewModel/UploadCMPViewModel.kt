package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadV3Response
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadWBCMPResponse
import com.cbi.mobile_plantation.data.repository.UploadCMPRepository
import com.cbi.mobile_plantation.ui.adapter.UploadCMPItem
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import kotlinx.coroutines.launch
import java.util.UUID

class UploadCMPViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UploadCMPRepository = UploadCMPRepository(application)


    data class FileZipAndStatus(
        val nama_file: String,
        val status: Int
    )

//    private val _updateStatus = MutableLiveData<Pair<Int, Boolean>>()
//    val updateStatusUploadCMP: LiveData<Pair<Int, Boolean>> = _updateStatus
//
//    private val _uploadProgressCMP = MutableLiveData<Int>()
//    val uploadProgressCMP: LiveData<Int> get() = _uploadProgressCMP
//
//    private val _uploadStatusCMP = MutableLiveData<String>()
//    val uploadStatusCMP: LiveData<String> get() = _uploadStatusCMP
//
//    private val _uploadErrorCMP = MutableLiveData<String?>()
//    val uploadErrorCMP: LiveData<String?> get() = _uploadErrorCMP
//
//    private val _uploadResult = MutableLiveData<Result<UploadCMPResponse>>()
//    val uploadResult: LiveData<Result<UploadCMPResponse>> get() = _uploadResult
//
//    private val _uploadResponseCMP = MutableLiveData<UploadCMPResponse?>()
//    val uploadResponseCMP: LiveData<UploadCMPResponse?> get() = _uploadResponseCMP

    private val _allIds = MutableLiveData<List<String>>() // Store only IDs
    val allIds: LiveData<List<String>> get() = _allIds

    fun getAllIds() {
        viewModelScope.launch {
            val data = repository.getAllData()
            val extractedIds = data.mapNotNull { it.tracking_id }
            _allIds.postValue(extractedIds) // Post IDs to LiveData
        }
    }

    private val _fileData = MutableLiveData<List<FileZipAndStatus>>() // Store nama_file and status
    val fileData: LiveData<List<FileZipAndStatus>> get() = _fileData

    fun getUploadCMPTodayData() {
        viewModelScope.launch {
            val data = repository.getAllData()
            val mappedData = data.map {
                FileZipAndStatus(
                    it.nama_file!!,
                    it.status!!
                )
            } // Convert to FileZipAndStatus
            _fileData.postValue(mappedData) // Post mapped data
        }
    }

    fun resetState() {
        viewModelScope.launch {
            // Reset all LiveData values
            _completedCount.value = 0
            _totalCount.value = 0
            _uploadProgressCMP.value = 0
            _uploadStatusCMP.value = AppUtils.UploadStatusUtils.WAITING
            _uploadErrorCMP.value = null
            _uploadResponseCMP.value = null

            // Clear all maps
            _itemProgressMap.value = emptyMap()
            _itemStatusMap.value = emptyMap()
            _itemErrorMap.value = emptyMap()
            _itemResponseMap.value = emptyMap()
        }
    }


    fun UpdateOrInsertDataUpload(
        tracking_id: String,
        nama_file: String,
        status: Int,
        tanggal_upload: String,
        table_ids: String
    ) {
        viewModelScope.launch {
            try {
                repository.UpdateOrInsertDataUpload(
                    UploadCMPModel(
                        tracking_id = tracking_id,
                        nama_file = nama_file,
                        status = status,
                        tanggal_upload = tanggal_upload,
                        table_ids = table_ids
                    )
                )
                _updateStatus.postValue(Pair(tracking_id, true)) // ✅ Return ID and Success
            } catch (e: Exception) {
                _updateStatus.postValue(Pair(tracking_id, false)) // ❌ Return ID and Failure
            }
        }
    }

//    fun uploadZipToServer(fileZip: String) {
//        viewModelScope.launch {
//            // Reset progress values (assuming single Int/Status LiveData)
//            _uploadProgressCMP.value = 0
//            _uploadStatusCMP.value = AppUtils.UploadStatusUtils.WAITING
//            _uploadErrorCMP.value = null
//            _uploadResponseCMP.value = null // Reset previous response
//
//            val result = repository.uploadZipToServer(fileZip) { progress, isSuccess, error ->
//                // Update LiveData
//                _uploadProgressCMP.postValue(progress)
//                _uploadStatusCMP.postValue(
//                    when {
//                        !isSuccess && !error.isNullOrEmpty() -> AppUtils.UploadStatusUtils.FAILED
//                        isSuccess -> AppUtils.UploadStatusUtils.SUCCESS
//                        progress in 1..99 -> AppUtils.UploadStatusUtils.UPLOADING
//                        else -> AppUtils.UploadStatusUtils.WAITING
//                    }
//                )
//                _uploadErrorCMP.postValue(error)
//            }
//
//            result?.let {
//                if (it.isSuccess) {
//                    _uploadResponseCMP.postValue(it.getOrNull()) // Store response
//                }
//            }
//        }
//    }


    private val _updateStatus = MutableLiveData<Pair<String, Boolean>>()
    val updateStatusUploadCMP: LiveData<Pair<String, Boolean>> = _updateStatus

    private val _uploadProgressCMP = MutableLiveData<Int>()
    val uploadProgressCMP: LiveData<Int> get() = _uploadProgressCMP

    private val _uploadStatusCMP = MutableLiveData<String>()
    val uploadStatusCMP: LiveData<String> get() = _uploadStatusCMP

    private val _uploadErrorCMP = MutableLiveData<String?>()
    val uploadErrorCMP: LiveData<String?> get() = _uploadErrorCMP

    private val _uploadResult = MutableLiveData<Result<UploadV3Response>>()
    val uploadResult: LiveData<Result<UploadV3Response>> get() = _uploadResult

    private val _uploadResponseCMP = MutableLiveData<UploadV3Response?>()
    val uploadResponseCMP: LiveData<UploadV3Response?> get() = _uploadResponseCMP

    // New LiveData for multiple uploads
    // Map to track progress for each upload item by ID
    private val _itemProgressMap = MutableLiveData<Map<Int, Int>>(mutableMapOf())
    val itemProgressMap: LiveData<Map<Int, Int>> get() = _itemProgressMap

    // Map to track status for each upload item by ID
    private val _itemStatusMap = MutableLiveData<Map<Int, String>>(mutableMapOf())
    val itemStatusMap: LiveData<Map<Int, String>> get() = _itemStatusMap

    // Map to track errors for each upload item by ID
    private val _itemErrorMap = MutableLiveData<Map<Int, String?>>(mutableMapOf())
    val itemErrorMap: LiveData<Map<Int, String?>> get() = _itemErrorMap

    // Map to store responses for each upload item by ID
    private val _itemResponseMap = MutableLiveData<Map<Int, UploadV3Response?>>(mutableMapOf())
    val itemResponseMap: LiveData<Map<Int, UploadV3Response?>> get() = _itemResponseMap

    // Track completed uploads count
    val _completedCount = MutableLiveData(0)
    val completedCount: LiveData<Int> get() = _completedCount

    // Track total uploads count
    private val _totalCount = MutableLiveData(0)
    val totalCount: LiveData<Int> get() = _totalCount


    private val _allIdsAndFilenames = MutableLiveData<List<Pair<String, String>>>()
    val allIdsAndFilenames: LiveData<List<Pair<String, String>>> = _allIdsAndFilenames


    // Update the ViewModel to get both tracking IDs and filenames
    fun getAllIdsAndFilenames() {
        viewModelScope.launch {
            val data = repository.getAllData()
            val extractedData = data.mapNotNull { item ->
                if (item.tracking_id != null) {
                    Pair(item.tracking_id, item.nama_file ?: "")
                } else {
                    null
                }
            }
            _allIdsAndFilenames.postValue(extractedData) // Post pairs to LiveData
        }
    }

    fun uploadMultipleJsonsV3(items: List<UploadCMPItem>) {
        viewModelScope.launch {
            // Reset counters - these operations are safe as they're in the viewModelScope
            _completedCount.value = 0
            _totalCount.value = items.size

            val progressMap = items.associate { it.id to 0 }
            val statusMap = items.associate {
                // First item should be UPLOADING, others WAITING
                if (it.id == items.firstOrNull()?.id) {
                    it.id to AppUtils.UploadStatusUtils.UPLOADING
                } else {
                    it.id to AppUtils.UploadStatusUtils.WAITING
                }
            }
            val errorMap = items.associate { it.id to null as String? }
            val responseMap = items.associate { it.id to null as UploadV3Response? }

            _itemProgressMap.value = progressMap
            _itemStatusMap.value = statusMap
            _itemErrorMap.value = errorMap
            _itemResponseMap.value = responseMap

            // For each item, upload sequentially
            for (item in items) {
                if (item.id != items.firstOrNull()?.id) {
                    // Update current item status to UPLOADING
                    AppLogger.d("Moving to next item: ${item.title}")
                    updateItemStatus(item.id, AppUtils.UploadStatusUtils.UPLOADING)
                }

                // For current item, also update the original LiveData
                _uploadProgressCMP.value = 0
                _uploadStatusCMP.value = AppUtils.UploadStatusUtils.UPLOADING
                _uploadErrorCMP.value = null
                _uploadResponseCMP.value = null

                val jsonFilePath = item.fullPath
                val filename = item.baseFilename
                val data = item.data
                val type = item.type
                val tableIds = item.tableIds
                val databaseTable = item.databaseTable

                val result = repository.uploadJsonToServerV3(
                    jsonFilePath = jsonFilePath,
                    filename = filename,
                    data = data,
                    type = type,
                    tableIds = tableIds,
                    databaseTable = databaseTable,
                    onProgressUpdate = { progress, isSuccess, error ->
                        // Update item's progress
                        updateItemProgress(item.id, progress)

                        // FOR IMAGE UPLOADS: Only update status when progress reaches 100%
                        if (type == "image") {
                            if (progress >= 100) {
                                AppLogger.d("Image upload completed for item ${item.id}: progress=$progress, isSuccess=$isSuccess")

                                updateItemStatus(item.id, AppUtils.UploadStatusUtils.UPLOADING)
                                // FIXED: Only set actual errors, not success messages
                                if (!error.isNullOrEmpty() && !error.startsWith("✓") && !error.contains("uploaded successfully")) {
                                    updateItemError(item.id, error) // Only actual errors
                                } else {
                                    updateItemError(item.id, null) // Clear error for success messages
                                }

                                _uploadProgressCMP.value = progress
                                _uploadStatusCMP.value = AppUtils.UploadStatusUtils.UPLOADING
                                _uploadErrorCMP.value = if (!error.isNullOrEmpty() && !error.startsWith("✓") && !error.contains("uploaded successfully")) error else null

                                AppLogger.d("Image upload progress 100% for item ${item.id} - waiting for final result")
                            } else {
                                // For intermediate progress (1-99%), keep status as UPLOADING
                                updateItemStatus(item.id, AppUtils.UploadStatusUtils.UPLOADING)
                                // FIXED: Only set actual errors, not success messages
                                if (!error.isNullOrEmpty() && !error.startsWith("✓") && !error.contains("uploaded successfully")) {
                                    updateItemError(item.id, error) // Only actual errors
                                } else {
                                    updateItemError(item.id, null) // Clear error for success messages
                                }

                                _uploadProgressCMP.value = progress
                                _uploadStatusCMP.value = AppUtils.UploadStatusUtils.UPLOADING
                                _uploadErrorCMP.value = if (!error.isNullOrEmpty() && !error.startsWith("✓") && !error.contains("uploaded successfully")) error else null

                                AppLogger.d("Intermediate progress for image item ${item.id}: $progress% - Status: UPLOADING - Message: $error")
                            }
                        } else {
                            // FOR NON-IMAGE UPLOADS: Keep original logic
                            val status = when {
                                !isSuccess && !error.isNullOrEmpty() -> AppUtils.UploadStatusUtils.FAILED
                                isSuccess -> AppUtils.UploadStatusUtils.SUCCESS
                                else -> AppUtils.UploadStatusUtils.UPLOADING
                            }

                            updateItemStatus(item.id, status)
                            // FIXED: Only set actual errors for non-image uploads too
                            if (!error.isNullOrEmpty() && status == AppUtils.UploadStatusUtils.FAILED) {
                                updateItemError(item.id, error) // Only actual errors
                            } else {
                                updateItemError(item.id, null) // Clear error for success
                            }

                            _uploadProgressCMP.value = progress
                            _uploadStatusCMP.value = status
                            _uploadErrorCMP.value = if (!error.isNullOrEmpty() && status == AppUtils.UploadStatusUtils.FAILED) error else null
                        }
                    }
                )

                result.fold(
                    onSuccess = { response ->
                        AppLogger.d("Upload result for item ${item.id}: success=${response.success}")

                        updateItemResponse(item.id, response)

                        val finalStatus = if (response.success) {
                            AppUtils.UploadStatusUtils.SUCCESS
                        } else {
                            AppUtils.UploadStatusUtils.FAILED
                        }

                        updateItemStatus(item.id, finalStatus)

                        // FIXED: Only set error if it's actually a failure
                        if (!response.success) {
                            updateItemError(item.id, response.message ?: "Upload failed")
                        } else {
                            updateItemError(item.id, null) // Clear any previous errors on success
                        }

                        AppLogger.d("Final status set for item ${item.id}: $finalStatus")

                        _uploadStatusCMP.value = finalStatus
                        _uploadErrorCMP.value = if (!response.success) response.message else null
                    },
                    onFailure = { error ->
                        AppLogger.d("Upload failed for ${item.title}: ${error.message}")

                        updateItemStatus(item.id, AppUtils.UploadStatusUtils.FAILED)
                        updateItemError(item.id, error.message) // This is a real error

                        _uploadStatusCMP.value = AppUtils.UploadStatusUtils.FAILED
                        _uploadErrorCMP.value = error.message
                    }
                )

                // Increase completed count regardless of success or failure
                _completedCount.value = (_completedCount.value ?: 0) + 1
            }
        }
    }


    // Helper functions for updating individual items
    private fun updateItemProgress(id: Int, progress: Int) {
        _itemProgressMap.value = _itemProgressMap.value?.toMutableMap()?.apply {
            put(id, progress)
        }
    }

    private fun updateItemStatus(id: Int, status: String) {
        _itemStatusMap.value = _itemStatusMap.value?.toMutableMap()?.apply {
            put(id, status)
        }
    }

    private fun updateItemError(id: Int, error: String?) {
        _itemErrorMap.value = _itemErrorMap.value?.toMutableMap()?.apply {
            put(id, error)
        }
    }

    private fun updateItemResponse(id: Int, response: UploadV3Response?) {
        _itemResponseMap.value = _itemResponseMap.value?.toMutableMap()?.apply {
            put(id, response)
        }
    }


    class UploadCMPViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UploadCMPViewModel::class.java)) {
                return UploadCMPViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}