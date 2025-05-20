package com.cbi.mobile_plantation.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.cbi.mobile_plantation.ui.adapter.UploadCMPItem
import com.cbi.mobile_plantation.ui.adapter.UploadItem
import com.cbi.mobile_plantation.ui.adapter.UploadProgressAdapter
import com.cbi.mobile_plantation.ui.adapter.UploadProgressCMPDataAdapter
import com.cbi.mobile_plantation.ui.adapter.WBData
import com.cbi.mobile_plantation.ui.adapter.WeighBridgeAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.HomePageActivity.ResponseJsonUpload
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.io.FileOutputStream
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
    private var infoApp: String = ""
    private lateinit var datasetViewModel: DatasetViewModel
    private var globalFormattedDate: String = ""
    private var uploadCMPData: List<Pair<String, String>> = emptyList()
    private var globalESPBIds: List<Int> = emptyList()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var uploadCMPViewModel: UploadCMPViewModel
    private lateinit var speedDial: SpeedDialView
    private var globalIdESPBKraniTimbang: List<Int> = emptyList()
    private lateinit var tvEmptyState: TextView // Add this
    private lateinit var headerCheckBoxWB: CheckBox // Add this
    private lateinit var dateButton: Button

    private var allJsonData = mutableListOf<JsonData>()

    data class JsonData(
        val data: String,           // The JSON string data
    )

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

    @SuppressLint("SetTextI18n")
    private fun handleUpload(selectedItems: List<Map<String, Any>>) {

        var number = 0

        val pproItems = selectedItems.map { item ->
            UploadCMPItem(
                id = item["id"] as Int,
                title = item["no_espb"] as String,
                fullPath = "",
                baseFilename = "",
                data = Gson().toJson(
                    mapOf(
                        "id" to item["id"] as Int,
                        "ip" to item["ip"].toString(),
                        "num" to number++,
                        "dept_ppro" to (item["dept_ppro"] as Number).toInt(),
                        "divisi_ppro" to (item["divisi_ppro"] as Number).toInt(),
                        "commodity" to (item["commodity"] as Number).toInt(),
                        "blok_jjg" to item["blok_jjg"] as String,
                        "nopol" to item["nopol"] as String,
                        "driver" to item["driver"] as String,
                        "pemuat_id" to item["pemuat_id"].toString(),
                        "transporter_id" to (item["transporter_id"] as Number).toInt(),
                        "mill_id" to (item["mill_id"] as Number).toInt(),
                        "created_by_id" to (item["created_by_id"] as Number).toInt(),
                        "created_at" to item["created_at"] as String,
                        "no_espb" to item["no_espb"] as String,
                        "uploader_info" to infoApp,
                        "uploaded_at" to SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date()),
                        "uploaded_by_id" to prefManager!!.idUserLogin!!.toInt()
                    )
                ),
                type = AppUtils.DatabaseServer.PPRO,
                databaseTable = ""
            )
        }

        val cmpItems = allJsonData.mapIndexed { index, jsonData ->
            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            UploadCMPItem(
                id = pproItems.maxByOrNull { it.id }?.id?.plus(index + 1) ?: (index + 1),
                title = "ESPB Data (${allJsonData.size} item)", // Use the noESPB in the title
                fullPath = "", // Empty since we don't have filePath
                baseFilename = "", // Empty since we don't have fileName
                data = Gson().toJson(mapOf(
                    "espb_json" to jsonData.data,
                    "espb_ids" to globalESPBIds,
                    "uploader_info" to infoApp,
                    "uploaded_at" to currentDate,
                    "uploaded_by_id" to prefManager!!.idUserLogin!!.toInt(),
                )),
                type = AppUtils.DatabaseServer.CMP,
                databaseTable = ""
            )
        }

        val allUploadItems = cmpItems
        uploadCMPViewModel.resetState()
        loadingDialog.dismiss()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)
        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Upload Data CMP"

        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
        val totalSizeProgressTV = dialogView.findViewById<TextView>(R.id.total_size_progress)
        val counterSizeFile = dialogView.findViewById<LinearLayout>(R.id.counterSizeFile)
        counterSizeFile.visibility = View.VISIBLE

        // Get all buttons
        val closeDialogBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val btnUploadDataCMP = dialogView.findViewById<MaterialButton>(R.id.btnUploadDataCMP)
        val btnRetryUpload = dialogView.findViewById<MaterialButton>(R.id.btnRetryDownloadDataset)

        val containerDownloadDataset =
            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        containerDownloadDataset.visibility = View.VISIBLE

        // Initially show only close and upload buttons
        closeDialogBtn.visibility = View.VISIBLE
        btnUploadDataCMP.visibility = View.VISIBLE
        btnRetryUpload.visibility = View.GONE

        var isRetryOperation = false
        var failedUploads: List<UploadCMPItem> = listOf()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UploadProgressCMPDataAdapter(allUploadItems)
        recyclerView.adapter = adapter

        adapter.updateItems(allUploadItems)

        Handler(Looper.getMainLooper()).postDelayed({
            if (counterTV.text == "0/0" && allUploadItems.size > 0) {
                counterTV.text = "0/${allUploadItems.size}"
            }
        }, 100)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        closeDialogBtn.setOnClickListener {
            allJsonData.clear()
            uploadCMPViewModel.resetState()
            weightBridgeViewModel.loadHistoryESPB()
            dialog.dismiss()
        }

        fun startUpload(itemsToUpload: List<UploadCMPItem> = allUploadItems) {
            if (!AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withSingleAction(
                    this@ListHistoryWeighBridgeActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
                return
            }

            // Disable all buttons during upload
            btnUploadDataCMP.isEnabled = false
            closeDialogBtn.isEnabled = false
            btnRetryUpload.isEnabled = false
            btnUploadDataCMP.alpha = 0.7f
            closeDialogBtn.alpha = 0.7f
            btnRetryUpload.alpha = 0.7f
            btnUploadDataCMP.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            closeDialogBtn.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            btnRetryUpload.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

            // Reset title color
            titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))
            titleTV.text = "Upload Data CMP"

            uploadCMPViewModel.uploadMultipleJsonsV3(itemsToUpload)

        }

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
                        isRetryOperation = false  // Reset retry flag for fresh upload
                        startUpload()
                    },
                    cancelFunction = { }
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

        btnRetryUpload.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                // Create new upload items only for failed uploads
                val retryUploadItems = mutableListOf<UploadCMPItem>()
                var itemId = 0

                AppLogger.d("failedUploads $failedUploads")

                val originalSizes = mutableMapOf<String, Long>()

                // Store sizes of original failed uploads by title/path to preserve them
                failedUploads.forEach { failedItem ->
                    // Get the size from the adapter's fileSizeMap
                    val originalSize = adapter.getFileSizeById(failedItem.id)
                    if (originalSize > 0) {
                        // Create a unique key using title and path to identify items
                        val itemKey = "${failedItem.title}:${failedItem.fullPath}"
                        originalSizes[itemKey] = originalSize
                    }
                }

                failedUploads.forEach { failedItem ->
                    when (failedItem.type) {
                        AppUtils.DatabaseServer.PPRO -> {
                            // For PPRO uploads, we need to preserve the data field
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = failedItem.data,
                                    type = AppUtils.DatabaseServer.PPRO,
                                    databaseTable = ""
                                )
                            )
                        }

                        AppUtils.DatabaseServer.CMP -> {
                            // For CMP uploads, we focus on the file path
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = "",
                                    type = AppUtils.DatabaseServer.CMP,
                                    databaseTable = ""
                                )
                            )
                        }

                        "json" -> {
                            // For JSON uploads (could be Panen or ESPB from setupDialogUpload)
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = "", // For JSON files, path is what matters
                                    type = "json",
                                    databaseTable = ""
                                )
                            )
                        }

                        else -> {
                            // Unknown type, add as is
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = failedItem.data,
                                    type = failedItem.type,
                                    databaseTable = ""
                                )
                            )
                        }
                    }
                }

                isRetryOperation = true

                // Clear and update the RecyclerView with only failed items
                adapter.updateItems(retryUploadItems)


                retryUploadItems.forEach { retryItem ->
                    // Create a key to match with original size map
                    val itemKey = "${retryItem.title}:${retryItem.fullPath}"
                    val size = originalSizes[itemKey] ?: 0L

                    AppLogger.d("Setting file size for item ${retryItem.id} (${retryItem.title}): originalSize=$size")

                    // If we have data but no size from the map, calculate from data length
                    if (size == 0L && retryItem.data.isNotEmpty()) {
                        val dataSize = retryItem.data.length.toLong()
                        AppLogger.d("No original size found, using data length: $dataSize")
                        adapter.setFileSize(retryItem.id, dataSize)
                    } else {
                        // Use the stored size
                        AppLogger.d("Using original size: $size")
                        adapter.setFileSize(retryItem.id, size)
                    }

                    // Verify the size was set correctly
                    val verifySize = adapter.getFileSizeById(retryItem.id)
                    AppLogger.d("Verified size for item ${retryItem.id}: $verifySize")
                }

                // Reset adapter state (progress bars, status icons, etc.)
                adapter.resetState()

                // Reset view model state
                uploadCMPViewModel.resetState()

                AppLogger.d("retryUploadItems $retryUploadItems")
                // Update UI elements
                counterTV.text = "0/${retryUploadItems.size}"
                titleTV.text = "Retrying Failed Uploads"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))

                // Hide retry button, show upload button
                btnRetryUpload.visibility = View.GONE
                btnUploadDataCMP.visibility = View.VISIBLE

                startUpload(retryUploadItems)
            } else {
                AlertDialogUtility.withSingleAction(
                    this@ListHistoryWeighBridgeActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
            }
        }

        uploadCMPViewModel.completedCount.observe(this) { completed ->
            val total = uploadCMPViewModel.totalCount.value ?: allUploadItems.size
            counterTV.text = "$completed/$total"
        }

        // Observe progress for each item
        uploadCMPViewModel.itemProgressMap.observe(this) { progressMap ->
            // Update progress for each item
            for ((id, progress) in progressMap) {
                AppLogger.d("Progress update for item $id: $progress%")
                adapter.updateProgress(id, progress)
            }

            // Update title if any upload is in progress
            if (progressMap.values.any { it in 1..99 }) {
                titleTV.text = "Sedang Upload Data..."
            }

            val uploadedBytes = adapter.getTotalUploadedBytes()
            val totalBytes = adapter.getTotalFileSize()
            val overallProgress = adapter.getOverallProgress()

            totalSizeProgressTV.text =
                " ${AppUtils.formatFileSize(uploadedBytes)} / ${AppUtils.formatFileSize(totalBytes)} ($overallProgress%)"
        }

        // Observe status for each item
        uploadCMPViewModel.itemStatusMap.observe(this) { statusMap ->
            // Update status for each item
            for ((id, status) in statusMap) {
                AppLogger.d("Status for item $id: $status")
                adapter.updateStatus(id, status)
            }

            val allFinished = statusMap.values.none {
                it == AppUtils.UploadStatusUtils.WAITING || it == AppUtils.UploadStatusUtils.UPLOADING
            }

            val allSuccess = statusMap.values.all { it == AppUtils.UploadStatusUtils.SUCCESS }

            AppLogger.d("statusMap $statusMap")

            if (allFinished && statusMap.isNotEmpty()) {
                lifecycleScope.launch {
                    loadingDialog.show()
                    loadingDialog.setMessage("Sedang proses data", true)
                    uploadCMPViewModel.getAllIds()
                    delay(500)

                    withContext(Dispatchers.Main) {

                        AppLogger.d("allSuccess $allSuccess")
                        if (allSuccess) {
                            // Reset retry flag since we succeeded
                            isRetryOperation = false

                            launch {
//                                val processingComplete = processUploadResponses()
                                val processingComplete = true

                                withContext(Dispatchers.Main) {
                                    if (processingComplete) {
                                        // Processing was successful
                                        titleTV.text = "Upload Berhasil"
                                        titleTV.setTextColor(
                                            ContextCompat.getColor(
                                                titleTV.context,
                                                R.color.greenDarker
                                            )
                                        )

                                        btnUploadDataCMP.visibility = View.GONE
                                        btnRetryUpload.visibility = View.GONE
                                    } else {
                                        // Upload completed but processing not finished (status code not 3)
                                        titleTV.text = "Upload Gagal"
                                        titleTV.setTextColor(
                                            ContextCompat.getColor(
                                                titleTV.context,
                                                R.color.colorRedDark
                                            )
                                        )

                                        // Show retry button to let user retry processing
                                        btnUploadDataCMP.visibility = View.GONE
                                        btnRetryUpload.visibility = View.VISIBLE
                                    }

                                    // Re-enable buttons
                                    closeDialogBtn.isEnabled = true
                                    closeDialogBtn.alpha = 1f
                                    closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)

                                    btnRetryUpload.isEnabled = true
                                    btnRetryUpload.alpha = 1f
                                    btnRetryUpload.iconTint = ColorStateList.valueOf(Color.WHITE)

                                    loadingDialog.dismiss()
                                }
                            }
                        } else {

                            if (isRetryOperation) {
                                // If we're already in a retry operation, use the current adapter's items
                                val currentItems = adapter.getItems()

                                failedUploads = currentItems.filter { item ->
                                    val status = statusMap[item.id]
                                    status != AppUtils.UploadStatusUtils.SUCCESS
                                }
                            } else {
                                // First failure, use the original upload items
                                failedUploads = allUploadItems.filter { item ->
                                    val status = statusMap[item.id]
                                    status != AppUtils.UploadStatusUtils.SUCCESS
                                }
                            }

                            AppLogger.d("Collected ${failedUploads.size} failed uploads for retry")

                            launch {
//                                val processingComplete = processUploadResponses()
                                val processingComplete = true
                                withContext(Dispatchers.Main) {
                                    if (processingComplete) {
                                        titleTV.text = "Terjadi Kesalahan Upload"
                                        titleTV.setTextColor(
                                            ContextCompat.getColor(
                                                titleTV.context,
                                                R.color.colorRedDark
                                            )
                                        )

                                        // Show retry button and hide upload button
                                        btnUploadDataCMP.visibility = View.GONE
                                        btnRetryUpload.visibility = View.VISIBLE

                                        // Re-enable buttons
                                        closeDialogBtn.isEnabled = true
                                        closeDialogBtn.alpha = 1f
                                        closeDialogBtn.iconTint =
                                            ColorStateList.valueOf(Color.WHITE)

                                        btnRetryUpload.isEnabled = true
                                        btnRetryUpload.alpha = 1f
                                        btnRetryUpload.iconTint =
                                            ColorStateList.valueOf(Color.WHITE)

                                        loadingDialog.dismiss()

                                    } else {
                                        titleTV.text = "Terjadi Kesalahan Upload"
                                        titleTV.setTextColor(
                                            ContextCompat.getColor(
                                                titleTV.context,
                                                R.color.colorRedDark
                                            )
                                        )

                                        // Show retry button and hide upload button
                                        btnUploadDataCMP.visibility = View.GONE
                                        btnRetryUpload.visibility = View.VISIBLE

                                        // For error case, dismiss the dialog immediately
                                        AppLogger.d("gas brroooo")
                                        // Re-enable buttons
                                        closeDialogBtn.isEnabled = true
                                        closeDialogBtn.alpha = 1f
                                        closeDialogBtn.iconTint =
                                            ColorStateList.valueOf(Color.WHITE)

                                        btnRetryUpload.isEnabled = true
                                        btnRetryUpload.alpha = 1f
                                        btnRetryUpload.iconTint =
                                            ColorStateList.valueOf(Color.WHITE)

                                        loadingDialog.dismiss()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//
        // Observe errors for each item
        uploadCMPViewModel.itemErrorMap.observe(this) { errorMap ->
            for ((id, error) in errorMap) {
                if (!error.isNullOrEmpty()) {
                    adapter.updateError(id, error)
                }
            }

            if (errorMap.values.any { !it.isNullOrEmpty() }) {
                titleTV.text = "Terjadi Kesalahan Upload"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.colorRedDark))
            }
        }

        uploadCMPViewModel.itemResponseMap.observe(this) { responseMap ->
            lifecycleScope.launch {
                AppLogger.d("responseMap $responseMap")

//                for ((_, response) in responseMap) {
//                    response?.let {
//                        // Check if response type is JSON
//                        if (response.type == "json") {
//                            globalResponseJsonUploadList.add(
//                                ResponseJsonUpload(
//                                    response.trackingId,
//                                    response.nama_file,
//                                    response.status,
//                                    response.tanggal_upload,
//                                    response.type
//                                )
//                            )
//
//                            val keyJsonName = response.trackingId.toString()
//
//                            if (response.success) {
//                                try {
//                                    val extractionDeferred =
//                                        CompletableDeferred<Pair<List<Int>, List<Int>>>()
//
//                                    AppLogger.d("Starting JSON extraction for ${response.nama_file}")
//
//                                    launch(Dispatchers.IO) {
//                                        try {
//                                            val result = AppUtils.extractIdsFromJsonFile(
//                                                context = this@ListHistoryWeighBridgeActivity,
//                                                fileName = response.nama_file
//                                            )
//                                            extractionDeferred.complete(result)
//                                        } catch (e: Exception) {
//                                            extractionDeferred.completeExceptionally(e)
//                                        }
//                                    }
//
//                                    val (panenIds, espbIds) = withTimeout(5000) {
//                                        extractionDeferred.await()
//                                    }
//
//                                    AppLogger.d("Extraction complete for JSON $keyJsonName. PANEN IDs: ${panenIds.size}, ESPB IDs: ${espbIds.size}")
//
//                                    // Store IDs by part number
//                                    globalPanenIdsByPart[keyJsonName] = panenIds
//                                    globalEspbIdsByPart[keyJsonName] = espbIds
//
//                                } catch (e: Exception) {
//                                    AppLogger.e("Error during JSON extraction for file $keyJsonName: ${e.message}")
//
//                                    globalPanenIdsByPart[keyJsonName] = emptyList()
//                                    globalEspbIdsByPart[keyJsonName] = emptyList()
//                                }
//                            } else {
//
//                            }
//                        } else if (response.type == "image") {
//                            globalResponseJsonUploadList.add(
//                                ResponseJsonUpload(
//                                    trackingId = 0,
//                                    nama_file = "",
//                                    status = 0,
//                                    tanggal_upload = "",
//                                    type = response.type
//                                )
//                            )
//                            if (!response.success) {
//
//                            } else {
//
//                            }
//                        } else {
//                            AppLogger.d("Skipping non-JSON upload: type = ${response.type}")
//                        }
//                    }
//                }

//                AppLogger.d("Stored IDs by part: ${globalPanenIdsByPart.keys}")
//                AppLogger.d("Total IDs - PANEN: ${globalPanenIds.size}, ESPB: ${globalESPBIds.size}")
            }
        }
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
                                    dataDeferred.complete(
                                        data ?: emptyList()
                                    ) // Ensure it's never null
                                }
                                val data = dataDeferred.await()

                                //kode khusus untuk update UploadCMP sebelum melakukan upload
                                uploadCMPData = data
                                if (uploadCMPData.isNotEmpty()) {
                                    AppLogger.d("Starting update for ${uploadCMPData.size} items")
                                    val updateSuccessful =
                                        datasetViewModel.updateLocalUploadCMP(uploadCMPData,
                                            prefManager!!.jabatanUserLogin!!
                                        ).await()
                                    AppLogger.d("Update status: $updateSuccessful, now proceeding to file check")
                                } else {
                                    AppLogger.d("No data to update")

                                }

                                AppUtils.clearTempJsonFiles(this@ListHistoryWeighBridgeActivity)

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
                                    val espbList = espbDeferred.await()
                                    var espbJsonString: String? = null // Store the JSON string instead of filepath

                                    try {
                                        if (espbList.isNotEmpty()) {
                                            val allZipped = espbList.all { it.dataIsZipped == 1 }

                                            // Find only the items that need uploading (status is between 1 and 3)
                                            val itemsNeedingUpload =
                                                espbList.filter { it.status_upload_cmp_wb !in 1..3 }

                                            AppLogger.d("itemsNeedingUpload $itemsNeedingUpload")

                                            if (itemsNeedingUpload.isEmpty()) {
                                                // If no items need upload, log and skip JSON creation
                                                globalESPBIds = espbList.map { it.id }

                                                if (allZipped) {
                                                    zipDeferred.complete(true)
                                                }
                                            } else {
                                                // Map and process only the items that need upload
                                                mappedESPBData = itemsNeedingUpload.map { data ->
                                                    val blokJjgList =
                                                        data.blok_jjg.split(";").mapNotNull {
                                                            it.split(",").takeIf { it.size == 2 }
                                                                ?.let { (id, jjg) ->
                                                                    id.toIntOrNull()
                                                                        ?.let { it to jjg.toIntOrNull() }
                                                                }
                                                        }
                                                    val idBlokList = blokJjgList.map { it.first }
                                                    val concatenatedIds =
                                                        idBlokList.joinToString(",")
                                                    val totalJjg =
                                                        blokJjgList.mapNotNull { it.second }.sum()

                                                    val firstBlockId = idBlokList.firstOrNull()

                                                    // Create a CompletableDeferred to handle the async operation
                                                    val tphDeferred =
                                                        CompletableDeferred<TPHNewModel?>()

                                                    // Fetch the TPH data if we have a block ID
                                                    firstBlockId?.let { blockId ->
                                                        weightBridgeViewModel.fetchTPHByBlockId(
                                                            blockId
                                                        )

                                                        // Set up a one-time observer for the LiveData
                                                        weightBridgeViewModel.tphData.observeOnce(
                                                            this@ListHistoryWeighBridgeActivity
                                                        ) { tphModel ->
                                                            tphDeferred.complete(tphModel)
                                                        }
                                                    }
                                                        ?: tphDeferred.complete(null) // Complete with null if no block ID

                                                    // Wait for the TPH data
                                                    val tphData = tphDeferred.await()


                                                    AppLogger.d(tphData.toString())
                                                    mapOf(
                                                        "id" to data.id,
                                                        "regional" to (tphData?.regional ?: ""),
                                                        "wilayah" to (tphData?.wilayah ?: ""),
                                                        "company" to (tphData?.company ?: ""),
                                                        "dept" to (tphData?.dept ?: ""),
                                                        "divisi" to (tphData?.divisi ?: ""),
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
                                                        "updated_nama" to prefManager!!.nameUserLogin.toString(),
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

                                                // Only create JSON if we have items to upload
                                                if (mappedESPBData.isNotEmpty()) {
                                                    // Wrap the ESPB data as requested
                                                    val wrappedEspbData = mapOf(
                                                        AppUtils.DatabaseTables.ESPB to mappedESPBData
                                                    )

                                                    // Convert the wrapped data to JSON
                                                    espbJsonString = Gson().toJson(wrappedEspbData)


                                                    try {
                                                        val tempDir =
                                                            File(
                                                                getExternalFilesDir(null),
                                                                "TEMP"
                                                            ).apply {
                                                                if (!exists()) mkdirs()
                                                            }

                                                        val filename =
                                                            "espb_wb_data_${System.currentTimeMillis()}.json"
                                                        val tempFile = File(tempDir, filename)

                                                        FileOutputStream(tempFile).use { fos ->
                                                            fos.write(espbJsonString.toByteArray())
                                                        }

                                                        AppLogger.d("Saved raw espb_wb data to temp file: ${tempFile.absolutePath}")
                                                    } catch (e: Exception) {
                                                        AppLogger.e("Failed to save espb_wb data to temp file: ${e.message}")
                                                        e.printStackTrace()
                                                    }

                                                    // Add the JSON string to our data structure instead of file info
                                                    allJsonData.add(
                                                        JsonData(
                                                            data = espbJsonString
                                                        )
                                                    )

                                                    AppLogger.d("Created JSON string for upload with ${itemsNeedingUpload.size} items")
                                                }

                                                // Only skip the zipping process if all items are already zipped
                                                if (allZipped) {
                                                    Log.d(
                                                        "UploadCheck",
                                                        "✅ All ESPB data is already zipped, skipping zipping process"
                                                    )
                                                    zipDeferred.complete(true)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("UploadCheck", "❌ Error: ${e.message}")
                                    } finally {
                                        // Only do zipping if not all items were already zipped
                                        val allZipped = espbList?.all { it.dataIsZipped == 1 } ?: false

                                        if (!allZipped) {
                                            val uploadDataList = mutableListOf<Pair<String, List<Map<String, Any>>>>()
                                            if (mappedESPBData.isNotEmpty()) uploadDataList.add(
                                                AppUtils.DatabaseTables.ESPB to mappedESPBData
                                            )

                                            if (uploadDataList.isNotEmpty()) {
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    AppUtils.createAndSaveZipUploadCMPSingle(
                                                        this@ListHistoryWeighBridgeActivity,
                                                        uploadDataList,
                                                        prefManager!!.idUserLogin.toString()
                                                    ) { success, fileName, fullPath, zipFile ->
                                                        if (success) {
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
                                        }

                                        loadingDialog.dismiss()
                                    }

                                    val zipSuccess = zipDeferred.await()

                                    handleUpload(selectedItems)
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

                                AppLogger.d("item $item")
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

                                    val transporterName = if (transporterId == 0) {
                                        "Internal"
                                    } else {
                                        // Only fetch from database if transporter_id is not 0
                                        val transporterData = withContext(Dispatchers.IO) {
                                            try {
                                                weightBridgeViewModel.getTransporterName(transporterId)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Transporter Data: ${e.message}")
                                                null
                                            }
                                        } ?: emptyList()

                                        AppLogger.d(transporterData.toString())
                                        transporterData.firstOrNull()?.nama ?: "-"
                                    }
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
                                        date_scan = item.date_scan
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