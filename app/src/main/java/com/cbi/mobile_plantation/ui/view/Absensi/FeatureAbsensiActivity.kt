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
//import com.cbi.cmp_project.data.model.KemandoranDetailModel
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
//import com.cbi.cmp_project.utils.DataCacheManager
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

open class FeatureAbsensiActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    private var photoCount = 0
    private val photoFiles = mutableListOf<String>() // Store filenames
    private val komentarFoto = mutableListOf<String>() // Store filenames

    private var lat: Double? = null
    private var lon: Double? = null
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


    private lateinit var selectedKemandoranAdapter: SelectedWorkerAdapter
    private lateinit var selectedKemandoranLainAdapter: SelectedWorkerAdapter
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
    private var filteredKemandoranId: Int? = null
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
//                mbSaveDataAbsensi.isEnabled = false
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
                                    val (karyawanMasuk, karyawanTidakMasuk) = absensiList.partition { it.isChecked }

                                    val karyawanMskId =
                                        karyawanMasuk.joinToString(",") { it.id.toString() }
                                    val karyawanTdkMskId =
                                        karyawanTidakMasuk.joinToString(",") { it.id.toString() }

                                    val dateAbsen =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            Date()
                                        )

                                    val karyawanMskIdList =
                                        karyawanMskId.split(",") // Konversi ke List<String>
                                    val isDuplicate = absensiViewModel.isAbsensiExist(
                                        dateAbsen,
                                        karyawanMskIdList
                                    )
                                    if (isDuplicate) {
                                        return@withContext SaveDataAbsensiState.Error("Data absensi sudah ada untuk sebagian karyawan.")
                                    }

                                    AppLogger.d("Tgl ${dateAbsen} + idKar ${karyawanMskId}")

                                    val listKemandoran = listOfNotNull(
                                        filteredKemandoranId,
                                        selectedKemandoranIdLainAbsensi
                                    )
                                        .joinToString(",")

                                    val photoFilesString = photoFiles.joinToString(";")
                                    val komentarFotoString = komentarFoto.joinToString(";")

                                    absensiViewModel.saveDataAbsensi(
                                        kemandoran_id = listKemandoran,
                                        date_absen = dateAbsen,
                                        created_by = userId!!,
                                        karyawan_msk_id = karyawanMskId,
                                        karyawan_tdk_msk_id = karyawanTdkMskId,
                                        foto = photoFilesString,
                                        komentar = komentarFotoString,
                                        asistensi = asistensi ?: 0,
                                        lat = lat ?: 0.0,
                                        lon = lon ?: 0.0,
                                        info = infoApp ?: "",
                                        archive = 0
                                    )
                                }

                                when (result) {
                                    is SaveDataAbsensiState.Success -> {
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
        selectedKemandoranAdapter = SelectedWorkerAdapter()
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
        selectedKemandoranLainAdapter = SelectedWorkerAdapter()
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

        takeFotoPreviewAdapter = TakeFotoPreviewAdapter(1, cameraViewModel, this, featureName)
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

                val nonSelectedAfdelingKemandoran = try {
                    divisiList.filter { it.divisi_abbr != selectedAfdeling }
                } catch (e: Exception) {
                    AppLogger.e("Error filtering nonSelectedAfdelingKemandoran: ${e.message}")
                    emptyList()
                }

                val nonSelectedIdAfdeling = try {
                    nonSelectedAfdelingKemandoran.map { it.divisi }
                } catch (e: Exception) {
                    AppLogger.e("Error mapping nonSelectedIdAfdeling: ${e.message}")
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
                                    nonSelectedIdAfdeling as List<Int>
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

                val kemandoranMap = kemandoranList.associateBy({ it.nama }, { it.id })

                val selecteKemandoranId = kemandoranMap[selectedKemandoran]
                if (selecteKemandoranId != null) {
                    val worker = Worker(selecteKemandoranId.toString(), selectedKemandoran)
                    selectedKemandoranAdapter.addWorker(worker)

                    val availableWorkers = selectedKemandoranAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }

                    AppLogger.d("Selected Worker: $selectedKemandoran, ID: $selecteKemandoranId")
                }

                AppLogger.d(estateId.toString())
                AppLogger.d(selectedDivisiValue.toString())

                filteredKemandoranId = try {
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
                                datasetViewModel.getKaryawanList(filteredKemandoranId!!)
                            }

                            karyawanList = karyawanDeferred.await()
                            val karyawanNames = karyawanList.map { it.nama }

                            AppLogger.d(karyawanNames.toString())
                            AppLogger.d(karyawanList.toString())

                            val absensiList = karyawanList.map { karyawan ->
                                AbsensiDataList(
                                    id = karyawan.id!!,
                                    nama = "${karyawan.nik} - ${karyawan.nama}",
                                    jabatan = karyawan.jabatan ?: "-" // Tangani null
                                )
                            }
                            withContext(Dispatchers.Main) {
                                absensiAdapter.updateList(absensiList, append = true)
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
                    val worker = Worker(selecteKemandoranLainId.toString(), selectedKemandoranLain)
                    selectedKemandoranLainAdapter.addWorker(worker)

                    val availableWorkers = selectedKemandoranLainAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })  // Extract names
                    }

                    AppLogger.d("Selected Worker: $selectedKemandoranLain, ID: $selecteKemandoranLainId")
                }

                AppLogger.d(kemandoranLainList.toString())
                selectedKemandoranIdLainAbsensi = try {
                    kemandoranLainList.find {
                        it.nama == selectedKemandoranLain
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding Kemandoran ID: ${e.message}")
                    null
                }

                AppLogger.d(selectedKemandoranIdLainAbsensi.toString())

                if (selectedKemandoranIdLainAbsensi != null) {
                    AppLogger.d("Filtered Kemandoran Lain ID: $selectedKemandoranIdLainAbsensi")

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }
                        try {
                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(selectedKemandoranIdLainAbsensi!!)
                            }
                            karyawanLainList = karyawanDeferred.await()

                            val karyawanLainNames = karyawanLainList.map { it.nama }
                            AppLogger.d(karyawanLainNames.toString())
                            AppLogger.d(karyawanLainList.toString())

                            val absensiLainList = karyawanLainList.map { karyawan ->
                                AbsensiDataList(
                                    id = karyawan.id!!,
                                    nama = "${karyawan.nik} - ${karyawan.nama}",
                                    jabatan = karyawan.jabatan ?: "-" // Tangani null
                                )
                            }
                            withContext(Dispatchers.Main) {
                                AppLogger.d(absensiLainList.toString())
                                absensiAdapter.updateList(absensiLainList, append = true)
                            }
                            AppLogger.d("Jumlah data di adapter: ${absensiAdapter.itemCount}")
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
        komentar: String?
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.rcFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

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