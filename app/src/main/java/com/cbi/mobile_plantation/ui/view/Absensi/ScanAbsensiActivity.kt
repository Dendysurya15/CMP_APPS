package com.cbi.mobile_plantation.ui.view.Absensi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScanAbsensiActivity : AppCompatActivity() {

    data class KemandoranQRData(
        val idKemandoran: List<String>, // Change from String to List<String>
        val idKaryawan: List<String>
    )

    private lateinit var absensiViewModel: AbsensiViewModel
    private var prefManager: PrefManager? = null

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var isScanning = true

    private lateinit var loadingDialog: LoadingDialog

    var globalIdKemandoran: String? = null
    var globalDateTime: String = ""
    var globalKaryawanMskId: String? = ""
    var globalKaryawanTdkMskId: String? = ""
    var globalCreatedBy: Int? = null
    var globalFoto: String = ""
    var globalKomentar: String = ""
    var globalAsistensi: Int? = null
    var globalLat: Double? = null
    var globalLon: Double? = null
    var globalInfo: String = ""
    var globalDept : String = ""
    var globalDeptAbbr : String = ""
    var globalDivisi : String = ""
    var globalDivisiAbbr : String = ""
    var globalKaryawanMskNama : String = ""
    var globalKaryawanTdkMskNama : String = ""
    var globalKaryawanMskNik : String = ""
    var globalKaryawanTdkMskNik : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_scan_absensi)

        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupBottomSheet()
        setupQRScanner()
    }

    private fun initViewModel() {
        val factory = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel = ViewModelProvider(this, factory)[AbsensiViewModel::class.java]
    }

    @SuppressLint("MissingInflatedId")
    private fun setupBottomSheet() {
        bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_absensi, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.behavior.apply {
            isDraggable = false
        }

//        bottomSheetView.findViewById<Button>(R.id.btnSaveUploadAbsensi)?.setOnClickListener {
//            isScanning = false
//            pauseScanner()
//
//            AppLogger.d("Tombol Simpan Absen Diklik")
//
//
//        }

        bottomSheetView.findViewById<Button>(R.id.btnScanAgainAbsensi)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setOnDismissListener {
            if (barcodeView.visibility == View.VISIBLE && !isScanning) {
                resumeScanner()
            }
        }
    }


    private fun setupQRScanner() {
        barcodeView = findViewById(R.id.barcode_scanner_absensi)
        setMaxBrightness(this@ScanAbsensiActivity, true)
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
                        AppLogger.d("tes")
                        processQRResult(qrCodeValue)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })

        resumeScanner()
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

    private fun processQRResult(qrResult: String) {

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(500)
            }
            try {
                withContext(Dispatchers.IO) {
                    // Membaca JSON dari hasil scan QR
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)
                    AppLogger.d("cek Json: $jsonStr")

                    if (jsonStr.isNullOrEmpty()) {
                        throw Exception("JSON string is empty or null")
                    }

                    val jsonObject = JSONObject(jsonStr)

                    // Extract the essential data
                    val idKemandoranList = jsonObject.optJSONArray("id_kemandoran")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()

                    val datetimeList = jsonObject.optJSONArray("datetime")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val rawDatetime = datetimeList.firstOrNull() ?: ""

                    // Extract dept and dept_abbr (previously estate)
                    val deptList = jsonObject.optJSONArray("dept")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val dept = deptList.firstOrNull() ?: ""

                    val deptAbbrList = jsonObject.optJSONArray("dept_abbr")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val deptAbbr = deptAbbrList.firstOrNull() ?: ""

                    // Use estate as fallback for dept_abbr if not present
                    val estateList = jsonObject.optJSONArray("dept")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val estate = estateList.firstOrNull() ?: ""

                    val finalDeptAbbr = if (deptAbbr.isNotEmpty()) deptAbbr else estate

                    // Extract divisi and divisi_abbr (previously afdeling)
                    val divisiList = jsonObject.optJSONArray("divisi")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val divisi = divisiList.firstOrNull() ?: ""

                    val divisiAbbrList = jsonObject.optJSONArray("divisi_abbr")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val divisiAbbr = divisiAbbrList.firstOrNull() ?: ""

                    // Use afdeling as fallback for divisi_abbr if not present
                    val afdelingList = jsonObject.optJSONArray("divisi")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val afdeling = afdelingList.firstOrNull() ?: ""

                    val finalDivisiAbbr = if (divisiAbbr.isNotEmpty()) divisiAbbr else afdeling

                    // Extract info field
                    val infoList = jsonObject.optJSONArray("info")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val info = infoList.firstOrNull() ?: ""

                    // Extract created_by field
                    val createdByList = jsonObject.optJSONArray("created_by")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val createdBy = createdByList.firstOrNull()?.toIntOrNull() ?: 0

                    val formattedDatetime = if (rawDatetime.isNotEmpty()) {
                        try {
                            // Check if we need to add seconds (since your formatting function expects HH:mm:ss)
                            val dateTimeWithSeconds = if (rawDatetime.contains(" ")) {
                                // Already has time component
                                if (rawDatetime.matches(Regex(".*\\d\\d:\\d\\d:\\d\\d.*"))) {
                                    // Already has seconds
                                    rawDatetime
                                } else {
                                    // Add seconds
                                    rawDatetime.replace(Regex("(\\d\\d:\\d\\d)"), "$1:00")
                                }
                            } else {
                                // Only date component, add a default time
                                "$rawDatetime 00:00:00"
                            }

                            AppUtils.formatToIndonesianDate(dateTimeWithSeconds)
                        } catch (e: Exception) {
                            AppLogger.e("Error formatting datetime: ${e.message}")
                            rawDatetime // fallback to raw datetime if parsing fails
                        }
                    } else {
                        ""
                    }

                    // Process the employee data
                    // First try karyawan_msk_id and karyawan_tdk_msk_id
                    val karyawanMskIdList = jsonObject.optJSONArray("karyawan_msk_id")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val karyawanTdkMskIdList = jsonObject.optJSONArray("karyawan_tdk_msk_id")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()

                    // Fallback to NIK if ID lists are empty
                    val nikKaryawanMskList = jsonObject.optJSONArray("karyawan_msk_nik")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val nikKaryawanTdkMskList = jsonObject.optJSONArray("karyawan_tdk_msk_nik")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()

                    // Use IDs if available, otherwise use NIKs
                    val finalKaryawanMskIdList = if (karyawanMskIdList.isNotEmpty()) karyawanMskIdList else nikKaryawanMskList
                    val finalKaryawanTdkMskIdList = if (karyawanTdkMskIdList.isNotEmpty()) karyawanTdkMskIdList else nikKaryawanTdkMskList

                    // Join lists to strings for global variables
                    val karyawanMskIdString = finalKaryawanMskIdList.joinToString(",")
                    val karyawanTdkMskIdString = finalKaryawanTdkMskIdList.joinToString(",")
                    val nikKaryawanMskString = nikKaryawanMskList.joinToString(",")
                    val nikKaryawanTdkMskString = nikKaryawanTdkMskList.joinToString(",")
                    val idKemandoranString = idKemandoranList.joinToString(",")

                    AppLogger.d("Present employees (ID/NIK): ${finalKaryawanMskIdList.joinToString(", ")}")
                    AppLogger.d("Absent employees (ID/NIK): ${finalKaryawanTdkMskIdList.joinToString(", ")}")

                    // Get employee names if needed
                    var karyawanMskNamaString = ""
                    var karyawanTdkMskNamaString = ""

                    try {
                        if (nikKaryawanMskList.isNotEmpty()) {
                            val presentEmployees = absensiViewModel.getKaryawanByNikList(nikKaryawanMskList)
                            val nikToNameMap = presentEmployees.associate { it.nik to it.nama }
                            karyawanMskNamaString = nikKaryawanMskList.mapNotNull { nik ->
                                nikToNameMap[nik]
                            }.joinToString(",")
                        }

                        if (nikKaryawanTdkMskList.isNotEmpty()) {
                            val absentEmployees = absensiViewModel.getKaryawanByNikList(nikKaryawanTdkMskList)
                            val nikToNameMap = absentEmployees.associate { it.nik to it.nama }
                            karyawanTdkMskNamaString = nikKaryawanTdkMskList.mapNotNull { nik ->
                                nikToNameMap[nik]
                            }.joinToString(",")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error getting employee names: ${e.message}")
                    }

                    var namaKemandoran = "-"
                    try {
                        val kemandoranData = absensiViewModel.getKemandoranById(idKemandoranList)
                        namaKemandoran = kemandoranData?.mapNotNull { it.nama }?.takeIf { it.isNotEmpty() }
                            ?.joinToString("\n") ?: "-"
                        AppLogger.d("Kemandoran name: $namaKemandoran")
                    } catch (e: Exception) {
                        AppLogger.e("Error getting kemandoran name: ${e.message}")
                    }

                    // Calculate attendance statistics
                    val totalMasuk = finalKaryawanMskIdList.size
                    val totalTidakMasuk = finalKaryawanTdkMskIdList.size
                    val totalAll = totalMasuk + totalTidakMasuk
                    val kehadiranText = "$totalMasuk / $totalAll"

                    AppLogger.d("Attendance stats: $kehadiranText")
                    AppLogger.d("dept: $dept, dept_abbr: $finalDeptAbbr")
                    AppLogger.d("divisi: $divisi, divisi_abbr: $finalDivisiAbbr")

                    // Set global variables for saving data
                    globalIdKemandoran = idKemandoranString
                    globalDateTime = rawDatetime
                    globalCreatedBy = createdBy
                    globalKaryawanMskId = karyawanMskIdString
                    globalKaryawanTdkMskId = karyawanTdkMskIdString
                    globalInfo = info

                    // Store dept and divisi information
                    globalDept = dept
                    globalDeptAbbr = finalDeptAbbr
                    globalDivisi = divisi
                    globalDivisiAbbr = finalDivisiAbbr

                    // Store employee names

                    AppLogger.d(nikKaryawanMskString)
                    AppLogger.d(karyawanMskNamaString)
                    globalKaryawanMskNama = karyawanMskNamaString
                    globalKaryawanTdkMskNama = karyawanTdkMskNamaString
                    globalKaryawanMskNik = nikKaryawanMskString
                    globalKaryawanTdkMskNik = nikKaryawanTdkMskString

                    withContext(Dispatchers.Main) {
                        try {
                            showBottomSheetWithData(
                                datetimeQR = formattedDatetime,
                                estateAfdelingQR = "$finalDeptAbbr / $finalDivisiAbbr",
                                namaKemandoranQR = namaKemandoran,
                                kehadiranQR = kehadiranText,
                                hasError = false,
                                errorMessage = null
                            )
                        } catch (e: Exception) {
                            AppLogger.e("Error showing bottom sheet with data: ${e.message}")
                            e.printStackTrace()
                            showBottomSheetWithData(
                                datetimeQR = "-",
                                estateAfdelingQR = "-",
                                namaKemandoranQR = "-",
                                kehadiranQR = "-",
                                hasError = true,
                                errorMessage = "Error showing data: ${e.message ?: "Unknown error"}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    try {
                        showBottomSheetWithData(
                            datetimeQR = "-",
                            estateAfdelingQR = "-",
                            namaKemandoranQR = "-",
                            kehadiranQR = "-",
                            hasError = true,
                            errorMessage = e.message ?: "Unknown error occurred"
                        )
                    } catch (showError: Exception) {
                        AppLogger.e("Failed even to show error dialog: ${showError.message}")
                        showError.printStackTrace()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    try {
                        loadingDialog.dismiss()
                    } catch (e: Exception) {
                        AppLogger.e("Error dismissing loading dialog: ${e.message}")
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showBottomSheetWithData(
        datetimeQR: String,
        estateAfdelingQR: String,
        namaKemandoranQR: String,
        kehadiranQR: String,
        hasError: Boolean = false,
        errorMessage: String? = null
    ) {
        try {
            bottomSheetDialog?.let { dialog ->
                try {
                    // Get all views with detailed logging
                    val titleDialogDetailTable = dialog.findViewById<TextView>(R.id.titleDialogDetailTableAbsensi)
                    val dashedline = dialog.findViewById<View>(R.id.dashedLineAbsensi)
                    val errorCard = dialog.findViewById<LinearLayout>(R.id.errorCardAbsensi)
                    val dataContainer = dialog.findViewById<LinearLayout>(R.id.dataContainerAbsensi)
                    val errorText = dialog.findViewById<TextView>(R.id.errorTextAbsensi)
                    val btnProcess = dialog.findViewById<Button>(R.id.btnSaveUploadAbsensi)

                    if (titleDialogDetailTable == null || errorCard == null || dataContainer == null ||
                        errorText == null || btnProcess == null) {
                        AppLogger.e("Cannot proceed with bottom sheet - essential views are missing")
                        return@let
                    }

                    // Configure based on error state
                    if (hasError) {
                        titleDialogDetailTable.text = "Terjadi Kesalahan Scan QR!"
                        titleDialogDetailTable.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.colorRedDark
                            )
                        )
                        errorCard.visibility = View.VISIBLE
                        dataContainer.visibility = View.GONE
                        errorText.text = errorMessage ?: "Unknown error"
                        btnProcess.visibility = View.GONE

                    } else {


                        titleDialogDetailTable.text = "Konfirmasi Data Absensi"
                        titleDialogDetailTable.setTextColor(ContextCompat.getColor(this, R.color.black))

                        errorCard.visibility = View.GONE
                        dataContainer.visibility = View.VISIBLE
                        btnProcess.visibility = View.VISIBLE

                        try {
                            playSound(R.raw.berhasil_scan)
                        } catch (e: Exception) {
                            AppLogger.e("Failed to play sound: ${e.message}")
                        }

                        try {
                            val availableInfoTypes = InfoType.values().joinToString { "${it.name}: ${it.id}" }
                            AppLogger.d("Available InfoTypes: $availableInfoTypes")

                            val infoItems = listOf(
                                InfoType.DATE to datetimeQR,
                                InfoType.ESTATEAFDELING to estateAfdelingQR,
                                InfoType.KEMANDORAN to namaKemandoranQR,
                                InfoType.KEHADIRAN to kehadiranQR
                            )

                            // Debug info items
                            AppLogger.d("Info items to set: $infoItems")

                            infoItems.forEach { (type, value) ->
                                try {
                                    AppLogger.d("Finding view for InfoType: ${type.name} with ID: ${type.id}")
                                    val itemView = dialog.findViewById<View>(type.id)

                                    if (itemView != null) {
                                        setInfoItemValues(itemView, type.label, value)
                                    } else {
                                        AppLogger.e("View not found for InfoType: ${type.name} with ID: ${type.id}")
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Error setting info for ${type.name}: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error setting info items: ${e.message}")
                        }
                    }

                    // Set bottom sheet behavior safely
                    try {
                        val maxHeight = (resources.displayMetrics.heightPixels * 0.7).toInt()
                        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

                        if (bottomSheet != null) {
                            val behavior = BottomSheetBehavior.from(bottomSheet)

                            behavior.apply {
                                this.peekHeight = maxHeight
                                this.state = BottomSheetBehavior.STATE_EXPANDED
                                this.isFitToContents = true
                                this.isDraggable = false
                            }

                            bottomSheet.layoutParams?.height = maxHeight
                        } else {
                            AppLogger.e("Bottom sheet view not found")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error setting bottom sheet behavior: ${e.message}")
                    }

                    // Set up button click listener
                    btnProcess.setOnClickListener {
                        try {
                            AlertDialogUtility.withTwoActions(
                                this,
                                "Simpan Data",
                                getString(R.string.confirmation_dialog_title),
                                getString(R.string.al_submit_upload_data_absensi),
                                "warning.json",
                                ContextCompat.getColor(
                                    this@ScanAbsensiActivity,
                                    R.color.bluedarklight
                                ),
                                function = {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        try {
                                            // Debug log sebelum menyimpan
                                            AppLogger.d("TAG" +
                                                    "kemandoran_id=$globalIdKemandoran, " +
                                                    "date_absen=$globalDateTime, " +
                                                    "created_by=$globalCreatedBy, " +
                                                    "karyawan_msk_id=$globalKaryawanMskId, " +
                                                    "karyawan_tdk_msk_id=$globalKaryawanTdkMskId, " +
                                                    "foto=$globalFoto, " +
                                                    "komentar=$globalKomentar, " +
                                                    "asistensi=$globalAsistensi, " +
                                                    "lat=$globalLat, lon=$globalLon, " +
                                                    "info=$globalInfo")

                                            val result = withContext(Dispatchers.IO) {
                                                val response = absensiViewModel.saveDataAbsensi(
                                                    dept = globalDept ?: "",
                                                    dept_abbr = globalDeptAbbr ?: "",
                                                    divisi = globalDivisi ?: "",
                                                    divisi_abbr = globalDivisiAbbr ?: "",
                                                    kemandoran_id = globalIdKemandoran ?: "",
                                                    date_absen = globalDateTime,
                                                    created_by = globalCreatedBy ?: 0,
                                                    karyawan_msk_id = globalKaryawanMskId ?: "",
                                                    karyawan_tdk_msk_id = globalKaryawanTdkMskId ?: "",
                                                    karyawan_msk_nik = globalKaryawanMskNik ?: "",  // Use the NIK values
                                                    karyawan_tdk_msk_nik = globalKaryawanTdkMskNik ?: "",  // Use the NIK values
                                                    karyawan_msk_nama = globalKaryawanMskNama ?: "",
                                                    karyawan_tdk_msk_nama = globalKaryawanTdkMskNama ?: "",
                                                    foto = globalFoto,
                                                    komentar = globalKomentar,
                                                    asistensi = globalAsistensi ?: 0,
                                                    lat = globalLat ?: 0.0,
                                                    lon = globalLon ?: 0.0,
                                                    info = globalInfo ?: "",
                                                    status_scan = 1,
                                                    archive = 0
                                                )

                                                AppLogger.d("Hasil penyimpanan data: $response")
                                                response
                                            }

                                            if (result != null) {
                                                AppLogger.d("TAG", "Data berhasil disimpan!")
                                                AlertDialogUtility.withSingleAction(
                                                    this@ScanAbsensiActivity,
                                                    stringXML(R.string.al_back),
                                                    stringXML(R.string.al_success_save_local),
                                                    stringXML(R.string.al_description_success_absensi),
                                                    "success.json",
                                                    R.color.greendarkerbutton
                                                ) {
                                                    val intent = Intent(this@ScanAbsensiActivity, HomePageActivity::class.java)
                                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                                    startActivity(intent)
                                                    finish()
                                                }
                                            } else {
                                                AppLogger.d("Gagal menyimpan data: response null")
                                                AlertDialogUtility.withSingleAction(
                                                    this@ScanAbsensiActivity,
                                                    stringXML(R.string.al_back),
                                                    stringXML(R.string.al_failed_save_local),
                                                    stringXML(R.string.al_failed_save_local_krani_timbang),
                                                    "warning.json",
                                                    R.color.colorRedDark
                                                ) {}
                                            }

                                        } catch (e: Exception) {
                                            AppLogger.d("Unexpected error: ${e.message}")
                                            loadingDialog.dismiss()

                                            AlertDialogUtility.withSingleAction(
                                                this@ScanAbsensiActivity,
                                                stringXML(R.string.al_back),
                                                stringXML(R.string.al_failed_save_local),
                                                "${stringXML(R.string.al_failed_save_local_krani_timbang)} : ${e.message}",
                                                "warning.json",
                                                R.color.colorRedDark
                                            ) {}
                                        }
                                    }
                                }
                            )

                        } catch (e: Exception) {
                            AppLogger.e("Error in button click handler: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    AppLogger.e("Error setting up bottom sheet views: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Show the dialog safely
            try {
                bottomSheetDialog?.show()
            } catch (e: Exception) {
                AppLogger.e("Error showing bottom sheet dialog: ${e.message}")
                e.printStackTrace()
            }

        } catch (e: Exception) {
            AppLogger.e("Critical error in showBottomSheetWithData: ${e.message}")
            e.printStackTrace()

            // Last resort - show a toast
            try {
                Toast.makeText(
                    this,
                    "Terjadi kesalahan: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (t: Exception) {
                // Nothing more we can do
            }
        }
    }

    enum class InfoType(val id: Int, val label: String) {
        DATE(R.id.tglAbsensi, "Tanggal"),
        ESTATEAFDELING(R.id.estateAfdAbsensi, "Estate/Afdeling"),
        KEMANDORAN(R.id.kemandoranAbsensi, "Kemandoran"),
        KEHADIRAN(R.id.kehadiranAbsensi, "Total Kehadiran"),
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {
        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        val tvValue = view.findViewById<TextView>(R.id.tvValue)
        if (tvValue != null) {
            if (value.contains("\n")) {
                // For multi-line text, convert to bullet points and remove the colon
                val lines = value.split("\n")
                val bulletedText = lines.joinToString("\n") { "• $it" }

                // Set the bulleted text
                tvValue.text = bulletedText

                // Adjust styling for multi-line text
                tvValue.gravity = Gravity.TOP
                tvValue.setLineSpacing(6f, 1.2f)  // Extra spacing between lines
            } else {
                // For single-line text, use the regular format with colon
                val formattedValue = when (view.id) {
                    R.id.infoBlok -> value
                    else -> ": $value"
                }
                tvValue.text = formattedValue
                tvValue.gravity = Gravity.CENTER_VERTICAL
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (barcodeView.visibility == View.VISIBLE) {
            setMaxBrightness(this@ScanAbsensiActivity, true)
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
        setMaxBrightness(this@ScanAbsensiActivity, false)
    }

    @Deprecated("Use onBackPressedDispatcher instead")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            ::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing -> {
                bottomSheetDialog.dismiss()
            }

            barcodeView.visibility == View.VISIBLE -> {
                pauseScanner()
                barcodeView.barcodeView?.cameraInstance?.close()
                super.onBackPressed()
            }

            else -> super.onBackPressed()
        }
    }
}