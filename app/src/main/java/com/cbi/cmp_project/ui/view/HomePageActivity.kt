package com.cbi.cmp_project.ui.view

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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Entity
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.database.KaryawanDao
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.databinding.ActivityHomePageBinding
import com.cbi.cmp_project.ui.adapter.DisplayType
import com.cbi.cmp_project.ui.adapter.DownloadItem
import com.cbi.cmp_project.ui.adapter.DownloadProgressDatasetAdapter
import com.cbi.cmp_project.ui.adapter.FeatureCard
import com.cbi.cmp_project.ui.adapter.FeatureCardAdapter
import com.cbi.cmp_project.ui.view.Absensi.FeatureAbsensiActivity
import com.cbi.cmp_project.ui.view.Absensi.ListAbsensiActivity
import com.cbi.cmp_project.ui.adapter.UploadCMPItem
import com.cbi.cmp_project.ui.adapter.UploadProgressAdapter
import com.cbi.cmp_project.ui.adapter.UploadProgressCMPDataAdapter
import com.cbi.cmp_project.ui.view.espb.ListHistoryESPBActivity
import com.cbi.cmp_project.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.ui.view.weighBridge.ListHistoryWeighBridgeActivity
import com.cbi.cmp_project.ui.view.weighBridge.ScanWeighBridgeActivity

import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.ESPBViewModel
import com.cbi.cmp_project.ui.viewModel.PanenViewModel
import com.cbi.cmp_project.ui.viewModel.UploadCMPViewModel
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager

