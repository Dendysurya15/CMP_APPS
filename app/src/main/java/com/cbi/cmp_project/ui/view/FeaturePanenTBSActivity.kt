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
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.cmp_project.ui.adapter.SelectedWorkerAdapter
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeFragment
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.DataCacheManager
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.MathFun
import com.cbi.cmp_project.utils.PrefManager
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
import kotlinx.coroutines.async
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
import android.text.InputType as AndroidInputType
import android.view.inputmethod.EditorInfo
open class FeaturePanenTBSActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    private var jumTBS = 0

    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var vCut = 0
    private var lat: Double? = null
    private var lon: Double? = null
    var currentAccuracy : Float = 0F
    private var prefManager: PrefManager? = null

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
    private var kemandoranLainList: List<KemandoranModel> = emptyList()
    private var kemandoranDetailList: List<KemandoranDetailModel> = emptyList()
    private var tphList: List<TPHNewModel> = emptyList()

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private lateinit var selectedPemanenLainAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var rvSelectedPemanenLain: RecyclerView
    private lateinit var dataCacheManager: DataCacheManager
    private var selectedKemandoranValue: Int? = null

    enum class InputType {
        SPINNER,
        EDITTEXT
    }
    private var selectedRegional: String = ""
    private var selectedWilayah: String = ""
    private var selectedEstate: String = ""
    private var selectedAfdeling: String = ""
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""
    private var selectedKemandoranLain: String = ""


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
    private var selectedKemandoranLainSpinnerIndex: Int? = null
    private var selectedTPHSpinnerIndex: Int? = null

    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0
    var persenMentah = 0f
    var persenLewatMasak = 0f
    var persenJjgKosong = 0f
    var persenMasak = 0f


    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>
    private lateinit var datasetViewModel: DatasetViewModel
    private var regionalId: String? = null
    private var regionalName: String? = null
    private var estateId: String? = null
    private var estateName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        loadingDialog = LoadingDialog(this)
        dataCacheManager = DataCacheManager(this)
        prefManager = PrefManager(this)
        initViewModel()

        regionalId = prefManager!!.getRegionalUserLogin("regional_id")
        regionalName = prefManager!!.getRegionalUserLogin("regional_name")
        estateId = prefManager!!.getEstateUserLogin("estate_id")
        estateName = prefManager!!.getEstateUserLogin("estate_name")

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
            }

            try {

                val deptDeferred = async { datasetViewModel.getDeptList(regionalId!!, estateName!!) }
                val divisiDeferred = async { datasetViewModel.getDivisiList(estateId!!.toInt()) }
                deptList = deptDeferred.await()
                divisiList = divisiDeferred.await()

                withContext(Dispatchers.Main) {
                    setupLayout()
                }
            } catch (e: Exception) {
                AppLogger.e("Error fetching data: ${e.message}")
            } finally {
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

//                panenTBSViewModel.insertPanenTBSVM(
//                    user_id = 1,
//                    tanggal = "2024-12-26",
//                    name = "John Doe",
//                    estate = "Estate 1",
//                    id_estate = 101,
//                    afdeling = "Afdeling A",
//                    id_afdeling = 202,
//                    blok = "Blok X",
//                    id_blok = 303,
//                    tahun_tanam = 2020,
//                    id_tt = 404,
//                    ancak = "No",
//                    id_ancak = 505,
//                    tph = "TPH-1",
//                    id_tph = 606,
//                    jenis_panen = "Harvest Type A",
//                    list_pemanen = "John, Alice, Bob",
//                    list_idpemanen = "1,2,3",
//                    tbs = 1000,
//                    tbs_mentah = 200,
//                    tbs_lewatmasak = 100,
//                    tks = 50,
//                    abnormal = 10,
//                    tikus = 5,
//                    tangkai_panjang = 30,
//                    vcut = 5,
//                    tbs_masak = 900,
//                    tbs_dibayar = 800,
//                    tbs_kirim = 750,
//                    latitude = "1.234567",
//                    longitude = "103.456789",
//                    foto = "image_url.jpg"
//                )
//
//                panenTBSViewModel.insertDBPanenTBS.observe(this) { isInserted ->
//
//                    Log.d("testing", isInserted.toString())
//                    if (isInserted){
//                        AlertDialogUtility.alertDialogAction(
//                            this,
//                            "Sukses",
//                            "Data berhasil disimpan!",
//                            "success.json"
//                        ) {
//                            Toast.makeText(
//                                this,
//                                "sukses bro",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        }
//                    }else{
//                        Toast.makeText(
//                            this,
//                            "Gagal bro",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                }


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
        findViewById<TextView>(R.id.tvPercentBuahMasak).text = "($persenMasak)%"
    }

    private fun updateDependentCounters(layoutId: Int, change: Int, counterVar: KMutableProperty0<Int>,  tvPercent: TextView?) {
        when (layoutId) {
            R.id.layoutJumTBS -> {
                if (change > 0) { // When change is positive (Increment)
                    jumTBS += change
                    counterVar.set(jumTBS)
                } else if (change < 0) { // When change is negative (Decrement)
                    if (buahMasak > 0) {  // Prevent going negative
                        jumTBS += change
                        counterVar.set(jumTBS)
                    }else{
                        vibrate()
                    }
                }
            }

            R.id.layoutBMentah -> {
                if (change > 0) {
                    if (buahMasak > 0 ) {
                        bMentah += change
                        counterVar.set(bMentah)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (bMentah > 0) { // Prevent going negative
                        bMentah += change
                        counterVar.set(bMentah)
                    }else{
                        vibrate()
                    }
                }
            }

            R.id.layoutBLewatMasak -> {
                if (change > 0) { // When change is positive (Increment)
                    if (buahMasak > 0) {
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (bLewatMasak > 0) { // Prevent going negative
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    }else{
                        vibrate()
                    }
                }
            }

            R.id.layoutJjgKosong -> {
                if (change > 0) { // When change is positive (Increment)
                    if (buahMasak > 0) {
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (jjgKosong > 0) { // Prevent going negative
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    }else{
                        vibrate()
                    }
                }
            }

            R.id.layoutAbnormal -> {
                if (change > 0) { // When change is positive (Increment)
                    if (jumTBS > abnormal) { // Prevent abnormal from exceeding tbs
                        abnormal += change
                        counterVar.set(abnormal)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (abnormal > 0) { // Prevent going negative
                        abnormal += change
                        counterVar.set(abnormal)
                    }else{
                        vibrate()
                    }
                }
            }
            R.id.layoutSeranganTikus -> {
                if (change > 0) {
                    if (jumTBS > seranganTikus) {
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (seranganTikus > 0) { // Prevent going negative
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    }else{
                        vibrate()
                    }
                }
            }
            R.id.layoutTangkaiPanjang -> {
                if (change > 0) {
                    if (jumTBS > tangkaiPanjang) {
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (tangkaiPanjang > 0) { // Prevent going negative
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    }else{
                        vibrate()
                    }
                }
            }
            R.id.layoutVcut -> {
                if (change > 0) {
                    if (jumTBS > vCut) {
                        vCut += change
                        counterVar.set(vCut)
                    }else{
                        vibrate()
                    }
                } else if (change < 0) {
                    if (jumTBS > 0) {
                        vCut += change
                        counterVar.set(vCut)
                    }else{
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
        }
        else if (layoutId == R.id.layoutBLewatMasak) {
            tvPercent?.let {
                it.setText("${persenLewatMasak}%")
            }
        }
        else if(layoutId == R.id.layoutJjgKosong){
            tvPercent?.let {
                it.setText("${persenJjgKosong}%")
            }
        }
    }

    private fun formulas(){
        buahMasak = jumTBS - jjgKosong - bMentah - bLewatMasak
        bMentah = jumTBS - bLewatMasak - jjgKosong - buahMasak
        bLewatMasak = jumTBS - bMentah - buahMasak - jjgKosong
        jjgKosong = jumTBS - bMentah - buahMasak - bLewatMasak
        tbsDibayar = jumTBS - bMentah - jjgKosong
        kirimPabrik = jumTBS - jjgKosong - abnormal
        persenMentah = MathFun().round((bMentah.toFloat()/jumTBS.toFloat()*100), 2)!!
        persenMasak = MathFun().round((bMentah.toFloat()/jumTBS.toFloat()*100), 2)!!
        persenLewatMasak = MathFun().round((bLewatMasak.toFloat()/jumTBS.toFloat()*100), 2)!!
        persenJjgKosong = MathFun().round((jjgKosong.toFloat()/jumTBS.toFloat()*100), 2)!!
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

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]
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
            Triple(findViewById<LinearLayout>(R.id.layoutEstate), getString(R.string.field_estate), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAfdeling), getString(R.string.field_afdeling), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTahunTanam), getString(R.string.field_tahun_tanam), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutBlok), getString(R.string.field_blok), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTipePanen), getString(R.string.field_tipe_panen), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutNoTPH), getString(R.string.field_no_tph), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAncak), getString(R.string.field_ancak), InputType.EDITTEXT),
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
                        R.id.layoutEstate -> {
                            val namaEstate = deptList!!.map { it.nama }
                            setupSpinnerView(layoutView, namaEstate)
                            findViewById<MaterialSpinner>(R.id.spPanenTBS).setSelectedIndex(0)
                        }
                        R.id.layoutAfdeling -> {
                            val divisiNames = divisiList.map { it.abbr }
                            setupSpinnerView(layoutView, divisiNames)
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

                InputType.EDITTEXT -> setupEditTextView(layoutView)

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
            Triple(R.id.layoutVcut, "Tidak V-Cut", ::vCut)
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
        setupSwitch()
    }

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

        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
                MCVSpinner.setBackgroundColor(ContextCompat.getColor(layoutView.context, R.color.graytextdark))
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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
        kemandoranDetailList = emptyList()

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

    private fun setupSpinnerView(linearLayout: LinearLayout, data: List<String>, onItemSelected: (Int) -> Unit = {}) {
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)
        spinner.setTextSize(18f)

        if (linearLayout.id == R.id.layoutEstate) {
            spinner.isEnabled = false // Disable the spinner
        }
        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE

            when(linearLayout.id){
                R.id.layoutAfdeling -> {
                    resetDependentSpinners(linearLayout.rootView)
                    selectedAfdeling = item.toString()
                    val selectedDivisiId = divisiList.find {
                        it.dept == estateId!!.toInt() && it.abbr == selectedAfdeling
                    }?.id

                    val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()

                    selectedDivisionSpinnerIndex = position
                    selectedDivisiValue = selectedDivisiId
                    val estateAbbr = deptList.firstOrNull()?.abbr

                    val nonSelectedAfdelingKemandoran = divisiList.filter { it.abbr != selectedAfdeling && it.dept == estateId!!.toInt()}

                    val nonSelectedIdAfdeling = nonSelectedAfdelingKemandoran.map{it.id}

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading afdeling data...")
                        }


                        try {
                            val blokDeferred = async {
                                datasetViewModel.getBlokList(
                                    regionalId!!.toInt(),
                                    estateId!!.toInt(),
                                    selectedDivisiId!!,
                                    estateAbbr!!
                                )
                            }
                            val kemandoranDeferred = async {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    selectedDivisiIdList,
                                    estateAbbr!!
                                )
                            }

                            val kemandoranLainDeferred = async {
                                datasetViewModel.getKemandoranList(
                                    estateId!!.toInt(),
                                    nonSelectedIdAfdeling, // Already a List<Int>
                                    estateAbbr!!
                                )
                            }

                            blokList = blokDeferred.await()
                            val tahunTanamList = blokList.map { it.tahun }.distinct().sortedBy { it!!.toInt() }
                            kemandoranList = kemandoranDeferred.await()

                            kemandoranLainList = kemandoranLainDeferred.await()


                            withContext(Dispatchers.Main) {


                                val layoutTahunTanam = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutTahunTanam)
                                val layoutKemandoran = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoran)
                                val layoutKemandoranLain = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoranLain)
                                if (tahunTanamList.isNotEmpty()) {
                                    setupSpinnerView(layoutTahunTanam, tahunTanamList as List<String>)
                                } else {
                                    setupSpinnerView(layoutTahunTanam, emptyList())
                                }


                                val kemandoranNames = kemandoranList.map { it.nama }
                                if (kemandoranNames.isNotEmpty()) {
                                    setupSpinnerView(layoutKemandoran, kemandoranNames as List<String>)

                                } else {
                                    setupSpinnerView(layoutKemandoran, emptyList())
                                }

                                val kemandoranLainListNames = kemandoranLainList.map { it.nama }

                                AppLogger.d(kemandoranLainList.toString())
                                if (nonSelectedAfdelingKemandoran.isNotEmpty()) {
                                    setupSpinnerView(layoutKemandoranLain,
                                        kemandoranLainListNames as List<String>
                                    )
                                } else {
                                    setupSpinnerView(layoutKemandoranLain, emptyList())
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
                                loadingDialog.dismiss()
                            }
                        }
                    }

                }
                R.id.layoutTahunTanam->{
                    resetTPHSpinner(linearLayout.rootView)
                    val selectedTahunTanam = item.toString()
                    selectedTahunTanamValue = selectedTahunTanam
                    val estateAbbr = deptList.find { it.id == estateId!!.toInt() }?.abbr
                    val filteredBlokCodes = blokList.filter {
                        it.regional == regionalId!!.toInt() &&
                                it.dept == estateId!!.toInt() &&
                                ((it.divisi != null && it.divisi.toString().isNotEmpty() && it.divisi == selectedDivisiValue) || it.dept_abbr == estateAbbr) &&
                                it.tahun == selectedTahunTanamValue
                    }

                    val layoutBlok = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutBlok)
                    if (filteredBlokCodes.isNotEmpty()) {
                        val blokNames = filteredBlokCodes.map { it.kode }
                        setupSpinnerView(layoutBlok, blokNames as List<String>)
                        layoutBlok.visibility = View.VISIBLE
                    } else {
                        layoutBlok.visibility = View.GONE
                    }
                }
                R.id.layoutBlok -> {
                    resetTPHSpinner(linearLayout.rootView)
                    selectedBlok = item.toString()
                    selectedFieldCodeSpinnerIndex = position
                    val estateAbbr = deptList.find { it.id == estateId!!.toInt() }?.abbr
                    val selectedFieldId = blokList.find { blok ->
                        blok.regional == regionalId!!.toInt() &&
                                blok.dept == estateId!!.toInt() &&
                                ((blok.divisi != null && blok.divisi.toString().isNotEmpty() && blok.divisi == selectedDivisiValue) || blok.dept_abbr == estateAbbr) &&
                                blok.tahun == selectedTahunTanamValue &&
                                blok.kode == selectedBlok
                    }?.id

                    selectedBlokValue = selectedFieldId


                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading afdeling data...")
                        }

