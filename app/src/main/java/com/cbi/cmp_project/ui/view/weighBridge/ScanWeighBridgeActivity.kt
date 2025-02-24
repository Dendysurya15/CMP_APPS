package com.cbi.cmp_project.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.weighBridge.wbQRData
import com.cbi.cmp_project.ui.view.HomePageActivity

import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.formatToIndonesianDate
import com.cbi.cmp_project.utils.AppUtils.setMaxBrightness
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNREACHABLE_CODE")
class ScanWeighBridgeActivity : AppCompatActivity() {
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private var prefManager: PrefManager? = null

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var isScanning = true

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_scan_weigh_bridge)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupBottomSheet()
        setupQRScanner()

    }

    private fun setupQRScanner() {
        barcodeView = findViewById(R.id.barcode_scanner_krani_timbang)
        barcodeView.visibility = View.VISIBLE
        setMaxBrightness(this@ScanWeighBridgeActivity, true)
        barcodeView.findViewById<TextView>(com.google.zxing.client.android.R.id.zxing_status_view)?.visibility = View.VISIBLE
        barcodeView.findViewById<TextView>(com.google.zxing.client.android.R.id.zxing_status_view)?.text = "Letakkan QR ke dalam kotak scan!"

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

        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.behavior.apply {
            isDraggable = false
        }
        bottomSheetView.findViewById<Button>(R.id.btnSaveUploadeSPB)?.setOnClickListener {
            isScanning = false
            pauseScanner()

                AlertDialogUtility.withTwoActions(
                    this,
                    "Simpan Data",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_submit_upload_data_espb_by_krani_timbang),
                    "warning.json"
                ) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Sedang Simpan Data e-SPB...")
                            delay(500)
                        }

                        try {
                            // Simulate saving and uploading process
                            withContext(Dispatchers.IO) {



                                weightBridgeViewModel.saveDataLocalKraniTimbangESPB(
                                    tph_id = selectedTPHValue?.toString() ?: "",
                                    date_created = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    created_by = userId!!,  // Prevent crash if userId is null
                                    karyawan_id = (selectedPemanenIds + selectedPemanenLainIds).joinToString(
                                        ","
                                    ),
                                    jjg_json = jjg_json,
                                    foto = photoFilesString,
                                    komentar = komentarFotoString,
                                    asistensi = asistensi ?: 0, // Default to 0 if null
                                    lat = lat ?: 0.0, // Default to 0.0 if null
                                    lon = lon ?: 0.0, // Default to 0.0 if null
                                    jenis_panen = selectedTipePanen?.toIntOrNull()
                                        ?: 0, // Avoid NumberFormatException
                                    ancakInput = ancakInput ?: "0", // Default to "0" if null
                                    info = infoApp ?: "",
                                    archive = 0
                                )

//                                throw Exception("Simulasi error saat upload data.")
                            }


                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                                Toasty.success(
                                    this@ScanWeighBridgeActivity,
                                    "Data berhasil disimpan dan diupload!",
                                    Toasty.LENGTH_SHORT,
                                    true
                                ).show()

                                AlertDialogUtility.withSingleAction(
                                    this@ScanWeighBridgeActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_success_save_local),
                                    stringXML(R.string.al_description_success_save_local_and_upload_to_server_cmp),
                                    "success.json",
                                    R.color.greenDefault
                                ) {
                                    val intent = Intent(this@ScanWeighBridgeActivity, HomePageActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }

                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()

                                AlertDialogUtility.withSingleAction(
                                    this@ScanWeighBridgeActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_fetch_data),
                                    "${stringXML(R.string.al_failed_save_local_and_upload_to_server_cmp)} ${e.message}",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {

                                }
                            }
                        }
                    }
                }


//            bottomSheetDialog.dismiss()
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
                loadingDialog.setMessage("Loading data...")
                delay(500)
            }
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)
                    val parsedData = Gson().fromJson(jsonStr, wbQRData::class.java)

                    val blokJjgList = "3301,312;3303,154;3309,321;3310,215;3312,421;3315,233"
