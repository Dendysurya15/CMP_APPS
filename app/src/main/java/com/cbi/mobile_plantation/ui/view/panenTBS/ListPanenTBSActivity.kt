package com.cbi.mobile_plantation.ui.view.panenTBS

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter
import com.cbi.mobile_plantation.ui.adapter.TPHItem
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.adapter.detailESPBListTPHAdapter
import com.cbi.mobile_plantation.ui.view.HektarPanen.TransferHektarPanenActivity.VerificationResult
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.espb.FormESPBActivity
import com.cbi.mobile_plantation.ui.view.ScanQR

import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel
import com.cbi.mobile_plantation.ui.viewModel.MutuBuahViewModel

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
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Suppress("IMPLICIT_CAST_TO_ANY")
class ListPanenTBSActivity : AppCompatActivity() {
    private var featureName = ""
    private var listTPHDriver = ""
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var mutuBuahViewModel: MutuBuahViewModel
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var listAdapter: ListPanenTPHAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var currentState = 0 // 0 for tersimpan, 1 for terscan
    private var prefManager: PrefManager? = null
    private var isSettingUpCheckbox = false
    private var activityInitialized = false

    private var globalFormattedDate: String = ""
    private lateinit var absensiViewModel: AbsensiViewModel
    private var dropdownAbsensiEdit: List<String> = emptyList()
    private var dropdownAbsensiFullData = emptyList<KaryawanModel>() // <- GLOBAL VARIABLE
    private var presentNikSet = mutableSetOf<String>()
    private var lastClickedPosition: Int = -1
    private var shouldReopenLastPosition = false
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var cardRekapPerPemanen: MaterialCardView
    private lateinit var cardRekapPerBlok: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private lateinit var counterPerPemanen: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var speedDial: SpeedDialView
    private var pemuatNamaESPB = "-"
    private var isAscendingOrder = true
    private var originalMappedData: MutableList<Map<String, Any>> = mutableListOf()
    private lateinit var searchEditText: EditText
    private lateinit var sortButton: ImageView // Add this at class level
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private lateinit var btnAddMoreTph: FloatingActionButton
    private var tph1IdPanen = ""
    private var tph1NoIdPanen = ""
    private var mappedData: List<Map<String, Any>> = emptyList()

    private var espbId = 0
    private var jjg = 0
    private var noespb = "NULL"
    private var blok = "NULL"
    private var tph = 0
    private var tph0 = ""
    private var tph1 = ""
    private var limit = 0
    private var tphListScan: List<String> = emptyList()

