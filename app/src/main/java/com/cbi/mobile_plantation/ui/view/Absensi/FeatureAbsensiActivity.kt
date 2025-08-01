package com.cbi.mobile_plantation.ui.view.Absensi

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.KaryawanModel
//import com.cbi.mobile_plantation.data.model.KemandoranDetailModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.ui.adapter.AbsensiAdapter
import com.cbi.mobile_plantation.ui.adapter.AbsensiDataList
import com.cbi.mobile_plantation.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.LocationViewModel
import com.cbi.mobile_plantation.ui.viewModel.SaveDataAbsensiState
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
//import com.cbi.mobile_plantation.utils.DataCacheManager
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.ui.adapter.SelectedPemanenAbsensiAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
//import com.cbi.markertph.data.model.DeptModel
//import com.cbi.markertph.data.model.DivisiModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java


interface WorkerRemovalListener {
    fun onWorkerRemoved(workerId: String, workerName: String)
}



open class FeatureAbsensiActivity : AppCompatActivity(),WorkerRemovalListener,TakeFotoPreviewAdapter.LocationDataProvider, CameraRepository.PhotoCallback {

    private var photoCount = 0
    private val photoFiles = mutableListOf<String>() // Store filenames
    private val komentarFoto = mutableListOf<String>() // Store filenames

    private var lat: Double? = null
    private var lon: Double? = null
    private var finalLat: Double? = null
    private var finalLon: Double? = null
    var currentAccuracy: Float = 0F
    private var prefManager: PrefManager? = null

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private var locationEnable: Boolean = false
    private var isPermissionRationaleShown = false
    private lateinit var takeFotoPreviewAdapter: TakeFotoPreviewAdapter

    private var divisiList: List<TPHNewModel> = emptyList()

    private var karyawanList: List<KaryawanModel> = emptyList()
    private var karyawanLainList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()
    private lateinit var linearLayout: LinearLayout

    private lateinit var selectedKemandoranAdapter: SelectedPemanenAbsensiAdapter
    private lateinit var selectedKemandoranLainAdapter: SelectedPemanenAbsensiAdapter
    private lateinit var rvSelectedKemandoran: RecyclerView
    private lateinit var rvSelectedKemandoranLain: RecyclerView

    private lateinit var absensiAdapter: AbsensiAdapter

    private lateinit var loadingDialog: LoadingDialog

    enum class InputType {
        SPINNER,
        EDITTEXT
    }


    private var asistensi: Int = 0
    private var selectedAfdeling: String = ""
    private var selectedAfdelingIdSpinner: Int = 0
    private var selectedKemandoran: String = ""
    private var selectedKemandoranKode: String = ""
    private var selectedKemandoranLain: String = ""
    private var infoApp: String = ""

    private lateinit var backButton: ImageView

    private var selectedDivisiValue: Int? = null
    private var selectedDivisionSpinnerIndex: Int? = null
    private var activityInitialized = false
    private lateinit var inputMappings: List<Triple<LinearLayout, String, FeatureAbsensiActivity.InputType>>
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var absensiViewModel: AbsensiViewModel
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private var karyawanId: List<String> = emptyList()
//    private var filteredKemandoranId: Int? = null
//private var filteredKemandoranId: List<Int> = emptyList()
    private val filteredKemandoranId = mutableSetOf<Int>()
    private val selectedKemandoranIds = mutableSetOf<Int>()

    private val selectedKemandoranIdsLain = mutableSetOf<Int>()
    private val filteredKemandoranIdLain = mutableListOf<Int>()



