package com.cbi.mobile_plantation.ui.view.panenTBS

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.data.repository.PanenTBSRepository
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.LocationViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenTBSViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.MathFun
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.reflect.KMutableProperty0
import android.text.InputType as AndroidInputType
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter
import com.cbi.mobile_plantation.ui.adapter.ListTPHInsideRadiusAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CompletableDeferred
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNCHECKED_CAST")
open class FeaturePanenTBSActivity : AppCompatActivity(), CameraRepository.PhotoCallback,
    ListTPHInsideRadiusAdapter.OnTPHSelectedListener {
    private var isSnackbarShown = false
    private var photoCount = 0
    private val photoFiles = mutableListOf<String>() // Store filenames
    private val komentarFoto = mutableListOf<String>() // Store filenames
    private var jumTBS = 0
    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var tidakVCut = 0
    private var lat: Double? = null
    private var lon: Double? = null
    private var selectedTPHIdByScan: Int? = null

    var currentAccuracy: Float = 0F
    private var prefManager: PrefManager? = null

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var panenTBSViewModel: PanenTBSViewModel
    private var locationEnable: Boolean = false

    private lateinit var btnScanTPHRadius: MaterialButton
    private lateinit var tphScannedResultRecyclerView: RecyclerView
    private lateinit var titleScannedTPHInsideRadius: TextView
    private lateinit var descScannedTPHInsideRadius: TextView
    private lateinit var emptyScannedTPHInsideRadius: TextView
    private lateinit var alertCardScanRadius: MaterialCardView
    private lateinit var alertTvScannedRadius: TextView

    // Global Variables
    private lateinit var layoutAncak: View
    private lateinit var layoutKemandoran: LinearLayout
    private lateinit var layoutKemandoranLain: LinearLayout
    private lateinit var layoutTipePanen: LinearLayout
    private lateinit var layoutPemanenLain: LinearLayout
    private lateinit var layoutPemanen: LinearLayout
    private lateinit var layoutTahunTanam: LinearLayout
    private lateinit var layoutNoTPH: LinearLayout
    private lateinit var layoutBlok: LinearLayout
    private lateinit var layoutSelAsistensi: LinearLayout
    private lateinit var tvErrorScannedNotSelected: TextView

    private var keyboardBeingDismissed = false

    data class ScannedTPHLocation(
        val lat: Double,
        val lon: Double,
        val nomor: String,
        val blokKode: String
    )

    data class ScannedTPHSelectionItem(
        val id: Int,
        val number: String,
        val blockCode: String,
        val distance: Float
    )

    private var latLonMap: Map<Int, ScannedTPHLocation> = emptyMap()

    private lateinit var takeFotoPreviewAdapter: TakeFotoPreviewAdapter

    private var divisiList: List<TPHNewModel> = emptyList()
    private var blokList: List<TPHNewModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var karyawanLainList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()

    private var tphList: List<TPHNewModel> = emptyList()

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private lateinit var selectedPemanenLainAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var rvSelectedPemanenLain: RecyclerView
    private var selectedKemandoranValue: Int? = null

    enum class InputType {
        SPINNER,
        EDITTEXT,
        RADIO,
    }

    private var ancakInput: String = ""

    private var asistensi: Int = 0
    private var blokBanjir: Int = 0
    private var selectedTipePanen: String = ""
    private var selectedAfdeling: String = ""
    private var selectedAfdelingIdSpinner: Int = 0
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""
    private var selectedKemandoran: String = ""
    private var selectedKemandoranLain: String = ""
    private var selectedPemanen: String = ""
    private var selectedPemanenLain: String = ""
    private var infoApp: String = ""


    private var selectedDivisiValue: Int? = null
    private var selectedBlokValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedTPHValue: Int? = null

    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0
    var persenMentah = 0f
    var persenLewatMasak = 0f
    var persenAbnormal = 0f
    var persenJjgKosong = 0f
    var persenMasak = 0f

    private lateinit var jjg_json: String

    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var panenViewModel: PanenViewModel
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private var panenStoredLocal: MutableList<Int> = mutableListOf()
    private var radiusMinimum = 0F
    private var boundaryAccuracy = 0F
    private var isEmptyScannedTPH = true
    private var isTriggeredBtnScanned = false


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        loadingDialog = LoadingDialog(this)

        prefManager = PrefManager(this)

        radiusMinimum = prefManager!!.radiusMinimum
//        radiusMinimum = 80F
        boundaryAccuracy = prefManager!!.boundaryAccuracy

        initViewModel()
        initUI()
        initializeJjgJson()

        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
        setupTitleEachGroupInput()
        setupHeader()

        infoApp = AppUtils.getDeviceInfo(this@FeaturePanenTBSActivity).toString()


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
                        panenViewModel.activePanenList.observe(this@FeaturePanenTBSActivity) { list ->
                            panenDeferred.complete(list ?: emptyList()) // Ensure it's never null
                        }
                    }

                    val panenList = panenDeferred.await()


                    panenStoredLocal = panenList
                        .mapNotNull { it.tph?.id } // This ensures only non-null IDs are stored
                        .toMutableList()


                    val divisiDeferred = async {
                        try {
                            datasetViewModel.getDivisiList(estateIdInt)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching divisiList: ${e.message}")
                            emptyList() // Return an empty list to prevent crash
                        }
                    }

                    divisiList = divisiDeferred.await()

                    if (divisiList.isNullOrEmpty()) {
                        throw Exception("Periksa kembali dataset dengan melakukan Sinkronisasi Data!")
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    setupLayout()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"

                    val estateInfo = estateId?.takeIf { it.isBlank() }
                        ?.let { "2. ID Estate User Login: \"$it\"" }

                    // Combine messages dynamically (avoid extra \n\n if estateInfo is null)
                    val fullMessage = listOfNotNull(errorMessage, estateInfo).joinToString("\n\n")

                    AppLogger.e("Error fetching data: ${e.message}")

                    AlertDialogUtility.withSingleAction(
                        this@FeaturePanenTBSActivity,
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
        findViewById<View>(android.R.id.content).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocus = currentFocus
                if (currentFocus is EditText) {
                    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                    currentFocus.clearFocus()
                }
            }
            false // Don't consume the event so other touch handlers still work
        }

        val mbSaveDataPanenTBS = findViewById<MaterialButton>(R.id.mbSaveDataPanenTBS)

        mbSaveDataPanenTBS.setOnClickListener {
            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Simpan Data",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.confirmation_dialog_description),
                    "warning.json"
                ) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val selectedPemanen = selectedPemanenAdapter.getSelectedWorkers()
                            val selectedPemanenLain =
                                selectedPemanenLainAdapter.getSelectedWorkers()
                            val selectedPemanenIds = selectedPemanen.map { it.id }
                            val selectedPemanenLainIds = selectedPemanenLain.map { it.id }

                            val photoFilesString = photoFiles.joinToString(";")
                            val komentarFotoString = komentarFoto.joinToString(";")

                            val result = withContext(Dispatchers.IO) {
                                panenViewModel.saveDataPanen(
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
                                    ancakInput = ancakInput.toInt(), // Default to "0" if null
                                    info = infoApp ?: "",
                                    archive = 0,
                                    blokBanjir = blokBanjir
                                )
                            }


                            when (result) {
                                is AppRepository.SaveResultPanen.Success -> {
                                    AlertDialogUtility.withSingleAction(
                                        this@FeaturePanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        stringXML(R.string.al_success_save_local),
                                        stringXML(R.string.al_description_success_save_local),
                                        "success.json",
                                        R.color.greenDefault
                                    ) {

                                        panenStoredLocal.add(selectedTPHValue!!.toInt())
                                        resetFormAfterSaveData()
                                    }
                                }

                                is AppRepository.SaveResultPanen.Error -> {
                                    AlertDialogUtility.withSingleAction(
                                        this@FeaturePanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        stringXML(R.string.al_failed_save_local),
                                        "${stringXML(R.string.al_description_failed_save_local)} : ${result.exception.message}",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {}
                                }


                            }

                        } catch (e: Exception) {
                            AppLogger.d("Unexpected error: ${e.message}")

                            AlertDialogUtility.withSingleAction(
                                this@FeaturePanenTBSActivity,
                                stringXML(R.string.al_back),
                                stringXML(R.string.al_failed_save_local),
                                "${stringXML(R.string.al_description_failed_save_local)} : ${e.message}",
                                "warning.json",
                                R.color.colorRedDark
                            ) {}
                        }

                    }
                }
            }
        }
    }

    fun initializeJjgJson() {
        jjg_json = JSONObject().apply {
            put("TO", jumTBS)
            put("UN", bMentah)
            put("OV", bLewatMasak)
            put("EM", jjgKosong)
            put("AB", abnormal)
            put("RA", seranganTikus)
            put("LO", tangkaiPanjang)
            put("TI", tidakVCut)
            put("RI", buahMasak)
            put("KP", kirimPabrik)
            put("PA", tbsDibayar)
        }.toString() // Convert JSON object to string and store in jjg_json
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
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )
    }

    private fun updateCounterTextViews() {
        findViewById<TextView>(R.id.tvCounterBuahMasak).text = "$jumTBS Buah"
        findViewById<TextView>(R.id.tvCounterKirimPabrik).text = "$kirimPabrik Buah"
        findViewById<TextView>(R.id.tvCounterTBSDibayar).text = "$tbsDibayar Buah"
//        findViewById<TextView>(R.id.tvPercentBuahMasak).text = "($persenMasak)%"
    }


    private fun initUI() {

        alertCardScanRadius = findViewById(R.id.alertCardScanRadius)
        alertTvScannedRadius = findViewById(R.id.alertTvScannedRadius)
        btnScanTPHRadius = findViewById(R.id.btnScanTPHRadius)
        tphScannedResultRecyclerView = findViewById(R.id.tphScannedResultRecyclerView)
        titleScannedTPHInsideRadius = findViewById(R.id.titleScannedTPHInsideRadius)
        descScannedTPHInsideRadius = findViewById(R.id.descScannedTPHInsideRadius)
        emptyScannedTPHInsideRadius = findViewById(R.id.emptyScanTPHInsideRadius)

        layoutAncak = findViewById(R.id.layoutAncak)
        layoutPemanen = findViewById(R.id.layoutPemanen)
        layoutPemanenLain = findViewById(R.id.layoutPemanenLain)
        layoutBlok = findViewById(R.id.layoutBlok)
        layoutKemandoran = findViewById(R.id.layoutKemandoran)
        layoutKemandoranLain = findViewById(R.id.layoutKemandoranLain)
        layoutNoTPH = findViewById(R.id.layoutNoTPH)
        layoutSelAsistensi = findViewById(R.id.layoutSelAsistensi)
        layoutTipePanen = findViewById(R.id.layoutTipePanen)
        layoutTahunTanam = findViewById(R.id.layoutTahunTanam)

        tvErrorScannedNotSelected = findViewById(R.id.tvErrorScannedNotSelected)
    }


    private fun resetFormAfterSaveData() {
        selectedPemanenAdapter.clearAllWorkers()
        selectedPemanenLainAdapter.clearAllWorkers()

        if (blokBanjir == 0) {
            tphScannedResultRecyclerView.visibility = View.GONE
            titleScannedTPHInsideRadius.visibility = View.GONE
            descScannedTPHInsideRadius.visibility = View.GONE
            emptyScannedTPHInsideRadius.visibility = View.GONE
            layoutAncak.visibility = View.GONE
            layoutTipePanen.visibility = View.GONE
            layoutPemanen.visibility = View.GONE
            layoutSelAsistensi.visibility = View.GONE
            layoutKemandoran.visibility = View.GONE
            layoutKemandoranLain.visibility = View.GONE
            layoutPemanenLain.visibility = View.GONE
            alertCardScanRadius.visibility = View.GONE
            btnScanTPHRadius.visibility = View.GONE

        }

        val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
        setupSpinnerView(findViewById(R.id.layoutAfdeling), divisiNames)
        val layoutAfdeling = findViewById<View>(R.id.layoutAfdeling)
        val tvUnderFormFieldAfdeling =
            layoutAfdeling.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        tvUnderFormFieldAfdeling.text = "Pilih kembali Afdeling diatas!"
        tvUnderFormFieldAfdeling.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tvUnderFormFieldAfdeling.setTextColor(Color.BLACK)
        tvUnderFormFieldAfdeling.visibility = View.VISIBLE

        setupSpinnerView(findViewById(R.id.layoutTahunTanam), emptyList())
        setupSpinnerView(findViewById(R.id.layoutBlok), emptyList())
        val tipePanenOptions =
            resources.getStringArray(R.array.tipe_panen_options).toList()
        setupSpinnerView(findViewById(R.id.layoutTipePanen), tipePanenOptions)
        val tvUnderFormFieldTipePanen =
            layoutTipePanen.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        tvUnderFormFieldTipePanen.text = "Pilih kembali Tipe Panen diatas!"
        tvUnderFormFieldTipePanen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tvUnderFormFieldTipePanen.setTextColor(Color.BLACK)
        tvUnderFormFieldTipePanen.visibility = View.VISIBLE

        setupSpinnerView(findViewById(R.id.layoutKemandoran), emptyList())
        setupSpinnerView(findViewById(R.id.layoutPemanen), emptyList())
        setupSpinnerView(findViewById(R.id.layoutNoTPH), emptyList())
        setupSpinnerView(findViewById(R.id.layoutKemandoran), emptyList())
        setupSpinnerView(findViewById(R.id.layoutPemanen), emptyList())
        setupSpinnerView(findViewById(R.id.layoutKemandoranLain), emptyList())
        setupSpinnerView(findViewById(R.id.layoutPemanenLain), emptyList())


        val etHomeMarkerTPH = layoutAncak.findViewById<EditText>(R.id.etHomeMarkerTPH)
        etHomeMarkerTPH.setText("")

        blokList = emptyList()
        kemandoranList = emptyList()
        kemandoranLainList = emptyList()
        tphList = emptyList()
        karyawanList = emptyList()
        karyawanLainList = emptyList()
        ancakInput = ""


        //scroll to up
        val scPanen = findViewById<ScrollView>(R.id.scPanen)
        scPanen.post {
            scPanen.fullScroll(ScrollView.FOCUS_UP)
        }

        resetAllCounters()

        //reset all image
        photoCount = 0
        photoFiles.clear()
        komentarFoto.clear()
        takeFotoPreviewAdapter?.resetAllSections()

    }

    @SuppressLint("SetTextI18n")
    private fun updateDependentCounters(
        layoutId: Int,
        change: Int,
        counterVar: KMutableProperty0<Int>,
        tvPercent: TextView?
    ) {
        val sisa = jumTBS - abnormal - bLewatMasak - bMentah - jjgKosong
        when (layoutId) {

            R.id.layoutJumTBS -> {
                if (change > 0) { // When change is positive (Increment)
                    jumTBS += change
                    counterVar.set(jumTBS)
                } else if (change < 0) {
                    if (jumTBS > 0 && sisa > 0) {
                        jumTBS += change
                        counterVar.set(jumTBS)

                        val updates = listOf(
                            Pair(::bMentah, R.id.layoutBMentah),
                            Pair(::bLewatMasak, R.id.layoutBLewatMasak),
                            Pair(::jjgKosong, R.id.layoutJjgKosong),
                            Pair(::abnormal, R.id.layoutAbnormal),
                            Pair(::seranganTikus, R.id.layoutSeranganTikus),
                            Pair(::tangkaiPanjang, R.id.layoutTangkaiPanjang),
                            Pair(::tidakVCut, R.id.layoutVcut)
                        )


                        Handler(Looper.getMainLooper()).post {
                            for ((counter, layout) in updates) {
                                if (counter.get() > jumTBS) {
                                    updateDependentCounters(layout, -1, counter, null)
                                    updateEditText(layout, counter.get())
                                }
                            }
                        }


                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutBMentah -> {
                if (change > 0) {
                    if (jumTBS > 0 && bMentah < jumTBS && sisa > 0) {
                        bMentah += change
                        counterVar.set(bMentah)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (bMentah > 0) {
                        bMentah += change
                        counterVar.set(bMentah)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutBLewatMasak -> {
                if (change > 0) {
                    if (jumTBS > 0 && bLewatMasak < jumTBS && sisa > 0) {
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (bLewatMasak > 0) {
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutJjgKosong -> {
                if (change > 0) { // When change is positive (Increment)
                    if (jumTBS > 0 && jjgKosong < jumTBS && sisa > 0) {
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (jjgKosong > 0) { // Prevent going negative
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutAbnormal -> {
                if (change > 0) { // When change is positive (Increment)
                    AppLogger.d(jumTBS.toString())
                    AppLogger.d(abnormal.toString())
                    AppLogger.d(bLewatMasak.toString())
                    AppLogger.d(bMentah.toString())
                    AppLogger.d(jjgKosong.toString())



                    if (jumTBS > 0 && abnormal < jumTBS && sisa > 0) { // Prevent abnormal from exceeding tbs
                        abnormal += change
                        counterVar.set(abnormal)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (abnormal > 0) { // Prevent going negative
                        abnormal += change
                        counterVar.set(abnormal)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutSeranganTikus -> {
                if (change > 0) {
                    if (jumTBS > 0 && seranganTikus < jumTBS) {
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (seranganTikus > 0) { // Prevent going negative
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutTangkaiPanjang -> {
                if (change > 0) {
                    if (jumTBS > 0 && tangkaiPanjang < jumTBS) {
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (tangkaiPanjang > 0) { // Prevent going negative
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutVcut -> {
                if (change > 0) {
                    if (jumTBS > 0 && tidakVCut < jumTBS) {
                        tidakVCut += change
                        counterVar.set(tidakVCut)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (tidakVCut > 0) {
                        tidakVCut += change
                        counterVar.set(tidakVCut)
                    } else {
                        vibrate()
                    }
                }
            }
        }

        formulas()
        updateCounterTextViews()

        if (layoutId == R.id.layoutBMentah) {
            tvPercent?.let {
                it.setText("${persenMentah}%")
            }
        } else if (layoutId == R.id.layoutBLewatMasak) {
            tvPercent?.let {
                it.setText("${persenLewatMasak}%")
            }
        } else if (layoutId == R.id.layoutJjgKosong) {
            tvPercent?.let {
                it.setText("${persenJjgKosong}%")
            }
        } else if (layoutId == R.id.layoutAbnormal) {
            tvPercent?.let {
                it.setText("${persenAbnormal}%")
            }
        }
    }

    private fun updateEditText(layoutId: Int, value: Int) {
        val includedLayout = findViewById<View>(layoutId)
        val etNumber = includedLayout?.findViewById<EditText>(R.id.etNumber)
        etNumber?.setText(value.toString())
    }

    private fun formulas() {
//        buahMasak = jumTBS - jjgKosong - bMentah - bLewatMasak
//        bMentah = jumTBS - bLewatMasak - jjgKosong - buahMasak
//        bLewatMasak = jumTBS - bMentah - buahMasak - jjgKosong
//        jjgKosong = jumTBS - bMentah - buahMasak - bLewatMasak
//        tbsDibayar = jumTBS - bMentah - jjgKosong
//        kirimPabrik = jumTBS - jjgKosong - abnormal

        tbsDibayar = jumTBS - bMentah - jjgKosong
        kirimPabrik = jumTBS - jjgKosong
        buahMasak = jumTBS - abnormal - bMentah - jjgKosong - bLewatMasak

        persenMentah = MathFun().round((bMentah.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenMasak = MathFun().round((bMentah.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenLewatMasak = MathFun().round((bLewatMasak.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenAbnormal = MathFun().round((abnormal.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenJjgKosong = MathFun().round((jjgKosong.toFloat() / jumTBS.toFloat() * 100), 2)!!


        initializeJjgJson()
    }

    private fun initViewModel() {
        panenTBSViewModel = ViewModelProvider(
            this,
            PanenTBSViewModel.Factory(application, PanenTBSRepository(this))
        )[PanenTBSViewModel::class.java]


        val idTakeFotoLayout = findViewById<View>(R.id.id_take_foto_layout)
        val idEditFotoLayout = findViewById<View>(R.id.id_editable_foto_layout)
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

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]

        val factoryPanenViewModel = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factoryPanenViewModel)[PanenViewModel::class.java]
    }


    /**
     * Sets up all spinner mappings, counters, and the RecyclerView.
     */
    @SuppressLint("SetTextI18n", "CutPasteId")
    private fun setupLayout() {
        val featureName = intent.getStringExtra("FEATURE_NAME")
        if (featureName == "Panen TBS") {
            findViewById<LinearLayout>(R.id.layoutEstate).visibility = View.GONE
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
            isTriggeredBtnScanned = true
            if (currentAccuracy > boundaryAccuracy) {
                AlertDialogUtility.withTwoActions(
                    this@FeaturePanenTBSActivity, // Replace with your actual Activity name
                    "Lanjutkan",
                    getString(R.string.confirmation_dialog_title),
                    "Gps terdeteksi diluar dari ${boundaryAccuracy.toInt()} meter. Apakah tetap akan melanjutkan?",
                    "warning.json",
                    ContextCompat.getColor(this@FeaturePanenTBSActivity, R.color.greendarkerbutton)
                ) {
                    checkScannedTPHInsideRadius()
                }
            } else {
                checkScannedTPHInsideRadius()
            }

        }


        inputMappings = listOf(
            Triple(
                findViewById<LinearLayout>(R.id.layoutEstate),
                getString(R.string.field_estate),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAfdeling),
                getString(R.string.field_afdeling),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutTahunTanam),
                getString(R.string.field_tahun_tanam),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutBlok),
                getString(R.string.field_blok),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutTipePanen),
                getString(R.string.field_tipe_panen),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutNoTPH),
                getString(R.string.field_no_tph),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAncak),
                getString(R.string.field_ancak),
                InputType.EDITTEXT
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutKemandoran),
                getString(R.string.field_kemandoran),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutPemanen),
                getString(R.string.field_pemanen),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutKemandoranLain),
                getString(R.string.field_kemandoran_lain),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutPemanenLain),
                getString(R.string.field_pemanen_lain),
                InputType.SPINNER
            )
        )



        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.layoutEstate -> {
                            val namaEstate = listOf(prefManager!!.estateUserLengkapLogin ?: "")
                            setupSpinnerView(layoutView, namaEstate)
                            findViewById<MaterialSpinner>(R.id.spPanenTBS).setSelectedIndex(0)
                        }

                        R.id.layoutAfdeling -> {
                            val divisiNames =
                                divisiList.sortedBy { it.divisi_abbr }.mapNotNull { it.divisi_abbr }
                            setupSpinnerView(layoutView, divisiNames)
                        }

                        R.id.layoutTipePanen -> {
                            val tipePanenOptions =
                                resources.getStringArray(R.array.tipe_panen_options).toList()
                            setupSpinnerView(layoutView, tipePanenOptions)
                        }

                        else -> {
                            setupSpinnerView(layoutView, emptyList())
                        }

                    }
                }

                InputType.EDITTEXT -> setupEditTextView(layoutView)
                else -> {}

            }
        }


        val counterMappings = listOf(
            Triple(R.id.layoutJumTBS, "Jumlah TBS", ::jumTBS),
            Triple(R.id.layoutBMentah, "Buah Mentah", ::bMentah),
            Triple(R.id.layoutBLewatMasak, "Buah Lewat Masak", ::bLewatMasak),
            Triple(R.id.layoutJjgKosong, "Janjang Kosong", ::jjgKosong),
            Triple(R.id.layoutAbnormal, "Abnormal", ::abnormal),
            Triple(R.id.layoutSeranganTikus, "Serangan Tikus", ::seranganTikus),
            Triple(R.id.layoutTangkaiPanjang, "Tangkai Panjang", ::tangkaiPanjang),
            Triple(R.id.layoutVcut, "Tidak V-Cut", ::tidakVCut)
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPaneWithButtons(layoutId, R.id.tvNumberPanen, labelText, counterVar)
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


        val layoutPemanen = findViewById<LinearLayout>(R.id.layoutPemanen)
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

        val layoutPemanenLain = findViewById<LinearLayout>(R.id.layoutPemanenLain)
        val parentLayoutLain = layoutPemanenLain.parent as ViewGroup
        val index2 = parentLayoutLain.indexOfChild(layoutPemanenLain)
        parentLayoutLain.addView(rvSelectedPemanenLain, index2 + 1)

        setupRecyclerViewTakePreviewFoto()
        setupSwitchBlokBanjir()
        setupSwitchAsistensi()
        setupFormulasView()
    }


    private fun setupTitleEachGroupInput() {
        val tvDescDatagrading: TextView = findViewById(R.id.title_data_grading)
        val tvDescLampiran: TextView = findViewById(R.id.title_lampiran_foto)
        val tvDescInformasiBlok: TextView = findViewById(R.id.title_data_informasi_blok)

        val textGrading = "Data Grading*"
        val textLampiran = "Lampiran Foto*"
        val textBlok = "Informasi Blok*"
        val spannable = SpannableString(textGrading)
        val spannable2 = SpannableString(textLampiran)
        val spannable3 = SpannableString(textBlok)

        val starColor = ContextCompat.getColor(this, R.color.colorRedDark) //
        spannable.setSpan(
            ForegroundColorSpan(starColor),
            textGrading.length - 1,
            textGrading.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable2.setSpan(
            ForegroundColorSpan(starColor),
            textLampiran.length - 1,
            textLampiran.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable3.setSpan(
            ForegroundColorSpan(starColor),
            textBlok.length - 1,
            textBlok.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvDescDatagrading.text = spannable
        tvDescLampiran.text = spannable2
        tvDescInformasiBlok.text = spannable3

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
                tphScannedResultRecyclerView.adapter = ListTPHInsideRadiusAdapter(tphList, this)

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
                    tphScannedResultRecyclerView.overScrollMode =
                        View.OVER_SCROLL_ALWAYS
                } else {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = false
                    tphScannedResultRecyclerView.overScrollMode =
                        View.OVER_SCROLL_NEVER // Disable bounce effect
                }

                tphScannedResultRecyclerView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                tphScannedResultRecyclerView.isVerticalScrollBarEnabled =
                    true // Force scrollbar visibility
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

        loadingDialog.dismiss()
    }

    private fun setupFormulasView() {
        val tvFormulas = findViewById<TextView>(R.id.tvFormulas)

        tvFormulas.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_formulas_grading, null)

            view.background = ContextCompat.getDrawable(this, R.drawable.rounded_top_right_left)

            val dialog = BottomSheetDialog(this)
            dialog.setContentView(view)

            // Get screen width
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels

            // Set bottom sheet width to 80% of screen width
            dialog.window?.apply {
                setLayout(
                    (width * 0.8).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }

            // Set expanded state when showing
            dialog.setOnShowListener {
                val bottomSheet =
                    dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheet!!)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialog.show()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE


        // Set input type based on layout ID
        etHomeMarkerTPH.inputType = when (layoutView.id) {
            R.id.layoutAncak -> AndroidInputType.TYPE_CLASS_NUMBER
            else -> AndroidInputType.TYPE_CLASS_TEXT
        }

        // Enhanced keyboard hiding function with debounce mechanism
        fun hideKeyboard() {
            if (keyboardBeingDismissed) return

            keyboardBeingDismissed = true
            val imm =
                application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etHomeMarkerTPH.windowToken, 0)
            etHomeMarkerTPH.clearFocus()

            // Reset flag after delay to prevent rapid toggling
            Handler(Looper.getMainLooper()).postDelayed({
                keyboardBeingDismissed = false
            }, 300)
        }

        // Make EditText non-focusable when not editing
        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        val touchInterceptor = object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    hideKeyboard()
                }
                return false
            }
        }

        // Apply to parent views to ensure scrolling works
        val parentLayout = layoutView.findViewById<LinearLayout>(R.id.parentSpPanenTBS)
        parentLayout?.setOnTouchListener(touchInterceptor)

        // Apply specifically to the TextView area
        val tvTitle = layoutView.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        tvTitle?.setOnTouchListener(touchInterceptor)

        // Make MCVSpinner consume touch events but still hide keyboard
        MCVSpinner.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            // Let the view handle the event normally after hiding keyboard
            false
        }

        // Add global touch listener to root view that doesn't interfere with scrolling
        val rootView = layoutView.rootView
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (etHomeMarkerTPH.hasFocus()) {
                    hideKeyboard()
                }
            }
            false
        }

        // Focus change listener
        etHomeMarkerTPH.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }

        // Add this to handle MaterialSpinner clicks
        spHomeMarkerTPH.setOnClickListener {
            hideKeyboard()
        }

        // Add text changed listener
        etHomeMarkerTPH.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
                MCVSpinner.setBackgroundColor(
                    ContextCompat.getColor(
                        layoutView.context,
                        R.color.graytextdark
                    )
                )

                if (layoutView.id == R.id.layoutAncak) {
                    val inputText = s?.toString()?.trim() ?: ""
                    ancakInput = inputText

                    // Make another layout visible if there's text input
                    if (inputText.isNotEmpty()) {
                        // Replace R.id.yourOtherLayoutId with the actual ID of the layout you want to make visible
                        val otherLayout = findViewById<View>(R.id.layoutTipePanen)
                        otherLayout?.visibility = View.VISIBLE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Add this to your Activity class
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    v.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }


    private fun getTPHsInsideRadius(
        userLat: Double,
        userLon: Double,
        coordinates: Map<Int, ScannedTPHLocation>
    ): List<ScannedTPHSelectionItem> {
        val resultsList = mutableListOf<ScannedTPHSelectionItem>()

        for ((id, location) in coordinates) {
            if (id in panenStoredLocal) continue // Exclude IDs already in panenStoredLocal

            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLon, location.lat, location.lon, results)
            val distance = results[0]

            if (distance <= radiusMinimum) {
                resultsList.add(
                    ScannedTPHSelectionItem(
                        id = id,
                        number = location.nomor,
                        blockCode = location.blokKode,
                        distance = distance
                    )
                )
            }
        }

        return resultsList.sortedBy { it.distance } // Sort by distance
    }


    private fun resetDependentSpinners(rootView: View) {
        // List of all dependent layouts that need to be reset
        val dependentLayouts = listOf(
            R.id.layoutTahunTanam,
            R.id.layoutBlok,
            R.id.layoutNoTPH,
            R.id.layoutKemandoran,
            R.id.layoutPemanen,
            R.id.layoutKemandoranLain,
            R.id.layoutPemanenLain
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

    private fun resetTPHSpinner(rootView: View) {
        val layoutNoTPH = rootView.findViewById<LinearLayout>(R.id.layoutNoTPH)
        setupSpinnerView(layoutNoTPH, emptyList())
        tphList = emptyList()
        selectedTPH = ""
        selectedTPHValue = null
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinnerView(
        linearLayout: LinearLayout,
        data: List<String>,
        onItemSelected: (Int) -> Unit = {}
    ) {
        val editText = linearLayout.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)

        // Hide keyboard helper
        fun ensureKeyboardHidden() {
            val imm =
                application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(linearLayout.windowToken, 0)
            editText.clearFocus()
        }

        if (linearLayout.id == R.id.layoutKemandoran || linearLayout.id == R.id.layoutPemanen || linearLayout.id == R.id.layoutKemandoranLain || linearLayout.id == R.id.layoutPemanenLain) {
//            Spinner khusus saerch
            spinner.setOnTouchListener { _, event ->

                ensureKeyboardHidden()
                if (event.action == MotionEvent.ACTION_UP) {
                    // ✅ Pass `linearLayout` to avoid error
                    showPopupSearchDropdown(
                        spinner,
                        data,
                        editText,
                        linearLayout
                    ) { selectedItem, position ->
                        spinner.text = selectedItem // Update spinner UI
                        tvError.visibility = View.GONE
                        onItemSelected(position) // Ensure selection callback works
                    }
                }
                true // Consume event, preventing default behavior
            }
        }


        if (linearLayout.id == R.id.layoutEstate) {
            spinner.isEnabled = false // Disable spinner
        }

        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE
            handleItemSelection(
                linearLayout,
                position,
                item.toString()
            ) // ✅ Ensure `linearLayout` is passed
        }
    }


    private fun setupSwitchBlokBanjir() {
        val switchBlokBanjir = findViewById<SwitchMaterial>(R.id.selBlokBanjir)

        val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
        val etAncak = layoutAncak.findViewById<EditText>(R.id.etHomeMarkerTPH)

        switchBlokBanjir.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutTahunTanam.visibility = View.VISIBLE
                layoutBlok.visibility = View.VISIBLE
                layoutNoTPH.visibility = View.VISIBLE
                layoutAncak.visibility = View.VISIBLE
                layoutKemandoran.visibility = View.VISIBLE
                layoutPemanen.visibility = View.VISIBLE
                layoutSelAsistensi.visibility = View.VISIBLE
                layoutTipePanen.visibility = View.VISIBLE

                blokList = emptyList()
                kemandoranList = emptyList()
                kemandoranLainList = emptyList()
                tphList = emptyList()
                karyawanList = emptyList()
                karyawanLainList = emptyList()

                setupSpinnerView(layoutTahunTanam, emptyList())
                setupSpinnerView(layoutBlok, emptyList())
                setupSpinnerView(layoutNoTPH, emptyList())
                setupSpinnerView(layoutKemandoran, emptyList())
                setupSpinnerView(layoutPemanen, emptyList())

                etAncak.setText("")
                ancakInput = ""

                selectedTipePanen = ""
                setupSpinnerView(layoutTipePanen, tipePanenOptions)

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

                blokBanjir = 1

                alertCardScanRadius.visibility = View.GONE
                alertTvScannedRadius.visibility = View.GONE
                btnScanTPHRadius.visibility = View.GONE
                titleScannedTPHInsideRadius.visibility = View.GONE
                descScannedTPHInsideRadius.visibility = View.GONE
                emptyScannedTPHInsideRadius.visibility = View.GONE
                tphScannedResultRecyclerView.visibility = View.GONE

                tvErrorScannedNotSelected.visibility = View.GONE

                resetAllCounters()

                //reset all image
                photoCount = 0
                photoFiles.clear()
                komentarFoto.clear()
                takeFotoPreviewAdapter?.resetAllSections()
            } else {
                blokBanjir = 0

                layoutTahunTanam.visibility = View.GONE
                layoutBlok.visibility = View.GONE
                layoutNoTPH.visibility = View.GONE
                layoutAncak.visibility = View.GONE
                layoutKemandoran.visibility = View.GONE
                layoutPemanen.visibility = View.GONE
                layoutSelAsistensi.visibility = View.GONE
                layoutTipePanen.visibility = View.GONE
                setupSpinnerView(layoutTahunTanam, emptyList())
                setupSpinnerView(layoutBlok, emptyList())
                setupSpinnerView(layoutNoTPH, emptyList())
                setupSpinnerView(layoutKemandoran, emptyList())
                setupSpinnerView(layoutPemanen, emptyList())

                etAncak.setText("")
                ancakInput = ""

                selectedTipePanen = ""
                setupSpinnerView(layoutTipePanen, tipePanenOptions)

                blokList = emptyList()
                kemandoranList = emptyList()
                kemandoranLainList = emptyList()
                tphList = emptyList()
                karyawanList = emptyList()
                karyawanLainList = emptyList()

                selectedTahunTanamValue = null
                selectedBlok = ""
                selectedBlokValue = null
                selectedTPH = ""
                selectedTPHValue = null
                selectedKemandoranLain = ""

                selectedPemanenAdapter.clearAllWorkers()
                selectedPemanenLainAdapter.clearAllWorkers()
                resetAllCounters()

                //reset all image
                photoCount = 0
                photoFiles.clear()
                komentarFoto.clear()
                takeFotoPreviewAdapter?.resetAllSections()
            }
        }
    }


    private fun setupSwitchAsistensi() {
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)

        switchAsistensi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutKemandoranLain.visibility = View.VISIBLE
                layoutPemanenLain.visibility = View.VISIBLE

                asistensi = 1
//                setupSpinnerView(layoutKemandoranLain, workerGroupList.map { it.name })
            } else {
                // Hide layouts when switch is OFF
                asistensi = 0
                selectedPemanenLainAdapter.clearAllWorkers()
                layoutKemandoranLain.visibility = View.GONE
                layoutPemanenLain.visibility = View.GONE

            }
        }
    }

    private fun validateAndShowErrors(): Boolean {
        var isValid = true
        val missingFields = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

        if (!locationEnable || lat == 0.0 || lon == 0.0 || lat == null || lon == null) {
            isValid = false
            this.vibrate()
            errorMessages.add(stringXML(R.string.al_location_description_failed))
            missingFields.add("Location")
        }

        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)
        val switchBlokBanjir = findViewById<SwitchMaterial>(R.id.selBlokBanjir)
        val isSwitchBlokBanjirEnabled = switchBlokBanjir.isChecked
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
                            R.id.layoutAfdeling -> selectedAfdeling.isEmpty()
                            R.id.layoutTipePanen -> selectedTipePanen.isEmpty()
                            R.id.layoutKemandoran -> selectedKemandoran.isEmpty()
                            R.id.layoutPemanen -> selectedPemanen.isEmpty()
                            R.id.layoutNoTPH -> blokBanjir == 1 && selectedTPH.isEmpty()
                            R.id.layoutBlok -> blokBanjir == 1 && selectedBlok.isEmpty()
                            else -> spinner.selectedIndex == -1
                        }
                    }

                    InputType.EDITTEXT -> {
                        when (key) {
                            getString(R.string.field_ancak) -> ancakInput.trim().isEmpty()
                            else -> editText.text.toString().trim().isEmpty()
                        }
                    }

                    else -> false
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

        if (!isSwitchBlokBanjirEnabled && selectedAfdeling.isNotEmpty() && isTriggeredBtnScanned) {
            if (isEmptyScannedTPH) {
                isValid = false
                errorMessages.add(stringXML(R.string.al_no_tph_detected_trigger_submit))

                tvErrorScannedNotSelected.text =
                    stringXML(R.string.al_no_tph_detected_trigger_submit)
                tvErrorScannedNotSelected.visibility = View.VISIBLE
            } else {
                if (selectedTPHIdByScan == null) {
                    isValid = false
                    errorMessages.add(stringXML(R.string.al_no_tph_selected_by_scanned))
                    tvErrorScannedNotSelected.text =
                        stringXML(R.string.al_no_tph_selected_by_scanned)
                    tvErrorScannedNotSelected.visibility = View.VISIBLE
                }
            }
        }

        if (!isSwitchBlokBanjirEnabled && !isTriggeredBtnScanned) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_for_attempting_get_tph_in_radius))
            tvErrorScannedNotSelected.text = stringXML(R.string.al_for_attempting_get_tph_in_radius)
            tvErrorScannedNotSelected.visibility = View.VISIBLE
        }

        if (isAsistensiEnabled) {
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

        if (jumTBS <= 0) {
            isValid = false
            val layoutJumTBS = findViewById<ConstraintLayout>(R.id.layoutJumTBS)
            layoutJumTBS.findViewById<TextView>(R.id.tvErrorFormPanenTBS)?.apply {
                text = stringXML(R.string.al_total_tbs_description)
                visibility = View.VISIBLE
            }
            errorMessages.add(stringXML(R.string.al_total_tbs_description))
        } else {
            val layoutJumTBS = findViewById<ConstraintLayout>(R.id.layoutJumTBS)
            layoutJumTBS.findViewById<TextView>(R.id.tvErrorFormPanenTBS)?.apply {
                visibility = View.GONE
            }
        }

        if (photoCount == 0) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_photo_minimal_one))

            val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
            tvErrorNotAttachPhotos.visibility = View.VISIBLE
        }

        if (!isValid) {
            vibrate()
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

    private fun handleItemSelection(
        linearLayout: LinearLayout,
        position: Int,
        selectedItem: String
    ) {

        val rootView = linearLayout.rootView
        val currentFocus = rootView.findFocus()
        if (currentFocus is EditText) {
            currentFocus.clearFocus()
            val imm =
                application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }

        when (linearLayout.id) {
            R.id.layoutAfdeling -> {
                resetDependentSpinners(linearLayout.rootView)
                val tvErrorScannedNotSelected =
                    findViewById<TextView>(R.id.tvErrorScannedNotSelected)
                tvErrorScannedNotSelected.visibility = View.GONE
                isTriggeredBtnScanned = false
                selectedAfdeling = selectedItem
                selectedAfdelingIdSpinner = position

                val selectedDivisiId = try {
                    divisiList.find { it.divisi_abbr == selectedAfdeling }?.divisi
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedDivisiId: ${e.message}")
                    null
                }

                if (blokBanjir == 0) {
                    alertCardScanRadius.visibility = View.GONE
                    btnScanTPHRadius.visibility = View.GONE
                    titleScannedTPHInsideRadius.visibility = View.GONE
                    descScannedTPHInsideRadius.visibility = View.GONE
                    emptyScannedTPHInsideRadius.visibility = View.GONE
                    tphScannedResultRecyclerView.visibility = View.GONE

                }

                val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()
                selectedDivisiValue = selectedDivisiId

                val allIdAfdeling = try {
                    divisiList.map { it.divisi }
                } catch (e: Exception) {
                    AppLogger.e("Error mapping allIdAfdeling: ${e.message}")
                    emptyList()
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        animateLoadingDots(linearLayout)
                        delay(500) // 1 second delay
                    }

                    try {
                        if (estateId == null || selectedDivisiId == null) {
                            throw IllegalStateException("Estate ID or selectedDivisiId is null!")
                        }

                        val kemandoranDeferred = async {
                            try {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    selectedDivisiIdList
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching kemandoranList: ${e.message}")
                                emptyList()
                            }
                        }

                        val kemandoranLainDeferred = async {
                            try {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    allIdAfdeling as List<Int>
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching kemandoranLainList: ${e.message}")
                                emptyList()
                            }
                        }

                        var tahunTanamList: List<String> = emptyList()

                        if (blokBanjir == 1) {
                            val blokDeferred = async {
                                try {
                                    datasetViewModel.getBlokList(
                                        estateId!!.toInt(),
                                        selectedDivisiId
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error fetching blokList: ${e.message}")
                                    emptyList()
                                }
                            }
                            blokList = blokDeferred.await()
                            tahunTanamList = try {
                                blokList.mapNotNull { it.tahun }.distinct()
                                    .sortedBy { it.toIntOrNull() }
                            } catch (e: Exception) {
                                AppLogger.e("Error processing tahunTanamList: ${e.message}")
                                emptyList()
                            }

                        } else {
                            latLonMap = emptyMap()
                            latLonMap = async {
                                try {
                                    datasetViewModel.getLatLonDivisi(
                                        estateId!!.toInt(),
                                        selectedDivisiId
                                    )
                                        .mapNotNull {
                                            val id = it.id
                                            val lat = it.lat?.toDoubleOrNull()
                                            val lon = it.lon?.toDoubleOrNull()
                                            val nomor = it.nomor ?: ""
                                            val blokKode = it.blok_kode ?: ""

                                            if (id != null && lat != null && lon != null) {
                                                id to ScannedTPHLocation(lat, lon, nomor, blokKode)
                                            } else {
                                                null
                                            }
                                        }
                                        .toMap()
                                } catch (e: Exception) {
                                    AlertDialogUtility.withSingleAction(
                                        this@FeaturePanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        stringXML(R.string.al_failed_fetch_data),
                                        "Error fetching listLatLonAfd: ${e.message}",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {

                                    }
                                    emptyMap()
                                }
                            }.await()
                        }

                        kemandoranList = kemandoranDeferred.await()
                        kemandoranLainList = kemandoranLainDeferred.await()

                        withContext(Dispatchers.Main) {
                            try {


                                if (blokBanjir == 1) {
                                    val layoutTahunTanam =
                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutTahunTanam)
                                    setupSpinnerView(
                                        layoutTahunTanam,
                                        tahunTanamList.ifEmpty { emptyList() }
                                    )
                                } else {

                                    val alertCardScanRadius =
                                        findViewById<MaterialCardView>(R.id.alertCardScanRadius)
                                    val alertTvScannedRadius =
                                        findViewById<TextView>(R.id.alertTvScannedRadius)
                                    val btnScanTPHRadius =
                                        findViewById<MaterialButton>(R.id.btnScanTPHRadius)



                                    setupScanTPHTrigger()
                                }


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
                        AppLogger.e("Error fetching afdeling data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FeaturePanenTBSActivity,
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


            R.id.layoutTahunTanam -> {
                resetTPHSpinner(linearLayout.rootView)
                val selectedTahunTanam = selectedItem.toString()
                selectedTahunTanamValue = selectedTahunTanam


                AppLogger.d(estateId.toString())
                AppLogger.d(selectedDivisiValue.toString())
                AppLogger.d(selectedTahunTanamValue.toString())

                val filteredBlokCodes = blokList.filter {

                    it.dept == estateId!!.toInt() &&
                            it.divisi == selectedDivisiValue &&
                            it.tahun == selectedTahunTanamValue
                }

                val layoutBlok =
                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutBlok)
                if (filteredBlokCodes.isNotEmpty()) {
                    val blokNames = filteredBlokCodes.map { it.blok_kode }
                    setupSpinnerView(layoutBlok, blokNames as List<String>)
                    layoutBlok.visibility = View.VISIBLE
                } else {
                    setupSpinnerView(layoutBlok, emptyList())
                }
            }

            R.id.layoutTipePanen -> {
                selectedTipePanen = position.toString()

                if (blokBanjir == 0) {

                    layoutKemandoran.visibility = View.VISIBLE
                    layoutPemanen.visibility = View.VISIBLE


                    val switchAsistensi =
                        findViewById<LinearLayout>(R.id.layoutSelAsistensi)
                    switchAsistensi.visibility = View.VISIBLE
                    selectedTPHIdByScan?.let { tphId ->

                        val idList = listOf(tphId)

                        // Use lifecycleScope to launch a coroutine
                        lifecycleScope.launch {
                            try {
                                val tphList = datasetViewModel.getTPHsByIds(idList)

                                if (tphList.isNotEmpty()) {
                                    val tph = tphList.first()
                                    Log.d(
                                        "TPH_DATA",
                                        "Retrieved TPH: ID=${tph.id}, Nomor=${tph.nomor}"
                                    )

                                } else {
                                    Toast.makeText(
                                        this@FeaturePanenTBSActivity,
                                        "TPH data not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("TPH_ERROR", "Error loading TPH data: ${e.message}", e)
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Failed to load TPH data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } ?: run {

                        Log.w("TPH_WARNING", "No TPH selected")
                    }
                }
            }

            R.id.layoutBlok -> {
                resetTPHSpinner(linearLayout.rootView)
                selectedBlok = selectedItem.toString()


                val selectedFieldId = try {
                    blokList.find { blok ->
                        blok.dept == estateId?.toIntOrNull() && // Safe conversion
                                blok.divisi == selectedDivisiValue &&
                                blok.tahun == selectedTahunTanamValue &&
                                blok.blok_kode == selectedBlok
                    }?.blok
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected Blok ID: ${e.message}")
                    null
                }

                if (selectedFieldId != null) {
                    selectedBlokValue = selectedFieldId
                    AppLogger.d("Selected Blok ID: $selectedBlokValue")
                } else {
                    selectedBlokValue = null
                    AppLogger.e("Selected Blok ID is null, skipping processing.")
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

                        tphList = tphDeferred.await() ?: emptyList() // Avoid null crash

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
                                linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutNoTPH)

                            if (noTPHList.isNotEmpty()) {
                                setupSpinnerView(layoutNoTPH, noTPHList as List<String>)
                            } else {
                                setupSpinnerView(layoutNoTPH, emptyList())
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching afdeling data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FeaturePanenTBSActivity,
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
//
//
            R.id.layoutNoTPH -> {
                selectedTPH = selectedItem.toString()

                val selectedTPHId = try {
                    tphList?.find {
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

                AppLogger.d(selectedTPHId.toString())
                if (selectedTPHId != null) {
                    if (!panenStoredLocal.contains(selectedTPHId)) {
                        panenStoredLocal.add(selectedTPHId)
                        AppLogger.d("Added TPH ID to panenStoredLocal: $selectedTPHId")
                    } else {
                        AppLogger.d("TPH ID already exists in panenStoredLocal: $selectedTPHId")
                    }

                    selectedTPHValue = selectedTPHId
                    AppLogger.d("Selected TPH ID: $selectedTPHValue")
                } else {
                    selectedTPHValue = null
                    AppLogger.e("Selected TPH ID is null, skipping processing.")
                }
            }
//
//
            R.id.layoutKemandoran -> {
                selectedKemandoran = selectedItem.toString()

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
                    AppLogger.d("Filtered Kemandoran ID: $filteredKemandoranId")

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

                            val karyawanList = karyawanDeferred.await()
                            val karyawanNames = karyawanList
                                .sortedBy { it.nama } // Sort by name alphabetically
                                .map { "${it.nama} - ${it.nik}" }


                            withContext(Dispatchers.Main) {
                                val layoutPemanen =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                                layoutPemanen.visibility = View.VISIBLE


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
                                    this@FeaturePanenTBSActivity,
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
//
//
            R.id.layoutPemanen -> {
                selectedPemanen = selectedItem.toString()
                val selectedNama = selectedPemanen.substringBefore(" - ").trim()

                val karyawanMap = karyawanList.associateBy({ it.nama!!.trim() }, { it.id })

                val selectedPemanenId = karyawanMap[selectedNama]
                if (selectedPemanenId != null) {
                    val worker = Worker(selectedPemanenId.toString(), selectedPemanen)
                    selectedPemanenAdapter.addWorker(worker)

                    val availableWorkers = selectedPemanenAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }

                    AppLogger.d("Selected Worker: $selectedPemanen, ID: $selectedPemanenId")
                }
            }

            R.id.layoutKemandoranLain -> {
                selectedPemanenLainAdapter.clearAllWorkers()
                selectedKemandoranLain = selectedItem.toString()


                val selectedIdKemandoranLain: Int? = try {
                    kemandoranLainList.find {
                        it.nama == selectedKemandoranLain
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected Kemandoran: ${e.message}")
                    null // Return null to prevent crashes
                }


                if (selectedIdKemandoranLain != null) {
                    AppLogger.d("Selected ID Kemandoran Lain: $selectedIdKemandoranLain")

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
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanenLain)
                                if (namaKaryawanKemandoranLain.isNotEmpty()) {
                                    setupSpinnerView(
                                        layoutPemanenLain,
                                        namaKaryawanKemandoranLain as List<String>
                                    )
                                } else {
                                    setupSpinnerView(layoutPemanenLain, emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching kemandoran lain data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
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

            R.id.layoutPemanenLain -> {
                selectedPemanenLain = selectedItem.toString()
                val selectedNamaPemanenLain = selectedPemanenLain.substringBefore(" - ").trim()

                val karyawanMap = karyawanLainList.associateBy({ it.nama }, { it.id })

                val selectedPemanenLainId = karyawanMap[selectedNamaPemanenLain]

                if (selectedPemanenLainId != null) {
                    val worker = Worker(selectedPemanenLainId.toString(), selectedPemanenLain)
                    selectedPemanenLainAdapter.addWorker(worker)

                    val availableWorkers = selectedPemanenLainAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }

                    AppLogger.d("Selected Worker: $selectedPemanenLain, ID: $selectedPemanenLainId")
                }
            }


        }
    }


    private fun setupRecyclerViewTakePreviewFoto() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFotoPreview)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        takeFotoPreviewAdapter = TakeFotoPreviewAdapter(
            3,
            cameraViewModel,
            this,
            AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
        )
        recyclerView.adapter = takeFotoPreviewAdapter
    }

    /**
     * Updates the text of a spinner's label in the included layout.
     */

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }


    private fun resetAllCounters() {
        val counterMappings = listOf(
            Triple(R.id.layoutJumTBS, "Jumlah TBS", ::jumTBS),
            Triple(R.id.layoutBMentah, "Buah Mentah", ::bMentah),
            Triple(R.id.layoutBLewatMasak, "Buah Lewat Masak", ::bLewatMasak),
            Triple(R.id.layoutJjgKosong, "Janjang Kosong", ::jjgKosong),
            Triple(R.id.layoutAbnormal, "Abnormal", ::abnormal),
            Triple(R.id.layoutSeranganTikus, "Serangan Tikus", ::seranganTikus),
            Triple(R.id.layoutTangkaiPanjang, "Tangkai Panjang", ::tangkaiPanjang),
            Triple(R.id.layoutVcut, "Tidak V-Cut", ::tidakVCut)
        )
        counterMappings.forEach { (_, _, counterVar) ->
            counterVar.set(0)
        }

        counterMappings.forEach { (layoutId, _, counterVar) ->
            updateEditText(layoutId, counterVar.get()) // Update UI
        }

        formulas()
        updateCounterTextViews()
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
        btnScanTPHRadius.visibility = View.VISIBLE

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

    /**
     * Sets up a layout with increment and decrement buttons for counters.
     */
    private fun setupPaneWithButtons(
        layoutId: Int,
        textViewId: Int,
        labelText: String,
        counterVar: KMutableProperty0<Int>
    ) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)
        val tvPercent = includedLayout.findViewById<TextView>(R.id.tvPercent)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        etNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                etNumber.removeTextChangedListener(this)

                try {
                    val newValue = if (s.isNullOrEmpty()) 0 else s.toString().toInt()

                    if (layoutId == R.id.layoutJumTBS) {
                        val oldValue = jumTBS
                        val sumOfOthers = abnormal + bLewatMasak + bMentah + jjgKosong

                        // For jumTBS: Don't allow changing to a value less than the sum of other counters
                        if (newValue < sumOfOthers) {
                            etNumber.setText(oldValue.toString())
                            vibrate()
                            etNumber.addTextChangedListener(this)
                            return
                        }

                        jumTBS = newValue
                        counterVar.set(jumTBS)

                        // Update dependent counters if needed
                        val updates = listOf(
                            Pair(::bMentah, R.id.layoutBMentah),
                            Pair(::bLewatMasak, R.id.layoutBLewatMasak),
                            Pair(::jjgKosong, R.id.layoutJjgKosong),
                            Pair(::abnormal, R.id.layoutAbnormal),
                            Pair(::seranganTikus, R.id.layoutSeranganTikus),
                            Pair(::tangkaiPanjang, R.id.layoutTangkaiPanjang),
                            Pair(::tidakVCut, R.id.layoutVcut)
                        )

                        Handler(Looper.getMainLooper()).post {
                            for ((counter, layout) in updates) {
                                if (counter.get() > newValue) {
                                    updateDependentCounters(layout, -1, counter, null)
                                    updateEditText(layout, counter.get())
                                }
                            }
                        }
                    } else {
                        // For other layouts (bMentah, bLewatMasak, etc.)
                        val totalOthers = when (layoutId) {
                            R.id.layoutBMentah -> abnormal + bLewatMasak + newValue + jjgKosong
                            R.id.layoutBLewatMasak -> abnormal + newValue + bMentah + jjgKosong
                            R.id.layoutJjgKosong -> abnormal + bLewatMasak + bMentah + newValue
                            R.id.layoutAbnormal -> newValue + bLewatMasak + bMentah + jjgKosong
                            else -> abnormal + bLewatMasak + bMentah + jjgKosong
                        }

                        if (jumTBS > 0 && newValue <= jumTBS && totalOthers <= jumTBS) {
                            counterVar.set(newValue)
                        } else {
                            // Reset to previous value
                            etNumber.setText(counterVar.get().toString())
                            vibrate()
                            etNumber.addTextChangedListener(this)
                            return
                        }
                    }

                    // Update formulas and UI for all cases
                    formulas()
                    updatePercentages()
                    updateCounterTextViews()

                } catch (e: NumberFormatException) {
                    etNumber.setText(counterVar.get().toString())
                    vibrate()
                }

                etNumber.addTextChangedListener(this)
            }
        })

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)

        btDec.setOnClickListener {
            if (counterVar.get() > 0) {
                // Special handling for jumTBS decrement
                if (layoutId == R.id.layoutJumTBS) {
                    val sumOfOthers = abnormal + bLewatMasak + bMentah + jjgKosong
                    if (counterVar.get() - 1 < sumOfOthers) {
                        vibrate()
                        return@setOnClickListener
                    }
                }

                updateDependentCounters(
                    layoutId,
                    -1,
                    counterVar,
                    tvPercent
                )
                etNumber.setText(counterVar.get().toString())
            } else {
                vibrate()
            }
        }

        btInc.setOnClickListener {
            updateDependentCounters(
                layoutId,
                1,
                counterVar,
                tvPercent
            )
            etNumber.setText(counterVar.get().toString())
        }
    }

    // Helper function to update all percentage displays
    private fun updatePercentages() {
        findViewById<View>(R.id.layoutBMentah)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenMentah}%")
        findViewById<View>(R.id.layoutBLewatMasak)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenLewatMasak}%")
        findViewById<View>(R.id.layoutJjgKosong)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenJjgKosong}%")
        findViewById<View>(R.id.layoutAbnormal)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenAbnormal}%")
    }

    // Helper function for vibration
    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    50,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(50)
        }
    }


    override fun onTPHSelected(selectedTPH: ScannedTPHSelectionItem) {
//        Toast.makeText(
//            this,
//            "Selected: ID=${selectedTPH.id}, ${selectedTPH.number} - ${selectedTPH.blockCode}",
//            Toast.LENGTH_SHORT
//        ).show()

        val tvErrorScannedNotSelected = findViewById<TextView>(R.id.tvErrorScannedNotSelected)
        tvErrorScannedNotSelected.visibility = View.GONE

        selectedTPHIdByScan = selectedTPH.id
        selectedTPHValue = selectedTPHIdByScan
        layoutAncak.visibility = View.VISIBLE
    }

    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
        onItemSelected: (String, Int) -> Unit
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
                // If keyboard is shown and makes the EditText hidden
                if (keyboardHeight > 0) {
                    // Get spinner position
                    val spinnerLocation = IntArray(2)
                    spinner.getLocationOnScreen(spinnerLocation)

                    // If keyboard hides the EditText, scroll up
                    if (spinnerLocation[1] + spinner.height + popupWindow.height > rect.bottom) {
                        val scrollAmount =
                            spinnerLocation[1] - 400 // Scroll to show dropdown with extra space
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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

    // Helper function to find ScrollView
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


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            cameraViewModel.statusCamera() -> {
                // If in camera mode, close camera and return to previous screen
                cameraViewModel.closeCamera()
            }

            else -> {
                vibrate()
                AlertDialogUtility.withTwoActions(
                    this,
                    "Keluar",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_feature),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight)
                ) {
                    val intent = Intent(this, HomePageActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }

            }
        }

    }

    @Override
    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()

        // Manually check location permission
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
    }


    private fun showSnackbarWithSettings(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Settings") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
            }
            .show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbarWithSettings("Location permission is required for this app. Enable it in Settings.")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
                isSnackbarShown = false
            } else {
                showSnackbarWithSettings("Location permission denied. Enable it in Settings.")
            }
        }

    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()

    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()


    }

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

        val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
        tvErrorNotAttachPhotos.visibility = View.GONE

        adapter?.addPhotoFile("$position", photoFile)

        photoCount++
        photoFiles.add(fname)
        komentarFoto.add(komentar!!)

        val viewHolder =
            recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
        viewHolder?.let {
            Glide.with(this)
                .load(photoFile)
                .into(it.imageView)
        }
    }


}