    private var blok_jjg = "NULL"
    private var nopol = "NULL"
    private var driver = "NULL"
    private var pemuat_id = "NULL"
    private var kemandoran_id = "NULL"
    private var pemuat_nik = "NULL"
    private var transporter_id = 0
    private var mill_id = 0
    private var created_by_id = 0
    private var no_espb = "NULL"
    private var tph0QR = "NULL"
    private var tph1QR = "NULL"
    private var creatorInfo = "NULL"
    private var dateTime = "NULL"
    private lateinit var filterAllData: CheckBox
    private var idsToUpdate = "NULL"
    private val todayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
    private val todayDate = todayDateFormat.format(Date())
    private lateinit var ll_detail_espb: LinearLayout
    private lateinit var dateButton: Button
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private lateinit var bluetoothScanner: BluetoothScanner
    private var bluetoothJsonData: String = ""
    private var bluetoothDataInfo: String = ""
    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shouldRestoreCheckboxState = false
    private var previouslySelectedTphIds = mutableSetOf<String>()
    private val dateIndexMap = mutableMapOf<String, Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        //cek tanggal otomatis
        checkDateTimeSettings()
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
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun triggerViewModelByFeature(
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        isFilterAll: Boolean = false  // Add this parameter
    ) {
        // Determine the actual date to pass - null if filtering all, otherwise the date
        val filterDate = if (isFilterAll) null else date

        AppLogger.d("date: $date, isFilterAll: $isFilterAll, filterDate: $filterDate")

        if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
            if (currentState == 0) {
                panenViewModel.loadTPHNonESPB(0, 0, true, 0, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 0, filterDate)
                panenViewModel.countTPHESPB(1, 0, true, 0, filterDate)
            } else if (currentState == 1) {
                panenViewModel.loadTPHESPB(1, 0, true, 0, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 0, filterDate)
                panenViewModel.countTPHESPB(1, 0, true, 0, filterDate)
            } else if (currentState == 2) {
                panenViewModel.loadTPHNonESPB(1, 0, true, 0, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 0, filterDate)
                panenViewModel.countTPHESPB(1, 0, true, 0, filterDate)
            }
        } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
            if (currentState == 0) {
                AppLogger.d("bug kah ? ")
                panenViewModel.loadTPHNonESPB(0, 0, true, 1, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 1, filterDate)
                panenViewModel.countTPHESPB(0, 1, true, 1, filterDate)
                panenViewModel.countHasBeenESPB(0, 0, false, 1, filterDate)
            }
            else if (currentState == 1) {
                panenViewModel.loadTPHESPB(0, 1, true, 1, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 1, filterDate)
                panenViewModel.countTPHESPB(0, 1, true, 1, filterDate)
                panenViewModel.countHasBeenESPB(0, 0, false, 1, filterDate)
            } else {
                panenViewModel.loadTPHESPB(0, 0, false, 1, filterDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 1, filterDate)
                panenViewModel.countTPHESPB(0, 1, true, 1, filterDate)
                panenViewModel.countHasBeenESPB(0, 0, false, 1, filterDate)
            }
        } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
            panenViewModel.loadTPHNonESPB(0, 0, true, 1, filterDate)
        } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
            if (currentState == 0) {
                mutuBuahViewModel.loadMBUnuploaded(0, filterDate)
                mutuBuahViewModel.countMBUnuploaded(filterDate)
                mutuBuahViewModel.countMBUploaded(filterDate)
            } else {
                mutuBuahViewModel.loadMBUnuploaded(3, filterDate)
                mutuBuahViewModel.countMBUnuploaded(filterDate)
                mutuBuahViewModel.countMBUploaded(filterDate)
            }
        }
    }

    private fun processSelectedDate(selectedDate: String) {
//        loadingDialog.show()
//        loadingDialog.setMessage("Sedang mengambil data...", true)

        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate
        triggerViewModelByFeature(selectedDate, false)
        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE
//            loadingDialog.show()
//            loadingDialog.setMessage("Sedang mengambil data...", true)
            // Get today's date in backend format
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )

            // Reset the selected date in your utils
            AppUtils.setSelectedDate(todayBackendDate)

            // Update the dateButton to show today's date
            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate
            triggerViewModelByFeature(todayBackendDate, false)

        }

        filterDateContainer.visibility = View.VISIBLE
    }

    private fun setupUI() {
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        shouldRestoreCheckboxState = intent.getBooleanExtra("RESTORE_CHECKBOX_STATE", false)

        globalFormattedDate = AppUtils.currentDate
        if (featureName == AppUtils.ListFeatureNames.BuatESPB || featureName == AppUtils.ListFeatureNames.DetailESPB) {
            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.GONE
            findViewById<LinearLayout>(R.id.filterDateContainer).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
            dateButton = findViewById(R.id.calendarPicker)
            dateButton.text = AppUtils.getTodaysDate()

            filterAllData = findViewById(R.id.calendarCheckbox)

            filterAllData.setOnCheckedChangeListener { _, isChecked ->
                val selectedDate = globalFormattedDate // Get the selected date
                val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
                val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
                if (isChecked) {
//                    loadingDialog.show()
//                    loadingDialog.setMessage("Sedang mengambil data...", true)
                    filterDateContainer.visibility = View.VISIBLE
                    nameFilterDate.text = "Semua Data"
                    dateButton.isEnabled = false
                    dateButton.alpha = 0.5f
                    triggerViewModelByFeature("", true)
                } else {
                    val displayDate = formatGlobalDate(globalFormattedDate)
                    dateButton.text = displayDate
                    triggerViewModelByFeature(globalFormattedDate, false)
                    nameFilterDate.text = displayDate
                    dateButton.isEnabled = true
                    dateButton.alpha = 1f // Make the button appear darker
                    Log.d("FilterAllData", "Checkbox is UNCHECKED. Button enabled.")
                }


                val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

                removeFilterDate.setOnClickListener {
                    if (filterAllData.isChecked) {
                        filterAllData.isChecked = false
                    }

                    filterDateContainer.visibility = View.GONE


                    // Get today's date in backend format
                    val todayBackendDate = AppUtils.formatDateForBackend(
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                        Calendar.getInstance().get(Calendar.MONTH) + 1,
                        Calendar.getInstance().get(Calendar.YEAR)
                    )

                    // Reset the selected date in your utils
                    AppUtils.setSelectedDate(todayBackendDate)

                    // Update the dateButton to show today's date
                    val todayDisplayDate = AppUtils.getTodaysDate()
                    dateButton.text = todayDisplayDate
                    triggerViewModelByFeature(todayBackendDate, false)
                }
            }
        }

        listTPHDriver = try {
            AppUtils.readJsonFromEncryptedBase64Zip(
                intent.getStringExtra("scannedResult").toString()
            ).toString()
        } catch (e: Exception) {
            Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        Log.d("listTPHDriver", listTPHDriver.toString())

        prefManager = PrefManager(this)
        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin


        if (listTPHDriver.isNotEmpty() && !shouldRestoreCheckboxState) {

            AppLogger.d("masuk sini gask sih")
            AppLogger.d("listTPHDriver $listTPHDriver")


            AppLogger.d("tph1 bro $tph1")
            // Extract TPH IDs from the current scan
            val currentScanTphIds = try {
                val tphString = listTPHDriver
                    .removePrefix("""{"tph":"""")
                    .removeSuffix(""""}""")
                tphString
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }


            // If we have previous scan data stored in tph1, extract TPH IDs
            val previousScanTphIds = if (tph1.isNotEmpty()) {
                // Get the TPH IDs from tph1 string
                tph1.split(";").mapNotNull { entry ->
                    try {
                        entry.split(",").firstOrNull()
                    } catch (e: Exception) {
                        Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        null
                    }
                }.joinToString(";")
            } else ""

            // Combine previous and current scan TPH IDs
            val combinedTphIds =
                if (previousScanTphIds.isNotEmpty() && currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds;$currentScanTphIds"}"""
                } else if (currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$currentScanTphIds"}"""
                } else if (previousScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds"}"""
                } else ""

            // Update listTPHDriver with combined data
            if (combinedTphIds.isNotEmpty()) {
                listTPHDriver = combinedTphIds
                Log.d("ListPanenTBSActivity", "Combined TPH IDs: $listTPHDriver")
            }
        }

        setupHeader()
        initViewModel()
        initializeViews()
        loadingDialog = LoadingDialog(this)
        setupRecyclerView()
        setupSearch()
        setupObservers()
        bluetoothScanner = BluetoothScanner(this)
        if (featureName != "Buat eSPB" && featureName != "Detail eSPB") {
            setupSpeedDial()
            setupCheckboxControl()  // Add this
        }
        setupCardListeners()
        initializeFilterViews()
        setupSortButton()
        currentState = 0
        setActiveCard(cardTersimpan)
        absensiViewModel.loadActiveAbsensi()
        if (featureName == "Detail eSPB") {
            findViewById<TextView>(R.id.tv_card_tersimpan).text = "Rekap Per Transaksi"
            findViewById<TextView>(R.id.tv_card_terscan).text = "Rekap Per Blok"
            findViewById<TextView>(R.id.counter_item_tersimpan).visibility = View.GONE
            findViewById<TextView>(R.id.counter_item_terscan).visibility = View.GONE
            cardTersimpan.visibility = View.VISIBLE
            cardTerscan.visibility = View.VISIBLE
            val app = AppRepository(application)
            val espbViewModelFactory = ESPBViewModel.ESPBViewModelFactory(app)
            espbViewModel = ViewModelProvider(this, espbViewModelFactory)[ESPBViewModel::class.java]
            espbId = try {
                intent.getStringExtra("id_espb").toString().toInt()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil id eSPB: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                0
            }

            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.GONE
            findViewById<LinearLayout>(R.id.filterDateContainer).visibility = View.GONE
        }
        lifecycleScope.launch {
            if (featureName == "Buat eSPB") {
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                val isStopScan = intent.getBooleanExtra("IS_STOP_SCAN_ESPB", false)
                if (!isStopScan && !shouldRestoreCheckboxState) {

                    playSound(R.raw.berhasil_scan)
                }
                panenViewModel.loadTPHNonESPB(0, 0, true, 1, AppUtils.currentDate)
                findViewById<HorizontalScrollView>(R.id.horizontalCardFeature).visibility =
                    View.GONE
            } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                counterPerPemanen.visibility = View.GONE
                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBox.visibility = View.GONE
                val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
                flCheckBoxTableHeaderLayout.visibility = View.GONE
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE

                // Load initial data for Mutu Buah
                mutuBuahViewModel.loadMBUnuploaded(0, AppUtils.currentDate)
                mutuBuahViewModel.countMBUnuploaded(AppUtils.currentDate)
                mutuBuahViewModel.countMBUploaded(AppUtils.currentDate)
                findViewById<TextView>(R.id.tv_card_tersimpan).text = "Tersimpan"
                findViewById<TextView>(R.id.tv_card_terscan).text = "Sudah Upload"

            } else if (featureName == "Rekap panen dan restan") {

                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                findViewById<TextView>(R.id.tv_card_tersimpan).text = "Rekap TPH"
                findViewById<TextView>(R.id.tv_card_terscan).text = "Sudah Transfer"
                cardRekapPerPemanen.visibility = View.VISIBLE
                findViewById<TextView>(R.id.tv_card_pemanen).text = "TPH Menjadi E-SPB"
                panenViewModel.loadTPHNonESPB(0, 0, true, 1, AppUtils.currentDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 1, AppUtils.currentDate)
                panenViewModel.countTPHESPB(0, 1, true, 1, AppUtils.currentDate)
                panenViewModel.countHasBeenESPB(0, 0, false, 1, AppUtils.currentDate)
                val headerCheckBoxPanen = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBoxPanen.visibility = View.GONE
            } else if (featureName == "Detail eSPB") {
                val btnEditEspb = findViewById<FloatingActionButton>(R.id.btnEditEspb)
                btnEditEspb.visibility = View.VISIBLE
                val labelEditEspb = findViewById<TextView>(R.id.labelEditEspb)
                labelEditEspb.visibility = View.VISIBLE
                val labelGenerateQR = findViewById<TextView>(R.id.labelGenerateQR)
                labelGenerateQR.visibility = View.VISIBLE
                ll_detail_espb = findViewById<LinearLayout>(R.id.ll_detail_espb)
                ll_detail_espb.visibility = View.VISIBLE
                espbViewModel.getESPBById(espbId)
                espbViewModel.espbEntity.observe(this@ListPanenTBSActivity) { espbWithRelations ->
                    if (espbWithRelations != null) {
                        try {
                            // Extract ESPB data
                            val espb = espbWithRelations

                            // Find all included layouts
                            val tvNoEspb = findViewById<View>(R.id.tv_no_espb)
                            val tvNoPol = findViewById<View>(R.id.tv_no_pol)
                            val tvTransporter = findViewById<View>(R.id.tv_transporter)
                            val tvDriver = findViewById<View>(R.id.tv_driver)
                            val tvMill = findViewById<View>(R.id.tv_mill)
                            val tvMekanisasi = findViewById<View>(R.id.tv_mekanisasi)
//                            val tvDraft = findViewById<View>(R.id.tv_draft)

                            blok_jjg = espb.blok_jjg
                            nopol = espb.nopol
                            driver = espb.driver
                            pemuat_id = espb.pemuat_id
                            transporter_id = espb.transporter_id
                            mill_id = espb.mill_id
                            created_by_id = espb.created_by_id
                            no_espb = espb.noESPB
                            tph0QR = espb.tph0
                            tph1QR = espb.tph1
                            creatorInfo = espb.creator_info
                            dateTime = espb.created_at
                            kemandoran_id = espb.kemandoran_id
                            pemuat_nik = espb.pemuat_nik
                            tph1 = espb.tph1
                            tph0 = espb.tph0
                            idsToUpdate = espb.ids_to_update

                            val idKaryawanStringList = pemuat_id
                                .toString()                      // ensure it's a string
                                .split(",")                     // split on comma
                                .map { it.trim() }              // trim spaces
                                .filter { it.isNotEmpty() }     // remove empty strings

                            lifecycleScope.launch {
                                pemuatNamaESPB = try {
                                    val result = withContext(Dispatchers.IO) {
                                        panenViewModel.getPemuatByIdList(idKaryawanStringList)
                                    }

                                    result?.mapNotNull { it.nama }?.takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ") ?: "-"


                                } catch (e: Exception) {
                                    AppLogger.e("Gagal mendapatkan data pemuat: ${e.message}")
                                    Toasty.error(
                                        this@ListPanenTBSActivity,
                                        "Terjadi kesalahan saat mengambil data pemuat",
                                        Toasty.LENGTH_LONG
                                    ).show()
                                    "-"
                                }

                                AppLogger.d("Pemuat Nama ESPB: $pemuatNamaESPB") // ✅ Log AFTER the data is set
                            }

                            val btnTambahHapusTPHESPB =
                                findViewById<FloatingActionButton>(R.id.btnTambahHapusTPHESPB)

                            btnTambahHapusTPHESPB.setOnClickListener {
                                AlertDialogUtility.withTwoActions(
                                    this@ListPanenTBSActivity,
                                    "Lanjut",
                                    "Tambah/Hapus TPH",
                                    "Apakah anda yakin ingin Tambah/Hapus TPH di e-SPB ini?",
                                    "warning.json",
                                    function = {
                                        loadingDialog.show()
                                        fetchAndMergeTPHData(tph1)
                                    }
                                )
                            }
                            btnEditEspb.setOnClickListener {
                                AppLogger.d("tph1 $tph1")
                                AppLogger.d("tph0 $tph0")
                                AppLogger.d("espbId $espbId")
                                AppLogger.d("idsToUpdate $idsToUpdate")
                                AlertDialogUtility.withTwoActions(
                                    this@ListPanenTBSActivity,
                                    "EDIT",
                                    "Edit eSPB",
                                    "Apakah anda yakin ingin mengedit eSPB ini?",
                                    "warning.json",
                                    function = {
                                        val intent = Intent(
                                            this@ListPanenTBSActivity,
                                            FormESPBActivity::class.java
                                        )
                                        intent.putExtra("tph_1", tph1)
                                        Log.d("ListPanenTBSActivity", "tph1: $tph1")
                                        intent.putExtra("tph_0", tph0)
                                        Log.d("ListPanenTBSActivity", "tph0: $tph0")
                                        intent.putExtra("id_espb", espbId)
                                        Log.d("ListPanenTBSActivity", "id_espb: $espbId")
                                        intent.putExtra("tph_1_id_panen", idsToUpdate)
                                        Log.d(
                                            "ListPanenTBSActivity",
                                            "tph_1_id_panen: $idsToUpdate"
                                        )

                                        AppLogger.d("tph1 $tph1")
                                        AppLogger.d("tph0 $tph0")
                                        AppLogger.d("tph1 $espbId")
                                        AppLogger.d("idsToUpdate $idsToUpdate")
                                        playSound(R.raw.berhasil_edit_data)
                                        intent.putExtra("FEATURE_NAME", featureName)
                                        Log.d("ListPanenTBSActivity", "FEATURE_NAME: $featureName")
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                )
                            }

                            // Set No eSPB
                            tvNoEspb.findViewById<TextView>(R.id.tvTitleEspb).text = "No eSPB"
                            noespb = espb.noESPB
                            panenViewModel.getAllPanenWhereESPB(noespb)
                            tvNoEspb.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.noESPB

                            // Set No Polisi
                            tvNoPol.findViewById<TextView>(R.id.tvTitleEspb).text = "No Polisi"
                            tvNoPol.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.nopol

                            // Launch coroutines to fetch transporter and mill names
                            lifecycleScope.launch {
                                try {
                                    // Set Transporter
                                    tvTransporter.findViewById<TextView>(R.id.tvTitleEspb).text =
                                        "Transporter"
                                    val transporterName = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getTransporterNameById(espb.transporter_id)
                                                ?: "Internal"
                                        } catch (e: Exception) {
                                            Log.e(
                                                "ListPanenTBSActivity",
                                                "Error fetching transporter",
                                                e
                                            )
                                            "Internal"
                                        }
                                    }
                                    tvTransporter.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        transporterName

                                    // Set Mill
                                    tvMill.findViewById<TextView>(R.id.tvTitleEspb).text = "Mill"
                                    val millAbbr = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getMillNameById(espb.mill_id) ?: "Unknown"
                                        } catch (e: Exception) {
                                            Log.e("ListPanenTBSActivity", "Error fetching mill", e)
                                            "Unknown"
                                        }
                                    }
                                    tvMill.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        millAbbr
                                } catch (e: Exception) {
                                    Log.e("ListPanenTBSActivity", "Error in coroutine", e)
                                    Toasty.error(
                                        this@ListPanenTBSActivity,
                                        "Error loading details: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            // Set Driver
                            tvDriver.findViewById<TextView>(R.id.tvTitleEspb).text = "Driver"
                            tvDriver.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.driver

                            // Set Mekanisasi status
                            tvMekanisasi.findViewById<TextView>(R.id.tvTitleEspb).text =
                                "Mekanisasi"
                            val mekanisasiStatus =
                                if (espb.status_mekanisasi == 1) "Ya" else "Tidak"
                            tvMekanisasi.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                mekanisasiStatus

//                            // Set Draft status
//                            tvDraft.findViewById<TextView>(R.id.tvTitleEspb).text = "Status"
//                            val draftStatus = if (espb.status_draft == 1) "Draft" else "Final"
//                            tvDraft.findViewById<TextView>(R.id.tvSubTitleEspb).text = draftStatus

                            // Make the layout visible now that we've populated it
                            ll_detail_espb.visibility = View.VISIBLE

                        } catch (e: Exception) {
                            Toasty.error(
                                this@ListPanenTBSActivity,
                                "Error displaying eSPB details: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ListPanenTBSActivity", "Error displaying eSPB details", e)
                        }
                    } else {
                        Toasty.error(
                            this@ListPanenTBSActivity,
                            "eSPB data not found",
                            Toast.LENGTH_LONG
                        ).show()
                        ll_detail_espb.visibility = View.GONE
                    }
                }


                val btnTambahHapusTPHESPB =
                    findViewById<FloatingActionButton>(R.id.btnTambahHapusTPHESPB)
                btnTambahHapusTPHESPB.visibility = View.VISIBLE

                val labelTambahHapusTPHESPB =
                    findViewById<TextView>(R.id.labelTambahHapusTPHESPB)
                labelTambahHapusTPHESPB.visibility = View.VISIBLE


            } else {
                counterPerPemanen.visibility = View.GONE
                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBox.visibility = View.GONE
                val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
                flCheckBoxTableHeaderLayout.visibility = View.GONE
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                panenViewModel.loadTPHNonESPB(0, 0, true, 0, AppUtils.currentDate)
                panenViewModel.countTPHNonESPB(0, 0, true, 0, AppUtils.currentDate)
                panenViewModel.countTPHESPB(1, 0, true, 0, AppUtils.currentDate)
            }

        }

        setupButtonGenerateQR()
        setupTransferBT()

        if (featureName == "Buat eSPB") {
            btnAddMoreTph = FloatingActionButton(this)
            btnAddMoreTph.id = View.generateViewId()
            btnAddMoreTph.setImageResource(R.drawable.baseline_add_24) // Make sure you have this resource, or use baseline_add_24
            btnAddMoreTph.contentDescription = "Add More TPH"

            // Set button background color to green
            btnAddMoreTph.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_dark
                )
            )

            // Set icon color to white
            btnAddMoreTph.imageTintList = ColorStateList.valueOf(Color.WHITE)

            // Add the button to the layout
            val rootLayout =
                findViewById<ConstraintLayout>(R.id.clParentListPanen) // Assuming your root layout is a ConstraintLayout
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            try {
                params.bottomToTop = R.id.btnGenerateQRTPH
            } catch (e: Exception) {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            // Convert dp to pixels for proper margin setting
            val scale = resources.displayMetrics.density
            val marginInPixels = (30 * scale + 0.5f).toInt()
            params.setMargins(
                0,
                0,
                marginInPixels,
                marginInPixels
            ) // Set right and bottom margins to 30dp
            rootLayout.addView(btnAddMoreTph, params)

            btnAddMoreTph.setOnClickListener {
                getAllDataFromList(false)
                val intent = Intent(this, ScanQR::class.java)
                intent.putExtra("tph_1", tph1)
                intent.putExtra("tph_0", tph0)
                intent.putExtra("tph_1_id_panen", tph1IdPanen)
                intent.putExtra("FEATURE_NAME", featureName)
                Log.d("ListPanenTBSActivityPassData", "List tph1: $tph1")
                Log.d("ListPanenTBSActivityPassData", "List tph0: $tph0")
                Log.d("ListPanenTBSActivityPassData", "List tph1IdPanen: $tph1IdPanen")
                Log.d("ListPanenTBSActivityPassData", "List FEATURE_NAME: $featureName")
                startActivity(intent)
                finishAffinity()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(
                    Intent(
                        this@ListPanenTBSActivity,
                        HomePageActivity::class.java
                    )
                )
                finishAffinity()
            }
        })
    }

    private fun restorePreviousTphData() {
        val previousTph1 = intent.getStringExtra("previous_tph_1") ?: ""
        val previousTph0 = intent.getStringExtra("previous_tph_0") ?: ""
        val previousTph1IdPanen = intent.getStringExtra("previous_tph_1_id_panen") ?: ""

        if (previousTph1.isNotEmpty()) {
            tph1 = previousTph1
        }
        if (previousTph0.isNotEmpty()) {
            tph0 = previousTph0
        }
        if (previousTph1IdPanen.isNotEmpty()) {
            tph1IdPanen = previousTph1IdPanen
        }
    }

    private fun extractPreviousSelections() {
        try {
            previouslySelectedTphIds.clear()

            val previousTph1IdPanen = intent.getStringExtra("previous_tph_1_id_panen") ?: ""


            AppLogger.d(previousTph1IdPanen)
            if (previousTph1IdPanen.isNotEmpty()) {
                val panenIds = previousTph1IdPanen.split(",").mapNotNull { id ->
                    id.trim().takeIf { it.isNotEmpty() }
                }.toSet()

                previouslySelectedTphIds.addAll(panenIds)
            }
        } catch (e: Exception) {
            AppLogger.e("Error extracting previous selections: ${e.message}")
            previouslySelectedTphIds.clear()
        }
    }

    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {

            listAdapter.updateData(emptyList())
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen || featureName == AppUtils.ListFeatureNames.DetailESPB) {
                val standardHeaders = listOf("BLOK", "NO TPH", "KIRIM PABRIK", "JAM")
                updateTableHeaders(standardHeaders)
            } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                // Add header for Mutu Buah tersimpan
                val mutuBuahHeaders = listOf("BLOK", "NO TPH", "JJG PANEN", "JAM")
                updateTableHeaders(mutuBuahHeaders)
            }

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.GONE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.GONE

            if (featureName != AppUtils.ListFeatureNames.DetailESPB) {
                speedDial.visibility =
                    if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
            }

            // Check if filterAllData is che    cked
            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            if (featureName == "Buat eSPB") {
                flCheckBoxTableHeaderLayout.visibility = View.GONE
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {
                loadingDialog.setMessage("Loading data tph...")
                val headerCheckBoxPanen = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBoxPanen.visibility = View.GONE
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHNonESPB(0, 0, true, 1)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1)
                    panenViewModel.countTPHESPB(0, 1, true, 1)
                    panenViewModel.countHasBeenESPB(0, 0, true, 1)
                } else {
                    panenViewModel.loadTPHNonESPB(0, 0, true, 1, globalFormattedDate)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1, globalFormattedDate)
                    panenViewModel.countTPHESPB(0, 1, true, 1, globalFormattedDate)
                    panenViewModel.countHasBeenESPB(0, 1, false, 1, globalFormattedDate)
                }
            } else if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                loadingDialog.setMessage("Loading data per transaksi", true)
                panenViewModel.getAllPanenWhereESPB(noespb)
            } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                // Add specific handling for Mutu Buah tersimpan
                loadingDialog.setMessage("Loading data tersimpan mutu buah", true)
                val isAllDataFiltered = filterAllData.isChecked
                if (isAllDataFiltered) {
                    // Load unuploaded mutu buah data (status = 0 means not uploaded/tersimpan)
                    mutuBuahViewModel.loadMBUnuploaded(0)
                    mutuBuahViewModel.countMBUnuploaded()
                    mutuBuahViewModel.countMBUploaded()
                } else {
                    // Load unuploaded mutu buah data for specific date
                    mutuBuahViewModel.loadMBUnuploaded(0, globalFormattedDate)
                    mutuBuahViewModel.countMBUnuploaded(globalFormattedDate)
                    mutuBuahViewModel.countMBUploaded(globalFormattedDate)
                }
            } else {
                counterPerPemanen.visibility = View.GONE
                loadingDialog.setMessage("Loading data tersimpan", true)
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHNonESPB(0, 0, true, 0)
                    panenViewModel.countTPHNonESPB(0, 0, true, 0)
                    panenViewModel.countTPHESPB(1, 0, true, 0)
                } else {
                    panenViewModel.loadTPHNonESPB(0, 0, true, 0, globalFormattedDate)
                    panenViewModel.countTPHNonESPB(0, 0, true, 0, globalFormattedDate)
                    panenViewModel.countTPHESPB(1, 0, true, 0, globalFormattedDate)
                }
            }
        }

        cardTerscan.setOnClickListener {
            listAdapter.updateData(emptyList())
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()

            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                val standardHeaders = listOf("BLOK", "NO TPH", "JJG PANEN", "JAM")
                updateTableHeaders(standardHeaders)
            } else if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                //untuk rekap per blok
                val rekapHeaders =
                    listOf(
                        "NAMA\nBLOK",
                        "JUMLAH\nTRANSAKSI",
                        "TOTAL JJG\nKIRIM PABRIK"
                    )
                updateTableHeaders(rekapHeaders)
            } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                // Add header for Mutu Buah sudah upload
                val mutuBuahHeaders = listOf("BLOK", "NO TPH", "JJG PANEN", "JAM")
                updateTableHeaders(mutuBuahHeaders)
            }

            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (featureName != AppUtils.ListFeatureNames.DetailESPB) {
                speedDial.visibility = View.GONE
            }

            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.GONE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.GONE

            loadingDialog.setMessage("Loading data terscan", true)

            if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                loadingDialog.setMessage("Loading TPH")
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHESPB(0, 1, true, 1)
                    panenViewModel.countTPHESPB(0, 1, true, 1)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1)
                    panenViewModel.countHasBeenESPB(0, 0, false, 1)
                } else {
                    panenViewModel.loadTPHESPB(0, 1, true, 1, globalFormattedDate)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1, globalFormattedDate)
                    panenViewModel.countTPHESPB(0, 1, true, 1, globalFormattedDate)
                    panenViewModel.countHasBeenESPB(0, 0, false, 1, globalFormattedDate)
                }
            } else if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                loadingDialog.setMessage("Loading data per blok", true)
                panenViewModel.getAllPanenWhereESPB(noespb)
            } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                loadingDialog.setMessage("Loading data sudah upload mutu buah", true)
                val isAllDataFiltered = filterAllData.isChecked
                if (isAllDataFiltered) {
                    mutuBuahViewModel.loadMBUnuploaded(3)
                    mutuBuahViewModel.countMBUnuploaded()
                    mutuBuahViewModel.countMBUploaded()
                } else {
                    mutuBuahViewModel.loadMBUnuploaded(3, globalFormattedDate)
                    mutuBuahViewModel.countMBUnuploaded(globalFormattedDate)
                    mutuBuahViewModel.countMBUploaded(globalFormattedDate)
                }
            } else {
                loadingDialog.setMessage("Loading data terscan", true)
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHESPB(1, 0, true, 0)
                    panenViewModel.countTPHESPB(1, 0, true, 0)
                    panenViewModel.countTPHNonESPB(0, 0, true, 0)
                } else {
                    panenViewModel.loadTPHESPB(1, 0, true, 0, globalFormattedDate)
                    panenViewModel.countTPHESPB(1, 0, true, 0, globalFormattedDate)
                    panenViewModel.countTPHNonESPB(0, 0, true, 0, globalFormattedDate)
                }
            }
        }

        cardRekapPerPemanen.setOnClickListener {
            listAdapter.updateData(emptyList())
            currentState = 2
            setActiveCard(cardRekapPerPemanen)
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                counterPerPemanen.visibility = View.GONE
                val rekapHeaders =
                    listOf(
                        "NAMA\nPEMANEN",
                        "BLOK/JJG",
                        "JUMLAH\nTRANSAKSI",
                        "KIRIM PABRIK/\nJJG DIBAYAR"
                    )
                updateTableHeaders(rekapHeaders)
            }
            loadingDialog.show()

            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.GONE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.GONE

            if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                loadingDialog.setMessage("Loading TPH menjadi E-SPB...")

                if (isAllDataFiltered) {
                    panenViewModel.loadTPHESPB(0, 0, false, 1)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1)
                    panenViewModel.countTPHESPB(0, 1, true, 1)
                    panenViewModel.countHasBeenESPB(0, 0, false, 1)
                } else {
                    panenViewModel.loadTPHESPB(0, 0, false, 1, globalFormattedDate)
                    panenViewModel.countTPHNonESPB(0, 0, true, 1, globalFormattedDate)
                    panenViewModel.countTPHESPB(0, 1, true, 1, globalFormattedDate)
                    panenViewModel.countHasBeenESPB(0, 0, false, 1, globalFormattedDate)
                }
            } else {
                loadingDialog.setMessage("Loading Rekap Per Pemanen", true)
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHNonESPB(1, 0, true, 0)
                    panenViewModel.loadPanenCountArchive()
                    panenViewModel.countTPHESPB(1, 0, true, 0)
                } else {
                    panenViewModel.loadTPHNonESPB(1, 0, true, 0, globalFormattedDate)
                    panenViewModel.loadPanenCountArchive()
                    panenViewModel.countTPHESPB(1, 0, true, 0, globalFormattedDate)
                }
            }
        }

        cardRekapPerBlok.setOnClickListener {
            listAdapter.updateData(emptyList())
            currentState = 3
            setActiveCard(cardRekapPerBlok)
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                val rekapHeaders =
                    listOf("NAMA\nBLOK", "JUMLAH\nTRANSAKSI", "KIRIM\nPABRIK", "TOTAL\nDIBAYAR")
                updateTableHeaders(rekapHeaders)
            }
            loadingDialog.show()

            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.GONE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.GONE

            loadingDialog.setMessage("Loading Rekap Per Blok", true)
            if (isAllDataFiltered) {
                panenViewModel.loadTPHNonESPB(1, 0, true, 0)
                panenViewModel.loadPanenCountArchive()
                panenViewModel.countTPHESPB(1, 0, true, 0)
                panenViewModel.countTPHNonESPB(0, 0, true, 0)
            } else {
                panenViewModel.loadTPHNonESPB(1, 0, true, 0, dateToUse)
                panenViewModel.loadPanenCountArchive()
                panenViewModel.countTPHESPB(1, 0, true, 0, dateToUse)
                panenViewModel.countTPHNonESPB(0, 0, true, 0, dateToUse)
            }
        }
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.card_item_tersimpan)
        cardTerscan = findViewById(R.id.card_item_terscan)
        cardRekapPerPemanen = findViewById(R.id.card_rekap_per_pemanen)
        cardRekapPerBlok = findViewById(R.id.card_rekap_per_blok)
        cardRekapPerPemanen.visibility =
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) View.VISIBLE else View.GONE
        cardRekapPerBlok.visibility =
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) View.VISIBLE else View.GONE
        counterTersimpan = findViewById(R.id.counter_item_tersimpan)
        counterTerscan = findViewById(R.id.counter_item_terscan)
        counterPerPemanen = findViewById(R.id.counter_item_perpemanen)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.rvTableData) // Initialize RecyclerView
    }

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardRekapPerPemanen.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardRekapPerBlok.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }


    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }

            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")
                        val nomorPemanen = data["nomor_pemanen"]?.toString()
                            ?: throw IllegalArgumentException("Missing nomor_pemanen.")
                        val asistensi = data["asistensi"]?.toString()
                            ?: throw IllegalArgumentException("Missing asistensi.")
                        val asistensiDivisi = data["asistensi_divisi"]?.toString() ?: "0"
                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val key = "KP"

                        val toValue = if (jjgJson.has(key)) {
                            jjgJson.getInt(key)
                        } else {
                            throw IllegalArgumentException("Missing '$key' key in jjg_json: $jjgJsonString")
                        }

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-03-28
                        val time = dateParts[1]  // 13:15:18

                        // Use dateIndexMap.size as the index for new dates
                        // Format: tphId,dateIndex,time,toValue,nomorPemanen,asistensi,asistensiDivisi;
                        append("$tphId,${dateIndexMap.getOrPut(date) { dateIndexMap.size }},${time},$toValue,$nomorPemanen,$asistensi,$asistensiDivisi;")
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

            return JSONObject().apply {
                put("tph_0", formattedData)
                put("username", username)
                put("tgl", tglJson)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }


    fun convertToFormattedString(input: String, int: Int = 1): String {
        try {
            Log.d("DEBUG", "Input: $input")

            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            if (content.isEmpty()) {
                return ""
            }

            // Split the content into individual objects
            val objects = splitObjects(content)

            // Process each object and join with semicolons
            val results = objects.map { obj ->
                parseComplexObject(obj.trim(), int)
            }.filter { it.isNotEmpty() }

            val result = results.joinToString(";")
            Log.d("DEBUG", "Result: $result")
            return result

        } catch (e: Exception) {
            Log.e("DEBUG", "Error in convertToFormattedString: ${e.message}")
            e.printStackTrace()
            return ""
        }
    }

    fun splitObjects(content: String): List<String> {
        val objects = mutableListOf<String>()
        var braceCount = 0
        var bracketCount = 0
        var start = 0

        for (i in content.indices) {
            when (content[i]) {
                '{' -> braceCount++
                '}' -> braceCount--
                '[' -> bracketCount++
                ']' -> bracketCount--
                ',' -> {
                    // If we're at the top level (no open braces/brackets), this is a separator
                    if (braceCount == 0 && bracketCount == 0) {
                        // Check if this comma is between objects by looking ahead
                        val remaining = content.substring(i + 1).trim()
                        if (remaining.startsWith("{id=")) {
                            objects.add(content.substring(start, i).trim())
                            start = i + 1
                        }
                    }
                }
            }
        }

        // Add the last object
        if (start < content.length) {
            objects.add(content.substring(start).trim())
        }

        return objects.filter { it.isNotEmpty() }
    }

    fun parseComplexObject(objStr: String, int: Int): String {
        try {
            // Extract the key fields we need using regex patterns
            val tphId = extractValue(objStr, "tph_id")
            val dateCreated = extractValue(objStr, "date_created")
            val jjgJson = extractJjgJson(objStr)

            Log.d("DEBUG", "tph_id: $tphId")
            Log.d("DEBUG", "date_created: $dateCreated")
            Log.d("DEBUG", "jjg_json: $jjgJson")

            if (tphId.isEmpty() || dateCreated.isEmpty()) {
                return ""
            }

            return "$tphId,$dateCreated,$jjgJson,$int"

        } catch (e: Exception) {
            Log.e("DEBUG", "Error parsing object: ${e.message}")
            return ""
        }
    }

    fun extractValue(input: String, key: String): String {
        // Look for pattern: key=value, where value stops at the next ", key=" or end
        val pattern = "$key=([^,]*?)(?=,\\s*\\w+=|$)".toRegex()
        val match = pattern.find(input)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun extractJjgJson(input: String): String {
        // Extract the KP value from jjg_json field
        val pattern = "jjg_json=\\{\"KP\":\\s*(\\d+)\\}".toRegex()
        val match = pattern.find(input)
        return match?.groupValues?.get(1) ?: ""
    }

    fun convertToFormattedString(input: String, tphFilter: String): String {
        try {
            // Parse TPH filter string into a list of IDs
            val tphIds = tphFilter
                .trim()
                .removeSurrounding("{", "}")
                .substringAfter("\"tph\":\"")  // Get content after "tph":"
                .substringBefore("\"")         // Get content before the closing quote
                .split(";")
                .map { it.trim() }
                .toSet()

            Log.d("ListPanenTBSActivityESPB", "tphIds: $tphIds")

            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects
                .filter { objStr ->
                    // Extract tph_id from each object and check if it's in our filter list
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }
                    tphIds.contains(map["tph_id"])
                }
                .joinToString(";") { objStr ->
                    // Clean up the object string
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")

                    // Split into key-value pairs
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }

                    // Extract jjg_json value
                    val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                    // Construct the formatted string
                    "${map["tph_id"]},${map["date_created"]},${jjgJson},1"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    data class Entry(
        val id: String,
        val timestamp: String,
        val value: Int,
        val type: Int
    ) {
        override fun toString(): String = "$id,$timestamp,$value,$type"
    }

    fun String.toEntries(): Set<Entry> {
        if (this.isEmpty()) return emptySet()
        return split(";").map { entry ->
            val parts = entry.split(",")
            Entry(
                id = parts[0],
                timestamp = parts[1],
                value = parts[2].toInt(),
                type = parts[3].toInt()
            )
        }.toSet()
    }

    fun Set<Entry>.toString(): String {
        return if (isEmpty()) "" else joinToString(";")
    }

    private fun getAllDataFromList(playSound: Boolean = true) {
        //get manually selected items
        val selectedItems = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems: $selectedItems")
        val tph1AD0 =
            convertToFormattedString(selectedItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD0")
        val tph1AD2 =
            convertToFormattedString(selectedItems.toString(), 1).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD2")

        //get automatically selected items
        val selectedItems2 = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems2:$selectedItems2")

        // Extract the id values from the matches and join them with commas
        val newTph1IdPanen = try {
            val pattern = Regex("\\{id=(\\d+),")
            val matches = pattern.findAll(selectedItems2.toString())
            matches.map { it.groupValues[1] }.joinToString(", ")

        } catch (e: Exception) {
            Toasty.error(this, "Error parsing panen IDs: ${e.message}", Toast.LENGTH_LONG)
                .show()
            ""
        }

        // Combine with existing tph1IdPanen if it exists
        tph1IdPanen = if (tph1IdPanen.isEmpty()) {
            newTph1IdPanen
        } else {
            "$tph1IdPanen, $newTph1IdPanen"
        }
        Log.d("ListPanenTBSActivityESPB", "tph1IdPanen:$tph1IdPanen")


        //get automatically selected items
        val preSelectedItems = listAdapter.getPreSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "preSelectedItems:$preSelectedItems")

        // Extract the id values from the matches and join them with commas
        val newTph1NoIdPanen = try {
            val pattern = Regex("\\{id=(\\d+),")
            val matches = pattern.findAll(preSelectedItems.toString())
            matches.map { it.groupValues[1] }.joinToString(", ")

        } catch (e: Exception) {
            Toasty.error(this, "Error parsing panen IDs: ${e.message}", Toast.LENGTH_LONG)
                .show()
            ""
        }

        // Combine with existing tph1IdPanen if it exists
        tph1NoIdPanen = if (tph1NoIdPanen.isEmpty()) {
            newTph1NoIdPanen
        } else {
            "$tph1NoIdPanen, $newTph1NoIdPanen"
        }
        Log.d("ListPanenTBSActivityESPB", "tph1NoIdPanen:$tph1NoIdPanen")


        if (playSound && !shouldRestoreCheckboxState) {
            playSound(R.raw.berhasil_scan)
        }
        val allItems = listAdapter.getCurrentData()
        Log.d("ListPanenTBSActivityESPB", "listTPHDriver: $listTPHDriver")

        // Parse listTPHDriver to get the actual integer value
        val tphDriverValue = try {
            if (listTPHDriver.contains("null")) {
                0 // Default value when tph is null
            } else {
                // Extract number from JSON-like string, or parse directly if it's already a number
                val regex = Regex(""""tph":"?(\d+)"?""")
                val match = regex.find(listTPHDriver.toString())
                match?.groupValues?.get(1)?.toInt() ?: listTPHDriver.toString().toInt()
            }
        } catch (e: Exception) {
            Log.e("ListPanenTBSActivityESPB", "Error parsing listTPHDriver: ${e.message}")
            0 // Default value
        }

        val tph1NO = convertToFormattedString(
            selectedItems2.toString(),
            tphDriverValue
        ).replace("{\"KP\": ", "").replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsNO: $tph1NO")

        //get item which is not selected
        val tph0before =
            convertToFormattedString(allItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItems0: $tph0before")

        val set1 = tph1AD0.toEntries()
        val set2 = tph1AD2.toEntries()
        val set3 = tph1NO.toEntries()
        val set4 = tph0before.toEntries()

        // Calculate string5 = string4 - string1 - string3
        val newTph0 = (set4 - set1 - set3).toString().replace("[", "").replace("]", "")
            .replace(", ", ";")
        Log.d("ListPanenTBSActivityESPB", "New tph0: $newTph0")

        // Extract nomor_pemanen values and create mapping
        val tphToNomorPemanen = mutableMapOf<String, String>()

        // Parse selectedItems2 to create mapping of TPH entries to nomor_pemanen
        selectedItems2.forEach { item ->
            try {
                val itemMap = item as Map<String, Any>
                val tphId = itemMap["tph_id"]?.toString()
                val dateCreated = itemMap["date_created"]?.toString()
                val nomorPemanen = itemMap["nomor_pemanen"]?.toString()

                if (tphId != null && dateCreated != null && nomorPemanen != null) {
                    val key = "$tphId,$dateCreated" // Use tph_id + date as unique key
                    tphToNomorPemanen[key] = nomorPemanen
                    Log.d("ListPanenTBSActivityESPB", "Mapping: $key -> $nomorPemanen")
                }
            } catch (e: Exception) {
                Log.e("ListPanenTBSActivityESPB", "Error mapping nomor_pemanen: ${e.message}")
            }
        }

        // Calculate string6 = string2 + string3 and add nomor_pemanen
        val combinedEntries = (set2 + set3).map { entry ->
            val parts = entry.toString().split(",")
            if (parts.size >= 2) {
                val tphId = parts[0]
                val dateCreated = parts[1]
                val key = "$tphId,$dateCreated"

                if (tphToNomorPemanen.containsKey(key)) {
                    val result = "$entry,${tphToNomorPemanen[key]}"
                    Log.d("ListPanenTBSActivityESPB", "Adding nomor_pemanen to: $entry -> $result")
                    result
                } else {
                    Log.d("ListPanenTBSActivityESPB", "No nomor_pemanen found for: $entry")
                    entry.toString() // Keep original if no nomor_pemanen found
                }
            } else {
                entry.toString()
            }
        }

        var newTph1 = combinedEntries.joinToString(";")
        Log.d("ListPanenTBSActivityESPB", "New tph1 with nomor_pemanen: $newTph1")

        // Combine with existing data if it exists
        if (tph0.isNotEmpty() && newTph0.isNotEmpty()) {
            tph0 = "$tph0;$newTph0"
        } else if (newTph0.isNotEmpty()) {
            tph0 = newTph0
        }

        if (tph1.isNotEmpty() && newTph1.isNotEmpty()) {
            tph1 = "$tph1;$newTph1"
        } else if (newTph1.isNotEmpty()) {
            tph1 = newTph1
        }

        // Remove any duplicate entries from tph0 and tph1
        tph0 = removeDuplicateEntries(tph0)
        tph1 = removeDuplicateEntries(tph1)

        Log.d("ListPanenTBSActivityESPB", "Final tph0: $tph0")
        Log.d("ListPanenTBSActivityESPB", "Final tph1: $tph1")
        Log.d("ListPanenTBSActivityESPB", "Final tph1IdPanen: $tph1IdPanen")

    }

    private fun setupTransferBT(){
        val btnTransferBT = findViewById<FloatingActionButton>(R.id.btnTransferBT)
        btnTransferBT.setOnClickListener {
            checkBluetoothAndShowDialog()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetoothAndShowDialog() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        when {
            bluetoothAdapter == null -> {
                // Device doesn't support Bluetooth
                Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT)
                    .show()
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
                            this@ListPanenTBSActivity,
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
                    encodeJsonToBase64ZipQR(jsonData)
                        ?: throw Exception("Encoding failed - data too large or invalid")
                } catch (e: Exception) {
                    AppLogger.e("Error encoding data for Bluetooth: ${e.message}")
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@ListPanenTBSActivity,
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
                        this@ListPanenTBSActivity,
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

        val titleDialogBluetooth = dialogView.findViewById<TextView>(R.id.titleDialogBluetooth)
        titleDialogBluetooth.text = "Scan Perangkat $featureName"

        // UPDATE THE tvDataInfo WITH THE CALCULATED DATA
        tvDataInfo.text = bluetoothDataInfo
        AppLogger.d("UI UPDATE: Setting tvDataInfo.text = $bluetoothDataInfo")

        val adapter = ArrayAdapter(this, R.layout.bluetooth_device_item, deviceNames)
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
                        tvStatus.text =
                            "Selesai - 0 perangkat ditemukan\n(Pastikan perangkat sudah tersambung melalui Bluetooth)"
                    } else {
                        tvStatus.text = "Selesai - ${devices.size} perangkat ditemukan"
                    }
                    btnScanStop.text = "Scan"
                    btnScanStop.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.bluedarklight
                        )
                    )
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
                    val deviceInfo = formatDeviceInfo(deviceName, deviceAddress)
                    devices.add(device)
                    deviceNames.add(deviceInfo)
                    adapter.notifyDataSetChanged()
                    tvStatus.text = "Ditemukan ${devices.size} perangkat"
                } else {
                    // Update existing device info if we got a better name
                    val currentName = deviceNames[existingIndex]
                    if (currentName.contains("Unknown") && !deviceName.contains("Unknown")) {
                        val updatedInfo = formatDeviceInfo(deviceName, deviceAddress)
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
                this@ListPanenTBSActivity,
                "Kirim Data",
                "Konfirmasi Transfer Hektaran Panen",
                "Kirim data ke:\n$deviceInfo\n\n$bluetoothDataInfo",
                "warning.json",
                ContextCompat.getColor(this@ListPanenTBSActivity, R.color.bluedarklight),
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

    private fun getDeviceName(device: BluetoothDevice): String {
        return AppUtils.getDeviceName(device, this)
    }

    private fun formatDeviceInfo(name: String, address: String): String {
        return AppUtils.formatDeviceInfo(name, address)
    }

    private fun scanPairedPhoneDevices(
        devices: MutableList<BluetoothDevice>,
        deviceNames: MutableList<String>,
        deviceTypes: MutableMap<String, String>,
        adapter: ArrayAdapter<String>
    ) {
        AppUtils.scanPairedPhoneDevices(this, devices, deviceNames, deviceTypes, adapter)
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothTransfer(targetDevice: BluetoothDevice) {
        loadingDialog.show()
        loadingDialog.setMessage(
            "Mengirim data ke ${targetDevice.name ?: "Perangkat Tidak Dikenal"}...",
            true
        )

        Thread {
            var bluetoothSocket: BluetoothSocket? = null
            var feedbackReceived = false // Add this flag

            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                // Cancel any ongoing discovery first
                bluetoothAdapter?.cancelDiscovery()

                runOnUiThread {
                    loadingDialog.setMessage(
                        "Membuat koneksi ke ${targetDevice.name ?: "Perangkat"}...",
                        true
                    )
                }

                // Try multiple connection methods
                bluetoothSocket = try {
                    targetDevice.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    // Fallback method for some devices
                    AppLogger.d("Primary connection failed, trying fallback method")
                    val method = targetDevice.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
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
                    val currentChunkSize =
                        if (remainingBytes < chunkSize) remainingBytes else chunkSize

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
                Thread.sleep(1000) // Give receiver time to process

                AppLogger.d("Data sent successfully, now waiting for feedback...")

                // UPDATE: Listen for feedback from receiver
                runOnUiThread {
                    loadingDialog.setMessage(
                        "Menunggu response dari ${targetDevice.name ?: "perangkat"}...",
                        true
                    )
                }

                // Listen for feedback
                val inputStream = bluetoothSocket.inputStream
                val feedbackBuffer = ByteArray(2048)
                val feedbackBuilder = StringBuilder()
                var feedbackAttempts = 0
                val maxFeedbackAttempts = 30 // 30 seconds timeout

                while (!feedbackReceived && feedbackAttempts < maxFeedbackAttempts) {
                    try {
                        if (inputStream.available() > 0) {
                            val bytes = inputStream.read(feedbackBuffer)
                            if (bytes > 0) {
                                val receivedFeedback =
                                    String(feedbackBuffer, 0, bytes, Charsets.UTF_8)
                                feedbackBuilder.append(receivedFeedback)

                                val feedbackData = feedbackBuilder.toString()
                                AppLogger.d("Received feedback chunk: $receivedFeedback")
                                AppLogger.d("Total feedback so far: $feedbackData")

                                // Check for feedback markers
                                if (feedbackData.contains("FEEDBACK_START") && feedbackData.contains(
                                        "FEEDBACK_END"
                                    )
                                ) {
                                    val startIndex =
                                        feedbackData.indexOf("FEEDBACK_START") + "FEEDBACK_START".length
                                    val endIndex = feedbackData.indexOf("FEEDBACK_END")

                                    if (startIndex > 0 && endIndex > startIndex) {
                                        val feedbackJson =
                                            feedbackData.substring(startIndex, endIndex).trim()

                                        AppLogger.d("Complete feedback JSON received: $feedbackJson")

                                        feedbackReceived = true // Set flag to true

                                        runOnUiThread {
                                            loadingDialog.dismiss()
                                            processFeedbackFromReceiver(
                                                feedbackJson,
                                                targetDevice.name
                                            )
                                        }

                                        break
                                    }
                                }
                            }
                        } else {
                            Thread.sleep(1000) // Wait 1 second before checking again
                            feedbackAttempts++

                            // Update waiting message with countdown
                            val remainingTime = maxFeedbackAttempts - feedbackAttempts
                            runOnUiThread {
                                loadingDialog.setMessage(
                                    "Menunggu response dari ${targetDevice.name ?: "perangkat"}... ($remainingTime detik)",
                                    true
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error listening for feedback: ${e.message}")
                        break
                    }
                }

                // Handle timeout case - only if feedback was not received
                if (!feedbackReceived) {
                    runOnUiThread {
                        loadingDialog.dismiss()

                        AlertDialogUtility.withSingleAction(
                            this@ListPanenTBSActivity,
                            "Kembali",
                            "Transfer Timeout",
                            "Data berhasil dikirim ke ${targetDevice.name ?: "perangkat"}, tapi tidak menerima konfirmasi penyimpanan dalam 30 detik.\n\nSize: ${dataBytes.size} bytes",
                            "warning.json",
                            R.color.orange
                        ) {
                            // Action after timeout - refresh the data
                            AppLogger.d("Transfer completed with timeout - refreshing data")

                            panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, 0)
                            panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0,prefManager!!.afdelingIdUserLogin!!.toInt())
                            panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1,prefManager!!.afdelingIdUserLogin!!.toInt())
                        }
                    }

                    AppLogger.w("Feedback timeout - no response received within 30 seconds")
                }

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
                        this@ListPanenTBSActivity,
                        "Coba Lagi",
                        "Transfer Gagal",
                        errorMessage,
                        "warning.json",
                        R.color.colorRedDark,
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

    private suspend fun verifyAndUpdateAllTransferredData(
        savedData: List<Map<String, Any>>?,
        duplicateData: List<Map<String, Any>>?
    ): VerificationResult = withContext(Dispatchers.IO) {

        // Combine both saved and duplicate data
        val allTransferredData = mutableListOf<Map<String, Any>>()
        savedData?.let { allTransferredData.addAll(it) }
        duplicateData?.let { allTransferredData.addAll(it) }

        if (allTransferredData.isEmpty()) {
            AppLogger.d("No transferred data to verify")
            return@withContext VerificationResult(0, 0, listOf("No transferred data to verify"))
        }

        var verifiedCount = 0
        var notFoundCount = 0
        val errorMessages = mutableListOf<String>()
        val idsToUpdate = mutableListOf<Int>()

        AppLogger.d("Starting verification of ${allTransferredData.size} TOTAL transferred records (saved + duplicates)")

        allTransferredData.forEach { transferredRecord ->
            try {
                val transferredTphId = transferredRecord["tph_id"] as? String
                val transferredDateCreated = transferredRecord["date_created"] as? String
                val transferredJjgJson = transferredRecord["jjg_json"] as? String
                val transferredKaryawanNik = transferredRecord["karyawan_nik"] as? String

                AppLogger.d("Verifying transferred record: tph_id=$transferredTphId, date_created=$transferredDateCreated")

                if (transferredTphId.isNullOrEmpty() || transferredDateCreated.isNullOrEmpty() ||
                    transferredJjgJson.isNullOrEmpty() || transferredKaryawanNik.isNullOrEmpty()) {
                    errorMessages.add("Invalid transferred record: missing required fields")
                    return@forEach
                }

                val matchingLocalRecord = originalMappedData.find { localRecord ->
                    val localTphId = localRecord["tph_id"] as? String
                    val localDateCreated = localRecord["date_created"] as? String
                    val localJjgJson = localRecord["jjg_json"] as? String
                    val localKaryawanNik = localRecord["karyawan_nik"] as? String

                    // Basic field matches
                    val tphMatches = localTphId == transferredTphId
                    val dateMatches = localDateCreated == transferredDateCreated
                    val nikMatches = localKaryawanNik == transferredKaryawanNik

                    // Special JSON matching - check if all transferred JSON keys exist in local JSON with same values
                    val jjgMatches = try {
                        if (transferredJjgJson != null && localJjgJson != null) {
                            val transferredJsonObj = JSONObject(transferredJjgJson)
                            val localJsonObj = JSONObject(localJjgJson)

                            // Check if all keys from transferred data exist in local data with same values
                            var allKeysMatch = true
                            val transferredKeys = transferredJsonObj.keys()

                            while (transferredKeys.hasNext()) {
                                val key = transferredKeys.next()
                                val transferredValue = transferredJsonObj.get(key)
                                val localValue = localJsonObj.opt(key)

                                if (transferredValue != localValue) {
                                    allKeysMatch = false
                                    break
                                }
                            }

                            allKeysMatch
                        } else {
                            transferredJjgJson == localJjgJson
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error comparing JSON: ${e.message}")
                        false
                    }

                    val allMatch = tphMatches && dateMatches && jjgMatches && nikMatches

                    if (allMatch) {
                        AppLogger.d("✅ Perfect match found for transferred record: ID=${localRecord["id"]}")
                    }

                    allMatch
                }

                if (matchingLocalRecord != null) {
                    val recordId = matchingLocalRecord["id"] as? Int
                    if (recordId != null) {
                        // Check if this ID is already in the list to avoid duplicates
                        if (!idsToUpdate.contains(recordId)) {
                            idsToUpdate.add(recordId)
                            verifiedCount++
                            AppLogger.d("Found matching record - ID: $recordId, tph_id: $transferredTphId")
                        } else {
                            AppLogger.d("ID $recordId already in update list, skipping duplicate")
                        }
                    } else {
                        errorMessages.add("Found matching record but ID is null for tph_id: $transferredTphId")
                    }
                } else {
                    notFoundCount++
                    AppLogger.w("No matching local record found for transferred tph_id=$transferredTphId")
                }

            } catch (e: Exception) {
                errorMessages.add("Error processing record: ${e.message}")
                AppLogger.e("Error processing transferred record: ${e.message}")
            }
        }

        AppLogger.d("idsToUpdate $idsToUpdate")
        // Update all found IDs at once - for ALL transferred data (saved + duplicates)
        if (idsToUpdate.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                try {
                    when (featureName) {
                        AppUtils.ListFeatureNames.RekapHasilPanen,
                        AppUtils.ListFeatureNames.RekapPanenDanRestan -> {
                            panenViewModel.updateArchiveByFeature(featureName, idsToUpdate, 1)
                        }
                    }
                    AppLogger.d("Updated archive status for ${idsToUpdate.size} ALL transferred records: $idsToUpdate")
                } catch (e: Exception) {
                    AppLogger.e("Error updating archive status: ${e.message}")
                    errorMessages.add("Failed to update archive status: ${e.message}")
                }
            }
        }

        AppLogger.d("Verification completed: $verifiedCount verified, $notFoundCount not found (ALL transferred data)")
        VerificationResult(verifiedCount, notFoundCount, errorMessages)
    }

    private fun processFeedbackFromReceiver(feedbackJson: String, deviceName: String?) {
        try {
            AppLogger.d("Processing feedback JSON: $feedbackJson")

            val feedback = Gson().fromJson(feedbackJson, Map::class.java) as Map<String, Any>
            val status = feedback["status"] as? String
            val message = feedback["message"] as? String
            val savedCount = feedback["savedCount"] as? Double
            val duplicateCount = feedback["duplicateCount"] as? Double
            val error = feedback["error"] as? String
            val savedData = feedback["savedData"] as? List<Map<String, Any>>
            val duplicateData = feedback["duplicateData"] as? List<Map<String, Any>>

            AppLogger.d("Feedback parsed - Status: $status, Message: $message, SavedCount: $savedCount, DuplicateCount: $duplicateCount, Error: $error")
            AppLogger.d("SavedData count: ${savedData?.size ?: 0}, DuplicateData count: ${duplicateData?.size ?: 0}")

            when (status) {
                "success" -> {
                    // Show loading dialog for data verification
                    loadingDialog.show()
                    loadingDialog.setMessage("Sedang cek data...", true)

                    // Start data verification in background
                    lifecycleScope.launch {
                        try {
                            // Verify and update archive status for ALL transferred data (saved + duplicates)
                            val verificationResult =
                                verifyAndUpdateAllTransferredData(savedData, duplicateData)

                            // Hide loading dialog
                            loadingDialog.dismiss()

                            // Play success sound
                            playSound(R.raw.berhasil_simpan)

                            // Calculate total transferred
                            val totalTransferred =
                                (savedCount?.toInt() ?: 0) + (duplicateCount?.toInt() ?: 0)

                            val baseMessage = if (verificationResult.verifiedCount > 0) {
                                "Data berhasil dikirim dan disimpan di ${deviceName ?: "perangkat penerima"}!\n\n${verificationResult.verifiedCount} item diarsipkan"
                            } else {
                                "Data berhasil dikirim dan disimpan di ${deviceName ?: "perangkat penerima"}!"
                            }

                            // Enhanced message with transfer details
                            val successMessage = when {
                                savedCount != null && duplicateCount != null && duplicateCount > 0 -> {
                                    "$baseMessage\n\nDetail transfer:\n• ${savedCount.toInt()} data baru disimpan\n• ${duplicateCount.toInt()} data duplikat dilewati"
                                }

                                savedCount != null -> {
                                    "$baseMessage\n\n${savedCount.toInt()} data baru disimpan"
                                }

                                else -> baseMessage
                            }

                            // Determine color based on whether there were duplicates
                            val hasDuplicates = (duplicateCount?.toInt() ?: 0) > 0
                            val alertColor =
                                if (hasDuplicates) R.color.orange else R.color.greenDarker
                            val alertTitle =
                                if (hasDuplicates) "Transfer & Arsip Berhasil dengan Duplikat" else "Transfer & Arsip Data Berhasil"

                            AlertDialogUtility.withSingleAction(
                                this@ListPanenTBSActivity,
                                "Kembali",
                                alertTitle,
                                successMessage,
                                "success.json",
                                alertColor
                            ) {
                                AppLogger.d("Transfer completed successfully with feedback and verification")
//                                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, 0)
//                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0,prefManager!!.afdelingIdUserLogin.toInt())
//                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1,prefManager!!.afdelingIdUserLogin.toInt())
                            }

                            AppLogger.d("Transfer, save and verification completed successfully. Verified: ${verificationResult.verifiedCount}, Total transferred: $totalTransferred")

                        } catch (e: Exception) {
                            loadingDialog.dismiss()
                            AppLogger.e("Error during data verification: ${e.message}")

                            // Still show success but mention verification issue
                            AlertDialogUtility.withSingleAction(
                                this@ListPanenTBSActivity,
                                "Kembali",
                                "Transfer Berhasil",
                                "Data berhasil dikirim dan disimpan, namun terjadi error saat verifikasi lokal: ${e.message}",
                                "warning.json",
                                R.color.orange
                            ) { }
                        }
                    }
                }

                "error" -> {
                    // FIXED: Also handle ID verification and archive update for error case (all duplicates)
                    AppLogger.d("Error case - handling duplicate data verification and archive update")

                    // Show loading dialog for data verification
                    loadingDialog.show()
                    loadingDialog.setMessage("Sedang cek data duplikat...", true)

                    // Start data verification in background for duplicates
                    lifecycleScope.launch {
                        try {
                            // Extract duplicate data from error details and verify
                            val duplicateDataFromError = extractDataFromErrorDetails(error ?: "")
                            val verificationResult =
                                verifyAndUpdateAllTransferredData(null, duplicateDataFromError)

                            // Hide loading dialog
                            loadingDialog.dismiss()

                            val errorDetail = error ?: "Unknown error"
                            val processedErrorMessage =
                                processErrorDuplicateDetails(errorDetail, deviceName)

                            // Enhanced message with verification results
                            val finalMessage = if (verificationResult.verifiedCount > 0) {
                                "$processedErrorMessage\n\n${verificationResult.verifiedCount} item diarsipkan meskipun duplikat"
                            } else {
                                processedErrorMessage
                            }

                            AlertDialogUtility.withSingleAction(
                                this@ListPanenTBSActivity,
                                "Ok",
                                "Transfer Berhasil, Data Duplikat",
                                finalMessage,
                                "warning.json",
                                R.color.orange,
                            ) {
                                AppLogger.d("Transfer completed with duplicates, verification done")
//                                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, 0)
//                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0,prefManager!!.afdelingIdUserLogin.toInt())
//                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1,prefManager!!.afdelingIdUserLogin.toInt())
                            }

                            AppLogger.d("Transfer completed with all duplicates. Verified: ${verificationResult.verifiedCount}")

                        } catch (e: Exception) {
                            loadingDialog.dismiss()
                            AppLogger.e("Error during duplicate data verification: ${e.message}")

                            // Fallback to original error handling without verification
                            val errorDetail = error ?: "Unknown error"
                            val processedErrorMessage =
                                processErrorDuplicateDetails(errorDetail, deviceName)

                            AlertDialogUtility.withSingleAction(
                                this@ListPanenTBSActivity,
                                "Ok",
                                "Transfer Berhasil, Arsip Gagal",
                                processedErrorMessage,
                                "warning.json",
                                R.color.colorRedDark,
                            ) { }
                        }
                    }
                }

                else -> {
                    AlertDialogUtility.withSingleAction(
                        this@ListPanenTBSActivity,
                        "Ok",
                        "Transfer Berhasil, Penyimpanan Gagal",
                        "Menerima respons tidak dikenal dari ${deviceName ?: "perangkat"}: $feedbackJson",
                        "warning.json",
                        R.color.colorRedDark,
                    ) { }

                    AppLogger.w("Unknown feedback status: $status, Full feedback: $feedbackJson")
                }
            }

        } catch (e: Exception) {
            AppLogger.e("Error processing feedback: ${e.message}")
            AppLogger.e("Raw feedback was: $feedbackJson")

            AlertDialogUtility.withSingleAction(
                this@ListPanenTBSActivity,
                "Coba Lagi",
                "Transfer Berhasil, Penyimpanan Gagal",
                "Data berhasil dikirim tapi terjadi error saat arsip data: ${e.message}",
                "warning.json",
                R.color.colorRedDark,
            ) { }
        }
    }

    private fun processErrorDuplicateDetails(errorDetail: String, deviceName: String?): String {
        AppLogger.d("Processing error duplicate details: $errorDetail")

        // Check if this is a duplicate error
        if (!errorDetail.contains("duplikat", ignoreCase = true)) {
            // Not a duplicate error, return original message
            return "Data berhasil dikirim ke ${deviceName ?: "perangkat"} namun gagal diarsipkan:\n\n$errorDetail"
        }

        val duplicateInfoList = mutableListOf<String>()

        try {
            // Parse the error detail to extract TPH IDs and dates
            // Format: "TPH ID: 138470, Date: 2025-09-18 09:11:33"
            val lines = errorDetail.split("\n")

            lines.forEach { line ->
                if (line.contains("TPH ID:")) {
                    try {
                        // Extract TPH ID and Date from the line
                        val tphIdMatch = Regex("TPH ID: (\\d+)").find(line)
                        val dateMatch = Regex("Date: ([^\\n]+)").find(line)

                        val tphId = tphIdMatch?.groupValues?.get(1)
                        val dateCreated = dateMatch?.groupValues?.get(1)?.trim()

                        AppLogger.d("Extracted from error: tph_id=$tphId, date_created=$dateCreated")

                        if (!tphId.isNullOrEmpty() && !dateCreated.isNullOrEmpty()) {
                            // Find matching record in mappedData
                            val matchingLocalRecord = mappedData.find { localRecord ->
                                val localTphId = localRecord["tph_id"] as? String
                                val localDateCreated = localRecord["date_created"] as? String

                                AppLogger.d("Comparing error with local: $localTphId == $tphId && $localDateCreated == $dateCreated")

                                localTphId == tphId && localDateCreated == dateCreated
                            }

                            if (matchingLocalRecord != null) {
                                // Extract TPH details from the mapped data
                                val blokKode =
                                    matchingLocalRecord["blok_name"] as? String ?: "Unknown"
                                val tphNomor = matchingLocalRecord["nomor"] as? String ?: "Unknown"

                                AppLogger.d("Found matching error duplicate - Blok: $blokKode, Nomor: $tphNomor, Date: $dateCreated")

                                // Format: "Blok ABC, TPH 123, full date"
                                val duplicateInfo = "Blok $blokKode, TPH $tphNomor, $dateCreated"
                                duplicateInfoList.add(duplicateInfo)
                            } else {
                                AppLogger.w("No matching local record found for error tph_id=$tphId")
                                duplicateInfoList.add("TPH $tphId, $dateCreated")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error parsing duplicate line: $line, error: ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            AppLogger.e("Error processing error duplicate details: ${e.message}")
            return "Data berhasil dikirim ke ${deviceName ?: "perangkat"} namun gagal diarsipkan:\n\n$errorDetail"
        }

        // Build the final message
        val duplicateDetails = if (duplicateInfoList.isNotEmpty()) {
            duplicateInfoList.joinToString("\n")
        } else {
            "Data duplikat terdeteksi"
        }

        val finalMessage =
            "Data berhasil dikirim ke ${deviceName ?: "perangkat"} namun data duplikat:\n\n$duplicateDetails"
        AppLogger.d("Final error message with TPH details: $finalMessage")

        return finalMessage
    }

    private fun extractDataFromErrorDetails(errorDetail: String): List<Map<String, Any>>? {
        AppLogger.d("🔍 Extracting data from error details for verification: $errorDetail")

        val extractedData = mutableListOf<Map<String, Any>>()

        try {
            val lines = errorDetail.split("\n")

            lines.forEach { line ->
                if (line.contains("TPH ID:")) {
                    val tphIdMatch = Regex("TPH ID: (\\d+)").find(line)
                    val dateMatch = Regex("Date: ([^\\n]+)").find(line)

                    val tphId = tphIdMatch?.groupValues?.get(1)
                    val dateCreated = dateMatch?.groupValues?.get(1)?.trim()

                    AppLogger.d("🔍 Extracted from error line: tph_id=$tphId, date_created=$dateCreated")

                    if (!tphId.isNullOrEmpty() && !dateCreated.isNullOrEmpty()) {
                        // Find the full record in mappedData to get all required fields
                        val matchingRecord = mappedData.find { localRecord ->
                            val localTphId = localRecord["tph_id"] as? String
                            val localDateCreated = localRecord["date_created"] as? String
                            localTphId == tphId && localDateCreated == dateCreated
                        }

                        if (matchingRecord != null) {
                            // Create a map similar to what the receiver would send back with all required fields
                            val mockTransferredRecord = mapOf(
                                "tph_id" to tphId,
                                "date_created" to dateCreated,
                                "karyawan_nama" to (matchingRecord["karyawan_nama"] as? String
                                    ?: ""),
                                "karyawan_nik" to (matchingRecord["karyawan_nik"] as? String ?: "")
                            )
                            extractedData.add(mockTransferredRecord)
                            AppLogger.d("✅ Successfully extracted duplicate data for verification: tph_id=$tphId, date=$dateCreated")
                        } else {
                            AppLogger.w("⚠️ No matching local record found for error tph_id=$tphId, date=$dateCreated")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("❌ Error extracting data from error details: ${e.message}")
            return null
        }

        AppLogger.d("📊 Total extracted duplicate records for verification: ${extractedData.size}")
        return if (extractedData.isNotEmpty()) extractedData else null
    }

    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        val btnGenerateQRTPHUnl = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPHUnl)


        if (featureName == "Buat eSPB") {
            btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
            btnGenerateQRTPH.setOnClickListener {
                getAllDataFromList(false)
                if (tph1.isEmpty() && tph1IdPanen.isEmpty()) {
                    // No selected items, show error message
                    AlertDialogUtility.withSingleAction(
                        this@ListPanenTBSActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_have_check_data),
                        "${stringXML(R.string.al_must_have_check_data)}",
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                    }
                } else {

                    AppLogger.d("tph1 $tph1")
                    AlertDialogUtility.withTwoActions(
                        this,
                        "LANJUT",
                        "PERHATIAN!",
                        "Apakah anda ingin membuat eSPB dengan data ini?",
                        "warning.json", function = {
                            val intent = Intent(this, FormESPBActivity::class.java)
                            intent.putExtra("tph_1", tph1)
                            intent.putExtra("tph_normal", tph1NoIdPanen)
                            intent.putExtra("tph_0", tph0)
                            intent.putExtra("tph_1_id_panen", tph1IdPanen)
                            intent.putExtra("FEATURE_NAME", featureName)
                            startActivity(intent)
                            finishAffinity()
                        }
                    ) {
                    }
                }

            }
        } else {
            btnGenerateQRTPH.setOnClickListener {
                generateQRTPH(70)
            }
            btnGenerateQRTPHUnl.setOnClickListener {
                generateQRTPH(0)
            }
        }
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
                val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
                dialog.setContentView(view)
                val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
                val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                val tvTitleQRGenerate: TextView =
                    view.findViewById(R.id.textTitleQRGenerate)
                tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
                val capitalizedFeatureName =
                    featureName!!.split(" ").joinToString(" ") { word ->
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


//                        // Configure for better zooming
//                        qrCodeImageView.apply {
//                            // Set minimum scale lower to allow for initial smaller size
//                            minimumScale = 0.5f  // This allows scaling down to 50%
//                            maximumScale = 5.0f  // Maximum zoom
//                            mediumScale = 2.5f   // Medium zoom
//
//                             scale = 0.8f  // This will work since it's above minimumScale
//
//                            // Enable zooming
//                            isZoomable = true
//                        }
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

                if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan || featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                    btnConfirmScanPanenTPH.setOnClickListener {
                        AlertDialogUtility.withTwoActions(
                            this@ListPanenTBSActivity,
                            getString(R.string.al_yes),
                            getString(R.string.confirmation_dialog_title),
                            "${getString(R.string.al_make_sure_scanned_qr)}",
                            "warning.json",
                            ContextCompat.getColor(
                                this@ListPanenTBSActivity,
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
                                        val limitedData = mappedData.take(effectiveLimit)


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
                                                    if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                                                        panenViewModel.archivePanenById(id)
                                                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                                                        panenViewModel.changeStatusTransferRestan(
                                                            id
                                                        )
                                                    }

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
                                                            this@ListPanenTBSActivity,
                                                            "Gagal mengarsipkan data",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    hasError -> {
                                                        val errorDetail =
                                                            errorMessages.joinToString("\n")
                                                        AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                        Toast.makeText(
                                                            this@ListPanenTBSActivity,
                                                            "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    else -> {
                                                        AppLogger.d("All items archived successfully")
                                                        playSound(R.raw.berhasil_konfirmasi)
                                                        Toast.makeText(
                                                            this@ListPanenTBSActivity,
                                                            "Semua data berhasil diarsipkan",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    }
                                                }
                                                dialog.dismiss()
                                            } catch (e: Exception) {
                                                AppLogger.e("Error in UI update: ${e.message}")
                                                Toast.makeText(
                                                    this@ListPanenTBSActivity,
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
                                                    this@ListPanenTBSActivity,
                                                    "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                dialog.dismiss()
                                            } catch (dialogException: Exception) {
                                                AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                            }
                                        }
                                    }


                                    if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {

                                        panenViewModel.loadTPHNonESPB(
                                            0,
                                            0,
                                            true,
                                            1,
                                            globalFormattedDate
                                        )
                                        panenViewModel.countTPHNonESPB(
                                            0,
                                            0,
                                            true,
                                            1,
                                            globalFormattedDate
                                        )
                                        panenViewModel.countTPHESPB(
                                            0,
                                            1,
                                            true,
                                            1,
                                            globalFormattedDate
                                        )
                                        panenViewModel.countHasBeenESPB(
                                            0,
                                            0,
                                            false, 1,
                                            globalFormattedDate
                                        )
                                    } else {
                                        panenViewModel.loadTPHNonESPB(
                                            0,
                                            0, true,
                                            0,
                                            globalFormattedDate
                                        )
                                        panenViewModel.countTPHNonESPB(
                                            0,
                                            0,

                                            true, 0,
                                            globalFormattedDate
                                        )
                                        panenViewModel.countTPHESPB(
                                            1,
                                            0,

                                            true, 0,
                                            globalFormattedDate
                                        )
                                    }

                                }
                            }
                        ) {

                        }
                    }
                } else {

                    btnConfirmScanPanenTPH.setOnClickListener {
                        takeQRCodeScreenshot(view)
                        onBackPressed()
                    }
                }


                lifecycleScope.launch {
                    try {
                        delay(1000)

                        val jsonData = withContext(Dispatchers.IO) {
                            try {
                                if (featureName == "Detail eSPB") {

                                    val gson = Gson()
                                    val espbObject = JsonObject().apply {
                                        addProperty("blok_jjg", blok_jjg)
                                        addProperty("nopol", nopol)
                                        addProperty("driver", driver)
                                        addProperty("pemuat_id", pemuat_id)
                                        addProperty("kemandoran_id", kemandoran_id)
                                        addProperty("pemuat_nik", pemuat_nik)
                                        addProperty("transporter_id", transporter_id)
                                        addProperty("mill_id", mill_id)
                                        addProperty("created_by_id", created_by_id)
                                        addProperty("creator_info", creatorInfo)
                                        addProperty("no_espb", no_espb)
                                        addProperty("created_at", dateTime)
                                    }

                                    val rootObject = JsonObject().apply {
                                        add("espb", espbObject)
                                        addProperty("tph_0", tph0)
                                        addProperty("tph_1", tph1)
                                    }

                                    gson.toJson(rootObject)
                                } else {
                                    val effectiveLimit =
                                        if (limitFun == 0) mappedData.size else limitFun

                                    // Take only the required number of items
                                    val limitedData = mappedData.take(effectiveLimit)

                                    formatPanenDataForQR(limitedData)
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error generating JSON data: ${e.message}")
                                throw e
                            }
                        }


                        AppLogger.d("jsonData $jsonData")

                        // Check JSON size BEFORE encoding
                        val jsonSizeInBytes = jsonData.toByteArray(Charsets.UTF_8).size
                        val jsonSizeInKB = jsonSizeInBytes / 1024.0

                        AppLogger.d("JSON size: $jsonSizeInKB KB ($jsonSizeInBytes bytes)")

                        if (jsonSizeInKB > AppUtils.MAX_QR_SIZE_KB) {
                            withContext(Dispatchers.Main) {
                                stopLoadingAnimation(loadingLogo, loadingContainer)

                                AlertDialogUtility.withSingleAction(
                                    this@ListPanenTBSActivity,
                                    "OK",
                                    "Data Terlalu Besar",
                                    "Ukuran data ${String.format("%.2f", jsonSizeInKB)} KB melebihi batas maksimum ${AppUtils.MAX_QR_SIZE_KB} KB. Silakan kurangi jumlah data yang akan di-generate.",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {
                                    // Close bottom sheet or do nothing
                                }
                            }
                            return@launch // Stop execution
                        }

                        val encodedData = withContext(Dispatchers.IO) {
                            try {
                                encodeJsonToBase64ZipQR(jsonData)
                                    ?: throw Exception("Encoding failed")
                            } catch (e: Exception) {
                                AppLogger.e("Error encoding data: ${e.message}")
                                throw e
                            }
                        }


                        val effectiveLimit =
                            if (limit == 0) mappedData.size else limit
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
                        withContext(Dispatchers.Main) {
                            try {
                                generateHighQualityQRCode(
                                    encodedData, qrCodeImageView,
                                    this@ListPanenTBSActivity,
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
                                        @SuppressLint("SuspiciousIndentation")
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
        val changePhotoCard =
            fullscreenView.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val deletePhotoCard =
            fullscreenView.findViewById<MaterialCardView>(R.id.cardDeletePhoto)

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


    // Helper function to show errors
    fun showErrorMessageGenerateQR(view: View, message: String) {
        val titleLayoutText = view.findViewById<TextView>(R.id.textTitleQRGenerate)
        val dashedLine = view.findViewById<View>(R.id.dashedLine)
        dashedLine.visibility = View.VISIBLE
        titleLayoutText.visibility = View.VISIBLE
        titleLayoutText.text = "Terjadi Kesalahan Generate QR!"
        titleLayoutText.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorRedDark
            )
        )
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }


    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Minify JSON first
            val minifiedJson = JSONObject(jsonData).toString()
            val originalJsonSize = minifiedJson.toByteArray(StandardCharsets.UTF_8).size
            AppLogger.d("Original JSON size: $originalJsonSize bytes")

            // SIZE CHECK: If original JSON is more than 4KB, just return null
            if (originalJsonSize > 4096) { // 4KB = 4096 bytes
                AppLogger.e("❌ JSON TOO LARGE: $originalJsonSize bytes (limit: 4096 bytes)")
                AppLogger.e("❌ QR generation aborted - data exceeds size limit")
                return null // Return null to indicate failure
            }

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
                val zipSize = zipBytes.size

                // Enhanced zip size logging
                AppLogger.d("=== ZIP COMPRESSION DETAILS ===")
                AppLogger.d("ZIP SIZE: $zipSize bytes")
                AppLogger.d("ZIP SIZE: ${String.format("%.2f", zipSize / 1024.0)} KB")
                AppLogger.d(
                    "Compression ratio: ${
                        String.format(
                            "%.2f",
                            (originalJsonSize.toDouble() / zipSize.toDouble())
                        )
                    }:1"
                )
                AppLogger.d(
                    "Size reduction: ${
                        String.format(
                            "%.1f",
                            ((originalJsonSize - zipSize).toDouble() / originalJsonSize.toDouble() * 100)
                        )
                    }%"
                )
                AppLogger.d("=== END ZIP COMPRESSION DETAILS ===")

                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)
                val base64Size = base64Encoded.length
                AppLogger.d("Base64 encoded size: $base64Size characters")

                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                val finalResult = firstHalf + AppUtils.half_json_encrypted + secondHalf
                val finalSize = finalResult.length
                AppLogger.d("Final QR data size: $finalSize characters")

                // Size summary
                AppLogger.d("=== SIZE SUMMARY ===")
                AppLogger.d("Original JSON: $originalJsonSize bytes")
                AppLogger.d("Compressed ZIP: $zipSize bytes")
                AppLogger.d("Base64 encoded: $base64Size chars")
                AppLogger.d("Final QR data: $finalSize chars")

                // Additional check on final QR size
                if (finalSize > 2000) {
                    AppLogger.e("⚠️ WARNING: Final QR data is ${finalSize} characters - may be too large for scanning!")
                } else {
                    AppLogger.d("✅ QR data size is acceptable for scanning")
                }

                finalResult
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            null // Return null instead of throwing exception
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            null // Return null instead of throwing exception
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            null // Return null instead of throwing exception
        }
    }


    private fun setupObservers() {
        val listBlok = findViewById<TextView>(R.id.listBlok)
        val totalJjg = findViewById<TextView>(R.id.totalJjg)
        val totalTPH = findViewById<TextView>(R.id.totalTPH)
        val blokSection = findViewById<LinearLayout>(R.id.blok_section)
        val totalSection = findViewById<LinearLayout>(R.id.total_section)
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        val btnGenerateQRTPHUnl = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPHUnl)
        val btnTransferBT = findViewById<FloatingActionButton>(R.id.btnTransferBT)
        val tvTransferBT = findViewById<TextView>(R.id.tvTransferBT)
        val tvGenQR60 = findViewById<TextView>(R.id.tvGenQR60)
        val tvGenQRFull = findViewById<TextView>(R.id.tvGenQRFull)

        blokSection.visibility = View.GONE
        totalSection.visibility = View.GONE

        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        absensiViewModel.activeAbsensiList.observe(this@ListPanenTBSActivity) { absensiWithRelations ->
            val absensiData = absensiWithRelations ?: emptyList()

            // Extract all NIKs of present karyawan from all absensi entries
            val newPresentNikSet = mutableSetOf<String>()

            // Build the dropdown absensi edit list - simple strings
            val absensiWorkers = mutableListOf<String>()

            Log.d("AbsensiDebug", "Total absensi entries: ${absensiData.size}")

            absensiData.forEach { absensiRelation ->
                val absensi = absensiRelation.absensi

                Log.d("AbsensiDebug", "Raw karyawan_msk_nik: '${absensi.karyawan_msk_nik}'")
                Log.d("AbsensiDebug", "Raw karyawan_msk_nama: '${absensi.karyawan_msk_nama}'")

                // Split the comma-separated NIK and Nama strings
                val niks = absensi.karyawan_msk_nik.split(",")
                val names = absensi.karyawan_msk_nama.split(",")

                Log.d("AbsensiDebug", "Split NIKs: ${niks.size} items -> $niks")
                Log.d("AbsensiDebug", "Split Names: ${names.size} items -> $names")

                // Add to present NIK set (your existing logic)
                newPresentNikSet.addAll(niks.filter {
                    it.isNotEmpty() && it.trim().isNotEmpty()
                })

                // Create simple strings by merging one karyawan with one nik
                niks.forEachIndexed { index, nik ->
                    val cleanNik = nik.trim()
                    Log.d(
                        "AbsensiDebug",
                        "Processing NIK[$index]: '$nik' -> cleaned: '$cleanNik'"
                    )

                    if (cleanNik.isNotEmpty()) {
                        val workerName =
                            if (index < names.size && names[index].trim().isNotEmpty()) {
                                val rawName = names[index].trim()
                                Log.d("AbsensiDebug", "Found name at index $index: '$rawName'")
                                rawName
                            } else {
                                Log.w(
                                    "AbsensiDebug",
                                    "No valid name found at index $index! Using NIK as fallback"
                                )
                                "Worker $cleanNik" // Use NIK as fallback instead of "Unknown Worker"
                            }

                        // Create simple string: "WORKER_NAME - NIK"
                        val workerString = "$workerName - $cleanNik"
                        Log.d("AbsensiDebug", "Created worker string: '$workerString'")

                        // Avoid duplicates
                        if (!absensiWorkers.contains(workerString)) {
                            absensiWorkers.add(workerString)
                            Log.d("AbsensiDebug", "Added to list: '$workerString'")
                        } else {
                            Log.d("AbsensiDebug", "Duplicate skipped: '$workerString'")
                        }
                    }
                }
            }

            Log.d("AbsensiDebug", "Final dropdown list (${absensiWorkers.size} items):")
            absensiWorkers.forEachIndexed { index, worker ->
                Log.d("AbsensiDebug", "[$index] $worker")
            }

            // Update global variables
            dropdownAbsensiEdit = absensiWorkers
            presentNikSet = newPresentNikSet

            // Now get all karyawan and filter by present NIKs
            lifecycleScope.launch {
                panenViewModel.getAllKaryawan() // This should be added to your ViewModel
                delay(100)

                withContext(Dispatchers.Main) {
                    panenViewModel.allKaryawanList.observe(this@ListPanenTBSActivity) { list ->
                        val allKaryawan = list ?: emptyList()

                        // Only filter if presentNikSet has values
                        if (presentNikSet.isNotEmpty()) {
                            // Filter to get only present karyawan
                            val presentKaryawan = allKaryawan.filter { karyawan ->
                                karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                            }

                            // Store filtered (present) karyawan in global variable
                            dropdownAbsensiFullData = presentKaryawan

                            AppLogger.d("Total karyawan: ${allKaryawan.size}")
                            AppLogger.d("Filtered to present karyawan: ${presentKaryawan.size}")

                            // If we have present karyawan, log a sample
                            if (presentKaryawan.isNotEmpty()) {
                                val sampleSize = minOf(3, presentKaryawan.size)
                                val sample = presentKaryawan.take(sampleSize)
                                AppLogger.d("Sample present karyawan: $sample")
                            } else {
                                AppLogger.d("No present karyawan found after filtering")
                            }
                        } else {
                            dropdownAbsensiFullData = emptyList()
                        }
                    }
                }
            }
        }

        panenViewModel.panenCountActive.observe(this) { panenList ->
            val userAfdelingId = prefManager!!.afdelingIdUserLogin
            AppLogger.d("=== FILTERING ACTIVE COUNT ===")
            AppLogger.d("Total records before filtering: ${panenList.size}")

            val filteredPanenList = panenList.filter { panenEntityWithRelations ->
                val tphDivisi = panenEntityWithRelations.tph?.divisi.toString()
                val panenAsistensi = panenEntityWithRelations.panen.asistensi
                val panenAsistensiDivisi = panenEntityWithRelations.panen.asistensi_divisi

                val afdelingMatch = tphDivisi == userAfdelingId
                val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                val included = afdelingMatch || asistensiMatch

//                AppLogger.d("Active TPH ${panenEntityWithRelations.panen.tph_id}: divisi=$tphDivisi, user=$userAfdelingId, asistensi=$panenAsistensi, asistensi_divisi=$panenAsistensiDivisi -> ${if (included) "INCLUDED" else "EXCLUDED"}")

                included
            }

            AppLogger.d("Active count after filtering: ${filteredPanenList.size}")
            counterTersimpan.text = filteredPanenList.size.toString()
        }

        panenViewModel.panenCountArchived.observe(this) { panenList ->
            val userAfdelingId = prefManager!!.afdelingIdUserLogin
            AppLogger.d("=== FILTERING ARCHIVED COUNT ===")
            AppLogger.d("Total records before filtering: ${panenList.size}")

            val filteredPanenList = panenList.filter { panenEntityWithRelations ->
                val tphDivisi = panenEntityWithRelations.tph?.divisi.toString()
                val panenAsistensi = panenEntityWithRelations.panen.asistensi
                val panenAsistensiDivisi = panenEntityWithRelations.panen.asistensi_divisi

                val afdelingMatch = tphDivisi == userAfdelingId
                val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                val included = afdelingMatch || asistensiMatch

                included
            }

            AppLogger.d("Archived count after filtering: ${filteredPanenList.size}")
            counterTerscan.text = filteredPanenList.size.toString()
        }

        panenViewModel.panenCountHasBeenESPB.observe(this) { panenList ->
            val userAfdelingId = prefManager!!.afdelingIdUserLogin
            AppLogger.d("=== FILTERING ESPB COUNT ===")
            AppLogger.d("Total records before filtering: ${panenList.size}")

            val filteredPanenList = panenList.filter { panenEntityWithRelations ->
                val tphDivisi = panenEntityWithRelations.tph?.divisi.toString()
                val panenAsistensi = panenEntityWithRelations.panen.asistensi
                val panenAsistensiDivisi = panenEntityWithRelations.panen.asistensi_divisi

                val afdelingMatch = tphDivisi == userAfdelingId
                val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                val included = afdelingMatch || asistensiMatch

//                AppLogger.d("ESPB TPH ${panenEntityWithRelations.panen.tph_id}: divisi=$tphDivisi, user=$userAfdelingId, asistensi=$panenAsistensi, asistensi_divisi=$panenAsistensiDivisi -> ${if (included) "INCLUDED" else "EXCLUDED"}")

                included
            }

            AppLogger.d("ESPB count after filtering: ${filteredPanenList.size}")
            counterPerPemanen.text = filteredPanenList.size.toString()
        }

        mutuBuahViewModel.countMutuBuahUnuploaded.observe(this) { count ->
            counterTersimpan.text = count.toString()
        }
        mutuBuahViewModel.countMutuBuahUploaded.observe(this) { count ->
            counterTerscan.text = count.toString()
        }

        mutuBuahViewModel.activeMutuBuahList.observe(this) { mutuBuahList ->
            btnGenerateQRTPH.visibility = View.GONE
            btnGenerateQRTPHUnl.visibility = View.GONE
            if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    lifecycleScope.launch {

                        if (mutuBuahList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            // Map MutuBuahEntity to the standard format used by your adapter
                            mappedData = mutuBuahList.map { mutuBuahEntity ->
                                mapOf<String, Any>(
                                    "id" to (mutuBuahEntity.id as Any),
                                    "tph_id" to (mutuBuahEntity.tph as Any),
                                    "date_created" to (mutuBuahEntity.createdDate as Any),
                                    "blok_name" to (mutuBuahEntity.blokKode as Any),
                                    "nomor" to (mutuBuahEntity.tphNomor as Any),
                                    "created_by" to (mutuBuahEntity.createdBy as Any),
                                    "jjg_json" to createMutuBuahJjgJson(mutuBuahEntity),
                                    "foto" to (mutuBuahEntity.foto as Any),
                                    "komentar" to (mutuBuahEntity.komentar ?: "" as Any),
                                    "asistensi" to ("" as Any),
                                    "lat" to (mutuBuahEntity.lat as Any),
                                    "lon" to (mutuBuahEntity.lon as Any),
                                    "jenis_panen" to ("" as Any),
                                    "ancak" to ("" as Any),
                                    "archive" to ("" as Any),
                                    "nama_estate" to (mutuBuahEntity.deptAbbr as Any),
                                    "nama_afdeling" to (mutuBuahEntity.divisiAbbr as Any),
                                    "nomor_pemanen" to (mutuBuahEntity.nomorPemanen as Any),
                                    "blok_banjir" to ("" as Any),
                                    "tahun_tanam" to ("" as Any),
                                    "nama_karyawans" to (mutuBuahEntity.createdName as Any),
                                    "nama_kemandorans" to (mutuBuahEntity.kemandoran as Any),
                                    "username" to ("" as Any),
                                    "foto_selfie" to (mutuBuahEntity.foto_selfie as Any),
                                    "jjg_panen" to (mutuBuahEntity.jjgPanen as Any), // Add this line
                                    "jjg_masak" to (mutuBuahEntity.jjgMasak as Any),
                                    "jjg_mentah" to (mutuBuahEntity.jjgMentah as Any),
                                    "jjg_lewat_masak" to (mutuBuahEntity.jjgLewatMasak as Any),
                                    "jjg_kosong" to (mutuBuahEntity.jjgKosong as Any),
                                    "jjg_abnormal" to (mutuBuahEntity.jjgAbnormal as Any),
                                    "jjg_serangan_tikus" to (mutuBuahEntity.jjgSeranganTikus as Any),
                                    "jjg_panjang" to (mutuBuahEntity.jjgPanjang as Any),
                                    "jjg_tidak_vcut" to (mutuBuahEntity.jjgTidakVcut as Any),
                                    "jjg_bayar" to (mutuBuahEntity.jjgBayar as Any),
                                    "jjg_kirim" to (mutuBuahEntity.jjgKirim as Any)
                                )
                            }

                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            // Calculate summary data for Mutu Buah using jjg_panen
                            val totalJjgPanen = mappedData.sumOf { data ->
                                (data["jjg_panen"] as? Int) ?: 0
                            }

                            val blokNames = mappedData.map { it["blok_name"].toString() }.distinct()
                            val blokDisplay = if (blokNames.size <= 3) {
                                blokNames.joinToString(", ")
                            } else {
                                "${blokNames.take(3).joinToString(", ")}, ..."
                            }

                            blok = if (blokNames.isEmpty()) "-" else blokNames.joinToString(", ")

                            listBlok.text = blokDisplay
                            jjg = totalJjgPanen // Use jjg_panen total
                            totalJjg.text = totalJjgPanen.toString()
                            tph = mappedData.size
                            totalTPH.text = mappedData.size.toString()

                            // Set Blok
                            val tvBlok = findViewById<View>(R.id.tv_blok)
                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

                            // Set jjg (using jjg_panen)
                            val tvJjg = findViewById<View>(R.id.tv_jjg)
                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                totalJjgPanen.toString()

                            // Set TPH count
                            val tvTph = findViewById<View>(R.id.tv_total_tph)
                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumlah TPH"
                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                mappedData.size.toString()

                            listAdapter.updateData(mappedData)
                            originalData = emptyList()
                            filterSection.visibility = View.GONE

                        } else {
                            val emptyStateMessage = "Tidak ada data mutu buah"
                            tvEmptyState.text = emptyStateMessage
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE
                        }
                    }
                }, 500)
            }
        }

        panenViewModel.detailESPb.observe(this) { panenList ->

            Handler(Looper.getMainLooper()).postDelayed({
                loadingDialog.dismiss()
                lifecycleScope.launch {


                    if (panenList.isNotEmpty()) {


                        val processedDataList = mutableListOf<Map<String, Any>>()
                        var incrementalId = 1 // Start from 1

                        // Loop through each ESPB record
                        for (espbEntity in panenList) {
                            val tph1Data = espbEntity.tph1

                            if (!tph1Data.isNullOrEmpty()) {
                                // Split by semicolon to get individual TPH records
                                val tphRecords = tph1Data.split(";")

                                for (tphRecord in tphRecords) {
                                    if (tphRecord.isNotEmpty()) {
                                        // Split each record by comma
                                        val parts = tphRecord.split(",")

                                        if (parts.size >= 3) { // Changed to >= 3 to ensure we have the third index
                                            val tphId = parts[0].trim()
                                            val dateCreated = parts[1].trim()
                                            val kpValue =
                                                parts[2].trim() // Third index for KP value

                                            // Create JSON for jjg_json
                                            val jjgJson = "{\"KP\": $kpValue}"

                                            try {
                                                // Convert tphId to Int for the query
                                                val tphIdInt = tphId.toIntOrNull()

                                                if (tphIdInt != null) {
                                                    // Fetch TPH and Blok info from database through ViewModel
                                                    val tphBlokInfo =
                                                        panenViewModel.getTPHAndBlokInfo(
                                                            tphIdInt
                                                        )

                                                    val standardData = mapOf<String, Any>(
                                                        "id" to incrementalId.toString(), // Incremental ID as string
                                                        "tph_id" to tphId,
                                                        "date_created" to dateCreated,
                                                        "blok_name" to (tphBlokInfo?.blokKode
                                                            ?: "Unknown"),
                                                        "nomor" to (tphBlokInfo?.tphNomor
                                                            ?: "Unknown"),
                                                        "created_by" to "", // Empty as requested
                                                        "jjg_json" to jjgJson, // JSON with KP value
                                                        "foto" to "", // Empty as requested
                                                        "komentar" to "", // Empty as requested
                                                        "asistensi" to "", // Empty as requested
                                                        "lat" to "", // Empty as requested
                                                        "lon" to "", // Empty as requested
                                                        "jenis_panen" to "", // Empty as requested
                                                        "ancak" to "", // Empty as requested
                                                        "archive" to "", // Empty as requested
                                                        "nama_estate" to "", // Empty as requested
                                                        "nama_afdeling" to "", // Empty as requested
                                                        "blok_banjir" to "", // Empty as requested
                                                        "tahun_tanam" to "", // Empty as requested
                                                        "nama_karyawans" to "", // Empty as requested
                                                        "nama_kemandorans" to "", // Empty as requested
                                                        "username" to "" // Empty as requested
                                                    )

                                                    processedDataList.add(standardData)
                                                    incrementalId++ // Increment for next record

                                                    AppLogger.d("Processed TPH: ID=${incrementalId - 1}, TPH_ID=$tphId, Date=$dateCreated, KP=$kpValue, JSON=$jjgJson, Blok=${tphBlokInfo?.blokKode}, Nomor=${tphBlokInfo?.tphNomor}")
                                                } else {
                                                    AppLogger.w("Invalid TPH ID: $tphId")
                                                }
                                            } catch (e: Exception) {
                                                AppLogger.e("Error processing TPH record: $tphRecord, Error: ${e.message}")
                                            }
                                        } else {
                                            AppLogger.w("Invalid TPH record format (missing KP value): $tphRecord")
                                        }
                                    }
                                }
                            }
                        }

                        // Check current state and process accordingly
                        if (currentState == 0) {
                            // State 0: Show individual TPH records
                            mappedData = processedDataList

                            // Extract data from processedDataList
                            val distinctBlokNames =
                                processedDataList.map { it["blok_name"].toString() }.distinct()
                            val blokNamesString = distinctBlokNames.joinToString(", ")

// Count total TPH records (not distinct)
                            val totalTphCount = processedDataList.size

// Sum all KP values from jjg_json
                            var totalKpSum = 0.0
                            for (data in processedDataList) {
                                try {
                                    val jjgJson = JSONObject(data["jjg_json"].toString())
                                    val kpValue = jjgJson.optDouble("KP", 0.0)
                                    totalKpSum += kpValue
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing jjg_json: ${e.message}")
                                }
                            }

// Format the total KP sum
                            val formattedTotalKp =
                                if (totalKpSum == totalKpSum.toInt().toDouble()) {
                                    totalKpSum.toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.1f", totalKpSum)
                                }

// Set the values
                            blok = if (blokNamesString.isEmpty()) "-" else blokNamesString
                            listBlok.text = blokNamesString
                            jjg = formattedTotalKp.toDoubleOrNull()?.toInt() ?: 0
                            totalJjg.text = formattedTotalKp
                            tph = totalTphCount
                            totalTPH.text = totalTphCount.toString()

// Set Blok
                            val tvBlok = findViewById<View>(R.id.tv_blok)
                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

// Set jjg (now shows sum of KP values)
                            val tvJjg = findViewById<View>(R.id.tv_jjg)
                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                formattedTotalKp

// Set TPH count (total count, not distinct)
                            val tvTph = findViewById<View>(R.id.tv_total_tph)
                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumlah TPH"
                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                totalTphCount.toString()

                            listAdapter.updateData(processedDataList)
                        } else if (currentState == 1) {
                            // State 1: Merge by blok (similar to your existing merge logic)
                            val globalMergedBlokMap =
                                mutableMapOf<String, MutableMap<String, Any>>()
                            val jjgTypes = listOf("KP")

                            for (blokData in processedDataList) {
                                val blokName = blokData["blok_name"].toString()
                                val tphId = blokData["tph_id"].toString()
                                val jjgJson = JSONObject(blokData["jjg_json"].toString())

                                // Extract KP value
                                val jjgValues = jjgTypes.associateWith { type ->
                                    jjgJson.optDouble(type, 0.0)
                                }

                                if (globalMergedBlokMap.containsKey(blokName)) {
                                    val existingBlokData = globalMergedBlokMap[blokName]!!
                                    val existingJjgJson =
                                        JSONObject(existingBlokData["jjg_json"].toString())

                                    // Update KP value in the existing JSON
                                    for (type in jjgTypes) {
                                        val existingValue = existingJjgJson.optDouble(type, 0.0)
                                        val newValue = jjgValues[type] ?: 0.0
                                        val totalValue = existingValue + newValue
                                        existingJjgJson.put(type, totalValue)
                                    }

                                    // Update the JJG JSON in the existing blok data
                                    existingBlokData["jjg_json"] = existingJjgJson.toString()

                                    // For restan, use KP as the total
                                    val newTotalKP = existingJjgJson.optDouble("KP", 0.0)
                                    existingBlokData["jjg_total"] =
                                        if (newTotalKP == newTotalKP.toInt().toDouble()) {
                                            newTotalKP.toInt().toString()
                                        } else {
                                            String.format(Locale.US, "%.1f", newTotalKP)
                                        }

                                    existingBlokData["jjg_dibayar"] =
                                        existingBlokData["jjg_total"].toString() // Fix: Convert to string

                                    val existingTransactions =
                                        (existingBlokData["jumlah_transaksi"]?.toString()
                                            ?.toIntOrNull() ?: 1) + 1
                                    existingBlokData["jumlah_transaksi"] =
                                        existingTransactions.toString()

                                    val tphIds =
                                        (existingBlokData["tph_ids"]?.toString()
                                            ?: "").split(",")
                                            .filter { it.isNotEmpty() }.toMutableSet()
                                    tphIds.add(tphId) // Fix: Changed from tephId to tphId
                                    existingBlokData["tph_ids"] = tphIds.joinToString(",")
                                    existingBlokData["tph_count"] = tphIds.size.toString()

                                    existingBlokData["jjg_each_blok"] =
                                        "${existingBlokData["jjg_total"]} (${existingBlokData["jjg_dibayar"]})"

                                } else {
                                    // This is a new blok, create a new entry
                                    val mutableBlokData = blokData.toMutableMap()

                                    // Format the KP value
                                    val jjgKP = jjgValues["KP"] ?: 0.0
                                    val formattedJjgKP =
                                        if (jjgKP == jjgKP.toInt().toDouble()) {
                                            jjgKP.toInt().toString()
                                        } else {
                                            String.format(Locale.US, "%.1f", jjgKP)
                                        }

                                    // Add blok-specific fields
                                    mutableBlokData["jjg_total"] = formattedJjgKP
                                    mutableBlokData["jjg_dibayar"] =
                                        formattedJjgKP // Same as total for restan
                                    mutableBlokData["jumlah_transaksi"] = "1"
                                    mutableBlokData["tph_ids"] = tphId
                                    mutableBlokData["tph_count"] = "1"
                                    mutableBlokData["jjg_each_blok"] =
                                        "$formattedJjgKP ($formattedJjgKP)"

                                    // Empty worker tracking for restan
                                    mutableBlokData["nama_karyawans_all"] = ""
                                    mutableBlokData["karyawan_count"] = "0"
                                    mutableBlokData["nama_kemandorans_all"] = ""

                                    globalMergedBlokMap[blokName] = mutableBlokData
                                }
                            }

                            val finalMergedData = globalMergedBlokMap.values.toList().sortedBy {
                                it["blok_name"].toString()
                            }

                            listAdapter.updateData(finalMergedData)
                        }

                        originalData =
                            emptyList() // Reset original data when new data is loaded
                        filterSection.visibility = View.GONE // Hide filter section for new data

                    } else {
                        val emptyStateMessage = "Tidak ada data"
                        tvEmptyState.text = emptyStateMessage
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        blokSection.visibility = View.GONE
                        totalSection.visibility = View.GONE
                    }
                }
            }, 500)
        }

        panenViewModel.activePanenList.observe(this) { panenList ->
            AppLogger.d("=== FILTERING ACTIVE PANEN LIST ===")
            AppLogger.d("Total records before filtering: ${panenList.size}")

            val filteredPanenList = panenList.filter { panenEntityWithRelations ->
                val tphDivisi = panenEntityWithRelations.tph?.divisi.toString()
                val userAfdelingId = prefManager!!.afdelingIdUserLogin
                val panenAsistensi = panenEntityWithRelations.panen.asistensi
                val panenAsistensiDivisi = panenEntityWithRelations.panen.asistensi_divisi

                AppLogger.d("Checking record: TPH ID = ${panenEntityWithRelations.panen.tph_id}")
                AppLogger.d("  TPH divisi = $tphDivisi, User afdeling = $userAfdelingId")
                AppLogger.d("  Panen asistensi = $panenAsistensi, asistensi_divisi = $panenAsistensiDivisi")

                // First check: if afdeling matches, include it
                if (tphDivisi == userAfdelingId) {
                    AppLogger.d("  ✓ INCLUDED: Afdeling matches")
                    true
                } else {
                    // If afdeling doesn't match, check if asistensi is 2 AND asistensi_divisi matches
                    val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                    if (asistensiMatch) {
                        AppLogger.d("  ✓ INCLUDED: Afdeling doesn't match but asistensi = 2 and asistensi_divisi matches")
                    } else {
                        AppLogger.d("  ✗ EXCLUDED: Afdeling doesn't match and (asistensi ≠ 2 or asistensi_divisi doesn't match)")
                    }
                    asistensiMatch
                }
            }

            AppLogger.d("Total records after filtering: ${filteredPanenList.size}")
            AppLogger.d("=== FILTERING COMPLETE ===")

            if (currentState == 0 || currentState == 1 || currentState == 2 || currentState == 3) {

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (filteredPanenList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            val allWorkerData = mutableListOf<Map<String, Any>>()

                            originalMappedData.clear()
                            filteredPanenList.map { panenWithRelations ->


                                if (panenWithRelations.tph == null) {
                                    AlertDialogUtility.withSingleAction(
                                        this@ListPanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        "Data TPH Tidak Ditemukan",
                                        "TPH dengan ID ${panenWithRelations.panen.tph_id} tidak ditemukan di database. Silakan periksa data TPH.",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {
                                        finish()
                                    }
                                    return@launch // Exit the entire coroutine
                                }

                                val karyawanNiks =
                                    panenWithRelations.panen.karyawan_nik?.toString()
                                        ?.split(",")
                                        ?: emptyList()
                                val karyawanNamas =
                                    panenWithRelations.panen.karyawan_nama?.toString()
                                        ?.split(",")
                                        ?: emptyList()

                                // Create Worker objects from this specific data
                                val savedWorkerData = mutableListOf<Worker>()

                                karyawanNiks.forEachIndexed { index, nik ->
                                    val cleanNik = nik.trim()
                                    if (cleanNik.isNotEmpty()) {
                                        val workerName = if (index < karyawanNamas.size) {
                                            karyawanNamas[index].trim()
                                        } else {
                                            "Unknown Worker"
                                        }

                                        // Create Worker object like your example
                                        val worker = Worker(cleanNik, "$workerName - $cleanNik")
                                        savedWorkerData.add(worker)
                                    }
                                }

                                val standardData = mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode
                                        ?: "Unknown"),
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
                                    "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                    "foto" to (panenWithRelations.panen.foto as Any),
                                    "komentar" to (panenWithRelations.panen.komentar as Any),
                                    "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                    "karyawan_nik" to (panenWithRelations.panen.karyawan_nik as Any),
                                    "karyawan_nama" to (panenWithRelations.panen.karyawan_nama as Any),
                                    "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                    "kemandoran_id" to (panenWithRelations.panen.kemandoran_id as Any),
                                    "lat" to (panenWithRelations.panen.lat as Any),
                                    "lon" to (panenWithRelations.panen.lon as Any),
                                    "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                    "nomor_pemanen" to (panenWithRelations.panen.nomor_pemanen as Any),
                                    "ancak" to (panenWithRelations.panen.ancak as Any),
                                    "archive" to (panenWithRelations.panen.archive as Any),
                                    "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                    "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                    "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                    "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                    "nama_karyawans" to "",
                                    "nama_kemandorans" to "",
                                    "username" to (panenWithRelations.panen.username as Any),
                                    "available_workers" to savedWorkerData,
                                    "dropdown_absensi_edit" to dropdownAbsensiEdit,
                                    "dropdown_absensi_full_data" to dropdownAbsensiFullData
                                )

                                val originalDataMapped = standardData.toMutableMap()
                                originalMappedData.add(originalDataMapped)

                                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2) {
                                    val karyawanIds =
                                        panenWithRelations.panen.karyawan_id.toString()
                                            .split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }

                                    val kemandoranIds =
                                        panenWithRelations.panen.kemandoran_id?.toString()
                                            ?.split(",")
                                            ?.map { it.trim() }
                                            ?.filter { it.isNotEmpty() } ?: listOf()

                                    val karyawanNiks =
                                        panenWithRelations.panen.karyawan_nik?.toString()
                                            ?.split(",")
                                            ?.map { it.trim() }
                                            ?.filter { it.isNotEmpty() } ?: listOf()

                                    val jjgJsonStr =
                                        panenWithRelations.panen.jjg_json as? String
                                            ?: "{}" // Ensure it's a valid JSON string
                                    val jjgJson =
                                        JSONObject(jjgJsonStr) // Convert to JSONObject

                                    // Calculate total jjg count
                                    val totalTO = jjgJson["TO"].toString().toInt()
                                    val totalUN = jjgJson["UN"].toString().toInt()
                                    val totalOV = jjgJson["OV"].toString().toInt()
                                    val totalEM = jjgJson["EM"].toString().toInt()
                                    val totalAB = jjgJson["AB"].toString().toInt()
                                    val totalRA = jjgJson["RA"].toString().toInt()
                                    val totalLO = jjgJson["LO"].toString().toInt()
                                    val totalTI = jjgJson["TI"].toString().toInt()
                                    val totalRI = jjgJson["RI"].toString().toInt()
                                    val totalKP = jjgJson["KP"].toString().toInt()
                                    val totalPA = jjgJson["PA"].toString().toInt()

                                    // Determine how many workers we need to distribute the jjg to
                                    val workerCount =
                                        maxOf(karyawanIds.size, kemandoranIds.size, 1)

                                    // If there are multiple workers, create separate entries for each
                                    val multiWorkerData = mutableListOf<Map<String, Any>>()

                                    for (i in 0 until workerCount) {
                                        // Get the corresponding IDs for this worker
                                        val karyawanId =
                                            if (i < karyawanIds.size) karyawanIds[i] else ""
                                        val kemandoranId =
                                            if (i < kemandoranIds.size) kemandoranIds[i] else ""
                                        val karyawanNik =
                                            if (i < karyawanNiks.size) karyawanNiks[i] else ""

                                        // Divide the jjg counts equally among workers as a decimal value
                                        // Use toDouble() to ensure decimal division
                                        val workerTO = totalTO.toDouble() / workerCount
                                        val workerUN = totalUN.toDouble() / workerCount
                                        val workerOV = totalOV.toDouble() / workerCount
                                        val workerEM = totalEM.toDouble() / workerCount
                                        val workerAB = totalAB.toDouble() / workerCount
                                        val workerRA = totalRA.toDouble() / workerCount
                                        val workerLO = totalLO.toDouble() / workerCount
                                        val workerTI = totalTI.toDouble() / workerCount
                                        val workerRI = totalRI.toDouble() / workerCount
                                        val workerKP = totalKP.toDouble() / workerCount
                                        val workerPA = totalPA.toDouble() / workerCount

                                        // Create worker-specific jjg_json
                                        val workerJjgJson = JsonObject().apply {
                                            addProperty("TO", workerTO)
                                            addProperty("UN", workerUN)
                                            addProperty("OV", workerOV)
                                            addProperty("EM", workerEM)
                                            addProperty("AB", workerAB)
                                            addProperty("RA", workerRA)
                                            addProperty("LO", workerLO)
                                            addProperty("TI", workerTI)
                                            addProperty("RI", workerRI)
                                            addProperty("KP", workerKP)
                                            addProperty("PA", workerPA)
                                        }

                                        // Fetch karyawan name for this specific worker
                                        val singlePemuatData = withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getPemuatByIdList(
                                                    listOf(
                                                        karyawanId
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Single Pemuat Data: ${e.message}")
                                                null
                                            }
                                        }

                                        val workerName =
                                            singlePemuatData?.firstOrNull()?.nama ?: "-"
                                        val singleKaryawanNama =
                                            if (workerName != "-" && karyawanNik.isNotEmpty()) {
                                                "$workerName - $karyawanNik"
                                            } else {
                                                workerName
                                            }

                                        // Fetch kemandoran name for this specific worker
                                        val singleKemandoranData = withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getKemandoranById(
                                                    listOf(
                                                        kemandoranId
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Single Kemandoran Data: ${e.message}")
                                                null
                                            }
                                        }

                                        val singleKemandoranNama =
                                            singleKemandoranData?.firstOrNull()?.nama?.let { "$it" }
                                                ?: "-"

                                        // Create the map for this worker
                                        val workerData = mapOf<String, Any>(
                                            "id" to (panenWithRelations.panen.id as Any),
                                            "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                            "date_created" to (panenWithRelations.panen.date_created as Any),
                                            "blok_name" to (panenWithRelations.tph?.blok_kode
                                                ?: "Unknown"),
                                            "nomor" to (panenWithRelations.tph.nomor as Any),
                                            "created_by" to (panenWithRelations.panen.created_by as Any),
                                            "karyawan_id" to (karyawanId as Any),
                                            "kemandoran_id" to (kemandoranId as Any),
                                            "karyawan_nik" to (karyawanNik as Any),
                                            "jjg_json" to (workerJjgJson.toString() as Any),
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
                                            "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                            "nama_karyawans" to (singleKaryawanNama as Any),
                                            "nama_kemandorans" to (singleKemandoranNama as Any),
                                            "nomor_pemanen" to (panenWithRelations.panen.nomor_pemanen as Any),
                                            "username" to (panenWithRelations.panen.username as Any)
                                        )

                                        multiWorkerData.add(workerData)
                                    }


                                    allWorkerData.addAll(multiWorkerData)

                                    emptyList<Map<String, Any>>()
                                } else {
                                    val pemuatList =
                                        panenWithRelations.panen.karyawan_id.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }

                                    val pemuatData: List<KaryawanModel>? =
                                        withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getPemuatByIdList(pemuatList)
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
                                                panenViewModel.getKemandoranById(rawKemandoran)
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

                                    val updatedStandardData =
                                        standardData.toMutableMap().apply {
                                            this["nama_karyawans"] = karyawanNamas
                                            this["nama_kemandorans"] = kemandoranNamas
                                        }

                                    allWorkerData.add(updatedStandardData)

                                    listOf(updatedStandardData)
                                }
                            }.flatten()

                            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2) {
                                val globalMergedWorkerMap =
                                    mutableMapOf<String, MutableMap<String, Any>>()

                                // Define all JJG types to handle
                                val jjgTypes = listOf(
                                    "TO",
                                    "UN",
                                    "OV",
                                    "EM",
                                    "AB",
                                    "RA",
                                    "LO",
                                    "TI",
                                    "RI",
                                    "KP",
                                    "PA"
                                )

                                for (workerData in allWorkerData) {
                                    val workerName = workerData["nama_karyawans"].toString()
                                    val blokName = workerData["blok_name"].toString()
                                    val tphId = workerData["tph_id"].toString()
                                    val jjgJson = JSONObject(workerData["jjg_json"].toString())

                                    // Extract all JJG values
                                    val jjgValues = jjgTypes.associateWith { type ->
                                        jjgJson.optDouble(type, 0.0)
                                    }

                                    // Format JJG TO value
                                    val jjgTO = jjgValues["KP"] ?: 0.0
                                    val formattedJjgKP =
                                        if (jjgTO == jjgTO.toInt().toDouble()) {
                                            jjgTO.toInt().toString()
                                        } else {
                                            String.format(
                                                Locale.US,
                                                "%.1f",
                                                jjgTO
                                            )  // Using Locale.US for consistent decimal point
                                        }

                                    // Format JJG PA value
                                    val jjgPA = jjgValues["PA"] ?: 0.0
                                    val formattedJjgPA =
                                        if (jjgPA == jjgPA.toInt().toDouble()) {
                                            jjgPA.toInt().toString()
                                        } else {
                                            String.format(Locale.US, "%.1f", jjgPA)
                                        }

                                    if (globalMergedWorkerMap.containsKey(workerName)) {
                                        // Update existing worker data
                                        val existingWorkerData =
                                            globalMergedWorkerMap[workerName]!!

                                        // Update JJG JSON
                                        val existingJjgJson =
                                            JSONObject(existingWorkerData["jjg_json"].toString())
                                        for (type in jjgTypes) {
                                            val existingValue =
                                                existingJjgJson.optDouble(type, 0.0)
                                            val newValue = jjgValues[type] ?: 0.0
                                            val totalValue = existingValue + newValue
                                            existingJjgJson.put(type, totalValue)
                                        }
                                        existingWorkerData["jjg_json"] =
                                            existingJjgJson.toString()

                                        // Update JJG dibayar (PA)
                                        val newTotalPA = existingJjgJson.optDouble("PA", 0.0)
                                        existingWorkerData["jjg_dibayar"] =
                                            if (newTotalPA == newTotalPA.toInt().toDouble()) {
                                                newTotalPA.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", newTotalPA)
                                            }

                                        // Update JJG total (TO)
                                        val newTotalKP = existingJjgJson.optDouble("KP", 0.0)
                                        existingWorkerData["jjg_total_blok"] =
                                            if (newTotalKP == newTotalKP.toInt().toDouble()) {
                                                newTotalKP.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", newTotalKP)
                                            }

                                        // Update counters
                                        val existingOccurrences =
                                            (existingWorkerData["occurrence_count"]?.toString()
                                                ?.toIntOrNull() ?: 1) + 1
                                        existingWorkerData["occurrence_count"] =
                                            existingOccurrences.toString()

                                        // Update TPH tracking
                                        val tphIds =
                                            (existingWorkerData["tph_ids"]?.toString()
                                                ?: "").split(
                                                ","
                                            )
                                                .filter { it.isNotEmpty() }.toMutableSet()
                                        tphIds.add(tphId)
                                        existingWorkerData["tph_ids"] = tphIds.joinToString(",")
                                        existingWorkerData["tph_count"] = tphIds.size.toString()

                                        // Update jjg_each_blok
                                        val existingBlokMap = parseBlokMap(
                                            existingWorkerData["jjg_each_blok"]?.toString()
                                                ?: ""
                                        )

                                        // Update or add the current blok
                                        val currentBlokTO = existingBlokMap[blokName] ?: 0.0
                                        val newBlokTO = currentBlokTO + jjgTO
                                        existingBlokMap[blokName] = newBlokTO

                                        // Convert back to formatted string
                                        val formattedBlokEntries =
                                            existingBlokMap.entries.map { (blok, value) ->
                                                val formatted =
                                                    if (value == value.toInt().toDouble()) {
                                                        value.toInt().toString()
                                                    } else {
                                                        String.format(Locale.US, "%.1f", value)
                                                    }
                                                "$blok($formatted)"
                                            }

                                        // Join and store
                                        existingWorkerData["jjg_each_blok"] =
                                            formattedBlokEntries.joinToString("\n")

                                        // Update bullet point format
                                        existingWorkerData["jjg_each_blok_bullet"] =
                                            formattedBlokEntries.map { entry ->
                                                val regex =
                                                    "([A-Z0-9-]+)\\(([0-9.]+)\\)".toRegex()
                                                val matchResult = regex.find(entry)

                                                if (matchResult != null) {
                                                    val blokNameMatch =
                                                        matchResult.groupValues[1]
                                                    val count = matchResult.groupValues[2]
                                                    "• $blokNameMatch ($count Jjg)"
                                                } else {
                                                    "• $entry"
                                                }
                                            }.joinToString("\n")

                                    } else {
                                        // Create new worker entry
                                        val mutableWorkerData = workerData.toMutableMap()

                                        // Set initial values
                                        mutableWorkerData["jjg_each_blok"] =
                                            "$blokName($formattedJjgKP)"
                                        mutableWorkerData["jjg_each_blok_bullet"] =
                                            "• $blokName ($formattedJjgKP Jjg)"
                                        mutableWorkerData["jjg_total_blok"] = formattedJjgKP
                                        mutableWorkerData["jjg_dibayar"] = formattedJjgPA
                                        mutableWorkerData["occurrence_count"] = "1"
                                        mutableWorkerData["tph_ids"] = tphId
                                        mutableWorkerData["tph_count"] = "1"

                                        globalMergedWorkerMap[workerName] = mutableWorkerData
                                    }
                                }

                                // Sort and assign the final merged data
                                val finalMergedData =
                                    globalMergedWorkerMap.values.toList().sortedBy {
                                        it["nama_karyawans"].toString()
                                    }

                                mappedData = finalMergedData
                            } else if ((featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 3) || (featureName == AppUtils.ListFeatureNames.DetailESPB && currentState == 1)) {
                                val globalMergedBlokMap =
                                    mutableMapOf<String, MutableMap<String, Any>>()

                                val jjgTypes = listOf(
                                    "TO",
                                    "UN",
                                    "OV",
                                    "EM",
                                    "AB",
                                    "RA",
                                    "LO",
                                    "TI",
                                    "RI",
                                    "KP",
                                    "PA"
                                )

                                for (blokData in allWorkerData) {
                                    val blokName = blokData["blok_name"].toString()


                                    val tphId = blokData["tph_id"].toString()
                                    val jjgJson = JSONObject(blokData["jjg_json"].toString())

                                    // Extract all JJG values
                                    val jjgValues = jjgTypes.associateWith { type ->
                                        jjgJson.optDouble(type, 0.0)
                                    }

                                    val currentKaryawans = blokData["nama_karyawans"].toString()
                                    val currentKaryawansList = mutableSetOf<String>()

                                    // If there are multiple workers in this record (comma-separated)
                                    if (currentKaryawans.contains(",")) {
                                        currentKaryawansList.addAll(
                                            currentKaryawans.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() && it != "-" }
                                        )
                                    } else if (currentKaryawans.isNotEmpty() && currentKaryawans != "-") {
                                        currentKaryawansList.add(currentKaryawans)
                                    }

                                    // Same for kemandoran names
                                    val currentKemandorans =
                                        blokData["nama_kemandorans"].toString()
                                    val currentKemandoransList = mutableSetOf<String>()

                                    if (currentKemandorans.contains("\n")) {
                                        // Your kemandoran format uses bullet points with newlines
                                        currentKemandoransList.addAll(
                                            currentKemandorans.split("\n")
                                                .map { it.trim().removePrefix("• ") }
                                                .filter { it.isNotEmpty() && it != "-" }
                                        )
                                    } else if (currentKemandorans.isNotEmpty() && currentKemandorans != "-") {
                                        currentKemandoransList.add(currentKemandorans)
                                    }

                                    if (globalMergedBlokMap.containsKey(blokName)) {
                                        val existingBlokData = globalMergedBlokMap[blokName]!!

                                        val existingJjgJson =
                                            JSONObject(existingBlokData["jjg_json"].toString())

                                        // Update all JJG types in the existing JSON
                                        for (type in jjgTypes) {
                                            val existingValue =
                                                existingJjgJson.optDouble(type, 0.0)
                                            val newValue = jjgValues[type] ?: 0.0
                                            val totalValue = existingValue + newValue
                                            existingJjgJson.put(type, totalValue)
                                        }

                                        // Update the JJG JSON in the existing blok data
                                        existingBlokData["jjg_json"] =
                                            existingJjgJson.toString()

                                        // For jjg_total and jjg_dibayar, use TO and PA as in state 2
                                        val newTotalKP = existingJjgJson.optDouble("KP", 0.0)
                                        existingBlokData["jjg_total"] =
                                            if (newTotalKP == newTotalKP.toInt().toDouble()) {
                                                newTotalKP.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", newTotalKP)
                                            }

                                        val jjgPA = existingJjgJson.optDouble("PA", 0.0)
                                        existingBlokData["jjg_dibayar"] =
                                            if (jjgPA == jjgPA.toInt().toDouble()) {
                                                jjgPA.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", jjgPA)
                                            }

                                        val existingTransactions =
                                            (existingBlokData["jumlah_transaksi"]?.toString()
                                                ?.toIntOrNull() ?: 1) + 1
                                        existingBlokData["jumlah_transaksi"] =
                                            existingTransactions.toString()

                                        val tphIds =
                                            (existingBlokData["tph_ids"]?.toString()
                                                ?: "").split(",")
                                                .filter { it.isNotEmpty() }.toMutableSet()
                                        tphIds.add(tphId)
                                        existingBlokData["tph_ids"] = tphIds.joinToString(",")
                                        existingBlokData["tph_count"] = tphIds.size.toString()

                                        // Process karyawan names properly - get existing ones
                                        val existingKaryawans =
                                            (existingBlokData["nama_karyawans_all"]?.toString()
                                                ?: "").split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                                .toMutableSet()

                                        // Add all current karyawans to the set
                                        existingKaryawans.addAll(currentKaryawansList)

                                        // Update with the complete set
                                        existingBlokData["nama_karyawans_all"] =
                                            existingKaryawans.joinToString(", ")
                                        existingBlokData["karyawan_count"] =
                                            existingKaryawans.size.toString()

                                        // Process kemandoran names similarly
                                        val existingKemandorans =
                                            (existingBlokData["nama_kemandorans_all"]?.toString()
                                                ?: "").split("\n")
                                                .map { it.trim().removePrefix("• ") }
                                                .filter { it.isNotEmpty() }
                                                .toMutableSet()

                                        // Add all current kemandorans to the set
                                        existingKemandorans.addAll(currentKemandoransList)

                                        // Format kemandoran list with bullet points
                                        val formattedKemandorans = existingKemandorans
                                            .sorted()
                                            .joinToString("\n") { "• $it" }

                                        existingBlokData["nama_kemandorans_all"] =
                                            formattedKemandorans

                                        existingBlokData["jjg_each_blok"] =
                                            "${existingBlokData["jjg_total"]} (${existingBlokData["jjg_dibayar"]})"
                                    } else {
                                        // This is a new blok, create a new entry
                                        val mutableBlokData = blokData.toMutableMap()

                                        // Format the TO and PA values
                                        val jjgTO = jjgValues["TO"] ?: 0.0
                                        val formattedJjgKP =
                                            if (jjgTO == jjgTO.toInt().toDouble()) {
                                                jjgTO.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", jjgTO)
                                            }

                                        val jjgPA = jjgValues["PA"] ?: 0.0
                                        val formattedJjgPA =
                                            if (jjgPA == jjgPA.toInt().toDouble()) {
                                                jjgPA.toInt().toString()
                                            } else {
                                                String.format(Locale.US, "%.1f", jjgPA)
                                            }

                                        // Add blok-specific fields
                                        mutableBlokData["jjg_total"] = formattedJjgKP
                                        mutableBlokData["jjg_dibayar"] = formattedJjgPA
                                        mutableBlokData["jumlah_transaksi"] = "1"
                                        mutableBlokData["tph_ids"] = tphId
                                        mutableBlokData["tph_count"] = "1"
                                        mutableBlokData["jjg_each_blok"] =
                                            "$formattedJjgKP ($formattedJjgPA)"

                                        // Create worker tracking with proper handling of multiple workers
                                        mutableBlokData["nama_karyawans_all"] =
                                            currentKaryawansList.joinToString(", ")
                                        mutableBlokData["karyawan_count"] =
                                            currentKaryawansList.size.toString()

                                        // Format kemandoran list with bullet points
                                        val formattedKemandorans = currentKemandoransList
                                            .sorted()
                                            .joinToString("\n") { "• $it" }

                                        mutableBlokData["nama_kemandorans_all"] =
                                            formattedKemandorans

                                        globalMergedBlokMap[blokName] = mutableBlokData
                                    }
                                }

                                val finalMergedData =
                                    globalMergedBlokMap.values.toList().sortedBy {
                                        it["blok_name"].toString()
                                    }



                                mappedData = finalMergedData
                            } else {
                                mappedData = allWorkerData
                            }

                            val processedData =
                                AppUtils.getPanenProcessedData(originalMappedData, featureName)
                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            val blokNames = processedData["blokNames"]?.toString() ?: ""
                            blok = if (blokNames.isEmpty()) "-" else blokNames

                            listBlok.text = processedData["blokDisplay"]?.toString()
                            jjg = processedData["totalJjgCount"]?.toString()!!.toInt()
                            totalJjg.text = jjg.toString()
                            tph = processedData["tphCount"]?.toString()!!.toInt()
                            totalTPH.text = tph.toString()

                            // Set Blok
                            val tvBlok = findViewById<View>(R.id.tv_blok)
                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

                            // Set jjg
                            val tvJjg = findViewById<View>(R.id.tv_jjg)
                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                jjg.toString()

                            // Set jjg
                            val tvTph = findViewById<View>(R.id.tv_total_tph)
                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumlah TPH"
                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                tph.toString()

                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data


                        } else {


                            val emptyStateMessage =
                                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2)
                                    "Belum ada rekap data per pemanen atau pastikan sudah menyimpan/konfirmasi scan"
                                else if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 3) {
                                    "Belum ada rekap data per blok atau pastikan sudah menyimpan/konfirmasi scan"
                                } else
                                    "Tidak ada data"

                            tvEmptyState.text = emptyStateMessage
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE
                        }

                    }

                    if (filteredPanenList.size == 0 && featureName == "Rekap Hasil Panen") {
                        btnGenerateQRTPHUnl.visibility = View.GONE
//                        btnTransferBT.visibility = View.GONE
//                        tvTransferBT.visibility = View.GONE
                        tvGenQR60.visibility = View.GONE
                        tvGenQRFull.visibility = View.GONE
                        btnGenerateQRTPH.visibility = View.GONE


                    } else if (filteredPanenList.size > 0 && featureName == "Rekap Hasil Panen" && currentState != 2 && currentState != 3) {
                        btnGenerateQRTPH.visibility = View.VISIBLE
//                        btnTransferBT.visibility = View.VISIBLE
//                        tvTransferBT.visibility = View.VISIBLE
                        btnGenerateQRTPHUnl.visibility = View.GONE
                        tvGenQR60.visibility = View.VISIBLE
                        tvGenQRFull.visibility = View.VISIBLE

                        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                        headerCheckBox.visibility = View.VISIBLE
                        val flCheckBoxTableHeaderLayout =
                            findViewById<ConstraintLayout>(R.id.tableHeader)
                                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
                        flCheckBoxTableHeaderLayout.visibility = View.VISIBLE

                        if (shouldReopenLastPosition) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                val viewHolder = recyclerView.findViewHolderForAdapterPosition(
                                    lastClickedPosition
                                ) as? ListPanenTPHAdapter.ListPanenTPHViewHolder
                                viewHolder?.itemView?.performClick()
                            }, 300)
                            shouldReopenLastPosition = false
                        }
                    } else if (featureName == "Rekap Hasil Panen" && (currentState == 2 || currentState == 3)) {
                        btnGenerateQRTPHUnl.visibility = View.GONE
//                        btnTransferBT.visibility = View.GONE
//                        tvTransferBT.visibility = View.GONE
                        tvGenQR60.visibility = View.GONE
                        tvGenQRFull.visibility = View.GONE
                        btnGenerateQRTPH.visibility = View.GONE
                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                        if (filteredPanenList.size > 0) {
                            btnGenerateQRTPH.visibility = View.VISIBLE
                            tvGenQRFull.visibility = View.VISIBLE
                            btnGenerateQRTPHUnl.visibility = View.GONE
                            tvGenQR60.visibility = View.VISIBLE
                            val headerCheckBoxPanen =
                                findViewById<ConstraintLayout>(R.id.tableHeader)
                                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                            headerCheckBoxPanen.visibility = View.GONE
                        } else {
                            btnGenerateQRTPH.visibility = View.GONE
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                        // Hide all QR generation buttons for Mutu Buah

                        AppLogger.d("masuk sini gessss ")
                        btnGenerateQRTPH.visibility = View.GONE
                        btnGenerateQRTPHUnl.visibility = View.GONE
                        tvGenQR60.visibility = View.GONE
                        tvGenQRFull.visibility = View.GONE

                        val headerCheckBoxPanen = findViewById<ConstraintLayout>(R.id.tableHeader)
                            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                        headerCheckBoxPanen.visibility = View.GONE
                    } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                        val headerCheckBoxPanen =
                            findViewById<ConstraintLayout>(R.id.tableHeader)
                                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                        headerCheckBoxPanen.visibility = View.GONE
                    }

                }, 500)
            }
        }

        panenViewModel.archivedPanenList.observe(this) { panenList ->

            AppLogger.d("=== FILTERING ACTIVE PANEN LIST ===")
            AppLogger.d("Total records before filtering: ${panenList.size}")

            val filteredPanenList = panenList.filter { panenEntityWithRelations ->
                val tphDivisi = panenEntityWithRelations.tph?.divisi.toString()
                val userAfdelingId = prefManager!!.afdelingIdUserLogin
                val panenAsistensi = panenEntityWithRelations.panen.asistensi
                val panenAsistensiDivisi = panenEntityWithRelations.panen.asistensi_divisi

                AppLogger.d("Checking record: TPH ID = ${panenEntityWithRelations.panen.tph_id}")
                AppLogger.d("  TPH divisi = $tphDivisi, User afdeling = $userAfdelingId")
                AppLogger.d("  Panen asistensi = $panenAsistensi, asistensi_divisi = $panenAsistensiDivisi")

                // First check: if afdeling matches, include it
                if (tphDivisi == userAfdelingId) {
                    AppLogger.d("  ✓ INCLUDED: Afdeling matches")
                    true
                } else {
                    // If afdeling doesn't match, check if asistensi is 2 AND asistensi_divisi matches
                    val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                    if (asistensiMatch) {
                        AppLogger.d("  ✓ INCLUDED: Afdeling doesn't match but asistensi = 2 and asistensi_divisi matches")
                    } else {
                        AppLogger.d("  ✗ EXCLUDED: Afdeling doesn't match and (asistensi ≠ 2 or asistensi_divisi doesn't match)")
                    }
                    asistensiMatch
                }
            }

            if (currentState == 1 || currentState == 2) {
                btnGenerateQRTPH.visibility = View.GONE
                btnGenerateQRTPHUnl.visibility = View.GONE
                tvGenQR60.visibility = View.GONE
                tvGenQRFull.visibility = View.GONE

                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBox.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({


                    loadingDialog.dismiss()
                    lifecycleScope.launch {

                        if (filteredPanenList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            mappedData = filteredPanenList.map { panenWithRelations ->


                                if (panenWithRelations.tph == null) {
                                    AlertDialogUtility.withSingleAction(
                                        this@ListPanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        "Data TPH Tidak Ditemukan",
                                        "TPH dengan ID ${panenWithRelations.panen.tph_id} tidak ditemukan di database. Silakan periksa data TPH.",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {
                                        finish()
                                    }
                                    return@launch // Exit the entire coroutine
                                }
                                val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val pemuatData: List<KaryawanModel>? =
                                    withContext(Dispatchers.IO) {
                                        try {
                                            panenViewModel.getPemuatByIdList(pemuatList)
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
                                            panenViewModel.getKemandoranById(rawKemandoran)
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

                                mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode
                                        ?: "Unknown"), // Handle null safely
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
//                                    "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
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
                                    "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                    "nama_karyawans" to karyawanNamas as Any,
                                    "nomor_pemanen" to (panenWithRelations.panen.nomor_pemanen as Any),
                                    "nama_kemandorans" to kemandoranNamas as Any,
                                    "username" to (panenWithRelations.panen.username as Any)
                                )
                            }


                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }
                            val processedData =
                                AppUtils.getPanenProcessedData(mappedData, featureName)


                            totalTPH.text = tph.toString()
                            listBlok.text = processedData["blokDisplay"]?.toString()
                            totalJjg.text = processedData["totalJjgCount"]?.toString()
                            totalTPH.text = processedData["tphCount"]?.toString()

                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data
                        } else {
                            tvEmptyState.text = "Tidak ada data"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE

                        }

                    }
                }, 500)
            }
        }

        panenViewModel.error.observe(this) { errorMessage ->
            loadingDialog.dismiss()
            showErrorDialog(errorMessage)
        }
    }

    // Helper function to create JJG JSON for Mutu Buah data
    private fun createMutuBuahJjgJson(mutuBuah: MutuBuahEntity): String {
        return """
    {
        "PANEN": ${mutuBuah.jjgPanen},
        "MASAK": ${mutuBuah.jjgMasak},
        "MENTAH": ${mutuBuah.jjgMentah},
        "LEWAT_MASAK": ${mutuBuah.jjgLewatMasak},
        "KOSONG": ${mutuBuah.jjgKosong},
        "ABNORMAL": ${mutuBuah.jjgAbnormal},
        "SERANGAN_TIKUS": ${mutuBuah.jjgSeranganTikus},
        "PANJANG": ${mutuBuah.jjgPanjang},
        "TIDAK_VCUT": ${mutuBuah.jjgTidakVcut},
        "BAYAR": ${mutuBuah.jjgBayar},
        "KIRIM": ${mutuBuah.jjgPanen}
    }
    """.trimIndent()
    }


    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]

        val factoryAbsensiViewModel = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel =
            ViewModelProvider(this, factoryAbsensiViewModel)[AbsensiViewModel::class.java]

        val mutuBuahViewModelFactory = MutuBuahViewModel.MutuBuahViewModelFactory(application)
        mutuBuahViewModel =
            ViewModelProvider(this, mutuBuahViewModelFactory)[MutuBuahViewModel::class.java]
    }

    private fun setupSearch() {
        searchEditText = findViewById(R.id.search_feature)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                listAdapter.filterData(query)

                // Handle empty state
                if (listAdapter.itemCount == 0) {
                    tvEmptyState.text = "Tidak ada data yang dicari"
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        })
    }


    private fun showErrorDialog(errorMessage: String) {
        AlertDialogUtility.withSingleAction(
            this@ListPanenTBSActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)} ${errorMessage}",
            "warning.json",
            R.color.colorRedDark
        ) {
            finish()
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
        dateTimeCheckHandler.postDelayed(
            dateTimeCheckRunnable,
            AppUtils.DATE_TIME_INITIAL_DELAY
        )

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
        AppUtils.resetSelectedDate()
        SoundPlayer.releaseMediaPlayer()
        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun setupCheckboxControl() {
        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)

        headerCheckBox.apply {
            visibility = View.VISIBLE
            setOnCheckedChangeListener(null)
            setOnCheckedChangeListener { _, isChecked ->
                if (!isSettingUpCheckbox) {
                    listAdapter.selectAll(isChecked)
                    speedDial.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
            }
        }

        listAdapter.setOnSelectionChangedListener { selectedCount ->
            isSettingUpCheckbox = true
            headerCheckBox.isChecked = listAdapter.isAllSelected()

            speedDial.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            isSettingUpCheckbox = false
        }
    }

    fun formatGlobalDate(dateString: String): String {
        // Parse the date string in format "YYYY-MM-DD"
        val parts = dateString.split("-")
        if (parts.size != 3) return dateString // Return original if format doesn't match

        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        // Return formatted date string using getMonthFormat
        return "${AppUtils.getMonthFormat(month)} $day $year"
    }

    fun generateHighQualityQRCode(
        content: String,
        imageView: ImageView,
        context: Context,
        sizePx: Int = 1000,
        foregroundColorRes: Int? = R.color.black, // Optional: use custom color or default
        backgroundColorRes: Int? = R.color.white, // Optional: use custom color or default
        showLogo: Boolean = false, // Toggle to show/hide logo (default: no logo)
        logoRes: Int = R.drawable.cbi, // Default logo resource
        logoSizeRatio: Float = 0.12f // Logo size as ratio of QR code (12% by default)
    ) {
        try {
            // Get colors from colors.xml or use defaults
            val foregroundColor = foregroundColorRes?.let {
                ContextCompat.getColor(context, it)
            } ?: Color.BLACK // Default black if not specified

            val backgroundColor = backgroundColorRes?.let {
                ContextCompat.getColor(context, it)
            } ?: Color.WHITE // Default white if not specified

            // Create encoding hints for better quality
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.M
                ) // Change from H to M
                put(EncodeHintType.MARGIN, 3) // Increase from 1 to 3
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            // Create QR code writer with hints
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            // batas 2,7 kb json

            // Create bitmap with appropriate size
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Fill the bitmap with colors from colors.xml
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) foregroundColor else backgroundColor
                    )
                }
            }

            // Add logo if enabled
            if (showLogo) {
                val finalBitmap = addLogoToQRCode(bitmap, context, logoRes, logoSizeRatio)

                // Set the bitmap to ImageView with high quality scaling
                imageView.apply {
                    setImageBitmap(finalBitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            } else {
                // No logo, just set the QR code
                imageView.apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addLogoToQRCode(
        qrBitmap: Bitmap,
        context: Context,
        logoRes: Int,
        logoSizeRatio: Float
    ): Bitmap {
        try {
            // Create a mutable copy of the QR code bitmap
            val combinedBitmap = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(combinedBitmap)

            // Load and resize logo
            val logoDrawable = ContextCompat.getDrawable(context, logoRes)
            logoDrawable?.let { drawable ->

                // Calculate logo size
                val qrSize = qrBitmap.width
                val logoSize = (qrSize * logoSizeRatio).toInt()

                // Calculate center position
                val left = (qrSize - logoSize) / 2
                val top = (qrSize - logoSize) / 2
                val right = left + logoSize
                val bottom = top + logoSize

                // Create white background circle for logo (optional)
                val paint = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                }
                val radius = logoSize / 2f
                val centerX = qrSize / 2f
                val centerY = qrSize / 2f
                canvas.drawCircle(centerX, centerY, radius + 6, paint)

                // Draw logo
                drawable.setBounds(left, top, right, bottom)
                drawable.draw(canvas)
            }

            return combinedBitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return qrBitmap // Return original if logo addition fails
        }
    }


    private val STORAGE_PERMISSION_CODE = 101
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    // Check permissions
    private fun checkStoragePermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request permissions
    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            STORAGE_PERMISSION_CODE
        )
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, can proceed with screenshot
                AppLogger.d("Storage permissions granted")
            } else {
                // Permissions denied
                Toast.makeText(
                    this,
                    "Izin penyimpanan dibutuhkan untuk menyimpan screenshot",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun makeQRLayoutSquare(screenshotLayout: View) {
        val qrLayout = screenshotLayout.findViewById<FrameLayout>(R.id.fLayoutQR)

        // Get screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate square size (80% of screen width with padding)
        val padding = (32 * resources.displayMetrics.density).toInt() // 32dp padding
        val squareSize = screenWidth - padding

        // Set equal width and height
        val layoutParams = qrLayout.layoutParams
        layoutParams.width = squareSize
        layoutParams.height = squareSize
        qrLayout.layoutParams = layoutParams
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

                makeQRLayoutSquare(screenshotLayout)

                // Get references to views in the custom layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView =
                    screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
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
                val capitalizedFeatureName =
                    featureName!!.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }

                tvUserName.text =
                    "Hasil QR ${capitalizedFeatureName} dari ${prefManager!!.jabatanUserLogin} - ${prefManager!!.estateUserLogin}"

                if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                    val infoNoESPB = screenshotLayout.findViewById<View>(R.id.infoNoESPB)
                    val infoDriver = screenshotLayout.findViewById<View>(R.id.infoDriver)
                    val infoNopol = screenshotLayout.findViewById<View>(R.id.infoNopol)
                    val infoPemuat = screenshotLayout.findViewById<View>(R.id.infoPemuat)

                    infoNoESPB.visibility = View.VISIBLE
                    infoDriver.visibility = View.VISIBLE
                    infoNopol.visibility = View.VISIBLE
                    infoPemuat.visibility = View.VISIBLE

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
                    setInfoData(infoNoESPB, "E-SPB", ": $no_espb")
                    setInfoData(infoDriver, "Driver", ": $driver")
                    setInfoData(infoNopol, "Nomor Polisi", ": $nopol")
                    setInfoData(infoPemuat, "Pemuat", ": $pemuatNamaESPB")

                    // Add new info data for DetailESPB
                    setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                    setInfoData(
                        infoJamTanggal,
                        "Jam & Tanggal",
                        ": $formattedDate, $formattedTime"
                    )

                } else {
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

                    // Add new info data for other features
                    setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                    setInfoData(
                        infoJamTanggal,
                        "Jam & Tanggal",
                        ": $formattedDate, $formattedTime"
                    )
                }

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

                // Create a meaningful filename
                val screenshotFileName = if (featureName == "Detail eSPB") {
                    "eSPB_QR"
                } else {
                    "Panen_QR"
                }

                val watermarkType =
                    if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                        AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
                    } else if (featureName == AppUtils.ListFeatureNames.TransferHektarPanen) {
                        AppUtils.WaterMarkFotoDanFolder.WMTransferHektarPanen
                    } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                        AppUtils.WaterMarkFotoDanFolder.WMESPB
                    } else if (featureName == AppUtils.ListFeatureNames.AbsensiPanen) {
                        AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                        AppUtils.WaterMarkFotoDanFolder.WMRekapPanenDanRestan
                    } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
                        AppUtils.ListFeatureNames.RekapMutuBuah.uppercase().replace(" ", "_")
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
                        this@ListPanenTBSActivity,
                        "QR sudah tersimpan digaleri",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                }
            } catch (e: Exception) {
                AppLogger.e("Error taking QR screenshot: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ListPanenTBSActivity,
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

    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark),
            function = {
                loadingDialog.show()
                loadingDialog.setMessage("Deleting items...")

                panenViewModel.deleteMultipleItems(selectedItems)

                // Observe delete result
                panenViewModel.deleteItemsResult.observe(this) { isSuccess ->
                    loadingDialog.dismiss()
                    if (isSuccess) {
                        playSound(R.raw.data_terhapus)
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload data based on current state
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, true, 0, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, true, 0, globalFormattedDate)
                            panenViewModel.countTPHESPB(1, 0, true, 0, globalFormattedDate)
                        } else {
                            panenViewModel.loadArchivedPanen()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_failed_delete)} data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Reset UI state
                    val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                        .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                    headerCheckBox.isChecked = false
                    listAdapter.clearSelections()
                    speedDial.visibility = View.GONE
                }

                // Observe errors
                panenViewModel.error.observe(this) { errorMessage ->
                    loadingDialog.dismiss()
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        ) {

        }
    }


    private fun setupSpeedDial() {
        speedDial = findViewById(R.id.dial_tph_list)

        if (featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen) {
            speedDial.visibility = View.GONE
        } else {
            speedDial.visibility = View.VISIBLE
        }

        speedDial.apply {
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.scan_qr, R.drawable.baseline_qr_code_scanner_24)
//                    .setLabel(getString(R.string.generate_qr))
//                    .setFabBackgroundColor(
//                        ContextCompat.getColor(
//                            this@ListPanenTBSActivity,
//                            R.color.yellowbutton
//                        )
//                    )
//                    .create()
//            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.deleteSelected,
                    R.drawable.baseline_delete_forever_24
                )
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListPanenTBSActivity,
                            R.color.colorRedDark
                        )
                    )
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.scan_qr -> {
//
                        true
                    }
