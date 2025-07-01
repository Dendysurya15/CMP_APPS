package com.cbi.mobile_plantation.ui.view

import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import kotlin.reflect.full.findAnnotation
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.database.KaryawanDao
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.data.model.uploadCMP.UploadCMPResponse
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.databinding.ActivityHomePageBinding
import com.cbi.mobile_plantation.ui.adapter.DisplayType
import com.cbi.mobile_plantation.ui.adapter.DownloadItem
import com.cbi.mobile_plantation.ui.adapter.DownloadProgressDatasetAdapter
import com.cbi.mobile_plantation.ui.adapter.FeatureCard
import com.cbi.mobile_plantation.ui.adapter.FeatureCardAdapter
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.view.Absensi.FeatureAbsensiActivity
import com.cbi.mobile_plantation.ui.view.Absensi.ListAbsensiActivity
import com.cbi.mobile_plantation.ui.adapter.UploadCMPItem
import com.cbi.mobile_plantation.ui.adapter.UploadProgressCMPDataAdapter
import com.cbi.mobile_plantation.ui.view.Absensi.ScanAbsensiActivity
import com.cbi.mobile_plantation.ui.view.HektarPanen.DaftarHektarMPanen
import com.cbi.mobile_plantation.ui.view.Inspection.ListInspectionActivity
import com.cbi.mobile_plantation.ui.view.espb.ListHistoryESPBActivity
import com.cbi.mobile_plantation.ui.view.HektarPanen.TransferHektarPanenActivity
import com.cbi.mobile_plantation.ui.view.weighBridge.ListHistoryWeighBridgeActivity
import com.cbi.mobile_plantation.ui.view.weighBridge.ScanWeighBridgeActivity
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel

import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel
import com.cbi.mobile_plantation.ui.viewModel.HektarPanenViewModel
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.ui.viewModel.UploadCMPViewModel
import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToCamelCase
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.worker.DataCleanupWorker

