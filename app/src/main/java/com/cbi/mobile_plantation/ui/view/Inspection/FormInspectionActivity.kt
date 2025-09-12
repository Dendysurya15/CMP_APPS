package com.cbi.mobile_plantation.ui.view.Inspection

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
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
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
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
import android.widget.CheckBox
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
import android.text.InputType as AndroidInputType
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.ui.fragment.FormAncakFragment
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.utils.SoftKeyboardStateWatcher
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.data.repository.CameraRepository.CameraType
import com.cbi.mobile_plantation.data.repository.CameraRepository.CameraType.*
import com.cbi.mobile_plantation.ui.adapter.FormAncakPagerAdapter
import com.cbi.mobile_plantation.ui.adapter.ListTPHInsideRadiusAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.TakeFotoPreviewAdapter.Companion.CAMERA_PERMISSION_REQUEST_CODE
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.followUpInspeksi.ListFollowUpInspeksi
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel.InspectionParameterItem
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
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.w3c.dom.Text
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")
open class FormInspectionActivity : AppCompatActivity(),
    CameraRepository.PhotoCallback,
    ListTPHInsideRadiusAdapter.OnTPHSelectedListener {
    private var isEmptyScannedTPH = true

    data class SummaryItem(val title: String, val value: String)
    data class Location(val lat: Double = 0.0, val lon: Double = 0.0)

    private var parameterInspeksi: List<InspectionParameterItem> = emptyList()
    private var afdelingNameUser: String? = null
    private lateinit var alertCardScanRadius: MaterialCardView
    private lateinit var alertTvScannedRadius: TextView
    private lateinit var btnScanTPHRadius: MaterialButton
    private lateinit var btnMulaiDariTPH: MaterialButton
    private lateinit var btnMulaiDariPokok: MaterialButton
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var selectionScreen: LinearLayout
    private lateinit var mainContentWrapper: ConstraintLayout
    private lateinit var headerFormInspection: View
    private var prefManager: PrefManager? = null
    var selectedKemandoranId = 0
    private var radiusMinimum = 0F
    private var boundaryAccuracy = 0F
    private var featureName: String? = null
    private var dept_abbr_pasar_tengah: String? = null
    private var divisi_abbr_pasar_tengah: String? = null
    private var blok_kode_pasar_tengah: String? = null
    private var last_pokok_before_pasar_tengah: Int? = 1
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var estateAbbr: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var photoInTPH: String? = null
    private var komentarInTPH: String? = null
    private var infoApp: String = ""
    private var jenisTPHListGlobal: List<JenisTPHModel> = emptyList()
    private var lat: Double? = null
    private var lon: Double? = null
    private var currentAccuracy: Float = 0F
    private var selectedTPHIdByScan: Int? = null
    private var selectedIdPanenByScan: String? = null
    private var selectedEstateByScan: String? = null
    private var selectedBlokByScan: String? = null
    private var selectedAfdelingByScan: String? = null
    private var selectedTPHNomorByScan: Int? = null
    private var selectedAncakByScan: String? = null
    private var selectedTanggalPanenByScan: String? = null
    private lateinit var titleScannedTPHInsideRadius: TextView
    private lateinit var descScannedTPHInsideRadius: TextView
    private lateinit var emptyScannedTPHInsideRadius: TextView
    private lateinit var progressBarScanTPHManual: ProgressBar
    private lateinit var progressBarScanTPHAuto: ProgressBar
    private lateinit var lyEstInspect: LinearLayout
    private lateinit var lyAfdInspect: LinearLayout
    private lateinit var lyKemandoran: LinearLayout
    private lateinit var lyPemuat: LinearLayout
    private var shouldReopenBottomSheet = false
    private var isTriggeredBtnScanned = false
    private lateinit var switchAutoScan: SwitchMaterial
    private lateinit var layoutAutoScan: LinearLayout
    private var autoScanEnabled = false
    private val autoScanHandler = Handler(Looper.getMainLooper())
    private val autoScanInterval = 5000L
    private lateinit var tvErrorScannedNotSelected: TextView
    private var dateStartInspection: String = ""
    private var inspectionId: String? = null
    private var panenTPH: List<PanenEntityWithRelations> = emptyList()

    private data class TPHData(
        val count: Int,
        val jenisTPHId: Int = 1,
        val limitTPH: String? = null
    )

    private val trackingLocation: MutableMap<String, Location> = mutableMapOf()
    private val listRadioItems: Map<String, Map<String, String>> = mapOf(
        "InspectionType" to mapOf(
            "1" to "Inspeksi",
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
    private var currentInspectionData: InspectionWithDetailRelations? = null
    private var isTenthTrees = false
    private var isSnackbarShown = false
    private var locationEnable: Boolean = false
    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>
    private var hasInspectionStarted = false
    private var divisiList: List<TPHNewModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()
    private var pemuatList: List<KaryawanModel> = emptyList()
    private val karyawanIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranIdMap: MutableMap<String, Int> = mutableMapOf()

    private var totalPokokInspection = 0
    private var totalHarvestTree = 0
    private var jumBrdTglPath = 0
    private var jumBuahTglPath = 0
    private var latLonMap: Map<Int, ScannedTPHLocation> = emptyMap()
    private lateinit var tphScannedResultRecyclerView: RecyclerView
    private var selectedAfdeling: String = ""
    private var selectedAfdelingIdSpinner: Int = 0
    private var selectedEstate: String = ""
    private var selectedEstateIdSpinner: Int = 0
    private var selectedDivisiValue: Int? = null
    private var selectedTPHValue: Int? = null
    private var selectedJalurMasuk: String = ""

    private var selectedKondisiValue: String = ""

    private var isInTPH: Boolean = true
    private var br1Value: String = ""
    private var br2Value: String = ""
    private var isForFollowUp = false
    private var isBottomSheetOpen = false
    private var isCameraViewOpen = false
    private var keyboardOpenedWhileBottomSheetVisible = false
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var blokList: List<BlokModel> = emptyList()
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var formAncakViewModel: FormAncakViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var inspectionViewModel: InspectionViewModel

    private lateinit var formAncakPagerAdapter: FormAncakPagerAdapter
    private lateinit var keyboardWatcher: SoftKeyboardStateWatcher
    private var allAvailableKaryawanList: List<KaryawanInfo> = emptyList()
    private var allManualKaryawanList: List<KaryawanInfo> = emptyList()
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter // For automatic
    private lateinit var selectedPemanenManualAdapter: SelectedWorkerAdapter // For manual
    private lateinit var selectedPemuatAdapter: SelectedWorkerAdapter
    private var allPemuatEmployees: List<KaryawanModel> = emptyList()

    private lateinit var infoBlokView: ScrollView
    private lateinit var formInspectionView: ConstraintLayout
    private lateinit var map: MapView
    private var photoSelfie: String? = null
    private var isForSelfie: Boolean = false
    private lateinit var summaryView: ConstraintLayout
    private lateinit var bottomNavInspect: BottomNavigationView
    private lateinit var vpFormAncak: ViewPager2
    private lateinit var titlePemanenInspeksi: TextView
    private lateinit var descPemanenInspeksi: TextView
    private lateinit var fabPrevFormAncak: FloatingActionButton
    private lateinit var fabNextFormAncak: FloatingActionButton
    private lateinit var fabNextToFormAncak: FloatingActionButton
    private lateinit var fabPhotoFormAncak: FloatingActionButton
    private lateinit var fabFollowUpNow: FloatingActionButton
    private lateinit var fabFollowUpTPH: FloatingActionButton
    private lateinit var fabPhotoUser: FloatingActionButton
    private lateinit var fabPhotoUser2: FloatingActionButton
    private var photoTPHFollowUp: String? = null
    private var komentarTPHFollowUp: String? = null
    private lateinit var labelPhotoUser2: TextView
    private lateinit var labelPhotoUser: TextView
    private lateinit var labelPhotoFormInspect: TextView
    private lateinit var labelFollowUpNow: TextView
    private lateinit var labelPhotoInfoBlok: TextView
    private lateinit var labelFollowUpTPH: TextView
    private lateinit var badgePhotoFUTPH: ImageView
    private lateinit var badgePhotoInspect: ImageView


    private lateinit var fabPhotoInfoBlok: FloatingActionButton
    private lateinit var clInfoBlokSection: ConstraintLayout
    private lateinit var clFormInspection: ConstraintLayout
    private lateinit var clSummaryInspection: ConstraintLayout
    private lateinit var fabSaveFormAncak: FloatingActionButton
    private var activityInitialized = false
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private var isStartFromTPH = true // true = TPH first, false = Pokok first
    private var hasSelectedMode = false

    data class KaryawanInfo(
        val nik: String,
        val nama: String,
        val individualId: String
    )

    // Add this as a class property
    private var selectedKaryawanList: List<KaryawanInfo> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_inspection)
        checkDateTimeSettings()
    }

    private fun initUI() {
        vpFormAncak = findViewById(R.id.vpFormAncakInspect)
        fabPhotoInfoBlok = findViewById(R.id.fabPhotoInfoBlok)
        fabFollowUpTPH = findViewById(R.id.fabFollowUpTPH)
        labelPhotoInfoBlok = findViewById(R.id.labelPhotoInfoBlok)
        labelFollowUpTPH = findViewById(R.id.labelFollowUpTPH)
        badgePhotoFUTPH = findViewById(R.id.badgePhotoFUTPH)
        badgePhotoInspect = findViewById(R.id.badgePhotoInspect)
        lyEstInspect = findViewById(R.id.lyEstInspect)
        lyAfdInspect = findViewById(R.id.lyAfdInspect)
        lyKemandoran = findViewById(R.id.lyKemandoran)
        lyPemuat = findViewById(R.id.lyPemuat)

        labelFollowUpNow = findViewById(R.id.labelFollowUpNow)
        fabNextToFormAncak = findViewById(R.id.fabNextToFormAncak)
        bottomNavInspect = findViewById(R.id.bottomNavInspect)
        titlePemanenInspeksi = findViewById(R.id.titlePemanenInspeksi)
        descPemanenInspeksi = findViewById(R.id.descPemanenInspeksi)
        clSummaryInspection = findViewById(R.id.clSummaryInspection)
        clFormInspection = findViewById(R.id.clFormInspection)
        clInfoBlokSection = findViewById(R.id.clInfoBlokSection)
        selectionScreen = findViewById(R.id.selectionScreen)
        mainContentWrapper = findViewById(R.id.mainContentWrapper)
        headerFormInspection = findViewById(R.id.headerFormInspection)
        btnMulaiDariTPH = findViewById(R.id.btnMulaiDariTPH)
        alertCardScanRadius = findViewById(R.id.alertCardScanRadius)
        alertTvScannedRadius = findViewById(R.id.alertTvScannedRadius)
        btnScanTPHRadius = findViewById(R.id.btnScanTPHRadius)
        btnMulaiDariPokok = findViewById(R.id.btnMulaiDariPokok)
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
            selectedIdPanenByScan = null
            selectedEstateByScan = null
            selectedAfdelingByScan = null
            selectedBlokByScan = null
            selectedTPHIdByScan = null
            selectedTPHNomorByScan = null
            selectedAncakByScan = null
            selectedTanggalPanenByScan = null
            selectedTPHValue = null
        } else {
            btnScanTPHRadius.visibility = View.VISIBLE
        }

        layoutAutoScan.visibility = View.VISIBLE

        val radiusText = "${boundaryAccuracy.toInt()} m"
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
        radiusMinimum =200F
        boundaryAccuracy =200F
        initViewModel()
        initUI()
        dept_abbr_pasar_tengah = intent.getStringExtra("DEPT_ABBR").toString()
        divisi_abbr_pasar_tengah = intent.getStringExtra("DIVISI_ABBR").toString()
        blok_kode_pasar_tengah = intent.getStringExtra("BLOK_KODE").toString()
        last_pokok_before_pasar_tengah = intent.getIntExtra("LAST_NUMBER_POKOK", 1)

        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val inspectionIdInt = intent.getIntExtra("id_inspeksi", -1)
        inspectionId = if (inspectionIdInt != -1) {
            inspectionIdInt.toString()
        } else {
            null
        }
        if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            findViewById<TextView>(R.id.titleDetailTrackingMap).visibility =
                View.VISIBLE
            findViewById<CardView>(R.id.cardMap).visibility = View.VISIBLE
            map = findViewById(R.id.map)
            setupMapTouchHandling()
            setupMap()
            setupButtonListeners()
            updateSatelliteButtonAppearance()
            updateButtonSelection("default")
        }
        setupAutoScanSwitch()
        setKeyboardVisibilityListener()
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        infoApp = AppUtils.getDeviceInfo(this@FormInspectionActivity).toString()
        setupHeader()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }


        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)
            }

            try {
                val estateIdStr = estateId?.trim()

                val parameterInspeksiDeferred = async {
                    try {
                        inspectionViewModel.getParameterInspeksiJson()
                    } catch (e: Exception) {
                        AppLogger.e("Parameter loading failed: ${e.message}")
                        emptyList<InspectionParameterItem>()
                    }
                }
                delay(100)
                parameterInspeksi = parameterInspeksiDeferred.await()


                if (parameterInspeksi.isEmpty()) {
                    throw Exception("Parameter Inspeksi kosong! Harap Untuk melakukan sinkronisasi Data")
                }

                if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                    val estateIdInt = estateIdStr.toInt()

                    val panenDeferred = CompletableDeferred<List<PanenEntityWithRelations>>()

                    panenViewModel.getAllTPHinWeek(estateIdInt)
                    delay(100)

                    withContext(Dispatchers.Main) {
                        panenViewModel.activePanenList.observe(this@FormInspectionActivity) { list ->

                            AppLogger.d("panenTPH $panenTPH")
                            panenTPH = list ?: emptyList()
                            panenDeferred.complete(list ?: emptyList())
                        }
                    }

                    if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {

                        if (!inspectionId.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                inspectionViewModel.loadInspectionById(inspectionId!!)
                            }
                        } else {
                            throw Exception("Inspection ID not found!")
                        }
                    }

                    val afdelingNameDeferred = async {
                        try {
                            val afdelingId = prefManager!!.afdelingIdUserLogin
                            inspectionViewModel.getAfdelingName(afdelingId!!.toInt())
                        } catch (e: Exception) {
                            null // Return null if error
                        }
                    }
                    delay(100)

                    AppLogger.d("afdelingNameDeferred $afdelingNameDeferred")
                    afdelingNameUser = afdelingNameDeferred.await()

                    val jenisTPHDeferred = CompletableDeferred<List<JenisTPHModel>>()

                    panenViewModel.getAllJenisTPH()
                    delay(100)

                    withContext(Dispatchers.Main) {
                        panenViewModel.jenisTPHList.observe(this@FormInspectionActivity) { list ->
                            jenisTPHListGlobal = list ?: emptyList()
                            jenisTPHDeferred.complete(list ?: emptyList())
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
                        throw Exception("Divisi tidak ditemukan, Periksa kembali dataset dengan melakukan Sinkronisasi Data!")
                    }
                }

                withContext(Dispatchers.Main) {
                    setupLayout()
                    loadingDialog.dismiss()
                }
            } catch (e: Exception) {

                AppLogger.d("error $e")
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

        setupSelectionButtons()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    cameraViewModel.statusCamera() -> {
                        cameraViewModel.closeCamera()

                        if (shouldReopenBottomSheet) {
                            shouldReopenBottomSheet = false
                            Handler(Looper.getMainLooper()).postDelayed({
                                showWithAnimation(bottomNavInspect)
                                showWithAnimation(fabPrevFormAncak)
                                showWithAnimation(fabNextFormAncak)
                                showViewPhotoBottomSheet(null, isInTPH)
                            }, 100)
                        }
                    }

                    isCameraViewOpen && !cameraViewModel.statusCamera() -> {
                        val zoomView = findViewById<View>(R.id.incEditPhotoInspect)
                        if (zoomView.visibility == View.VISIBLE) {
                            val cardCloseZoom =
                                zoomView.findViewById<MaterialCardView>(R.id.cardCloseZoom)
                            cardCloseZoom?.performClick()  // This triggers the same logic as clicking close
                        }
                        isCameraViewOpen = false
                        if (!keyboardOpenedWhileBottomSheetVisible) {
                            showWithAnimation(bottomNavInspect)
                            showWithAnimation(fabPrevFormAncak)
                            showWithAnimation(fabNextFormAncak)
                        }
                    }

                    else -> {
                        if (hasExistingData()) {
                            vibrate()
                            AlertDialogUtility.withTwoActions(
                                this@FormInspectionActivity,
                                "Keluar",
                                "Data akan hilang",
                                "Data yang sudah diisi akan hilang. Apakah Anda yakin ingin kembali?",
                                "warning.json",
                                ContextCompat.getColor(
                                    this@FormInspectionActivity,
                                    R.color.bluedarklight
                                ),
                                function = {
                                    if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                                        val newIntent = Intent(
                                            this@FormInspectionActivity,
                                            ListFollowUpInspeksi::class.java
                                        )
                                        newIntent.putExtra(
                                            "FEATURE_NAME",
                                            AppUtils.ListFeatureNames.ListFollowUpInspeksi
                                        )
                                        startActivity(newIntent)
                                        finishAffinity()
                                    } else {
                                        val intent = Intent(
                                            this@FormInspectionActivity,
                                            HomePageActivity::class.java
                                        )
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                }
                            )
                        } else {
                            if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                                val newIntent = Intent(
                                    this@FormInspectionActivity,
                                    ListFollowUpInspeksi::class.java
                                )
                                newIntent.putExtra(
                                    "FEATURE_NAME",
                                    AppUtils.ListFeatureNames.ListFollowUpInspeksi
                                )
                                startActivity(newIntent)
                                finishAffinity()
                            } else {
                                val intent = Intent(
                                    this@FormInspectionActivity,
                                    HomePageActivity::class.java
                                )
                                startActivity(intent)
                                finishAffinity()
                            }
                        }
                    }
                }
            }
        })


    }

    private fun hasExistingData(): Boolean {
        return selectedIdPanenByScan != null ||
                selectedEstateByScan != null ||
                selectedAfdelingByScan != null ||
                selectedBlokByScan != null ||
                selectedTPHNomorByScan != null ||
                selectedAncakByScan != null ||
                selectedTanggalPanenByScan != null ||
                selectedTPHValue != null ||
                photoInTPH != null ||
                (br1Value != null && br1Value.isNotEmpty()) ||
                (br2Value != null && br2Value.isNotEmpty()) ||
                komentarInTPH != null ||
                jumBrdTglPath != 0 ||
                jumBuahTglPath != 0 ||
                selectedJalurMasuk.isNotEmpty()
    }

    private fun hideResultScan() {
        selectedPemanenAdapter.clearAllWorkers()
        titlePemanenInspeksi.visibility = View.GONE
        descPemanenInspeksi.visibility = View.GONE
        alertCardScanRadius.visibility = View.GONE
        alertTvScannedRadius.visibility = View.GONE
        btnScanTPHRadius.visibility = View.GONE
        titleScannedTPHInsideRadius.visibility = View.GONE
        descScannedTPHInsideRadius.visibility = View.GONE
        emptyScannedTPHInsideRadius.visibility = View.GONE
        tphScannedResultRecyclerView.visibility = View.GONE
        layoutAutoScan.visibility = View.GONE
        tvErrorScannedNotSelected.visibility = View.GONE
    }

    private fun showSelectionScreen() {
        selectionScreen.visibility = View.VISIBLE
        mainContentWrapper.visibility = View.GONE
        hasInspectionStarted = false
        hideResultScan()
        dateStartInspection = ""
        trackingLocation.clear()
        selectedJalurMasuk = ""

        setupSpinnerView(
            findViewById(R.id.lyJalurInspect),
            (listRadioItems["EntryPath"] ?: emptyMap()).values.toList()
        )

        jumBrdTglPath = 0
        jumBuahTglPath = 0
        // Reset any selected data
        selectedTPHIdByScan = null
        selectedIdPanenByScan = null
        selectedEstateByScan = null
        selectedAfdelingByScan = null
        selectedBlokByScan = null
        selectedTPHNomorByScan = null
        selectedAncakByScan = null
        selectedTanggalPanenByScan = null
        selectedTPHValue = null
        photoInTPH = null
        komentarInTPH = null

        formAncakViewModel.clearAllData()

        val counterMappings = listOf(
            Triple(
                R.id.lyBrdTglInspect,
                AppUtils.kodeInspeksi.brondolanTinggalTPH,
                ::jumBrdTglPath
            ),
            Triple(R.id.lyBuahTglInspect, AppUtils.kodeInspeksi.buahTinggalTPH, ::jumBuahTglPath),
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPanenWithButtons(layoutId, labelText, counterVar)
        }

        br1Value = ""
        br2Value = ""

        // Clear the actual EditText views
        val editTextLayouts = listOf(R.id.lyBaris1Inspect, R.id.lyBaris2Inspect)
        editTextLayouts.forEach { layoutId ->
            findViewById<View>(layoutId)?.findViewById<EditText>(R.id.etHomeMarkerTPH)?.setText("")
        }
    }

    private fun setupSelectionButtons() {
        if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            // Direct flow for FollowUpInspeksi or when coming from Pasar Tengah

            if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                isStartFromTPH = false
                hasSelectedMode = true
                setupNavigationForPokokMode()
            }
            showMainContent()
        } else {

            fabPhotoInfoBlok.visibility = View.VISIBLE
            showMainContent()

        }
    }

    private fun showMainContent() {
        selectionScreen.visibility = View.GONE
        mainContentWrapper.visibility = View.VISIBLE
        headerFormInspection.visibility = View.VISIBLE

//        if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
////            isInTPH = false
//            bottomNavInspect.selectedItemId = R.id.navMenuBlokInspect
//            showFormInspectionScreen()
//        } else {
//            // For other flows, show info blok screen first
        showInfoBlokScreen()
//        }
    }

    private fun showFormInspectionScreen() {
        if (!hasInspectionStarted) {
            if (!trackingLocation.containsKey("start")) {
                trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
            }

            dateStartInspection = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            hasInspectionStarted = true

        }

        if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
            val afdResult = afdelingNameUser!!.replaceFirst("AFD-", "")
            formAncakViewModel.updateInfoFormAncak(
                estateName ?: "",
                afdResult ?: "",
                selectedBlokByScan ?: ""
            )
        }