//
                    R.id.deleteSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()

//
                        true
                    }

                    else -> false
                }
            }
        }


    }

    fun Int.toPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun initializeFilterViews() {
        filterSection = findViewById(R.id.filterSection)
        filterName = findViewById(R.id.filterName)
        removeFilter = findViewById(R.id.removeFilter)

        // Initially hide the filter section
        filterSection.visibility = View.GONE
    }

    private fun setupSortButton() {
        sortButton = findViewById(R.id.btn_sort)
        updateSortIcon() // Set initial icon state

        sortButton.setOnClickListener {
            // Store original data order if this is the first sort
            if (originalData.isEmpty()) {
                originalData = listAdapter.getCurrentData()
            }

            isAscendingOrder = !isAscendingOrder
            updateSortIcon() // Update icon on click

            listAdapter.sortData(isAscendingOrder)
            listAdapter.sortByCheckedItems(false)
            updateFilterDisplay()
        }

        setupRemoveFilter()
    }


    private fun updateFilterDisplay() {
        filterSection.visibility = View.VISIBLE
        filterName.text =
            if (isAscendingOrder) "Urutan Nomor TPH Kecil - Besar" else "Urutan Nomor TPH Besar - Kecil"
    }


    private fun setupRemoveFilter() {
        removeFilter.setOnClickListener {
            // Get current search query
            val currentSearchQuery = searchEditText.text.toString().trim()

            // Reset sort state
            isAscendingOrder = true
            updateSortIcon()

            if (originalData.isNotEmpty()) {
                // Reset the sort but maintain the filter
                listAdapter.resetSort()
                if (currentSearchQuery.isNotEmpty()) {
                    listAdapter.filterData(currentSearchQuery)
                }
                originalData = emptyList()
            }

            // Hide filter section
            filterSection.visibility = View.GONE
        }
    }

