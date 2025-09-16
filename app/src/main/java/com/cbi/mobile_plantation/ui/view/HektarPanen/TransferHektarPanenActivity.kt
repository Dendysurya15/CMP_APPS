package com.cbi.mobile_plantation.ui.view.HektarPanen

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.ui.adapter.TransferHektarPanenAdapter
import com.cbi.mobile_plantation.ui.adapter.TransferHektarPanenData
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.BluetoothScanner
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScreenshotUtil
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


@Suppress("UNREACHABLE_CODE")
class TransferHektarPanenActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: PanenViewModel
    private lateinit var adapter: TransferHektarPanenAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var kemandoranUserLogin: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private lateinit var dateButton: Button
    private var mappedData: List<Map<String, Any>> = emptyList()
    private var globalFormattedDate: String = AppUtils.currentDate
    private val dateIndexMap = mutableMapOf<String, Int>()
    private val nikIndexMap = mutableMapOf<String, Int>()
    private lateinit var loadingDialog: LoadingDialog
    private var originalMappedData: MutableList<Map<String, Any>> = mutableListOf()
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order
    private var shouldStartBluetoothScan = false
    private lateinit var bluetoothScanner: BluetoothScanner
    private var jjg = 0
    private var blok = "NULL"
    private var tph = 0
    private var bluetoothJsonData: String = ""
    private var bluetoothDataInfo: String = ""
    private lateinit var tvEmptyState: TextView // Add this
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private var activityInitialized = false
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    private var limit = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        //cek tanggal otomatis
        checkDateTimeSettings()
        if (featureName == AppUtils.ListFeatureNames.TransferHektarPanen) {
            setupButtonGenerateQR()
        }
        loadingDialog = LoadingDialog(this)

    }

    private fun setupUI() {
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()
        bluetoothScanner = BluetoothScanner(this)
        findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        setupFilterAllData()

        viewModel.getAllScanMPanenByDate(0, AppUtils.currentDate) //        }

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
            // Update global date variable
            globalFormattedDate = formattedDate
            // Keep this if AppUtils.setSelectedDate is used elsewhere in your code
            AppUtils.setSelectedDate(formattedDate)

            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun processSelectedDate(selectedDate: String) {
        val filterAllData = findViewById<CheckBox>(R.id.calendarCheckbox)
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        // If "Filter All Data" is checked, uncheck it when user selects a specific date
        if (filterAllData.isChecked) {
            filterAllData.isChecked = false
        }

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate

        viewModel.getAllScanMPanenByDate(0, selectedDate)

        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE

            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )
            AppUtils.setSelectedDate(todayBackendDate)

            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate
            viewModel.getAllScanMPanenByDate(0, todayBackendDate)
        }
        filterDateContainer.visibility = View.VISIBLE
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

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    @SuppressLint("DefaultLocale")
    private fun setupObserveData() {
        viewModel.activePanenList.observe(this) { panenList ->
            AppLogger.d(panenList.toString())
            Handler(Looper.getMainLooper()).postDelayed({
                lifecycleScope.launch {
                    if (panenList.isNotEmpty()) {
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        val allWorkerData = mutableListOf<Map<String, Any>>()


                        originalMappedData.clear()
                        panenList.map { panenWithRelations ->
                            val standardData = mapOf<String, Any>(
                                "id" to (panenWithRelations.panen.id as Any),
                                "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                "date_created" to (panenWithRelations.panen.date_created as Any),
                                "blok_name" to (panenWithRelations.tph?.blok_kode ?: "Unknown"),
                                "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                "created_by" to (panenWithRelations.panen.created_by as Any),
                                "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                "foto" to (panenWithRelations.panen.foto as Any),
                                "komentar" to (panenWithRelations.panen.komentar as Any),
                                "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                "lat" to (panenWithRelations.panen.lat as Any),
                                "lon" to (panenWithRelations.panen.lon as Any),
                                "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                "ancak" to (panenWithRelations.panen.ancak as Any),
                                "archive" to (panenWithRelations.panen.archive as Any),
                                "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                "karyawan_nik" to (panenWithRelations.panen.karyawan_nik as Any),
                                "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                "nama_karyawans" to "",
                                "nama_kemandorans" to "",
                                "username" to (panenWithRelations.panen.username as Any)
                            )

                            val originalDataMapped = standardData.toMutableMap()
                            originalMappedData.add(originalDataMapped)

                            val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            val pemuatData: List<KaryawanModel>? =
                                withContext(Dispatchers.IO) {
                                    try {
                                        viewModel.getPemuatByIdList(pemuatList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                        null
                                    }
                                }

                            val rawKemandoran: List<String> = pemuatData
                                ?.mapNotNull { it.kemandoran_id?.toString() }
                                ?.distinct() ?: emptyList()

                            val kemandoranData: List<KemandoranModel>? =
                                withContext(Dispatchers.IO) {
                                    try {
                                        viewModel.getKemandoranById(rawKemandoran)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                        null
                                    }
                                }

                            val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                ?.takeIf { it.isNotEmpty() }
                                ?.joinToString("\n") { "• $it" } ?: "-"

                            val karyawanNamas = pemuatData?.mapNotNull { karyawan ->
                                karyawan.nama?.let { nama ->
                                    // Always append NIK for every worker
                                    "$nama - ${karyawan.nik ?: "N/A"}"
                                }
                            }?.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") ?: "-"

                            // Update the original data with the fetched names
                            originalDataMapped["nama_karyawans"] = karyawanNamas
                            originalDataMapped["nama_kemandorans"] = kemandoranNamas

                            val updatedStandardData = standardData.toMutableMap().apply {
                                this["nama_karyawans"] = karyawanNamas
                                this["nama_kemandorans"] = kemandoranNamas
                            }

                            allWorkerData.add(updatedStandardData)

                            listOf(updatedStandardData)

                        }.flatten()

                        mappedData = allWorkerData

                        val processedData =
                            AppUtils.getPanenProcessedData(originalMappedData, featureName)

                        if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen ||
                            featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan ||
                            featureName == AppUtils.ListFeatureNames.DetailESPB ||
                            featureName == AppUtils.ListFeatureNames.TransferHektarPanen
                        ) {

                            findViewById<LinearLayout>(R.id.blok_section).visibility = View.VISIBLE
                            findViewById<LinearLayout>(R.id.total_section).visibility = View.VISIBLE
                        }

                        // Extract values from processed data
                        val blokNames = processedData["blokNames"]?.toString() ?: ""
                        blok = if (blokNames.isEmpty()) "-" else blokNames

                        val blokDisplay = processedData["blokDisplay"]?.toString() ?: "-"
                        jjg = processedData["totalJjgCount"]?.toString()?.toIntOrNull() ?: 0
                        tph = processedData["tphCount"]?.toString()?.toIntOrNull() ?: 0

                        findViewById<TextView>(R.id.titleTotalJjg).text = "Jjg Bayar: "
                        findViewById<TextView>(R.id.listBlok).text = blokDisplay
                        findViewById<TextView>(R.id.totalJjg).text = jjg.toString()
                        findViewById<TextView>(R.id.totalTPH).text = tph.toString()

                        // Log the results for debugging with feature context
                        val jsonFieldUsed = "PA"

                        AppLogger.d("Feature: $featureName")
                        AppLogger.d("JSON field used: $jsonFieldUsed")
                        AppLogger.d("Blok Display: $blokDisplay")
                        AppLogger.d("Total JJG: $jjg")
                        AppLogger.d("Total TPH: $tph")

                        // First, convert the Map<String, Any> data to TransferHektarPanenData objects
                        val transferHektarPanenDataList = allWorkerData.map { item ->
                            val jjgStr =
                                JSONObject(item["jjg_json"] as? String).optDouble("PA", 0.0).toInt()
                                    .toString()
                            TransferHektarPanenData(
                                time = (item["date_created"] as? String) ?: "",
                                blok = (item["blok_name"] as? String) ?: "-",
                                janjang = jjgStr,
                                noTph = "${item["nomor"] ?: ""}",
                                namaPemanen = (item["nama_karyawans"] as? String) ?: "-",
                                status_scan = 1, // Or any appropriate default
                                id = (item["id"] as? String)?.toIntOrNull() ?: (item["id"] as? Int)
                                ?: 0
                            )
                        }

                        // Then update the adapter with the correctly typed list
                        adapter.updateList(transferHektarPanenDataList)
                        originalData =
                            emptyList()

                        loadingDialog.dismiss()
                    } else {
                        AppLogger.d("panenWithRelations panenList is empty")
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE

                        // Hide the summary sections when no data
                        findViewById<LinearLayout>(R.id.blok_section).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.total_section).visibility = View.GONE

                        loadingDialog.dismiss()
                    }
                }
            }, 500)
        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("WAKTU", "BLOK", "JANJANG", "PEMANEN", "NO TPH")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransferHektarPanenAdapter(emptyList(), this@TransferHektarPanenActivity)
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }

