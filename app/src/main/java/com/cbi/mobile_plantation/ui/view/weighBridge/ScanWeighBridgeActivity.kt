package com.cbi.mobile_plantation.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.weighBridge.wbQRData
import com.cbi.mobile_plantation.data.repository.WeighBridgeRepository
import com.cbi.mobile_plantation.ui.view.HomePageActivity

import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNREACHABLE_CODE", "UNUSED_CHANGED_VALUE")
class ScanWeighBridgeActivity : AppCompatActivity() {
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private var prefManager: PrefManager? = null

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var isScanning = true

    private lateinit var loadingDialog: LoadingDialog

    var globalBlokJjg: String = ""
    var globalBlokId: String = ""
    var globalTotalJjg: String = ""
    var globalCreatedById: Int? = null
    var globalNopol: String = ""
    var globalDriver: String = ""
    var globalTransporterId: Int? = null
    var globalPemuatId: String = ""
    var globalMillId: Int? = null
    var globalTph0: String = ""
    var globalTph1: String = ""
    var globalCreatorInfo: String = ""
    var globalCreatedAt : String = ""
    var globalUpdateInfoSP: String = ""
    var globalNoESPB: String = ""
    var globalDeptPPRO: Int = 0
    var globalDivisiPPRO: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_scan_weigh_bridge)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupBottomSheet()
        setupQRScanner()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    ::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing -> {
                        bottomSheetDialog.dismiss()
                    }

                    barcodeView.visibility == View.VISIBLE -> {
                        pauseScanner()
                        barcodeView.barcodeView?.cameraInstance?.close()
                        isEnabled = false // Disable this callback momentarily
                        onBackPressedDispatcher.onBackPressed() // Call the system back event
                    }

                    else -> {
                        isEnabled = false // Ensure default behavior is triggered
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun setupQRScanner() {
        barcodeView = findViewById(R.id.barcode_scanner_krani_timbang)
        setMaxBrightness(this@ScanWeighBridgeActivity, true)
        barcodeView.findViewById<TextView>(com.google.zxing.client.android.R.id.zxing_status_view)?.visibility =
            View.VISIBLE
        barcodeView.findViewById<TextView>(com.google.zxing.client.android.R.id.zxing_status_view)?.text =
            "Letakkan QR ke dalam kotak scan!"

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { qrCodeValue ->
                    if (isScanning) {
                        isScanning = false

                        pauseScanner()
                        processQRResult(qrCodeValue)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })

        resumeScanner()
    }

    private fun setupBottomSheet() {
        bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_wb, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.behavior.peekHeight =
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        bottomSheetDialog.behavior.isDraggable = true  // Allow dragging


//        bottomSheetDialog.setCanceledOnTouchOutside(false)
//        bottomSheetDialog.setCancelable(false)
//        bottomSheetDialog.behavior.apply {
//            isDraggable = false
//        }
        bottomSheetView.findViewById<Button>(R.id.btnSaveUploadeSPB)?.setOnClickListener {
            isScanning = false
            pauseScanner()

            AlertDialogUtility.withTwoActions(
                this,
                "Simpan & Upload Data",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.al_submit_upload_data_espb_by_krani_timbang),
                "warning.json"
            ) {
                lifecycleScope.launch(Dispatchers.Main) {

                    loadingDialog.show()
                    loadingDialog.setMessage("Sedang menyimpan e-SPB ke local database", true)
                    try {
                        val result = withContext(Dispatchers.IO) {
                            weightBridgeViewModel.saveDataLocalKraniTimbangESPB(
                                blok_jjg = globalBlokJjg,
                                created_by_id = globalCreatedById ?: 0,
                                created_at = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date()),
                                nopol = globalNopol,
                                driver = globalDriver,
                                transporter_id = globalTransporterId ?: 0,
                                pemuat_id = globalPemuatId,
                                mill_id = globalMillId!!,
                                archive = 0,
                                tph0 = globalTph0,
                                tph1 = globalTph1,
                                update_info_sp = "", // Changed from update_info
                                uploaded_by_id_wb = 0, // New field for WB
                                uploaded_at_wb = "",   // New field for WB
                                status_upload_cmp_wb = 0, // New field for WB
                                status_upload_ppro_wb = 0, // New field for WB
                                creator_info = globalCreatorInfo,
                                uploader_info_wb = "", // New field for WB
                                noESPB = globalNoESPB,
                                scan_status = 1, // Default scan_status
                            )

                        }

                        when (result) {
                            is WeighBridgeRepository.SaveResultESPBKrani.Success -> {

                                val savedItemId = result.id

                                var number = 0;
                                //untuk data PPRO Staging
                                val itemToUpload = mapOf(
                                    "num" to number++,
                                    "id" to savedItemId,
                                    "endpoint" to "PPRO",
                                    "uploader_info" to globalCreatorInfo,
                                    "uploaded_at" to SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    "uploaded_by_id" to (globalCreatedById ?: 0),
                                    "dept_ppro" to globalDeptPPRO,
                                    "divisi_ppro" to globalDivisiPPRO,
                                    "commodity" to "0",
                                    "blok_jjg" to globalBlokJjg,
                                    "nopol" to globalNopol,
                                    "driver" to globalDriver,
                                    "pemuat_id" to globalPemuatId.toString(),
                                    "transporter_id" to (globalTransporterId ?: 0).toString(),
                                    "mill_id" to globalMillId.toString(),
                                    "created_by_id" to (globalCreatedById ?: 0).toString(),
                                    "created_at" to SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    "no_espb" to globalNoESPB
                                )


                                lifecycleScope.launch {

                                    val zipDeferred = CompletableDeferred<Pair<Boolean, String?>>()
                                    var zipFilePath: String? = null
                                    loadingDialog.setMessage(
                                        "Sedang membuat file .zip untuk upload",
                                        true
                                    )
                                    //untuk data CMP

                                    var number =
                                        0
                                    val espbData = mapOf(
                                        "num" to number++,
                                        "id" to savedItemId,
                                        "blok_id" to globalBlokId,
                                        "blok_jjg" to globalBlokJjg,
                                        "jjg" to globalTotalJjg,
                                        "created_by_id" to (globalCreatedById ?: 0),
                                        "created_at" to globalCreatedAt,
                                        "nopol" to globalNopol,
                                        "driver" to globalDriver,
                                        "transporter_id" to (globalTransporterId ?: 0),
                                        "mill_id" to globalMillId,
                                        "creator_info" to globalCreatorInfo,
                                        "no_espb" to globalNoESPB,
                                        "tph0" to globalTph0,
                                        "tph1" to globalTph1,
                                        "update_info_sp" to globalUpdateInfoSP,
                                        "app_version" to AppUtils.getDeviceInfo(this@ScanWeighBridgeActivity)
                                            .toString(),
                                        "jabatan" to prefManager!!.jabatanUserLogin
                                    )


                                    AppLogger.d(espbData.toString())

                                    val uploadDataList =
                                        mutableListOf<Pair<String, List<Map<String, Any>>>>()

                                    val espbDataAsAny = espbData as Map<String, Any>

                                    uploadDataList.add(
                                        Pair(
                                            AppUtils.DatabaseTables.ESPB,
                                            listOf(espbDataAsAny)
                                        )
                                    )

                                    AppUtils.createAndSaveZipUploadCMP(
                                        this@ScanWeighBridgeActivity,
                                        uploadDataList,
                                        (globalCreatedById ?: 0).toString()
                                    ) { success, fileName, fullPath ->
                                        if (success) {
                                            zipFilePath = fullPath

                                            zipDeferred.complete(Pair(true, fullPath))
                                        } else {
                                            loadingDialog.addStatusMessage(
                                                "Gagal membuat file ZIP",
                                                LoadingDialog.StatusType.ERROR
                                            )
                                            zipDeferred.complete(Pair(false, null))
                                        }
                                    }

                                    val (zipSuccess, zipPath) = zipDeferred.await()

                                    // Initialize a variable for the CMP item outside the if-else
                                    var cmpItem: Map<String, Any>? = null


                                    if (zipSuccess) {

                                        weightBridgeViewModel.updateDataIsZippedESPB(
                                            listOf(savedItemId),
                                            1
                                        )

                                        // Create the CMP upload item with the ZIP file path
                                        cmpItem = mapOf(
                                            "num" to number++,
                                            "id" to savedItemId,
                                            "endpoint" to "CMP",
                                            "uploader_info" to globalCreatorInfo,
                                            "uploaded_at" to SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()
                                            ).format(Date()),
                                            "uploaded_by_id" to (globalCreatedById ?: 0),
                                            "file" to (zipPath
                                                ?: "") // Use empty string if zipPath is null
                                        )

                                    } else {


                                        cmpItem = mapOf(
                                            "num" to number++,
                                            "id" to savedItemId,
                                            "endpoint" to "CMP",
                                            "uploader_info" to globalCreatorInfo,
                                            "uploaded_at" to SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()
                                            ).format(Date()),
                                            "uploaded_by_id" to (globalCreatedById ?: 0),
                                            "file" to "" // Empty file path indicates it should be skipped
                                        )

                                    }

                                    val itemsToUpload = listOf(itemToUpload, cmpItem)
                                    val globalIdEspb = listOf(savedItemId)


                                    loadingDialog.setMessage(
                                        "Sedang mengupload data ke server",
                                        true
                                    )
                                    // Start the upload with both items
                                    weightBridgeViewModel.uploadESPBKraniTimbang(
                                        itemsToUpload,
                                        globalIdEspb
                                    )

                                    val processedEndpoints = mutableSetOf<String>()

                                    // Observe upload progress
                                    weightBridgeViewModel.uploadProgress.observe(this@ScanWeighBridgeActivity) { progressMap ->
                                        AppLogger.d("Upload progress: $progressMap")
                                    }

                                    // Observe upload status with endpoint info
                                    weightBridgeViewModel.uploadStatusEndpointMap.observe(this@ScanWeighBridgeActivity) { statusEndpointMap ->

                                        statusEndpointMap.forEach { (id, info) ->
                                            val endpointKey = info.endpoint

                                            // Only add a message if we haven't processed this endpoint for this ID yet
                                            if (!processedEndpoints.contains(endpointKey) && info.status == "Success") {
                                                processedEndpoints.add(endpointKey)
                                                loadingDialog.addStatusMessage(
                                                    "${info.endpoint} berhasil diupload",
                                                    LoadingDialog.StatusType.SUCCESS
                                                )
                                            } else if (!processedEndpoints.contains(endpointKey) && info.status == "Failed") {
                                                processedEndpoints.add(endpointKey)
                                                loadingDialog.addStatusMessage(
                                                    "${info.endpoint} gagal diupload",
                                                    LoadingDialog.StatusType.ERROR
                                                )
                                            }
                                        }

                                        val allComplete =
                                            statusEndpointMap.values.all { it.status == "Success" || it.status == "Failed" }
                                        if (allComplete) {
                                            loadingDialog.setMessage("Semua data telah diupload")
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                val allSuccessful =
                                                    statusEndpointMap.values.all { it.status == "Success" }
                                                if (allSuccessful) {
                                                    AlertDialogUtility.withSingleAction(
                                                        this@ScanWeighBridgeActivity,
                                                        stringXML(R.string.al_back),
                                                        stringXML(R.string.al_success_save_local),
                                                        stringXML(R.string.al_description_success_save_local_and_espb_krani_timbang) +
                                                                "\nBerhasil mengupload data ke server.",
                                                        "success.json",
                                                        R.color.greendarkerbutton
                                                    ) {
                                                        val intent = Intent(
                                                            this@ScanWeighBridgeActivity,
                                                            HomePageActivity::class.java
                                                        )
                                                        intent.flags =
                                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                } else {
                                                    val failedCount =
                                                        statusEndpointMap.values.count { it.status == "Failed" }

                                                    AlertDialogUtility.withSingleAction(
                                                        this@ScanWeighBridgeActivity,
                                                        stringXML(R.string.al_back),
                                                        stringXML(R.string.al_success_save_local),
                                                        stringXML(R.string.al_description_success_save_local_and_espb_krani_timbang) +
                                                                "\nData tersimpan lokal tetapi ada $failedCount data yang gagal upload!.Lakukan upload ulang pada menu Rekap Scan Timbangan Mill",
                                                        "warning.json",
                                                        R.color.orangeButton
                                                    ) {
                                                        val intent = Intent(
                                                            this@ScanWeighBridgeActivity,
                                                            HomePageActivity::class.java
                                                        )
                                                        intent.flags =
                                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                }

                                                // Finally dismiss the loading dialog
                                                loadingDialog.dismiss()

                                            }, 3000)
                                        }
                                    }

                                    // Observe errors to add detailed messages
                                    weightBridgeViewModel.uploadErrorMap.observe(this@ScanWeighBridgeActivity) { errorMap ->
                                        if (errorMap.isNotEmpty()) {
                                            errorMap.forEach { (id, errorMsg) ->
                                                // Find the corresponding endpoint
                                                val endpoint =
                                                    weightBridgeViewModel.uploadStatusEndpointMap.value?.get(
                                                        id
                                                    )?.endpoint ?: "Unknown"

                                                // Add error message to loading dialog
                                                val endpointErrorKey = "error_${endpoint}"
                                                if (!processedEndpoints.contains(endpointErrorKey)) {
                                                    processedEndpoints.add(endpointErrorKey)
                                                    loadingDialog.addStatusMessage(
                                                        "$endpoint: ${errorMsg.take(50)}${if (errorMsg.length > 50) "..." else ""}",
                                                        LoadingDialog.StatusType.ERROR,
                                                        showIcon = false  // Don't show the icon
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is WeighBridgeRepository.SaveResultESPBKrani.AlreadyExists -> {
                                AlertDialogUtility.withSingleAction(
                                    this@ScanWeighBridgeActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_save_local),
                                    "Data dengan nomor e-SPB ${globalNoESPB} sudah tersimpan sebelumnya.",
                                    "warning.json",
                                    R.color.orangeButton
                                ) {


                                }
                            }

                            is WeighBridgeRepository.SaveResultESPBKrani.Error -> {
                                AlertDialogUtility.withSingleAction(
                                    this@ScanWeighBridgeActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_save_local),
                                    "${stringXML(R.string.al_failed_save_local_krani_timbang)} : ${result.exception.message}",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {}
                            }
                        }

                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        AppLogger.d("Unexpected error: ${e.message}")

                        AlertDialogUtility.withSingleAction(
                            this@ScanWeighBridgeActivity,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_failed_save_local),
                            "${stringXML(R.string.al_failed_save_local_krani_timbang)} : ${e.message}",
                            "warning.json",
                            R.color.colorRedDark
                        ) {}
                    }
                }

            }
        }

        bottomSheetView.findViewById<Button>(R.id.btnScanAgain)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setOnDismissListener {
            if (barcodeView.visibility == View.VISIBLE && !isScanning) {
                resumeScanner()
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun showBottomSheetWithData(
        parsedData: wbQRData?,
        distinctDeptAbbr: String,
        distinctDivisiAbbr: String,
        formattedBlokList: String,
        pemuat: String,
        totalJjgSum: Int,
        millAbbr: String,
        transporterName: String,
        createAtFormatted: String,
        hasError: Boolean = false,
        errorMessage: String? = null
    ) {
        val bottomSheetView = bottomSheetDialog.findViewById<View>(R.id.bottomSheetContent)
        bottomSheetView?.let {
            val errorCard = it.findViewById<LinearLayout>(R.id.errorCard)
            val dataContainer = it.findViewById<LinearLayout>(R.id.dataContainer)
            val errorText = it.findViewById<TextView>(R.id.errorText)
            val btnProcess = it.findViewById<Button>(R.id.btnSaveUploadeSPB)



            if (hasError) {
                errorCard.visibility = View.VISIBLE
                dataContainer.visibility = View.GONE
                errorText.text = errorMessage
                btnProcess.visibility = View.GONE
            } else {
                errorCard.visibility = View.GONE
                dataContainer.visibility = View.VISIBLE
                btnProcess.visibility = View.VISIBLE

                val infoItems = listOf(
                    InfoType.ESPB to (parsedData?.espb?.noEspb ?: "-"),
                    InfoType.DATE to createAtFormatted,
                    InfoType.ESTATE to distinctDeptAbbr,
                    InfoType.AFDELING to distinctDivisiAbbr,
                    InfoType.NOPOL to (parsedData?.espb?.nopol ?: "-"),
                    InfoType.BLOK to formattedBlokList,
                    InfoType.TOTAL_JJG to "$totalJjgSum Jjg",
                    InfoType.PEMUAT to pemuat,
                    InfoType.DRIVER to (parsedData?.espb?.driver ?: "-"),
                    InfoType.MILL to millAbbr,
                    InfoType.TRANSPORTER to transporterName
                )

                infoItems.forEach { (type, value) ->
                    val itemView = it.findViewById<View>(type.id)
                    setInfoItemValues(itemView, type.label, value)
                }
            }

        }

        bottomSheetDialog.show()
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {

        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        view.findViewById<TextView>(R.id.tvValue)?.text = when (view.id) {
            R.id.infoBlok -> value
            else -> ": $value"
        }
    }

    private fun pauseScanner() {
        barcodeView.pause()
        isScanning = false
    }

    private fun resumeScanner() {
        try {
            barcodeView.resume()
            isScanning = true
        } catch (e: Exception) {
            AppLogger.e("Error resuming scanner: ${e.message}")
            // Try to recover by reinitializing
            lifecycleScope.launch {
                delay(100)
                try {
                    barcodeView.resume()
                    isScanning = true
                } catch (e: Exception) {
                    AppLogger.e("Failed to recover scanner: ${e.message}")
                }
            }
        }
    }

    enum class InfoType(val id: Int, val label: String) {
        ESPB(R.id.noEspbTitleScanWB, "e-SPB"),
        DATE(R.id.infoCreatedAt, "Date"),
        ESTATE(R.id.infoEstate, "Estate"),
        AFDELING(R.id.infoAfdeling, "Afdeling"),
        NOPOL(R.id.infoNoPol, "No. Polisi"),
        BLOK(R.id.infoBlok, "Blok"),
        TOTAL_JJG(R.id.infoTotalJjg, "Total Janjang"),
        PEMUAT(R.id.infoPemuat, "Pemuat"),
        DRIVER(R.id.infoNoDriver, "Driver"),
        MILL(R.id.infoMill, "Mill"),
        TRANSPORTER(R.id.infoTransporter, "Transporter")
    }

    private fun processQRResult(qrResult: String) {

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data")
                delay(500)
            }
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)
                    val parsedData = Gson().fromJson(jsonStr, wbQRData::class.java)

                    AppLogger.d("Parsed Data: $parsedData")

                    val blokJjgList = parsedData?.espb?.blokJjg?.split(";")?.mapNotNull {
                        it.split(",").takeIf { it.size == 2 }?.let { (id, jjg) ->
                            id.toIntOrNull()?.let { it to jjg.toIntOrNull() }
                        }
                    } ?: emptyList()

                    val idBlokList = blokJjgList.map { it.first }
                    val concatenatedIds = idBlokList.joinToString(",")
                    val pemuatList = parsedData?.espb?.pemuat_id?.split(",")?.map { it.trim() }
                        ?.filter { it.isNotEmpty() } ?: emptyList()

                    val pemuatDeferred = async {
                        try {
                            weightBridgeViewModel.getPemuatByIdList(pemuatList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                            null
                        }
                    }

                    val pemuatData = pemuatDeferred.await()
                    val pemuatNama = pemuatData?.mapNotNull { it.nama }?.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") ?: "-"

                    if (pemuatNama == "-") {
                        AppLogger.e("Pemuat Data is empty or failed to fetch.")
                    }

                    val blokData = try {
                        AppLogger.d(idBlokList.toString())
                        weightBridgeViewModel.getBlokById(idBlokList)
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                        null
                    } ?: emptyList()

                    val deptAbbr = blokData.firstOrNull()?.dept_abbr ?: "-"
                    val divisiAbbr = blokData.firstOrNull()?.divisi_abbr ?: "-"

                    globalDeptPPRO = blokData.firstOrNull()?.dept_ppro!!
                    globalDivisiPPRO = blokData.firstOrNull()?.divisi_ppro!!

                    var totalJjgSum = 0

                    val formattedBlokList = blokJjgList.mapNotNull { (idBlok, totalJjg) ->
                        val blokKode = blokData.find { it.blok == idBlok }?.blok_kode
                        if (blokKode != null && totalJjg != null) {
                            totalJjgSum += totalJjg
                            "• $blokKode ($totalJjg jjg)"
                        } else null
                    }.joinToString("\n").takeIf { it.isNotBlank() } ?: "-"

                    val millId = parsedData?.espb?.millId ?: 0
                    val transporterId = parsedData?.espb?.transporter ?: 0
                    val createdAt = parsedData?.espb?.createdAt ?: "-"
                    val createAtFormatted = formatToIndonesianDate(createdAt)

                    val millDataDeferred = async {
                        try {
                            weightBridgeViewModel.getMillName(millId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Mill Data: ${e.message}")
                            null
                        }
                    }

                    val millData = millDataDeferred.await() ?: emptyList()
                    val millAbbr = millData.firstOrNull()?.let { "${it.abbr} (${it.nama})" } ?: "-"

                    val transporterDeferred = async {
                        try {
                            weightBridgeViewModel.getTransporterName(transporterId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Transporter Data: ${e.message}")
                            null
                        }
                    }

                    val transporterData = transporterDeferred.await() ?: emptyList()
                    val transporterName = transporterData.firstOrNull()?.nama ?: "-"
                    val totalJjg = blokJjgList.mapNotNull { it.second }.sum()

                    globalBlokId = concatenatedIds
                    globalTotalJjg = totalJjg.toString()
                    globalBlokJjg = parsedData?.espb?.blokJjg ?: "-"
                    globalCreatedById = prefManager!!.idUserLogin
                    globalNopol = parsedData?.espb?.nopol ?: "-"
                    globalDriver = parsedData?.espb?.driver ?: "-"
                    globalTransporterId = transporterId
                    globalPemuatId = parsedData?.espb?.pemuat_id ?: "-"
                    globalMillId = millId
                    globalTph0 = parsedData?.tph0 ?: "-"
                    globalTph1 = parsedData?.tph1 ?: "-"
                    globalCreatedAt = parsedData?.espb?.createdAt.toString() ?: "-"
                    globalCreatorInfo = parsedData?.espb?.creatorInfo?.toString() ?: "-"
                    globalNoESPB = parsedData?.espb?.noEspb ?: "-"
                    globalUpdateInfoSP = parsedData?.espb?.update_info_sp ?: "-"

                    withContext(Dispatchers.Main) {
                        showBottomSheetWithData(
                            parsedData = parsedData,
                            distinctDeptAbbr = deptAbbr,
                            distinctDivisiAbbr = divisiAbbr,
                            formattedBlokList = formattedBlokList,
                            pemuat = pemuatNama,
                            totalJjgSum = totalJjgSum,
                            millAbbr = millAbbr,
                            transporterName = transporterName,
                            createAtFormatted = createAtFormatted,
                            hasError = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result: ${e.message}")
                withContext(Dispatchers.Main) {
                    showBottomSheetWithData(
                        parsedData = null,
                        distinctDeptAbbr = "-",
                        distinctDivisiAbbr = "-",
                        formattedBlokList = "-",
                        pemuat = "-",
                        totalJjgSum = 0,
                        millAbbr = "-",
                        transporterName = "-",
                        createAtFormatted = "-",
                        hasError = true,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                }
            }

        }
    }

    private fun initViewModel() {
        val factory = WeighBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeighBridgeViewModel::class.java]
    }


    override fun onResume() {
        super.onResume()
        if (barcodeView.visibility == View.VISIBLE) {
            setMaxBrightness(this@ScanWeighBridgeActivity, true)
            isScanning = false
            lifecycleScope.launch {
                delay(100)
                resumeScanner()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseScanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
        barcodeView.barcodeView?.cameraInstance?.close() // Release camera
        setMaxBrightness(this@ScanWeighBridgeActivity, false)
    }

//    @Deprecated("Use onBackPressedDispatcher instead")
//    @SuppressLint("MissingSuperCall")
//    override fun onBackPressed() {
//        when {
//            ::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing -> {
//                bottomSheetDialog.dismiss()
//            }
//
//            barcodeView.visibility == View.VISIBLE -> {
//                pauseScanner()
//                barcodeView.barcodeView?.cameraInstance?.close()
//                super.onBackPressed()
//            }
//
//            else -> super.onBackPressed()
//        }
//    }
}