//        fabPhotoInfoBlok.visibility = View.GONE
//        clInfoBlokSection.visibility = View.GONE
//        clSummaryInspection.visibility = View.GONE
//        clFormInspection.post {
//            vpFormAncak.post {
//                clFormInspection.visibility = View.VISIBLE
//            }
//        }
    }

    private fun setupNavigationForTPHMode() {
        updateBottomNavigationOrder(true)
    }

    private fun setupNavigationForPokokMode() {
        updateBottomNavigationOrder(true)
    }

    private fun updateBottomNavigationOrder(isTPHFirst: Boolean) {
        val menu = bottomNavInspect.menu
        menu.clear()

        if (isTPHFirst) {
            // TPH mode: Info Blok -> P. Ancak -> Summary
            menu.add(0, R.id.navMenuBlokInspect, 0, "Info. Blok")
                .setIcon(R.drawable.ic_home_black_24dp)
            menu.add(0, R.id.navMenuAncakInspect, 1, "P. Ancak")
                .setIcon(R.drawable.baseline_grain_24)
            menu.add(0, R.id.navMenuSummaryInspect, 2, "Summary")
                .setIcon(R.drawable.list_solid)
        } else {
            // Pokok mode: P. Ancak -> Info Blok -> Summary
            menu.add(0, R.id.navMenuAncakInspect, 0, "P. Ancak")
                .setIcon(R.drawable.baseline_grain_24)
            menu.add(0, R.id.navMenuBlokInspect, 1, "Info. Blok")
                .setIcon(R.drawable.ic_home_black_24dp)
            menu.add(0, R.id.navMenuSummaryInspect, 2, "Summary")
                .setIcon(R.drawable.list_solid)
        }
    }

    private fun showInfoBlokScreen() {
        clInfoBlokSection.visibility = View.VISIBLE

        if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
            // Normal inspection - show both FABs in their normal positions
            fabPhotoInfoBlok.visibility = View.VISIBLE
            labelPhotoInfoBlok.visibility = View.VISIBLE
            fabFollowUpTPH.visibility = View.VISIBLE
            labelFollowUpTPH.visibility = View.VISIBLE
        } else {
            // Follow Up Inspeksi - only show Follow Up FAB but move it to bottom position
            fabPhotoInfoBlok.visibility = View.GONE
            labelPhotoInfoBlok.visibility = View.GONE

            // Show Follow Up elements
            fabFollowUpTPH.visibility = View.VISIBLE
            labelFollowUpTPH.visibility = View.VISIBLE
            badgePhotoFUTPH.visibility = View.GONE

            // Move Follow Up FAB to the bottom position (same as fabPhotoInfoBlok)
            val layoutParams = fabFollowUpTPH.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = 15.dpToPx() // Use your existing extension function
            fabFollowUpTPH.layoutParams = layoutParams

            // Also adjust the badge position if needed
            val badgeLayoutParams = badgePhotoFUTPH.layoutParams as ConstraintLayout.LayoutParams
            badgeLayoutParams.marginStart = 45.dpToPx() // Use your existing extension function
            badgePhotoFUTPH.layoutParams = badgeLayoutParams
        }

        clFormInspection.visibility = View.GONE
        clSummaryInspection.visibility = View.GONE
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

        formAncakPagerAdapter = FormAncakPagerAdapter(this, totalPages, featureName)

        vpFormAncak.apply {
            adapter = formAncakPagerAdapter
            isUserInputEnabled = false
            setPageTransformer(createPageTransformer())
            offscreenPageLimit = 1
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

    private fun updateFragmentIfExists(pageNumber: Int) {
        // Get existing fragment from your ViewPager
        val fragment =
            supportFragmentManager.findFragmentByTag("f${pageNumber - 1}") // ViewPager uses 0-based index

        if (fragment is FormAncakFragment) {
            AppLogger.d("🔄 Found existing fragment for page $pageNumber, updating data")
            fragment.updatePageData()
        } else {
            AppLogger.d("🆕 No existing fragment for page $pageNumber (will create new)")
        }
    }

    private fun loadInspectionDataToViewModelSecondForm(inspectionData: InspectionWithDetailRelations) {
        val detailInspeksi = inspectionData.detailInspeksi

        val groupedByPokok = detailInspeksi.groupBy { it.no_pokok }.toSortedMap()

        formAncakViewModel.setCurrentPage(1)
        formAncakViewModel.setStartingPage(1)
        AppLogger.d("inspectionData inspeksi ${inspectionData.inspeksi}")
        formAncakViewModel.updateInfoFormAncak(
            inspectionData.inspeksi.dept_abbr ?: "",
            inspectionData.inspeksi.divisi_abbr ?: "",
            inspectionData.inspeksi.blok_kode ?: ""
        )
        groupedByPokok.forEach { (noPokak, details) ->
            val pageData = convertInspectionDetailsToPageData(noPokak, details)

            formAncakViewModel.savePageData(noPokak, pageData)
        }
        val maxPokok =
            groupedByPokok.keys.filter { it > 0 }.maxOrNull() ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        formAncakViewModel.updateTotalPages(maxPokok)

    }

    private fun convertInspectionDetailsToPageData(
        noPokak: Int,
        details: List<InspectionDetailModel>
    ): FormAncakViewModel.PageData {

        var emptyTree = 0
        var harvestTree = 0
        var neatPelepah = 0.0
        var pelepahSengkleh = 0.0
        var kondisiPruning = 1 // Default to Normal (1)
        var buahMasakTdkDipotong = 0.0
        var btPiringanGawangan = 0.0
        var brdKtpGawangan = 0.0
        var brdKtpPiringanPikulKetiak = 0.0
        var photo: String? = null
        var comment: String? = null
        var latIssue: Double? = null
        var lonIssue: Double? = null
        var createdDate: String? = null
        var createdBy: Int? = null
        var createdName: String? = null

        val firstDetail = details.firstOrNull()
        if (firstDetail != null) {
            photo = firstDetail.foto
            comment = firstDetail.komentar
            latIssue = null
            lonIssue = null
            createdDate = null
            createdBy = null
            createdName = null
        }

        // Get inspection configuration from parameterInspeksi
        val inspectionConfig = parameterInspeksi.associate { param ->
            param.id to (param.undivided == "True")
        }

        // Group by kode_inspeksi
        val groupedByKode = details.groupBy { it.kode_inspeksi }

        // Process each kode_inspeksi based on undivided status
        groupedByKode.forEach { (kodeInspeksi, detailsForKode) ->

            val isUndivided = inspectionConfig[kodeInspeksi] ?: true

            val finalValue = if (isUndivided) {
                // Sum all temuan_inspeksi values (preserve decimal values)
                val sumValue = detailsForKode.sumOf { it.temuan_inspeksi.toDouble() }
                sumValue
            } else {
                // Check if kode_inspeksi is between 7-10 for special handling
                if (kodeInspeksi in 7..10) {
                    val value = detailsForKode.firstOrNull()?.temuan_inspeksi?.toDouble() ?: 0.0
                    val convertedValue = if (value == 0.0) 2.0 else value
                    convertedValue
                } else {
                    val rawValue = detailsForKode.firstOrNull()?.temuan_inspeksi?.toDouble() ?: 0.0
                    rawValue
                }
            }

            when (kodeInspeksi) {
                1 -> {
                    brdKtpGawangan = finalValue
                    AppLogger.d("  → brdKtpGawangan = $finalValue")
                }

                2 -> {
                    brdKtpPiringanPikulKetiak = finalValue
                    AppLogger.d("  → brdKtpPiringanPikulKetiak = $finalValue")
                }

                3 -> {
                    buahMasakTdkDipotong = finalValue
                    AppLogger.d("  → buahMasakTdkDipotong = $finalValue")
                }

                4 -> {
                    btPiringanGawangan = finalValue
                    AppLogger.d("  → btPiringanGawangan = $finalValue")
                }

                5 -> {
                    AppLogger.d("  → Code 5 (TPH level data) = $finalValue - skipped")
                }

                6 -> {
                    AppLogger.d("  → Code 6 (TPH level data) = $finalValue - skipped")
                }

                7 -> {
                    neatPelepah = finalValue
                    AppLogger.d("  → neatPelepah = $finalValue")
                }

                8 -> {
                    pelepahSengkleh = finalValue
                    AppLogger.d("  → pelepahSengkleh = $finalValue")
                }

                9 -> {
                    // Over Pruning - set kondisiPruning to 2
                    if (finalValue == 1.0) {
                        kondisiPruning = 2
                        AppLogger.d("  → kondisiPruning set to 2 (Over Pruning) from code 9")
                    }
                }

                10 -> {
                    // Under Pruning - set kondisiPruning to 3
                    if (finalValue == 1.0) {
                        kondisiPruning = 3
                        AppLogger.d("  → kondisiPruning set to 3 (Under Pruning) from code 10")
                    }
                }
            }
        }

        // Add defaults for missing codes 7, 8
        if (!groupedByKode.containsKey(7)) {
            neatPelepah = 2.0
        }
        if (!groupedByKode.containsKey(8)) {
            pelepahSengkleh = 2.0
        }

        // Handle kondisiPruning logic:
        // If neither code 9 nor code 10 exists (or both have value 0), kondisiPruning remains 1 (Normal)
        // If code 9 exists with value 1, kondisiPruning = 2 (Over Pruning)
        // If code 10 exists with value 1, kondisiPruning = 3 (Under Pruning)
        AppLogger.d("  → Final kondisiPruning = $kondisiPruning")

        emptyTree = if (details.isNotEmpty()) {
            // Check for actual problems (value = 1 means "Ada"/"Ya")
            val hasNumericFindings = buahMasakTdkDipotong > 0.0 || btPiringanGawangan > 0.0 ||
                    brdKtpGawangan > 0.0 || brdKtpPiringanPikulKetiak > 0.0
            val hasRadioFindings = neatPelepah == 1.0 || pelepahSengkleh == 1.0 ||
                    kondisiPruning == 2 || kondisiPruning == 3 // Check for Over/Under Pruning

            if (hasNumericFindings || hasRadioFindings) {
                1 // Ada temuan
            } else {
                2 // Tidak ada temuan
            }
        } else {
            3 // Titik kosong (no data)
        }

        harvestTree = firstDetail?.pokok_panen ?: 2 // Default to "Tidak" if not specified

        val pageData = FormAncakViewModel.PageData(
            pokokNumber = noPokak,
            emptyTree = emptyTree,
            harvestTree = harvestTree,
            neatPelepah = neatPelepah.toInt(),
            pelepahSengkleh = pelepahSengkleh.toInt(),
            kondisiPruning = kondisiPruning,
            buahMasakTdkDipotong = buahMasakTdkDipotong.toInt(),
            btPiringanGawangan = btPiringanGawangan.toInt(),
            brdKtpGawangan = brdKtpGawangan.toInt(),
            brdKtpPiringanPikulKetiak = brdKtpPiringanPikulKetiak.toInt(),
            photo = photo,
            comment = comment,
            latIssue = latIssue,
            lonIssue = lonIssue,
            createdDate = createdDate,
            createdBy = createdBy,
            createdName = createdName
        )

        AppLogger.d("Final PageData created: $pageData")
        AppLogger.d("=== END CONVERSION FOR NO_POKOK $noPokak ===")

        return pageData
    }

    private fun observeViewModel() {

        inspectionViewModel.inspectionWithDetails.observe(this) { inspectionData ->
            if (inspectionData.isNotEmpty()) {
                val inspection = inspectionData.first()

                currentInspectionData = inspection
                if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    updateMapWithInspectionData(inspection)
                    populateFollowUpInspectionFirstFormUI(inspection)
                    setupCountersFromInspectionData(inspection.detailInspeksi)
                    setupPemanenRecyclerView(inspection.detailInspeksi)
                    loadInspectionDataToViewModelSecondForm(inspection)
                }

            }
        }

        formAncakViewModel.currentPage.observe(this) { page ->

            val pageIndex = page - 1

            if (vpFormAncak.currentItem != pageIndex) {
                AppLogger.d("🔄 Setting ViewPager to page: $pageIndex")
                vpFormAncak.setCurrentItem(pageIndex, true)
            }

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val totalPages =
                formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION



            Handler(Looper.getMainLooper()).postDelayed({
                val startingPage = formAncakViewModel.startingPage.value ?: 1
                fabPrevFormAncak.isEnabled = currentPage > startingPage
                fabPrevFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage <= startingPage) R.color.greytext else androidx.biometric.R.color.biometric_error_color
                    )
                )

                // Next button - disabled if at total pages
                fabNextFormAncak.isEnabled = currentPage < totalPages
                fabNextFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage >= totalPages) R.color.greytext else R.color.greenDefault
                    )
                )

                updateFragmentIfExists(page)
            }, 300)
        }

        formAncakViewModel.formData.observe(this) { formData ->
            updatePhotoBadgeVisibility()
            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val pageData = formData[currentPage]
            val emptyTreeValue = pageData?.emptyTree ?: 0

            // === fabPhotoUser & label ===
            val showFabPhotoUser = currentPage == AppUtils.MINIMAL_TAKE_SELFIE_INSPECTION
            fabPhotoUser.visibility = if (showFabPhotoUser) View.VISIBLE else View.GONE
            labelPhotoUser.visibility = fabPhotoUser.visibility

            // === fabFollowUpNow & label ===
            val showFabFollowUp = emptyTreeValue == 1
            fabFollowUpNow.visibility = if (showFabFollowUp) View.VISIBLE else View.GONE
            labelFollowUpNow.visibility = fabFollowUpNow.visibility

            // === fabPhotoFormInspect & label ===
            // Hide form photo elements if it's Follow Up feature
            val showFabFormPhoto = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                false
            } else {
                emptyTreeValue == 1
            }

            fabPhotoFormAncak.visibility = if (showFabFormPhoto) View.VISIBLE else View.GONE
            labelPhotoFormInspect.visibility = if (showFabFormPhoto) View.VISIBLE else View.GONE
