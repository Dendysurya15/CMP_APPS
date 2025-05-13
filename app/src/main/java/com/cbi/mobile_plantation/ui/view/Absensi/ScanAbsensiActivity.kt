package com.cbi.mobile_plantation.ui.view.Absensi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.weighBridge.wbQRData
import com.cbi.mobile_plantation.data.repository.AbsensiRepository
import com.cbi.mobile_plantation.data.repository.WeighBridgeRepository
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.weighBridge.ScanWeighBridgeActivity
import com.cbi.mobile_plantation.ui.view.weighBridge.ScanWeighBridgeActivity.InfoType
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
    var globalEstate: String = ""
    var globalAfdeling: String = ""
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
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_absen, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.behavior.apply {
            isDraggable = false
        }

        bottomSheetView.findViewById<Button>(R.id.btnSaveUploadeAbsen)?.setOnClickListener {
            isScanning = false
            pauseScanner()

            AppLogger.d("Tombol Simpan Absen Diklik")

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
                                    kemandoran_id = globalIdKemandoran ?: "",
                                    date_absen = globalDateTime,
                                    created_by = globalCreatedBy ?: 0,
                                    karyawan_msk_id = globalKaryawanMskId ?: "",
                                    karyawan_tdk_msk_id = globalKaryawanTdkMskId ?: "",
                                    karyawan_msk_nik = "",  // Default empty string for now
                                    karyawan_tdk_msk_nik = "",  // Default empty string for now
                                    karyawan_msk_nama = "",  // Default empty string for now
                                    karyawan_tdk_msk_nama = "",  // Default empty string for now
                                    foto = globalFoto,
                                    komentar = globalKomentar,
                                    asistensi = globalAsistensi ?: 0,
                                    lat = globalLat ?: 0.0,
                                    lon = globalLon ?: 0.0,
                                    info = globalInfo,
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
                                    stringXML(R.string.al_description_success_save_local_and_espb_krani_timbang),
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
        }

        bottomSheetView.findViewById<Button>(R.id.btnScanAgainAbsen)?.setOnClickListener {
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

                    val jsonObject = JSONObject(jsonStr)
                    AppLogger.d("gas jsonObject: $jsonObject")

                    val idKemandoranList = jsonObject.optJSONArray("idKemandoran")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalIdKemandoran = idKemandoranList.joinToString(",")

                    val idKaryawanList = jsonObject.optJSONArray("idKaryawan")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalKaryawanMskId = idKaryawanList.joinToString(",")

                    val datetimeList = jsonObject.optJSONArray("datetime")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    val rawDatetime = datetimeList.firstOrNull() ?: ""
                    globalDateTime = rawDatetime

                    val formattedDatetime = if (rawDatetime.isNotEmpty()) {
                        val parsedDate = LocalDate.parse(rawDatetime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        parsedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                    } else {
                        ""
                    }

                    val estateList = jsonObject.optJSONArray("estate")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalEstate =  estateList.firstOrNull() ?: ""

                    AppLogger.d("gasss $estateList")
                    AppLogger.d("gas $globalEstate")
                    val afdelingList = jsonObject.optJSONArray("afdeling")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalAfdeling = afdelingList.joinToString(", ")

//                    globalCreatedBy = if (jsonObject.has("createdBy")) jsonObject.optInt("createdBy") else null
                    globalCreatedBy = jsonObject.optJSONArray("createdBy")?.optString(0)?.toIntOrNull()


                    val fotoList = jsonObject.optJSONArray("foto")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalFoto = fotoList.joinToString(", ")

                    val komentarList = jsonObject.optJSONArray("komentar")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalKomentar = komentarList.joinToString("\n")

//                    globalAsistensi = if (jsonObject.has("asistensi")) jsonObject.optInt("asistensi") else null
//                    globalLat = if (jsonObject.has("lat")) jsonObject.optDouble("lat", 0.0) else null
//                    globalLon = if (jsonObject.has("lon")) jsonObject.optDouble("lon", 0.0) else null

//                    globalAsistensi = jsonObject.opt("asistensi") as? Int
//                    globalLat = jsonObject.opt("lat") as? Double
//                    globalLon = jsonObject.opt("lon") as? Double
                    globalAsistensi = jsonObject.optJSONArray("asistensi")?.optString(0)?.toIntOrNull()
                    globalLat = jsonObject.optJSONArray("lat")?.optString(0)?.toDoubleOrNull()
                    globalLon = jsonObject.optJSONArray("lon")?.optString(0)?.toDoubleOrNull()

                    val infoList = jsonObject.optJSONArray("info")
                        ?.let { array -> List(array.length()) { array.optString(it) } } ?: emptyList()
                    globalInfo = infoList.joinToString(", ")

                    AppLogger.d("Parsed JSON Data: idKemandoran=$globalIdKemandoran, idKaryawan=$globalKaryawanMskId, datetime=$globalDateTime, estate=$globalEstate, afdeling=$globalAfdeling, createdBy=$globalCreatedBy, foto=$globalFoto, komentar=$globalKomentar, asistensi=$globalAsistensi, lat=$globalLat, lon=$globalLon, info=$globalInfo")

                    // Mengambil data kemandoran berdasarkan ID
                    val kemandoranDeferred = async {
                        try {
                            absensiViewModel.getKemandoranById(idKemandoranList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                            null
                        }
                    }

                    val kemandoranData = kemandoranDeferred.await()
                    val kemandoranRow = kemandoranData?.mapNotNull { it.kode }?.takeIf { it.isNotEmpty() }
                        ?.joinToString("\n") ?: "-"

                    AppLogger.d(kemandoranRow)

                    // Mengambil data pemuat berdasarkan ID karyawan
                    val pemuatDeferred = async {
                        try {
                            absensiViewModel.getPemuatByIdList(idKaryawanList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                            null
                        }
                    }

                    val pemuatData = pemuatDeferred.await()
                    val pemuatNama = pemuatData?.mapNotNull { it.nama }?.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") ?: "-"

                    AppLogger.d(pemuatNama)

                    withContext(Dispatchers.Main) {
                        showBottomSheetWithData(
                            namaKemandoranQR = kemandoranRow,
                            namaKaryawanQR = pemuatNama,
                            datetimeQR = formattedDatetime,
                            estateQR = "",
                            afdelingQR = globalAfdeling,
                            createdByQR = globalCreatedBy?.toString() ?: "-",
                            fotoQR = globalFoto,
                            komentarQR = globalKomentar,
                            asistensiQR = globalAsistensi?.toString() ?: "-",
                            latQR = globalLat?.toString() ?: "-",
                            lonQR = globalLon?.toString() ?: "-",
                            infoQR = globalInfo,
                            hasError = false
                        )
                    }
                }

            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result: ${e.message}")
                withContext(Dispatchers.Main) {
                    showBottomSheetWithData(
                        namaKemandoranQR = "-",
                        namaKaryawanQR = "-",
                        datetimeQR = "-",
                        estateQR = "-",
                        afdelingQR = "-",
                        createdByQR = "-",
                        fotoQR = "-",
                        komentarQR = "-",
                        asistensiQR = "-",
                        latQR = "-",
                        lonQR = "-",
                        infoQR = "-",
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

    @SuppressLint("SetTextI18n")
    private fun showBottomSheetWithData(
        namaKemandoranQR: String,
        namaKaryawanQR: String,
        datetimeQR: String,
        estateQR: String,
        afdelingQR: String,
        createdByQR: String,
        fotoQR: String,
        komentarQR: String,
        asistensiQR: String,
        latQR: String,
        lonQR: String,
        infoQR: String,
        hasError: Boolean = false,
        errorMessage: String? = null
    ) {
        val bottomSheetView = bottomSheetDialog.findViewById<View>(R.id.bottomSheetContentAbsen)
        bottomSheetView?.let {
            val errorCard = it.findViewById<LinearLayout>(R.id.errorCardAbsen)
            val dataContainer = it.findViewById<LinearLayout>(R.id.dataContainerAbsen)
            val errorText = it.findViewById<TextView>(R.id.errorTextAbsen)
            val btnProcess = it.findViewById<Button>(R.id.btnSaveUploadeAbsen)

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
                    InfoType.DATE to datetimeQR,
                    InfoType.ESTATE to estateQR,
                    InfoType.AFDELING to afdelingQR,
                    InfoType.NAMAKEMANDORAN to namaKemandoranQR,
                    InfoType.NAMAKARYAWAN to namaKaryawanQR
                )

                infoItems.forEach { (type, value) ->
                    val itemView = it.findViewById<View>(type.id)
                    setInfoItemValues(itemView, type.label, value)
                }
            }

        }

        bottomSheetDialog.show()
    }

    enum class InfoType(val id: Int, val label: String) {
        DATE(R.id.infoCreatedAtAbsen, "Tanggal"),
        ESTATE(R.id.infoEstateAbsen, "Estate"),
        AFDELING(R.id.infoAfdelingAbsen, "Afdeling"),
        NAMAKEMANDORAN(R.id.infoKemandoranAbsen, "Kemandoran"),
        NAMAKARYAWAN(R.id.infoKaryawanAbsen, "Karyawan")
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {

        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        view.findViewById<TextView>(R.id.tvValue)?.text = when (view.id) {
            R.id.infoBlok -> value
            else -> ": $value"
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