//    @SuppressLint("MissingSuperCall")
//    override fun onBackPressed() {
//        vibrate()
////        AlertDialogUtility.withTwoActions(
////            this,
////            "Simpan",
////            getString(R.string.confirmation_dialog_title),
////            getString(R.string.al_confirm_feature),
////            "warning.json"
////        ) {
//        val intent = Intent(this, HomePageActivity::class.java)
//        startActivity(intent)
//        finishAffinity()
////        }
//
//    }

    private fun updateSortIcon() {
        sortButton.animate()
            .scaleY(if (isAscendingOrder) 1f else -1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }


    private fun setupHeader() {
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

    private fun parseBlokMap(blokString: String): MutableMap<String, Double> {
        val result = mutableMapOf<String, Double>()
        if (blokString.isEmpty()) return result

        blokString.split("\n").forEach { line ->
            val regex = "([A-Z0-9-]+)\\(([0-9,.]+)\\)".toRegex()
            val matchResult = regex.find(line)

            if (matchResult != null) {
                val blokName = matchResult.groupValues[1]
                // Handle both comma and period as decimal separators
                val countStr = matchResult.groupValues[2].replace(",", ".")
                val count = countStr.toDoubleOrNull() ?: 0.0
                result[blokName] = count
            }
        }

        return result
    }

    private fun setupRecyclerView() {
        val totalSection: LinearLayout = findViewById(R.id.total_section)
        val blokSection: LinearLayout = findViewById(R.id.blok_section)
        val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
        val totalTphTextView: TextView = findViewById(R.id.totalTPH)
        val tvTotalTPH: TextView = findViewById(R.id.tvTotalTPH)
        val listBlokTextView: TextView = findViewById(R.id.listBlok) // Add this line
        val titleTotalJjg: TextView = findViewById(R.id.titleTotalJjg)
        val headers = if (featureName == "Buat eSPB") {
            listOf("BLOK", "NO TPH/\nJJG KIRIM", "JAM", "KP")
        } else if (featureName == "Detail eSPB") {
            listOf("BLOK", "NO TPH", "KIRIM PABRIK", "JAM")
        } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
            listOf("BLOK", "NO TPH", "JJG PANEN", "JAM")
        } else {
            listOf("BLOK", "NO TPH", "KIRIM PABRIK", "JAM")
        }
        updateTableHeaders(headers)

        listAdapter = ListPanenTPHAdapter()

        listAdapter.setOnDataRefreshCallback { position ->

            loadingDialog.show()
            loadingDialog.setMessage("Sedang mengambil data...", true)
            AppLogger.d(position.toString())
            panenViewModel.loadTPHNonESPB(0, 0, true, 0, globalFormattedDate)
            lastClickedPosition = position
            shouldReopenLastPosition = true
        }

        recyclerView.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
        }

        if (shouldRestoreCheckboxState) {
            AppLogger.d("Restoring checkbox state from previous session")
            extractPreviousSelections()
            clearTphData()
            // ADD THIS: Set tphListScan for checkbox restoration
            tphListScan = previouslySelectedTphIds.toList()
        } else {
            restorePreviousTphData()
            // ADD THIS: Set tphListScan for normal scanning
            tphListScan = processScannedResult(listTPHDriver)
        }

        if (tphListScan.isEmpty() && !shouldRestoreCheckboxState) {
            Toast.makeText(this, "Failed to process TPH QR", Toast.LENGTH_SHORT).show()
        } else {
            AppLogger.d(tphListScan.toString())
            AppLogger.d("Setting feature and scanned TPHs: ${tphListScan.size} items")
            listAdapter.setFeatureAndScanned(
                featureName,
                tphListScan,
                shouldRestoreCheckboxState
            )
        }

        if (tphListScan.isEmpty() && !shouldRestoreCheckboxState) {
            Toast.makeText(this, "Failed to process TPH QR", Toast.LENGTH_SHORT).show()
        } else {
            AppLogger.d("Setting feature and scanned TPHs: ${tphListScan.size} items")
            listAdapter.setFeatureAndScanned(
                featureName,
                tphListScan,
                shouldRestoreCheckboxState
            )
        }

        if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
            listAdapter.setOnTotalsUpdateListener { tphCount, jjgCount, formattedBlocks ->
                if (tphCount > 0) {
                    totalSection.visibility = View.VISIBLE
                    blokSection.visibility = View.VISIBLE
                    totalTphTextView.text = tphCount.toString()
                    totalJjgTextView.text = jjgCount.toString()
                    tvTotalTPH.text = "Jmlh Transaksi: "
                    titleTotalJjg.text = "Kirim Pabrik: "

                    // No need to format again, just join the already formatted blocks
                    val blocksText = formattedBlocks.joinToString(", ")
                    listBlokTextView.text = blocksText
                    listBlokTextView.visibility = View.VISIBLE
                } else {
                    totalSection.visibility = View.GONE
                    blokSection.visibility = View.GONE
                    listBlokTextView.visibility = View.GONE
                }
            }
        } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
            titleTotalJjg.text = "Kirim Pabrik: "
        } else if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
            titleTotalJjg.text = "Panen: "
        } else {
            titleTotalJjg.text = "Kirim Pabrik: "
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

    private fun fetchAndMergeTPHData(espbTph1: String) {
        AppLogger.d("ESPB TPH1 data: $espbTph1")

        // Use lifecycleScope to handle suspend function
        lifecycleScope.launch {
            try {
                // Parse espb.tph1 to get list of TPH items that should be checked (NOW SUSPEND)
                val espbTphList = parseTPH1Data(espbTph1)
                AppLogger.d("ESPB TPH list: ${espbTphList.size} items")

                // Fetch all available TPH data
                panenViewModel.getAllPanenDataDetailESPB(0, 0, true, 1, null)

                delay(200)

                panenViewModel.detailNonESPBTPH.observeOnce(this@ListPanenTBSActivity) { panenWithRelationsList ->

                    AppLogger.d("=== FILTERING PANEN WITH RELATIONS LIST ===")
                    AppLogger.d("Total records before filtering: ${panenWithRelationsList?.size ?: 0}")

                    if (panenWithRelationsList != null) {

                        val filteredPanenList = panenWithRelationsList.filter { panenWithRelations ->
                            val tphDivisi = panenWithRelations.tph?.divisi.toString()
                            val userAfdelingId = prefManager!!.afdelingIdUserLogin
                            val panenAsistensi = panenWithRelations.panen.asistensi
                            val panenAsistensiDivisi = panenWithRelations.panen.asistensi_divisi

                            AppLogger.d("Checking record: TPH ID = ${panenWithRelations.panen.tph_id}")
                            AppLogger.d("  TPH divisi = $tphDivisi, User afdeling = $userAfdelingId")
                            AppLogger.d("  Panen asistensi = $panenAsistensi, asistensi_divisi = $panenAsistensiDivisi")

                            // First check: if afdeling matches, include it
                            if (tphDivisi == userAfdelingId) {
                                AppLogger.d("  ✓ INCLUDED: Afdeling matches")
                                true
                            } else {
                                // If afdeling doesn't match, check if asistensi is 2 AND asistensi_divisi matches
                                val asistensiMatch = panenAsistensi == 2 && panenAsistensiDivisi.toString() == userAfdelingId
                                if (asistensiMatch) {
                                    AppLogger.d("  ✓ INCLUDED: Afdeling doesn't match but asistensi = 2 and asistensi_divisi matches")
                                } else {
                                    AppLogger.d("  ✗ EXCLUDED: Afdeling doesn't match and (asistensi ≠ 2 or asistensi_divisi doesn't match)")
                                }
                                asistensiMatch
                            }
                        }

                        AppLogger.d("Total records after filtering: ${filteredPanenList.size}")
                        AppLogger.d("=== FILTERING COMPLETE ===")

                        AppLogger.d("Fetched ${filteredPanenList.size} available TPH items after filtering")

                        // Convert filtered TPH data to TPHItem list
                        val availableTphList = filteredPanenList.map { panenWithRelations ->

                            val kpNumber = try {
                                val jjgJson = panenWithRelations.panen.jjg_json ?: ""
                                if (jjgJson.startsWith("{") && jjgJson.contains("KP")) {
                                    val gson = Gson()
                                    val jsonObject = gson.fromJson(jjgJson, JsonObject::class.java)
                                    jsonObject.get("KP")?.asString ?: jjgJson
                                } else {
                                    // If it's not JSON, use as is
                                    jjgJson
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error parsing JJG JSON for TPH ${panenWithRelations.panen.tph_id}: ${e.message}")
                                panenWithRelations.panen.jjg_json ?: "" // Fallback to original value
                            }

                            TPHItem(
                                tphId = panenWithRelations.panen.tph_id.toString(),
                                dateCreated = panenWithRelations.panen.date_created ?: "",
                                jjgJson = kpNumber,
                                tphNomor = panenWithRelations.tph!!.nomor.toString(),
                                isChecked = false,
                                blokKode = panenWithRelations.tph!!.blok_kode.toString(),
                                nomorPemanen = panenWithRelations.panen!!.nomor_pemanen.toString(),
                            )
                        }

                        AppLogger.d("availableTphList after filtering: $availableTphList")

                        // Create a set of TPH IDs from ESPB for quick lookup
                        val espbTphIds = espbTphList.map { it.tphId }.toSet()
                        AppLogger.d("ESPB TPH IDs to be checked: $espbTphIds")

                        // Merge the lists: combine available TPH with ESPB TPH
                        val mergedTphList = mergeTPHLists(availableTphList, espbTphList, espbTphIds)

                        AppLogger.d("Final merged list: ${mergedTphList.size} items")
                        mergedTphList.forEachIndexed { index, item ->
                            AppLogger.d("Merged Item $index: TPH ID=${item.tphId}, Checked=${item.isChecked}, Source=${if (item.tphId in espbTphIds) "ESPB" else "Available"}")
                        }

                        // Show bottom sheet with merged data
                        showDetailESPBTPHBottomSheet(mergedTphList)

                    } else {
                        AppLogger.e("Failed to fetch available TPH data")
                        Toast.makeText(
                            this@ListPanenTBSActivity,
                            "Gagal mengambil data TPH",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                AppLogger.e("Error in fetchAndMergeTPHData: ${e.message}")
                Toast.makeText(
                    this@ListPanenTBSActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }


    private fun mergeTPHLists(
        availableTphList: List<TPHItem>,
        espbTphList: List<TPHItem>,
        espbTphIds: Set<String>
    ): List<TPHItem> {
        val mergedList = mutableListOf<TPHItem>()

//        AppLogger.d("=== MERGE DEBUG ===")
//        AppLogger.d("Available TPH records: ${availableTphList.size}")
//        availableTphList.forEach {
//            AppLogger.d("Available Record: TPH=${it.tphId}, KP=${it.jjgJson}, Date=${it.dateCreated}, Nomor=${it.tphNomor}")
//        }
//
//        AppLogger.d("ESPB TPH records: ${espbTphList.size}")
//        espbTphList.forEach {
//            AppLogger.d("ESPB Record: TPH=${it.tphId}, KP=${it.jjgJson}, Date=${it.dateCreated}")
//        }

        // Add ALL ESPB items (they are separate harvest records, not duplicates)
        for (espbTph in espbTphList) {
            // Get TPH metadata from database if needed
            val tphMetadata = if (espbTph.tphNomor.isEmpty()) {
                // Try to find metadata from available items with same tph_id
                availableTphList.find { it.tphId == espbTph.tphId }
            } else null

            mergedList.add(
                espbTph.copy(
                    isChecked = true, // ESPB records are checked by default
                    isFromESPB = true,
                    tphNomor = tphMetadata?.tphNomor ?: espbTph.tphNomor,
                    blokKode = tphMetadata?.blokKode ?: espbTph.blokKode
                )
            )
//            AppLogger.d("Added ESPB Record: TPH=${espbTph.tphId}, KP=${espbTph.jjgJson}, Checked=true")
        }


        for (availableTph in availableTphList) {
            mergedList.add(
                availableTph.copy(
                    isChecked = false, // Available records are unchecked by default
                    isFromESPB = false
                )
            )
//            AppLogger.d("Added Available Record: TPH=${availableTph.tphId}, KP=${availableTph.jjgJson}, Checked=false")
        }

//        AppLogger.d("=== MERGE RESULT ===")
//        AppLogger.d("Total merged records: ${mergedList.size}")
//        mergedList.forEachIndexed { index, item ->
//            AppLogger.d("Result $index: TPH=${item.tphId}, KP=${item.jjgJson}, Date=${item.dateCreated}, Nomor=${item.tphNomor}, Checked=${item.isChecked}, Source=${if (item.isFromESPB) "ESPB" else "Available"}")
//        }

        return mergedList
    }


    private suspend fun parseTPH1Data(tph1Data: String): List<TPHItem> {
        val tphItemList = mutableListOf<TPHItem>()

        if (!tph1Data.isNullOrEmpty()) {
            AppLogger.d("Parsing TPH1 data: $tph1Data")

            // Split by semicolon to get individual TPH records
            val tphRecords = tph1Data.split(";")
            AppLogger.d("Found ${tphRecords.size} TPH records in ESPB data")

            for (tphRecord in tphRecords) {
                if (tphRecord.isNotEmpty()) {
                    // Split each record by comma
                    val parts = tphRecord.split(",")

                    if (parts.size >= 5) {
                        val tphId = parts[0].trim()
                        val dateCreated = parts[1].trim()
                        val kpValue = parts[2].trim()
                        val status = parts[3].trim()
                        val nomorPemanen = parts[4].trim()

                        val kpNumber = try {
                            if (kpValue.startsWith("{") && kpValue.contains("KP")) {
                                // Parse JSON: {"PA": 18} -> "18"
                                val gson = Gson()
                                val jsonObject = gson.fromJson(kpValue, JsonObject::class.java)
                                jsonObject.get("KP")?.asString ?: kpValue
                            } else {
                                // If it's not JSON, use as is
                                kpValue
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error parsing KP JSON: ${e.message}")
                            kpValue // Fallback to original value
                        }

                        // Use withContext to run on IO dispatcher for database operations
                        val tphBlokInfo = withContext(Dispatchers.IO) {
                            panenViewModel.getTPHAndBlokInfo(tphId.toInt())
                        }

                        tphItemList.add(
                            TPHItem(
                                tphId = tphId,
                                dateCreated = dateCreated,
                                jjgJson = kpNumber,
                                tphNomor = tphBlokInfo?.tphNomor
                                    ?: "", // Use data from database if available
                                isChecked = false, // Will be set to true during merge
                                blokKode = tphBlokInfo!!.blokKode,
                                nomorPemanen = nomorPemanen
                            )
                        )

                        AppLogger.d("Parsed ESPB TPH: ID=$tphId, Date=$dateCreated, KP=$kpValue, Status=$status")
                    }
                }
            }
        }

        AppLogger.d("Total parsed ESPB TPH items: ${tphItemList.size}")
        return tphItemList
    }


    private fun processSelectedTPHItems(
        checkedItems: List<TPHItem>,
        uncheckedItems: List<TPHItem>
    ) {
        AppLogger.d("Processing TPH changes:")
        AppLogger.d("Items to ADD to ESPB (${checkedItems.size}):")
        checkedItems.forEach { item ->
            AppLogger.d("  + TPH ID: ${item.tphId}, Nomor: ${item.tphNomor}")
        }

        AppLogger.d("Items to REMOVE from ESPB (${uncheckedItems.size}):")
        uncheckedItems.forEach { item ->
            AppLogger.d("  - TPH ID: ${item.tphId}, Nomor: ${item.tphNomor}")
        }

        // Your logic to update the ESPB with new TPH selection
    }

    private fun convertTPHItemsToTph1String(tphItems: List<TPHItem>): String {
        return tphItems.filter { it.isChecked }.joinToString(";") { item ->
            AppLogger.d("Converting: TPH=${item.tphId}, Date=${item.dateCreated}, KP=${item.jjgJson}, NomorPemanen=${item.nomorPemanen}")
            // Format: tphId,dateCreated,kpValue,status,nomor_pemanen
            "${item.tphId},${item.dateCreated},${item.jjgJson},1,${item.nomorPemanen}"
        }
    }

    private fun showDetailESPBTPHBottomSheet(tphItemList: List<TPHItem>) {
        val view =
            layoutInflater.inflate(
                R.layout.layout_bottom_sheet_tambah_hapus_tph_detail_epsb,
                null
            )
        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
        dialog.setContentView(view)

        // Setup RecyclerView and content FIRST (before configuring behavior)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewTPHListDetailESPB)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTPH)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelTPH)

        val adapter =
            detailESPBListTPHAdapter(tphItemList.toMutableList()) { tphItem, isChecked ->
                AppLogger.d("TPH ${tphItem.tphId} is ${if (isChecked) "checked" else "unchecked"}")
            }

        recyclerView?.let { rv ->
            rv.layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
            rv.adapter = adapter
        }

        btnSave?.setOnClickListener {
            val checkedItems = adapter.getCheckedItems()

            AppLogger.d("checkedItems $checkedItems")
            val newTph1String = convertTPHItemsToTph1String(checkedItems)

            AppLogger.d("Original TPH1: $tph1")
            AppLogger.d("New TPH1: $newTph1String")
            AppLogger.d("noespb $noespb")
            AlertDialogUtility.withTwoActions(
                this@ListPanenTBSActivity,
                "Submit",
                stringXML(R.string.confirmation_dialog_title),
                stringXML(R.string.al_submit_upload_data_espb_by_krani_timbang),
                "warning.json",
                function = {
                    val checkedItems = adapter.getCheckedItems()

                    if (checkedItems.isNotEmpty()) {
                        val newTph1String = convertTPHItemsToTph1String(checkedItems)

                        AppLogger.d("Original TPH1: $tph1")
                        AppLogger.d("New TPH1: $newTph1String")

                        lifecycleScope.launch {
                            try {
                                val originalRecords = tph1.split(";").filter { it.isNotEmpty() }
                                val newRecords = newTph1String.split(";").filter { it.isNotEmpty() }

                                AppLogger.d("=== CORRECT TPH PROCESSING ===")
                                AppLogger.d("Original records count: ${originalRecords.size}")
                                AppLogger.d("New records count: ${newRecords.size}")

                                // STEP 1: RESET only REMOVED records (records in original but NOT in new)
                                AppLogger.d("--- RESETTING REMOVED Records (Original but NOT in New) ---")
                                var resetCount = 0

                                for (originalRecord in originalRecords) {
                                    if (!newRecords.contains(originalRecord)) {
                                        // This record was REMOVED - reset it
                                        resetCount++
                                        AppLogger.d("Resetting REMOVED record #$resetCount: $originalRecord")

                                        val parts = originalRecord.split(",")
                                        if (parts.size >= 5) {
                                            val tphId = parts[0].trim()
                                            val dateCreated = parts[1].trim()
                                            val kpValue = parts[2].trim()
                                            val nomorPemanen = parts[4].trim()

                                            val jsonKp = """{"KP": $kpValue}"""

                                            AppLogger.d("RESETTING REMOVED - TPH: $tphId, Date: $dateCreated")

                                            val resetResult = panenViewModel.resetEspbStatus(
                                                tphId,
                                                dateCreated,
                                                jsonKp,
                                                nomorPemanen
                                            )
                                            AppLogger.d("Reset result: $resetResult rows affected")
                                        } else {
                                            AppLogger.e("Invalid removed record format: $originalRecord")
                                        }
                                    } else {
                                        AppLogger.d("Keeping existing record (also in new): $originalRecord")
                                    }
                                }
                                AppLogger.d("Total REMOVED records reset: $resetCount")

                                // STEP 2: SET ALL NEW records (set ESPB status for all records in new TPH1)
                                AppLogger.d("--- SETTING ALL NEW Records ---")
                                var setCount = 0

                                for (newRecord in newRecords) {
                                    setCount++
                                    AppLogger.d("Setting record #$setCount: $newRecord")

                                    val parts = newRecord.split(",")
                                    if (parts.size >= 5) {
                                        val tphId = parts[0].trim()
                                        val dateCreated = parts[1].trim()
                                        val kpValue = parts[2].trim()
                                        val nomorPemanen = parts[4].trim()

                                        val jsonKp = """{"KP": $kpValue}"""

                                        if (originalRecords.contains(newRecord)) {
                                            AppLogger.d("SETTING EXISTING - TPH: $tphId, Date: $dateCreated, NoESPB: $no_espb")
                                        } else {
                                            AppLogger.d("SETTING NEW - TPH: $tphId, Date: $dateCreated, NoESPB: $no_espb")
                                        }

                                        val setResult = panenViewModel.setEspbStatus(
                                            tphId,
                                            dateCreated,
                                            jsonKp,
                                            nomorPemanen,
                                            no_espb
                                        )
                                        AppLogger.d("Set result: $setResult rows affected")
                                    } else {
                                        AppLogger.e("Invalid new record format: $newRecord")
                                    }
                                }
                                AppLogger.d("Total records set: $setCount")

                                val newBlokJjg = calculateBlokJjgFromTph1(newTph1String)
                                val updateResult = withContext(Dispatchers.IO) {
                                    espbViewModel.updateTPH1AndBlokJjg(noespb, newTph1String, newBlokJjg)
                                }

                                if (updateResult > 0) {
                                    AppLogger.d("✅ All updates successful")
                                    Toast.makeText(
                                        this@ListPanenTBSActivity,
                                        "TPH berhasil diperbarui",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    tph1 = newTph1String
                                    blok_jjg = newBlokJjg
                                    panenViewModel.getAllPanenWhereESPB(noespb)
                                    delay(100)

                                } else {
                                    AppLogger.e("Failed to update TPH1")
                                    Toast.makeText(
                                        this@ListPanenTBSActivity,
                                        "Gagal memperbarui TPH",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error updating TPH1: ${e.message}")
                                Toast.makeText(
                                    this@ListPanenTBSActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        dialog.dismiss()
                    }else {
                        Toast.makeText(
                            this@ListPanenTBSActivity,
                            "Pilih minimal satu TPH",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

        }


        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        // NOW configure the bottom sheet behavior (EXACT same pattern as your working example)
        val maxHeight =
            (resources.displayMetrics.heightPixels * 0.9).toInt() // You can adjust this

        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    this.peekHeight = maxHeight  // Set the initial height when peeking
                    this.state = BottomSheetBehavior.STATE_EXPANDED  // Start fully expanded
                    this.isFitToContents =
                        true  // Content will determine the height (up to maxHeight)
                    this.isDraggable = false  // Prevent user from dragging the sheet
                }

                // Set a fixed height for the bottom sheet
                bottomSheet.layoutParams?.height = maxHeight
            }

        // Show dialog LAST (same as your working pattern)
        dialog.show()

        adapter.isLoadingComplete.observe(this@ListPanenTBSActivity) { isComplete ->
            if (isComplete) {
                AppLogger.d("✅ Adapter loading complete - dismissing loading dialog")
                loadingDialog?.dismiss()

                // Remove observer to prevent multiple calls
                adapter.isLoadingComplete.removeObservers(this@ListPanenTBSActivity)
            }
        }
    }

    private suspend fun calculateBlokJjgFromTph1(tph1String: String): String {
        return try {
            AppLogger.d("=== CALCULATING BLOK_JJG ===")
            AppLogger.d("Input TPH1: $tph1String")

            // Extract TPH records with JJG values
            val tphRecords =
                mutableListOf<Triple<String, String, Int>>() // tphId, blokId, jjgValue

            if (tph1String.isNotEmpty()) {
                tph1String.split(";").forEach { record ->
                    if (record.isNotEmpty()) {
                        val parts = record.split(",")
                        if (parts.size >= 3) {
                            val tphId = parts[0].trim()
                            val jjgValue = parts[2].trim().toIntOrNull() ?: 0

                            // Get blok_ppro info for this TPH ID
                            val tphBlokPproInfo = withContext(Dispatchers.IO) {
                                panenViewModel.getTPHBlokPpro(tphId.toInt())
                            }

                            if (tphBlokPproInfo != null) {
                                tphRecords.add(Triple(tphId, tphBlokPproInfo.blok_ppro.toString(), jjgValue))
                                AppLogger.d("TPH $tphId -> Blok Ppro ${tphBlokPproInfo.blok_ppro} -> JJG $jjgValue")
                            }
                        }
                    }
                }
            }

            // Group by blokId and sum JJG values
            val blokJjgMap = tphRecords
                .groupBy { it.second } // Group by blokId
                .mapValues { entry ->
                    entry.value.sumOf { it.third } // Sum JJG values
                }

            AppLogger.d("Grouped blok sums: $blokJjgMap")

            // Create blok_jjg string: "blokId,totalJJG;blokId,totalJJG"
            val blokJjgString = blokJjgMap
                .map { (blokId, totalJjg) -> "$blokId,$totalJjg" }
                .joinToString(";")

            AppLogger.d("Final blok_jjg: $blokJjgString")
            return blokJjgString

        } catch (e: Exception) {
            AppLogger.e("Error calculating blok_jjg: ${e.message}")
            e.printStackTrace()
            return ""
        }
    }


    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tableHeader)

        // Adjust the header ID list to accommodate 5 columns if needed
        val headerIds = if (headerNames.size == 5) {
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

        } else {
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4)
        }

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }

        // Hide extra columns if not used (only applicable when switching from 5 to 4)
        if (headerNames.size < headerIds.size) {
            for (i in headerNames.size until headerIds.size) {
                tableHeader.findViewById<TextView>(headerIds[i]).visibility = View.GONE
            }
        }
    }

    private fun clearTphData() {
        tph1 = ""
        tph0 = ""
        tph1IdPanen = ""
    }

    fun processScannedResult(scannedResult: String): List<String> {
        // First check if it's already a list of IDs
        if (scannedResult.startsWith("[") && !scannedResult.contains("tph_0")) {
            return try {
                // This handles the case: [172355, 172357, 102354, ...]
                val listString = scannedResult.trim('[', ']')
                listString.split(", ").map { it.trim() }
            } catch (e: Exception) {
                Log.e("ListPanenTBSActivity", "Error parsing list format: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }

        if (scannedResult.contains("tph_0")) {
            Log.e("ListPanenTBSActivity", "Invalid data format containing tph_0")
            return emptyList() // Return null to indicate invalid format
        }
        // Default case - try the original parsing method
        return try {
            val tphString = scannedResult
                .removePrefix("""{"tph":"""")
                .removeSuffix(""""}""")
            tphString.split(";")
        } catch (e: Exception) {
            Log.e("ListPanenTBSActivity", "Error with default parsing: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }


    private fun removeDuplicateEntries(entries: String): String {
        if (entries.isEmpty()) return ""

        val uniqueEntries = entries.split(";")
            .filter { it.isNotEmpty() }
            .filter { entry ->
                val parts = entry.split(",")
                // Keep entries where type field (index 3) is not "0"
                parts.size < 4 || parts[3] != "0"
            }
            // ✅ Deduplicate only by the first 3 fields
            .distinctBy { entry ->
                val parts = entry.split(",")
                if (parts.size >= 3) "${parts[0]},${parts[1]},${parts[2]}" else entry
            }
            .joinToString(";")

        return uniqueEntries
    }

}
