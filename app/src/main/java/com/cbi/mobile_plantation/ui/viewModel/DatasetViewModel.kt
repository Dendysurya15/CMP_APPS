package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.database.KaryawanDao

import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.data.model.uploadCMP.FetchResponseItem
import com.cbi.mobile_plantation.data.model.uploadCMP.FetchStatusCMPResponse
import com.cbi.mobile_plantation.data.repository.DatasetRepository
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.File


@Suppress("NAME_SHADOWING")
class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)
    private val prefManager = PrefManager(application)

    private val database = AppDatabase.getDatabase(application)
    private val uploadCMPDao = database.uploadCMPDao()
    private val espbDao = database.espbDao()
    private val panenDao = database.panenDao()
    private val absensiDao = database.absensiDao()
    private val blokDao = database.blokDao()


    private val _downloadStatuses = MutableLiveData<Map<String, Resource<Response<ResponseBody>>>>()
    val downloadStatuses: LiveData<Map<String, Resource<Response<ResponseBody>>>> =
        _downloadStatuses

    private val _kemandoranStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kemandoranStatus: StateFlow<Result<Boolean>> = _kemandoranStatus.asStateFlow()

    private val _karyawanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val karyawanStatus: StateFlow<Result<Boolean>> = _karyawanStatus.asStateFlow()

    private val _blokStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val blokStatus: StateFlow<Result<Boolean>> = _blokStatus.asStateFlow()

    private val _millStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val millStatus: StateFlow<Result<Boolean>> = _millStatus.asStateFlow()

    private val _transporterStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val transporterStatus: StateFlow<Result<Boolean>> = _transporterStatus.asStateFlow()

    private val _kendaraanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kendaraanStatus: StateFlow<Result<Boolean>> = _kendaraanStatus.asStateFlow()

    private val _tphStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val tphStatus: StateFlow<Result<Boolean>> = _tphStatus.asStateFlow()

    private val _fetchStatusUploadCMPLiveData = MutableLiveData<List<FetchResponseItem>>()
    val fetchStatusUploadCMPLiveData: LiveData<List<FetchResponseItem>> =
        _fetchStatusUploadCMPLiveData

    sealed class Resource<T>(
        val data: T? = null,
        val message: String? = null,
        val progress: Int = 0,
        val isExtracting: Boolean = false,
        val isStoring: Boolean = false,  // Add this
        val isUpToDate: Boolean = false
    ) {
        class Success<T>(data: T, message: String? = null) : Resource<T>(data, message)
        class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
        class Loading<T>(progress: Int = 0) : Resource<T>(progress = progress)
        class Extracting<T>(dataset: String) :
            Resource<T>(message = "Extracting $dataset...", isExtracting = true)

        class Storing<T>(dataset: String) :
            Resource<T>(message = "Storing $dataset to database...", isStoring = true)  // Add this

        class UpToDate<T>(dataset: String) :
            Resource<T>(message = "Dataset $dataset is up to date", isUpToDate = true)  // Add this
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

    fun clearAllData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { uploadCMPDao.dropAllData() }
                withContext(Dispatchers.IO) { espbDao.dropAllData() }
                withContext(Dispatchers.IO) { panenDao.dropAllData() }
                withContext(Dispatchers.IO) { absensiDao.dropAllData() }
                withContext(Dispatchers.IO) { blokDao.dropAllData() }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error clearing database: ${e.message}")
            }
        }
    }

    fun updateOrInsertBlok(blok: List<BlokModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertBlok(blok)
                _blokStatus.value = Result.success(true)
            } catch (e: Exception) {
                _blokStatus.value = Result.failure(e)
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

    fun InsertKendaraan(kendaraan: List<KendaraanModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.InsertKendaraan(kendaraan)
                _kendaraanStatus.value = Result.success(true)
            } catch (e: Exception) {
                _kendaraanStatus.value = Result.failure(e)
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
        return repository.getBlokList(idEstate, idDivisi)
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

    suspend fun getKemandoranEstateExcept(
        idEstate: Int,
        idDivisiArray: List<Int>,
    ): List<KemandoranModel> {
        return repository.getKemandoranEstateExcept(idEstate, idDivisiArray)
    }

    suspend fun getAllTransporter(): List<TransporterModel> {
        return repository.getAllTransporter()
    }

    suspend fun getAllNopol(): List<KendaraanModel> {
        return repository.getAllNopol()
    }

    suspend fun getTPHList(

        idEstate: Int,
        idDivisi: Int,
        tahunTanam: String,
        idBlok: Int
    ): List<TPHNewModel> {
        return repository.getTPHList(idEstate, idDivisi, tahunTanam, idBlok)
    }

    suspend fun getKaryawanList(filteredId: Int): List<KaryawanModel> {
        return repository.getKaryawanList(filteredId)
    }


    suspend fun getKaryawanKemandoranList(filteredIds: List<String>): List<KaryawanDao.KaryawanKemandoranData> {
        return repository.getKaryawanKemandoranList(filteredIds) // Pass list directly
    }


    private fun parseTPHJsonToList(jsonContent: String): List<TPHNewModel> {
        try {
            val jsonObject = JsonParser().parse(jsonContent).asJsonObject
            val keyMappings = jsonObject.getAsJsonObject("key")
                .entrySet()
                .associate { (key, value) -> key to value.asString }

            val dataArray = jsonObject.getAsJsonArray("data")
            val resultList = mutableListOf<TPHNewModel>()

            dataArray.forEachIndexed { index, element ->
                try {
                    val obj = element.asJsonObject
                    val mappedObj = JsonObject()

                    // Map the numbered keys to actual field names
                    obj.entrySet().forEach { (key, value) ->
                        val fieldName = keyMappings[key] ?: return@forEach
                        mappedObj.add(fieldName, value)
                    }

                    // Log the mapped JSON before trying to convert
                    AppLogger.d("Processing item #${index + 1}, mapped JSON: $mappedObj")

                    // Safe way to get values that handles null values properly
                    fun safeGetString(field: String): String? {
                        val value = mappedObj.get(field)
                        return if (value == null || value.isJsonNull) null else value.asString
                    }

                    fun safeGetInt(field: String): Int? {
                        val value = mappedObj.get(field)
                        return if (value == null || value.isJsonNull) null else value.asInt
                    }

                    fun safeGetDouble(field: String): String? {
                        val value = mappedObj.get(field)
                        return if (value == null || value.isJsonNull) null else value.asDouble.toString()
                    }

                    // Manual conversion to TPHNewModel with proper null handling
                    val model = TPHNewModel(
                        id = safeGetInt("id"),
                        regional = safeGetString("regional") ?: safeGetInt("regional")?.toString() ?: "0",
                        company = safeGetInt("company"),
                        company_abbr = safeGetString("company_abbr"),
                        wilayah = safeGetString("wilayah"),
                        dept = safeGetInt("dept"),
                        dept_ppro = safeGetInt("dept_ppro"),
                        dept_abbr = safeGetString("dept_abbr"),
                        divisi = safeGetInt("divisi"),
                        divisi_ppro = safeGetInt("divisi_ppro"),
                        divisi_abbr = safeGetString("divisi_abbr"),
                        divisi_nama = safeGetString("divisi_nama"),
                        blok = safeGetInt("blok"),
                        blok_ppro = safeGetInt("blok_ppro"),
                        blok_kode = safeGetString("blok_kode"),
                        blok_nama = safeGetString("blok_nama"),
                        ancak = safeGetString("ancak"),
                        nomor = safeGetString("nomor"),
                        tahun = safeGetString("tahun"),
                        luas_area = safeGetDouble("luas_area"),
                        jml_pokok = safeGetString("jml_pokok"),
                        jml_pokok_ha = safeGetString("jml_pokok_ha"),
                        lat = safeGetString("lat"),
                        lon = safeGetString("lon"),
                        update_date = safeGetString("update_date"),
                        status = safeGetString("status")
                    )

                    // Log the first few items to check conversion
                    if (resultList.size < 2) {
                        AppLogger.d("Converted item ${resultList.size + 1}: $model")
                    }

                    AppLogger.d("model $model")
                    resultList.add(model)
                } catch (e: Exception) {
                    AppLogger.e("Error converting item #${index + 1}")
                    AppLogger.e("Error JSON: ${element}")
                    AppLogger.e("Error details: ${e.message}")

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

    suspend fun getTPHsByIds(tphIds: List<Int>): List<TPHNewModel> {
        return repository.getTPHsByIds(tphIds)
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
            val dataList = try {
                when (dataset) {
                    AppUtils.DatasetNames.tph -> parseTPHJsonToList(jsonContent) as List<T>
                    else -> parseStructuredJsonToList(jsonContent, modelClass)
                }
            } catch (e: Exception) {
                updatedHasShownError = true
                results[dataset] = Resource.Error("Error parsing $dataset data: ${e.message}")
                return updatedHasShownError
            }
//
//            AppLogger.d("Parsed ${dataset} list size: ${dataList.size}")

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
                                company_abbr == null && wilayah == null && dept == null && dept_ppro == null && dept_abbr == null &&
                                divisi == null && divisi_ppro == null && divisi_abbr == null && divisi_nama == null && blok == null &&
                                blok_ppro == null && blok_kode == null && blok_nama == null && ancak == null && nomor == null &&
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
                    results[dataset] =
                        Resource.Error("Invalid data format for $dataset: All fields are null")
                    return updatedHasShownError
                }
            }


            try {
                AppLogger.d("Executing update operation for dataset: $dataset")
                updateOperation(dataList).join()
            } catch (e: Exception) {
                AppLogger.e("Error updating dataset $dataset: ${e.message}")
                updatedHasShownError = true
                results[dataset] =
                    Resource.Error("Failed to update dataset: $dataset - ${e.message}")
                return updatedHasShownError
            }

            if (statusFlow.value.isSuccess) {
                AppLogger.d("Update operation successful for dataset: $dataset")
                results[dataset] = Resource.Success(response)

                when (dataset) {
                    AppUtils.DatasetNames.tph -> prefManager.lastModifiedDatasetTPH =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.kemandoran -> prefManager.lastModifiedDatasetKemandoran =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.pemanen -> prefManager.lastModifiedDatasetPemanen =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.transporter -> prefManager.lastModifiedDatasetTransporter =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.blok -> prefManager.lastModifiedDatasetBlok =
                        lastModifiedTimestamp
                }
                prefManager!!.addDataset(dataset)
            } else {
                val error = statusFlow.value.exceptionOrNull()
                AppLogger.e("Database error for dataset $dataset: ${error?.message}")
                updatedHasShownError = true
                results[dataset] =
                    Resource.Error("Database error while processing $dataset: ${error?.message ?: "Unknown error"}")
                return updatedHasShownError
            }
        } catch (e: Exception) {
            AppLogger.e("$dataset processing error: ${e.message}")
            updatedHasShownError = true
            results[dataset] = Resource.Error("Error processing $dataset data: ${e.message}")
        }

        return updatedHasShownError
    }

    suspend fun getLatLonDivisi(
        idEstate: Int,
        idDivisi: Int,
    ): List<TPHNewModel> {
        return repository.getLatLonDivisi(idEstate, idDivisi)
    }

    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted

    private val _updateResult = MutableLiveData<UpdateResult>()
    val updateResultStatusUploadCMP: LiveData<UpdateResult> = _updateResult

    // Data class to hold result information
    data class UpdateResult(
        val success: Boolean,
        val message: String,
        val errorItems: List<ErrorItem> = emptyList()
    )

    data class ErrorItem(
        val fileName: String,
        val message: String
    )

    fun updateLocalUploadCMP(trackingIds: List<String>) {
        viewModelScope.launch {
            _isCompleted.value = false

            try {
                val response = repository.checkStatusUploadCMP(trackingIds)
                val responseBodyString = response.body()?.string() ?: "Empty Response"
                try {
                    val jsonResponse = Gson().fromJson(
                        responseBodyString,
                        FetchStatusCMPResponse::class.java
                    )

                    viewModelScope.launch(Dispatchers.IO) {
                        val panenIdsToUpdate = mutableListOf<Int>()
                        val espbIdsToUpdate = mutableListOf<Int>()
                        val status = mutableMapOf<String, Int>()  // Store status for batch updates

                        try {
                            val deferredUpdates = jsonResponse.data.map { item ->
                                AppLogger.d(item.toString())
                                async {
                                    try {
                                        // Get table_ids JSON from DB
                                        val tableIdsJson = uploadCMPDao.getTableIdsByTrackingId(item.id) ?: "{}"
                                        val tableIdsObj = Gson().fromJson(
                                            tableIdsJson,
                                            JsonObject::class.java
                                        )

                                        AppLogger.d(tableIdsObj.toString())

                                        tableIdsObj?.keySet()?.forEach { tableName ->
                                            val ids = tableIdsObj.getAsJsonArray(tableName).map { it.asInt }

                                            when (tableName) {
                                                AppUtils.DatabaseTables.PANEN -> panenIdsToUpdate.addAll(ids)
                                                AppUtils.DatabaseTables.ESPB -> espbIdsToUpdate.addAll(ids)
                                            }

                                            status[tableName] = item.status
                                        }

                                        uploadCMPDao.updateStatus(item.id, item.status)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error processing item ${item.id}: ${e.localizedMessage}")
                                    }
                                }
                            }

                            deferredUpdates.awaitAll()

                            try {
                                if (panenIdsToUpdate.isNotEmpty()) {
                                    panenDao.updateDataIsZippedPanen(
                                        panenIdsToUpdate,
                                        status[AppUtils.DatabaseTables.PANEN] ?: 0
                                    )
                                }
                                if (espbIdsToUpdate.isNotEmpty()) {
                                    espbDao.updateDataIsZippedESPB(
                                        espbIdsToUpdate,
                                        status[AppUtils.DatabaseTables.ESPB] ?: 0
                                    )
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error in batch updates: ${e.localizedMessage}")
                            }

                            withContext(Dispatchers.Main) {
                                val sortedList = jsonResponse.data
                                    .filter { it.status >= 4 }
                                    .sortedByDescending { it.tanggal_upload }

                                val errorItems = sortedList.map { item ->
                                    ErrorItem(
                                        fileName = item.nama_file,
                                        message = item.message ?: "Unknown error"
                                    )
                                }

                                val message = if (sortedList.isNotEmpty()) {
                                    "Terjadi kesalahan insert di server!"
                                } else {
                                    "Berhasil sinkronisasi data"
                                }

                                // Set the result
                                _updateResult.postValue(
                                    UpdateResult(
                                        success = sortedList.isEmpty(),
                                        message = message,
                                        errorItems = errorItems
                                    )
                                )

                                // Set completed status
                                _isCompleted.postValue(true)
                            }

                        } catch (e: Exception) {
                            AppLogger.e("Error in processing dataset updateSyncLocalData: ${e.localizedMessage}")
                            _updateResult.postValue(
                                UpdateResult(
                                    success = false,
                                    message = "Error processing data: ${e.localizedMessage}"
                                )
                            )
                            _isCompleted.postValue(true)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error parsing JSON response: ${e.localizedMessage}")
                    _updateResult.postValue(
                        UpdateResult(
                            success = false,
                            message = "Error parsing response: ${e.localizedMessage}"
                        )
                    )
                    _isCompleted.postValue(true)
                }
            } catch (e: Exception) {
                AppLogger.e("Network error: ${e.localizedMessage}")
                _updateResult.postValue(
                    UpdateResult(
                        success = false,
                        message = "Network error: ${e.localizedMessage}"
                    )
                )
                _isCompleted.postValue(true)
            }
        }
    }

    fun downloadDatasetsSilently(requests: List<DatasetRequest>): Deferred<Map<String, Boolean>> {
        return viewModelScope.async {
            val results = mutableMapOf<String, Boolean>() // Dataset name -> success
            AppLogger.d("=== Starting silent download for ${requests.size} datasets ===")

            for (request in requests) {
                try {
                    AppLogger.d("Silent download: Processing ${request.dataset}")
                    var modifiedRequest = request

                    // Handle datasets that need special lastModified treatment
                    val validDatasets = setOf(
                        AppUtils.DatasetNames.pemanen,
                        AppUtils.DatasetNames.kemandoran,
                        AppUtils.DatasetNames.tph,
                        AppUtils.DatasetNames.transporter,
                        AppUtils.DatasetNames.kendaraan,
                    )

                    if (request.dataset in validDatasets) {
                        val count = withContext(Dispatchers.IO) {
                            repository.getDatasetCount(request.dataset)
                        }

                        if (count == 0) {
                            // If no data, modify lastModified to null
                            modifiedRequest = request.copy(lastModified = null)
                            AppLogger.d("Silent download: Dataset ${request.dataset} has no records, setting lastModified to null")
                        } else {
                            AppLogger.d("Silent download: Dataset ${request.dataset} has $count records, keeping lastModified: ${modifiedRequest.lastModified}")
                        }
                    }

                    // Make the appropriate API call based on dataset type
                    AppLogger.d("Silent download: Making API call for ${request.dataset}")
                    val response = when (request.dataset) {
                        AppUtils.DatasetNames.mill ->
                            repository.downloadSmallDataset(request.regional ?: 0)

                        AppUtils.DatasetNames.updateSyncLocalData ->
                            repository.checkStatusUploadCMP(request.data!!)

                        AppUtils.DatasetNames.settingJSON ->
                            repository.downloadSettingJson(request.lastModified!!)

                        else ->
                            repository.downloadDataset(modifiedRequest)
                    }

                    // Process response based on HTTP status code
                    AppLogger.d("Silent download: Received response for ${request.dataset} with status code: ${response.code()}")

                    if (response.isSuccessful) {
                        val contentType = response.headers()["Content-Type"]
                        val lastModified = response.headers()["Last-Modified-Dataset"]
                        AppLogger.d("Silent download: Content type: $contentType, Last-Modified: $lastModified")

                        when {
                            // Handle ZIP files
                            contentType?.contains("application/zip") == true -> {
                                AppLogger.d("Silent download: Processing ZIP response for ${request.dataset}, size: ${response.body()?.contentLength() ?: 0} bytes")
                                val inputStream = response.body()?.byteStream()
                                if (inputStream != null) {
                                    try {
                                        // Create temp file
                                        val tempFile = File.createTempFile(
                                            "temp_",
                                            ".zip",
                                            getApplication<Application>().cacheDir
                                        )
                                        tempFile.outputStream().use { fileOut ->
                                            inputStream.copyTo(fileOut)
                                        }

                                        // Extract with password
                                        AppLogger.d("Silent download: Extracting ZIP file for ${request.dataset}")
                                        val zipFile = net.lingala.zip4j.ZipFile(tempFile)
                                        zipFile.setPassword(AppUtils.ZIP_PASSWORD.toCharArray())

                                        val extractDir = File(
                                            getApplication<Application>().cacheDir,
                                            "extracted_${System.currentTimeMillis()}"
                                        )
                                        zipFile.extractAll(extractDir.absolutePath)
                                        AppLogger.d("Silent download: Successfully extracted ZIP for ${request.dataset}")

                                        // Read output.json
                                        AppLogger.d("Silent download: Reading output.json for ${request.dataset}")
                                        val jsonFile = File(extractDir, "output.json")
                                        if (jsonFile.exists()) {
                                            val jsonContent = jsonFile.readText()
                                            AppLogger.d("Silent download: JSON content size for ${request.dataset}: ${jsonContent.length} chars")

                                            try {
                                                // Process the dataset silently using the proper parsing functions
                                                AppLogger.d("Silent download: Starting JSON parsing for ${request.dataset}")
                                                when (request.dataset) {
                                                    AppUtils.DatasetNames.tph -> {
                                                        val models = parseTPHJsonToList(jsonContent)
                                                        AppLogger.d("Silent download: Parsed ${models.size} TPH items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for TPH")
                                                            results[request.dataset] = false
                                                        } else {
                                                            // Check first item for all null values
                                                            val firstItem = models.firstOrNull()
                                                            if (firstItem != null) {
                                                                val allFieldsNull = firstItem.run {
                                                                    id == null && regional == null && company == null &&
                                                                            company_abbr == null && wilayah == null && dept == null &&
                                                                            dept_ppro == null && dept_abbr == null &&
                                                                            divisi == null && divisi_ppro == null && divisi_abbr == null &&
                                                                            divisi_nama == null && blok == null &&
                                                                            blok_ppro == null && blok_kode == null && blok_nama == null &&
                                                                            ancak == null && nomor == null &&
                                                                            tahun == null && luas_area == null && jml_pokok == null &&
                                                                            jml_pokok_ha == null && lat == null && lon == null &&
                                                                            update_date == null && status == null
                                                                }

                                                                if (allFieldsNull) {
                                                                    AppLogger.e("Silent download: Invalid data format - All fields are null in TPH")
                                                                    results[request.dataset] = false
                                                                } else {
                                                                    try {
                                                                        updateOrInsertTPH(models).join()
                                                                        AppLogger.d("Silent download: Successfully stored ${models.size} TPH items")
                                                                        prefManager!!.lastModifiedDatasetTPH = lastModified ?: ""
                                                                        AppLogger.d("Silent download: Updated lastModified for TPH to: ${lastModified ?: "[not set]"}")
                                                                        prefManager!!.addDataset(request.dataset)
                                                                        results[request.dataset] = true
                                                                    } catch (e: Exception) {
                                                                        AppLogger.e("Silent download: Error updating TPH dataset: ${e.message}")
                                                                        results[request.dataset] = false
                                                                    }
                                                                }
                                                            } else {
                                                                results[request.dataset] = false
                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.kemandoran -> {
                                                        val models = parseStructuredJsonToList(jsonContent, KemandoranModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Kemandoran items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Kemandoran")
                                                        } else {
                                                            try {
                                                                updateOrInsertKemandoran(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Kemandoran items")
                                                                prefManager!!.lastModifiedDatasetKemandoran = lastModified ?: ""
                                                                AppLogger.d("Silent download: Updated lastModified for Kemandoran to: ${lastModified ?: "[not set]"}")
                                                                prefManager!!.addDataset(request.dataset)

                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Kemandoran dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.pemanen -> {
                                                        val models = parseStructuredJsonToList(jsonContent, KaryawanModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Pemanen items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Pemanen")

                                                        } else {
                                                            try {
                                                                updateOrInsertKaryawan(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Pemanen items")
                                                                prefManager!!.lastModifiedDatasetPemanen = lastModified ?: ""
                                                                AppLogger.d("Silent download: Updated lastModified for Pemanen to: ${lastModified ?: "[not set]"}")
                                                                prefManager!!.addDataset(request.dataset)
                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Pemanen dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.mill -> {
                                                        val models = parseStructuredJsonToList(jsonContent, MillModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Mill items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Mill")

                                                        } else {
                                                            try {
                                                                updateOrInsertMill(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Mill items")
                                                                prefManager!!.addDataset(request.dataset)

                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Mill dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.blok -> {
                                                        val models = parseStructuredJsonToList(jsonContent, BlokModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Blok items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Blok")

                                                        } else {
                                                            try {
                                                                updateOrInsertBlok(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Blok items")
                                                                prefManager!!.lastModifiedDatasetBlok = lastModified ?: ""
                                                                AppLogger.d("Silent download: Updated lastModified for Blok to: ${lastModified ?: "[not set]"}")
                                                                prefManager!!.addDataset(request.dataset)

                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Blok dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.transporter -> {
                                                        val models = parseStructuredJsonToList(jsonContent, TransporterModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Transporter items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Transporter")

                                                        } else {
                                                            try {
                                                                InsertTransporter(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Transporter items")
                                                                prefManager!!.lastModifiedDatasetTransporter = lastModified ?: ""
                                                                AppLogger.d("Silent download: Updated lastModified for Transporter to: ${lastModified ?: "[not set]"}")
                                                                prefManager!!.addDataset(request.dataset)

                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Transporter dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    AppUtils.DatasetNames.kendaraan -> {
                                                        val models = parseStructuredJsonToList(jsonContent, KendaraanModel::class.java)
                                                        AppLogger.d("Silent download: Parsed ${models.size} Kendaraan items")

                                                        // Validate data
                                                        if (models.isEmpty()) {
                                                            AppLogger.e("Silent download: No valid data found for Kendaraan")

                                                        } else {
                                                            try {
                                                                InsertKendaraan(models).join()
                                                                AppLogger.d("Silent download: Successfully stored ${models.size} Kendaraan items")
                                                                prefManager!!.lastModifiedDatasetKendaraan = lastModified ?: ""
                                                                AppLogger.d("Silent download: Updated lastModified for Kendaraan to: ${lastModified ?: "[not set]"}")
                                                                prefManager!!.addDataset(request.dataset)

                                                            } catch (e: Exception) {
                                                                AppLogger.e("Silent download: Error updating Kendaraan dataset: ${e.message}")

                                                            }
                                                        }
                                                    }

                                                    else -> {
                                                        AppLogger.d("Silent download: No processing for ${request.dataset}")

                                                    }
                                                }

                                            } catch (e: Exception) {
                                                AppLogger.e("Silent download: Error processing ${request.dataset}: ${e.message}")
                                                AppLogger.e("Silent download: Error details", e.toString()) // Log stack trace

                                            }
                                        } else {
                                            AppLogger.e("Silent download: output.json not found for ${request.dataset}")

                                        }

                                        // Cleanup
                                        tempFile.delete()
                                        extractDir.deleteRecursively()

                                    } catch (e: Exception) {
                                        AppLogger.e("Silent download: Error extracting zip for ${request.dataset}: ${e.message}")
                                        AppLogger.e("Silent download: Error details", e.toString()) // Log stack trace

                                    }
                                } else {
                                    AppLogger.e("Silent download: Null input stream for ${request.dataset}")

                                }
                            }

                            // Handle JSON responses
                            contentType?.contains("application/json") == true -> {
                                val responseBodyString = response.body()?.string() ?: "Empty Response"
                                AppLogger.d("Silent download: JSON response size for ${request.dataset}: ${responseBodyString.length} chars")

                                if (responseBodyString.contains("\"success\":false") &&
                                    responseBodyString.contains("\"message\":\"Dataset is up to date\"")) {
                                    // Dataset is up to date
                                    AppLogger.d("Silent download: Dataset ${request.dataset} is up to date")

                                } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                                    try {
                                        AppLogger.d("Silent download: Processing setting.json response")
                                        val jsonObject = JSONObject(responseBodyString)
                                        val tphRadius = jsonObject.optInt("tph_radius", -1)
                                        val gpsAccuracy = jsonObject.optInt("gps_accuracy", -1)
                                        AppLogger.d("Silent download: Setting values - tphRadius: $tphRadius, gpsAccuracy: $gpsAccuracy")

                                        var isStored = false
                                        if (tphRadius != -1) {
                                            prefManager.radiusMinimum = tphRadius.toFloat()
                                            isStored = true
                                        }
                                        if (gpsAccuracy != -1) {
                                            prefManager.boundaryAccuracy = gpsAccuracy.toFloat()
                                            isStored = true
                                        }

                                        if (isStored) {
                                            prefManager!!.addDataset(request.dataset)
                                            prefManager!!.lastModifiedSettingJSON = lastModified ?: ""
                                            AppLogger.d("Silent download: Successfully updated settings, lastModified: ${lastModified ?: "[not set]"}")

                                        } else {
                                            AppLogger.d("Silent download: No settings were updated")

                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Silent download: Error parsing JSON for ${request.dataset}: ${e.message}")
                                        AppLogger.e("Silent download: Error details", e.toString()) // Log stack trace

                                    }
                                } else if (request.dataset == AppUtils.DatasetNames.mill) {
                                    try {
                                        AppLogger.d("Silent download: Processing mill JSON response")
                                        // For mill dataset, use the custom parse function
                                        fun <T> parseMillJsonToList(jsonContent: String, classType: Class<T>): List<T> {
                                            val gson = Gson()
                                            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
                                            val dataArray = jsonObject.getAsJsonArray("data")
                                            return gson.fromJson(
                                                dataArray,
                                                TypeToken.getParameterized(List::class.java, classType).type
                                            )
                                        }

                                        val millList = parseMillJsonToList(responseBodyString, MillModel::class.java)
                                        AppLogger.d("Silent download: Parsed ${millList.size} mill items")

                                        // Validate data
                                        if (millList.isEmpty()) {
                                            AppLogger.e("Silent download: No valid data found for Mill")
                                            results[request.dataset] = false
                                        } else {
                                            try {
                                                updateOrInsertMill(millList).join()
                                                AppLogger.d("Silent download: Successfully stored ${millList.size} Mill items")
                                                prefManager!!.addDataset(request.dataset)

                                            } catch (e: Exception) {
                                                AppLogger.e("Silent download: Error updating Mill dataset: ${e.message}")

                                            }
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Silent download: Error processing mill data: ${e.message}")
                                        AppLogger.e("Silent download: Error details", e.toString()) // Log stack trace

                                    }
                                } else if (request.dataset == AppUtils.DatasetNames.updateSyncLocalData) {
                                    try {
                                        AppLogger.d("Silent download: Processing updateSyncLocalData response")
                                        val jsonResponse = Gson().fromJson(
                                            responseBodyString,
                                            FetchStatusCMPResponse::class.java
                                        )

                                        // Validate data
                                        if (jsonResponse.data.isEmpty()) {
                                            AppLogger.e("Silent download: No valid data found for updateSyncLocalData")
                                            results[request.dataset] = false
                                        } else {
                                            val panenIdsToUpdate = mutableListOf<Int>()
                                            val espbIdsToUpdate = mutableListOf<Int>()
                                            val status = mutableMapOf<String, Int>()

                                            try {
                                                val deferredUpdates = jsonResponse.data.map { item ->
                                                    async {
                                                        try {
                                                            val tableIdsJson = uploadCMPDao.getTableIdsByTrackingId(item.id) ?: "{}"
                                                            val tableIdsObj = Gson().fromJson(tableIdsJson, JsonObject::class.java)

                                                            tableIdsObj?.keySet()?.forEach { tableName ->
                                                                val ids = tableIdsObj.getAsJsonArray(tableName).map { it.asInt }

                                                                when (tableName) {
                                                                    AppUtils.DatabaseTables.PANEN -> panenIdsToUpdate.addAll(ids)
                                                                    AppUtils.DatabaseTables.ESPB -> espbIdsToUpdate.addAll(ids)
                                                                }

                                                                status[tableName] = item.status
                                                            }

                                                            uploadCMPDao.updateStatus(item.id, item.status)
                                                        } catch (e: Exception) {
                                                            AppLogger.e("Silent download: Error processing item ${item.id}: ${e.message}")
                                                        }
                                                    }
                                                }

                                                deferredUpdates.awaitAll()

                                                try {
                                                    AppLogger.d("Silent download: Updating ${panenIdsToUpdate.size} PANEN records and ${espbIdsToUpdate.size} ESPB records")

                                                    if (panenIdsToUpdate.isNotEmpty()) {
                                                        panenDao.updateDataIsZippedPanen(
                                                            panenIdsToUpdate,
                                                            status[AppUtils.DatabaseTables.PANEN] ?: 0
                                                        )
                                                    }
                                                    if (espbIdsToUpdate.isNotEmpty()) {
                                                        espbDao.updateDataIsZippedESPB(
                                                            espbIdsToUpdate,
                                                            status[AppUtils.DatabaseTables.ESPB] ?: 0
                                                        )
                                                    }

                                                    AppLogger.d("Silent download: Successfully updated records for updateSyncLocalData")


                                                } catch (e: Exception) {
                                                    AppLogger.e("Silent download: Error in batch updates: ${e.message}")
                                                    AppLogger.e("Silent download: Error details", e.toString())

                                                }
                                            } catch (e: Exception) {
                                                AppLogger.e("Silent download: Error processing updateSyncLocalData items: ${e.message}")
                                                AppLogger.e("Silent download: Error details", e.toString())

                                            }
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Silent download: Error parsing JSON response: ${e.message}")
                                        AppLogger.e("Silent download: Error details", e.toString())

                                    }
                                } else {
                                    // Generic success for other JSON endpoints
                                    AppLogger.d("Silent download: Generic success for ${request.dataset}")

                                }
                            }

                            else -> {
                                AppLogger.d("Silent download: Unknown content type for ${request.dataset}: $contentType")

                            }
                        }
                    } else {
                        // Handle HTTP error
                        AppLogger.e("Silent download: HTTP error for ${request.dataset}: ${response.code()}")
                        if (response.errorBody() != null) {
                            try {
                                val errorContent = response.errorBody()!!.string()
                                AppLogger.e("Silent download: Error response body: $errorContent")
                            } catch (e: Exception) {
                                AppLogger.e("Silent download: Could not read error body")
                            }
                        }

                    }

                } catch (e: Exception) {
                    AppLogger.e("Silent download: Error for ${request.dataset}: ${e.message}")
                    AppLogger.e("Silent download: Error details", e.toString())

                }

                AppLogger.d("Silent download: Completed processing ${request.dataset} with result: ${results[request.dataset]}")
            }

            val successCount = results.values.count { it }
            AppLogger.d("=== Silent download completed: $successCount/${results.size} datasets processed successfully ===")
            AppLogger.d("Silent download results by dataset: ${results.entries.joinToString { "${it.key}: ${it.value}" }}")
            return@async results
        }
    }

    fun downloadMultipleDatasets(requests: List<DatasetRequest>) {
        viewModelScope.launch {
            val results = mutableMapOf<String, Resource<Response<ResponseBody>>>()

            requests.forEach { request ->
                var hasShownError = false  // Flag to track if error has been shown

                results[request.dataset] = Resource.Loading(0)
                _downloadStatuses.postValue(results.toMap())


                val validDatasets = setOf(
                    AppUtils.DatasetNames.pemanen,
                    AppUtils.DatasetNames.kemandoran,
                    AppUtils.DatasetNames.tph,
                    AppUtils.DatasetNames.transporter
                )
                val datasetName = request.dataset
                var modifiedRequest = request  // Create a mutable copy of the request

                if (datasetName in validDatasets) {
                    val count =
                        withContext(Dispatchers.IO) { repository.getDatasetCount(datasetName) }
                    withContext(Dispatchers.Main) {
                        if (count == 0) {
                            // If no data, modify lastModified to null
                            modifiedRequest = request.copy(lastModified = null)
                            Log.d(
                                "DownloadResponse",
                                "Dataset $datasetName has no records, setting lastModified to null."
                            )
                        }
                    }
                }

                try {
                    var response: Response<ResponseBody>? = null
                    if (request.dataset == AppUtils.DatasetNames.mill) {
                        response = repository.downloadSmallDataset(request.regional ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.updateSyncLocalData) {
                        response = repository.checkStatusUploadCMP(request.data!!)
                    } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                        response = repository.downloadSettingJson(request.lastModified!!)
                    } else {
                        response = repository.downloadDataset(modifiedRequest)
                    }


                    // Get headers
                    val contentType = response.headers()["Content-Type"]
                    val lastModified = response.headers()["Last-Modified-Dataset"]
                    val lastModifiedSettingsJson = response.headers()["Last-Modified-Settings"]

                    when (response.code()) {
                        200 -> {
                            if (contentType?.contains("application/zip") == true) {
                                val inputStream = response.body()?.byteStream()
                                if (inputStream != null) {
                                    Log.d(
                                        "DownloadResponse",
                                        "Received ZIP file, size: ${
                                            response.body()?.contentLength()
                                        } bytes"
                                    )

                                    try {
                                        // Update status to extracting
                                        results[request.dataset] =
                                            Resource.Extracting(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Create temp file
                                        val tempFile = File.createTempFile(
                                            "temp_",
                                            ".zip",
                                            getApplication<Application>().cacheDir
                                        )
                                        tempFile.outputStream().use { fileOut ->
                                            inputStream.copyTo(fileOut)
                                        }

                                        // Extract with password
                                        val zipFile = net.lingala.zip4j.ZipFile(tempFile)
                                        zipFile.setPassword(AppUtils.ZIP_PASSWORD.toCharArray())

                                        val extractDir = File(
                                            getApplication<Application>().cacheDir,
                                            "extracted_${System.currentTimeMillis()}"
                                        )
                                        zipFile.extractAll(extractDir.absolutePath)

                                        // Read output.json
                                        val jsonFile = File(extractDir, "output.json")
                                        if (jsonFile.exists()) {
                                            val jsonContent = jsonFile.readText()
                                            Log.d(
                                                "DownloadResponse",
                                                "Extracted JSON content: $jsonContent"
                                            )

                                            // Update status to storing
                                            results[request.dataset] =
                                                Resource.Storing(request.dataset)
                                            _downloadStatuses.postValue(results.toMap())


                                            AppLogger.d(request.dataset)
                                            AppLogger.d(lastModified.toString())
                                            try {
                                                when (request.dataset) {
                                                    AppUtils.DatasetNames.tph -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = TPHNewModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertTPH,
                                                            statusFlow = tphStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.kemandoran -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = KemandoranModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertKemandoran,
                                                            statusFlow = kemandoranStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.pemanen -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = KaryawanModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertKaryawan,
                                                            statusFlow = karyawanStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.mill -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = MillModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertMill,
                                                            statusFlow = millStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.blok -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = BlokModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertBlok,
                                                            statusFlow = blokStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.transporter -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = TransporterModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::InsertTransporter,
                                                            statusFlow = transporterStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    AppUtils.DatasetNames.kendaraan -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = KendaraanModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::InsertKendaraan,
                                                            statusFlow = kendaraanStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: ""
                                                        )

                                                    else -> {

                                                        results[request.dataset] =
                                                            Resource.Error("Failed to stored because no process storing dataset for  ${request.dataset}")
                                                        hasShownError = true
                                                    }

                                                }
                                                _downloadStatuses.postValue(results.toMap())
                                            } catch (e: Exception) {
                                                AppLogger.e("General processing error: ${e.message}")
                                                if (!hasShownError) {
                                                    results[request.dataset] =
                                                        Resource.Error("Processing error: ${e.message ?: "Unknown error"}")
                                                    hasShownError = true
                                                }
                                                _downloadStatuses.postValue(results.toMap())
                                            }
                                        }

                                        // Cleanup
                                        tempFile.delete()
                                        extractDir.deleteRecursively()

                                    } catch (e: Exception) {
                                        Log.e(
                                            "DownloadResponse",
                                            "Error extracting zip: ${e.message}"
                                        )
                                        if (!hasShownError) {
                                            results[request.dataset] =
                                                Resource.Error("Error extracting zip: ${e.message}")
                                            hasShownError = true
                                        }
                                    }
                                } else {
                                    Log.d("DownloadResponse", "ZIP response body is null")
                                    results[request.dataset] =
                                        Resource.Error("ZIP response body is null")
                                }
                            } else if (contentType?.contains("application/json") == true) {

                                Log.d("DownloadResponse", request.lastModified.toString())
                                val responseBodyString =
                                    response.body()?.string() ?: "Empty Response"
                                Log.d("DownloadResponse", "Received JSON: $responseBodyString")

                                if (responseBodyString.contains("\"success\":false") && responseBodyString.contains(
                                        "\"message\":\"Dataset is up to date\""
                                    )
                                ) {
                                    results[request.dataset] = Resource.UpToDate(request.dataset)
                                } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {

                                    if (responseBodyString.isBlank()) {
                                        Log.e("DownloadResponse", "Received empty JSON response")
                                        results[request.dataset] =
                                            Resource.Error("Empty JSON response")
                                        _downloadStatuses.postValue(results.toMap())
                                    }

                                    try {
                                        val jsonObject = JSONObject(responseBodyString)

                                        val tphRadius = jsonObject.optInt(
                                            "tph_radius",
                                            -1
                                        ) // Default -1 if not found
                                        val gpsAccuracy = jsonObject.optInt(
                                            "gps_accuracy",
                                            -1
                                        ) // Default -1 if not found

                                        var isStored = false

                                        if (tphRadius != -1) {
                                            prefManager.radiusMinimum = tphRadius.toFloat()
                                            isStored = true
                                        }
                                        if (gpsAccuracy != -1) {
                                            prefManager.boundaryAccuracy = gpsAccuracy.toFloat()
                                            isStored = true
                                        }

                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // ✅ Run only if data was stored
                                        if (isStored) {
                                            prefManager!!.addDataset(request.dataset)
                                            results[request.dataset] = Resource.Success(response)
                                            _downloadStatuses.postValue(results.toMap())
                                        }

                                    } catch (e: JSONException) {
                                        Log.e(
                                            "DownloadResponse",
                                            "Error parsing JSON: ${e.message}",
                                            e
                                        )
                                        results[request.dataset] =
                                            Resource.Error("Error parsing JSON: ${e.message}")
                                        _downloadStatuses.postValue(results.toMap())
                                    }

                                } else if (request.dataset == AppUtils.DatasetNames.mill) {

                                    try {

                                        fun <T> parseMillJsonToList(
                                            jsonContent: String,
                                            classType: Class<T>
                                        ): List<T> {
                                            val gson = Gson()
                                            val jsonObject =
                                                gson.fromJson(jsonContent, JsonObject::class.java)
                                            val dataArray = jsonObject.getAsJsonArray("data")
                                            return gson.fromJson(
                                                dataArray,
                                                TypeToken.getParameterized(
                                                    List::class.java,
                                                    classType
                                                ).type
                                            )
                                        }

                                        // Use the new parse function
                                        val millList = parseMillJsonToList(
                                            responseBodyString,
                                            MillModel::class.java
                                        )

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
                                        Log.e(
                                            "DownloadResponse",
                                            "Error processing mill data: ${e.message}"
                                        )
                                        if (!hasShownError) {
                                            results[request.dataset] =
                                                Resource.Error("Error processing mill data: ${e.message}")
                                            hasShownError = true
                                        }
                                        _downloadStatuses.postValue(results.toMap())
                                    }
                                } else if (request.dataset == AppUtils.DatasetNames.updateSyncLocalData) {
                                    results[request.dataset] = Resource.Loading(0)

                                    try {
                                        val jsonResponse = Gson().fromJson(
                                            responseBodyString,
                                            FetchStatusCMPResponse::class.java
                                        )

                                        viewModelScope.launch(Dispatchers.IO) {
                                            val panenIdsToUpdate = mutableListOf<Int>()
                                            val espbIdsToUpdate = mutableListOf<Int>()
                                            val status =
                                                mutableMapOf<String, Int>()  // Store status for batch updates

                                            try {
                                                val deferredUpdates =
                                                    jsonResponse.data.map { item ->

                                                        AppLogger.d(item.toString())
                                                        async {
                                                            try {
                                                                // Get table_ids JSON from DB
                                                                val tableIdsJson =
                                                                    uploadCMPDao.getTableIdsByTrackingId(
                                                                        item.id
                                                                    ) ?: "{}"
                                                                val tableIdsObj = Gson().fromJson(
                                                                    tableIdsJson,
                                                                    JsonObject::class.java
                                                                )

                                                                AppLogger.d(tableIdsObj.toString())

                                                                tableIdsObj?.keySet()
                                                                    ?.forEach { tableName ->
                                                                        val ids =
                                                                            tableIdsObj.getAsJsonArray(
                                                                                tableName
                                                                            ).map { it.asInt }

                                                                        when (tableName) {
                                                                            AppUtils.DatabaseTables.PANEN -> panenIdsToUpdate.addAll(
                                                                                ids
                                                                            )

                                                                            AppUtils.DatabaseTables.ESPB -> espbIdsToUpdate.addAll(
                                                                                ids
                                                                            )
                                                                        }

                                                                        status[tableName] =
                                                                            item.status
                                                                    }

                                                                uploadCMPDao.updateStatus(
                                                                    item.id,
                                                                    item.status
                                                                )
                                                            } catch (e: Exception) {
                                                                AppLogger.e("Error processing item ${item.id}: ${e.localizedMessage}")
                                                            }
                                                        }
                                                    }

                                                deferredUpdates.awaitAll()

                                                try {
                                                    if (panenIdsToUpdate.isNotEmpty()) {
                                                        panenDao.updateDataIsZippedPanen(
                                                            panenIdsToUpdate,
                                                            status[AppUtils.DatabaseTables.PANEN]
                                                                ?: 0
                                                        )
                                                    }
                                                    if (espbIdsToUpdate.isNotEmpty()) {
                                                        espbDao.updateDataIsZippedESPB(
                                                            espbIdsToUpdate,
                                                            status[AppUtils.DatabaseTables.ESPB]
                                                                ?: 0
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    AppLogger.e("Error in batch updates: ${e.localizedMessage}")
                                                }

                                                withContext(Dispatchers.Main) {
                                                    val sortedList = jsonResponse.data
                                                        .filter { it.status >= 4 }
                                                        .sortedByDescending { it.tanggal_upload }
                                                    val message = if (sortedList.isNotEmpty()) {
                                                        "Terjadi kesalahan insert di server!\n\n" + sortedList.joinToString(
                                                            "\n"
                                                        ) { item ->
                                                            "• ${item.nama_file} (${item.message})"
                                                        }
                                                    } else {
                                                        "Berhasil sinkronisasi data"
                                                    }
                                                    results[request.dataset] =
                                                        Resource.Success(response, message)
                                                    _downloadStatuses.postValue(results.toMap())
                                                }

                                            } catch (e: Exception) {
                                                AppLogger.e("Error in processing dataset updateSyncLocalData: ${e.localizedMessage}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error parsing JSON response: ${e.localizedMessage}")
                                    }
                                } else {
                                    results[request.dataset] = Resource.Success(response)
                                }
                            } else {
                                Log.d("DownloadResponse", "Unknown response type: $contentType")
                                results[request.dataset] =
                                    Resource.Error("Unknown response type: $contentType")
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
                                results[request.dataset] =
                                    Resource.Error("Download failed", response)
                            }
                        }

                        else -> {
                            val errorBody = response?.errorBody()?.string() ?: "Unknown error"

                            // Extract the message from JSON safely
                            val extractedMessage = try {
                                val jsonObject = JSONObject(errorBody)
                                jsonObject.optString(
                                    "message",
                                    "Unknown error"
                                ) // Get "message" key or default
                            } catch (e: Exception) {
                                errorBody // If JSON parsing fails, return the raw error
                            }

                            val errorMessage =
                                "Download failed with status code: ${response?.code()} - $extractedMessage"

                            Log.e("DownloadError", errorMessage) // Log the extracted message

                            if (!hasShownError) {
                                results[request.dataset] = Resource.Error(errorMessage, response)
                                hasShownError = true
                            } else {
                                results[request.dataset] =
                                    Resource.Error("Download failed", response)
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
            return gson.fromJson(
                transformedArray,
                TypeToken.getParameterized(List::class.java, classType).type
            )
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