import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomePageActivity : AppCompatActivity() {

    private lateinit var featureAdapter: FeatureCardAdapter
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private lateinit var uploadCMPViewModel: UploadCMPViewModel
    private var isTriggerButtonSinkronisasiData: Boolean = false
    private lateinit var dialog: Dialog
    private var countPanenTPH: Int = 0  // Global variable for count
    private var countPanenTPHApproval: Int = 0  // Global variable for count
    private var counteSPBWBScanned: Int = 0  // Global variable for count
    private var countActiveESPB: Int = 0  // Global variable for count


    private var hasShownErrorDialog = false  // Add this property
    private val permissionRequestCode = 1001
    private lateinit var adapter: DownloadProgressDatasetAdapter

    private var globalESPBList: List<Map<String, Any>> = emptyList()
    private var globalPanenList: List<Map<String, Any>> = emptyList()

    private var globalPanenIds: List<Int> = emptyList()
    private var globalESPBIds: List<Int> = emptyList()
    private var zipFilePath: String? = null
    private var zipFileName: String? = null
    private var trackingIdsUpload: List<Int> = emptyList()
    private lateinit var allUploadZipFilesToday: MutableList<File>


    private lateinit var datasetViewModel: DatasetViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("HomePage: onCreate started")

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)

        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupDownloadDialog()
        setupName()
        checkPermissions()
        setupRecyclerView()


        panenViewModel.updateStatus.observeOnce(this) { success ->
            if (success) {
                AppLogger.d("✅ Panen Archive Updated Successfully")
            } else {
                AppLogger.e("❌ Panen Archive Update Failed")
            }
        }

        uploadCMPViewModel.updateStatusUploadCMP.observeOnce(this) { (id, success) ->
            if (success) {
                AppLogger.d("✅ Upload Data with Tracking ID $id Inserted or Updated Successfully")
            } else {
                AppLogger.e("❌ Upload Data with Tracking ID $id Insertion Failed")
            }
        }

        //cek semua data upload apakah ada dalam satu hari ini
        uploadCMPViewModel.allIds.observeOnce(this) { data ->
            loadingDialog.dismiss()
            if (data.isNotEmpty()) {
                trackingIdsUpload = data

            }
        }

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
                    val countDeferred = async { espbViewModel.getCountDraftESPB() }
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
                    val counteSPBWBDeferred = async { weightBridgeViewModel.coundESPBUploaded() }
                    counteSPBWBScanned = counteSPBWBDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount(
                            "Rekap e-SPB Timbangan Mill",
                            counteSPBWBScanned.toString()
                        )
                        featureAdapter.hideLoadingForFeature("Rekap e-SPB Timbangan Mill")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap e-SPB Timbangan Mill")
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
                featureName = "Panen TBS",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                count = null,
                functionDescription = "Pencatatatan panen TBS di TPH oleh kerani panen",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Rekap Hasil Panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = countPanenTPH.toString(),
                functionDescription = "Rekapitulasi panen TBS dan transfer data ke suoervisi",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Scan Hasil Panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                count = null,
                functionDescription = "Transfer data dari kerani panen ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Rekap panen dan restan",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = countPanenTPHApproval.toString(),
                functionDescription = "Rekapitulsasi panen TBS dan restan dari kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Buat eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Transfer data dari driver ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi eSPB dan transfer data ke driver",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Inspeksi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Buat inspeksi panen",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap inspeksi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi inspeksi panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Scan e-SPB Timbangan Mill",
                featureNameBackgroundColor = R.color.orange,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Transfer data eSPB dari driver"
            ),

            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap e-SPB Timbangan Mill",
                featureNameBackgroundColor = R.color.orange,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.COUNT,
                subTitle = "Transfer data eSPB dari driver"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Absensi kehadiran karyawan panen oleh supervisi",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi absensi karyawan dan transfer data ke kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Scan absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Transfer data abseni dari supervisi ke kerani panen",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Sinkronisasi data",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Sinkronisasi data manual"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Upload data CMP",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Upload Semua Data CMP"
            )
        )

        val gridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

        binding.featuresRecyclerView.apply {
            layoutManager = gridLayoutManager
            featureAdapter = FeatureCardAdapter { featureCard ->
                onFeatureCardClicked(featureCard)
            }

            adapter = featureAdapter
            featureAdapter.setFeatures(features)

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


    private fun onFeatureCardClicked(feature: FeatureCard) {
        when (feature.featureName) {
            "Panen TBS" -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, FeaturePanenTBSActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Rekap Hasil Panen" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListPanenTBSActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Scan Hasil Panen" -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanQR::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Buat eSPB" -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanQR::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }


            "Rekap eSPB" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListHistoryESPBActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Rekap panen dan restan" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListPanenTBSActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Absensi panen" -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, FeatureAbsensiActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Rekap absensi panen" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListAbsensiActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Scan e-SPB Timbangan Mill" -> {
                if (feature.displayType == DisplayType.ICON) {
                    val intent = Intent(this, ScanWeighBridgeActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Rekap e-SPB Timbangan Mill" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    val intent = Intent(this, ListHistoryWeighBridgeActivity::class.java)
                    intent.putExtra("FEATURE_NAME", feature.featureName)
                    startActivity(intent)
                }
            }

            "Sinkronisasi data" -> {
                if (feature.displayType == DisplayType.ICON) {
                    if (AppUtils.isNetworkAvailable(this)) {
                        isTriggerButtonSinkronisasiData = true
                        uploadCMPViewModel.getAllIds()
                        loadingDialog.show()
                        loadingDialog.setMessage("Sedang mempersiapkan data...")

                        lifecycleScope.launch {
                            val data = withTimeoutOrNull(500) {
                                suspendCancellableCoroutine<List<Int>> { continuation ->
                                    uploadCMPViewModel.allIds.observeOnce(this@HomePageActivity) { ids ->
                                        continuation.resume(ids) {}
                                    }
                                }
                            } ?: emptyList()

                            loadingDialog.dismiss()

                            if (data.isNotEmpty()) {
                                trackingIdsUpload = data
                                startDownloads()
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

                        }


                    }

                }
            }


            "Upload data CMP" -> {
                if (feature.displayType == DisplayType.ICON) {

                    lifecycleScope.launch {
                        loadingDialog.show()
                        loadingDialog.setMessage("Sedang mempersiapkan data...")
                        delay(500)

                        allUploadZipFilesToday =
                            AppUtils.checkUploadZipReadyToday(this@HomePageActivity).toMutableList()

                        if (allUploadZipFilesToday.isNotEmpty()) {
                            uploadCMPViewModel.getUploadCMPTodayData()
                            delay(100)
                            val filteredFiles = withContext(Dispatchers.Main) {
                                suspendCoroutine<List<File>> { continuation ->
                                    uploadCMPViewModel.fileData.observeOnce(this@HomePageActivity) { fileList ->
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

                        AppLogger.d("Final allUploadZipFilesToday: $allUploadZipFilesToday")

                        val featuresToFetch = listOf(
                            AppUtils.DatabaseTables.ESPB,
                            AppUtils.DatabaseTables.PANEN
                        )
                        lifecycleScope.launch {
                            val panenDeferred =
                                CompletableDeferred<List<PanenEntityWithRelations>>()
                            val espbDeferred = CompletableDeferred<List<ESPBEntity>>()
                            val zipDeferred = CompletableDeferred<Boolean>()

                            // Load Panen Data
                            panenViewModel.loadActivePanenESPB()
                            delay(100)
                            panenViewModel.activePanenList.observeOnce(this@HomePageActivity) { list ->
                                Log.d("UploadCheck", "Panen Data Size: ${list.size}")
                                panenDeferred.complete(
                                    list ?: emptyList()
                                ) // Ensure it's never null
                            }

                            // Load ESPB Data
                            weightBridgeViewModel.fetchActiveESPB()
                            delay(100)
                            weightBridgeViewModel.activeESPBUploadCMP.observeOnce(this@HomePageActivity) { list ->
                                Log.d("UploadCheck", "📌 ESPB Data Received: ${list.size}")
                                espbDeferred.complete(list ?: emptyList()) // Ensure it's never null
                            }

                            // Initialize outside try-catch to avoid uninitialized errors
                            var mappedPanenData: List<Map<String, Any>> = emptyList()
                            var mappedESPBData: List<Map<String, Any>> = emptyList()

                            try {
                                val panenList = panenDeferred.await()
                                val espbList = espbDeferred.await()

                                if (panenList.isNotEmpty()) {
                                    mappedPanenData = panenList.map { panenWithRelations ->
                                        val jjgJson =
                                            panenWithRelations.panen.jjg_json as? Map<String, Any>
                                                ?: emptyMap()
                                        val karyawanIds = panenWithRelations.panen.karyawan_id
                                            ?.split(",")
                                            ?.map { it.trim() }
                                            ?: emptyList()
                                        val jumlahPemanen = karyawanIds.size
                                        val kemandoranData = if (karyawanIds.isNotEmpty()) {
                                            datasetViewModel.getKaryawanKemandoranList(karyawanIds)
                                        } else {
                                            emptyList()
                                        }
                                        val jsonResultKemandoran =
                                            convertToJsonKaryawanKemandoran(kemandoranData)

                                        mapOf(
                                            "id" to panenWithRelations.panen.id,
                                            "tanggal" to panenWithRelations.panen.date_created,
                                            "tipe" to panenWithRelations.panen.jenis_panen,
                                            "dept" to (panenWithRelations.tph?.dept ?: 0) as Int,
                                            "dept_abbr" to (panenWithRelations.tph?.dept_abbr
                                                ?: "") as String,
                                            "divisi" to (panenWithRelations.tph?.divisi
                                                ?: 0) as Int,
                                            "divisi_abbr" to (panenWithRelations.tph?.divisi_abbr
                                                ?: "") as String,
                                            "blok" to (panenWithRelations.tph?.blok ?: 0) as Int,
                                            "blok_name" to (panenWithRelations.tph?.blok_kode
                                                ?: "") as String,
                                            "tph_nomor" to (panenWithRelations.tph?.nomor ?: ""),
                                            "ancak" to panenWithRelations.panen.ancak,
                                            "updated_date" to panenWithRelations.panen.date_created,
                                            "updated_by" to panenWithRelations.panen.created_by,
                                            "asistensi" to if ((panenWithRelations.panen.asistensi as? Int) == 0) 1 else 2,
                                            "kemandoran" to jsonResultKemandoran,
                                            "jumlah_pemanen" to jumlahPemanen,
                                            "jjg_panen" to (jjgJson["TO"] ?: 0),
                                            "jjg_mentah" to (jjgJson["UN"] ?: 0),
                                            "jjg_lewat_masak" to (jjgJson["OV"] ?: 0),
                                            "jjg_kosong" to (jjgJson["EM"] ?: 0),
                                            "jjg_abnormal" to (jjgJson["AB"] ?: 0),
                                            "jjg_serangan_tikus" to (jjgJson["RA"] ?: 0),
                                            "jjg_panjang" to (jjgJson["LO"] ?: 0),
                                            "jjg_tidak_vcut" to (jjgJson["TI"] ?: 0),
                                            "jjg_masak" to (jjgJson["RI"] ?: 0),
                                            "jjg_kirim" to (jjgJson["KP"] ?: 0),
                                            "foto" to panenWithRelations.panen.foto,
                                            "komentar" to panenWithRelations.panen.komentar,
                                            "lat" to panenWithRelations.panen.lat,
                                            "lon" to panenWithRelations.panen.lon
                                        )
                                    }

                                    globalPanenList = mappedPanenData
                                    globalPanenIds = mappedPanenData.map { it["id"] as Int }
                                }

                                if (espbList.isNotEmpty()) {
                                    mappedESPBData = espbList.map { data ->
                                        val blokJjgList = data.blok_jjg.split(";").mapNotNull {
                                            it.split(",").takeIf { it.size == 2 }
                                                ?.let { (id, jjg) ->
                                                    id.toIntOrNull()
                                                        ?.let { it to jjg.toIntOrNull() }
                                                }
                                        }
                                        val idBlokList = blokJjgList.map { it.first }
                                        val concatenatedIds = idBlokList.joinToString(",")
                                        val totalJjg = blokJjgList.mapNotNull { it.second }.sum()
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

                                        val regional = blokData.firstOrNull()?.regional ?: ""
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

                                    globalESPBList = mappedESPBData
                                    globalESPBIds = mappedESPBData.map { it["id"] as Int }
                                }

                            } catch (e: Exception) {
                                Log.e("UploadCheck", "❌ Error: ${e.message}")
                            } finally {
                                val uploadDataList =
                                    mutableListOf<Pair<String, List<Map<String, Any>>>>()
                                if (mappedPanenData.isNotEmpty()) uploadDataList.add(AppUtils.DatabaseTables.PANEN to mappedPanenData)
                                if (mappedESPBData.isNotEmpty()) uploadDataList.add(AppUtils.DatabaseTables.ESPB to mappedESPBData)


                                Log.d("UploadCheck", uploadDataList.toString())

                                if (uploadDataList.isNotEmpty()) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        AppUtils.createAndSaveZipUploadCMP(
                                            this@HomePageActivity,
                                            uploadDataList,
                                            prefManager!!.idUserLogin.toString()
                                        ) { success, fileName, fullPath ->
                                            if (success) {
                                                zipFilePath = fullPath
                                                zipFileName = fileName

                                                Log.d("UploadCheck", zipFilePath.toString())
                                                Log.d("UploadCheck", zipFileName.toString())
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

                                loadingDialog.dismiss()
                            }

                            // Wait for ZIP to complete before calling the next function
                            val zipSuccess = zipDeferred.await()
                            if (zipSuccess || allUploadZipFilesToday.isNotEmpty()) {
                                Log.d(
                                    "UploadCheck",
                                    "🎉 ZIP creation done! Proceeding to the next step."
                                )
                                setupDialogUpload()
                            } else {
                                Log.e("UploadCheck", "⛔ ZIP creation failed! Skipping next step.")
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
            }
        }
    }


    fun createJsonTableNameMapping(): String {
        val tableMap = mapOf(
            AppUtils.DatabaseTables.PANEN to globalPanenIds,
            AppUtils.DatabaseTables.ESPB to globalESPBIds
        )
        return Gson().toJson(tableMap) // Convert to JSON string
    }

    private val archiveUpdateActions = mapOf(
        AppUtils.DatabaseTables.ESPB to { ids: List<Int> ->
            weightBridgeViewModel.updateArchiveESPB(
                ids,
                1
            )
        },
        AppUtils.DatabaseTables.PANEN to { ids: List<Int> ->
            panenViewModel.updateArchivePanen(
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
    private fun setupDialogUpload() {
        loadingDialog.dismiss()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)
        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Upload Data CMP"

        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)

        val closeDialogBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val btnUploadDataCMP = dialogView.findViewById<MaterialButton>(R.id.btnUploadDataCMP)
        val containerDownloadDataset =
            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        containerDownloadDataset.visibility = View.VISIBLE
        closeDialogBtn.visibility = View.VISIBLE
        btnUploadDataCMP.visibility = View.VISIBLE

        // Create a list of upload items
        val uploadItems = mutableListOf<UploadCMPItem>()

        allUploadZipFilesToday.forEachIndexed { index, file ->
            val fullPath = file.absolutePath
            val fileName = file.name

            if (fullPath.isNotEmpty() && fileName.isNotEmpty()) {
                uploadItems.add(
                    UploadCMPItem(
                        id = index, // Unique ID for each file
                        title = fileName,
                        fullPath = fullPath
                    )
                )
            }
        }


        if (!zipFilePath.isNullOrEmpty() && !zipFileName.isNullOrEmpty()) {
            uploadItems.add(
                UploadCMPItem(
                    id = 0,
                    title = zipFileName ?: "",
                    fullPath = zipFilePath ?: ""
                )
            )
        }


        // Set initial counter
        AppLogger.d(uploadItems.size.toString())
        counterTV.text = "0/${uploadItems.size}"

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UploadProgressCMPDataAdapter(uploadItems)
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

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

                // Start uploading all files
                uploadCMPViewModel.uploadMultipleZips(uploadItems)
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

        closeDialogBtn.setOnClickListener {
            zipFileName = null
            zipFilePath = null
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
                adapter.updateProgress(id, progress)
            }

            // Update title if any upload is in progress
            if (progressMap.values.any { it in 1..99 }) {
                titleTV.text = "Sedang Upload Data..."
            }
        }

        // Observe status for each item
        uploadCMPViewModel.itemStatusMap.observe(this) { statusMap ->
            // Update status for each item
            for ((id, status) in statusMap) {
                adapter.updateStatus(id, status)
            }

            // Check overall status
            val allFinished = statusMap.values.none {
                it == AppUtils.UploadStatusUtils.WAITING || it == AppUtils.UploadStatusUtils.UPLOADING
            }

            val allSuccess = statusMap.values.all { it == AppUtils.UploadStatusUtils.SUCCESS }

            if (allFinished && statusMap.isNotEmpty()) {
                if (allSuccess) {
                    titleTV.text = "Upload Berhasil"
                } else {
                    titleTV.text = "Upload Selesai Dengan Kesalahan"
                }

                // Enable close button, hide upload button
                btnUploadDataCMP.visibility = View.GONE
                closeDialogBtn.isEnabled = true
                closeDialogBtn.alpha = 1f
                closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
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
                titleTV.text = "Terjadi Kesalahan..."
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.colorRedDark))
            }
        }

//        // Observe original LiveData for compatibility (affects currently uploading file)
//        uploadCMPViewModel.uploadProgressCMP.observe(this) { progress ->
//            // You can use this for showing progress in some global UI element if needed
//        }
//
//        uploadCMPViewModel.uploadStatusCMP.observe(this) { status ->
//            // You can use this for showing status in some global UI element if needed
//        }
//
//        uploadCMPViewModel.uploadErrorCMP.observe(this) { error ->
//            if (!error.isNullOrEmpty()) {
//                // Handle global error display if needed
//            }
//        }

        // Process responses for database updates
        uploadCMPViewModel.itemResponseMap.observe(this) { responseMap ->
            lifecycleScope.launch {
                for ((_, response) in responseMap) {
                    AppLogger.d("response for update or insert table upload_cmp $response")
                    response?.let {
                        val jsonResultTableIds = createJsonTableNameMapping()
                        uploadCMPViewModel.UpdateOrInsertDataUpload(
                            response.trackingId,
                            response.nama_file,
                            response.status,
                            response.tanggal_upload,
                            jsonResultTableIds
                        )
                        delay(100) // Add delay before calling the function
                    }
                }
            }
        }

    }


//    private fun startUploadAllDataCMP(fileZip: String?) {
//        uploadCMPViewModel.uploadZipToServer(fileZip ?: "")
//    }


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


                if (prefManager!!.isFirstTimeLaunch && downloadItems.any { it.isStoringCompleted || it.isUpToDate || it.error != null }) {
                    prefManager!!.isFirstTimeLaunch = false
                    AppLogger.d("First-time launch flag updated to false")
                }

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

    private fun startDownloads() {
        val regionalIdString = prefManager!!.regionalIdUserLogin
        val estateIdString = prefManager!!.estateIdUserLogin
        val lastModifiedDatasetTPH = prefManager!!.lastModifiedDatasetTPH
        val lastModifiedDatasetBlok = prefManager!!.lastModifiedDatasetBlok
        val lastModifiedDatasetKemandoran = prefManager!!.lastModifiedDatasetKemandoran
        val lastModifiedDatasetPemanen = prefManager!!.lastModifiedDatasetPemanen
        val lastModifiedDatasetTransporter = prefManager!!.lastModifiedDatasetTransporter

        if (estateIdString.isNullOrEmpty() || estateIdString.isBlank()) {
            AppLogger.d("Downloads: Estate ID is null or empty, aborting download")
            showErrorDialog("Estate ID is not valid. Current value: '$estateIdString'")
            return
        }

        try {
            val estateId = estateIdString.toInt()
            if (estateId <= 0) {
                AppLogger.d("Downloads: Estate ID is not a valid positive number: $estateId")
                showErrorDialog("Estate ID must be a positive number")
                return
            }

            val filteredRequests = if (isTriggerButtonSinkronisasiData) {
                getDatasetsToDownload(
                    regionalIdString!!.toInt(),
                    estateId,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetKemandoran,
                    lastModifiedDatasetTransporter,
                )
            } else {
                getDatasetsToDownload(
                    regionalIdString!!.toInt(),
                    estateId,
                    lastModifiedDatasetTPH,
                    lastModifiedDatasetBlok,
                    lastModifiedDatasetPemanen,
                    lastModifiedDatasetKemandoran,
                    lastModifiedDatasetTransporter
                ).filterNot { prefManager!!.datasetMustUpdate.contains(it.dataset) }
            }

            if (filteredRequests.isNotEmpty()) {
                dialog.show()
                datasetViewModel.downloadMultipleDatasets(filteredRequests)
            } else {
                AppLogger.d("All datasets are up-to-date, no download needed.")
            }


        } catch (e: NumberFormatException) {
            AppLogger.d("Downloads: Failed to parse Estate ID to integer: ${e.message}")
            showErrorDialog("Invalid Estate ID format: ${e.message}")
        }
    }

    private fun getDatasetsToDownload(
        regionalId: Int,
        estateId: Int,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetKemandoran: String?,
        lastModifiedDatasetTransporter: String?,
    ): List<DatasetRequest> {
        return listOf(
            //khusus mill
            DatasetRequest(regional = regionalId, lastModified = null, dataset = "mill"),
            //khusus dataset
            DatasetRequest(
                estate = estateId,
                lastModified = lastModifiedDatasetTPH,
                dataset = "tph"
            ),
            DatasetRequest(
                estate = estateId,
                lastModified = lastModifiedDatasetPemanen,
                dataset = "pemanen"
            ),
            DatasetRequest(
                estate = estateId,
                lastModified = lastModifiedDatasetKemandoran,
                dataset = "kemandoran"
            ),
            DatasetRequest(
                lastModified = lastModifiedDatasetTransporter,
                dataset = "transporter"
            )
        )
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
//            dialog.dismiss()  // Dismiss the download progress dialog
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


    private fun setupName() {
        val userName = prefManager!!.nameUserLogin ?: "Unknown"
        val jobTitle = "${prefManager!!.jabatanUserLogin} - ${prefManager!!.estateUserLogin}"
        val initials = userName.split(" ").take(2).joinToString("") { it.take(1).uppercase() }

        AppLogger.d(userName)
        findViewById<TextView>(R.id.userNameLogin).text = userName
        findViewById<TextView>(R.id.jabatanUserLogin).text = jobTitle
        findViewById<TextView>(R.id.initalName).text = initials
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