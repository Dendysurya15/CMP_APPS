package com.cbi.cmp_project.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.BUnitCodeModel
import com.cbi.cmp_project.data.model.CompanyCodeModel
import com.cbi.cmp_project.data.model.DivisionCodeModel
import com.cbi.cmp_project.data.model.FieldCodeModel
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.TPHModel
import com.cbi.cmp_project.data.model.WorkerGroupModel
import com.cbi.cmp_project.data.model.WorkerInGroupModel
import com.cbi.cmp_project.data.model.WorkerModel
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.cmp_project.ui.adapter.SelectedWorkerAdapter
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeFragment
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.DataCacheManager
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.reflect.KMutableProperty0

open class FeaturePanenTBSActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    private var jumTBS = 0
    var tbsMasak = 0
    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var tidakVcut = 0
    private var lat: Double? = null
    private var lon: Double? = null
    var currentAccuracy : Float = 0F

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var panenTBSViewModel: PanenTBSViewModel
    private var locationEnable:Boolean = false
    private var isPermissionRationaleShown = false

    private var regionalList: List<RegionalModel> = emptyList()
    private var wilayahList: List<WilayahModel> = emptyList()
    private var deptList: List<DeptModel> = emptyList()
    private var divisiList: List<DivisiModel> = emptyList()
    private var blokList: List<BlokModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranDetailList: List<KemandoranDetailModel> = emptyList()
    private var tphList: List<TPHNewModel>? = null // Lazy-loaded

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var selectedWorkerAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedWorkers: RecyclerView
    private lateinit var dataCacheManager: DataCacheManager
    private var selectedKemandoranValue: Int? = null

    enum class InputType {
        SPINNER,
    }
    private var selectedRegional: String = ""
    private var selectedWilayah: String = ""
    private var selectedEstate: String = ""
    private var selectedAfdeling: String = ""
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""


    private var selectedRegionalValue: Int? = null
    private var selectedWilayahValue: Int? = null
    private var selectedEstateValue: Int? = null
    private var selectedDivisiValue: Int? = null
    private var selectedBlokValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedFieldCodeValue: Int? = null
    private var selectedTPHValue: Int? = null

    private var selectedDivisionSpinnerIndex: Int? = null
    private var selectedRegionalSpinnerIndex: Int? = null
    private var selectedWilayahSpinnerIndex: Int? = null
    private var selectedEstateSpinnerIndex: Int? = null
    private var selectedBUnitSpinnerIndex: Int? = null
    private var selectedFieldCodeSpinnerIndex: Int? = null
    private var selectedTPHSpinnerIndex: Int? = null

    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0



    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        loadingDialog = LoadingDialog(this)
        dataCacheManager = DataCacheManager(this)
        initViewModel()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()  // Show loading at start
                loadingDialog.setMessage("Loading data...")
            }

            try {
                val cachedData = dataCacheManager.getDatasets()

                if (cachedData != null) {




                    val hasEmptyDatasets = listOf(
                        "regionalList" to cachedData.regionalList,
                        "wilayahList" to cachedData.wilayahList,
                        "deptList" to cachedData.deptList,
                        "divisiList" to cachedData.divisiList,
                        "blokList" to cachedData.blokList,
                        "tphList" to cachedData.tphList,
                        "karyawanList" to cachedData.karyawanList,
                        "kemandoranList" to cachedData.kemandoranList,
                        "kemandoranDetailList" to cachedData.kemandoranDetailList
                    ).map { (name, list) ->
                        if (list.isEmpty()) {
                            AppLogger.d("$name is empty.")
                            true
                        } else {
                            AppLogger.d("$name has data with size: ${list.size}.")
                            false
                        }
                    }.any { it }

                    if (hasEmptyDatasets) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            AlertDialogUtility.alertDialogAction(
                                this@FeaturePanenTBSActivity,
                                "Terjadi Kesalahan!",
                                "Dataset Gagal di-load! Mohon mengunduh data di halaman home",
                                "warning.json"
                            ) {
                            }
                        }
                    } else {
                        regionalList = cachedData.regionalList
                        wilayahList = cachedData.wilayahList
                        deptList = cachedData.deptList
                        divisiList = cachedData.divisiList
                        blokList = cachedData.blokList
                        tphList = cachedData.tphList
                        karyawanList = cachedData.karyawanList
                        kemandoranList = cachedData.kemandoranList
                        kemandoranDetailList = cachedData.kemandoranDetailList

                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            setupLayout()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()

                    }
                }
            } catch (e: Exception) {
                Log.e("DataLoading", "Error loading data: ${e.message}")
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                }
            }
        }



        val mbSaveDataPanenTBS = findViewById<MaterialButton>(R.id.mbSaveDataPanenTBS)

        mbSaveDataPanenTBS.setOnClickListener{

            AlertDialogUtility.withTwoActions(
                this,
                "Simpan",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.confirmation_dialog_description),
                "warning.json"
            ) {

                panenTBSViewModel.insertPanenTBSVM(
                    user_id = 1,
                    tanggal = "2024-12-26",
                    name = "John Doe",
                    estate = "Estate 1",
                    id_estate = 101,
                    afdeling = "Afdeling A",
                    id_afdeling = 202,
                    blok = "Blok X",
                    id_blok = 303,
                    tahun_tanam = 2020,
                    id_tt = 404,
                    ancak = "No",
                    id_ancak = 505,
                    tph = "TPH-1",
                    id_tph = 606,
                    jenis_panen = "Harvest Type A",
                    list_pemanen = "John, Alice, Bob",
                    list_idpemanen = "1,2,3",
                    tbs = 1000,
                    tbs_mentah = 200,
                    tbs_lewatmasak = 100,
                    tks = 50,
                    abnormal = 10,
                    tikus = 5,
                    tangkai_panjang = 30,
                    vcut = 5,
                    tbs_masak = 900,
                    tbs_dibayar = 800,
                    tbs_kirim = 750,
                    latitude = "1.234567",
                    longitude = "103.456789",
                    foto = "image_url.jpg"
                )

                panenTBSViewModel.insertDBPanenTBS.observe(this) { isInserted ->

                    Log.d("testing", isInserted.toString())
                    if (isInserted){
                        AlertDialogUtility.alertDialogAction(
                            this,
                            "Sukses",
                            "Data berhasil disimpan!",
                            "success.json"
                        ) {
                            Toast.makeText(
                                this,
                                "sukses bro",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }else{
                        Toast.makeText(
                            this,
                            "Gagal bro",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


            }

        }
    }



    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }

    private fun initializeCounters() {
        updateCounterTextViews() // This will set all counters to "0 Buah" initially
    }

    private fun updateCounterTextViews() {
        findViewById<TextView>(R.id.tvCounterBuahMasak).text = "$buahMasak Buah"
        findViewById<TextView>(R.id.tvCounterKirimPabrik).text = "$kirimPabrik Buah"
        findViewById<TextView>(R.id.tvCounterTBSDibayar).text = "$tbsDibayar Buah"
    }

    private fun updateDependentCounters(layoutId: Int, change: Int) {
        when (layoutId) {
            R.id.layoutJumTBS -> {
                if (change > 0) {
                    jumTBS += 1
                    formulas()
                } else { // When decrementing
                    if (tbsMasak > 0) {
                        jumTBS -= 1
                        formulas()
                    }
                }
            }
            R.id.layoutBMentah -> {
                if (change > 0) {
                    if (tbsMasak > 0) {
                        bMentah += 1
                        formulas()
                    }
                } else { // When decrementing
                    if (bMentah > 0) {
                        bMentah -= 1
                        formulas()
                    }
                }
            }
            R.id.layoutBLewatMasak -> {
                // Case 3: When BLewatMasak changes, subtract from all EXCEPT buahMasak

            }
            R.id.layoutJjgKosong -> {

            }
        }
        updateCounterTextViews()
    }

    private fun formulas(){
        buahMasak = jumTBS - jjgKosong - bMentah - bLewatMasak

        Log.d("testing", buahMasak.toString())
        bMentah = jumTBS - bLewatMasak - jjgKosong - tbsMasak
        bLewatMasak = jumTBS - bMentah - tbsMasak - jjgKosong
        jjgKosong = jumTBS - bMentah - tbsMasak - bLewatMasak
        tbsDibayar = jumTBS - bMentah - jjgKosong
        kirimPabrik = jumTBS - jjgKosong - abnormal
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
            LocationViewModel.Factory(application,status_location, this)
        )[LocationViewModel::class.java]
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

    /**
     * Sets up all spinner mappings, counters, and the RecyclerView.
     */
    private fun setupLayout() {

        inputMappings = listOf(
            Triple(findViewById<LinearLayout>(R.id.layoutRegional), getString(R.string.field_regional), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutWilayah), getString(R.string.field_wilayah), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutEstate), getString(R.string.field_estate), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAfdeling), getString(R.string.field_afdeling), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTahunTanam), getString(R.string.field_tahun_tanam), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutBlok), getString(R.string.field_blok), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAncak), getString(R.string.field_ancak), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTipePanen), getString(R.string.field_tipe_panen), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutNoTPH), getString(R.string.field_no_tph), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutKemandoran), getString(R.string.field_kemandoran), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutPemanen), getString(R.string.field_pemanen), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutKemandoranLain), getString(R.string.field_kemandoran_lain), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutPemanenLain), getString(R.string.field_pemanen_lain), InputType.SPINNER)
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.layoutRegional -> {
                            val bUnitNames = regionalList.map { it.nama }
                            Log.d("testing", bUnitNames.toString())
                            setupSpinnerView(layoutView, bUnitNames)
                        }
                        R.id.layoutTipePanen->{
                            val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
                            setupSpinnerView(layoutView, tipePanenOptions)
                        }
                        else -> {
                            setupSpinnerView(layoutView, emptyList())
                        }

                    }
                }
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
            Triple(R.id.layoutTidakVcut, "Tidak V-Cut", ::tidakVcut)
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPaneWithButtons(layoutId, R.id.tvNumberPanen, labelText, counterVar)
        }

        rvSelectedWorkers = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.top_margin)  // Or direct pixels: 32
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        selectedWorkerAdapter = SelectedWorkerAdapter()
        rvSelectedWorkers.adapter = selectedWorkerAdapter


        val layoutPemanen = findViewById<LinearLayout>(R.id.layoutPemanen)
        val parentLayout = layoutPemanen.parent as ViewGroup
        val index = parentLayout.indexOfChild(layoutPemanen)
        parentLayout.addView(rvSelectedWorkers, index + 1)
        setupRecyclerViewTakePreviewFoto()
        setupSwitch()
    }

    fun resetViewsBelow(triggeredLayout: Int) {
        when (triggeredLayout) {
            R.id.layoutRegional -> {
                clearSpinnerView(R.id.layoutWilayah, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutEstate, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(R.id.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
            R.id.layoutWilayah -> {
                clearSpinnerView(R.id.layoutEstate, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(R.id.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
            R.id.layoutEstate -> {
                clearSpinnerView(R.id.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(R.id.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(R.id.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
            R.id.layoutAfdeling -> {
                clearSpinnerView(R.id.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(R.id.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
                clearSpinnerView(R.id.layoutKemandoran, ::resetSelectedKemandoran)
            }
            R.id.layoutTahunTanam -> {
                clearSpinnerView(R.id.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
            R.id.layoutBlok -> {
                clearSpinnerView(R.id.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
            R.id.layoutAncak -> {
                clearSpinnerView(R.id.layoutNoTPH, ::resetSelectedTPH)
            }
        }
    }

    fun clearSpinnerView(layoutId: Int, resetSelectedValue: () -> Unit) {
        val layoutView = findViewById<LinearLayout>(layoutId)
        if (layoutId != R.id.layoutWilayah) {
            layoutView.visibility = View.GONE
        }
        setupSpinnerView(layoutView, emptyList()) // Pass an empty list to reset the spinner
        resetSelectedValue() // Reset the associated selected value
    }

    // Functions to reset selected values
    fun resetSelectedDivisionCode() {
        selectedDivisionCodeValue = null
    }

    fun resetSelectedTahunTanam() {
        selectedTahunTanamValue = null
    }

    fun resetSelectedFieldCode() {
        selectedFieldCodeValue = null
    }

    fun resetSelectedAncak() {
//        selectedAncakValue = null
    }

    fun resetSelectedTPH() {
        // Assuming you have a variable for TPH selection
        selectedTPHValue = null
    }

    fun resetSelectedKemandoran(){
        selectedKemandoranValue = null
    }

    private fun setupSpinnerView(linearLayout: LinearLayout, data: List<String>, onItemSelected: (Int) -> Unit = {}) {
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)
        spinner.setTextSize(18f)
        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE

            when(linearLayout.id){
                R.id.layoutRegional->{
                    resetViewsBelow(R.id.layoutRegional)
                    selectedRegional = item.toString()

                    val selectedRegionalId = regionalList.find { it.nama == selectedRegional }?.id
                    selectedRegionalValue = selectedRegionalId
                    selectedRegionalSpinnerIndex = position

                    if (selectedRegionalId != null) {
                        val filteredWilayahList = wilayahList.filter { it.regional == selectedRegionalId }
                        val wilayahNames = filteredWilayahList.map { it.nama }

                        val layoutWilayah = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutWilayah)

                        if (wilayahNames.isNotEmpty()) {
                            setupSpinnerView(layoutWilayah, wilayahNames)
                            layoutWilayah.visibility = View.VISIBLE
                        } else {
                            layoutWilayah.visibility = View.GONE
                        }
                    } else {
                        val layoutWilayah = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutWilayah)
                        layoutWilayah.visibility = View.GONE
                    }

                }
                R.id.layoutWilayah -> {
                    resetViewsBelow(R.id.layoutWilayah)
                    selectedWilayah = item.toString()

                    val selectedWilayahId = wilayahList.find { it.nama == selectedWilayah }?.id
                    selectedWilayahValue = selectedWilayahId
                    selectedWilayahSpinnerIndex = position
                    val layoutEstate = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutEstate)
                    if (selectedWilayahId != null) {
                        val filteredDeptList = deptList.filter { it.regional == selectedRegionalValue && it.wilayah == selectedWilayahValue }
                        val deptCodeNames = filteredDeptList.map { it.nama }


                        if (deptCodeNames.isNotEmpty()) {


                            setupSpinnerView(layoutEstate, deptCodeNames)
                            layoutEstate.visibility = View.VISIBLE
                        } else {
                            layoutEstate.visibility = View.GONE
                        }
                    } else {
                        layoutEstate.visibility = View.GONE
                    }
                }
                R.id.layoutEstate -> {
                    selectedEstateValue = null
                    resetViewsBelow(R.id.layoutEstate)
                    selectedEstate = item.toString()
                    val selectedEstateId = deptList.find { it.nama == selectedEstate  && it.regional == selectedRegionalValue && it.wilayah == selectedWilayahValue}?.id
                    selectedEstateValue = selectedEstateId
                    selectedEstateSpinnerIndex = position
                    val layoutAfdeling = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutAfdeling)
                    if (selectedEstateId != null){
                        val filteredDivisiList = divisiList.filter{it.dept == selectedEstateId}
                        val divisiCodeNames = filteredDivisiList.map{it.abbr}
                        if (divisiCodeNames.isNotEmpty()){
                            setupSpinnerView(layoutAfdeling, divisiCodeNames)
                            layoutAfdeling.visibility = View.VISIBLE
                        }else{
                            layoutAfdeling.visibility = View.GONE
                        }
                    }else{
                        layoutAfdeling.visibility = View.GONE
                    }

                }
                R.id.layoutAfdeling -> {
                    resetViewsBelow(R.id.layoutAfdeling)
                    selectedAfdeling = item.toString()

                    // Get the selected division ID
                    val selectedDivisiId = divisiList.find { it.abbr == selectedAfdeling && it.dept == selectedEstateValue }?.id
                    selectedDivisionSpinnerIndex = position
                    selectedDivisiValue = selectedDivisiId

                    val layoutTahunTanam = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutTahunTanam)
                    val layoutKemandoran = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoran)
                    val layoutKemandoranLain = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoranLain)

                    if (selectedDivisiId != null) {
                        // Filter the Blok list for Tahun Tanam
                        val filteredBlokList = blokList.filter {
                            it.regional == selectedRegionalValue &&
                                    it.dept == selectedEstateValue &&
                                    it.divisi == selectedDivisiId
                        }

                        val tahunTanamList = filteredBlokList.map { it.tahun }.distinct().sorted()

                        if (tahunTanamList.isNotEmpty()) {
                            setupSpinnerView(layoutTahunTanam, tahunTanamList)
                            layoutTahunTanam.visibility = View.VISIBLE
                        } else {
                            layoutTahunTanam.visibility = View.GONE
                        }
                    } else {
                        layoutTahunTanam.visibility = View.GONE
                    }

                    // Filter the Kemandoran list for the selected Afdeling
                    val filteredKemandoranList = kemandoranList.filter {
                        it.dept == selectedEstateValue &&
                                it.divisi == selectedDivisiValue
                    }
                    val kemandoranNames = filteredKemandoranList.map { it.nama }

                    Log.d("testing", kemandoranNames.toString())
                    if (kemandoranNames.isNotEmpty()) {
                        setupSpinnerView(layoutKemandoran, kemandoranNames)
                        layoutKemandoran.visibility = View.VISIBLE
                    } else {
                        layoutKemandoran.visibility = View.GONE
                    }

                    // Filter the non-selected Afdeling
                    val filteredDivisiList = divisiList.filter { it.dept == selectedEstateValue }
                    val divisiCodeNames = filteredDivisiList.map { it.abbr }
                    val nonSelectedAfdelingKemandoran = divisiCodeNames.filter { it != selectedAfdeling }

                    if (nonSelectedAfdelingKemandoran.isNotEmpty()) {
                        setupSpinnerView(layoutKemandoranLain, nonSelectedAfdelingKemandoran)
                        layoutKemandoranLain.visibility = View.VISIBLE
                    } else {
                        layoutKemandoranLain.visibility = View.GONE
                    }

                }
                R.id.layoutTahunTanam->{
                    val selectedTahunTanam = item.toString()
                    resetViewsBelow(R.id.layoutTahunTanam)
                    selectedTahunTanamValue = selectedTahunTanam
                    val filteredBlokCodes = blokList.filter { it ->
                        it.regional == selectedRegionalValue && it.dept == selectedEstateValue && it.divisi == selectedDivisiValue  && it.tahun == selectedTahunTanamValue
                    }
                    val layoutBlok = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutBlok)
                    if (filteredBlokCodes.isNotEmpty()) {
                        val blokNames = filteredBlokCodes.map { it.kode }
                        setupSpinnerView(layoutBlok, blokNames)
                        layoutBlok.visibility = View.VISIBLE
                    } else {
                        layoutBlok.visibility = View.GONE
                    }
                }
                R.id.layoutBlok -> {
                    resetViewsBelow(R.id.layoutBlok)
//                    binding.layoutAncak.root.visibility = View.VISIBLE
                    selectedBlok = item.toString()
                    selectedFieldCodeSpinnerIndex = position
                    val selectedFieldId = blokList.find { it.regional == selectedRegionalValue && it.tahun == selectedTahunTanamValue &&   it.kode == selectedBlok && it.dept == selectedEstateValue && it.divisi == selectedDivisiValue  }?.id
                    selectedBlokValue = selectedFieldId

//                    Log.d("testing", selectedRegionalValue.toString())
//                    Log.d("testing", selectedEstateValue.toString())
//                    Log.d("testing", selectedDivisiValue.toString())
//                    Log.d("testing", selectedBlok.toString())
//                    Log.d("testing", selectedBlokValue.toString())
//                    Log.d("testing", selectedTahunTanamValue.toString())
                    val layoutNoTPH = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutNoTPH)
                    lifecycleScope.launch {
                        val filteredTPH = withContext(Dispatchers.Default) {
                            tphList?.filter { tph ->
                                tph.regional == selectedRegionalValue &&
                                        tph.dept == selectedEstateValue &&
                                        tph.divisi == selectedDivisiValue &&
                                        tph.tahun == selectedTahunTanamValue &&
                                        tph.blok == selectedBlokValue
                            }
                        }


                        if (!filteredTPH.isNullOrEmpty()) {
                            val tphNumbers = filteredTPH.map { it.nomor }
                            setupSpinnerView(layoutNoTPH, tphNumbers)
                            layoutNoTPH.visibility = View.VISIBLE
                        } else {
                            layoutNoTPH.visibility = View.VISIBLE
                        }
                    }
                }

                R.id.layoutNoTPH ->{
                    selectedTPH = item.toString()
                    selectedTPHSpinnerIndex = position
                    val selectedTPHId = tphList!!.find {
                        it.regional == selectedRegionalValue &&
                                it.dept == selectedEstateValue &&
                                it.divisi == selectedDivisiValue &&
                                it.blok == selectedBlokValue &&
                                it.tahun == selectedTahunTanamValue &&
                                it.nomor == selectedTPH
                    }
                    selectedTPHValue = selectedTPHId?.id


                }
                R.id.layoutKemandoran -> {
                    selectedWorkerAdapter.clearAllWorkers()
                    val selectedKemandoran = item.toString()
                    val filteredKemandoranId = kemandoranList.find {
                        it.dept == selectedEstateValue &&
                                it.divisi == selectedDivisiValue
                                it.nama == selectedKemandoran
                    }?.id


                    val filteredKemandoranDetails  = kemandoranDetailList.filter { it ->
                        it.header == filteredKemandoranId
                    }

                    val matchingKaryawanList = karyawanList.filter { karyawan ->
                        filteredKemandoranDetails.any { detail ->
                            detail.header == karyawan.id // Assuming `header` is a field in KaryawanModel
                        }
                    }
                    val layoutPemanen = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                    if (matchingKaryawanList.isNotEmpty()) {
                        val karyawanNames = matchingKaryawanList.map { it.nama }
                        setupSpinnerView(layoutPemanen, karyawanNames)
                        layoutPemanen.visibility = View.VISIBLE
                    } else {
                        AppLogger.d("No matching karyawan found for the given filters.")
                        layoutPemanen.visibility = View.GONE
                    }

                }
                R.id.layoutPemanen -> {
                    val selectedWorker = item.toString()
                    selectedWorkerAdapter.addWorker(selectedWorker)
                    setupSpinnerView(linearLayout, selectedWorkerAdapter.getAvailableWorkers())

                    val asistensiLayoutView = findViewById<LinearLayout>(R.id.layoutSelAsistensi)
                    asistensiLayoutView.visibility = View.VISIBLE
                }


                }
        }
    }

    private fun setupSwitch() {
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)
        val layoutKemandoranLain = findViewById<LinearLayout>(R.id.layoutKemandoranLain)
        val layoutPemanenLain = findViewById<LinearLayout>(R.id.layoutPemanenLain)

        switchAsistensi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show layouts when switch is ON
                layoutKemandoranLain.visibility = View.VISIBLE
                layoutPemanenLain.visibility = View.VISIBLE

                // Setup spinner for KemandoranLain if needed
//                setupSpinnerView(layoutKemandoranLain, workerGroupList.map { it.name })
            } else {
                // Hide layouts when switch is OFF
                layoutKemandoranLain.visibility = View.GONE
                layoutPemanenLain.visibility = View.GONE

            }
        }
    }

    /**
     * Configures the RecyclerView to repeat the layout 3 times.
     */
    private fun setupRecyclerViewTakePreviewFoto() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFotoPreview)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val itemWidth = resources.getDimensionPixelSize(R.dimen.item_width)

        val spanCount = if (itemWidth > 0) (screenWidth / itemWidth).coerceAtLeast(1) else 1

        val layoutManager = GridLayoutManager(this, spanCount)
        recyclerView.layoutManager = layoutManager

        val adapter = TakeFotoPreviewAdapter(3, cameraViewModel, this, featureName)
        recyclerView.adapter = adapter
    }

    /**
     * Updates the text of a spinner's label in the included layout.
     */