//                    val blokJjgList = parsedData.espb.blokJjg
                        .split(";")
                        .mapNotNull {
                            it.split(",").takeIf { it.size == 2 }?.let { (id, jjg) ->
                                id.toIntOrNull()?.let { it to jjg.toIntOrNull() }
                            }
                        }

                    val idBlokList = blokJjgList.map { it.first }

                    val blokListDeferred = async {
                        try {
                            AppLogger.d(idBlokList.toString())
                            weightBridgeViewModel.getBlokById(idBlokList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Blok Data: ${e.message}")
                            null
                        }
                    }

                    val pemuatDeferred = async {
                        try {
                            // Split the string by commas, trim spaces, and filter out any empty entries
                            val pemuatList = parsedData.espb.pemuat
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            // Pass the list to the function
                            weightBridgeViewModel.getPemuatByIdList(pemuatList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Blok Data: ${e.message}")
                            null
                        }
                    }

                    val pemuatData = pemuatDeferred.await()
                        ?: throw Exception("Failed to fetch Pemuat Data! Please check the dataset.")

                    val blokData = try {
                        AppLogger.d(idBlokList.toString())
                        weightBridgeViewModel.getBlokById(idBlokList)
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                        null
                    }
                        ?: throw Exception("Failed to fetch Blok Data! Please check the dataset.")

                    val groupedDeptDivisi = blokData?.groupBy { it.dept_abbr }
                        ?.mapNotNull { (dept, blokList) ->
                            dept?.let {
                                val divisiList =
                                    blokList.mapNotNull { it.divisi_abbr }
                                        .distinct()
                                if (divisiList.isNotEmpty()) {
                                    "$dept ${divisiList.joinToString(" ")}"
                                } else {
                                    dept // If no divisions, show only the department
                                }
                            }
                        } ?: listOf("-") // Default when blokData is null

                    // Check if only one distinct department exists
                    val distinctDeptAbbr =
                        groupedDeptDivisi.map { it.split(" ").first() }
                            .distinct()
                            .joinToString(", ")

                    val distinctDeptCount =
                        groupedDeptDivisi.map { it.split(" ").first() }
                            .distinct().size

                    val distinctDivisiAbbr = if (distinctDeptCount == 1) {
                        // Only one department → Show only divisions
                        groupedDeptDivisi.flatMap {
                            it.split(" ").drop(1)
                        } // Drop department name
                            .distinct()
                            .joinToString(" ")
                    } else {
                        // Multiple departments → Show dept with divisions
                        groupedDeptDivisi.joinToString(", ")
                    }

                    val pemuatNama =
                        pemuatData.mapNotNull { it.nama }.takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") ?: "-"

                    var totalJjgSum = 0

                    val formattedBlokList = blokJjgList.mapNotNull { (idBlok, totalJjg) ->
                        val blokKode = blokData.find { it.blok == idBlok }?.blok_kode
                        if (blokKode != null && totalJjg != null) {
                            totalJjgSum += totalJjg
                            "• $blokKode ($totalJjg jjg)"
                        } else {
                            null
                        }
                    }.joinToString("\n").takeIf { it.isNotBlank() } ?: ": -"

                    val millId = parsedData.espb.millId
                    val transporterId = parsedData.espb.transporter
                    val createdAt = parsedData.espb.createdAt
                    val createAtFormatted = formatToIndonesianDate(createdAt)

                    val millDataDeferred = async {
                        try {
                            weightBridgeViewModel.getMillName(millId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Mill Data: ${e.message}")
                            null
                        }
                    }

                    val millData = millDataDeferred.await()
                        ?: throw Exception("Failed to fetch Mill Data! Please check the dataset.")

                    val transporterDeferred = async {
                        try {
                            weightBridgeViewModel.getTransporterName(transporterId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Transporter Data: ${e.message}")
                            null
                        }
                    }

                    val transporterData = transporterDeferred.await()
                        ?: throw Exception("Failed to fetch Transporter Data! Please check the dataset.")

                    withContext(Dispatchers.Main) {
                        val millAbbr =
                            millData.firstOrNull()?.let { "${it.abbr} (${it.nama})" } ?: "-"
                        val transporterName = transporterData.firstOrNull()?.let { it.nama } ?: "-"

                        showBottomSheetWithData(
                            parsedData = parsedData,
                            distinctDeptAbbr = distinctDeptAbbr,
                            distinctDivisiAbbr = distinctDivisiAbbr,
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