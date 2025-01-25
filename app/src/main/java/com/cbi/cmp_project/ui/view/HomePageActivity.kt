package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.ActivityHomePageBinding
import com.cbi.cmp_project.ui.adapter.ProgressUploadAdapter
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.DataCacheManager
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null

    private lateinit var dataCacheManager: DataCacheManager
    data class ErrorResponse(
        val statusCode: Int,
        val message: String,
        val error: String? = null
    )
    private val permissionRequestCode = 1001
    companion object {
        private const val CHUNK_SIZE = 8192 // 8KB chunks
        private const val DEFAULT_BUFFER_SIZE = 8192 * 4 // Increased buffer size for better performance
    }

    private var filesToUpdate = mutableListOf<String>()
    private var regionalList: List<RegionalModel> = emptyList()
    private var wilayahList: List<WilayahModel> = emptyList()
    private var deptList: List<DeptModel> = emptyList()
    private var divisiList: List<DivisiModel> = emptyList()
    private var blokList: List<BlokModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranDetailList: List<KemandoranDetailModel> = emptyList()
    private var tphList: List<TPHNewModel>? = null

    private lateinit var datasetViewModel: DatasetViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        dataCacheManager = DataCacheManager(this)
        checkPermissions()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_page)

        navView.setupWithNavController(navController)
    }

    private fun initViewModel() {
        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]
    }


    private fun startFileDownload() {
//
//        if (!isInternetAvailable()) {
//            AlertDialogUtility.withSingleAction(
//                this,
//                stringXML(R.string.al_back),
//                stringXML(R.string.al_no_internet_connection),
//                stringXML(R.string.al_no_internet_connection_description_download_dataset),
//                "network_error.json",
//                R.color.colorRedDark
//            ) {}
//            return
//        }

        lifecycleScope.launch {
            // Inflate dialog layout
            val dialogView = layoutInflater.inflate(R.layout.list_card_upload, null)
            val alertDialog = AlertDialog.Builder(this@HomePageActivity)
                .setCancelable(false)
                .setView(dialogView)
                .create()

            alertDialog.show()
            alertDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
            recyclerView?.layoutManager = LinearLayoutManager(this@HomePageActivity, LinearLayoutManager.VERTICAL, false)

            // Get saved file list and determine which files need downloading
            val savedFileList = prefManager!!.getFileList().let { list ->
                if (list.isEmpty() || list.size != AppUtils.ApiCallManager.apiCallList.size) {
                    // If empty or wrong size, create new list with correct size
                    MutableList<String?>(AppUtils.ApiCallManager.apiCallList.size) { index ->
                        // Copy existing values if any, null otherwise
                        list.getOrNull(index)
                    }
                } else {
                    list.toMutableList()
                }
            }

            val filesToDownload = AppUtils.ApiCallManager.apiCallList.filterIndexed { index, pair ->
                val fileName = pair.first
                val file = File(this@HomePageActivity.getExternalFilesDir(null), fileName)
                val needsDownload = savedFileList.getOrNull(index) == null || !file.exists() ||  filesToUpdate.contains(fileName)
                Log.d("FileDownload", "File: $fileName, Needs download: $needsDownload")
                needsDownload
            }

            val apiCallsSize = filesToDownload.size
            if (apiCallsSize == 0) {
                Log.d("FileDownload", "No files need downloading")
                alertDialog.dismiss()
                return@launch
            }

            val progressList = MutableList(apiCallsSize) { 0 }
            val statusList = MutableList(apiCallsSize) { "Menunggu" }
            val iconList = MutableList(apiCallsSize) { R.id.progress_circular_loading }
            val fileNames = filesToDownload.map { it.first }

            val progressAdapter = ProgressUploadAdapter(progressList, statusList, iconList, fileNames.toMutableList())
            recyclerView?.adapter = progressAdapter

            val titleTextView = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
            val counterTextView = dialogView.findViewById<TextView>(R.id.counter_dataset)
            counterTextView.text = "0 / $apiCallsSize"

            lifecycleScope.launch(Dispatchers.Main) {
                var dots = 0
                while (alertDialog.isShowing) {
                    titleTextView.text = "Mengunduh Dataset" + ".".repeat(dots)
                    dots = if (dots >= 4) 1 else dots + 1
                    delay(500)
                }
            }

            for (i in 0 until apiCallsSize) {
                withContext(Dispatchers.Main) {
                    progressAdapter.updateProgress(i, 0, "Menunggu", R.id.progress_circular_loading)
                }
            }

            var completedCount = 0
            val downloadsDir = this@HomePageActivity.getExternalFilesDir(null)

            for ((index, apiCall) in filesToDownload.withIndex()) {
                val fileName = apiCall.first

                val apiCallFunction = apiCall.second
                val originalIndex = AppUtils.ApiCallManager.apiCallList.indexOfFirst { it.first == fileName }

                withContext(Dispatchers.Main) {
                    progressAdapter.resetProgress(index)
                    progressAdapter.updateProgress(index, 0, "Sedang Mengunduh", R.id.progress_circular_loading)
                }

                for (progress in 0..100 step 10) {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, progress, "Sedang Mengunduh", R.id.progress_circular_loading)
                    }
                }

                val (isSuccessful, message) = downloadFile(fileName, apiCallFunction, downloadsDir, savedFileList)

                if (isSuccessful) {
                    completedCount++
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_check_24)
                    }
                    savedFileList[originalIndex] = fileName
                } else {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_close_24)
                    }
                    savedFileList[originalIndex] = null
                }

                withContext(Dispatchers.Main) {
                    counterTextView.text = "$completedCount / $apiCallsSize"
                }
            }
            val cleanedList = savedFileList.toMutableList()
            for (i in cleanedList.indices.reversed()) {
                val fileName = cleanedList[i]
                if (fileName != null) {
                    // Check if this fileName appears earlier in the list
                    val firstIndex = cleanedList.indexOf(fileName)
                    if (firstIndex != i) {
                        // If found earlier, remove this duplicate
                        cleanedList.removeAt(i)
                    }
                }
            }

            prefManager!!.saveFileList(cleanedList)
            val closeText = dialogView.findViewById<TextView>(R.id.close_progress_statement)
            closeText.visibility = View.VISIBLE

            for (i in 3 downTo 1) {
                withContext(Dispatchers.Main) {
                    closeText.text = "Dialog tertutup otomatis dalam ${i} detik"
                    delay(1000)
                }
            }

            loadAllFilesAsync()
            alertDialog.dismiss()
        }
    }

    private fun loadAllFilesAsync() {
        val filesToDownload = AppUtils.ApiCallManager.apiCallList.map { it.first }
        Log.d("LoadFiles", "Starting to load files: ${filesToDownload.joinToString()}")

        loadingDialog.show()
        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_dataset)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500)
            }
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    filesToDownload.forEachIndexed { index, fileName ->
                        Log.d("LoadFiles", "Processing file $fileName (${index + 1}/${filesToDownload.size})")
                        val file = File(application.getExternalFilesDir(null), fileName)
                        if (file.exists()) {
                            Log.d("LoadFiles", "File exists: ${file.length()} bytes")
                            decompressFile(file, index == filesToDownload.lastIndex)
                        } else {
                            Log.e("LoadFiles", "File not found: $fileName")
                        }
                    }
                }

                Log.d("LoadFiles", """
                Data loaded:
                - Regionals: ${regionalList.size}
                - Wilayah: ${wilayahList.size}
                - Dept: ${deptList.size}
                - Divisi: ${divisiList.size}
                - Blok: ${blokList.size}
                - TPH: ${tphList?.size ?: 0}
            """.trimIndent())

                dataCacheManager.saveDatasets(
                    regionalList,
                    wilayahList,
                    deptList,
                    divisiList,
                    blokList,
                    tphList!!,
                    karyawanList,
                    kemandoranList,
                    kemandoranDetailList
                )
            } catch (e: Exception) {
                Log.e("LoadFiles", "Error loading files", e)
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    progressJob.cancel()

                }
            }
        }
    }

    private fun decompressFile(file: File, isLastFile: Boolean) {
        try {
            Log.d("Decompress", "Starting decompression of ${file.name}")

            when (file.name) {
                "datasetTPH.zip" -> {
                    Log.d("Decompress", "Processing large TPH file")
                    handleLargeFileChunked(file, isLastFile)
                }
                else -> {
                    GZIPInputStream(file.inputStream()).use { gzipInputStream ->
                        val decompressedData = gzipInputStream.readBytes()
                        Log.d("Decompress", "Decompressed ${file.name}: ${decompressedData.size} bytes")
                        val jsonString = String(decompressedData, Charsets.UTF_8)
                        parseJsonData(jsonString, isLastFile)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Decompress", "Error processing ${file.name}", e)
            throw e
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun parseJsonData(jsonString: String, isLastFile: Boolean) {
        try {
            val jsonObject = JSONObject(jsonString)
            val gson = Gson()

            val keyObject = jsonObject.getJSONObject("key")
            val dateModified = jsonObject.getString("date_modified")

            // Parse CompanyCodeDB
            if (jsonObject.has("RegionalDB")) {
                val companyCodeArray = jsonObject.getJSONArray("RegionalDB")

                val dateModified = jsonObject.getString("date_modified")
                val transformedCompanyCodeArray = transformJsonArray(companyCodeArray, keyObject)
                val regionalList: List<RegionalModel> = gson.fromJson(
                    transformedCompanyCodeArray.toString(),
                    object : TypeToken<List<RegionalModel>>() {}.type
                )

                prefManager?.setDateModified("RegionalDB", dateModified)
                datasetViewModel.updateOrInsertRegional(regionalList)
                this.regionalList = regionalList
            } else {
                Log.e("ParseJsonData", "RegionalDB key is missing")
            }


            if (jsonObject.has("WilayahDB")) {
                val companyCodeArray = jsonObject.getJSONArray("WilayahDB")
                val transformedCompanyCodeArray = transformJsonArray(companyCodeArray, keyObject)
                val wilayahList: List<WilayahModel> = gson.fromJson(
                    transformedCompanyCodeArray.toString(),
                    object : TypeToken<List<WilayahModel>>() {}.type
                )
                Log.d("ParsedData", "WilayahDB: $wilayahList")
                this.wilayahList = wilayahList
//                datasetViewModel.updateOrInsertWilayah(wilayahList)
                prefManager?.setDateModified("WilayahDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "WilayahDB key is missing")
            }

            if (jsonObject.has("DeptDB")) {
                val bUnitCodeArray = jsonObject.getJSONArray("DeptDB")
                val transformedBUnitCodeArray = transformJsonArray(bUnitCodeArray, keyObject)
                val deptList: List<DeptModel> = gson.fromJson(
                    transformedBUnitCodeArray.toString(),
                    object : TypeToken<List<DeptModel>>() {}.type
                )
                Log.d("ParsedData", "BUnitCode: $deptList")
                this.deptList = deptList
//                datasetViewModel.updateOrInsertDept(deptList)
                prefManager?.setDateModified("DeptDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "DeptDB key is missing")
            }

            // Parse DivisionCodeDB
            if (jsonObject.has("DivisiDB")) {
                val divisionCodeArray = jsonObject.getJSONArray("DivisiDB")
                val transformedDivisionCodeArray = transformJsonArray(divisionCodeArray, keyObject)
                val divisiList: List<DivisiModel> = gson.fromJson(
                    transformedDivisionCodeArray.toString(),
                    object : TypeToken<List<DivisiModel>>() {}.type
                )
                Log.d("ParsedData", "DivisionCode: $divisiList")
                this.divisiList = divisiList
//                datasetViewModel.updateOrInsertDivisi(divisiList)
                prefManager?.setDateModified("DivisiDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "DivisiDB key is missing")
            }

            // Parse FieldCodeDB
            if (jsonObject.has("BlokDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("BlokDB")
                val transformedFieldCodeArray = transformJsonArray(fieldCodeArray, keyObject)
                val blokList: List<BlokModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<BlokModel>>() {}.type
                )
                Log.d("ParsedData", "FieldCode: $blokList")
                this.blokList = blokList
//                datasetViewModel.updateOrInsertBlok(blokList)
                prefManager?.setDateModified("BlokDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "BlokDB key is missing")
            }

            // Parse KaryawanDB
            if (jsonObject.has("KaryawanDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("KaryawanDB")
                val transformedFieldCodeArray = transformJsonArrayInChunks(fieldCodeArray, keyObject)
                val karyawanList: List<KaryawanModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<KaryawanModel>>() {}.type
                )
                Log.d("ParsedData", "KaryawanDB: $karyawanList")
                this.karyawanList = karyawanList
//                datasetViewModel.updateOrInsertKaryawan(karyawanList)
                prefManager?.setDateModified("KaryawanDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "KaryawanDB key is missing")
            }

            // Parse KemandoranDB
            if (jsonObject.has("KemandoranDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("KemandoranDB")
                val transformedFieldCodeArray = transformJsonArrayInChunks(fieldCodeArray, keyObject)
                val kemandoranList: List<KemandoranModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<KemandoranModel>>() {}.type
                )
                Log.d("ParsedData", "KemandoranDB: $kemandoranList")
                this.kemandoranList = kemandoranList
//                datasetViewModel.updateOrInsertKemandoran(kemandoranList)
                prefManager?.setDateModified("KemandoranDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "KemandoranDB key is missing")
            }

            // Parse KemandoranDetailDB
            if (jsonObject.has("KemandoranDetailDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("KemandoranDetailDB")
                val transformedFieldCodeArray = transformJsonArray(fieldCodeArray, keyObject)
                val kemandoranDetailList: List<KemandoranDetailModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<KemandoranDetailModel>>() {}.type
                )
                Log.d("ParsedData", "KemandoranDetailDB: $kemandoranDetailList")
                this.kemandoranDetailList = kemandoranDetailList
//                datasetViewModel.updateOrInsertKemandoranDetail(kemandoranDetailList)
                prefManager?.setDateModified("KemandoranDetailDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "KemandoranDetailDB key is missing")
            }

            this.regionalList = regionalList
            this.wilayahList = wilayahList
            this.deptList = deptList
            this.divisiList = divisiList
            this.blokList = blokList
            this.karyawanList = karyawanList
            this.kemandoranList = kemandoranList
            this.kemandoranDetailList = kemandoranDetailList

        } catch (e: JSONException) {
            Log.e("ParseJsonData", "Error parsing JSON: ${e.message}")
        }
    }

    fun transformJsonArray(jsonArray: JSONArray, keyObject: JSONObject): JSONArray {
        val transformedArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val transformedItem = JSONObject()

            keyObject.keys().forEach { key ->
                val fieldName = keyObject.getString(key)  // This gets the field name from the key object
                val fieldValue = item.get(key)  // This gets the corresponding value from the item
                transformedItem.put(fieldName, fieldValue)
            }

            transformedArray.put(transformedItem)
        }

        return transformedArray
    }

    fun transformJsonArrayInChunks(jsonArray: JSONArray, keyObject: JSONObject): JSONArray {
        val transformedArray = JSONArray()
        val chunkSize = 100 // Adjust this based on your needs

        try {
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val transformedItem = JSONObject()

                keyObject.keys().forEach { key ->
                    val fieldName = keyObject.getString(key)
                    val fieldValue = item.get(key)
                    transformedItem.put(fieldName, fieldValue)
                }

                transformedArray.put(transformedItem)

                // After each chunk is processed, suggest garbage collection
                if (i % chunkSize == 0) {
                    System.gc()
                }
            }
        } catch (e: Exception) {
            Log.e("Transform", "Error transforming array: ${e.message}")
        }

        return transformedArray
    }

    private fun loadTPHData(jsonObject: JSONObject) {
        try {
            if (jsonObject.has("TPHDB")) {
                Log.d("testing", "masuk sini ges")
                val tphArray = jsonObject.getJSONArray("TPHDB")
                val keyObject = jsonObject.getJSONObject("key")
                val chunkSize = 50 // Adjust the chunk size as needed

                // Create a mutable list to accumulate all TPHNewModel objects
                val accumulatedTPHList = mutableListOf<TPHNewModel>()

                // Process the TPHDB array in chunks
                for (i in 0 until tphArray.length() step chunkSize) {
                    val chunk = JSONArray()
                    for (j in i until (i + chunkSize).coerceAtMost(tphArray.length())) {
                        chunk.put(tphArray.getJSONObject(j))
                    }

                    // Transform the current chunk
                    val transformedChunk = transformJsonArray(chunk, keyObject)

                    // Parse the transformed chunk into a list of TPHNewModel
                    val chunkList: List<TPHNewModel> = Gson().fromJson(
                        transformedChunk.toString(),
                        object : TypeToken<List<TPHNewModel>>() {}.type
                    )

                    accumulatedTPHList.addAll(chunkList)

                    Log.d("LoadTPHData", "Processed chunk: $i to ${(i + chunkSize - 1).coerceAtMost(tphArray.length() - 1)}")
                }

                this.tphList = accumulatedTPHList
                val dateModified = jsonObject.getString("date_modified")

                prefManager?.setDateModified("TPHDB", dateModified) // Store dynamically
                Log.d("ParsedData", "Total TPHDB items: ${tphList?.size ?: 0}")
            } else {
                Log.e("LoadTPHData", "TPHDB key is missing")
            }
        } catch (e: JSONException) {
            Log.e("LoadTPHData", "Error processing TPH data: ${e.message}")
        }
    }


    private fun handleLargeFileChunked(file: File, isLastFile: Boolean) {
        try {
            Log.d("HandleLargeFile", "Starting chunked processing of: ${file.name}")
            val startTime = System.currentTimeMillis()

            // Create a temporary file to store decompressed data
            val tempFile = File(file.parent, "temp_decompressed.json")

            // Step 1: Decompress the file
            GZIPInputStream(file.inputStream().buffered(DEFAULT_BUFFER_SIZE)).use { gzipInputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var totalBytes = 0L
                    var bytesRead: Int

                    while (gzipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        if (totalBytes % (10 * 1024 * 1024) == 0L) {
                            Log.d("HandleLargeFile", "Decompressed: ${totalBytes / (1024 * 1024)} MB")
                        }
                    }
                    Log.d("HandleLargeFile", "Total decompressed size: ${totalBytes / (1024 * 1024)} MB")
                }
            }

            // Step 2: Read and parse in a controlled way
            try {
                tempFile.inputStream().bufferedReader().use { reader ->
                    val jsonContent = reader.readText()
                    Log.d("HandleLargeFile", "JSON loaded into memory, size: ${jsonContent.length} chars")

                    val jsonObject = JSONObject(jsonContent)
                    Log.d("HandleLargeFile", "JSON successfully parsed")

                    // Check if this is TPH data
                    if (jsonObject.has("TPHDB")) {
                        Log.d("HandleLargeFile", "Found TPHDB")
                        // Process TPH data directly
                        loadTPHData(jsonObject)
                    } else {
                        // Process other data
                        Log.d("HandleLargeFile", "Processing regular data")
                        parseJsonData(jsonContent, isLastFile)
                    }
                }
            } catch (e: OutOfMemoryError) {
                Log.e("HandleLargeFile", "OutOfMemoryError: ${e.message}")
                System.gc() // Request garbage collection
                e.printStackTrace()
            } catch (e: JSONException) {
                Log.e("HandleLargeFile", "JSON parsing error: ${e.message}")
                e.printStackTrace()
            }

            // Clean up
            tempFile.delete()

            val endTime = System.currentTimeMillis()
            Log.d("HandleLargeFile", "Total processing time: ${endTime - startTime} ms")

        } catch (e: Exception) {
            Log.e("HandleLargeFile", "Error in processing: ${e.message}")
            e.printStackTrace()
        }
    }
    private suspend fun downloadFile(
        fileName: String,
        apiCall: suspend () -> Response<ResponseBody>,
        downloadsDir: File?,
        fileList: MutableList<String?>
    ): Pair<Boolean, String> {  // Changed return type to include message
        return try {
            withContext(Dispatchers.IO) {
                val response = apiCall()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val file = File(downloadsDir, fileName)
                        saveFileToStorage(responseBody, file)
                        fileList.add(fileName)
                        Log.d("FileDownload", "$fileName downloaded successfully.")
                        Pair(true, "Unduh Selesai")
                    } else {
                        fileList.add(null)
                        Log.e("FileDownload", "Response body is null.")
                        Pair(false, "Response body kosong")
                    }
                } else {
                    fileList.add(null)
                    try {
                        val errorBody = response.errorBody()?.string()
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        Log.e("FileDownload", "Error message: ${errorResponse.message}")
                        Pair(false, "Unduh Gagal! ${errorResponse.message}")  // Added prefix here
                    } catch (e: Exception) {
                        Pair(false, "Unduh Gagal! Kode: ${response.code()}")  // Added prefix here
                    }
                }
            }
        } catch (e: Exception) {
            fileList.add(null)
            Log.e("FileDownload", "Error downloading file: ${e.message}")
            Pair(false, "Unduh Gagal! ${e.message}")  // Added prefix here
        }
    }



    private fun saveFileToStorage(body: ResponseBody, file: File): Boolean {
        return try {
            body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FileDownload", "Error saving file: ${e.message}")
            false
        }
    }



    /**
     * Checks for internet availability.
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Check internet capability and perform ping
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && pingGoogle()
    }

    /**
     * Pings Google to verify internet connectivity.
     */
    private fun pingGoogle(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 www.google.com")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e("PingGoogle", "Ping failed: ${e.message}")
            false
        }
    }

    private suspend fun shouldStartFileDownload(): Boolean {
        val savedFileList = prefManager!!.getFileList()
        val downloadsDir = this.getExternalFilesDir(null)

        if (prefManager!!.isFirstTimeLaunch) {
            Log.d("FileCheck", "First time launch detected.")
            prefManager!!.isFirstTimeLaunch = false
            return true
        }

        if (savedFileList.isNotEmpty()) {
            if (savedFileList.contains(null)) {
                Log.e("FileCheck", "Null entries found in savedFileList.")
                return true
            }

            val missingFiles = savedFileList.filterNot { fileName ->
                val file = File(downloadsDir, fileName)
                val exists = file.exists()
                Log.d("FileCheck", "Checking file: ${file.path} -> Exists: $exists")
                fileName != null && exists
            }

            if (missingFiles.isNotEmpty()) {
                Log.e("FileCheck", "Missing files detected: $missingFiles")
                return true
            }
        } else {
            Log.d("FileCheck", "Saved file list is empty.")
            return true
        }


//        if (!isInternetAvailable()) {
//            Log.d("NetworkCheck", "No internet connection available")
//            filesToUpdate.clear()  // Clear any pending updates
//            return false
//        }

        val shouldDownload = checkServerDates()
        Log.d("testing", filesToUpdate.toString())
        Log.d("testing", shouldDownload.toString())

        return shouldDownload
    }


    private suspend fun checkServerDates(): Boolean {
        try {
            filesToUpdate.clear()

            val response = RetrofitClient.instance.getTablesLatestModified()
            if (response.isSuccessful && response.body()?.statusCode == 1) {
                val serverData = response.body()?.data ?: return true
                val localData = prefManager?.getAllDateModified() ?: return true

                val keyMapping = mapOf(
                    AppUtils.ApiCallManager.apiCallList[0].first to Pair("regional", "RegionalDB"),
                    AppUtils.ApiCallManager.apiCallList[1].first to Pair("wilayah", "WilayahDB"),
                    AppUtils.ApiCallManager.apiCallList[2].first to Pair("dept", "DeptDB"),
                    AppUtils.ApiCallManager.apiCallList[3].first to Pair("divisi", "DivisiDB"),
                    AppUtils.ApiCallManager.apiCallList[4].first to Pair("blok", "BlokDB"),
                    AppUtils.ApiCallManager.apiCallList[5].first to Pair("tph", "TPHDB")
                )

                keyMapping.forEach { (filename, keys) ->
                    val (serverKey, localKey) = keys
                    val serverDate = serverData[serverKey]
                    val localDate = localData[localKey]

                    Log.d("DateComparison", """
                    Comparing dates for $localKey:
                    Server date ($serverKey): $serverDate
                    Local date ($localKey): $localDate
                """.trimIndent())

                    if (serverDate != null && localDate != null) {
                        val serverDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(serverDate)
                        val localDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(localDate)

                        if (serverDateTime != null && localDateTime != null) {
                            if (serverDateTime.after(localDateTime)) {
                                Log.d("DateComparison", "$localKey needs update: Server date is newer")
                                filesToUpdate.add(filename)
                            }
                        }
                    }
                }

                return filesToUpdate.isNotEmpty()
            }
            return true
        } catch (e: Exception) {
            Log.e("FileCheck", "Error checking server dates", e)
            return true
        }
    }


    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }else{
            lifecycleScope.launch {
                val shouldDownload = shouldStartFileDownload()
                if (shouldDownload) {
                    Log.d("FileCheck", "Starting file download...")
                    startFileDownload()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            val deniedPermissions = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "The following permissions are required: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }else {
                lifecycleScope.launch {
                    if (shouldStartFileDownload()) {
                        Log.d("FileCheck", "Starting file download...")
                        startFileDownload()
                    } else {
                        Log.d("FileCheck", "File download not required.")
                    }
                }
            }
        }
    }
}