//    private fun updateTextInPertanyaanSpinner(layoutId: Int, textViewId: Int, newText: String) {
//        val includedLayout = findViewById<View>(layoutId)
//        val textView = includedLayout.findViewById<TextView>(textViewId)
//        textView.text = newText
//    }

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }

    /**
     * Sets up a layout with increment and decrement buttons for counters.
     */
    private fun setupPaneWithButtons(layoutId: Int, textViewId: Int, labelText: String, counterVar: KMutableProperty0<Int>) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)

        fun syncCounterWithEditText() {
            val enteredValue = etNumber.text.toString().toIntOrNull()
            if (enteredValue != null) {
                counterVar.set(enteredValue)
            }
        }

        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }
        }

        fun changeEditTextStyle(isNegativeOrZero: Boolean) {
            if (isNegativeOrZero) {
                val redColor = ContextCompat.getColor(this, R.color.colorRedDark)
                etNumber.setTextColor(ColorStateList.valueOf(redColor))
                etNumber.setTypeface(null, Typeface.BOLD)
            } else {
                etNumber.setTextColor(ColorStateList.valueOf(Color.BLACK))
                etNumber.setTypeface(null, Typeface.NORMAL)
            }
        }

        btDec.setOnClickListener {
            syncCounterWithEditText()
            if (counterVar.get() > 0) {
                counterVar.set(counterVar.get() - 1)
                etNumber.setText(counterVar.get().toString())
                updateDependentCounters(layoutId, -1)
            } else {
                vibrate()
                changeEditTextStyle(counterVar.get() <= 0)
            }
        }

        btInc.setOnClickListener {
            syncCounterWithEditText()
            counterVar.set(counterVar.get() + 1)
            etNumber.setText(counterVar.get().toString())
            updateDependentCounters(layoutId, 1)
            changeEditTextStyle(counterVar.get() <= 0)
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            cameraViewModel.statusCamera() -> {
                // If in camera mode, close camera and return to previous screen
                cameraViewModel.closeCamera()
            }
            else -> {
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        }

    }

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
    ) {

        // Update the RecyclerView item with the new photo
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

        adapter?.addPhotoFile("${position}", photoFile)

        // Find the specific ImageView in the RecyclerView item
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
        viewHolder?.let {
            // Load the image using Glide or your preferred image loading library
            Glide.with(this)
                .load(photoFile)
                .into(it.imageView)
        }
    }


}