    private var selectedKemandoranIdLainAbsensi: Int? = null

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_absensi)
        checkDateTimeSettings()
    }

    private fun setupUI(){
        loadingDialog = LoadingDialog(this)

        prefManager = PrefManager(this)
        initViewModel()
        initUI()
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener {
            backButton.isEnabled = false
            onBackPressed()
        }

        setupHeader()

        infoApp = AppUtils.getDeviceInfo(this@FeatureAbsensiActivity).toString()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
            }
            try {
                val estateIdStr = estateId?.trim()
                if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                    val estateIdInt = estateIdStr.toInt()
//                    val deptDeferred = async { datasetViewModel.getDeptList(estateIdStr) }
                    val divisiDeferred = async { datasetViewModel.getDivisiList(estateIdInt) }
                    divisiList = divisiDeferred.await()
                }
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    setupLayout()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {  // Ensure dialog is shown on Main thread
                    AppLogger.e("Error fetching data: ${e.message}")

                    AlertDialogUtility.withSingleAction(
                        this@FeatureAbsensiActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        "${stringXML(R.string.al_failed_fetch_data_desc)},  ${e.message} penyebab ID Estate User Login: \"$estateId\"",
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                        finish()
                    }
                }
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvTableDataAbsensi)

        // Inisialisasi Adapter dengan List kosong
        absensiAdapter =
            AbsensiAdapter(mutableListOf()) // Gunakan mutableListOf() agar sesuai dengan MutableList
        recyclerView.adapter = absensiAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val mbSaveDataAbsensi = findViewById<MaterialButton>(R.id.mbSaveDataAbsensi)
        mbSaveDataAbsensi.setOnClickListener {
            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Simpan Data",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.confirmation_dialog_description),
                    "warning.json",
                    function = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val absensiList = absensiAdapter.getItems()

                                    // Group by kemandoranId first
                                    val groupedByKemandoran = absensiList.groupBy { it.kemandoranId }

                                    val hadirIdMap = mutableMapOf<String, List<String>>()
                                    val mangkirIdMap = mutableMapOf<String, List<String>>()
                                    val hadirNikMap = mutableMapOf<String, List<String>>()
                                    val mangkirNikMap = mutableMapOf<String, List<String>>()
                                    val hadirNamaMap = mutableMapOf<String, List<String>>()
                                    val mangkirNamaMap = mutableMapOf<String, List<String>>()

                                    // Process each kemandoran group
                                    groupedByKemandoran.forEach { (kemandoranId, karyawanList) ->
                                        val (karyawanMasuk, karyawanTidakMasuk) = karyawanList.partition { it.isChecked }

                                        val kemandoranIdStr = kemandoranId.toString()

                                        // Store hadir (present) employees
                                        if (karyawanMasuk.isNotEmpty()) {
                                            hadirIdMap[kemandoranIdStr] = karyawanMasuk.map { it.id.toString() }
                                            hadirNikMap[kemandoranIdStr] = karyawanMasuk.map { it.nik }
                                            hadirNamaMap[kemandoranIdStr] = karyawanMasuk.map { it.namaOnly }
                                        }

                                        // Store mangkir (absent) employees
                                        if (karyawanTidakMasuk.isNotEmpty()) {
                                            mangkirIdMap[kemandoranIdStr] = karyawanTidakMasuk.map { it.id.toString() }
                                            mangkirNikMap[kemandoranIdStr] = karyawanTidakMasuk.map { it.nik }
                                            mangkirNamaMap[kemandoranIdStr] = karyawanTidakMasuk.map { it.namaOnly }
                                        }
                                    }

                                    // Create JSON for MSK (Present) - using "h" key for hadir
                                    val karyawanMskIdJson = createAttendanceJson(hadirEmployees = hadirIdMap)
                                    val karyawanMskNikJson = createAttendanceJson(hadirEmployees = hadirNikMap)
                                    val karyawanMskNamaJson = createAttendanceJson(hadirEmployees = hadirNamaMap)

                                    // Create JSON for TDK_MSK (Absent) - using "m" key for mangkir
                                    val karyawanTdkMskIdJson = createAttendanceJson(mangkirEmployees = mangkirIdMap)
                                    val karyawanTdkMskNikJson = createAttendanceJson(mangkirEmployees = mangkirNikMap)
                                    val karyawanTdkMskNamaJson = createAttendanceJson(mangkirEmployees = mangkirNamaMap)

                                    val dateAbsen = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                                    // Here's an example checking all present employees across all kemandoran
                                    val allKaryawanMasuk = absensiList.filter { it.isChecked }
                                    val allKaryawanMskIdList = allKaryawanMasuk.map { it.id.toString() }

                                    val isDuplicate = absensiViewModel.isAbsensiExist(dateAbsen, allKaryawanMskIdList)
                                    if (isDuplicate) {
                                        return@withContext SaveDataAbsensiState.Error("Data absensi sudah ada untuk sebagian karyawan.")
                                    }

                                    AppLogger.d("Tgl ${dateAbsen} + JSON Structure Created")
                                    AppLogger.d("karyawanMskIdJson: ${karyawanMskIdJson.toString()}")

                                    val listKemandoran = (filteredKemandoranId + selectedKemandoranIds + filteredKemandoranIdLain + selectedKemandoranIdsLain)
                                        .sortedBy { id -> kemandoranList.find { it.id == id }?.divisi_abbr ?: "" }
                                        .joinToString(",")

                                    AppLogger.d("Sorted Kemandoran List: $listKemandoran")

                                    val photoFilesString = photoFiles.joinToString(";")
                                    val komentarFotoString = komentarFoto.joinToString(";")
                                        .takeIf { it.isNotBlank() && it != ";" && !it.matches(Regex("^;+$")) }

                                    absensiViewModel.saveDataAbsensi(
                                        kemandoran_id = listKemandoran,
                                        date_absen = dateAbsen,
                                        created_by = userId!!,
                                        dept = prefManager!!.estateIdUserLogin!!,
                                        dept_abbr = prefManager!!.estateUserLogin!!,
                                        divisi = selectedDivisiValue.toString(),
                                        divisi_abbr = selectedAfdeling,
                                        karyawan_msk_id = karyawanMskIdJson.toString(),      // JSON with "h" key
                                        karyawan_tdk_msk_id = karyawanTdkMskIdJson.toString(), // JSON with "m" key
                                        karyawan_msk_nik = karyawanMskNikJson.toString(),    // JSON with "h" key
                                        karyawan_tdk_msk_nik = karyawanTdkMskNikJson.toString(), // JSON with "m" key
                                        karyawan_msk_nama = karyawanMskNamaJson.toString(),    // JSON with "h" key
                                        karyawan_tdk_msk_nama = karyawanTdkMskNamaJson.toString(), // JSON with "m" key
                                        foto = photoFilesString,
                                        komentar = komentarFotoString ?: "",
                                        asistensi = asistensi ?: 0,
                                        lat = lat ?: 0.0,
                                        lon = lon ?: 0.0,
                                        info = infoApp ?: "",
                                        archive = 0
                                    )
                                }


                                when (result) {
                                    is SaveDataAbsensiState.Success -> {
                                        playSound(R.raw.berhasil_simpan)
                                        AlertDialogUtility.withSingleAction(
                                            this@FeatureAbsensiActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_success_save_local),
                                            stringXML(R.string.al_description_success_save_local),
                                            "success.json",
                                            R.color.greenDefault
                                        ) {
                                            val intent = Intent(
                                                this@FeatureAbsensiActivity,
                                                ListAbsensiActivity::class.java
                                            )
                                            // Add the FEATURE_NAME extra to match the homepage behavior
                                            intent.putExtra("FEATURE_NAME", AppUtils.ListFeatureNames.RekapAbsensiPanen)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    }

                                    is SaveDataAbsensiState.Error -> {
                                        AlertDialogUtility.withSingleAction(
                                            this@FeatureAbsensiActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_failed_save_local),
                                            "${stringXML(R.string.al_description_failed_save_local)} : ${result.message}",
                                            "warning.json",
                                            R.color.colorRedDark
                                        ) { }
                                    }

                                    SaveDataAbsensiState.Loading -> TODO()
                                }
                            } catch (e: Exception) {
                                loadingDialog.dismiss()
                                AppLogger.e("Error in saveDataAbsensi", e.toString())
                                AlertDialogUtility.withSingleAction(
                                    this@FeatureAbsensiActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_save_local),
                                    "${stringXML(R.string.al_description_failed_save_local)} : ${e.message ?: "Unknown error"}",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) { }
                            }
                        }