//        val th5 = tableHeader.findViewById<TextView>(R.id.th5)
//        val th6 = tableHeader.findViewById<TextView>(R.id.th6)
//        val layoutParamsTh5 = th5.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTh5.weight = 0.3f
//        th5.layoutParams = layoutParamsTh5
//        val layoutParamsTh6 = th6.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTh6.weight = 0.3f
//        th6.layoutParams = layoutParamsTh6
        val flCheckBoxTableHeaderLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
    }

    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
    }

    // Add this after your dateButton setup in setupUI() method
    private fun setupFilterAllData() {
        val filterAllData = findViewById<CheckBox>(R.id.calendarCheckbox)
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)

        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // User wants to see all data
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"

                // Disable date picker button when viewing all data
                dateButton.isEnabled = false
                dateButton.alpha = 0.5f

                // Load all data without date filter
                viewModel.getAllScanMPanenByDate(0, null)  // Pass null to load all data
            } else {
                // User wants to filter by date
                val displayDate = AppUtils.formatSelectedDateForDisplay(globalFormattedDate)

                // Update UI
                dateButton.text = displayDate
                nameFilterDate.text = displayDate

                // Enable date picker button
                dateButton.isEnabled = true
                dateButton.alpha = 1f

                // Load data for the selected date
                viewModel.getAllScanMPanenByDate(0, globalFormattedDate)
            }

            // Setup remove filter button
            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)
            removeFilterDate.setOnClickListener {
                if (filterAllData.isChecked) {
                    filterAllData.isChecked = false
                }

                filterDateContainer.visibility = View.GONE

                // Reset to today's date
                val todayBackendDate = AppUtils.formatDateForBackend(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR)
                )

                globalFormattedDate = todayBackendDate

                // Update UI
                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate

                // Load today's data
                viewModel.getAllScanMPanenByDate(0, todayBackendDate)
            }
        }
    }

    private fun setupHeader() {
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        kemandoranUserLogin = prefManager!!.kemandoranUserLogin
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
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

    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        val btnTransferBT = findViewById<FloatingActionButton>(R.id.btnTransferBT)
        val btnGenerateQRTPHUnl = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPHUnl)
        val tvGenQR60 = findViewById<TextView>(R.id.tvGenQR60)
        val tvTransferBT = findViewById<TextView>(R.id.tvTransferBT)
        val tvGenQRFull = findViewById<TextView>(R.id.tvGenQRFull)

        btnGenerateQRTPH.visibility = View.VISIBLE
        btnTransferBT.visibility = View.VISIBLE
        btnGenerateQRTPHUnl.visibility = View.GONE
        tvGenQR60.visibility = View.VISIBLE
        tvTransferBT.visibility = View.VISIBLE
        tvGenQRFull.visibility = View.VISIBLE

        btnGenerateQRTPH.setOnClickListener {
            limit = 0
            generateQRTPH(0)
        }

        btnGenerateQRTPHUnl.setOnClickListener {
            limit = 0
            generateQRTPH(0)
        }

        // UPDATED - Check Bluetooth first, then show dialog
        btnTransferBT.setOnClickListener {
            checkBluetoothAndShowDialog()
        }
    }

    // ADD THIS METHOD
    @SuppressLint("MissingPermission")
    private fun checkBluetoothAndShowDialog() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        when {
            bluetoothAdapter == null -> {
                // Device doesn't support Bluetooth
                Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT).show()
            }
            !bluetoothAdapter.isEnabled -> {
                // Bluetooth is not enabled, ask user to enable it
                AlertDialogUtility.withTwoActions(
                    this,
                    "Aktifkan",
                    "Bluetooth Nonaktif",
                    "Aktifkan Bluetooth untuk memindai perangkat",
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    },
                    cancelFunction = {
                        // User cancelled enabling Bluetooth
                    }
                )
            }
            else -> {
                // Bluetooth is enabled, proceed with scanning
                generateJsonAndShowBluetoothDialog()
            }
        }
    }

    // ADD THIS CONSTANT AT THE TOP OF YOUR CLASS
    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    // ADD THIS METHOD TO HANDLE BLUETOOTH ENABLE RESULT
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was enabled, now generate JSON and show the dialog
                Toast.makeText(this, "Bluetooth diaktifkan. Menyiapkan data...", Toast.LENGTH_SHORT).show()
                generateJsonAndShowBluetoothDialog()
            } else {
                Toast.makeText(this, "Bluetooth diperlukan untuk transfer data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateJsonAndShowBluetoothDialog() {
        // Show loading dialog while generating JSON
        loadingDialog.show()
        loadingDialog.setMessage("Menyiapkan data untuk transfer...", true)

        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            try {
                // DEBUG: Check mappedData first
                AppLogger.d("DEBUG: mappedData size = ${mappedData?.size ?: "NULL"}")
                AppLogger.d("DEBUG: mappedData content = $mappedData")

                if (mappedData.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@TransferHektarPanenActivity,
                            "Tidak ada data untuk ditransfer. Pastikan data sudah dimuat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Generate JSON data for all items
                val effectiveLimit = mappedData.size

                val jsonData = try {
                    // Take all items for Bluetooth transfer
                    val limitedData = mappedData.take(effectiveLimit)
                    AppLogger.d("DEBUG: limitedData size = ${limitedData.size}")
                    formatPanenDataForQR(limitedData)
                } catch (e: Exception) {
                    AppLogger.e("Error generating JSON data for Bluetooth: ${e.message}")
                    throw e
                }

                AppLogger.d("Original JSON data: $jsonData")

                // ENCODE THE JSON DATA BEFORE SENDING
                val encodedData = try {
                    encodeJsonToBase64ZipQR(jsonData) ?: throw Exception("Encoding failed - data too large or invalid")
                } catch (e: Exception) {
                    AppLogger.e("Error encoding data for Bluetooth: ${e.message}")
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@TransferHektarPanenActivity,
                            "Terjadi kesalah ketika hash data ke Base64: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                AppLogger.d("Encoded data size: ${encodedData.length} characters")

                // Get processed data info for display
                val limitedData = mappedData.take(effectiveLimit)
                val processedData = AppUtils.getPanenProcessedData(limitedData, featureName)

                // DEBUG: Check processed data
                AppLogger.d("DEBUG: processedData = $processedData")
                AppLogger.d("DEBUG: featureName = $featureName")

                // Store the ENCODED data for transfer instead of raw JSON
                bluetoothJsonData = encodedData

                val capitalizedFeatureName = featureName?.split(" ")?.joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } ?: "Unknown Feature"

                bluetoothDataInfo = """
        Data $capitalizedFeatureName:
        • Blok: ${processedData["blokDisplay"] ?: "N/A"}
        • Total JJG: ${processedData["totalJjgCount"] ?: "0"}
        • Total TPH: ${processedData["tphCount"] ?: "0"}
        • Size: ${String.format("%.2f", encodedData.length / 1024.0)} KB (encoded)
    """.trimIndent()

                AppLogger.d("DEBUG: bluetoothDataInfo = $bluetoothDataInfo")

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    // Now show Bluetooth devices dialog
                    showBluetoothDevicesDialog()
                }

            } catch (e: Exception) {
                AppLogger.e("Error in Bluetooth JSON generation: ${e.message}")
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@TransferHektarPanenActivity,
                        "Gagal menyiapkan data untuk transfer: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showBluetoothDevicesDialog() {
        val devices = mutableListOf<BluetoothDevice>()
        val deviceNames = mutableListOf<String>()
        val deviceTypes = mutableMapOf<String, String>() // Store device type info
        var isScanning = false

        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.layout_bluetooth_scanner, null)
        bottomSheetDialog.setContentView(dialogView)

        // Use the SAME working pattern as your receiver
        val maxHeight = (resources.displayMetrics.heightPixels * 0.7).toInt()

        bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    this.peekHeight = maxHeight
                    this.state = BottomSheetBehavior.STATE_EXPANDED
                    this.isFitToContents = true
                    this.isDraggable = false
                    this.isHideable = false
                }

                bottomSheet.layoutParams?.height = maxHeight
            }

        val listView = dialogView.findViewById<ListView>(R.id.lvBluetoothDevices)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)
        val btnScanStop = dialogView.findViewById<Button>(R.id.btnScanStop)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        val tvDataInfo = dialogView.findViewById<TextView>(R.id.tvDataInfo)

        // UPDATE THE tvDataInfo WITH THE CALCULATED DATA
        tvDataInfo.text = bluetoothDataInfo
        AppLogger.d("UI UPDATE: Setting tvDataInfo.text = $bluetoothDataInfo")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = adapter

        // Prevent outside touch dismissal
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        // Handle dialog dismissal cleanup
        bottomSheetDialog.setOnDismissListener {
            bluetoothScanner.stopScan()
            bluetoothScanner.onDeviceFound = null
            bluetoothScanner.onDiscoveryFinished = null
        }

        btnScanStop.setOnClickListener {
            if (!isScanning) {
                devices.clear()
                deviceNames.clear()
                deviceTypes.clear()
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.VISIBLE
                tvStatus.text = "Mencari perangkat yang sudah dipasangkan..."
                btnScanStop.text = "Stop"
                btnScanStop.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRedDark))
                isScanning = true

                // Only scan for paired phone devices
                scanPairedPhoneDevices(devices, deviceNames, deviceTypes, adapter)

                Handler(Looper.getMainLooper()).postDelayed({
                    progressBar.visibility = View.GONE
                    // ✅ CHECK IF NO DEVICES FOUND AND SHOW HELPFUL MESSAGE
                    if (devices.size == 0) {
                        tvStatus.text = "Selesai - 0 perangkat ditemukan\n(Pastikan perangkat sudah tersambung melalui Bluetooth)"
                    } else {
                        tvStatus.text = "Selesai - ${devices.size} perangkat ditemukan"
                    }
                    btnScanStop.text = "Scan"
                    btnScanStop.setBackgroundColor(ContextCompat.getColor(this, R.color.bluedarklight))
                    isScanning = false
                }, 1000)
            } else {
                bluetoothScanner.stopScan()
                progressBar.visibility = View.GONE
                tvStatus.text = "Scan stopped - Found ${devices.size} device(s)"
                btnScanStop.text = "Scan"
                btnScanStop.setBackgroundColor(ContextCompat.getColor(this, R.color.bluedarklight))
                isScanning = false
            }
        }

        btnClose.setOnClickListener {
            bluetoothScanner.stopScan()
            bottomSheetDialog.dismiss()
        }

        bluetoothScanner.onDeviceFound = { device ->
            runOnUiThread {
                val deviceName = getDeviceName(device)
                val deviceAddress = device.address

                AppLogger.d("Found device: Name='$deviceName', Address='$deviceAddress'")

                // Check if device already exists (avoid duplicates)
                val existingIndex = devices.indexOfFirst { it.address == deviceAddress }
                if (existingIndex == -1) {
                    val deviceInfo = formatDeviceInfo(deviceName, deviceAddress )
                    devices.add(device)
                    deviceNames.add(deviceInfo)
                    adapter.notifyDataSetChanged()
                    tvStatus.text = "Ditemukan ${devices.size} perangkat"
                } else {
                    // Update existing device info if we got a better name
                    val currentName = deviceNames[existingIndex]
                    if (currentName.contains("Unknown") && !deviceName.contains("Unknown")) {
                        val updatedInfo = formatDeviceInfo(deviceName, deviceAddress )
                        deviceNames[existingIndex] = updatedInfo
                        adapter.notifyDataSetChanged()
                        AppLogger.d("Updated device name: $updatedInfo")
                    }
                }
            }
        }

        bluetoothScanner.onDiscoveryFinished = { allDevices ->
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (allDevices.isEmpty() && devices.isEmpty()) {
                    tvStatus.text = "Tidak ada perangkat ditemukan"
                } else {
                    tvStatus.text = "Pemindaian selesai - ${devices.size} perangkat ditemukan"
                }
                btnScanStop.text = "Scan"
                btnScanStop.setBackgroundColor(ContextCompat.getColor(this, R.color.bluedarklight))
                isScanning = false
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = devices[position]
            val deviceInfo = deviceNames[position]

            // Show confirmation dialog before transfer
            AlertDialogUtility.withTwoActions(
                this@TransferHektarPanenActivity,
                "Kirim Data",
                "Konfirmasi Transfer Hektaran Panen",
                "Kirim data ke:\n$deviceInfo\n\n$bluetoothDataInfo",
                "warning.json",
                ContextCompat.getColor(this@TransferHektarPanenActivity, R.color.bluedarklight),
                function = {
                    // User confirmed - start the transfer
                    bluetoothScanner.stopScan()
                    bottomSheetDialog.dismiss()
                    startBluetoothTransfer(selectedDevice)
                },
                cancelFunction = {
                    // User cancelled - just close dialog and continue scanning
                }
            )
        }

        bottomSheetDialog.show()
        btnScanStop.performClick()
    }

    // Helper function to get better device names
    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice): String {
        return try {
            // Try multiple methods to get device name
            var name = device.name

            if (name.isNullOrBlank()) {
                // Try to get name from bonded devices
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                val bondedDevice = bluetoothAdapter?.bondedDevices?.find { it.address == device.address }
                name = bondedDevice?.name
            }

            if (name.isNullOrBlank()) {
                // Generate a more descriptive unknown name based on device type and full address
                when (device.type) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic Device (${device.address})"
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE Device (${device.address})"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode (${device.address})"
                    else -> "Unknown Device (${device.address})"
                }
            } else {
                name
            }
        } catch (e: Exception) {
            AppLogger.e("Error getting device name: ${e.message}")
            "Device (${device.address})"
        }
    }

    // Helper function to get device type information with permission check
    private fun getDeviceTypeInfo(device: BluetoothDevice): String {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED) {
            when (device.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                else -> "Unknown Type"
            }
        } else {
            "Permission Required"
        }
    }

    // Helper function to format device info for display
    private fun formatDeviceInfo(name: String, address: String): String {
        return "$name"
    }

    // Helper function to include ONLY paired PHONE devices
    @SuppressLint("MissingPermission")
    private fun scanPairedPhoneDevices(
        devices: MutableList<BluetoothDevice>,
        deviceNames: MutableList<String>,
        deviceTypes: MutableMap<String, String>,
        adapter: ArrayAdapter<String>
    ) {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val pairedDevices = bluetoothAdapter?.bondedDevices

            // Filter for phone devices only
            pairedDevices?.forEach { device ->
                if (isPhoneDevice(device)) {
                    val deviceName = device.name ?: "Phone Device (${device.address.takeLast(8).replace(":", "")})"
                    val deviceTypeInfo = "${getDeviceTypeInfo(device)} • Tersambung"
                    val deviceInfo = formatDeviceInfo(deviceName, device.address)

                    devices.add(device)
                    deviceNames.add(deviceInfo)
                    deviceTypes[device.address] = deviceTypeInfo

                    AppLogger.d("Added paired PHONE: Name='$deviceName', Address='${device.address}', Type=${device.type}")
                } else {
                    AppLogger.d("Skipped non-phone device: Name='${device.name}', Address='${device.address}'")
                }
            }

            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            AppLogger.e("Error scanning paired phone devices: ${e.message}")
        }
    }

    // Helper function to identify if device is a phone using Bluetooth Device Class
    @SuppressLint("MissingPermission")
    private fun isPhoneDevice(device: BluetoothDevice): Boolean {
        val deviceClass = device.bluetoothClass

        if (deviceClass == null) {
            AppLogger.d("Device '${device.name}': No BluetoothClass available, checking by type")
            // If no bluetooth class, check if it's a dual mode device (likely phone)
            return device.type == BluetoothDevice.DEVICE_TYPE_DUAL || device.type == BluetoothDevice.DEVICE_TYPE_LE
        }

        val majorDeviceClass = deviceClass.majorDeviceClass
        val minorDeviceClass = deviceClass.deviceClass and 0xFF // Get minor class from device class

        // Phone identification using Bluetooth Device Class
        val isPhone = when {
            // Major Device Class: Phone (0x200 = 512)
            majorDeviceClass == BluetoothClass.Device.Major.PHONE -> true

            // Has telephony service
            deviceClass.hasService(BluetoothClass.Service.TELEPHONY) -> true

            // Computer class with phone-like minor classes
            majorDeviceClass == BluetoothClass.Device.Major.COMPUTER &&
                    (minorDeviceClass == BluetoothClass.Device.Major.PHONE ||
                            deviceClass.hasService(BluetoothClass.Service.TELEPHONY)) -> true

            // Uncategorized but has telephony or networking services (modern smartphones)
            majorDeviceClass == BluetoothClass.Device.Major.UNCATEGORIZED &&
                    (deviceClass.hasService(BluetoothClass.Service.TELEPHONY) ||
                            deviceClass.hasService(BluetoothClass.Service.NETWORKING)) -> true

            else -> false
        }

        // Log detailed device class information
        AppLogger.d("Device '${device.name}': " +
                "MajorClass=$majorDeviceClass (${getDeviceClassString(majorDeviceClass)}), " +
                "MinorClass=$minorDeviceClass, " +
                "HasTelephony=${deviceClass.hasService(BluetoothClass.Service.TELEPHONY)}, " +
                "HasNetworking=${deviceClass.hasService(BluetoothClass.Service.NETWORKING)}, " +
                "IsPhone=$isPhone")

        return isPhone
    }

    // Helper function to get human readable device class string
    private fun getDeviceClassString(majorDeviceClass: Int): String {
        return when (majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.HEALTH -> "Health"
            BluetoothClass.Device.Major.IMAGING -> "Imaging"
            BluetoothClass.Device.Major.MISC -> "Miscellaneous"
            BluetoothClass.Device.Major.NETWORKING -> "Networking"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.TOY -> "Toy"
            BluetoothClass.Device.Major.UNCATEGORIZED -> "Uncategorized"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable"
            else -> "Unknown($majorDeviceClass)"
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothTransfer(targetDevice: BluetoothDevice) {
        loadingDialog.show()
        loadingDialog.setMessage("Mengirim data ke ${targetDevice.name ?: "Perangkat Tidak Dikenal"}...", true)

        Thread {
            var bluetoothSocket: BluetoothSocket? = null
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                // Cancel any ongoing discovery first
                bluetoothAdapter?.cancelDiscovery()

                runOnUiThread {
                    loadingDialog.setMessage("Membuat koneksi ke ${targetDevice.name ?: "Perangkat"}...", true)
                }

                // Try multiple connection methods
                bluetoothSocket = try {
                    targetDevice.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    // Fallback method for some devices
                    AppLogger.d("Primary connection failed, trying fallback method")
                    val method = targetDevice.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(targetDevice, 1) as BluetoothSocket
                }

                runOnUiThread {
                    loadingDialog.setMessage("Menghubungkan...", true)
                }

                // Set connection timeout and connect
                bluetoothSocket!!.connect()

                // Wait for connection to stabilize
                Thread.sleep(1000)

                runOnUiThread {
                    loadingDialog.setMessage("Terhubung! Mengirim data...", true)
                }

                // Send data with proper formatting
                val outputStream = bluetoothSocket.outputStream

                // Add a simple header and footer to mark start/end of transmission
                val dataToSend = "START_DATA\n$bluetoothJsonData\nEND_DATA"
                val dataBytes = dataToSend.toByteArray(Charsets.UTF_8)

                AppLogger.d("Sending ${dataBytes.size} bytes of data")

                // Send data in smaller chunks with delays
                val chunkSize = 512 // Smaller chunks for better reliability
                var bytesSent = 0

                while (bytesSent < dataBytes.size) {
                    val remainingBytes = dataBytes.size - bytesSent
                    val currentChunkSize = if (remainingBytes < chunkSize) remainingBytes else chunkSize

                    // Send chunk
                    outputStream.write(dataBytes, bytesSent, currentChunkSize)
                    outputStream.flush()

                    bytesSent += currentChunkSize

                    val progress = (bytesSent * 100) / dataBytes.size
                    runOnUiThread {
                        loadingDialog.setMessage("Mengirim data... $progress%", true)
                    }

                    // Longer delay between chunks for stability
                    Thread.sleep(100)
                }

                // Send final flush and wait for processing
                outputStream.flush()
                Thread.sleep(2000) // Give receiver time to process

                runOnUiThread {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@TransferHektarPanenActivity,
                        "Data berhasil dikirim ke ${targetDevice.name ?: "Perangkat Tidak Dikenal"}!\nSize: ${dataBytes.size} bytes",
                        Toast.LENGTH_LONG
                    ).show()
                }

                AppLogger.d("Bluetooth transfer completed successfully")

            } catch (e: Exception) {
                AppLogger.e("Bluetooth transfer error: ${e.message}")

                runOnUiThread {
                    loadingDialog.dismiss()

                    val errorMessage = when {
                        e.message?.contains("read failed") == true ->
                            "Koneksi terputus saat transfer. Pastikan kedua perangkat dalam jarak dekat dan Mandor panen Scan Data dengan Transfer Bluetooth"
                        e.message?.contains("Service discovery failed") == true ->
                            "Perangkat tidak mendukung layanan transfer data"
                        e.message?.contains("Connection refused") == true ->
                            "Koneksi ditolak. Pastikan perangkat penerima siap menerima data"
                        e.message?.contains("Device or resource busy") == true ->
                            "Perangkat sedang sibuk. Tutup aplikasi Bluetooth lain dan coba lagi"
                        e.message?.contains("timeout") == true ->
                            "Koneksi timeout. Pastikan kedua perangkat dalam jarak dekat"
                        else -> "Gagal mengirim data: ${e.message}"
                    }

                    AlertDialogUtility.withTwoActions(
                        this@TransferHektarPanenActivity,
                        "Coba Lagi",
                        "Transfer Gagal",
                        errorMessage,
                        "warning.json",
                        ContextCompat.getColor(this@TransferHektarPanenActivity, R.color.colorRedDark),
                        function = {
                            startBluetoothTransfer(targetDevice)
                        },
                        cancelFunction = { }
                    )
                }
            } finally {
                try {
                    bluetoothSocket?.close()
                    AppLogger.d("Bluetooth socket closed")
                } catch (e: Exception) {
                    AppLogger.e("Error closing Bluetooth socket: ${e.message}")
                }
            }
        }.start()
    }

    private fun generateQRTPH(limitFun: Int) {
        limit = limitFun
        AlertDialogUtility.withTwoActions(
            this,
            "Generate QR",
            getString(R.string.confirmation_dialog_title),
            getString(R.string.al_confirm_generate_qr),
            "warning.json",
            ContextCompat.getColor(this, R.color.bluedarklight),
            function = {
                val view =
                    layoutInflater.inflate(
                        R.layout.layout_bottom_sheet_generate_qr_panen,
                        null
                    )
                val dialog = BottomSheetDialog(this)
                dialog.setContentView(view)
                // Get references to views
                val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
                val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                val tvTitleQRGenerate: TextView =
                    view.findViewById(R.id.textTitleQRGenerate)
                tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
                val capitalizedFeatureName = featureName!!.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                tvTitleQRGenerate.text = "Hasil QR $capitalizedFeatureName"
                val dashedLine: View = view.findViewById(R.id.dashedLine)
                val loadingContainer: LinearLayout =
                    view.findViewById(R.id.loadingDotsContainerBottomSheet)
                val titleQRConfirm: TextView = view.findViewById(R.id.titleAfterScanQR)
                val descQRConfirm: TextView = view.findViewById(R.id.descAfterScanQR)
                val confimationContainer: LinearLayout =
                    view.findViewById(R.id.confirmationContainer)
                val scrollContent: NestedScrollView = view.findViewById(R.id.scrollContent)
                scrollContent.post {
                    // Scroll to the middle to show QR code in center
                    // 300dp space + approximately half of QR view height (125dp)
                    scrollContent.smoothScrollTo(0, 600)
                }

                val btnConfirmScanPanenTPH: MaterialButton =
                    view.findViewById(R.id.btnConfirmScanPanenTPH)
                val btnPreviewFullQR: MaterialButton =
                    view.findViewById(R.id.btnPreviewFullQR)

                btnPreviewFullQR.setOnClickListener {
                    showQrCodeFullScreen(qrCodeImageView.drawable, view)
                }

                // Initially hide QR code and dashed line, show loading
                qrCodeImageView.visibility = View.GONE
                loadingLogo.visibility = View.VISIBLE
                loadingContainer.visibility = View.VISIBLE

                // Initial setup for text elements
                titleQRConfirm.setResponsiveTextSizeWithConstraints(21F, 17F, 25F)
                descQRConfirm.setResponsiveTextSizeWithConstraints(19F, 15F, 23F)

                // Load and start bounce animation
                val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
                loadingLogo.startAnimation(bounceAnimation)

                // Setup dots animation
                val dots = listOf(
                    loadingContainer.findViewById<View>(R.id.dot1),
                    loadingContainer.findViewById<View>(R.id.dot2),
                    loadingContainer.findViewById<View>(R.id.dot3),
                    loadingContainer.findViewById<View>(R.id.dot4)
                )

                dots.forEachIndexed { index, dot ->
                    val translateAnimation =
                        ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                    val scaleXAnimation =
                        ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
                    val scaleYAnimation =
                        ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)

                    listOf(
                        translateAnimation,
                        scaleXAnimation,
                        scaleYAnimation
                    ).forEach { animation ->
                        animation.duration = 500
                        animation.repeatCount = ObjectAnimator.INFINITE
                        animation.repeatMode = ObjectAnimator.REVERSE
                        animation.startDelay = (index * 100).toLong()
                        animation.start()
                    }
                }

                val maxHeight = (resources.displayMetrics.heightPixels * 0.85).toInt()

                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bottomSheet ->
                        val behavior = BottomSheetBehavior.from(bottomSheet)

                        behavior.apply {
                            this.peekHeight =
                                maxHeight  // Set the initial height when peeking
                            this.state =
                                BottomSheetBehavior.STATE_EXPANDED  // Start fully expanded
                            this.isFitToContents =
                                true  // Content will determine the height (up to maxHeight)
                            this.isDraggable =
                                false  // Prevent user from dragging the sheet
                        }

                        // Set a fixed height for the bottom sheet
                        bottomSheet.layoutParams?.height = maxHeight
                    }

                dialog.show()

                btnConfirmScanPanenTPH.setOnClickListener {
                    AlertDialogUtility.withTwoActions(
                        this,
                        getString(R.string.al_yes),
                        getString(R.string.confirmation_dialog_title),
                        "${getString(R.string.al_make_sure_scanned_qr)}",
                        "warning.json",
                        ContextCompat.getColor(
                            this,
                            R.color.bluedarklight
                        ),
                        function = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    withContext(Dispatchers.Main) {
                                        loadingDialog.show()
                                    }

                                    // Validate data first
                                    if (mappedData.isEmpty()) {
                                        throw Exception("No data to archive")
                                    }

                                    var hasError = false
                                    var successCount = 0
                                    val errorMessages = mutableListOf<String>()

                                    val effectiveLimit =
                                        if (limit == 0) mappedData.size else limit
                                    takeQRCodeScreenshot(view)

                                    // Take only the required number of items
                                    val limitedData =
                                        mappedData.take(effectiveLimit).toMutableList()
                                    val itemsToRemove = mutableListOf<Map<String, Any?>>()

                                    limitedData.forEach { item ->
                                        try {
                                            // Null check for item
                                            if (item == null) {
                                                errorMessages.add("Found null item in data")
                                                hasError = true
                                                return@forEach
                                            }

                                            // ID validation
                                            val id = when (val idValue = item["id"]) {
                                                null -> {
                                                    errorMessages.add("ID is null")
                                                    hasError = true
                                                    return@forEach
                                                }

                                                !is Number -> {
                                                    errorMessages.add("Invalid ID format: $idValue")
                                                    hasError = true
                                                    return@forEach
                                                }

                                                else -> idValue.toInt()
                                            }

                                            if (id <= 0) {
                                                errorMessages.add("Invalid ID value: $id")
                                                hasError = true
                                                return@forEach
                                            }

                                            try {
                                                viewModel.archiveMpanenByID(id)

                                                // Mark this item for removal from mappedData after successful archiving
                                                itemsToRemove.add(item)

                                                successCount++
                                            } catch (e: SQLiteException) {
                                                errorMessages.add("Database error for ID $id: ${e.message}")
                                                hasError = true
                                            } catch (e: Exception) {
                                                errorMessages.add("Error archiving ID $id: ${e.message}")
                                                hasError = true
                                            }

                                        } catch (e: Exception) {
                                            errorMessages.add("Unexpected error processing item: ${e.message}")
                                            hasError = true
                                        }
                                    }

// Remove successfully processed items from mappedData
//                                    mappedData = mappedData.filter { it !in itemsToRemove }.toMutableList()

                                    // Show results
                                    withContext(Dispatchers.Main) {
                                        try {
                                            loadingDialog.dismiss()

                                            when {
                                                successCount == 0 -> {
                                                    val errorDetail =
                                                        errorMessages.joinToString("\n")
                                                    AppLogger.e("Archive failed. Errors:\n$errorDetail")
                                                    Toast.makeText(
                                                        this@TransferHektarPanenActivity,
                                                        "Gagal mengarsipkan data",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                hasError -> {
                                                    val errorDetail =
                                                        errorMessages.joinToString("\n")
                                                    AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                    Toast.makeText(
                                                        this@TransferHektarPanenActivity,
                                                        "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                else -> {
                                                    AppLogger.d("All items archived successfully")
                                                    playSound(R.raw.berhasil_konfirmasi)

                                                    delay(200)
                                                    viewModel.getAllScanMPanenByDate(
                                                        0,
                                                        globalFormattedDate
                                                    )
                                                    delay(400)
                                                    AppLogger.d("All items archived successfully")
                                                    loadingDialog.show()
                                                    loadingDialog.setMessage(
                                                        "Sedang mengambil data",
                                                        true
                                                    )

                                                    Toast.makeText(
                                                        this@TransferHektarPanenActivity,
                                                        "Semua data berhasil diarsipkan",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                }
                                            }
                                            dialog.dismiss()
                                        } catch (e: Exception) {
                                            AppLogger.e("Error in UI update: ${e.message}")
                                            Toast.makeText(
                                                this@TransferHektarPanenActivity,
                                                "Terjadi kesalahan pada UI",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                } catch (e: Exception) {
                                    AppLogger.e("Fatal error in archiving process: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        try {
                                            loadingDialog.dismiss()
                                            Toast.makeText(
                                                this@TransferHektarPanenActivity,
                                                "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            dialog.dismiss()
                                        } catch (dialogException: Exception) {
                                            AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                        }
                                    }
                                }
                            }
                        }
                    ) {


                    }
                }

                lifecycleScope.launch {
                    try {
                        val effectiveLimit =
                            if (limit == 0) mappedData.size else limit

                        val jsonData = withContext(Dispatchers.IO) {
                            try {

                                // Take only the required number of items
                                val limitedData = mappedData.take(effectiveLimit)
                                formatPanenDataForQR(limitedData)
                            } catch (e: Exception) {
                                AppLogger.e("Error generating JSON data: ${e.message}")
                                throw e
                            }
                        }

                        AppLogger.d("jsonData $jsonData")
                        val encodedData = withContext(Dispatchers.IO) {
                            try {
                                encodeJsonToBase64ZipQR(jsonData)
                                    ?: throw Exception("Encoding failed")
                            } catch (e: Exception) {
                                AppLogger.e("Error encoding data: ${e.message}")
                                throw e
                            }
                        }

                        val limitedData = mappedData.take(effectiveLimit)
                        val processedData =
                            AppUtils.getPanenProcessedData(limitedData, featureName)
                        val listBlok = view.findViewById<TextView>(R.id.listBlok)
                        val totalJjg = view.findViewById<TextView>(R.id.totalJjg)
                        val totalTPH = view.findViewById<TextView>(R.id.totalTPH)
                        val blokSection = view.findViewById<LinearLayout>(R.id.blok_section)
                        val totalSection = view.findViewById<LinearLayout>(R.id.total_section)
                        listBlok.text = processedData["blokDisplay"].toString()
                        totalJjg.text = processedData["totalJjgCount"].toString()
                        totalTPH.text = processedData["tphCount"].toString()

                        // Switch to the main thread for UI updates
                        withContext(Dispatchers.Main) {
                            try {
                                ListPanenTBSActivity().generateHighQualityQRCode(
                                    encodedData,
                                    qrCodeImageView,
                                    this@TransferHektarPanenActivity,
                                    showLogo = false
                                )
                                val fadeOut =
                                    ObjectAnimator.ofFloat(loadingLogo, "alpha", 1f, 0f)
                                        .apply {
                                            duration = 250
                                        }
                                val fadeOutDots =
                                    ObjectAnimator.ofFloat(
                                        loadingContainer,
                                        "alpha",
                                        1f,
                                        0f
                                    )
                                        .apply {
                                            duration = 250
                                        }

                                // Ensure QR code and other elements start invisible
                                blokSection.alpha = 0f
                                totalSection.alpha = 0f
                                qrCodeImageView.alpha = 0f
                                dashedLine.alpha = 0f
                                tvTitleQRGenerate.alpha = 0f
                                titleQRConfirm.alpha = 0f
                                confimationContainer.alpha = 0f
                                descQRConfirm.alpha = 0f
                                btnConfirmScanPanenTPH.alpha = 0f
                                btnPreviewFullQR.alpha = 0f

                                // Create fade-in animations
                                val fadeInBlokSection =
                                    ObjectAnimator.ofFloat(blokSection, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInTotalSection =
                                    ObjectAnimator.ofFloat(totalSection, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInQR =
                                    ObjectAnimator.ofFloat(qrCodeImageView, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInDashedLine =
                                    ObjectAnimator.ofFloat(dashedLine, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInTitle =
                                    ObjectAnimator.ofFloat(
                                        tvTitleQRGenerate,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInTitleConfirm =
                                    ObjectAnimator.ofFloat(titleQRConfirm, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInDescConfirm =
                                    ObjectAnimator.ofFloat(descQRConfirm, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInConfirmationContainer = ObjectAnimator.ofFloat(
                                    confimationContainer,
                                    "alpha",
                                    0f,
                                    1f
                                ).apply {
                                    duration = 250
                                    startDelay = 150
                                }
                                val fadeInButton =
                                    ObjectAnimator.ofFloat(
                                        btnConfirmScanPanenTPH,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInButtonPreviewBtn =
                                    ObjectAnimator.ofFloat(
                                        btnPreviewFullQR,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }


                                // Run animations sequentially
                                AnimatorSet().apply {
                                    playTogether(fadeOut, fadeOutDots)
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            // Hide loading elements
                                            loadingLogo.visibility = View.GONE
                                            loadingContainer.visibility = View.GONE

                                            // Show elements
                                            confimationContainer.visibility = View.VISIBLE
                                            tvTitleQRGenerate.visibility = View.VISIBLE
                                            qrCodeImageView.visibility = View.VISIBLE
                                            dashedLine.visibility = View.VISIBLE
                                            blokSection.visibility = View.VISIBLE
                                            totalSection.visibility = View.VISIBLE
                                            btnConfirmScanPanenTPH.visibility = View.VISIBLE
                                            btnPreviewFullQR.visibility = View.VISIBLE

                                            lifecycleScope.launch {
                                                delay(200)
                                                playSound(R.raw.berhasil_generate_qr)
                                                delay(300)
                                            }

                                            // Start fade-in animations
                                            fadeInBlokSection.start()
                                            fadeInTotalSection.start()
                                            fadeInQR.start()
                                            fadeInDashedLine.start()
                                            fadeInTitle.start()
                                            fadeInTitleConfirm.start()
                                            fadeInConfirmationContainer.start()
                                            fadeInDescConfirm.start()
                                            fadeInButton.start()
                                            fadeInButtonPreviewBtn.start()

                                        }
                                    })
                                    start()
                                }
                            } catch (e: Exception) {
                                // Handle UI-related errors on the main thread
                                loadingLogo.animation?.cancel()
                                loadingLogo.clearAnimation()
                                loadingLogo.visibility = View.GONE
                                loadingContainer.visibility = View.GONE
                                AppLogger.e("QR Generation UI Error: ${e.message}")
                                showErrorMessageGenerateQR(
                                    view,
                                    "Error generating QR code: ${e.message}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any other errors
                        withContext(Dispatchers.Main) {
                            AppLogger.e("Error in QR process: ${e.message}")
                            stopLoadingAnimation(loadingLogo, loadingContainer)
                            showErrorMessageGenerateQR(
                                view,
                                "Error processing QR code: ${e.message}"
                            )
                        }
                    }
                }
            },
            cancelFunction = {

            }
        )
    }

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }
            Log.d("formatPanenDataForQR", "mappedData: $mappedData")
            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")
                        val nik = data["karyawan_nik"]?.toString()
                            ?: throw IllegalArgumentException("Missing karyawan_nik.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val keyUn = "UN"
                        val unValue = if (jjgJson.has(keyUn)) {
                            jjgJson.getInt(keyUn)
                        } else {
                            throw IllegalArgumentException("Missing '$keyUn' key in jjg_json: $jjgJsonString")
                        }

                        val keyOv = "OV"
                        val ovValue = if (jjgJson.has(keyOv)) {
                            jjgJson.getInt(keyOv)
                        } else {
                            throw IllegalArgumentException("Missing '$keyOv' key in jjg_json: $jjgJsonString")
                        }

                        val keyEm = "EM"
                        val emValue = if (jjgJson.has(keyEm)) {
                            jjgJson.getInt(keyEm)
                        } else {
                            throw IllegalArgumentException("Missing '$keyEm' key in jjg_json: $jjgJsonString")
                        }

                        val keyAb = "AB"
                        val abValue = if (jjgJson.has(keyAb)) {
                            jjgJson.getInt(keyAb)
                        } else {
                            throw IllegalArgumentException("Missing '$keyAb' key in jjg_json: $jjgJsonString")
                        }

                        val keyRi = "RI"
                        val riValue = if (jjgJson.has(keyRi)) {
                            jjgJson.getInt(keyRi)
                        } else {
                            throw IllegalArgumentException("Missing '$keyRi' key in jjg_json: $jjgJsonString")
                        }

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-03-28
                        val time = dateParts[1]  // 13:15:18

                        // Use dateIndexMap.size as the index for new dates
                        append(
                            "${dateIndexMap.getOrPut(date) { dateIndexMap.size }}," +
                                    "$time," +
                                    "$tphId," +
                                    "${nikIndexMap.getOrPut(nik) { nikIndexMap.size }}," +
                                    "$unValue,$ovValue,$emValue,$abValue,$riValue;"
                        )
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            val username = try {
                PrefManager(this).username.toString().split("@")[0].takeLast(3).uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil username: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                "NULL"
            }

            // Create the tgl object with date mappings
            val tglJson = JSONObject()
            dateIndexMap.forEach { (date, index) ->
                tglJson.put(index.toString(), date)
            }

            // Create the tgl object with date mappings
            val nikJson = JSONObject()
            nikIndexMap.forEach { (date, index) ->
                nikJson.put(index.toString(), date)
            }

            val json = JSONObject().apply {
                put("tph_0", formattedData)
                put("username", username)
                put("tgl", tglJson)
                put("nik", nikJson)
                put("kemandoran_id", kemandoranUserLogin)
            }.toString()

            Log.d("formatPanenDataForQR", "json: $json")

            return json
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

    // Helper function to show errors
    fun showErrorMessageGenerateQR(view: View, message: String) {
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }

    // Helper function to stop the loading animation and hide UI
    fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
    }

    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Minify JSON first
            val minifiedJson = JSONObject(jsonData).toString()

            // Reject empty JSON
            if (minifiedJson == "{}") {
                AppLogger.e("Empty JSON detected, returning null")
                throw IllegalArgumentException("Empty JSON detected")
            }

            // Create a byte array output stream to hold the zip data
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                ZipOutputStream(byteArrayOutputStream).apply {
                    setLevel(Deflater.BEST_COMPRESSION)
                }.use { zipOutputStream ->
                    val entry = ZipEntry("output.json")
                    zipOutputStream.putNextEntry(entry)
                    zipOutputStream.write(minifiedJson.toByteArray(StandardCharsets.UTF_8))
                    zipOutputStream.closeEntry()
                }

                val zipBytes = byteArrayOutputStream.toByteArray()
                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)

                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                firstHalf + "5nqHzPKdlILxS9ABpClq" + secondHalf
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            throw IllegalArgumentException(e.message.toString())
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        }
    }


    private fun showQrCodeFullScreen(qrDrawable: Drawable?, bottomSheetView: View) {
        if (qrDrawable == null) return

        // Get the bottom sheet behavior to control it
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView.parent as View)

        // Save current state to restore later
        val previousState = bottomSheetBehavior.state

        // Expand bottom sheet fully first
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Create a dialog to display the QR code
        val context = bottomSheetView.context
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Make dialog dismissible with back button
        dialog.setCancelable(true)
        dialog.setOnCancelListener {
            // Restore previous bottom sheet state when dismissed with back button
            bottomSheetBehavior.state = previousState
        }

        // Inflate the camera_edit layout
        val fullscreenView = layoutInflater.inflate(R.layout.camera_edit, null)
        dialog.setContentView(fullscreenView)

        // Find views within the dialog layout
        val fotoLayout = fullscreenView.findViewById<ConstraintLayout>(R.id.clZoomLayout)
        val photoView = fullscreenView.findViewById<PhotoView>(R.id.fotoZoom)
        val closeZoomCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardCloseZoom)
        val changePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val deletePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardDeletePhoto)

        // Find the TextView and ImageView for color changes
        val tvCardCloseButton = fullscreenView.findViewById<TextView>(R.id.tvCardCloseButton)
        val closeZoomIcon = fullscreenView.findViewById<ImageView>(R.id.closeZoom)

        // Set the image to the PhotoView
        photoView.setImageDrawable(qrDrawable)

        // Hide edit options
        changePhotoCard.visibility = View.GONE
        deletePhotoCard.visibility = View.GONE

        // Set background color of the layout to white using your color resource
        fotoLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white))

        val closeCardLinearLayout = closeZoomCard.getChildAt(0) as LinearLayout
        closeCardLinearLayout.setBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.greenDarker
            )
        )

        // Change the text color to white
        tvCardCloseButton.setTextColor(ContextCompat.getColor(context, R.color.white))

        // Change the close icon tint to white
        closeZoomIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))

        // Set up close button to restore previous bottom sheet state
        closeZoomCard.setOnClickListener {
            dialog.dismiss()
            // Restore previous bottom sheet state
            bottomSheetBehavior.state = previousState
        }

        // Make dialog display properly
        dialog.window?.apply {
            // Set window background to white using your color resource
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.white)))
            setDimAmount(0f) // Remove dimming since we have a white background

            // This is important - use TYPE_APPLICATION to ensure it appears above the bottom sheet
            attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION

            // Make sure to set the layout flags properly
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // Add FLAG_NOT_TOUCH_MODAL to make sure it gets all touch events
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

            // This helps ensure it appears on top
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    private fun takeQRCodeScreenshot(view: View) {

        lifecycleScope.launch {
            try {
                val screenshotLayout: View =
                    if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                        layoutInflater.inflate(R.layout.layout_screenshot_qr_mandor, null)
                    } else {
                        layoutInflater.inflate(R.layout.layout_screenshot_qr_kpanen, null)
                    }

                // Get references to views in the custom layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView = screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
                val tvFooter = screenshotLayout.findViewById<TextView>(R.id.tvFooter)

                // Get references to included layouts
                val infoBlokList = screenshotLayout.findViewById<View>(R.id.infoBlokList)
                val infoTotalJjg = screenshotLayout.findViewById<View>(R.id.infoTotalJjg)
                val infoTotalTransaksi =
                    screenshotLayout.findViewById<View>(R.id.infoTotalTransaksi)

                // Add references for new info views
                val infoUrutanKe = screenshotLayout.findViewById<View>(R.id.infoUrutanKe)
                val infoJamTanggal = screenshotLayout.findViewById<View>(R.id.infoJamTanggal)

                fun setInfoData(includeView: View, labelText: String, valueText: String) {
                    val tvLabel = includeView.findViewById<TextView>(R.id.tvLabel)
                    val tvValue = includeView.findViewById<TextView>(R.id.tvValue)
                    tvLabel.text = labelText
                    tvValue.text = valueText
                }

                // Get the QR code bitmap from the current view
                val currentQrImageView = view.findViewById<ImageView>(R.id.qrCodeImageView)
                val qrBitmap = currentQrImageView.drawable?.let { drawable ->
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        // Convert drawable to bitmap if not already a BitmapDrawable
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    }
                }

                qrCodeImageView.setImageBitmap(qrBitmap)

                // Generate current date and time for footer
                val currentDate = Date()
                val indonesianLocale = Locale("id", "ID")
                val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val formattedDate = dateFormat.format(currentDate).uppercase(indonesianLocale)
                val formattedTime = timeFormat.format(currentDate)

                // Get and increment screenshot counter
                val screenshotNumber = getAndIncrementScreenshotCounter()

                val effectiveLimit = if (limit == 0) mappedData.size else limit
                val limitedData = mappedData.take(effectiveLimit)

                val processedData = AppUtils.getPanenProcessedData(limitedData, featureName)

                val capitalizedFeatureName = featureName!!.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                tvUserName.text =
                    "Hasil QR ${capitalizedFeatureName} dari ${prefManager!!.jabatanUserLogin} - ${prefManager!!.estateUserLogin}"

                setInfoData(infoBlokList, "Blok", ": ${processedData["blokDisplay"]}")
                setInfoData(
                    infoTotalJjg,
                    "Total Janjang",
                    ": ${processedData["totalJjgCount"]} jjg"
                )
                setInfoData(
                    infoTotalTransaksi,
                    "Jumlah Transaksi",
                    ": ${processedData["tphCount"]}"
                )

                // Add new info data
                setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                setInfoData(infoJamTanggal, "Jam & Tanggal", ": $formattedDate, $formattedTime")

                tvFooter.text =
                    "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = LinearLayout.LayoutParams.WRAP_CONTENT

                screenshotLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                screenshotLayout.layout(
                    0, 0, screenshotLayout.measuredWidth, screenshotLayout.measuredHeight
                )

                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val screenshotFileName = "Transer_Hektar_Panen_$date"

                val watermarkType = if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
                } else if (featureName == AppUtils.ListFeatureNames.TransferHektarPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMTransferHektarPanen
                } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                    AppUtils.WaterMarkFotoDanFolder.WMESPB
                } else if (featureName == AppUtils.ListFeatureNames.AbsensiPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
                } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                    AppUtils.WaterMarkFotoDanFolder.WMRekapPanenDanRestan
                } else if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                    AppUtils.WaterMarkFotoDanFolder.WMESPB
                } else {
                    AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
                }

                val screenshotFile = ScreenshotUtil.takeScreenshot(
                    screenshotLayout,
                    screenshotFileName,
                    watermarkType
                )

                if (screenshotFile != null) {
                    Toasty.success(
                        this@TransferHektarPanenActivity,
                        "QR sudah tersimpan digaleri",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                }
            } catch (e: Exception) {
                AppLogger.e("Error taking QR screenshot: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferHektarPanenActivity,
                        "Gagal menyimpan QR Code: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getAndIncrementScreenshotCounter(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefManager!!.getScreenshotDate(featureName!!)
        val currentCounter = prefManager!!.getScreenshotCounter(featureName!!)

        return if (lastDate != today) {
            // Reset counter for new day
            prefManager!!.setScreenshotDate(featureName!!, today)
            prefManager!!.setScreenshotCounter(featureName!!, 1)
            1
        } else {
            // Increment counter for same day
            val newCounter = currentCounter + 1
            prefManager!!.setScreenshotCounter(featureName!!, newCounter)
            newCounter
        }
    }
}
