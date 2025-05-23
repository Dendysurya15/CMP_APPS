package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.markertph.data.model.JenisTPHModel
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
import com.cbi.mobile_plantation.data.database.DepartmentInfo
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.EstateModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.repository.RestanRepository
import com.cbi.mobile_plantation.ui.adapter.UploadCMPItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@Suppress("NAME_SHADOWING")
class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)
    private val restanRepository: RestanRepository = RestanRepository(application)
    private val prefManager = PrefManager(application)

    private val database = AppDatabase.getDatabase(application)
    private val uploadCMPDao = database.uploadCMPDao()
    private val espbDao = database.espbDao()
    private val panenDao = database.panenDao()
    private val absensiDao = database.absensiDao()
    private val blokDao = database.blokDao()
    private val hektarPanenDao = database.hektarPanenDao()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _downloadStatuses = MutableLiveData<Map<String, Resource<Response<ResponseBody>>>>()
    val downloadStatuses: LiveData<Map<String, Resource<Response<ResponseBody>>>> =
        _downloadStatuses

    private val _kemandoranStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kemandoranStatus: StateFlow<Result<Boolean>> = _kemandoranStatus.asStateFlow()

    private val _karyawanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val karyawanStatus: StateFlow<Result<Boolean>> = _karyawanStatus.asStateFlow()

    private val _jenisTPHStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val jenisTPHStatus: StateFlow<Result<Boolean>> = _jenisTPHStatus.asStateFlow()

    private val _blokStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val blokStatus: StateFlow<Result<Boolean>> = _blokStatus.asStateFlow()

    private val _millStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val millStatus: StateFlow<Result<Boolean>> = _millStatus.asStateFlow()


    private val _allEstatesList = MutableLiveData<List<EstateModel>>()
    val allEstatesList: LiveData<List<EstateModel>> = _allEstatesList

    private val _estateStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val estateStatus: StateFlow<Result<Boolean>> = _estateStatus.asStateFlow()


    private val _afdelingStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val afdelingStatus: StateFlow<Result<Boolean>> = _afdelingStatus.asStateFlow()

    private val _transporterStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val transporterStatus: StateFlow<Result<Boolean>> = _transporterStatus.asStateFlow()

    private val _kendaraanStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val kendaraanStatus: StateFlow<Result<Boolean>> = _kendaraanStatus.asStateFlow()

    private val _tphStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val tphStatus: StateFlow<Result<Boolean>> = _tphStatus.asStateFlow()

    private val _fetchStatusUploadCMPLiveData = MutableLiveData<List<FetchResponseItem>>()
    val fetchStatusUploadCMPLiveData: LiveData<List<FetchResponseItem>> =
        _fetchStatusUploadCMPLiveData

    private val _distinctDeptInfoList = MutableLiveData<List<DepartmentInfo>>()
    val distinctDeptInfoList: LiveData<List<DepartmentInfo>> = _distinctDeptInfoList

    private val _distinctDeptInfoListCopy = MutableLiveData<List<DepartmentInfo>>()
    val distinctDeptInfoListCopy: LiveData<List<DepartmentInfo>> = _distinctDeptInfoListCopy

    private val _restanPreviewData = MutableLiveData<String>()
    val restanPreviewData: LiveData<String> = _restanPreviewData


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

    fun getDistinctMasterDeptInfo() {
        viewModelScope.launch {
            repository.getDistinctDeptInfo()
                .onSuccess { deptInfoList ->
                    _distinctDeptInfoList.postValue(deptInfoList)
                }
                .onFailure { exception ->
                    // Handle error if needed
                    Log.e("DatasetViewModel", "Error fetching dept info", exception)
                }
        }
    }

    fun getDistinctMasterDeptInfoCopy() {
        viewModelScope.launch {
            repository.getDistinctDeptInfo()
                .onSuccess { deptInfoList ->
                    _distinctDeptInfoListCopy.postValue(deptInfoList)
                }
                .onFailure { exception ->
                    // Handle error if needed
                    Log.e("DatasetViewModel", "Error fetching dept info", exception)
                }
        }
    }

    fun updateOrInsertAfdeling(afdelings: List<AfdelingModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertAfdeling(afdelings)
                _afdelingStatus.value = Result.success(true)
            } catch (e: Exception) {
                _afdelingStatus.value = Result.failure(e)
            }
        }

    suspend fun getBlokByDivisiAndDept(divisiId: String, deptId: String): KemandoranModel? {
        return repository.getBlokByDivisiAndDept(divisiId, deptId)
    }


    fun updateOrInsertEstate(estate: List<EstateModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertEstate(estate)
                _estateStatus.value = Result.success(true)
            } catch (e: Exception) {
                _estateStatus.value = Result.failure(e)
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

    fun updateOrInsertJenisTPH(jenisTPH: List<JenisTPHModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertJenisTPH(jenisTPH)
                _jenisTPHStatus.value = Result.success(true)
            } catch (e: Exception) {
                _jenisTPHStatus.value = Result.failure(e)
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



    fun updateOrInsertTPH(tph: List<TPHNewModel>, isAsistensi: Boolean = false) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppLogger.d("ViewModel: Starting updateOrInsertTPH with ${tph.size} records, asistensi: $isAsistensi")

                if (isAsistensi) {
                    // Just insert the new records without deleting existing ones
                    repository.insertTPH(tph)
                } else {
                    // Original behavior: delete all and then insert
                    repository.updateOrInsertTPH(tph)
                }

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

    suspend fun getListAfdeling(
        idEstate: String,
    ): List<AfdelingModel> {
        return repository.getListAfdeling(idEstate)
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


    fun getAllEstates() {
        viewModelScope.launch {
            repository.getAllEstates()
                .onSuccess { estateList ->
                    _allEstatesList.postValue(estateList)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message ?: "Failed to load estate data")
                }
        }
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
//                    AppLogger.d("Processing item #${index + 1}, mapped JSON: $mappedObj")

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
                        regional = safeGetString("regional") ?: safeGetInt("regional")?.toString()
                        ?: "0",
                        company = safeGetInt("company"),
                        company_abbr = safeGetString("company_abbr"),
                        company_nama = safeGetString("company_nama"),
                        wilayah = safeGetString("wilayah"),
                        dept = safeGetInt("dept"),
                        dept_ppro = safeGetInt("dept_ppro"),
                        dept_abbr = safeGetString("dept_abbr"),
                        dept_nama = safeGetString("dept_nama"),
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
                        status = safeGetString("status"),
                        jenis_tph_id = safeGetString("jenis_tph_id"),
                        limit_tph = safeGetString("limit_tph"),
                    )

                    // Log the first few items to check conversion
                    if (resultList.size < 2) {
                        AppLogger.d("Converted item ${resultList.size + 1}: $model")
                    }

//                    AppLogger.d("model $model")
                    resultList.add(model)
                } catch (e: Exception) {
                    AppLogger.e("Error converting item #${index + 1}")
                    AppLogger.e("Error JSON: ${element}")
                    AppLogger.e("Error details: ${e.message}")

                    // Continue with next item
                }
            }