//            badgePhotoInspect.visibility = if (showFabFormPhoto) View.VISIBLE else View.GONE

            if (showFabPhotoUser) {
                val layoutParams = fabPhotoUser.layoutParams as ConstraintLayout.LayoutParams
                when {
                    showFabFollowUp && showFabFormPhoto -> {
                        layoutParams.bottomToTop = R.id.fabFollowUpNow
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    }

                    showFabFormPhoto -> {
                        layoutParams.bottomToTop = R.id.fabPhotoFormInspect
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    }

                    showFabFollowUp -> {
                        layoutParams.bottomToTop = R.id.fabFollowUpNow
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    }

                    else -> {
                        layoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                }
                layoutParams.bottomMargin = (15 * resources.displayMetrics.density).toInt()
                fabPhotoUser.layoutParams = layoutParams
            }

            // === Update fabFollowUpNow Position ===
            if (showFabFollowUp) {
                val layoutParams = fabFollowUpNow.layoutParams as ConstraintLayout.LayoutParams
                if (showFabFormPhoto) {
                    layoutParams.bottomToTop = R.id.fabPhotoFormInspect
                    layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                } else {
                    layoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
                layoutParams.bottomMargin = (15 * resources.displayMetrics.density).toInt()
                fabFollowUpNow.layoutParams = layoutParams
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
            val currentPokok = formAncakViewModel.currentPage.value ?: 1
            val nextPokok = currentPokok + 1
            val totalPokok =
                formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val pokokData = formData[currentPokok]
            val photoValue = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                pokokData?.foto_pemulihan ?: ""
            } else {
                pokokData?.photo ?: ""
            }

            // NEW: Selfie validation - check if current page >= minimal and selfie is missing
            val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()

            AppLogger.d("Next button - currentPokok: $currentPokok, hasSelfiePhoto: $hasSelfiePhoto")

            if (currentPokok >= AppUtils.MINIMAL_TAKE_SELFIE_INSPECTION && !hasSelfiePhoto) {
                vibrate(500)
                isForSelfie = true
                showViewPhotoBottomSheet(null, false, true, false) // Selfie photo
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon dapat mengambil foto user/selfie terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            val buahMasakTdkDipotong = pokokData?.buahMasakTdkDipotong ?: 0
            val btPiringanGawangan = pokokData?.btPiringanGawangan ?: 0
            val brdKtpGawangan = pokokData?.brdKtpGawangan ?: 0
            val brdKtpPiringanPikulKetiak = pokokData?.brdKtpPiringanPikulKetiak ?: 0

            // Check if photo is required based on findings
            val hasFindings = (buahMasakTdkDipotong > 0) ||
                    (btPiringanGawangan > 0) ||
                    (btPiringanGawangan > 0) ||
                    ((brdKtpGawangan + brdKtpPiringanPikulKetiak) > 50)

            val hasValidPhoto = !photoValue.isNullOrEmpty() && photoValue.trim().isNotEmpty()
            val emptyTreeValue = pokokData?.emptyTree ?: 1 // Replace with actual field name
            if (emptyTreeValue == 1 && hasFindings && !hasValidPhoto) {
                vibrate(500)
                if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    showViewPhotoBottomSheet(null, isInTPH, false, false)
                } else {
                    isForFollowUp = true
                    showViewPhotoBottomSheet(null, false, false, true) // Follow-up photo
                }
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi)
                        "Mohon dapat mengambil foto pemulihan terlebih dahulu!"
                    else
                        "Mohon dapat mengambil foto temuan terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            val validationResult = formAncakViewModel.validateCurrentPage(1)

            if (!validationResult.isValid) {
                vibrate(500)
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

            pokokData?.let { data ->
                // Check if location update is actually needed
                if (formAncakViewModel.shouldSetLatLonIssue(data)) {
                    // Only call if there are issues that need tracking
                    formAncakViewModel.updatePokokDataWithLocationAndGetTrackingStatus(
                        currentPokok,
                        lat,
                        lon,
                        prefManager!!,
                        this@FormInspectionActivity
                    )
                } else {
                    val currentDate =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val updatedData = data.copy(
                        createdDate = currentDate,
                        createdBy = prefManager!!.idUserLogin,
                        createdName = prefManager!!.nameUserLogin,
                        latIssue = lat,
                        lonIssue = lon
                    )
                    formAncakViewModel.savePageData(currentPokok, updatedData)
                    AppLogger.d("Updated metadata only for pokok $currentPokok (no location update needed)")
                }
            }

            if (nextPokok <= totalPokok) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage("Loading data...")

                        // Handle 10th pokok tracking
                        if (currentPokok % 10 == 0 && !trackingLocation.containsKey(currentPokok.toString())) {
                            isTenthTrees = true
                            trackingLocation[currentPokok.toString()] =
                                Location(lat ?: 0.0, lon ?: 0.0)
                            AppLogger.d("Adding 10th pokok location for pokok $currentPokok: lat=$lat, lon=$lon")
                        } else if (isTenthTrees) {
                            isTenthTrees = false
                        }

                        // Navigate to next page
                        AppLogger.d(" askdjflkasjd flk safldkj sldkj")
                        formAncakViewModel.nextPage()

                        // Small delay for smooth transition
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (loadingDialog.isShowing) {
                                scrollToTopOfFormAncak()
                                loadingDialog.dismiss()
                            }
                        }, 300)
                    }
                }
            }

            AppLogger.d(trackingLocation.toString())
        }

        fabPrevFormAncak.setOnClickListener {
            val currentPokok = formAncakViewModel.currentPage.value ?: 1
            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val prevPage = currentPage - 1
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val pokokData = formData[currentPokok]

            // NEW: Selfie validation - check if current page >= minimal and selfie is missing
            val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()

            AppLogger.d("Prev button - currentPokok: $currentPokok, hasSelfiePhoto: $hasSelfiePhoto")

            if (currentPokok >= AppUtils.MINIMAL_TAKE_SELFIE_INSPECTION && !hasSelfiePhoto) {
                vibrate(500)
                isForSelfie = true
                showViewPhotoBottomSheet(null, false, true, false) // Selfie photo
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon dapat mengambil foto user/selfie terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            val buahMasakTdkDipotong = pokokData?.buahMasakTdkDipotong ?: 0
            val btPiringanGawangan = pokokData?.btPiringanGawangan ?: 0
            val brdKtpGawangan = pokokData?.brdKtpGawangan ?: 0
            val brdKtpPiringanPikulKetiak = pokokData?.brdKtpPiringanPikulKetiak ?: 0
            val hasFindings = (buahMasakTdkDipotong > 0) ||
                    (btPiringanGawangan > 0) ||
                    (btPiringanGawangan > 0) ||
                    ((brdKtpGawangan + brdKtpPiringanPikulKetiak) > 50)

            val photoValue = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                pokokData?.foto_pemulihan ?: ""
            } else {
                pokokData?.photo ?: ""
            }
            val hasValidPhoto = !photoValue.isNullOrEmpty() && photoValue.trim().isNotEmpty()
            val emptyTreeValue = pokokData?.emptyTree ?: 1 // Replace with actual field name
            if (emptyTreeValue == 1 && hasFindings && !hasValidPhoto) {
                vibrate(500)
                if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    showViewPhotoBottomSheet(null, isInTPH, false, false)
                } else {
                    isForFollowUp = true
                    showViewPhotoBottomSheet(null, false, false, true) // Follow-up photo
                }
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi)
                        "Mohon dapat mengambil foto pemulihan terlebih dahulu!"
                    else
                        "Mohon dapat mengambil foto temuan terlebih dahulu!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            pokokData?.let { data ->
                // Check if location update is actually needed
                if (formAncakViewModel.shouldSetLatLonIssue(data)) {
                    // Only call if there are issues that need tracking
                    formAncakViewModel.updatePokokDataWithLocationAndGetTrackingStatus(
                        currentPokok,
                        lat,
                        lon,
                        prefManager!!,
                        this@FormInspectionActivity
                    )
                } else {
                    // Just update metadata without location
                    val currentDate =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val updatedData = data.copy(
                        createdDate = currentDate,
                        createdBy = prefManager!!.idUserLogin,
                        createdName = prefManager!!.nameUserLogin,
                        latIssue = lat,
                        lonIssue = lon
                    )
                    formAncakViewModel.savePageData(currentPokok, updatedData)
                    AppLogger.d("Updated metadata only for pokok $currentPokok (no location update needed)")
                }
            }

            if (prevPage >= 1) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage("Loading data...")

                        // Navigate to previous page
                        formAncakViewModel.previousPage()

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (loadingDialog.isShowing) {
                                loadingDialog.dismiss()
                            }
                        }, 300)
                    }
                }
            }
        }

        fabPhotoFormAncak.setOnClickListener {
            AppLogger.d("fabPhotoFormAncak Triggered")
            isForSelfie = false
            isInTPH = false
            isForFollowUp = false


            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, false, false, false) // Form photo
            }, 150)
        }

        fabPhotoUser.setOnClickListener {
            AppLogger.d("fabPhotoUser Triggered")
            isForSelfie = true
            isInTPH = false
            isForFollowUp = false


            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, false, true, false) // Selfie photo
            }, 150)
        }

        fabFollowUpTPH.setOnClickListener {
            AppLogger.d("fabFollowUpTPH Triggered")
            isInTPH = true
            isForSelfie = false
            isForFollowUp = true


            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, true, false, true) // TPH Follow-up photo
            }, 150)
        }

        fabPhotoInfoBlok.setOnClickListener {
            AppLogger.d("fabPhotoInfoBlok Triggered")
            isForSelfie = false
            isInTPH = true
            isForFollowUp = false


            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, true, false, false) // TPH photo
            }, 150) // Incre
        }

        fabNextToFormAncak.setOnClickListener {
            if (!validateAndShowErrors()) {
                vibrate(500)
                return@setOnClickListener
            }


            bottomNavInspect.selectedItemId = R.id.navMenuAncakInspect
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    isInTPH = false
                    clInfoBlokSection.visibility = View.GONE
                    clFormInspection.visibility = View.VISIBLE
                    if (!trackingLocation.containsKey("start")) {
                        trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
                    }

                    if (!hasInspectionStarted) {
                        if (!trackingLocation.containsKey("start")) {
                            trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
                        }

                        dateStartInspection = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                        hasInspectionStarted = true
                        AppLogger.d("Inspection started at: $dateStartInspection")
                    } else {
                        AppLogger.d("Inspection already started, keeping original start time: $dateStartInspection")
                    }

                    val afdResult = selectedAfdeling.replaceFirst("AFD-", "")

                    val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true

                    val estateAbbrLocal = if (isGM) estateAbbr else estateName

                    formAncakViewModel.updateInfoFormAncak(
                        estateAbbrLocal ?: "",
                        afdResult,
                        selectedBlokByScan ?: ""
                    )
                    fabPhotoInfoBlok.visibility = View.GONE
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
        }

        fabPhotoUser2?.setOnClickListener {
            isForSelfie = true

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, false, true, false) // Selfie photo
            }, 150)
        }

        fabFollowUpNow.setOnClickListener {
            isForSelfie = false
            isForFollowUp = true


            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Clear focus from any EditText
            currentFocus?.clearFocus()

            // Add a small delay to ensure keyboard is fully closed and layout is adjusted
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, false, false, true) // Follow-up photo
            }, 150)
        }

        fabSaveFormAncak.setOnClickListener {
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            val totalPagesWithData = formData.size
            val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()

            if (!hasSelfiePhoto) {
                vibrate(500)
                isForSelfie = true
                showViewPhotoBottomSheet(null, false, true, false)
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_data_not_completed),
                    "Mohon dapat mengambil foto user/selfie terlebih dahulu sebelum menyimpan!",
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

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

            val isFollowUp = featureName == AppUtils.ListFeatureNames.FollowUpInspeksi

            AlertDialogUtility.withTwoActions(
                this,
                if (isFollowUp) "Update Data" else "Simpan Data",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.confirmation_dialog_description),
                "warning.json",
                function = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            loadingDialog.show()
                            loadingDialog.setMessage(
                                if (isFollowUp) "Mengupdate data..." else "Menyimpan data...",
                                true
                            )

                            if (!isTenthTrees) {
                                trackingLocation["end"] = Location(lat ?: 0.0, lon ?: 0.0)
                            }

                            val trackingJson = JSONObject().apply {
                                trackingLocation.forEach { (key, location) ->
                                    put(key, JSONObject().apply {
                                        put("lat", location.lat)
                                        put("lon", location.lon)
                                    })
                                }
                            }

                            val dateEndInspection = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())


                            val result = if (isFollowUp) {
                                inspectionViewModel.saveDataInspection(
                                    created_date_start = currentInspectionData?.inspeksi?.created_date
                                        ?: "",
                                    created_by = currentInspectionData?.inspeksi?.created_by ?: "",
                                    created_name = currentInspectionData?.inspeksi?.created_name
                                        ?: "",
                                    tph_id = currentInspectionData?.inspeksi?.tph_id ?: 0,
                                    id_panen = currentInspectionData?.inspeksi?.id_panen ?: "0",
                                    date_panen = currentInspectionData?.inspeksi?.date_panen ?: "",
                                    jalur_masuk = currentInspectionData?.inspeksi?.jalur_masuk
                                        ?: "",
                                    jenis_kondisi = currentInspectionData?.inspeksi?.jenis_kondisi
                                        ?: 1,
                                    baris = currentInspectionData?.inspeksi?.baris ?: "",
                                    jml_pkk_inspeksi = currentInspectionData?.inspeksi?.jml_pkk_inspeksi
                                        ?: 0,
                                    tracking_path = currentInspectionData?.inspeksi?.tracking_path
                                        ?: "",
                                    foto_user = "",
                                    jjg_panen = currentInspectionData?.inspeksi?.jjg_panen ?: 0,
                                    foto_user_pemulihan = photoSelfie,
                                    app_version = currentInspectionData?.inspeksi?.app_version
                                        ?: "",
                                    app_version_pemulihan = infoApp,
                                    status_upload = "0",
                                    status_uploaded_image = "0",
                                    // Follow-up specific parameters
                                    isFollowUp = true,
                                    existingInspectionId = currentInspectionData?.inspeksi?.id,
                                    tracking_path_pemulihan = trackingJson.toString(),
                                    updated_date_start = dateStartInspection,
                                    updated_date_end = dateEndInspection,
                                    updated_by = userId.toString(),
                                    updated_name = prefManager!!.nameUserLogin
                                )
                            } else {

                                if (selectedTPHIdByScan == null) {
                                    throw Exception("TPH ID tidak boleh kosong")
                                }

                                val entriesWithEmptyTree1 =
                                    formData.values.filter { it.emptyTree == 1 }

                                val allEntriesComplete = entriesWithEmptyTree1.all { pageData ->
                                    val isComplete =
                                        !pageData.foto_pemulihan.isNullOrEmpty() && pageData.status_pemulihan == 1
                                    isComplete
                                }

                                val allConditionsMet =
                                    allEntriesComplete && photoTPHFollowUp != null

                                val inspeksiPutaran = if (allConditionsMet) 2 else 1

                                inspectionViewModel.saveDataInspection(
                                    created_date_start = dateStartInspection,
                                    created_by = userId.toString(),
                                    created_name = prefManager!!.nameUserLogin ?: "",
                                    tph_id = selectedTPHIdByScan ?: 0,
                                    id_panen = selectedIdPanenByScan ?: "0",
                                    date_panen = selectedTanggalPanenByScan!!,
                                    foto_user = photoSelfie ?: "",
                                    jjg_panen = totalHarvestTree,
                                    inspeksi_putaran = inspeksiPutaran,
                                    jalur_masuk = selectedJalurMasuk,
                                    jenis_kondisi = selectedKondisiValue.toInt(),
                                    baris = if (br2Value.isNotEmpty()) "$br1Value,$br2Value" else br1Value,
                                    jml_pkk_inspeksi = totalPokokInspection,
                                    tracking_path = trackingJson.toString(),
                                    app_version = infoApp,
                                    status_upload = "0",
                                    status_uploaded_image = "0"
                                )
                            }

                            when (result) {
                                is InspectionViewModel.SaveDataInspectionState.Success -> {
                                    inspectionId = result.inspectionId.toString()
                                    if (!isFollowUp) {
                                        val formData =
                                            formAncakViewModel.formData.value ?: mutableMapOf()
                                        val selectedPemuatWorkers =
                                            selectedPemuatAdapter.getSelectedWorkers()

                                        val detailResult =
                                            inspectionViewModel.saveDataInspectionDetails(
                                                inspectionId = result.inspectionId.toString(),
                                                formData = formData,
                                                jumBrdTglPath = jumBrdTglPath,
                                                jumBuahTglPath = jumBuahTglPath,
                                                parameterInspeksi = parameterInspeksi,
                                                createdDate = dateStartInspection,
                                                createdName = prefManager?.nameUserLogin ?: "",
                                                createdBy = userId.toString(),
                                                latTPH = lat ?: 0.0,
                                                lonTPH = lon ?: 0.0,
                                                foto = photoInTPH,
                                                komentar = komentarInTPH ?: "",
                                                foto_pemulihan_tph = photoTPHFollowUp ?: "",
                                                komentar_pemulihan_tph = komentarTPHFollowUp ?: "",
                                                pemuatWorkers = selectedPemuatWorkers
                                            )

                                        when (detailResult) {
                                            is InspectionViewModel.SaveDataInspectionDetailsState.Success -> {
                                                showSuccessDialog()
                                            }

                                            is InspectionViewModel.SaveDataInspectionDetailsState.Error -> {
                                                showErrorDialog(detailResult.message)
                                            }
                                        }
                                    } else {
                                        val formData =
                                            formAncakViewModel.formData.value ?: mutableMapOf()

                                        val detailResult =
                                            inspectionViewModel.updateDataInspectionDetailsForFollowUp(
                                                detailInspeksiList = currentInspectionData?.detailInspeksi
                                                    ?: emptyList(),
                                                formData = formData,
                                                latTPH = lat ?: 0.0,
                                                lonTPH = lon ?: 0.0,
                                                photoTPHFollowUp = photoTPHFollowUp ?: "",
                                                komentarTPHFollowUp = komentarTPHFollowUp ?: "",
                                                createdDateStart = dateStartInspection,
                                                createdName = prefManager?.nameUserLogin ?: "",
                                                createdBy = userId.toString()
                                            )

                                        when (detailResult) {
                                            is InspectionViewModel.SaveDataInspectionDetailsState.Success -> {
                                                showSuccessDialog()
                                            }

                                            is InspectionViewModel.SaveDataInspectionDetailsState.Error -> {
                                                showErrorDialog(detailResult.message)
                                            }
                                        }
                                    }
                                    loadingDialog.dismiss()
                                }

                                is InspectionViewModel.SaveDataInspectionState.Error -> {
                                    loadingDialog.dismiss()
                                    showErrorDialog(result.message)
                                }
                            }

                        } catch (e: Exception) {
                            loadingDialog.dismiss()
                            showErrorDialog(e.message ?: "Unknown error")
                        }
                    }
                }
            )
        }
    }

    private fun showSuccessDialog() {
        AlertDialogUtility.withSingleAction(
            this@FormInspectionActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_success_save_local),
            stringXML(R.string.al_description_success_save_local),
            "success.json",
            R.color.greenDefault
        ) {
            val intent = Intent(this@FormInspectionActivity, HomePageActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialogUtility.withSingleAction(
            this@FormInspectionActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_save_local),
            "${stringXML(R.string.al_description_failed_save_local)} : $message",
            "warning.json",
            R.color.colorRedDark
        ) {}
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
    private fun showViewPhotoBottomSheet(
        fileName: String? = null,
        isInTPH: Boolean? = null,
        isForSelfie: Boolean? = null,
        isForFollowUp: Boolean? = null
    ) {
        isBottomSheetOpen = true
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val currentData =
            formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_inspection_photo, null)
        bottomSheetDialog.setContentView(view)

        view.background = ContextCompat.getDrawable(this, R.drawable.rounded_top_right_left)

        val ibDeletePhotoInspect = view.findViewById<ImageButton>(R.id.ibDeletePhotoInspect)
        val incLytPhotosInspect = view.findViewById<View>(R.id.incLytPhotosInspect)
        val ivAddPhoto = incLytPhotosInspect.findViewById<ImageView>(R.id.ivAddFoto)
        val tvPhotoComment = incLytPhotosInspect.findViewById<TextView>(R.id.tvPhotoComment)
        tvPhotoComment.visibility = View.GONE
        val etPhotoComment = incLytPhotosInspect.findViewById<EditText>(R.id.etPhotoComment)

        etPhotoComment.visibility = View.VISIBLE

        etPhotoComment.apply {
            isEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = AndroidInputType.TYPE_CLASS_TEXT
        }

        etPhotoComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etPhotoComment.windowToken, 0)
                etPhotoComment.clearFocus()
                true
            } else {
                false
            }
        }

        etPhotoComment.setOnClickListener {
            etPhotoComment.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etPhotoComment, InputMethodManager.SHOW_IMPLICIT)
            if (bottomNavInspect.visibility == View.VISIBLE) {
                hideWithAnimation(bottomNavInspect, 50)
            }


        }

        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }

        val titlePhotoTemuan = view.findViewById<TextView>(R.id.titlePhotoTemuan)
        val titleComment = incLytPhotosInspect.findViewById<TextView>(R.id.titleComment)

        when {
            isForSelfie == true -> {
                // Selfie photo case
                titlePhotoTemuan.text =
                    if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
                        "Lampiran Foto Selfie User"
                    } else {
                        "Lampiran Foto Selfie Pemulihan User"
                    }
                titleComment.visibility = View.GONE
                etPhotoComment.visibility = View.GONE
            }

            isInTPH == true && isForFollowUp == true -> {
                // TPH Follow-up photo case - NEW CASE
                titlePhotoTemuan.text = "Lampiran Foto Pemulihan di TPH"
                titleComment.text = "Komentar Pemulihan TPH"
                titleComment.visibility = View.VISIBLE
                etPhotoComment.visibility = View.VISIBLE
            }

            isForFollowUp == true -> {

                titlePhotoTemuan.text = "Lampiran Foto Pemulihan"
                titleComment.text = "Komentar Pemulihan"
            }

            isInTPH == true -> {

                if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    titlePhotoTemuan.text = "Lampiran Foto Pemulihan di TPH"
                    titleComment.text = "Komentar Temuan Pemulihan"
                } else {
                    titlePhotoTemuan.text = "Lampiran Foto di TPH"
                    titleComment.text = "Komentar"
                }
                updatePhotoBadgeVisibility()
            }

            else -> {
                if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    titlePhotoTemuan.text = "Lampiran Foto Pemulihan"
                    titleComment.text = "Komentar Temuan Inspeksi"
                } else {
                    titlePhotoTemuan.text = "Lampiran Foto Temuan"
                    titleComment.text = "Komentar"
                }
            }
        }

        val photoToShow = when {
            isForSelfie == true -> photoSelfie
            isInTPH == true && isForFollowUp == true -> photoTPHFollowUp // NEW CASE
            isInTPH == true -> photoInTPH
            isForFollowUp == true -> currentData.foto_pemulihan
            else -> currentData.photo
        }

        val watermarkType = when {
            isForSelfie == true && featureName == AppUtils.ListFeatureNames.FollowUpInspeksi -> WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser
            isForSelfie == true -> WaterMarkFotoDanFolder.WMBuktiInspeksiUser
            isInTPH == true && isForFollowUp == true -> WaterMarkFotoDanFolder.WMFUInspeksiTPH
            isForFollowUp == true -> WaterMarkFotoDanFolder.WMFUInspeksiPokok
            featureName == AppUtils.ListFeatureNames.FollowUpInspeksi -> {
                if (isInTPH == true) {
                    WaterMarkFotoDanFolder.WMFUInspeksiTPH
                } else {
                    WaterMarkFotoDanFolder.WMFUInspeksiPokok
                }
            }

            else -> {
                if (isInTPH == true) {
                    WaterMarkFotoDanFolder.WMInspeksiTPH
                } else {
                    WaterMarkFotoDanFolder.WMInspeksiPokok
                }
            }
        }

        ibDeletePhotoInspect.visibility = View.GONE

        val performDeleteAction = {
            ibDeletePhotoInspect.visibility = View.GONE
            ivAddPhoto.setImageResource(R.drawable.baseline_add_a_photo_24)
            etPhotoComment.setText("")
            etPhotoComment.clearFocus()

            when {
                isForSelfie == true -> {
                    photoSelfie = null
                }

                isInTPH == true && isForFollowUp == true -> {
                    photoTPHFollowUp = null
                    komentarTPHFollowUp = null
                }

                isInTPH == true -> {
                    photoInTPH = null
                    komentarInTPH = null
                }

                isForFollowUp == true -> {
                    formAncakViewModel.savePageData(
                        currentPage,
                        currentData.copy(
                            foto_pemulihan = null,
                            komentar_pemulihan = null,
                            status_pemulihan = 0,
                        )
                    )
                }

                else -> {
                    formAncakViewModel.savePageData(
                        currentPage,
                        currentData.copy(
                            photo = null,
                            comment = null,
                        )
                    )
                }
            }

            updatePhotoBadgeVisibility()
            showWithAnimation(bottomNavInspect)
            showWithAnimation(fabPrevFormAncak)
            showWithAnimation(fabNextFormAncak)
            bottomSheetDialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(null, isInTPH, isForSelfie, isForFollowUp)
            }, 100)
        }

        when {
            isInTPH == true && isForFollowUp == true -> {
                // TPH Follow-up comment - NEW CASE
                etPhotoComment.setText(komentarTPHFollowUp)
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
                        komentarTPHFollowUp = s?.toString() ?: ""
                    }
                })
            }

            isInTPH == true -> {
                etPhotoComment.setText(komentarInTPH)
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
                        komentarInTPH = s?.toString() ?: ""
                    }
                })
            }

            isForFollowUp == true -> {
                etPhotoComment.setText(currentData.komentar_pemulihan)
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
                            currentData.copy(komentar_pemulihan = s?.toString() ?: "")
                        )
                    }
                })
            }

            else -> {
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
            }
        }

        var resultFileName = photoToShow ?: ""
        if (fileName != null) {
            resultFileName = fileName
        }
        val rootApp = File(
            this.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-${watermarkType}"
        ).toString()

        val filePath = File(rootApp, resultFileName)
        ivAddPhoto.setOnClickListener {

            if (bottomNavInspect.visibility == View.VISIBLE) {
                hideWithAnimation(bottomNavInspect, 50)
            }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etPhotoComment.windowToken, 0)
            etPhotoComment.clearFocus()


            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    isCameraViewOpen = true
                    bottomSheetDialog.dismiss()

                    if (resultFileName.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            cameraViewModel.openZoomPhotos(
                                file = filePath,
                                position = currentPage.toString(),
                                onChangePhoto = {
                                    shouldReopenBottomSheet = true
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        headerFormInspection.visibility = View.GONE
                                        bottomNavInspect.visibility = View.GONE
                                        cameraViewModel.takeCameraPhotos(
                                            this,
                                            currentPage.toString(),
                                            ivAddPhoto,
                                            currentPage,
                                            null,
                                            "", // soon assign lat lon
                                            currentPage.toString(),
                                            watermarkType,
                                            lat,
                                            lon,
                                            generateSourceFoto(currentData),
                                            if (isForSelfie == true) CameraType.FRONT
                                            else CameraType.BACK
                                        )
                                    }, 100)
                                },
                                onDeletePhoto = { pos ->
                                    performDeleteAction()
                                },
                                onClosePhoto = {
                                    isCameraViewOpen = false
                                    if (!keyboardOpenedWhileBottomSheetVisible) {
                                        showWithAnimation(bottomNavInspect)
                                        showWithAnimation(fabPrevFormAncak)
                                        showWithAnimation(fabNextFormAncak)
                                    }
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showViewPhotoBottomSheet(
                                            null,
                                            isInTPH,
                                            isForSelfie,
                                            isForFollowUp
                                        )
                                    }, 100)
                                }
                            )
                        }, 100)
                    } else {
                        shouldReopenBottomSheet = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            hideWithAnimation(bottomNavInspect, 50)
                            cameraViewModel.takeCameraPhotos(
                                this,
                                currentPage.toString(),
                                ivAddPhoto,
                                currentPage,
                                null,
                                "",
                                currentPage.toString(),
                                watermarkType,
                                lat,
                                lon,
                                generateSourceFoto(currentData),
                                if (isForSelfie == true) CameraType.FRONT
                                else CameraType.BACK
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

        bottomSheetDialog.setOnDismissListener {
            isBottomSheetOpen = false

            // Only show bottom nav if:
            // 1. Camera view is not open AND
            // 2. Keyboard was not opened while bottom sheet was visible
            if (!isCameraViewOpen && !keyboardOpenedWhileBottomSheetVisible) {
                showWithAnimation(bottomNavInspect)
                showWithAnimation(fabPrevFormAncak)
                showWithAnimation(fabNextFormAncak)
            }
        }


        bottomSheetDialog.show()
    }

    fun generateSourceFoto(data: FormAncakViewModel.PageData): String {
        return if (isInTPH == true) {
            val limitedKomentar = if ((komentarInTPH?.length ?: 0) > 250) {
                "${komentarInTPH?.substring(0, 250)}..."
            } else {
                komentarInTPH ?: ""
            }

            val locationInfo =
                "$selectedEstateByScan $selectedAfdelingByScan $selectedBlokByScan TPH $selectedTPHNomorByScan"

            // Only add newline if comment exists and is not empty
            if (limitedKomentar.isNotEmpty()) {
                "$limitedKomentar\n$locationInfo"
            } else {
                locationInfo
            }
        } else {
            val kondisi = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                // Use data from currentInspectionData for follow-up
                val jenisKondisi = currentInspectionData?.inspeksi?.jenis_kondisi ?: 0
                val baris = currentInspectionData?.inspeksi?.baris ?: ""
                if (jenisKondisi == 2) "Terasan Baris No:$baris" else "Baris No:$baris"
            } else {
                if (selectedKondisiValue.toInt() == 2) "Terasan Baris No:$br1Value" else "br1:${br1Value} br2:$br2Value"
            }

            val limitedComment = if ((data.comment?.length ?: 0) > 250) {
                "${data.comment?.substring(0, 250)}..."
            } else {
                data.comment ?: ""
            }

            val pokokInfo = "$kondisi #Pokok ${data.pokokNumber}"

            // Only add newline if comment exists and is not empty
            if (limitedComment.isNotEmpty()) {
                "$limitedComment\n$pokokInfo"
            } else {
                pokokInfo
            }
        }
    }


    private fun setKeyboardVisibilityListener() {
        val rootView = findViewById<View>(android.R.id.content)
        keyboardWatcher = SoftKeyboardStateWatcher(
            rootView,
            object : SoftKeyboardStateWatcher.OnSoftKeyboardStateChangedListener {
                override fun onSoftKeyboardOpened(keyboardHeight: Int) {
                    bottomNavInspect.post {
                        // Always hide bottom nav when keyboard opens
                        if (bottomNavInspect.visibility == View.VISIBLE) {
                            hideWithAnimation(bottomNavInspect, 50)
                            hideWithAnimation(fabPrevFormAncak, 50)
                            hideWithAnimation(fabNextFormAncak, 50)
                        }

                        // Track if keyboard opened while bottom sheet is visible
                        keyboardOpenedWhileBottomSheetVisible = isBottomSheetOpen
                    }
                }

                override fun onSoftKeyboardClosed() {
                    bottomNavInspect.post {
                        // Only show bottom nav if:
                        // 1. No bottom sheet is open AND
                        // 2. No camera view is open
                        AppLogger.d("masuk gess")
                        // Note: showWithAnimation handles visibility check internally
                        AppLogger.d(" isBottomSheetOpen $isBottomSheetOpen")
                        AppLogger.d(" isCameraViewOpen $isCameraViewOpen")
                        if (!isBottomSheetOpen && !isCameraViewOpen) {
                            AppLogger.d("masukgak sih  gess")
                            showWithAnimation(bottomNavInspect)
                            showWithAnimation(fabPrevFormAncak)
                            showWithAnimation(fabNextFormAncak)
                        }
                        keyboardOpenedWhileBottomSheetVisible = false
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

    @SuppressLint("CutPasteId", "SetTextI18n")
    private fun setupLayout() {
        infoBlokView = findViewById(R.id.svInfoBlokInspection)
        formInspectionView = findViewById(R.id.clFormInspection)
        summaryView = findViewById(R.id.clSummaryInspection)
        fabPrevFormAncak = findViewById(R.id.fabPrevFormInspect)
        fabNextFormAncak = findViewById(R.id.fabNextFormInspect)
        fabPhotoFormAncak = findViewById(R.id.fabPhotoFormInspect)
        labelPhotoFormInspect = findViewById(R.id.labelPhotoFormInspect)
        labelPhotoUser2 = findViewById(R.id.labelPhotoUser2)
        labelPhotoUser = findViewById(R.id.labelPhotoUser)
        fabFollowUpNow = findViewById(R.id.fabFollowUpNow)
        fabPhotoUser = findViewById(R.id.fabPhotoUser)
        fabPhotoUser2 = findViewById(R.id.fabPhotoUser2)

        fabSaveFormAncak = findViewById(R.id.fabSaveFormInspect)

        fabSaveFormAncak.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bluedarklight))

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                setupPemanenSpinner()
                setupPemuatSpinner()
                setupViewPager()
                observeViewModel()
                setupPressedFAB()
            }
        }

        val radiusText = "${boundaryAccuracy.toInt()} m"
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
            AppLogger.d("btnScanTPHRadius clicked")

            try {
                AppLogger.d("Current accuracy: $currentAccuracy, Boundary accuracy: $boundaryAccuracy")

                // Validate accuracy values
                if (!currentAccuracy.isFinite() || !boundaryAccuracy.isFinite()) {
                    AppLogger.e("Invalid accuracy values - current: $currentAccuracy, boundary: $boundaryAccuracy")
                    Toasty.error(
                        this,
                        "Error: Nilai akurasi GPS tidak valid!",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                    return@setOnClickListener
                }

                if (currentAccuracy <= boundaryAccuracy) {
                    AppLogger.d("GPS is within boundary - proceeding with scan")

                    try {
                        // GPS is within boundary - proceed directly
                        isTriggeredBtnScanned = true
                        selectedEstateByScan = null
                        selectedIdPanenByScan = null
                        selectedAfdelingByScan = null
                        selectedBlokByScan = null
                        selectedTPHNomorByScan = null
                        selectedAncakByScan = null
                        selectedTanggalPanenByScan = null
                        selectedTPHValue = null

                        AppLogger.d("Reset selection values and set trigger flag")

                        // Validate progress bar exists
                        if (::progressBarScanTPHManual.isInitialized) {
                            progressBarScanTPHManual.visibility = View.VISIBLE
                            AppLogger.d("Progress bar shown")
                        } else {
                            AppLogger.w("progressBarScanTPHManual not initialized")
                        }

                        // Use Handler with additional error checking
                        try {
                            Handler(Looper.getMainLooper()).postDelayed({
                                AppLogger.d("Handler delay completed, calling checkScannedTPHInsideRadius")
                                try {
                                    checkScannedTPHInsideRadius()
                                } catch (e: Exception) {
                                    AppLogger.e("Error in delayed checkScannedTPHInsideRadius: ${e.message}")
                                    AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")

                                    // Hide progress bar on error
                                    if (::progressBarScanTPHManual.isInitialized) {
                                        progressBarScanTPHManual.visibility = View.GONE
                                    }

                                    Toasty.error(
                                        this,
                                        "Error saat mencari TPH: ${e.message}",
                                        Toast.LENGTH_LONG,
                                        true
                                    ).show()
                                }
                            }, 400)
                        } catch (e: Exception) {
                            AppLogger.e("Error setting up Handler: ${e.message}")
                            Toasty.error(
                                this,
                                "Error sistem: ${e.message}",
                                Toast.LENGTH_LONG,
                                true
                            ).show()
                        }

                    } catch (e: Exception) {
                        AppLogger.e("Error in GPS boundary processing: ${e.message}")
                        Toasty.error(
                            this,
                            "Error memproses permintaan: ${e.message}",
                            Toast.LENGTH_LONG,
                            true
                        ).show()
                    }

                } else {
                    AppLogger.d("GPS accuracy outside boundary - showing error message")

                    val boundaryInt = try {
                        boundaryAccuracy.toInt()
                    } catch (e: Exception) {
                        AppLogger.e("Error converting boundaryAccuracy to int: ${e.message}")
                        100 // Default fallback
                    }

                    Toasty.error(
                        this,
                        "Akurasi GPS harus dalam radius ${boundaryInt} meter untuk melanjutkan!",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                }

            } catch (e: Exception) {
                AppLogger.e("Critical error in btnScanTPHRadius click listener: ${e.message}")
                AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")

                // Hide progress bar if visible
                try {
                    if (::progressBarScanTPHManual.isInitialized &&
                        progressBarScanTPHManual.visibility == View.VISIBLE) {
                        progressBarScanTPHManual.visibility = View.GONE
                    }
                } catch (progressBarError: Exception) {
                    AppLogger.e("Error hiding progress bar: ${progressBarError.message}")
                }

                Toasty.error(
                    this,
                    "Error sistem: ${e.message}",
                    Toast.LENGTH_LONG,
                    true
                ).show()
            }
        }

        bottomNavInspect.setOnItemSelectedListener { item ->
            val activeBottomNavId = bottomNavInspect.selectedItemId
            if (activeBottomNavId == item.itemId) return@setOnItemSelectedListener false

            if (activeBottomNavId == R.id.navMenuBlokInspect &&
                (item.itemId == R.id.navMenuAncakInspect || item.itemId == R.id.navMenuSummaryInspect)
            ) {
                AppLogger.d("Validating Blok section before navigation...")
                if (!validateAndShowErrors()) {
                    vibrate(500)
                    return@setOnItemSelectedListener false
                }
            }

            if (activeBottomNavId == R.id.navMenuAncakInspect &&
                (item.itemId == R.id.navMenuBlokInspect || item.itemId == R.id.navMenuSummaryInspect)
            ) {
                AppLogger.d("Validating Form Ancak before navigation...")
                if (!validateAndShowErrors()) {
                    vibrate(500)
                    return@setOnItemSelectedListener false
                }
            }

            if (activeBottomNavId == R.id.navMenuAncakInspect) {
                val currentPokok = formAncakViewModel.currentPage.value ?: 1
                val formData = formAncakViewModel.formData.value ?: mutableMapOf()
                val pokokData = formData[currentPokok]
                val photoValue = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                    pokokData?.foto_pemulihan ?: ""
                } else {
                    pokokData?.photo ?: ""
                }

                val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()
                val currentPage = formAncakViewModel.currentPage.value ?: 1

                val validationResult = formAncakViewModel.validateCurrentPage(1)

                if (!validationResult.isValid) {
                    vibrate(500)
                    AlertDialogUtility.withSingleAction(
                        this,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_data_not_completed),
                        "Mohon diisi data yang diperlukan!",
                        "warning.json",
                        R.color.colorRedDark
                    ) {}
                    return@setOnItemSelectedListener false
                }

                if (currentPage == AppUtils.MINIMAL_TAKE_SELFIE_INSPECTION && !hasSelfiePhoto) {
                    vibrate(500)
                    isForSelfie = true
                    showViewPhotoBottomSheet(null, false, true, false) // Selfie photo
                    AlertDialogUtility.withSingleAction(
                        this,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_data_not_completed),
                        "Mohon dapat mengambil foto selfie terlebih dahulu!",
                        "warning.json",
                        R.color.colorRedDark
                    ) {}
                    return@setOnItemSelectedListener false
                }

                if (pokokData != null) {
                    val emptyTreeValue = pokokData?.emptyTree ?: 0

                    // Only check findings and photo if emptyTree == 1
                    if (emptyTreeValue == 1) {
                        val buahMasakTdkDipotong = pokokData?.buahMasakTdkDipotong ?: 0
                        val btPiringanGawangan = pokokData?.btPiringanGawangan ?: 0
                        val brdKtpGawangan = pokokData?.brdKtpGawangan ?: 0
                        val brdKtpPiringanPikulKetiak = pokokData?.brdKtpPiringanPikulKetiak ?: 0

                        // Check if photo is required based on findings
                        val hasFindings = (buahMasakTdkDipotong > 0) ||
                                (btPiringanGawangan > 0) ||
                                (btPiringanGawangan > 0) ||
                                ((brdKtpGawangan + brdKtpPiringanPikulKetiak) > 50)
                        val hasValidPhoto =
                            photoValue.isNotEmpty() && photoValue.trim().isNotEmpty()

                        AppLogger.d("emptyTree: $emptyTreeValue, hasFindings: $hasFindings, hasValidPhoto: $hasValidPhoto")

                        if (hasFindings && !hasValidPhoto) {
                            vibrate(500)
                            if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
                                showViewPhotoBottomSheet(null, isInTPH, false, false)
                            } else {
                                isForFollowUp = true
                                showViewPhotoBottomSheet(
                                    null,
                                    false,
                                    false,
                                    true
                                ) // Follow-up photo
                            }
                            AlertDialogUtility.withSingleAction(
                                this,
                                stringXML(R.string.al_back),
                                stringXML(R.string.al_data_not_completed),
                                if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi)
                                    "Mohon dapat mengambil foto pemulihan terlebih dahulu!"
                                else
                                    "Mohon dapat mengambil foto temuan terlebih dahulu!",
                                "warning.json",
                                R.color.colorRedDark
                            ) {}
                            return@setOnItemSelectedListener false
                        }

                        if (hasFindings) {
                            formAncakViewModel.updatePokokDataWithLocationAndGetTrackingStatus(
                                currentPokok,
                                lat,
                                lon,
                                prefManager!!,
                                this@FormInspectionActivity
                            )
                        } else {
                            AppLogger.d("masuk ges $lat")
                            AppLogger.d("masuk ndak $lon")
                            formAncakViewModel.updatePokokDataWithLocationAndGetTrackingStatus(
                                currentPokok,
                                lat,
                                lon,
                                prefManager!!,
                                this@FormInspectionActivity
                            )
                        }
                    } else {
                        AppLogger.d("Skipping findings check - emptyTree is not 1 (value: $emptyTreeValue)")
                    }
                }

            }

            loadingDialog.show()
            loadingDialog.setMessage("Loading data...")

            lifecycleScope.launch {
                when (item.itemId) {
                    R.id.navMenuBlokInspect -> {
                        withContext(Dispatchers.Main) {
                            clFormInspection.visibility = View.GONE
                            clInfoBlokSection.visibility = View.VISIBLE
                            infoBlokView.visibility = View.VISIBLE
                            if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
                                fabPhotoInfoBlok.visibility = View.GONE

                            } else {
                                fabPhotoInfoBlok.visibility = View.VISIBLE
                                labelPhotoInfoBlok.visibility = View.VISIBLE
                                fabFollowUpTPH.visibility = View.VISIBLE
                                labelFollowUpTPH.visibility = View.VISIBLE
                            }

                            formInspectionView.visibility = View.GONE
                            summaryView.visibility = View.GONE
                            isInTPH = true
                            delay(200)
                            loadingDialog.dismiss()
                        }
                    }

                    R.id.navMenuAncakInspect -> {
                        AppLogger.d("menu form")
                        withContext(Dispatchers.Main) {
                            isInTPH = false
                            clInfoBlokSection.visibility = View.GONE
                            clFormInspection.visibility = View.VISIBLE
                            if (!trackingLocation.containsKey("start")) {
                                trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
                            }
                            updateWorkerDataInViewModel()
                            if (!hasInspectionStarted) {
                                if (!trackingLocation.containsKey("start")) {
                                    trackingLocation["start"] = Location(lat ?: 0.0, lon ?: 0.0)
                                }

                                dateStartInspection = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())

                                hasInspectionStarted = true
                                AppLogger.d("Inspection started at: $dateStartInspection")
                            } else {
                                AppLogger.d("Inspection already started, keeping original start time: $dateStartInspection")
                            }

                            val afdResult = selectedAfdeling.replaceFirst("AFD-", "")
                            formAncakViewModel.updateInfoFormAncak(
                                estateName ?: "",
                                afdResult,
                                selectedBlokByScan ?: ""
                            )
                            fabPhotoInfoBlok.visibility = View.GONE
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
                            isInTPH = false
                            setupSummaryPage()

                            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
                            val actualPagesVisited = formData.values.count { pageData ->
                                pageData.emptyTree != 0
                            }

                            // Show selfie FAB if actual pages with data is under 10
                            val shouldShowSelfieFab =
                                actualPagesVisited < AppUtils.MINIMAL_TAKE_SELFIE_INSPECTION

                            fabPhotoInfoBlok.visibility = View.GONE
                            labelPhotoInfoBlok.visibility = View.GONE
                            fabFollowUpTPH.visibility = View.GONE
                            labelFollowUpTPH.visibility = View.GONE
                            // Show/hide selfie FAB in summary
                            fabPhotoUser2?.visibility =
                                if (shouldShowSelfieFab) View.VISIBLE else View.GONE
                            labelPhotoUser2?.visibility =
                                if (shouldShowSelfieFab) View.VISIBLE else View.GONE

                            // Update selfie badge visibility
                            updateSelfiePhotoBadgeVisibility()

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
        // Check feature name and set visibility accordingly
        val shouldHideElements = featureName == AppUtils.ListFeatureNames.FollowUpInspeksi

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
                findViewById(R.id.lyKemandoran),
                "Kemandoran",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyPemuat),
                "Pemuat",
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
            if (shouldHideElements) {
                layoutView.visibility = View.GONE
                findViewById<TextView>(R.id.titleInformasiBlok).text = "Informasi Inspeksi"
                findViewById<TextView>(R.id.descTitleBlokInspeksi).text =
                    "Detail informasi inspeksi"
                findViewById<LinearLayout>(R.id.detailInspeksiForFollowUp).visibility = View.VISIBLE
                val warningCard = findViewById<View>(R.id.warning_card)
                warningCard.visibility = View.VISIBLE

                val warningTextView = warningCard.findViewById<TextView>(R.id.warningText)
                warningTextView.text =
                    "Mohon melakukan update data dibawah ini jika memang tidak ada temuan!"

                val btnCloseWarning = warningCard.findViewById<ImageButton>(R.id.btnCloseWarning)
                btnCloseWarning.setOnClickListener {
                    warningCard.visibility = View.GONE
                }

            } else {

                if (layoutView.id == R.id.lyKemandoran || layoutView.id == R.id.lyPemuat) {
                    layoutView.visibility = View.GONE
                } else {
                    layoutView.visibility = View.VISIBLE
                }

                updateLabelTextView(layoutView, key)
                when (inputType) {
                    InputType.SPINNER -> {
                        when (layoutView.id) {
                            R.id.lyEstInspect -> {
                                val namaEstate = prefManager!!.estateUserLengkapLogin
                                AppLogger.d("estateIdUserLogin: ${prefManager!!.estateIdUserLogin}")
                                AppLogger.d("estateUserLengkapLogin: ${prefManager!!.estateUserLengkapLogin}")

                                val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true

                                if (isGM) {
                                    // Split the estate names into a list for GM
                                    val estateList =
                                        namaEstate?.split(",")?.map { it.trim() } ?: emptyList()
                                    AppLogger.d("GM detected - Estate list: $estateList")
                                    setupSpinnerView(layoutView, estateList)
                                } else {
                                    // Single estate for non-GM users
                                    val singleEstateList = if (namaEstate.isNullOrEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(namaEstate)
                                    }
                                    AppLogger.d("Non-GM user - Single estate: $singleEstateList")
                                    setupSpinnerView(layoutView, singleEstateList)
                                }
//                                setupSpinnerView(layoutView, emptyList())
//                                val pemanenSpinner =
//                                    layoutView.findViewById<MaterialSpinner>(R.id.spPanenTBS)
//                                pemanenSpinner.setHint(namaEstate)
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
        }

        val counterMappings = listOf(
            Triple(
                R.id.lyBrdTglInspect,
                AppUtils.kodeInspeksi.brondolanTinggalTPH,
                ::jumBrdTglPath
            ),
            Triple(R.id.lyBuahTglInspect, AppUtils.kodeInspeksi.buahTinggalTPH, ::jumBuahTglPath),
        )


        if (featureName != AppUtils.ListFeatureNames.FollowUpInspeksi) {
            counterMappings.forEach { (layoutId, labelText, counterVar) ->
                setupPanenWithButtons(layoutId, labelText, counterVar)
            }
        }

    }

    private fun updateSelfiePhotoBadgeVisibility() {
        // Badge for User Selfie Section (fabPhotoUser2 in summary)
        val badgePhotoUser2 = findViewById<View>(R.id.badgePhotoUser2)
        val fabPhotoUser2 = findViewById<View>(R.id.fabPhotoUser2)
        val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()


        badgePhotoUser2?.visibility =
            if (fabPhotoUser2?.visibility == View.VISIBLE && hasSelfiePhoto) {
                View.VISIBLE
            } else {
                View.GONE
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

        // Badge for Form Data Section (fabPhotoFormInspect)
        val badgePhotoInspect = findViewById<ImageView>(R.id.badgePhotoInspect)
        val hasFormPhoto = currentData.photo != null

        badgePhotoInspect.visibility = if (hasFormPhoto) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Badge for Follow-Up Section (fabFollowUpNow)
        val badgeFollowUpNow = findViewById<ImageView>(R.id.badgeFollowUpNow)
        val hasFollowUpPhoto = currentData.foto_pemulihan != null

        badgeFollowUpNow.visibility = if (hasFollowUpPhoto) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Badge for Info Blok Section (fabPhotoInfoBlok)
        val badgePhotoInfoBlok = findViewById<View>(R.id.badgePhotoInfoBlok)
        val hasTPHPhoto = !photoInTPH.isNullOrEmpty()

        badgePhotoInfoBlok.visibility = if (hasTPHPhoto) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val badgePhotoFUTPH = findViewById<View>(R.id.badgePhotoFUTPH)
        val hasTPHFollowUpPhoto = !photoTPHFollowUp.isNullOrEmpty()

        badgePhotoFUTPH.visibility = if (hasTPHFollowUpPhoto) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Badge for User Selfie Section (fabPhotoUser)
        val badgePhotoUser = findViewById<View>(R.id.badgePhotoUser)
        val hasSelfiePhoto = !photoSelfie.isNullOrEmpty()

        badgePhotoUser.visibility = if (hasSelfiePhoto) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setupSummaryPage() {

        fun createRowTextView(
            text: String,
            gravity: Int,
            weight: Float,
            isTitle: Boolean = false
        ): TextView {
            return TextView(this).apply {
                this.text = text
                setPadding(32, 32, 32, 32)
                setTextColor(Color.BLACK)
                textSize = 15f
                this.gravity = gravity or Gravity.CENTER_VERTICAL
                // Use Manrope font
                typeface =
                    ResourcesCompat.getFont(this@FormInspectionActivity, R.font.manrope_semibold)
                maxLines = Int.MAX_VALUE
                ellipsize = null
                isSingleLine = false

                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
                        .apply {
                            setMargins(5, 5, 5, 5)
                        }

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(
                        ColorUtils.setAlphaComponent(
                            ContextCompat.getColor(
                                this@FormInspectionActivity,
                                R.color.graydarker
                            ), (0.2 * 255).toInt()
                        )
                    )
                    cornerRadii = if (isTitle) floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 20f, 20f)
                    else floatArrayOf(0f, 0f, 20f, 20f, 20f, 20f, 0f, 0f)
                }
            }
        }

        fun getPhotoCountForTemuan(temuanName: String): Int {
            return when (temuanName) {
                "Temuan di TPH" -> {
                    if (photoInTPH.isNullOrEmpty()) 0 else 1
                }

                "Path / Pokok" -> {
                    val formData = formAncakViewModel.formData.value ?: mutableMapOf()
                    formData.values.count { pageData ->
                        pageData.emptyTree == 1 && !pageData.photo.isNullOrEmpty()
                    }
                }

                else -> 0
            }
        }

        fun getTemuanCountForPath(): Int {
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()
            return formData.values.count { pageData ->
                pageData.emptyTree == 1
            }
        }

        fun getPathTotals(): Map<String, Int> {
            val formData = formAncakViewModel.formData.value ?: mutableMapOf()

            var totalbuahMasakTdkDipotong = 0
            var totalbuahTinggalPirGawangan = 0
            var totalbrdKtpGawangan = 0
            var totalbrdKtpPiringanPikulKetiak = 0

            formData.values.forEach { pageData ->
                // Only count values where emptyTree = 1
                if (pageData.emptyTree == 1) {
                    totalbuahMasakTdkDipotong += pageData.buahMasakTdkDipotong
                    totalbuahTinggalPirGawangan += pageData.btPiringanGawangan
                    totalbrdKtpGawangan += pageData.brdKtpGawangan
                    totalbrdKtpPiringanPikulKetiak += pageData.brdKtpPiringanPikulKetiak
                }
            }
            return mapOf(
                "buahMasakTdkDipotong" to totalbuahMasakTdkDipotong,
                "btPiringanGawangan" to totalbuahTinggalPirGawangan,
                "brdKtpGawangan" to totalbrdKtpGawangan,
                "brdKtpPiringanPikulKetiak" to totalbrdKtpPiringanPikulKetiak
            )
        }

        val pathTotals = getPathTotals()

        val desTPHEstateAfd = findViewById<TextView>(R.id.desTPHEstateAfd)
        desTPHEstateAfd.text = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            "${currentInspectionData?.tph?.dept_abbr} ${currentInspectionData?.tph?.divisi_abbr} ${currentInspectionData?.tph?.blok_kode}"
        } else {
            "${prefManager!!.estateUserLogin} ${selectedAfdeling} ${selectedBlokByScan}"
        }

        val desTPH = findViewById<TextView>(R.id.desTPH)
        val spannable = SpannableStringBuilder()

        spannable.append("TPH ")
        val tphStart = spannable.length

        val tphNomor = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            currentInspectionData?.tph?.nomor?.toString() ?: ""
        } else {
            selectedTPHNomorByScan?.toString() ?: ""
        }
        spannable.append(tphNomor)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            tphStart,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.append(" ,Ancak ")
        val ancakStart = spannable.length

        val ancakValue = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            currentInspectionData?.panen?.ancak?.toString() ?: ""
        } else {
            selectedAncakByScan ?: ""
        }

        spannable.append(ancakValue)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            ancakStart,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.append("\nTanggal Panen: ")
        val tanggalStart = spannable.length

        val formattedDate = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            currentInspectionData?.inspeksi?.date_panen?.let { datePanenJson ->
                try {
                    val datesJson = JSONArray(datePanenJson)
                    when {
                        datesJson.length() == 0 -> "Tidak tersedia"
                        datesJson.length() == 1 -> {
                            val singleDate = datesJson.getString(0)
                            AppUtils.formatToIndonesianDate(singleDate) // Just the formatted date
                        }

                        else -> {
                            val datesList = mutableListOf<String>()
                            for (i in 0 until datesJson.length()) {
                                val dateStr = datesJson.getString(i)
                                val formattedDate = AppUtils.formatToIndonesianDate(dateStr)
                                datesList.add("• $formattedDate") // Use bullet point instead of dash
                            }
                            val joinedDates = datesList.joinToString("\n")
                            "Total ${datesJson.length()} Transaksi\n$joinedDates"
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: treat as single date string for backward compatibility
                    AppUtils.formatToIndonesianDate(datePanenJson)
                }
            } ?: "Tidak tersedia"
        } else {
            selectedTanggalPanenByScan?.let { datePanenJson ->
                try {
                    val datesJson = JSONArray(datePanenJson)
                    when {
                        datesJson.length() == 0 -> "Tidak tersedia"
                        datesJson.length() == 1 -> {
                            val singleDate = datesJson.getString(0)
                            AppUtils.formatToIndonesianDate(singleDate) // Just the formatted date
                        }

                        else -> {
                            val datesList = mutableListOf<String>()
                            for (i in 0 until datesJson.length()) {
                                val dateStr = datesJson.getString(i)
                                val formattedDate = AppUtils.formatToIndonesianDate(dateStr)
                                datesList.add("• $formattedDate") // Use bullet point instead of dash
                            }
                            val joinedDates = datesList.joinToString("\n")
                            "Total ${datesJson.length()} Transaksi\n$joinedDates"
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: treat as single date string for backward compatibility
                    AppUtils.formatToIndonesianDate(datePanenJson)
                }
            } ?: "Tidak tersedia"
        }
        spannable.append(formattedDate)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            tanggalStart,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        desTPH.text = spannable

        val desJalurMasuk = findViewById<TextView>(R.id.desJalurMasuk)
        val jalurMasukText = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            currentInspectionData?.inspeksi?.jalur_masuk ?: ""
        } else {
            selectedJalurMasuk
        }
        desJalurMasuk.text = "Jalur Masuk: $jalurMasukText"

        val desJenisKondisi = findViewById<TextView>(R.id.desJenisKondisi)

        val kondisiText = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            when (currentInspectionData?.inspeksi?.jenis_kondisi) {
                1 -> "Datar"
                2 -> "Teras"
                else -> "Tidak Diketahui"
            }
        } else {
            when (selectedKondisiValue) {
                "1" -> "Datar"
                "2" -> "Teras"
                else -> "Tidak Diketahui"
            }
        }

        desJenisKondisi.text = "Jenis Kondisi: $kondisiText"

        val desBr1 = findViewById<TextView>(R.id.desBr1)
        val desBr2 = findViewById<TextView>(R.id.desBr2)

        desBr1.text = if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            val baris = currentInspectionData?.inspeksi?.baris ?: ""
            "Baris: $baris"
        } else {
            if (selectedKondisiValue.toInt() == 2) {
                "Baris: $br1Value"
            } else {
                "Baris: $br1Value, $br2Value"
            }
        }

        desBr2.visibility = View.GONE // Hide second TextView

        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        val formData = formAncakViewModel.formData.value ?: mutableMapOf()

        totalPokokInspection = (1..totalPages).count { pageNumber ->
            val emptyTreeValue = formData[pageNumber]?.emptyTree ?: 0
            emptyTreeValue == 1 || emptyTreeValue == 2
        }

        totalHarvestTree = (1..totalPages).sumOf { pageNumber ->
            formData[pageNumber]?.harvestJjg ?: 0
        }

        // Get container for dynamic cards
        val containerTemuanCards = findViewById<LinearLayout>(R.id.containerTemuanCards)
        containerTemuanCards.removeAllViews()

        // Dynamic temuan data
        val temuanDataList = listOf(
            "Temuan di TPH" to listOf(
                SummaryItem(AppUtils.kodeInspeksi.brondolanTinggalTPH, jumBrdTglPath.toString()),
                SummaryItem(AppUtils.kodeInspeksi.buahTinggalTPH, jumBuahTglPath.toString())
            ),
            "Path / Pokok" to listOf(
                SummaryItem("Jumlah Janjang Panen", totalHarvestTree.toString()),
                SummaryItem("Total Pokok Inspeksi", totalPokokInspection.toString()),
                SummaryItem(
                    AppUtils.kodeInspeksi.buahMasakTidakDipotong,
                    pathTotals["buahMasakTdkDipotong"].toString()
                ),
                SummaryItem(
                    AppUtils.kodeInspeksi.buahTertinggalPiringan,
                    pathTotals["btPiringanGawangan"].toString()
                ),
                SummaryItem(
                    AppUtils.kodeInspeksi.brondolanDigawangan,
                    pathTotals["brdKtpGawangan"].toString()
                ),
                SummaryItem(
                    AppUtils.kodeInspeksi.brondolanTidakDikutip,
                    pathTotals["brdKtpPiringanPikulKetiak"].toString()
                )
            )
        )

        for ((temuanName, data) in temuanDataList) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.layout_card_temuan, containerTemuanCards, false)

            // Set the temuan name
            cardView.findViewById<TextView>(R.id.name_temuan).text = temuanName

            val detailCard = cardView.findViewById<MaterialCardView>(R.id.cardDetailInspeksi)
            val tvCardDetailInspeksi = cardView.findViewById<TextView>(R.id.tvCardDetailInspeksi)
            val photoCard = cardView.findViewById<MaterialCardView>(R.id.photoCard)
            val issuesCard = cardView.findViewById<MaterialCardView>(R.id.issuesCard)

            val countPhotos = cardView.findViewById<TextView>(R.id.countPhotos)
            val countIssues = cardView.findViewById<TextView>(R.id.countIssues)

            // Set photo count (always visible)
            val photoCount = getPhotoCountForTemuan(temuanName)
            countPhotos.text = "$photoCount Foto"

            // Handle card visibility based on temuan type and counts
            when (temuanName) {
                "Temuan di TPH" -> {
                    tvCardDetailInspeksi.text = "Detail Foto"
                    issuesCard.visibility = View.GONE

                    if (photoCount > 0) {
                        detailCard.visibility = View.VISIBLE
                        photoCard.visibility = View.VISIBLE
                    } else {
                        photoCard.visibility = View.GONE
                        detailCard.visibility = View.GONE
                    }
                }

                "Path / Pokok" -> {
                    tvCardDetailInspeksi.text = "Detail Temuan Path"
                    if (photoCount > 0) {
                        photoCard.visibility = View.VISIBLE
                    } else {
                        photoCard.visibility = View.GONE
                    }

                    val issuesCount = getTemuanCountForPath()
                    if (issuesCount > 0) {
                        detailCard.visibility = View.VISIBLE
                        issuesCard.visibility = View.VISIBLE
                        countIssues.text = "$issuesCount Temuan"
                    } else {
                        detailCard.visibility = View.GONE
                        issuesCard.visibility = View.GONE
                    }
                }

                else -> {
                    // Default behavior for other types
                    photoCard.visibility = View.VISIBLE
                    issuesCard.visibility = View.VISIBLE
                    val issuesCount = data.size
                    countIssues.text = "$issuesCount Temuan"
                }
            }

            // Add click listener for detail card
            detailCard.setOnClickListener {
                showDetailBottomSheet(temuanName)
            }

            // Build the summary table
            val summaryContainer = cardView.findViewById<LinearLayout>(R.id.tblLytSummary)
            summaryContainer.removeAllViews()

            for (item in data) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    addView(createRowTextView(item.title, Gravity.START, 2f, true))
                    addView(createRowTextView(item.value, Gravity.CENTER, 1f, false))
                }

                summaryContainer.addView(rowLayout)
            }

            containerTemuanCards.addView(cardView)
        }
    }

    // Bottom sheet function
    // Updated Bottom sheet function with height control and disabled drag-to-dismiss
    private fun showDetailBottomSheet(temuanType: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_detail_inspeksi, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val llContainer = view.findViewById<LinearLayout>(R.id.llDetailContainer)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseDetail)

        // Set title
        tvTitle.text = "Detail $temuanType"

        // Handle content based on temuan type
        when (temuanType) {
            "Temuan di TPH" -> {
                setupTPHDetail(ivImage, llContainer, bottomSheetDialog)
            }

            "Path / Pokok" -> {
                setupPathDetail(ivImage, llContainer, bottomSheetDialog)
            }
        }

        // Close button
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)

        // Set bottom sheet height to 80% of screen height and disable drag-to-dismiss
        bottomSheetDialog.setOnShowListener { dialog ->
            val bottomSheet =
                (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val screenHeight = resources.displayMetrics.heightPixels
                var desiredHeight = 0
                if (temuanType == "Temuan di TPH") {
                    desiredHeight = (screenHeight * 0.5).toInt()
                } else {
                    desiredHeight = (screenHeight * 0.8).toInt()
                }
                // 80% of screen height

                val layoutParams = bottomSheet.layoutParams
                layoutParams.height = desiredHeight
                bottomSheet.layoutParams = layoutParams

                // Set the view height as well
                val viewLayoutParams = view.layoutParams ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    desiredHeight
                )
                viewLayoutParams.height = desiredHeight
                view.layoutParams = viewLayoutParams

                // Disable dragging to prevent accidental dismiss
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.isDraggable = false // Disable drag to dismiss
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // Disable swipe to dismiss
                bottomSheetDialog.setCancelable(false)
                bottomSheetDialog.setCanceledOnTouchOutside(false)
            }
        }

        bottomSheetDialog.show()
    }


    // Updated setupTPHDetail - DON'T dismiss bottom sheet, just hide it temporarily
    private fun setupTPHDetail(
        imageView: ImageView,
        container: LinearLayout,
        bottomSheetDialog: BottomSheetDialog? = null
    ) {
        // Show image for TPH
        if (!photoInTPH.isNullOrEmpty()) {
            imageView.visibility = View.VISIBLE

            // Build the correct file path
            val rootApp = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "CMP-${AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH}"
            ).toString()

            val fullImagePath = File(rootApp, photoInTPH).absolutePath

            AppLogger.d("TPH Photo Details:")
            AppLogger.d("photoInTPH: $photoInTPH")
            AppLogger.d("rootApp: $rootApp")
            AppLogger.d("fullImagePath: $fullImagePath")

            val file = File(fullImagePath)
            AppLogger.d("File exists: ${file.exists()}")

            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(fullImagePath)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        AppLogger.d("Image loaded successfully")

                        // Add click listener to IMAGE for full screen view
                        imageView.setOnClickListener {
                            // Hide bottom sheet temporarily instead of dismissing
//                            bottomSheetDialog?.hide()
                            showFullScreenPhoto(
                                fullImagePath,
                                "Detail Temuan di TPH",
                                bottomSheetDialog
                            )
                        }
                    } else {
                        AppLogger.e("Failed to decode bitmap from file")
                        imageView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error loading image: ${e.message}")
                    imageView.visibility = View.GONE
                }
            } else {
                AppLogger.e("Image file not found at: $fullImagePath")
                imageView.visibility = View.GONE
            }
        } else {
            AppLogger.d("photoInTPH is null or empty")
            imageView.visibility = View.GONE
        }

        // Hide container for TPH (no additional content needed)
        container.visibility = View.GONE
    }

    // Updated showFullScreenPhoto - accept bottomSheetDialog to show it back when closing
    private fun showFullScreenPhoto(
        imagePath: String,
        title: String,
        bottomSheetDialog: BottomSheetDialog? = null
    ) {
        AppLogger.d("showFullScreenPhoto called with: $imagePath, $title")

        // Create full screen dialog
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // Inflate your existing camera_edit layout
        val view = LayoutInflater.from(this).inflate(R.layout.camera_edit, null)
        dialog.setContentView(view)

        // Find components in the inflated layout
        val fotoZoom = view.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.fotoZoom)
        val cardCloseZoom = view.findViewById<MaterialCardView>(R.id.cardCloseZoom)
        val cardChangePhoto = view.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val cardDeletePhoto = view.findViewById<MaterialCardView>(R.id.cardDeletePhoto)
        val clZoomLayout = view.findViewById<ConstraintLayout>(R.id.clZoomLayout)

        AppLogger.d("Dialog components found: fotoZoom=${fotoZoom != null}, cardCloseZoom=${cardCloseZoom != null}")

        // Make sure the zoom layout is visible
        clZoomLayout?.visibility = View.VISIBLE

        // Load image into PhotoView
        val file = File(imagePath)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    fotoZoom?.setImageBitmap(bitmap)
                    AppLogger.d("Full screen photo loaded successfully in dialog: $title")
                } else {
                    AppLogger.e("Failed to decode bitmap for full screen")
                    return
                }
            } catch (e: Exception) {
                AppLogger.e("Error loading full screen photo: ${e.message}")
                return
            }
        } else {
            AppLogger.e("Image file not found for full screen: $imagePath")
            return
        }

        // Hide change and delete buttons for view-only mode
        cardChangePhoto?.visibility = View.GONE
        cardDeletePhoto?.visibility = View.GONE

        // Function to close full screen and show bottom sheet again
        fun closeFullScreenAndShowBottomSheet() {
            AppLogger.d("Closing full screen and showing bottom sheet again")
            dialog.dismiss()
//            bottomSheetDialog?.show() // Show the bottom sheet again
        }

        // Set up close button
        cardCloseZoom?.setOnClickListener {
            AppLogger.d("Close button clicked")
            closeFullScreenAndShowBottomSheet()
        }

        // Optional: Close on photo tap
        fotoZoom?.setOnClickListener {
            AppLogger.d("Photo tapped - closing dialog")
            closeFullScreenAndShowBottomSheet()
        }

        // Show dialog
        try {
            dialog.show()
            AppLogger.d("Full screen dialog shown successfully")
        } catch (e: Exception) {
            AppLogger.e("Error showing full screen dialog: ${e.message}")
        }
    }

    private fun createPathDetailCard(
        pageNumber: Int,
        pageData: FormAncakViewModel.PageData,
        bottomSheetDialog: BottomSheetDialog? = null
    ): View {
        // Inflate the card layout
        val cardView = LayoutInflater.from(this).inflate(R.layout.layout_temuan_path_inspeksi, null)

        // Set margins programmatically for proper spacing
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 5.dpToPx(), 0, 5.dpToPx()) // 5dp top and bottom margins
        }
        cardView.layoutParams = layoutParams

        // Find views
        val tvPokokNumber = cardView.findViewById<TextView>(R.id.tvPokokNumber)
        val cardFoto = cardView.findViewById<MaterialCardView>(R.id.cardFoto)
        val cardTelusuri = cardView.findViewById<MaterialCardView>(R.id.cardEdit)
        val ivPokokPhoto = cardView.findViewById<ImageView>(R.id.ivPokokPhoto)
        val llDetailsList = cardView.findViewById<LinearLayout>(R.id.llDetailsList)

        // Set pokok number
        tvPokokNumber.text = "Pokok #$pageNumber"

        // Determine if photo is required based on findings
        val buahMasakTdkDipotong = pageData.buahMasakTdkDipotong
        val btPiringanGawangan = pageData.btPiringanGawangan
        val brdKtpGawangan = pageData.brdKtpGawangan
        val brdKtpPiringanPikulKetiak = pageData.brdKtpPiringanPikulKetiak
        val emptyTreeValue = pageData.emptyTree

        val hasFindings = (emptyTreeValue == 1) ||
                (buahMasakTdkDipotong > 0) ||
                (btPiringanGawangan > 0) ||
                (brdKtpGawangan > 0) ||
                (brdKtpPiringanPikulKetiak > 50)

        // Handle photo availability and card visibility based on findings
        if (hasFindings) {
            // Show FOTO card when findings exist
            if (!pageData.photo.isNullOrEmpty()) {
                // Build correct path for pageData photo
                val rootApp = File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "CMP-${AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok}"
                ).toString()

                val fullImagePath = File(rootApp, pageData.photo).absolutePath

                AppLogger.d("Path Photo - pageNumber: $pageNumber")
                AppLogger.d("pageData.photo: ${pageData.photo}")
                AppLogger.d("fullImagePath: $fullImagePath")

                val file = File(fullImagePath)
                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(fullImagePath)
                        if (bitmap != null) {
                            // Show the actual image preview in the small ImageView
                            ivPokokPhoto.setImageBitmap(bitmap)
                            ivPokokPhoto.scaleType = ImageView.ScaleType.CENTER_CROP

                            // 🚀 REMOVE THE WHITE TINT FOR ACTUAL PHOTOS
                            ivPokokPhoto.imageTintList = null

                            cardFoto.visibility = View.VISIBLE
                            AppLogger.d("Path image loaded successfully for pokok $pageNumber")

                            // Add click listener to FOTO CARD for full screen
                            cardFoto.setOnClickListener {
                                showFullScreenPhoto(fullImagePath, "Pokok $pageNumber")
                            }
                        } else {
                            AppLogger.e("Failed to decode bitmap for pokok $pageNumber")
                            // Show camera icon as fallback
                            ivPokokPhoto.setImageResource(R.drawable.baseline_image_not_supported_24)
                            ivPokokPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE

                            // Keep white tint for the fallback icon
                            ivPokokPhoto.imageTintList =
                                ContextCompat.getColorStateList(this, R.color.white)

                            cardFoto.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error loading path image for pokok $pageNumber: ${e.message}")
                        // Show camera icon as fallback
                        ivPokokPhoto.setImageResource(R.drawable.baseline_image_not_supported_24)
                        ivPokokPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        cardFoto.visibility = View.VISIBLE
                    }
                } else {
                    cardFoto.visibility = View.GONE
                }
            } else {
                // No photo available but findings exist - show camera icon (needs photo)
                ivPokokPhoto.setImageResource(R.drawable.baseline_image_not_supported_24)
                ivPokokPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
                cardFoto.visibility = View.VISIBLE
            }
        } else {
            // No significant findings - hide FOTO card completely
            cardFoto.visibility = View.GONE
            AppLogger.d("FOTO card hidden - no significant findings for pokok $pageNumber")
        }

        // Add click listener to Telusuri card (for future detail functionality)
        cardTelusuri.setOnClickListener {
            AppLogger.d("Edit clicked for pokok $pageNumber")

            // Close the bottom sheet first
            bottomSheetDialog?.dismiss()

            // 🚀 TRIGGER THE NAVIGATION PROGRAMMATICALLY
            // This will trigger bottomNavInspect.setOnItemSelectedListener automatically
            bottomNavInspect.selectedItemId = R.id.navMenuAncakInspect

            // Wait a bit for navigation to complete, then navigate to specific pokok
            Handler(Looper.getMainLooper()).postDelayed({
                // Set current page to the clicked pokok number
                formAncakViewModel.setCurrentPage(pageNumber)

                // Update ViewPager to show the correct page
                vpFormAncak.setCurrentItem(pageNumber - 1, true)

                AppLogger.d("Navigated to pokok $pageNumber in form")
            }, 500) // Increased delay to let navigation finish first
        }

        // Add detail rows dynamically (only show non-zero values)
        var hasData = false

        // Helper function to get text value from radio items
        fun getRadioText(category: String, value: Int): String {
            val listRadioItems: Map<String, Map<String, String>> = mapOf(
                "YesOrNoOrTitikKosong" to mapOf(
                    "1" to "Ya",
                    "2" to "Tidak",
                    "3" to "Titik Kosong"
                ),
                "YesOrNo" to mapOf(
                    "1" to "Ya",
                    "2" to "Tidak"
                ),
                "HighOrLow" to mapOf(
                    "1" to "Tinggi",
                    "2" to "Rendah"
                ),
                "ExistsOrNot" to mapOf(
                    "1" to "Ada",
                    "2" to "Tidak"
                ),
                "NeatOrNot" to mapOf(
                    "1" to "Standar",
                    "2" to "Tidak Standar"
                ),
                "PelepahType" to mapOf(
                    "1" to "Ada",
                    "2" to "Tidak ada"
                ),
                "PruningType" to mapOf(
                    "1" to "Standard",
                    "2" to "Overpruning",
                    "3" to "Underpruning"
                )
            )

            val categoryMap = when (category) {
                "harvestTree" -> listRadioItems["YesOrNo"]
                "neatPelepah" -> listRadioItems["NeatOrNot"]
                "pelepahSengkleh" -> listRadioItems["PelepahType"]
                "overPruning" -> listRadioItems["PelepahType"]
                "underPruning" -> listRadioItems["PelepahType"]
                else -> null
            }

            val result = categoryMap?.get(value.toString()) ?: value.toString()
            return result
        }


        if (pageData.harvestTree > 0) {
            llDetailsList.addView(
                createDetailRow(
                    "Pokok di Panen",
                    ": ${getRadioText("harvestTree", pageData.harvestTree)}"
                )
            )
            hasData = true
        }

        if (pageData.neatPelepah > 0) {
            llDetailsList.addView(
                createDetailRow(
                    "Susunan Pelepah",
                    ": ${getRadioText("neatPelepah", pageData.neatPelepah)}"
                )
            )
            hasData = true
        }

        if (pageData.pelepahSengkleh > 0) {
            llDetailsList.addView(
                createDetailRow(
                    "Pelepah Sengkleh",
                    ": ${getRadioText("pelepahSengkleh", pageData.pelepahSengkleh)}"
                )
            )
            hasData = true
        }

        if (pageData.kondisiPruning > 0) {
            llDetailsList.addView(
                createDetailRow(
                    "Over Pruning",
                    ": ${getRadioText("kondisiPruning", pageData.kondisiPruning)}"
                )
            )
            hasData = true
        }

