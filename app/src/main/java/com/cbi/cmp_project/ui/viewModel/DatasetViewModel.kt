package com.cbi.cmp_project.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.load.engine.Resource
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.model.dataset.ProgressResponseBody
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.DatasetRepository
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.File


class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)
    private val prefManager = PrefManager(application)

    private val _regionalStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val regionalStatus: StateFlow<Result<Boolean>> = _regionalStatus.asStateFlow()

    private val _downloadStatuses = MutableLiveData<Map<String, Resource<Response<ResponseBody>>>>()
    val downloadStatuses: LiveData<Map<String, Resource<Response<ResponseBody>>>> =
        _downloadStatuses

    private val _wilayahStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val wilayahStatus: StateFlow<Result<Boolean>> = _wilayahStatus.asStateFlow()

    private val _deptStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val deptStatus: StateFlow<Result<Boolean>> = _deptStatus.asStateFlow()

    private val _divisiStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val divisiStatus: StateFlow<Result<Boolean>> = _divisiStatus.asStateFlow()

    private val _blokStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val blokStatus: StateFlow<Result<Boolean>> = _blokStatus.asStateFlow()

    private val _kemandoranStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kemandoranStatus: StateFlow<Result<Boolean>> = _kemandoranStatus.asStateFlow()

    private val _kemandoranDetailStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kemandoranDetailStatus: StateFlow<Result<Boolean>> = _kemandoranDetailStatus.asStateFlow()

    private val _karyawanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val karyawanStatus: StateFlow<Result<Boolean>> = _karyawanStatus.asStateFlow()

    private val _tphStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val tphStatus: StateFlow<Result<Boolean>> = _tphStatus.asStateFlow()

    sealed class Resource<T>(
        val data: T? = null,
        val message: String? = null,
        val progress: Int = 0,
        val isExtracting: Boolean = false,
        val isStoring: Boolean = false ,  // Add this
        val isUpToDate: Boolean = false
    ) {
        class Success<T>(data: T) : Resource<T>(data)
        class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
        class Loading<T>(progress: Int = 0) : Resource<T>(progress = progress)
        class Extracting<T>(dataset: String) : Resource<T>(message = "Extracting $dataset", isExtracting = true)
        class Storing<T>(dataset: String) : Resource<T>(message = "Storing $dataset", isStoring = true)  // Add this
        class UpToDate<T>(dataset: String) : Resource<T>(message = "Dataset $dataset is up to date", isUpToDate = true)  // Add this
    }

    fun updateOrInsertRegional(regionals: List<RegionalModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertRegional(regionals)
                _regionalStatus.value = Result.success(true)
            } catch (e: Exception) {
                _regionalStatus.value = Result.failure(e)
            }
        }

    fun updateOrInsertWilayah(wilayah: List<WilayahModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertWilayah(wilayah)
            _wilayahStatus.value = Result.success(true)
        } catch (e: Exception) {
            _wilayahStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertDept(dept: List<DeptModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertDept(dept)
            _deptStatus.value = Result.success(true)
        } catch (e: Exception) {
            _deptStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertDivisi(divisions: List<DivisiModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertDivisi(divisions)
            _divisiStatus.value = Result.success(true)
        } catch (e: Exception) {
            _divisiStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertBlok(blok: List<BlokModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertBlok(blok)
            _blokStatus.value = Result.success(true)
        } catch (e: Exception) {
            _blokStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertKemandoran(kemandoran)
                _kemandoranStatus.value = Result.success(true)
            } catch (e: Exception) {
                _kemandoranStatus.value = Result.failure(e)
            }
        }

    fun updateOrInsertKemandoranDetail(kemandoran_detail: List<KemandoranDetailModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertKemandoranDetail(kemandoran_detail)
                _kemandoranDetailStatus.value = Result.success(true)
            } catch (e: Exception) {
                _kemandoranDetailStatus.value = Result.failure(e)
            }
        }

    fun updateOrInsertKaryawan(karyawan: List<KaryawanModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertKaryawan(karyawan)
                _karyawanStatus.value = Result.success(true)
            } catch (e: Exception) {
                _karyawanStatus.value = Result.failure(e)
            }
        }

        fun updateOrInsertTPH(tph: List<TPHNewModel>) = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertTPH(tph)
                _tphStatus.value = Result.success(true)
            } catch (e: Exception) {
                _tphStatus.value = Result.failure(e)
            }
        }

    suspend fun getDeptList(estateId: String): List<DeptModel> {
        return repository.getDeptByRegionalAndEstate(estateId)
    }

    suspend fun getDivisiList(idEstate: Int): List<DivisiModel> {
        return repository.getDivisiList(idEstate)
    }

    suspend fun getBlokList(
        idRegional: Int,
        idEstate: Int,
        idDivisi: Int,
        estateAbbr: String
    ): List<BlokModel> {
        return repository.getBlokList(idRegional, idEstate, idDivisi, estateAbbr)
    }

    suspend fun getKemandoranList(
        idEstate: Int,
        idDivisiArray: List<Int>,
        estateAbbr: String
    ): List<KemandoranModel> {
        return repository.getKemandoranList(idEstate, idDivisiArray, estateAbbr)
    }

    suspend fun getTPHList(
        idRegional: Int,
        idEstate: Int,
        idDivisi: Int,
        estateAbbr: String,
        tahunTanam: String,
        idBlok: Int
    ): List<TPHNewModel> {
        return repository.getTPHList(idRegional, idEstate, idDivisi, estateAbbr, tahunTanam, idBlok)
    }

    suspend fun getKemandoranDetailList(idHeader: Int): List<KemandoranDetailModel> {
        return repository.getKemandoranDetailList(idHeader)
    }

    suspend fun getKaryawanList(filteredId: Array<String>): List<KaryawanModel> {
        return repository.getKaryawanList(filteredId)
    }

    private suspend fun <T> processDataset(
        jsonContent: String,
        dataset: String,
        modelClass: Class<T>,
        results: MutableMap<String, Resource<Response<ResponseBody>>>,
        response: Response<ResponseBody>,
        updateOperation: suspend (List<T>) -> Job,
        statusFlow: StateFlow<Result<Boolean>>,
        hasShownError: Boolean, // Add as reference
        lastModifiedTimestamp: String,
    ): Boolean {  // Return updated hasShownError value
        var updatedHasShownError = hasShownError
        try {
            val dataList = parseStructuredJsonToList(jsonContent, modelClass)
            AppLogger.d("Parsed ${dataset} list size: ${dataList.size}")

            updateOperation(dataList).join()

            if (statusFlow.value.isSuccess) {
                results[dataset] = Resource.Success(response)
                when (dataset) {
                    "tph" -> prefManager.lastModifiedDatasetTPH = lastModifiedTimestamp
                    "blok" -> prefManager.lastModifiedDatasetBlok = lastModifiedTimestamp
                    "kemandoran" -> prefManager.lastModifiedDatasetKemandoran = lastModifiedTimestamp
                    "pemanen" -> prefManager.lastModifiedDatasetPemanen = lastModifiedTimestamp
                }
                prefManager!!.addDataset(dataset)
            } else {
                val error = statusFlow.value.exceptionOrNull()
                throw error ?: Exception("Unknown database error")
            }
        } catch (e: Exception) {
            AppLogger.e("$dataset processing error: ${e.message}")
            if (!updatedHasShownError) {
                results[dataset] = Resource.Error("Error processing $dataset data: ${e.message}")
                updatedHasShownError = true
            }
        }
        return updatedHasShownError
    }




    fun downloadMultipleDatasets(requests: List<DatasetRequest>) {
        viewModelScope.launch {
            val results = mutableMapOf<String, Resource<Response<ResponseBody>>>()
            var hasShownError = false  // Flag to track if error has been shown

            requests.forEach { request ->

                val keyDownload = request.dataset
                val tphId = request.estate
                val regionalId = request.regional
                results[request.dataset] = Resource.Loading(0)
                _downloadStatuses.postValue(results.toMap())

                try {
                    val response = repository.downloadDataset(request)

                    // Get headers
                    val contentType = response.headers()["Content-Type"]
                    val lastModified = response.headers()["Last-Modified-Dataset"]
                    val contentDisposition = response.headers()["Content-Disposition"]

                    Log.d("DownloadResponse", "Last-Updated: $lastModified")
                    Log.d("DownloadResponse", "Content-Disposition: $contentDisposition")

                    when (response.code()) {
                        200 -> {
                            if (contentType?.contains("application/zip") == true) {
                                val inputStream = response.body()?.byteStream()
                                if (inputStream != null) {
                                    Log.d("DownloadResponse", "Received ZIP file, size: ${response.body()?.contentLength()} bytes")

                                    try {
                                        // Update status to extracting
                                        results[request.dataset] = Resource.Extracting(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Create temp file
                                        val tempFile = File.createTempFile("temp_", ".zip", getApplication<Application>().cacheDir)
                                        tempFile.outputStream().use { fileOut ->
                                            inputStream.copyTo(fileOut)
                                        }

                                        // Extract with password
                                        val zipFile = net.lingala.zip4j.ZipFile(tempFile)
                                        zipFile.setPassword("CBI@2025".toCharArray())

                                        val extractDir = File(getApplication<Application>().cacheDir, "extracted_${System.currentTimeMillis()}")
                                        zipFile.extractAll(extractDir.absolutePath)

                                        // Read output.json
                                        val jsonFile = File(extractDir, "output.json")
                                        if (jsonFile.exists()) {
                                            val jsonContent = jsonFile.readText()
                                            Log.d("DownloadResponse", "Extracted JSON content: $jsonContent")

                                            // Update status to storing
                                            results[request.dataset] = Resource.Storing(request.dataset)
                                            _downloadStatuses.postValue(results.toMap())

                                            try {
                                                when (request.dataset) {
                                                    "tph" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = TPHNewModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::updateOrInsertTPH,
                                                        statusFlow = tphStatus,
                                                        hasShownError = hasShownError,
                                                       lastModifiedTimestamp = lastModified ?: ""
                                                    )
                                                    "blok" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = BlokModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::updateOrInsertBlok,
                                                        statusFlow = blokStatus,
                                                        hasShownError = hasShownError,
                                                       lastModifiedTimestamp = lastModified ?: ""
                                                    )
                                                    "kemandoran" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = KemandoranModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::updateOrInsertKemandoran,
                                                        statusFlow = kemandoranStatus,
                                                        hasShownError = hasShownError,
                                                       lastModifiedTimestamp = lastModified ?: ""
                                                    )
                                                    "pemanen" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = KaryawanModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::updateOrInsertKaryawan,
                                                        statusFlow = karyawanStatus,
                                                        hasShownError = hasShownError,
                                                       lastModifiedTimestamp = lastModified ?: ""
                                                    )
                                                    else -> {

                                                        results[request.dataset] = Resource.Error("Failed to stored because no process storing dataset for  ${request.dataset}")
                                                        hasShownError = true
                                                    }

                                                }
                                                _downloadStatuses.postValue(results.toMap())
                                            } catch (e: Exception) {
                                                AppLogger.e("General processing error: ${e.message}")
                                                if (!hasShownError) {
                                                    results[request.dataset] = Resource.Error("Processing error: ${e.message ?: "Unknown error"}")
                                                    hasShownError = true
                                                }
                                                _downloadStatuses.postValue(results.toMap())
                                            }
                                        }

                                        // Cleanup
                                        tempFile.delete()
                                        extractDir.deleteRecursively()

                                    } catch (e: Exception) {
                                        Log.e("DownloadResponse", "Error extracting zip: ${e.message}")
                                        if (!hasShownError) {
                                            results[request.dataset] = Resource.Error("Error extracting zip: ${e.message}")
                                            hasShownError = true
                                        }
                                    }
                                } else {
                                    Log.d("DownloadResponse", "ZIP response body is null")
                                    results[request.dataset] = Resource.Error("ZIP response body is null")
                                }
                            } else if (contentType?.contains("application/json") == true) {
                                val responseBodyString = response.body()?.string() ?: "Empty Response"
                                Log.d("DownloadResponse", "Received JSON: $responseBodyString")

                                if (responseBodyString.contains("\"success\":false") && responseBodyString.contains("\"message\":\"Dataset is up to date\"")) {
                                    results[request.dataset] = Resource.UpToDate(request.dataset)
                                } else {
                                    results[request.dataset] = Resource.Success(response)
                                }
                            } else {
                                Log.d("DownloadResponse", "Unknown response type: $contentType")
                                results[request.dataset] = Resource.Error("Unknown response type: $contentType")
                            }
                        }
                        401 -> {
                            if (!hasShownError) {
                                results[request.dataset] = Resource.Error(
                                    "Authentication failed. Please login again.",
                                    response
                                )
                                hasShownError = true
                            } else {
                                results[request.dataset] = Resource.Error("Download failed", response)
                            }
                        }
                        else -> {
                            if (!hasShownError) {
                                results[request.dataset] = Resource.Error(
                                    "Download failed with status code: ${response.code()}",
                                    response
                                )
                                hasShownError = true
                            } else {
                                results[request.dataset] = Resource.Error("Download failed", response)
                            }
                        }
                    }
                    _downloadStatuses.postValue(results.toMap())

                } catch (e: Exception) {
//                    if (!hasShownError) {
                        results[request.dataset] = Resource.Error(
                            "Network error: ${e.message ?: "Unknown error"}",
                            null
                        )
                        hasShownError = true
//                    } else {
//                        results[request.dataset] = Resource.Error("Download failed", null)
//                    }
                    _downloadStatuses.postValue(results.toMap())
                }
                _downloadStatuses.postValue(results.toMap())
            }
        }
    }

    private fun <T> parseStructuredJsonToList(jsonContent: String, classType: Class<T>): List<T> {
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

        // Get the key mappings
        val keyMappings = jsonObject.getAsJsonObject("key")
            .entrySet()
            .associate { (key, value) ->
                key to value.asString
            }

        // Get the data array
        val dataArray = jsonObject.getAsJsonArray("data")

        // Transform each data object using the key mappings
        val transformedArray = JsonArray().apply {
            dataArray.forEach { element ->
                val originalObj = element.asJsonObject
                val transformedObj = JsonObject()

                // For each numbered field in the data object
                originalObj.entrySet().forEach { (key, value) ->
                    // Get the actual field name from key mappings
                    val fieldName = keyMappings[key] ?: return@forEach
                    // Add to transformed object with proper field name
                    transformedObj.add(fieldName, value)
                }

                add(transformedObj)
            }
        }

        // Log the transformed JSON for debugging
        Log.d("JsonTransform", "Transformed JSON: $transformedArray")

        // Convert to your data class list
        return gson.fromJson(transformedArray, TypeToken.getParameterized(List::class.java, classType).type)
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