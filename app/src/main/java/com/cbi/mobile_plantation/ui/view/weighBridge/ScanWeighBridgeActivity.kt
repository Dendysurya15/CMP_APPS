package com.cbi.mobile_plantation.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.weighBridge.wbQRData
import com.cbi.mobile_plantation.data.repository.WeighBridgeRepository
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel

import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import org.json.JSONObject
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
    private lateinit var datasetViewModel: DatasetViewModel
    var globalBlokJjg: String = ""
    var globalBlokPPROJjg: String = ""
    var globalRegional: String = ""
    var globalWilayah: String = ""
    var globalCompany: Int = 0
    var globalDept: Int = 0
    var globalDivisi: Int = 0
    var globalBlokId: String = ""
    var globalTotalJjg: String = ""
    var globalCreatedById: Int? = null
    var globalNopol: String = ""
    var globalDriver: String = ""
    var globalTransporterId: Int? = null
    var globalPemuatId: String = ""
    var globalKemandoranId: String = ""
    var globalPemuatNik: String = ""
    var globalMillId: Int? = null
    var globalTph0: String = ""
    var globalTph1: String = ""
    var globalCreatorInfo: String = ""
    var globalCreatedAt: String = ""
    var globalUpdateInfoSP: String = ""
    var globalIpMill: String = ""
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
        setMaxBrightness(this@ScanWeighBridgeActivity, false)
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

        val bottomSheetView =
            layoutInflater.inflate(
                R.layout.layout_bottom_sheet_scan_wb,
                null
            )
        bottomSheetDialog.setContentView(bottomSheetView)


        val btnSaveUploadESPB = bottomSheetView.findViewById<Button>(R.id.btnSaveUploadeSPB)

        btnSaveUploadESPB.setOnClickListener {
            btnSaveUploadESPB.isEnabled = false
            isScanning = false
            pauseScanner()

            // Always allow saving to local database regardless of network connection
            AlertDialogUtility.withTwoActions(
                this,
                "Simpan Data",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.al_submit_upload_data_espb_by_krani_timbang),
                "warning.json",
                function = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage(
                            "Sedang menyimpan e-SPB ke local database",
                            true
                        )
                        try {
                            val result = withContext(Dispatchers.IO) {
                                weightBridgeViewModel.saveDataLocalKraniTimbangESPB(
                                    blok_jjg = globalBlokJjg,
                                    created_by_id = globalCreatedById ?: 0,
                                    created_at = globalCreatedAt,
                                    nopol = globalNopol,
                                    driver = globalDriver,
                                    transporter_id = globalTransporterId ?: 0,
                                    pemuat_id = globalPemuatId,
                                    kemandoran_id = globalKemandoranId,
                                    pemuat_nik = globalPemuatNik,
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
                                    scan_status = 1,
                                    date_scan = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) // Current datetime
                                )
                            }

                            when (result) {
                                is WeighBridgeRepository.SaveResultESPBKrani.Success -> {
                                    loadingDialog.setMessage(
                                        "Berhasil simpan data e-SPB ke local",
                                    )

                                    playSound(R.raw.berhasil_simpan)
                                    val savedItemId = result.id

                                    // Check if network is available for upload attempt
                                    if (AppUtils.isNetworkAvailable(this@ScanWeighBridgeActivity)) {
                                        // Try to upload with network connection



                                        var number = 0
                                        // Data for PPRO Staging
                                        val itemToUpload = mapOf(
                                            "num" to number++,
                                            "ip" to globalIpMill,
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
                                            "commodity" to "2",
                                            "blok_jjg" to globalBlokPPROJjg,
                                            "nopol" to globalNopol,
                                            "driver" to globalDriver,
                                            "pemuat_id" to globalPemuatId.toString(),
                                            "transporter_id" to (globalTransporterId
                                                ?: 0).toString(),
                                            "mill_id" to globalMillId.toString(),
                                            "created_by_id" to (globalCreatedById ?: 0).toString(),
                                            "created_at" to globalCreatedAt,
                                            "no_espb" to globalNoESPB
                                        )

                                        lifecycleScope.launch {
                                            val zipDeferred = CompletableDeferred<Pair<Boolean, String?>>()
                                            var zipFilePath: String? = null
                                            loadingDialog.setMessage(
                                                "Sedang membuat file .zip untuk upload",
                                                true
                                            )
// For CMP data
                                            var number = 0

                                            val espbData = mapOf(
                                                "num" to number++,
                                                "ip" to globalIpMill,
                                                "id" to savedItemId,
                                                "regional" to globalRegional,
                                                "wilayah" to globalWilayah,
                                                "company" to globalCompany,
                                                "dept" to globalDept,
                                                "divisi" to globalDivisi,
                                                "blok_id" to globalBlokId,
                                                "blok_jjg" to globalBlokJjg,
                                                "jjg" to globalTotalJjg,
                                                "created_by_id" to (globalCreatedById ?: 0),
                                                "created_at" to globalCreatedAt,
                                                "pemuat_id" to globalPemuatId,
                                                "kemandoran_id" to globalKemandoranId,
                                                "pemuat_nik" to globalPemuatNik,
                                                "nopol" to globalNopol,
                                                "driver" to globalDriver,
                                                "updated_nama" to prefManager!!.nameUserLogin.toString(),
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

                                            val espbDataList = listOf(espbData)

                                            // Wrap the data in a structure as requested
                                            val wrappedEspbData = mapOf(
                                                AppUtils.DatabaseTables.ESPB to espbDataList
                                            )

                                            // Convert the wrapped data to JSON
                                            val espbJson = Gson().toJson(wrappedEspbData)

                                            val uploadDataList = mutableListOf<Pair<String, List<Map<String, Any>>>>()
                                            val espbDataAsAny = espbData as Map<String, Any>
                                            uploadDataList.add(
                                                Pair(
                                                    AppUtils.DatabaseTables.ESPB,
                                                    listOf(espbDataAsAny)
                                                )
                                            )

                                            AppUtils.createAndSaveZipUploadCMPSingle(
                                                this@ScanWeighBridgeActivity,
                                                uploadDataList,
                                                (globalCreatedById ?: 0).toString()
                                            ) { success, fileName, fullPath, zipFile ->
                                                if (success) {
                                                    zipFilePath = fullPath
                                                    AppLogger.d("sukses membuat zip $fileName")
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
                                            var cmpItem: Map<String, Any>? = null

                                            if (zipSuccess) {
                                                weightBridgeViewModel.updateDataIsZippedESPB(
                                                    listOf(savedItemId),
                                                    1
                                                )
                                                cmpItem = mapOf(
                                                    "num" to number++,
                                                    "ip" to globalIpMill,
                                                    "id" to savedItemId,
                                                    "endpoint" to "CMP",
                                                    "uploader_info" to globalCreatorInfo,
                                                    "uploaded_at" to SimpleDateFormat(
                                                        "yyyy-MM-dd HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()),
                                                    "uploaded_by_id" to (globalCreatedById ?: 0),
                                                    "data" to espbJson  // Changed from "file" to "data" and using the JSON string directly
                                                )
                                            } else {
                                                cmpItem = mapOf(
                                                    "num" to number++,
                                                    "ip" to globalIpMill,
                                                    "id" to savedItemId,
                                                    "endpoint" to "CMP",
                                                    "uploader_info" to globalCreatorInfo,
                                                    "uploaded_at" to SimpleDateFormat(
                                                        "yyyy-MM-dd HH:mm:ss",
                                                        Locale.getDefault()
                                                    ).format(Date()),
                                                    "uploaded_by_id" to (globalCreatedById ?: 0),
                                                    "data" to espbJson  // Changed from "file" to "data" and using the JSON string directly
                                                )
                                            }

                                            val itemsToUpload = listOf(itemToUpload, cmpItem)
                                            val globalIdEspb = listOf(savedItemId)

                                            loadingDialog.setMessage(
                                                "Sedang mengupload data ke server, harap tunggu",
                                                true
                                            )
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
                                            weightBridgeViewModel.uploadStatusEndpointMap.observe(
                                                this@ScanWeighBridgeActivity
                                            ) { statusEndpointMap ->
                                                statusEndpointMap.forEach { (id, info) ->
                                                    val endpointKey = info.endpoint

                                                    // Only add a message if we haven't processed this endpoint for this ID yet
                                                    if (!processedEndpoints.contains(endpointKey) && info.status == "Success") {
                                                        processedEndpoints.add(endpointKey)
                                                        loadingDialog.addStatusMessage(
                                                            "${info.endpoint} berhasil diupload",
                                                            LoadingDialog.StatusType.SUCCESS
                                                        )
                                                    } else if (!processedEndpoints.contains(
                                                            endpointKey
                                                        ) && info.status == "Failed"
                                                    ) {
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
                                                        if (!processedEndpoints.contains(
                                                                endpointErrorKey
                                                            )
                                                        ) {
                                                            processedEndpoints.add(endpointErrorKey)
                                                            loadingDialog.addStatusMessage(
                                                                "$endpoint: ${errorMsg.take(1000)}${if (errorMsg.length > 1000) "..." else ""}",
                                                                LoadingDialog.StatusType.ERROR,
                                                                showIcon = false  // Don't show the icon
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // No network available, just show success for local save
                                        loadingDialog.dismiss()
                                        AlertDialogUtility.withSingleAction(
                                            this@ScanWeighBridgeActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_success_save_local),
                                            stringXML(R.string.al_description_success_save_local_and_espb_krani_timbang) +
                                                    "\nData tersimpan di lokal namun tidak dapat diupload karena tidak ada koneksi. " +
                                                    "Lakukan upload ulang pada menu Rekap Scan Timbangan Mill saat koneksi tersedia.",
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
                                }

                                is WeighBridgeRepository.SaveResultESPBKrani.AlreadyExists -> {
                                    loadingDialog.dismiss()
                                    AlertDialogUtility.withSingleAction(
                                        this@ScanWeighBridgeActivity,
                                        stringXML(R.string.al_back),
                                        stringXML(R.string.al_failed_save_local),
                                        "Data dengan nomor e-SPB ${globalNoESPB} sudah tersimpan sebelumnya.",
                                        "warning.json",
                                        R.color.orangeButton
                                    ) {}
                                }

                                is WeighBridgeRepository.SaveResultESPBKrani.Error -> {
                                    loadingDialog.dismiss()
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
                    btnSaveUploadESPB.isEnabled = true
                },
                cancelFunction = {
                    btnSaveUploadESPB.isEnabled = true
                }
            )
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


        bottomSheetDialog?.let {
            val titleDialogDetailTable = it.findViewById<TextView>(R.id.titleDialogDetailTable)
            val dashedline = it.findViewById<View>(R.id.dashedLine)
            val errorCard = it.findViewById<LinearLayout>(R.id.errorCard)
            val dataContainer = it.findViewById<LinearLayout>(R.id.dataContainer)
            val errorText = it.findViewById<TextView>(R.id.errorText)
            val btnProcess = it.findViewById<Button>(R.id.btnSaveUploadeSPB)

            if (hasError) {
                titleDialogDetailTable!!.text = "Terjadi Kesalahan Scan QR!"
                titleDialogDetailTable!!.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorRedDark
                    )
                )
                errorCard!!.visibility = View.VISIBLE
                dataContainer!!.visibility = View.GONE
                errorText!!.text = errorMessage
                btnProcess!!.visibility = View.GONE
                val maxHeight = (resources.displayMetrics.heightPixels * 0.7).toInt()

                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bottomSheet ->
                        val behavior = BottomSheetBehavior.from(bottomSheet)

                        behavior.apply {
                            this.peekHeight = maxHeight
                            this.state = BottomSheetBehavior.STATE_EXPANDED
                            this.isFitToContents = true
                            this.isDraggable = false
                        }

                        bottomSheet.layoutParams?.height = maxHeight
                    }
            } else {
                val maxHeight = (resources.displayMetrics.heightPixels * 0.7).toInt()
                titleDialogDetailTable!!.text = "Konfirmasi Data e-SPB"
                titleDialogDetailTable!!.setTextColor(ContextCompat.getColor(this, R.color.black))
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bottomSheet ->
                        val behavior = BottomSheetBehavior.from(bottomSheet)

                        behavior.apply {
                            this.peekHeight = maxHeight
                            this.state = BottomSheetBehavior.STATE_EXPANDED
                            this.isFitToContents = true
                            this.isDraggable = false
                        }

                        bottomSheet.layoutParams?.height = maxHeight
                    }
                errorCard!!.visibility = View.GONE
                dataContainer!!.visibility = View.VISIBLE
                btnProcess!!.visibility = View.VISIBLE
                playSound(R.raw.berhasil_scan)

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
                    setInfoItemValues(itemView!!, type.label, value)
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
        DATE(R.id.infoCreatedAt, "Tanggal Buat"),
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
                    AppLogger.d("Raw JSON: $jsonStr")

                    // Try with manual JSONObject for debugging
                    val jsonObject = JSONObject(jsonStr)
                    AppLogger.d("JSON keys: ${jsonObject.keys().asSequence().toList()}")

                    // Then proceed with your Gson parsing
                    val parsedData = Gson().fromJson(jsonStr, wbQRData::class.java)
                    AppLogger.d("Parsed Data: $parsedData")

                    // Create modified parsed data with reconstructed dates if needed
                    var modifiedParsedData = parsedData

                    // Check if we have the new format with tgl field
                    if (parsedData?.tgl != null && !parsedData.tph1.isNullOrEmpty()) {
                        try {
                            // Get the date from tgl field
                            val date = parsedData.tgl["0"] ?: ""

                            if (date.isNotEmpty()) {
                                // Split tph1 entries
                                val tph1Entries = parsedData.tph1.split(";")
                                val reconstructedTph1 = tph1Entries.joinToString(";") { entry ->
                                    val parts = entry.split(",")
                                    if (parts.size >= 3) {
                                        // Original format: ID,0,TIME,VALUE1,VALUE2
                                        // New format: ID,DATE TIME,VALUE1,VALUE2
                                        val id = parts[0]
                                        val time = parts[2]
                                        val restValues = parts.subList(3, parts.size).joinToString(",")
                                        "$id,$date $time,$restValues"
                                    } else {
                                        entry
                                    }
                                }

                                // Create a "patched" version of the parsed data with the reconstructed tph1
                                modifiedParsedData =  wbQRData(
                                    espb = parsedData.espb,
                                    tph0 = parsedData.tph0,
                                    tph1 = reconstructedTph1,
                                    tgl = parsedData.tgl
                                )

                                AppLogger.d("Reconstructed tph1: $reconstructedTph1")
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error reconstructing tph1 dates: ${e.message}")
                            // Continue with original parsedData if reconstruction fails
                        }
                    }

                    val blokJjgList = modifiedParsedData?.espb?.blokJjg?.split(";")?.mapNotNull {
                        it.split(",").takeIf { it.size == 2 }?.let { (id, jjg) ->
                            id.toIntOrNull()?.let { it to jjg.toIntOrNull() }
                        }
                    } ?: emptyList()

                    val idBlokList = blokJjgList.map { it.first }

                    val tphData = withContext(Dispatchers.Main) {
                        val tphDeferred = CompletableDeferred<TPHNewModel?>()
                        val firstBlockId = idBlokList.firstOrNull()

                        // Fetch the TPH data if we have a block ID
                        firstBlockId?.let { blockId ->
                            weightBridgeViewModel.fetchTPHByBlockId(blockId)

                            // Set up a one-time observer for the LiveData (now on main thread)
                            weightBridgeViewModel.tphData.observe(this@ScanWeighBridgeActivity) { tphModel ->
                                // Remove the observer to prevent future callbacks
                                weightBridgeViewModel.tphData.removeObservers(this@ScanWeighBridgeActivity)
                                tphDeferred.complete(tphModel)
                            }
                        } ?: tphDeferred.complete(null) // Complete with null if no block ID

                        // Wait for the TPH data and return it
                        tphDeferred.await()
                    }
                    val concatenatedIds = idBlokList.joinToString(",")
                    val pemuatList = modifiedParsedData?.espb?.pemuat_id?.split(",")?.map { it.trim() }
                        ?.filter { it.isNotEmpty() } ?: emptyList()

                    AppLogger.d("pemuatList $pemuatList")

                    val pemuatDeferred = async {
                        try {
                            weightBridgeViewModel.getPemuatByIdList(pemuatList)
                        } catch (e: Exception) {
                            AppLogger.e("Gagal mendapatkan data pemuat")
                            null
                        }
                    }

                    val pemuatData = pemuatDeferred.await()

                    AppLogger.d(pemuatData.toString())
                    val pemuatNama = pemuatData?.mapNotNull {"${it.nik} (${it.nama})"}?.takeIf { it.isNotEmpty() }
                        ?.joinToString("\n  ") ?: "-"


                    AppLogger.d("pemuatNama $pemuatNama")

                    AppLogger.d(idBlokList.toString())
                    val blokData = try {
                        AppLogger.d(idBlokList.toString())
                        weightBridgeViewModel.getDataByIdInBlok(idBlokList)
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                        null
                    } ?: emptyList()

                    val blokIdToPproMap = blokData.associate { it.id to it.id_ppro }

                    val BlokPPROJjg = blokJjgList.mapNotNull { (id, jjg) ->
                        blokIdToPproMap[id]?.let { "$it,$jjg" }
                    }.joinToString(";")

                    val deptAbbr = blokData.firstOrNull()?.dept_abbr ?: "-"
                    val divisiAbbr = blokData.firstOrNull()?.divisi_abbr ?: "-"

                    try {
                        // Check if first item exists and has dept_ppro and divisi_ppro
                        val firstBlok = blokData.firstOrNull()
                            ?: throw Exception("Terjadi kesalahan. Mohon ulangi pemindaian dengan fokus kamera yang tepat.")

                        val deptPpro = firstBlok.dept_ppro
                            ?: throw Exception("Terjadi kesalahan. Mohon ulangi pemindaian dengan fokus kamera yang tepat.")

                        val divisiPpro = firstBlok.divisi_ppro
                            ?: throw Exception("Terjadi kesalahan. Mohon ulangi pemindaian dengan fokus kamera yang tepat.")

                        // Assign only if we didn't throw any exceptions
                        globalDeptPPRO = deptPpro
                        globalDivisiPPRO = divisiPpro
                    } catch (e: Exception) {
                        // Log and rethrow with your custom message
                        AppLogger.e(e.message ?: "Unknown error")
                        throw e  // This will be caught by your outer catch block
                    }

                    val formattedBlokList = blokJjgList.mapNotNull { (idBlok, totalJjg) ->
                        val blokKode = blokData.find { it.id == idBlok }?.kode
                        if (blokKode != null && totalJjg != null) {
                            "• $blokKode ($totalJjg jjg)"
                        } else null
                    }.joinToString("\n").takeIf { it.isNotBlank() } ?: "-"

                    val millId = modifiedParsedData?.espb?.millId ?: 0

                    val transporterId = modifiedParsedData?.espb?.transporter ?: 0
                    val createdAt = modifiedParsedData?.espb?.createdAt ?: "-"
                    val createAtFormatted = formatToIndonesianDate(createdAt)

                    val millDataDeferred = async {
                        try {
                            weightBridgeViewModel.getMillName(millId)
                        } catch (e: Exception) {
                            AppLogger.e("Gagal mendapatkan data mill")
                            null
                        }
                    }

                    val millData = millDataDeferred.await() ?: emptyList()
                    val millAbbr = millData.firstOrNull()?.let { "${it.abbr} (${it.nama})" } ?: "-"
                    val millIP = millData.firstOrNull()?.let { "${it.ip_address}" } ?: "-"


                    val transporterName = if (transporterId == 0) {
                        "Internal"
                    } else {
                        // Otherwise get transporter name from API
                        val transporterDeferred = async {
                            try {
                                weightBridgeViewModel.getTransporterName(transporterId)
                            } catch (e: Exception) {
                                AppLogger.e("Gagal mendapatkan data transporter")
                                null
                            }
                        }

                        val transporterData = transporterDeferred.await() ?: emptyList()
                        transporterData.firstOrNull()?.nama ?: "-"
                    }
                    val totalJjg = blokJjgList.mapNotNull { it.second }.sum()

                    // Get the string from pemuat_nik
                    val pemuatNikString = modifiedParsedData?.espb?.pemuat_nik.toString()

// Simple string extraction to get NIK values
                    val nikList = mutableListOf<String>()
                    var currentIndex = 0

                    while (true) {
                        // Find next occurrence of "nik="
                        val nikIndex = pemuatNikString.indexOf("nik=", currentIndex)
                        if (nikIndex == -1) break // No more NIKs found

                        // Move position after "nik="
                        currentIndex = nikIndex + 4

                        // Find comma after the NIK value
                        val commaIndex = pemuatNikString.indexOf(",", currentIndex)
                        if (commaIndex == -1) break // Unexpected format

                        // Extract the NIK value
                        val nikValue = pemuatNikString.substring(currentIndex, commaIndex)
                        nikList.add(nikValue)

                        // Move position for next search
                        currentIndex = commaIndex + 1
                    }

                    // Join all NIK values with commas
                    val nikValues = nikList.joinToString(",")

                    // Log and store the result
                    AppLogger.d("Extracted NIK values: $nikValues")
                    globalPemuatNik = nikValues
                    globalRegional = tphData?.regional ?: ""
                    globalWilayah = tphData?.wilayah ?: ""
                    globalCompany = tphData?.company ?: 0
                    globalDept = tphData?.dept ?: 0
                    globalDivisi = tphData?.divisi ?: 0
                    globalBlokId = concatenatedIds
                    globalTotalJjg = totalJjg.toString()
                    globalBlokPPROJjg = BlokPPROJjg
                    globalBlokJjg = modifiedParsedData?.espb?.blokJjg ?: "-"
                    globalCreatedById = prefManager!!.idUserLogin
                    globalNopol = modifiedParsedData?.espb?.nopol ?: "-"
                    globalDriver = modifiedParsedData?.espb?.driver ?: "-"
                    globalTransporterId = transporterId
                    globalPemuatId = modifiedParsedData?.espb?.pemuat_id ?: "-"
                    globalKemandoranId = modifiedParsedData?.espb?.kemandoran_id ?: "-"
                    globalPemuatNik = nikValues
                    globalMillId = millId
                    globalTph0 = modifiedParsedData?.tph0 ?: "-"
                    globalTph1 = modifiedParsedData?.tph1 ?: "-"
                    globalCreatedAt = modifiedParsedData?.espb?.createdAt.toString() ?: "-"
                    globalCreatorInfo = modifiedParsedData?.espb?.creatorInfo?.toString() ?: "-"
                    globalNoESPB = modifiedParsedData?.espb?.noEspb ?: "-"
                    globalUpdateInfoSP = modifiedParsedData?.espb?.update_info_sp ?: "-"
                    globalIpMill = millIP

                    withContext(Dispatchers.Main) {
                        showBottomSheetWithData(
                            parsedData = modifiedParsedData,
                            distinctDeptAbbr = deptAbbr,
                            distinctDivisiAbbr = divisiAbbr,
                            formattedBlokList = formattedBlokList,
                            pemuat = pemuatNama,
                            totalJjgSum = totalJjg,
                            millAbbr = millAbbr,
                            transporterName = transporterName,
                            createAtFormatted = createAtFormatted,
                            hasError = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result: ${e.message}")
                AppLogger.e("Stack trace: ${e.stackTraceToString()}") // Log full stack trace
                withContext(Dispatchers.Main) {
                    val errorDetails = "Error: ${e.javaClass.simpleName} - ${e.message}"
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
                        errorMessage = errorDetails
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
        val factory2 = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory2)[DatasetViewModel::class.java]
    }


    override fun onResume() {
        super.onResume()
        if (barcodeView.visibility == View.VISIBLE) {
            setMaxBrightness(this@ScanWeighBridgeActivity, false)
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
        SoundPlayer.releaseMediaPlayer()
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