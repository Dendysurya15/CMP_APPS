package com.cbi.mobile_plantation.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.ui.adapter.UploadItem
import com.cbi.mobile_plantation.ui.adapter.UploadProgressAdapter
import com.cbi.mobile_plantation.ui.adapter.WBData
import com.cbi.mobile_plantation.ui.adapter.WeighBridgeAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.UploadCMPViewModel
import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("UNREACHABLE_CODE")
class ListHistoryWeighBridgeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private lateinit var adapter: WeighBridgeAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private var infoApp: String = ""
    private lateinit var datasetViewModel: DatasetViewModel
    private var globalFormattedDate: String = ""
    private var uploadCMPData: List<Pair<String, String>> = emptyList()
    private var globalESPBIds: List<Int> = emptyList()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var uploadCMPViewModel: UploadCMPViewModel
    private lateinit var speedDial: SpeedDialView
    private lateinit var allUploadZipFilesToday: MutableList<File>
    private var globalIdESPBKraniTimbang: List<Int> = emptyList()
    private var zipFilePath: String? = null
    private var zipFileName: String? = null
    private lateinit var tvEmptyState: TextView // Add this
    private lateinit var headerCheckBoxWB: CheckBox // Add this
    private lateinit var dateButton: Button

    private var trackingIdsUpload: List<String> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        infoApp = AppUtils.getDeviceInfo(this@ListHistoryWeighBridgeActivity).toString()
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupSpeedDial()
        setupObserveData()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener {
            onBackPressed()
        }
        globalFormattedDate = AppUtils.currentDate
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        weightBridgeViewModel.loadHistoryESPB(todayDate)

        findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()

        val filterAllData: CheckBox = findViewById(R.id.calendarCheckbox)

        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
            val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
            if (isChecked) {
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"

                dateButton.isEnabled = false
                dateButton.alpha = 0.5f

                weightBridgeViewModel.loadHistoryESPB()
            } else {
                // For line 136 (use date from date picker)
                val displayDate = formatGlobalDate(globalFormattedDate)
                weightBridgeViewModel.loadHistoryESPB(globalFormattedDate)
                nameFilterDate.text = displayDate
                dateButton.isEnabled = true
                dateButton.alpha = 1f // Make the button appear darker
                Log.d("FilterAllData", "Checkbox is UNCHECKED. Button enabled.")
            }

            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

            removeFilterDate.setOnClickListener {
                if (filterAllData.isChecked) {
                    filterAllData.isChecked = false
                }

                filterDateContainer.visibility = View.GONE

                val todayBackendDate = AppUtils.formatDateForBackend(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR)
                )
                // Reset the selected date in your utils
                AppUtils.setSelectedDate(todayBackendDate)

                // Update the dateButton to show today's date
                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate

            }
        }
    }

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyStateKraniTimbang)
        speedDial = findViewById(R.id.dial_tph_list_krani_timbang_espb)
    }

    private fun handleUpload(selectedItems: List<Map<String, Any>>) {
        var number =
            0

        val uploadItems = selectedItems.map { item ->
            UploadItem(
                id = item["id"] as Int,
                ip = item["ip"].toString(),
                num = number++,
                deptPpro = (item["dept_ppro"] as Number).toInt(),
                divisiPpro = (item["divisi_ppro"] as Number).toInt(),
                commodity = (item["commodity"] as Number).toInt(),
                blokJjg = item["blok_jjg"] as String,
                nopol = item["nopol"] as String,
                driver = item["driver"] as String,
                pemuatId = item["pemuat_id"].toString(),
                transporterId = (item["transporter_id"] as Number).toInt(),
                millId = (item["mill_id"] as Number).toInt(),
                createdById = (item["created_by_id"] as Number).toInt(),
                createdAt = item["created_at"] as String,
                no_espb = item["no_espb"] as String,
                uploader_info = infoApp,
                uploaded_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ),
                uploaded_by_id = prefManager!!.idUserLogin!!.toInt(),
                file = "",
                endpoint = AppUtils.DatabaseServer.PPRO
            )
        }

