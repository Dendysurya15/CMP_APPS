package com.cbi.cmp_project.ui.view.weighBridge

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
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.repository.WeighBridgeRepository
import com.cbi.cmp_project.ui.adapter.UploadCMPItem
import com.cbi.cmp_project.ui.adapter.UploadItem
import com.cbi.cmp_project.ui.adapter.UploadProgressAdapter
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.adapter.WeighBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.viewModel.UploadCMPViewModel
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.button.MaterialButton
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        infoApp = AppUtils.getDeviceInfo(this@ListHistoryWeighBridgeActivity).toString()
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupSpeedDial()
        setupObserveData()
        weightBridgeViewModel.loadHistoryUploadeSPB()
    }

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyStateKraniTimbang)
        speedDial = findViewById(R.id.dial_tph_list_krani_timbang_espb)
    }


    private fun handleUpload(selectedItems: List<Map<String, Any>>) {

        val uploadItems = selectedItems.map { item ->
            UploadItem(
                id = item["id"] as Int,
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

        var nextId = (uploadItems.maxByOrNull { it.id }?.id ?: 0) + 1

        val allItems = mutableListOf<UploadItem>().apply { addAll(uploadItems) }

        if (!zipFilePath.isNullOrEmpty() && !zipFileName.isNullOrEmpty()) {
            allItems.add(
                UploadItem(
                    id = nextId++,
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
                    no_espb = zipFileName.toString(),
                    uploader_info = "",
                    uploaded_at = "",
                    uploaded_by_id = 0,
                    file = zipFilePath.toString(),
                    endpoint = AppUtils.DatabaseServer.CMP
                )
            )
        }

        allUploadZipFilesToday.forEach { file ->
            allItems.add(
                UploadItem(
                    id = nextId++,
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
            weightBridgeViewModel.loadHistoryUploadeSPB()
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
                // Disable buttons
                btnUploadDataCMP.isEnabled = false
                closeDialogBtn.isEnabled = false
                btnUploadDataCMP.alpha = 0.7f
                closeDialogBtn.alpha = 0.7f
                btnUploadDataCMP.iconTint =
                    ColorStateList.valueOf(Color.parseColor("#80FFFFFF")) // 50% transparent white
                closeDialogBtn.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

                weightBridgeViewModel.uploadESPBKraniTimbang(
                    allUploadItems.map { uploadItem ->
                        mapOf(
                            "id" to uploadItem.id,
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
                containerDownloadDataset.visibility = View.VISIBLE
                cancelDownloadDataset.visibility = View.VISIBLE
                btnUploadDataCMP.visibility = View.GONE
                closeDialogBtn.isEnabled = true
                closeDialogBtn.alpha = 1f
                closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
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
            ContextCompat.getColor(this, R.color.colorRedDark)
        ) {
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
                    weightBridgeViewModel.loadHistoryUploadeSPB()
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
        }
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
//                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
//
//                        view.background = ContextCompat.getDrawable(this@ListPanenTBSActivity, R.drawable.rounded_top_right_left)
//
//                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
//                        dialog.setContentView(view)
////                        view.layoutParams.height = 500.toPx()
//
//                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
//                        val data = "test"
//                        generateHighQualityQRCode(data, qrCodeImageView)
//                        dialog.setOnShowListener {
//                            val bottomSheet =
//                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
//                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        }
//                        dialog.show()
                        true
                    }

                    R.id.deleteSelected -> {
                        val selectedItems = adapter.getSelectedItemsIdLocal()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = adapter.getSelectedItemsForUpload()

                        val selectedIds = selectedItems.map { it["id"] as Int }

                        if (AppUtils.isNetworkAvailable(this@ListHistoryWeighBridgeActivity)) {

                            lifecycleScope.launch {
                                loadingDialog.show()
                                loadingDialog.setMessage("Sedang mempersiapkan zip...")
                                delay(500)


                                allUploadZipFilesToday =
                                    AppUtils.checkUploadZipReadyToday(this@ListHistoryWeighBridgeActivity)
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
                                                val blokData = withContext(Dispatchers.IO) {
                                                    try {
                                                        weightBridgeViewModel.getBlokById(idBlokList)
                                                    } catch (e: Exception) {
                                                        Log.e(
                                                            "UploadCheck",
                                                            "❌ Error fetching Blok Data: ${e.message}"
                                                        )
                                                        null
                                                    }
                                                }
                                                    ?: throw Exception("Failed to fetch Blok Data! Please check the dataset.")

                                                val regional =
                                                    blokData.firstOrNull()?.regional ?: ""
                                                val company = blokData.firstOrNull()?.company ?: ""
                                                val dept = blokData.firstOrNull()?.dept ?: ""
                                                val divisi = blokData.firstOrNull()?.divisi ?: ""

                                                mapOf(
                                                    "id" to data.id,
                                                    "regional" to regional,
                                                    "company" to company,
                                                    "dept" to dept,
                                                    "divisi" to divisi,
                                                    "blok_id" to concatenatedIds,
                                                    "jjg" to totalJjg,
                                                    "created_by_id" to data.created_by_id,
                                                    "created_at" to data.created_at,
                                                    "nopol" to data.nopol,
                                                    "driver" to data.driver,
                                                    "transporter_id" to data.transporter_id,
                                                    "mill_id" to data.mill_id,
                                                    "info_app" to data.creator_info,
                                                    "no_espb" to data.noESPB
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
                                        Log.d(
                                            "UploadCheck",
                                            "🎉 ZIP creation done! Proceeding to the next step."
                                        )
                                        handleUpload(selectedItems)
                                    } else {
                                        Log.e(
                                            "UploadCheck",
                                            "⛔ ZIP creation failed! Skipping next step."
                                        )
                                        AlertDialogUtility.withSingleAction(
                                            this@ListHistoryWeighBridgeActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_no_data_for_upload_cmp),
                                            stringXML(R.string.al_no_data_for_upload_cmp_description),
                                            "success.json",
                                            R.color.greendarkerbutton
                                        ) { }
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

                                    val blokData = try {
                                        withContext(Dispatchers.IO) { // Database operation on IO thread
                                            weightBridgeViewModel.getBlokById(idBlokList)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                                        null
                                    }

                                    val deptAbbr = blokData?.firstOrNull()?.dept_abbr
                                        ?: "-"

                                    val deptPPRO = blokData?.firstOrNull()?.dept_ppro
                                        ?: 0

                                    val divisiAbbr = blokData?.firstOrNull()?.divisi_abbr ?: "-"

                                    val divisiPPRO = blokData?.firstOrNull()?.dept_ppro
                                        ?: 0

                                    WBData(
                                        //data untuk upload staging
                                        id = item.id,
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
                                        //untuk table
                                        estate = deptAbbr.ifEmpty { "-" },
                                        afdeling = divisiAbbr.ifEmpty { "-" },
                                        datetime = item.created_at.ifEmpty { "-" },
                                        status_cmp = item.status_upload_cmp_wb,
                                        status_ppro = item.status_upload_ppro_wb
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
        val headers = listOf("e-SPB", "ESTATE", "AFDELING", "TGL PROSES", "STATUS UPLOAD")
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
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

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
        val factory4 = UploadCMPViewModel.UploadCMPViewModelFactory(application)
        uploadCMPViewModel = ViewModelProvider(this, factory4)[UploadCMPViewModel::class.java]
    }

    private fun setupHeader() {
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.GONE

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()

        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()


    }
}