import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomePageActivity : AppCompatActivity() {

    private lateinit var featureAdapter: FeatureCardAdapter
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null
    private lateinit var absensiViewModel: AbsensiViewModel
    private var uploadCMPData: List<Pair<String, String>> = emptyList()
    private val _allIdsAndFilenames = MutableLiveData<List<Pair<String, String>>>()
    val allIdsAndFilenames: LiveData<List<Pair<String, String>>> = _allIdsAndFilenames
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private lateinit var uploadCMPViewModel: UploadCMPViewModel
    private lateinit var inspectionViewModel: InspectionViewModel
    private lateinit var hektarPanenViewModel: HektarPanenViewModel

    private var isTriggerButtonSinkronisasiData: Boolean = false
    private var isTriggerFeatureInspection: Boolean = false
    private lateinit var dialog: Dialog
    private var countAbsensi: Int = 0  // Global variable for count
    private var countPanenTPH: Int = 0  // Global variable for count
    private var countPanenTPHApproval: Int = 0  // Global variable for count
    private var counteSPBWBScanned: Int = 0  // Global variable for count
    private var countActiveESPB: Int = 0  // Global variable for count
    private var countHektarZero: Int = 0  // Global variable for count
    private var countScanMpanen: Int = 0  // Global variable for count
    private var countInspection: String = ""

    private val _globalLastSync = MutableLiveData<String>()
    private val globalLastSync: LiveData<String> get() = _globalLastSync

    private var activityInitialized = false

    private var hasShownErrorDialog = false  // Add this property
    private val permissionRequestCode = 1001
    private lateinit var adapter: DownloadProgressDatasetAdapter
    private val globalPanenIdsByPart = mutableMapOf<String, List<Int>>()
    private val globalEspbIdsByPart = mutableMapOf<String, List<Int>>()
    private val globalHektarPanenIdsByPart = mutableMapOf<String, List<Int>>()
    private val globalAbsensiPanenIdsByPart = mutableMapOf<String, List<Int>>()
    private var globalPanenIds: List<Int> = emptyList()
    private var globalESPBIds: List<Int> = emptyList()
    private var globalHektaranIds: List<Int> = emptyList()
    private var globalAbsensiIds: List<Int> = emptyList()

    private var zipFilePath: String? = null
    private var zipFileName: String? = null
    private var trackingIdsUpload: List<String> = emptyList()
    private var globalImageUploadError: List<String> = emptyList()
    private var globalImageNameError: List<String> = emptyList()

    // PDF-related variables
//    private lateinit var pdfManager: PDFManager
    private val pdfFileName = "User_Manual_CMP.pdf"
    private val pdfUrl =
        "https://cmp.citraborneo.co.id/apkcmp/MANUALBOOK/User_Manual_CMP.pdf" // Replace with your actual PDF URL
    private var isPdfDownloaded = false
    private var isDownloading = false

    data class ResponseJsonUpload(
        val trackingId: Int,
        val nama_file: String,
        val status: Int,
        val tanggal_upload: String,
        val type: String,
    )

    private val globalResponseJsonUploadList = mutableListOf<ResponseJsonUpload>()

    private lateinit var datasetViewModel: DatasetViewModel
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DataCleanupWorker.schedule(this)

        //cek tanggal otomatis
        checkDateTimeSettings()

        val appVersion = AppUtils.getAppVersion(this)

        // Initialize PDF manager
        initializePDFManager()
    }


    private fun initializePDFManager() {
        // Check if PDF is already downloaded locally
        val pdfFile = getLocalPDFFile(pdfFileName)
        isPdfDownloaded = pdfFile != null

        // Setup click listener for info icon
        try {
            val infoPatchNoteIcon = findViewById<TextView>(R.id.infoPatchNote)
            infoPatchNoteIcon?.setOnClickListener {
                handlePdfViewRequest()
            }
        } catch (e: Exception) {
            AppLogger.e("Info patch note icon not found: ${e.message}")
        }

        // Automatically start download if not already downloaded
        if (!isPdfDownloaded && !isDownloading) {
            downloadPdfSilently()
        }
    }

    private fun handlePdfViewRequest() {
        if (isPdfDownloaded) {
            // PDF is already downloaded, open it
            openPdfViewer()
        } else if (isDownloading) {
            // PDF is currently downloading
            Toast.makeText(this, "PDF sedang didownload, mohon tunggu...", Toast.LENGTH_SHORT)
                .show()
        } else {
            // Start download and then open after user clicks
            downloadPdfAndOpen()
        }
    }

    // Method baru untuk download tanpa auto-open (background download)
    private fun downloadPdfSilently() {
        if (!AppUtils.isNetworkAvailable(this)) {
            // Jika tidak ada internet, tidak perlu tampilkan error
            // Karena ini background download
            AppLogger.d("No internet connection for background PDF download")
            return
        }

        isDownloading = true

        Thread {
            try {
                val connection = URL(pdfUrl).openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val file = File(filesDir, pdfFileName)
                val outputStream = FileOutputStream(file)

                // Simple copy
                inputStream.copyTo(outputStream)

                outputStream.close()
                inputStream.close()

                runOnUiThread {
                    isDownloading = false
                    if (file.exists() && file.length() > 0) {
                        isPdfDownloaded = true
                        AppLogger.d("PDF downloaded successfully in background")
                        // Optional: Toast.makeText(this@HomePageActivity, "Patch notes siap dibaca", Toast.LENGTH_SHORT).show()
                    } else {
                        AppLogger.e("Background PDF download failed: File is empty")
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    isDownloading = false
                    AppLogger.e("Background PDF download failed: ${e.message}")
                }
            }
        }.start()
    }

    // Method untuk download dengan progress dialog dan auto-open setelah selesai
    private fun downloadPdfAndOpen() {
        if (!AppUtils.isNetworkAvailable(this)) {
            AlertDialogUtility.withSingleAction(
                this@HomePageActivity,
                stringXML(R.string.al_back),
                stringXML(R.string.al_no_internet_connection),
                stringXML(R.string.al_no_internet_connection_description_login),
                "network_error.json",
                R.color.colorRedDark
            ) { }
            return
        }

        isDownloading = true
        loadingDialog.show()

        Thread {
            try {
                val connection = URL(pdfUrl).openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val file = File(filesDir, pdfFileName)
                val outputStream = FileOutputStream(file)

                // Simple copy
                inputStream.copyTo(outputStream)

                outputStream.close()
                inputStream.close()

                runOnUiThread {
                    loadingDialog.dismiss()
                    isDownloading = false
                    if (file.exists() && file.length() > 0) {
                        isPdfDownloaded = true
                        openPdfViewer()
                    } else {
                        AlertDialogUtility.withSingleAction(
                            this@HomePageActivity,
                            stringXML(R.string.al_back),
                            "Download Failed",
                            "Downloaded file is empty or corrupted",
                            "warning.json",
                            R.color.colorRedDark
                        ) { }
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    isDownloading = false
                    AlertDialogUtility.withSingleAction(
                        this@HomePageActivity,
                        stringXML(R.string.al_back),
                        "Download Failed",
                        "Failed to download PDF: ${e.message}",
                        "warning.json",
                        R.color.colorRedDark
                    ) { }
                }
            }
        }.start()
    }


    private fun openPdfViewer() {
        val pdfFile = getLocalPDFFile(pdfFileName)
        if (pdfFile != null) {
            // Using afreakyelf PDF Viewer
            startActivity(
                PdfViewerActivity.launchPdfFromPath(
                    context = this,
                    path = pdfFile.absolutePath,
                    pdfTitle = "User Manual CMP",
                    saveTo = saveTo.ASK_EVERYTIME,

                    )
            )
        } else {
            Toast.makeText(this, "PDF file not found. Please download again.", Toast.LENGTH_SHORT)
                .show()
            isPdfDownloaded = false
        }
    }

    // Helper method to get local PDF file
    private fun getLocalPDFFile(fileName: String): File? {
        val file = File(filesDir, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    // Helper method to show download error
    private fun showDownloadError(message: String) {
        AlertDialogUtility.withSingleAction(
            this@HomePageActivity,
            stringXML(R.string.al_back),
            "Download Failed",
            message,
            "warning.json",
            R.color.colorRedDark
        ) { }
    }


    private fun fetchDataEachCard() {

        if (this::featureAdapter.isInitialized) {  // Changed to positive condition
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    featureAdapter.showLoadingForFeature("Rekap Hasil Panen")
                    delay(300)
                }
                try {
                    val countDeferred = async { panenViewModel.loadPanenCount() }
                    countPanenTPH = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount("Rekap Hasil Panen", countPanenTPH.toString())
                        featureAdapter.hideLoadingForFeature("Rekap Hasil Panen")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap Hasil Panen")
                    }
                }
                try {
                    val countDeferred = async { panenViewModel.loadPanenCountApproval() }
                    countPanenTPHApproval = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            "Rekap panen dan restan",
                            countPanenTPHApproval.toString()
                        )
                        featureAdapter.hideLoadingForFeature("Rekap panen dan restan")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap panen dan restan")
                    }
                }
                try {
                    val countDeferred = async { espbViewModel.getCountCreatedToday() }
                    countActiveESPB = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount("Rekap eSPB", countActiveESPB.toString())
                        featureAdapter.hideLoadingForFeature("Rekap eSPB")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap eSPB")
                    }
                }
                try {
                    val countDeferred =
                        async { hektarPanenViewModel.countWhereLuasPanenIsZeroAndDateToday() }
                    countHektarZero = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            AppUtils.ListFeatureNames.DaftarHektarPanen,
                            countHektarZero.toString()
                        )
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.DaftarHektarPanen)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.DaftarHektarPanen)
                    }
                }
                try {
                    val counteSPBWBDeferred = async { weightBridgeViewModel.getCountCreatedToday() }
                    counteSPBWBScanned = counteSPBWBDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            "Rekap e-SPB Timbangan Mill",
                            counteSPBWBScanned.toString().replace(" ", "")
                        )
                        featureAdapter.hideLoadingForFeature("Rekap e-SPB Timbangan Mill")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap e-SPB Timbangan Mill")
                    }
                }
                try {
                    withContext(Dispatchers.Main) {
                        featureAdapter.showLoadingForFeature(AppUtils.ListFeatureNames.RekapAbsensiPanen)
                    }
                    val countDeferredAbsensi = async { absensiViewModel.loadAbsensiCount() }
                    countAbsensi = countDeferredAbsensi.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(AppUtils.ListFeatureNames.RekapAbsensiPanen, countAbsensi.toString())
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.RekapAbsensiPanen)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching absensi data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.RekapAbsensiPanen)

                    }
                }
                try {
                    val countDeferred = async { inspectionViewModel.getInspectionCount(0) }
                    countInspection = countDeferred.await().toString()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            AppUtils.ListFeatureNames.RekapInspeksiPanen,
                            countInspection
                        )
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.RekapInspeksiPanen)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.RekapInspeksiPanen)
                    }
                }
                try {
                    val countDeferred = async { panenViewModel.getCountScanMPanen(0) }
                    countScanMpanen = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            AppUtils.ListFeatureNames.TransferHektarPanen,
                            countScanMpanen.toString()
                        )
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.TransferHektarPanen)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature(AppUtils.ListFeatureNames.TransferHektarPanen)
                    }
                }
            }
        } else {
            AppLogger.e("Feature adapter not initialized yet")
        }
    }


    private fun setupRecyclerView() {
        val features = listOf(
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.PanenTBS,
                featureNameBackgroundColor = R.color.greenBorder,
                iconResource = R.drawable.panen_tbs_icon,
                count = null,
                functionDescription = "Pencatatan panen TBS di TPH oleh kerani panen",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.RekapHasilPanen,
                featureNameBackgroundColor = R.color.yellowbutton,
                iconResource = null,
                count = countPanenTPH.toString(),
                functionDescription = "Rekapitulasi panen TBS dan transfer data ke supervisi",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.AsistensiEstateLain,
                featureNameBackgroundColor = R.color.bluedarklight,
                iconResource = R.drawable.panen_tbs_icon,
                count = null,
                functionDescription = "Asistensi pencatatan panen TBS ke estate lain",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.ScanHasilPanen,
                featureNameBackgroundColor = R.color.blueLightBorder,
                iconResource = R.drawable.scan_hasil_panen_icon,
                count = null,
                functionDescription = "Transfer data dari kerani panen ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.RekapPanenDanRestan,
                featureNameBackgroundColor = R.color.blueLightBorder,
                iconResource = null,
                count = countPanenTPHApproval.toString(),
                functionDescription = "Rekapitulsasi panen TBS dan restan dari kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.BuatESPB,
                featureNameBackgroundColor = R.color.yellowBorder,
                iconResource = R.drawable.espb_icon,
                functionDescription = "Transfer data dari driver ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.RekapESPB,
                featureNameBackgroundColor = R.color.yellowBorder,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi eSPB dan transfer data ke driver",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.InspeksiPanen,
                featureNameBackgroundColor = R.color.blueDarkborder,
                iconResource = R.drawable.inspeksi_icon,
                functionDescription = "Buat inspeksi panen",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.RekapInspeksiPanen,
                featureNameBackgroundColor = R.color.blueDarkborder,
                iconResource = null,
                count = countInspection,
                functionDescription = "Rekapitulasi inspeksi panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.ScanESPBTimbanganMill,
                featureNameBackgroundColor = R.color.yellowBorder,
                iconResource = R.drawable.timbang_icon,
                functionDescription = "Scan data eSPB dari driver oleh kerani timbang",
                displayType = DisplayType.ICON,
                subTitle = "Transfer data eSPB dari driver"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.RekapESPBTimbanganMill,
                featureNameBackgroundColor = R.color.colorRedDark,
                iconResource = R.drawable.cbi,
                functionDescription = "Rekapitulasi eSPB yang telah discan",
                displayType = DisplayType.COUNT,
                subTitle = "Transfer data eSPB dari driver"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.AbsensiPanen,
                featureNameBackgroundColor = R.color.greenBorder,
                iconResource = R.drawable.absensi_panen_icon,
                count = null,
                functionDescription = "Absensi kehadiran karyawan panen oleh supervisi",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.RekapAbsensiPanen,
                featureNameBackgroundColor = R.color.greenBorder,
                iconResource = null,
                count = countAbsensi.toString(),
                functionDescription = "Rekapitulasi absensi karyawan dan transfer data ke kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.ScanAbsensiPanen,
                featureNameBackgroundColor = R.color.greenBorder,
                iconResource = R.drawable.scan_qr_icon,
                count = null,
                functionDescription = "Transfer data abseni dari supervisi ke kerani panen",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.SinkronisasiData,
                featureNameBackgroundColor = R.color.toscaBorder,
                iconResource = R.drawable.sync_icon,
                functionDescription = "Update semua data master",
                displayType = DisplayType.ICON,
                subTitle = "Sinkronisasi data manual"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = AppUtils.ListFeatureNames.UploadDataCMP,
                featureNameBackgroundColor = R.color.colorRedDark,
                iconResource = R.drawable.upload_icon_2,
                functionDescription = "Upload semua data di aplikasi",
                displayType = DisplayType.ICON,
                subTitle = "Upload Semua Data CMP"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.ScanPanenMPanen,
                featureNameBackgroundColor = R.color.colorRedDark,
                iconResource = R.drawable.scan_hasil_panen_icon,
                count = null,
                functionDescription = "Transfer data dari kerani panen ke mandor panen untuk input hektar panen",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.DaftarHektarPanen,
                featureNameBackgroundColor = R.color.colorRedDark,
                iconResource = null,
                count = countPanenTPH.toString(),
                functionDescription = "Input dan upload hektar panen oleh mandor panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = AppUtils.ListFeatureNames.TransferHektarPanen,
                featureNameBackgroundColor = R.color.colorRedDark,
                iconResource = null,
                count = countPanenTPH.toString(),
                functionDescription = "Transfer data dari kerani panen ke mandor panen untuk input hektar panen",
                displayType = DisplayType.COUNT
            )
        )

        fun getFilteredFeaturesByJabatan(jabatan: String): List<FeatureCard> {
            val commonFeatures = listOf(
                features.find { it.featureName == AppUtils.ListFeatureNames.SinkronisasiData },
//                features.find { it.featureName == AppUtils.ListFeatureNames.UploadDataCMP }
            ).filterNotNull()

            // Determine which role pattern matches the jabatan
            val matchedRole = when {
                jabatan.contains(AppUtils.ListFeatureByRoleUser.KeraniPanen, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.KeraniPanen

                jabatan.contains(AppUtils.ListFeatureByRoleUser.KeraniTimbang, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.KeraniTimbang

                jabatan.contains(AppUtils.ListFeatureByRoleUser.Mandor1, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.Mandor1

                jabatan.contains(AppUtils.ListFeatureByRoleUser.Asisten, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.Asisten

                jabatan.contains(AppUtils.ListFeatureByRoleUser.MandorPanen, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.MandorPanen

                jabatan.contains(AppUtils.ListFeatureByRoleUser.IT, ignoreCase = true) ->
                    AppUtils.ListFeatureByRoleUser.IT

                else -> ""
            }

            val specificFeatures = when (matchedRole) {
                AppUtils.ListFeatureByRoleUser.KeraniPanen -> listOfNotNull(
                    features.find { it.featureName == AppUtils.ListFeatureNames.PanenTBS },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapHasilPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.AsistensiEstateLain },
                    features.find { it.featureName == AppUtils.ListFeatureNames.TransferHektarPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.InspeksiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapInspeksiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.ScanAbsensiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.UploadDataCMP }
                ).filterNotNull()

                AppUtils.ListFeatureByRoleUser.KeraniTimbang -> listOfNotNull(
                    features.find { it.featureName == AppUtils.ListFeatureNames.ScanESPBTimbanganMill },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapESPBTimbanganMill },
                )

                AppUtils.ListFeatureByRoleUser.Mandor1 -> listOfNotNull(
                    features.find { it.featureName == AppUtils.ListFeatureNames.ScanHasilPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan },
                    features.find { it.featureName == AppUtils.ListFeatureNames.BuatESPB },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapESPB },
                    features.find { it.featureName == AppUtils.ListFeatureNames.InspeksiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapInspeksiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.AbsensiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.UploadDataCMP }
                )

                AppUtils.ListFeatureByRoleUser.Asisten -> listOfNotNull(
                    features.find { it.featureName == AppUtils.ListFeatureNames.ScanHasilPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan },
                    features.find { it.featureName == AppUtils.ListFeatureNames.BuatESPB },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapESPB },
                    features.find { it.featureName == AppUtils.ListFeatureNames.InspeksiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapInspeksiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.AbsensiPanen },
//                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.UploadDataCMP },
                )

                AppUtils.ListFeatureByRoleUser.MandorPanen -> listOfNotNull(
                    features.find { it.featureName == AppUtils.ListFeatureNames.ScanPanenMPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.DaftarHektarPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.InspeksiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapInspeksiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.AbsensiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen },
                    features.find { it.featureName == AppUtils.ListFeatureNames.UploadDataCMP },

                    )

                AppUtils.ListFeatureByRoleUser.IT -> features

                else -> emptyList()
            }

            return if (matchedRole == AppUtils.ListFeatureByRoleUser.IT) {
                specificFeatures
            } else {
                specificFeatures + commonFeatures
            }
        }


        val gridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

        val jabatan = prefManager!!.jabatanUserLogin!!
        val filteredFeatures = getFilteredFeaturesByJabatan(jabatan)

        val tvCountFeatures = findViewById<TextView>(R.id.tvCountFeatures)
        tvCountFeatures.text = filteredFeatures.size.toString()

        binding.featuresRecyclerView.apply {
            layoutManager = gridLayoutManager
            featureAdapter = FeatureCardAdapter { featureCard ->
                onFeatureCardClicked(featureCard)
            }

            adapter = featureAdapter
            featureAdapter.setFeatures(filteredFeatures)

            post {
                fetchDataEachCard()
            }

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                    outRect.left = spacing
                    outRect.right = spacing
                    outRect.top = spacing
                    outRect.bottom = spacing
                }
            })
        }
    }

    private fun checkDateTimeSettings() {
        if (!AppUtils.isDateTimeValid(this)) {
            dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
            AppUtils.showDateTimeNetworkWarning(this)
        } else if (!activityInitialized) {
            initializeActivity()
            startPeriodicDateTimeChecking()
        }
    }

    private fun startPeriodicDateTimeChecking() {
        dateTimeCheckHandler.postDelayed(dateTimeCheckRunnable, AppUtils.DATE_TIME_INITIAL_DELAY)

    }

    override fun onResume() {
        super.onResume()
        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {
        super.onPause()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun setupUI() {
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)
        initViewModel()
        _globalLastSync.value = prefManager!!.lastSyncDate
        setupDownloadDialog()
        setupTitleAppNameAndVersion()
        setupName()
        setupLogout()
        checkPermissions()
        setupRecyclerView()
        setupCheckingAfterLogoutUser()
        prefManager!!.registeredDeviceUsername = prefManager!!.username
        panenViewModel.updateStatus.observeOnce(this) { success ->
            if (success) {
                AppLogger.d("✅ Panen Archive Updated Successfully")
            } else {
                AppLogger.e("❌ Panen Archive Update Failed")
            }
        }

        uploadCMPViewModel.updateStatusUploadCMP.observe(this) { (id, success) ->
            if (success) {
                AppLogger.d("✅ Upload Data with Tracking ID $id Inserted or Updated Successfully")
            } else {
                AppLogger.e("❌ Upload Data with Tracking ID $id Insertion Failed")
            }
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Keluar",
            getString(R.string.confirmation_dialog_title),
            getString(R.string.al_confirm_out),
            "warning.json",
            ContextCompat.getColor(this, R.color.bluedarklight),
            function = {

                finishAffinity()
            },
            cancelFunction = {
//                        backButton.isEnabled = true // Re-enable button when user cancels
            }
        )
    }

    private fun onFeatureCardClicked(feature: FeatureCard) {

        vibrate()
        when (feature.featureName) {
            AppUtils.ListFeatureNames.PanenTBS -> {
                if (feature.displayType == DisplayType.ICON) {
                    lifecycleScope.launch {
                        try {
                            // Get afdeling ID from preferences
                            val afdelingId = prefManager!!.afdelingIdUserLogin

                            if (afdelingId == "X" || afdelingId!!.isEmpty()) {
                                AlertDialogUtility.withSingleAction(
                                    this@HomePageActivity,
                                    "Kembali",
                                    "Afdeling tidak valid",
                                    "Data afdeling saat ini adalah $afdelingId",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {
                                    // Do nothing on click
                                }
                                return@launch
                            }

                            // Check afdeling data and sinkronisasi_otomatis
                            val afdelingDeferred = CompletableDeferred<AfdelingModel?>()

                            withContext(Dispatchers.IO) {
                                val afdelingData = datasetViewModel.getAfdelingById(afdelingId!!.toInt())
                                afdelingDeferred.complete(afdelingData)
                            }

                            val afdeling = afdelingDeferred.await()

                            if (afdeling == null) {
                                AlertDialogUtility.withSingleAction(
                                    this@HomePageActivity,
                                    "Kembali",
                                    "Data Afdeling Tidak Ditemukan",
                                    "Data afdeling tidak ditemukan di database lokal. Silakan sinkronisasi terlebih dahulu.",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {
                                    // Do nothing on click
                                }
                                return@launch
                            }

                            // Check sinkronisasi_otomatis value
                            if (afdeling.sinkronisasi_otomatis != 0) {
                                // Check if sinkronisasi is required
                                val lastSyncDateTime = prefManager!!.lastSyncDate
                                val lastSyncDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSyncDateTime)
                                )
                                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                                if (lastSyncDate != currentDate) {
                                    AlertDialogUtility.withSingleAction(
                                        this@HomePageActivity,
                                        "Kembali",
                                        "Sinkronisasi Database Diperlukan",
                                        "Database perlu disinkronisasi untuk hari ini. Silakan lakukan sinkronisasi terlebih dahulu sebelum menggunakan fitur ini.",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {
                                        // Do nothing on click
                                    }
                                    return@launch
                                }
                            }

                            // All validations passed, proceed with attendance check
                            val absensiDeferred = CompletableDeferred<List<AbsensiKemandoranRelations>>()

                            absensiViewModel.loadActiveAbsensi()
                            delay(100)

                            withContext(Dispatchers.Main) {
                                absensiViewModel.activeAbsensiList.observe(this@HomePageActivity) { absensiWithRelations ->
                                    val absensiData = absensiWithRelations ?: emptyList()
                                    absensiDeferred.complete(absensiData)
                                }
                            }

                            // Wait for absensi data
                            val absensiData = absensiDeferred.await()

                            // Extract all NIKs of present karyawan from all absensi entries
                            val presentNikSet = mutableSetOf<String>()
                            absensiData.forEach { absensiRelation ->
                                val absensi = absensiRelation.absensi
                                // Split the comma-separated NIK string and add each NIK to the set
                                val niks = absensi.karyawan_msk_nik.split(",")
                                presentNikSet.addAll(niks.filter {
                                    it.isNotEmpty() && it.trim().isNotEmpty()
                                })
                            }

                            val karyawanDeferred = CompletableDeferred<List<KaryawanModel>>()

                            panenViewModel.getAllKaryawan()
                            delay(100)

                            withContext(Dispatchers.Main) {
                                panenViewModel.allKaryawanList.observe(this@HomePageActivity) { list ->
                                    val allKaryawan = list ?: emptyList()

                                    // Only filter if presentNikSet has values
                                    val presentKaryawan = if (presentNikSet.isNotEmpty()) {
                                        // Filter to get only present karyawan
                                        allKaryawan.filter { karyawan ->
                                            karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                                        }
                                    } else {
                                        emptyList()
                                    }

                                    AppLogger.d("Total karyawan: ${allKaryawan.size}")
                                    AppLogger.d("Filtered to present karyawan: ${presentKaryawan.size}")

                                    // Complete the deferred with the filtered karyawan
                                    karyawanDeferred.complete(presentKaryawan)
                                }
                            }

                            // Wait for karyawan data
                            val presentKaryawan = karyawanDeferred.await()

                            // Check if presentKaryawan is empty
                            if (presentKaryawan.isEmpty()) {
                                // Show alert if no present karyawan found
                                AlertDialogUtility.withSingleAction(
                                    this@HomePageActivity,
                                    "Kembali",
                                    "Data Karyawan Tidak Hadir",
                                    "Tidak ditemukan data kehadiran karyawan untuk hari ini.\nSilakan melakukan scan QR Absensi dari Mandor Panen.",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {
                                    // Do nothing on click
                                }
                            } else {
                                // Present karyawan found, proceed to activity
                                val intent = Intent(
                                    this@HomePageActivity,
                                    FeaturePanenTBSActivity::class.java
                                )
                                intent.putExtra("FEATURE_NAME", feature.featureName)
                                startActivity(intent)
                            }

                        } catch (e: Exception) {
                            AppLogger.e("Error in validation checks: ${e.message}")
                            AlertDialogUtility.withSingleAction(
                                this@HomePageActivity,
                                "Kembali",
                                "Terjadi Kesalahan",
                                "Gagal melakukan validasi data. Silakan coba lagi.",
                                "error.json",
                                R.color.colorRedDark
                            ) {
                                // Do nothing on click
                            }
                        }
                    }
                }
            }

            AppUtils.ListFeatureNames.RekapHasilPanen -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListPanenTBSActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.AsistensiEstateLain -> {
                if (feature.displayType == DisplayType.ICON) {

                    AlertDialogUtility.withTwoActions(
                        this,
                        "Ya",
                        getString(R.string.confirmation_asistensi_estate),
                        getString(R.string.al_confirm_asistensi),
                        "warning.json",
                        ContextCompat.getColor(this, R.color.bluedarklight),
                        function = {
                            vibrate()
                            val intent = Intent(this, FeaturePanenTBSActivity::class.java)
                            intent.putExtra("FEATURE_NAME", feature.featureName)
                            startActivity(intent)
                        },
                        cancelFunction = { }
                    )

                }
            }

            AppUtils.ListFeatureNames.ScanHasilPanen -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanQR::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.BuatESPB -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanQR::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }


            AppUtils.ListFeatureNames.RekapESPB -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListHistoryESPBActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.RekapPanenDanRestan -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListPanenTBSActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.InspeksiPanen -> {
                if (feature.displayType == DisplayType.ICON) {
                    AlertDialogUtility.withTwoActions(
                        this,
                        "Unduh/Perbarui Data Panen",
                        getString(R.string.confirmation_dialog_title),
                        getString(R.string.al_confirm_download_data_panen_case_inspection),
                        "warning.json",
                        ContextCompat.getColor(this, R.color.bluedarklight),
                        cancelText = "Lanjutkan Isi Form Inspeksi",
                        function = {
                            isTriggerFeatureInspection = true
                            loadingDialog.show()
                            loadingDialog.setMessage("Sedang mempersiapkan data...")
                            lifecycleScope.launch {
                                try {
                                    delay(500)

                                    val isMandor1 = prefManager!!.jabatanUserLogin!!.contains(
                                        AppUtils.ListFeatureByRoleUser.Mandor1,
                                        ignoreCase = true
                                    )
                                    val isAsisten = prefManager!!.jabatanUserLogin!!.contains(
                                        AppUtils.ListFeatureByRoleUser.Asisten,
                                        ignoreCase = true
                                    )

                                    var previewDataPanenInspeksi = ""
                                    val estateIdString = prefManager!!.estateIdUserLogin!!.toInt()
                                    val afdelingIdString = prefManager!!.afdelingIdUserLogin

                                    // Validate afdelingId and get valid integer value
                                    var validAfdelingId: Int? = null

                                    if (isMandor1 || isAsisten) {
                                        AppLogger.d("Validation triggered for Mandor Panen or Asisten")

                                        // Check if afdelingId is null or empty
                                        if (afdelingIdString.isNullOrEmpty()) {
                                            AppLogger.d("afdelingId is null or empty - blocking user")
                                            withContext(Dispatchers.Main) {
                                                previewDataPanenInspeksi =
                                                    "Error: Afdeling ID tidak boleh kosong untuk ${prefManager!!.jabatanUserLogin}"
                                            }
                                        } else {
                                            // Try to convert afdelingId to integer
                                            validAfdelingId = try {
                                                afdelingIdString.toInt()
                                            } catch (e: NumberFormatException) {
                                                AppLogger.d("NumberFormatException caught: ${e.message}")
                                                null
                                            }

                                            // If conversion failed, set error message
                                            if (validAfdelingId == null) {
                                                AppLogger.d("afdelingId is not a valid integer - blocking user")
                                                withContext(Dispatchers.Main) {
                                                    previewDataPanenInspeksi =
                                                        "Error: Afdeling ID harus berupa angka yang valid untuk ${prefManager!!.jabatanUserLogin}. Afdeling ID saat ini: '$afdelingIdString'"
                                                }
                                            } else {
                                                AppLogger.d("Afdeling ID validation passed for ${prefManager!!.jabatanUserLogin}: $validAfdelingId")
                                            }
                                        }
                                    } else {
                                        AppLogger.d("Validation skipped - user is not Mandor 1 or Asisten")
                                    }

                                    // Only call API if user is Mandor1/Asisten AND afdelingId is valid
                                    if ((isMandor1 || isAsisten) && validAfdelingId != null) {
                                        // Create a deferred result that will be completed when data is received
                                        val dataPanenInspeksiDeffered =
                                            CompletableDeferred<String>()

                                        // Set up the observer for restanPreviewData
                                        val dataPanenObserver = Observer<String> { data ->
                                            if (!dataPanenInspeksiDeffered.isCompleted) {
                                                dataPanenInspeksiDeffered.complete(data)
                                            }
                                        }

                                        // Register the observer
                                        datasetViewModel.dataPanenInspeksiPreview.observe(
                                            this@HomePageActivity,
                                            dataPanenObserver
                                        )

                                        // Start the API request with validated afdelingId
                                        datasetViewModel.getPreviewDataPanenInspeksiWeek(
                                            estateIdString,
                                            validAfdelingId.toString()
                                        )

                                        try {
                                            // Wait for the API response with a timeout
                                            previewDataPanenInspeksi = withTimeout(15000) {
                                                dataPanenInspeksiDeffered.await()
                                            }

                                            AppLogger.d("previewDataPanenInspeksi $previewDataPanenInspeksi")

                                            // Remove the observer to prevent memory leaks
                                            withContext(Dispatchers.Main) {
                                                datasetViewModel.dataPanenInspeksiPreview.removeObserver(
                                                    dataPanenObserver
                                                )
                                            }
                                        } catch (e: Exception) {
                                            // Clean up observer in case of timeout or error
                                            withContext(Dispatchers.Main) {
                                                datasetViewModel.dataPanenInspeksiPreview.removeObserver(
                                                    dataPanenObserver
                                                )
                                            }
                                            AppLogger.e("Error getting restan data: ${e.message}")
                                            // Don't throw here, we'll continue with empty previewDataPanenInspeksi
                                        }
                                    } else {
                                        AppLogger.d("Skipping restan data fetch - either user is not Mandor1/Asisten or afdelingId is invalid")
                                    }

                                    loadingDialog.dismiss()
                                    withContext(Dispatchers.Main) {
                                        startDownloads(previewDataPanenInspeksi, "Unduh Data Panen")
                                    }
                                } catch (e: Exception) {
                                    // Handle any errors
                                    AppLogger.d("Loading estates failed: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        loadingDialog.dismiss()
                                        showErrorDialog("Error loading estates data: ${e.message}")
                                    }
                                }
                            }
                        },
                        cancelFunction = {
                            val intent = Intent(this, FormInspectionActivity::class.java)
                            intent.putExtra("FEATURE_NAME", feature.featureName)
                            startActivity(intent)
                        }
                    )

                }
            }

            AppUtils.ListFeatureNames.RekapInspeksiPanen -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListInspectionActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.AbsensiPanen -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, FeatureAbsensiActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.RekapAbsensiPanen -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListAbsensiActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.ScanAbsensiPanen -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanAbsensiActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.ScanESPBTimbanganMill -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanWeighBridgeActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.RekapESPBTimbanganMill -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListHistoryWeighBridgeActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.ScanESPBTimbanganMill -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanWeighBridgeActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanQR::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.DaftarHektarPanen -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListHistoryESPBActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.TransferHektarPanen -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, TransferHektarPanenActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            AppUtils.ListFeatureNames.SinkronisasiData -> {
                if (feature.displayType == DisplayType.ICON) {
                    if (AppUtils.isNetworkAvailable(this)) {
                        isTriggerButtonSinkronisasiData = true

                        // Show loading dialog
                        loadingDialog.show()
                        loadingDialog.setMessage("Sedang mempersiapkan data...")

                        lifecycleScope.launch {
                            try {
                                delay(500)
                                // Start data loading
                                datasetViewModel.getAllEstates()

                                val isMandor1 = prefManager!!.jabatanUserLogin!!.contains(
                                    AppUtils.ListFeatureByRoleUser.Mandor1,
                                    ignoreCase = true
                                )
                                val isAsisten = prefManager!!.jabatanUserLogin!!.contains(
                                    AppUtils.ListFeatureByRoleUser.Asisten,
                                    ignoreCase = true
                                )

                                var previewRestanData = ""
                                val estateIdString = prefManager!!.estateIdUserLogin!!.toInt()
                                val afdelingIdString = prefManager!!.afdelingIdUserLogin

                                // Add debug logging
                                AppLogger.d("User role: ${prefManager!!.jabatanUserLogin}")
                                AppLogger.d("afdelingIdString: $afdelingIdString")
                                AppLogger.d("isMandor1: $isMandor1")
                                AppLogger.d("isAsisten: $isAsisten")

                                // Validate afdelingId and get valid integer value
                                var validAfdelingId: Int? = null

                                if (isMandor1 || isAsisten) {
                                    AppLogger.d("Validation triggered for Mandor Panen or Asisten")

                                    // Check if afdelingId is null or empty
                                    if (afdelingIdString.isNullOrEmpty()) {
                                        AppLogger.d("afdelingId is null or empty - blocking user")
                                        withContext(Dispatchers.Main) {
                                            previewRestanData =
                                                "Error: Afdeling ID tidak boleh kosong untuk ${prefManager!!.jabatanUserLogin}"
                                        }
                                    } else {
                                        // Try to convert afdelingId to integer
                                        validAfdelingId = try {
                                            afdelingIdString.toInt()
                                        } catch (e: NumberFormatException) {
                                            AppLogger.d("NumberFormatException caught: ${e.message}")
                                            null
                                        }

                                        // If conversion failed, set error message
                                        if (validAfdelingId == null) {
                                            AppLogger.d("afdelingId is not a valid integer - blocking user")
                                            withContext(Dispatchers.Main) {
                                                previewRestanData =
                                                    "Error: Afdeling ID harus berupa angka yang valid untuk ${prefManager!!.jabatanUserLogin}. Afdeling ID saat ini: '$afdelingIdString'"
                                            }
                                        } else {
                                            AppLogger.d("Afdeling ID validation passed for ${prefManager!!.jabatanUserLogin}: $validAfdelingId")
                                        }
                                    }
                                } else {
                                    AppLogger.d("Validation skipped - user is not Mandor 1 or Asisten")
                                }

                                // Only call API if user is Mandor1/Asisten AND afdelingId is valid
                                if ((isMandor1 || isAsisten) && validAfdelingId != null) {
                                    // Create a deferred result that will be completed when data is received
                                    val restanDataDeferred = CompletableDeferred<String>()

                                    // Set up the observer for restanPreviewData
                                    val restanObserver = Observer<String> { data ->
                                        if (!restanDataDeferred.isCompleted) {
                                            restanDataDeferred.complete(data)
                                        }
                                    }

                                    // Register the observer
                                    datasetViewModel.restanPreviewData.observe(
                                        this@HomePageActivity,
                                        restanObserver
                                    )

                                    // Start the API request with validated afdelingId
                                    datasetViewModel.getPreviewDataRestanWeek(
                                        estateIdString,
                                        validAfdelingId.toString()
                                    )

                                    try {
                                        // Wait for the API response with a timeout
                                        previewRestanData = withTimeout(15000) {
                                            restanDataDeferred.await()
                                        }

                                        AppLogger.d("previewRestanData $previewRestanData")

                                        // Remove the observer to prevent memory leaks
                                        withContext(Dispatchers.Main) {
                                            datasetViewModel.restanPreviewData.removeObserver(
                                                restanObserver
                                            )
                                        }
                                    } catch (e: Exception) {
                                        // Clean up observer in case of timeout or error
                                        withContext(Dispatchers.Main) {
                                            datasetViewModel.restanPreviewData.removeObserver(
                                                restanObserver
                                            )
                                        }
                                        AppLogger.e("Error getting restan data: ${e.message}")
                                        // Don't throw here, we'll continue with empty previewRestanData
                                    }
                                } else {
                                    AppLogger.d("Skipping restan data fetch - either user is not Mandor1/Asisten or afdelingId is invalid")
                                }

                                // Wait for estates list with timeout
                                withTimeout(10000) {
                                    while (datasetViewModel.allEstatesList.value == null) {
                                        delay(100)
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    startDownloads(previewRestanData, "Sinkronisasi Data")
                                }
                            } catch (e: Exception) {
                                // Handle any errors
                                AppLogger.d("Loading estates failed: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    showErrorDialog("Error loading estates data: ${e.message}")
                                }
                            }
                        }
                    } else {
                        AlertDialogUtility.withSingleAction(
                            this@HomePageActivity,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_no_internet_connection),
                            stringXML(R.string.al_no_internet_connection_description_login),
                            "network_error.json",
                            R.color.colorRedDark
                        ) {
                            // Do nothing on click
                        }
                    }
                }
            }


            AppUtils.ListFeatureNames.UploadDataCMP -> {
                if (feature.displayType == DisplayType.ICON) {
                    if (AppUtils.isNetworkAvailable(this)) {


                        val isMandorPanen = prefManager!!.jabatanUserLogin!!.contains(
                            AppUtils.ListFeatureByRoleUser.MandorPanen,
                            ignoreCase = true
                        )

                        if (isMandorPanen) {
                            val indonesianDateFormat =
                                SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))

                            lifecycleScope.launch {
                                hektarPanenViewModel.getAllHektarPanen()
                                delay(100)

                                val dataDeferred = CompletableDeferred<List<HektarPanenEntity>>()
                                hektarPanenViewModel.hektarPanenTodayList.observe(this@HomePageActivity) { hektarPanenList ->
                                    dataDeferred.complete(hektarPanenList ?: emptyList())
                                }
                                val hektarPanenData = dataDeferred.await()

                                Log.d("HektarPanen", "all data count: ${hektarPanenData.size}")

                                // Get data with luas_panen = 0 and get distinct dates only
                                val zeroLuasPanenData =
                                    hektarPanenData.filter { it.luas_panen == 0f }

                                if (zeroLuasPanenData.isNotEmpty()) {
                                    // Found data with luas_panen = 0, don't run upload flow
                                    Log.d(
                                        "HektarPanen",
                                        "Cannot upload: Some data has luas_panen = 0"
                                    )

                                    // Extract distinct dates from date_created_panen and format to Indonesian
                                    val distinctDates = zeroLuasPanenData
                                        .flatMap { entity ->
                                            // Split by semicolon and extract date part from each datetime
                                            entity.date_created_panen.split(";")
                                                .map { datetime ->
                                                    val dateString = datetime.trim()
                                                        .split(" ")[0] // Get "2025-05-13"
                                                    try {
                                                        // Parse the date and format to Indonesian
                                                        val date = SimpleDateFormat(
                                                            "yyyy-MM-dd",
                                                            Locale.getDefault()
                                                        ).parse(dateString)
                                                        indonesianDateFormat.format(date!!)
                                                    } catch (e: Exception) {
                                                        dateString // Fallback to original if parsing fails
                                                    }
                                                }
                                        }
                                        .distinct() // Remove duplicates
                                        .joinToString(", ")

                                    // Create message with distinct dates only
                                    val detailMessage =
                                        "Terdapat data dengan luas panen 0 pada tanggal:\n\n$distinctDates\n\nMohon lengkapi luasan Hektar Panen terlebih dahulu."

                                    AlertDialogUtility.withSingleAction(
                                        this@HomePageActivity,
                                        "Kembali",
                                        "Hektar Panen Tidak Lengkap",
                                        detailMessage,
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {
                                        // Do nothing or navigate to edit screen
                                    }
                                } else {
                                    // All luas_panen values are > 0, proceed with upload
                                    Log.d("HektarPanen", "All data valid, proceeding with upload")
                                    runUploadDataFlow()
                                }
                            }
                        } else {
                            runUploadDataFlow()
                        }

                    } else {
                        AlertDialogUtility.withSingleAction(
                            this@HomePageActivity,
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
            }
        }
    }


    private fun runUploadDataFlow() {
        lifecycleScope.launch {
            loadingDialog.show()
            loadingDialog.setMessage("Sedang mengupdate data...")

            uploadCMPData = emptyList()
            uploadCMPViewModel.getAllIdsAndFilenames()
            delay(500)

            val dataDeferred = CompletableDeferred<List<Pair<String, String>>>()
            uploadCMPViewModel.allIdsAndFilenames.observe(this@HomePageActivity) { data ->
                dataDeferred.complete(data ?: emptyList()) // Ensure it's never null
            }
            val data = dataDeferred.await()

            //kode khusus untuk update UploadCMP sebelum melakukan upload
            uploadCMPData = data
            if (uploadCMPData.isNotEmpty()) {
                AppLogger.d("Starting update for ${uploadCMPData.size} items")
                val updateSuccessful =
                    datasetViewModel.updateLocalUploadCMP(
                        uploadCMPData,
                        prefManager!!.jabatanUserLogin!!
                    ).await()
                AppLogger.d("Update status: $updateSuccessful, now proceeding to file check")
            } else {
                AppLogger.d("No data to update")

            }


            val featuresToFetch = listOf(
                AppUtils.DatabaseTables.ESPB,
                AppUtils.DatabaseTables.PANEN
            )
            val combinedUploadData = mutableMapOf<String, Any>()
            lifecycleScope.launch {
                val panenDeferred =
                    CompletableDeferred<List<PanenEntityWithRelations>>()
                val espbDeferred = CompletableDeferred<List<ESPBEntity>>()
                val absensiDeferred =
                    CompletableDeferred<List<AbsensiKemandoranRelations>>()
                val hektarPanenDeferred =
                    CompletableDeferred<List<HektarPanenEntity>>()
                val zipDeferred = CompletableDeferred<Boolean>()

                panenViewModel.loadActivePanenESPBAll()
                delay(100)
                panenViewModel.activePanenList.observeOnce(this@HomePageActivity) { list ->
                    Log.d("UploadCheck", "Panen Data Size: ${list.size}")
                    panenDeferred.complete(
                        list ?: emptyList()
                    ) // Ensure it's never null
                }

                // Load ESPB Data
                weightBridgeViewModel.fetchActiveESPBAll()
                delay(100)
                weightBridgeViewModel.activeESPBUploadCMP.observeOnce(this@HomePageActivity) { list ->
                    Log.d("UploadCheck", "ESPB Data Received: ${list.size}")
                    espbDeferred.complete(
                        list ?: emptyList()
                    ) // Ensure it's never null
                }

                // Load absensi
                absensiViewModel.getAllData(0)
                delay(100)
                absensiViewModel.savedDataAbsensiList.observeOnce(this@HomePageActivity) { list ->
                    Log.d("UploadCheck", "Absensi Data Received: ${list.size}")
                    absensiDeferred.complete(
                        list ?: emptyList()
                    ) // Ensure it's never null
                }

                hektarPanenViewModel.loadHektarPanenData()
                delay(100)
                hektarPanenViewModel.historyHektarPanen.observeOnce(this@HomePageActivity) { list ->
                    Log.d("UploadCheck", "Absensi Data Received: ${list.size}")
                    hektarPanenDeferred.complete(
                        list ?: emptyList()
                    ) // Ensure it's never null
                }


                var unzippedPanenData: List<Map<String, Any>> = emptyList()
                var unzippedESPBData: List<Map<String, Any>> = emptyList()
                var unzippedHektaranData: List<Map<String, Any>> = emptyList()
                var unzippedAbsensiData: List<Map<String, Any>> = emptyList()

                var mappedPanenData: List<Map<String, Any>> = emptyList()
                var mappedESPBData: List<Map<String, Any>> = emptyList()

                var allPhotosPanen = mutableListOf<Map<String, String>>()
                var allPhotosAbsensi = mutableListOf<Map<String, String>>()
                var hektaranJson = ""
                var absensiJson = ""
                try {
                    val panenList = panenDeferred.await()
                    val espbList = espbDeferred.await()
                    val absensiList = absensiDeferred.await()
                    val hektarPanenList = hektarPanenDeferred.await()

                    // Prepare to search for photo files in CMP directories
                    val picturesDirs = listOf(
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        File(getExternalFilesDir(null)?.parent ?: "", "Pictures")
                    ).filterNotNull()

                    // Find all CMP directories upfront
                    val cmpDirectories = mutableListOf<File>()
                    for (picturesDir in picturesDirs) {
                        if (!picturesDir.exists() || !picturesDir.isDirectory) {
                            AppLogger.w("Pictures directory not found: ${picturesDir.absolutePath}")
                            continue
                        }

                        val dirs = picturesDir.listFiles { file ->
                            file.isDirectory && file.name.startsWith("CMP")
                        } ?: emptyArray()

                        cmpDirectories.addAll(dirs)
                    }

                    if (panenList.isNotEmpty()) {

                        val uniquePhotos =
                            mutableMapOf<String, Map<String, String>>()

                        // Prepare to search for photo files in CMP directories
                        val picturesDirs = listOf(
                            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            File(
                                getExternalFilesDir(null)?.parent ?: "",
                                "Pictures"
                            )
                        ).filterNotNull()

                        // Find all CMP directories upfront
                        val cmpDirectories = mutableListOf<File>()
                        for (picturesDir in picturesDirs) {
                            if (!picturesDir.exists() || !picturesDir.isDirectory) {
                                AppLogger.w("Pictures directory not found: ${picturesDir.absolutePath}")
                                continue
                            }

                            // Look specifically for CMP-PANEN TPH directory
                            val cmpPanenDir = File(picturesDir, "CMP-PANEN TPH")
                            if (cmpPanenDir.exists() && cmpPanenDir.isDirectory) {
                                cmpDirectories.add(cmpPanenDir)
                            }

                            // Also check for any other CMP directories
                            val otherCmpDirs = picturesDir.listFiles { file ->
                                file.isDirectory && file.name.startsWith("CMP") && file.name != "CMP-PANEN TPH"
                            } ?: emptyArray()

                            cmpDirectories.addAll(otherCmpDirs)
                        }

                        AppLogger.d("Found ${cmpDirectories.size} CMP directories")

                        mappedPanenData = panenList.map { panenWithRelations ->

                            val photoNames =
                                panenWithRelations.panen.foto?.split(";")
                                    ?: listOf()

                            // Process each photo in the semicolon-separated list
                            for (photoName in photoNames) {
                                val trimmedName = photoName.trim()
                                if (trimmedName.isEmpty()) continue

                                if (trimmedName in uniquePhotos) continue

                                val uploadStatusImage =
                                    panenWithRelations.panen.status_uploaded_image

                                // Skip only if status is 200 (fully uploaded)
                                if (uploadStatusImage == "200") {
                                    AppLogger.d("Skipping photo $trimmedName - record ${panenWithRelations.panen.id} fully uploaded (status 200)")
                                    continue
                                }

                                var photoFound = false

                                for (cmpDir in cmpDirectories) {
                                    val photoFile = File(cmpDir, trimmedName)

                                    if (photoFile.exists() && photoFile.isFile) {
                                        // Add to uniquePhotos if:
                                        // 1. status is 0 (not uploaded yet)
                                        // 2. status is error JSON and this photo is in the error array
                                        var shouldAdd = false

                                        if (uploadStatusImage == "0") {
                                            // Default status - hasn't been uploaded yet
                                            shouldAdd = true
                                            AppLogger.d("Photo $trimmedName hasn't been uploaded (status 0)")
                                        } else if (uploadStatusImage.startsWith("{")) {
                                            try {
                                                // Using Gson to parse the JSON
                                                val errorJson = Gson().fromJson(
                                                    uploadStatusImage,
                                                    JsonObject::class.java
                                                )
                                                val errorArray =
                                                    errorJson?.get("error")?.asJsonArray

                                                errorArray?.forEach { errorItem ->
                                                    if (errorItem.asString == trimmedName) {
                                                        shouldAdd = true
                                                        AppLogger.d("Photo $trimmedName is marked as error in record ${panenWithRelations.panen.id}")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                AppLogger.e("Error parsing upload status JSON: ${e.message}")
                                            }
                                        }

                                        val createdDate =
                                            panenWithRelations.panen.date_created
                                                ?: ""
                                        val formattedDate = try {
                                            val dateFormat = SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()
                                            )
                                            val date = dateFormat.parse(createdDate)
                                            val outputFormat = SimpleDateFormat(
                                                "yyyy/MM/dd/",
                                                Locale.getDefault()
                                            )
                                            outputFormat.format(date ?: Date())
                                        } catch (e: Exception) {
                                            AppLogger.e("Error formatting date: ${e.message}")
                                            // Default to current date if parsing fails
                                            val outputFormat = SimpleDateFormat(
                                                "yyyy/MM/dd/",
                                                Locale.getDefault()
                                            )
                                            outputFormat.format(Date())
                                        }

                                        // Create the base path by appending the estate code
                                        val basePathImage =
                                            formattedDate + prefManager!!.estateUserLogin

                                        if (shouldAdd) {
                                            uniquePhotos[trimmedName] = mapOf(
                                                "name" to trimmedName,
                                                "path" to photoFile.absolutePath,
                                                "size" to photoFile.length()
                                                    .toString(),
                                                "table_ids" to panenWithRelations.panen.id.toString(),
                                                "base_path" to basePathImage,
                                                "database" to AppUtils.DatabaseTables.PANEN
                                            )
                                            AppLogger.d("Added photo for upload: $trimmedName at ${photoFile.absolutePath}")
                                        } else {
                                            AppLogger.d("Skipping photo $trimmedName - no upload needed")
                                        }

                                        photoFound = true
                                        break
                                    }
                                }

                                if (!photoFound) {
                                    AppLogger.w("Photo not found: $trimmedName")
                                }
                            }

                            //handle kemandoran_field
                            val kemandoranId =
                                prefManager!!.kemandoranUserLogin.toString()
                            val kemandoranPPRO =
                                prefManager!!.kemandoranPPROUserLogin.toString()
                            val kemandoranKode =
                                prefManager!!.kemandoranKodeUserLogin.toString()
                            val kemandoranNama =
                                prefManager!!.kemandoranNamaUserLogin.toString()

                            val pemanen = mutableListOf<Map<String, String>>()
                            val nikList =
                                panenWithRelations.panen.karyawan_nik?.split(",")
                                    ?: listOf()


                            AppLogger.d(panenWithRelations.panen.karyawan_nama.toString())
                            val namaList =
                                panenWithRelations.panen.karyawan_nama?.split(",")
                                    ?: listOf()

                            AppLogger.d(namaList.toString())

                            for (i in nikList.indices) {
                                if (i < namaList.size) {
                                    pemanen.add(
                                        mapOf(
                                            "nik" to nikList[i].trim(),
                                            "nama" to namaList[i].trim()
                                        )
                                    )
                                } else {
                                    // In case there are more NIKs than names
                                    pemanen.add(
                                        mapOf(
                                            "nik" to nikList[i].trim(),
                                            "nama" to ""
                                        )
                                    )
                                }
                            }

                            val kemandoranJsonMap = mutableMapOf<String, Any>()
                            kemandoranJsonMap["id"] =
                                kemandoranPPRO.toIntOrNull() ?: 0
                            kemandoranJsonMap["kode"] = kemandoranKode
                            kemandoranJsonMap["nama"] = kemandoranNama
                            kemandoranJsonMap["pemanen"] = pemanen

                            val kemandoranJson = listOf(kemandoranJsonMap)
                            val kemandoranJsonString = Gson().toJson(kemandoranJson)
                            val jumlahPemanen = pemanen.size


                            val createdDate =
                                panenWithRelations.panen.date_created ?: ""
                            val formattedDate = try {
                                val dateFormat = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                )
                                val date = dateFormat.parse(createdDate)
                                val outputFormat = SimpleDateFormat(
                                    "yyyy/MM/dd",
                                    Locale.getDefault()
                                )
                                outputFormat.format(date ?: Date())
                            } catch (e: Exception) {
                                AppLogger.e("Error formatting date: ${e.message}")
                                // Default to current date if parsing fails
                                val outputFormat = SimpleDateFormat(
                                    "yyyy/MM/dd",
                                    Locale.getDefault()
                                )
                                outputFormat.format(Date())
                            }

                            // handle foto dengan path tanggal
                            val basePath =
                                "$formattedDate/${prefManager!!.estateUserLogin}/"

                            // Process the photo filenames to prepend the base path
                            val originalFotoString =
                                panenWithRelations.panen.foto ?: ""
                            val modifiedFotoString =
                                if (originalFotoString.contains(";")) {
                                    // Multiple photos - split, modify each one, and rejoin
                                    originalFotoString.split(";")
                                        .map { photoName -> "$basePath${photoName.trim()}" }
                                        .joinToString(";")
                                } else if (originalFotoString.isNotEmpty()) {
                                    // Single photo - just prepend the base path
                                    "$basePath$originalFotoString"
                                } else {
                                    // No photos
                                    ""
                                }

                            mapOf(
                                "id" to panenWithRelations.panen.id,
                                "tanggal" to panenWithRelations.panen.date_created,
                                "jjg_json" to panenWithRelations.panen.jjg_json,
                                "tipe" to panenWithRelations.panen.jenis_panen,
                                "created_by" to prefManager!!.idUserLogin.toString(),
                                "created_name" to prefManager!!.nameUserLogin.toString(),
                                "created_date" to panenWithRelations.panen.date_created,
                                "jabatan" to prefManager!!.jabatanUserLogin.toString(),
                                "status_pengangkutan" to panenWithRelations.panen.status_pengangkutan,
                                "regional" to panenWithRelations.tph?.regional.toString(),
                                "wilayah" to panenWithRelations.tph?.wilayah.toString(),
                                "company" to panenWithRelations.tph?.company.toString(),
                                "company_abbr" to panenWithRelations.tph?.company_abbr.toString(),
                                "company_nama" to panenWithRelations.tph?.company_nama.toString(),
                                "dept" to panenWithRelations.tph?.dept.toString(),
                                "dept_ppro" to panenWithRelations.tph?.dept_ppro.toString(),
                                "dept_abbr" to panenWithRelations.tph?.dept_abbr.toString(),
                                "dept_nama" to panenWithRelations.tph?.dept_nama.toString(),
                                "divisi" to panenWithRelations.tph?.divisi.toString(),
                                "divisi_abbr" to panenWithRelations.tph?.divisi_abbr.toString(),
                                "divisi_ppro" to panenWithRelations.tph?.divisi_ppro.toString(),
                                "divisi_nama" to panenWithRelations.tph?.divisi_nama.toString(),
                                "blok" to panenWithRelations.tph?.blok.toString(),
                                "blok_ppro" to panenWithRelations.tph?.blok_ppro.toString(),
                                "blok_kode" to panenWithRelations.tph?.blok_kode.toString(),
                                "blok_nama" to panenWithRelations.tph?.blok_nama.toString(),
                                "tph" to (panenWithRelations.tph?.id ?: 0) as Int,
                                "tph_nomor" to (panenWithRelations.tph?.nomor
                                    ?: ""),
                                "ancak" to panenWithRelations.panen.ancak,
                                "asistensi" to if ((panenWithRelations.panen.asistensi as? Int) == 0) 1 else 2,
                                "kemandoran_id" to panenWithRelations.panen.kemandoran_id,
                                "karyawan_id" to panenWithRelations.panen.karyawan_id,
                                "karyawan_nik" to panenWithRelations.panen.karyawan_nik,
                                "foto" to modifiedFotoString,
                                "komentar" to panenWithRelations.panen.komentar,
                                "lat" to panenWithRelations.panen.lat,
                                "lon" to panenWithRelations.panen.lon,
                                "status_banjir" to panenWithRelations.panen.status_banjir,
                                "status_pengangkutan" to panenWithRelations.panen.status_pengangkutan,
                                "app_version" to AppUtils.getDeviceInfo(this@HomePageActivity)
                                    .toString(),
                                "kemandoran_user" to kemandoranNama,
                                "kemandoran_user_kode" to kemandoranKode,
                                "kemandoran_user_id" to kemandoranId,
                                "kemandoran_ppro" to kemandoranPPRO,
                                "kemandoran" to kemandoranJsonString,
                                "jumlah_pemanen" to jumlahPemanen,
                                "restan" to 0,
                                "status_espb" to 0
                            )
                        }

                        // First, filter the mapped panen data to only include items with status_upload == 0
                        val panenDataToUpload = mappedPanenData.filter { panenMap ->
                            val id = panenMap["id"] as? Int ?: 0
                            val original = panenList.find { it.panen.id == id }

                            // Only include if status_upload == 0
                            original?.panen?.status_upload == 0
                        }


                        // Only create the panen JSON file if there's data to upload
                        if (panenDataToUpload.isNotEmpty()) {
                            // Split into batches of 50
                            val panenBatches = panenDataToUpload.chunked(50)
                            val panenBatchMap = mutableMapOf<String, Any>()

                            panenBatches.forEachIndexed { batchIndex, batch ->
                                // Create a wrapper with the table name for this batch
                                val wrappedBatch = mapOf(
                                    AppUtils.DatabaseTables.PANEN to batch
                                )

                                // Convert the wrapped batch to JSON
                                val batchJson = Gson().toJson(wrappedBatch)
                                val batchKey = "batch_${batchIndex + 1}"

                                // Store the IDs for this batch
                                val batchIds = batch.mapNotNull { it["id"] as? Int }

                                // Create filename - remove batch reference if only one batch exists
                                val filename = if (panenBatches.size == 1) {
                                    "Data Panen ${prefManager!!.estateUserLogin}"
                                } else {
                                    "Data Panen ${prefManager!!.estateUserLogin} batch ${batchIndex + 1}"
                                }
//                                AppUtils.clearTempJsonFiles(this@HomePageActivity)
//                                try {
//                                    val tempDir = File(getExternalFilesDir(null), "TEMP").apply {
//                                        if (!exists()) mkdirs()
//                                    }
//
//                                    val filename = "panen_data_${System.currentTimeMillis()}.json"
//                                    val tempFile = File(tempDir, filename)
//
//                                    FileOutputStream(tempFile).use { fos ->
//                                        fos.write(batchJson.toByteArray())
//                                    }
//
//                                    AppLogger.d("Saved raw absensi data to temp file: ${tempFile.absolutePath}")
//                                } catch (e: Exception) {
//                                    AppLogger.e("Failed to save absensi data to temp file: ${e.message}")
//                                    e.printStackTrace()
//                                }


                                panenBatchMap[batchKey] = mapOf(
                                    "data" to batchJson,
                                    "filename" to filename,
                                    "ids" to batchIds
                                )
                            }


                            if (panenBatchMap.isNotEmpty()) {
                                combinedUploadData[AppUtils.DatabaseTables.PANEN] =
                                    panenBatchMap
                            }
                        }




                        allPhotosPanen = uniquePhotos.values.toMutableList()
                        if (allPhotosPanen.isNotEmpty()) {
                            AppLogger.d("Adding ${allPhotosPanen.size} unique photos to upload data")
                            combinedUploadData["foto_panen"] = allPhotosPanen

                        } else {
                            AppLogger.w("No photos found to upload")
                        }

                        unzippedPanenData = mappedPanenData.filter { item ->
                            // Get the ID
                            val id = item["id"] as? Int ?: 0

                            // Check if this item has dataIsZipped = 0 in the original data
                            val original = panenList.find { it.panen.id == id }
                            val isZipped = original?.panen?.dataIsZipped ?: 0

                            // Only include items that are not yet zipped
                            isZipped == 0
                        }

                        globalPanenIds = unzippedPanenData.mapNotNull { item ->
                            item["id"] as? Int
                        }
                    }

                    if (espbList.isNotEmpty()) {
                        val espbDataToUpload = espbList.filter { data ->
                            data.status_upload_cmp_sp == 0
                        }

                        if (espbDataToUpload.isNotEmpty()) {
                            mappedESPBData = espbDataToUpload.map { data ->
                                val blokJjgList =
                                    data.blok_jjg.split(";").mapNotNull {
                                        it.split(",").takeIf { it.size == 2 }
                                            ?.let { (id, jjg) ->
                                                id.toIntOrNull()
                                                    ?.let { it to jjg.toIntOrNull() }
                                            }
                                    }
                                val idBlokList = blokJjgList.map { it.first }
                                val totalJjg =
                                    blokJjgList.mapNotNull { it.second }.sum()
                                val concatenatedIds =
                                    idBlokList.joinToString(",").trimEnd(',')
                                val firstBlockId = idBlokList.firstOrNull()

                                // Create a CompletableDeferred to handle the async operation
                                val tphDeferred =
                                    CompletableDeferred<TPHNewModel?>()


                                AppLogger.d("firstBlockId $firstBlockId")
                                // Fetch the TPH data if we have a block ID
                                firstBlockId?.let { blockId ->
                                    weightBridgeViewModel.fetchTPHByBlockId(blockId)

                                    // Set up a one-time observer for the LiveData
                                    weightBridgeViewModel.tphData.observeOnce(this@HomePageActivity) { tphModel ->
                                        tphDeferred.complete(tphModel)
                                    }
                                }
                                    ?: tphDeferred.complete(null) // Complete with null if no block ID

                                // Wait for the TPH data
                                val tphData = tphDeferred.await()


                                AppLogger.d("tphData $tphData")

                                val pemuatNikString = data.pemuat_nik

                                val nikList = mutableListOf<String>()
                                var currentIndex = 0

                                while (true) {
                                    // Find next occurrence of "nik="
                                    val nikIndex = pemuatNikString.indexOf(
                                        "nik=",
                                        currentIndex
                                    )
                                    if (nikIndex == -1) break // No more NIKs found

                                    // Move position after "nik="
                                    currentIndex = nikIndex + 4

                                    // Find comma after the NIK value
                                    val commaIndex =
                                        pemuatNikString.indexOf(",", currentIndex)
                                    if (commaIndex == -1) break // Unexpected format

                                    // Extract the NIK value
                                    val nikValue = pemuatNikString.substring(
                                        currentIndex,
                                        commaIndex
                                    )
                                    nikList.add(nikValue)

                                    // Move position for next search
                                    currentIndex = commaIndex + 1
                                }

                                val nikValues = nikList.joinToString(",")

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
                                    "pemuat_nik" to nikValues,
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
                                    "app_version" to AppUtils.getDeviceInfo(this@HomePageActivity)
                                        .toString(),
                                    "jabatan" to prefManager!!.jabatanUserLogin.toString(),
                                )
                            }




                            if (espbDataToUpload.isNotEmpty()) {
                                // Create a wrapper with the table name
                                val wrappedData = mapOf(
                                    AppUtils.DatabaseTables.ESPB to mappedESPBData
                                )


                                // Convert to JSON
                                val espbJson = Gson().toJson(wrappedData)
//                                                AppLogger.d("espbJson $espbJson")
//
//                                                try {
//                                                    val tempDir =
//                                                        File(getExternalFilesDir(null), "TEMP").apply {
//                                                            if (!exists()) mkdirs()
//                                                        }
//
//                                                    val filename =
//                                                        "espb_data_${System.currentTimeMillis()}.json"
//                                                    val tempFile = File(tempDir, filename)
//
//                                                    FileOutputStream(tempFile).use { fos ->
//                                                        fos.write(espbJson.toByteArray())
//                                                    }
//
//                                                    AppLogger.d("Saved raw espb data to temp file: ${tempFile.absolutePath}")
//                                                } catch (e: Exception) {
//                                                    AppLogger.e("Failed to save espb data to temp file: ${e.message}")
//                                                    e.printStackTrace()
//                                                }
                                AppLogger.d(espbJson.toString())
                                // Extract all IDs
                                val espbIds = ArrayList<Int>()
                                for (item in mappedESPBData) {
                                    try {
                                        val jsonObj =
                                            JSONObject(Gson().toJson(item))
                                        val id = jsonObj.optInt("id", 0)
                                        if (id > 0) {
                                            espbIds.add(id)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error extracting ESPB ID: ${e.message}")
                                    }
                                }

                                unzippedESPBData = mappedESPBData.filter { item ->
                                    // Get the ID
                                    val id = item["id"] as? Int ?: 0

                                    // Check if this item has dataIsZipped = 0 in the original data
                                    val original = espbList.find { it.id == id }
                                    val isZipped = original?.dataIsZipped ?: 0

                                    // Only include items that are not yet zipped
                                    isZipped == 0
                                }

                                globalESPBIds = unzippedESPBData.mapNotNull { item ->
                                    item["id"] as? Int
                                }


                                combinedUploadData[AppUtils.DatabaseTables.ESPB] =
                                    mapOf(
                                        "data" to espbJson,
                                        "filename" to "espb_data.json",
                                        "ids" to globalESPBIds
                                    )
                            }

                        } else {
                            AppLogger.d("No ESPB data with status_upload == 0 to upload")
                            // Initialize empty arrays if no data to upload
                            mappedESPBData = emptyList()
                            globalESPBIds = emptyList()
                            unzippedESPBData = emptyList()
                        }
                    }
//                                    AppUtils.clearTempJsonFiles(this@HomePageActivity)
                    if (hektarPanenList.isNotEmpty()) {
                        val hektarPanenToUpload = hektarPanenList.filter { data ->
                            data.status_upload == 0
                        }

                        if (hektarPanenToUpload.isNotEmpty()) {
                            // Process data for HEKTARAN (summary by date AND blok)
                            val groupedByDateAndBlok = hektarPanenToUpload.groupBy { data ->
                                // Extract first date from date_created_panen
                                val firstDate = data.date_created_panen.split(";").firstOrNull() ?: ""
                                val dateOnly = if (firstDate.isNotEmpty()) {
                                    try {
                                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val date = inputFormat.parse(firstDate)
                                        date?.let { outputFormat.format(it) } ?: firstDate.split(" ")[0]
                                    } catch (e: Exception) {
                                        firstDate.split(" ")[0] // fallback to first part before space
                                    }
                                } else {
                                    data.date_created?.split(" ")?.get(0) ?: ""
                                }

                                // Create composite key: date + blok
                                "${dateOnly}_${data.blok}"
                            }

                            AppLogger.d("Grouped by date and blok:")
                            groupedByDateAndBlok.forEach { (key, dataList) ->
                                AppLogger.d("Key: $key, Count: ${dataList.size}")
                            }

                            // Create a list to hold our restructured data (with nested children)
                            val restructuredData = groupedByDateAndBlok.map { (compositeKey, dataList) ->
                                // Extract date and blok from composite key
                                val (dateOnly, blokId) = compositeKey.split("_").let {
                                    it[0] to it[1].toIntOrNull()
                                }

                                // Get first item to extract common properties
                                val firstItem = dataList.first()

                                // Calculate luasan_panen sum for this date+blok combination
                                val totalLuasanPanen = dataList.sumOf { it.luas_panen.toDouble() }.toFloat()

                                // Count distinct pemanen_nama for this date+blok combination
                                val distinctPemanen = dataList.map { it.pemanen_nama }.distinct().size

                                // Create a structure for this date+blok with its child details
                                val hektaranData = mutableMapOf<String, Any>(
                                    "tanggal" to dateOnly,
                                    "regional" to (firstItem.regional ?: ""),
                                    "wilayah" to (firstItem.wilayah ?: ""),
                                    "company" to (firstItem.company ?: 0),
                                    "company_abbr" to (firstItem.company_abbr ?: ""),
                                    "company_nama" to (firstItem.company_nama ?: ""),
                                    "dept" to (firstItem.dept ?: 0),
                                    "dept_ppro" to (firstItem.dept_ppro ?: 0),
                                    "dept_abbr" to (firstItem.dept_abbr ?: ""),
                                    "dept_nama" to (firstItem.dept_nama ?: ""),
                                    "divisi" to (firstItem.divisi ?: 0),
                                    "divisi_abbr" to (firstItem.divisi_abbr ?: ""),
                                    "divisi_nama" to (firstItem.divisi_nama ?: ""),
                                    "blok" to (blokId ?: 0),
                                    "blok_ppro" to (firstItem.blok_ppro ?: 0),
                                    "blok_kode" to (firstItem.blok_kode ?: 0),
                                    "blok_nama" to (firstItem.blok_nama ?: ""),
                                    "luasan_blok" to (firstItem.luas_blok ?: ""),
                                    "luasan_panen" to totalLuasanPanen,
                                    "jumlah_pemanen" to distinctPemanen,
                                    "created_name" to "",
                                    "created_by" to (firstItem.created_by ?: ""),
                                    "created_date" to (firstItem.date_created ?: ""),
                                    "app_version" to AppUtils.getDeviceInfo(this@HomePageActivity).toString(),
                                )

                                // Process detail records for this date+blok combination
                                val detailRecords = mutableListOf<Map<String, Any>>()

                                // Process all data items for this date+blok combination
                                for (data in dataList) {
                                    // Split all arrays
                                    val tphIdsList = data.tph_ids.split(";")
                                    val totalJjgList = data.total_jjg_arr.split(";")
                                    val unripeList = data.unripe_arr.split(";")
                                    val overripeList = data.overripe_arr.split(";")
                                    val emptyBunchList = data.empty_bunch_arr.split(";")
                                    val abnormalList = data.abnormal_arr.split(";")
                                    val ripeList = data.ripe_arr.split(";")
                                    val kirimList = data.kirim_pabrik_arr.split(";")
                                    val dibayarList = data.dibayar_arr.split(";")
                                    val dateCreatedPanenList = data.date_created_panen.split(";")

                                    AppLogger.d("Processing data for ${data.pemanen_nama} - Date: $dateOnly, Blok: $blokId")
                                    AppLogger.d("TPH IDs: $tphIdsList")
                                    AppLogger.d("Total JJG: $totalJjgList")

                                    // Get kemandoran data (keeping your existing code)
                                    val kemandoranDeferred = CompletableDeferred<List<KemandoranModel>>()
                                    val kemandoranIds = listOf(data.kemandoran_id ?: "")

                                    if (kemandoranIds.first().isNotEmpty()) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val kemandoranList = absensiViewModel.getKemandoranById(kemandoranIds)
                                                kemandoranDeferred.complete(kemandoranList)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching kemandoran data: ${e.message}")
                                                kemandoranDeferred.complete(emptyList())
                                            }
                                        }
                                    } else {
                                        kemandoranDeferred.complete(emptyList())
                                    }

                                    val kemandoranList = try {
                                        kemandoranDeferred.await()
                                    } catch (e: Exception) {
                                        AppLogger.e("Error waiting for kemandoran data: ${e.message}")
                                        emptyList()
                                    }

                                    val kemandoranPpro = if (kemandoranList.isNotEmpty()) {
                                        kemandoranList.first().kemandoran_ppro ?: ""
                                    } else {
                                        ""
                                    }

                                    val kemandoranKode = if (kemandoranList.isNotEmpty()) {
                                        kemandoranList.first().kode ?: ""
                                    } else {
                                        ""
                                    }

                                    // Initialize totals for THIS data item
                                    var totalJjgPanen = 0.0
                                    var totalJjgMentah = 0.0
                                    var totalJjgLewatMasak = 0.0
                                    var totalJjgKosong = 0.0
                                    var totalJjgAbnormal = 0.0
                                    var totalJjgMasak = 0.0
                                    var totalJjgKirim = 0.0
                                    var totalJjgBayar = 0.0
                                    val dateCreatedArray = mutableListOf<String>()
                                    val tphArray = mutableListOf<String>()
                                    val entryCount = tphIdsList.size

                                    // Sum all JJG values for this data item
                                    for (i in 0 until entryCount) {
                                        if (i < tphIdsList.size && tphIdsList[i].isNotEmpty()) {
                                            // Sum all JJG values
                                            totalJjgPanen += (if (i < totalJjgList.size) totalJjgList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgMentah += (if (i < unripeList.size) unripeList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgLewatMasak += (if (i < overripeList.size) overripeList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgKosong += (if (i < emptyBunchList.size) emptyBunchList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgAbnormal += (if (i < abnormalList.size) abnormalList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgMasak += (if (i < ripeList.size) ripeList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgKirim += (if (i < kirimList.size) kirimList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            totalJjgBayar += (if (i < dibayarList.size) dibayarList[i].toDoubleOrNull() ?: 0.0 else 0.0)
                                            tphArray.add(tphIdsList[i])

                                            // Collect date_created values
                                            val dateCreated = if (i < dateCreatedPanenList.size) dateCreatedPanenList[i] else data.date_created
                                            if (dateCreated != null && dateCreated.isNotEmpty()) {
                                                dateCreatedArray.add(dateCreated)
                                            }
                                        }
                                    }

                                    // Debug log to see what values we're getting
                                    AppLogger.d("Data item: ${data.nik}, Total JJG Panen: $totalJjgPanen")
                                    AppLogger.d("Creating hektaran_detail for Date: $dateOnly, Blok: $blokId, Pemanen: ${data.pemanen_nama}")

                                    // Create single entry with summed values for this data item
                                    detailRecords.add(
                                        mapOf<String, Any>(
                                            "tipe" to "",
                                            "blok" to (data.blok ?: 0),
                                            "kemandoran_id" to (data.kemandoran_id ?: ""),
                                            "kemandoran_nama" to (data.kemandoran_nama ?: ""),
                                            "kemandoran_ppro" to kemandoranPpro,
                                            "kemandoran_kode" to kemandoranKode,
                                            "pemanen_nik" to (data.nik ?: ""),
                                            "pemanen_nama" to (data.pemanen_nama ?: ""),
                                            "tph" to Gson().toJson(tphArray),
                                            "ancak" to "",
                                            "jjg_panen" to totalJjgPanen.toString(),
                                            "jjg_masak" to totalJjgMasak.toString(),
                                            "jjg_mentah" to totalJjgMentah.toString(),
                                            "jjg_lewat_masak" to totalJjgLewatMasak.toString(),
                                            "jjg_kosong" to totalJjgKosong.toString(),
                                            "jjg_abnormal" to totalJjgAbnormal.toString(),
                                            "jjg_serangan_tikus" to "0",
                                            "jjg_panjang" to "0",
                                            "jjg_tidak_vcut" to "0",
                                            "jjg_kirim" to totalJjgKirim.toString(),
                                            "jjg_bayar" to totalJjgBayar.toString(),
                                            "luasan" to data.luas_panen,
                                            "date_panen" to Gson().toJson(dateCreatedArray),
                                            "status" to 1,
                                        )
                                    )
                                }

                                AppLogger.d("Created hektaran for Date: $dateOnly, Blok: $blokId with ${detailRecords.size} detail records")

                                // Add the detail records as a child element to the hektaran data
                                hektaranData[AppUtils.DatabaseTables.HEKTARAN_DETAIL] = detailRecords

                                // Return the complete hektaran structure with child details
                                hektaranData
                            }

                            AppLogger.d("Final restructured data count: ${restructuredData.size}")
                            restructuredData.forEachIndexed { index, item ->
                                val tanggal = item["tanggal"]
                                val blok = item["blok"]
                                val detailCount = (item[AppUtils.DatabaseTables.HEKTARAN_DETAIL] as? List<*>)?.size ?: 0
                                AppLogger.d("Hektaran $index: Date=$tanggal, Blok=$blok, Details=$detailCount")
                            }

                            // Create the final structure with only "hektaran" as the root element
                            val finalData = mapOf<String, Any>(
                                AppUtils.DatabaseTables.HEKTARAN to restructuredData
                            )

                            // Convert to JSON
                            hektaranJson = Gson().toJson(finalData)

                            // Save JSON to a temporary file for inspection
//                            try {
//                                val tempDir = File(getExternalFilesDir(null), "TEMP").apply {
//                                    if (!exists()) mkdirs()
//                                }
//
//                                val filename = "hektaran_data_${System.currentTimeMillis()}.json"
//                                val tempFile = File(tempDir, filename)
//
//                                FileOutputStream(tempFile).use { fos ->
//                                    fos.write(hektaranJson.toByteArray())
//                                }
//
//                                AppLogger.d("Saved raw hektaran data to temp file: ${tempFile.absolutePath}")
//                            } catch (e: Exception) {
//                                AppLogger.e("Failed to save hektaran data to temp file: ${e.message}")
//                                e.printStackTrace()
//                            }

                            // Extract all IDs for tracking
                            val hektaranIds = hektarPanenToUpload.mapNotNull { it.id }

                            // Store as a single entry
                            combinedUploadData[AppUtils.DatabaseTables.HEKTAR_PANEN] = mapOf(
                                "data" to hektaranJson,
                                "filename" to "hektaran_data.json",
                                "ids" to hektaranIds
                            )

                            unzippedHektaranData = restructuredData.filter { item ->
                                // Get the blok value from the current item
                                val blok = item["blok"] as? Int ?: 0
                                val tanggal = item["tanggal"] as? String ?: ""

                                // Find data items with this date+blok that have dataIsZipped = 0
                                val notYetZipped = hektarPanenList.any { data ->
                                    val firstDate = data.date_created_panen.split(";").firstOrNull() ?: ""
                                    val dateOnly = if (firstDate.isNotEmpty()) {
                                        try {
                                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val date = inputFormat.parse(firstDate)
                                            date?.let { outputFormat.format(it) } ?: firstDate.split(" ")[0]
                                        } catch (e: Exception) {
                                            firstDate.split(" ")[0]
                                        }
                                    } else {
                                        data.date_created?.split(" ")?.get(0) ?: ""
                                    }

                                    data.blok == blok && dateOnly == tanggal && data.status_upload == 0 && data.dataIsZipped == 0
                                }

                                notYetZipped
                            }

                            globalHektaranIds = hektaranIds

                        } else {
                            globalHektaranIds = emptyList()
                            unzippedHektaranData = emptyList()
                        }
                    }

//                                    AppUtils.clearTempJsonFiles(this@HomePageActivity)
                    if (absensiList.isNotEmpty()) {
                        // First, process photos/images regardless of status_upload
                        val absensiWithPendingImages = absensiList.filter { data ->
                            val uploadStatusImage =
                                data.absensi.status_uploaded_image
                            val photoName = data.absensi.foto.trim()

                            photoName.isNotEmpty() && (uploadStatusImage == "0" ||
                                    (uploadStatusImage.startsWith("{") && try {
                                        val errorJson = Gson().fromJson(
                                            uploadStatusImage,
                                            JsonObject::class.java
                                        )
                                        val errorArray =
                                            errorJson?.get("error")?.asJsonArray
                                        errorArray?.any { it.asString == photoName }
                                            ?: false
                                    } catch (e: Exception) {
                                        AppLogger.e("Error parsing upload status JSON: ${e.message}")
                                        false
                                    }))
                        }

                        val uniquePhotos =
                            mutableMapOf<String, Map<String, String>>()

                        // Prepare to search for photo files in CMP directories
                        val picturesDirs = listOf(
                            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            File(
                                getExternalFilesDir(null)?.parent ?: "",
                                "Pictures"
                            )
                        ).filterNotNull()

                        val cmpDirectories = mutableListOf<File>()
                        for (picturesDir in picturesDirs) {
                            if (!picturesDir.exists() || !picturesDir.isDirectory) {
                                AppLogger.w("Pictures directory not found: ${picturesDir.absolutePath}")
                                continue
                            }

                            // Look specifically for CMP-ABSENSI directory
                            val cmpAbsensiDir = File(
                                picturesDir,
                                AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
                            )
                            if (cmpAbsensiDir.exists() && cmpAbsensiDir.isDirectory) {
                                cmpDirectories.add(cmpAbsensiDir)
                            }

                            // Also check for any other CMP directories
                            val otherCmpDirs = picturesDir.listFiles { file ->
                                file.isDirectory && file.name.startsWith("CMP") && file.name != AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
                            } ?: emptyArray()

                            cmpDirectories.addAll(otherCmpDirs)
                        }

                        // Process photos for absensi with pending images
                        if (absensiWithPendingImages.isNotEmpty()) {
                            for (absensiRelation in absensiWithPendingImages) {
                                val absensi = absensiRelation.absensi

                                // Process photo for upload
                                val photoName = absensi.foto.trim()
                                if (photoName.isNotEmpty() && photoName !in uniquePhotos) {
                                    val uploadStatusImage =
                                        absensi.status_uploaded_image

                                    // Determine if photo needs uploading
                                    var shouldAddPhoto = false

                                    if (uploadStatusImage == "0") {
                                        // Default status - hasn't been uploaded yet
                                        shouldAddPhoto = true
                                        AppLogger.d("Photo $photoName hasn't been uploaded (status 0)")
                                    } else if (uploadStatusImage.startsWith("{")) {
                                        try {
                                            // Using Gson to parse the JSON
                                            val errorJson = Gson().fromJson(
                                                uploadStatusImage,
                                                JsonObject::class.java
                                            )
                                            val errorArray =
                                                errorJson?.get("error")?.asJsonArray

                                            errorArray?.forEach { errorItem ->
                                                if (errorItem.asString == photoName) {
                                                    shouldAddPhoto = true
                                                    AppLogger.d("Photo $photoName is marked as error in record ${absensi.id}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            AppLogger.e("Error parsing upload status JSON: ${e.message}")
                                        }
                                    }

                                    // Only process the photo if it needs uploading
                                    if (shouldAddPhoto) {
                                        var photoFound = false

                                        for (cmpDir in cmpDirectories) {
                                            val photoFile = File(cmpDir, photoName)

                                            if (photoFile.exists() && photoFile.isFile) {
                                                val createdDate =
                                                    absensi.date_absen ?: ""
                                                val formattedDate = try {
                                                    val dateFormat =
                                                        SimpleDateFormat(
                                                            "yyyy-MM-dd HH:mm:ss",
                                                            Locale.getDefault()
                                                        )
                                                    val date =
                                                        dateFormat.parse(createdDate)
                                                    val outputFormat =
                                                        SimpleDateFormat(
                                                            "yyyy/MM/dd/",
                                                            Locale.getDefault()
                                                        )
                                                    outputFormat.format(
                                                        date ?: Date()
                                                    )
                                                } catch (e: Exception) {
                                                    AppLogger.e("Error formatting date: ${e.message}")
                                                    // Default to current date if parsing fails
                                                    val outputFormat =
                                                        SimpleDateFormat(
                                                            "yyyy/MM/dd/",
                                                            Locale.getDefault()
                                                        )
                                                    outputFormat.format(Date())
                                                }

                                                // Create the base path by appending the estate code or other identifier
                                                val basePathImage =
                                                    formattedDate + prefManager!!.estateUserLogin

                                                uniquePhotos[photoName] = mapOf(
                                                    "name" to photoName,
                                                    "path" to photoFile.absolutePath,
                                                    "size" to photoFile.length()
                                                        .toString(),
                                                    "table_ids" to absensi.id.toString(),
                                                    "base_path" to basePathImage,
                                                    "database" to AppUtils.DatabaseTables.ABSENSI
                                                )

                                                AppLogger.d("Added absensi photo for upload: $photoName at ${photoFile.absolutePath}")
                                                photoFound = true
                                                break
                                            }
                                        }

                                        if (!photoFound) {
                                            AppLogger.w("Absensi photo not found: $photoName")
                                        }
                                    } else {
                                        AppLogger.d("Skipping photo upload for $photoName - already uploaded (status: $uploadStatusImage)")
                                    }
                                }
                            }

                            // Add photos to the combined upload data if we found any
                            allPhotosAbsensi = uniquePhotos.values.toMutableList()
                            if (allPhotosAbsensi.isNotEmpty()) {
                                AppLogger.d("Adding ${allPhotosAbsensi.size} absensi photos to upload data")
                                combinedUploadData["foto_absensi"] =
                                    allPhotosAbsensi
                            } else {
                                AppLogger.w("No absensi photos found to upload")
                            }
                        }

                        // Now process data with status_upload == 0, completely separate from the photo logic
                        val absensiToUpload = absensiList.filter { data ->
                            data.absensi.status_upload == 0
                        }

                        if (absensiToUpload.isNotEmpty()) {
                            // Create a mutable list to hold our restructured data
                            val restructuredData = mutableListOf<Map<String, Any>>()

                            // Process each absensi record
                            for (absensiRelation in absensiToUpload) {
                                val absensi = absensiRelation.absensi

                                // Split the kemandoran_id string into individual IDs
                                val kemandoranIds = absensi.kemandoran_id.split(",")
                                    .filter { it.isNotEmpty() }.map { it.trim() }

                                // Create a deferred to fetch all kemandoran data in one go
                                val kemandoranDeferred =
                                    CompletableDeferred<List<KemandoranModel>>()

                                // Fetch kemandoran data from database
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val kemandoranList =
                                            absensiViewModel.getKemandoranById(
                                                kemandoranIds
                                            )
                                        kemandoranDeferred.complete(kemandoranList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching kemandoran data: ${e.message}")
                                        kemandoranDeferred.complete(emptyList())
                                    }
                                }

                                // Wait for the kemandoran data
                                val kemandoranList = try {
                                    kemandoranDeferred.await()
                                } catch (e: Exception) {
                                    AppLogger.e("Error waiting for kemandoran data: ${e.message}")
                                    emptyList()
                                }

                                // Create a map of ID to KemandoranModel for easy lookup
                                val kemandoranMap =
                                    kemandoranList.associateBy { it.id.toString() }

                                // Collect all NIKs from present and absent employees
                                val allNiks = mutableListOf<String>()

                                // Add present employee NIKs
                                allNiks.addAll(
                                    absensi.karyawan_msk_nik.split(",")
                                        .filter { it.isNotEmpty() }
                                        .map { it.trim() }
                                )

                                // Add absent employee NIKs
                                allNiks.addAll(
                                    absensi.karyawan_tdk_msk_nik.split(",")
                                        .filter { it.isNotEmpty() }
                                        .map { it.trim() }
                                )

                                // Create a deferred to fetch all employee data
                                val karyawanDeferred =
                                    CompletableDeferred<List<KaryawanModel>>()

                                // Fetch employee data from database
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val karyawanList =
                                            absensiViewModel.getKaryawanByNikList(
                                                allNiks
                                            )
                                        karyawanDeferred.complete(karyawanList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching karyawan data: ${e.message}")
                                        karyawanDeferred.complete(emptyList())
                                    }
                                }

                                // Wait for the employee data
                                val karyawanList = try {
                                    karyawanDeferred.await()
                                } catch (e: Exception) {
                                    AppLogger.e("Error waiting for karyawan data: ${e.message}")
                                    emptyList()
                                }


                                val kemandoranNamaDeferred =
                                    CompletableDeferred<KemandoranModel?>()

// Fetch blok data from database using absensi fields
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val blokData =
                                            datasetViewModel.getBlokByDivisiAndDept(
                                                absensi.divisi,
                                                absensi.dept
                                            )
                                        kemandoranNamaDeferred.complete(blokData)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching blok data: ${e.message}")
                                        kemandoranNamaDeferred.complete(null)
                                    }
                                }

// Wait for the blok data
                                val blokData = try {
                                    kemandoranNamaDeferred.await()
                                } catch (e: Exception) {
                                    AppLogger.e("Error waiting for blok data: ${e.message}")
                                    null
                                }

                                // Create a map of NIK to KaryawanModel for easy lookup
                                val karyawanMap =
                                    karyawanList.associateBy { it.nik }

                                // Process each kemandoran ID separately to create individual records
                                for (singleKemandoranId in kemandoranIds) {
                                    // Get the related kemandoran model for this ID
                                    val kemandoran =
                                        kemandoranMap[singleKemandoranId]

                                    val dateStr = try {
                                        val fullDate = absensi.date_absen ?: ""
                                        if (fullDate.isNotEmpty()) {
                                            val inputFormat = SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()
                                            )
                                            val outputFormat = SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            )
                                            val date = inputFormat.parse(fullDate)
                                            date?.let { outputFormat.format(it) }
                                                ?: fullDate
                                        } else {
                                            ""
                                        }
                                    } catch (e: Exception) {
                                        // If parsing fails, use the original string
                                        AppLogger.e("Error parsing date: ${e.message}")
                                        absensi.date_absen ?: ""
                                    }

                                    val formattedDatePath = try {
                                        val dateFormat = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                        )
                                        val date = dateFormat.parse(
                                            absensi.date_absen ?: ""
                                        )
                                        val outputFormat = SimpleDateFormat(
                                            "yyyy/MM/dd",
                                            Locale.getDefault()
                                        )
                                        outputFormat.format(date ?: Date())
                                    } catch (e: Exception) {
                                        AppLogger.e("Error formatting date for path: ${e.message}")
                                        // Default to current date if parsing fails
                                        val outputFormat = SimpleDateFormat(
                                            "yyyy/MM/dd",
                                            Locale.getDefault()
                                        )
                                        outputFormat.format(Date())
                                    }

                                    // Create base path for photo, exactly like in panenList
                                    val basePath =
                                        "$formattedDatePath/${prefManager!!.estateUserLogin}/"

                                    // Process the photo filenames to prepend the base path, exactly like in panenList
                                    val originalFotoString = absensi.foto ?: ""
                                    val modifiedFotoString =
                                        if (originalFotoString.contains(";")) {
                                            // Multiple photos - split, modify each one, and rejoin
                                            originalFotoString.split(";")
                                                .map { photoName -> "$basePath${photoName.trim()}" }
                                                .joinToString(";")
                                        } else if (originalFotoString.isNotEmpty()) {
                                            // Single photo - just prepend the base path
                                            "$basePath$originalFotoString"
                                        } else {
                                            // No photos
                                            ""
                                        }

                                    // Create structure for this absensi record
                                    val absensiData = mutableMapOf<String, Any>(
                                        "kemandoran_id" to singleKemandoranId,
                                        "date" to dateStr,
                                        "tanggal" to (absensi.date_absen ?: ""),
                                        "company" to (blokData?.company ?: 0),
                                        "company_ppro" to (blokData?.company_ppro
                                            ?: 0),
                                        "company_abbr" to (blokData?.company_abbr
                                            ?: ""),
                                        "company_nama" to (blokData?.company_nama
                                            ?: ""), // Not available in BlokModel
                                        "dept" to (absensi.dept ?: 0),
                                        "dept_ppro" to (blokData?.dept_ppro ?: 0),
                                        "dept_abbr" to (absensi.dept_abbr ?: ""),
                                        "dept_nama" to (blokData?.dept_nama
                                            ?: ""), // Not available in BlokModel
                                        "divisi" to (absensi.divisi ?: 0),
                                        "divisi_ppro" to (blokData?.divisi_ppro
                                            ?: 0),
                                        "divisi_abbr" to (absensi.divisi_abbr
                                            ?: ""),
                                        "divisi_nama" to (blokData?.divisi_nama
                                            ?: ""),
                                        "kemandoran_ppro" to (kemandoran?.kemandoran_ppro
                                            ?: ""),
                                        "kemandoran_kode" to (kemandoran?.kode
                                            ?: ""),
                                        "kemandoran_nama" to (kemandoran?.nama
                                            ?: ""),
                                        "foto" to modifiedFotoString,
                                        "komentar" to (absensi.komentar ?: ""),
                                        "created_by" to (absensi.created_by),
                                        "created_name" to "",
                                        "created_date" to (absensi.date_absen ?: "")
                                    )

                                    val detailRecords =
                                        mutableListOf<Map<String, Any>>()

// Extract employees for this specific kemandoran ID from JSON
                                    val presentEmployeeNiks =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_msk_nik,
                                            singleKemandoranId
                                        )
                                    val presentEmployeeIds =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_msk_id,
                                            singleKemandoranId
                                        )
                                    val presentEmployeeNames =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_msk_nama,
                                            singleKemandoranId
                                        )

                                    val absentEmployeeNiks =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_tdk_msk_nik,
                                            singleKemandoranId
                                        )
                                    val absentEmployeeIds =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_tdk_msk_id,
                                            singleKemandoranId
                                        )
                                    val absentEmployeeNames =
                                        extractEmployeesFromJson(
                                            absensi.karyawan_tdk_msk_nama,
                                            singleKemandoranId
                                        )

//                                                    AppLogger.d("Processing kemandoran $singleKemandoranId:")
//                                                    AppLogger.d("Present employees: $presentEmployeeNiks")
//                                                    AppLogger.d("Absent employees: $absentEmployeeNiks")

// Process employees who are present for this kemandoran
                                    presentEmployeeNiks.forEachIndexed { index, nik ->
                                        // Try to get employee data from database map first, then fallback to JSON data
                                        val karyawan = karyawanMap[nik]
                                        val employeeName = karyawan?.nama
                                            ?: (if (index < presentEmployeeNames.size) presentEmployeeNames[index] else "")

                                        detailRecords.add(
                                            mapOf(
                                                "nik" to nik,
                                                "nama" to employeeName,
                                                "status_kehadiran" to 1, // 1 = present
                                                "date_created" to (absensi.date_absen
                                                    ?: "")
                                            )
                                        )
                                    }

// Process employees who are absent for this kemandoran
                                    absentEmployeeNiks.forEachIndexed { index, nik ->
                                        // Try to get employee data from database map first, then fallback to JSON data
                                        val karyawan = karyawanMap[nik]
                                        val employeeName = karyawan?.nama
                                            ?: (if (index < absentEmployeeNames.size) absentEmployeeNames[index] else "")

                                        detailRecords.add(
                                            mapOf(
                                                "nik" to nik,
                                                "nama" to employeeName,
                                                "status_kehadiran" to 0, // 0 = absent
                                                "date_created" to (absensi.date_absen
                                                    ?: "")
                                            )
                                        )
                                    }

                                    AppLogger.d("Created ${detailRecords.size} detail records for kemandoran $singleKemandoranId")

// Add the detail records as a child element to the absensi data
                                    absensiData[AppUtils.DatabaseTables.ABSENSI_DETAIL] =
                                        detailRecords

// Add this complete record to our restructured data
                                    restructuredData.add(absensiData)
                                }
                            }

                            // Create the final structure with only "absensi" as the root element
                            val finalData = mapOf<String, Any>(
                                AppUtils.DatabaseTables.ABSENSI to restructuredData
                            )

                            // Convert to JSON
                            absensiJson = Gson().toJson(finalData)

                            // Save JSON to a temporary file for inspection
//                                            try {
//                                                val tempDir =
//                                                    File(getExternalFilesDir(null), "TEMP").apply {
//                                                        if (!exists()) mkdirs()
//                                                    }
//
//                                                val filename =
//                                                    "absensi_data_${System.currentTimeMillis()}.json"
//                                                val tempFile = File(tempDir, filename)
//
//                                                FileOutputStream(tempFile).use { fos ->
//                                                    fos.write(absensiJson.toByteArray())
//                                                }
//
//                                                AppLogger.d("Saved raw absensi data to temp file: ${tempFile.absolutePath}")
//                                            } catch (e: Exception) {
//                                                AppLogger.e("Failed to save absensi data to temp file: ${e.message}")
//                                                e.printStackTrace()
//                                            }

                            AppLogger.d(absensiJson)

                            // Extract all IDs for tracking
                            val absensiIds = absensiToUpload.map { it.absensi.id }

                            unzippedAbsensiData = restructuredData.filter { item ->
                                // Get the kemandoran_id from the current item
                                val singleKemandoranId =
                                    item["kemandoran_id"] as? String ?: ""

                                // Find data items with this kemandoran_id that have dataIsZipped = 0
                                val notYetZipped =
                                    absensiList.any { absensiRelation ->
                                        val kemandoranIds =
                                            absensiRelation.absensi.kemandoran_id.split(
                                                ","
                                            )
                                        singleKemandoranId in kemandoranIds &&
                                                absensiRelation.absensi.status_upload == 0 &&
                                                absensiRelation.absensi.dataIsZipped == 0
                                    }

                                notYetZipped
                            }

                            // Extract IDs from items that match the unzippedAbsensiData criteria
                            globalAbsensiIds = absensiList.filter { absensiRelation ->
                                absensiRelation.absensi.status_upload == 0 &&
                                        absensiRelation.absensi.dataIsZipped == 0
                            }.map { it.absensi.id }

                            combinedUploadData[AppUtils.DatabaseTables.ABSENSI] =
                                mapOf(
                                    "data" to absensiJson,
                                    "filename" to "absensi_data.json",
                                    "ids" to globalAbsensiIds
                                )
                        } else {
                            globalAbsensiIds = emptyList()
                            unzippedAbsensiData = emptyList()
                        }
                    }

                } catch (e: Exception) {
                    Log.e("UploadCheck", "❌ Error: ${e.message}")
                } finally {

                    // Create the upload data list with only the unzipped items
                    val uploadDataList =
                        mutableListOf<Pair<String, List<Map<String, Any>>>>()

                    // Use the filtered data for zip creation
                    if (unzippedPanenData.isNotEmpty()) {
                        uploadDataList.add(AppUtils.DatabaseTables.PANEN to unzippedPanenData)
                    }
                    if (unzippedESPBData.isNotEmpty()) {
                        uploadDataList.add(AppUtils.DatabaseTables.ESPB to unzippedESPBData)
                    }

                    if (uploadDataList.isNotEmpty()) {

                        lifecycleScope.launch(Dispatchers.IO) {
                            AppUtils.createAndSaveZipUploadCMPSingle(
                                this@HomePageActivity,
                                uploadDataList,
                                prefManager!!.idUserLogin.toString()
                            ) { success, fileName, fullPath, zipFile ->
                                if (success) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        featuresToFetch.forEach { feature ->
                                            val ids = when (feature) {
                                                AppUtils.DatabaseTables.ESPB -> globalESPBIds
                                                AppUtils.DatabaseTables.PANEN -> globalPanenIds
                                                else -> emptyList()
                                            }

                                            if (ids.isNotEmpty()) {
                                                archiveUpdateActions[feature]?.invoke(
                                                    ids
                                                )
                                            }
                                        }
                                    }
                                    zipDeferred.complete(true)
                                } else {
                                    Log.e("UploadCheck", "❌ ZIP creation failed")
                                    zipDeferred.complete(false)
                                }
                            }
                        }
                    } else {
                        zipDeferred.complete(false)
                    }

                    if (hektaranJson.isNotEmpty() && unzippedHektaranData.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            AppUtils.createAndSaveZipUpload(
                                this@HomePageActivity,
                                hektaranJson,
                                prefManager!!.idUserLogin.toString(),
                                AppUtils.DatabaseTables.HEKTARAN,
                            ) { success, fileName, fullPath, zipFile ->
                                if (success) {
                                    AppLogger.d("Successfully created hektaran ZIP: $fileName")
                                    AppLogger.d("ZIP file path: $fullPath")

                                    // Update database to mark as zipped
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        archiveUpdateActions[AppUtils.DatabaseTables.HEKTAR_PANEN]?.invoke(
                                            globalHektaranIds
                                        )
                                    }

                                    zipDeferred.complete(true)
                                } else {
                                    AppLogger.e("Failed to create hektaran ZIP: $fileName")
                                    zipDeferred.complete(false)
                                }
                            }
                        }
                    } else {
                        zipDeferred.complete(false)
                    }

                    if (absensiJson.isNotEmpty() && unzippedAbsensiData.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            AppUtils.createAndSaveZipUpload(
                                this@HomePageActivity,
                                absensiJson,
                                prefManager!!.idUserLogin.toString(),
                                AppUtils.DatabaseTables.ABSENSI,
                                allPhotosAbsensi,
                            ) { success, fileName, fullPath, zipFile ->
                                if (success) {
                                    AppLogger.d("Successfully created absensi ZIP: $fileName")
                                    AppLogger.d("ZIP file path: $fullPath")

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        archiveUpdateActions[AppUtils.DatabaseTables.ABSENSI]?.invoke(
                                            globalAbsensiIds
                                        )
                                    }

                                    zipDeferred.complete(true)
                                } else {
                                    AppLogger.e("Failed to create absensi ZIP: $fileName")
                                    zipDeferred.complete(false)
                                }
                            }
                        }
                    } else {
                        zipDeferred.complete(false)
                    }

                    loadingDialog.dismiss()
                }

                val zipSuccess = zipDeferred.await()
                val updatedPanenList = panenDeferred.await()
                val updatedESPBList = espbDeferred.await()
                val updatedHektarPanenList = hektarPanenDeferred.await()
                val updatedAbsensiList = absensiDeferred.await()

                val panenToUpload = updatedPanenList.filter {
                    it.panen.status_upload == 0
                }
                val espbToUpload = updatedESPBList.filter {
                    it.status_upload_cmp_sp == 0
                }
                val hektarPanenToUpload = updatedHektarPanenList.filter {
                    it.status_upload == 0
                }
                val absensiPanenToUpload = updatedAbsensiList.filter {
                    it.absensi.status_upload == 0
                }

                val hasPhotosPanenToUpload = allPhotosPanen.isNotEmpty()
                val hasPhotosAbsensiToUpload = allPhotosAbsensi.isNotEmpty()
                val hasItemsToUpload =
                    panenToUpload.isNotEmpty() || espbToUpload.isNotEmpty() || hasPhotosPanenToUpload || hektarPanenToUpload.isNotEmpty() || absensiPanenToUpload.isNotEmpty() || hasPhotosAbsensiToUpload

                if (hasItemsToUpload) {
                    val uploadDataJson = Gson().toJson(combinedUploadData)
                    setupDialogUpload(uploadDataJson)
                } else {
                    AlertDialogUtility.withSingleAction(
                        this@HomePageActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_no_data_for_upload_cmp),
                        stringXML(R.string.al_no_data_for_upload_cmp_description),
                        "success.json",
                        R.color.greendarkerbutton
                    ) { }
                }
            }

        }
    }


    private fun createJsonTableNameMapping(partNumber: String): String {
        val tableMap = mutableMapOf<String, List<Int>>()

        // Get IDs for this specific part
        val panenIds = globalPanenIdsByPart[partNumber] ?: emptyList()
        val espbIds = globalEspbIdsByPart[partNumber] ?: emptyList()
        val hektarPanenIds = globalHektarPanenIdsByPart[partNumber] ?: emptyList()
        val absensiPanenIds = globalAbsensiPanenIdsByPart[partNumber] ?: emptyList()

        if (panenIds.isNotEmpty()) {
            tableMap[AppUtils.DatabaseTables.PANEN] = panenIds
        }
        if (espbIds.isNotEmpty()) {
            tableMap[AppUtils.DatabaseTables.ESPB] = espbIds
        }
        if (hektarPanenIds.isNotEmpty()) {
            tableMap[AppUtils.DatabaseTables.HEKTAR_PANEN] = hektarPanenIds
        }
        if (absensiPanenIds.isNotEmpty()) {
            tableMap[AppUtils.DatabaseTables.ABSENSI] = absensiPanenIds
        }

        return Gson().toJson(tableMap) // Convert to JSON string
    }

    private val archiveUpdateActions = mapOf(
        AppUtils.DatabaseTables.ESPB to { ids: List<Int> ->
            weightBridgeViewModel.updateDataIsZippedESPB(
                ids,
                1
            )
        },
        AppUtils.DatabaseTables.PANEN to { ids: List<Int> ->
            panenViewModel.updateDataIsZippedPanen(
                ids,
                1
            )
        },
        AppUtils.DatabaseTables.HEKTAR_PANEN to { ids: List<Int> ->
            hektarPanenViewModel.updateDataIsZippedHP(
                ids,
                1
            )
        },
        AppUtils.DatabaseTables.ABSENSI to { ids: List<Int> ->
            absensiViewModel.updateDataIsZippedAbsensi(
                ids,
                1
            )
        }
    )

    data class Pemanen(val nik: String, val nama: String)
    data class Kemandoran(
        val id: Int,
        val kode: String,
        val nama: String,
        val pemanen: List<Pemanen>
    )

    fun convertToJsonKaryawanKemandoran(kemandoranData: List<KaryawanDao.KaryawanKemandoranData>): String {
        val groupedData = kemandoranData
            .groupBy { it.kemandoranId }
            .map { (kemandoranId, dataList) ->
                Kemandoran(
                    id = kemandoranId,
                    kode = dataList.first().kodeKemandoran,
                    nama = dataList.first().kemandoranNama,
                    pemanen = dataList.map { Pemanen(it.nik, it.namaKaryawan.trim()) }
                )
            }
        return Gson().toJson(groupedData)
    }

    @SuppressLint("SetTextI18n")
    private fun setupDialogUpload(uploadData: String? = null) {


        AppLogger.d("uploadData $uploadData")
        // Reset state for a fresh upload dialog
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
        val uploadItems = mutableListOf<UploadCMPItem>()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UploadProgressCMPDataAdapter(uploadItems)
        recyclerView.adapter = adapter

        if (uploadData != null) {
            try {
                val gson = Gson()
                val dataMap = gson.fromJson(uploadData, Map::class.java)
                AppLogger.d("Data map keys: ${dataMap.keys}")

                // Extract the data for panen, espb, and photo files
                val panenFilePath = dataMap[AppUtils.DatabaseTables.PANEN] as? String
                val espbFilePath = dataMap[AppUtils.DatabaseTables.ESPB] as? String
                val fotoPanen = dataMap["foto_panen"] as? List<*>
                val fotoAbsensi = dataMap["foto_absensi"] as? List<*>

                AppLogger.d("Panen file path: $panenFilePath")
                AppLogger.d("ESPB file path: $espbFilePath")
                AppLogger.d("Photo file path: $fotoPanen")

                var itemId = 0

                val panenBatches = dataMap[AppUtils.DatabaseTables.PANEN] as? Map<*, *>
                if (panenBatches != null) {
                    panenBatches.entries.forEachIndexed { index, entry ->
                        val batchKey = entry.key as? String ?: ""
                        val batchInfo = entry.value as? Map<*, *> ?: mapOf<String, Any>()

                        val panenData = batchInfo["data"] as? String
                        val panenFilename = batchInfo["filename"] as? String
                        val panenIds = batchInfo["ids"] as? List<Int> ?: emptyList()

                        if (panenData != null && panenData.isNotEmpty() && panenFilename != null) {
                            val batchNumber = batchKey.replace("batch_", "")
                            val dataSize = panenData.length.toLong()

                            val tableIdsJson = JSONObject().apply {
                                put(AppUtils.DatabaseTables.PANEN, JSONArray(panenIds))
                            }.toString()

                            val uploadItem = UploadCMPItem(
                                id = itemId++,
                                title = panenFilename,
                                fullPath = "",
                                baseFilename = panenFilename,
                                data = panenData,
                                type = "json",
                                tableIds = tableIdsJson,
                                databaseTable = AppUtils.DatabaseTables.PANEN
                            )

                            uploadItems.add(uploadItem)
                            adapter.setFileSize(uploadItem.id, dataSize)
                            AppLogger.d("Added panen batch $batchNumber to upload items (size: $dataSize bytes)")
                        }
                    }
                }

                // Process ESPB batches
                val espbInfo = dataMap[AppUtils.DatabaseTables.ESPB] as? Map<*, *>
                if (espbInfo != null) {
                    AppLogger.d("Found espb_table data: $espbInfo")

                    val espbData = espbInfo["data"] as? String
                    val espbFilename = espbInfo["filename"] as? String
                    val espbIds = espbInfo["ids"] as? List<Int> ?: emptyList()

                    if (espbData != null && espbData.isNotEmpty()) {
                        val dataSize = espbData.length.toLong()
                        AppLogger.d("ESPB data size: $dataSize")

                        val tableIdsJson = JSONObject().apply {
                            put(AppUtils.DatabaseTables.ESPB, JSONArray(espbIds))
                        }.toString()

                        val uploadItem = UploadCMPItem(
                            id = itemId++,
                            title = "Data ESPB",
                            fullPath = "",
                            baseFilename = espbFilename!!,
                            data = espbData,
                            type = "json",
                            tableIds = tableIdsJson,
                            databaseTable = AppUtils.DatabaseTables.ESPB
                        )

                        uploadItems.add(uploadItem)
                        adapter.setFileSize(uploadItem.id, dataSize)
                        adapter.notifyDataSetChanged() // Make sure adapter updates
                        AppLogger.d("Added ESPB to upload items (size: $dataSize bytes)")
                    } else {
                        AppLogger.d("ESPB data is missing required fields: data=$espbData, filename=$espbFilename")
                    }
                }

                // Process Hektar Panen
                val hektarPanenInfo = dataMap[AppUtils.DatabaseTables.HEKTAR_PANEN] as? Map<*, *>
                if (hektarPanenInfo != null) {
                    AppLogger.d("Found hektar_panen data: $hektarPanenInfo")

                    val hektarPanenData = hektarPanenInfo["data"] as? String
                    val espbFilename = hektarPanenInfo["filename"] as? String
                    val espbIds = hektarPanenInfo["ids"] as? List<Int> ?: emptyList()

                    if (hektarPanenData != null && hektarPanenData.isNotEmpty()) {
                        val dataSize = hektarPanenData.length.toLong()
                        AppLogger.d("Hektar Panen data size: $dataSize")

                        val tableIdsJson = JSONObject().apply {
                            put(AppUtils.DatabaseTables.HEKTAR_PANEN, JSONArray(espbIds))
                        }.toString()

                        val uploadItem = UploadCMPItem(
                            id = itemId++,
                            title = "Data Hektar Panen",
                            fullPath = "",
                            baseFilename = espbFilename!!,
                            data = hektarPanenData,
                            type = "json",
                            tableIds = tableIdsJson,
                            databaseTable = AppUtils.DatabaseTables.HEKTAR_PANEN
                        )

                        uploadItems.add(uploadItem)
                        adapter.setFileSize(uploadItem.id, dataSize)
                        adapter.notifyDataSetChanged() // Make sure adapter updates
                        AppLogger.d("Added ESPB to upload items (size: $dataSize bytes)")
                    } else {
                        AppLogger.d("ESPB data is missing required fields: data=$hektarPanenData, filename=$espbFilename")
                    }
                }

                // Process Absensi Panen
                val absensiPanenInfo = dataMap[AppUtils.DatabaseTables.ABSENSI] as? Map<*, *>
                if (absensiPanenInfo != null) {
                    AppLogger.d("Found abasensi data: $absensiPanenInfo")

                    val absensiPanenData = absensiPanenInfo["data"] as? String
                    val espbFilename = absensiPanenInfo["filename"] as? String
                    val espbIds = absensiPanenInfo["ids"] as? List<Int> ?: emptyList()

                    if (absensiPanenData != null && absensiPanenData.isNotEmpty()) {
                        val dataSize = absensiPanenData.length.toLong()
                        AppLogger.d("absensi data size: $dataSize")

                        val tableIdsJson = JSONObject().apply {
                            put(AppUtils.DatabaseTables.ABSENSI, JSONArray(espbIds))
                        }.toString()

                        val uploadItem = UploadCMPItem(
                            id = itemId++,
                            title = "Data Absensi Panen",
                            fullPath = "",
                            baseFilename = espbFilename!!,
                            data = absensiPanenData,
                            type = "json",
                            tableIds = tableIdsJson,
                            databaseTable = AppUtils.DatabaseTables.ABSENSI
                        )

                        uploadItems.add(uploadItem)
                        adapter.setFileSize(uploadItem.id, dataSize)
                        adapter.notifyDataSetChanged() // Make sure adapter updates
                        AppLogger.d("Added ESPB to upload items (size: $dataSize bytes)")
                    } else {
                        AppLogger.d("ESPB data is missing required fields: data=$absensiPanenData, filename=$espbFilename")
                    }
                }

                if (fotoPanen != null && fotoPanen.isNotEmpty()) {
                    AppLogger.d("Processing photo data: ${fotoPanen.size} photos")
                    var totalPhotoSize = 0L
                    val foundPhotoCount = fotoPanen.count { photoData ->
                        try {
                            (photoData as? Map<*, *>)?.let { photoMap ->
                                val name = photoMap["name"] as? String ?: ""
                                val sizeStr = photoMap["size"] as? String
                                val size = sizeStr?.toLongOrNull() ?: 0L
                                totalPhotoSize += size
                                AppLogger.d("Photo: $name, size: $size")
                                size > 0
                            } ?: false
                        } catch (e: Exception) {
                            AppLogger.e("Error processing photo data: ${e.message}")
                            false
                        }
                    }

                    AppLogger.d("Found $foundPhotoCount photos with a total size of $totalPhotoSize bytes")
                    if (foundPhotoCount > 0) {
                        val photoTitle = "Foto Panen ($foundPhotoCount file)"
                        val uploadItem = UploadCMPItem(
                            id = itemId++,
                            title = photoTitle,
                            fullPath = "foto_panen",
                            baseFilename = "",
                            data = gson.toJson(fotoPanen),
                            type = "image",
                            databaseTable = ""
                        )

                        uploadItems.add(uploadItem)
                        AppLogger.d("Adding photo upload item with ID ${uploadItem.id}")
                        adapter.setFileSize(uploadItem.id, totalPhotoSize)
                    } else {
                        AppLogger.w("No photo files found for upload")
                    }
                }

                if (fotoAbsensi != null && fotoAbsensi.isNotEmpty()) {
                    AppLogger.d("Processing photo data: ${fotoAbsensi.size} photos")
                    var totalPhotoSize = 0L
                    val foundPhotoCount = fotoAbsensi.count { photoData ->
                        try {
                            (photoData as? Map<*, *>)?.let { photoMap ->
                                val name = photoMap["name"] as? String ?: ""
                                val sizeStr = photoMap["size"] as? String
                                val size = sizeStr?.toLongOrNull() ?: 0L
                                totalPhotoSize += size
                                AppLogger.d("Photo: $name, size: $size")
                                size > 0
                            } ?: false
                        } catch (e: Exception) {
                            AppLogger.e("Error processing photo data: ${e.message}")
                            false
                        }
                    }

                    AppLogger.d("Found $foundPhotoCount photos with a total size of $totalPhotoSize bytes")
                    if (foundPhotoCount > 0) {
                        val photoTitle = "Foto Absensi ($foundPhotoCount file)"
                        val uploadItem = UploadCMPItem(
                            id = itemId++,
                            title = photoTitle,
                            fullPath = "foto_absensi",
                            baseFilename = "",
                            data = gson.toJson(fotoAbsensi),
                            type = "image",
                            databaseTable = ""
                        )

                        uploadItems.add(uploadItem)
                        AppLogger.d("Adding photo upload item with ID ${uploadItem.id}")
                        adapter.setFileSize(uploadItem.id, totalPhotoSize)
                    } else {
                        AppLogger.w("No photo files found for upload")
                    }
                }

                adapter.setItems(uploadItems)

            } catch (e: Exception) {
                AppLogger.e("Error parsing upload data: ${e.message}")
                e.printStackTrace()
            }
        }


        Handler(Looper.getMainLooper()).postDelayed({
            if (counterTV.text == "0/0" && uploadItems.size > 0) {
                counterTV.text = "0/${uploadItems.size}"
            }
        }, 100)


        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        fun startUpload(itemsToUpload: List<UploadCMPItem> = uploadItems) {
            if (!AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withSingleAction(
                    this@HomePageActivity,
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

            val itemsToUpload = uploadItems.toList()


            AppLogger.d("itemsToUpload $itemsToUpload")

            // Start the upload process
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
                    this@HomePageActivity,
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

        // Modify the btnRetryUpload.setOnClickListener code block
        btnRetryUpload.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                // Create new upload items only for failed uploads
                val retryUploadItems = mutableListOf<UploadCMPItem>()
                var itemId = 0

                AppLogger.d("failedUploads $failedUploads")
                AppLogger.d("globalImageNameError $globalImageNameError")

                // Track the sizes of the original items to preserve them
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
                        "image" -> {
                            // For image uploads, filter only the failed images
                            if (globalImageNameError.isNotEmpty()) {
                                try {
                                    // Parse the original data
                                    val imageList = Gson().fromJson(
                                        failedItem.data,
                                        object : TypeToken<List<Map<String, Any>>>() {}.type
                                    ) as List<Map<String, String>>

                                    // Filter to only include images that are in the global error list
                                    val onlyFailedImages = imageList.filter { image ->
                                        val imageName = image["name"] ?: ""
                                        globalImageNameError.contains(imageName)
                                    }

                                    AppLogger.d("Original images: ${imageList.size}, Filtered failed images: ${onlyFailedImages.size}")

                                    // Only add to retry if there are failed images that match
                                    if (onlyFailedImages.isNotEmpty()) {
                                        // Convert back to JSON for retry
                                        val failedImagesJson = Gson().toJson(onlyFailedImages)

                                        // Extract the base title (without the count in parentheses)
                                        val baseTitle = failedItem.title.replace(
                                            Regex("\\s*\\(\\d+\\s+file[s]?\\)\\s*$"),
                                            ""
                                        ).trim()
                                        val newTitle = "$baseTitle (${onlyFailedImages.size} file)"

                                        val newItem = UploadCMPItem(
                                            id = itemId++,
                                            title = newTitle,
                                            fullPath = failedItem.fullPath,
                                            baseFilename = failedItem.baseFilename,
                                            data = failedImagesJson,
                                            type = failedItem.type,
                                            databaseTable = failedItem.databaseTable
                                        )

                                        retryUploadItems.add(newItem)

                                        AppLogger.d("Filtered images for retry: ${onlyFailedImages.map { it["name"] }}")
                                    } else {
                                        AppLogger.d("No matching failed images found - skipping this item")
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing image data for retry: ${e.message}")
                                    // If parsing fails, add the whole item for retry
                                    retryUploadItems.add(
                                        UploadCMPItem(
                                            id = itemId++,
                                            title = failedItem.title,
                                            fullPath = failedItem.fullPath,
                                            baseFilename = failedItem.baseFilename,
                                            data = failedItem.data,
                                            type = failedItem.type,
                                            databaseTable = failedItem.databaseTable
                                        )
                                    )
                                }
                            }
                        }

                        "json" -> {
                            // For JSON uploads (Panen or ESPB), add as is
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = failedItem.data,
                                    type = failedItem.type,
                                    databaseTable = failedItem.databaseTable
                                )
                            )
                        }

                        else -> {
                            retryUploadItems.add(
                                UploadCMPItem(
                                    id = itemId++,
                                    title = failedItem.title,
                                    fullPath = failedItem.fullPath,
                                    baseFilename = failedItem.baseFilename,
                                    data = failedItem.data,
                                    type = failedItem.type,
                                    databaseTable = failedItem.databaseTable
                                )
                            )
                        }
                    }
                }

                // Flag that we're in a retry operation
                isRetryOperation = true

// First update the adapter items
                adapter.updateItems(retryUploadItems)

// Then set the file sizes for each retry item
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

                // Clear global error variables to prepare for next upload
                globalImageUploadError = emptyList()
                globalImageNameError = emptyList()

                // Update UI elements
                counterTV.text = "0/${retryUploadItems.size}"
                titleTV.text = "Upload Data CMP"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))

                // Hide retry button, show upload button
                btnRetryUpload.visibility = View.GONE
                btnUploadDataCMP.visibility = View.VISIBLE

                startUpload(retryUploadItems)
            } else {
                AlertDialogUtility.withSingleAction(
                    this@HomePageActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
            }
        }

        closeDialogBtn.setOnClickListener {
            zipFileName = null
            zipFilePath = null
            uploadCMPViewModel.resetState()
            // (recyclerView.adapter as? UploadProgressCMPDataAdapter)?.onDestroy()
            dialog.dismiss()
        }

        uploadCMPViewModel.completedCount.observe(this) { completed ->
            val total = uploadCMPViewModel.totalCount.value ?: uploadItems.size
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
                    trackingIdsUpload = emptyList()
                    uploadCMPViewModel.getAllIds()
                    delay(500)

                    withContext(Dispatchers.Main) {

                        AppLogger.d("allSuccess $allSuccess")
                        if (allSuccess) {
                            // Reset retry flag since we succeeded
                            isRetryOperation = false

                            launch {
                                val processingComplete = processUploadResponses()

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
                            // Determine which items to include in failedUploads based on retry status
                            if (isRetryOperation) {
                                // If we're already in a retry operation, use the current adapter's items
                                val currentItems = adapter.getItems()

                                failedUploads = currentItems.filter { item ->
                                    val status = statusMap[item.id]
                                    status != AppUtils.UploadStatusUtils.SUCCESS
                                }
                            } else {
                                // First failure, use the original upload items
                                failedUploads = uploadItems.filter { item ->
                                    val status = statusMap[item.id]
                                    status != AppUtils.UploadStatusUtils.SUCCESS
                                }
                            }

                            AppLogger.d("Collected ${failedUploads.size} failed uploads for retry")

                            launch {
                                val processingComplete = processUploadResponses()

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
                // Clear previous data
                globalPanenIdsByPart.clear()
                globalEspbIdsByPart.clear()
                globalHektarPanenIdsByPart.clear()
                globalAbsensiPanenIdsByPart.clear()
                globalResponseJsonUploadList.clear()

                AppLogger.d("responseMap $responseMap")

                for ((_, response) in responseMap) {
                    response?.let {
                        // Check if response type is JSON
                        if (response.type == "json") {
                            globalResponseJsonUploadList.add(
                                ResponseJsonUpload(
                                    response.trackingId,
                                    response.nama_file,
                                    response.status,
                                    response.tanggal_upload,
                                    response.type
                                )
                            )

                            val keyJsonName = response.trackingId.toString()

                            if (response.success) {
                                try {
                                    // Extract table_ids from the response
                                    val tableIds = response.table_ids
                                    if (tableIds != null) {
                                        // Parse the table_ids to determine if it's PANEN or ESPB
                                        val tableIdsJson = JSONObject(tableIds)

                                        AppLogger.d(tableIdsJson.toString())

                                        if (tableIdsJson.has(AppUtils.DatabaseTables.PANEN)) {
                                            val panenIdsArray =
                                                tableIdsJson.getJSONArray(AppUtils.DatabaseTables.PANEN)
                                            val panenIds = (0 until panenIdsArray.length()).map {
                                                panenIdsArray.getInt(it)
                                            }
                                            globalPanenIdsByPart[keyJsonName] = panenIds
                                            AppLogger.d("Extracted PANEN IDs from response: $panenIds")
                                        } else if (tableIdsJson.has(AppUtils.DatabaseTables.ESPB)) {
                                            val espbIdsArray =
                                                tableIdsJson.getJSONArray(AppUtils.DatabaseTables.ESPB)
                                            val espbIds = (0 until espbIdsArray.length()).map {
                                                espbIdsArray.getInt(it)
                                            }
                                            globalEspbIdsByPart[keyJsonName] = espbIds
                                            AppLogger.d("Extracted ESPB IDs from response: $espbIds")
                                        } else if (tableIdsJson.has(AppUtils.DatabaseTables.HEKTAR_PANEN)) {
                                            val hektarPanenIdsArray =
                                                tableIdsJson.getJSONArray(AppUtils.DatabaseTables.HEKTAR_PANEN)
                                            val hektarPanenIds =
                                                (0 until hektarPanenIdsArray.length()).map {
                                                    hektarPanenIdsArray.getInt(it)
                                                }
                                            globalHektarPanenIdsByPart[keyJsonName] = hektarPanenIds
                                            AppLogger.d("Extracted Hektar Panen IDs from response: $hektarPanenIds")
                                        } else if (tableIdsJson.has(AppUtils.DatabaseTables.ABSENSI)) {
                                            val absensiPanenIdsArray =
                                                tableIdsJson.getJSONArray(AppUtils.DatabaseTables.ABSENSI)
                                            val absensiPanenIds =
                                                (0 until absensiPanenIdsArray.length()).map {
                                                    absensiPanenIdsArray.getInt(it)
                                                }
                                            globalAbsensiPanenIdsByPart[keyJsonName] =
                                                absensiPanenIds
                                            AppLogger.d("Extracted Absensi IDs from response: $absensiPanenIds")
                                        } else {

                                        }
                                    } else {
                                        AppLogger.w("No table_ids found in response for $keyJsonName")
                                        globalPanenIdsByPart[keyJsonName] = emptyList()
                                        globalEspbIdsByPart[keyJsonName] = emptyList()
                                        globalHektarPanenIdsByPart[keyJsonName] = emptyList()
                                        globalAbsensiPanenIdsByPart[keyJsonName] = emptyList()
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing table_ids for file $keyJsonName: ${e.message}")
                                    globalPanenIdsByPart[keyJsonName] = emptyList()
                                    globalEspbIdsByPart[keyJsonName] = emptyList()
                                    globalHektarPanenIdsByPart[keyJsonName] = emptyList()
                                    globalAbsensiPanenIdsByPart[keyJsonName] = emptyList()
                                }
                            } else {
                                globalPanenIdsByPart[keyJsonName] = emptyList()
                                globalEspbIdsByPart[keyJsonName] = emptyList()
                                globalHektarPanenIdsByPart[keyJsonName] = emptyList()
                                globalAbsensiPanenIdsByPart[keyJsonName] = emptyList()
                            }
                        } else if (response.type == "image") {
                            globalResponseJsonUploadList.add(
                                ResponseJsonUpload(
                                    trackingId = 0,
                                    nama_file = "",
                                    status = 0,
                                    tanggal_upload = "",
                                    type = response.type
                                )
                            )
                            if (!response.success) {
                                globalImageUploadError = response.imageFullPath ?: emptyList()
                                globalImageNameError = response.imageName ?: emptyList()
                                AppLogger.d("Failed images: ${globalImageNameError.size}")
                                AppLogger.d("Failed image paths: $globalImageUploadError")
                                AppLogger.d("Failed image names: $globalImageNameError")
                            } else {

                            }
                        } else {
                            AppLogger.d("Skipping non-JSON upload: type = ${response.type}")
                        }
                    }
                }

//                AppLogger.d("Stored IDs by part: ${globalPanenIdsByPart.keys}")
//                AppLogger.d("Total IDs - PANEN: ${globalPanenIds.size}, ESPB: ${globalESPBIds.size}")
            }
        }
    }

    private fun processUploadResponses(): Boolean {
        if (globalResponseJsonUploadList.isEmpty()) {
            AppLogger.d("No responses found in globalResponseJsonUploadList")
            return false
        }

        AppLogger.d("globalResponseJsonUploadList $globalResponseJsonUploadList")

        var successfullyProcessedCount = 0

        // Process each successful upload (status codes 1, 2, 3)
        for (responseInfo in globalResponseJsonUploadList) {

            if (responseInfo.type == "image") {
                AppLogger.d("Detected image type for response, returning true early")
                return true
            }
            // Check if the status code indicates success
            if (responseInfo.status == 1 || responseInfo.status == 2 || responseInfo.status == 3) {
//                val fileName = responseInfo.nama_file
                val trackingId = responseInfo.trackingId.toString()

                try {
                    // Get the PANEN IDs for this file
                    val panenIds = globalPanenIdsByPart[trackingId] ?: emptyList()

                    if (panenIds.isNotEmpty()) {
                        AppLogger.d("Found ${panenIds.size} panen IDs for file $trackingId: $panenIds")

                        // Update status_upload for PANEN IDs
                        panenViewModel.updateStatusUploadPanen(panenIds, responseInfo.status)
                        AppLogger.d("Updated status_upload to ${responseInfo.status} for panen IDs: $panenIds")
                    } else {
                        AppLogger.d("No panen IDs found for file $trackingId")
                    }

                    // Get the ESPB IDs for this file
                    val espbIds = globalEspbIdsByPart[trackingId] ?: emptyList()

                    if (espbIds.isNotEmpty()) {
                        AppLogger.d("Found ${espbIds.size} ESPB IDs for file $trackingId: $espbIds")

                        // Update status_upload for ESPB IDs
                        weightBridgeViewModel.updateStatusUploadEspbCmpSp(
                            espbIds,
                            responseInfo.status
                        )
                        AppLogger.d("Updated status_upload to ${responseInfo.status} for ESPB IDs: $espbIds")
                    } else {
                        AppLogger.d("No ESPB IDs found for file $trackingId")
                    }

                    val hektarPanenIds = globalHektarPanenIdsByPart[trackingId] ?: emptyList()

                    if (hektarPanenIds.isNotEmpty()) {
                        AppLogger.d("Found ${hektarPanenIds.size} hektar panen IDs for file $trackingId: $hektarPanenIds")

                        // Update status_upload for ESPB IDs
                        hektarPanenViewModel.updateStatusUploadHektarPanen(
                            hektarPanenIds,
                            responseInfo.status
                        )
                        AppLogger.d("Updated status_upload to ${responseInfo.status} for hektar panen  IDs: $hektarPanenIds")
                    } else {
                        AppLogger.d("No hektar panen  IDs found for file $trackingId")
                    }

                    val absensiPanenIds = globalAbsensiPanenIdsByPart[trackingId] ?: emptyList()
                    if (absensiPanenIds.isNotEmpty()) {
                        AppLogger.d("Found ${absensiPanenIds.size} absensi IDs for file $trackingId: $absensiPanenIds")

                        absensiViewModel.updateStatusUploadAbsensiPanen(
                            absensiPanenIds,
                            responseInfo.status
                        )
                        AppLogger.d("Updated status_upload to ${responseInfo.status} for absensi IDs: $absensiPanenIds")
                    } else {
                        AppLogger.d("No absensi IDs found for file $trackingId")
                    }

                    val jsonResultTableIds = createJsonTableNameMapping(trackingId)
                    AppLogger.d("Processing successful upload: $trackingId with tracking ID $trackingId, status code: ${responseInfo.status}")

                    // Update or insert data using the original date
                    uploadCMPViewModel.UpdateOrInsertDataUpload(
                        trackingId,
                        responseInfo.nama_file,
                        responseInfo.status,
                        responseInfo.tanggal_upload,
                        jsonResultTableIds
                    )

                    successfullyProcessedCount++
                } catch (e: Exception) {
                    AppLogger.e("Error processing upload for file $trackingId: ${e.message}")
                }
            } else {
                AppLogger.d("Skipping upload with unsuccessful status code: ${responseInfo.status} for file ${responseInfo.nama_file}")
            }
        }

        AppLogger.d("Successfully processed $successfullyProcessedCount uploads")
        return successfullyProcessedCount > 0
    }

    private fun formatDateString(dateStr: String): String {
        if (!dateStr.contains("T")) return dateStr

        return try {
            // Use built-in API to parse ISO date
            val instant = Instant.parse(dateStr)
            // Format to desired pattern
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            AppLogger.e("Error formatting date: ${e.message}")
            dateStr
        }
    }


    private fun setupDownloadDialog() {

        dialog = Dialog(this)

        val view = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        dialog.setContentView(view)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        val recyclerView = view.findViewById<RecyclerView>(R.id.features_recycler_view)
        adapter = DownloadProgressDatasetAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)


        val titleTV = view.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Progress Import Dataset..."
        val counterTV = view.findViewById<TextView>(R.id.counter_dataset)

        val closeStatement = view.findViewById<TextView>(R.id.close_progress_statement)

        val retryDownloadDataset =
            view.findViewById<MaterialButton>(R.id.btnRetryDownloadDataset)
        val cancelDownloadDataset =
            view.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val containerDownloadDataset =
            view.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        cancelDownloadDataset.setOnClickListener {
            isTriggerButtonSinkronisasiData = false
            dialog.dismiss()
        }
        retryDownloadDataset.setOnClickListener {


//            val storedList = prefManager!!.datasetMustUpdate // Retrieve list

            containerDownloadDataset.visibility = View.GONE
            cancelDownloadDataset.visibility = View.GONE
            retryDownloadDataset.visibility = View.GONE
            closeStatement.visibility = View.GONE
            startDownloads()
        }

        datasetViewModel.downloadStatuses.observe(this) { statusMap ->

            val downloadItems = statusMap.map { (dataset, resource) ->
                when (resource) {
                    is DatasetViewModel.Resource.Success -> {
                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isCompleted = false,
                            isExtractionCompleted = false,
                            isStoringCompleted = true, // Final state is storage complete
                            message = resource.message
                        )
                    }

                    is DatasetViewModel.Resource.Error -> {
                        if (!hasShownErrorDialog) {
                            val errorMessage = resource.message ?: "Unknown error occurred"
                            if (errorMessage.contains("host", ignoreCase = true)) {
                                showErrorDialog("Mohon cek koneksi Internet Smartphone anda!")
                            } else {
                                showErrorDialog(errorMessage)
                            }
                            hasShownErrorDialog = true
                        }
                        DownloadItem(dataset = dataset, error = resource.message)
                    }

                    is DatasetViewModel.Resource.Loading -> {
                        DownloadItem(
                            dataset = dataset,
                            progress = resource.progress,
                            isLoading = true
                        )
                    }

                    is DatasetViewModel.Resource.Extracting -> {

                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isLoading = false,
                            isExtracting = true,
                            message = resource.message
                        )
                    }

                    is DatasetViewModel.Resource.Storing -> {
                        AppLogger.d("Download Status: $dataset is being stored")
                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isLoading = false,
                            isExtracting = false,
                            isStoring = true,
                            message = resource.message
                        )

                    }

                    is DatasetViewModel.Resource.UpToDate -> {

                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isUpToDate = true,
                            message = resource.message
                        )
                    }
                }
            }

            adapter.updateItems(downloadItems)

            val completedCount =
                downloadItems.count { it.isStoringCompleted || it.isUpToDate || it.error != null }
            AppLogger.d("Progress: $completedCount/${downloadItems.size} completed")
            counterTV.text = "$completedCount/${downloadItems.size}"

            if (downloadItems.all { it.isStoringCompleted || it.isUpToDate || it.error != null }) {

                if (downloadItems.any { it.isStoringCompleted || it.isUpToDate }) {
                    val indonesiaTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
                    val simpleDateFormat =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    simpleDateFormat.timeZone = indonesiaTimeZone
                    val currentDateTimeIndonesia = simpleDateFormat.format(Date())

                    prefManager!!.lastSyncDate = currentDateTimeIndonesia

                    _globalLastSync.value = currentDateTimeIndonesia
                }

                if (prefManager!!.isFirstTimeLaunch && downloadItems.any { it.isStoringCompleted || it.isUpToDate || it.error != null }) {
                    prefManager!!.isFirstTimeLaunch = false
                    AppLogger.d("First-time launch flag updated to false")
                }

                // Show appropriate UI based on errors
                if (downloadItems.any { it.error != null }) {
                    containerDownloadDataset.visibility = View.VISIBLE
                    retryDownloadDataset.visibility = View.VISIBLE
                    cancelDownloadDataset.visibility = View.VISIBLE
                } else {
                    containerDownloadDataset.visibility = View.VISIBLE
                    cancelDownloadDataset.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startDownloadsV2(
        datasetRequests: List<DatasetRequest>,
        previewData: String? = null,
        titleDialog:String? = "Sinkronisasi Restan & Dataset"
    ) {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)
        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = titleDialog


        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
        val counterSizeFile = dialogView.findViewById<LinearLayout>(R.id.counterSizeFile)
        counterSizeFile.visibility = View.GONE

        // Get all buttons
        val closeDialogBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val btnSinkronisasiDataset = dialogView.findViewById<MaterialButton>(R.id.btnUploadDataCMP)
        val btnRetryDownload = dialogView.findViewById<MaterialButton>(R.id.btnRetryDownloadDataset)

        // Update button text to reflect download operation
        btnSinkronisasiDataset.text = "Sinkronisasi Data"
        btnSinkronisasiDataset.setIconResource(R.drawable.baseline_refresh_24) // Assuming you have this icon

        val containerDownloadDataset =
            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        containerDownloadDataset.visibility = View.VISIBLE

        // Initially show only close and download buttons
        closeDialogBtn.visibility = View.VISIBLE
        btnSinkronisasiDataset.visibility = View.VISIBLE
        btnRetryDownload.visibility = View.GONE

        // Create upload items from dataset requests (we'll reuse the existing adapter)
        val downloadItems = mutableListOf<UploadCMPItem>()

        AppLogger.d("previewData $previewData")
        var itemId = 0
        datasetRequests.forEach { request ->
            val itemTitle = if (!request.estateAbbr.isNullOrEmpty()) {
                "Master TPH ${request.estateAbbr}"
            } else {
                "${request.dataset}"
            }
            val itemData =
                if ((request.dataset == AppUtils.DatasetNames.sinkronisasiRestan || request.dataset == AppUtils.DatasetNames.sinkronisasiDataPanen) && !previewData.isNullOrEmpty()) {
                    previewData
                } else {
                    ""
                }

            downloadItems.add(
                UploadCMPItem(
                    id = itemId++,
                    title = itemTitle,
                    fullPath = "",
                    baseFilename = request.estateAbbr ?: "",
                    data = itemData,  // Use the data we just determined
                    type = "",
                    databaseTable = ""
                )
            )
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (counterTV.text == "0/0" && downloadItems.size > 0) {
                counterTV.text = "0/${downloadItems.size}"
            }
        }, 100)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UploadProgressCMPDataAdapter(downloadItems)
        recyclerView.adapter = adapter

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        fun startDownload(
            requestsToDownload: List<DatasetRequest> = datasetRequests,
            itemsToShow: List<UploadCMPItem> = downloadItems
        ) {
            // Check network connectivity first
            if (!AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withSingleAction(
                    this@HomePageActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
                return
            }

            // Disable buttons during download
            btnSinkronisasiDataset.isEnabled = false
            closeDialogBtn.isEnabled = false
            btnRetryDownload.isEnabled = false
            btnSinkronisasiDataset.alpha = 0.7f
            closeDialogBtn.alpha = 0.7f
            btnRetryDownload.alpha = 0.7f
            btnSinkronisasiDataset.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            closeDialogBtn.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            btnRetryDownload.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

            // Reset title color
            titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))
            titleTV.text = "Sedang Melakukan Sinkronisasi..."

            datasetViewModel.downloadDataset(requestsToDownload, itemsToShow, false)
        }

        btnSinkronisasiDataset.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Perbarui",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_sinkronisasi_master_dataset),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = { startDownload() },
                    cancelFunction = { }
                )
            } else {
                AlertDialogUtility.withSingleAction(
                    this@HomePageActivity,
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

        var failedRequests: List<DatasetRequest> = listOf()
        btnRetryDownload.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                // Create new download items only for failed requests
                val retryDownloadItems = mutableListOf<UploadCMPItem>()
                var itemId = 0

                AppLogger.d("failedRequests $failedRequests")

                failedRequests.forEach { request ->
                    retryDownloadItems.add(
                        UploadCMPItem(
                            id = itemId++,
                            title = "${request.estateAbbr} - ${request.dataset}",
                            fullPath = "",
                            baseFilename = request.estateAbbr ?: "",
                            data = "",
                            type = "",
                            databaseTable = ""
                        )
                    )
                }

                // Clear and update the RecyclerView with only failed items
                adapter.updateItems(retryDownloadItems)

                // Reset adapter state (progress bars, status icons, etc.)
                adapter.resetState()

                // Reset view model state
                datasetViewModel.resetState()

                // Update UI elements
                counterTV.text = "0/${retryDownloadItems.size}"
                titleTV.text = "Download Dataset"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))

                // Hide retry button, show download button
                btnRetryDownload.visibility = View.GONE
                btnSinkronisasiDataset.visibility = View.VISIBLE

                // Start download with only failed requests
                startDownload(failedRequests, retryDownloadItems)
            } else {
                AlertDialogUtility.withSingleAction(
                    this@HomePageActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
            }
        }


        closeDialogBtn.setOnClickListener {

            refreshPanenCount()
            datasetViewModel.processingComplete.removeObservers(this)
            datasetViewModel.itemProgressMap.removeObservers(this)
            datasetViewModel.completedCount.removeObservers(this)
            datasetViewModel.itemStatusMap.removeObservers(this)
            datasetViewModel.itemErrorMap.removeObservers(this)

            datasetViewModel.resetState()

            isTriggerFeatureInspection = false
            dialog.dismiss()
        }

        // Observe completed count (connect this to your actual download view model)
        datasetViewModel.completedCount.observe(this) { completed ->
            val total = datasetViewModel.totalCount.value ?: downloadItems.size
            counterTV.text = "$completed/$total"
        }

        // Observe download progress
        datasetViewModel.itemProgressMap.observe(this) { progressMap ->
            // Update progress for each item
            for ((id, progress) in progressMap) {
                AppLogger.d("Progress update for item $id: $progress%")
                adapter.updateProgress(id, progress)
            }

            // Update title if any download is in progress
            if (progressMap.values.any { it in 1..99 }) {
                titleTV.text = "Sedang Melakukan Sinkronisasi..."
            }

        }


        datasetViewModel.processingComplete.observe(this) { isComplete ->
            if (isComplete) {
                val currentStatusMap = datasetViewModel.itemStatusMap.value ?: emptyMap()

                // Separate successful and failed downloads
                val successfulIds = mutableListOf<Int>()
                val failedIds = mutableListOf<Int>()

                currentStatusMap.forEach { (id, status) ->
                    if (status == AppUtils.UploadStatusUtils.DOWNLOADED || status == AppUtils.UploadStatusUtils.UPTODATE || status == AppUtils.UploadStatusUtils.UPDATED) {
                        successfulIds.add(id)
                    } else {
                        failedIds.add(id)
                    }
                }

                // Get successful estate abbreviations
                val successfulEstates = datasetRequests.filterIndexed { index, _ ->
                    index in successfulIds
                }.mapNotNull { it.estateAbbr }

                // Store failed requests for retry
                failedRequests = datasetRequests.filterIndexed { index, _ ->
                    index in failedIds
                }
//
//                // Always reset successful estates, even if some failed
//                if (successfulEstates.isNotEmpty()) {
//                    resetEstateSelection(successfulEstates)
//                }

                // Refresh master data
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        // Update UI based on download results
                        if (failedIds.isEmpty()) {
                            // All successful
                            titleTV.text = "Sinkronisasi Berhasil"
                            titleTV.setTextColor(
                                ContextCompat.getColor(
                                    titleTV.context,
                                    R.color.greenDarker
                                )
                            )
                            btnSinkronisasiDataset.visibility = View.GONE
                            btnRetryDownload.visibility = View.GONE

                            val indonesiaTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
                            val simpleDateFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            simpleDateFormat.timeZone = indonesiaTimeZone
                            val currentDateTimeIndonesia = simpleDateFormat.format(Date())

                            prefManager!!.lastSyncDate = currentDateTimeIndonesia

                            _globalLastSync.value = currentDateTimeIndonesia
                        } else {
                            // Some or all failed
                            AppLogger.d("ada yg error update dataset")
                            AppLogger.d(failedIds.toString())
                            titleTV.text = "Terjadi Kesalahan Sinkronisasi"
                            titleTV.setTextColor(
                                ContextCompat.getColor(
                                    titleTV.context,
                                    R.color.colorRedDark
                                )
                            )
                            btnSinkronisasiDataset.visibility = View.GONE
                            btnRetryDownload.visibility = View.VISIBLE
                            btnRetryDownload.isEnabled = true
                            btnRetryDownload.alpha = 1f
                            btnRetryDownload.iconTint = ColorStateList.valueOf(Color.WHITE)
                        }

                        // Enable close button
                        closeDialogBtn.isEnabled = true
                        closeDialogBtn.alpha = 1f
                        closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)

                    }
                }
            }
        }

        datasetViewModel.itemStatusMap.observe(this) { statusMap ->
            // Update status for each item
            for ((id, status) in statusMap) {
                // No need for mapping - just pass the status directly to adapter
                adapter.updateStatus(id, status)
            }
        }

        // Observe errors for each item
        datasetViewModel.itemErrorMap.observe(this) { errorMap ->
            for ((id, error) in errorMap) {
                if (!error.isNullOrEmpty()) {
                    adapter.updateError(id, error)
                }
            }

            if (errorMap.values.any { !it.isNullOrEmpty() }) {
                titleTV.text = "Terjadi Kesalahan Download"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.colorRedDark))
            }
        }
    }

    private fun extractEmployeesFromJson(jsonString: String, kemandoranId: String): List<String> {
        return try {
            val jsonObj = JSONObject(jsonString)
            val employeeArray = jsonObj.optString(kemandoranId, "")
            employeeArray.split(",").filter { it.trim().isNotEmpty() }
        } catch (e: Exception) {
            AppLogger.e("Error: ${e.message}")
            emptyList()
        }
    }

    private fun startDownloads(previewData: String? = null, titleDialog : String?= null) {
        val regionalIdString = prefManager!!.regionalIdUserLogin
        val estateIdString = prefManager!!.estateIdUserLogin
        val afdelingIdString = prefManager!!.afdelingIdUserLogin
        val lastModifiedDatasetEstate = prefManager!!.lastModifiedDatasetEstate
        val lastModifiedDatasetTPH = prefManager!!.lastModifiedDatasetTPH
        val lastModifiedDatasetJenisTPH = prefManager!!.lastModifiedDatasetJenisTPH
        val lastModifiedDatasetBlok = prefManager!!.lastModifiedDatasetBlok
        val lastModifiedDatasetKemandoran = prefManager!!.lastModifiedDatasetKemandoran
        val lastModifiedDatasetPemanen = prefManager!!.lastModifiedDatasetPemanen
        val lastModifiedDatasetTransporter = prefManager!!.lastModifiedDatasetTransporter
        val lastModifiedDatasetKendaraan = prefManager!!.lastModifiedDatasetKendaraan
        val lastModifiedSettingJSON = prefManager!!.lastModifiedSettingJSON

        if (estateIdString.isNullOrEmpty() || estateIdString.isBlank()) {
            AppLogger.d("Downloads: Estate ID is null or empty, aborting download")
            showErrorDialog("Estate ID is not valid. Current value: '$estateIdString'")
            loadingDialog.dismiss()
            return
        }
        try {
            val estateId = estateIdString.toInt()
            if (estateId <= 0) {
                AppLogger.d("Downloads: Estate ID is not a valid positive number: $estateId")
                showErrorDialog("Estate ID must be a positive number")
                loadingDialog.dismiss()
                return
            }

//            prefManager!!.afdelingIdUserLogin = null

            AppLogger.d("prefManager!!.afdelingIdUserLogin ${prefManager!!.afdelingIdUserLogin}")

            val filteredRequests = if (isTriggerButtonSinkronisasiData) {
                // Get datasets - estates are already loaded from the click handler
                getDatasetsToDownload(
                    regionalIdString!!.toInt(),
                    estateId,
                    afdelingIdString!!,
                    lastModifiedDatasetEstate,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetJenisTPH,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetKemandoran,
                    lastModifiedDatasetTransporter,
                    lastModifiedDatasetKendaraan,
                    lastModifiedSettingJSON
                )
            } else {
                getDatasetsToDownload(
                    regionalIdString!!.toInt(),
                    estateId,
                    afdelingIdString!!,
                    lastModifiedDatasetEstate,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetJenisTPH,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetKemandoran,
                    lastModifiedDatasetTransporter,
                    lastModifiedDatasetKendaraan,
                    lastModifiedSettingJSON
                ).filterNot { prefManager!!.datasetMustUpdate.contains(it.dataset) }
            }

            // Dismiss loading dialog if it was shown
            if (isTriggerButtonSinkronisasiData) {
                loadingDialog.dismiss()
            }

            if (filteredRequests.isNotEmpty()) {
                if (isTriggerButtonSinkronisasiData || isTriggerFeatureInspection) {
                    startDownloadsV2(filteredRequests, previewData, titleDialog)
                } else {
                    dialog.show()
                    datasetViewModel.downloadMultipleDatasets(filteredRequests)
                }
            } else {
                AppLogger.d("All datasets are up-to-date, no download needed.")
            }
        } catch (e: NumberFormatException) {
            loadingDialog.dismiss()
            AppLogger.d("Downloads: Failed to parse Estate ID to integer: ${e.message}")
            showErrorDialog("Invalid Estate ID format: ${e.message}")
        }
    }

    private fun getDatasetsToDownload(
        regionalId: Int,
        estateId: Int,
        afdelingId: String,
        lastModifiedDatasetEstate: String?,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetJenisTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
        lastModifiedDatasetKendaraan: String?,
        lastModifiedSettingJSON: String?
    ): List<DatasetRequest> {
        val datasets = mutableListOf<DatasetRequest>()
        AppLogger.d("prefmanager id ${prefManager!!.idUserLogin}")
        val jabatan = prefManager!!.jabatanUserLogin
        val regionalUser = prefManager!!.regionalIdUserLogin!!.toInt()
        val isKeraniTimbang =
            jabatan!!.contains(AppUtils.ListFeatureByRoleUser.KeraniTimbang, ignoreCase = true)

        val isMandor1 =
            jabatan!!.contains(AppUtils.ListFeatureByRoleUser.Mandor1, ignoreCase = true)

        val isAsisten =
            jabatan!!.contains(AppUtils.ListFeatureByRoleUser.Asisten, ignoreCase = true)

        val isMandorPanen =
            jabatan!!.contains(AppUtils.ListFeatureByRoleUser.MandorPanen, ignoreCase = true)

        if (isTriggerButtonSinkronisasiData && !isKeraniTimbang) {
            // Get all estate timestamps directly from prefManager
            val estateTimestamps = prefManager!!.getMasterTPHEstateLastModifiedMap()
            AppLogger.d("Estate timestamps (${estateTimestamps.size} estates):")

            // Check if estateTimestamps is not empty before proceeding
            if (estateTimestamps.isNotEmpty()) {
                // For each estate in the timestamps map
                estateTimestamps.forEach { (abbr, timestamp) ->
                    AppLogger.d("  $abbr: $timestamp")

                    // Find the estate ID from the allEstatesList
                    val estate = datasetViewModel.allEstatesList.value?.find { it.abbr == abbr }
                    val estateId = estate?.id?.toInt()
                    val estateName = estate?.nama ?: "Unknown Estate"

                    if (estateId != null) {
                        AppLogger.d("Adding estate dataset: $abbr ($estateName)")
                        datasets.add(
                            DatasetRequest(
                                estate = estateId,
                                estateAbbr = abbr,
                                lastModified = timestamp,
                                dataset = AppUtils.DatasetNames.tph
                            )
                        )
                    } else {
                        AppLogger.d("Skipping estate $abbr - could not find estate ID")
                    }
                }
            } else {
                AppLogger.d("No estate timestamps found to process")
            }
        }


        if (isTriggerFeatureInspection && (isMandor1 || isAsisten)) {
            AppLogger.d(isTriggerFeatureInspection.toString())
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiDataPanen
                )
            )
        }

        // Add sinkronisasiRestan dataset for Mandor1 and Asisten when sync button triggered
        if (isTriggerButtonSinkronisasiData && (isMandor1 || isAsisten)) {
            datasets.add(
                DatasetRequest(
                    afdeling = afdelingId,
                    estate = estateId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.sinkronisasiRestan
                )
            )
        }

        if (isTriggerButtonSinkronisasiData) {
            datasets.add(
                DatasetRequest(
                    lastModified = null,
                    idUser = prefManager!!.idUserLogin,
                    dataset = AppUtils.DatasetNames.sinkronisasiDataUser
                )
            )
        }

        if (isMandorPanen) {
            datasets.add(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                ),
            )
        }

        if (isKeraniTimbang) {
            datasets.add(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetBlok,
                    dataset = AppUtils.DatasetNames.blok
                ),
            )
            datasets.add(
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
            )
            datasets.add(
                DatasetRequest(
                    regional = regionalId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                )
            )
        } else {
            datasets.add(
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetTPH,
                    dataset = AppUtils.DatasetNames.tph
                ),
            )

            datasets.add(
                DatasetRequest(
                    estate = estateId,
                    lastModified = lastModifiedDatasetPemanen,
                    dataset = AppUtils.DatasetNames.pemanen
                ),
            )

            datasets.add(
                DatasetRequest(
                    regional = regionalUser,
                    lastModified = lastModifiedDatasetEstate,
                    dataset = AppUtils.DatasetNames.estate
                ),
            )
        }

        datasets.addAll(
            listOf(
                DatasetRequest(
                    regional = regionalId,
                    lastModified = null,
                    dataset = AppUtils.DatasetNames.mill
                ),
                DatasetRequest(
                    lastModified = lastModifiedDatasetJenisTPH,
                    dataset = AppUtils.DatasetNames.jenisTPH
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

    private fun refreshPanenCount() {
        lifecycleScope.launch {
            try {
                // Create a deferred result for the count operation
                val countDeferred = async { panenViewModel.loadPanenCountApproval() }
                val newCount = countDeferred.await()

                // Update the UI with the new count
                withContext(Dispatchers.Main) {
                    featureAdapter.updateCount(
                        "Rekap panen dan restan",
                        newCount.toString()
                    )
                    featureAdapter.hideLoadingForFeature("Rekap panen dan restan")

                    // Optionally log the updated count
                    AppLogger.d("Panen count refreshed: $newCount")
                }
            } catch (e: Exception) {
                AppLogger.e("Error refreshing panen count: ${e.message}")
            }
        }
    }

    private fun hasRole(role: String): Boolean {
        return prefManager!!.jabatanUserLogin?.contains(role, ignoreCase = true) ?: false
    }


    private fun showErrorDialog(errorMessage: String) {
        AppLogger.d("Showing error dialog with message: $errorMessage")
        AlertDialogUtility.withSingleAction(
            this@HomePageActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)}, $errorMessage",
            "warning.json",
            R.color.colorRedDark
        ) {
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

    @SuppressLint("SetTextI18n")
    private fun setupTitleAppNameAndVersion() {
        val appVersion = AppUtils.getAppVersion(this)
        findViewById<TextView>(R.id.titleAppNameAndVersion).text = "CMP - $appVersion"
    }


    private fun setupName() {
        val userName = prefManager!!.nameUserLogin ?: "Unknown"
        val userInfo = buildString {
            userName?.takeIf { it.isNotEmpty() }?.let { append(formatToCamelCase(it)) }
            prefManager!!.jabatanUserLogin?.takeIf { it.isNotEmpty() }?.let {
                if (length > 0) append(" - ")
                append(it)
            }
        }
        findViewById<TextView>(R.id.userSection).text = userInfo
        globalLastSync.observe(this) { timestamp ->

            val formattedDate = if (timestamp.isNullOrEmpty()) {
                "-"
            } else {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM, HH:mm", Locale("id", "ID"))
                try {
                    val date = inputFormat.parse(timestamp)
                    outputFormat.format(date ?: "-")
                } catch (e: Exception) {
                    "-"
                }
            }

            findViewById<TextView>(R.id.lastUpdate).text = "Update:\n$formattedDate"
        }

    }

    private fun setupCheckingAfterLogoutUser() {
        if (prefManager!!.datasetMustUpdate.isEmpty()) {
            startDownloads()
        }
    }

    private fun setupLogout() {

//        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
//
//        btnLogout.setOnClickListener {
//            vibrate()
//            btnLogout.isEnabled = false
//            AlertDialogUtility.withTwoActions(
//                this,
//                "Logout",
//                getString(R.string.confirmation_dialog_title),
//                getString(R.string.al_confirm_logout),
//                "warning.json",
//                ContextCompat.getColor(this, R.color.colorRedDark),
//                function = {
//                    prefManager!!.isFirstTimeLaunch = false
//                    prefManager!!.rememberLogin = false
//                    prefManager!!.token = null
//                    prefManager!!.username = null
//                    prefManager!!.password = null
//                    prefManager!!.nameUserLogin = null
//                    prefManager!!.idUserLogin = 0  // Resetting Int to 0
//                    prefManager!!.jabatanUserLogin = null
//                    prefManager!!.estateUserLogin = null
//                    prefManager!!.estateUserLengkapLogin = null
//                    prefManager!!.estateIdUserLogin = null
//                    prefManager!!.regionalIdUserLogin = null
//                    prefManager!!.companyIdUserLogin = null
//                    prefManager!!.companyAbbrUserLogin = null
//                    prefManager!!.companyNamaUserLogin = null
//                    prefManager!!.lastModifiedDatasetTPH = null
//                    prefManager!!.lastModifiedDatasetKemandoran = null
//                    prefManager!!.lastModifiedDatasetPemanen = null
//                    prefManager!!.lastModifiedDatasetTransporter = null
//                    prefManager!!.lastModifiedDatasetBlok = null
//                    prefManager!!.lastSyncDate = null
//                    prefManager!!.clearDatasetMustUpdate()
//
//                    datasetViewModel.clearAllData()
//
//                    val intent = Intent(this, LoginActivity::class.java)
//                    Toasty.success(this, "Berhasil Logout", Toast.LENGTH_LONG, true).show()
//                    startActivity(intent)
//                    finishAffinity()
//                    btnLogout.isEnabled = true
//                },
//                cancelFunction = {
//
//                    btnLogout.isEnabled = true
//                }
//            )
//        }

    }


    private fun initViewModel() {
        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]

        val factory2 = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory2)[PanenViewModel::class.java]

        val factory3 = WeighBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory3)[WeighBridgeViewModel::class.java]

        val factory4 = UploadCMPViewModel.UploadCMPViewModelFactory(application)
        uploadCMPViewModel = ViewModelProvider(this, factory4)[UploadCMPViewModel::class.java]

        val appRepository = AppRepository(application)
        val factory5 = ESPBViewModel.ESPBViewModelFactory(appRepository)
        espbViewModel = ViewModelProvider(this, factory5)[ESPBViewModel::class.java]

        val factoryHektarVM = HektarPanenViewModel.HektarPanenViewModelFactory(appRepository)
        hektarPanenViewModel =
            ViewModelProvider(this, factoryHektarVM)[HektarPanenViewModel::class.java]

        val factory6 = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel = ViewModelProvider(this, factory6)[AbsensiViewModel::class.java]

        val factoryInspection = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel =
            ViewModelProvider(this, factoryInspection)[InspectionViewModel::class.java]
    }


    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(it)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        } else {
            startDownloads()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            val deniedPermissions =
                permissions.filterIndexed { i, _ -> grantResults[i] != PackageManager.PERMISSION_GRANTED }

            if (deniedPermissions.isNotEmpty()) {
                showStackedSnackbar(deniedPermissions)
            } else {
                startDownloads()
            }
        }
    }

    private fun showStackedSnackbar(deniedPermissions: List<String>) {
        val message = buildString {
            append("The app needs the following permissions for full functionality:\n")
            deniedPermissions.forEach { append("- ${it.replace("android.permission.", "")}\n") }
            append("\nPlease enable them in Settings.")
        }

        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Settings") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }.apply {
                view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines =
                    7
            }.show()
    }


}