//        if (pageData.underPruning > 0) {
//            llDetailsList.addView(
//                createDetailRow(
//                    "Under Pruning",
//                    ": ${getRadioText("underPruning", pageData.underPruning)}"
//                )
//            )
//            hasData = true
//        }

// Check if any numeric field has data > 0
        val hasNumericData = (pageData.buahMasakTdkDipotong > 0) ||
                (pageData.btPiringanGawangan > 0) ||
                (pageData.brdKtpGawangan > 0) ||
                (pageData.brdKtpPiringanPikulKetiak > 0)

        if (hasNumericData) {
            // Show ALL numeric fields if ANY has data > 0
            llDetailsList.addView(
                createDetailRow(
                    AppUtils.kodeInspeksi.buahMasakTidakDipotong,
                    pageData.buahMasakTdkDipotong.toString()
                )
            )

            llDetailsList.addView(
                createDetailRow(
                    AppUtils.kodeInspeksi.buahTertinggalPiringan,
                    pageData.btPiringanGawangan.toString()
                )
            )

            llDetailsList.addView(
                createDetailRow(
                    AppUtils.kodeInspeksi.brondolanDigawangan,
                    pageData.brdKtpGawangan.toString()
                )
            )

            llDetailsList.addView(
                createDetailRow(
                    AppUtils.kodeInspeksi.brondolanTidakDikutip,
                    pageData.brdKtpPiringanPikulKetiak.toString()
                )
            )

            hasData = true
        }


        if (!hasData) {
            val emptyText = TextView(this).apply {
                text = "Tidak ada temuan"
                textSize = 15f
                typeface =
                    ResourcesCompat.getFont(this@FormInspectionActivity, R.font.manrope_semibold)
                setTextColor(
                    ContextCompat.getColor(
                        this@FormInspectionActivity,
                        R.color.graydarker
                    )
                )
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }

            // Set proper layout params when adding to container
            val emptyTextLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            llDetailsList.addView(emptyText, emptyTextLayoutParams)
        }

        return cardView
    }

    // Fixed Helper function to create detail rows with proper layout parameters
    private fun createDetailRow(label: String, value: String): View {
        val rowView = LayoutInflater.from(this).inflate(R.layout.list_issue_inspeksi_path, null)


        val tvLabel = rowView.findViewById<TextView>(R.id.tvLabel)
        val tvValue = rowView.findViewById<TextView>(R.id.tvValue)

        // Configure label text view for better text wrapping
        tvLabel.apply {
            text = label
        }

        tvValue.apply {
            text = value
        }

        return rowView
    }

    // Extension function to convert dp to px (add this if you don't have it already)
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    // Updated setupPathDetail with "Tidak ada temuan" message
    private fun setupPathDetail(
        imageView: ImageView,
        container: LinearLayout,
        bottomSheetDialog: BottomSheetDialog? = null
    ) {
        // Hide image for Path
        imageView.visibility = View.GONE

        // Show container for Path
        container.visibility = View.VISIBLE
        container.removeAllViews()

        // Get form data
        val formData = formAncakViewModel.formData.value ?: mutableMapOf()

        // Create detail items for each page with temuan
        formData.forEach { (pageNumber, pageData) ->
            if (pageNumber != 0 && (pageData.emptyTree == 1 || pageData.emptyTree == 2)) {
                val detailCard = createPathDetailCard(pageNumber, pageData, bottomSheetDialog)
                container.addView(detailCard)
            }
        }

        // If no items, show empty message
        if (container.childCount == 0) {
            val emptyText = TextView(this).apply {
                text = "Tidak ada temuan"
                textSize = 16f
                typeface =
                    ResourcesCompat.getFont(this@FormInspectionActivity, R.font.manrope_semibold)
                setTextColor(
                    ContextCompat.getColor(
                        this@FormInspectionActivity,
                        R.color.graydarker
                    )
                )
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            container.addView(emptyText)
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

        val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true

        if (!isGM) {
            if (linearLayout.id == R.id.lyEstInspect) {
                spinner.isEnabled = false
            }
        }

        if (linearLayout.id == R.id.lyPemuat || linearLayout.id == R.id.lyKemandoran) {
            fun ensureKeyboardHidden() {
                try {
                    val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(linearLayout.windowToken, 0)
                    editText.clearFocus()
                } catch (e: Exception) {
                    Log.e("SetupSpinnerView", "Error hiding keyboard: ${e.message}", e)
                }
            }

            spinner.setOnTouchListener { _, event ->
                try {
                    ensureKeyboardHidden()
                    if (event.action == MotionEvent.ACTION_UP) {
                        showPopupSearchDropdown(
                            spinner,
                            data,
                            editText,
                            linearLayout,
                            false // Single-select for lypemuat
                        ) { selectedItem, position ->
                            try {
                                spinner.text = selectedItem // Update spinner UI
                                tvError.visibility = View.GONE
                                handleItemSelection(linearLayout, position, selectedItem)
                            } catch (e: Exception) {
                                Log.e("SetupSpinnerView", "Error in item selection: ${e.message}", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SetupSpinnerView", "Error in touch listener: ${e.message}", e)
                }
                true // Consume event, preventing default behavior
            }
        } else {
            // Keep original behavior for other layouts
            spinner.setOnItemSelectedListener { _, position, _, item ->
                tvError.visibility = View.GONE
                handleItemSelection(linearLayout, position, item.toString())
            }
        }
    }

    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
        isMultiSelect: Boolean = false,
        onItemSelected: (String, Int) -> Unit
    ) {
        val popupView = LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
        val listView = popupView.findViewById<ListView>(R.id.listViewChoices)
        val editTextSearch = popupView.findViewById<EditText>(R.id.searchEditText)

        val scrollView = findScrollView(linearLayout)
        val rootView = linearLayout.rootView

        // Simple selection tracking for multi-select (if needed)
        val selectedItems = mutableMapOf<String, Boolean>()

        // Create PopupWindow
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

        // Keyboard adjustment
        var keyboardHeight = 0
        val rootViewLayout = rootView.viewTreeObserver
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
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

        // Choose adapter based on selection mode
        val adapter = if (isMultiSelect) {
            object : ArrayAdapter<String>(
                spinner.context,
                R.layout.list_item_dropdown_multiple,
                R.id.text1,
                filteredData
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
                    val textView = view.findViewById<TextView>(R.id.text1)
                    val itemValue = filteredData[position]

                    textView.text = itemValue
                    textView.setTextColor(Color.BLACK)
                    checkbox.isChecked = selectedItems[itemValue] == true

                    checkbox.setOnClickListener {
                        selectedItems[itemValue] = checkbox.isChecked
                    }

                    view.setOnClickListener {
                        checkbox.isChecked = !checkbox.isChecked
                        selectedItems[itemValue] = checkbox.isChecked
                    }

                    return view
                }

                override fun isEnabled(position: Int): Boolean {
                    return filteredData.isNotEmpty()
                }
            }
        } else {
            object : ArrayAdapter<String>(
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
        }

        listView.adapter = adapter

        // Search functionality
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

                // Update adapter with filtered data
                val filteredAdapter = if (isMultiSelect) {
                    object : ArrayAdapter<String>(
                        spinner.context,
                        R.layout.list_item_dropdown_multiple,
                        R.id.text1,
                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            listOf("Data tidak tersedia!")
                        } else {
                            filteredData
                        }
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(R.id.text1)
                            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)

                            if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                                textView.setTextColor(ContextCompat.getColor(context, R.color.colorRedDark))
                                textView.setTypeface(textView.typeface, Typeface.ITALIC)
                                checkbox.visibility = View.GONE
                                view.isEnabled = false
                            } else {
                                val itemValue = filteredData[position]
                                textView.text = itemValue
                                textView.setTextColor(Color.BLACK)
                                textView.setTypeface(textView.typeface, Typeface.NORMAL)
                                checkbox.visibility = View.VISIBLE
                                checkbox.isChecked = selectedItems[itemValue] == true

                                checkbox.setOnClickListener {
                                    selectedItems[itemValue] = checkbox.isChecked
                                }

                                view.setOnClickListener {
                                    checkbox.isChecked = !checkbox.isChecked
                                    selectedItems[itemValue] = checkbox.isChecked
                                }
                                view.isEnabled = true
                            }
                            return view
                        }

                        override fun isEnabled(position: Int): Boolean {
                            return filteredData.isNotEmpty()
                        }
                    }
                } else {
                    object : ArrayAdapter<String>(
                        spinner.context,
                        android.R.layout.simple_list_item_1,
                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            listOf("Data tidak tersedia!")
                        } else {
                            filteredData
                        }
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)

                            if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                                textView.setTextColor(ContextCompat.getColor(context, R.color.colorRedDark))
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
                }

                listView.adapter = filteredAdapter
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Handle item selection (single-select mode)
        listView.setOnItemClickListener { _, _, position, _ ->
            if (filteredData.isNotEmpty()) {
                val selectedItem = filteredData[position]
                val originalPosition = data.indexOf(selectedItem)
                spinner.text = selectedItem
                editText.setText(selectedItem)
                onItemSelected(selectedItem, originalPosition)
                popupWindow.dismiss()
            }
        }

        // Show popup and focus on search
        popupWindow.showAsDropDown(spinner)
        editTextSearch.requestFocus()
        val imm = spinner.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
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

            R.id.lyEstInspect -> { // Add this case for estate selection
                AppLogger.d("Estate selected: $selectedItem at position $position")

                // Reset related data when estate changes
                selectedEstate = selectedItem
                selectedEstateIdSpinner = position

                // Get the estate ID from the estate list
                val selectedEstateId = try {
                    // Assuming you have an estate list with IDs corresponding to positions
                    // You might need to adjust this based on your estate data structure
                    val estateIds =
                        prefManager!!.estateIdUserLogin?.split(",")?.map { it.trim().toInt() }
                            ?: emptyList()
                    if (position < estateIds.size) {
                        estateIds[position]
                    } else {
                        AppLogger.e("Invalid estate position: $position")
                        return
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error getting estate ID: ${e.message}")
                    return
                }

                val estateAbbrList = prefManager!!.estateUserLogin
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()

                val selectedEstateAbbr = if (position < estateAbbrList.size) {
                    estateAbbrList[position]
                } else {
                    AppLogger.e("Invalid estate position: $position")
                    return
                }

                estateAbbr = selectedEstateAbbr

                // Update current estate
                estateId = selectedEstateId.toString()
                AppLogger.d("Updated estateId to: $estateId")

                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        animateLoadingDots(linearLayout)
                        delay(300)
                    }

                    try {
                        val divisiDeferred = async {
                            try {
                                datasetViewModel.getDivisiList(selectedEstateId)
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching divisi list: ${e.message}")
                                emptyList()
                            }
                        }

                        divisiList = divisiDeferred.await()

                        withContext(Dispatchers.Main) {
                            setupSpinnerView(lyAfdInspect, divisiList.mapNotNull { it.divisi_abbr })
                        }

                    } catch (e: Exception) {
                        AppLogger.e("Error loading estate data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FormInspectionActivity,
                                "Error loading estate data: ${e.message}",
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

            R.id.lyPemanenOtomatis -> {
                AppLogger.d("Automatic spinner selection triggered: $selectedItem at position $position")

                // Find the selected worker from automatic available list
                val selectedWorker = allAvailableKaryawanList.find { karyawan ->
                    val formattedName = "${karyawan.nik} - ${karyawan.nama}"
                    formattedName == selectedItem
                }

                AppLogger.d("Found selected automatic worker: ${selectedWorker?.nama} (NIK: ${selectedWorker?.nik})")

                if (selectedWorker != null) {
                    // Check if worker already exists in selectedKaryawanList
                    val isDuplicate = selectedKaryawanList.any { existing ->
                        existing.nik == selectedWorker.nik && existing.nama == selectedWorker.nama
                    }

                    if (isDuplicate) {
                        AppLogger.w("Worker ${selectedWorker.nama} (NIK: ${selectedWorker.nik}) already selected! Skipping duplicate.")
//                        Toast.makeText(
//                            this,
//                            "${selectedWorker.nama} (NIK: ${selectedWorker.nik}) sudah dipilih sebelumnya!",
//                            Toast.LENGTH_SHORT
//                        ).show()

                        Toasty.error(
                            this,
                            "${selectedWorker.nama} (NIK: ${selectedWorker.nik}) sudah dipilih sebelumnya!",
                            Toast.LENGTH_SHORT,
                            true
                        ).show()
                        return
                    }

                    // Add to AUTOMATIC RecyclerView
                    val worker = Worker(selectedWorker.individualId, selectedItem)
                    selectedPemanenAdapter.addWorker(worker)

                    // Add to selectedKaryawanList
                    val originalSize = selectedKaryawanList.size
                    selectedKaryawanList = selectedKaryawanList + selectedWorker

                    AppLogger.d("selectedPemanenAdapter $selectedPemanenAdapter")
                    AppLogger.d("selectedKaryawanList size changed from $originalSize to ${selectedKaryawanList.size}")

                    // Show automatic RecyclerView if it's hidden
                    val rvSelectedPemanenOtomatis =
                        findViewById<RecyclerView>(R.id.rvSelectedPemanenOtomatisInspection)
                    rvSelectedPemanenOtomatis.visibility = View.VISIBLE

                    // Update automatic spinner to remove selected worker
                    AppLogger.d("Calling updatePemanenSpinnerAfterRemoval after selection...")
                    updatePemanenSpinnerAfterRemoval()

                    AppLogger.d("Added automatic worker to RecyclerView: ${selectedWorker.nama} (NIK: ${selectedWorker.nik})")
                    AppLogger.d("Updated selectedKaryawanList size: ${selectedKaryawanList.size}")
                } else {
                    AppLogger.e("Could not find selected automatic worker: $selectedItem")
                }
            }

            R.id.lyPemanenManual -> {
                AppLogger.d("Manual spinner selection triggered: $selectedItem at position $position")

                // Find the selected worker from manual available list
                val selectedWorker = allManualKaryawanList.find { karyawan ->
                    val formattedName = "${karyawan.nik} - ${karyawan.nama}"
                    formattedName == selectedItem
                }

                AppLogger.d("Found selected manual worker: ${selectedWorker?.nama} (NIK: ${selectedWorker?.nik})")

                if (selectedWorker != null) {
                    // Check if worker already exists in selectedKaryawanList
                    val isDuplicate = selectedKaryawanList.any { existing ->
                        existing.nik == selectedWorker.nik && existing.nama == selectedWorker.nama
                    }

                    if (isDuplicate) {
                        AppLogger.w("Worker ${selectedWorker.nama} (NIK: ${selectedWorker.nik}) already selected! Skipping duplicate.")

                        Toasty.error(
                            this,
                            "${selectedWorker.nama} (NIK: ${selectedWorker.nik}) sudah dipilih sebelumnya!",
                            Toast.LENGTH_SHORT,
                            true
                        ).show()
                        return
                    }

                    // Add to MANUAL RecyclerView
                    val worker = Worker(selectedWorker.individualId, selectedItem)
                    selectedPemanenManualAdapter.addWorker(worker)

                    AppLogger.d("selectedPemanenManualAdapter $selectedPemanenManualAdapter")

                    // Add to selectedKaryawanList
                    val originalSize = selectedKaryawanList.size
                    selectedKaryawanList = selectedKaryawanList + selectedWorker
                    AppLogger.d("Manual selectedKaryawanList size changed from $originalSize to ${selectedKaryawanList.size}")

                    // Show manual RecyclerView if it's hidden
                    val rvSelectedPemanenManual =
                        findViewById<RecyclerView>(R.id.rvSelectedPemanenManualInspection)
                    rvSelectedPemanenManual.visibility = View.VISIBLE

                    // Update manual spinner to remove selected worker
                    AppLogger.d("Calling updateManualPemanenSpinnerAfterRemoval after selection...")
                    updateManualPemanenSpinnerAfterRemoval()

                    AppLogger.d("Added manual worker to RecyclerView: ${selectedWorker.nama} (NIK: ${selectedWorker.nik})")
                    AppLogger.d("Updated selectedKaryawanList size: ${selectedKaryawanList.size}")
                } else {
                    AppLogger.e("Could not find selected manual worker: $selectedItem")
                }
            }

            R.id.lyAfdInspect -> {
                hideResultScan()
                selectedAfdeling = selectedItem
                selectedAfdelingIdSpinner = position

                isTriggeredBtnScanned = false

                val isGM = jabatanUser?.contains(
                    AppUtils.ListFeatureByRoleUser.GM,
                    ignoreCase = true
                ) == true

                AppLogger.d("isGM $isGM")
                AppLogger.d("selectedAfdeling $selectedAfdeling")

                AppLogger.d("divisiList $divisiList")
                val selectedDivisiId = try {
                    if (isGM) {
                        divisiList.find {
                            it.divisi_abbr == selectedAfdeling && it.dept_nama == selectedEstate
                        }?.divisi
                    } else {
                        divisiList.find {
                            it.divisi_abbr == selectedAfdeling
                        }?.divisi
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error finding divisi: ${e.message}")
                    null
                }

                val allIdAfdeling = try {
                    divisiList.map { it.divisi }
                } catch (e: Exception) {
                    AppLogger.e("Error mapping allIdAfdeling: ${e.message}")
                    emptyList()
                }

                AppLogger.d("selectedDivisiId $selectedDivisiId")
                AppLogger.d("allIdAfdeling $allIdAfdeling")
                val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()
                selectedDivisiValue = selectedDivisiId?.toInt()

                val nonSelectedAfdelingKemandoran = try {
                    divisiList.filter { it.divisi_abbr != selectedAfdeling }
                } catch (e: Exception) {
                    emptyList()
                }

                lifecycleScope.launch {

                    if (isGM) {
                        try {
                            val estateIdStr = estateId?.trim()
                            if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                                val estateIdInt = estateIdStr.toInt()

                                val panenDeferred = CompletableDeferred<List<PanenEntityWithRelations>>()

                                panenViewModel.getAllTPHinWeek(estateIdInt)
                                delay(100)

                                withContext(Dispatchers.Main) {
                                    panenViewModel.activePanenList.observe(this@FormInspectionActivity) { list ->
                                        AppLogger.d("panenTPH updated for GM after afdeling selection: ${list?.size}")
                                        panenTPH = list ?: emptyList()
                                        panenDeferred.complete(list ?: emptyList())
                                    }
                                }

                                // Wait for the data to be loaded
                                panenDeferred.await()
                                AppLogger.d("GM - panenTPH reloaded with ${panenTPH.size} records")
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error reloading panenTPH for GM: ${e.message}")
                        }
                    } else {
                        AppLogger.d("Non-GM user - skipping panenTPH reload")
                    }

                    // Continue with existing logic
                    withContext(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            setupScanTPHTrigger()
                            animateLoadingDots(linearLayout)
                            delay(300) // 1 second delay
                        }

                        val blokDeferred = async {
                            try {
                                val estateIdToUse = if (isGM) {
                                    estateId!!.toInt()
                                } else {
                                    estateId!!.toInt()
                                }
                                datasetViewModel.getListOfBlok(estateIdToUse, selectedDivisiId ?: 0)
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching blokList: ${e.message}")
                                emptyList()
                            }
                        }
                        blokList = blokDeferred.await()

                        try {
                            if (estateId == null || selectedDivisiId == null) {
                                throw IllegalStateException("Estate ID or selectedDivisiId is null!")
                            }

                            latLonMap = emptyMap()

                            val latLonResult = async {
                                try {
                                    val estateIdToUse = estateId!!.toInt()
                                    val resultMap = mutableMapOf<Int, ScannedTPHLocation>()

                                    AppLogger.d("Loading TPHs from database - normal flow")
                                    AppLogger.d("panenTPH size: ${panenTPH.size}")

                                    // 🚀 OPTIMIZED: Process in background thread with progress logging
                                    AppLogger.d("Starting groupBy operation for ${panenTPH.size} records...")

                                    val panenGroupedByTPH = panenTPH
                                        .asSequence() // Use sequence for lazy evaluation
                                        .mapNotNull { panenWithRelationship ->
                                            val tphId = panenWithRelationship.panen.tph_id?.toIntOrNull()
                                            if (tphId != null) tphId to panenWithRelationship else null
                                        }
                                        .groupBy({ it.first }, { it.second }) // Group by TPH ID

                                    AppLogger.d("GroupBy completed. Found ${panenGroupedByTPH.size} unique TPH groups")

                                    val selectedTPHIds = panenGroupedByTPH.keys.toList()
                                    AppLogger.d("Selected TPH IDs from panenTPH: ${selectedTPHIds.size} unique TPHs")

                                    // 🚀 OPTIMIZED: Batch the database query if needed
                                    AppLogger.d("Fetching TPH data from database...")
                                    val tphList = if (selectedTPHIds.size > 1000) {
                                        // If too many TPH IDs, process in batches
                                        val batchSize = 500
                                        val allResults = mutableListOf<TPHNewModel>()

                                        selectedTPHIds.chunked(batchSize)
                                            .forEachIndexed { index, batch ->
                                                AppLogger.d("Processing TPH batch ${index + 1}/${(selectedTPHIds.size + batchSize - 1) / batchSize}")
                                                val batchResults = datasetViewModel.getLatLonDivisiByTPHIds(
                                                    estateIdToUse,
                                                    selectedDivisiId,
                                                    batch
                                                )
                                                allResults.addAll(batchResults)
                                            }
                                        allResults
                                    } else {
                                        // Normal single query
                                        datasetViewModel.getLatLonDivisiByTPHIds(
                                            estateIdToUse,
                                            selectedDivisiId,
                                            selectedTPHIds
                                        )
                                    }

                                    data class BlokKey(
                                        val dept: String,
                                        val divisi: String,
                                        val kode: String
                                    )

                                    val blokLookupMap = blokList.associateBy { blok ->
                                        BlokKey(
                                            blok.dept?.toString() ?: "",
                                            blok.divisi?.toString() ?: "",
                                            blok.kode ?: ""
                                        )
                                    }

                                    tphList.forEach { tph ->
                                        val tphId = tph.id
                                        val lat = tph.lat?.toDoubleOrNull()
                                        val lon = tph.lon?.toDoubleOrNull()
                                        val nomor = tph.nomor ?: ""
                                        val baseBlokKode = tph.blok_kode ?: ""
                                        val divisiKode = tph.divisi ?: ""
                                        val deptKode = tph.dept ?: ""
                                        val jenisTPHId = tph.jenis_tph_id ?: "1"

                                        val blokKey = BlokKey(
                                            deptKode.toString(),
                                            divisiKode.toString(), baseBlokKode
                                        )
                                        val matchingBlok = blokLookupMap[blokKey]

                                        val jmlPokokHa = matchingBlok?.jml_pokok_ha

                                        if (tphId != null && lat != null && lon != null) {
                                            // Get all panen records for this TPH ID
                                            val matchingPanenList = panenGroupedByTPH[tphId] ?: emptyList()

                                            if (matchingPanenList.isNotEmpty()) {
                                                val mergedData = mergePanenRecordsForTPH(matchingPanenList)

                                                val blokKode = if (mergedData.dateList.size > 1) {
                                                    "$baseBlokKode (${mergedData.dateList.size} transaksi)"
                                                } else {
                                                    baseBlokKode
                                                }

                                                resultMap[tphId] = ScannedTPHLocation(
                                                    lat,
                                                    lon,
                                                    nomor,
                                                    blokKode,
                                                    divisiKode.toString(),
                                                    deptKode.toString(),
                                                    jmlPokokHa,
                                                    jenisTPHId
                                                )
                                            }
                                        }
                                    }

                                    AppLogger.d("Final resultMap size: ${resultMap.size}")
                                    resultMap

                                } catch (e: Exception) {
                                    AppLogger.e("Error in latLonResult: ${e.message}", e.toString())
                                    throw e
                                }
                            }

                            try {
                                latLonMap = latLonResult.await()
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

                            val kemandoranDeferred = async {
                                try {
                                    datasetViewModel.getKemandoranEstate(
                                        estateId!!.toInt()
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error fetching kemandoran list: ${e.message}")
                                    emptyList()
                                }
                            }

                            kemandoranList = kemandoranDeferred.await()

                            withContext(Dispatchers.Main) {
                                try {
                                    val kemandoranNames = kemandoranList.map { it.nama }
                                    setupSpinnerView(
                                        lyKemandoran,
                                        kemandoranNames as List<String>
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error updating UI: ${e.message}")
                                }
                            }

                        } catch (e: Exception) {
                            AppLogger.e("Error loading afdeling data: ${e.message}", e.toString())
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
            }

            R.id.lyJalurInspect -> {
                val mapData = listRadioItems["EntryPath"] ?: emptyMap()
                val selectedKey = mapData.entries.find { it.value == selectedItem }?.value


                selectedJalurMasuk = selectedKey ?: ""
                AppLogger.d("selectedJalurMasuk $selectedJalurMasuk")
            }

            R.id.lyKemandoran -> {
                selectedKemandoranId = try {
                    kemandoranList.find { it.nama == selectedItem }?.id!!
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedKemandoranId: ${e.message}")
                    0
                }
                Log.d("FormESPBActivityKemandoran", "selectedKemandoranId: $selectedKemandoranId")

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val karyawanDeferred = async {
                            datasetViewModel.getKaryawanList(selectedKemandoranId)
                        }
                        pemuatList = karyawanDeferred.await()

                        // IMPORTANT: Store the complete list here!
                        allPemuatEmployees = pemuatList.toList()

                        val pemuatNames = pemuatList.map { "${it.nik ?: "N/A"} - ${it.nama}" }


                        withContext(Dispatchers.Main) {
                            val rvSelectedPemanen =
                                findViewById<RecyclerView>(R.id.rvSelectedPemuatInspection)
                            rvSelectedPemanen.visibility = View.VISIBLE
                            setupSpinnerView(lyPemuat, pemuatNames)
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
                            // Any cleanup
                        }
                    }
                }
            }

            R.id.lyPemuat -> {
                val selectedPemuat = selectedItem.toString()
                AppLogger.d("Selected Pemuat: $selectedPemuat")

                // Extract NIK and Name from the selection (format: "NIK - Name")
                val firstDashIndex = selectedPemuat.indexOf(" - ")

                val selectedNik = if (firstDashIndex != -1) {
                    selectedPemuat.substring(0, firstDashIndex).trim()
                } else ""

                val selectedName =
                    if (firstDashIndex != -1 && firstDashIndex < selectedPemuat.length - 3) {
                        selectedPemuat.substring(firstDashIndex + 3).trim()
                    } else ""

                AppLogger.d("Extracted NIK: $selectedNik")
                AppLogger.d("Extracted Name: $selectedName")

                // Find the selected employee in pemuatList
                var selectedEmployee = pemuatList.firstOrNull {
                    it.nik == selectedNik || it.nama?.trim()
                        ?.equals(selectedName.trim(), ignoreCase = true) == true
                }

                // If not found by exact match, try partial match on name
                if (selectedEmployee == null && selectedName.isNotEmpty()) {
                    selectedEmployee = pemuatList.firstOrNull {
                        it.nama?.trim()?.equals(selectedName, ignoreCase = true) == true
                    }
                }

                // Fallback: try to find by NIK or name contains
                if (selectedEmployee == null) {
                    selectedEmployee = pemuatList.firstOrNull {
                        it.nik?.contains(selectedNik, ignoreCase = true) == true ||
                                it.nama?.contains(selectedName, ignoreCase = true) == true
                    }
                }

                if (selectedEmployee != null) {
                    // Create worker and add to adapter
                    val worker = Worker(selectedEmployee.id.toString(), selectedPemuat)

                    // IMPORTANT: Set all available workers first (using the stored complete list)
                    val allWorkers = allPemuatEmployees.map {
                        Worker(it.id.toString(), "${it.nik ?: "N/A"} - ${it.nama}")
                    }
                    selectedPemuatAdapter.setAvailableWorkers(allWorkers)

                    // Now add the selected worker
                    selectedPemuatAdapter.addWorker(worker)

                    // Remove the selected employee from pemuatList
                    pemuatList = pemuatList.filter { it.id != selectedEmployee.id }
                    AppLogger.d("Removed worker from pemuatList. Remaining count: ${pemuatList.size}")

                    // Update spinner with remaining workers
                    updatePemuatSpinnerAfterRemoval()

                    AppLogger.d("Selected Pemuat Worker: $selectedPemuat, ID: ${selectedEmployee.id}")
                    AppLogger.d("Remaining workers in spinner: ${pemuatList.size}")
                } else {
                    AppLogger.d("Error: Could not find pemuat worker with name $selectedName or NIK $selectedNik")
                    Toast.makeText(
                        this@FormInspectionActivity,
                        "Error: Worker not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private var isProcessingTPHSelection = false // Add this as a class variable

    override fun onTPHSelected(selectedTPHInLIst: ScannedTPHSelectionItem) {
        if (isProcessingTPHSelection) {
            AppLogger.d("TPH selection already in progress, ignoring...")
            return
        }

        if (photoInTPH != null) {
            AlertDialogUtility.withTwoActions(
                this@FormInspectionActivity,
                "Lanjutkan Hapus Foto",
                getString(R.string.confirmation_dialog_title),
                "Foto di TPH sudah terpilih, jika anda melanjutkan maka foto akan terhapus di TPH lama",
                "warning.json",
                ContextCompat.getColor(this@FormInspectionActivity, R.color.greendarkerbutton),
                function = {
                    photoInTPH = null
                    komentarInTPH = null
                    updatePhotoBadgeVisibility()
                },
                cancelFunction = {
                }
            )
        } else {
            isProcessingTPHSelection = true

            AppLogger.d("selectedTPHInLIst $selectedTPHInLIst")
            tvErrorScannedNotSelected.visibility = View.GONE

            //sph * 0.55 = total pages
            val calculatedPages = ceil((selectedTPHInLIst.jml_pokok_ha ?: 0) * 0.55).toInt()
            formAncakViewModel.updateTotalPages(calculatedPages)
            // Make title and description visible
            val titlePemanenInspeksi = findViewById<TextView>(R.id.titlePemanenInspeksi)
            val descPemanenInspeksi = findViewById<TextView>(R.id.descPemanenInspeksi)
            titlePemanenInspeksi.visibility = View.VISIBLE
            descPemanenInspeksi.visibility = View.VISIBLE

            val lyPemanenOtomatis = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)
            val lyPemanenManual = findViewById<LinearLayout>(R.id.lyPemanenManual)
            lyPemanenOtomatis.visibility = View.GONE
            lyPemanenManual.visibility = View.GONE

            lyKemandoran.visibility = View.GONE

            // Clear RecyclerView and maps FIRST
            selectedPemanenAdapter.clearAllWorkers()
            selectedPemanenManualAdapter.clearAllWorkers()
            selectedPemuatAdapter.clearAllWorkers()
            karyawanIdMap.clear()
            kemandoranIdMap.clear()

            // Clear the RecyclerView but DON'T recreate the adapter
            val rvSelectedPemanenOtomatis =
                findViewById<RecyclerView>(R.id.rvSelectedPemanenOtomatisInspection)
            val rvSelectedPemanenManual =
                findViewById<RecyclerView>(R.id.rvSelectedPemanenManualInspection)
            val rvSelectedPemuat =
                findViewById<RecyclerView>(R.id.rvSelectedPemuatInspection)
            rvSelectedPemanenOtomatis.visibility = View.GONE
            rvSelectedPemanenManual.visibility = View.GONE


            allAvailableKaryawanList = emptyList()
            allManualKaryawanList = emptyList()

            // Re-setup the callbacks for both adapters
            setupAdapterCallbacks()
            setupManualAdapterCallbacks()

            selectedTPHNomorByScan = selectedTPHInLIst.number.toInt()
            selectedKaryawanList = emptyList()

            selectedTPHNomorByScan = selectedTPHInLIst.number.toInt()
            selectedKaryawanList = emptyList()

            lyKemandoran.visibility = View.VISIBLE
            lyPemuat.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val matchingPanenList = panenTPH.filter { panenWithRelations ->
                        val tphId = panenWithRelations.panen?.tph_id?.toIntOrNull()
                        tphId == selectedTPHInLIst.id
                    }

                    if (matchingPanenList.isEmpty()) {
                        AppLogger.e("No matching panen records found for TPH ID: ${selectedTPHInLIst.id}")
                        return@postDelayed
                    }

                    // Merge all panen records for this TPH
                    val mergedData = mergePanenRecordsForTPH(matchingPanenList)

                    AppLogger.d("Merged data for TPH ${selectedTPHInLIst.id}:")
                    AppLogger.d("- Workers: ${mergedData.workerCount}")
                    AppLogger.d("- Dates: ${mergedData.dateList}")
                    AppLogger.d("- Ancaks: ${mergedData.ancakList}")

                    // Get info from first record for basic data
                    val firstPanen = matchingPanenList.first().panen
                    val firstTph = matchingPanenList.first().tph

                    selectedTPHIdByScan = selectedTPHInLIst.id
                    selectedEstateByScan = firstTph?.dept_abbr ?: ""
                    selectedAfdelingByScan = firstTph?.divisi_abbr ?: ""
                    selectedBlokByScan = firstTph?.blok_kode ?: ""

                    // Store all available workers for spinner
                    val allAvailableWorkers = mutableListOf<Worker>()
                    val allKaryawanInfo = mutableListOf<KaryawanInfo>()

                    // Process merged workers and prepare them for spinner
                    mergedData.workerSet.forEach { formattedWorker ->
                        val dashIndex = formattedWorker.indexOf(" - ")
                        val selectedNik = if (dashIndex != -1) {
                            formattedWorker.substring(0, dashIndex).trim()
                        } else {
                            ""
                        }
                        val selectedName = if (dashIndex != -1) {
                            formattedWorker.substring(dashIndex + 3).trim()
                        } else {
                            formattedWorker.trim()
                        }

                        // Find the corresponding employee data from any of the panen records
                        var selectedEmployee: PanenEntity? = null
                        var individualKaryawanId: String? = null

                        for (panenWithRelations in matchingPanenList) {
                            val panenEntity = panenWithRelations.panen ?: continue
                            val karyawanNik = panenEntity.karyawan_nik
                            val karyawanNama = panenEntity.karyawan_nama
                            val karyawanId = panenEntity.karyawan_id

                            if (!karyawanNik.isNullOrBlank() && !karyawanNama.isNullOrBlank() && !karyawanId.isNullOrBlank()) {
                                val niks = karyawanNik.split(",").map { it.trim() }
                                val names = karyawanNama.split(",").map { it.trim() }
                                val ids = karyawanId.split(",").map { it.trim() }

                                val nikIndex = niks.indexOf(selectedNik)
                                val nameIndex = names.indexOf(selectedName)

                                if (nikIndex != -1 && nameIndex != -1 && nikIndex < ids.size) {
                                    selectedEmployee = panenEntity
                                    individualKaryawanId = ids[nikIndex]
                                    break
                                }
                            }
                        }

                        if (selectedEmployee != null && individualKaryawanId != null) {
                            karyawanIdMap[formattedWorker] = individualKaryawanId.toIntOrNull() ?: 0
                            kemandoranIdMap[formattedWorker] =
                                selectedEmployee.kemandoran_id.toIntOrNull() ?: 0

                            if (selectedNik.isNotEmpty()) {
                                karyawanIdMap[selectedNik] = individualKaryawanId.toIntOrNull() ?: 0
                                kemandoranIdMap[selectedNik] =
                                    selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
                            }
                            if (selectedName.isNotEmpty()) {
                                karyawanIdMap[selectedName] =
                                    individualKaryawanId.toIntOrNull() ?: 0
                                kemandoranIdMap[selectedName] =
                                    selectedEmployee.kemandoran_id.toIntOrNull() ?: 0
                            }

                            val worker = Worker(individualKaryawanId, formattedWorker)
                            allAvailableWorkers.add(worker)

                            allKaryawanInfo.add(
                                KaryawanInfo(
                                    nik = selectedNik,
                                    nama = selectedName,
                                    individualId = individualKaryawanId
                                )
                            )

                            AppLogger.d("Added worker to spinner: $formattedWorker, ID: $individualKaryawanId")
                        }
                    }

                    // Store all available workers
                    allAvailableKaryawanList = allKaryawanInfo

                    // Create description
                    val ancakText = if (mergedData.ancakList.isNotEmpty()) {
                        mergedData.ancakList.joinToString(", ")
                    } else {
                        "tidak diketahui"
                    }

                    val dateText = if (mergedData.dateList.isNotEmpty()) {
                        if (mergedData.dateList.size == 1) {
                            mergedData.dateList.first()
                        } else {
                            "${mergedData.dateList.joinToString(", ")} (${mergedData.dateList.size} transaksi)"
                        }
                    } else {
                        "tidak diketahui"
                    }

                    val panenIdsJson = JSONArray()
                    matchingPanenList.forEach { panenWithRelations ->
                        panenWithRelations.panen?.id?.let { panenId ->
                            panenIdsJson.put(panenId)
                        }
                    }

                    selectedIdPanenByScan = panenIdsJson.toString()
                    selectedAncakByScan = ancakText
                    val datesJson = JSONArray()
                    mergedData.dateList.forEach { date ->
                        datesJson.put(date)
                    }
                    selectedTanggalPanenByScan = datesJson.toString()

                    val today = LocalDate.now()

                    val formattedDatesWithH = mergedData.dateList.mapNotNull { dateStr ->
                        try {
                            val formatter = DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            )
                            val date = LocalDateTime.parse(dateStr, formatter).toLocalDate()
                            val hPlus = ChronoUnit.DAYS.between(date, today)
                            "$dateStr (H + $hPlus)"
                        } catch (e: Exception) {
                            AppLogger.e("Date parsing failed for $dateStr: ${e.message}")
                            null
                        }
                    }

                    val finalDateText = if (formattedDatesWithH.size == 1) {
                        formattedDatesWithH.first()
                    } else {
                        formattedDatesWithH.joinToString(", ")
                    }

                    val descriptionText = if (mergedData.dateList.size > 1) {
                        "Panen sudah dilakukan ancak <b>$ancakText</b> pada <b>$finalDateText</b> oleh total <b>${mergedData.workerCount} pekerja</b>. Pilih pekerja untuk inspeksi:"
                    } else {
                        "Panen sudah dilakukan ancak <b>$ancakText</b> pada <b>$finalDateText</b>. Pilih pekerja untuk inspeksi:"
                    }

                    descPemanenInspeksi.text =
                        Html.fromHtml(descriptionText, Html.FROM_HTML_MODE_COMPACT)

                    // NOW POPULATE THE SPINNER with all available workers
                    populatePemanenSpinner(allAvailableWorkers)
                    setupManualPemanenSpinner()

                    AppLogger.d("Total workers available in spinner: ${allAvailableWorkers.size}")

                } catch (e: Exception) {
                    AppLogger.e("Error processing merged TPH selection: ${e.message}")
                } finally {
                    isProcessingTPHSelection = false
                }
            }, 200)
        }
    }

    private fun populateManualPemanenSpinner(availableWorkers: List<Worker>) {
        val lyPemanenManual = findViewById<LinearLayout>(R.id.lyPemanenManual)

        // Always show spinner if there are workers available
        if (availableWorkers.isNotEmpty()) {
            lyPemanenManual.visibility = View.VISIBLE

            // Sort workers by name alphabetically (extract name part after first " - ")
            val sortedWorkers = availableWorkers.sortedBy { worker ->
                val dashIndex = worker.name.indexOf(" - ")
                if (dashIndex != -1) {
                    worker.name.substring(dashIndex + 3).trim() // Sort by name part
                } else {
                    worker.name // Fallback to full string
                }
            }

            // Create list of worker names for spinner
            val workerNames = sortedWorkers.map { it.name }

            // Setup spinner with available workers
            setupSpinnerView(lyPemanenManual, workerNames)

            AppLogger.d("Manual spinner populated with ${workerNames.size} workers (sorted alphabetically)")
        } else {
            lyPemanenManual.visibility = View.GONE
            AppLogger.d("No manual workers available for spinner")
        }
    }


    private fun setupManualPemanenSpinner() {
        AppLogger.d("Setting up manual pemanen spinner for blok: $selectedBlokByScan")

        // Get all unique workers from all panen records that match the selected blok_kode
        val matchingPanenByBlok = panenTPH.filter { panenWithRelations ->
            val blokKode = panenWithRelations.tph?.blok_kode
            blokKode == selectedBlokByScan
        }

        AppLogger.d("Found ${matchingPanenByBlok.size} panen records matching blok: $selectedBlokByScan")

        if (matchingPanenByBlok.isEmpty()) {
            AppLogger.d("No panen records found for blok: $selectedBlokByScan")
            return
        }

        // Extract all unique workers from matching panen records
        val allManualWorkers = mutableListOf<Worker>()
        val allManualKaryawanInfo = mutableListOf<KaryawanInfo>()
        val uniqueWorkerSet = mutableSetOf<String>() // To track unique nik-nama combinations

        matchingPanenByBlok.forEach { panenWithRelations ->
            val panenEntity = panenWithRelations.panen
            val karyawanNik = panenEntity.karyawan_nik
            val karyawanNama = panenEntity.karyawan_nama
            val karyawanId = panenEntity.karyawan_id
            val kemandoranId = panenEntity.kemandoran_id

            if (!karyawanNik.isNullOrBlank() && !karyawanNama.isNullOrBlank() && !karyawanId.isNullOrBlank()) {
                // Split by comma and process each worker
                val niks = karyawanNik.split(",").map { it.trim() }
                val names = karyawanNama.split(",").map { it.trim() }
                val ids = karyawanId.split(",").map { it.trim() }

                // Process each worker combination
                for (i in niks.indices) {
                    if (i < names.size && i < ids.size) {
                        val nik = niks[i]
                        val nama = names[i]
                        val individualId = ids[i]

                        // Create unique key for this worker
                        val uniqueKey = "$nik-$nama"

                        if (!uniqueWorkerSet.contains(uniqueKey) && nik.isNotEmpty() && nama.isNotEmpty()) {
                            uniqueWorkerSet.add(uniqueKey)

                            val formattedName = "$nik - $nama"
                            val worker = Worker(individualId, formattedName)
                            allManualWorkers.add(worker)

                            // Add to maps for quick lookup
                            karyawanIdMap[formattedName] = individualId.toIntOrNull() ?: 0
                            kemandoranIdMap[formattedName] = kemandoranId.toIntOrNull() ?: 0
                            karyawanIdMap[nik] = individualId.toIntOrNull() ?: 0
                            kemandoranIdMap[nik] = kemandoranId.toIntOrNull() ?: 0
                            karyawanIdMap[nama] = individualId.toIntOrNull() ?: 0
                            kemandoranIdMap[nama] = kemandoranId.toIntOrNull() ?: 0

                            allManualKaryawanInfo.add(
                                KaryawanInfo(
                                    nik = nik,
                                    nama = nama,
                                    individualId = individualId
                                )
                            )

                            AppLogger.d("Added manual worker: $formattedName, ID: $individualId")
                        }
                    }
                }
            }
        }

        // Store all available manual workers
        allManualKaryawanList = allManualKaryawanInfo

        // Populate the manual spinner
        populateManualPemanenSpinner(allManualWorkers)

        AppLogger.d("Total unique manual workers found: ${allManualWorkers.size}")
    }

    private fun populatePemanenSpinner(availableWorkers: List<Worker>) {
        val lyPemanen = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)

        // Always show spinner once TPH is selected, regardless of worker count
        lyPemanen.visibility = View.VISIBLE

        // Sort workers by name alphabetically (extract name part after first " - ")
        val sortedWorkers = availableWorkers.sortedBy { worker ->
            val dashIndex = worker.name.indexOf(" - ")
            if (dashIndex != -1) {
                worker.name.substring(dashIndex + 3).trim() // Sort by name part
            } else {
                worker.name // Fallback to full string
            }
        }

        // Create list of worker names for spinner
        val workerNames = sortedWorkers.map { it.name }

        // Setup spinner with available workers (even if empty)
        setupSpinnerView(lyPemanen, workerNames)

        if (availableWorkers.isEmpty()) {
            AppLogger.d("All workers have been selected - spinner shows empty")
        } else {
            AppLogger.d("Spinner populated with ${workerNames.size} workers (sorted alphabetically)")
        }
    }

    // New method to update spinner after worker removal
    private fun updatePemanenSpinnerAfterRemoval() {
        AppLogger.d("updatePemanenSpinnerAfterRemoval called")

        // Get currently selected workers from AUTOMATIC RecyclerView
        val selectedWorkers = selectedPemanenAdapter.getSelectedWorkers()
        val selectedWorkerIds = selectedWorkers.map { it.id }.toSet()

        AppLogger.d("Currently selected automatic worker IDs: $selectedWorkerIds")
        AppLogger.d("All available automatic workers count: ${allAvailableKaryawanList.size}")

        // Filter available workers to exclude already selected ones
        val availableWorkers = allAvailableKaryawanList
            .filter { karyawan ->
                val isSelected = selectedWorkerIds.contains(karyawan.individualId)
                AppLogger.d("Automatic  Worker ${karyawan.nama} (ID: ${karyawan.individualId}) - Selected: $isSelected")
                !isSelected
            }
            .map { karyawan ->
                val formattedName = "${karyawan.nik} - ${karyawan.nama}"
                Worker(karyawan.individualId, formattedName)
            }

        AppLogger.d("Available automatic workers for spinner: ${availableWorkers.size}")

        // Always keep spinner visible and populate it
        val lyPemanen = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)
        lyPemanen.visibility = View.VISIBLE

        val workerNames = availableWorkers.map { it.name }

        // Setup spinner with available workers
        setupSpinnerView(lyPemanen, workerNames)

        // If empty, set the spinner to show hint and disable selection
        val spinner = lyPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        spinner.setHint("Pilih Pemanen")
//        spinner.setSelectedIndex(-1) // Clear any selected item

        AppLogger.d("Automatic spinner updated with ${workerNames.size} available workers")
    }

    private fun updateManualPemanenSpinnerAfterRemoval() {
        AppLogger.d("updateManualPemanenSpinnerAfterRemoval called")

        // Get currently selected workers from MANUAL RecyclerView
        val selectedWorkers = selectedPemanenManualAdapter.getSelectedWorkers()
        val selectedWorkerIds = selectedWorkers.map { it.id }.toSet()

        AppLogger.d("Currently selected manual worker IDs: $selectedWorkerIds")
        AppLogger.d("All available manual workers count: ${allManualKaryawanList.size}")

        // Filter available workers to exclude already selected ones
        val availableWorkers = allManualKaryawanList
            .filter { karyawan ->
                val isSelected = selectedWorkerIds.contains(karyawan.individualId)
                AppLogger.d("Manual Worker ${karyawan.nama} (ID: ${karyawan.individualId}) - Selected: $isSelected")
                !isSelected
            }
            .map { karyawan ->
                val formattedName = "${karyawan.nik} - ${karyawan.nama}"
                Worker(karyawan.individualId, formattedName)
            }

        AppLogger.d("Available manual workers for spinner: ${availableWorkers.size}")

        // Always keep spinner visible and populate it (same as automatic spinner)
        val lyPemanenManual = findViewById<LinearLayout>(R.id.lyPemanenManual)
        lyPemanenManual.visibility = View.VISIBLE  // Always visible!

        // Create list of worker names for spinner
        val workerNames = availableWorkers.map { it.name }


        // Setup spinner with available workers (could be empty list)
        setupSpinnerView(lyPemanenManual, workerNames)

        val spinner = lyPemanenManual.findViewById<MaterialSpinner>(R.id.spPanenTBS)


        spinner.setHint("Pilih Pemanen")


        AppLogger.d("Manual spinner updated with ${workerNames.size} available workers")
    }

    private fun updateWorkerDataInViewModel() {
        // Get current selected workers
        val automaticWorkers = selectedPemanenAdapter.getSelectedWorkers()
        val manualWorkers = selectedPemanenManualAdapter.getSelectedWorkers()

        // Combine both lists for pemanen selection
        val allWorkerNames = mutableListOf<String>()

        // Add automatic workers
        automaticWorkers.forEach { worker ->
            allWorkerNames.add(worker.name)
        }

        // Add manual workers
        manualWorkers.forEach { worker ->
            allWorkerNames.add(worker.name)
        }

        AppLogger.d("Updating ViewModel with workers: Total=${allWorkerNames.size}")
        allWorkerNames.forEach { worker ->
            AppLogger.d("  - Updating worker: $worker")
        }

        // Update ViewModel - this will notify all fragments
        formAncakViewModel.updateAvailableWorkers(allWorkerNames)
    }


    override fun getCurrentlySelectedTPHId(): Int? {
        return selectedTPHIdByScan
    }

    private fun checkScannedTPHInsideRadius() {
        AppLogger.d("Starting checkScannedTPHInsideRadius")
        try {
            // Hide progress bars first
            if (progressBarScanTPHManual.visibility == View.VISIBLE) {
                progressBarScanTPHManual.visibility = View.GONE
            }
            if (progressBarScanTPHAuto.visibility == View.VISIBLE) {
                progressBarScanTPHAuto.visibility = View.GONE
            }

            // Validate coordinates
            if (lat == null || lon == null) {
                AppLogger.e("Coordinates are null - lat: $lat, lon: $lon")
                Toasty.error(this, "Pastikan GPS mendapatkan titik Koordinat!", Toast.LENGTH_LONG, true).show()
                isEmptyScannedTPH = true
                return
            }

            // Validate finite coordinates
            if (!lat!!.isFinite() || !lon!!.isFinite()) {
                AppLogger.e("Invalid coordinates - lat: $lat, lon: $lon")
                Toasty.error(this, "Koordinat GPS tidak valid!", Toast.LENGTH_LONG, true).show()
                isEmptyScannedTPH = true
                return
            }

            AppLogger.d("Using coordinates - lat: $lat, lon: $lon")
            AppLogger.d("latLonMap size: ${latLonMap.size}")

            // Validate latLonMap
            if (latLonMap.isEmpty()) {
                AppLogger.w("latLonMap is empty")
                showEmptyTPHState()
                return
            }

            val tphList = try {
                getTPHsInsideRadius(lat!!, lon!!, latLonMap)
            } catch (e: Exception) {
                AppLogger.e("Error calling getTPHsInsideRadius: ${e.message}")
                AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")
                showErrorState("Error mencari TPH dalam radius: ${e.message}")
                return
            }

            AppLogger.d("getTPHsInsideRadius returned ${tphList.size} items")

            if (tphList.isNotEmpty()) {
                try {
                    showTPHResults(tphList)
                } catch (e: Exception) {
                    AppLogger.e("Error showing TPH results: ${e.message}")
                    showErrorState("Error menampilkan hasil TPH: ${e.message}")
                }
            } else {
                AppLogger.d("No TPHs found within radius")
                showEmptyTPHState()
            }

        } catch (e: Exception) {
            AppLogger.e("Critical error in checkScannedTPHInsideRadius: ${e.message}")
            AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")
            showErrorState("Error sistem: ${e.message}")
        }
    }

    private fun showTPHResults(tphList: List<ScannedTPHSelectionItem>) {
        try {
            isEmptyScannedTPH = false
            tphScannedResultRecyclerView.visibility = View.VISIBLE
            titleScannedTPHInsideRadius.visibility = View.VISIBLE
            descScannedTPHInsideRadius.visibility = View.VISIBLE
            emptyScannedTPHInsideRadius.visibility = View.GONE

            // Create adapter with error handling
            try {
                tphScannedResultRecyclerView.adapter = ListTPHInsideRadiusAdapter(
                    tphList,
                    this,
                    jenisTPHListGlobal,
                    false
                )
                AppLogger.d("Adapter set successfully")
            } catch (e: Exception) {
                AppLogger.e("Error setting adapter: ${e.message}")
                throw e
            }

            // Calculate and set height
            try {
                val itemHeight = 50
                val maxHeight = 250
                val density = tphScannedResultRecyclerView.resources.displayMetrics.density
                val maxHeightPx = (maxHeight * density).toInt()
                val recyclerViewHeightPx = (tphList.size * itemHeight * density).toInt()

                tphScannedResultRecyclerView.layoutParams.height =
                    if (recyclerViewHeightPx > maxHeightPx) maxHeightPx else ViewGroup.LayoutParams.WRAP_CONTENT

                tphScannedResultRecyclerView.requestLayout()

                // Set scroll behavior
                if (recyclerViewHeightPx > maxHeightPx) {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = true
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_ALWAYS
                } else {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = false
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
                }

                tphScannedResultRecyclerView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                tphScannedResultRecyclerView.isVerticalScrollBarEnabled = true

                AppLogger.d("RecyclerView configured successfully")
            } catch (e: Exception) {
                AppLogger.e("Error configuring RecyclerView: ${e.message}")
                // Don't throw here, the adapter is already set
            }

            // Handle selected TPH scrolling
            try {
                if (selectedTPHIdByScan != null) {
                    for (i in tphList.indices) {
                        if (tphList[i].id == selectedTPHIdByScan) {
                            tphScannedResultRecyclerView.scrollToPosition(i)
                            AppLogger.d("Scrolled to selected TPH at position $i")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error scrolling to selected TPH: ${e.message}")
                // Don't throw here, results are already shown
            }

        } catch (e: Exception) {
            AppLogger.e("Error in showTPHResults: ${e.message}")
            throw e
        }
    }

    private fun showEmptyTPHState() {
        try {
            tphScannedResultRecyclerView.visibility = View.GONE
            titleScannedTPHInsideRadius.visibility = View.VISIBLE
            descScannedTPHInsideRadius.visibility = View.VISIBLE
            emptyScannedTPHInsideRadius.visibility = View.VISIBLE
            isEmptyScannedTPH = true
            AppLogger.d("Empty TPH state displayed")
        } catch (e: Exception) {
            AppLogger.e("Error showing empty TPH state: ${e.message}")
        }
    }

    private fun showErrorState(message: String) {
        try {
            tphScannedResultRecyclerView.visibility = View.GONE
            titleScannedTPHInsideRadius.visibility = View.VISIBLE
            descScannedTPHInsideRadius.visibility = View.VISIBLE
            emptyScannedTPHInsideRadius.visibility = View.VISIBLE
            isEmptyScannedTPH = true

            Toasty.error(this, message, Toast.LENGTH_LONG, true).show()
            AppLogger.d("Error state displayed with message: $message")
        } catch (e: Exception) {
            AppLogger.e("Error showing error state: ${e.message}")
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
        AppLogger.d("Starting getTPHsInsideRadius - userLat: $userLat, userLon: $userLon")
        AppLogger.d("Coordinates map size: ${coordinates.size}")

        val resultsList = mutableListOf<ScannedTPHSelectionItem>()
        val regularTPHs = mutableListOf<ScannedTPHSelectionItem>()

        try {
            // Validate input parameters
            if (!userLat.isFinite() || !userLon.isFinite()) {
                AppLogger.e("Invalid user coordinates - userLat: $userLat, userLon: $userLon")
                return emptyList()
            }

            if (coordinates.isEmpty()) {
                AppLogger.w("Coordinates map is empty")
                return emptyList()
            }

            AppLogger.d("Processing ${coordinates.size} TPH coordinates")

            for ((id, location) in coordinates) {
                try {
                    AppLogger.d("Processing TPH ID: $id")

                    // Validate location object
                    if (location.lat == null || location.lon == null) {
                        AppLogger.w("TPH ID $id has null coordinates - lat: ${location.lat}, lon: ${location.lon}")
                        continue
                    }

                    // Validate coordinates are finite numbers
                    if (!location.lat.isFinite() || !location.lon.isFinite()) {
                        AppLogger.w("TPH ID $id has invalid coordinates - lat: ${location.lat}, lon: ${location.lon}")
                        continue
                    }

                    // Validate jenisTPHId
                    val jenisTPHId = try {
                        location.jenisTPHId.toInt()
                    } catch (e: NumberFormatException) {
                        AppLogger.e("Invalid jenisTPHId for TPH $id: ${location.jenisTPHId} - ${e.message}")
                        1 // Default value
                    }

                    AppLogger.d("TPH $id - jenisTPHId: $jenisTPHId, lat: ${location.lat}, lon: ${location.lon}")

                    // Calculate distance
                    val results = FloatArray(1)
                    try {
                        android.location.Location.distanceBetween(
                            userLat,
                            userLon,
                            location.lat,
                            location.lon,
                            results
                        )
                    } catch (e: Exception) {
                        AppLogger.e("Error calculating distance for TPH $id: ${e.message}")
                        continue
                    }

                    val distance = results[0]
                    AppLogger.d("TPH $id - calculated distance: $distance, radiusMinimum: $radiusMinimum")

                    // Validate distance result
                    if (!distance.isFinite() || distance < 0) {
                        AppLogger.w("Invalid distance calculated for TPH $id: $distance")
                        continue
                    }

                    // Get clean block code
                    val cleanBlockCode = try {
                        location.blokKode ?: ""
                    } catch (e: Exception) {
                        AppLogger.e("Error getting block code for TPH $id: ${e.message}")
                        ""
                    }

                    // Check if TPH is within radius
                    if (distance <= radiusMinimum) {
                        try {
                            // Validate jmlPokokHa
                            val jmlPokokHa = try {
                                location.jmlPokokHa ?: 0
                            } catch (e: Exception) {
                                AppLogger.e("Error getting jmlPokokHa for TPH $id: ${e.message}")
                                0
                            }

                            val tphItem = ScannedTPHSelectionItem(
                                id = id,
                                number = location.nomor ?: "",
                                blockCode = cleanBlockCode,
                                divisiCode = location.divisiKode ?: "",
                                deptCode = location.deptKode ?: "",
                                distance = distance,
                                jml_pokok_ha = jmlPokokHa,
                                isAlreadySelected = false,
                                selectionCount = 0,
                                canBeSelectedAgain = true,
                                isWithinRange = distance <= radiusMinimum,
                                jenisTPHId = jenisTPHId.toString(),
                                customLimit = "0"
                            )

                            regularTPHs.add(tphItem)
                            AppLogger.d("Successfully added regular TPH: $id with distance: $distance")

                        } catch (e: Exception) {
                            AppLogger.e("Error creating ScannedTPHSelectionItem for TPH $id: ${e.message}")
                            AppLogger.e("Location data - nomor: ${location.nomor}, divisiKode: ${location.divisiKode}, deptKode: ${location.deptKode}")
                            continue
                        }
                    } else {
                        AppLogger.d("TPH $id outside radius - distance: $distance > $radiusMinimum")
                    }

                } catch (e: Exception) {
                    AppLogger.e("Error processing TPH $id: ${e.message}")
                    AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")
                    continue
                }
            }

            AppLogger.d("Processed all TPHs. Regular TPHs found: ${regularTPHs.size}")

            // Sort regular TPHs by distance
            try {
                regularTPHs.sortBy { it.distance }
                AppLogger.d("Successfully sorted TPHs by distance")
            } catch (e: Exception) {
                AppLogger.e("Error sorting TPHs: ${e.message}")
            }

            // Add regular TPHs (only if scan button was triggered)
            if (isTriggeredBtnScanned) {
                resultsList.addAll(regularTPHs)
                AppLogger.d("Added ${regularTPHs.size} TPHs to results (scan button triggered)")
            } else {
                AppLogger.d("Scan button not triggered, not adding TPHs to results")
            }

        } catch (e: Exception) {
            AppLogger.e("Critical error in getTPHsInsideRadius: ${e.message}")
            AppLogger.e("Exception stack trace: ${e.stackTraceToString()}")
            return emptyList()
        }

        AppLogger.d("getTPHsInsideRadius completed. Returning ${resultsList.size} items")
        return resultsList
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

    @SuppressLint("SetTextI18n")
    private fun populateFollowUpInspectionFirstFormUI(inspectionData: InspectionWithDetailRelations) {
        val inspection = inspectionData.inspeksi
        val tph = inspectionData.tph
        val panen = inspectionData.panen
        val detailInspeksi = inspectionData.detailInspeksi

        // Est/Afd/Blok
        findViewById<TextView>(R.id.tvEstAfdBlok).text =
            "${tph?.dept_abbr ?: "-"}/${tph?.divisi_abbr ?: "-"}/${tph?.blok_kode ?: "-"}\nTPH nomor ${tph?.nomor ?: "-"}"

        findViewById<TextView>(R.id.tvTanggalInspeksi).text =
            AppUtils.formatToIndonesianDate(inspection.created_date)

        findViewById<TextView>(R.id.tvJalurMasuk).text =
            inspection.jalur_masuk ?: "-"

        val jenisKondisiText = when (inspection.jenis_kondisi) {
            1 -> "Datar"
            2 -> "Terasan"
            else -> inspection.jenis_kondisi?.toString() ?: "-"
        }
        findViewById<TextView>(R.id.tvBaris).text =
            "($jenisKondisiText) ${inspection.baris ?: "-"}"

        findViewById<TextView>(R.id.tvTglPanen).text = try {
            val datePanenJson = inspection.date_panen ?: "-"

            if (datePanenJson == "-") {
                "-"
            } else {
                val datesJson = JSONArray(datePanenJson)
                when {
                    datesJson.length() == 0 -> "-"
                    datesJson.length() == 1 -> {
                        val singleDate = datesJson.getString(0)
                        AppUtils.formatToIndonesianDate(singleDate)
                    }

                    else -> {
                        val datesList = mutableListOf<String>()
                        for (i in 0 until datesJson.length()) {
                            val dateStr = datesJson.getString(i)
                            val formattedDate = AppUtils.formatToIndonesianDate(dateStr)
                            datesList.add("- $formattedDate")
                        }
                        val joinedDates = datesList.joinToString("\n")
                        "Total ${datesJson.length()} Transaksi\n$joinedDates"
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: treat as single date string for backward compatibility
            inspection.date_panen?.let { AppUtils.formatToIndonesianDate(it) } ?: "-"
        }

        val tphKomentar = detailInspeksi
            .filter { it.no_pokok == 0 }
            .firstOrNull()
            ?.komentar

        findViewById<TextView>(R.id.tvKomentarTPH).text = tphKomentar

    }

    private fun getInspectionValueByKode(
        detailInspeksi: List<InspectionDetailModel>,
        kodeInspeksi: Int
    ): Int {
        return detailInspeksi
            .firstOrNull { it.kode_inspeksi == kodeInspeksi }
            ?.temuan_inspeksi?.toInt() ?: 0
    }

    private fun setupCountersFromInspectionData(detailInspeksi: List<InspectionDetailModel>) {
        // Get values from inspection details
        val brondolanValue =
            getInspectionValueByKode(detailInspeksi, 6) // kode_inspeksi 6 for brondolan
        val buahValue = getInspectionValueByKode(detailInspeksi, 5) // kode_inspeksi 5 for buah

        // Update the global counter variables
        jumBrdTglPath = brondolanValue
        jumBuahTglPath = buahValue

        // Setup the counter UI components
        val counterMappings = listOf(
            Triple(
                R.id.lyBrdTglInspect,
                AppUtils.kodeInspeksi.brondolanTinggalTPH,
                ::jumBrdTglPath
            ),
            Triple(R.id.lyBuahTglInspect, AppUtils.kodeInspeksi.buahTinggalTPH, ::jumBuahTglPath),
        )

        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPanenWithButtons(layoutId, labelText, counterVar)
        }

        if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            val lyBuahTglInspect = findViewById<View>(R.id.lyBuahTglInspect)
            val layoutParams = lyBuahTglInspect.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin =
                (50 * resources.displayMetrics.density).toInt() // Convert dp to px
            lyBuahTglInspect.layoutParams = layoutParams
        }
    }

    private fun setupAdapterCallbacks() {
        AppLogger.d("Setting up automatic adapter callbacks...")

        selectedPemanenAdapter.setOnWorkerActuallyRemovedListener { removedWorker ->
            AppLogger.d("Automatic worker removal callback triggered for: ${removedWorker.name}")

            // Remove from selectedKaryawanList
            val dashIndex = removedWorker.name.indexOf(" - ")
            val removedNik = if (dashIndex != -1) {
                removedWorker.name.substring(0, dashIndex).trim()
            } else {
                ""
            }
            val removedName = if (dashIndex != -1) {
                removedWorker.name.substring(dashIndex + 3).trim()
            } else {
                removedWorker.name.trim()
            }

            // Update selectedKaryawanList by removing the worker
            val originalSize = selectedKaryawanList.size
            selectedKaryawanList = selectedKaryawanList.filter { karyawan ->
                !(karyawan.nik == removedNik && karyawan.nama == removedName)
            }

            AppLogger.d("selectedKaryawanList size changed from $originalSize to ${selectedKaryawanList.size}")

            // Remove from maps
            karyawanIdMap.remove(removedWorker.name)
            kemandoranIdMap.remove(removedWorker.name)
            karyawanIdMap.remove(removedNik)
            kemandoranIdMap.remove(removedNik)
            karyawanIdMap.remove(removedName)
            kemandoranIdMap.remove(removedName)

            // Update automatic spinner to show the removed worker again
            AppLogger.d("Calling updatePemanenSpinnerAfterRemoval...")
            updatePemanenSpinnerAfterRemoval()

            AppLogger.d("Removed automatic worker from selectedKaryawanList: $removedName (NIK: $removedNik)")
            AppLogger.d("Updated selectedKaryawanList size: ${selectedKaryawanList.size}")
        }
    }

    private fun setupSelectedPemanenRecyclerView() {
        val rvSelectedPemanen = findViewById<RecyclerView>(R.id.rvSelectedPemanenOtomatisInspection)
        selectedPemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = selectedPemanenAdapter
        rvSelectedPemanen.layoutManager = LinearLayoutManager(this)

        // Setup callbacks for automatic adapter
        setupAdapterCallbacks()

        rvSelectedPemanen.visibility = View.GONE
    }

    private fun setupSelectedPemanenManualRecyclerView() {
        val rvSelectedPemanenManual =
            findViewById<RecyclerView>(R.id.rvSelectedPemanenManualInspection)
        selectedPemanenManualAdapter = SelectedWorkerAdapter()
        rvSelectedPemanenManual.adapter = selectedPemanenManualAdapter
        rvSelectedPemanenManual.layoutManager = FlexboxLayoutManager(this).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Setup callbacks for manual adapter
        setupManualAdapterCallbacks()

        rvSelectedPemanenManual.visibility = View.GONE
    }

    private fun setupManualAdapterCallbacks() {
        AppLogger.d("Setting up manual adapter callbacks...")

        selectedPemanenManualAdapter.setOnWorkerActuallyRemovedListener { removedWorker ->
            AppLogger.d("Manual worker removal callback triggered for: ${removedWorker.name}")

            // Remove from selectedKaryawanList
            val dashIndex = removedWorker.name.indexOf(" - ")
            val removedNik = if (dashIndex != -1) {
                removedWorker.name.substring(0, dashIndex).trim()
            } else {
                ""
            }
            val removedName = if (dashIndex != -1) {
                removedWorker.name.substring(dashIndex + 3).trim()
            } else {
                removedWorker.name.trim()
            }

            // Update selectedKaryawanList by removing the worker
            val originalSize = selectedKaryawanList.size
            selectedKaryawanList = selectedKaryawanList.filter { karyawan ->
                !(karyawan.nik == removedNik && karyawan.nama == removedName)
            }

            AppLogger.d("Manual selectedKaryawanList size changed from $originalSize to ${selectedKaryawanList.size}")

            // Remove from maps
            karyawanIdMap.remove(removedWorker.name)
            kemandoranIdMap.remove(removedWorker.name)
            karyawanIdMap.remove(removedNik)
            kemandoranIdMap.remove(removedNik)
            karyawanIdMap.remove(removedName)
            kemandoranIdMap.remove(removedName)

            // Update manual spinner to show the removed worker again
            AppLogger.d("Calling updateManualPemanenSpinnerAfterRemoval...")
            updateManualPemanenSpinnerAfterRemoval()

            AppLogger.d("Removed manual worker from selectedKaryawanList: $removedName (NIK: $removedNik)")
            AppLogger.d("Updated selectedKaryawanList size: ${selectedKaryawanList.size}")
        }
    }


    private fun setupPemanenSpinner() {
        val lyPemanenOtomatis = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)
        val lyPemanenManual = findViewById<LinearLayout>(R.id.lyPemanenManual)

        lyPemanenOtomatis.findViewById<TextView>(R.id.tvTitleFormPanenTBS).text =
            "Pilih Pemanen (Otomatis)"
        lyPemanenManual.findViewById<TextView>(R.id.tvTitleFormPanenTBS).text =
            "Pilih Pemanen (Manual)"

        // Initially hide both spinners
        lyPemanenOtomatis.visibility = View.GONE
        lyPemanenManual.visibility = View.GONE

        // Setup both RecyclerViews
        setupSelectedPemanenRecyclerView() // For automatic
        setupSelectedPemanenManualRecyclerView() // For manual
    }

    private fun setupPemuatSpinner() {
        val rvSelectedPemanenManual = findViewById<RecyclerView>(R.id.rvSelectedPemuatInspection)
        selectedPemuatAdapter = SelectedWorkerAdapter()
        rvSelectedPemanenManual.adapter = selectedPemuatAdapter
        rvSelectedPemanenManual.layoutManager = FlexboxLayoutManager(this).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Set up the remove listener
        selectedPemuatAdapter.setOnWorkerActuallyRemovedListener { removedWorker ->
            AppLogger.d("Pemuat worker removal callback triggered for: ${removedWorker.name}")

            // Find the original employee by ID
            val originalEmployee = allPemuatEmployees.find { it.id.toString() == removedWorker.id }

            if (originalEmployee != null) {
                // Add back to pemuatList if not already there
                if (!pemuatList.any { it.id == originalEmployee.id }) {
                    pemuatList = pemuatList + originalEmployee
                    AppLogger.d("Added worker back to pemuatList: ${originalEmployee.nama}")
                }

                // Update the spinner
                updatePemuatSpinnerAfterRemoval()
                AppLogger.d("Successfully added worker back to spinner: ${originalEmployee.nama}")
            } else {
                AppLogger.d("Could not find original employee with ID: ${removedWorker.id}")

                // Try to find by name as fallback
                val employeeByName = allPemuatEmployees.find { emp ->
                    removedWorker.name.contains(emp.nama ?: "", ignoreCase = true)
                }

                if (employeeByName != null) {
                    AppLogger.d("Found employee by name: ${employeeByName.nama}")
                    if (!pemuatList.any { it.id == employeeByName.id }) {
                        pemuatList = pemuatList + employeeByName
                        updatePemuatSpinnerAfterRemoval()
                        AppLogger.d("Successfully added worker back to spinner by name: ${employeeByName.nama}")
                    }
                } else {
                    AppLogger.d("Could not find employee by name either: ${removedWorker.name}")
                }
            }
        }
    }


    private fun updatePemuatSpinnerAfterRemoval() {
        AppLogger.d("updatePemuatSpinnerAfterRemoval called")

        // Get currently selected workers from Pemuat RecyclerView
        val selectedWorkers = selectedPemuatAdapter.getSelectedWorkers()
        val selectedWorkerIds = selectedWorkers.map { it.id }.toSet()

        AppLogger.d("Currently selected pemuat worker IDs: $selectedWorkerIds")
        AppLogger.d("Current pemuatList count: ${pemuatList.size}")

        // Create list of worker names for spinner from current pemuatList
        val pemuatNames = pemuatList.map { "${it.nik ?: "N/A"} - ${it.nama}" }

        // Setup spinner with available workers
        setupSpinnerView(lyPemuat, pemuatNames)

        // Set hint for the spinner
        val spinner = lyPemuat.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner?.setHint("Pilih Pemuat")

        AppLogger.d("Pemuat spinner updated with ${pemuatNames.size} available workers")
    }


    private fun setupPemanenRecyclerView(detailInspeksi: List<InspectionDetailModel>) {
        val rvSelectedPemanen = findViewById<RecyclerView>(R.id.rvSelectedPemanenFollowUp)
        val pemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = pemanenAdapter
        rvSelectedPemanen.layoutManager = FlexboxLayoutManager(this).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Set display mode to show names without remove buttons
        pemanenAdapter.setDisplayOnly(true)

        AppLogger.d("detailInspeksi size: ${detailInspeksi.size}")

        if (detailInspeksi.isNotEmpty()) {
            rvSelectedPemanen.visibility = View.VISIBLE

            // Group by NIK and Nama to get unique workers
            val uniqueWorkers = detailInspeksi
                .filter { it.nik.isNotEmpty() && it.nama.isNotEmpty() } // Filter out empty nik/nama
                .groupBy { "${it.nik}-${it.nama}" } // Group by nik-nama combination
                .map { (_, details) ->
                    // Take the first detail from each group since they represent the same worker
                    val detail = details.first()
                    Pair(detail.nik, detail.nama)
                }
                .toSet() // Convert to set to ensure uniqueness

            AppLogger.d("uniqueWorkers found: ${uniqueWorkers.size}")

            // Add workers to adapter
            uniqueWorkers.forEach { (nik, nama) ->
                val formattedName = "$nik - $nama"
                val worker = Worker(nik, formattedName)
                pemanenAdapter.addWorker(worker)
            }

            // Style the RecyclerView items
            rvSelectedPemanen.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rvSelectedPemanen.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Style all visible items to be more compact
                    for (i in 0 until rvSelectedPemanen.childCount) {
                        val childView = rvSelectedPemanen.getChildAt(i)

                        // Make text smaller
                        val textView = childView.findViewById<TextView>(R.id.worker_name)
                        textView?.textSize = 12f

                        // Reduce container padding
                        val container = childView.findViewById<LinearLayout>(R.id.worker_container)
                        val density = resources.displayMetrics.density
                        container?.setPadding(
                            (8 * density).toInt(), // 8dp to pixels
                            (4 * density).toInt(), // 4dp to pixels
                            (8 * density).toInt(), // 8dp to pixels
                            (4 * density).toInt()  // 4dp to pixels
                        )
                    }
                }
            })
        } else {
            // No inspection details or no worker data
            rvSelectedPemanen.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapTouchHandling() {
        val scPanen = findViewById<ScrollView>(R.id.svInfoBlokInspection)

        // Disable parent scrolling when touching the map
        map.setOnTouchListener { _, _ ->
            // Request that the parent (ScrollView) doesn't intercept touch events
            scPanen.requestDisallowInterceptTouchEvent(true)
            false // Let the map handle the touch
        }

        // Alternative approach - you can also try this
        scPanen.setOnTouchListener { _, _ ->
            // Allow scrollview to handle touches outside the map
            scPanen.requestDisallowInterceptTouchEvent(false)
            false
        }
    }

    private fun setupButtonListeners() {
        val btnDefault = findViewById<MaterialButton>(R.id.btnDefault)
        val btnSatellite = findViewById<MaterialButton>(R.id.btnSatellite)

        // Map type switcher buttons
        btnDefault.setOnClickListener {
            switchToDefault()
            updateButtonSelection("default")
        }

        btnSatellite.setOnClickListener {
            // Check internet connection before switching to satellite
            if (AppUtils.isNetworkAvailable(this)) {
                switchToGoogleSatellite()
                updateButtonSelection("satellite")
            } else {
                // Show custom alert for no internet connection
                showNoInternetAlert()
            }
        }
    }

    private fun switchToGoogleSatellite() {
        // Alternative: Google Satellite (may require API key for production)
        val googleSatellite = object : OnlineTileSourceBase(
            "GoogleSatellite",
            0, 18, 256, ".png",
            arrayOf("https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return baseUrl.replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                    .replace("{z}", zoom.toString())
            }
        }
        map.setTileSource(googleSatellite)
        map.invalidate()
    }

    private fun showNoInternetAlert() {
        AlertDialogUtility.withSingleAction(
            this@FormInspectionActivity,
            "Kembali",
            "Tidak Ada Koneksi Internet",
            "Fitur satelit memerlukan koneksi internet untuk memuat peta. Pastikan perangkat terhubung ke internet dan coba lagi.",
            "warning.json",
            R.color.colorRedDark
        ) {
            // When user dismisses the alert, go back to default button
            switchToDefault()
            updateButtonSelection("default")
        }
    }


    private fun switchToDefault() {
        // Switch back to default OpenStreetMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.invalidate()
    }

    private fun updateButtonSelection(selectedType: String) {
        resetButtonStyles()
        val btnDefault = findViewById<MaterialButton>(R.id.btnDefault)
        val btnSatellite = findViewById<MaterialButton>(R.id.btnSatellite)

        when (selectedType) {
            "default" -> highlightButton(btnDefault)
            "satellite" -> highlightButton(btnSatellite)
        }
    }

    private fun resetButtonStyles() {
        val btnDefault = findViewById<MaterialButton>(R.id.btnDefault)
        val btnSatellite = findViewById<MaterialButton>(R.id.btnSatellite)

        btnDefault.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grayBorder)
        btnSatellite.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grayBorder)

        // Reset text colors
        btnDefault.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnSatellite.setTextColor(ContextCompat.getColor(this, R.color.black))

        // Reset default button icon tint
        btnDefault.iconTint = ContextCompat.getColorStateList(this, R.color.black)

        // For satellite button, preserve the warning icon if no internet, otherwise no icon
        if (!AppUtils.isNetworkAvailable(this)) {
            // Keep the red warning icon when no internet
            btnSatellite.iconTint =
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        } else {
            // No icon when internet is available
            btnSatellite.iconTint = null
        }
    }

    private fun highlightButton(button: com.google.android.material.button.MaterialButton) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.greenDarker)
        button.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Handle icon tint based on which button and internet status
        when (button.id) {
            R.id.btnDefault -> {
                // Default button always has white icon when selected
                button.iconTint = ContextCompat.getColorStateList(this, R.color.white)
            }

            R.id.btnSatellite -> {
                if (!AppUtils.isNetworkAvailable(this)) {
                    // Keep red warning icon even when selected if no internet
                    button.iconTint =
                        ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                } else {
                    // No icon when internet is available (icon is null)
                    button.iconTint = null
                }
            }
        }
    }

    data class TrackingPath(
        val start: LatLon,
        val end: LatLon
    )

    data class LatLon(
        val lat: Double,
        val lon: Double
    )

    private fun parseTrackingPath(trackingPathJson: String?): TrackingPath? {
        return try {
            if (trackingPathJson.isNullOrEmpty()) return null

            val jsonRegex =
                """"start":\{"lat":(-?\d+\.?\d*),"lon":(-?\d+\.?\d*)\},"end":\{"lat":(-?\d+\.?\d*),"lon":(-?\d+\.?\d*)\}""".toRegex()
            val matchResult = jsonRegex.find(trackingPathJson)

            matchResult?.let { match ->
                val (startLat, startLon, endLat, endLon) = match.destructured
                TrackingPath(
                    start = LatLon(startLat.toDouble(), startLon.toDouble()),
                    end = LatLon(endLat.toDouble(), endLon.toDouble())
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Error parsing tracking path: ${e.message}")
            null
        }
    }

    private fun setupMap() {
        // IMPORTANT: Enable multi-touch controls FIRST before setting tile source
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        // Set tile source to OpenStreetMap default (Mapnik)
        map.setTileSource(TileSourceFactory.MAPNIK) // Default OSM

        // Set initial zoom level and center point
        val mapController = map.controller
        mapController.setZoom(15.0)

        // Set default location (Surakarta, Central Java, Indonesia)
        val startPoint = GeoPoint(-7.5755, 110.8243)
        mapController.setCenter(startPoint)

        // CRITICAL: Additional touch and zoom settings
        map.isTilesScaledToDpi = true
        map.setUseDataConnection(true)

        // Set minimum and maximum zoom levels
        map.minZoomLevel = 3.0  // Increased range
        map.maxZoomLevel = 21.0

        // Enable gestures explicitly
        map.setMultiTouchControls(true) // Set again to ensure it's enabled

        // Check internet connection periodically for button updates
        checkInternetConnectionPeriodically()
    }

    private fun addTrackingMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        type: String
    ) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = "Tracking point"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Different icons for start and end
        val drawable = when (type) {
            "start" -> ContextCompat.getDrawable(this, R.drawable.baseline_circle_24)
            "end" -> ContextCompat.getDrawable(this, R.drawable.baseline_flag_24) // Flag for end
            else -> ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)
        }

        val colorRes = when (type) {
            "start" -> android.R.color.holo_green_dark
            "end" -> android.R.color.holo_red_dark
            else -> android.R.color.holo_blue_dark
        }

        drawable?.setTint(ContextCompat.getColor(this, colorRes))
        marker.icon = drawable

        // NO CLICK LISTENER - Remove touch events for start and end
        // marker.setOnMarkerClickListener { _, _ -> ... }

        map.overlays.add(marker)
        AppLogger.d("Added tracking marker: $title at $latitude, $longitude")
    }


    private fun updateMapWithInspectionData(inspection: InspectionWithDetailRelations) {
        AppLogger.d("Updating map with inspection data")
        clearMarkers()

        // Parse tracking path JSON
        val trackingPath = parseTrackingPath(inspection.inspeksi.tracking_path)

        // Store all points for drawing lines
        val allPoints = mutableListOf<GeoPoint>()

        if (trackingPath != null) {
            AppLogger.d("Adding start marker at: ${trackingPath.start.lat}, ${trackingPath.start.lon}")
            addTrackingMarker(
                trackingPath.start.lat,
                trackingPath.start.lon,
                "Start Point",
                "start"
            )
            allPoints.add(GeoPoint(trackingPath.start.lat, trackingPath.start.lon))

            // Group inspection details by no_pokok and add markers
            val groupedDetails = inspection.detailInspeksi.groupBy { it.no_pokok }
            AppLogger.d("Grouped details: ${groupedDetails.size} trees")

            // Sort by no_pokok to maintain order
            groupedDetails.toSortedMap().forEach { (noPokak, details) ->
                val firstDetail = details.first()
                AppLogger.d("Adding tree marker for pokok $noPokak at: ${firstDetail.latIssue}, ${firstDetail.lonIssue}")
                addInspectionDetailMarker(
                    firstDetail.latIssue,
                    firstDetail.lonIssue,
                    "Tree #$noPokak",
                    details,
                    noPokak
                )
                allPoints.add(GeoPoint(firstDetail.latIssue, firstDetail.lonIssue))
            }

            AppLogger.d("Adding end marker at: ${trackingPath.end.lat}, ${trackingPath.end.lon}")
            addTrackingMarker(trackingPath.end.lat, trackingPath.end.lon, "End Point", "end")
            allPoints.add(GeoPoint(trackingPath.end.lat, trackingPath.end.lon))

            // Draw lines connecting all points
            if (allPoints.size > 1) {
                addConnectingLines(allPoints)
            }

            // Move map to start location
            moveToLocation(trackingPath.start.lat, trackingPath.start.lon, 16.0)
        } else {
            AppLogger.e("Could not parse tracking path")
        }

        map.invalidate()
    }

    // Add method to draw connecting lines
    private fun addConnectingLines(points: List<GeoPoint>) {
        if (points.size < 2) return

        // Create polyline connecting all points
        val polyline = org.osmdroid.views.overlay.Polyline()
        polyline.setPoints(points)
        polyline.color = ContextCompat.getColor(this, android.R.color.holo_blue_dark)
        polyline.width = 5.0f

        // Add the polyline to map
        map.overlays.add(polyline)

        AppLogger.d("Added connecting line with ${points.size} points")
    }


    // First, add this function to handle radio selection
    private fun selectedKondisiValue(selectedId: Int) {
        val kondisiLayout = findViewById<LinearLayout>(R.id.lyConditionType)
        val fblRadioComponents = kondisiLayout?.findViewById<FlexboxLayout>(R.id.fblRadioComponents)
        val kondisiMap = listRadioItems["ConditionType"] ?: emptyMap()
        val targetValue = kondisiMap[selectedId.toString()]

        AppLogger.d("Looking for radio button with value: $targetValue")

        fblRadioComponents?.let { flexLayout ->
            // Uncheck all radio buttons first
            for (i in 0 until flexLayout.childCount) {
                val radioButton = flexLayout.getChildAt(i) as? RadioButton
                radioButton?.isChecked = false
            }

            // Find and select the correct radio button
            for (i in 0 until flexLayout.childCount) {
                val radioButton = flexLayout.getChildAt(i) as? RadioButton
                radioButton?.let { rb ->
                    if (rb.text.toString() == targetValue) {
                        rb.performClick() // This will trigger all the click logic
                        AppLogger.d("Successfully selected radio button: ${rb.text}")
                        return // Exit the function once we found and selected the right button
                    }
                }
            }
            AppLogger.d("Radio button not found for value: $targetValue")
        }
    }

    private fun preselectRadioValues(inspection: InspectionWithDetailRelations) {
        inspection.inspeksi.jenis_kondisi?.let { jenisKondisi ->
            AppLogger.d("jenis_kondisi: $jenisKondisi")

            val kondisiLayout = findViewById<LinearLayout>(R.id.lyConditionType)
            kondisiLayout?.let { layout ->
                val kondisiMap = listRadioItems["ConditionType"] ?: emptyMap()
                val kondisiValue = kondisiMap[jenisKondisi.toString()]

                if (kondisiValue != null) {
                    // Simply call the selection function
                    selectedKondisiValue(jenisKondisi)

                    // Set visibility logic for Baris2
                    findViewById<LinearLayout>(R.id.lyBaris2Inspect).visibility =
                        if (jenisKondisi == 2) View.GONE else View.VISIBLE

                    AppLogger.d("Pre-selected Jenis Kondisi: $kondisiValue (ID: $jenisKondisi)")
                    AppLogger.d("Baris 2 visibility: ${if (jenisKondisi == 2) "GONE" else "VISIBLE"}")
                } else {
                    AppLogger.d("Jenis Kondisi not found in list: $jenisKondisi")
                    AppLogger.d("Available kondisi items: $kondisiMap")
                }
            }
        }
    }

    private fun preselectEditTextValues(inspection: InspectionWithDetailRelations) {
        inspection.inspeksi.baris?.let { baris ->
            val barisValues = baris.split(",").map { it.trim() }

            if (barisValues.isNotEmpty() && barisValues[0].isNotEmpty()) {
                val baris1Layout = findViewById<LinearLayout>(R.id.lyBaris1Inspect)
                val baris1EditText = baris1Layout?.findViewById<EditText>(R.id.etHomeMarkerTPH)

                baris1EditText?.let { editText ->
                    editText.setText(barisValues[0])
                    br1Value = barisValues[0]
                }
            }

            if (barisValues.size > 1 && barisValues[1].isNotEmpty()) {
                val baris2Layout = findViewById<LinearLayout>(R.id.lyBaris2Inspect)
                val baris2EditText = baris2Layout?.findViewById<EditText>(R.id.etHomeMarkerTPH)

                baris2EditText?.let { editText ->
                    editText.setText(barisValues[1])
                    // Also update the global variable
                    br2Value = barisValues[1]
                }
            } else {
                AppLogger.d("No Baris 2 value found or empty")
            }
        }
    }

    // Update clearMarkers to also remove polylines
    private fun clearMarkers() {
        AppLogger.d("Clearing all markers and lines")
        val overlaysToRemove = map.overlays.filter {
            it !is MyLocationNewOverlay
        }
        map.overlays.removeAll(overlaysToRemove)
        map.invalidate()
    }

    data class InspectionParameter(
        val id: Int,
        val nama: String,
        val status_ppro: Int,
        val undivided: String,
        val temuan_pokok: Int,
    )

    private fun createSimpleDetailSummary(processedDetails: List<ProcessedInspectionDetail>): String {
        return processedDetails.joinToString(System.lineSeparator()) { detail ->
            "${detail.kodeInspeksi}. ${detail.nama}: ${detail.temuanTotal}"
        }
    }


    private fun addInspectionDetailMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        details: List<InspectionDetailModel>,
        noPokak: Int
    ) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Use ONLY location pin icon
        val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)

        val hasUnresolvedIssues = details.any { it.status_pemulihan == 0 }
        val colorRes = if (hasUnresolvedIssues) {
            android.R.color.holo_orange_dark
        } else {
            android.R.color.holo_green_light
        }

        drawable?.setTint(ContextCompat.getColor(this, colorRes))
        marker.icon = drawable

        // Process and group the details properly
        val processedDetails = processInspectionDetails(details, noPokak)

        // Set marker info with simplified data
        marker.title = "Pokok #$noPokak"
        marker.snippet = createSimpleDetailSummary(processedDetails)

        marker.setOnMarkerClickListener { clickedMarker, _ ->
            clickedMarker.showInfoWindow()
            true
        }

        map.overlays.add(marker)
        AppLogger.d("Added detail marker: $title at $latitude, $longitude")
    }

    private fun preselectSpinnerValues(inspection: InspectionWithDetailRelations) {
        inspection.tph?.let { tph ->
            AppLogger.d("tph ${tph.divisi_abbr}")
            tph.divisi_abbr?.let { divisiAbbr ->
                // Correct way: First get the LinearLayout, then find the MaterialSpinner inside it
                val afdLayout = findViewById<LinearLayout>(R.id.lyAfdInspect)
                val afdSpinner = afdLayout?.findViewById<MaterialSpinner>(R.id.spPanenTBS)

                afdSpinner?.let { spinner ->
                    val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
                    val position = divisiNames.indexOf(divisiAbbr)
                    if (position >= 0) {
                        spinner.selectedIndex = position
                        afdLayout?.let { layout ->
                            handleItemSelection(layout, position, divisiAbbr)
                        }
                    } else {
                        AppLogger.d("Divisi not found in list: $divisiAbbr")
                    }
                }
            }
        }

        inspection.inspeksi.jalur_masuk?.let { jalurMasuk ->
            AppLogger.d("jalur_masuk ${jalurMasuk}")

            // Correct way: First get the LinearLayout, then find the MaterialSpinner inside it
            val jalurLayout = findViewById<LinearLayout>(R.id.lyJalurInspect)
            val jalurSpinner = jalurLayout?.findViewById<MaterialSpinner>(R.id.spPanenTBS)

            jalurSpinner?.let { spinner ->
                val jalurItems = (listRadioItems["EntryPath"] ?: emptyMap()).values.toList()
                val position = jalurItems.indexOf(jalurMasuk)
                if (position >= 0) {
                    // Set the selected index
                    spinner.selectedIndex = position
                    AppLogger.d("Selected jalur at position: $position, value: $jalurMasuk")

                    // Manually trigger the selection handler to execute the logic
                    jalurLayout?.let { layout ->
                        handleItemSelection(layout, position, jalurMasuk)
                    }
                } else {
                    AppLogger.d("Jalur Masuk not found in list: $jalurMasuk")
                    AppLogger.d("Available jalur items: $jalurItems")
                }
            }
        }

    }

    private fun processInspectionDetails(
        details: List<InspectionDetailModel>,
        noPokak: Int
    ): List<ProcessedInspectionDetail> {
        val inspectionParameters = getInspectionParameters()
        val processedList = mutableListOf<ProcessedInspectionDetail>()

        val groupedByCode = details.groupBy { it.kode_inspeksi }

        groupedByCode.forEach { (kodeInspeksi, detailsForCode) ->
            val parameter = inspectionParameters.find { it.id == kodeInspeksi }
            val parameterName = parameter?.nama ?: "Unknown"

            when (kodeInspeksi) {
                in 1..4 -> {
                    val totalTemuan = detailsForCode.sumOf { it.temuan_inspeksi }

                    processedList.add(
                        ProcessedInspectionDetail(
                            kodeInspeksi = kodeInspeksi,
                            nama = parameterName,
                            temuanTotal = totalTemuan,
                            statusPemulihan = false, // Remove pemulihan logic
                            count = detailsForCode.size,
                            type = "SUMMED"
                        )
                    )
                }

                5, 6 -> {
                    val firstDetail = detailsForCode.first()
                    processedList.add(
                        ProcessedInspectionDetail(
                            kodeInspeksi = kodeInspeksi,
                            nama = parameterName,
                            temuanTotal = firstDetail.temuan_inspeksi,
                            statusPemulihan = false, // Remove pemulihan logic
                            count = detailsForCode.size,
                            type = "SAME_VALUE"
                        )
                    )
                }

                in 7..9 -> {
                    val firstDetail = detailsForCode.first()
                    processedList.add(
                        ProcessedInspectionDetail(
                            kodeInspeksi = kodeInspeksi,
                            nama = parameterName,
                            temuanTotal = firstDetail.temuan_inspeksi,
                            statusPemulihan = false, // Remove pemulihan logic
                            count = detailsForCode.size,
                            type = "TREE_SPECIFIC"
                        )
                    )
                }
            }
        }

        return processedList.sortedBy { it.kodeInspeksi }
    }

    // Data class for processed inspection details
    data class ProcessedInspectionDetail(
        val kodeInspeksi: Int,
        val nama: String,
        val temuanTotal: Double,
        val statusPemulihan: Boolean,
        val count: Int,
        val type: String
    )

    private fun getInspectionParameters(): List<InspectionParameter> {
        return parameterInspeksi.map { param ->
            InspectionParameter(
                id = param.id,
                nama = param.nama,
                status_ppro = param.status_ppro,
                undivided = param.undivided,
                temuan_pokok = param.temuan_pokok
            )
        }
    }

    fun moveToLocation(latitude: Double, longitude: Double, zoom: Double = 15.0) {
        val geoPoint = GeoPoint(latitude, longitude)
        map.controller.animateTo(geoPoint)
        map.controller.setZoom(zoom)
    }


    private fun checkInternetConnectionPeriodically() {
        // Update button appearance every 5 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateSatelliteButtonAppearance()
                handler.postDelayed(this, 5000) // Check every 5 seconds
            }
        }
        handler.post(runnable)
    }

    private fun updateSatelliteButtonAppearance() {
        val btnSatellite = findViewById<MaterialButton>(R.id.btnSatellite)

        if (AppUtils.isNetworkAvailable(this)) {
            btnSatellite.text = "Satelit"
            btnSatellite.icon = null // Remove icon completely
            btnSatellite.iconTint = null
        } else {
            // No internet - show warning icon with red color
            btnSatellite.text = "Satelit"
            btnSatellite.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)
            btnSatellite.iconTint =
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        }
    }

    data class MergedPanenData(
        val workerSet: Set<String>, // Unique workers (NIK - Name format)
        val workerCount: Int,
        val dateList: List<String>, // All unique dates
        val ancakList: List<String>, // All ancak values
        val allPanenRecords: List<PanenEntityWithRelations> // Keep reference to original records
    )

    // Helper function to merge panen records for a single TPH
    fun mergePanenRecordsForTPH(panenList: List<PanenEntityWithRelations>): MergedPanenData {
        val workerSet = mutableSetOf<String>()
        val dateSet = mutableSetOf<String>()
        val ancakSet = mutableSetOf<String>()

        panenList.forEach { panenWithRelations ->
            val panenEntity = panenWithRelations.panen
            val tphEntity = panenWithRelations.tph

            // Add date
            panenEntity?.date_created?.let { dateSet.add(it) }

            // Add ancak
            tphEntity?.ancak?.let { ancakSet.add(it) }

            // Process workers
            val karyawanNama = panenEntity?.karyawan_nama
            val karyawanNik = panenEntity?.karyawan_nik

            if (!karyawanNama.isNullOrBlank() && !karyawanNik.isNullOrBlank()) {
                val names = karyawanNama.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val niks = karyawanNik.split(",").map { it.trim() }.filter { it.isNotBlank() }

                val minSize = minOf(names.size, niks.size)
                for (i in 0 until minSize) {
                    val formattedWorker = "${niks[i]} - ${names[i]}"
                    workerSet.add(formattedWorker)
                }
            }
        }

        return MergedPanenData(
            workerSet = workerSet,
            workerCount = workerSet.size,
            dateList = dateSet.sorted(),
            ancakList = ancakSet.toList(),
            allPanenRecords = panenList
        )
    }

    private fun validateAndShowErrors(): Boolean {
        var isValid = true
        val missingFields = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

        if (featureName == AppUtils.ListFeatureNames.FollowUpInspeksi) {
            if (photoTPHFollowUp == null) {
                isValid = false
                isInTPH = true
                isForSelfie = false
                isForFollowUp = true
                showViewPhotoBottomSheet(null, true, false, true)
                errorMessages.add("Foto Pemuliahan di TPH wajib")
                missingFields.add("Foto Pemulihan TPH")
            }
        } else {
            if (photoInTPH == null) {
                isValid = false
                showViewPhotoBottomSheet(null, isInTPH, false, false)
                errorMessages.add("Foto di TPH wajib")
                missingFields.add("Foto TPH")
            }

            // PEMANEN VALIDATION (consolidated)
            val automaticWorkers = selectedPemanenAdapter.getSelectedWorkers()
            val manualWorkers = selectedPemanenManualAdapter.getSelectedWorkers()
            val totalSelectedWorkers = automaticWorkers.size + manualWorkers.size

            if (totalSelectedWorkers == 0 || selectedKaryawanList.isEmpty()) {
                AppLogger.d("No workers selected! Total: $totalSelectedWorkers, List: ${selectedKaryawanList.size}")
                errorMessages.add("Minimal 1 pemanen yang dipilih!")
                missingFields.add("Pilih Pemanen")

                // Show error on automatic spinner (use global variable if available)
                val layoutPemanenOtomatis = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)
                showValidationError(
                    layoutPemanenOtomatis,
                    "Minimal 1 pemanen yang dipilih!"
                )
                isValid = false
            } else {
                AppLogger.d("Workers selected! Automatic: ${automaticWorkers.size}, Manual: ${manualWorkers.size}")

                // Hide error from pemanen spinner
                val layoutPemanenOtomatis = findViewById<LinearLayout>(R.id.lyPemanenOtomatis)
                hideValidationError(layoutPemanenOtomatis)
            }

            // PEMUAT VALIDATION
            val pemuats = selectedPemuatAdapter.getSelectedWorkers()
            val totalPemuat = pemuats.size

            AppLogger.d("totalPemuat $totalPemuat")

            if (totalPemuat == 0) {
                AppLogger.d("No pemuat selected!")
                errorMessages.add("Minimal 1 pemuat yang dipilih!")
                missingFields.add("Pilih Pemuat")

                showValidationError(lyPemuat, "Minimal 1 pemuat yang dipilih!")
                isValid = false
            } else {
                AppLogger.d("Pemuat workers selected! Total: $totalPemuat")
                hideValidationError(lyPemuat)
            }

            // LOCATION VALIDATION
            if (!locationEnable || lat == 0.0 || lon == 0.0 || lat == null || lon == null) {
                isValid = false
                errorMessages.add(stringXML(R.string.al_location_description_failed))
                missingFields.add("Location")
            }

            // INPUT MAPPINGS VALIDATION (with consistent stroke width)
            inputMappings.forEach { (layout, key, inputType) ->
                val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
                val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)
                val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                val editText = layout.findViewById<EditText>(R.id.etHomeMarkerTPH)

                val isEmpty = when (inputType) {
                    InputType.SPINNER -> {
                        when (layout.id) {
                            R.id.lyAfdInspect -> selectedAfdeling.isEmpty()
                            R.id.lyJalurInspect -> selectedJalurMasuk.isEmpty()
                            else -> spinner.selectedIndex == -1
                        }
                    }

                    InputType.EDITTEXT -> {
                        when (layout.id) {
                            R.id.lyBaris1Inspect -> br1Value.trim().isEmpty()
                            R.id.lyBaris2Inspect -> if (selectedKondisiValue.toInt() != 2) br2Value.trim()
                                .isEmpty() else false

                            else -> editText.text.toString().trim().isEmpty()
                        }
                    }

                    InputType.RADIO -> {
                        when (layout.id) {
                            R.id.lyConditionType -> selectedKondisiValue.isEmpty()
                            else -> false
                        }
                    }
                }

                if (isEmpty) {
                    // FIXED: Set both color AND width for consistent appearance
                    tvError.visibility = View.VISIBLE
                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.colorRedDark)
                    mcvSpinner.strokeWidth = 4 // Ensure stroke is visible
                    missingFields.add(key)
                    isValid = false
                } else {
                    tvError.visibility = View.GONE
                    mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.graytextdark)
                    mcvSpinner.strokeWidth = 2 // Reset to normal width
                }
            }

            // TPH SCAN VALIDATION
            if (selectedTPHIdByScan == null && selectedAfdeling.isNotEmpty()) {
                if (isTriggeredBtnScanned) {
                    if (isEmptyScannedTPH) {
                        tvErrorScannedNotSelected.text =
                            stringXML(R.string.al_no_tph_detected_trigger_submit)
                        tvErrorScannedNotSelected.visibility = View.VISIBLE
                        errorMessages.add(stringXML(R.string.al_no_tph_detected_trigger_submit))
                        isValid = false
                    } else {
                        tvErrorScannedNotSelected.text =
                            "Silakan untuk memilih TPH yang ingin diperiksa!"
                        tvErrorScannedNotSelected.visibility = View.VISIBLE
                        errorMessages.add("Silakan untuk memilih TPH yang ingin diperiksa!")
                        isValid = false
                    }
                } else {
                    tvErrorScannedNotSelected.text = "Silakan tekan tombol scan untuk mencari TPH"
                    tvErrorScannedNotSelected.visibility = View.VISIBLE
                    errorMessages.add("Silakan tekan tombol scan untuk mencari TPH")
                    isValid = false
                }
            } else {
                tvErrorScannedNotSelected.visibility = View.GONE
            }

            // BARIS VALIDATION (br1 and br2 cannot be the same)
            if (selectedKondisiValue.toInt() == 1 && br1Value.trim().isNotEmpty() && br2Value.trim()
                    .isNotEmpty()
            ) {
                val br1Int = br1Value.trim().toIntOrNull() ?: 0
                val br2Int = br2Value.trim().toIntOrNull() ?: 0

                AppLogger.d("br1: $br1Int, br2: $br2Int")
                if (br1Int == br2Int) {
                    val layoutBaris2 = findViewById<LinearLayout>(R.id.lyBaris2Inspect)
                    showValidationError(
                        layoutBaris2,
                        "Baris pertama dan Baris kedua tidak boleh sama"
                    )
                    errorMessages.add("Baris pertama dan Baris kedua tidak boleh sama")
                    isValid = false
                }
            }
        }

        // SHOW ERROR DIALOG IF VALIDATION FAILED
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

    // HELPER FUNCTIONS for consistent validation UI
    private fun showValidationError(layout: LinearLayout, errorMessage: String) {
        val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)

        tvError?.apply {
            text = errorMessage
            visibility = View.VISIBLE
        }

        mcvSpinner?.apply {
            strokeColor = ContextCompat.getColor(this@FormInspectionActivity, R.color.colorRedDark)
            strokeWidth = 4 // Ensure stroke is visible
        }
    }

    private fun hideValidationError(layout: LinearLayout) {
        val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)

        tvError?.visibility = View.GONE

        mcvSpinner?.apply {
            strokeColor = ContextCompat.getColor(this@FormInspectionActivity, R.color.graytextdark)
            strokeWidth = 2 // Reset to normal width
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
            isCameraViewOpen = false  // Reset camera state

            // Only show bottom nav if keyboard is not currently open
            if (!keyboardOpenedWhileBottomSheetVisible) {
                showWithAnimation(bottomNavInspect)
                showWithAnimation(fabPrevFormAncak)
                showWithAnimation(fabNextFormAncak)
            }

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val currentData =
                formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()


            when {
                isForSelfie -> {
                    AppLogger.d("selfie")
                    photoSelfie = fname
                }

                isInTPH && isForFollowUp -> {
                    AppLogger.d("in tph and follow up")
                    photoTPHFollowUp = fname
                }


                isInTPH -> {
                    AppLogger.d("isInTPH ....")
                    photoInTPH = fname
                }

                isForFollowUp -> {

                    AppLogger.d("is for follow up ")
                    formAncakViewModel.savePageData(
                        currentPage,
                        currentData.copy(
                            foto_pemulihan = fname,
                            status_pemulihan = 1
                        )
                    )
                }

                else -> {
                    formAncakViewModel.savePageData(
                        currentPage,
                        currentData.copy(photo = fname)
                    )
                }
            }

            updatePhotoBadgeVisibility()
            updateSelfiePhotoBadgeVisibility()

            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(fname, isInTPH, isForSelfie, isForFollowUp)
                isForSelfie = false
                isInTPH = false
                isForFollowUp = false
            }, 100)
        }
    }
}