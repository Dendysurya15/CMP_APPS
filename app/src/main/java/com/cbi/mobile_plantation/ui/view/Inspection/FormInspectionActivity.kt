package com.cbi.mobile_plantation.ui.view.Inspection

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.mobile_plantation.ui.fragment.FormAncakFragment
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.utils.SoftKeyboardStateWatcher
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.ui.adapter.FormAncakPagerAdapter
import com.cbi.mobile_plantation.ui.adapter.ListTPHInsideRadiusAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.TakeFotoPreviewAdapter.Companion.CAMERA_PERMISSION_REQUEST_CODE
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.LocationViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.WaterMarkFotoDanFolder
import com.cbi.mobile_plantation.utils.AppUtils.hideWithAnimation
import com.cbi.mobile_plantation.utils.AppUtils.showWithAnimation
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScannedTPHLocation
import com.cbi.mobile_plantation.utils.ScannedTPHSelectionItem
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaredrummler.materialspinner.MaterialSpinner
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")
open class FormInspectionActivity : AppCompatActivity(),
    CameraRepository.PhotoCallback,
    ListTPHInsideRadiusAdapter.OnTPHSelectedListener {
    private var isEmptyScannedTPH = true

    data class SummaryItem(val title: String, val value: String)
    data class Location(val lat: Double = 0.0, val lon: Double = 0.0)

    private lateinit var btnScanTPHRadius: MaterialButton
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null
    private var radiusMinimum = 0F
    private var boundaryAccuracy = 0F
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var infoApp: String = ""
    private var jenisTPHListGlobal: List<JenisTPHModel> = emptyList()
    private var lat: Double? = null
    private var lon: Double? = null
    private var currentAccuracy: Float = 0F
    private var selectedTPHIdByScan: Int? = null
    private lateinit var titleScannedTPHInsideRadius: TextView
    private lateinit var descScannedTPHInsideRadius: TextView
    private lateinit var emptyScannedTPHInsideRadius: TextView
    private lateinit var progressBarScanTPHManual: ProgressBar
    private lateinit var progressBarScanTPHAuto: ProgressBar
    private var shouldReopenBottomSheet = false
    private var isTriggeredBtnScanned = false
    private lateinit var switchAutoScan: SwitchMaterial
    private lateinit var layoutAutoScan: LinearLayout
    private var autoScanEnabled = false
    private val autoScanHandler = Handler(Looper.getMainLooper())
    private val autoScanInterval = 5000L
    private lateinit var tvErrorScannedNotSelected: TextView

    //    private lateinit var lyPemanen1Inspect: LinearLayout
    private var panenStoredLocal: MutableMap<Int, TPHData> = mutableMapOf()

    private data class TPHData(
        val count: Int,
        val jenisTPHId: Int = 1,
        val limitTPH: String? = null
    )

    private var inspectionStoredLocal: MutableMap<Int, TPHData> = mutableMapOf()
    private val trackingLocation: MutableMap<String, Location> = mutableMapOf()
    private val listRadioItems: Map<String, Map<String, String>> = mapOf(
        "InspectionType" to mapOf(
            "1" to "Inspeksi",
            "2" to "AKP"
        ),
        "ConditionType" to mapOf(
            "1" to "Datar",
            "2" to "Teras"
        ),
        "StatusPanen" to mapOf(
            "1" to "H+1",
            "2" to "H+2",
            "3" to "H+3",
            "4" to "H+4",
            "5" to "H+5",
        ),
        "EntryPath" to mapOf(
            "US" to "Utara - Selatan",
            "SU" to "Selatan - Utara",
            "BT" to "Barat - Timur",
            "TB" to "Timur - Barat",
        )
    )

    private var isTenthTrees = false
    private var isSnackbarShown = false
    private var locationEnable: Boolean = false

    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>

    private var divisiList: List<TPHNewModel> = emptyList()
    private var blokList: List<TPHNewModel> = emptyList()
    private var tphList: List<PanenEntityWithRelations> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()

    private val karyawanIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranIdMap: MutableMap<String, Int> = mutableMapOf()
    private val karyawanLainIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranLainIdMap: MutableMap<String, Int> = mutableMapOf()

    private var totalPokokInspection = 0
    private var jumBrdTglPath = 0
    private var jumBuahTglPath = 0
    private var latLonMap: Map<Int, ScannedTPHLocation> = emptyMap()
    private lateinit var tphScannedResultRecyclerView: RecyclerView
    private var asistensi: Int = 0
    private var selectedAfdeling: String = ""
    private var selectedAfdelingIdSpinner: Int = 0
    private var selectedDivisiValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedBlok: String = ""
    private var selectedBlokValue: Int? = null
    private var selectedTPH: String = ""
    private var selectedTPHValue: Int? = null
    private var selectedStatusPanen: String = ""
    private var selectedJalurMasuk: String = ""
    private var selectedInspeksiValue: String = ""
    private var selectedKemandoran: String = ""
    private var selectedPemanen: String = ""
    private var selectedKemandoranLain: String = ""
    private var selectedPemanenLain: String = ""
    private var selectedKondisiValue: String = ""

    private var ancakValue: String = ""
    private var br1Value: String = ""
    private var br2Value: String = ""

    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private lateinit var selectedPemanenLainAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var rvSelectedPemanenLain: RecyclerView

    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var formAncakViewModel: FormAncakViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var inspectionViewModel: InspectionViewModel

    private lateinit var formAncakPagerAdapter: FormAncakPagerAdapter
    private lateinit var keyboardWatcher: SoftKeyboardStateWatcher

    private lateinit var infoBlokView: ScrollView
    private lateinit var formInspectionView: ConstraintLayout
    private lateinit var summaryView: ConstraintLayout
    private lateinit var bottomNavInspect: BottomNavigationView
    private lateinit var vpFormAncak: ViewPager2
    private lateinit var fabPrevFormAncak: FloatingActionButton
    private lateinit var fabNextFormAncak: FloatingActionButton
    private lateinit var fabPhotoFormAncak: FloatingActionButton
    private lateinit var fabSaveFormAncak: FloatingActionButton
    private var activityInitialized = false
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_inspection)
        //cek tanggal otomatis
        checkDateTimeSettings()
    }

    private fun initUI() {
        btnScanTPHRadius = findViewById(R.id.btnScanTPHRadius)
        tphScannedResultRecyclerView = findViewById(R.id.tphScannedResultRecyclerView)
        titleScannedTPHInsideRadius = findViewById(R.id.titleScannedTPHInsideRadius)
        descScannedTPHInsideRadius = findViewById(R.id.descScannedTPHInsideRadius)
        emptyScannedTPHInsideRadius = findViewById(R.id.emptyScanTPHInsideRadius)
        progressBarScanTPHManual = findViewById(R.id.progressBarScanTPHManual)
        progressBarScanTPHAuto = findViewById(R.id.progressBarScanTPHAuto)

        tvErrorScannedNotSelected = findViewById(R.id.tvErrorScannedNotSelected)
    }

    private fun setupScanTPHTrigger() {
        val alertCardScanRadius =
            findViewById<MaterialCardView>(R.id.alertCardScanRadius)
        alertCardScanRadius.visibility = View.VISIBLE

        val alertTvScannedRadius =
            findViewById<TextView>(R.id.alertTvScannedRadius)
        alertTvScannedRadius.visibility = View.VISIBLE

        val btnScanTPHRadius =
            findViewById<MaterialButton>(R.id.btnScanTPHRadius)

        if (autoScanEnabled) {
            btnScanTPHRadius.visibility = View.GONE
            selectedTPHIdByScan = null
            selectedTPHValue = null
        } else {
            btnScanTPHRadius.visibility = View.VISIBLE
        }

        // Show auto scan switch when scanning is available
        layoutAutoScan.visibility = View.VISIBLE

        val radiusText = "${radiusMinimum.toInt()} m"
        val text =
            "Lakukan Refresh saat $radiusText dalam radius terdekat TPH"
        val asterisk = "*"

        val spannableScanTPHTitle =
            SpannableString("$text $asterisk").apply {
                val startIndex = text.indexOf(radiusText)
                val endIndex = startIndex + radiusText.length

                setSpan(
                    StyleSpan(Typeface.BOLD), // Make text bold
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                setSpan(
                    StyleSpan(Typeface.ITALIC), // Make text bold
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                setSpan(
                    ForegroundColorSpan(Color.RED), // Make asterisk red
                    text.length,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        alertTvScannedRadius.text = spannableScanTPHTitle
    }

    private fun setupUI() {
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)
        radiusMinimum = prefManager!!.radiusMinimum
        boundaryAccuracy = prefManager!!.boundaryAccuracy
        initViewModel()
        initUI()
        setupAutoScanSwitch()
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        infoApp = AppUtils.getDeviceInfo(this@FormInspectionActivity).toString()

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupHeader()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)
            }

            try {
                val estateIdStr = estateId?.trim()

                if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                    val estateIdInt = estateIdStr.toInt()

                    val panenDeferred = CompletableDeferred<List<PanenEntityWithRelations>>()

                    panenViewModel.getAllTPHHasBeenSelected()
                    delay(100)

                    withContext(Dispatchers.Main) {
                        panenViewModel.activePanenList.observe(this@FormInspectionActivity) { list ->

                            tphList = list
                            val tphDataMap = mutableMapOf<Int, TPHData>()

                            list?.forEach { panen ->
                                val tphId = panen.tph?.id
                                val jenisTPHId = panen.tph?.jenis_tph_id?.toInt()
                                val limitTPH =
                                    panen.tph?.limit_tph  // Extract limit_tph directly from panen.tph

                                if (tphId != null && jenisTPHId != null) {
                                    val existingData = tphDataMap[tphId]
                                    tphDataMap[tphId] = FormInspectionActivity.TPHData(
                                        count = 0,
                                        jenisTPHId = jenisTPHId,
                                        limitTPH = limitTPH!!
                                    )

                                }
                            }

                            panenStoredLocal.clear()
                            panenStoredLocal.putAll(tphDataMap)

                            panenDeferred.complete(list ?: emptyList())
                        }
                    }


                    val jenisTPHDeferred = CompletableDeferred<List<JenisTPHModel>>()

                    panenViewModel.getAllJenisTPH()
                    delay(100)

                    withContext(Dispatchers.Main) {
                        panenViewModel.jenisTPHList.observe(this@FormInspectionActivity) { list ->
                            jenisTPHListGlobal = list ?: emptyList()
                            jenisTPHDeferred.complete(list ?: emptyList())
                        }
                    }


                    val inspectionDeferred = CompletableDeferred<List<InspectionModel>>()

                    inspectionViewModel.getTPHHasBeenInspect()
                    delay(100)

                    withContext(Dispatchers.Main) { // Ensure observation is on main thread
                        inspectionViewModel.inspectionList.observe(this@FormInspectionActivity) { list ->
                            val tphDataMap = mutableMapOf<Int, TPHData>()

                            list?.forEach { inspection ->
                                val tphId = inspection.tph_id

                                if (tphId != null) {
                                    val existingData = tphDataMap[tphId]
                                    if (existingData != null) {
                                        // Increment the count for existing TPH
                                        tphDataMap[tphId] =
                                            existingData.copy(count = existingData.count + 1)
                                    } else {
                                        tphDataMap[tphId] =
                                            TPHData(
                                                count = 1,
                                                limitTPH = "1"
                                            )
                                    }
                                }
                            }

                            inspectionStoredLocal.clear()
                            inspectionStoredLocal.putAll(tphDataMap)

                            inspectionDeferred.complete(list ?: emptyList())
                        }
                    }


                    val divisiDeferred = async {
                        try {
                            datasetViewModel.getDivisiList(estateIdInt)
                        } catch (e: Exception) {
                            emptyList() // Return an empty list to prevent crash
                        }
                    }

                    divisiList = divisiDeferred.await()

                    if (divisiList.isEmpty()) {
                        throw Exception("Periksa kembali dataset dengan melakukan Sinkronisasi Data!")
                    }
                }

                withContext(Dispatchers.Main) {
                    setupLayout()
                    setKeyboardVisibilityListener()
                    loadingDialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"

                    val estateInfo = estateId?.takeIf { it.isBlank() }
                        ?.let { "2. ID Estate User Login: \"$it\"" }

                    val fullMessage = listOfNotNull(errorMessage, estateInfo).joinToString("\n\n")

                    AlertDialogUtility.withSingleAction(
                        this@FormInspectionActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        fullMessage,
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                        finish()
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    cameraViewModel.statusCamera() -> {
                        cameraViewModel.closeCamera()

                        if (shouldReopenBottomSheet) {
                            shouldReopenBottomSheet = false
                            bottomNavInspect.visibility = View.VISIBLE
                            Handler(Looper.getMainLooper()).postDelayed({
                                showViewPhotoBottomSheet()
                            }, 100)
                        }
                    }

                    else -> {
                        vibrate()
                        AlertDialogUtility.withTwoActions(
                            this@FormInspectionActivity,
                            "Keluar",
                            getString(R.string.confirmation_dialog_title),
                            getString(R.string.al_confirm_feature),
                            "warning.json",
                            ContextCompat.getColor(
                                this@FormInspectionActivity,
                                R.color.bluedarklight
                            ),
                            function = {
                                val intent =
                                    Intent(
                                        this@FormInspectionActivity,
                                        HomePageActivity::class.java
                                    )
                                startActivity(intent)
                                finishAffinity()
                            }
                        )
                    }
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        val isLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isLocationGranted) {
            locationViewModel.startLocationUpdates()
            isSnackbarShown = false // Reset snackbar flag
        } else if (!isSnackbarShown) {
            showSnackbarWithSettings("Location permission is required for this app. Enable it in Settings.")
            isSnackbarShown = true // Prevent duplicate snackbars
        }


        locationViewModel.airplaneModeState.observe(this) { isAirplaneMode ->
            if (isAirplaneMode) {
                locationViewModel.stopLocationUpdates()
            } else {
                // Only restart if we have permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationViewModel.startLocationUpdates()
                }
            }
        }

        // Observe location updates
        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
        }

        locationViewModel.locationAccuracy.observe(this) { accuracy ->
            findViewById<TextView>(R.id.accuracyLocation).text = String.format("%.1f m", accuracy)
            currentAccuracy = accuracy
        }

        super.onResume()

        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {

        autoScanEnabled = false
        autoScanHandler.removeCallbacks(autoScanRunnable)

        locationViewModel.stopLocationUpdates()
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        locationViewModel.stopLocationUpdates()
        keyboardWatcher.unregister()
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
        super.onDestroy()
    }

    private fun initViewModel() {
        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]

        val factoryPanenViewModel = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factoryPanenViewModel)[PanenViewModel::class.java]

        formAncakViewModel = ViewModelProvider(this)[FormAncakViewModel::class.java]

        val idTakeFotoLayout = findViewById<View>(R.id.incTakePhotoInspect)
        val idEditFotoLayout = findViewById<View>(R.id.incEditPhotoInspect)
        val cameraRepository = CameraRepository(this, window, idTakeFotoLayout, idEditFotoLayout)
        cameraRepository.setPhotoCallback(this)
        cameraViewModel = ViewModelProvider(
            this,
            CameraViewModel.Factory(cameraRepository)
        )[CameraViewModel::class.java]

        val status_location = findViewById<ImageView>(R.id.statusLocation)
        locationViewModel = ViewModelProvider(
            this,
            LocationViewModel.Factory(application, status_location, this)
        )[LocationViewModel::class.java]

        val factoryInspection = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel =
            ViewModelProvider(this, factoryInspection)[InspectionViewModel::class.java]
    }

    private fun setupViewPager() {
        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        formAncakPagerAdapter = FormAncakPagerAdapter(this, totalPages)

        vpFormAncak.apply {
            adapter = formAncakPagerAdapter
            isUserInputEnabled = false
            setPageTransformer(createPageTransformer())
        }

        Handler(Looper.getMainLooper()).post {
            preloadAllPages()
        }
    }

    private fun createPageTransformer(): ViewPager2.PageTransformer {
        return ViewPager2.PageTransformer { page, position ->
            val absPosition = abs(position)
            page.alpha = 0.8f.coerceAtLeast(1 - absPosition)

            val scale = 0.95f.coerceAtLeast(1 - absPosition * 0.05f)
            page.scaleX = scale
            page.scaleY = scale
            page.translationX = page.width * (if (position < 0) -0.02f else 0.02f) * position
        }
    }

    private fun preloadAllPages() {
        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        for (i in 0 until totalPages) {
            vpFormAncak.setCurrentItem(i, false)
        }

        vpFormAncak.setCurrentItem(0, false)
        vpFormAncak.requestLayout()
        vpFormAncak.invalidate()
    }

    private fun observeViewModel() {
        formAncakViewModel.currentPage.observe(this) { page ->
            val pageIndex = page - 1

            if (vpFormAncak.currentItem != pageIndex) {
                vpFormAncak.setCurrentItem(pageIndex, true)
            }

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val totalPages =
                formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION

            Handler(Looper.getMainLooper()).postDelayed({
                fabPrevFormAncak.isEnabled = if (currentPage <= 1) false else true
                fabPrevFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage <= 1) R.color.greytext else androidx.biometric.R.color.biometric_error_color
                    )
                )
                fabNextFormAncak.isEnabled = if (currentPage >= totalPages) false else true
                fabNextFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage >= totalPages) R.color.greytext else R.color.greenDefault
                    )
                )
            }, 300)
        }

        formAncakViewModel.formData.observe(this) { formData ->
            updatePhotoBadgeVisibility()

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val pageData = formData[currentPage]
            val emptyTreeValue = pageData?.emptyTree ?: 0
            val inspectionType = selectedInspeksiValue.toInt()

            // Show/hide photo FAB based on conditions
            fabPhotoFormAncak.visibility = if (inspectionType == 1 && emptyTreeValue == 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        formAncakViewModel.fieldValidationError.observe(this) { errorMap ->
            val currentFragment =
                supportFragmentManager.findFragmentByTag("f${vpFormAncak.currentItem}")
            if (currentFragment is FormAncakFragment) {
                if (errorMap.isNotEmpty()) {
                    errorMap.forEach { (fieldId, errorMessage) ->
                        currentFragment.showValidationError(fieldId, errorMessage)
                    }
                } else {
                    currentFragment.clearValidationErrors()
                }
            }
        }
    }

    private fun setupPressedFAB() {
        fabNextFormAncak.setOnClickListener {
            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val nextPage = currentPage + 1
            val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val pageData = formData[currentPage]
            val photoValue = pageData?.photo ?: ""
            val emptyTreeValue = pageData?.emptyTree ?: 0

            // DETAILED LOGGING
            AppLogger.d("=== NEXT BUTTON DEBUG ===")
            AppLogger.d("currentPage: $currentPage")
            AppLogger.d("nextPage: $nextPage")
            AppLogger.d("totalPages: $totalPages")
            AppLogger.d("selectedInspeksiValue: ${selectedInspeksiValue.toInt()}")
            AppLogger.d("emptyTreeValue: $emptyTreeValue")
            AppLogger.d("photoValue: '$photoValue'")
            AppLogger.d("photoValue.isEmpty(): ${photoValue.isEmpty()}")
            AppLogger.d("pageData: $pageData")

            // PHOTO VALIDATION CHECK
            AppLogger.d("=== PHOTO VALIDATION CHECK ===")
            AppLogger.d("selectedInspeksiValue.toInt() == 1: ${selectedInspeksiValue.toInt() == 1}")
            AppLogger.d("emptyTreeValue == 1: ${emptyTreeValue == 1}")
            AppLogger.d("photoValue.isEmpty(): ${photoValue.isEmpty()}")
            AppLogger.d("All conditions: ${selectedInspeksiValue.toInt() == 1 && (emptyTreeValue == 1) && photoValue.isEmpty()}")

            if (selectedInspeksiValue.toInt() == 1 && (emptyTreeValue == 1) && photoValue.isEmpty()) {
                AppLogger.d("BLOCKED: Photo validation - inspection=1, emptyTree=1, photo empty")
                vibrate(500)
                showViewPhotoBottomSheet()
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon dapat mengambil foto temuan terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            AppLogger.d("=== CALLING VIEWMODEL VALIDATION ===")
            val validationResult = formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())

            AppLogger.d("=== VALIDATION RESULT ===")
            AppLogger.d("ValidationResult.isValid: ${validationResult.isValid}")
            AppLogger.d("ValidationResult.fieldId: ${validationResult.fieldId}")


            if (!validationResult.isValid) {
                AppLogger.d("BLOCKED: ViewModel validation failed")
                vibrate(500)

                // Show alert with validation message
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon diisi data yang diperlukan!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}

                return@setOnClickListener
            }

            if (nextPage <= totalPages) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage("Loading data...")

                        vpFormAncak.post {
                            vpFormAncak.setCurrentItem(nextPage - 1, false)
                            vpFormAncak.setCurrentItem(currentPage - 1, false)

                            Handler(Looper.getMainLooper()).post {
                                val pageChangeCallback = createPageChangeCallback()
                                vpFormAncak.registerOnPageChangeCallback(pageChangeCallback)

                                if (currentPage % 10 == 0 && !trackingLocation.containsKey(currentPage.toString())) {
                                    isTenthTrees = true
                                    trackingLocation[currentPage.toString()] = Location(lat ?: 0.0, lon ?: 0.0)
                                } else if (isTenthTrees) {
                                    isTenthTrees = false
                                }

                                formAncakViewModel.nextPage()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (loadingDialog.isShowing) {
                                        scrollToTopOfFormAncak()
                                        loadingDialog.dismiss()
                                        vpFormAncak.unregisterOnPageChangeCallback(pageChangeCallback)
                                    }
                                }, 500)
                            }
                        }
                    }
                }
            }
        }

        fabPrevFormAncak.setOnClickListener {
            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val prevPage = currentPage - 1

            if (prevPage >= 1) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage("Loading data...")

                        vpFormAncak.post {
                            vpFormAncak.setCurrentItem(prevPage - 1, false)
                            vpFormAncak.setCurrentItem(currentPage - 1, false)

                            Handler(Looper.getMainLooper()).post {
                                val pageChangeCallback = createPageChangeCallback()
                                vpFormAncak.registerOnPageChangeCallback(pageChangeCallback)

                                formAncakViewModel.previousPage()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (loadingDialog.isShowing) {
                                        loadingDialog.dismiss()
                                        vpFormAncak.unregisterOnPageChangeCallback(
                                            pageChangeCallback
                                        )
                                    }
                                }, 500)
                            }
                        }
                    }
                }
            }
        }

        fabPhotoFormAncak.setOnClickListener {
//            val validationResult =
//                formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())
//            if (validationResult.isValid) {
            showViewPhotoBottomSheet()
//            } else {
//                vibrate(500)
//            }
        }

        fabSaveFormAncak.setOnClickListener {
            if (totalPokokInspection <= 0) {
                vibrate(500)
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon dapat melakukan pemeriksaan terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            AlertDialogUtility.withTwoActions(
                this,
                "Simpan Data",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.confirmation_dialog_description),
                "warning.json",
                function = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            loadingDialog.show()
                            loadingDialog.setMessage("Menyimpan data...")

                            if (!isTenthTrees) {
                                trackingLocation["end"] = Location(lat ?: 0.0, lon ?: 0.0)
                            }

                            val selectedPemanen = selectedPemanenAdapter.getSelectedWorkers()
                            val idKaryawanList = selectedPemanen.mapNotNull {
                                karyawanIdMap[it.name.substringBefore(" - ").trim()]
                            }
                            val kemandoranIdList = selectedPemanen.mapNotNull {
                                kemandoranIdMap[it.name.substringBefore(" - ").trim()]
                            }
                            val selectedPemanenLain =
                                selectedPemanenLainAdapter.getSelectedWorkers()
                            val idKaryawanLainList = selectedPemanenLain.mapNotNull {
                                karyawanLainIdMap[it.name.substringBefore(" - ").trim()]
                            }
                            val kemandoranLainIdList = selectedPemanenLain.mapNotNull {
                                kemandoranLainIdMap[it.name.substringBefore(" - ").trim()]
                            }

                            val selectedNikPemanenIds = selectedPemanen.map { it.id }
                            val selectedNikPemanenLainIds = selectedPemanenLain.map { it.id }
                            val uniqueNikPemanen =
                                (selectedNikPemanenIds + selectedNikPemanenLainIds)
                                    .distinct()
                                    .joinToString(",")

                            val uniqueIdKaryawan = (idKaryawanList + idKaryawanLainList)
                                .distinct()
                                .map { it.toString() }
                                .joinToString(",")

                            val uniqueKemandoranId = (kemandoranIdList + kemandoranLainIdList)
                                .map { it.toString() }
                                .joinToString(",")

                            val datetimeFormattedForPath =
                                SimpleDateFormat(
                                    "yyMMddHHmmssSSS",
                                    Locale.getDefault()
                                ).format(Date())
                            val uniquePathId = "$userId$datetimeFormattedForPath"

                            val formattedTracking =
                                trackingLocation.values.joinToString("#") { "${it.lat},${it.lon}" }

                            val datetimeCreated = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())

                            // Check from radio button AKP/Inspeksi
                            val isInspection = selectedInspeksiValue.toInt() == 1

                            val totalPages =
                                formAncakViewModel.totalPages.value
                                    ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
                            val formData = formAncakViewModel.formData.value ?: mutableMapOf()

                            val inspectionDataList = mutableListOf<InspectionModel>()
                            for (page in 1..totalPages) {
                                val pageData = formData[page]
                                val emptyTreeValue = pageData?.emptyTree ?: 0
                                val isEmpty =
                                    if (isInspection) emptyTreeValue == 1 else emptyTreeValue == 2

                                if (emptyTreeValue > 0 && !isEmpty) {
                                    val inspection = InspectionModel(
                                        id_path = uniquePathId,
                                        tph_id = selectedTPHValue ?: 0,
                                        ancak = ancakValue.toInt(),
                                        status_panen = selectedStatusPanen.toInt(),
                                        jalur_masuk = selectedJalurMasuk,
                                        brd_tinggal = jumBrdTglPath,
                                        buah_tinggal = jumBuahTglPath,
                                        jenis_inspeksi = selectedInspeksiValue.toInt(),
                                        kemandoran_id = uniqueKemandoranId,
                                        karyawan_id = uniqueIdKaryawan,
                                        karyawan_nik = uniqueNikPemanen,
                                        asistensi = asistensi,
                                        jenis_kondisi = selectedKondisiValue.toInt(),
                                        baris1 = br1Value.toInt(),
                                        baris2 = if (selectedKondisiValue.toInt() == 1) br2Value.toInt() else null,
                                        no_pokok = page,
                                        jml_pokok = totalPokokInspection,
                                        titik_kosong = emptyTreeValue,
                                        jjg_akp = if (isInspection) null else (pageData?.jjgAkp
                                            ?: 0),
                                        prioritas = if (isInspection) (pageData?.priority
                                            ?: 0) else null,
                                        pokok_panen = if (isInspection) (pageData?.harvestTree
                                            ?: 0) else null,
                                        serangan_tikus = if (isInspection) (pageData?.ratAttack
                                            ?: 0) else null,
                                        ganoderma = if (isInspection) (pageData?.ganoderma
                                            ?: 0) else null,
                                        susunan_pelepah = if (isInspection) (pageData?.neatPelepah
                                            ?: 0) else null,
                                        pelepah_sengkleh = if (isInspection) (pageData?.pelepahSengkleh
                                            ?: 0) else null,
                                        kondisi_pruning = if (isInspection) (pageData?.pruning
                                            ?: 0) else null,
                                        kentosan = if (isInspection) (pageData?.kentosan
                                            ?: 0) else null,
                                        buah_masak = if (isInspection) (pageData?.ripe
                                            ?: 0) else null,
                                        buah_mentah = if (isInspection) (pageData?.buahM1
                                            ?: 0) else null,
                                        buah_matang = if (isInspection) (pageData?.buahM2
                                            ?: 0) else null,
                                        buah_matahari = if (isInspection) (pageData?.buahM3
                                            ?: 0) else null,
                                        brd_tidak_dikutip = if (isInspection) (pageData?.brdKtp
                                            ?: 0) else null,
                                        brd_dlm_piringan = if (isInspection) (pageData?.brdIn
                                            ?: 0) else null,
                                        brd_luar_piringan = if (isInspection) (pageData?.brdOut
                                            ?: 0) else null,
                                        brd_pasar_pikul = if (isInspection) (pageData?.pasarPikul
                                            ?: 0) else null,
                                        brd_ketiak = if (isInspection) (pageData?.ketiak
                                            ?: 0) else null,
                                        brd_parit = if (isInspection) (pageData?.parit
                                            ?: 0) else null,
                                        brd_segar = if (isInspection) (pageData?.brdSegar
                                            ?: 0) else null,
                                        brd_busuk = if (isInspection) (pageData?.brdBusuk
                                            ?: 0) else null,
                                        foto = if (isInspection) pageData?.photo else null,
                                        komentar = if (isInspection) pageData?.comment else null,
                                        info = infoApp,
                                        created_by = userId ?: 0,
                                        created_date = datetimeCreated,
                                    )
                                    inspectionDataList.add(inspection)
                                }
                            }

                            if (inspectionDataList.isNotEmpty()) {
                                val pathData = InspectionPathModel(
                                    id = uniquePathId,
                                    tracking_path = formattedTracking
                                )

                                val pathInsertSuccess = withContext(Dispatchers.IO) {
                                    try {
                                        val insertedPathId =
                                            inspectionViewModel.insertPathDataSync(pathData)
                                        insertedPathId != null
                                    } catch (e: Exception) {
                                        AppLogger.d("Error inserting path: ${e.message}")
                                        false
                                    }
                                }

                                if (!pathInsertSuccess) {
                                    val deleteResult =
                                        inspectionViewModel.deleteInspectionDatas(
                                            listOf(
                                                uniquePathId
                                            )
                                        )
                                    deleteResult.onFailure { error ->
                                        AppLogger.d("Failed to delete data path: $error")
                                    }
                                    throw Exception("Failed to insert path data")
                                }

                                val inspectionInsertSuccess = withContext(Dispatchers.IO) {
                                    try {
                                        val insertedInspectionIds =
                                            inspectionViewModel.insertInspectionDataSync(
                                                inspectionDataList
                                            )
                                        insertedInspectionIds.isNotEmpty()
                                    } catch (e: Exception) {
                                        AppLogger.d("Error inserting inspection: ${e.message}")
                                        false
                                    }
                                }

                                if (!inspectionInsertSuccess) {
                                    val deleteResult =
                                        inspectionViewModel.deleteInspectionDatas(
                                            listOf(
                                                uniquePathId
                                            )
                                        )
                                    deleteResult.onFailure { error ->
                                        AppLogger.d("Failed to delete data inspection: $error")
                                    }
                                    throw Exception("Failed to insert inspection data")
                                }

                                AlertDialogUtility.withSingleAction(
                                    this@FormInspectionActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_success_save_local),
                                    stringXML(R.string.al_description_success_save_local),
                                    "success.json",
                                    R.color.greenDefault
                                ) {
                                    val intent = Intent(
                                        this@FormInspectionActivity,
                                        HomePageActivity::class.java
                                    )
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            } else {
                                throw Exception("Failed to add list inspection data")
                            }
                        } catch (e: Exception) {
                            AppLogger.d("Unexpected error: ${e.message}")
                            AlertDialogUtility.withSingleAction(
                                this@FormInspectionActivity,
                                stringXML(R.string.al_back),
                                stringXML(R.string.al_failed_save_local),
                                "${stringXML(R.string.al_description_failed_save_local)} : ${e.message}",
                                "warning.json",
                                R.color.colorRedDark
                            ) {}
                        } finally {
                            if (loadingDialog.isShowing) {
                                loadingDialog.dismiss()
                            }
                        }
                    }
                }
            )
        }
    }

    private fun createPageChangeCallback(): ViewPager2.OnPageChangeCallback {
        return object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    scrollToTopOfFormAncak()
                    loadingDialog.dismiss()
                    vpFormAncak.unregisterOnPageChangeCallback(this)
                }
            }
        }
    }

    // Function to force scroll to top of FormAncakFragment
    private fun scrollToTopOfFormAncak() {
        val fragmentIndex = vpFormAncak.currentItem
        val fragmentTag = "f$fragmentIndex"

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment is FormAncakFragment) {
            fragment.scrollToTop()
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun showViewPhotoBottomSheet(fileName: String? = null) {
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val currentData =
            formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()
        val rootApp = File(
            this.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-${WaterMarkFotoDanFolder.WMInspeksi}"
        ).toString()

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_inspection_photo, null)
        bottomSheetDialog.setContentView(view)

        view.background = ContextCompat.getDrawable(this, R.drawable.rounded_top_right_left)

        val ibDeletePhotoInspect = view.findViewById<ImageButton>(R.id.ibDeletePhotoInspect)
        val incLytPhotosInspect = view.findViewById<View>(R.id.incLytPhotosInspect)
        val ivAddPhoto = incLytPhotosInspect.findViewById<ImageView>(R.id.ivAddFoto)
        val tvPhotoComment = incLytPhotosInspect.findViewById<TextView>(R.id.tvPhotoComment)
        val etPhotoComment = incLytPhotosInspect.findViewById<EditText>(R.id.etPhotoComment)

        tvPhotoComment.visibility = View.GONE

        ibDeletePhotoInspect.visibility =
            if (currentData.photo != null || currentData.comment != null) View.VISIBLE else View.GONE
        ibDeletePhotoInspect.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                this,
                "Hapus",
                this.getString(R.string.confirmation_dialog_title),
                "Apakah anda yakin untuk menghapus lampiran ini?",
                "warning.json",
                ContextCompat.getColor(this, R.color.greenDarker),
                function = {
                    ibDeletePhotoInspect.visibility = View.GONE
                    ivAddPhoto.setImageResource(R.drawable.baseline_add_a_photo_24)
                    etPhotoComment.setText("")
                    etPhotoComment.clearFocus()

                    formAncakViewModel.savePageData(
                        currentPage,
                        currentData.copy(
                            photo = null,
                            comment = null,
                            latIssue = null,
                            lonIssue = null
                        )
                    )

                    updatePhotoBadgeVisibility()
                }
            )
        }

        etPhotoComment.visibility = View.VISIBLE
        etPhotoComment.setText(currentData.comment)
        etPhotoComment.addTextChangedListener(object : TextWatcher {
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
                formAncakViewModel.savePageData(
                    currentPage,
                    currentData.copy(comment = s?.toString() ?: "")
                )
            }
        })

        var resultFileName = currentData.photo ?: ""
        if (fileName != null) {
            resultFileName = fileName
        }

        val filePath = File(rootApp, resultFileName)
        ivAddPhoto.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    bottomNavInspect.visibility = View.GONE
                    bottomSheetDialog.dismiss()

                    if (resultFileName.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            cameraViewModel.openZoomPhotos(
                                file = filePath,
                                position = currentPage.toString(),
                                onChangePhoto = {
                                    shouldReopenBottomSheet = true
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        cameraViewModel.takeCameraPhotos(
                                            this,
                                            currentPage.toString(),
                                            ivAddPhoto,
                                            currentPage,
                                            null,
                                            "", // soon assign lat lon
                                            currentPage.toString(),
                                            WaterMarkFotoDanFolder.WMInspeksi,
                                            null,
                                            null,
                                            ""
                                        )
                                    }, 100)
                                },
                                onDeletePhoto = { pos ->
                                    ivAddPhoto.setImageResource(R.drawable.baseline_add_a_photo_24)
                                    formAncakViewModel.savePageData(
                                        currentPage,
                                        currentData.copy(photo = "")
                                    )
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showViewPhotoBottomSheet()
                                    }, 100)
                                },
                                onClosePhoto = {
                                    bottomNavInspect.visibility = View.VISIBLE
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showViewPhotoBottomSheet()
                                    }, 100)
                                }
                            )
                        }, 100)
                    } else {
                        shouldReopenBottomSheet = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            cameraViewModel.takeCameraPhotos(
                                this,
                                currentPage.toString(),
                                ivAddPhoto,
                                currentPage,
                                null,
                                "", // soon assign lat lon
                                currentPage.toString(),
                                WaterMarkFotoDanFolder.WMInspeksi,
                                null,
                                null,
                                ""
                            )
                        }, 100)
                    }
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                ) -> {
                    showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
                }

                else -> {
                    // If permission is permanently denied, show settings option
                    if (isPermissionPermanentlyDenied()) {
                        showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
                    } else {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_PERMISSION_REQUEST_CODE
                        )
                    }
                }
            }
        }

        if (resultFileName.isNotEmpty()) {
            Glide.with(this)
                .load(filePath)
                .into(ivAddPhoto)
        }

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels

        bottomSheetDialog.window?.apply {
            setLayout(
                (width * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bottomSheetDialog.show()
    }

    private fun setKeyboardVisibilityListener() {
        val rootView = findViewById<View>(android.R.id.content)
        keyboardWatcher = SoftKeyboardStateWatcher(
            rootView,
            object : SoftKeyboardStateWatcher.OnSoftKeyboardStateChangedListener {
                override fun onSoftKeyboardOpened(keyboardHeight: Int) {
                    bottomNavInspect.post {
                        hideWithAnimation(bottomNavInspect, 100)
                        hideWithAnimation(fabPrevFormAncak, 100)
                        hideWithAnimation(fabNextFormAncak, 100)
                        hideWithAnimation(fabPhotoFormAncak, 100)
                    }
                }

                override fun onSoftKeyboardClosed() {
                    bottomNavInspect.post {
                        showWithAnimation(bottomNavInspect)
                        showWithAnimation(fabPrevFormAncak)
                        showWithAnimation(fabNextFormAncak)
                        showWithAnimation(fabPhotoFormAncak)
                    }
                }
            })
    }

    private fun setupAutoScanSwitch() {
        layoutAutoScan = findViewById(R.id.layoutAutoScan)
        switchAutoScan = findViewById(R.id.switchAutoScan)

        switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            autoScanEnabled = isChecked

            if (isChecked) {
                autoScanHandler.post(autoScanRunnable)
                btnScanTPHRadius.visibility = View.GONE
                Toast.makeText(
                    this@FormInspectionActivity,
                    "Auto-refresh TPH setiap 5 detik diaktifkan",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Stop automatic scanning
                autoScanHandler.removeCallbacks(autoScanRunnable)
                btnScanTPHRadius.visibility = View.VISIBLE
                Toast.makeText(
                    this@FormInspectionActivity,
                    "Auto-refresh TPH dinonaktifkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.VISIBLE

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

    @SuppressLint("CutPasteId")
    private fun setupLayout() {
        infoBlokView = findViewById(R.id.svInfoBlokInspection)
        formInspectionView = findViewById(R.id.clFormInspection)
        summaryView = findViewById(R.id.clSummaryInspection)
        bottomNavInspect = findViewById(R.id.bottomNavInspect)
        vpFormAncak = findViewById(R.id.vpFormAncakInspect)
        fabPrevFormAncak = findViewById(R.id.fabPrevFormInspect)
        fabNextFormAncak = findViewById(R.id.fabNextFormInspect)
        fabPhotoFormAncak = findViewById(R.id.fabPhotoFormInspect)
        fabSaveFormAncak = findViewById(R.id.fabSaveFormInspect)

        fabSaveFormAncak.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bluedarklight))

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                setupViewPager()
                observeViewModel()
                setupPressedFAB()
            }
        }

        val radiusText = "${radiusMinimum.toInt()} m"
        val fullText =
            "Berikut adalah daftar lokasi TPH yang berada dalam radius $radiusText dari lokasi anda:"
        val spannableString = SpannableString(fullText)
        val startIndex = fullText.indexOf(radiusText)
        val endIndex = startIndex + radiusText.length
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            StyleSpan(Typeface.ITALIC),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        descScannedTPHInsideRadius.text = spannableString

        tphScannedResultRecyclerView.layoutManager = LinearLayoutManager(this)
        tphScannedResultRecyclerView.isNestedScrollingEnabled = false
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        tphScannedResultRecyclerView.removeItemDecoration(decoration) // Remove if applied

        btnScanTPHRadius.setOnClickListener {
            if (currentAccuracy > boundaryAccuracy) {
                AlertDialogUtility.withTwoActions(
                    this@FormInspectionActivity, // Replace with your actual Activity name
                    "Lanjutkan",
                    getString(R.string.confirmation_dialog_title),
                    "Gps terdeteksi diluar dari ${boundaryAccuracy.toInt()} meter. Apakah tetap akan melanjutkan?",
                    "warning.json",
                    ContextCompat.getColor(this@FormInspectionActivity, R.color.greendarkerbutton),
                    function = {
                        isTriggeredBtnScanned = true
                        selectedTPHIdByScan = null
                        selectedTPHValue = null
                        progressBarScanTPHManual.visibility = View.VISIBLE
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkScannedTPHInsideRadius()
                        }, 500)
                    },
                    cancelFunction = {
                    }
                )
            } else {
                isTriggeredBtnScanned = true
                selectedTPHIdByScan = null
                selectedTPHValue = null
                progressBarScanTPHManual.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    checkScannedTPHInsideRadius()
                }, 400)
            }
        }

        bottomNavInspect.setOnItemSelectedListener { item ->
            Handler(Looper.getMainLooper()).postDelayed({
                formAncakViewModel.clearValidation()
            }, 200)

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val pageData = formData[currentPage]
            val emptyTreeValue = pageData?.emptyTree ?: 0
            val photoValue = pageData?.photo ?: ""

            val activeBottomNavId = bottomNavInspect.selectedItemId
            if (activeBottomNavId == item.itemId) return@setOnItemSelectedListener false

            loadingDialog.show()
            loadingDialog.setMessage("Loading data...")

            if (!validateAndShowErrors() || activeBottomNavId == R.id.navMenuAncakInspect) {
                loadingDialog.dismiss()

                if (activeBottomNavId == R.id.navMenuAncakInspect) {
                    val inspectionType = selectedInspeksiValue.toInt()
                    if (inspectionType == 1 && emptyTreeValue == 2 && photoValue.isEmpty()) {
                        vibrate(500)

                        showViewPhotoBottomSheet()
                        AlertDialogUtility.withSingleAction(
                            this,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_data_not_completed),
                            "Mohon dapat mengambil foto temuan terlebih dahulu!",
                            "warning.json",
                            R.color.colorRedDark
                        ) {}

                        return@setOnItemSelectedListener false
                    }

                    if (inspectionType == 2 && emptyTreeValue == 1) {
                        val validationResult =
                            formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())
                        if (!validationResult.isValid) {
                            vibrate(500)
                            return@setOnItemSelectedListener false
                        }
                    }
                } else {
                    vibrate(500)
                    return@setOnItemSelectedListener false
                }
            }

            lifecycleScope.launch {

                when (item.itemId) {
                    R.id.navMenuBlokInspect -> {
                        withContext(Dispatchers.Main) {
                            infoBlokView.visibility = View.VISIBLE
                            formInspectionView.visibility = View.GONE
                            summaryView.visibility = View.GONE

                            delay(200)
                            loadingDialog.dismiss()
                        }
                    }

                    R.id.navMenuAncakInspect -> {
                        withContext(Dispatchers.Main) {
                            if (!trackingLocation.containsKey("start")) {
                                trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
                            }

                            val afdResult = selectedAfdeling.replaceFirst("AFD-", "")
                            formAncakViewModel.updateInfoFormAncak(
                                estateName ?: "",
                                afdResult,
                                selectedBlok
                            )
                            formAncakViewModel.updateTypeInspection(selectedInspeksiValue.toInt() == 1)

                            infoBlokView.visibility = View.GONE
                            summaryView.visibility = View.GONE
                            formInspectionView.post {
                                vpFormAncak.post {
                                    formInspectionView.visibility = View.VISIBLE
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        loadingDialog.dismiss()
                                    }, 300)
                                }
                            }
                        }
                    }

                    R.id.navMenuSummaryInspect -> {
                        withContext(Dispatchers.Main) {
                            setupSummaryPage()

                            infoBlokView.visibility = View.GONE
                            formInspectionView.visibility = View.GONE
                            summaryView.post {
                                summaryView.visibility = View.VISIBLE
                                Handler(Looper.getMainLooper()).postDelayed({
                                    loadingDialog.dismiss()
                                }, 300)
                            }
                        }
                    }
                }
            }

            return@setOnItemSelectedListener when (item.itemId) {
                R.id.navMenuBlokInspect, R.id.navMenuAncakInspect, R.id.navMenuSummaryInspect -> true
                else -> false
            }
        }
        bottomNavInspect.selectedItemId = R.id.navMenuBlokInspect

        inputMappings = listOf(
            Triple(
                findViewById(R.id.lyEstInspect),
                getString(R.string.field_estate),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyAfdInspect),
                getString(R.string.field_afdeling),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyJalurInspect),
                "Jalur Masuk",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyInspectionType),
                "Jenis Inspeksi",
                InputType.RADIO
            ),
            Triple(
                findViewById(R.id.lyMandor2Inspect),
                "Kemandoran Lain",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyConditionType),
                "Jenis Kondisi",
                InputType.RADIO
            ),
            Triple(
                findViewById(R.id.lyBaris1Inspect),
                "Baris Pertama",
                InputType.EDITTEXT
            ),
            Triple(
                findViewById(R.id.lyBaris2Inspect),
                "Baris Kedua",
                InputType.EDITTEXT
            ),
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateLabelTextView(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.lyEstInspect -> {
                            val namaEstate = prefManager!!.estateUserLengkapLogin
                            setupSpinnerView(layoutView, emptyList())
                            val pemanenSpinner =
                                layoutView.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                            pemanenSpinner.setHint(namaEstate)
                        }

                        R.id.lyAfdInspect -> {
                            val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
                            setupSpinnerView(layoutView, divisiNames)
                        }

                        R.id.lyJalurInspect -> setupSpinnerView(
                            layoutView,
                            (listRadioItems["EntryPath"] ?: emptyMap()).values.toList()
                        )

                        else -> setupSpinnerView(layoutView, emptyList())
                    }
                }

                InputType.EDITTEXT -> setupEditTextView(layoutView)

                InputType.RADIO -> {
                    when (layoutView.id) {
                        R.id.lyInspectionType -> setupRadioView(
                            layoutView,
                            listRadioItems["InspectionType"] ?: emptyMap(), ::selectedInspeksiValue
                        )

                        R.id.lyConditionType -> setupRadioView(
                            layoutView,
                            listRadioItems["ConditionType"] ?: emptyMap(),
                            ::selectedKondisiValue
                        ) { id ->
                            findViewById<LinearLayout>(R.id.lyBaris2Inspect).visibility =
                                if (id.toInt() == 2) View.GONE else View.VISIBLE
                        }
                    }
                }
            }
        }

        val counterMappings = listOf(
            Triple(R.id.lyBrdTglInspect, "Brondolan Tinggal", ::jumBrdTglPath),
            Triple(R.id.lyBuahTglInspect, "Buah Tinggal", ::jumBuahTglPath),
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPanenWithButtons(layoutId, labelText, counterVar)
        }

        rvSelectedPemanen = findViewById<RecyclerView>(R.id.rvSelectedPemanenInspection)

        selectedPemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = selectedPemanenAdapter
        rvSelectedPemanen.layoutManager = FlexboxLayoutManager(this).apply {
            justifyContent = JustifyContent.FLEX_START
        }
    }

    private fun updateLabelTextView(linearLayout: LinearLayout, text: String) {
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)

        val spannable = SpannableString("$text *")
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorRed)),
            text.length, spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }

    private fun updatePhotoBadgeVisibility() {
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val currentData =
            formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()

        val badgePhotoInspect = findViewById<View>(R.id.badgePhotoInspect)
        badgePhotoInspect.visibility = if (currentData.photo != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setupSummaryPage() {
        fun createTextView(
            text: String,
            gravity: Int,
            weight: Float,
            isTitle: Boolean = false
        ): TextView {
            val textView = TextView(this)
            textView.text = text
            textView.setPadding(32, 32, 32, 32)
            textView.setTextColor(Color.BLACK)
            textView.textSize = 18f
            textView.gravity = gravity
            textView.setTypeface(null, if (isTitle) Typeface.NORMAL else Typeface.BOLD)

            val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
            params.setMargins(5, 5, 5, 5)
            textView.layoutParams = params

            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.setColor(
                ColorUtils.setAlphaComponent(
                    ContextCompat.getColor(
                        this,
                        R.color.greenDarker
                    ), (0.2 * 255).toInt()
                )
            )
            shape.cornerRadii = if (isTitle) {
                floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 20f, 20f)
            } else {
                floatArrayOf(0f, 0f, 20f, 20f, 20f, 20f, 0f, 0f)
            }


            textView.background = shape
            return textView
        }

        val tableLayout = findViewById<TableLayout>(R.id.tblLytSummaryInspect)
        tableLayout.removeAllViews()

        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        val formData = formAncakViewModel.formData.value ?: mutableMapOf()


        AppLogger.d("formData $formData")
        totalPokokInspection = (1..totalPages).count { (formData[it]?.emptyTree ?: 0) > 0 }

        val data = listOf(
            SummaryItem("Jumlah Pokok", totalPokokInspection.toString()),
        )

        for (item in data) {
            val tableRow = TableRow(this)

            val titleTextView = createTextView(item.title, Gravity.START, 2f, true)
            tableRow.addView(titleTextView)

            val valueTextView = createTextView(item.value, Gravity.CENTER, 1f)
            tableRow.addView(valueTextView)

            tableLayout.addView(tableRow)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinnerView(
        linearLayout: LinearLayout,
        data: List<String>
    ) {
        val editText = linearLayout.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)

        if (linearLayout.id == R.id.lyEstInspect) {
            spinner.isEnabled = false // Disable spinner
        }

        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE
            handleItemSelection(linearLayout, position, item.toString())
        }
    }

    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE

        // Set input type based on layout ID
        etHomeMarkerTPH.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etHomeMarkerTPH.imeOptions = if (layoutView.id in listOf(
                R.id.lyBaris1Inspect,
                R.id.lyBaris2Inspect
            )
        ) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT

        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm =
                    application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                findViewById<MaterialSpinner>(R.id.spPanenTBS)?.requestFocus()
                true
            } else {
                false
            }
        }

        etHomeMarkerTPH.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
                MCVSpinner.setBackgroundColor(
                    ContextCompat.getColor(
                        layoutView.context,
                        R.color.graytextdark
                    )
                )

                when (layoutView.id) {
                    R.id.lyBaris1Inspect -> {
                        br1Value = s?.toString()?.trim() ?: ""
                    }

                    R.id.lyBaris2Inspect -> {
                        br2Value = s?.toString()?.trim() ?: ""
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRadioView(
        layoutView: LinearLayout,
        itemList: Map<String, String>,
        nameVar: KMutableProperty0<String>,
        onConditionChanged: ((String) -> Unit)? = null
    ) {
        val mcvSpinner = layoutView.findViewById<MaterialCardView>(R.id.MCVSpinner)
        val fblRadioComponents = layoutView.findViewById<FlexboxLayout>(R.id.fblRadioComponents)

        mcvSpinner.visibility = View.GONE
        fblRadioComponents.visibility = View.VISIBLE

        var lastSelectedRadioButton: RadioButton? = null
        val isFirstIndex = itemList.entries.firstOrNull()
        itemList.forEach { (id, label) ->
            val radioButton = RadioButton(layoutView.context).apply {
                text = label
                tag = View.generateViewId()
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(10, 0, 30, 0)
                buttonTintList = getColorStateList(R.color.greenDefault)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                if (id == isFirstIndex?.key) {
                    isChecked = true
                    lastSelectedRadioButton = this
                    nameVar.set(id)
                }

                setOnClickListener {
                    onConditionChanged?.invoke(id)
                    lastSelectedRadioButton?.isChecked = false
                    isChecked = true
                    lastSelectedRadioButton = this
                    nameVar.set(id)
                }
            }

            fblRadioComponents.addView(radioButton)
        }
    }

    private fun setupPanenWithButtons(
        layoutId: Int,
        labelText: String,
        counterVar: KMutableProperty0<Int>
    ) {
        val layoutView = findViewById<View>(layoutId) ?: return

        val titleTextView = layoutView.findViewById<TextView>(R.id.tvNumberPanen)
        titleTextView.text = labelText

        val editText = layoutView.findViewById<EditText>(R.id.etNumber)
        val btnMinus = layoutView.findViewById<CardView>(R.id.btDec)
        val btnPlus = layoutView.findViewById<CardView>(R.id.btInc)

        editText.setText(counterVar.get().toString())

        handleLongPress(editText, btnPlus)
        btnPlus.setOnClickListener {
            val currentVal = editText.text.toString().toIntOrNull() ?: 0
            val newValue = currentVal + 1

            editText.setText(newValue.toString())
            counterVar.set(newValue)
        }

        handleLongPress(editText, btnMinus, false)
        btnMinus.setOnClickListener {
            val currentVal = editText.text.toString().toIntOrNull() ?: 0
            val newValue = maxOf(currentVal - 1, 0)

            editText.setText(newValue.toString())
            counterVar.set(newValue)
        }


        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    try {
                        val enteredValue = s.toString().toInt()

                        val validatedValue = when {
                            enteredValue < 0 -> 0
                            else -> enteredValue
                        }

                        if (enteredValue != validatedValue) {
                            editText.setText(validatedValue.toString())
                            editText.setSelection(editText.text.length)
                        }

                        counterVar.set(validatedValue)
                    } catch (e: NumberFormatException) {
                        editText.setText("0")
                        counterVar.set(0)
                    }
                }
            }
        })
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun handleLongPress(
        editText: EditText,
        button: CardView,
        isIncrement: Boolean = true
    ) {
        var isLongPressing = false
        var runnableData: Runnable? = null
        val handler = Handler(Looper.getMainLooper())
        runnableData = Runnable {
            if (isLongPressing) {
                val currentVal = editText.text.toString().toIntOrNull() ?: 0
                val newValue = if (isIncrement) currentVal + 1 else maxOf(currentVal - 1, 0)

                editText.setText(newValue.toString())

                if (isIncrement || newValue > 0) {
                    val delay = if (isLongPressing) 100L else 300L
                    handler.postDelayed(runnableData!!, delay)
                } else {
                    isLongPressing = false
                }
            }
        }

        button.setOnLongClickListener {
            isLongPressing = true
            handler.post(runnableData)
            true
        }

        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongPressing = false
            }
            false
        }
    }

    private fun handleItemSelection(
        linearLayout: LinearLayout,
        position: Int,
        selectedItem: String
    ) {
        when (linearLayout.id) {
            R.id.lyAfdInspect -> {
                selectedAfdeling = selectedItem
                selectedAfdelingIdSpinner = position
                isTriggeredBtnScanned = false
                val selectedDivisiId = try {
                    divisiList.find { it.divisi_abbr == selectedAfdeling }?.divisi
                } catch (e: Exception) {
                    null
                }

                val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()
                selectedDivisiValue = selectedDivisiId

                val nonSelectedAfdelingKemandoran = try {
                    divisiList.filter { it.divisi_abbr != selectedAfdeling }
                } catch (e: Exception) {
                    emptyList()
                }

                val nonSelectedIdAfdeling = try {
                    nonSelectedAfdelingKemandoran.map { it.divisi }
                } catch (e: Exception) {
                    emptyList()
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        setupScanTPHTrigger()
                        animateLoadingDots(linearLayout)
                        delay(200) // 1 second delay
                    }

                    try {
                        if (estateId == null || selectedDivisiId == null) {
                            throw IllegalStateException("Estate ID or selectedDivisiId is null!")
                        }

                        latLonMap = emptyMap()

                        val latLonResult = async {
                            try {
                                val estateIdToUse = estateId!!.toInt()

                                // Get the TPH IDs that have been selected for panen
                                val selectedTPHIds = panenStoredLocal.keys.toList()
                                AppLogger.d("Selected TPH IDs from panenStoredLocal: $selectedTPHIds (count: ${selectedTPHIds.size})")

                                AppLogger.d(selectedTPHIds.size.toString())
                                val tphList = datasetViewModel.getLatLonDivisiByTPHIds(
                                    estateIdToUse,
                                    selectedDivisiId,
                                    selectedTPHIds
                                )

                                AppLogger.d("Database returned ${tphList.size} TPH records")

                                tphList.mapNotNull { tph ->
                                    val id = tph.id
                                    val lat = tph.lat?.toDoubleOrNull()
                                    val lon = tph.lon?.toDoubleOrNull()
                                    val nomor = tph.nomor ?: ""
                                    val blokKode = tph.blok_kode ?: ""
                                    val jenisTPHId = tph.jenis_tph_id ?: "1"

                                    if (id != null && lat != null && lon != null) {
                                        AppLogger.d("Processing valid TPH: ID=$id, lat=$lat, lon=$lon, nomor=$nomor")
                                        id to ScannedTPHLocation(
                                            lat,
                                            lon,
                                            nomor,
                                            blokKode,
                                            jenisTPHId
                                        )
                                    } else {
                                        AppLogger.w("Skipping invalid TPH: ID=$id, lat=$lat, lon=$lon")
                                        null
                                    }
                                }.toMap()

                            } catch (e: Exception) {
                                AppLogger.e("Error in latLonResult: ${e.message}", e.toString())
                                throw e
                            }
                        }

                        try {
                            latLonMap = latLonResult.await()
                            AppLogger.d("latLonMap created successfully with ${latLonMap.size} entries")
                            AppLogger.d("latLonMap keys: ${latLonMap.keys}")
                        } catch (e: Exception) {
                            AppLogger.e("Error awaiting latLonResult: ${e.message}", e.toString())
                            withContext(Dispatchers.Main) {
                                AlertDialogUtility.withSingleAction(
                                    this@FormInspectionActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_fetch_data),
                                    "Error fetching listLatLonAfd: ${e.message}",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) { }
                            }
                            latLonMap = emptyMap()
                        }


                        val blokDeferred = async {
                            try {
                                datasetViewModel.getBlokList(
                                    estateId!!.toInt(),
                                    selectedDivisiId
                                )
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        val kemandoranDeferred = async {
                            try {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    selectedDivisiIdList
                                )
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        val kemandoranLainDeferred = async {
                            try {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    nonSelectedIdAfdeling as List<Int>
                                )
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        blokList = blokDeferred.await()
                        kemandoranList = kemandoranDeferred.await()
                        kemandoranLainList = kemandoranLainDeferred.await()

                    } catch (e: Exception) {

                        AppLogger.d("Error loading afdeling data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FormInspectionActivity,
                                "Error loading afdeling data: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            hideLoadingDots(linearLayout)
                        }
                    }
                }
            }

            R.id.lyJalurInspect -> {
                val mapData = listRadioItems["EntryPath"] ?: emptyMap()
                val selectedKey = mapData.entries.find { it.value == selectedItem }?.key
                selectedJalurMasuk = selectedKey ?: ""
            }

//            R.id.lyMandor1Inspect -> {
//                selectedPemanenAdapter.clearAllWorkers()
//                selectedKemandoran = selectedItem
//
//                val filteredKemandoranId: Int? = try {
//                    kemandoranList.find {
//                        it.dept == estateId?.toIntOrNull() && // Avoids force unwrap (!!)
//                                it.divisi == selectedDivisiValue &&
//                                it.nama == selectedKemandoran
//                    }?.id
//                } catch (e: Exception) {
//                    AppLogger.e("Error finding Kemandoran ID: ${e.message}")
//                    null
//                }
//
//                if (filteredKemandoranId != null) {
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        withContext(Dispatchers.Main) {
//                            animateLoadingDots(linearLayout)
//                            delay(1000) // 1 second delay
//                        }
//
//                        try {
//                            val karyawanDeferred = async {
//                                datasetViewModel.getKaryawanList(filteredKemandoranId)
//                            }
//
//                            karyawanList = karyawanDeferred.await()
//                            val karyawanNames = karyawanList
//                                .sortedBy { it.nama } // Sort by name alphabetically
//                                .map { "${it.nama} - ${it.nik}" }
//
//                            withContext(Dispatchers.Main) {
//                                val layoutPemanen =
//                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyPemanen1Inspect)
//                                if (karyawanNames.isNotEmpty()) {
//                                    setupSpinnerView(layoutPemanen, karyawanNames)
//                                } else {
//                                    setupSpinnerView(layoutPemanen, emptyList())
//                                }
//                            }
//
//                        } catch (e: Exception) {
//                            AppLogger.e("Error fetching afdeling data: ${e.message}")
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(
//                                    this@FormInspectionActivity,
//                                    "Error loading afdeling data: ${e.message}",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            }
//                        } finally {
//                            withContext(Dispatchers.Main) {
//                                hideLoadingDots(linearLayout)
//                            }
//                        }
//                    }
//                } else {
//                    AppLogger.e("Filtered Kemandoran ID is null, skipping data fetch.")
//                }
//            }

//            R.id.lyPemanen1Inspect -> {
//                selectedPemanen = selectedItem.toString()
//                AppLogger.d("Selected Pemanen: $selectedPemanen")
//                AppLogger.d("Selected TPH ID: $selectedTPHIdByScan")
//
//                // Extract NIK and Name from the formatted selection (e.g., "L92832 - Ahmad")
//                val dashIndex = selectedPemanen.indexOf(" - ")
//                val selectedNik = if (dashIndex != -1) {
//                    selectedPemanen.substring(0, dashIndex).trim()
//                } else {
//                    ""
//                }
//
//                val selectedName = if (dashIndex != -1) {
//                    selectedPemanen.substring(dashIndex + 3).trim()
//                } else {
//                    selectedPemanen.trim()
//                }
//
//                AppLogger.d("Extracted NIK: '$selectedNik'")
//                AppLogger.d("Extracted Name: '$selectedName'")
//
//                // Find all matching panen records for the selected TPH
//                val matchingPanenList = tphList.filter { panenWithRelations ->
//                    val tphId = panenWithRelations.tph?.id
//                    tphId == selectedTPHIdByScan
//                }
//
//                AppLogger.d("Found ${matchingPanenList.size} matching panen records for TPH ID: $selectedTPHIdByScan")
//
//                // Find the selected employee from the matching panen records
//                var selectedEmployee: PanenEntity? = null
//
//                // First, try to find by NIK match
//                if (selectedNik.isNotEmpty()) {
//                    for (panenWithRelations in matchingPanenList) {
//                        val panenEntity = panenWithRelations.panen ?: continue
//                        val karyawanNik = panenEntity.karyawan_nik
//
//                        if (karyawanNik == selectedNik) {
//                            selectedEmployee = panenEntity
//                            AppLogger.d("Found matching employee by NIK: ${panenEntity.karyawan_nik}")
//                            break
//                        }
//                    }
//                }
//
//                // If not found by NIK, try to find by name match
//                if (selectedEmployee == null) {
//                    for (panenWithRelations in matchingPanenList) {
//                        val panenEntity = panenWithRelations.panen ?: continue
//                        val karyawanNama = panenEntity.karyawan_nama
//
//                        if (!karyawanNama.isNullOrBlank()) {
//                            // Split karyawan_nama by comma and check each name
//                            val names = karyawanNama.split(",").map { it.trim() }
//
//                            if (names.any { it.equals(selectedName, ignoreCase = true) }) {
//                                selectedEmployee = panenEntity
//                                AppLogger.d("Found matching employee by name: ${panenEntity.karyawan_nama}")
//                                break
//                            }
//                        }
//                    }
//                }
//
//                // If still not found, try partial match on name
//                if (selectedEmployee == null) {
//                    for (panenWithRelations in matchingPanenList) {
//                        val panenEntity = panenWithRelations.panen ?: continue
//                        val karyawanNama = panenEntity.karyawan_nama
//
//                        if (!karyawanNama.isNullOrBlank()) {
//                            val names = karyawanNama.split(",").map { it.trim() }
//
//                            if (names.any { it.contains(selectedName, ignoreCase = true) }) {
//                                selectedEmployee = panenEntity
//                                AppLogger.d("Found matching employee by partial match: ${panenEntity.karyawan_nama}")
//                                break
//                            }
//                        }
//                    }
//                }
//
//                if (selectedEmployee != null) {
//                    AppLogger.d("Selected Employee Found:")
//                    AppLogger.d("- Karyawan ID: ${selectedEmployee.karyawan_id}")
//                    AppLogger.d("- Karyawan NIK: ${selectedEmployee.karyawan_nik}")
//                    AppLogger.d("- Karyawan Nama: ${selectedEmployee.karyawan_nama}")
//                    AppLogger.d("- Kemandoran ID: ${selectedEmployee.kemandoran_id}")
//
//                    // Build the maps using the selected employee data
//                    karyawanIdMap.clear()
//                    kemandoranIdMap.clear()
//
//                    // Add the selected employee to the maps using different key formats
//                    // Use the original formatted selection as key
//                    karyawanIdMap[selectedPemanen] = selectedEmployee.karyawan_id.toIntOrNull() ?: 0
//                    kemandoranIdMap[selectedPemanen] = selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
//
//                    // Also add by NIK as key
//                    if (selectedNik.isNotEmpty()) {
//                        karyawanIdMap[selectedNik] = selectedEmployee.karyawan_id.toIntOrNull() ?: 0
//                        kemandoranIdMap[selectedNik] = selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
//                    }
//
//                    // Also add by name as key
//                    if (selectedName.isNotEmpty()) {
//                        karyawanIdMap[selectedName] = selectedEmployee.karyawan_id.toIntOrNull() ?: 0
//                        kemandoranIdMap[selectedName] = selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
//                    }
//
//                    // Create Worker and add to adapter
//                    val workerId = selectedEmployee.karyawan_id
//                    val worker = Worker(workerId, selectedPemanen)
//                    selectedPemanenAdapter.addWorker(worker)
//
//                    val availableWorkers = selectedPemanenAdapter.getAvailableWorkers()
//
//                    if (availableWorkers.isNotEmpty()) {
//                        setupSpinnerView(
//                            linearLayout,
//                            availableWorkers.map { it.name }
//                        )
//                    }
//
//                    AppLogger.d("Selected Worker: $selectedPemanen, Karyawan ID: $workerId")
//                    AppLogger.d("Available workers count: ${availableWorkers.size}")
//
//                } else {
//                    AppLogger.e("Error: Could not find worker with NIK '$selectedNik' or name '$selectedName' in TPH ID: $selectedTPHIdByScan")
//                    AppLogger.d("Available records in matching TPH:")
//
//                    matchingPanenList.forEach { panenWithRelations ->
//                        val panenEntity = panenWithRelations.panen
//                        if (panenEntity != null) {
//                            AppLogger.d("- NIK: '${panenEntity.karyawan_nik}', Names: '${panenEntity.karyawan_nama}'")
//                        }
//                    }
//                }
//            }
//
//            R.id.lyMandor2Inspect -> {
//                selectedPemanenLainAdapter.clearAllWorkers()
//                selectedKemandoranLain = selectedItem
//
//                val selectedIdKemandoranLain: Int? = try {
//                    kemandoranLainList.find {
//                        it.nama == selectedKemandoranLain
//                    }?.id
//                } catch (e: Exception) {
//                    null
//                }
//
//                if (selectedIdKemandoranLain != null) {
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        withContext(Dispatchers.Main) {
//                            animateLoadingDots(linearLayout)
//                            delay(1000) // 1 second delay
//                        }
//
//                        try {
//                            val karyawanDeferred = async {
//                                datasetViewModel.getKaryawanList(selectedIdKemandoranLain)
//                            }
//
//                            karyawanLainList = karyawanDeferred.await()
//                            val namaKaryawanKemandoranLain =
//                                karyawanLainList.sortedBy { it.nama } // Sort by name alphabetically
//                                    .map { "${it.nama} - ${it.nik}" }
//
//                            withContext(Dispatchers.Main) {
//                                val layoutPemanenLain =
//                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyPemanen2Inspect)
//                                if (namaKaryawanKemandoranLain.isNotEmpty()) {
//                                    setupSpinnerView(
//                                        layoutPemanenLain,
//                                        namaKaryawanKemandoranLain
//                                    )
//                                } else {
//                                    setupSpinnerView(layoutPemanenLain, emptyList())
//                                }
//                            }
//                        } catch (e: Exception) {
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(
//                                    this@FormInspectionActivity,
//                                    "Error loading kemandoran lain data: ${e.message}",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            }
//                        } finally {
//                            withContext(Dispatchers.Main) {
//                                hideLoadingDots(linearLayout)
//                            }
//                        }
//                    }
//                } else {
//                    AppLogger.e("Selected ID Kemandoran Lain is null, skipping data fetch.")
//                }
//            }
//
//            R.id.lyPemanen2Inspect -> {
//                selectedPemanenLain = selectedItem
//
//                val selectedNamaPemanenLain = selectedPemanenLain.substringBefore(" - ").trim()
//                val karyawanLainNikMap =
//                    karyawanLainList.associateBy({ it.nama!!.trim() }, { it.nik!! })
//                karyawanLainList.forEach {
//                    it.nama?.trim()?.let { nama ->
//                        karyawanLainIdMap[nama] = it.id!!
//                        kemandoranLainIdMap[nama] = it.kemandoran_id!!
//                    }
//                }
//
//                val selectedPemanenLainId = karyawanLainNikMap[selectedNamaPemanenLain]
//                if (selectedPemanenLainId != null) {
//                    val worker = Worker(selectedPemanenLainId.toString(), selectedPemanenLain)
//                    selectedPemanenLainAdapter.addWorker(worker)
//
//                    val availableWorkers = selectedPemanenLainAdapter.getAvailableWorkers()
//                    if (availableWorkers.isNotEmpty()) {
//                        setupSpinnerView(
//                            linearLayout,
//                            availableWorkers.map { it.name })
//                    }
//                }
//            }
        }
    }


    private var isProcessingTPHSelection = false // Add this as a class variable

    override fun onTPHSelected(selectedTPHInLIst: ScannedTPHSelectionItem) {
        // Prevent rapid clicking
        if (isProcessingTPHSelection) {
            AppLogger.d("TPH selection already in progress, ignoring...")
            return
        }

        isProcessingTPHSelection = true

        tvErrorScannedNotSelected.visibility = View.GONE

        // Make title and description visible
        val titlePemanenInspeksi = findViewById<TextView>(R.id.titlePemanenInspeksi)
        val descPemanenInspeksi = findViewById<TextView>(R.id.descPemanenInspeksi)
        titlePemanenInspeksi.visibility = View.VISIBLE
        descPemanenInspeksi.visibility = View.VISIBLE

        // Clear adapter and maps FIRST
        selectedPemanenAdapter.clearAllWorkers()
        karyawanIdMap.clear()
        kemandoranIdMap.clear()
        rvSelectedPemanen.visibility = View.VISIBLE

        // Set display mode to show names without remove buttons
        selectedPemanenAdapter.setDisplayOnly(true)

        selectedTPHIdByScan = selectedTPHInLIst.id

        // Add a small delay to allow UI to update and prevent rapid clicking
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val matchingPanenList = tphList.filter { panenWithRelations ->
                    val tphId = panenWithRelations.tph?.id
//                    AppLogger.d("Checking TPH ID: $tphId against selected: ${selectedTPHInLIst.id}")
                    tphId == selectedTPHInLIst.id
                }

                // Get date_created and ancak from the first matching record for description
                var dateCreated = ""
                var ancakValue = ""
                if (matchingPanenList.isNotEmpty()) {
                    val firstPanen = matchingPanenList.first().panen
                    val firstTph = matchingPanenList.first().tph
                    dateCreated = firstPanen?.date_created ?: ""
                    ancakValue = firstTph?.ancak ?: ""
                }

                // Convert to Indonesian date format directly
                val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID"))
                val indonesianDate = if (dateCreated.isNotBlank()) {
                    try {
                        // Check if the date string contains time information
                        val inputFormat = if (dateCreated.contains(" ")) {
                            // Has time: "2025-05-30 14:30:00" or "2025-05-30 14:30"
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        } else {
                            // Only date: "2025-05-30" - will default to 00:00
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        }

                        val date = inputFormat.parse(dateCreated)
                        outputFormat.format(date ?: Date())
                    } catch (e: Exception) {
                        // If parsing with seconds fails, try without seconds
                        try {
                            val inputFormat2 =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val date = inputFormat2.parse(dateCreated)
                            outputFormat.format(date ?: Date())
                        } catch (e2: Exception) {
                            dateCreated
                        }
                    }
                } else {
                    "tanggal tidak tersedia"
                }

                // Use Set to avoid duplicates - store the formatted "NIK - Name" strings
                val karyawanFormattedSet = mutableSetOf<String>()

                matchingPanenList.forEach { panenWithRelations ->
                    val panenEntity = panenWithRelations.panen
                    val karyawanNama = panenEntity?.karyawan_nama
                    val karyawanNik = panenEntity?.karyawan_nik

                    if (!karyawanNama.isNullOrBlank() && !karyawanNik.isNullOrBlank()) {
                        // Split both names and NIKs by comma and trim whitespace
                        val names = karyawanNama.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        val niks = karyawanNik.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        AppLogger.d("Split names: $names")
                        AppLogger.d("Split NIKs: $niks")

                        val minSize = minOf(names.size, niks.size)
                        for (i in 0 until minSize) {
                            val formattedName = "${niks[i]} - ${names[i]}"
                            karyawanFormattedSet.add(formattedName)
                            AppLogger.d("Added formatted name: '$formattedName'")
                        }

                        if (names.size != niks.size) {
                            AppLogger.w("Mismatch between names count (${names.size}) and NIKs count (${niks.size})")
                        }
                    } else {
                        AppLogger.w("Missing karyawan_nama or karyawan_nik for record")
                    }
                }

                val karyawanNames = karyawanFormattedSet.toList().sorted()
                AppLogger.d("Total unique karyawan found: ${karyawanNames.size}")

                // Automatically add all karyawan to the adapter
                karyawanNames.forEach { formattedName ->
                    // Extract NIK and Name from formatted string
                    val dashIndex = formattedName.indexOf(" - ")
                    val selectedNik = if (dashIndex != -1) {
                        formattedName.substring(0, dashIndex).trim()
                    } else {
                        ""
                    }
                    val selectedName = if (dashIndex != -1) {
                        formattedName.substring(dashIndex + 3).trim()
                    } else {
                        formattedName.trim()
                    }

                    // Find the corresponding employee data
                    var selectedEmployee: PanenEntity? = null
                    var individualKaryawanId: String? = null

                    // Find by matching NIK and name combination
                    for (panenWithRelations in matchingPanenList) {
                        val panenEntity = panenWithRelations.panen ?: continue
                        val karyawanNik = panenEntity.karyawan_nik
                        val karyawanNama = panenEntity.karyawan_nama
                        val karyawanId = panenEntity.karyawan_id

                        if (!karyawanNik.isNullOrBlank() && !karyawanNama.isNullOrBlank() && !karyawanId.isNullOrBlank()) {
                            val niks = karyawanNik.split(",").map { it.trim() }
                            val names = karyawanNama.split(",").map { it.trim() }
                            val ids = karyawanId.split(",").map { it.trim() }

                            // Check if this employee record contains our selected NIK and name
                            val nikIndex = niks.indexOf(selectedNik)
                            val nameIndex = names.indexOf(selectedName)

                            if (nikIndex != -1 && nameIndex != -1 && nikIndex < ids.size) {
                                selectedEmployee = panenEntity
                                individualKaryawanId =
                                    ids[nikIndex] // Get the specific ID for this worker
                                AppLogger.d("Found matching employee: NIK='$selectedNik', Name='$selectedName', Individual ID='$individualKaryawanId'")
                                break
                            }
                        }
                    }

                    if (selectedEmployee != null && individualKaryawanId != null) {
                        // Add to maps using individual ID
                        karyawanIdMap[formattedName] = individualKaryawanId.toIntOrNull() ?: 0
                        kemandoranIdMap[formattedName] =
                            selectedEmployee.kemandoran_id.toIntOrNull() ?: 0

                        // Also add by NIK and name as keys
                        if (selectedNik.isNotEmpty()) {
                            karyawanIdMap[selectedNik] = individualKaryawanId.toIntOrNull() ?: 0
                            kemandoranIdMap[selectedNik] =
                                selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
                        }
                        if (selectedName.isNotEmpty()) {
                            karyawanIdMap[selectedName] = individualKaryawanId.toIntOrNull() ?: 0
                            kemandoranIdMap[selectedName] =
                                selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
                        }

                        // Create Worker with individual ID
                        val worker = Worker(individualKaryawanId, formattedName)
                        selectedPemanenAdapter.addWorker(worker)

                        AppLogger.d("Auto-added worker: $formattedName, Individual Karyawan ID: $individualKaryawanId")
                    } else {
                        AppLogger.e("Could not find employee data for: $formattedName")
                    }
                }

                // Set the description text with bold ancak and Indonesian date
                val ancakText = if (ancakValue.isNotBlank()) ancakValue else "tidak diketahui"
                val descriptionText =
                    "Panen sudah dilakukan ancak <b>$ancakText</b> pada <b>$indonesianDate</b> oleh :"
                descPemanenInspeksi.text =
                    Html.fromHtml(descriptionText, Html.FROM_HTML_MODE_COMPACT)

                AppLogger.d("Total workers added to adapter: ${selectedPemanenAdapter.getSelectedWorkers().size}")

            } catch (e: Exception) {
                AppLogger.e("Error processing TPH selection: ${e.message}")
            } finally {
                // Re-enable TPH selection after processing is complete
                isProcessingTPHSelection = false
            }
        }, 200)
    }

    override fun getCurrentlySelectedTPHId(): Int? {
        return selectedTPHIdByScan
    }


    private fun checkScannedTPHInsideRadius() {
        if (lat != null && lon != null) {
            val tphList = getTPHsInsideRadius(lat!!, lon!!, latLonMap)

            if (tphList.isNotEmpty()) {
                isEmptyScannedTPH = false
                tphScannedResultRecyclerView.visibility = View.VISIBLE
                titleScannedTPHInsideRadius.visibility = View.VISIBLE
                descScannedTPHInsideRadius.visibility = View.VISIBLE
                emptyScannedTPHInsideRadius.visibility = View.GONE
                tphScannedResultRecyclerView.adapter =
                    ListTPHInsideRadiusAdapter(tphList, this, jenisTPHListGlobal)


                val itemHeight = 50
                val maxHeight = 250

                val density = tphScannedResultRecyclerView.resources.displayMetrics.density
                val maxHeightPx = (maxHeight * density).toInt()
                val recyclerViewHeightPx = (tphList.size * itemHeight * density).toInt()

                tphScannedResultRecyclerView.layoutParams.height =
                    if (recyclerViewHeightPx > maxHeightPx) maxHeightPx else ViewGroup.LayoutParams.WRAP_CONTENT

                tphScannedResultRecyclerView.requestLayout()

                if (recyclerViewHeightPx > maxHeightPx) {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = true
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_ALWAYS
                } else {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = false
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
                }

                tphScannedResultRecyclerView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                tphScannedResultRecyclerView.isVerticalScrollBarEnabled = true

                if (selectedTPHIdByScan != null) {
                    for (i in tphList.indices) {
                        if (tphList[i].id == selectedTPHIdByScan) {
                            tphScannedResultRecyclerView.scrollToPosition(i)
                            break
                        }
                    }
                }
            } else {
                tphScannedResultRecyclerView.visibility = View.GONE
                titleScannedTPHInsideRadius.visibility = View.VISIBLE
                descScannedTPHInsideRadius.visibility = View.VISIBLE
                emptyScannedTPHInsideRadius.visibility = View.VISIBLE
                isEmptyScannedTPH = true
            }
        } else {
            Toasty.error(this, "Pastikan GPS mendapatkan titik Koordinat!", Toast.LENGTH_LONG, true)
                .show()
            isEmptyScannedTPH = true
        }

        if (progressBarScanTPHManual.visibility == View.VISIBLE) {
            progressBarScanTPHManual.visibility = View.GONE
        }

        if (progressBarScanTPHAuto.visibility == View.VISIBLE) {
            progressBarScanTPHAuto.visibility = View.GONE
        }
    }

    private val autoScanRunnable = object : Runnable {
        override fun run() {
            if (autoScanEnabled) {
                progressBarScanTPHAuto.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    checkScannedTPHInsideRadius()
                    autoScanHandler.postDelayed(this, autoScanInterval)
                }, 400)
            }
        }
    }

    private fun getTPHsInsideRadius(
        userLat: Double,
        userLon: Double,
        coordinates: Map<Int, ScannedTPHLocation>
    ): List<ScannedTPHSelectionItem> {
        val resultsList = mutableListOf<ScannedTPHSelectionItem>()

        // First, add all TPHs within radius
        for ((id, location) in coordinates) {

            val jenisTPHId = location.jenisTPHId.toInt()


            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                userLat,
                userLon,
                location.lat,
                location.lon,
                results
            )
            val distance = results[0]

            val tphData = inspectionStoredLocal[id]
            val selectedCount = tphData?.count ?: 0
            val isSelected = selectedCount > 0
            val isCurrentlySelected = id == selectedTPHIdByScan

            // Calculate the final limit to use
            val limit = tphData?.limitTPH?.toInt()

            // Include if within radius OR is the currently selected TPH
            if (distance <= radiusMinimum || isCurrentlySelected) {
                resultsList.add(
                    ScannedTPHSelectionItem(
                        id = id,
                        number = location.nomor,
                        blockCode = location.blokKode,
                        distance = distance,
                        isAlreadySelected = isSelected,
                        selectionCount = selectedCount,
                        canBeSelectedAgain = selectedCount < limit ?: 1,
                        isWithinRange = distance <= radiusMinimum,
                        jenisTPHId = jenisTPHId.toString(),
                        customLimit = limit.toString()
                    )
                )

            }
        }

        return resultsList.sortedBy { it.distance }
    }

    private fun animateLoadingDots(linearLayout: LinearLayout) {
        val loadingContainer = linearLayout.findViewById<LinearLayout>(R.id.loadingDotsContainer)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val dots = listOf(
            loadingContainer.findViewById<TextView>(R.id.dot1),
            loadingContainer.findViewById<TextView>(R.id.dot2),
            loadingContainer.findViewById<TextView>(R.id.dot3),
            loadingContainer.findViewById<TextView>(R.id.dot4)
        )

        spinner.visibility = View.INVISIBLE
        loadingContainer.visibility = View.VISIBLE

        // Animate each dot
        dots.forEachIndexed { index, dot ->
            val animation = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
            animation.duration = 600
            animation.repeatCount = ObjectAnimator.INFINITE
            animation.repeatMode = ObjectAnimator.REVERSE
            animation.startDelay = (index * 100).toLong() // Stagger the animations
            animation.start()
        }
    }

    private fun hideLoadingDots(linearLayout: LinearLayout) {
        val loadingContainer = linearLayout.findViewById<LinearLayout>(R.id.loadingDotsContainer)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        loadingContainer.visibility = View.GONE
        spinner.visibility = View.VISIBLE
    }

    @SuppressLint("InflateParams")
    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
    ) {
        val popupView =
            LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
        val listView = popupView.findViewById<ListView>(R.id.listViewChoices)
        val editTextSearch = popupView.findViewById<EditText>(R.id.searchEditText)

        val scrollView = findScrollView(linearLayout)
        val rootView = linearLayout.rootView

        // Create PopupWindow first
        val popupWindow = PopupWindow(
            popupView,
            spinner.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        var keyboardHeight = 0
        val rootViewLayout = rootView.viewTreeObserver
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height

            // Get keyboard height
            val newKeyboardHeight = screenHeight - rect.bottom

            if (newKeyboardHeight != keyboardHeight) {
                keyboardHeight = newKeyboardHeight
                if (keyboardHeight > 0) {
                    val spinnerLocation = IntArray(2)
                    spinner.getLocationOnScreen(spinnerLocation)

                    if (spinnerLocation[1] + spinner.height + popupWindow.height > rect.bottom) {
                        val scrollAmount = spinnerLocation[1] - 400
                        scrollView?.smoothScrollBy(0, scrollAmount)
                    }
                }
            }
        }

        rootViewLayout.addOnGlobalLayoutListener(layoutListener)

        popupWindow.setOnDismissListener {
            rootViewLayout.removeOnGlobalLayoutListener(layoutListener)
        }

        var filteredData = data
        val adapter = object : ArrayAdapter<String>(
            spinner.context,
            android.R.layout.simple_list_item_1,
            filteredData
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK)
                return view
            }
        }
        listView.adapter = adapter

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val titleSearch = popupView.findViewById<TextView>(R.id.titleSearchDropdown)

                filteredData = if (!s.isNullOrEmpty()) {
                    titleSearch.visibility = View.VISIBLE
                    data.filter { it.contains(s, ignoreCase = true) }
                } else {
                    titleSearch.visibility = View.GONE
                    data
                }

                val filteredAdapter = object : ArrayAdapter<String>(
                    spinner.context,
                    android.R.layout.simple_list_item_1,
                    if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                        listOf("Data tidak tersedia!")
                    } else {
                        filteredData
                    }
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)

                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.colorRedDark
                                )
                            )
                            textView.setTypeface(textView.typeface, Typeface.ITALIC)
                            view.isEnabled = false
                        } else {
                            textView.setTextColor(Color.BLACK)
                            textView.setTypeface(textView.typeface, Typeface.NORMAL)
                            view.isEnabled = true
                        }
                        return view
                    }

                    override fun isEnabled(position: Int): Boolean {
                        return filteredData.isNotEmpty()
                    }
                }
                listView.adapter = filteredAdapter
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = filteredData[position]
            spinner.text = selectedItem
            editText.setText(selectedItem)
            handleItemSelection(linearLayout, position, selectedItem)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(spinner)

        editTextSearch.requestFocus()
        val imm =
            spinner.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun validateAndShowErrors(): Boolean {
        var isValid = true
        val missingFields = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

//        if (!locationEnable || lat == 0.0 || lon == 0.0 || lat == null || lon == null) {
//            isValid = false
//            errorMessages.add(stringXML(R.string.al_location_description_failed))
//            missingFields.add("Location")
//        }
//
//        inputMappings.forEach { (layout, key, inputType) ->
//            if (layout.id != R.id.layoutKemandoranLain && layout.id != R.id.layoutPemanenLain) {
//
//                val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
//                val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)
//                val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
//                val editText = layout.findViewById<EditText>(R.id.etHomeMarkerTPH)
//
//                val isEmpty = when (inputType) {
//                    InputType.SPINNER -> {
//                        when (layout.id) {
//                            R.id.lyAfdInspect -> selectedAfdeling.isEmpty()
//                            R.id.lyJalurInspect -> selectedJalurMasuk.isEmpty()
//                            else -> spinner.selectedIndex == -1
//                        }
//                    }
//
//                    InputType.EDITTEXT -> {
//                        when (layout.id) {
//                            R.id.lyBaris1Inspect -> br1Value.trim().isEmpty()
//                            R.id.lyBaris2Inspect -> if (selectedKondisiValue.toInt() != 2) br2Value.trim()
//                                .isEmpty() else false
//
//                            else -> editText.text.toString().trim().isEmpty()
//                        }
//                    }
//
//                    InputType.RADIO -> {
//                        when (layout.id) {
//                            R.id.lyInspectionType -> selectedInspeksiValue.isEmpty()
//                            R.id.lyConditionType -> selectedKondisiValue.isEmpty()
//                            else -> false
//                        }
//                    }
//
//                }
//
//                if (isEmpty) {
//                    tvError.visibility = View.VISIBLE
//                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.colorRedDark)
//                    missingFields.add(key)
//                    isValid = false
//                } else {
//                    tvError.visibility = View.GONE
//                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.graytextdark)
//                }
//            }
//        }
//
//
//        if (selectedTPHIdByScan == null && selectedAfdeling.isNotEmpty()) {
//            if (isTriggeredBtnScanned) {
//                // Button was triggered - show appropriate message based on search results
//                if (isEmptyScannedTPH) {
//                    // Search was done but no TPH found
//                    tvErrorScannedNotSelected.text = stringXML(R.string.al_no_tph_detected_trigger_submit)
//                    tvErrorScannedNotSelected.visibility = View.VISIBLE
//                    errorMessages.add(stringXML(R.string.al_no_tph_detected_trigger_submit))
//                } else {
//                    // Search was done and TPH found, but user hasn't selected one
//                    tvErrorScannedNotSelected.text = "Silakan untuk memilih TPH yang ingin diperiksa!"
//                    tvErrorScannedNotSelected.visibility = View.VISIBLE
//                    errorMessages.add("Silakan untuk memilih TPH yang ingin diperiksa!")
//                }
//            } else {
//                // Button not triggered yet - ask user to search first
//                tvErrorScannedNotSelected.text = "Silakan tekan tombol scan untuk mencari TPH"
//                tvErrorScannedNotSelected.visibility = View.VISIBLE
//                errorMessages.add("Silakan tekan tombol scan untuk mencari TPH")
//            }
//        } else {
//            // TPH is selected or no afdeling selected - hide error
//            tvErrorScannedNotSelected.visibility = View.GONE
//        }
//
//        // NEW: Simple validation - br1 and br2 cannot be the same when kondisi == 0
//        if (selectedKondisiValue.toInt() == 1 && br1Value.trim().isNotEmpty() && br2Value.trim().isNotEmpty()) {
//            val br1Int = br1Value.trim().toIntOrNull() ?: 0
//            val br2Int = br2Value.trim().toIntOrNull() ?: 0
//
//            AppLogger.d(br1Int.toString())
//            AppLogger.d(br2Int.toString())
//            if (br1Int == br2Int) {
//                val layoutBaris2 = findViewById<LinearLayout>(R.id.lyBaris2Inspect)
//                val tvErrorBaris2 = layoutBaris2.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
//                val mcvBaris2 = layoutBaris2.findViewById<MaterialCardView>(R.id.MCVSpinner)
//
//                tvErrorBaris2.text = "Baris pertama dan Baris kedua tidak boleh sama"
//                tvErrorBaris2.visibility = View.VISIBLE
//                mcvBaris2.strokeColor = ContextCompat.getColor(this, R.color.colorRedDark)
//                errorMessages.add("Baris pertama dan Baris kedua tidak boleh sama")
//                isValid = false
//            }
//        }
//
//        if (!isValid) {
//            vibrate(500)
//            val combinedErrorMessage = buildString {
//                val allMessages = mutableListOf<String>()
//                if (missingFields.isNotEmpty()) {
//                    allMessages.add(stringXML(R.string.al_pls_complete_data))
//                }
//
//                allMessages.addAll(errorMessages)
//                allMessages.forEachIndexed { index, message ->
//                    append("${index + 1}. $message")
//                    if (index < allMessages.size - 1) append("\n")
//                }
//            }
//
//            AlertDialogUtility.withSingleAction(
//                this,
//                stringXML(R.string.al_back),
//                stringXML(R.string.al_data_not_completed),
//                combinedErrorMessage,
//                "warning.json",
//                R.color.colorRedDark
//            ) {}
//        }

        return isValid
    }

    private fun findScrollView(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
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


    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun isPermissionPermanentlyDenied(): Boolean {
        val sharedPref = this.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
        val firstRequest = sharedPref.getBoolean("first_camera_request", true)

        if (firstRequest) {
            sharedPref.edit().putBoolean("first_camera_request", false).apply()
            return false
        }

        return !ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.CAMERA
        )
    }

    private fun showSnackbarWithSettings(message: String) {
        Snackbar.make(this.findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Settings") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", this.packageName, null)
                )
                this.startActivity(intent)
            }
            .show()
    }

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        pageForm: Int,
        komentar: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        if (shouldReopenBottomSheet) {
            shouldReopenBottomSheet = false

            bottomNavInspect.visibility = View.VISIBLE

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val currentData =
                formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()

            formAncakViewModel.savePageData(
                currentPage,
                currentData.copy(photo = fname, latIssue = lat, lonIssue = lon)
            )

            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(fname)
            }, 100)
        }
    }
}