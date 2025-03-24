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
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
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
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.ui.adapter.FormAncakPagerAdapter
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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaredrummler.materialspinner.MaterialSpinner
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
class FormInspectionActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    data class SummaryItem(val title: String, val value: String)
    data class Location(val lat: Double = 0.0, val lon: Double = 0.0)

    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null

    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var infoApp: String = ""

    private var lat: Double? = null
    private var lon: Double? = null
    private var currentAccuracy: Float = 0F

    private var shouldReopenBottomSheet = false

    private var panenStoredLocal: MutableList<Int> = mutableListOf()
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
    private var tphList: List<TPHNewModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var karyawanLainList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()

    private val karyawanIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranIdMap: MutableMap<String, Int> = mutableMapOf()
    private val karyawanLainIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranLainIdMap: MutableMap<String, Int> = mutableMapOf()

    private var totalPokokInspection = 0
    private var jumBrdTglPath = 0
    private var jumBuahTglPath = 0

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

    private fun setupUI() {
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)

        initViewModel()

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

                    panenViewModel.loadActivePanenESPB()
                    delay(100)

                    withContext(Dispatchers.Main) { // Ensure observation is on main thread
                        panenViewModel.activePanenList.observe(this@FormInspectionActivity) { list ->
                            panenDeferred.complete(list ?: emptyList()) // Ensure it's never null
                        }
                    }

                    val panenList = panenDeferred.await()
                    panenStoredLocal = panenList
                        .mapNotNull { it.tph?.id }
                        .toMutableList()

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
            val totalPages =
                formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION

            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val pageData = formData[currentPage]
            val emptyTreeValue = pageData?.emptyTree ?: 0
            val photoValue = pageData?.photo ?: ""

            when {
                selectedInspeksiValue.toInt() == 1 && emptyTreeValue == 2 && photoValue.isEmpty() -> {
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

                else -> {
                    val validationResult =
                        formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())
                    if (validationResult.isValid && nextPage <= totalPages) {
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

                                        // Get the current location from user each 10 tree
                                        if (currentPage % 10 == 0 && !trackingLocation.containsKey(
                                                currentPage.toString()
                                            )
                                        ) {
                                            isTenthTrees = true
                                            trackingLocation[currentPage.toString()] =
                                                Location(lat ?: 0.0, lon ?: 0.0)
                                        } else if (isTenthTrees) {
                                            isTenthTrees = false
                                        }

                                        formAncakViewModel.nextPage()

                                        Handler(Looper.getMainLooper()).postDelayed({
                                            if (loadingDialog.isShowing) {
                                                scrollToTopOfFormAncak()
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
                    } else {
                        vibrate(500)
                        return@setOnClickListener
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
            val validationResult =
                formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())
            if (validationResult.isValid) {
                showViewPhotoBottomSheet()
            } else {
                vibrate(500)
            }
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
                            val uniqueNikPemanen = (selectedNikPemanenIds + selectedNikPemanenLainIds)
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
                                        inspectionViewModel.deleteInspectionDatas(listOf(uniquePathId))
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
                                        inspectionViewModel.deleteInspectionDatas(listOf(uniquePathId))
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
                                            currentPage.toString(),
                                            ivAddPhoto,
                                            currentPage,
                                            null,
                                            "", // soon assign lat lon
                                            currentPage.toString(),
                                            WaterMarkFotoDanFolder.WMInspeksi
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
                                currentPage.toString(),
                                ivAddPhoto,
                                currentPage,
                                null,
                                "", // soon assign lat lon
                                currentPage.toString(),
                                WaterMarkFotoDanFolder.WMInspeksi
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

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = "",
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
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
                setupSwitch()
                setupPressedFAB()
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
                        val validationResult = formAncakViewModel.validateCurrentPage(selectedInspeksiValue.toInt())
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
                fabPhotoFormAncak.visibility =
                    if (selectedInspeksiValue.toInt() == 1) View.VISIBLE else View.GONE

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
                findViewById(R.id.lyTtInspect),
                getString(R.string.field_tahun_tanam),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyBlokInspect),
                getString(R.string.field_blok),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyAncakInspect),
                getString(R.string.field_ancak),
                InputType.EDITTEXT
            ),
            Triple(
                findViewById(R.id.lyNoTphInspect),
                getString(R.string.field_no_tph),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyStatusPanenInspect),
                "Status Panen",
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
                findViewById(R.id.lyMandor1Inspect),
                "Kemandoran",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyPemanen1Inspect),
                "Pemanen",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyMandor2Inspect),
                "Kemandoran Lain",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyPemanen2Inspect),
                "Pemanen Lain",
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
                            val namaEstate = listOf(prefManager!!.estateUserLengkapLogin ?: "")
                            setupSpinnerView(layoutView, namaEstate)
                            findViewById<MaterialSpinner>(R.id.spPanenTBS).selectedIndex = 0
                        }

                        R.id.lyAfdInspect -> {
                            val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
                            setupSpinnerView(layoutView, divisiNames)
                        }

                        R.id.lyStatusPanenInspect -> setupSpinnerView(
                            layoutView,
                            (listRadioItems["StatusPanen"] ?: emptyMap()).values.toList()
                        )

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

        rvSelectedPemanen = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.top_margin)
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        selectedPemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = selectedPemanenAdapter

        val layoutPemanen = findViewById<LinearLayout>(R.id.lyPemanen1Inspect)
        val parentLayout = layoutPemanen.parent as ViewGroup
        val index = parentLayout.indexOfChild(layoutPemanen)
        parentLayout.addView(rvSelectedPemanen, index + 1)

        rvSelectedPemanenLain = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.top_margin)
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        selectedPemanenLainAdapter = SelectedWorkerAdapter()
        rvSelectedPemanenLain.adapter = selectedPemanenLainAdapter

        val layoutPemanenLain = findViewById<LinearLayout>(R.id.lyPemanen2Inspect)
        val parentLayoutLain = layoutPemanenLain.parent as ViewGroup
        val index2 = parentLayoutLain.indexOfChild(layoutPemanenLain)
        parentLayoutLain.addView(rvSelectedPemanenLain, index2 + 1)
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

        if (linearLayout.id == R.id.lyMandor1Inspect || linearLayout.id == R.id.lyPemanen1Inspect || linearLayout.id == R.id.lyMandor2Inspect || linearLayout.id == R.id.lyPemanen2Inspect) {
            spinner.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    showPopupSearchDropdown(
                        spinner,
                        data,
                        editText,
                        linearLayout
                    )
                }
                true
            }
        }


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
                R.id.lyAncakInspect,
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
                    R.id.lyAncakInspect -> {
                        ancakValue = s?.toString()?.trim() ?: ""
                    }

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

    private fun setupSwitch() {
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.smAnotherKemandoran)
        val layoutKemandoranLain = findViewById<LinearLayout>(R.id.lyMandor2Inspect)
        val layoutPemanenLain = findViewById<LinearLayout>(R.id.lyPemanen2Inspect)

        switchAsistensi.setOnCheckedChangeListener { _, isChecked ->
            layoutKemandoranLain.visibility = if (isChecked) View.VISIBLE else View.GONE
            layoutPemanenLain.visibility = if (isChecked) View.VISIBLE else View.GONE

            asistensi = if (isChecked) 1 else 0

            if (!isChecked) {
                selectedPemanenLainAdapter.clearAllWorkers()
            }
        }
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
                resetDependentSpinners(linearLayout.rootView)

                selectedAfdeling = selectedItem
                selectedAfdelingIdSpinner = position

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
                        animateLoadingDots(linearLayout)
                        delay(1000) // 1 second delay
                    }

                    try {
                        if (estateId == null || selectedDivisiId == null) {
                            throw IllegalStateException("Estate ID or selectedDivisiId is null!")
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

                        val tahunTanamList = try {
                            blokList.mapNotNull { it.tahun }.distinct()
                                .sortedBy { it.toIntOrNull() }
                        } catch (e: Exception) {
                            emptyList()
                        }

                        withContext(Dispatchers.Main) {
                            try {
                                val layoutTahunTanam =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyTtInspect)
                                val layoutKemandoran =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyMandor1Inspect)
                                val layoutKemandoranLain =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyMandor2Inspect)

                                setupSpinnerView(
                                    layoutTahunTanam,
                                    if (tahunTanamList.isNotEmpty()) tahunTanamList else emptyList()
                                )

                                val kemandoranNames = kemandoranList.map { it.nama }
                                setupSpinnerView(
                                    layoutKemandoran,
                                    if (kemandoranNames.isNotEmpty()) kemandoranNames as List<String> else emptyList()
                                )

                                val kemandoranLainListNames = kemandoranLainList.map { it.nama }
                                setupSpinnerView(
                                    layoutKemandoranLain,
                                    if (kemandoranLainListNames.isNotEmpty()) kemandoranLainListNames as List<String> else emptyList()
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error updating UI: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
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

            R.id.lyTtInspect -> {
                resetTPHSpinner(linearLayout.rootView)
                val selectedTahunTanam = selectedItem
                selectedTahunTanamValue = selectedTahunTanam

                val filteredBlokCodes = blokList.filter {
                    it.dept == estateId!!.toInt() &&
                            it.divisi == selectedDivisiValue &&
                            it.tahun == selectedTahunTanamValue
                }

                val layoutBlok =
                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyBlokInspect)
                if (filteredBlokCodes.isNotEmpty()) {
                    val blokNames = filteredBlokCodes.map { it.blok_kode }
                    setupSpinnerView(layoutBlok, blokNames as List<String>)
                    layoutBlok.visibility = View.VISIBLE
                } else {
                    setupSpinnerView(layoutBlok, emptyList())
                }
            }

            R.id.lyBlokInspect -> {
                resetTPHSpinner(linearLayout.rootView)
                selectedBlok = selectedItem

                val selectedFieldId = try {
                    blokList.find { blok ->
                        blok.dept == estateId?.toIntOrNull() &&
                                blok.divisi == selectedDivisiValue &&
                                blok.tahun == selectedTahunTanamValue &&
                                blok.blok_kode == selectedBlok
                    }?.blok
                } catch (e: Exception) {
                    null
                }

                if (selectedFieldId != null) {
                    selectedBlokValue = selectedFieldId
                } else {
                    selectedBlokValue = null
                    return
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        animateLoadingDots(linearLayout)
                        delay(1000)
                    }

                    try {
                        if (estateId == null || selectedDivisiValue == null || selectedTahunTanamValue == null || selectedBlokValue == null) {
                            throw IllegalStateException("One or more required parameters are null!")
                        }

                        val tphDeferred = async {
                            datasetViewModel.getTPHList(
                                estateId!!.toInt(),
                                selectedDivisiValue!!,
                                selectedTahunTanamValue!!,
                                selectedBlokValue!!
                            )
                        }

                        tphList = tphDeferred.await() // Avoid null crash

                        //exclude no tph yang sudah pernah dipilih atau di store di database
                        val storedTPHIds =
                            panenStoredLocal.toSet()

                        val filteredTPHList = tphList.filter {
                            val isExcluded = it.id in storedTPHIds
                            !isExcluded
                        }
                        val noTPHList = filteredTPHList.map { it.nomor }

                        withContext(Dispatchers.Main) {
                            val layoutNoTPH =
                                linearLayout.rootView.findViewById<LinearLayout>(R.id.lyNoTphInspect)

                            if (noTPHList.isNotEmpty()) {
                                setupSpinnerView(layoutNoTPH, noTPHList as List<String>)
                            } else {
                                setupSpinnerView(layoutNoTPH, emptyList())
                            }
                        }
                    } catch (e: Exception) {
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

            R.id.lyNoTphInspect -> {
                selectedTPH = selectedItem

                val selectedTPHId = try {
                    tphList.find {
                        it.dept == estateId?.toIntOrNull() && // Safe conversion to prevent crashes
                                it.divisi == selectedDivisiValue &&
                                it.blok == selectedBlokValue &&
                                it.tahun == selectedTahunTanamValue &&
                                it.nomor == selectedTPH
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected TPH ID: ${e.message}")
                    null
                }

                if (selectedTPHId != null) {
                    if (!panenStoredLocal.contains(selectedTPHId)) {
                        panenStoredLocal.add(selectedTPHId)
                    }

                    selectedTPHValue = selectedTPHId
                } else {
                    selectedTPHValue = null
                }
            }

            R.id.lyStatusPanenInspect -> {
                val mapData = listRadioItems["StatusPanen"] ?: emptyMap()
                val selectedKey = mapData.entries.find { it.value == selectedItem }?.key
                selectedStatusPanen = selectedKey ?: ""
            }

            R.id.lyJalurInspect -> {
                val mapData = listRadioItems["EntryPath"] ?: emptyMap()
                val selectedKey = mapData.entries.find { it.value == selectedItem }?.key
                selectedJalurMasuk = selectedKey ?: ""
            }

            R.id.lyMandor1Inspect -> {
                selectedPemanenAdapter.clearAllWorkers()
                selectedKemandoran = selectedItem

                val filteredKemandoranId: Int? = try {
                    kemandoranList.find {
                        it.dept == estateId?.toIntOrNull() && // Avoids force unwrap (!!)
                                it.divisi == selectedDivisiValue &&
                                it.nama == selectedKemandoran
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding Kemandoran ID: ${e.message}")
                    null
                }

                if (filteredKemandoranId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }

                        try {
                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(filteredKemandoranId)
                            }

                            karyawanList = karyawanDeferred.await()
                            val karyawanNames = karyawanList
                                .sortedBy { it.nama } // Sort by name alphabetically
                                .map { "${it.nama} - ${it.nik}" }

                            withContext(Dispatchers.Main) {
                                val layoutPemanen =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyPemanen1Inspect)
                                if (karyawanNames.isNotEmpty()) {
                                    setupSpinnerView(layoutPemanen, karyawanNames)
                                } else {
                                    setupSpinnerView(layoutPemanen, emptyList())
                                }
                            }

                        } catch (e: Exception) {
                            AppLogger.e("Error fetching afdeling data: ${e.message}")
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
                } else {
                    AppLogger.e("Filtered Kemandoran ID is null, skipping data fetch.")
                }
            }

            R.id.lyPemanen1Inspect -> {
                selectedPemanen = selectedItem

                val selectedNama = selectedPemanen.substringBefore(" - ").trim()
                val karyawanNikMap = karyawanList.associateBy({ it.nama!!.trim() }, { it.nik!! })
                karyawanList.forEach {
                    it.nama?.trim()?.let { nama ->
                        karyawanIdMap[nama] = it.id!!
                        kemandoranIdMap[nama] = it.kemandoran_id!!
                    }
                }

                val selectedPemanenId = karyawanNikMap[selectedNama]
                if (selectedPemanenId != null) {
                    val worker = Worker(selectedPemanenId.toString(), selectedPemanen)
                    selectedPemanenAdapter.addWorker(worker)

                    val availableWorkers = selectedPemanenAdapter.getAvailableWorkers()
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }
                }
            }

            R.id.lyMandor2Inspect -> {
                selectedPemanenLainAdapter.clearAllWorkers()
                selectedKemandoranLain = selectedItem

                val selectedIdKemandoranLain: Int? = try {
                    kemandoranLainList.find {
                        it.nama == selectedKemandoranLain
                    }?.id
                } catch (e: Exception) {
                    null
                }

                if (selectedIdKemandoranLain != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }

                        try {
                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(selectedIdKemandoranLain)
                            }

                            karyawanLainList = karyawanDeferred.await()
                            val namaKaryawanKemandoranLain =
                                karyawanLainList.sortedBy { it.nama } // Sort by name alphabetically
                                    .map { "${it.nama} - ${it.nik}" }

                            withContext(Dispatchers.Main) {
                                val layoutPemanenLain =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.lyPemanen2Inspect)
                                if (namaKaryawanKemandoranLain.isNotEmpty()) {
                                    setupSpinnerView(
                                        layoutPemanenLain,
                                        namaKaryawanKemandoranLain
                                    )
                                } else {
                                    setupSpinnerView(layoutPemanenLain, emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FormInspectionActivity,
                                    "Error loading kemandoran lain data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                hideLoadingDots(linearLayout)
                            }
                        }
                    }
                } else {
                    AppLogger.e("Selected ID Kemandoran Lain is null, skipping data fetch.")
                }
            }

            R.id.lyPemanen2Inspect -> {
                selectedPemanenLain = selectedItem

                val selectedNamaPemanenLain = selectedPemanenLain.substringBefore(" - ").trim()
                val karyawanLainNikMap =
                    karyawanLainList.associateBy({ it.nama!!.trim() }, { it.nik!! })
                karyawanLainList.forEach {
                    it.nama?.trim()?.let { nama ->
                        karyawanLainIdMap[nama] = it.id!!
                        kemandoranLainIdMap[nama] = it.kemandoran_id!!
                    }
                }

                val selectedPemanenLainId = karyawanLainNikMap[selectedNamaPemanenLain]
                if (selectedPemanenLainId != null) {
                    val worker = Worker(selectedPemanenLainId.toString(), selectedPemanenLain)
                    selectedPemanenLainAdapter.addWorker(worker)

                    val availableWorkers = selectedPemanenLainAdapter.getAvailableWorkers()
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })
                    }
                }
            }
        }
    }

    private fun resetTPHSpinner(rootView: View) {
        val layoutNoTPH = rootView.findViewById<LinearLayout>(R.id.lyNoTphInspect)
        setupSpinnerView(layoutNoTPH, emptyList())
        tphList = emptyList()
        selectedTPH = ""
        selectedTPHValue = null
    }

    private fun resetDependentSpinners(rootView: View) {
        // List of all dependent layouts that need to be reset
        val dependentLayouts = listOf(
            R.id.lyTtInspect,
            R.id.lyBlokInspect,
            R.id.lyNoTphInspect,
            R.id.lyMandor1Inspect,
            R.id.lyPemanen1Inspect,
            R.id.lyMandor2Inspect,
            R.id.lyPemanen2Inspect
        )

        // Reset each dependent spinner
        dependentLayouts.forEach { layoutId ->
            val layout = rootView.findViewById<LinearLayout>(layoutId)
            setupSpinnerView(layout, emptyList())
        }

        // Reset related data
        blokList = emptyList()
        kemandoranList = emptyList()
        kemandoranLainList = emptyList()
        tphList = emptyList()
        karyawanList = emptyList()
        karyawanLainList = emptyList()


        // Reset selected values
        selectedTahunTanamValue = null
        selectedBlok = ""
        selectedBlokValue = null
        selectedTPH = ""
        selectedTPHValue = null
        selectedKemandoranLain = ""

        // Clear adapters if they exist
        selectedPemanenAdapter.clearAllWorkers()
        selectedPemanenLainAdapter.clearAllWorkers()
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

        if (!locationEnable || lat == 0.0 || lon == 0.0 || lat == null || lon == null) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_location_description_failed))
            missingFields.add("Location")
        }

        val switchAsistensi = findViewById<SwitchMaterial>(R.id.smAnotherKemandoran)
        val isAsistensiEnabled = switchAsistensi.isChecked

        inputMappings.forEach { (layout, key, inputType) ->
            if (layout.id != R.id.layoutKemandoranLain && layout.id != R.id.layoutPemanenLain) {

                val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
                val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)
                val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                val editText = layout.findViewById<EditText>(R.id.etHomeMarkerTPH)

                val isEmpty = when (inputType) {
                    InputType.SPINNER -> {
                        when (layout.id) {
                            R.id.lyEstInspect -> estateName!!.isEmpty()
                            R.id.lyAfdInspect -> selectedAfdeling.isEmpty()
                            R.id.lyTtInspect -> selectedTahunTanamValue?.isEmpty() ?: true
                            R.id.lyBlokInspect -> selectedBlok.isEmpty()
                            R.id.lyNoTphInspect -> selectedTPH.isEmpty()
                            R.id.lyStatusPanenInspect -> selectedStatusPanen.isEmpty()
                            R.id.lyJalurInspect -> selectedJalurMasuk.isEmpty()
                            R.id.lyMandor1Inspect -> selectedKemandoran.isEmpty()
                            R.id.lyPemanen1Inspect -> selectedPemanen.isEmpty()
                            else -> spinner.selectedIndex == -1
                        }
                    }

                    InputType.EDITTEXT -> {
                        when (layout.id) {
                            R.id.lyAncakInspect -> ancakValue.trim().isEmpty()
                            R.id.lyBaris1Inspect -> br1Value.trim().isEmpty()
                            R.id.lyBaris2Inspect -> if (selectedKondisiValue.toInt() != 2) br2Value.trim()
                                .isEmpty() else false

                            else -> editText.text.toString().trim().isEmpty()
                        }
                    }

                    InputType.RADIO -> {
                        when (layout.id) {
                            R.id.lyInspectionType -> selectedInspeksiValue.isEmpty()
                            R.id.lyConditionType -> selectedKondisiValue.isEmpty()
                            else -> false
                        }
                    }

                }

                if (isEmpty) {
                    tvError.visibility = View.VISIBLE
                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.colorRedDark)
                    missingFields.add(key)
                    isValid = false
                } else {
                    tvError.visibility = View.GONE
                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.graytextdark)
                }
            }
        }

        if (isAsistensiEnabled) {
            val layoutKemandoranLain = findViewById<LinearLayout>(R.id.lyMandor2Inspect)
            val layoutPemanenLain = findViewById<LinearLayout>(R.id.lyPemanen2Inspect)

            val isKemandoranLainEmpty = selectedKemandoranLain.isEmpty()
            val isPemanenLainEmpty = selectedPemanenLainAdapter.itemCount == 0

            if (isKemandoranLainEmpty || isPemanenLainEmpty) {
                if (isKemandoranLainEmpty) {
                    layoutKemandoranLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                        View.VISIBLE
                    layoutKemandoranLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                        ContextCompat.getColor(this, R.color.colorRedDark)
                    missingFields.add(getString(R.string.field_kemandoran_lain))
                }

                if (isPemanenLainEmpty) {
                    layoutPemanenLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                        View.VISIBLE
                    layoutPemanenLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                        ContextCompat.getColor(this, R.color.colorRedDark)
                    missingFields.add(getString(R.string.field_pemanen_lain))
                }
                isValid = false
            }
        }

        if (!isValid) {
            vibrate(500)
            val combinedErrorMessage = buildString {
                val allMessages = mutableListOf<String>()
                if (missingFields.isNotEmpty()) {
                    allMessages.add(stringXML(R.string.al_pls_complete_data))
                }

                allMessages.addAll(errorMessages)
                allMessages.forEachIndexed { index, message ->
                    append("${index + 1}. $message")
                    if (index < allMessages.size - 1) append("\n")
                }
            }

            AlertDialogUtility.withSingleAction(
                this,
                stringXML(R.string.al_back),
                stringXML(R.string.al_data_not_completed),
                combinedErrorMessage,
                "warning.json",
                R.color.colorRedDark
            ) {}
        }

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
        komentar: String?
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