//        val uploadItems = emptyList<UploadItem>()

        var nextId = (uploadItems.maxByOrNull { it.id }?.id ?: 0) + 1

        val allItems = mutableListOf<UploadItem>().apply { addAll(uploadItems) }

//        if (!zipFilePath.isNullOrEmpty() && !zipFileName.isNullOrEmpty()) {
//            allItems.add(
//                UploadItem(
//                    id = nextId++,
//                    ip = "",
//                    num = number++,
//                    deptPpro = 0,
//                    divisiPpro = 0,
//                    commodity = 0,
//                    blokJjg = "",
//                    nopol = "",
//                    driver = "",
//                    pemuatId = "",
//                    transporterId = 0,
//                    millId = 0,
//                    createdById = 0,
//                    createdAt = "",
//                    no_espb = zipFileName.toString(),
//                    uploader_info = "",
//                    uploaded_at = "",
//                    uploaded_by_id = 0,
//                    file = zipFilePath.toString(),
//                    endpoint = AppUtils.DatabaseServer.CMP
//                )
//            )
//        }

        if (allUploadZipFilesToday.isNotEmpty()) {
            allUploadZipFilesToday.forEach { file ->
                lifecycleScope.launch {
                    try {
                        val extractionDeferred = CompletableDeferred<Pair<List<Int>, List<Int>>>()
                        launch(Dispatchers.IO) {
                            try {
                                val result = AppUtils.extractIdsFromZipFile(
                                    context = this@ListHistoryWeighBridgeActivity,
                                    fileName = file.name,
                                    zipPassword = AppUtils.ZIP_PASSWORD
                                )
                                // Complete the deferred with the result
                                extractionDeferred.complete(result)
                            } catch (e: Exception) {
                                // Complete exceptionally if there's an error
                                extractionDeferred.completeExceptionally(e)
                            }
                        }
                        val (panenIds, espbIds) = withTimeout(5000) { // 10 second timeout
                            extractionDeferred.await()
                        }
                        globalESPBIds = espbIds
                    } catch (e: Exception) {
                        AppLogger.e("Error during ZIP extraction: ${e.message}")
                        globalESPBIds = emptyList()
                    }
                }

                allItems.add(
                    UploadItem(
                        id = nextId++,
                        ip = "",
                        num = number++,
                        deptPpro = 0,
                        divisiPpro = 0,
                        commodity = 0,
                        blokJjg = "",
                        nopol = "",
                        driver = "",
                        pemuatId = "",
                        transporterId = 0,
                        millId = 0,
                        createdById = 0,
                        createdAt = "",
                        no_espb = file.name,
                        uploader_info = "",
                        uploaded_at = "",
                        uploaded_by_id = 0,
                        file = file.absolutePath,
                        endpoint = AppUtils.DatabaseServer.CMP
                    )
                )
            }
        }


        // Now `allItems` contains both selected items and all ZIP-related items
        val allUploadItems = allItems


        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)

        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Progress Upload..."
        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
        counterTV.text = "0/${allUploadItems.size}"
        val cancelDownloadDataset =
            dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val containerDownloadDataset =
            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UploadProgressAdapter(allUploadItems, weightBridgeViewModel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()
        val closeDialogBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        closeDialogBtn.visibility = View.VISIBLE

        cancelDownloadDataset.setOnClickListener {
            speedDial.close()
            weightBridgeViewModel.loadHistoryESPB()
            zipFileName = null
            zipFilePath = null
            dialog.dismiss()
        }
        val btnUploadDataCMP = dialogView.findViewById<MaterialButton>(R.id.btnUploadDataCMP)

        containerDownloadDataset.visibility = View.VISIBLE
        closeDialogBtn.visibility = View.VISIBLE
        btnUploadDataCMP.visibility = View.VISIBLE
        btnUploadDataCMP.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Upload",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_upload),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {
                        btnUploadDataCMP.isEnabled = false
                        closeDialogBtn.isEnabled = false
                        btnUploadDataCMP.alpha = 0.7f
                        closeDialogBtn.alpha = 0.7f
                        btnUploadDataCMP.iconTint =
                            ColorStateList.valueOf(Color.parseColor("#80FFFFFF")) // 50% transparent white
                        closeDialogBtn.iconTint =
                            ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

                        weightBridgeViewModel.uploadESPBKraniTimbang(
                            allUploadItems.map { uploadItem ->
                                mapOf(
                                    "id" to uploadItem.id,
                                    "ip" to uploadItem.ip,
                                    "num" to uploadItem.num,
                                    "dept_ppro" to uploadItem.deptPpro,
                                    "divisi_ppro" to uploadItem.divisiPpro,
                                    "commodity" to uploadItem.commodity,
                                    "blok_jjg" to uploadItem.blokJjg,
                                    "nopol" to uploadItem.nopol,
                                    "driver" to uploadItem.driver,
                                    "pemuat_id" to uploadItem.pemuatId,
                                    "transporter_id" to uploadItem.transporterId,
                                    "mill_id" to uploadItem.millId,
                                    "created_by_id" to uploadItem.createdById,
                                    "created_at" to uploadItem.createdAt,
                                    "no_espb" to uploadItem.no_espb,
                                    "uploader_info" to uploadItem.uploader_info,
                                    "uploaded_at" to uploadItem.uploaded_at,
                                    "uploaded_by_id" to uploadItem.uploaded_by_id,
                                    "file" to uploadItem.file,
                                    "endpoint" to uploadItem.endpoint
                                )
                            },
                            globalESPBIds
                        )
                    },
                    cancelFunction = {
                    }
                )

            } else {
                AlertDialogUtility.withSingleAction(
                    this@ListHistoryWeighBridgeActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) {
                    // Do nothing
                }
            }
        }

        weightBridgeViewModel.uploadStatusMap.observe(this) { statusMap ->
            val completedCount = statusMap.count { it.value == "Success" || it.value == "Failed" }
            AppLogger.d(completedCount.toString())
            counterTV.text = "$completedCount/${allUploadItems.size}"
            if (completedCount == allUploadItems.size) {

                lifecycleScope.launch {
                    loadingDialog.show()
                    loadingDialog.setMessage("Sedang memproses data", true)

                    delay(500)


                    withContext(Dispatchers.Main) {
                        containerDownloadDataset.visibility = View.VISIBLE
                        cancelDownloadDataset.visibility = View.VISIBLE
                        btnUploadDataCMP.visibility = View.GONE
                        closeDialogBtn.isEnabled = true
                        closeDialogBtn.alpha = 1f
                        closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
                        loadingDialog.dismiss()
                    }

                }

            }
        }