//            AppLogger.d("Successfully converted ${resultList.size} items")
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
        isDownloadMasterTPHAsistensi: Boolean = false,
        estateAbbr: String? = null  // Add estateAbbr parameter
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

                // Store last modified timestamp
                when (dataset) {
                    AppUtils.DatasetNames.tph -> {
                        if (isDownloadMasterTPHAsistensi) {
                            // If it's an asistensi download, only store the estate-specific timestamp
                            if (!estateAbbr.isNullOrEmpty() && lastModifiedTimestamp.isNotEmpty()) {
                                AppLogger.d("Storing last modified timestamp for estate $estateAbbr: $lastModifiedTimestamp")
                                prefManager.setEstateLastModified(estateAbbr, lastModifiedTimestamp)
                            }
                        } else {
                            // For regular downloads, store the global timestamp

                            AppLogger.d("gas bro")
                            prefManager.lastModifiedDatasetTPH = lastModifiedTimestamp
                        }
                    }

                    AppUtils.DatasetNames.kemandoran -> prefManager.lastModifiedDatasetKemandoran =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.pemanen -> prefManager.lastModifiedDatasetPemanen =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.transporter -> prefManager.lastModifiedDatasetTransporter =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.blok -> prefManager.lastModifiedDatasetBlok =
                        lastModifiedTimestamp

                    AppUtils.DatasetNames.jenisTPH -> prefManager.lastModifiedDatasetJenisTPH =
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

    data class ErrorItem(
        val fileName: String,
        val message: String
    )

    fun updateLocalUploadCMP(uploadData: List<Pair<String, String>>, jabatan :String): Deferred<Boolean> {
        val result = CompletableDeferred<Boolean>()

        viewModelScope.launch {
            try {
                AppLogger.d("=== Starting updateLocalUploadCMP ===")
                AppLogger.d("Processing ${uploadData.size} items")
                uploadData.forEachIndexed { index, (id, file) ->
                    AppLogger.d("Item $index: trackingId=$id, filename=$file")
                }

                val errorItems = mutableListOf<ErrorItem>()

                // Process each item sequentially
                uploadData.forEachIndexed { index, (trackingId, filename) ->
                    AppLogger.d("\n--- Processing item ${index + 1}/${uploadData.size} ---")
                    AppLogger.d("TrackingId: $trackingId")

                    try {
                        // Get status for this tracking ID
                        AppLogger.d("Requesting status for trackingId: $trackingId")
                        val response = repository.checkStatusUploadCMP(trackingId)

                        AppLogger.d("Response received - isSuccessful: ${response.isSuccessful}")
                        AppLogger.d("Response code: ${response.code()}")

                        if (response.isSuccessful && response.body() != null) {
                            val statusResponse = response.body()!!

                            AppLogger.d("Response body success: ${statusResponse.success}")
                            AppLogger.d("Data count: ${statusResponse.data.size}")
                            AppLogger.d("Full response: ${statusResponse}")

                            // Since we're not checking filenames anymore, just use the first data item
                            // Or implement a different logic to choose the right status data
                            val statusData = statusResponse.data.firstOrNull()

                            AppLogger.d("\nStatus data result:")
                            AppLogger.d("Found: ${statusData != null}")
                            if (statusData != null) {
                                AppLogger.d("Status: ${statusData.status}")
                                AppLogger.d("Message: ${statusData.message}")
                                AppLogger.d("StatusText: ${statusData.statusText}")
                            }

                            if (statusData != null) {
                                // Get the status code for database update
                                val statusCode = statusData.status
                                AppLogger.d("Updating database - trackingId=$trackingId, statusCode=$statusCode")

                                // Update status in the database
                                uploadCMPDao.updateStatus(trackingId, statusCode)
                                AppLogger.d("Database update completed successfully")

                                // Get table_ids from database
                                val tableIdsJson = uploadCMPDao.getTableIdsByTrackingId(trackingId)
                                AppLogger.d("Retrieved table_ids JSON: $tableIdsJson")

                                if (tableIdsJson != null) {
                                    try {
                                        // Parse JSON using Gson or JSONObject
                                        val json = JSONObject(tableIdsJson)
                                        val keys = json.keys()

                                        while (keys.hasNext()) {
                                            val tableName = keys.next()
                                            val ids = json.getJSONArray(tableName)
                                            val idList = mutableListOf<Int>()

                                            for (i in 0 until ids.length()) {
                                                idList.add(ids.getInt(i))
                                            }

                                            AppLogger.d("Updating $tableName with ids: $idList, status: $statusCode")

                                            when (tableName) {
                                                AppUtils.DatabaseTables.PANEN -> {
                                                    panenDao.updateStatusUploadPanen(idList, statusCode)
                                                    AppLogger.d("Updated panen_table successfully")
                                                }
                                                AppUtils.DatabaseTables.ESPB -> {
                                                    // Conditional update based on jabatan/role
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.Mandor1,
                                                        AppUtils.ListFeatureByRoleUser.Asisten -> {
                                                            espbDao.updateStatusUploadEspbCmpSp(idList, statusCode)
                                                            AppLogger.d("Updated espb_table status_upload_cmp_sp successfully")
                                                        }
                                                        AppUtils.ListFeatureByRoleUser.KeraniTimbang -> {
                                                            espbDao.updateStatusUploadEspbCmpWb(idList, statusCode)
                                                            AppLogger.d("Updated espb_table status_upload_cmp_wb successfully")
                                                        }
                                                    }
                                                }
                                                AppUtils.DatabaseTables.HEKTAR_PANEN -> {
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.MandorPanen -> {
                                                            hektarPanenDao.updateStatusUploadHektarPanen(idList, statusCode)
                                                            AppLogger.d("Updated panen_table successfully")
                                                        }
                                                    }
                                                }
                                                AppUtils.DatabaseTables.ABSENSI -> {
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.MandorPanen -> {
                                                            absensiDao.updateStatusUploadAbsensiPanen(idList, statusCode)
                                                            AppLogger.d("Updated panen_table successfully")
                                                        }
                                                    }

                                                }
                                                else -> {
                                                    AppLogger.w("Unknown table name: $tableName")
                                                }
                                            }
                                        }
                                    } catch (jsonException: Exception) {
                                        AppLogger.e("Error parsing table_ids JSON: ${jsonException.message}")
                                        errorItems.add(
                                            ErrorItem(
                                                fileName = "",  // No filename needed
                                                message = "Failed to parse table_ids JSON"
                                            )
                                        )
                                    }
                                } else {
                                    AppLogger.w("No table_ids found for trackingId: $trackingId")
                                }

                                // Check if this was an error status
                                AppLogger.d("Checking if status code ($statusCode) >= 4")
                                if (statusCode >= 4) {
                                    AppLogger.d("Error status detected - adding to error items")
                                    errorItems.add(
                                        ErrorItem(
                                            fileName = "",  // No filename needed
                                            message = statusData.message
                                        )
                                    )
                                    AppLogger.d("Error item added: message=${statusData.message}")
                                } else {
                                    AppLogger.d("Status code is OK (< 4)")
                                }
                            } else {
                                // No status data found
                                AppLogger.e("ERROR: No status data found for trackingId: $trackingId")
                                errorItems.add(
                                    ErrorItem(
                                        fileName = "",  // No filename needed
                                        message = "No status data found for this tracking ID"
                                    )
                                )
                            }
                        } else {
                            // Handle unsuccessful response
                            AppLogger.e("ERROR: Failed response for $trackingId")
                            AppLogger.e("Response code: ${response.code()}")
                            AppLogger.e("Error body: ${response.errorBody()?.string()}")
                            errorItems.add(
                                ErrorItem(
                                    fileName = "",  // No filename needed
                                    message = "Server error: ${response.code()}"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        AppLogger.e("EXCEPTION: Network error for $trackingId")
                        AppLogger.e("Exception type: ${e.javaClass.simpleName}")
                        AppLogger.e("Exception message: ${e.localizedMessage}")
                        AppLogger.e("Stack trace: ${e.stackTrace.take(5).joinToString("\n")}")
                        errorItems.add(
                            ErrorItem(
                                fileName = "",  // No filename needed
                                message = "Network error: ${e.localizedMessage}"
                            )
                        )
                    }

                    AppLogger.d("--- Completed item ${index + 1}/${uploadData.size} ---\n")
                }

                // All processing complete
                AppLogger.d("\n=== All processing complete ===")
                AppLogger.d("Total error items: ${errorItems.size}")
                errorItems.forEachIndexed { index, error ->
                    AppLogger.d("Error $index: fileName=${error.fileName}, message=${error.message}")
                }

                withContext(Dispatchers.Main) {
                    val message = if (errorItems.isNotEmpty()) {
                        "Terjadi kesalahan insert di server!"
                    } else {
                        "Berhasil sinkronisasi data"
                    }

                    AppLogger.d("Final message to display: $message")
                    AppLogger.d("Completing with result: ${errorItems.isEmpty()}")

                    // Complete with success if no errors, or with false if there were errors
                    result.complete(errorItems.isEmpty())
                }

            } catch (e: Exception) {
                AppLogger.e("=== FATAL ERROR in updateLocalUploadCMP ===")
                AppLogger.e("Exception type: ${e.javaClass.simpleName}")
                AppLogger.e("Exception message: ${e.localizedMessage}")
                AppLogger.e("Full stack trace:")
                e.printStackTrace()
                result.complete(false)
            }
        }

        return result
    }

    private val _totalCount = MutableLiveData<Int>(0)
    val totalCount: LiveData<Int> get() = _totalCount


    private val _itemProgressMap = MutableLiveData<Map<Int, Int>>(mutableMapOf())
    val itemProgressMap: LiveData<Map<Int, Int>> get() = _itemProgressMap

    // Map to track status for each upload item by ID
    private val _itemStatusMap = MutableLiveData<Map<Int, String>>(mutableMapOf())
    val itemStatusMap: LiveData<Map<Int, String>> get() = _itemStatusMap

    // Map to track errors for each upload item by ID
    private val _itemErrorMap = MutableLiveData<Map<Int, String?>>(mutableMapOf())
    val itemErrorMap: LiveData<Map<Int, String?>> get() = _itemErrorMap


    private val _completedCount = MutableLiveData<Int>(0)
    val completedCount: LiveData<Int> get() = _completedCount

    private fun incrementCompletedCount() {
        val current = _completedCount.value ?: 0
        _completedCount.value = current + 1
    }

    private val _processingComplete = MutableLiveData<Boolean>(false)
    val processingComplete: LiveData<Boolean> = _processingComplete

    fun TPHNewModel.isCompletelyNull(): Boolean {
        return id == null && regional == null && company == null &&
                company_abbr == null && wilayah == null && dept == null && dept_ppro == null && dept_abbr == null &&
                divisi == null && divisi_ppro == null && divisi_abbr == null && divisi_nama == null && blok == null &&
                blok_ppro == null && blok_kode == null && blok_nama == null && ancak == null && nomor == null &&
                tahun == null && luas_area == null && jml_pokok == null &&
                jml_pokok_ha == null && lat == null && lon == null &&
                update_date == null && status == null
    }

    fun downloadDataset(requests: List<DatasetRequest>, downloadItems: List<UploadCMPItem>,  isDownloadDataset: Boolean = true) {
        AppLogger.d("Starting download for ${requests.size} datasets")
        val mutableRequests = requests.toMutableList()
        // Initialize counts
        _totalCount.value = requests.size
        _completedCount.value = 0

        // Create mutable maps for tracking progress and status
        val progressMap = mutableMapOf<Int, Int>()
        val statusMap = mutableMapOf<Int, String>()
        val errorMap = mutableMapOf<Int, String?>()

        // Set initial status for all items
        downloadItems.forEachIndexed { index, item ->
            statusMap[item.id] = AppUtils.UploadStatusUtils.WAITING
            progressMap[item.id] = 0
            errorMap[item.id] = null
        }

        // Update LiveData with initial values
        _itemStatusMap.value = statusMap.toMap()
        _itemProgressMap.value = progressMap.toMap()
        _itemErrorMap.value = errorMap.toMap()

        // Process each request sequentially
        viewModelScope.launch {
            for (index in requests.indices) {
                var request = mutableRequests[index]
                val itemId = downloadItems[index].id

                AppLogger.d("Processing request $index: estate=${request.estateAbbr}, dataset=${request.dataset}")

                // Use DOWNLOADING status
                statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                _itemStatusMap.value = statusMap.toMap()

                if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                    // Check if settings are missing in prefManager
                    val radiusValue = prefManager.radiusMinimum
                    val accuracyValue = prefManager.boundaryAccuracy

                    if (radiusValue == 0f || accuracyValue == 0f) {
                        // Settings are missing, force re-download by setting lastModified to null
                        AppLogger.d("Settings values missing in prefManager, forcing re-download")
                        request = request.copy(lastModified = null)
                        mutableRequests[index] = request
                    }
                }

                // Check for sinkronisasiRestan with error data - skip API call if error detected
                if (request.dataset == AppUtils.DatasetNames.sinkronisasiRestan) {
                    val downloadItem = downloadItems[index]
                    val itemData = downloadItem.data

                    AppLogger.d("downloadItem.data: $itemData")

                    if (itemData.contains("Error", ignoreCase = true)) {
                        AppLogger.d("Skipping sinkronisasiRestan API call due to error in data: $itemData")

                        // Set progress to 100% and status to UP_TO_DATE
                        progressMap[itemId] = 100
                        statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                        errorMap[itemId] = itemData

                        _itemProgressMap.postValue(progressMap.toMap())
                        _itemStatusMap.postValue(statusMap.toMap())
                        _itemErrorMap.postValue(errorMap.toMap())

                        incrementCompletedCount()
                        continue // Skip to next item
                    }
                }

                try {
                    // Handle special case for valid datasets
                    val validDatasets = setOf(
                        AppUtils.DatasetNames.pemanen,
                        AppUtils.DatasetNames.kemandoran,
                        AppUtils.DatasetNames.tph,
                        AppUtils.DatasetNames.transporter,
                        AppUtils.DatasetNames.kendaraan,
                        AppUtils.DatasetNames.jenisTPH,
                    )

                    var modifiedRequest = request
                    if (request.dataset in validDatasets) {
                        val deptId = if (request.dataset == AppUtils.DatasetNames.tph) request.estate else null

                        val count = withContext(Dispatchers.IO) {
                            repository.getDatasetCount(request.dataset, deptId)
                        }

                        if (count == 0) {
                            modifiedRequest = request.copy(lastModified = null)
                            if (deptId != null) {
                                AppLogger.d("Dataset ${request.dataset} has no records for department ID $deptId, setting lastModified to null")
                            } else {
                                AppLogger.d("Dataset ${request.dataset} has no records, setting lastModified to null")
                            }
                        }
                    }

                    AppLogger.d("Downloading dataset for ${modifiedRequest.estateAbbr}/ ${modifiedRequest.dataset}")


                    var response: Response<ResponseBody>? = null
                    if (request.dataset == AppUtils.DatasetNames.mill) {
                        response = repository.downloadSmallDataset(request.regional ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.estate) {
                        response = repository.downloadListEstate(request.regional ?: 0)
                    }else if(request.dataset == AppUtils.DatasetNames.sinkronisasiRestan){
                        response = restanRepository.getDataRestan(request.estate!!, request.afdeling!!)
                    } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                        response = repository.downloadSettingJson(request.lastModified ?: "")
                    } else {
                        response = repository.downloadDataset(modifiedRequest)
                    }

                    if (response.isSuccessful && response.code() == 200) {
                        val contentType = response.headers()["Content-Type"]
                        val lastModified = response.headers()["Last-Modified-Dataset"]
                        val lastModifiedSettingsJson = response.headers()["Last-Modified-Settings"]

                        AppLogger.d("lastModifiedSettingsJson $lastModifiedSettingsJson")


                        AppLogger.d(contentType.toString())
                        if (contentType?.contains("application/zip") == true) {
                            // Create temp file
                            val tempFile = withContext(Dispatchers.IO) {
                                val inputStream = response.body()?.byteStream()
                                if (inputStream == null) {
                                    return@withContext null
                                }

                                val tempFile = File.createTempFile(
                                    "temp_", ".zip", getApplication<Application>().cacheDir
                                )

                                // Download with progress updates
                                val contentLength = response.body()?.contentLength() ?: 0L
                                var downloadedBytes = 0L

                                tempFile.outputStream().use { fileOut ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int

                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        fileOut.write(buffer, 0, bytesRead)
                                        downloadedBytes += bytesRead

                                        // Update progress
                                        if (contentLength > 0) {
                                            val progress = (downloadedBytes * 100 / contentLength).toInt()
                                            progressMap[itemId] = progress
                                            _itemProgressMap.postValue(progressMap.toMap())
                                        }
                                    }
                                }

                                inputStream.close()
                                tempFile
                            }

                            if (tempFile == null) {
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "Failed to download file"
                                _itemStatusMap.postValue(statusMap.toMap())
                                _itemErrorMap.postValue(errorMap.toMap())
                                incrementCompletedCount()
                                continue
                            }

                            // Extract zip file
                            val extractDir = withContext(Dispatchers.IO) {
                                val zipFile = net.lingala.zip4j.ZipFile(tempFile)
                                zipFile.setPassword(AppUtils.ZIP_PASSWORD.toCharArray())

                                val dir = File(
                                    getApplication<Application>().cacheDir,
                                    "extracted_${System.currentTimeMillis()}"
                                )
                                zipFile.extractAll(dir.absolutePath)
                                dir
                            }

                            // Process output.json
                            val jsonFile = File(extractDir, "output.json")
                            if (jsonFile.exists()) {
                                val jsonContent = withContext(Dispatchers.IO) {
                                    jsonFile.readText()
                                }

                                // Handle different dataset types using specific processing
                                var processed = false

                                when (request.dataset) {
                                    AppUtils.DatasetNames.tph -> {
                                        val tphList = parseTPHJsonToList(jsonContent) as List<TPHNewModel>
                                        val filteredTphList = tphList.filterNot { it.isCompletelyNull() }

                                        AppLogger.d("Parsed ${tphList.size} TPH records, ${filteredTphList.size} valid after filtering")

                                        if (filteredTphList.isEmpty()) {
                                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                            errorMap[itemId] = "All TPH records are empty or invalid"
                                            _itemStatusMap.postValue(statusMap.toMap())
                                            _itemErrorMap.postValue(errorMap.toMap())
                                            incrementCompletedCount()
                                            continue
                                        }

                                        withContext(Dispatchers.IO) {
                                            try {
                                                if (request.isDownloadMasterTPHAsistensi) {
                                                    repository.insertTPH(filteredTphList)
                                                } else {
                                                    val deptId = request.estate
                                                    AppLogger.d("TPH Update - Department ID from request: $deptId, Estate Abbr: ${request.estateAbbr}")

                                                    val dataToUse = if (deptId != null) {
                                                        val tphForDept = filteredTphList.filter { it.dept == deptId }
                                                        AppLogger.d("TPH Update - Filtered ${tphForDept.size} records for department $deptId")
                                                        tphForDept
                                                    } else {
                                                        AppLogger.d("TPH Update - No department ID specified, updating all ${filteredTphList.size} records")
                                                        filteredTphList
                                                    }
                                                    repository.updateOrInsertTPH(dataToUse)
                                                }
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error: ${e.message}")
                                                throw e
                                            }
                                        }

                                        request.estateAbbr?.let {
                                            prefManager.setEstateLastModified(it, lastModified!!)
                                        } ?: run {
                                            prefManager.lastModifiedDatasetTPH = lastModified
                                        }

                                        processed = true
                                    }


                                    AppUtils.DatasetNames.kemandoran -> {
                                        val kemandoranList = parseStructuredJsonToList(jsonContent, KemandoranModel::class.java)
                                        AppLogger.d("Parsed ${kemandoranList.size} kemandoran records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.updateOrInsertKemandoran(kemandoranList)
                                                AppLogger.d("Database update completed for kemandoran dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for kemandoran: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetKemandoran = lastModified
                                        }

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.pemanen -> {
                                        val karyawanList = parseStructuredJsonToList(jsonContent, KaryawanModel::class.java)
                                        AppLogger.d("Parsed ${karyawanList.size} pemanen records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.updateOrInsertKaryawan(karyawanList)
                                                AppLogger.d("Database update completed for pemanen dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for pemanen: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetPemanen = lastModified
                                        }

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.blok -> {
                                        val blokList = parseStructuredJsonToList(jsonContent, BlokModel::class.java)
                                        AppLogger.d("Parsed ${blokList.size} blok records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.updateOrInsertBlok(blokList)
                                                AppLogger.d("Database update completed for blok dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for blok: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetBlok = lastModified
                                        }

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.transporter -> {
                                        val transporterList = parseStructuredJsonToList(jsonContent, TransporterModel::class.java)
                                        AppLogger.d("Parsed ${transporterList.size} transporter records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.InsertTransporter(transporterList)
                                                AppLogger.d("Database update completed for transporter dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for transporter: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetTransporter = lastModified
                                        }

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.kendaraan -> {
                                        val kendaraanList = parseStructuredJsonToList(jsonContent, KendaraanModel::class.java)
                                        AppLogger.d("Parsed ${kendaraanList.size} kendaraan records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.InsertKendaraan(kendaraanList)
                                                AppLogger.d("Database update completed for kendaraan dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for kendaraan: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetKendaraan = lastModified
                                        }

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.jenisTPH -> {
                                        val jenisTPHList = parseStructuredJsonToList(jsonContent, JenisTPHModel::class.java)
                                        AppLogger.d("Parsed ${jenisTPHList.size} jenis TPH records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.updateOrInsertJenisTPH(jenisTPHList)
                                                AppLogger.d("Database update completed for kendaraan dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for kendaraan: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetJenisTPH = lastModified
                                        }

                                        processed = true
                                    }
                                }

                                // Set success status if processed
                                if (processed) {
                                    statusMap[itemId] = if (isDownloadDataset) {
                                        AppUtils.UploadStatusUtils.DOWNLOADED
                                    } else {
                                        AppUtils.UploadStatusUtils.UPDATED
                                    }
                                } else {
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                    errorMap[itemId] = "No processor found for dataset: ${request.dataset}"
                                }
                            } else {
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "Output.json not found in ZIP file"
                            }

                            // Clean up files
                            withContext(Dispatchers.IO) {
                                tempFile.delete()
                                extractDir.deleteRecursively()
                            }
                        }
                        else if (contentType?.contains("application/json") == true) {
                            handleJsonResponse(request, itemId, response, statusMap, errorMap, progressMap, lastModified, lastModifiedSettingsJson, isDownloadDataset)
                        }else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Unsupported content type: $contentType"
                        }
                    }
                    else {
                        val errorBody = response.errorBody()?.string()

                        AppLogger.d("API Error response: $errorBody")
                        var errorMessage = "API Error: ${response.code()}"

                        if (!errorBody.isNullOrEmpty()) {
                            try {
                                // If error is JSON, parse it
                                val jsonError = JSONObject(errorBody)

                                // Priority order for error messages:
                                // 1. message field if available
                                // 2. error field if available
                                // 3. Fallback to raw error body
                                errorMessage = when {
                                    jsonError.has("message") -> "API Error ${response.code()}: ${jsonError.getString("message")}"
                                    jsonError.has("error") -> "API Error ${response.code()}: ${jsonError.getString("error")}"
                                    else -> "API Error ${response.code()}: $errorBody"
                                }

                                // If both message and error are available, combine them for more detail
                                if (jsonError.has("message") && jsonError.has("error")) {
                                    errorMessage = "API Error ${response.code()}: ${jsonError.getString("message")} - Details: ${jsonError.getString("error")}"
                                }

                            } catch (e: Exception) {
                                // If not JSON, use raw error body
                                errorMessage = "API Error ${response.code()}: $errorBody"
                            }
                        }

                        statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                        errorMap[itemId] = errorMessage
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error processing item $itemId: ${e.message}")
                    AppLogger.e(e.stackTraceToString())
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error: ${e.message}"
                }

                // Update LiveData AFTER all operations are complete for this item
                _itemStatusMap.postValue(statusMap.toMap())
                _itemErrorMap.postValue(errorMap.toMap())

                // Log the status update
                AppLogger.d("Item $itemId processing complete with status: ${statusMap[itemId]}")

                incrementCompletedCount()
            }

            _processingComplete.postValue(true)

            AppLogger.d("All downloads complete with statuses: $statusMap")
        }
    }

    fun getPreviewDataRestanWeek(estate: Int, afdeling: String) {
        viewModelScope.launch {

            try {
                val response = withContext(Dispatchers.IO) {
                    restanRepository.getDataRestan(estate, afdeling)
                }

                AppLogger.d(response.body().toString())
                if (response.isSuccessful && response.code() == 200) {
                    val jsonString = response.body()?.string() ?: ""

                    AppLogger.d(jsonString.toString())
                    // Process the JSON response to create formatted summary
                    val formattedData = processPreviewDataRestanUI(jsonString)

                    _restanPreviewData.value = formattedData
                } else {
                    _restanPreviewData.value = "Gagal memuat data: ${response.message()}"
                }
            } catch (e: Exception) {
                AppLogger.e("Error loading Restan preview: ${e.message}")
                _restanPreviewData.value = "Error: ${e.message}"
            }
        }
    }

    private suspend fun handleJsonResponse(
        request: DatasetRequest,
        itemId: Int,
        response: Response<ResponseBody>,
        statusMap: MutableMap<Int, String>,
        errorMap: MutableMap<Int, String?>,
        progressMap: MutableMap<Int, Int>,  // Add progressMap parameter
        lastModified: String?,
        lastModifiedSettingsJson: String?,
        isDownloadDataset: Boolean = true
    ) {
        // Initial progress update - start at 0%
        progressMap[itemId] = 0
        _itemProgressMap.postValue(progressMap.toMap())

        // Read response body - show 25% progress when starting to read
        progressMap[itemId] = 25
        _itemProgressMap.postValue(progressMap.toMap())

        val responseBodyString = response.body()?.string() ?: "Empty Response"
        AppLogger.d("Received JSON: $responseBodyString")

        // Update to 50% after reading the response
        progressMap[itemId] = 50
        _itemProgressMap.postValue(progressMap.toMap())

        // Check if it's an "up to date" response
        if (responseBodyString.contains("\"success\":false") &&
            responseBodyString.contains("\"message\":\"Dataset is up to date\"")) {
            progressMap[itemId] = 100
            statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
            _itemProgressMap.postValue(progressMap.toMap())
            AppLogger.d("Dataset ${request.dataset} is up to date")
            return
        }

        // Handle different dataset types
        when (request.dataset) {
            AppUtils.DatasetNames.settingJSON -> {
                // Handle settings JSON with progress updates
                if (responseBodyString.isBlank()) {
                    AppLogger.e("Received empty JSON response for settings")
                    progressMap[itemId] = 100  // Still show 100% even on error
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Empty JSON response"
                    _itemProgressMap.postValue(progressMap.toMap())
                    return
                }

                try {
                    // Parsing - update to 75%
                    progressMap[itemId] = 75
                    _itemProgressMap.postValue(progressMap.toMap())

                    val jsonObject = JSONObject(responseBodyString)
                    val tphRadius = jsonObject.optInt("tph_radius", -1)
                    val gpsAccuracy = jsonObject.optInt("gps_accuracy", -1)

                    var isStored = false

                    if (tphRadius != -1) {
                        prefManager.radiusMinimum = tphRadius.toFloat()
                        isStored = true
                    }
                    if (gpsAccuracy != -1) {
                        prefManager.boundaryAccuracy = gpsAccuracy.toFloat()
                        isStored = true
                    }

                    // Final update - 100%
                    progressMap[itemId] = 100
                    _itemProgressMap.postValue(progressMap.toMap())

                    if (isStored) {
                        prefManager.lastModifiedSettingJSON = lastModifiedSettingsJson
                        statusMap[itemId] = if (isDownloadDataset) {
                            AppUtils.UploadStatusUtils.DOWNLOADED
                        } else {
                            AppUtils.UploadStatusUtils.UPDATED
                        }
                        AppLogger.d("Successfully stored settings JSON")
                    } else {
                        if (responseBodyString.contains("\"success\":false") &&
                            (responseBodyString.contains("\"message\":\"Settings is up to date\"") ||
                                    responseBodyString.contains("\"message\":\"Dataset is up to date\""))) {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                            AppLogger.d("${AppUtils.DatasetNames.settingJSON} are up to date")
                        } else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "No valid settings found in response"
                        }
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error parsing settings JSON: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error parsing settings: ${e.message}"
                }
            }

            AppUtils.DatasetNames.mill -> {
                try {
                    // Parsing - update to 60%
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    val millList = parseMill(responseBodyString)
                    AppLogger.d("Parsed ${millList.size} mill records")

                    // Update to 75% before database operations
                    progressMap[itemId] = 75
                    _itemProgressMap.postValue(progressMap.toMap())

                    withContext(Dispatchers.IO) {
                        try {
                            repository.updateOrInsertMill(millList)
                            AppLogger.d("Successfully stored mill data")

                            // Final update - 100%
                            progressMap[itemId] = 100
                            _itemProgressMap.postValue(progressMap.toMap())

                            statusMap[itemId] = if (isDownloadDataset) {
                                AppUtils.UploadStatusUtils.DOWNLOADED
                            } else {
                                AppUtils.UploadStatusUtils.UPDATED
                            }
                        } catch (e: Exception) {
                            progressMap[itemId] = 100  // Still show 100% even on error
                            _itemProgressMap.postValue(progressMap.toMap())

                            AppLogger.e("Error storing mill data: ${e.message}")
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Error storing mill data: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error parsing mill data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error parsing mill data: ${e.message}"
                }
            }

            AppUtils.DatasetNames.estate -> {
                try {
                    // Parsing - update to 60%
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    val estateAndAfdeling = parseEstate(responseBodyString)
                    val estateList = estateAndAfdeling.first
                    val afdelingList = estateAndAfdeling.second

                    AppLogger.d("Parsed ${estateList.size} estates and ${afdelingList.size} afdelings")

                    // Update to 75% before database operations
                    progressMap[itemId] = 75
                    _itemProgressMap.postValue(progressMap.toMap())

                    withContext(Dispatchers.IO) {
                        try {
                            // Store the estate data - 85%
                            repository.updateOrInsertEstate(estateList)
                            progressMap[itemId] = 85
                            _itemProgressMap.postValue(progressMap.toMap())

                            // Store the afdeling data - 95%
                            if (afdelingList.isNotEmpty()) {
                                repository.updateOrInsertAfdeling(afdelingList)
                                progressMap[itemId] = 95
                                _itemProgressMap.postValue(progressMap.toMap())
                            }

                            AppLogger.d("Successfully stored estate data")

                            // Final update - 100%
                            progressMap[itemId] = 100
                            _itemProgressMap.postValue(progressMap.toMap())

                            statusMap[itemId] = if (isDownloadDataset) {
                                AppUtils.UploadStatusUtils.DOWNLOADED
                            } else {
                                AppUtils.UploadStatusUtils.UPDATED
                            }

                            // Update last modified timestamp
                            if (lastModified != null) {
                                prefManager.lastModifiedDatasetEstate = lastModified
                            }
                        } catch (e: Exception) {
                            progressMap[itemId] = 100  // Still show 100% even on error
                            _itemProgressMap.postValue(progressMap.toMap())

                            AppLogger.e("Error storing estate data: ${e.message}")
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Error storing estate data: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error parsing estate data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error parsing estate data: ${e.message}"
                }
            }

            AppUtils.DatasetNames.sinkronisasiRestan -> {
                try {
                    // We're already at 50% when this code starts
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    // Parse the JSON response to extract the restan data
                    val jsonObject = JSONObject(responseBodyString)

                    if (jsonObject.optBoolean("success", false)) {
                        val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                        val panenList = mutableListOf<PanenEntity>()
                        val recordsToDelete = mutableListOf<Pair<String, String>>() // Store (tph_id, date_created) pairs for records to delete

                        // Update to 70% before processing records
                        progressMap[itemId] = 70
                        _itemProgressMap.postValue(progressMap.toMap())

                        // Add this debugging code in your processing loop
// Process each record from the response
                        var status0Count = 0
                        var status1Count = 0
                        var status2Count = 0
                        var status0WithNullSpb = 0
                        var status0WithNonNullSpb = 0

// NEW: Check for data inconsistencies
                        var status1WithNullSpb = 0
                        var status2WithNullSpb = 0
                        var status1WithNonNullSpb = 0
                        var status2WithNonNullSpb = 0

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)

                            // Extract required fields
                            val tphId = item.optString("tph", "")
                            val createdDate = item.optString("created_date", "")
                            val statusEspb = item.optInt("status_espb", -1)
                            val jjgKirim = item.optInt("jjg_kirim", 0)

                            // For spb_kode, check specifically for null vs. empty string
                            val spbKode: String? = if (item.has("spb_kode") && !item.isNull("spb_kode")) {
                                item.optString("spb_kode")
                            } else {
                                null
                            }

                            // Count status_espb values and check spb_kode relationship
                            when (statusEspb) {
                                0 -> {
                                    status0Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status0WithNullSpb++
                                    } else {
                                        status0WithNonNullSpb++
                                        // Log some examples
                                        if (status0WithNonNullSpb <= 3) {
                                            AppLogger.d("Status_espb=0 with spb_kode: '$spbKode' (tph: $tphId)")
                                        }
                                    }
                                }
                                1 -> {
                                    status1Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status1WithNullSpb++
                                        // This should be rare/impossible - log these cases
                                        AppLogger.w("⚠️ INCONSISTENCY: Status_espb=1 but spb_kode is NULL/empty (tph: $tphId, date: $createdDate)")
                                    } else {
                                        status1WithNonNullSpb++
                                        // Log some examples
                                        if (status1WithNonNullSpb <= 3) {
                                            AppLogger.d("Status_espb=1 with spb_kode: '$spbKode' (tph: $tphId)")
                                        }
                                    }
                                }
                                2 -> {
                                    status2Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status2WithNullSpb++
                                        // This should be rare/impossible - log these cases
                                        AppLogger.w("⚠️ INCONSISTENCY: Status_espb=2 but spb_kode is NULL/empty (tph: $tphId, date: $createdDate)")
                                    } else {
                                        status2WithNonNullSpb++
                                        // Log some examples
                                        if (status2WithNonNullSpb <= 3) {
                                            AppLogger.d("Status_espb=2 with spb_kode: '$spbKode' (tph: $tphId)")
                                        }
                                    }
                                }
                            }

                            // ... rest of your processing logic
                        }

// Enhanced summary with spb_kode relationship check
                        AppLogger.d("=== RESTAN PROCESSING SUMMARY ===")
                        AppLogger.d("Total records processed: ${dataArray.length()}")
                        AppLogger.d("Status_espb = 0: $status0Count")
                        AppLogger.d("  - With null/empty spb_kode: $status0WithNullSpb (will INSERT/UPDATE)")
                        AppLogger.d("  - With non-null spb_kode: $status0WithNonNullSpb (will DELETE)")
                        AppLogger.d("Status_espb = 1: $status1Count")
                        AppLogger.d("  - With null/empty spb_kode: $status1WithNullSpb (⚠️ INCONSISTENT)")
                        AppLogger.d("  - With non-null spb_kode: $status1WithNonNullSpb (normal)")
                        AppLogger.d("Status_espb = 2: $status2Count")
                        AppLogger.d("  - With null/empty spb_kode: $status2WithNullSpb (⚠️ INCONSISTENT)")
                        AppLogger.d("  - With non-null spb_kode: $status2WithNonNullSpb (normal)")
                        AppLogger.d("Records to INSERT/UPDATE: ${panenList.size}")
                        AppLogger.d("Records to DELETE: ${recordsToDelete.size}")

// Data consistency check
                        if (status1WithNullSpb > 0 || status2WithNullSpb > 0) {
                            AppLogger.e("🚨 DATA INCONSISTENCY DETECTED!")
                            AppLogger.e("Found ${status1WithNullSpb} records with status_espb=1 but null spb_kode")
                            AppLogger.e("Found ${status2WithNullSpb} records with status_espb=2 but null spb_kode")
                            AppLogger.e("This suggests data quality issues in the backend!")
                        } else {
                            AppLogger.d("✅ Data consistency check passed - no inconsistencies found")
                        }
                        AppLogger.d("==================================")

                        // Update to 80% when processing is complete
                        progressMap[itemId] = 80
                        _itemProgressMap.postValue(progressMap.toMap())

                        withContext(Dispatchers.IO) {
                            try {
                                var successCount = 0
                                var failCount = 0
                                var deleteCount = 0

                                // STEP 1: Delete records that should be removed from local DB
                                if (recordsToDelete.isNotEmpty()) {
                                    AppLogger.d("Processing ${recordsToDelete.size} records for deletion...")

                                    for (recordToDelete in recordsToDelete) {
                                        val (tphId, dateCreated) = recordToDelete

                                        // Check if the record exists in local DB
                                        val exists = panenDao.exists(tphId, dateCreated)

                                        if (exists) {
                                            try {
                                                // Get the record ID
                                                val recordId = panenDao.getIdByTphIdAndDateCreated(tphId, dateCreated)

                                                if (recordId > 0) {
                                                    // Delete the record
                                                    panenDao.deleteById(recordId)
                                                    deleteCount++
                                                    AppLogger.d("Deleted record: TPH=$tphId, date=$dateCreated")
                                                }
                                            } catch (e: Exception) {
                                                AppLogger.e("Error deleting record: ${e.message}")
                                            }
                                        }
                                    }

                                    AppLogger.d("Deleted $deleteCount records from local DB")
                                }

                                // STEP 2: Insert or Update records that should be in local DB
                                if (panenList.isNotEmpty()) {
                                    AppLogger.d("Processing ${panenList.size} records for insert/update...")

                                    for (panen in panenList) {
                                        try {
                                            // Check if record already exists in local DB
                                            val exists = panenDao.exists(panen.tph_id, panen.date_created)

                                            if (!exists) {
                                                // Record doesn't exist -> INSERT
                                                val result = panenDao.insertWithTransaction(panen)
                                                if (result.isSuccess) {
                                                    successCount++
                                                    AppLogger.d("Inserted new restan record: ${panen.tph_id}, ${panen.date_created}")
                                                } else {
                                                    failCount++
                                                    AppLogger.e("Failed to insert restan record: ${panen.tph_id}, ${panen.date_created}")
                                                }
                                            } else {
                                                // Record exists -> UPDATE
                                                AppLogger.d("Record exists, updating: ${panen.tph_id}, ${panen.date_created}")

                                                try {
                                                    // Get the existing record ID
                                                    val recordId = panenDao.getIdByTphIdAndDateCreated(panen.tph_id, panen.date_created)

                                                    if (recordId > 0) {
                                                        // Create updated record with same ID
                                                        val updatedRecord = panen.copy(id = recordId)

                                                        // Update the record
                                                        panenDao.update(listOf(updatedRecord))
                                                        successCount++
                                                        AppLogger.d("Updated existing record ID $recordId: ${panen.tph_id}, ${panen.date_created}")
                                                    } else {
                                                        // Fallback: insert as new if we can't find the ID
                                                        AppLogger.e("Couldn't find existing record to update: ${panen.tph_id}, ${panen.date_created}")
                                                        val result = panenDao.insertWithTransaction(panen)
                                                        if (result.isSuccess) {
                                                            successCount++
                                                            AppLogger.d("Inserted as new after update failure: ${panen.tph_id}, ${panen.date_created}")
                                                        } else {
                                                            failCount++
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    failCount++
                                                    AppLogger.e("Error updating record: ${e.message}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            failCount++
                                            AppLogger.e("Error processing restan record: ${e.message}")
                                        }
                                    }
                                }

                                // Final summary
                                AppLogger.d("=== RESTAN SYNC COMPLETE ===")
                                AppLogger.d("Inserted/Updated: $successCount")
                                AppLogger.d("Deleted: $deleteCount")
                                AppLogger.d("Failed: $failCount")
                                AppLogger.d("Total records for insert/update: ${panenList.size}")
                                AppLogger.d("Total records for delete: ${recordsToDelete.size}")
                                AppLogger.d("============================")

                                // Final update - 100%
                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())

                                // Set the status based on the result
                                if (failCount == 0) {
                                    statusMap[itemId] = if (isDownloadDataset) {
                                        AppUtils.UploadStatusUtils.DOWNLOADED
                                    } else {
                                        AppUtils.UploadStatusUtils.UPDATED
                                    }
                                } else if (successCount > 0 || deleteCount > 0) {
                                    // Partial success
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPDATED
                                    errorMap[itemId] = "Partial success: $failCount/${panenList.size + deleteCount} records failed"
                                } else {
                                    // Complete failure
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                    errorMap[itemId] = "Failed to process restan data: $failCount/${panenList.size + deleteCount} records failed"
                                }
                            } catch (e: Exception) {
                                progressMap[itemId] = 100  // Still show 100% even on error
                                _itemProgressMap.postValue(progressMap.toMap())

                                AppLogger.e("Error processing restan data: ${e.message}")
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "Error processing restan data: ${e.message}"
                            }
                        }
                    } else {
                        progressMap[itemId] = 100
                        _itemProgressMap.postValue(progressMap.toMap())

                        statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                        val errorMessage = jsonObject.optString("message", "Unknown error")
                        errorMap[itemId] = "API Error: $errorMessage"
                        AppLogger.e("Restan API returned error: $errorMessage")
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error processing restan data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing restan data: ${e.message}"
                }
            }

            else -> {
                progressMap[itemId] = 100  // Still show 100% even on error
                _itemProgressMap.postValue(progressMap.toMap())

                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                errorMap[itemId] = "Unsupported JSON dataset: ${request.dataset}"
            }
        }
    }

    fun processPreviewDataRestanUI(jsonResponse: String): String {
        try {
            // Parse the JSON response
            val jsonObject = JSONObject(jsonResponse)

            // Check if response is successful and contains data
            if (jsonObject.optBoolean("success", false)) {
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()

                // NEW: Data consistency check counters
                var status0Count = 0
                var status1Count = 0
                var status2Count = 0
                var status0WithNullSpb = 0
                var status0WithNonNullSpb = 0
                var status1WithNullSpb = 0
                var status2WithNullSpb = 0
                var status1WithNonNullSpb = 0
                var status2WithNonNullSpb = 0

                // Set up date range
                val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormatter = SimpleDateFormat("d MMMM", Locale("id", "ID")) // Indonesian date format
                val calendar = Calendar.getInstance()

                // Yesterday
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = inputFormatter.format(calendar.time)
                val yesterdayDate = calendar.time

                // 7 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val sevenDaysAgo = inputFormatter.format(calendar.time)
                val sevenDaysAgoDate = calendar.time

                // Format the date range for display (e.g., "28 April - 5 Mei")
                val endDateDisplay = displayFormatter.format(yesterdayDate)
                val startDateDisplay = displayFormatter.format(sevenDaysAgoDate)

                // Create a list of all dates in the range
                val allDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = sevenDaysAgoDate

                while (!tempCalendar.time.after(yesterdayDate)) {
                    allDates.add(inputFormatter.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Initialize maps for all dates in range with zeros
                val jjgKirimByDate = mutableMapOf<String, Int>()
                val tphCountByDate = mutableMapOf<String, Int>() // Total TPH count (not unique)
                val recordCountByDate = mutableMapOf<String, Int>() // Track record count by date

                for (date in allDates) {
                    jjgKirimByDate[date] = 0
                    tphCountByDate[date] = 0
                    recordCountByDate[date] = 0
                }

                // Process each item in the array
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)

                    val statusEspb = item.optInt("status_espb", -1)
                    val tphId = item.optString("tph", "")
                    val createdDateFull = item.optString("created_date", "")

                    // Check spb_kode - same logic as in your insert process
                    val spbKode: String? = if (item.has("spb_kode") && !item.isNull("spb_kode")) {
                        item.optString("spb_kode")
                    } else {
                        null
                    }

                    // NEW: Data consistency check for all records
                    when (statusEspb) {
                        0 -> {
                            status0Count++
                            if (spbKode.isNullOrEmpty()) {
                                status0WithNullSpb++
                            } else {
                                status0WithNonNullSpb++
                                // Log some examples
                                if (status0WithNonNullSpb <= 3) {
                                    AppLogger.d("Status_espb=0 with spb_kode: '$spbKode' (tph: $tphId)")
                                }
                            }
                        }
                        1 -> {
                            status1Count++
                            if (spbKode.isNullOrEmpty()) {
                                status1WithNullSpb++
                                // This should be impossible - log these inconsistencies
                                AppLogger.w("⚠️ INCONSISTENCY: Status_espb=1 but spb_kode is NULL/empty (tph: $tphId, date: $createdDateFull)")
                            } else {
                                status1WithNonNullSpb++
                                // Log some examples
                                if (status1WithNonNullSpb <= 3) {
                                    AppLogger.d("Status_espb=1 with spb_kode: '$spbKode' (tph: $tphId)")
                                }
                            }
                        }
                        2 -> {
                            status2Count++
                            if (spbKode.isNullOrEmpty()) {
                                status2WithNullSpb++
                                // This should be impossible - log these inconsistencies
                                AppLogger.w("⚠️ INCONSISTENCY: Status_espb=2 but spb_kode is NULL/empty (tph: $tphId, date: $createdDateFull)")
                            } else {
                                status2WithNonNullSpb++
                                // Log some examples
                                if (status2WithNonNullSpb <= 3) {
                                    AppLogger.d("Status_espb=2 with spb_kode: '$spbKode' (tph: $tphId)")
                                }
                            }
                        }
                    }

                    // EXISTING LOGIC: Only process status_espb = 0 with null/empty spb_kode for preview
                    if (statusEspb != 0) {
                        continue // Skip records with status_espb != 0
                    }

                    // Skip records with non-null/non-empty spb_kode
                    // Only show records that will actually be inserted (status_espb = 0 AND spb_kode null/empty)
                    if (!spbKode.isNullOrEmpty()) {
                        continue // Skip records with spb_kode - these will be deleted, not inserted
                    }

                    // Extract created_date and format to get just the date part
                    val createdDate = if (createdDateFull.isNotEmpty()) {
                        createdDateFull.split(" ")[0] // Take only the date part (YYYY-MM-DD)
                    } else {
                        continue // Skip if no date
                    }

                    // Skip data outside our date range
                    if (!allDates.contains(createdDate)) {
                        continue
                    }

                    // Increment record count for this date
                    recordCountByDate[createdDate] = recordCountByDate.getOrDefault(createdDate, 0) + 1

                    // Get jjg_kirim value
                    val jjgKirim = item.optInt("jjg_kirim", 0)

                    // Update the total jjg_kirim for this date
                    jjgKirimByDate[createdDate] = jjgKirimByDate.getOrDefault(createdDate, 0) + jjgKirim

                    // Simply increment the TPH count for this date (no uniqueness check)
                    // Only count if tph value is greater than 0
                    val tphIdInt = item.optInt("tph", 0)
                    if (tphIdInt > 0) {
                        tphCountByDate[createdDate] = tphCountByDate.getOrDefault(createdDate, 0) + 1
                    }
                }

                // NEW: Log the data consistency check results
                AppLogger.d("=== PREVIEW DATA CONSISTENCY CHECK ===")
                AppLogger.d("Total records in response: ${dataArray.length()}")
                AppLogger.d("Status_espb = 0: $status0Count")
                AppLogger.d("  - With null/empty spb_kode: $status0WithNullSpb (shown in preview)")
                AppLogger.d("  - With non-null spb_kode: $status0WithNonNullSpb (hidden from preview)")
                AppLogger.d("Status_espb = 1: $status1Count")
                AppLogger.d("  - With null/empty spb_kode: $status1WithNullSpb (⚠️ INCONSISTENT)")
                AppLogger.d("  - With non-null spb_kode: $status1WithNonNullSpb (expected)")
                AppLogger.d("Status_espb = 2: $status2Count")
                AppLogger.d("  - With null/empty spb_kode: $status2WithNullSpb (⚠️ INCONSISTENT)")
                AppLogger.d("  - With non-null spb_kode: $status2WithNonNullSpb (expected)")

                // Data consistency verdict
                if (status1WithNullSpb > 0 || status2WithNullSpb > 0) {
                    AppLogger.e("🚨 DATA INCONSISTENCY DETECTED IN PREVIEW!")
                    AppLogger.e("Found ${status1WithNullSpb} records with status_espb=1 but null spb_kode")
                    AppLogger.e("Found ${status2WithNullSpb} records with status_espb=2 but null spb_kode")
                    AppLogger.e("This indicates backend data quality issues!")
                } else {
                    AppLogger.d("✅ Data consistency check PASSED")
                    AppLogger.d("All status_espb=1&2 records have non-null spb_kode as expected")
                }
                AppLogger.d("=====================================")

                // Calculate total jjg_kirim for all dates
                val totalJjgKirim = jjgKirimByDate.values.sum()

                // Build the final string
                val resultBuilder = StringBuilder()
                resultBuilder.append("Data Restan dalam 7 hari terakhir ($startDateDisplay - $endDateDisplay):\n")

                // Add each date's transactions with jjg_kirim count, but only if they have data
                var hasValidData = false
                for (date in allDates.sortedDescending()) {
                    val jjgKirimCount = jjgKirimByDate[date] ?: 0
                    val tphCount = tphCountByDate[date] ?: 0
                    val recordCount = recordCountByDate[date] ?: 0

                    // Skip dates with zero records
                    if (recordCount == 0) {
                        continue
                    }

                    hasValidData = true

                    // Format date for display (e.g., "5 Mei")
                    val dateObj = inputFormatter.parse(date)
                    val dateDisplay = displayFormatter.format(dateObj!!)

                    resultBuilder.append("$dateDisplay: $jjgKirimCount jjg dari $tphCount TPH\n")
                }

                // If no valid restan data found
                if (!hasValidData) {
                    resultBuilder.append("Tidak ada data restan yang valid dalam periode ini.\n")
                }

                resultBuilder.append("\nTotal jjg restan: $totalJjgKirim")

                return resultBuilder.toString().trim()
            } else {
                return "Failed to process data: Success flag is false"
            }
        } catch (e: Exception) {
            Log.e("RestanDataProcessor", "Error processing data: ${e.message}")
            return "Error processing data: ${e.message}"
        }
    }

    // Helper function to parse mill JSON
    private fun parseMill(jsonContent: String): List<MillModel> {
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
        val dataArray = jsonObject.getAsJsonArray("data")
        return gson.fromJson(
            dataArray,
            TypeToken.getParameterized(List::class.java, MillModel::class.java).type
        )
    }

    // Helper function to parse estate JSON
    private fun parseEstate(jsonContent: String): Pair<List<EstateModel>, List<AfdelingModel>> {
        val estateList = mutableListOf<EstateModel>()
        val afdelingList = mutableListOf<AfdelingModel>()

        if (jsonContent.isBlank()) {
            AppLogger.w("Empty JSON content received for estate")
            return Pair(estateList, afdelingList)
        }

        val gson = Gson()
        try {
            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

            // Check if the JSON has the expected structure
            if (!jsonObject.has("data") || jsonObject.get("data").isJsonNull) {
                AppLogger.e("JSON does not contain 'data' field or it's null")
                return Pair(estateList, afdelingList)
            }

            val dataArray = jsonObject.getAsJsonArray("data")

            for (i in 0 until dataArray.size()) {
                try {
                    val estateObject = dataArray.get(i).asJsonObject

                    // Safely extract estate values with null checks
                    val id = if (estateObject.has("id") && !estateObject.get("id").isJsonNull)
                        estateObject.get("id").asInt else 0

                    val idPpro = if (estateObject.has("id_ppro") && !estateObject.get("id_ppro").isJsonNull)
                        estateObject.get("id_ppro").asInt else null

                    val abbr = if (estateObject.has("abbr") && !estateObject.get("abbr").isJsonNull)
                        estateObject.get("abbr").asString else null

                    val nama = if (estateObject.has("nama") && !estateObject.get("nama").isJsonNull)
                        estateObject.get("nama").asString else null

                    // Create EstateModel
                    val estate = EstateModel(
                        id = id,
                        id_ppro = idPpro,
                        abbr = abbr,
                        nama = nama
                    )
                    estateList.add(estate)

                    // Parse afdeling data if present
                    if (estateObject.has("divisi") && !estateObject.get("divisi").isJsonNull) {
                        val divisiArray = estateObject.getAsJsonArray("divisi")

                        for (j in 0 until divisiArray.size()) {
                            try {
                                val divisiObject = divisiArray.get(j).asJsonObject

                                val divisiId = if (divisiObject.has("id") && !divisiObject.get("id").isJsonNull)
                                    divisiObject.get("id").asInt else continue

                                val divisiIdPpro = if (divisiObject.has("id_ppro") && !divisiObject.get("id_ppro").isJsonNull)
                                    divisiObject.get("id_ppro").asInt else null

                                val divisiAbbr = if (divisiObject.has("abbr") && !divisiObject.get("abbr").isJsonNull)
                                    divisiObject.get("abbr").asString?.trim() else null

                                val divisiNama = if (divisiObject.has("nama") && !divisiObject.get("nama").isJsonNull)
                                    divisiObject.get("nama").asString else null

                                val afdeling = AfdelingModel(
                                    id = divisiId,
                                    id_ppro = divisiIdPpro,
                                    abbr = divisiAbbr,
                                    nama = divisiNama,
                                    estate_id = id
                                )
                                afdelingList.add(afdeling)
                            } catch (e: Exception) {
                                AppLogger.e("Error parsing afdeling at index $j for estate $id: ${e.message}")
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error parsing estate at index $i: ${e.message}")
                    continue
                }
            }

            AppLogger.d("Successfully parsed ${estateList.size} estates and ${afdelingList.size} afdelings")
        } catch (e: Exception) {
            AppLogger.e("Error parsing JSON: ${e.message}")
        }

        return Pair(estateList, afdelingList)
    }

    // Helper function to parse JSON to model list (assuming you have this somewhere)
    private inline fun <reified T> parseJsonToList(jsonContent: String, modelClass: Class<T>): List<T> {
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
        val dataArray = jsonObject.getAsJsonArray("data")
        return gson.fromJson(
            dataArray,
            TypeToken.getParameterized(List::class.java, modelClass).type
        )
    }



    fun resetState() {
        viewModelScope.launch {
            _processingComplete.postValue(false)
            _itemProgressMap.postValue(emptyMap())
            _itemStatusMap.postValue(emptyMap())
            _itemErrorMap.postValue(emptyMap())
            _completedCount.postValue(0)
            _totalCount.postValue(0)

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
                    AppUtils.DatasetNames.transporter,
                    AppUtils.DatasetNames.jenisTPH
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

                AppLogger.d(modifiedRequest.toString())

                try {
                    var response: Response<ResponseBody>? = null
                    if (request.dataset == AppUtils.DatasetNames.mill) {
                        response = repository.downloadSmallDataset(request.regional ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.estate) {
                        response = repository.downloadListEstate(request.regional ?: 0)
                    }
                    else if (request.dataset == AppUtils.DatasetNames.sinkronisasiRestan) {
                        response = restanRepository.getDataRestan(request.estate!!, request.afdeling!!)
                    }
                    else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
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
                                                            updateOperation = if (request.isDownloadMasterTPHAsistensi)
                                                                { tph ->
                                                                    updateOrInsertTPH(
                                                                        tph,
                                                                        true
                                                                    )
                                                                }
                                                            else
                                                                { tph ->
                                                                    updateOrInsertTPH(
                                                                        tph,
                                                                        false
                                                                    )
                                                                },
                                                            statusFlow = tphStatus,
                                                            hasShownError = hasShownError,
                                                            lastModifiedTimestamp = lastModified
                                                                ?: "",
                                                            isDownloadMasterTPHAsistensi = request.isDownloadMasterTPHAsistensi,
                                                            estateAbbr = request.estateAbbr
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

                                                    AppUtils.DatasetNames.jenisTPH -> hasShownError =
                                                        processDataset(
                                                            jsonContent = jsonContent,
                                                            dataset = request.dataset,
                                                            modelClass = JenisTPHModel::class.java,
                                                            results = results,
                                                            response = response,
                                                            updateOperation = ::updateOrInsertJenisTPH,
                                                            statusFlow = jenisTPHStatus,
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
                                                AppLogger.d("resultss bro $results")
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
                            }
                            else if (contentType?.contains("application/json") == true) {

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
                                } else if (request.dataset == AppUtils.DatasetNames.estate) {
                                    try {
                                        // Define the lists outside the function
                                        val estateList = mutableListOf<EstateModel>()
                                        val afdelingList = mutableListOf<AfdelingModel>()

                                        fun parseEstateJsonToList(jsonContent: String) {
                                            if (jsonContent.isBlank()) {
                                                Log.w("EstateParser", "Empty JSON content received")
                                                return
                                            }

                                            val gson = Gson()
                                            try {
                                                val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

                                                // Check if the JSON has the expected structure
                                                if (!jsonObject.has("data") || jsonObject.get("data").isJsonNull) {
                                                    Log.e("EstateParser", "JSON does not contain 'data' field or it's null")
                                                    return
                                                }

                                                val dataArray = jsonObject.getAsJsonArray("data")

                                                for (i in 0 until dataArray.size()) {
                                                    try {
                                                        val estateObject = dataArray.get(i).asJsonObject

                                                        // Safely extract estate values with null checks
                                                        val id = if (estateObject.has("id") && !estateObject.get("id").isJsonNull)
                                                            estateObject.get("id").asInt else 0

                                                        val idPpro = if (estateObject.has("id_ppro") && !estateObject.get("id_ppro").isJsonNull)
                                                            estateObject.get("id_ppro").asInt else null

                                                        val abbr = if (estateObject.has("abbr") && !estateObject.get("abbr").isJsonNull)
                                                            estateObject.get("abbr").asString else null

                                                        val nama = if (estateObject.has("nama") && !estateObject.get("nama").isJsonNull)
                                                            estateObject.get("nama").asString else null

                                                        // Create EstateModel
                                                        val estate = EstateModel(
                                                            id = id,
                                                            id_ppro = idPpro,
                                                            abbr = abbr,
                                                            nama = nama
                                                        )
                                                        estateList.add(estate)

                                                        // Parse afdeling data if present
                                                        if (estateObject.has("divisi") && !estateObject.get("divisi").isJsonNull) {
                                                            val divisiArray = estateObject.getAsJsonArray("divisi")

                                                            for (j in 0 until divisiArray.size()) {
                                                                try {
                                                                    val divisiObject = divisiArray.get(j).asJsonObject

                                                                    val divisiId = if (divisiObject.has("id") && !divisiObject.get("id").isJsonNull)
                                                                        divisiObject.get("id").asInt else continue

                                                                    val divisiIdPpro = if (divisiObject.has("id_ppro") && !divisiObject.get("id_ppro").isJsonNull)
                                                                        divisiObject.get("id_ppro").asInt else null

                                                                    val divisiAbbr = if (divisiObject.has("abbr") && !divisiObject.get("abbr").isJsonNull)
                                                                        divisiObject.get("abbr").asString?.trim() else null

                                                                    val divisiNama = if (divisiObject.has("nama") && !divisiObject.get("nama").isJsonNull)
                                                                        divisiObject.get("nama").asString else null

                                                                    val afdeling = AfdelingModel(
                                                                        id = divisiId,
                                                                        id_ppro = divisiIdPpro,
                                                                        abbr = divisiAbbr,
                                                                        nama = divisiNama,
                                                                        estate_id = id
                                                                    )
                                                                    afdelingList.add(afdeling)
                                                                } catch (e: Exception) {
                                                                    Log.e("AfdelingParser", "Error parsing afdeling at index $j for estate $id: ${e.message}")
                                                                    continue
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("EstateParser", "Error parsing estate at index $i: ${e.message}")
                                                        continue
                                                    }
                                                }

                                                Log.d("EstateParser", "Successfully parsed ${estateList.size} estates and ${afdelingList.size} afdelings")
                                            } catch (e: Exception) {
                                                Log.e("EstateParser", "Error parsing JSON: ${e.message}")
                                            }
                                        }

                                        // Parse the estate data from JSON
                                        parseEstateJsonToList(responseBodyString)

                                        // Check if we have any data to store
                                        if (estateList.isEmpty()) {
                                            Log.w("DownloadResponse", "No estate data to store")
                                            results[request.dataset] = Resource.Error("No estate data found in response")
                                            _downloadStatuses.postValue(results.toMap())
                                        }

                                        Log.d("DownloadResponse", "Storing ${estateList.size} estates and ${afdelingList.size} afdelings")

                                        // Update status to storing
                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Store the estate data
                                        updateOrInsertEstate(estateList).join()

                                        // Store the afdeling data
                                        if (afdelingList.isNotEmpty()) {
                                            updateOrInsertAfdeling(afdelingList).join()
                                        }

                                        if (estateStatus.value.isSuccess) {
                                            results[request.dataset] = Resource.Success(response)
                                            prefManager!!.addDataset(request.dataset)
                                        } else {
                                            val error = estateStatus.value.exceptionOrNull()
                                            throw error ?: Exception("Unknown database error")
                                        }

                                        _downloadStatuses.postValue(results.toMap())

                                    } catch (e: Exception) {
                                        Log.e("DownloadResponse", "Error processing estate data: ${e.message}")
                                        if (!hasShownError) {
                                            results[request.dataset] = Resource.Error("Error processing estate data: ${e.message}")
                                            hasShownError = true
                                        }
                                        _downloadStatuses.postValue(results.toMap())
                                    }
                                }
//                                else if (request.dataset == AppUtils.DatasetNames.updateSyncLocalData) {
//                                    results[request.dataset] = Resource.Loading(0)
//
//                                    try {
//                                        val jsonResponse = Gson().fromJson(
//                                            responseBodyString,
//                                            FetchStatusCMPResponse::class.java
//                                        )
//
//                                        viewModelScope.launch(Dispatchers.IO) {
//                                            val panenIdsToUpdate = mutableListOf<Int>()
//                                            val espbIdsToUpdate = mutableListOf<Int>()
//                                            val status =
//                                                mutableMapOf<String, Int>()  // Store status for batch updates
//
//                                            try {
//                                                val deferredUpdates =
//                                                    jsonResponse.data.map { item ->
//
//                                                        AppLogger.d(item.toString())
//                                                        async {
//                                                            try {
//                                                                // Get table_ids JSON from DB
//                                                                val tableIdsJson =
//                                                                    uploadCMPDao.getTableIdsByTrackingId(
//                                                                        item.id
//                                                                    ) ?: "{}"
//                                                                val tableIdsObj = Gson().fromJson(
//                                                                    tableIdsJson,
//                                                                    JsonObject::class.java
//                                                                )
//
//                                                                AppLogger.d(tableIdsObj.toString())
//
//                                                                tableIdsObj?.keySet()
//                                                                    ?.forEach { tableName ->
//                                                                        val ids =
//                                                                            tableIdsObj.getAsJsonArray(
//                                                                                tableName
//                                                                            ).map { it.asInt }
//
//                                                                        when (tableName) {
//                                                                            AppUtils.DatabaseTables.PANEN -> panenIdsToUpdate.addAll(
//                                                                                ids
//                                                                            )
//
//                                                                            AppUtils.DatabaseTables.ESPB -> espbIdsToUpdate.addAll(
//                                                                                ids
//                                                                            )
//                                                                        }
//
//                                                                        status[tableName] =
//                                                                            item.status
//                                                                    }
//
//                                                                uploadCMPDao.updateStatus(
//                                                                    item.id.toString(),
//                                                                    item.nama_file,
//                                                                    item.status
//                                                                )
//                                                            } catch (e: Exception) {
//                                                                AppLogger.e("Error processing item ${item.id}: ${e.localizedMessage}")
//                                                            }
//                                                        }
//                                                    }
//
//                                                deferredUpdates.awaitAll()
//
//                                                try {
//                                                    if (panenIdsToUpdate.isNotEmpty()) {
//                                                        panenDao.updateDataIsZippedPanen(
//                                                            panenIdsToUpdate,
//                                                            status[AppUtils.DatabaseTables.PANEN]
//                                                                ?: 0
//                                                        )
//                                                    }
//                                                    if (espbIdsToUpdate.isNotEmpty()) {
//                                                        espbDao.updateDataIsZippedESPB(
//                                                            espbIdsToUpdate,
//                                                            status[AppUtils.DatabaseTables.ESPB]
//                                                                ?: 0
//                                                        )
//                                                    }
//                                                } catch (e: Exception) {
//                                                    AppLogger.e("Error in batch updates: ${e.localizedMessage}")
//                                                }
//
//                                                withContext(Dispatchers.Main) {
//                                                    val sortedList = jsonResponse.data
//                                                        .filter { it.status >= 4 }
//                                                        .sortedByDescending { it.tanggal_upload }
//                                                    val message = if (sortedList.isNotEmpty()) {
//                                                        "Terjadi kesalahan insert di server!\n\n" + sortedList.joinToString(
//                                                            "\n"
//                                                        ) { item ->
//                                                            "• ${item.nama_file} (${item.message})"
//                                                        }
//                                                    } else {
//                                                        "Berhasil sinkronisasi data"
//                                                    }
//                                                    results[request.dataset] =
//                                                        Resource.Success(response, message)
//                                                    _downloadStatuses.postValue(results.toMap())
//                                                }
//
//                                            } catch (e: Exception) {
//                                                AppLogger.e("Error in processing dataset updateSyncLocalData: ${e.localizedMessage}")
//                                            }
//                                        }
//                                    } catch (e: Exception) {
//                                        AppLogger.e("Error parsing JSON response: ${e.localizedMessage}")
//                                    }
//                                } else {
//                                    results[request.dataset] = Resource.Success(response)
//                                }
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