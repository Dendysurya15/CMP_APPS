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
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response


class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)

    private val _regionalStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val regionalStatus: StateFlow<Result<Boolean>> = _regionalStatus.asStateFlow()

    private val _downloadStatuses = MutableLiveData<Map<String, Resource<Response<ResponseBody>>>>()
    val downloadStatuses: LiveData<Map<String, Resource<Response<ResponseBody>>>> = _downloadStatuses

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
        val progress: Int = 0
    ) {
        class Success<T>(data: T) : Resource<T>(data)
        class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
        class Loading<T>(progress: Int = 0) : Resource<T>(progress = progress)  // Added progress parameter
    }


    fun updateOrInsertRegional(regionals: List<RegionalModel>) = viewModelScope.launch(Dispatchers.IO) {
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

    fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertKemandoran(kemandoran)
            _kemandoranStatus.value = Result.success(true)
        } catch (e: Exception) {
            _kemandoranStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertKemandoranDetail(kemandoran_detail: List<KemandoranDetailModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateOrInsertKemandoranDetail(kemandoran_detail)
            _kemandoranDetailStatus.value = Result.success(true)
        } catch (e: Exception) {
            _kemandoranDetailStatus.value = Result.failure(e)
        }
    }

    fun updateOrInsertKaryawan(karyawan: List<KaryawanModel>) = viewModelScope.launch(Dispatchers.IO) {
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
        return repository.getDivisiList( idEstate)
    }

    suspend fun getBlokList(idRegional :Int,idEstate: Int, idDivisi:Int, estateAbbr :String): List<BlokModel> {
        return repository.getBlokList(idRegional,idEstate, idDivisi,estateAbbr)
    }

    suspend fun getKemandoranList(idEstate: Int, idDivisiArray: List<Int>, estateAbbr: String): List<KemandoranModel> {
        return repository.getKemandoranList(idEstate, idDivisiArray, estateAbbr)
    }

    suspend fun getTPHList(idRegional:Int, idEstate: Int, idDivisi:Int, estateAbbr :String, tahunTanam : String, idBlok :Int): List<TPHNewModel> {
        return repository.getTPHList(idRegional, idEstate, idDivisi, estateAbbr,tahunTanam,  idBlok)
    }

    suspend fun getKemandoranDetailList(idHeader: Int): List<KemandoranDetailModel> {
        return repository.getKemandoranDetailList(idHeader)
    }

    suspend fun getKaryawanList(filteredId: Array<String>): List<KaryawanModel> {
        return repository.getKaryawanList(filteredId)
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
                    val lastModified = response.headers()["last_modified"]
                    val contentDisposition = response.headers()["Content-Disposition"]

// Log headers
                    Log.d("DownloadResponse", "Last-Modified: $lastModified")
                    Log.d("DownloadResponse", "Content-Disposition: $contentDisposition")

// Extract filename if available
                    val filename = contentDisposition?.let {
                        val regex = "filename=\"([^\"]+)\"".toRegex()
                        regex.find(it)?.groups?.get(1)?.value
                    } ?: "unknown.zip"

                    Log.d("DownloadResponse", "Extracted Filename: $filename")

// Read response
                    if (contentType?.contains("application/zip") == true) {
                        val inputStream = response.body()?.byteStream()
                        if (inputStream != null) {
                            Log.d("DownloadResponse", "Received ZIP file, size: ${response.body()?.contentLength()} bytes")
                            Log.d("DownloadResponse", "Saving as: $filename")

                            // TODO: Save the ZIP file if needed
                        } else {
                            Log.d("DownloadResponse", "ZIP response body is null")
                        }
                    } else if (contentType?.contains("application/json") == true) {
                        val responseBodyString = response.body()?.string() ?: "Empty Response"
                        Log.d("DownloadResponse", "Received JSON: $responseBodyString")
                    } else {
                        Log.d("DownloadResponse", "Unknown response type: $contentType")
                    }


                    when (response.code()) {
                        200 -> results[request.dataset] = Resource.Success(response)
                        401 -> {
//                            AppLogger.d("Download Status: ${request.dataset} failed with unauthorized access")
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
//                            AppLogger.d("Download Status: ${request.dataset} failed with code: ${response.code()}")
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
                } catch (e: Exception) {
//                    AppLogger.d("Download Status: ${request.dataset} failed with exception: ${e.message}")
                    if (!hasShownError) {
                        results[request.dataset] = Resource.Error(
                            "Network error: ${e.message ?: "Unknown error"}",
                            null
                        )
                        hasShownError = true
                    } else {
                        results[request.dataset] = Resource.Error("Download failed", null)
                    }
                }
                _downloadStatuses.postValue(results.toMap())
            }
        }
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