//        weightBridgeViewModel.uploadResult.observe(this) { result ->
//            result.onSuccess {
//                Toast.makeText(this, "Upload Successful!", Toast.LENGTH_SHORT).show()
//            }.onFailure {
//                Toast.makeText(this, "Upload Failed: ${it.message}", Toast.LENGTH_LONG).show()
//            }
//        }


    }


    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark),
            function = {
                loadingDialog.show()
                loadingDialog.setMessage("Deleting items...")

                weightBridgeViewModel.deleteMultipleItems(selectedItems)

                weightBridgeViewModel.deleteItemsResult.observe(this) { isSuccess ->
                    loadingDialog.dismiss()
                    if (isSuccess) {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload data based on current state
                        weightBridgeViewModel.loadHistoryESPB()
                    } else {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_failed_delete)} data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    speedDial.visibility = View.GONE
                }

                weightBridgeViewModel.error.observe(this) { errorMessage ->
                    loadingDialog.dismiss()
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            },
            cancelFunction = {

            }
        )
    }

    private fun setupSpeedDial() {

        speedDial.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.uploadSelected, R.drawable.baseline_file_upload_24)
                    .setLabel(getString(R.string.dial_upload_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListHistoryWeighBridgeActivity,
                            R.color.bluedarklight
                        )
                    )
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.deleteSelected,
                    R.drawable.baseline_delete_forever_24
                )
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListHistoryWeighBridgeActivity,
                            R.color.colorRedDark
                        )
                    )
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.scan_qr -> {
//
                        true
                    }

                    R.id.deleteSelected -> {
                        val selectedItems = adapter.getSelectedItemsIdLocal()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = adapter.getSelectedItemsForUpload()
                        if (selectedItems.isEmpty()) {
                            // Show a message to the user that they need to select items first
                            AlertDialogUtility.withSingleAction(
                                this@ListHistoryWeighBridgeActivity,
                                getString(R.string.al_back),
                                "Tidak ada data dipilih",
                                "Mohon untuk melakukan centang/pilih untuk upload data",
                                "warning.json",
                                R.color.yellowbutton
                            ) {}
                            return@setOnActionSelectedListener true
                        }
                        val selectedIds = selectedItems.map { it["id"] as Int }

                        if (AppUtils.isNetworkAvailable(this@ListHistoryWeighBridgeActivity)) {

                            lifecycleScope.launch {
                                loadingDialog.show()
                                loadingDialog.setMessage("Sedang mengupdate data...")
                                uploadCMPData = emptyList()
                                uploadCMPViewModel.getAllIdsAndFilenames()
                                delay(500)

                                val dataDeferred = CompletableDeferred<List<Pair<String, String>>>()
                                uploadCMPViewModel.allIdsAndFilenames.observe(this@ListHistoryWeighBridgeActivity) { data ->
                                    dataDeferred.complete(data ?: emptyList()) // Ensure it's never null
                                }
                                val data = dataDeferred.await()

                                //kode khusus untuk update UploadCMP sebelum melakukan upload
                                uploadCMPData = data
                                if (uploadCMPData.isNotEmpty()) {
                                    AppLogger.d("Starting update for ${uploadCMPData.size} items")
                                    val updateSuccessful = datasetViewModel.updateLocalUploadCMP(uploadCMPData).await()
                                    AppLogger.d("Update status: $updateSuccessful, now proceeding to file check")
                                } else {
                                    AppLogger.d("No data to update")

                                }

                                allUploadZipFilesToday =
                                    AppUtils.checkAllUploadZipFiles(
                                        prefManager!!.idUserLogin.toString(),
                                        this@ListHistoryWeighBridgeActivity
                                    )
                                        .toMutableList()

                                if (allUploadZipFilesToday.isNotEmpty()) {
                                    uploadCMPViewModel.getUploadCMPTodayData()
                                    delay(100)
                                    val filteredFiles = withContext(Dispatchers.Main) {
                                        suspendCoroutine<List<File>> { continuation ->
                                            uploadCMPViewModel.fileData.observeOnce(this@ListHistoryWeighBridgeActivity) { fileList ->
                                                val filesToRemove =
                                                    fileList.filter { it.status == 2 || it.status == 3 }
                                                        .map { it.nama_file }
                                                // Filter files and update `allUploadZipFilesToday`
                                                allUploadZipFilesToday =
                                                    allUploadZipFilesToday.filter { file ->
                                                        !filesToRemove.contains(file.name)
                                                    }.toMutableList()

                                                continuation.resume(allUploadZipFilesToday) // Resume coroutine with valid files
                                            }
                                        }
                                    }

                                    if (filteredFiles.isNotEmpty()) {
                                        Log.d("VALID_FILES", "Filtered valid files: $filteredFiles")
                                    } else {
                                        Log.d("VALID_FILES", "No valid files found.")
                                    }
                                }

                                lifecycleScope.launch {
                                    val espbDeferred = CompletableDeferred<List<ESPBEntity>>()
                                    val zipDeferred = CompletableDeferred<Boolean>()

                                    weightBridgeViewModel.fetchActiveESPBByIds(selectedIds)
                                    delay(100)
                                    weightBridgeViewModel.activeESPBByIds.observeOnce(this@ListHistoryWeighBridgeActivity) { list ->
                                        Log.d(
                                            "UploadCheck",
                                            "📌 Filtered ESPB Data Received: ${list.size}"
                                        )
                                        espbDeferred.complete(list ?: emptyList())
                                    }

                                    var mappedESPBData: List<Map<String, Any>> = emptyList()

                                    try {
                                        val espbList = espbDeferred.await()


                                        AppLogger.d("as;dkf $espbList")
                                        if (espbList.isNotEmpty()) {
                                            mappedESPBData = espbList.map { data ->
                                                val blokJjgList =
                                                    data.blok_jjg.split(";").mapNotNull {
                                                        it.split(",").takeIf { it.size == 2 }
                                                            ?.let { (id, jjg) ->
                                                                id.toIntOrNull()
                                                                    ?.let { it to jjg.toIntOrNull() }
                                                            }
                                                    }
                                                val idBlokList = blokJjgList.map { it.first }
                                                val concatenatedIds = idBlokList.joinToString(",")
                                                val totalJjg =
                                                    blokJjgList.mapNotNull { it.second }.sum()


                                                AppLogger.d("gas $data")
                                                mapOf(
                                                    "id" to data.id,
                                                    "blok_id" to concatenatedIds,
                                                    "blok_jjg" to data.blok_jjg,
                                                    "jjg" to totalJjg,
                                                    "created_by_id" to data.created_by_id,
                                                    "created_at" to data.created_at,
                                                    "pemuat_id" to data.pemuat_id,
                                                    "kemandoran_id" to data.kemandoran_id,
                                                    "pemuat_nik" to data.pemuat_nik,
                                                    "nopol" to data.nopol,
                                                    "driver" to data.driver,
                                                    "transporter_id" to data.transporter_id,
                                                    "mill_id" to data.mill_id,
                                                    "creator_info" to data.creator_info,
                                                    "no_espb" to data.noESPB,
                                                    "tph0" to data.tph0,
                                                    "tph1" to data.tph1,
                                                    "update_info_sp" to data.update_info_sp,
                                                    "app_version" to AppUtils.getDeviceInfo(this@ListHistoryWeighBridgeActivity)
                                                        .toString(),
                                                    "jabatan" to prefManager!!.jabatanUserLogin.toString(),
                                                )
                                            }
                                            globalESPBIds = mappedESPBData.map { it["id"] as Int }
                                        }

                                    } catch (e: Exception) {
                                        Log.e("UploadCheck", "❌ Error: ${e.message}")
                                    } finally {
                                        val uploadDataList =
                                            mutableListOf<Pair<String, List<Map<String, Any>>>>()
                                        if (mappedESPBData.isNotEmpty()) uploadDataList.add(AppUtils.DatabaseTables.ESPB to mappedESPBData)
                                        if (uploadDataList.isNotEmpty()) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                AppUtils.createAndSaveZipUploadCMP(
                                                    this@ListHistoryWeighBridgeActivity,
                                                    uploadDataList,
                                                    prefManager!!.idUserLogin.toString()
                                                ) { success, fileName, fullPath ->
                                                    if (success) {
                                                        zipFilePath = fullPath
                                                        zipFileName = fileName

                                                        lifecycleScope.launch(Dispatchers.IO) {
                                                            val ids = globalESPBIds
                                                            if (ids.isNotEmpty()) {
                                                                weightBridgeViewModel.updateDataIsZippedESPB(
                                                                    ids,
                                                                    1
                                                                )
                                                            }
                                                        }
                                                        zipDeferred.complete(true)
                                                    } else {
                                                        Log.e(
                                                            "UploadCheck",
                                                            "❌ ZIP creation failed"
                                                        )
                                                        zipDeferred.complete(false)
                                                    }
                                                }
                                            }
                                        } else {
                                            zipDeferred.complete(false)
                                        }

                                        loadingDialog.dismiss()
                                    }

                                    // Wait for ZIP to complete before calling the next function
                                    val zipSuccess = zipDeferred.await()
                                    if (zipSuccess || allUploadZipFilesToday.isNotEmpty()) {
                                        handleUpload(selectedItems)
                                    } else {
                                        handleUpload(selectedItems)
                                    }
                                }


                            }


                        } else {
                            AlertDialogUtility.withSingleAction(
                                this@ListHistoryWeighBridgeActivity,
                                getString(R.string.al_back),
                                getString(R.string.al_no_internet_connection),
                                getString(R.string.al_no_internet_connection_description_upload_espb_krani),
                                "network_error.json",
                                R.color.colorRedDark
                            ) {}
                        }
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun setupObserveData() {
        weightBridgeViewModel.savedESPBByKrani.observe(this) { data ->

            if (data.isNotEmpty()) {
                speedDial.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                lifecycleScope.launch {
                    try {
                        val filteredData = coroutineScope {
                            globalIdESPBKraniTimbang = data.map { it.id }
                            data.map { item ->
                                async {
                                    val blokJjgList = item.blok_jjg
                                        .split(";")
                                        .mapNotNull {
                                            it.split(",").takeIf { it.size == 2 }
                                                ?.let { (id, jjg) ->
                                                    id.toIntOrNull()
                                                        ?.let { idInt -> idInt to jjg.toIntOrNull() }
                                                }
                                        }

                                    val idBlokList = blokJjgList.map { it.first }
                                    val pemuatList = item.pemuat_id.split(",")?.map { it.trim() }
                                        ?.filter { it.isNotEmpty() } ?: emptyList()

                                    val pemuatData = withContext(Dispatchers.IO) {
                                        try {
                                            weightBridgeViewModel.getPemuatByIdList(pemuatList)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                            null
                                        }
                                    }


                                    val pemuatNama = pemuatData?.mapNotNull { it.nama }
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ") ?: "-"

                                    val blokData = withContext(Dispatchers.IO) {
                                        try {
                                            weightBridgeViewModel.getDataByIdInBlok(idBlokList)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Blok Data: ${e.message}")
                                            null
                                        }
                                    }

                                    val deptAbbr = blokData?.firstOrNull()?.dept_abbr ?: "-"
                                    val divisiAbbr = blokData?.firstOrNull()?.divisi_abbr ?: "-"
                                    val deptPPRO = blokData?.firstOrNull()?.dept_ppro ?: 0
                                    val divisiPPRO = blokData?.firstOrNull()?.dept_ppro ?: 0

                                    val formattedBlokList =
                                        blokJjgList.mapNotNull { (idBlok, totalJjg) ->
                                            val blokKode =
                                                blokData?.find { it.id == idBlok }?.nama
                                            if (blokKode != null && totalJjg != null) {
                                                "• $blokKode ($totalJjg jjg)"
                                            } else null
                                        }.joinToString("\n").takeIf { it.isNotBlank() } ?: "-"

                                    val millId = item.mill_id ?: 0
                                    val transporterId = item.transporter_id ?: 0
                                    val createdAt = item.created_at
                                    val createAtFormatted = formatToIndonesianDate(createdAt)


                                    val millData = withContext(Dispatchers.IO) {
                                        try {
                                            weightBridgeViewModel.getMillName(millId)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Mill Data: ${e.message}")
                                            null
                                        }
                                    } ?: emptyList()

                                    val millAbbr =
                                        millData.firstOrNull()?.let { "${it.abbr} (${it.nama})" }
                                            ?: "-"
                                    val millIp = millData.firstOrNull().let { it!!.ip_address }

                                    val transporterData = withContext(Dispatchers.IO) {
                                        try {
                                            weightBridgeViewModel.getTransporterName(transporterId)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Transporter Data: ${e.message}")
                                            null
                                        }
                                    } ?: emptyList()

                                    val transporterName = transporterData.firstOrNull()?.nama ?: "-"
                                    val totalJjg = blokJjgList.mapNotNull { it.second }.sum()


                                    WBData(
                                        //data untuk upload staging
                                        id = item.id,
                                        ip = millIp.toString(),
                                        dept_ppro = deptPPRO,
                                        divisi_ppro = divisiPPRO,
                                        commodity = 0,
                                        blok_jjg = item.blok_jjg,
                                        nopol = item.nopol,
                                        driver = item.driver,
                                        pemuat_id = item.pemuat_id,
                                        transporter_id = item.transporter_id,
                                        mill_id = item.mill_id,
                                        created_by_id = item.created_by_id,
                                        created_at = item.created_at,
                                        noSPB = item.noESPB.ifEmpty { "-" },
                                        estate = deptAbbr.ifEmpty { "-" },
                                        afdeling = divisiAbbr.ifEmpty { "-" },
                                        datetime = createAtFormatted,
                                        status_upload_cmp_wb = item.status_upload_cmp_wb,
                                        status_upload_ppro_wb = item.status_upload_ppro_wb,
                                        uploaded_at_wb = item.uploaded_at_wb,
                                        uploaded_at_ppro_wb = item.uploaded_at_ppro_wb,
                                        uploaded_wb_response = item.uploaded_wb_response,
                                        uploaded_ppro_response = item.uploaded_ppro_response,
                                        formattedBlokList = formattedBlokList,
                                        pemuat_nama = pemuatNama,
                                        totalJjg = totalJjg,
                                        mill_name = millAbbr,
                                        transporter_name = transporterName,
                                    )

                                }
                            }.map { it.await() } // Wait for all async tasks to complete
                        }

                        adapter.updateList(filteredData)
                    } catch (e: Exception) {
                        AppLogger.e("Data processing error: ${e.message}")
                    }
                }
            } else {
                tvEmptyState.text = "No Uploaded e-SPB data available"
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }

        }
    }

    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
        observe(lifecycleOwner, object : Observer<T> {
            override fun onChanged(value: T) {
                removeObserver(this)
                observer.onChanged(value)
            }
        })
    }

    private fun setupRecyclerView() {
        val headers = listOf("E-SPB", "ESTATE\nAFDELING", "TGL BUAT", "STATUS UPLOAD")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WeighBridgeAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)
        headerCheckBoxWB = tableHeader.findViewById(R.id.headerCheckBoxPanen)

        headerCheckBoxWB.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAllItems(isChecked)
        }
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th5)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }
    }


    private fun initViewModel() {
        val factory = WeighBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeighBridgeViewModel::class.java]

        val factory2 = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory2)[DatasetViewModel::class.java]

        val factory4 = UploadCMPViewModel.UploadCMPViewModelFactory(application)
        uploadCMPViewModel = ViewModelProvider(this, factory4)[UploadCMPViewModel::class.java]
    }

    private fun getDatasetsToDownload(
        regionalId: Int,
        estateId: Int,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
        lastModifiedDatasetKendaraan: String?,
        lastModifiedSettingJSON: String?
    ): List<DatasetRequest> {
        val datasets = mutableListOf<DatasetRequest>()

        val jabatan = prefManager!!.jabatanUserLogin
        val regionalUser = prefManager!!.regionalIdUserLogin!!.toInt()
        if (jabatan!!.contains(AppUtils.ListFeatureByRoleUser.KeraniTimbang, ignoreCase = true)) {
            datasets.add(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                )
            )
        }

        datasets.addAll(
            listOf(
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
                DatasetRequest(
                    regional = regionalId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.mill
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                ),
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetKemandoran,
                    dataset = AppUtils.DatasetNames.kemandoran
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetTransporter,
                    dataset = AppUtils.DatasetNames.transporter
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetKendaraan,
                    dataset = AppUtils.DatasetNames.kendaraan
                ),
                DatasetRequest(
                    lastModified = lastModifiedSettingJSON,
                    dataset = AppUtils.DatasetNames.settingJSON
                )
            )
        )

        return datasets
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.GONE

        AppUtils.setupUserHeader(
            userName = userName,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName,
            prefManager = prefManager,
            lastUpdateText = lastUpdateText,
            titleAppNameAndVersionText = titleAppNameAndVersion,
            context = this
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()

        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    fun formatGlobalDate(dateString: String): String {
        // Parse the date string in format "YYYY-MM-DD"
        val parts = dateString.split("-")
        if (parts.size != 3) return dateString // Return original if format doesn't match

        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        // Return formatted date string using getMonthFormat
        return "${AppUtils.getMonthFormat(month)} $day $year"
    }

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun processSelectedDate(selectedDate: String) {

        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate
        weightBridgeViewModel.loadHistoryESPB(selectedDate)

        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE
//            loadingDialog.show()
//            loadingDialog.setMessage("Sedang mengambil data...", true)
            // Get today's date in backend format
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )

            // Reset the selected date in your utils
            AppUtils.setSelectedDate(todayBackendDate)

            // Update the dateButton to show today's date
            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate

            weightBridgeViewModel.loadHistoryESPB(selectedDate)

        }

        filterDateContainer.visibility = View.VISIBLE
    }


}