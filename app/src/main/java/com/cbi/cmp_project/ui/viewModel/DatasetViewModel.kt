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
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.DatasetRepository
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializer
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

    private val _kemandoranStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kemandoranStatus: StateFlow<Result<Boolean>> = _kemandoranStatus.asStateFlow()

    private val _karyawanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val karyawanStatus: StateFlow<Result<Boolean>> = _karyawanStatus.asStateFlow()

    private val _millStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val millStatus: StateFlow<Result<Boolean>> = _millStatus.asStateFlow()

    private val _transporterStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val transporterStatus: StateFlow<Result<Boolean>> = _transporterStatus.asStateFlow()

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

    fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertKemandoran(kemandoran)
                _kemandoranStatus.value = Result.success(true)
            } catch (e: Exception) {
                _kemandoranStatus.value = Result.failure(e)
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

    fun updateOrInsertMill(mill: List<MillModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertMill(mill)
                _karyawanStatus.value = Result.success(true)
            } catch (e: Exception) {
                _karyawanStatus.value = Result.failure(e)
            }
        }

    fun InsertTransporter(transporter: List<TransporterModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.InsertTransporter(transporter)
                _karyawanStatus.value = Result.success(true)
            } catch (e: Exception) {
                _karyawanStatus.value = Result.failure(e)
            }
        }

    fun updateOrInsertTPH(tph: List<TPHNewModel>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            AppLogger.d("ViewModel: Starting updateOrInsertTPH with ${tph.size} records")
            repository.updateOrInsertTPH(tph)
            _tphStatus.value = Result.success(true)
            AppLogger.d("ViewModel: Successfully updated database")
        } catch (e: Exception) {
            _tphStatus.value = Result.failure(e)
            AppLogger.e("ViewModel: Error updating database - ${e.message}")
        }
    }



    suspend fun getDivisiList(idEstate: Int): List<TPHNewModel> {
        return repository.getDivisiList(idEstate)
    }

    suspend fun getBlokList(
        idEstate: Int,
        idDivisi: Int,
    ): List<TPHNewModel> {
        return repository.getBlokList( idEstate, idDivisi)
    }

    suspend fun getKemandoranList(
        idEstate: Int,
        idDivisiArray: List<Int>
    ): List<KemandoranModel> {
        return repository.getKemandoranList(idEstate, idDivisiArray)
    }

    suspend fun getKemandoranEstate(
        idEstate: Int,
    ): List<KemandoranModel> {
        return repository.getKemandoranEstate(idEstate)
    }

    suspend fun getKemandoranAbsensiList(
        idEstate: Int,
        idDivisiArray: List<Int>
    ): List<KemandoranModel> {
        return repository.getKemandoranAbsensiList(idEstate, idDivisiArray)
    }

    suspend fun getTPHList(

        idEstate: Int,
        idDivisi: Int,
        tahunTanam: String,
        idBlok: Int
    ): List<TPHNewModel> {
        return repository.getTPHList( idEstate, idDivisi, tahunTanam, idBlok)
    }

    suspend fun getKaryawanList(filteredId: Int): List<KaryawanModel> {
        return repository.getKaryawanList(filteredId)
    }

    private fun parseTPHJsonToList(jsonContent: String): List<TPHNewModel> {
        try {
            val jsonObject = JsonParser().parse(jsonContent).asJsonObject
            val keyMappings = jsonObject.getAsJsonObject("key")
                .entrySet()
                .associate { (key, value) -> key to value.asString }

            val dataArray = jsonObject.getAsJsonArray("data")
            val resultList = mutableListOf<TPHNewModel>()

            dataArray.forEach { element ->
                try {
                    val obj = element.asJsonObject
                    val mappedObj = JsonObject()

                    // Map the numbered keys to actual field names
                    obj.entrySet().forEach { (key, value) ->
                        val fieldName = keyMappings[key] ?: return@forEach
                        mappedObj.add(fieldName, value)
                    }

                    // Manual conversion to TPHNewModel
                    val model = TPHNewModel(
                        id = mappedObj.get("id")?.asInt,
                        regional = mappedObj.get("regional").toString(),
                        company = mappedObj.get("company")?.asInt,
                        company_abbr = mappedObj.get("company_abbr")?.asString,
                        dept = mappedObj.get("dept")?.asInt,
                        dept_abbr = mappedObj.get("dept_abbr")?.asString,
                        divisi = mappedObj.get("divisi")?.asInt,
                        divisi_abbr = mappedObj.get("divisi_abbr")?.asString,
                        blok = mappedObj.get("blok")?.asInt,
                        blok_kode = mappedObj.get("blok_kode")?.asString,
                        ancak = mappedObj.get("ancak")?.asString,
                        nomor = mappedObj.get("nomor")?.asString,
                        tahun = mappedObj.get("tahun")?.asString,
                        luas_area = mappedObj.get("luas_area")?.asString,
                        jml_pokok = mappedObj.get("jml_pokok")?.asString,
                        jml_pokok_ha = mappedObj.get("jml_pokok_ha")?.asString,
                        lat = mappedObj.get("lat")?.asString,
                        lon = mappedObj.get("lon")?.asString,
                        update_date = mappedObj.get("update_date")?.asString,
                        status = mappedObj.get("status")?.asString
                    )

                    // Log the first few items to check conversion
                    if (resultList.size < 2) {
                        AppLogger.d("Converted item ${resultList.size + 1}: $model")
                    }

                    resultList.add(model)
                } catch (e: Exception) {
                    AppLogger.e("Error converting single item: ${e.message}")
                    // Continue with next item
                }
            }

            AppLogger.d("Successfully converted ${resultList.size} items")
            return resultList

        } catch (e: Exception) {
            AppLogger.e("Error parsing JSON: ${e.message}")
            throw e
        }
    }

    private suspend fun <T> processDataset(
        jsonContent: String,
        dataset: String,
        modelClass: Class<T>,
        results: MutableMap<String, Resource<Response<ResponseBody>>>,
        response: Response<ResponseBody>,
        updateOperation: suspend (List<T>) -> Job,
        statusFlow: StateFlow<Result<Boolean>>,
        hasShownError: Boolean,
        lastModifiedTimestamp: String,
    ): Boolean {
        var updatedHasShownError = hasShownError
        try {
            AppLogger.d(jsonContent)
            val dataList = try {
                when (dataset) {
                    "tph" -> parseTPHJsonToList(jsonContent) as List<T>
                    else -> parseStructuredJsonToList(jsonContent, modelClass)
                }
            } catch (e: Exception) {
                updatedHasShownError = true
                results[dataset] = Resource.Error("Error parsing $dataset data: ${e.message}")
                return updatedHasShownError
            }

            AppLogger.d("Parsed ${dataset} list size: ${dataList.size}")

            // Check if data is valid
            if (dataList.isEmpty()) {
                updatedHasShownError = true
                results[dataset] = Resource.Error("No valid data found for $dataset")
                return updatedHasShownError
            }

            // Check first item for all null values
            val firstItem = dataList.firstOrNull()
            if (firstItem != null) {
                val allFieldsNull = when (firstItem) {
                    is TPHNewModel -> firstItem.run {
                        id == null && regional == null && company == null &&
                                company_abbr == null && dept == null && dept_abbr == null &&
                                divisi == null && divisi_abbr == null && blok == null &&
                                blok_kode == null && ancak == null && nomor == null &&
                                tahun == null && luas_area == null && jml_pokok == null &&
                                jml_pokok_ha == null && lat == null && lon == null &&
                                update_date == null && status == null
                    }
                    // Add other model checks if needed
                    else -> false
                }

                if (allFieldsNull) {
                    AppLogger.e("Invalid data format: All fields are null in $dataset")
                    updatedHasShownError = true
                    results[dataset] = Resource.Error("Invalid data format for $dataset: All fields are null")
                    return updatedHasShownError
                }
            }

            AppLogger.d("Data validation passed for $dataset")
            AppLogger.d(dataList.toString())

            try {
                AppLogger.d("Executing update operation for dataset: $dataset")
                updateOperation(dataList).join()
            } catch (e: Exception) {
                AppLogger.e("Error updating dataset $dataset: ${e.message}")
                updatedHasShownError = true
                results[dataset] = Resource.Error("Failed to update dataset: $dataset - ${e.message}")
                return updatedHasShownError
            }

            if (statusFlow.value.isSuccess) {
                AppLogger.d("Update operation successful for dataset: $dataset")
                results[dataset] = Resource.Success(response)

                when (dataset) {
                    "tph" -> prefManager.lastModifiedDatasetTPH = lastModifiedTimestamp
                    "blok" -> prefManager.lastModifiedDatasetBlok = lastModifiedTimestamp
                    "kemandoran" -> prefManager.lastModifiedDatasetKemandoran = lastModifiedTimestamp
                    "pemanen" -> prefManager.lastModifiedDatasetPemanen = lastModifiedTimestamp
                    "transporter" -> prefManager.lastModifiedDatasetTransporter = lastModifiedTimestamp
                }
                prefManager!!.addDataset(dataset)
            } else {
                val error = statusFlow.value.exceptionOrNull()
                AppLogger.e("Database error for dataset $dataset: ${error?.message}")
                updatedHasShownError = true
                results[dataset] = Resource.Error("Database error while processing $dataset: ${error?.message ?: "Unknown error"}")
                return updatedHasShownError
            }
        } catch (e: Exception) {
            AppLogger.e("$dataset processing error: ${e.message}")
            updatedHasShownError = true
            results[dataset] = Resource.Error("Error processing $dataset data: ${e.message}")
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
                    var response: Response<ResponseBody>? = null
                    if (request.dataset == "mill") {
                        response = repository.downloadSmallDataset(request.regional ?: 0)

                        AppLogger.d(response.toString())
                    } else {
                        response = repository.downloadDataset(request)
                    }


                    // Get headers
                    val contentType = response.headers()["Content-Type"]
                    val lastModified = response.headers()["Last-Modified-Dataset"]

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


                                            AppLogger.d(request.dataset)
                                            AppLogger.d(lastModified.toString())
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
                                                    "mill" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = MillModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::updateOrInsertMill,
                                                        statusFlow = millStatus,
                                                        hasShownError = hasShownError,
                                                        lastModifiedTimestamp = lastModified ?: ""
                                                    )
                                                    "transporter" -> hasShownError = processDataset(
                                                        jsonContent = jsonContent,
                                                        dataset = request.dataset,
                                                        modelClass = TransporterModel::class.java,
                                                        results = results,
                                                        response = response,
                                                        updateOperation = ::InsertTransporter,
                                                        statusFlow = transporterStatus,
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
                                }
                                else if (request.dataset == "mill") {

                                    try {
                                        // Create a new simpler parse function just for mill data
                                        fun <T> parseMillJsonToList(jsonContent: String, classType: Class<T>): List<T> {
                                            val gson = Gson()
                                            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
                                            val dataArray = jsonObject.getAsJsonArray("data")
                                            return gson.fromJson(dataArray, TypeToken.getParameterized(List::class.java, classType).type)
                                        }

                                        // Use the new parse function
                                        val millList = parseMillJsonToList(responseBodyString, MillModel::class.java)

                                        // Update status to storing
                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Store the data
                                        updateOrInsertMill(millList).join()

                                        if (millStatus.value.isSuccess) {
                                            results[request.dataset] = Resource.Success(response)
                                            prefManager!!.addDataset(request.dataset)
                                        } else {
                                            val error = millStatus.value.exceptionOrNull()
                                            throw error ?: Exception("Unknown database error")
                                        }

                                        _downloadStatuses.postValue(results.toMap())

                                    } catch (e: Exception) {
                                        Log.e("DownloadResponse", "Error processing mill data: ${e.message}")
                                        if (!hasShownError) {
                                            results[request.dataset] = Resource.Error("Error processing mill data: ${e.message}")
                                            hasShownError = true
                                        }
                                        _downloadStatuses.postValue(results.toMap())
                                    }
                                }
                                else {
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
        val gson = GsonBuilder()
            .setLenient()  // Add lenient parsing
            .create()

        try {
            val jsonObject = JsonParser().parse(jsonContent).asJsonObject
            val keyMappings = jsonObject.getAsJsonObject("key")
                .entrySet()
                .associate { (key, value) -> key to value.asString }

            val dataArray = jsonObject.getAsJsonArray("data")
            val transformedArray = JsonArray()

            // Transform all at once instead of chunks
            dataArray.forEach { element ->
                val originalObj = element.asJsonObject
                val transformedObj = JsonObject()

                originalObj.entrySet().forEach { (key, value) ->
                    val fieldName = keyMappings[key] ?: return@forEach
                    transformedObj.add(fieldName, value)
                }

                transformedArray.add(transformedObj)
            }

            AppLogger.d("About to convert array of size: ${transformedArray.size()}")

            AppLogger.d(transformedArray.toString())
            return gson.fromJson(transformedArray,
                TypeToken.getParameterized(List::class.java, classType).type)
        } catch (e: Exception) {
            AppLogger.e("Error parsing JSON: ${e.message}")
            throw e
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