//
                        try {
                            val tphDeferred = async {
                                datasetViewModel.getTPHList(
                                    regionalId!!.toInt(),
                                    estateId!!.toInt(),
                                    selectedDivisiValue!!,
                                    estateAbbr!!,
                                    selectedTahunTanamValue!!,
                                    selectedBlokValue!!
                                )
                            }

                            tphList = tphDeferred.await()
                            val noTPHList = tphList.map { it.nomor }


                            withContext(Dispatchers.Main) {
                                val layoutNoTPH = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutNoTPH)

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
                                loadingDialog.dismiss()
                            }
                        }
                    }
                }

                R.id.layoutNoTPH ->{
                    selectedTPH = item.toString()
                    selectedTPHSpinnerIndex = position
                    val estateAbbr = deptList.find { it.id == estateId!!.toInt() }?.abbr
                    val selectedTPHId = tphList!!.find {
                        it.regional == regionalId!!.toInt() &&
                                it.dept == estateId!!.toInt() &&
                                ((it.divisi != null && it.divisi.toString().isNotEmpty() && it.divisi == selectedDivisiValue) || it.dept_abbr == estateAbbr) &&
                                it.blok == selectedBlokValue &&
                                it.tahun == selectedTahunTanamValue &&
                                it.nomor == selectedTPH
                    }

                    selectedTPHValue = selectedTPHId?.id
                }
                R.id.layoutKemandoran -> {
//                    selectedPemanenAdapter.clearAllWorkers()
                    val selectedKemandoran = item.toString()
                    val filteredKemandoranId = kemandoranList.find {
                        it.dept == estateId!!.toInt() &&
                                it.divisi == selectedDivisiValue
                                it.nama == selectedKemandoran
                    }?.id

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading karyawan data...")
                        }

                        try {
                            val kemandoranDetailDeferred = async {
                                datasetViewModel.getKemandoranDetailList(
                                    filteredKemandoranId!!,
                                )
                            }

                            kemandoranDetailList = kemandoranDetailDeferred.await()

                            val filteredKemandoranDetails = kemandoranDetailList.mapNotNull {it.nik}

                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(
                                    filteredKemandoranDetails.toTypedArray()
                                )
                            }

                            karyawanList = karyawanDeferred.await()

                            val karyawanNames = karyawanList.map { it.nama }

                            withContext(Dispatchers.Main) {
                                val layoutPemanen = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                                if (karyawanNames.isNotEmpty()) {
                                    setupSpinnerView(layoutPemanen, karyawanNames as List<String>)
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
                                loadingDialog.dismiss()
                            }
                        }
                    }
                }
                R.id.layoutPemanen -> {
                    val selectedPemanen = item.toString()
                    selectedPemanenAdapter.addWorker(selectedPemanen)

                    // Get the updated list of available workers
                    val availableWorkers = selectedPemanenAdapter.getAvailableWorkers()

                    // Only update the spinner if there are still available workers
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(linearLayout, availableWorkers)
                    }

                }

                R.id.layoutKemandoranLain -> {
//                    selectedPemanenLainAdapter.clearAllWorkers()
                    selectedKemandoranLain = item.toString()
                    selectedKemandoranLainSpinnerIndex = position
                    val estateAbbr = deptList.find { it.id == estateId!!.toInt() }?.abbr

                    val selectedIdKemandoranLain = kemandoranLainList.find {
                        it.dept == estateId!!.toInt() &&
                                (it.divisi != null && it.divisi.toString().isNotEmpty() && it.divisi == selectedDivisiValue || it.dept_abbr == estateAbbr) &&
                                it.nama == selectedKemandoranLain
                    }?.id



                    AppLogger.d(selectedIdKemandoranLain.toString())
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading karyawan data...")
                        }

                        try {
                            val kemandoranDetailDeferred = async {
                                datasetViewModel.getKemandoranDetailList(
                                    selectedIdKemandoranLain!!,
                                )
                            }

                            val kemandoranDetailList = kemandoranDetailDeferred.await()

                            val allKaryawanIdInKemandoranDetail = kemandoranDetailList.mapNotNull {it.nik}

                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(
                                    allKaryawanIdInKemandoranDetail.toTypedArray()
                                )
                            }

                            val listKaryawanKemandoranLain = karyawanDeferred.await()

                            val namaKaryawanKemandoranLain = listKaryawanKemandoranLain.map { it.nama }

                            withContext(Dispatchers.Main) {
                                val layoutPemanenLain = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanenLain)
                                if (namaKaryawanKemandoranLain.isNotEmpty()) {
                                    setupSpinnerView(layoutPemanenLain, namaKaryawanKemandoranLain as List<String>)
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
                                loadingDialog.dismiss()
                            }
                        }
                    }
                }

                R.id.layoutPemanenLain->{
                    val selectedPemanenLain = item.toString()
                    selectedPemanenLainAdapter.addWorker(selectedPemanenLain)
                    val availableWorkers = selectedPemanenLainAdapter.getAvailableWorkers()

                    // Only update the spinner if there are still available workers
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(linearLayout, availableWorkers)
                    }
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
                selectedPemanenLainAdapter.clearAllWorkers()
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
        val tvPercent = includedLayout.findViewById<TextView>(R.id.tvPercent)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)
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
            if (counterVar.get() > 0) {
                updateDependentCounters(layoutId, -1, counterVar,  tvPercent)  // Decrement through dependent counter
                etNumber.setText(counterVar.get().toString())
            } else {
                vibrate()
                changeEditTextStyle(true)
            }
        }


        btInc.setOnClickListener {
            updateDependentCounters(layoutId, 1, counterVar,  tvPercent)  // Increment through dependent counter
            etNumber.setText(counterVar.get().toString())
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
