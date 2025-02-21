package com.cbi.cmp_project.ui.view.weightBridge

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.weightBridge.wbQRData

import com.cbi.cmp_project.ui.viewModel.WeightBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.formatToIndonesianDate
import com.cbi.cmp_project.utils.AppUtils.setMaxBrightness
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanWeightBridgeActivity : AppCompatActivity() {
    private lateinit var weightBridgeViewModel: WeightBridgeViewModel
    private var prefManager: PrefManager? = null

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var isScanning = true

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
    }

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_scan_weight_bridge)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupBottomSheet()
        setupQRScanner()

    }

    private fun setupQRScanner() {
        barcodeView = findViewById(R.id.barcode_scanner_krani_timbang)
        barcodeView.visibility = View.VISIBLE
        setMaxBrightness(this@ScanWeightBridgeActivity, true)
        // Hide status view
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

        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.behavior.apply {
            isDraggable = false
        }
        bottomSheetView.findViewById<Button>(R.id.btnProcess)?.setOnClickListener {
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
                        loadingDialog.setMessage("Sedang Simpan & Upload Data...")
                        delay(500)
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
            val btnProcess = it.findViewById<Button>(R.id.btnProcess)

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

                // And update how you set values:
                infoItems.forEach { (type, value) ->
                    val itemView = it.findViewById<View>(type.id)
                    setInfoItemValues(itemView, type.label, value)
                }
            }

        }

        bottomSheetDialog.show()
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {
        // Find the label TextView (tvLabel) in our included layout and set its text
        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        // Find the value TextView
        view.findViewById<TextView>(R.id.tvValue)?.text = when (view.id) {
            R.id.infoBlok -> value
            else -> ": $value"
        }
    }

    private fun pauseScanner() {
        barcodeView.pause()
        isScanning = false
    }

    // Also modify resumeScanner to be more robust
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

                    // Extract only idBlok from blokJjgList and map it to pairs of (idBlok, totalJjg)
//                    val blokJjgList = "3301,312;3303,154;3309,321;3310,215;3312,421;3315,233"
                    val blokJjgList = parsedData.espb.blokJjg
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

                    val blokData = blokListDeferred.await()
                        ?: throw Exception("Failed to fetch Blok Data! Please check the dataset.")

                    val distinctDeptAbbr =
                        blokData.mapNotNull { it.dept_abbr }.distinct().takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") ?: "-"
                    val distinctDivisiAbbr =
                        blokData.mapNotNull { it.divisi_abbr }.distinct().takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") ?: "-"

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
                    // Show error state
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
        val factory = WeightBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeightBridgeViewModel::class.java]
    }


    override fun onResume() {
        super.onResume()
        if (barcodeView.visibility == View.VISIBLE) {
            // Set max brightness first
            setMaxBrightness(this@ScanWeightBridgeActivity, true)
            // Always reset scanning state and resume scanner when screen visible again
            isScanning = false // Reset state
            lifecycleScope.launch {
                delay(100) // Small delay to ensure proper initialization
                resumeScanner() // This will set isScanning = true and resume camera
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Always pause the scanner when leaving the activity
        pauseScanner()
    }

    // Add this to properly release resources
    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()  // Ensure scanner is paused
        barcodeView.barcodeView?.cameraInstance?.close() // Release camera
        setMaxBrightness(this@ScanWeightBridgeActivity, false)
    }

    @Deprecated("Use onBackPressedDispatcher instead")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            // If bottom sheet is showing, dismiss it
            ::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing -> {
                bottomSheetDialog.dismiss()
            }
            // If scanner is active, properly clean up before exiting
            barcodeView.visibility == View.VISIBLE -> {
                pauseScanner()
                barcodeView.barcodeView?.cameraInstance?.close()
                super.onBackPressed()
            }
            // Otherwise, just navigate back
            else -> super.onBackPressed()
        }
    }
}