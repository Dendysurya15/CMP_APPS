package com.cbi.mobile_plantation.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.JenisTPHModel
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
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.R

import com.cbi.mobile_plantation.data.database.DepartmentInfo
import com.cbi.mobile_plantation.data.database.TPHDao
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.DownloadMapProgressResponse
import com.cbi.mobile_plantation.data.model.DownloadMapResponse
import com.cbi.mobile_plantation.data.model.EstateModel
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.ParameterModel
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.repository.DataPanenInspectionRepository
import com.cbi.mobile_plantation.data.repository.DownloadIDMapRepository
import com.cbi.mobile_plantation.data.repository.HektaranPanenRepository
import com.cbi.mobile_plantation.data.repository.PemanenFaceRepository
import com.cbi.mobile_plantation.data.repository.RestanRepository
import com.cbi.mobile_plantation.data.repository.SaveTPHResult
import com.cbi.mobile_plantation.data.repository.SyncDataUserRepository
import com.cbi.mobile_plantation.data.repository.VersioningAppRepository
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
import java.lang.reflect.Parameter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Suppress("NAME_SHADOWING")
class DatasetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DatasetRepository = DatasetRepository(application)

    private val appRepository: AppRepository = AppRepository(application)
    private val hektarPanenRepository: HektaranPanenRepository =
        HektaranPanenRepository(application)
    private val restanRepository: RestanRepository = RestanRepository(application)
    private val syncDataUserRepository: SyncDataUserRepository = SyncDataUserRepository(application)
    private val versioningAppRepository: VersioningAppRepository =
        VersioningAppRepository(application)
    private val dataPanenInspectionRepository: DataPanenInspectionRepository =
        DataPanenInspectionRepository(application)
    private val pemanenFaceRepository: PemanenFaceRepository =
        PemanenFaceRepository(application)
    private val prefManager = PrefManager(application)

    private val downloadMapRepository = DownloadIDMapRepository()

    private val database = AppDatabase.getDatabase(application)
    private val uploadCMPDao = database.uploadCMPDao()
    private val espbDao = database.espbDao()
    private val panenDao = database.panenDao()
    private val absensiDao = database.absensiDao()
    private val inspeksiDao = database.inspectionDao()
    private val mutuBuahDao = database.mutuBuahDao()
    private val karyawanDao = database.karyawanDao()
    private val blokDao = database.blokDao()
    private val hektarPanenDao = database.hektarPanenDao()
    private val inspectionDao = database.inspectionDao()
    private val inspectionDetailDao = database.inspectionDetailDao()
    private val parameterDao = database.parameterDao()

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

    private val _parameterStatus = MutableStateFlow<Result<Boolean>>(Result.success(false))
    val parameterStatus: StateFlow<Result<Boolean>> = _parameterStatus.asStateFlow()

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

    private val _downloadMapList = MutableLiveData<Result<DownloadMapResponse>>()
    val downloadMapList: LiveData<Result<DownloadMapResponse>> = _downloadMapList

    private val _downloadMapProgress = MutableLiveData<Result<DownloadMapProgressResponse>>()
    val downloadMapProgress: LiveData<Result<DownloadMapProgressResponse>> = _downloadMapProgress


    private val _fetchStatusUploadCMPLiveData = MutableLiveData<List<FetchResponseItem>>()
    val fetchStatusUploadCMPLiveData: LiveData<List<FetchResponseItem>> =
        _fetchStatusUploadCMPLiveData

    private val _distinctDeptInfoList = MutableLiveData<List<DepartmentInfo>>()
    val distinctDeptInfoList: LiveData<List<DepartmentInfo>> = _distinctDeptInfoList

    private val _distinctDeptInfoListCopy = MutableLiveData<List<DepartmentInfo>>()
    val distinctDeptInfoListCopy: LiveData<List<DepartmentInfo>> = _distinctDeptInfoListCopy

    private val _restanPreviewData = MutableLiveData<String>()
    val restanPreviewData: LiveData<String> = _restanPreviewData

    private val _dataPanenInspeksiPreview = MutableLiveData<String>()
    val dataPanenInspeksiPreview: LiveData<String> = _dataPanenInspeksiPreview

    private val _followUpInspeksiPreview = MutableLiveData<String>()
    val followUpInspeksiPreview: LiveData<String> = _followUpInspeksiPreview


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

    suspend fun getEstateById(estateId: Int): EstateModel? {
        return repository.getEstateById(estateId)
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

    suspend fun getAfdelingById(afdelingId: Int): AfdelingModel? {
        return repository.getAfdelingById(afdelingId)
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


    suspend fun getTPHDetailsByID(tphId: Int): TPHDao.TPHDetails? {
        return try {
            repository.getTPHDetailsByID(tphId)
        } catch (e: Exception) {
            AppLogger.e("Error loading TPH details: ${e.message}")
            null
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

    fun updateOrInsertParameter(jenisTPH: List<ParameterModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateOrInsertParameter(jenisTPH)
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

    suspend fun getListOfBlok(
        idEstate: Int,
        idDivisi: Int,
    ): List<BlokModel> {
        return repository.getListOfBlok(idEstate, idDivisi)
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

    suspend fun getTphOtomatisByEstate(estateAbbr: String): Int? {
        return repository.getTphOtomatisByEstate(estateAbbr)
    }


    // Modified parseTPHJsonToList function to handle both formats
    private fun parseTPHJsonToList(jsonContent: String): List<TPHNewModel> {
        try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

            // Check if it's the direct format (success + data) - this is the regional TPH format
            if (jsonObject.has("success") && jsonObject.has("data")) {
                // Regional TPH format - direct data array without key mappings
                AppLogger.d("Using regional format with direct field names")
                val dataArray = jsonObject.getAsJsonArray("data")
                val resultList = mutableListOf<TPHNewModel>()

                dataArray.forEachIndexed { index, element ->
                    try {
                        val obj = element.asJsonObject

                        // Safe way to get values that handles null values properly
                        fun safeGetString(field: String): String? {
                            val value = obj.get(field)
                            return if (value == null || value.isJsonNull) null else value.asString
                        }

                        fun safeGetInt(field: String): Int? {
                            val value = obj.get(field)
                            return if (value == null || value.isJsonNull) null else value.asInt
                        }

                        fun safeGetDouble(field: String): String? {
                            val value = obj.get(field)
                            return if (value == null || value.isJsonNull) null else value.asDouble.toString()
                        }

                        // Direct conversion from TPH regional API response
                        val model = TPHNewModel(
                            id = safeGetInt("id"),
                            regional = safeGetString("regional")
                                ?: safeGetInt("regional")?.toString() ?: "0",
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
                            nomor = safeGetString("nomor") ?: safeGetInt("nomor")?.toString(),
                            tahun = safeGetString("tahun"),
                            luas_area = safeGetDouble("luas_area"),
                            jml_pokok = safeGetString("jml_pokok"),
                            jml_pokok_ha = safeGetString("jml_pokok_ha"),
                            lat = safeGetString("lat"),
                            lon = safeGetString("lon"),
                            update_date = safeGetString("update_date"),
                            status = safeGetString("status") ?: safeGetInt("status")?.toString(),
                            jenis_tph_id = safeGetString("jenis_tph_id")
                                ?: safeGetInt("jenis_tph_id")?.toString(),
                            limit_tph = safeGetString("limit_tph"),
                        )

                        // Log the first few items to check conversion
                        if (resultList.size < 2) {
                            AppLogger.d("Converted regional TPH item ${resultList.size + 1}: $model")
                        }

                        resultList.add(model)
                    } catch (e: Exception) {
                        AppLogger.e("Error converting regional TPH item #${index + 1}")
                        AppLogger.e("Error JSON: ${element}")
                        AppLogger.e("Error details: ${e.message}")
                        // Continue with next item
                    }
                }

                AppLogger.d("Successfully converted ${resultList.size} regional TPH items")
                return resultList
            }

            // Check if it's the original format with key mappings
            if (jsonObject.has("key")) {
                // Original format with key mappings
                AppLogger.d("Using original format with key mappings")
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
                            regional = safeGetString("regional")
                                ?: safeGetInt("regional")?.toString() ?: "0",
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

                        resultList.add(model)
                    } catch (e: Exception) {
                        AppLogger.e("Error converting item #${index + 1}")
                        AppLogger.e("Error JSON: ${element}")
                        AppLogger.e("Error details: ${e.message}")
                        // Continue with next item
                    }
                }

                return resultList
            }

            // If neither format matches, throw error
            throw Exception("Unknown JSON format - missing both 'success' and 'key' fields")

        } catch (e: Exception) {
            AppLogger.e("Error parsing TPH JSON: ${e.message}")
            AppLogger.e("JSON content: $jsonContent")
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
                    AppUtils.DatasetNames.parameter -> parseParameter(jsonContent) as List<T>
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

                AppLogger.d("masuk sini gak sih gesss")
                prefManager!!.addDataset(dataset)

                AppLogger.d("${prefManager!!.datasetMustUpdate}")
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

    suspend fun getAllTPHInBlock(
        idEstate: Int,
        idDivisi: Int,
        blokKode: String
    ): List<TPHNewModel> {
        return repository.getAllTPHInBlock(idEstate, idDivisi, blokKode)
    }

    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted

    data class ErrorItem(
        val fileName: String,
        val message: String
    )



    fun getDownloadMapList() {
        viewModelScope.launch {
            try {
                val result = downloadMapRepository.getDownloadMapList()
                _downloadMapList.postValue(result)
            } catch (e: Exception) {
                AppLogger.e("Error in getDownloadMapList: ${e.message}")
                _downloadMapList.postValue(Result.failure(e))
            }
        }
    }

    // Change this function signature
    suspend fun getDownloadMapProgress(downloadId: String): Result<DownloadMapProgressResponse> {
        return try {
            // Call repository directly and return the result
            downloadMapRepository.getDownloadMapProgress(downloadId)
        } catch (e: Exception) {
            AppLogger.e("Error in getDownloadMapProgress: ${e.message}")
            Result.failure(e)
        }
    }

    fun updateLocalUploadCMP(
        uploadData: List<Pair<String, String>>,
        jabatan: String
    ): Deferred<Boolean> {
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
                                                    panenDao.updateStatusUploadPanen(
                                                        idList,
                                                        statusCode
                                                    )
                                                    AppLogger.d("Updated panen_table successfully")
                                                }

                                                AppUtils.DatabaseTables.ESPB -> {
                                                    // Conditional update based on jabatan/role
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.Mandor1,
                                                        AppUtils.ListFeatureByRoleUser.Asisten -> {
                                                            espbDao.updateStatusUploadEspbCmpSp(
                                                                idList,
                                                                statusCode
                                                            )
                                                            AppLogger.d("Updated espb_table status_upload_cmp_sp successfully")
                                                        }

                                                        AppUtils.ListFeatureByRoleUser.KeraniTimbang -> {
                                                            espbDao.updateStatusUploadEspbCmpWb(
                                                                idList,
                                                                statusCode
                                                            )
                                                            AppLogger.d("Updated espb_table status_upload_cmp_wb successfully")
                                                        }
                                                    }
                                                }

                                                AppUtils.DatabaseTables.HEKTAR_PANEN -> {
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.MandorPanen -> {
                                                            hektarPanenDao.updateStatusUploadHektarPanen(
                                                                idList,
                                                                statusCode
                                                            )
                                                            AppLogger.d("Updated hektar_panen_table successfully")
                                                        }
                                                    }
                                                }

                                                AppUtils.DatabaseTables.ABSENSI -> {
                                                    when (jabatan) {
                                                        AppUtils.ListFeatureByRoleUser.MandorPanen -> {
                                                            absensiDao.updateStatusUploadAbsensiPanen(
                                                                idList,
                                                                statusCode
                                                            )
                                                            AppLogger.d("Updated absensi_table successfully")
                                                        }
                                                    }
                                                }

                                                AppUtils.DatabaseTables.INSPEKSI -> {
                                                    // Add inspeksi update logic
                                                    inspeksiDao.updateStatusUploadInspeksiPanen(
                                                        idList,
                                                        statusCode
                                                    )
                                                    AppLogger.d("Updated inspeksi_table successfully")
                                                }

                                                AppUtils.DatabaseTables.INSPEKSI_DETAIL -> {
                                                    // Add inspeksi detail update logic
                                                    inspeksiDao.updateStatusUploadInspeksiDetailPanen(
                                                        idList,
                                                        statusCode
                                                    )
                                                    AppLogger.d("Updated inspeksi_detail_table successfully")
                                                }

                                                AppUtils.DatabaseTables.MUTU_BUAH -> {
                                                    // Add inspeksi detail update logic
                                                    mutuBuahDao.updateStatusUploadMutuBuah(
                                                        idList,
                                                        statusCode
                                                    )
                                                    AppLogger.d("Updated mutu buah table successfully")
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

    fun downloadDataset(
        requests: List<DatasetRequest>,
        downloadItems: List<UploadCMPItem>,
        isDownloadDataset: Boolean = true
    ) {
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
                        AppUtils.DatasetNames.blok
                    )

                    var modifiedRequest = request
                    if (request.jabatan != AppUtils.ListFeatureByRoleUser.GM && request.dataset in validDatasets) {

                        val deptId: Int? = if (request.dataset == AppUtils.DatasetNames.tph) {
                            request.estate?.toString()?.toIntOrNull()  // Returns Int?
                        } else {
                            null  // Returns Int? (null)
                        }

                        if (deptId != null || request.dataset != AppUtils.DatasetNames.tph) {
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
                        } else {
                            AppLogger.d("Invalid estate ID for tph dataset: ${request.estate}")
                        }
                    }

                    AppLogger.d("Downloading dataset for ${modifiedRequest.estateAbbr}/ ${modifiedRequest.dataset}")

                    var response: Response<ResponseBody>? = null
                    if (request.dataset == AppUtils.DatasetNames.mill) {
                        response = repository.downloadSmallDataset(request.regional ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.estate) {
                        response = repository.downloadListEstate(request.regional ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiRestan) {
                        // Skip restan data for askep and manager users since they can have multiple afdelings
                        if (
                            request.jabatan != AppUtils.ListFeatureByRoleUser.ASKEP &&
                            request.jabatan != AppUtils.ListFeatureByRoleUser.Manager
                        ) {

                            response = restanRepository.getDataRestan(
                                request.estate.toString().toInt(),
                                request.afdeling.toString()
                            )
                        } else {
                            progressMap[itemId] = 100
                            statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                            errorMap[itemId] = "Skipped for multiple afdeling users"

                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())
                            _itemErrorMap.postValue(errorMap.toMap())

                            incrementCompletedCount()
                            continue // Skip to next item
                        }
                    } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataUser) {
                            response = syncDataUserRepository.getDataUser(request.idUser ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataWajahPemanen) {
                        val syncResult = withContext(Dispatchers.IO) {
                            pemanenFaceRepository.syncFromServer()
                        }

                        progressMap[itemId] = 100
                        statusMap[itemId] = if (syncResult.success) {
                            AppUtils.UploadStatusUtils.SUCCESS
                        } else {
                            AppUtils.UploadStatusUtils.FAILED
                        }
                        errorMap[itemId] = if (syncResult.success) {
                            syncResult.message
                        } else {
                            syncResult.message
                        }

                        _itemProgressMap.postValue(progressMap.toMap())
                        _itemStatusMap.postValue(statusMap.toMap())
                        _itemErrorMap.postValue(errorMap.toMap())

                        incrementCompletedCount()
                        continue
                    } else if (request.dataset == AppUtils.DatasetNames.checkAppVersion) {
                        response = versioningAppRepository.getDataAppVersion(request.idUser ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataPanen) {
                        val estateId = when (request.estate) {
                            is Int -> request.estate
                            is String -> request.estate
                            is List<*> -> (request.estate as List<Int>).first() // Take first estate for now
                            else -> request.estate as Int
                        }

                        response = dataPanenInspectionRepository.getDataPanen(
                            estateId!!,
                        )

                    } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi) {
                        val estateId = when (request.estate) {
                            is Int -> request.estate
                            is String -> request.estate
                            is List<*> -> (request.estate as List<Int>).first() // Take first estate for now
                            else -> request.estate as Int
                        }

                        response = dataPanenInspectionRepository.getDataInspeksi(
                            estateId!!,
                            true,
                            parameterDao
                        )
                    } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                        response = repository.downloadSettingJson(request.lastModified ?: "")
                    } else if (request.dataset == AppUtils.DatasetNames.parameter) {
                        response = repository.getParameter()
                    } else if (request.dataset == AppUtils.DatasetNames.hektaran) {
                        val estateId: Int = when (val estate = request.estate) {
                            is Int -> estate
                            is String -> estate.toIntOrNull() ?: 0
                            is List<*> -> (estate.firstOrNull() as? Int) ?: 0
                            else -> 0
                        }

                        val afdelingId: Int = when (val estate = request.afdeling) {
                            is Int -> estate
                            is String -> estate.toIntOrNull() ?: 0
                            is List<*> -> (estate.firstOrNull() as? Int) ?: 0
                            else -> 0
                        }
                        response =
                            hektarPanenRepository.getDataHektaranHektarDetail(estateId, afdelingId)

                    } else if (request.downloadIdMap != null && request.totalChunks != null) {
                        // Handle offline map download
                        try {
                            // Use fields directly - no need to parse data
                            val downloadId = request.downloadIdMap!!
                            val totalChunks = request.totalChunks!!

                            AppLogger.d("Starting offline map download - ID: $downloadId, Chunks: $totalChunks")

                            // Create map offline directory
                            val mapOfflineDir = File(
                                getApplication<Application>().getExternalFilesDir(null),
                                "map_offline/${request.estateAbbr}"
                            )

                            if (!mapOfflineDir.exists()) {
                                mapOfflineDir.mkdirs()
                            }

                            AppLogger.d("Map offline directory: ${mapOfflineDir.absolutePath}")

                            // Download each chunk with progress
                            var downloadedChunks = 0
                            val chunkFiles = mutableListOf<File>()

                            for (chunkIndex in 0 until totalChunks) {
                                try {
                                    // Calculate progress (0-100%)
                                    val progress =
                                        ((chunkIndex.toFloat() / totalChunks) * 100).toInt()
                                    progressMap[itemId] = progress
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                                    _itemProgressMap.postValue(progressMap.toMap())
                                    _itemStatusMap.postValue(statusMap.toMap())

                                    // Log with 1-based indexing for readability (chunk 1 of 10)
                                    AppLogger.d("Downloading chunk ${chunkIndex + 1}/$totalChunks (${progress}%)")

                                    // Download chunk using 0-based index for API
                                    val chunkResponse = downloadMapRepository.downloadMapChunk(
                                        downloadId,
                                        chunkIndex
                                    )

                                    if (chunkResponse.isSuccessful) {
                                        val chunkFile = File(
                                            mapOfflineDir,
                                            "chunk_${String.format("%03d", chunkIndex)}.bin"
                                        )

                                        // Save chunk to file
                                        withContext(Dispatchers.IO) {
                                            chunkResponse.body()?.byteStream()?.use { input ->
                                                chunkFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        }

                                        chunkFiles.add(chunkFile)
                                        downloadedChunks++

                                        AppLogger.d("Chunk ${chunkIndex + 1}/$totalChunks saved: ${chunkFile.name} (${chunkFile.length()} bytes)")
                                    } else {
                                        throw Exception("Failed to download chunk ${chunkIndex + 1}/$totalChunks: ${chunkResponse.code()}")
                                    }

                                } catch (e: Exception) {
                                    AppLogger.e("Error downloading chunk ${chunkIndex + 1}/$totalChunks: ${e.message}")
                                    throw e
                                }
                            }

                            // All chunks downloaded successfully
                            if (downloadedChunks == totalChunks) {
                                progressMap[itemId] = 100
                                statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADED
                                errorMap[itemId] = null

                                AppLogger.d("Map download completed - ${downloadedChunks} chunks saved to ${mapOfflineDir.absolutePath}")

                                // Save metadata
                                val metadataFile = File(mapOfflineDir, "metadata.json")
                                val metadata = JSONObject().apply {
                                    put("downloadId", downloadId)
                                    put("estateAbbr", request.estateAbbr)
                                    put("totalChunks", totalChunks)
                                    put("downloadedAt", System.currentTimeMillis())
                                }

                                withContext(Dispatchers.IO) {
                                    metadataFile.writeText(metadata.toString())
                                }

                            } else {
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] =
                                    "Downloaded $downloadedChunks/$totalChunks chunks"
                            }

                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())
                            _itemErrorMap.postValue(errorMap.toMap())

                            incrementCompletedCount()
                            continue // Skip normal response processing

                        } catch (e: Exception) {
                            AppLogger.e("Error in offline map download: ${e.message}")
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Map download error: ${e.message}"
                            _itemStatusMap.postValue(statusMap.toMap())
                            _itemErrorMap.postValue(errorMap.toMap())
                            incrementCompletedCount()
                            continue
                        }
                    } else if (request.dataset == AppUtils.DatasetNames.tph && request.estate is List<*>) {
                        val estateId = request.estate as List<*>
                        val allTphData = mutableListOf<TPHNewModel>()
                        var lastSuccessResponse: Response<ResponseBody>? = null

                        val totalEstates = estateId.size
                        var processedEstates = 0

                        estateId.forEach { estate ->
                            try {
                                // Calculate progress based on estate processing (0-90% for API calls)
                                val apiProgress =
                                    ((processedEstates.toFloat() / totalEstates) * 90).toInt()
                                progressMap[itemId] = apiProgress
                                statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                                _itemProgressMap.postValue(progressMap.toMap())
                                _itemStatusMap.postValue(statusMap.toMap())

                                AppLogger.d("Processing estate ${processedEstates + 1}/$totalEstates: $estate (${apiProgress}%)")

                                val estateResponse =
                                    repository.getTPHEstate(estate.toString(), false)
                                AppLogger.d("responseBody $estateResponse for estate $estate")

                                if (estateResponse.isSuccessful && estateResponse.code() == 200) {
                                    lastSuccessResponse = estateResponse
                                    val responseBody = estateResponse.body()?.string() ?: ""
                                    if (responseBody.isNotBlank()) {
                                        val tphList = parseTPHJsonToList(responseBody)
                                        allTphData.addAll(tphList)
                                        AppLogger.d("Estate $estate: Added ${tphList.size} TPH records. Total: ${allTphData.size}")
                                    }
                                } else {
                                    AppLogger.w("Estate $estate failed with code: ${estateResponse.code()}")
                                }

                                processedEstates++
                                prefManager.addDataset(request.dataset)
                                // Update progress after each estate
                                val currentProgress =
                                    ((processedEstates.toFloat() / totalEstates) * 90).toInt()
                                progressMap[itemId] = currentProgress
                                _itemProgressMap.postValue(progressMap.toMap())

                            } catch (e: Exception) {
                                AppLogger.e("Error getting TPH data for estate ${estate}: ${e.message}")
                                processedEstates++
                            }
                        }

                        // Check if we have any data
                        if (allTphData.isNotEmpty()) {
                            AppLogger.d("Starting database storage for ${allTphData.size} TPH records")

                            // Set progress to 90% and update status to storing
                            progressMap[itemId] = 90
                            statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())

                            // Database storage progress (90-100%)
                            progressMap[itemId] = 95
                            _itemProgressMap.postValue(progressMap.toMap())

                            withContext(Dispatchers.IO) {
                                try {
                                    if (request.isDownloadMasterTPHAsistensi) {
                                        repository.insertTPH(allTphData)
                                    } else {
                                        repository.updateOrInsertTPH(allTphData)
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Database update error: ${e.message}")
                                    throw e
                                }
                            }

                            // Set final progress and success status
                            progressMap[itemId] = 100
                            statusMap[itemId] = if (isDownloadDataset) {
                                AppUtils.UploadStatusUtils.DOWNLOADED
                            } else {
                                AppUtils.UploadStatusUtils.UPDATED
                            }

                            // Set lastModified to current datetime
                            val currentDateTime = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())
                            prefManager.lastModifiedDatasetTPH = currentDateTime

                            AppLogger.d("TPH list download completed successfully - ${allTphData.size} records from ${estateId.size} estates")
                        } else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "No TPH data found for any estate"
                            progressMap[itemId] = 0
                        }

                        _itemProgressMap.postValue(progressMap.toMap())
                        _itemStatusMap.postValue(statusMap.toMap())
                        _itemErrorMap.postValue(errorMap.toMap())

                        incrementCompletedCount()
                        continue // Skip the normal response processing since we handled everything here
                    } else if (request.dataset == AppUtils.DatasetNames.tph && request.regional != null) {
                        AppLogger.d("Starting TPH regional download for ${request.estateAbbr}")

                        // Get all estates first
                        val estatesResult = repository.getAllEstates()
                        if (estatesResult.isSuccess) {
                            val estates = estatesResult.getOrNull() ?: emptyList()
                            val allTphData = mutableListOf<TPHNewModel>()
                            var lastSuccessResponse: Response<ResponseBody>? = null
                            val totalEstates = estates.size
                            var processedEstates = 0

                            AppLogger.d("Processing TPH data for ${totalEstates} estates")

                            // Set initial progress
                            progressMap[itemId] = 0
                            statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())

                            estates.forEachIndexed { index, estate ->
                                estate.abbr?.let { estateAbbr ->
                                    try {
                                        // Calculate progress based on estate processing (0-90% for API calls, 90-100% for database)
                                        val apiProgress =
                                            ((processedEstates.toFloat() / totalEstates) * 90).toInt()
                                        progressMap[itemId] = apiProgress
                                        statusMap[itemId] = AppUtils.UploadStatusUtils.DOWNLOADING
                                        _itemProgressMap.postValue(progressMap.toMap())
                                        _itemStatusMap.postValue(statusMap.toMap())

                                        AppLogger.d("Processing estate ${processedEstates + 1}/$totalEstates: $estateAbbr (${apiProgress}%)")

                                        val estateResponse = repository.getTPHEstate(estateAbbr)

                                        if (estateResponse.isSuccessful && estateResponse.code() == 200) {
                                            lastSuccessResponse = estateResponse
                                            val responseBody = estateResponse.body()?.string() ?: ""
                                            if (responseBody.isNotBlank()) {
                                                val tphList = parseTPHJsonToList(responseBody)
                                                allTphData.addAll(tphList)
                                                AppLogger.d("Estate $estateAbbr: Added ${tphList.size} TPH records. Total: ${allTphData.size}")
                                            }
                                        } else {
                                            AppLogger.w("Estate $estateAbbr failed with code: ${estateResponse.code()}")
                                        }

                                        processedEstates++

                                        // Update progress after each estate
                                        val currentProgress =
                                            ((processedEstates.toFloat() / totalEstates) * 90).toInt()
                                        progressMap[itemId] = currentProgress
                                        _itemProgressMap.postValue(progressMap.toMap())

                                    } catch (e: Exception) {
                                        AppLogger.e("Error getting TPH data for estate ${estateAbbr}: ${e.message}")
                                        processedEstates++
                                    }
                                } ?: run {
                                    AppLogger.w("Estate abbr is null for estate: ${estate.id}")
                                    processedEstates++
                                }
                            }

                            // Set progress to 90% and update status to storing
                            progressMap[itemId] = 90
                            statusMap[itemId] =
                                AppUtils.UploadStatusUtils.DOWNLOADING // or create a new status for storing
                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())

                            // Check if we have any data
                            if (allTphData.isNotEmpty()) {
                                AppLogger.d("Starting database storage for ${allTphData.size} TPH records")

                                // Database storage progress (90-100%)
                                progressMap[itemId] = 95
                                _itemProgressMap.postValue(progressMap.toMap())

                                withContext(Dispatchers.IO) {
                                    try {
                                        if (request.isDownloadMasterTPHAsistensi) {
                                            repository.insertTPH(allTphData)
                                        } else {
                                            repository.updateOrInsertTPH(allTphData)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Database update error: ${e.message}")
                                        throw e
                                    }
                                }

                                // Set final progress and success status
                                progressMap[itemId] = 100
                                statusMap[itemId] = if (isDownloadDataset) {
                                    AppUtils.UploadStatusUtils.DOWNLOADED
                                } else {
                                    AppUtils.UploadStatusUtils.UPDATED
                                }

                                // Set lastModified to current datetime
                                val currentDateTime = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())
                                prefManager.lastModifiedDatasetTPH = currentDateTime
                                prefManager.addDataset(request.dataset)
                                AppLogger.d("TPH regional download completed successfully - ${allTphData.size} records from $totalEstates estates")
                            } else {
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "No TPH data found for any estate"
                                progressMap[itemId] = 0
                            }

                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())
                            _itemErrorMap.postValue(errorMap.toMap())

                            // Continue to next item (don't process normal response since we handled everything here)
                            incrementCompletedCount()
                            continue
                        } else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] =
                                "Failed to get estates: ${estatesResult.exceptionOrNull()?.message}"
                            progressMap[itemId] = 0
                            _itemProgressMap.postValue(progressMap.toMap())
                            _itemStatusMap.postValue(statusMap.toMap())
                            _itemErrorMap.postValue(errorMap.toMap())
                            incrementCompletedCount()
                            continue
                        }
                    } else {
                        AppLogger.d("modifiedRequest $modifiedRequest")
                        response = repository.downloadDataset(modifiedRequest)
                    }


                    AppLogger.d("")

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
                                            val progress =
                                                (downloadedBytes * 100 / contentLength).toInt()
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
                                        val tphList =
                                            parseTPHJsonToList(jsonContent) as List<TPHNewModel>
                                        val filteredTphList =
                                            tphList.filterNot { it.isCompletelyNull() }

                                        AppLogger.d("Parsed ${tphList.size} TPH records, ${filteredTphList.size} valid after filtering")

                                        AppLogger.d("filteredTphList $filteredTphList")
                                        if (filteredTphList.isEmpty()) {
                                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                            errorMap[itemId] =
                                                "All TPH records are empty or invalid"
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

                                                    val uniqueDepts =
                                                        filteredTphList.map { it.dept }.distinct()
                                                    AppLogger.d("TPH Update - Unique department IDs in data: $uniqueDepts")

                                                    val dataToUse = if (deptId != null) {
                                                        val tphForDept =
                                                            filteredTphList.filter { it.dept.toString() == deptId.toString() }
                                                        AppLogger.d("TPH Update - Filtered ${tphForDept.size} records for department $deptId")

                                                        // If no records match, log a few sample records for debugging
                                                        if (tphForDept.isEmpty() && filteredTphList.isNotEmpty()) {
                                                            AppLogger.d("TPH Update - No matches found. Sample records:")
                                                        }

                                                        tphForDept
                                                    } else {
                                                        AppLogger.d("TPH Update - No department ID specified, updating all ${filteredTphList.size} records")
                                                        filteredTphList
                                                    }

                                                    // Only call repository if we have data
                                                    if (dataToUse.isNotEmpty()) {
                                                        AppLogger.d("TPH Update - Calling repository.updateOrInsertTPH with ${dataToUse.size} records")
                                                        repository.updateOrInsertTPH(dataToUse)
                                                    } else {
                                                        AppLogger.w("TPH Update - No data to update/insert for department $deptId")
                                                    }
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

                                        // Check if dataset is in datasetMustUpdate and add it

                                        prefManager.addDataset(request.dataset)


                                        processed = true
                                    }

                                    AppUtils.DatasetNames.kemandoran -> {
                                        val kemandoranList = parseStructuredJsonToList(
                                            jsonContent,
                                            KemandoranModel::class.java
                                        )
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

                                        prefManager.addDataset(request.dataset)

                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")

                                        processed = true
                                    }

                                    AppUtils.DatasetNames.pemanen -> {
                                        val karyawanList = parseStructuredJsonToList(
                                            jsonContent,
                                            KaryawanModel::class.java
                                        )
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

                                        prefManager.addDataset(request.dataset)
                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")
                                        processed = true
                                    }

                                    AppUtils.DatasetNames.blok -> {
                                        val blokList = parseStructuredJsonToList(
                                            jsonContent,
                                            BlokModel::class.java
                                        )
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

                                        prefManager.addDataset(request.dataset)
                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")
                                        processed = true
                                    }

                                    AppUtils.DatasetNames.transporter -> {
                                        val transporterList = parseStructuredJsonToList(
                                            jsonContent,
                                            TransporterModel::class.java
                                        )
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
                                            prefManager.lastModifiedDatasetTransporter =
                                                lastModified
                                        }

                                        prefManager.addDataset(request.dataset)
                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")
                                        processed = true
                                    }

                                    AppUtils.DatasetNames.kendaraan -> {
                                        val kendaraanList = parseStructuredJsonToList(
                                            jsonContent,
                                            KendaraanModel::class.java
                                        )
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

                                        prefManager.addDataset(request.dataset)
                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")
                                        processed = true
                                    }

                                    AppUtils.DatasetNames.jenisTPH -> {
                                        val jenisTPHList = parseStructuredJsonToList(
                                            jsonContent,
                                            JenisTPHModel::class.java
                                        )
                                        AppLogger.d("Parsed ${jenisTPHList.size} jenis TPH records, starting database update")

                                        withContext(Dispatchers.IO) {
                                            try {
                                                repository.updateOrInsertJenisTPH(jenisTPHList)
                                                AppLogger.d("Database update completed for jenisTPH dataset")
                                            } catch (e: Exception) {
                                                AppLogger.e("Database update error for jenisTPH: ${e.message}")
                                                throw e
                                            }
                                        }

                                        if (lastModified != null) {
                                            prefManager.lastModifiedDatasetJenisTPH = lastModified
                                        }

                                        prefManager.addDataset(request.dataset)
                                        AppLogger.d("${prefManager.datasetMustUpdate}")
                                        AppLogger.d("${request.dataset}")
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
                                    errorMap[itemId] =
                                        "No processor found for dataset: ${request.dataset}"
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

                            AppLogger.d("tipe json bro ")
                            handleJsonResponse(
                                request,
                                itemId,
                                response,
                                statusMap,
                                errorMap,
                                progressMap,
                                lastModified,
                                lastModifiedSettingsJson,
                                isDownloadDataset
                            )
                        } else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Unsupported content type: $contentType"
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()

                        AppLogger.d("API Error response: $errorBody")
                        var errorMessage = "API Error: ${response.code()}"

                        if (!errorBody.isNullOrEmpty()) {
                            try {
                                // If error is JSON, parse it
                                val jsonError = JSONObject(errorBody)

                                errorMessage = when {
                                    jsonError.has("message") -> "API Error ${response.code()}: ${
                                        jsonError.getString(
                                            "message"
                                        )
                                    }"

                                    jsonError.has("error") -> "API Error ${response.code()}: ${
                                        jsonError.getString(
                                            "error"
                                        )
                                    }"

                                    else -> "API Error ${response.code()}: $errorBody"
                                }

                                // If both message and error are available, combine them for more detail
                                if (jsonError.has("message") && jsonError.has("error")) {
                                    errorMessage =
                                        "API Error ${response.code()}: ${jsonError.getString("message")} - Details: ${
                                            jsonError.getString("error")
                                        }"
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

    fun getPreviewDataFollowUpInspeksiWeek(estate: Any, afdeling: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // ✅ Pass parameterDao to the repository method

                    AppLogger.d("estate salkjfksdf $estate")
                    dataPanenInspectionRepository.getDataInspeksi(
                        estate = estate,
                        joinTable = false,
                        parameterDao = parameterDao
                    )
                }

                AppLogger.d(response.body().toString())
                if (response.isSuccessful && response.code() == 200) {
                    val jsonString = response.body()?.string() ?: ""
                    AppLogger.d(jsonString.toString())


                    val formattedData = processPreviewDataInspeksi(jsonString,estate)
                    _followUpInspeksiPreview.value = formattedData
                } else {
                    _followUpInspeksiPreview.value = "Gagal memuat data: ${response.message()}"
                }
            } catch (e: Exception) {
                AppLogger.e("Error loading Follow-up Inspeksi preview: ${e.message}")
                _followUpInspeksiPreview.value = "Error: ${e.message}"
            }
        }
    }

    fun getPreviewDataPanenInspeksiWeek(estate: Any) {
        viewModelScope.launch {
            try {
                AppLogger.d("estate $estate")
                val response = withContext(Dispatchers.IO) {
                    dataPanenInspectionRepository.getDataPanen(estate)
                }

                AppLogger.d(response.body().toString())
                if (response.isSuccessful && response.code() == 200) {
                    val jsonString = response.body()?.string() ?: ""

                    AppLogger.d(jsonString.toString())
                    // Process the JSON response - pass estate to know if grouping is needed
                    val formattedData = processPreviewDataPanenInspeksi(jsonString, estate)

                    _dataPanenInspeksiPreview.value = formattedData
                } else {
                    _dataPanenInspeksiPreview.value = "Gagal memuat data: ${response.message()}"
                }
            } catch (e: Exception) {
                AppLogger.e("Error loading Restan preview: ${e.message}")
                _dataPanenInspeksiPreview.value = "Error: ${e.message}"
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
        progressMap[itemId] = 45
        _itemProgressMap.postValue(progressMap.toMap())

        // Check if it's an "up to date" response
        if (responseBodyString.contains("\"success\":false") &&
            responseBodyString.contains("\"message\":\"Dataset is up to date\"")
        ) {
            progressMap[itemId] = 100
            statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
            _itemProgressMap.postValue(progressMap.toMap())
            prefManager.addDataset(request.dataset)
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
                                    responseBodyString.contains("\"message\":\"Dataset is up to date\""))
                        ) {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                            AppLogger.d("${AppUtils.DatasetNames.settingJSON} are up to date")
                        } else {
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "No valid settings found in response"
                        }
                    }

                    if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                        prefManager.addDataset(request.dataset)
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
                            if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                prefManager.addDataset(request.dataset)
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

                            if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                prefManager.addDataset(request.dataset)
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

            AppUtils.DatasetNames.parameter -> {
                try {
                    // Parsing - update to 60%
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    val parameterList = parseParameter(responseBodyString)
                    AppLogger.d("Parsed ${parameterList.size} parameter records")

                    // Update to 75% before database operations
                    progressMap[itemId] = 75
                    _itemProgressMap.postValue(progressMap.toMap())

                    withContext(Dispatchers.IO) {
                        try {
                            repository.updateOrInsertParameter(parameterList)
                            AppLogger.d("Successfully stored parameter data")

                            // Final update - 100%
                            progressMap[itemId] = 100
                            _itemProgressMap.postValue(progressMap.toMap())

                            statusMap[itemId] = if (isDownloadDataset) {
                                AppUtils.UploadStatusUtils.DOWNLOADED
                            } else {
                                AppUtils.UploadStatusUtils.UPDATED
                            }

                            if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                prefManager.addDataset(request.dataset)
                            }

                        } catch (e: Exception) {
                            progressMap[itemId] = 100  // Still show 100% even on error
                            _itemProgressMap.postValue(progressMap.toMap())

                            AppLogger.e("Error storing parameter data: ${e.message}")
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "Error storing parameter data: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error parsing parameter data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error parsing parameter data: ${e.message}"
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

                        // Update to 70% before processing records
                        progressMap[itemId] = 70
                        _itemProgressMap.postValue(progressMap.toMap())

                        // Add this debugging code in your processing loop
                        var status0Count = 0
                        var status1Count = 0
                        var status2Count = 0
                        var status0WithNullSpb = 0
                        var status0WithNonNullSpb = 0

                        var status1WithNullSpb = 0
                        var status2WithNullSpb = 0
                        var status1WithNonNullSpb = 0
                        var status2WithNonNullSpb = 0

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)

                            // Extract required fields
                            val tphId = item.optString("tph", "")
                            val createdDate = item.optString("created_date_kp", "")
                            val statusEspb = item.optInt("status_espb", -1)
                            val jjgKirim = item.optInt("jjg_kirim", 0)
                            val nomorPemanen = item.optInt("nomor_pemanen", 0)
                            val ancak = item.optInt("ancak", 0)
                            val createdName = item.optString("created_name_kp", "")
                            val spbKode: String? =
                                if (item.has("spb_kode") && !item.isNull("spb_kode")) {
                                    item.optString("spb_kode")
                                } else {
                                    null
                                }

                            val username = if (createdName.isNullOrEmpty() || createdName.equals(
                                    "NULL",
                                    ignoreCase = true
                                )
                            ) {
                                ""
                            } else {
                                extractUsernameFromCreatedName(createdName)
                            }

                            when (statusEspb) {
                                0 -> {
                                    status0Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status0WithNullSpb++
                                    } else {
                                        status0WithNonNullSpb++
                                    }
                                }

                                1 -> {
                                    status1Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status1WithNullSpb++
                                    } else {
                                        status1WithNonNullSpb++
                                    }
                                }

                                2 -> {
                                    status2Count++
                                    if (spbKode.isNullOrEmpty()) {
                                        status2WithNullSpb++
                                    } else {
                                        status2WithNonNullSpb++
                                    }
                                }
                            }

                            // ADD ALL RECORDS TO PANEN LIST - regardless of status or spb_kode
//                            AppLogger.d("Creating entity for insert/update: tphId=$tphId, date=$createdDate, statusEspb=$statusEspb, spbKode=$spbKode")
//                            val jjgJson = "{\"KP\": $jjgKirim}"

                            // Create a PanenEntity with the required fields
                            val panenEntity = PanenEntity(
                                tph_id = tphId.toString(),
                                date_created = createdDate,
                                created_name = createdName,
                                created_by = 0,
                                karyawan_id = "",
                                kemandoran_id = "",
                                karyawan_nik = "",
                                karyawan_nama = "",
                                jjg_json =  JSONObject().put("KP", jjgKirim).toString(),
                                foto = "",
                                komentar = "",
                                asistensi = 0,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = 0,
                                ancak = ancak,
                                info = "",
                                archive = 0,
                                nomor_pemanen = nomorPemanen,
                                status_banjir = 0,
                                status_espb = statusEspb,
                                status_restan = 1,
                                scan_status = 1,
                                dataIsZipped = 0,
                                no_espb = spbKode ?: "",
                                username = username,
                                status_upload = 0,
                                status_uploaded_image = "0",
                                status_pengangkutan = 0,
                                status_insert_mpanen = 0,
                                status_scan_mpanen = 0,
                                jumlah_pemanen = 0,
                                archive_mpanen = 0,
                                isPushedToServer = 1
                            )

                            panenList.add(panenEntity)
                        }


                        // Update to 80% when processing is complete
                        progressMap[itemId] = 80
                        _itemProgressMap.postValue(progressMap.toMap())

                        withContext(Dispatchers.IO) {
                            try {
                                var successCount = 0
                                var failCount = 0

                                if (panenList.isNotEmpty()) {
                                    AppLogger.d("Processing ${panenList.size} records for insert/update...")

                                    for (panen in panenList) {
                                        try {
                                            // Use the same pattern as sinkronisasi data panen
                                            val existingRecord = panenDao.findByTphAndDate(
                                                panen.tph_id,
                                                panen.date_created
                                            )

                                            if (existingRecord == null) {
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
                                                AppLogger.d("Updating record: ${panen.tph_id}, ${panen.date_created}")

                                                val incomingKp = extractKp(panen.jjg_json)

                                                val updatedRecord = existingRecord.copy(
                                                    jjg_json = run {
                                                        val existingJson = existingRecord.jjg_json
                                                        val existingKp = extractKp(existingJson)

                                                        when {
                                                            existingJson.isNullOrEmpty() || existingJson == "NULL" -> {
                                                                panen.jjg_json
                                                            }

                                                            existingKp == incomingKp -> {
                                                                existingJson
                                                            }

                                                            else -> {
                                                                JSONObject(existingJson)
                                                                    .put("KP", incomingKp)
                                                                    .toString()
                                                            }
                                                        }
                                                    },


                                                    no_espb = panen.no_espb,

                                                    username = if (existingRecord.username.isNullOrEmpty() || existingRecord.username == "NULL") {
                                                        panen.username
                                                    } else {
                                                        existingRecord.username
                                                    },

                                                    created_name = if (existingRecord.created_name.isNullOrEmpty() || existingRecord.created_name == "NULL") {
                                                        panen.created_name
                                                    } else {
                                                        existingRecord.created_name
                                                    },

                                                    nomor_pemanen = if (existingRecord.nomor_pemanen == 0) {
                                                        panen.nomor_pemanen
                                                    } else {
                                                        existingRecord.nomor_pemanen
                                                    },

                                                    ancak = panen.ancak,

                                                    status_espb = if (existingRecord.status_espb == 0) {
                                                        panen.status_espb
                                                    } else {
                                                        existingRecord.status_espb
                                                    },

                                                    scan_status = if (existingRecord.scan_status == 0) {
                                                        panen.scan_status
                                                    } else {
                                                        existingRecord.scan_status
                                                    },

                                                    isPushedToServer = 1
                                                )


                                                panenDao.update(listOf(updatedRecord))
                                                successCount++
                                                AppLogger.d("Updated existing record: ${panen.tph_id}, ${panen.date_created} - updated jjg_json, no_espb, username, status_espb")

                                            }

                                            if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                                prefManager.addDataset(request.dataset)
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
                                AppLogger.d("Failed: $failCount")
                                AppLogger.d("Total records processed: ${panenList.size}")
                                AppLogger.d("============================")

                                // Final update - 100%
                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())

                                if (panenList.isEmpty()) {
                                    // No records to process - everything is up to date
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                                    AppLogger.d("No records to process - dataset is up to date")
                                } else if (failCount == 0) {
                                    statusMap[itemId] = if (isDownloadDataset) {
                                        AppUtils.UploadStatusUtils.DOWNLOADED
                                    } else {
                                        AppUtils.UploadStatusUtils.UPDATED
                                    }
                                } else if (successCount > 0) {
                                    // Partial success
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPDATED
                                    errorMap[itemId] =
                                        "Partial success: $failCount/${panenList.size} records failed"
                                } else {
                                    // Complete failure
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                    errorMap[itemId] =
                                        "Failed to process restan data: $failCount/${panenList.size} records failed"
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

            AppUtils.DatasetNames.sinkronisasiDataUser -> {
                try {
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    val jsonObject = JSONObject(responseBodyString)
                    AppLogger.d(jsonObject.toString())

                    if (jsonObject.optBoolean("success", false)) {
                        val dataArray = jsonObject.optJSONArray("data")

                        if (dataArray != null && dataArray.length() > 0) {
                            progressMap[itemId] = 75
                            _itemProgressMap.postValue(progressMap.toMap())

                            val userData = dataArray.getJSONObject(0)
                            val username = userData.optString("username", "")
                            val nama = userData.optString("nama", "")
                            val jabatan = userData.optString("jabatan", "")

                            // Kemandoran kode
                            val kemandoranDataObject = userData.optJSONObject("kemandoranData")
                            var kemandoranKode = ""
                            if (kemandoranDataObject != null) {
                                kemandoranKode = kemandoranDataObject.optString("kode", "")
                            }

                            // userOrg
                            val userOrgArray = userData.optJSONArray("userOrg")
                            var dept = ""
                            var divisi = ""
                            if (userOrgArray != null && userOrgArray.length() > 0) {
                                val userOrg = userOrgArray.getJSONObject(0)
                                dept = userOrg.optString("dept", "")
                                divisi = userOrg.optString("divisi", "")
                            }

                            val deptsArray = userData.optJSONArray("Depts")
                            var regional = ""
                            var company = 0
                            var companyAbbr = ""
                            var companyNama = ""
                            var estateAbbr = ""
                            var estateNama = ""

                            if (deptsArray != null && deptsArray.length() > 0) {
                                val estateAbbrList = mutableListOf<String>()
                                val estateNamaList = mutableListOf<String>()
                                val regionalList = mutableListOf<String>()
                                val companyList = mutableListOf<Int>()
                                val companyAbbrList = mutableListOf<String>()
                                val companyNamaList = mutableListOf<String>()

                                for (i in 0 until deptsArray.length()) {
                                    val deptData = deptsArray.getJSONObject(i)

                                    val abbr = deptData.optString("abbr", "")
                                    val nama = deptData.optString("nama", "")
                                    val reg = deptData.optString("regional", "")
                                    val comp = deptData.optInt("company", 0)
                                    val compAbbr = deptData.optString("company_abbr", "")
                                    val compNama = deptData.optString("company_nama", "")

                                    if (abbr.isNotEmpty()) estateAbbrList.add(abbr)
                                    if (nama.isNotEmpty()) estateNamaList.add(nama)
                                    if (reg.isNotEmpty()) regionalList.add(reg)
                                    if (comp > 0) companyList.add(comp)
                                    if (compAbbr.isNotEmpty()) companyAbbrList.add(compAbbr)
                                    if (compNama.isNotEmpty()) companyNamaList.add(compNama)
                                }

                                estateAbbr = estateAbbrList.joinToString(",")
                                estateNama = estateNamaList.joinToString(",")
                                regional = regionalList.distinct().joinToString(",")
                                company = companyList.firstOrNull() ?: 0
                                companyAbbr = companyAbbrList.distinct().joinToString(",")
                                companyNama = companyNamaList.distinct().joinToString(",")

                                AppLogger.d("🏡 Combined Estates -> Abbr: $estateAbbr | Nama: $estateNama")
                                AppLogger.d("🌍 Regional: $regional | 🏢 Company: $companyAbbr ($companyNama)")
                            }



                            progressMap[itemId] = 85
                            _itemProgressMap.postValue(progressMap.toMap())

                            val kemandoran = userData.optInt("kemandoran", 0)
                            val kemandoranPpro = userData.optInt("kemandoran_ppro", 0)
                            val kemandoranNama = userData.optString("kemandoran_nama", "")

                            try {


                                prefManager.apply {
                                    nameUserLogin = nama
                                    jabatanUserLogin = jabatan
                                    estateUserLogin = estateAbbr
                                    estateUserLengkapLogin = estateNama
                                    estateIdUserLogin = dept
                                    regionalIdUserLogin = regional
                                    companyIdUserLogin = company.toString()
                                    companyAbbrUserLogin = companyAbbr
                                    companyNamaUserLogin = companyNama
                                    afdelingIdUserLogin = divisi

                                    if (kemandoranPpro > 0) {
                                        kemandoranPPROUserLogin = kemandoranPpro.toString()
                                        kemandoranUserLogin = kemandoranPpro.toString()
                                        kemandoranNamaUserLogin = kemandoranNama
                                        kemandoranKodeUserLogin = kemandoranKode
                                        AppLogger.d("✅ Saved kemandoran data - PPRO: $kemandoranPpro, Kode: $kemandoranKode")
                                    } else {
                                        kemandoranPPROUserLogin = ""
                                        kemandoranUserLogin = ""
                                        kemandoranNamaUserLogin = ""
                                        kemandoranKodeUserLogin = ""
                                        AppLogger.d("🚫 Cleared kemandoran data (no valid PPRO)")
                                    }
                                }

                                progressMap[itemId] = 95
                                _itemProgressMap.postValue(progressMap.toMap())

                                AppLogger.d("✅ Successfully updated user data in preferences")

                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())

                                statusMap[itemId] = if (isDownloadDataset) {
                                    AppUtils.UploadStatusUtils.DOWNLOADED
                                } else {
                                    AppUtils.UploadStatusUtils.UPDATED
                                }
                                if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                    prefManager.addDataset(request.dataset)
                                }

                            } catch (prefException: Exception) {
                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())

                                AppLogger.e("❌ Error updating preferences: ${prefException.message}")
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] =
                                    "Error updating preferences: ${prefException.message}"
                            }

                        } else {
                            progressMap[itemId] = 100
                            _itemProgressMap.postValue(progressMap.toMap())

                            AppLogger.e("❌ No user data found in response")
                            statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                            errorMap[itemId] = "No user data found in response"
                        }
                    } else {
                        progressMap[itemId] = 100
                        _itemProgressMap.postValue(progressMap.toMap())

                        val message = jsonObject.optString("message", "Unknown error")
                        AppLogger.e("❌ API returned error: $message")
                        statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                        errorMap[itemId] = "API error: $message"
                    }

                } catch (e: Exception) {
                    progressMap[itemId] = 100
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("💥 Error processing user data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing user data: ${e.message}"
                }
            }


            AppUtils.DatasetNames.checkAppVersion -> {
                try {
                    // Parsing - update to 75%
                    progressMap[itemId] = 75
                    _itemProgressMap.postValue(progressMap.toMap())

                    // Log the complete JSON response
                    AppLogger.d("App Version JSON Response: $responseBodyString")

                    // Parse JSON and extract required_version
                    if (responseBodyString.isNotBlank()) {
                        try {
                            val jsonObject = JSONObject(responseBodyString)
                            val success = jsonObject.optBoolean("success", false)

                            if (success) {
                                val dataArray = jsonObject.optJSONArray("data")
                                if (dataArray != null && dataArray.length() > 0) {
                                    val firstItem = dataArray.getJSONObject(0)
                                    val requiredVersion =
                                        firstItem.optString("required_version", "")

                                    if (requiredVersion.isNotEmpty()) {
                                        prefManager.latestAppVersionSystem = requiredVersion
                                        AppLogger.d("Stored latest app version: $requiredVersion")
                                    } else {
                                        AppLogger.w("Required version is empty in response")
                                    }
                                } else {
                                    AppLogger.w("No data array or empty data in response")
                                }
                            } else {
                                AppLogger.w("API response success is false")
                            }
                        } catch (e: JSONException) {
                            AppLogger.e("Error parsing app version JSON: ${e.message}")
                        }
                    }

                    // Final update - 100%
                    progressMap[itemId] = 100
                    _itemProgressMap.postValue(progressMap.toMap())

                    // Set status based on response
                    statusMap[itemId] = if (responseBodyString.isBlank()) {
                        AppLogger.e("Received empty JSON response for app version")
                        errorMap[itemId] = "Empty JSON response"
                        AppUtils.UploadStatusUtils.FAILED
                    } else {
                        if (isDownloadDataset) {
                            AppUtils.UploadStatusUtils.DOWNLOADED
                        } else {
                            AppUtils.UploadStatusUtils.DONE_CHECK
                        }
                    }

                    if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                        prefManager.addDataset(request.dataset)
                    }

                    AppLogger.d("App version check completed successfully")

                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error processing app version response: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing app version: ${e.message}"
                }
            }

            AppUtils.DatasetNames.hektaran -> {
                try {
                    progressMap[itemId] = 50
                    _itemProgressMap.postValue(progressMap.toMap())
                    statusMap[itemId] = "Sedang Mempersiapkan data Hektaran"
                    _itemStatusMap.postValue(statusMap.toMap())
                    AppLogger.d("responseBodyString $responseBodyString")

                    if (responseBodyString.isNotBlank()) {
                        try {
                            val jsonObject = JSONObject(responseBodyString)
                            val success = jsonObject.optBoolean("success", false)

                            if (success) {
                                val dataArray = jsonObject.optJSONArray("data")
                                if (dataArray != null && dataArray.length() > 0) {

                                    // ======================
// 🔹 STEP 1: Filter Hektaran with Valid date_panen
// ======================

                                    val validHektaranArray = JSONArray()
                                    var skippedCount = 0

                                    for (i in 0 until dataArray.length()) {
                                        val item = dataArray.getJSONObject(i)
                                        val blokKode = item.optString("blok_kode", "")
                                        val details = item.optJSONArray("HektaranDetails")

                                        // Find valid date_panen entries
                                        val validDates = mutableListOf<String>()
                                        if (details != null) {
                                            for (j in 0 until details.length()) {
                                                val d = details.getJSONObject(j)
                                                val datePanenRaw =
                                                    d.opt("date_panen") // this keeps it as nullable Any
                                                val datePanen =
                                                    if (datePanenRaw != null && datePanenRaw != JSONObject.NULL) {
                                                        datePanenRaw.toString().trim()
                                                    } else {
                                                        ""
                                                    }

                                                // ✅ Exclude blank, "null", and null values
                                                if (datePanen.isNotBlank() && !datePanen.equals(
                                                        "null",
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    validDates.add(datePanen)
                                                }
                                            }
                                        }

                                        if (validDates.isEmpty()) {
                                            skippedCount++
//      AppLogger.d("⏩ Skipped Hektaran id=${item.optInt("id")} blok=$blokKode → all date_panen=null")
                                        } else {
                                            AppLogger.d(
                                                "✅ Valid Hektaran id=${item.optInt("id")} blok=$blokKode → date_panen=${
                                                    validDates.joinToString(
                                                        ","
                                                    )
                                                }"
                                            )
                                            validHektaranArray.put(item)
                                        }
                                    }

                                    AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    AppLogger.d("📦 Raw Server Hektaran: ${dataArray.length()}")
                                    AppLogger.d("📊 Filtered Hektaran (valid date_panen): ${validHektaranArray.length()}")
                                    AppLogger.d("⏩ Skipped Hektaran (no valid date_panen): $skippedCount")
                                    AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                    val filteredDataArray = validHektaranArray

// ======================
// 🔹 STEP 2: Load Local Panen Data
// ======================

                                    val formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                                    val today = LocalDate.now()
                                    val startDate = today.minusDays(2).format(formatterDate) + " 00:00:00"
                                    val endDate = today.format(formatterDate) + " 23:59:59"

                                    val afdelingId = request.afdeling.toString().toIntOrNull() ?: 0
                                    val panenList = panenDao.findPanenLast3DaysByAfdeling(
                                        startDate,
                                        endDate,
                                        afdelingId
                                    )

                                    AppLogger.d("📦 Loaded Local Panen (with relations): ${panenList.size}")

// ======================
// 🔹 STEP 3: Parse & Flatten Server Hektaran
// ======================

                                    data class HektaranDetailRecord(
                                        val datePanen: LocalDate,
                                        val blokKode: String,
                                        val kemandoranId: Int,
                                        val nik: String,
                                        val nama: String,
                                        val jjgMasak: Double,
                                        val jjgMentah: Double,
                                        val jjgLewatMasak: Double,
                                        val jjgKosong: Double,
                                        val jjgAbnormal: Double,
                                        val jjgBayar: Double,
                                        val jjgKirim: Double
                                    )

                                    val gson = Gson()
                                    val dateArrayType = object : TypeToken<List<String>>() {}.type

                                    val hektaranRecords = mutableListOf<HektaranDetailRecord>()

                                    for (i in 0 until filteredDataArray.length()) {
                                        val header = filteredDataArray.getJSONObject(i)
                                        val blokKode = header.optString("blok_kode", "")
                                        val details = header.optJSONArray("HektaranDetails") ?: continue

                                        for (j in 0 until details.length()) {
                                            val d = details.getJSONObject(j)

                                            // Parse "date_panen" (string that looks like ["2025-10-22 08:42:55", ...])
                                            val rawDatePanen = d.optString("date_panen", "")
                                            val dateList: List<String> = try {
                                                gson.fromJson(rawDatePanen, dateArrayType)
                                            } catch (e: Exception) {
                                                emptyList()
                                            }

                                            for (dateStr in dateList) {
                                                val dateParsed = try {
                                                    LocalDate.parse(dateStr.substring(0, 10), formatterDate)
                                                } catch (e: Exception) {
                                                    continue
                                                }

                                                val record = HektaranDetailRecord(
                                                    datePanen = dateParsed,
                                                    blokKode = blokKode,
                                                    kemandoranId = d.optInt("kemandoran_ppro", 0),
                                                    nik = d.optString("pemanen_nik", ""),
                                                    nama = d.optString("pemanen_nama", ""),
                                                    jjgMasak = d.optDouble("jjg_masak", 0.0),
                                                    jjgMentah = d.optDouble("jjg_mentah", 0.0),
                                                    jjgLewatMasak = d.optDouble("jjg_lewat_masak", 0.0),
                                                    jjgKosong = d.optDouble("jjg_kosong", 0.0),
                                                    jjgAbnormal = d.optDouble("jjg_abnormal", 0.0),
                                                    jjgBayar = d.optDouble("jjg_panen", 0.0),
                                                    jjgKirim = d.optDouble("jjg_kirim", 0.0)
                                                )
                                                hektaranRecords.add(record)
                                            }
                                        }
                                    }

                                    AppLogger.d("📊 Flattened Server Hektaran Records: ${hektaranRecords.size}")
                                    AppLogger.d("👥 Unique Names in Hektaran: ${hektaranRecords.map { it.nama }.distinct().joinToString()}")

// ======================
// 🔹 STEP 4: Group Local Panen by (date, blok, nik)
// ======================

                                    data class PanenKey(val date: LocalDate, val blokKode: String, val nik: String)
                                    data class GroupedPanen(
                                        val key: PanenKey,
                                        val panenList: List<PanenEntityWithRelations>,
                                        val totalJjgMasak: Double,
                                        val totalJjgMentah: Double,
                                        val totalJjgLewatMasak: Double,
                                        val totalJjgKosong: Double,
                                        val totalJjgAbnormal: Double,
                                        val totalJjgBayar: Double,
                                        val totalJjgKirim: Double
                                    )

                                    val groupedPanenMap = panenList.groupBy { panen ->
                                        val date = try {
                                            LocalDate.parse(panen.panen.date_created.substring(0, 10), formatterDate)
                                        } catch (_: Exception) {
                                            LocalDate.MIN
                                        }
                                        PanenKey(date, panen.tph?.blok_kode ?: "", panen.panen.karyawan_nik)
                                    }.mapValues { (key, panens) ->
                                        var sumMasak = 0.0
                                        var sumMentah = 0.0
                                        var sumLewat = 0.0
                                        var sumKosong = 0.0
                                        var sumAbnormal = 0.0
                                        var sumBayar = 0.0
                                        var sumKirim = 0.0

                                        panens.forEach { panen ->
                                            val jjgJson = try {
                                                JSONObject(panen.panen.jjg_json ?: "{}")
                                            } catch (_: Exception) {
                                                JSONObject()
                                            }
                                            sumMasak += jjgJson.optDouble("TO", 0.0)
                                            sumMentah += jjgJson.optDouble("UN", 0.0)
                                            sumLewat += jjgJson.optDouble("OV", 0.0)
                                            sumKosong += jjgJson.optDouble("EM", 0.0)
                                            sumAbnormal += jjgJson.optDouble("AB", 0.0)
                                            sumBayar += jjgJson.optDouble("PA", 0.0)
                                            sumKirim += jjgJson.optDouble("KP", 0.0)
                                        }

                                        GroupedPanen(key, panens, sumMasak, sumMentah, sumLewat, sumKosong, sumAbnormal, sumBayar, sumKirim)
                                    }

                                    AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    AppLogger.d("📦 Total Local Panen Records: ${panenList.size}")
                                    AppLogger.d("📊 Grouped Local Panen Keys: ${groupedPanenMap.size}")
                                    AppLogger.d("📊 Server Hektaran Records: ${hektaranRecords.size}")
                                    AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

// ======================
// 🔹 STEP 5: Create Hektaran Index with DEDUPLICATION
// ======================

                                    data class HektaranKey(val date: LocalDate, val blokKode: String, val nik: String)

                                    val hektaranMap = hektaranRecords.groupBy { hekt ->
                                        HektaranKey(hekt.datePanen, hekt.blokKode, hekt.nik)
                                    }.mapValues { (key, hektList) ->
                                        // ⚠️ Check if all records have identical values (duplicates) or need summing
                                        val first = hektList.first()
                                        val allIdentical = hektList.all {
                                            it.jjgMasak == first.jjgMasak &&
                                                    it.jjgMentah == first.jjgMentah &&
                                                    it.jjgLewatMasak == first.jjgLewatMasak &&
                                                    it.jjgKosong == first.jjgKosong &&
                                                    it.jjgAbnormal == first.jjgAbnormal &&
                                                    it.jjgBayar == first.jjgBayar &&
                                                    it.jjgKirim == first.jjgKirim
                                        }

                                        if (allIdentical && hektList.size > 1) {
                                            AppLogger.d("⚠️ DUPLICATE detected: ${first.nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | count=${hektList.size} → using single value (${first.jjgMasak})")
                                            // Use only one record's values (they're all the same)
                                            mapOf(
                                                "masak" to first.jjgMasak,
                                                "mentah" to first.jjgMentah,
                                                "lewat" to first.jjgLewatMasak,
                                                "kosong" to first.jjgKosong,
                                                "abnormal" to first.jjgAbnormal,
                                                "bayar" to first.jjgBayar,
                                                "kirim" to first.jjgKirim
                                            )
                                        } else {
                                            // Sum all records (they have different values)
                                            var sumMasak = 0.0
                                            var sumMentah = 0.0
                                            var sumLewat = 0.0
                                            var sumKosong = 0.0
                                            var sumAbnormal = 0.0
                                            var sumBayar = 0.0
                                            var sumKirim = 0.0

                                            hektList.forEach { h ->
                                                sumMasak += h.jjgMasak
                                                sumMentah += h.jjgMentah
                                                sumLewat += h.jjgLewatMasak
                                                sumKosong += h.jjgKosong
                                                sumAbnormal += h.jjgAbnormal
                                                sumBayar += h.jjgBayar
                                                sumKirim += h.jjgKirim
                                            }

                                            if (hektList.size > 1) {
                                                AppLogger.d("📊 SUMMING: ${first.nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | count=${hektList.size} → total=$sumMasak")
                                            }

                                            mapOf(
                                                "masak" to sumMasak,
                                                "mentah" to sumMentah,
                                                "lewat" to sumLewat,
                                                "kosong" to sumKosong,
                                                "abnormal" to sumAbnormal,
                                                "bayar" to sumBayar,
                                                "kirim" to sumKirim
                                            )
                                        }
                                    }

// ======================
// 🔹 STEP 6: Find Local Panen NOT in Hektaran (need to upload)
// ======================

                                    val notInHektaranList = mutableListOf<PanenEntityWithRelations>()
                                    val alreadyInHektaranList = mutableListOf<PanenEntityWithRelations>()

                                    fun Double.eq(other: Double) = kotlin.math.abs(this - other) < 0.001

                                    groupedPanenMap.forEach { (key, grouped) ->
                                        val hektaranData = hektaranMap[HektaranKey(key.date, key.blokKode, key.nik)]

                                        if (hektaranData != null) {
                                            // Check if JJG values match
                                            val jjgMatch =
                                                grouped.totalJjgMasak.eq(hektaranData["masak"] ?: 0.0) &&
                                                        grouped.totalJjgMentah.eq(hektaranData["mentah"] ?: 0.0) &&
                                                        grouped.totalJjgLewatMasak.eq(hektaranData["lewat"] ?: 0.0) &&
                                                        grouped.totalJjgKosong.eq(hektaranData["kosong"] ?: 0.0) &&
                                                        grouped.totalJjgAbnormal.eq(hektaranData["abnormal"] ?: 0.0) &&
                                                        grouped.totalJjgBayar.eq(hektaranData["bayar"] ?: 0.0) &&
                                                        grouped.totalJjgKirim.eq(hektaranData["kirim"] ?: 0.0)

                                            if (jjgMatch) {
                                                alreadyInHektaranList.addAll(grouped.panenList)
                                                AppLogger.d("✅ Already in Hektaran — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | records=${grouped.panenList.size}")
                                            } else {
                                                notInHektaranList.addAll(grouped.panenList)
                                                AppLogger.d("⚠️ JJG Mismatch — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date}")
                                                AppLogger.d("   📊 Local:  TO=${grouped.totalJjgMasak}, PA=${grouped.totalJjgBayar}, KP=${grouped.totalJjgKirim}")
                                                AppLogger.d("   📊 Server: TO=${hektaranData["masak"]}, PA=${hektaranData["bayar"]}, KP=${hektaranData["kirim"]}")
                                            }
                                        } else {
                                            // Not found in hektaran server → needs to be uploaded!
                                            notInHektaranList.addAll(grouped.panenList)
                                            AppLogger.d("🆕 NOT in Hektaran (new) — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | records=${grouped.panenList.size}")
                                        }
                                    }
// ======================
// 🔹 STEP 7: Summary & Prepare Upload
// ======================

                                    AppLogger.d("──────────── SUMMARY ────────────")
                                    AppLogger.d("📋 Total Local Panen: ${panenList.size}")
                                    AppLogger.d("✅ Already in Hektaran: ${alreadyInHektaranList.size}")
                                    AppLogger.d("🆕 NOT in Hektaran (need upload): ${notInHektaranList.size}")
                                    AppLogger.d("─────────────────────────────────")

                                    if (notInHektaranList.isNotEmpty()) {
                                        notInHektaranList.take(20).forEachIndexed { index, panen ->
                                            AppLogger.d("  ${index + 1}. ${panen.panen.karyawan_nama} (${panen.panen.karyawan_nik}) | blok=${panen.tph?.blok_kode} | date=${panen.panen.date_created}")
                                            AppLogger.d("     🔸 local_jjg_json=${panen.panen.jjg_json}")
                                        }
//                                        if (notInHektaranList.size > 10) {
//                                            AppLogger.d("  ... and ${notInHektaranList.size - 10} more")
//                                        }
                                    }

                                    val saveDataMPanenList = notInHektaranList.map { relation ->
                                        val p = relation.panen
                                        PanenEntity(
                                            tph_id = p.tph_id ?: "0",
                                            date_created = p.date_created,
                                            karyawan_nik = p.karyawan_nik ?: "",
                                            jjg_json = p.jjg_json ?: "",
                                            foto = p.foto ?: "",
                                            komentar = p.komentar ?: "",
                                            asistensi = p.asistensi ?: 0,
                                            lat = p.lat ?: 0.0,
                                            lon = p.lon ?: 0.0,
                                            jenis_panen = p.jenis_panen ?: 0,
                                            ancak = p.ancak ?: 0,
                                            info = p.info ?: "NULL",
                                            scan_status = p.scan_status ?: 0,
                                            nomor_pemanen = p.nomor_pemanen ?: 0,
                                            dataIsZipped = p.dataIsZipped ?: 0,
                                            created_by = p.created_by ?: 0,
                                            karyawan_id = p.karyawan_id ?: "NULL",
                                            kemandoran_id = p.kemandoran_id ?: "",
                                            karyawan_nama = p.karyawan_nama ?: "NULL",
                                            jumlah_pemanen = p.jumlah_pemanen ?: 1,
                                            status_scan_mpanen = 1
                                        )
                                    }

                                    AppLogger.d("💾 Ready to save new Panen count: ${saveDataMPanenList.size}")
                                    AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                    progressMap[itemId] = 75
                                    _itemProgressMap.postValue(progressMap.toMap())
                                    statusMap[itemId] = "Sedang menyimpan data hektaran baru"
                                    _itemStatusMap.postValue(statusMap.toMap())

                                        val result = appRepository.saveScanMPanen(
                                            saveDataMPanenList,
                                            request.createdBy.toString(),
                                            request.creatorInfo,
                                            null,
                                        )

                                    result.fold(
                                        onSuccess = { saveResult ->
                                            when (saveResult) {
                                                is SaveTPHResult.AllSuccess -> {
                                                    progressMap[itemId] = 100
                                                    _itemProgressMap.postValue(progressMap.toMap())
                                                    statusMap[itemId] =
                                                        AppUtils.UploadStatusUtils.SAVED.format("Hektaran")
                                                    _itemStatusMap.postValue(statusMap.toMap())
                                                }

                                                is SaveTPHResult.PartialSuccess -> {
                                                    progressMap[itemId] = 100
                                                    _itemProgressMap.postValue(progressMap.toMap())
                                                    statusMap[itemId] =
                                                        AppUtils.UploadStatusUtils.DUPLICATE_PARTIAL.format(
                                                            saveResult.duplicateCount
                                                        )
                                                    _itemStatusMap.postValue(statusMap.toMap())
                                                }

                                                is SaveTPHResult.AllDuplicate -> {
                                                    progressMap[itemId] = 100
                                                    _itemProgressMap.postValue(progressMap.toMap())
                                                    statusMap[itemId] =
                                                        AppUtils.UploadStatusUtils.DUPLICATE
                                                    _itemStatusMap.postValue(statusMap.toMap())
                                                }
                                            }
                                        },
                                        onFailure = { exception ->
                                            progressMap[itemId] = 100
                                            _itemProgressMap.postValue(progressMap.toMap())
                                            statusMap[itemId] = AppUtils.UploadStatusUtils.DUPLICATE
                                            _itemStatusMap.postValue(statusMap.toMap())

                                        }
                                    )


                                }
                            }
                        } catch (e: JSONException) {
                            AppLogger.e("Error parsing JSON: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    progressMap[itemId] = 100
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error processing hektaran response: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing app version: ${e.message}"
                }
            }

            AppUtils.DatasetNames.sinkronisasiDataPanen -> {
                try {
                    // We're already at 50% when this code starts
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    // Parse the JSON response to extract the panen data
                    val jsonObject = JSONObject(responseBodyString)

                    if (jsonObject.optBoolean("success", false)) {
                        val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                        val panenList = mutableListOf<PanenEntity>()

                        // Update to 70% before processing records
                        progressMap[itemId] = 70
                        _itemProgressMap.postValue(progressMap.toMap())

                        AppLogger.d("Processing ${dataArray.length()} panen records...")

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)

                            // Extract required fields
                            val idPanen = item.optInt("id", 0)
                            val tphId = item.optString("tph", "")
                            val createdDate = item.optString("created_date", "")
                            val createdBy = item.optInt("created_by", 0)
                            val ancak = item.optInt("ancak", 0)
                            val nomor_pemanen = item.optInt("nomor_pemanen", 0)
                            val asistensi = item.optInt("asistensi", 0)

                            // Extract new asistensi fields with proper null handling
                            val asistensiDept = if (item.isNull("asistensi_dept")) {
                                null
                            } else {
                                item.optInt("asistensi_dept", 0).takeIf { it != 0 }
                            }

                            val asistensiDeptNama = if (item.isNull("asistensi_dept_nama")) {
                                null
                            } else {
                                item.optString("asistensi_dept_nama", "").takeIf {
                                    it.isNotEmpty() && !it.equals("null", ignoreCase = true)
                                }
                            }

                            val asistensiDivisi = if (item.isNull("asistensi_divisi")) {
                                null
                            } else {
                                item.optInt("asistensi_divisi", 0).takeIf { it != 0 }
                            }

                            val jenis_panen = item.optInt("tipe", 0)
                            val jjgKirim = item.optInt("jjg_kirim", 0)
                            val jjgMasak = item.optInt("jjg_masak", 0)
                            val jjgMentah = item.optInt("jjg_mentah", 0)
                            val jjgLewatMasak = item.optInt("jjg_lewat_masak", 0)
                            val jjgKosong = item.optInt("jjg_kosong", 0)
                            val jjgAbnormal = item.optInt("jjg_abnormal", 0)
                            val jjgBayar = item.optInt("jjg_bayar", 0)
                            val jjgJson = JSONObject().apply {
                                put("TO", jjgMasak)
                                put("UN", jjgMentah)
                                put("OV", jjgLewatMasak)
                                put("EM", jjgKosong)
                                put("AB", jjgAbnormal)
                                put("KP", jjgKirim)
                                put("PA", jjgBayar)
                            }.toString()

                            val createdName = item.optString("created_name", "")
                            val username = if (createdName.isNullOrEmpty() || createdName.equals(
                                    "NULL",
                                    ignoreCase = true
                                )
                            ) {
                                ""  // Keep it empty if null/NULL
                            } else {
                                extractUsernameFromCreatedName(createdName)
                            }
                            // Parse kemandoran JSON to extract kemandoran_id and karyawan info
                            val kemandoranString = item.optString("kemandoran", "")
                            var kemandoranId = ""
                            var karyawanNikList = mutableListOf<String>()
                            var karyawanNamaList = mutableListOf<String>()


                            if (kemandoranString.isNotEmpty() &&
                                kemandoranString != "null" &&
                                !item.isNull("kemandoran")
                            ) {
                                try {
                                    val kemandoranArray = JSONArray(kemandoranString)
                                    if (kemandoranArray.length() > 0) {
                                        val kemandoranObj = kemandoranArray.getJSONObject(0)
                                        kemandoranId = kemandoranObj.optString("id", "")

                                        val pemanenArray = kemandoranObj.optJSONArray("pemanen")
                                        if (pemanenArray != null) {
                                            for (j in 0 until pemanenArray.length()) {
                                                val pemanenObj = pemanenArray.getJSONObject(j)
                                                val nik = pemanenObj.optString("nik", "")
                                                val nama = pemanenObj.optString("nama", "")
                                                if (nik.isNotEmpty()) {
                                                    karyawanNikList.add(nik)
                                                    karyawanNamaList.add(nama)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e(
                                        "Error parsing kemandoran JSON for record ${
                                            item.optInt(
                                                "id",
                                                0
                                            )
                                        }: ${e.message}"
                                    )
                                    AppLogger.d("Kemandoran raw value: '$kemandoranString'")
                                }
                            } else {
                                AppLogger.d(
                                    "Kemandoran data is null or empty for record ${
                                        item.optInt(
                                            "id",
                                            0
                                        )
                                    }"
                                )
                            }

                            // Get karyawan_id from database using NIK
                            var karyawanIdList = mutableListOf<String>()
                            if (karyawanNikList.isNotEmpty()) {
                                try {
                                    val karyawanList =
                                        karyawanDao.getKaryawanByNikList(karyawanNikList)
                                    for (karyawan in karyawanList) {
                                        karyawan.id?.let { karyawanIdList.add(it.toString()) }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Error getting karyawan data: ${e.message}")
                                }
                            }

                            // Join multiple values with comma
                            val karyawanId = karyawanIdList.joinToString(",")
                            val karyawanNik = karyawanNikList.joinToString(",")
                            val karyawanNama = karyawanNamaList.joinToString(",")

                            AppLogger.d("Creating entity: tphId=$tphId, date=$createdDate, kemandoranId=$kemandoranId, karyawanId=$karyawanId, karyawanNik=$karyawanNik")

                            val panenEntity = PanenEntity(
                                id = idPanen,
                                tph_id = tphId.toString(),
                                date_created = createdDate,
                                created_by = createdBy,
                                created_name = createdName,
                                karyawan_id = karyawanId,
                                kemandoran_id = kemandoranId,
                                karyawan_nik = karyawanNik,
                                karyawan_nama = karyawanNama,
                                jjg_json = jjgJson,
                                foto = "",
                                komentar = "",
                                asistensi = asistensi,
                                asistensi_dept = asistensiDept,
                                asistensi_dept_nama = asistensiDeptNama,
                                asistensi_divisi = asistensiDivisi,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = jenis_panen,
                                ancak = ancak,
                                info = "",
                                archive = 0,
                                status_banjir = 0,
                                status_espb = 0,
                                status_restan = 0,
                                scan_status = 1,
                                dataIsZipped = 0,
                                no_espb = "",
                                username = username,
                                status_upload = 0,
                                status_uploaded_image = "0",
                                status_pengangkutan = 0,
                                status_insert_mpanen = 0,
                                status_scan_mpanen = 0,
                                jumlah_pemanen = karyawanNikList.size,
                                archive_mpanen = 0,
                                nomor_pemanen = nomor_pemanen,
                                isPushedToServer = 1
                            )

                            panenList.add(panenEntity)
                        }

                        AppLogger.d("=== PANEN PROCESSING SUMMARY ===")
                        AppLogger.d("Total records processed: ${dataArray.length()}")
                        AppLogger.d("Records to INSERT/UPDATE: ${panenList.size}")
                        AppLogger.d("==================================")

                        // Update to 80% when processing is complete
                        progressMap[itemId] = 80
                        _itemProgressMap.postValue(progressMap.toMap())

                        withContext(Dispatchers.IO) {
                            try {
                                var successCount = 0
                                var failCount = 0

                                AppLogger.d("Processing ${panenList.size} records for insert/update...")

                                // Batch processing - process in chunks of 500
                                val batchSize = 500
                                val batches = panenList.chunked(batchSize)

                                for ((batchIndex, batch) in batches.withIndex()) {
                                    AppLogger.d("Processing batch ${batchIndex + 1}/${batches.size} (${batch.size} records)")

                                    val insertList = mutableListOf<PanenEntity>()
                                    val updateList = mutableListOf<PanenEntity>()

                                    // Separate records into insert and update lists
                                    for (panen in batch) {
                                        try {
                                            // Check if record exists by tph_id and date_created (NOT by ID)
                                            val existingRecord = panenDao.findByTphAndDate(
                                                panen.tph_id,
                                                panen.date_created
                                            )

                                            if (existingRecord == null) {
                                                // Record doesn't exist -> INSERT
                                                insertList.add(panen)
                                                AppLogger.d("Queued for insert: ID=${panen.id}, ${panen.tph_id}, ${panen.date_created}")
                                            } else {
                                                // Record exists -> UPDATE
                                                AppLogger.d("Record exists, queued for update: TPH=${panen.tph_id}, Date=${panen.date_created}")

                                                val updatedRecord = existingRecord.copy(
                                                    // Update basic info from server
                                                    created_by = panen.created_by,
                                                    created_name = if (existingRecord.created_name.isNullOrEmpty() || existingRecord.created_name == "NULL") {
                                                        panen.created_name
                                                    } else {
                                                        existingRecord.created_name
                                                    },
                                                    jenis_panen = if (existingRecord.jenis_panen == 0 && panen.jenis_panen != 0) {
                                                        panen.jenis_panen
                                                    } else {
                                                        existingRecord.jenis_panen
                                                    },
                                                    asistensi = panen.asistensi,

                                                    asistensi_dept = if (panen.asistensi_dept != null) {
                                                        panen.asistensi_dept
                                                    } else {
                                                        existingRecord.asistensi_dept
                                                    },

                                                    asistensi_dept_nama = if (panen.asistensi_dept_nama != null) {
                                                        panen.asistensi_dept_nama
                                                    } else {
                                                        existingRecord.asistensi_dept_nama
                                                    },

                                                    asistensi_divisi = if (panen.asistensi_divisi != null) {
                                                        panen.asistensi_divisi
                                                    } else {
                                                        existingRecord.asistensi_divisi
                                                    },

                                                    ancak = if (existingRecord.ancak == 0 && panen.ancak != 0) {
                                                        panen.ancak
                                                    } else {
                                                        existingRecord.ancak
                                                    },
                                                    nomor_pemanen = if (existingRecord.nomor_pemanen == 0 && panen.nomor_pemanen != 0) {
                                                        panen.nomor_pemanen
                                                    } else {
                                                        existingRecord.nomor_pemanen
                                                    },
                                                    jjg_json = if ((existingRecord.jjg_json.isNullOrEmpty() ||
                                                                existingRecord.jjg_json == "NULL" ||
                                                                existingRecord.jjg_json == "{}" ||
                                                                existingRecord.jjg_json == "{\"KP\": 0}") &&
                                                        panen.jjg_json.isNotEmpty() &&
                                                        panen.jjg_json != "NULL" &&
                                                        panen.jjg_json != "{\"KP\": 0}"
                                                    ) {
                                                        panen.jjg_json
                                                    } else {
                                                        existingRecord.jjg_json
                                                    },
                                                    isPushedToServer = 1,

                                                    // Update karyawan info if existing is null/empty
                                                    karyawan_id = if ((existingRecord.karyawan_id.isNullOrEmpty() ||
                                                                existingRecord.karyawan_id == "NULL") &&
                                                        panen.karyawan_id.isNotEmpty() &&
                                                        panen.karyawan_id != "NULL"
                                                    ) {
                                                        panen.karyawan_id
                                                    } else {
                                                        existingRecord.karyawan_id
                                                    },

                                                    kemandoran_id = if ((existingRecord.kemandoran_id.isNullOrEmpty() ||
                                                                existingRecord.kemandoran_id == "NULL") &&
                                                        panen.kemandoran_id.isNotEmpty() &&
                                                        panen.kemandoran_id != "NULL"
                                                    ) {
                                                        panen.kemandoran_id
                                                    } else {
                                                        existingRecord.kemandoran_id
                                                    },

                                                    karyawan_nik = if ((existingRecord.karyawan_nik.isNullOrEmpty() ||
                                                                existingRecord.karyawan_nik == "NULL") &&
                                                        panen.karyawan_nik.isNotEmpty() &&
                                                        panen.karyawan_nik != "NULL"
                                                    ) {
                                                        panen.karyawan_nik
                                                    } else {
                                                        existingRecord.karyawan_nik
                                                    },

                                                    karyawan_nama = if ((existingRecord.karyawan_nama.isNullOrEmpty() ||
                                                                existingRecord.karyawan_nama == "NULL") &&
                                                        panen.karyawan_nama.isNotEmpty() &&
                                                        panen.karyawan_nama != "NULL"
                                                    ) {
                                                        panen.karyawan_nama
                                                    } else {
                                                        existingRecord.karyawan_nama
                                                    },

                                                    // Update jumlah_pemanen if existing is 0 or 1 (default)
                                                    jumlah_pemanen = if (existingRecord.jumlah_pemanen <= 1 && panen.jumlah_pemanen > 1) {
                                                        panen.jumlah_pemanen
                                                    } else {
                                                        existingRecord.jumlah_pemanen
                                                    },

                                                    status_upload = existingRecord.status_upload,
                                                    status_uploaded_image = existingRecord.status_uploaded_image,
                                                    status_scan_inspeksi = existingRecord.status_scan_inspeksi,
                                                    scan_status = if (existingRecord.scan_status == 0) {
                                                        1  // Set to 1 if existing is 0
                                                    } else {
                                                        existingRecord.scan_status  // Keep existing value if not 0
                                                    },
                                                    // Preserve local photos and comments if they exist
                                                    foto = if (existingRecord.foto.isNotEmpty() && existingRecord.foto != "NULL") {
                                                        existingRecord.foto
                                                    } else {
                                                        panen.foto
                                                    },

                                                    komentar = if (existingRecord.komentar.isNotEmpty() && existingRecord.komentar != "NULL") {
                                                        existingRecord.komentar
                                                    } else {
                                                        panen.komentar
                                                    },
                                                    username = if (existingRecord.username.isNullOrEmpty() || existingRecord.username == "NULL") {
                                                        panen.username
                                                    } else {
                                                        existingRecord.username
                                                    },
                                                    // Preserve local coordinates if they exist
                                                    lat = if (existingRecord.lat != 0.0) existingRecord.lat else panen.lat,
                                                    lon = if (existingRecord.lon != 0.0) existingRecord.lon else panen.lon
                                                )

                                                updateList.add(updatedRecord)
                                            }
                                        } catch (e: Exception) {
                                            failCount++
                                            AppLogger.e("Error processing panen record: ${e.message}")
                                        }
                                    }

                                    // Batch insert new records
                                    if (insertList.isNotEmpty()) {
                                        try {
                                            AppLogger.d("Batch inserting ${insertList.size} new records...")
                                            for (panen in insertList) {
                                                val result = panenDao.insertWithTransaction(panen)
                                                if (result.isSuccess) {
                                                    successCount++
                                                    AppLogger.d("Inserted new panen record: ID=${panen.id}, ${panen.tph_id}, ${panen.date_created}")
                                                } else {
                                                    failCount++
                                                    AppLogger.e("Failed to insert panen record: ${panen.tph_id}, ${panen.date_created}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            failCount += insertList.size
                                            AppLogger.e("Error batch inserting records: ${e.message}")
                                        }
                                    }

                                    // Batch update existing records
                                    if (updateList.isNotEmpty()) {
                                        try {
                                            AppLogger.d("Batch updating ${updateList.size} existing records...")
                                            panenDao.update(updateList)
                                            successCount += updateList.size
                                            AppLogger.d("Updated ${updateList.size} existing records")
                                        } catch (e: Exception) {
                                            failCount += updateList.size
                                            AppLogger.e("Error batch updating records: ${e.message}")
                                        }
                                    }
                                }

                                // Final update - 100%
                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())
                                if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                    prefManager.addDataset(request.dataset)
                                }

                                if (panenList.isEmpty()) {
                                    // No records to process - everything is up to date
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                                    AppLogger.d("No records to process - dataset is up to date")
                                } else if (failCount == 0) {
                                    statusMap[itemId] = if (isDownloadDataset) {
                                        AppUtils.UploadStatusUtils.DOWNLOADED
                                    } else {
                                        AppUtils.UploadStatusUtils.UPDATED
                                    }
                                } else if (successCount > 0) {
                                    // Partial success
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPDATED
                                    errorMap[itemId] =
                                        "Partial success: $failCount/${panenList.size} records failed"
                                } else {
                                    // Complete failure
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                    errorMap[itemId] =
                                        "Failed to process panen data: $failCount/${panenList.size} records failed"
                                }
                            } catch (e: Exception) {
                                progressMap[itemId] = 100  // Still show 100% even on error
                                _itemProgressMap.postValue(progressMap.toMap())

                                AppLogger.e("Error processing panen data: ${e.message}")
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "Error processing panen data: ${e.message}"
                            }
                        }
                    } else {
                        progressMap[itemId] = 100
                        _itemProgressMap.postValue(progressMap.toMap())

                        statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                        val errorMessage = jsonObject.optString("message", "Unknown error")
                        errorMap[itemId] = "API Error: $errorMessage"
                        AppLogger.e("Panen API returned error: $errorMessage")
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error processing panen data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing panen data: ${e.message}"
                }
            }

            AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi -> {
                try {
                    // We're already at 50% when this code starts
                    progressMap[itemId] = 60
                    _itemProgressMap.postValue(progressMap.toMap())

                    // Parse the JSON response to extract the inspection data
                    val jsonObject = JSONObject(responseBodyString)

                    if (jsonObject.optBoolean("success", false)) {
                        val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()

                        // Update to 70% before processing records
                        progressMap[itemId] = 70
                        _itemProgressMap.postValue(progressMap.toMap())

                        AppLogger.d("Processing ${dataArray.length()} inspection records...")

                        // Update to 80% when processing is complete
                        progressMap[itemId] = 80
                        _itemProgressMap.postValue(progressMap.toMap())

                        withContext(Dispatchers.IO) {
                            try {
                                var successCount = 0
                                var failCount = 0
                                var skippedCount = 0

                                AppLogger.d("Processing ${dataArray.length()} inspection records for insert/update...")

                                // Process each inspection record with its details
                                for (i in 0 until dataArray.length()) {
                                    val item = dataArray.getJSONObject(i)

                                    try {
                                        val idPanen = item.optString("id_panen", "0")
                                        val tphId = item.optInt("tph", 0)
                                        val tglInspeksi = item.optString("tgl_inspeksi", "")
                                        val tglPanen = item.optString("tgl_panen", "")
                                        val jjgPanen = item.optInt("jjg_panen", 0)
                                        val jalurMasuk = item.optString("rute_masuk", "")
                                        val jenisInspeksi = item.optInt("jenis_inspeksi", 0)
                                        val baris = item.optString("baris", "")
                                        val jmlPokokInspeksi = item.optInt("jml_pokok_inspeksi", 0)
                                        val createdName = item.optString("created_name", "")
                                        val app_version = item.optString("app_version", "")
                                        val createdBy = item.optInt("created_by", 0)
                                        val trackingPath = item.optString("tracking_path", "")

                                        // NEW: Parse the 4 pemuat fields
                                        val kemandoranPproPemuat =
                                            item.optString("kemandoran_ppro_pemuat", "")
                                        val kemandoranNamaPemuat =
                                            item.optString("kemandoran_nama_pemuat", "")
                                        val nikPemuat = item.optString("nik_pemuat", "")
                                        val namaPemuat = item.optString("nama_pemuat", "")

                                        // Handle nullable fields
                                        val dept =
                                            if (item.has("dept") && !item.isNull("dept")) item.optInt(
                                                "dept"
                                            ) else null
                                        val deptPpro =
                                            if (item.has("dept_ppro") && !item.isNull("dept_ppro")) item.optInt(
                                                "dept_ppro"
                                            ) else null
                                        val deptAbbr =
                                            if (item.has("dept_abbr") && !item.isNull("dept_abbr")) item.optString(
                                                "dept_abbr"
                                            ) else null
                                        val deptNama =
                                            if (item.has("dept_nama") && !item.isNull("dept_nama")) item.optString(
                                                "dept_nama"
                                            ) else null
                                        val divisi =
                                            if (item.has("divisi") && !item.isNull("divisi")) item.optInt(
                                                "divisi"
                                            ) else null
                                        val divisiPpro =
                                            if (item.has("divisi_ppro") && !item.isNull("divisi_ppro")) item.optInt(
                                                "divisi_ppro"
                                            ) else null
                                        val divisiAbbr =
                                            if (item.has("divisi_abbr") && !item.isNull("divisi_abbr")) item.optString(
                                                "divisi_abbr"
                                            ) else null
                                        val divisiNama =
                                            if (item.has("divisi_nama") && !item.isNull("divisi_nama")) item.optString(
                                                "divisi_nama"
                                            ) else null
                                        val blok =
                                            if (item.has("blok") && !item.isNull("blok")) item.optInt(
                                                "blok"
                                            ) else null
                                        val blokPpro =
                                            if (item.has("blok_ppro") && !item.isNull("blok_ppro")) item.optInt(
                                                "blok_ppro"
                                            ) else null
                                        val blokKode =
                                            if (item.has("blok_kode") && !item.isNull("blok_kode")) item.optString(
                                                "blok_kode"
                                            ) else null
                                        val blokNama =
                                            if (item.has("blok_nama") && !item.isNull("blok_nama")) item.optString(
                                                "blok_nama"
                                            ) else null
                                        val tphNomor =
                                            if (item.has("tph_nomor") && !item.isNull("tph_nomor")) item.optInt(
                                                "tph_nomor"
                                            ) else null
                                        val ancak =
                                            if (item.has("ancak") && !item.isNull("ancak")) item.optString(
                                                "ancak"
                                            ) else null

                                        val inspectionEntity = InspectionModel(
                                            id = 0,
                                            created_date = tglInspeksi,
                                            created_by = createdBy.toString(),
                                            created_name = createdName,
                                            tph_id = tphId,
                                            id_panen = idPanen,
                                            dept = dept,
                                            dept_ppro = deptPpro,
                                            dept_abbr = deptAbbr,
                                            dept_nama = deptNama,
                                            divisi = divisi,
                                            divisi_ppro = divisiPpro,
                                            divisi_abbr = divisiAbbr,
                                            divisi_nama = divisiNama,
                                            blok = blok,
                                            blok_ppro = blokPpro,
                                            blok_kode = blokKode,
                                            blok_nama = blokNama,
                                            tph_nomor = tphNomor,
                                            ancak = ancak,
                                            foto_user = "",
                                            jjg_panen = jjgPanen,
                                            date_panen = tglPanen,
                                            jalur_masuk = jalurMasuk,
                                            jenis_kondisi = jenisInspeksi,
                                            baris = baris,
                                            jml_pkk_inspeksi = jmlPokokInspeksi,
                                            tracking_path = trackingPath,
                                            dataIsZipped = 0,
                                            app_version = app_version,
                                            status_upload = "0",
                                            status_uploaded_image = "0",
                                            isPushedToServer = 1,
                                            kemandoran_ppro_pemuat = kemandoranPproPemuat,
                                            kemandoran_nama_pemuat = kemandoranNamaPemuat,
                                            nik_pemuat = nikPemuat,
                                            nama_pemuat = namaPemuat
                                        )

                                        // Check if inspection has details first - if not, skip the entire inspection
                                        val inspectionDetails = item.optJSONArray("InspeksiDetails")

                                        if (inspectionDetails == null || inspectionDetails.length() == 0) {
                                            AppLogger.d("Skipping inspection TPH=${inspectionEntity.tph_id} - no details found")
                                            skippedCount++
                                            continue // Skip this entire inspection record
                                        }

                                        // If we reach here, inspection has details - proceed with processing

                                        val existingRecord =
                                            inspectionDao.getInspectionByBusinessKey(
                                                inspectionEntity.tph_id,
                                                inspectionEntity.created_date,
                                                inspectionEntity.dept ?: 0,
                                                inspectionEntity.divisi ?: 0,
                                                1 // isPushedToServer = 1 (only check server records)
                                            )

                                        var localInspectionId: Int

                                        if (existingRecord == null) {
                                            // Record doesn't exist -> INSERT new inspection
                                            val result =
                                                inspectionDao.insertWithTransaction(inspectionEntity)
                                            if (result.isSuccess) {
                                                // Get the newly inserted ID
                                                localInspectionId =
                                                    inspectionDao.getInspectionByBusinessKey(
                                                        inspectionEntity.tph_id,
                                                        inspectionEntity.created_date,
                                                        inspectionEntity.dept ?: 0,
                                                        inspectionEntity.divisi ?: 0,
                                                        1
                                                    )?.id ?: 0

                                                successCount++
                                                AppLogger.d("Inserted new inspection: TPH=${inspectionEntity.tph_id}, Local ID=$localInspectionId")

                                                // INSERT all detail records since parent is new
                                                insertAllDetailRecords(
                                                    inspectionDetails,
                                                    localInspectionId,
                                                    tglInspeksi
                                                )

                                            } else {
                                                failCount++
                                                AppLogger.e("Failed to insert inspection: TPH=${inspectionEntity.tph_id}")
                                                continue // Skip processing details if inspection failed
                                            }
                                        } else {
                                            // Record exists -> UPDATE inspection and all its details
                                            localInspectionId = existingRecord.id

                                            // Update the inspection record
                                            val updatedRecord = inspectionEntity.copy(
                                                id = existingRecord.id, // Keep local auto-increment ID
                                                status_upload = existingRecord.status_upload,
                                                status_uploaded_image = existingRecord.status_uploaded_image
                                            )
                                            inspectionDao.update(listOf(updatedRecord))
                                            successCount++
                                            AppLogger.d("Updated existing inspection: TPH=${inspectionEntity.tph_id}, Local ID=$localInspectionId")

                                            // DELETE old details and INSERT new ones
                                            inspectionDetailDao.deleteByInspectionId(
                                                localInspectionId.toString()
                                            )
                                            insertAllDetailRecords(
                                                inspectionDetails,
                                                localInspectionId,
                                                tglInspeksi
                                            )
                                        }

                                    } catch (e: Exception) {
                                        AppLogger.e("Error processing inspection item: ${e.message}")
                                        failCount++
                                    }
                                }

                                // Final summary
                                AppLogger.d("=== INSPECTION SYNC COMPLETE ===")
                                AppLogger.d("Inserted/Updated: $successCount")
                                AppLogger.d("Failed: $failCount")
                                AppLogger.d("Skipped (no details): $skippedCount")
                                AppLogger.d("Total records processed: ${dataArray.length()}")
                                AppLogger.d("==================================")

                                // Final update - 100%
                                progressMap[itemId] = 100
                                _itemProgressMap.postValue(progressMap.toMap())
                                if (prefManager.datasetMustUpdate.contains(request.dataset)) {
                                    prefManager.addDataset(request.dataset)
                                }

                                if (dataArray.length() == 0) {
                                    // No records to process - everything is up to date
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPTODATE
                                    AppLogger.d("No records to process - dataset is up to date")
                                } else if (failCount == 0) {
                                    statusMap[itemId] = if (isDownloadDataset) {
                                        AppUtils.UploadStatusUtils.DOWNLOADED
                                    } else {
                                        AppUtils.UploadStatusUtils.UPDATED
                                    }
                                } else if (successCount > 0) {
                                    // Partial success
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.UPDATED
                                    errorMap[itemId] =
                                        "Partial success: $failCount/${dataArray.length()} records failed, $skippedCount skipped"
                                } else {
                                    // Complete failure
                                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                    errorMap[itemId] =
                                        "Failed to process inspection data: $failCount/${dataArray.length()} records failed, $skippedCount skipped"
                                }
                            } catch (e: Exception) {
                                progressMap[itemId] = 100  // Still show 100% even on error
                                _itemProgressMap.postValue(progressMap.toMap())

                                AppLogger.e("Error processing inspection data: ${e.message}")
                                statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                                errorMap[itemId] = "Error processing inspection data: ${e.message}"
                            }
                        }
                    } else {
                        progressMap[itemId] = 100
                        _itemProgressMap.postValue(progressMap.toMap())

                        statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                        val errorMessage = jsonObject.optString("message", "Unknown error")
                        errorMap[itemId] = "API Error: $errorMessage"
                        AppLogger.e("Inspection API returned error: $errorMessage")
                    }
                } catch (e: Exception) {
                    progressMap[itemId] = 100  // Still show 100% even on error
                    _itemProgressMap.postValue(progressMap.toMap())

                    AppLogger.e("Error processing inspection data: ${e.message}")
                    statusMap[itemId] = AppUtils.UploadStatusUtils.FAILED
                    errorMap[itemId] = "Error processing inspection data: ${e.message}"
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

    private suspend fun insertAllDetailRecords(
        inspectionDetails: JSONArray,
        localInspectionId: Int,
        tglInspeksi: String
    ) {

        var detailSuccessCount = 0
        var detailFailCount = 0

        for (j in 0 until inspectionDetails.length()) {
            val detail = inspectionDetails.getJSONObject(j)

            try {
                val noPokPok = detail.optInt("no_pokok", 0)
                val pokokPanen = detail.optInt("pokok_panen", 0)
                val kodeInspeksi = detail.optInt("kode_inspeksi", 0)
                val temuanInspeksi = detail.optDouble("temuan_inspeksi", 0.0)
                val statusPemulihan = detail.optInt("status_pemulihan", 0)
                val nik = detail.optString("nik", "")
                val nama = detail.optString("nama", "")
                val catatan = detail.optString("catatan", null)
                val createdDate = detail.optString("created_date", tglInspeksi)
                val createdName = detail.optString("created_name", "")
                val createdBy = detail.optString("created_by", "")
                val latDetail = detail.optString("lat", "0.0").toDoubleOrNull() ?: 0.0
                val lonDetail = detail.optString("lon", "0.0").toDoubleOrNull() ?: 0.0

                AppLogger.d("Detail ${j + 1}/${inspectionDetails.length()}: NIK=$nik, Code=$kodeInspeksi, Pokok=$noPokPok")

                val inspectionDetailEntity = InspectionDetailModel(
                    id = 0,
                    id_inspeksi = localInspectionId.toString(),
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    nik = nik,
                    nama = nama,
                    no_pokok = noPokPok,
                    pokok_panen = if (pokokPanen == 0) null else pokokPanen,
                    kode_inspeksi = kodeInspeksi,
                    temuan_inspeksi = temuanInspeksi,
                    status_pemulihan = statusPemulihan,
                    foto = null,
                    foto_pemulihan = null,
                    komentar = catatan,
                    latIssue = latDetail,
                    lonIssue = lonDetail,
                    status_upload = "0",
                    status_uploaded_image = "0",
                    isPushedToServer = 1
                )

                val result = inspectionDetailDao.insertWithTransaction(inspectionDetailEntity)
                if (result.isSuccess) {
                    detailSuccessCount++
                    AppLogger.d("✅ Inserted detail ${j + 1}: NIK=$nik, Code=$kodeInspeksi")
                } else {
                    detailFailCount++
                    AppLogger.e("❌ Failed to insert detail ${j + 1}: NIK=$nik, Code=$kodeInspeksi")
                }
            } catch (e: Exception) {
                detailFailCount++
                AppLogger.e("❌ Error processing detail ${j + 1}: ${e.message}")
            }
        }

        AppLogger.d("<<< Finished insertAllDetailRecords for inspection ID=$localInspectionId")
        AppLogger.d("Detail results: Success=$detailSuccessCount, Failed=$detailFailCount")
    }

    fun processPreviewDataInspeksi(jsonResponse: String, estate: Any): String {
        try {
            val jsonObject = JSONObject(jsonResponse)

            if (jsonObject.optBoolean("success", false)) {
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()

                val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormatter = SimpleDateFormat("d MMMM", Locale("id", "ID"))
                val calendar = Calendar.getInstance()

                // Today at end of day
                val todayDate = calendar.time
                val today = inputFormatter.format(todayDate)

                calendar.add(Calendar.DAY_OF_YEAR, -3)
                val sevenDaysAgoDate = calendar.time
                val sevenDaysAgo = inputFormatter.format(sevenDaysAgoDate)

                // Create all dates in range
                val allDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = sevenDaysAgoDate

                while (!tempCalendar.time.after(todayDate)) {
                    allDates.add(inputFormatter.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val startDateDisplay = displayFormatter.format(sevenDaysAgoDate)
                val endDateDisplay = displayFormatter.format(todayDate)

                val resultBuilder = StringBuilder()
                resultBuilder.append("Data Inspeksi ($startDateDisplay - $endDateDisplay)\n")

                // Check if we need grouping by dept_abbr
                AppLogger.d("processPreviewDataInspeksi - Is estate a List? ${estate is List<*>}")

                if (estate is List<*>) {
                    AppLogger.d("processPreviewDataInspeksi - Processing multiple estates")

                    // Group by dept_abbr for multiple estates
                    val dataByEstate = mutableMapOf<String, MutableList<JSONObject>>()

                    // Categorize data by dept_abbr
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val deptAbbr = item.optString("dept_abbr", "Unknown")

                        AppLogger.d("processPreviewDataInspeksi - Item $i dept_abbr: $deptAbbr")

                        if (!dataByEstate.containsKey(deptAbbr)) {
                            dataByEstate[deptAbbr] = mutableListOf()
                        }
                        dataByEstate[deptAbbr]!!.add(item)
                    }

                    AppLogger.d("processPreviewDataInspeksi - Found ${dataByEstate.size} estates")
                    AppLogger.d("processPreviewDataInspeksi - Estate keys: ${dataByEstate.keys}")

                    resultBuilder.append("\n")

                    // Process each estate
                    dataByEstate.forEach { (deptAbbr, estateData) ->
                        AppLogger.d("processPreviewDataInspeksi - Processing estate: $deptAbbr with ${estateData.size} items")

                        resultBuilder.append("=== $deptAbbr ===\n")

                        val inspeksiCountByDate = mutableMapOf<String, Int>()
                        for (date in allDates) {
                            inspeksiCountByDate[date] = 0
                        }

                        // Process inspections for this estate
                        for (item in estateData) {
                            val inspeksiDateFull = item.optString("tgl_inspeksi", "")
                            val inspeksiDate = if (inspeksiDateFull.isNotEmpty()) {
                                inspeksiDateFull.split(" ")[0]
                            } else {
                                continue
                            }

                            if (!allDates.contains(inspeksiDate)) continue

                            val idPanen = item.optString("id_panen", "")
                            val tphNomor = item.optString("tph_nomor", "")
                            val ancak = item.optString("ancak", "")

                            if (idPanen.isNotEmpty() || tphNomor.isNotEmpty() || ancak.isNotEmpty()) {
                                inspeksiCountByDate[inspeksiDate] =
                                    inspeksiCountByDate.getOrDefault(inspeksiDate, 0) + 1
                            }
                        }

                        // Add this estate's data
                        var hasValidData = false
                        for (date in allDates.sortedDescending()) {
                            val inspeksiCount = inspeksiCountByDate[date] ?: 0

                            if (inspeksiCount == 0) {
                                continue
                            }

                            hasValidData = true
                            val dateObj = inputFormatter.parse(date)
                            val dateDisplay = displayFormatter.format(dateObj!!)
                            resultBuilder.append("$dateDisplay - $inspeksiCount Transaksi\n")
                        }

                        if (!hasValidData) {
                            resultBuilder.append("Tidak ada data inspeksi dalam periode ini.\n")
                        }

                        resultBuilder.append("\n")
                    }
                } else {
                    AppLogger.d("processPreviewDataInspeksi - Processing single estate")

                    // Single estate - use original logic
                    val inspeksiCountByDate = mutableMapOf<String, Int>()
                    for (date in allDates) {
                        inspeksiCountByDate[date] = 0
                    }

                    // Process inspections
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)

                        val inspeksiDateFull = item.optString("tgl_inspeksi", "")
                        val inspeksiDate = if (inspeksiDateFull.isNotEmpty()) {
                            inspeksiDateFull.split(" ")[0]
                        } else {
                            continue
                        }

                        if (!allDates.contains(inspeksiDate)) continue

                        val idPanen = item.optString("id_panen", "")
                        val tphNomor = item.optString("tph_nomor", "")
                        val ancak = item.optString("ancak", "")

                        if (idPanen.isNotEmpty() || tphNomor.isNotEmpty() || ancak.isNotEmpty()) {
                            inspeksiCountByDate[inspeksiDate] =
                                inspeksiCountByDate.getOrDefault(inspeksiDate, 0) + 1
                        }
                    }

                    var hasValidData = false
                    for (date in allDates.sortedDescending()) {
                        val inspeksiCount = inspeksiCountByDate[date] ?: 0
                        if (inspeksiCount > 0) {
                            hasValidData = true
                            val dateObj = inputFormatter.parse(date)
                            val dateDisplay = displayFormatter.format(dateObj!!)
                            resultBuilder.append("$dateDisplay - $inspeksiCount Transaksi\n")
                        }
                    }

                    if (!hasValidData) {
                        resultBuilder.append("Tidak ada data inspeksi dalam periode ini.")
                    }
                }

                val finalResult = resultBuilder.toString().trim()
                AppLogger.d("processPreviewDataInspeksi - Final result:\n$finalResult")

                return finalResult
            } else {
                return "Failed to process data: Success flag is false"
            }
        } catch (e: Exception) {
            AppLogger.e("processPreviewDataInspeksi - Error: ${e.message}")
            e.printStackTrace()
            return "Error processing data: ${e.message}"
        }
    }

    fun processPreviewDataPanenInspeksi(jsonResponse: String, estate: Any): String {
        try {
            // Parse the JSON response
            val jsonObject = JSONObject(jsonResponse)

            // Check if response is successful and contains data
            if (jsonObject.optBoolean("success", false)) {
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()

                // Set up date range
                val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormatter = SimpleDateFormat("d MMMM", Locale("id", "ID"))
                val calendar = Calendar.getInstance()

                // Today at end of day
                val todayDate = calendar.time
                val today = inputFormatter.format(todayDate)

                calendar.add(Calendar.DAY_OF_YEAR, -3)
                val sevenDaysAgo = inputFormatter.format(calendar.time)
                val sevenDaysAgoDate = calendar.time

                // Create a list of all dates in the range
                val allDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = sevenDaysAgoDate

                while (!tempCalendar.time.after(todayDate)) {
                    allDates.add(inputFormatter.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val startDateDisplay = displayFormatter.format(sevenDaysAgoDate)
                val endDateDisplay = displayFormatter.format(todayDate)

                val resultBuilder = StringBuilder()
                resultBuilder.append("Data Panen ($startDateDisplay - $endDateDisplay)\n")


                // Check if we need grouping by dept_abbr
                if (estate is List<*>) {
                    // Group by dept_abbr for multiple estates
                    val dataByEstate = mutableMapOf<String, MutableList<JSONObject>>()

                    // Categorize data by dept_abbr
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val deptAbbr = item.optString("dept_abbr", "Unknown")

                        if (!dataByEstate.containsKey(deptAbbr)) {
                            dataByEstate[deptAbbr] = mutableListOf()
                        }
                        dataByEstate[deptAbbr]!!.add(item)
                    }

                    resultBuilder.append("\n")

                    // Process each estate
                    dataByEstate.forEach { (deptAbbr, estateData) ->
                        resultBuilder.append("=== $deptAbbr ===\n")

                        // Process data for this estate
                        val tphCountByDate = mutableMapOf<String, Int>()
                        val recordCountByDate = mutableMapOf<String, Int>()

                        for (date in allDates) {
                            tphCountByDate[date] = 0
                            recordCountByDate[date] = 0
                        }

                        for (item in estateData) {
                            val createdDateFull = item.optString("created_date", "")
                            val createdDate = if (createdDateFull.isNotEmpty()) {
                                createdDateFull.split(" ")[0]
                            } else {
                                continue
                            }

                            if (!allDates.contains(createdDate)) {
                                continue
                            }

                            recordCountByDate[createdDate] =
                                recordCountByDate.getOrDefault(createdDate, 0) + 1

                            val tphIdInt = item.optInt("tph", 0)
                            if (tphIdInt > 0) {
                                tphCountByDate[createdDate] =
                                    tphCountByDate.getOrDefault(createdDate, 0) + 1
                            }
                        }

                        // Add this estate's data
                        var hasValidData = false
                        for (date in allDates.sortedDescending()) {
                            val tphCount = tphCountByDate[date] ?: 0
                            val recordCount = recordCountByDate[date] ?: 0

                            if (recordCount == 0) {
                                continue
                            }

                            hasValidData = true
                            val dateObj = inputFormatter.parse(date)
                            val dateDisplay = displayFormatter.format(dateObj!!)
                            resultBuilder.append("$dateDisplay - $tphCount Transaksi\n")
                        }

                        if (!hasValidData) {
                            resultBuilder.append("Tidak ada data panen dalam periode ini.\n")
                        }

                        resultBuilder.append("\n")
                    }
                } else {
                    // Single estate - use original logic
                    val tphCountByDate = mutableMapOf<String, Int>()
                    val recordCountByDate = mutableMapOf<String, Int>()

                    for (date in allDates) {
                        tphCountByDate[date] = 0
                        recordCountByDate[date] = 0
                    }

                    // Process each item in the array
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)

                        val createdDateFull = item.optString("created_date", "")
                        val createdDate = if (createdDateFull.isNotEmpty()) {
                            createdDateFull.split(" ")[0]
                        } else {
                            continue
                        }

                        if (!allDates.contains(createdDate)) {
                            continue
                        }

                        recordCountByDate[createdDate] =
                            recordCountByDate.getOrDefault(createdDate, 0) + 1

                        val tphIdInt = item.optInt("tph", 0)
                        if (tphIdInt > 0) {
                            tphCountByDate[createdDate] =
                                tphCountByDate.getOrDefault(createdDate, 0) + 1
                        }
                    }

                    // Add single estate data
                    var hasValidData = false
                    for (date in allDates.sortedDescending()) {
                        val tphCount = tphCountByDate[date] ?: 0
                        val recordCount = recordCountByDate[date] ?: 0

                        if (recordCount == 0) {
                            continue
                        }

                        hasValidData = true
                        val dateObj = inputFormatter.parse(date)
                        val dateDisplay = displayFormatter.format(dateObj!!)
                        resultBuilder.append("$dateDisplay - $tphCount TPH\n")
                    }

                    if (!hasValidData) {
                        resultBuilder.append("Tidak ada data panen dalam periode ini.")
                    }
                }

                return resultBuilder.toString().trim()
            } else {
                return "Failed to process data: Success flag is false"
            }
        } catch (e: Exception) {
            Log.e("PreviewDataProcessor", "Error processing data: ${e.message}")
            return "Error processing data: ${e.message}"
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
                val displayFormatter = SimpleDateFormat("d MMMM", Locale("id", "ID"))
                val calendar = Calendar.getInstance()

                // Today
                val today = inputFormatter.format(calendar.time)
                val todayDate = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, -3)
                val sevenDaysAgo = inputFormatter.format(calendar.time)
                val sevenDaysAgoDate = calendar.time

                // Format the date range for display (e.g., "28 April - 5 Mei")
                val endDateDisplay = displayFormatter.format(todayDate)
                val startDateDisplay = displayFormatter.format(sevenDaysAgoDate)

                // Create a list of all dates in the range
                val allDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = sevenDaysAgoDate

                while (!tempCalendar.time.after(todayDate)) {
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
                    recordCountByDate[createdDate] =
                        recordCountByDate.getOrDefault(createdDate, 0) + 1

                    // Get jjg_kirim value
                    val jjgKirim = item.optInt("jjg_kirim", 0)

                    // Update the total jjg_kirim for this date
                    jjgKirimByDate[createdDate] =
                        jjgKirimByDate.getOrDefault(createdDate, 0) + jjgKirim

                    // Simply increment the TPH count for this date (no uniqueness check)
                    // Only count if tph value is greater than 0
                    val tphIdInt = item.optInt("tph", 0)
                    if (tphIdInt > 0) {
                        tphCountByDate[createdDate] =
                            tphCountByDate.getOrDefault(createdDate, 0) + 1
                    }
                }


                // Calculate total jjg_kirim for all dates
                val totalJjgKirim = jjgKirimByDate.values.sum()

                // Build the final string
                val resultBuilder = StringBuilder()
                resultBuilder.append("Data Restan ($startDateDisplay - $endDateDisplay):\n")

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


                    val dateObj = inputFormatter.parse(date)
                    val dateDisplay = displayFormatter.format(dateObj!!)

                    resultBuilder.append("$dateDisplay: $jjgKirimCount jjg dari $tphCount Transaksi\n")
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

                    val idPpro =
                        if (estateObject.has("id_ppro") && !estateObject.get("id_ppro").isJsonNull)
                            estateObject.get("id_ppro").asInt else null

                    val abbr = if (estateObject.has("abbr") && !estateObject.get("abbr").isJsonNull)
                        estateObject.get("abbr").asString else null

                    val nama = if (estateObject.has("nama") && !estateObject.get("nama").isJsonNull)
                        estateObject.get("nama").asString else null

                    val tph_otomatis =
                        if (estateObject.has("tph_otomatis") && !estateObject.get("tph_otomatis").isJsonNull)
                            estateObject.get("tph_otomatis").asString else null

                    val estate = EstateModel(
                        id = id,
                        id_ppro = idPpro,
                        abbr = abbr,
                        nama = nama,
                        tph_otomatis = tph_otomatis!!.toInt()
                    )
                    estateList.add(estate)

                    // Parse afdeling data if present
                    if (estateObject.has("divisi") && !estateObject.get("divisi").isJsonNull) {
                        val divisiArray = estateObject.getAsJsonArray("divisi")

                        for (j in 0 until divisiArray.size()) {
                            try {
                                val divisiObject = divisiArray.get(j).asJsonObject

                                val divisiId =
                                    if (divisiObject.has("id") && !divisiObject.get("id").isJsonNull)
                                        divisiObject.get("id").asInt else continue

                                val divisiIdPpro =
                                    if (divisiObject.has("id_ppro") && !divisiObject.get("id_ppro").isJsonNull)
                                        divisiObject.get("id_ppro").asInt else null

                                val divisiAbbr =
                                    if (divisiObject.has("abbr") && !divisiObject.get("abbr").isJsonNull)
                                        divisiObject.get("abbr").asString?.trim() else null

                                val divisiNama =
                                    if (divisiObject.has("nama") && !divisiObject.get("nama").isJsonNull)
                                        divisiObject.get("nama").asString else null

                                val sinkronisasi_otomatis =
                                    if (divisiObject.has("sinkronisasi_otomatis") && !divisiObject.get(
                                            "sinkronisasi_otomatis"
                                        ).isJsonNull
                                    )
                                        divisiObject.get("sinkronisasi_otomatis").asInt else 0

                                val afdeling = AfdelingModel(
                                    id = divisiId,
                                    id_ppro = divisiIdPpro,
                                    abbr = divisiAbbr,
                                    nama = divisiNama,
                                    estate_id = id,
                                    sinkronisasi_otomatis = sinkronisasi_otomatis
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
    private inline fun <reified T> parseJsonToList(
        jsonContent: String,
        modelClass: Class<T>
    ): List<T> {
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
                    AppUtils.DatasetNames.jenisTPH,
                    AppUtils.DatasetNames.parameter,
                    AppUtils.DatasetNames.sinkronisasiDataPanen,
                    AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi
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
                    } else if (request.dataset == AppUtils.DatasetNames.estate) {
                        response = repository.downloadListEstate(request.regional ?: 0)
                    }
                    else if (request.dataset == AppUtils.DatasetNames.sinkronisasiRestan) {
                        if (request.jabatan != AppUtils.ListFeatureByRoleUser.ASKEP &&
                            request.jabatan != AppUtils.ListFeatureByRoleUser.Manager
                        ) {
                            response = restanRepository.getDataRestan(
                                request.estate as Int,
                                request.afdeling.toString()
                            )
                        } else {
                            return@forEach
                        }
                    }
                    else if (request.dataset == AppUtils.DatasetNames.checkAppVersion) {
                        response = versioningAppRepository.getDataAppVersion(request.idUser ?: 0)
                    } else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                        response = repository.downloadSettingJson(request.lastModified!!)
                    } else if (request.dataset == AppUtils.DatasetNames.parameter) {
                        response = repository.getParameter()
                    }
                    else if (request.dataset == AppUtils.DatasetNames.hektaran) {

                        val estateId: Int = when (val estate = request.estate) {
                            is Int -> estate
                            is String -> estate.toIntOrNull() ?: 0
                            is List<*> -> (estate.firstOrNull() as? Int) ?: 0
                            else -> 0
                        }

                        val afdelingId: Int = when (val estate = request.afdeling) {
                            is Int -> estate
                            is String -> estate.toIntOrNull() ?: 0
                            is List<*> -> (estate.firstOrNull() as? Int) ?: 0
                            else -> 0
                        }
                        response =
                            hektarPanenRepository.getDataHektaranHektarDetail(estateId, afdelingId)
                    }
                    else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataPanen) {
                        response = dataPanenInspectionRepository.getDataPanen(
                            request.estate!!,
                        )
                    }
                    else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataWajahPemanen) {
                        val syncResult = withContext(Dispatchers.IO) {
                            pemanenFaceRepository.syncFromServer()
                        }

                        results[request.dataset] = if (syncResult.success) {
                            Resource.Success(Response.success(null))
                        } else {
                            Resource.Error(syncResult.message)
                        }
                        _downloadStatuses.postValue(results.toMap())
                        return@forEach
                    }
                    else if (request.dataset == AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi) {
                        response = dataPanenInspectionRepository.getDataInspeksi(
                            request.estate!!,
                            true,
                            parameterDao
                        )
                    }
                    else if (request.dataset == AppUtils.DatasetNames.tph && request.estate is List<*>) {
                        val estateId = request.estate as List<*>
                        val allTphData = mutableListOf<TPHNewModel>()
                        var lastSuccessResponse: Response<ResponseBody>? = null

                        results[request.dataset] = Resource.Loading(0)
                        _downloadStatuses.postValue(results.toMap())

                        results[request.dataset] = Resource.Storing(request.dataset)
                        _downloadStatuses.postValue(results.toMap())

                        // Loop through provided estate IDs
                        estateId.forEach { estate ->
                            try {
                                val estateResponse =
                                    repository.getTPHEstate(estate.toString(), false)
                                AppLogger.d("responseBody $estateResponse for estate $estate")
                                if (estateResponse.isSuccessful && estateResponse.code() == 200) {
                                    lastSuccessResponse = estateResponse
                                    val responseBody = estateResponse.body()?.string() ?: ""
                                    if (responseBody.isNotBlank()) {
                                        val tphList = parseTPHJsonToList(responseBody)
                                        allTphData.addAll(tphList)
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error getting TPH data for estate ${estate}: ${e.message}")
                            }
                        }

                        // Check if we have any data
                        if (allTphData.isNotEmpty()) {
                            updateOrInsertTPH(allTphData, false).join()

                            if (tphStatus.value.isSuccess) {
                                results[request.dataset] = Resource.Success(
                                    lastSuccessResponse!!,
                                    "TPH data processed successfully"
                                )
                                prefManager!!.addDataset(request.dataset)

                                // Set lastModified to current datetime
                                val currentDateTime = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())
                                prefManager.lastModifiedDatasetTPH = currentDateTime
                            } else {
                                val error = tphStatus.value.exceptionOrNull()
                                results[request.dataset] =
                                    Resource.Error("Database error: ${error?.message ?: "Unknown error"}")
                            }
                        } else {
                            results[request.dataset] =
                                Resource.Error("No TPH data found for any estate")
                        }

                        _downloadStatuses.postValue(results.toMap())
                        return@forEach
                    }
                    else if (request.dataset == AppUtils.DatasetNames.tph && request.regional != null) {

                        val estatesResult = repository.getAllEstates()
                        if (estatesResult.isSuccess) {
                            val estates = estatesResult.getOrNull() ?: emptyList()
                            val allTphData = mutableListOf<TPHNewModel>()
                            var lastSuccessResponse: Response<ResponseBody>? = null

                            // Update status to loading/processing
                            results[request.dataset] = Resource.Loading(0)
                            _downloadStatuses.postValue(results.toMap())

                            results[request.dataset] = Resource.Storing(request.dataset)
                            _downloadStatuses.postValue(results.toMap())
                            estates.forEach { estate ->
                                estate.abbr?.let { estateAbbr ->
                                    try {
                                        val estateResponse =
                                            repository.getTPHEstate(estateAbbr, true)
                                        AppLogger.d("responseBody $estateResponse")
                                        if (estateResponse.isSuccessful && estateResponse.code() == 200) {
                                            lastSuccessResponse = estateResponse
                                            val responseBody = estateResponse.body()?.string() ?: ""
                                            if (responseBody.isNotBlank()) {
                                                val tphList = parseTPHJsonToList(responseBody)
                                                allTphData.addAll(tphList)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error getting TPH data for estate ${estateAbbr}: ${e.message}")
                                    }
                                }
                            }

                            // Check if we have any data
                            if (allTphData.isNotEmpty()) {


                                updateOrInsertTPH(allTphData, false).join()


                                if (tphStatus.value.isSuccess) {
                                    results[request.dataset] = Resource.Success(
                                        lastSuccessResponse!!,
                                        "TPH data processed successfully"
                                    )
                                    prefManager!!.addDataset(request.dataset)

                                    // Set lastModified to current datetime
                                    val currentDateTime = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date())
                                    prefManager.lastModifiedDatasetTPH = currentDateTime
                                } else {
                                    val error = tphStatus.value.exceptionOrNull()
                                    results[request.dataset] =
                                        Resource.Error("Database error: ${error?.message ?: "Unknown error"}")
                                }
                            } else {
                                results[request.dataset] =
                                    Resource.Error("No TPH data found for any estate")
                            }

                            _downloadStatuses.postValue(results.toMap())
                            return@forEach
                        } else {
                            results[request.dataset] =
                                Resource.Error("Failed to get estates: ${estatesResult.exceptionOrNull()?.message}")
                            _downloadStatuses.postValue(results.toMap())
                            return@forEach
                        }
                    }
                    else {
                        response = repository.downloadDataset(modifiedRequest)
                    }

                    // Get headers
                    val contentType = response.headers()["Content-Type"]
                    val lastModified = response.headers()["Last-Modified-Dataset"]
                    val lastModifiedSettingsJson = response.headers()["Last-Modified-Settings"]

                    AppLogger.d("${contentType}")
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
                                    prefManager.addDataset(request.dataset)
                                    AppLogger.d("masuk sini gess ")
                                    AppLogger.d("${prefManager.datasetMustUpdate}")
                                } else if (request.dataset == AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi) {

                                    try {
                                        results[request.dataset] = Resource.Loading(60)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Parse the JSON response to extract the inspection data
                                        val jsonObject = JSONObject(responseBodyString)

                                        if (jsonObject.optBoolean("success", false)) {
                                            val dataArray =
                                                jsonObject.optJSONArray("data") ?: JSONArray()

                                            results[request.dataset] = Resource.Loading(70)
                                            _downloadStatuses.postValue(results.toMap())

                                            AppLogger.d("Processing ${dataArray.length()} follow-up inspection records...")

                                            results[request.dataset] = Resource.Loading(80)
                                            _downloadStatuses.postValue(results.toMap())

                                            withContext(Dispatchers.IO) {
                                                try {
                                                    var successCount = 0
                                                    var failCount = 0
                                                    var skippedCount = 0

                                                    AppLogger.d("=== FOLLOW-UP INSPECTION SYNC START ===")
                                                    AppLogger.d("Processing ${dataArray.length()} inspection records for insert...")

                                                    // Process each inspection record with its details
                                                    for (i in 0 until dataArray.length()) {
                                                        val item = dataArray.getJSONObject(i)

                                                        try {
                                                            val idPanen =
                                                                item.optString("id_panen", "0")
                                                            val tphId = item.optInt("tph", 0)
                                                            val tglInspeksi =
                                                                item.optString("tgl_inspeksi", "")
                                                            val tglPanen =
                                                                item.optString("tgl_panen", "")
                                                            val jjgPanen =
                                                                item.optInt("jjg_panen", 0)
                                                            val jalurMasuk =
                                                                item.optString("rute_masuk", "")
                                                            val jenisInspeksi =
                                                                item.optInt("jenis_inspeksi", 0)
                                                            val baris = item.optString("baris", "")
                                                            val jmlPokokInspeksi =
                                                                item.optInt("jml_pokok_inspeksi", 0)
                                                            val createdName =
                                                                item.optString("created_name", "")
                                                            val app_version =
                                                                item.optString("app_version", "")
                                                            val createdBy =
                                                                item.optInt("created_by", 0)
                                                            val trackingPath =
                                                                item.optString("tracking_path", "")

                                                            // NEW: Parse the 4 pemuat fields for follow-up inspection
                                                            val kemandoranPproPemuat =
                                                                item.optString(
                                                                    "kemandoran_ppro_pemuat",
                                                                    ""
                                                                )
                                                            val kemandoranNamaPemuat =
                                                                item.optString(
                                                                    "kemandoran_nama_pemuat",
                                                                    ""
                                                                )
                                                            val nikPemuat =
                                                                item.optString("nik_pemuat", "")
                                                            val namaPemuat =
                                                                item.optString("nama_pemuat", "")

                                                            // Handle nullable fields
                                                            val dept =
                                                                if (item.has("dept") && !item.isNull(
                                                                        "dept"
                                                                    )
                                                                ) item.optInt("dept") else null
                                                            val deptPpro =
                                                                if (item.has("dept_ppro") && !item.isNull(
                                                                        "dept_ppro"
                                                                    )
                                                                ) item.optInt("dept_ppro") else null
                                                            val deptAbbr =
                                                                if (item.has("dept_abbr") && !item.isNull(
                                                                        "dept_abbr"
                                                                    )
                                                                ) item.optString("dept_abbr") else null
                                                            val deptNama =
                                                                if (item.has("dept_nama") && !item.isNull(
                                                                        "dept_nama"
                                                                    )
                                                                ) item.optString("dept_nama") else null
                                                            val divisi =
                                                                if (item.has("divisi") && !item.isNull(
                                                                        "divisi"
                                                                    )
                                                                ) item.optInt("divisi") else null
                                                            val divisiPpro =
                                                                if (item.has("divisi_ppro") && !item.isNull(
                                                                        "divisi_ppro"
                                                                    )
                                                                ) item.optInt("divisi_ppro") else null
                                                            val divisiAbbr =
                                                                if (item.has("divisi_abbr") && !item.isNull(
                                                                        "divisi_abbr"
                                                                    )
                                                                ) item.optString("divisi_abbr") else null
                                                            val divisiNama =
                                                                if (item.has("divisi_nama") && !item.isNull(
                                                                        "divisi_nama"
                                                                    )
                                                                ) item.optString("divisi_nama") else null
                                                            val blok =
                                                                if (item.has("blok") && !item.isNull(
                                                                        "blok"
                                                                    )
                                                                ) item.optInt("blok") else null
                                                            val blokPpro =
                                                                if (item.has("blok_ppro") && !item.isNull(
                                                                        "blok_ppro"
                                                                    )
                                                                ) item.optInt("blok_ppro") else null
                                                            val blokKode =
                                                                if (item.has("blok_kode") && !item.isNull(
                                                                        "blok_kode"
                                                                    )
                                                                ) item.optString("blok_kode") else null
                                                            val blokNama =
                                                                if (item.has("blok_nama") && !item.isNull(
                                                                        "blok_nama"
                                                                    )
                                                                ) item.optString("blok_nama") else null
                                                            val tphNomor =
                                                                if (item.has("tph_nomor") && !item.isNull(
                                                                        "tph_nomor"
                                                                    )
                                                                ) item.optInt("tph_nomor") else null
                                                            val ancak =
                                                                if (item.has("ancak") && !item.isNull(
                                                                        "ancak"
                                                                    )
                                                                ) item.optString("ancak") else null

                                                            // Create InspectionModel with new pemuat fields
                                                            val inspectionEntity = InspectionModel(
                                                                id = 0, // Always 0 for auto-increment
                                                                created_date = tglInspeksi,
                                                                created_by = createdBy.toString(),
                                                                created_name = createdName,
                                                                tph_id = tphId,
                                                                id_panen = idPanen,
                                                                dept = dept,
                                                                dept_ppro = deptPpro,
                                                                dept_abbr = deptAbbr,
                                                                dept_nama = deptNama,
                                                                divisi = divisi,
                                                                divisi_ppro = divisiPpro,
                                                                divisi_abbr = divisiAbbr,
                                                                divisi_nama = divisiNama,
                                                                blok = blok,
                                                                blok_ppro = blokPpro,
                                                                blok_kode = blokKode,
                                                                blok_nama = blokNama,
                                                                tph_nomor = tphNomor,
                                                                ancak = ancak,
                                                                foto_user = "",
                                                                jjg_panen = jjgPanen,
                                                                date_panen = tglPanen,
                                                                jalur_masuk = jalurMasuk,
                                                                jenis_kondisi = jenisInspeksi,
                                                                baris = baris,
                                                                jml_pkk_inspeksi = jmlPokokInspeksi,
                                                                tracking_path = trackingPath,
                                                                dataIsZipped = 0,
                                                                app_version = app_version,
                                                                status_upload = "0",
                                                                status_uploaded_image = "0",
                                                                isPushedToServer = 1,
                                                                // NEW: Add the 4 pemuat fields for follow-up inspection
                                                                kemandoran_ppro_pemuat = kemandoranPproPemuat,
                                                                kemandoran_nama_pemuat = kemandoranNamaPemuat,
                                                                nik_pemuat = nikPemuat,
                                                                nama_pemuat = namaPemuat
                                                            )

                                                            // Check if inspection has details - if not, skip the entire inspection
                                                            val inspectionDetails =
                                                                item.optJSONArray("InspeksiDetails")

                                                            if (inspectionDetails == null || inspectionDetails.length() == 0) {
                                                                AppLogger.d("Skipping inspection TPH=${inspectionEntity.tph_id} - no details found")
                                                                skippedCount++
                                                                continue // Skip this entire inspection record
                                                            }

                                                            // Direct insert without checking if record exists
                                                            val result =
                                                                inspectionDao.insertWithTransaction(
                                                                    inspectionEntity
                                                                )
                                                            if (result.isSuccess) {
                                                                // Get the newly inserted ID
                                                                val localInspectionId =
                                                                    inspectionDao.getInspectionByBusinessKey(
                                                                        inspectionEntity.tph_id,
                                                                        inspectionEntity.created_date,
                                                                        inspectionEntity.dept ?: 0,
                                                                        inspectionEntity.divisi
                                                                            ?: 0,
                                                                        1
                                                                    )?.id ?: 0

                                                                successCount++

                                                                // Insert all detail records
                                                                insertAllDetailRecordsFollowUp(
                                                                    inspectionDetails,
                                                                    localInspectionId,
                                                                    tglInspeksi
                                                                )

                                                            } else {
                                                                failCount++
                                                                continue // Skip processing details if inspection failed
                                                            }

                                                        } catch (e: Exception) {
                                                            AppLogger.e("Error processing follow-up inspection item: ${e.message}")
                                                            failCount++
                                                        }
                                                    }

                                                    if (dataArray.length() == 0) {
                                                        results[request.dataset] = Resource.Success(
                                                            response,
                                                            "Partial success: $successCount processed, $failCount failed, $skippedCount skipped"
                                                        )
                                                        prefManager!!.addDataset(request.dataset)
                                                    } else if (failCount == 0) {
                                                        results[request.dataset] = Resource.Success(
                                                            response,
                                                            "Follow-up inspection data processed successfully: $successCount records"
                                                        )
                                                        prefManager!!.addDataset(request.dataset)
                                                    } else if (successCount > 0) {
                                                        // Partial success
                                                        results[request.dataset] = Resource.Success(
                                                            response,
                                                            "Partial success: $successCount processed, $failCount failed, $skippedCount skipped"
                                                        )
                                                        prefManager!!.addDataset(request.dataset)
                                                    } else {
                                                        // Complete failure
                                                        results[request.dataset] = Resource.Error(
                                                            "Failed to process follow-up inspection data: $failCount/${dataArray.length()} records failed, $skippedCount skipped"
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    results[request.dataset] =
                                                        Resource.Error("Error processing follow-up inspection data: ${e.message}")
                                                }
                                            }
                                        } else {
                                            val errorMessage =
                                                jsonObject.optString("message", "Unknown error")
                                            results[request.dataset] =
                                                Resource.Error("API Error: $errorMessage")
                                            AppLogger.e("Follow-up Inspection API returned error: $errorMessage")
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error processing follow-up inspection JSON: ${e.message}")
                                        results[request.dataset] =
                                            Resource.Error("Error processing follow-up inspection data: ${e.message}")
                                    }
                                }
                                else if (request.dataset == AppUtils.DatasetNames.sinkronisasiDataPanen) {
                                    try {
                                        results[request.dataset] = Resource.Loading(60)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Parse the JSON response to extract the panen data
                                        val jsonObject = JSONObject(responseBodyString)

                                        if (jsonObject.optBoolean("success", false)) {
                                            val dataArray =
                                                jsonObject.optJSONArray("data") ?: JSONArray()
                                            val allPanenData =
                                                mutableListOf<PanenEntity>() // Collect ALL data
                                            val allKaryawanNikLists = mutableListOf<List<String>>()

                                            results[request.dataset] = Resource.Loading(70)
                                            _downloadStatuses.postValue(results.toMap())

//            AppLogger.d("Processing ${dataArray.length()} panen records for first-time download...")

                                            for (i in 0 until dataArray.length()) {
                                                val item = dataArray.getJSONObject(i)

                                                // Extract required fields
                                                val idPanen = item.optInt("id", 0)
                                                val tphId = item.optString("tph", "")
                                                val createdDate = item.optString("created_date", "")
                                                val createdBy = item.optInt("created_by", 0)
                                                val asistensi = item.optInt("asistensi", 0)
                                                val asistensiDept =
                                                    if (item.isNull("asistensi_dept")) {
                                                        null
                                                    } else {
                                                        item.optInt("asistensi_dept", 0)
                                                            .takeIf { it != 0 }
                                                    }

                                                val asistensiDeptNama =
                                                    if (item.isNull("asistensi_dept_nama")) {
                                                        null
                                                    } else {
                                                        item.optString("asistensi_dept_nama", "")
                                                            .takeIf {
                                                                it.isNotEmpty() && !it.equals(
                                                                    "null",
                                                                    ignoreCase = true
                                                                )
                                                            }
                                                    }
                                                val asistensiDivisi =
                                                    if (item.isNull("asistensi_divisi")) {
                                                        null
                                                    } else {
                                                        item.optInt("asistensi_divisi", 0)
                                                            .takeIf { it != 0 }
                                                    }
                                                val ancak = item.optInt("ancak", 0)
                                                val jenis_panen = item.optInt("tipe", 0)
                                                val jjgKirim = item.optInt("jjg_kirim", 0)
                                                val jjgMasak = item.optInt("jjg_masak", 0)
                                                val jjgMentah = item.optInt("jjg_mentah", 0)
                                                val jjgLewatMasak =
                                                    item.optInt("jjg_lewat_masak", 0)
                                                val jjgKosong = item.optInt("jjg_kosong", 0)
                                                val jjgAbnormal = item.optInt("jjg_abnormal", 0)
                                                val jjgBayar = item.optInt("jjg_bayar", 0)
                                                val spb_kode = item.optString("spb_kode", "").let {
                                                    if (it.equals(
                                                            "null",
                                                            ignoreCase = true
                                                        )
                                                    ) "" else it
                                                }
                                                val status_espb = item.optInt("status_espb", 0)
                                                val jjgJson = JSONObject().apply {
                                                    put(
                                                        "TO",
                                                        jjgMasak
                                                    )         // example: "TO" -> total masak
                                                    put("UN", jjgMentah)
                                                    put("OV", jjgLewatMasak)
                                                    put("EM", jjgKosong)
                                                    put("AB", jjgAbnormal)
                                                    put("KP", jjgKirim)
                                                    put("PA", jjgBayar)
                                                }.toString()
                                                val createdName = item.optString("created_name", "")
                                                val username =
                                                    if (createdName.isNullOrEmpty() || createdName.equals(
                                                            "NULL",
                                                            ignoreCase = true
                                                        )
                                                    ) {
                                                        ""
                                                    } else {
                                                        extractUsernameFromCreatedName(createdName)
                                                    }

                                                // Parse kemandoran JSON to extract kemandoran_id and karyawan info
                                                val kemandoranString =
                                                    item.optString("kemandoran", "")
                                                var kemandoranId = ""
                                                var karyawanNikList = mutableListOf<String>()
                                                var karyawanNamaList = mutableListOf<String>()

                                                // Enhanced kemandoran checking
                                                if (kemandoranString.isNotEmpty() &&
                                                    kemandoranString != "null" &&
                                                    !item.isNull("kemandoran")
                                                ) {
                                                    try {
                                                        val kemandoranArray =
                                                            JSONArray(kemandoranString)
                                                        if (kemandoranArray.length() > 0) {
                                                            val kemandoranObj =
                                                                kemandoranArray.getJSONObject(0)
                                                            kemandoranId =
                                                                kemandoranObj.optString("id", "")

                                                            val pemanenArray =
                                                                kemandoranObj.optJSONArray("pemanen")
                                                            if (pemanenArray != null) {
                                                                for (j in 0 until pemanenArray.length()) {
                                                                    val pemanenObj =
                                                                        pemanenArray.getJSONObject(j)
                                                                    val nik = pemanenObj.optString(
                                                                        "nik",
                                                                        ""
                                                                    )
                                                                    val nama = pemanenObj.optString(
                                                                        "nama",
                                                                        ""
                                                                    )
                                                                    if (nik.isNotEmpty()) {
                                                                        karyawanNikList.add(nik)
                                                                        karyawanNamaList.add(nama)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        // Error handling
                                                    }
                                                }

                                                // Store NIK list for bulk karyawan lookup
                                                allKaryawanNikLists.add(karyawanNikList.toList())

                                                // Create entity with empty karyawan_id (will be filled after bulk lookup)
                                                val panenEntity = PanenEntity(
                                                    id = idPanen,
                                                    tph_id = tphId.toString(),
                                                    date_created = createdDate,
                                                    created_name = createdName,
                                                    created_by = createdBy,
                                                    karyawan_id = "", // Will be filled after bulk lookup
                                                    kemandoran_id = kemandoranId,
                                                    karyawan_nik = karyawanNikList.joinToString(","),
                                                    karyawan_nama = karyawanNamaList.joinToString(","),
                                                    jjg_json = jjgJson,
                                                    foto = "",
                                                    komentar = "",
                                                    asistensi = asistensi,
                                                    asistensi_dept = asistensiDept,
                                                    asistensi_dept_nama = asistensiDeptNama,
                                                    asistensi_divisi = asistensiDivisi,
                                                    lat = 0.0,
                                                    lon = 0.0,
                                                    jenis_panen = jenis_panen,
                                                    ancak = ancak,
                                                    info = "",
                                                    archive = 0,
                                                    status_banjir = 0,
                                                    status_espb = status_espb,
                                                    nomor_pemanen = 0,
                                                    status_restan = 0,
                                                    scan_status = 1,
                                                    dataIsZipped = 0,
                                                    no_espb = spb_kode,
                                                    username = username,
                                                    status_upload = 0,
                                                    status_uploaded_image = "0",
                                                    status_pengangkutan = 0,
                                                    status_insert_mpanen = 0,
                                                    status_scan_mpanen = 0,
                                                    jumlah_pemanen = karyawanNikList.size,
                                                    archive_mpanen = 0,
                                                    isPushedToServer = 1
                                                )

                                                allPanenData.add(panenEntity)
                                            }

                                            results[request.dataset] = Resource.Loading(75)
                                            _downloadStatuses.postValue(results.toMap())

                                            // STEP 2: BULK karyawan lookup for ALL records at once
                                            val allNiks = allKaryawanNikLists.flatten().distinct()
                                            val karyawanMap = if (allNiks.isNotEmpty()) {
                                                try {
                                                    karyawanDao.getKaryawanByNikList(allNiks)
                                                        .associateBy { it.nik }
                                                        .mapValues { it.value.id?.toString() ?: "" }
                                                } catch (e: Exception) {
//                    AppLogger.e("Error getting bulk karyawan data: ${e.message}")
                                                    emptyMap()
                                                }
                                            } else {
                                                emptyMap()
                                            }

                                            // STEP 3: Update karyawan_id for ALL entities
                                            allPanenData.forEachIndexed { index, panen ->
                                                val nikList = allKaryawanNikLists[index]
                                                val karyawanIdList = nikList.mapNotNull { nik ->
                                                    karyawanMap[nik]?.takeIf { it.isNotEmpty() }
                                                }
                                                allPanenData[index] = panen.copy(
                                                    karyawan_id = karyawanIdList.joinToString(",")
                                                )
                                            }

//            AppLogger.d("Processing ${allPanenData.size} records for bulk insert...")

                                            results[request.dataset] = Resource.Loading(80)
                                            _downloadStatuses.postValue(results.toMap())

                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val startTime = System.currentTimeMillis()

                                                    // STEP 4: BULK INSERT ALL RECORDS (NO COMPARISON NEEDED FOR FIRST TIME)
                                                    if (allPanenData.isNotEmpty()) {
                                                        panenDao.insertBatch(allPanenData)
//                        AppLogger.d("Bulk inserted ${allPanenData.size} panen records successfully!")
                                                    }

                                                    val endTime = System.currentTimeMillis()
                                                    val processingTime =
                                                        (endTime - startTime) / 1000.0

//                    AppLogger.d("=== PANEN SYNC COMPLETE ===")
//                    AppLogger.d("Total records processed: ${allPanenData.size}")
//                    AppLogger.d("All records inserted successfully!")
//                    AppLogger.d("Processing time: ${processingTime}s")
//                    AppLogger.d("Records per second: ${if (processingTime > 0) (allPanenData.size / processingTime).toInt() else 0}")
//                    AppLogger.d("============================")

                                                    // Set final result - since it's first time, everything should be successful
                                                    if (allPanenData.isEmpty()) {
                                                        // No records to process - everything is up to date
                                                        results[request.dataset] =
                                                            Resource.UpToDate(request.dataset)
//                        AppLogger.d("No records to process - dataset is up to date")
                                                    } else {
                                                        results[request.dataset] = Resource.Success(
                                                            response,
                                                            "Panen data downloaded successfully: ${allPanenData.size} records in ${processingTime}s"
                                                        )
                                                        prefManager!!.addDataset(request.dataset)
                                                    }
                                                } catch (e: Exception) {
//                    AppLogger.e("Error inserting panen data: ${e.message}")
                                                    results[request.dataset] =
                                                        Resource.Error("Error inserting panen data: ${e.message}")
                                                }
                                            }
                                        } else {
                                            val errorMessage =
                                                jsonObject.optString("message", "Unknown error")
                                            results[request.dataset] =
                                                Resource.Error("API Error: $errorMessage")
//            AppLogger.e("Panen API returned error: $errorMessage")
                                        }
                                    } catch (e: Exception) {
//        AppLogger.e("Error processing panen JSON: ${e.message}")
                                        results[request.dataset] =
                                            Resource.Error("Error processing panen data: ${e.message}")
                                    }
                                }
                                else if (request.dataset == AppUtils.DatasetNames.settingJSON) {
                                    AppLogger.d("Processing settingJSON dataset")

                                    if (responseBodyString.isBlank()) {
                                        AppLogger.e("Received empty JSON response for settingJSON")
                                        results[request.dataset] =
                                            Resource.Error("Empty JSON response")
                                        _downloadStatuses.postValue(results.toMap())
                                    } else {
                                        AppLogger.d("settingJSON response body length: ${responseBodyString.length}")
                                        AppLogger.d("settingJSON response content: $responseBodyString")

                                        try {
                                            val jsonObject = JSONObject(responseBodyString)
                                            AppLogger.d("Successfully parsed JSON object")

                                            // Check if settings are up to date
                                            val message = jsonObject.optString("message", "")
                                            AppLogger.d("Server message: $message")

                                            if (message.contains("up to date", ignoreCase = true)) {
                                                // Settings are up to date - this is SUCCESS
                                                AppLogger.d("Settings are up to date - treating as success")

                                                results[request.dataset] =
                                                    Resource.Success(response)
                                                _downloadStatuses.postValue(results.toMap())

                                                // Add dataset since the request was successful
                                                prefManager!!.addDataset(request.dataset)
                                                AppLogger.d("Settings up to date - dataset added successfully")

                                            } else {
                                                // Settings have updates - process tph_radius and gps_accuracy
                                                AppLogger.d("Settings have updates - processing new values")

                                                val tphRadius = jsonObject.optInt("tph_radius", -1)
                                                val gpsAccuracy =
                                                    jsonObject.optInt("gps_accuracy", -1)
                                                AppLogger.d("Parsed values - tphRadius: $tphRadius, gpsAccuracy: $gpsAccuracy")

                                                var isStored = false

                                                if (tphRadius != -1) {
                                                    prefManager.radiusMinimum = tphRadius.toFloat()
                                                    AppLogger.d("Stored tphRadius: ${tphRadius.toFloat()}")
                                                    isStored = true
                                                }

                                                if (gpsAccuracy != -1) {
                                                    prefManager.boundaryAccuracy =
                                                        gpsAccuracy.toFloat()
                                                    AppLogger.d("Stored gpsAccuracy: ${gpsAccuracy.toFloat()}")
                                                    isStored = true
                                                }

                                                if (isStored) {
                                                    results[request.dataset] =
                                                        Resource.Success(response)
                                                    _downloadStatuses.postValue(results.toMap())

                                                    prefManager!!.addDataset(request.dataset)
                                                    AppLogger.d("New settings stored and dataset added")
                                                } else {
                                                    AppLogger.w("No valid settings data found in response")
                                                    results[request.dataset] =
                                                        Resource.Error("No valid settings data")
                                                    _downloadStatuses.postValue(results.toMap())
                                                }
                                            }

                                        } catch (e: JSONException) {
                                            AppLogger.e("Error parsing JSON for settingJSON: ${e.message}")
                                            results[request.dataset] =
                                                Resource.Error("Error parsing JSON: ${e.message}")
                                            _downloadStatuses.postValue(results.toMap())
                                        }
                                    }

                                    AppLogger.d("Finished processing settingJSON dataset")
                                }
                                else if (request.dataset == AppUtils.DatasetNames.checkAppVersion) {
                                    if (responseBodyString.isBlank()) {
                                        AppLogger.e("Received empty JSON response for app version")
                                        results[request.dataset] =
                                            Resource.Error("Empty JSON response")
                                        _downloadStatuses.postValue(results.toMap())
                                    }

                                    try {
                                        // Log the complete JSON response
                                        AppLogger.d("App Version JSON Response: $responseBodyString")

                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Parse JSON and extract required_version
                                        val jsonObject = JSONObject(responseBodyString)
                                        val success = jsonObject.optBoolean("success", false)

                                        if (success) {
                                            val dataArray = jsonObject.optJSONArray("data")
                                            if (dataArray != null && dataArray.length() > 0) {
                                                val firstItem = dataArray.getJSONObject(0)
                                                val requiredVersion =
                                                    firstItem.optString("required_version", "")

                                                if (requiredVersion.isNotEmpty()) {
                                                    prefManager!!.latestAppVersionSystem =
                                                        requiredVersion
                                                    AppLogger.d("Stored latest app version: $requiredVersion")
                                                } else {
                                                    AppLogger.w("Required version is empty in response")
                                                }
                                            } else {
                                                AppLogger.w("No data array or empty data in response")
                                            }
                                        } else {
                                            AppLogger.w("API response success is false")
                                        }

                                        prefManager!!.addDataset(request.dataset)
                                        results[request.dataset] = Resource.Success(response)
                                        _downloadStatuses.postValue(results.toMap())

                                    } catch (e: JSONException) {
                                        AppLogger.e(
                                            "Error parsing app version JSON: ${e.message}",
                                            e.toString()
                                        )
                                        results[request.dataset] =
                                            Resource.Error("Error parsing JSON: ${e.message}")
                                        _downloadStatuses.postValue(results.toMap())
                                    }
                                } else if (request.dataset == AppUtils.DatasetNames.parameter) {
                                    try {
                                        // Use your existing parseParameter function
                                        val parameterList = parseParameter(responseBodyString)

                                        Log.d(
                                            "DownloadResponse",
                                            "Parsed ${parameterList.size} parameter records"
                                        )

                                        // Update status to storing
                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Store the data using your existing repository method
                                        updateOrInsertParameter(parameterList).join()

                                        // Check if parameter status flow exists, if not create one or use a generic approach
                                        // Assuming you have parameterStatus flow, if not you can create it or use a different approach
                                        if (parameterStatus.value.isSuccess) {
                                            results[request.dataset] = Resource.Success(response)
                                            prefManager!!.addDataset(request.dataset)

                                            // Store last modified timestamp if available
//                                            lastModified?.let {
//                                                prefManager.lastModifiedDatasetParameter = it
//                                            }
                                        } else {
                                            val error = parameterStatus.value.exceptionOrNull()
                                            throw error ?: Exception("Unknown database error")
                                        }

                                        _downloadStatuses.postValue(results.toMap())

                                    } catch (e: Exception) {
                                        Log.e(
                                            "DownloadResponse",
                                            "Error processing parameter data: ${e.message}"
                                        )
                                        if (!hasShownError) {
                                            results[request.dataset] =
                                                Resource.Error("Error processing parameter data: ${e.message}")
                                            hasShownError = true
                                        }
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
                                }
                                else if (request.dataset == AppUtils.DatasetNames.hektaran) {
                                    if (responseBodyString.isBlank()) {
                                        AppLogger.e("Received empty JSON response for hektaran")
                                        results[request.dataset] =
                                            Resource.Error("Empty JSON response")
                                        _downloadStatuses.postValue(results.toMap())
                                    }

                                    try {
                                        // Log the complete JSON response
                                        AppLogger.d("hektaran JSON Response: $responseBodyString")

                                        // Show storing status
                                        results[request.dataset] = Resource.Storing(request.dataset)
                                        _downloadStatuses.postValue(results.toMap())

                                        // Parse JSON
                                        val jsonObject = JSONObject(responseBodyString)
                                        val success = jsonObject.optBoolean("success", false)

                                        if (success) {
                                            val dataArray = jsonObject.optJSONArray("data")

                                            if (dataArray != null && dataArray.length() > 0) {

                                                // ======================
                                                // 🔹 STEP 1: Filter Hektaran with Valid date_panen
                                                // ======================

                                                val validHektaranArray = JSONArray()
                                                var skippedCount = 0

                                                for (i in 0 until dataArray.length()) {
                                                    val item = dataArray.getJSONObject(i)
                                                    val blokKode = item.optString("blok_kode", "")
                                                    val details = item.optJSONArray("HektaranDetails")

                                                    // Find valid date_panen entries
                                                    val validDates = mutableListOf<String>()
                                                    if (details != null) {
                                                        for (j in 0 until details.length()) {
                                                            val d = details.getJSONObject(j)
                                                            val datePanenRaw =
                                                                d.opt("date_panen") // this keeps it as nullable Any
                                                            val datePanen =
                                                                if (datePanenRaw != null && datePanenRaw != JSONObject.NULL) {
                                                                    datePanenRaw.toString().trim()
                                                                } else {
                                                                    ""
                                                                }

                                                            // ✅ Exclude blank, "null", and null values
                                                            if (datePanen.isNotBlank() && !datePanen.equals(
                                                                    "null",
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                validDates.add(datePanen)
                                                            }
                                                        }
                                                    }

                                                    if (validDates.isEmpty()) {
                                                        skippedCount++
                                                    } else {
                                                        AppLogger.d(
                                                            "✅ Valid Hektaran id=${item.optInt("id")} blok=$blokKode → date_panen=${
                                                                validDates.joinToString(
                                                                    ","
                                                                )
                                                            }"
                                                        )
                                                        validHektaranArray.put(item)
                                                    }
                                                }

                                                AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                AppLogger.d("📦 Raw Server Hektaran: ${dataArray.length()}")
                                                AppLogger.d("📊 Filtered Hektaran (valid date_panen): ${validHektaranArray.length()}")
                                                AppLogger.d("⏩ Skipped Hektaran (no valid date_panen): $skippedCount")
                                                AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                val filteredDataArray = validHektaranArray

                                                // ======================
                                                // 🔹 STEP 2: Load Local Panen Data
                                                // ======================

                                                val formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                                                val today = LocalDate.now()
                                                val startDate = today.minusDays(2).format(formatterDate) + " 00:00:00"
                                                val endDate = today.format(formatterDate) + " 23:59:59"

                                                val afdelingId = request.afdeling.toString().toIntOrNull() ?: 0
                                                val panenList = panenDao.findPanenLast3DaysByAfdeling(
                                                    startDate,
                                                    endDate,
                                                    afdelingId
                                                )

                                                AppLogger.d("📦 Loaded Local Panen (with relations): ${panenList.size}")

                                                // ======================
                                                // 🔹 STEP 3: Parse & Flatten Server Hektaran
                                                // ======================

                                                data class HektaranDetailRecord(
                                                    val datePanen: LocalDate,
                                                    val blokKode: String,
                                                    val kemandoranId: Int,
                                                    val nik: String,
                                                    val nama: String,
                                                    val jjgMasak: Double,
                                                    val jjgMentah: Double,
                                                    val jjgLewatMasak: Double,
                                                    val jjgKosong: Double,
                                                    val jjgAbnormal: Double,
                                                    val jjgBayar: Double,
                                                    val jjgKirim: Double
                                                )

                                                val gson = Gson()
                                                val dateArrayType = object : TypeToken<List<String>>() {}.type

                                                val hektaranRecords = mutableListOf<HektaranDetailRecord>()

                                                for (i in 0 until filteredDataArray.length()) {
                                                    val header = filteredDataArray.getJSONObject(i)
                                                    val blokKode = header.optString("blok_kode", "")
                                                    val details = header.optJSONArray("HektaranDetails") ?: continue

                                                    for (j in 0 until details.length()) {
                                                        val d = details.getJSONObject(j)

                                                        // Parse "date_panen" (string that looks like ["2025-10-22 08:42:55", ...])
                                                        val rawDatePanen = d.optString("date_panen", "")
                                                        val dateList: List<String> = try {
                                                            gson.fromJson(rawDatePanen, dateArrayType)
                                                        } catch (e: Exception) {
                                                            emptyList()
                                                        }

                                                        for (dateStr in dateList) {
                                                            val dateParsed = try {
                                                                LocalDate.parse(dateStr.substring(0, 10), formatterDate)
                                                            } catch (e: Exception) {
                                                                continue
                                                            }

                                                            val record = HektaranDetailRecord(
                                                                datePanen = dateParsed,
                                                                blokKode = blokKode,
                                                                kemandoranId = d.optInt("kemandoran_ppro", 0),
                                                                nik = d.optString("pemanen_nik", ""),
                                                                nama = d.optString("pemanen_nama", ""),
                                                                jjgMasak = d.optDouble("jjg_masak", 0.0),
                                                                jjgMentah = d.optDouble("jjg_mentah", 0.0),
                                                                jjgLewatMasak = d.optDouble("jjg_lewat_masak", 0.0),
                                                                jjgKosong = d.optDouble("jjg_kosong", 0.0),
                                                                jjgAbnormal = d.optDouble("jjg_abnormal", 0.0),
                                                                jjgBayar = d.optDouble("jjg_panen", 0.0),
                                                                jjgKirim = d.optDouble("jjg_kirim", 0.0)
                                                            )
                                                            hektaranRecords.add(record)
                                                        }
                                                    }
                                                }

                                                AppLogger.d("📊 Flattened Server Hektaran Records: ${hektaranRecords.size}")
                                                AppLogger.d("👥 Unique Names in Hektaran: ${hektaranRecords.map { it.nama }.distinct().joinToString()}")

                                                // ======================
                                                // 🔹 STEP 4: Group Local Panen by (date, blok, nik)
                                                // ======================

                                                data class PanenKey(val date: LocalDate, val blokKode: String, val nik: String)
                                                data class GroupedPanen(
                                                    val key: PanenKey,
                                                    val panenList: List<PanenEntityWithRelations>,
                                                    val totalJjgMasak: Double,
                                                    val totalJjgMentah: Double,
                                                    val totalJjgLewatMasak: Double,
                                                    val totalJjgKosong: Double,
                                                    val totalJjgAbnormal: Double,
                                                    val totalJjgBayar: Double,
                                                    val totalJjgKirim: Double
                                                )

                                                val groupedPanenMap = panenList.groupBy { panen ->
                                                    val date = try {
                                                        LocalDate.parse(panen.panen.date_created.substring(0, 10), formatterDate)
                                                    } catch (_: Exception) {
                                                        LocalDate.MIN
                                                    }
                                                    PanenKey(date, panen.tph?.blok_kode ?: "", panen.panen.karyawan_nik)
                                                }.mapValues { (key, panens) ->
                                                    var sumMasak = 0.0
                                                    var sumMentah = 0.0
                                                    var sumLewat = 0.0
                                                    var sumKosong = 0.0
                                                    var sumAbnormal = 0.0
                                                    var sumBayar = 0.0
                                                    var sumKirim = 0.0

                                                    panens.forEach { panen ->
                                                        val jjgJson = try {
                                                            JSONObject(panen.panen.jjg_json ?: "{}")
                                                        } catch (_: Exception) {
                                                            JSONObject()
                                                        }
                                                        sumMasak += jjgJson.optDouble("TO", 0.0)
                                                        sumMentah += jjgJson.optDouble("UN", 0.0)
                                                        sumLewat += jjgJson.optDouble("OV", 0.0)
                                                        sumKosong += jjgJson.optDouble("EM", 0.0)
                                                        sumAbnormal += jjgJson.optDouble("AB", 0.0)
                                                        sumBayar += jjgJson.optDouble("PA", 0.0)
                                                        sumKirim += jjgJson.optDouble("KP", 0.0)
                                                    }

                                                    GroupedPanen(key, panens, sumMasak, sumMentah, sumLewat, sumKosong, sumAbnormal, sumBayar, sumKirim)
                                                }

                                                AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                AppLogger.d("📦 Total Local Panen Records: ${panenList.size}")
                                                AppLogger.d("📊 Grouped Local Panen Keys: ${groupedPanenMap.size}")
                                                AppLogger.d("📊 Server Hektaran Records: ${hektaranRecords.size}")
                                                AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                // ======================
                                                // 🔹 STEP 5: Create Hektaran Index with DEDUPLICATION
                                                // ======================

                                                data class HektaranKey(val date: LocalDate, val blokKode: String, val nik: String)

                                                val hektaranMap = hektaranRecords.groupBy { hekt ->
                                                    HektaranKey(hekt.datePanen, hekt.blokKode, hekt.nik)
                                                }.mapValues { (key, hektList) ->
                                                    // ⚠️ Check if all records have identical values (duplicates) or need summing
                                                    val first = hektList.first()
                                                    val allIdentical = hektList.all {
                                                        it.jjgMasak == first.jjgMasak &&
                                                                it.jjgMentah == first.jjgMentah &&
                                                                it.jjgLewatMasak == first.jjgLewatMasak &&
                                                                it.jjgKosong == first.jjgKosong &&
                                                                it.jjgAbnormal == first.jjgAbnormal &&
                                                                it.jjgBayar == first.jjgBayar &&
                                                                it.jjgKirim == first.jjgKirim
                                                    }

                                                    if (allIdentical && hektList.size > 1) {
                                                        AppLogger.d("⚠️ DUPLICATE detected: ${first.nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | count=${hektList.size} → using single value (${first.jjgMasak})")
                                                        // Use only one record's values (they're all the same)
                                                        mapOf(
                                                            "masak" to first.jjgMasak,
                                                            "mentah" to first.jjgMentah,
                                                            "lewat" to first.jjgLewatMasak,
                                                            "kosong" to first.jjgKosong,
                                                            "abnormal" to first.jjgAbnormal,
                                                            "bayar" to first.jjgBayar,
                                                            "kirim" to first.jjgKirim
                                                        )
                                                    } else {
                                                        // Sum all records (they have different values)
                                                        var sumMasak = 0.0
                                                        var sumMentah = 0.0
                                                        var sumLewat = 0.0
                                                        var sumKosong = 0.0
                                                        var sumAbnormal = 0.0
                                                        var sumBayar = 0.0
                                                        var sumKirim = 0.0

                                                        hektList.forEach { h ->
                                                            sumMasak += h.jjgMasak
                                                            sumMentah += h.jjgMentah
                                                            sumLewat += h.jjgLewatMasak
                                                            sumKosong += h.jjgKosong
                                                            sumAbnormal += h.jjgAbnormal
                                                            sumBayar += h.jjgBayar
                                                            sumKirim += h.jjgKirim
                                                        }

                                                        if (hektList.size > 1) {
                                                            AppLogger.d("📊 SUMMING: ${first.nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | count=${hektList.size} → total=$sumMasak")
                                                        }

                                                        mapOf(
                                                            "masak" to sumMasak,
                                                            "mentah" to sumMentah,
                                                            "lewat" to sumLewat,
                                                            "kosong" to sumKosong,
                                                            "abnormal" to sumAbnormal,
                                                            "bayar" to sumBayar,
                                                            "kirim" to sumKirim
                                                        )
                                                    }
                                                }

                                                // ======================
                                                // 🔹 STEP 6: Find Local Panen NOT in Hektaran (need to upload)
                                                // ======================

                                                val notInHektaranList = mutableListOf<PanenEntityWithRelations>()
                                                val alreadyInHektaranList = mutableListOf<PanenEntityWithRelations>()

                                                fun Double.eq(other: Double) = kotlin.math.abs(this - other) < 0.001

                                                groupedPanenMap.forEach { (key, grouped) ->
                                                    val hektaranData = hektaranMap[HektaranKey(key.date, key.blokKode, key.nik)]

                                                    if (hektaranData != null) {
                                                        // Check if JJG values match
                                                        val jjgMatch =
                                                            grouped.totalJjgMasak.eq(hektaranData["masak"] ?: 0.0) &&
                                                                    grouped.totalJjgMentah.eq(hektaranData["mentah"] ?: 0.0) &&
                                                                    grouped.totalJjgLewatMasak.eq(hektaranData["lewat"] ?: 0.0) &&
                                                                    grouped.totalJjgKosong.eq(hektaranData["kosong"] ?: 0.0) &&
                                                                    grouped.totalJjgAbnormal.eq(hektaranData["abnormal"] ?: 0.0) &&
                                                                    grouped.totalJjgBayar.eq(hektaranData["bayar"] ?: 0.0) &&
                                                                    grouped.totalJjgKirim.eq(hektaranData["kirim"] ?: 0.0)

                                                        if (jjgMatch) {
                                                            alreadyInHektaranList.addAll(grouped.panenList)
                                                            AppLogger.d("✅ Already in Hektaran — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | records=${grouped.panenList.size}")
                                                        } else {
                                                            notInHektaranList.addAll(grouped.panenList)
                                                            AppLogger.d("⚠️ JJG Mismatch — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date}")
                                                            AppLogger.d("   📊 Local:  TO=${grouped.totalJjgMasak}, PA=${grouped.totalJjgBayar}, KP=${grouped.totalJjgKirim}")
                                                            AppLogger.d("   📊 Server: TO=${hektaranData["masak"]}, PA=${hektaranData["bayar"]}, KP=${hektaranData["kirim"]}")
                                                        }
                                                    } else {
                                                        // Not found in hektaran server → needs to be uploaded!
                                                        notInHektaranList.addAll(grouped.panenList)
                                                        AppLogger.d("🆕 NOT in Hektaran (new) — ${grouped.panenList.first().panen.karyawan_nama} (${key.nik}) | blok=${key.blokKode} | date=${key.date} | records=${grouped.panenList.size}")
                                                    }
                                                }

                                                // ======================
                                                // 🔹 STEP 7: Summary & Prepare Upload
                                                // ======================

                                                AppLogger.d("──────────── SUMMARY ────────────")
                                                AppLogger.d("📋 Total Local Panen: ${panenList.size}")
                                                AppLogger.d("✅ Already in Hektaran: ${alreadyInHektaranList.size}")
                                                AppLogger.d("🆕 NOT in Hektaran (need upload): ${notInHektaranList.size}")
                                                AppLogger.d("─────────────────────────────────")

                                                if (notInHektaranList.isNotEmpty()) {
                                                    notInHektaranList.forEachIndexed { index, panen ->
                                                        AppLogger.d("  ${index + 1}. ${panen.panen.karyawan_nama} (${panen.panen.karyawan_nik}) | blok=${panen.tph?.blok_kode} | date=${panen.panen.date_created}")
                                                        AppLogger.d("     🔸 local_jjg_json=${panen.panen.jjg_json}")
                                                    }
                                                }

                                                val saveDataMPanenList = notInHektaranList.map { relation ->
                                                    val p = relation.panen
                                                    PanenEntity(
                                                        tph_id = p.tph_id ?: "0",
                                                        date_created = p.date_created,
                                                        karyawan_nik = p.karyawan_nik ?: "",
                                                        jjg_json = p.jjg_json ?: "",
                                                        foto = p.foto ?: "",
                                                        komentar = p.komentar ?: "",
                                                        asistensi = p.asistensi ?: 0,
                                                        lat = p.lat ?: 0.0,
                                                        lon = p.lon ?: 0.0,
                                                        jenis_panen = p.jenis_panen ?: 0,
                                                        ancak = p.ancak ?: 0,
                                                        info = p.info ?: "NULL",
                                                        scan_status = p.scan_status ?: 0,
                                                        nomor_pemanen = p.nomor_pemanen ?: 0,
                                                        dataIsZipped = p.dataIsZipped ?: 0,
                                                        created_by = p.created_by ?: 0,
                                                        karyawan_id = p.karyawan_id ?: "NULL",
                                                        kemandoran_id = p.kemandoran_id ?: "",
                                                        karyawan_nama = p.karyawan_nama ?: "NULL",
                                                        jumlah_pemanen = p.jumlah_pemanen ?: 1,
                                                        status_scan_mpanen = 1
                                                    )
                                                }

                                                AppLogger.d("💾 Ready to save new Panen count: ${saveDataMPanenList.size}")
                                                AppLogger.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")



                                                val result = appRepository.saveScanMPanen(
                                                    saveDataMPanenList,
                                                    request.createdBy.toString(),
                                                    request.creatorInfo,
                                                    null
                                                )

                                                result.fold(
                                                    onSuccess = { saveResult ->
                                                        when (saveResult) {
                                                            is SaveTPHResult.AllSuccess -> {




                                                                prefManager?.addDataset(request.dataset)
                                                                results[request.dataset] = Resource.Success(
                                                                    response,
                                                                    "Data Hektaran baru berhasil tersimpan!"
                                                                )
                                                                _downloadStatuses.postValue(results.toMap())
                                                            }

                                                            is SaveTPHResult.PartialSuccess -> {




                                                                AppLogger.d("Partial duplicates:\n${saveResult.duplicateInfo}")

                                                                prefManager?.addDataset(request.dataset)
                                                                results[request.dataset] = Resource.Success(
                                                                    response,
                                                                    "Sebagian data berhasil disimpan (${saveResult.duplicateCount} duplikat)"
                                                                )
                                                                _downloadStatuses.postValue(results.toMap())
                                                            }

                                                            is SaveTPHResult.AllDuplicate -> {




                                                                prefManager?.addDataset(request.dataset)
                                                                results[request.dataset] = Resource.Success(
                                                                    response,
                                                                    "Semua data duplikat, tidak ada hektaran baru tersimpan!"
                                                                )
                                                                _downloadStatuses.postValue(results.toMap())
                                                            }
                                                        }
                                                    },
                                                    onFailure = { exception ->

                                                        AppLogger.d("Error saving hektaran: ${exception.message}")



                                                        prefManager?.addDataset(request.dataset)
                                                        results[request.dataset] = Resource.Success(
                                                            response,
                                                            "Semua data duplikat, tidak ada hektaran baru tersimpan!"
                                                        )
                                                        _downloadStatuses.postValue(results.toMap())
                                                    }
                                                )

                                            } else {
                                                AppLogger.w("No data array or empty data in response")
                                                results[request.dataset] = Resource.Error("No data in response")
                                                _downloadStatuses.postValue(results.toMap())
                                            }
                                        } else {
                                            AppLogger.w("API response success is false")
                                            results[request.dataset] = Resource.Error("API response failed")
                                            _downloadStatuses.postValue(results.toMap())
                                        }

                                    } catch (e: JSONException) {
                                        AppLogger.e("Error parsing hektaran JSON: ${e.message}", e.toString())
                                        results[request.dataset] = Resource.Error("Error parsing JSON: ${e.message}")
                                        _downloadStatuses.postValue(results.toMap())
                                    } catch (e: Exception) {
                                        AppLogger.e("Error processing hektaran response: ${e.message}")
                                        results[request.dataset] = Resource.Error("Error processing: ${e.message}")
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
                                                val jsonObject = gson.fromJson(
                                                    jsonContent,
                                                    JsonObject::class.java
                                                )

                                                // Check if the JSON has the expected structure
                                                if (!jsonObject.has("data") || jsonObject.get("data").isJsonNull) {
                                                    Log.e(
                                                        "EstateParser",
                                                        "JSON does not contain 'data' field or it's null"
                                                    )
                                                    return
                                                }

                                                val dataArray = jsonObject.getAsJsonArray("data")

                                                for (i in 0 until dataArray.size()) {
                                                    try {
                                                        val estateObject =
                                                            dataArray.get(i).asJsonObject

                                                        // Safely extract estate values with null checks
                                                        val id =
                                                            if (estateObject.has("id") && !estateObject.get(
                                                                    "id"
                                                                ).isJsonNull
                                                            )
                                                                estateObject.get("id").asInt else 0

                                                        val idPpro =
                                                            if (estateObject.has("id_ppro") && !estateObject.get(
                                                                    "id_ppro"
                                                                ).isJsonNull
                                                            )
                                                                estateObject.get("id_ppro").asInt else null

                                                        val abbr =
                                                            if (estateObject.has("abbr") && !estateObject.get(
                                                                    "abbr"
                                                                ).isJsonNull
                                                            )
                                                                estateObject.get("abbr").asString else null

                                                        val nama =
                                                            if (estateObject.has("nama") && !estateObject.get(
                                                                    "nama"
                                                                ).isJsonNull
                                                            )
                                                                estateObject.get("nama").asString else null

                                                        val tph_otomatis =
                                                            if (estateObject.has("tph_otomatis") && !estateObject.get(
                                                                    "tph_otomatis"
                                                                ).isJsonNull
                                                            )
                                                                estateObject.get("tph_otomatis").asString else null

                                                        // Create EstateModel
                                                        val estate = EstateModel(
                                                            id = id,
                                                            id_ppro = idPpro,
                                                            abbr = abbr,
                                                            nama = nama,
                                                            tph_otomatis = tph_otomatis!!.toInt()
                                                        )
                                                        estateList.add(estate)

                                                        // Parse afdeling data if present
                                                        if (estateObject.has("divisi") && !estateObject.get(
                                                                "divisi"
                                                            ).isJsonNull
                                                        ) {
                                                            val divisiArray =
                                                                estateObject.getAsJsonArray("divisi")

                                                            for (j in 0 until divisiArray.size()) {
                                                                try {
                                                                    val divisiObject =
                                                                        divisiArray.get(j).asJsonObject

                                                                    val divisiId =
                                                                        if (divisiObject.has("id") && !divisiObject.get(
                                                                                "id"
                                                                            ).isJsonNull
                                                                        )
                                                                            divisiObject.get("id").asInt else continue

                                                                    val divisiIdPpro =
                                                                        if (divisiObject.has("id_ppro") && !divisiObject.get(
                                                                                "id_ppro"
                                                                            ).isJsonNull
                                                                        )
                                                                            divisiObject.get("id_ppro").asInt else null

                                                                    val divisiAbbr =
                                                                        if (divisiObject.has("abbr") && !divisiObject.get(
                                                                                "abbr"
                                                                            ).isJsonNull
                                                                        )
                                                                            divisiObject.get("abbr").asString?.trim() else null

                                                                    val divisiNama =
                                                                        if (divisiObject.has("nama") && !divisiObject.get(
                                                                                "nama"
                                                                            ).isJsonNull
                                                                        )
                                                                            divisiObject.get("nama").asString else null

                                                                    val sinkronisasi_otomatis =
                                                                        if (divisiObject.has("sinkronisasi_otomatis") && !divisiObject.get(
                                                                                "sinkronisasi_otomatis"
                                                                            ).isJsonNull
                                                                        )
                                                                            divisiObject.get("sinkronisasi_otomatis").asInt else 0

                                                                    val afdeling = AfdelingModel(
                                                                        id = divisiId,
                                                                        id_ppro = divisiIdPpro,
                                                                        abbr = divisiAbbr,
                                                                        nama = divisiNama,
                                                                        estate_id = id,
                                                                        sinkronisasi_otomatis = sinkronisasi_otomatis
                                                                    )
                                                                    afdelingList.add(afdeling)
                                                                } catch (e: Exception) {
                                                                    Log.e(
                                                                        "AfdelingParser",
                                                                        "Error parsing afdeling at index $j for estate $id: ${e.message}"
                                                                    )
                                                                    continue
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(
                                                            "EstateParser",
                                                            "Error parsing estate at index $i: ${e.message}"
                                                        )
                                                        continue
                                                    }
                                                }

                                                Log.d(
                                                    "EstateParser",
                                                    "Successfully parsed ${estateList.size} estates and ${afdelingList.size} afdelings"
                                                )
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "EstateParser",
                                                    "Error parsing JSON: ${e.message}"
                                                )
                                            }
                                        }

                                        // Parse the estate data from JSON
                                        parseEstateJsonToList(responseBodyString)

                                        // Check if we have any data to store
                                        if (estateList.isEmpty()) {
                                            Log.w("DownloadResponse", "No estate data to store")
                                            results[request.dataset] =
                                                Resource.Error("No estate data found in response")
                                            _downloadStatuses.postValue(results.toMap())
                                        }

                                        Log.d(
                                            "DownloadResponse",
                                            "Storing ${estateList.size} estates and ${afdelingList.size} afdelings"
                                        )

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
                                        Log.e(
                                            "DownloadResponse",
                                            "Error processing estate data: ${e.message}"
                                        )
                                        if (!hasShownError) {
                                            results[request.dataset] =
                                                Resource.Error("Error processing estate data: ${e.message}")
                                            hasShownError = true
                                        }
                                        _downloadStatuses.postValue(results.toMap())
                                    }
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
                    AppLogger.d("msuk geesssssdj lsjd")
                    results[request.dataset] = Resource.Error(
                        "Network error: ${e.message ?: "Unknown error"}",
                        null
                    )
                    hasShownError = true
                    _downloadStatuses.postValue(results.toMap())
                }
                _downloadStatuses.postValue(results.toMap())
            }
        }
    }

    private suspend fun insertAllDetailRecordsFollowUp(
        inspectionDetails: JSONArray,
        localInspectionId: Int,
        tglInspeksi: String
    ) {
        AppLogger.d(">>> Starting insertAllDetailRecordsFollowUp for inspection ID=$localInspectionId")
        AppLogger.d("Processing ${inspectionDetails.length()} details for inspection ID=$localInspectionId")

        var detailSuccessCount = 0
        var detailFailCount = 0

        for (j in 0 until inspectionDetails.length()) {
            val detail = inspectionDetails.getJSONObject(j)

            try {
                val noPokPok = detail.optInt("no_pokok", 0)
                val pokokPanen = detail.optInt("pokok_panen", 0)
                val kodeInspeksi = detail.optInt("kode_inspeksi", 0)
                val temuanInspeksi = detail.optDouble("temuan_inspeksi", 0.0)
                val statusPemulihan = detail.optInt("status_pemulihan", 0)
                val nik = detail.optString("nik", "")
                val nama = detail.optString("nama", "")
                val catatan = detail.optString("catatan", null)
                val createdDate = detail.optString("created_date", tglInspeksi)
                val createdName = detail.optString("created_name", "")
                val createdBy = detail.optString("created_by", "")
                val latDetail = detail.optString("lat", "0.0").toDoubleOrNull() ?: 0.0
                val lonDetail = detail.optString("lon", "0.0").toDoubleOrNull() ?: 0.0

//                AppLogger.d("Detail ${j + 1}/${inspectionDetails.length()}: NIK=$nik, Code=$kodeInspeksi, Pokok=$noPokPok")

                val inspectionDetailEntity = InspectionDetailModel(
                    id = 0,
                    id_inspeksi = localInspectionId.toString(),
                    created_date = createdDate,
                    created_name = createdName,
                    created_by = createdBy,
                    nik = nik,
                    nama = nama,
                    no_pokok = noPokPok,
                    pokok_panen = if (pokokPanen == 0) null else pokokPanen,
                    kode_inspeksi = kodeInspeksi,
                    temuan_inspeksi = temuanInspeksi,
                    status_pemulihan = statusPemulihan,
                    foto = null,
                    foto_pemulihan = null,
                    komentar = catatan,
                    latIssue = latDetail,
                    lonIssue = lonDetail,
                    status_upload = "0",
                    status_uploaded_image = "0",
                    isPushedToServer = 1
                )

                val result = inspectionDetailDao.insertWithTransaction(inspectionDetailEntity)
                if (result.isSuccess) {
                    detailSuccessCount++
//                    AppLogger.d("✅ Inserted detail ${j + 1}: NIK=$nik, Code=$kodeInspeksi")
                } else {
                    detailFailCount++
//                    AppLogger.e("❌ Failed to insert detail ${j + 1}: NIK=$nik, Code=$kodeInspeksi")
                }
            } catch (e: Exception) {
                detailFailCount++
//                AppLogger.e("❌ Error processing detail ${j + 1}: ${e.message}")
            }
        }

        AppLogger.d("<<< Finished insertAllDetailRecordsFollowUp for inspection ID=$localInspectionId")
        AppLogger.d("Detail results: Success=$detailSuccessCount, Failed=$detailFailCount")
    }

    fun extractKp(jjgJson: String?): Int? {
        if (jjgJson.isNullOrEmpty() || jjgJson == "NULL") return null
        return try {
            JSONObject(jjgJson).optInt("KP", -1).takeIf { it >= 0 }
        } catch (e: Exception) {
            null
        }
    }


    private fun parseParameter(jsonContent: String): List<ParameterModel> {
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
        val dataArray = jsonObject.getAsJsonArray("data")
        return gson.fromJson(
            dataArray,
            TypeToken.getParameterized(List::class.java, ParameterModel::class.java).type
        )
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


    private fun extractUsernameFromCreatedName(createdName: String): String {
        // Return empty if input is null, empty, or "NULL"
        if (createdName.isEmpty() || createdName.equals("NULL", ignoreCase = true)) {
            return ""
        }

        try {
            // Remove extra spaces and split by space
            val parts = createdName.trim().split("\\s+".toRegex())

            if (parts.isEmpty()) return ""

            // Look for the pattern: 2-3 letters followed by a number
            // Start from the end and work backwards
            var result = ""
            var foundNumber = false
            var letters = ""

            // Go through parts from right to left
            for (i in parts.indices.reversed()) {
                val part = parts[i]

                // Check if this part is just a number
                if (part.matches("\\d+".toRegex()) && !foundNumber) {
                    result = part + result
                    foundNumber = true
                    continue
                }

                // Check if this part contains letters (and possibly numbers)
                if (part.matches(".*[A-Za-z].*".toRegex())) {
                    // Extract only the letters from this part
                    val lettersInPart = part.replace("[^A-Za-z]".toRegex(), "")

                    if (lettersInPart.length >= 2) {
                        // Take the last 2 characters if more than 2, or all if exactly 2
                        letters = if (lettersInPart.length > 2) {
                            lettersInPart.takeLast(2)
                        } else {
                            lettersInPart
                        }
                        result = letters + result
                        break
                    }
                }
            }

            // If we couldn't find the pattern, try a simpler approach
            if (result.length < 3) {
                // Look for any combination that has letters followed by numbers
                val combined = parts.joinToString("")
                val regex = "([A-Za-z]{2,3})(\\d+)".toRegex()
                val match = regex.findAll(combined).lastOrNull()

                if (match != null) {
                    val matchedLetters = match.groupValues[1]
                    val matchedNumber = match.groupValues[2]
                    result = matchedLetters.takeLast(2) + matchedNumber.takeLast(1)
                }
            }

            // Ensure result is exactly 3 characters and uppercase
            return if (result.length >= 3) {
                result.takeLast(3).uppercase()
            } else {
                // If still can't extract proper username, return empty instead of incomplete result
                ""
            }

        } catch (e: Exception) {
            AppLogger.e("Error extracting username from '$createdName': ${e.message}")
            return ""
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