//                        mbSaveDataAbsensi.isEnabled = true
                    },
                    cancelFunction = {
//                        mbSaveDataAbsensi.isEnabled = true
                    }
                )
            }
        }
    }

    fun createAttendanceJson(
        hadirEmployees: Map<String, List<String>> = emptyMap(),
        mangkirEmployees: Map<String, List<String>> = emptyMap(),
        sakitEmployees: Map<String, List<String>> = emptyMap(),
        izinEmployees: Map<String, List<String>> = emptyMap(),
        cutiEmployees: Map<String, List<String>> = emptyMap()
    ): JSONObject {
        val attendanceJson = JSONObject()

        // Add Hadir (h)
        if (hadirEmployees.isNotEmpty()) {
            val hadirJson = JSONObject()
            hadirEmployees.forEach { (kemandoran, employeeList) ->
                hadirJson.put(kemandoran, employeeList.joinToString(","))
            }
            attendanceJson.put("h", hadirJson)
        }

        // Add Mangkir (m)
        if (mangkirEmployees.isNotEmpty()) {
            val mangkirJson = JSONObject()
            mangkirEmployees.forEach { (kemandoran, employeeList) ->
                mangkirJson.put(kemandoran, employeeList.joinToString(","))
            }
            attendanceJson.put("m", mangkirJson)
        }

        // Add Sakit (s)
        if (sakitEmployees.isNotEmpty()) {
            val sakitJson = JSONObject()
            sakitEmployees.forEach { (kemandoran, employeeList) ->
                sakitJson.put(kemandoran, employeeList.joinToString(","))
            }
            attendanceJson.put("s", sakitJson)
        }

        // Add Izin (i)
        if (izinEmployees.isNotEmpty()) {
            val izinJson = JSONObject()
            izinEmployees.forEach { (kemandoran, employeeList) ->
                izinJson.put(kemandoran, employeeList.joinToString(","))
            }
            attendanceJson.put("i", izinJson)
        }

        // Add Cuti (c)
        if (cutiEmployees.isNotEmpty()) {
            val cutiJson = JSONObject()
            cutiEmployees.forEach { (kemandoran, employeeList) ->
                cutiJson.put(kemandoran, employeeList.joinToString(","))
            }
            attendanceJson.put("c", cutiJson)
        }

        return attendanceJson
    }


    private fun initUI() {
        backButton = findViewById(R.id.btn_back)
    }

    private fun initViewModel() {
        val idTakeFotoLayout = findViewById<View>(R.id.fotoAbsensi)
        val idEditFotoLayout = findViewById<View>(R.id.editFotoAbsensi)
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

        val factoryAbsensiViewModel = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel =
            ViewModelProvider(this, factoryAbsensiViewModel)[AbsensiViewModel::class.java]
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

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }

    @SuppressLint("CutPasteId")
    private fun setupLayout() {
        inputMappings = listOf(
            Triple(
                findViewById<LinearLayout>(R.id.layoutEstateAbsensi),
                getString(R.string.field_estate),
                FeatureAbsensiActivity.InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAfdelingAbsensi),
                getString(R.string.field_afdeling),
                FeatureAbsensiActivity.InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi),
                getString(R.string.field_kemandoran),
                FeatureAbsensiActivity.InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutKemandoranLainAbsensi),
                getString(R.string.field_kemandoran_lain),
                FeatureAbsensiActivity.InputType.SPINNER
            )
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                FeatureAbsensiActivity.InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.layoutEstateAbsensi -> {
                            val namaEstate = listOf(prefManager!!.estateUserLengkapLogin ?: "")
                            setupSpinnerView(layoutView, namaEstate)
                            findViewById<MaterialSpinner>(R.id.spPanenTBS).setSelectedIndex(0)
                        }
                        R.id.layoutAfdelingAbsensi -> {
                            AppLogger.d(divisiList.toString())
                            val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
                            setupSpinnerView(layoutView, divisiNames)
                        }

                        else -> {
                            setupSpinnerView(layoutView, emptyList())
                        }

                    }
                }

                FeatureAbsensiActivity.InputType.EDITTEXT -> setupEditTextView(layoutView)
            }
        }

        rvSelectedKemandoran = RecyclerView(this).apply {
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
        selectedKemandoranAdapter = SelectedPemanenAbsensiAdapter(this)
        rvSelectedKemandoran.adapter = selectedKemandoranAdapter

        val layoutKemandoranAbsensi = findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi)
        val parentLayout = layoutKemandoranAbsensi.parent as ViewGroup
        val index = parentLayout.indexOfChild(layoutKemandoranAbsensi)
        parentLayout.addView(rvSelectedKemandoran, index + 1)

        rvSelectedKemandoranLain = RecyclerView(this).apply {
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
        selectedKemandoranLainAdapter = SelectedPemanenAbsensiAdapter(this)
        rvSelectedKemandoranLain.adapter = selectedKemandoranLainAdapter

        val layoutKemandoranLainAbsensi =
            findViewById<LinearLayout>(R.id.layoutKemandoranLainAbsensi)
        val parentLayoutLain = layoutKemandoranLainAbsensi.parent as ViewGroup
        val index2 = parentLayoutLain.indexOfChild(layoutKemandoranLainAbsensi)
        parentLayoutLain.addView(rvSelectedKemandoranLain, index2 + 1)

        setupRecyclerViewTakePreviewFoto()
        setupSwitch()
    }

    private fun setupRecyclerViewTakePreviewFoto() {
        val recyclerView: RecyclerView = findViewById(R.id.rcFotoPreview)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        takeFotoPreviewAdapter = TakeFotoPreviewAdapter(1, cameraViewModel, this, AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen)
        recyclerView.adapter = takeFotoPreviewAdapter
    }

    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
                MCVSpinner.setBackgroundColor(
                    ContextCompat.getColor(
                        layoutView.context,
                        R.color.graytextdark
                    )
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onWorkerRemoved(workerId: String, workerName: String) {
        // Remove this kemandoran from the selectedKemandoranIds set
        val kemandoranId = workerId.toIntOrNull()
        AppLogger.d("masuk ges")
        if (kemandoranId != null) {
            selectedKemandoranIds.remove(kemandoranId)

            // Update filteredKemandoranId list
            filteredKemandoranId.clear()
            filteredKemandoranId.addAll(selectedKemandoranIds)

            // Remove all karyawan belonging to this kemandoran from the absensiAdapter
            absensiAdapter.removeWorkerById(workerId)

            // If you have other logic that depends on the selected kemandoran list,
            // update that here as well

            // Log the removal for debugging
            AppLogger.d("Removed Kemandoran: $workerName (ID: $workerId)")

            // Optionally, you may need to refresh your spinner with available workers
            val availableWorkers = selectedKemandoranAdapter.getAvailableWorkers()
            if (availableWorkers.isNotEmpty()) {
                setupSpinnerView(
                    linearLayout,
                    availableWorkers.map { it.name })  // Extract names
            }
        }
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

    private fun setupSwitch() {
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensiAbsensi)
        val layoutKemandoranLain = findViewById<LinearLayout>(R.id.layoutKemandoranLainAbsensi)

        switchAsistensi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show layouts when switch is ON
                layoutKemandoranLain.visibility = View.VISIBLE

                asistensi = 1
                // Setup spinner for KemandoranLain if needed
//                setupSpinnerView(layoutKemandoranLain, workerGroupList.map { it.name })
            } else {
                // Hide layouts when switch is OFF
                asistensi = 0
//                selectedPemanenLainAdapter.clearAllWorkers()
                layoutKemandoranLain.visibility = View.GONE

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

        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensiAbsensi)
        val isAsistensiEnabled = switchAsistensi.isChecked

        inputMappings.forEach { (layout, key, inputType) ->
            if (layout.id != R.id.layoutKemandoranLainAbsensi && layout.id != R.id.layoutkemandoranAbsensi) {

                val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
                val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)
                val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                val editText = layout.findViewById<EditText>(R.id.etHomeMarkerTPH)

                val isEmpty = when (inputType) {
                    FeatureAbsensiActivity.InputType.SPINNER -> {
                        when (layout.id) {
                            R.id.layoutEstateAbsensi -> estateName!!.isEmpty()
                            R.id.layoutAfdelingAbsensi -> selectedAfdeling.isEmpty()
                            R.id.layoutkemandoranAbsensi -> selectedKemandoran.isEmpty()
                            else -> spinner.selectedIndex == -1
                        }
                    }

                    FeatureAbsensiActivity.InputType.EDITTEXT -> {
                        when (key) {
                            else -> editText.text.toString().trim().isEmpty()
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

        // Check asistensi fields
        if (isAsistensiEnabled) {
            val layoutKemandoran = findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi)
            val layoutKemandoranLain = findViewById<LinearLayout>(R.id.layoutKemandoranLainAbsensi)
            val isKemandoranEmpty = selectedKemandoran.isEmpty()
            val isKemandoranLainEmpty = selectedKemandoranLain.isEmpty()

            if (isKemandoranLainEmpty || isKemandoranEmpty) {
                if (isKemandoranLainEmpty) {
                    layoutKemandoranLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                        View.VISIBLE
                    layoutKemandoranLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                        ContextCompat.getColor(this, R.color.colorRedDark)
                    missingFields.add(getString(R.string.field_kemandoran_lain))
                }
                if (isKemandoranEmpty) {
                    layoutKemandoran.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                        View.VISIBLE
                    layoutKemandoran.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                        ContextCompat.getColor(this, R.color.colorRedDark)
                    missingFields.add(getString(R.string.field_pemanen_lain))
                }
                isValid = false
            }
        }

        // Check photo count
        if (photoCount == 0) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_photo_minimal_one))
        }

        if (!isValid) {
            vibrate()
            // Create combined error message
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

        if (linearLayout.id == R.id.layoutkemandoranAbsensi || linearLayout.id == R.id.layoutKemandoranLainAbsensi) {
//            Spinner khusus saerch
            spinner.setOnTouchListener { _, event ->
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

        if (linearLayout.id == R.id.layoutEstateAbsensi) {
            spinner.isEnabled = false // Disable the spinner
        }
        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE
            handleItemSelection(linearLayout, position, item.toString())
        }
    }

    private fun resetDependentSpinners(rootView: View) {
        // List of all dependent layouts that need to be reset
        val dependentLayouts = listOf(
            R.id.layoutkemandoranAbsensi,
            R.id.layoutKemandoranLainAbsensi
        )

        // Reset each dependent spinner
        dependentLayouts.forEach { layoutId ->
            val layout = rootView.findViewById<LinearLayout>(layoutId)
            setupSpinnerView(layout, emptyList())
        }

        kemandoranList = emptyList()
        kemandoranLainList = emptyList()
        karyawanList = emptyList()
        karyawanLainList = emptyList()


        // Reset selected values
        selectedKemandoranLain = ""

        // Clear adapters if they exist
        selectedKemandoranAdapter.clearAllWorkers()
        selectedKemandoranLainAdapter.clearAllWorkers()

        // Ensure RecyclerView is cleared
        absensiAdapter.clearList()
    }

    private fun handleItemSelection(
        linearLayout: LinearLayout,
        position: Int,
        selectedItem: String
    ) {
        when (linearLayout.id) {
            R.id.layoutAfdelingAbsensi -> {
                resetDependentSpinners(linearLayout.rootView)
                selectedAfdeling = selectedItem.toString()
                selectedAfdelingIdSpinner = position

                val selectedDivisiId = try {
                    divisiList.find { it.divisi_abbr == selectedAfdeling }?.divisi
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedDivisiId: ${e.message}")
                    null
                }

                val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()
                selectedDivisionSpinnerIndex = position
                selectedDivisiValue = selectedDivisiId

                val allIdAfdeling = try {
                    divisiList.map { it.divisi }
                } catch (e: Exception) {
                    AppLogger.e("Error mapping allIdAfdeling: ${e.message}")
                    emptyList()
                }

                val otherDivisiIds = try {
                    allIdAfdeling.filter { divisiId ->
                        selectedDivisiId == null || divisiId != selectedDivisiId
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error filtering otherDivisiIds: ${e.message}")
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

                        val kemandoranDeferred = async {
                            try {
                                datasetViewModel.getKemandoranEstateExcept(
                                    estateId!!.toInt(),
                                    otherDivisiIds as List<Int>
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching kemandoranList: ${e.message}")
                                emptyList()
                            }
                        }

                        val kemandoranLainDeferred = async {
                            try {
                                datasetViewModel.getKemandoranEstate(
                                    estateId!!.toInt()
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching kemandoranLainList: ${e.message}")
                                emptyList()
                            }
                        }

                        kemandoranList = kemandoranDeferred.await()
                        kemandoranLainList = kemandoranLainDeferred.await()

                        withContext(Dispatchers.Main) {
                            try {
                                val layoutKemandoran =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi)
                                val layoutKemandoranLain =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoranLainAbsensi)

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
                                this@FeatureAbsensiActivity,
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

            R.id.layoutkemandoranAbsensi -> {
                selectedKemandoran = selectedItem.toString()
//                val selectedNama = selectedKemandoran.substringAfter(" - ")

                val selectedKemandoranObject = kemandoranList.find { it.nama == selectedKemandoran }

                // Now you can get both the id and kode
                val selectedKemandoranId = selectedKemandoranObject?.id
                selectedKemandoranKode = selectedKemandoranObject?.kode!!
                if (selectedKemandoranId != null) {
                    selectedKemandoranIds.add(selectedKemandoranId) // Tambahkan ke Set agar tidak duplikat

                    val worker = Worker(selectedKemandoranId.toString(), selectedKemandoran)
                    selectedKemandoranAdapter.addWorker(worker)

                    val availableWorkers = selectedKemandoranAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }

                    AppLogger.d("Selected Worker: $selectedKemandoran, ID: $selectedKemandoranId")
                }

                AppLogger.d(estateId.toString())
                AppLogger.d(selectedDivisiValue.toString())

//                filteredKemandoranId = try {
//                    kemandoranList.find {
//                        it.dept == estateId?.toIntOrNull() && // Avoids force unwrap (!!)
//                                it.divisi == selectedDivisiValue &&
//                                it.nama == selectedKemandoran
//                    }?.id
//                } catch (e: Exception) {
//                    AppLogger.e("Error finding Kemandoran ID: ${e.message}")
//                    null
//                }

                filteredKemandoranId.addAll(
                    kemandoranList.filter {
                        it.dept == estateId?.toIntOrNull() &&
                                it.divisi == selectedDivisiValue
                    }.map { it.id }
                )

                filteredKemandoranId.clear()
                filteredKemandoranId.addAll(selectedKemandoranIds)

                if (filteredKemandoranId != null) {
                    AppLogger.d("Filtered Kemandoran ID: $filteredKemandoranId")

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }

                        try {
//
                            val karyawanDeferred = async {
                                filteredKemandoranId.map { id ->
                                    async { datasetViewModel.getKaryawanList(id) }
                                }.awaitAll().flatten()
                            }


                            karyawanList = karyawanDeferred.await()

                            AppLogger.d(karyawanList.toString())
                            // When adding items to the adapter, include the kemandoran name
                            val kemandoranName = selectedKemandoran // This should be the name of the selected kemandoran
                            val absensiList = karyawanList.sortedBy { it.nama }.map { karyawan ->
                                AbsensiDataList(
                                    id = karyawan.id!!,
                                    nama = "${karyawan.nik}\n${karyawan.nama}",
                                    namaOnly = karyawan.nama!!,
                                    nik = karyawan.nik!!,
                                    kemandoranId = selectedKemandoranId!!
                                )
                            }

                            AppLogger.d("absensiList $absensiList")

                            withContext(Dispatchers.Main) {
                                absensiAdapter.updateList(absensiList, append = true, kemandoranName = kemandoranName)
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching afdeling data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeatureAbsensiActivity,
                                    "Error loading kemandoran: ${e.message}",
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

            R.id.layoutKemandoranLainAbsensi -> {
                selectedKemandoranLain = selectedItem.toString()

                val kemandoranMap = kemandoranLainList.associateBy({ it.nama }, { it.id })
                val selecteKemandoranLainId = kemandoranMap[selectedKemandoranLain]

                if (selecteKemandoranLainId != null) {
                    selectedKemandoranIdsLain.add(selecteKemandoranLainId) // Tambahkan ke set agar unik

                    val worker = Worker(selecteKemandoranLainId.toString(), selectedKemandoranLain)
                    selectedKemandoranLainAdapter.addWorker(worker)

                    val availableWorkers = selectedKemandoranLainAdapter.getAvailableWorkers()
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(linearLayout, availableWorkers.map { it.name })
                    }

                    AppLogger.d("Selected Worker: $selectedKemandoranLain, ID: $selecteKemandoranLainId")
                }

                // Hapus isi sebelumnya agar tidak menumpuk
                filteredKemandoranIdLain.clear()
                filteredKemandoranIdLain.addAll(selectedKemandoranIdsLain)

                AppLogger.d("Filtered Kemandoran Lain ID: $filteredKemandoranIdLain")

                // Ambil data karyawan hanya jika ada ID kemandoran lain yang dipilih
                if (filteredKemandoranIdLain.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // Simulasi loading
                        }
                        try {
                            val karyawanDeferred = async {
                                filteredKemandoranIdLain.map { id ->
                                    async { datasetViewModel.getKaryawanList(id) }
                                }.awaitAll().flatten()
                            }

                            karyawanLainList = karyawanDeferred.await()

                            val karyawanLainNames = karyawanLainList.map { it.nama }
                            val kemandoranName = selectedKemandoranLain // This should be the name of the selected kemandoran
                            val absensiLainList = karyawanLainList.sortedBy { it.nama }.map { karyawan ->
                                AbsensiDataList(
                                    id = karyawan.id!!,
                                    nama = "${karyawan.nik}\n${karyawan.nama}",
                                    namaOnly = karyawan.nama!!,
                                    nik = karyawan.nik!!,
                                    kemandoranId = selecteKemandoranLainId!!
                                )
                            }

                            withContext(Dispatchers.Main) {
                                AppLogger.d(absensiLainList.toString())
                                absensiAdapter.updateList(absensiLainList, append = true,kemandoranName = kemandoranName)
                            }



                            AppLogger.d("Jumlah data di adapter: ${absensiAdapter.itemCount}")
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching afdeling data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeatureAbsensiActivity,
                                    "Error loading kemandoran lain: ${e.message}",
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
                    AppLogger.e("Filtered Kemandoran ID is empty, skipping data fetch.")
                }
            }

        }
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            cameraViewModel.statusCamera() -> {
                // If in camera mode, close camera and return to previous screen
                cameraViewModel.closeCamera()
            }

            else -> {
                backButton.isEnabled = false
                vibrate()
                AlertDialogUtility.withTwoActions(
                    this,
                    "Keluar",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_feature),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {
                        val intent = Intent(this, HomePageActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                        backButton.isEnabled = true
                    }, cancelFunction = {
                        backButton.isEnabled = true
                    }
                )

            }
        }

    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()
        locationViewModel.locationPermissions.observe(this) { isLocationEnabled ->
            if (!isLocationEnabled) {
                requestLocationPermission()
            } else {
                locationViewModel.startLocationUpdates()
            }
        }

        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude

        }

        locationViewModel.locationAccuracy.observe(this) { accuracy ->
            findViewById<TextView>(R.id.accuracyLocation).text = String.format("%.1f m", accuracy)

            currentAccuracy = accuracy
        }

        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun getCurrentLocationData(): TakeFotoPreviewAdapter.LocationData {
        return TakeFotoPreviewAdapter.LocationData(
            estate = prefManager!!.estateUserLogin,
            afdeling = selectedAfdeling,
            blok = "",
            tph = ""
        )
    }

    override fun getCurrentCoordinates(): Pair<Double?, Double?> {
        return Pair(lat, lon)
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

    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
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

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.rcFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

        adapter?.addPhotoFile("$position", photoFile)

        if (position < photoFiles.size) {
            // Position exists, replace it
            photoFiles[position] = fname
            komentarFoto[position] = komentar ?: ""
        } else {
            while (photoFiles.size < position) {
                photoFiles.add("")
                komentarFoto.add("")
            }
            photoFiles.add(fname)
            komentarFoto.add(komentar ?: "")
            photoCount++
        }

        finalLat = latitude
        finalLon = longitude

        val viewHolder =
            recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
        viewHolder?.let {
            Glide.with(this)
                .load(photoFile)
                .into(it.imageView)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbar("Location permission is required for this app. Change in Settings App")
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
                showSnackbar("Location permission denied.")
            }
        }

}