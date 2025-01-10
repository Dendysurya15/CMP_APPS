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
import com.cbi.cmp_project.data.model.TPHModel
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeFragment
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var companyCodeList: List<CompanyCodeModel> = emptyList()
    private var bUnitCodeList: List<BUnitCodeModel> = emptyList()
    private var divisionCodeList: List<DivisionCodeModel> = emptyList()
    private var fieldCodeList: List<FieldCodeModel> = emptyList()
    private var tphList: List<TPHModel>? = null // Lazy-loaded
    private lateinit var loadingDialog: LoadingDialog

    private var selectedBUnitCodeValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedFieldCodeValue: Int? = null
    private var selectedAncakValue: Int? = null
    private var selectedTPHValue: Int? = null

    enum class InputType {
        SPINNER,
    }

    // Add these new counter variables
    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0



    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()

        loadFileAsync()



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

    private fun loadFileAsync() {
        val downloadedFile = File(application.getExternalFilesDir(null), "dataset_company.zip")
        loadingDialog.show()
        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_dataset)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500) // Update every 500ms
            }
        }


        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    decompressFile(downloadedFile)
                }
            } catch (e: Exception) {
                Log.e("LoadFileAsync", "Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    progressJob.cancel()
                    setupLayout()
                }
            }
        }
    }

    private fun decompressFile(file: File) {
        try {
            // Read the GZIP-compressed file directly
            val gzipInputStream = GZIPInputStream(file.inputStream())
            val decompressedData = gzipInputStream.readBytes()

            // Convert the decompressed bytes to a JSON string
            val jsonString = String(decompressedData, Charsets.UTF_8)
            Log.d("DecompressedJSON", "Decompressed JSON: $jsonString")

            // Parse the JSON into data classes
            parseJsonData(jsonString)

        } catch (e: Exception) {
            Log.e("DecompressFile", "Error decompressing file: ${e.message}")
            e.printStackTrace()
        }
    }


    private fun parseJsonData(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val gson = Gson()

            // Parse lightweight data
//            val companyCodeList: List<CompanyCodeModel> = gson.fromJson(
//                jsonObject.getJSONArray("CompanyCode").toString(),
//                object : TypeToken<List<CompanyCodeModel>>() {}.type
//            )

            val bUnitCodeList : List<BUnitCodeModel> = gson.fromJson(
                jsonObject.getJSONArray("BUnitCode").toString(),
                object :TypeToken<List<BUnitCodeModel>>() {}.type
            )

            val divisionCodeList: List<DivisionCodeModel> = gson.fromJson(
                jsonObject.getJSONArray("DivisionCode").toString(),
                object : TypeToken<List<DivisionCodeModel>>() {}.type
            )

            val fieldCodeList: List<FieldCodeModel> = gson.fromJson(
                jsonObject.getJSONArray("FieldCode").toString(),
                object : TypeToken<List<FieldCodeModel>>() {}.type
            )

            // Log parsed data for debugging
            Log.d("ParsedData", "CompanyCode: $companyCodeList")
            Log.d("ParsedData", "bUnitCodeList: $bUnitCodeList")
            Log.d("ParsedData", "DivisionCode: $divisionCodeList")
            Log.d("ParsedData", "FieldCode: $fieldCodeList")

            // Cache lightweight data
            this.companyCodeList = companyCodeList
            this.bUnitCodeList = bUnitCodeList
            this.divisionCodeList = divisionCodeList
            this.fieldCodeList = fieldCodeList

//            // Lazy-load TPH only when needed
//            this.tphList = null // Initialize as null

            loadTPHData(jsonObject)

        } catch (e: JSONException) {
            Log.e("ParseJsonData", "Error parsing JSON: ${e.message}")
        }
    }

    private fun loadTPHData(jsonObject: JSONObject) {
        if (tphList == null) {
            val gson = Gson()
            tphList = gson.fromJson(
                jsonObject.getJSONArray("TPH").toString(),
                object : TypeToken<List<TPHModel>>() {}.type
            )
            Log.d("TPHData", "Loaded TPH data with ${tphList?.size} entries")
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
                        R.id.layoutEstate -> {

                            val bUnitNames = bUnitCodeList.map { it.BUnitName }
                            setupSpinnerView(layoutView, bUnitNames)
                        }
                        else -> {
                            // Set empty list for any other spinner
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

        setupRecyclerViewTakePreviewFoto()
    }

    fun resetViewsBelow(triggeredLayout: Int) {
        when (triggeredLayout) {
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
        selectedAncakValue = null
    }

    fun resetSelectedTPH() {
        // Assuming you have a variable for TPH selection
        selectedTPHValue = null
    }

    private fun setupSpinnerView(linearLayout: LinearLayout, data: List<String>, onItemSelected: (Int) -> Unit = {}) {
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)
        spinner.setTextSize(18f)
        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE

            when(linearLayout.id){
                R.id.layoutEstate->{

                    val selectedBUnit = bUnitCodeList.getOrNull(position)
                    resetViewsBelow(R.id.layoutEstate)
                    selectedBUnit?.let { bUnit ->
                        // Filter DivisionCode list based on selected BUnitCode
                        val filteredDivisionCodes = divisionCodeList.filter { division ->
                            division.BUnitCode == bUnit.BUnitCode  // Match the code (adjust field name as needed)
                        }
                        val divisionCodeNames = filteredDivisionCodes.map { it.DivisionName }
                        val afdelingLayoutView = findViewById<LinearLayout>(R.id.layoutAfdeling)

                        selectedBUnitCodeValue = bUnit.BUnitCode
                        setupSpinnerView(afdelingLayoutView, divisionCodeNames)
                    } ?: run {
                        // If no BUnitCode is selected (shouldn't happen if the list is valid)
                        Log.e("Spinner", "Invalid BUnitCode selection")
                    }
                }
                R.id.layoutAfdeling -> {
                    val selectedAfdeling = item.toString()
                    resetViewsBelow(R.id.layoutAfdeling)
                    val selectedDivisionCode = divisionCodeList.find { it.DivisionName == selectedAfdeling }?.DivisionCode
                    selectedDivisionCodeValue = selectedDivisionCode ?: run {
                        // Handle the case where no matching DivisionCode is found
                        Log.e("Spinner", "No DivisionCode found for DivisionName: $selectedAfdeling")
                        null
                    }

                    // Filter the fieldCodeList based on the selected BUnitCode and DivisionCode
                    val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
                        fieldCode.BUnitCode == selectedBUnitCodeValue && fieldCode.DivisionCode == selectedDivisionCodeValue
                    }

                    if (filteredFieldCodes.isNotEmpty()) {
                        // Extract PlantingYear from the filtered results
                        val plantingYears = filteredFieldCodes
                            .map { it.PlantingYear.toString() } // Convert each PlantingYear to String
                            .distinct() // Remove duplicate years
                            .sorted() // Sort the years in ascending order

                        val plantingYearLayoutView = findViewById<LinearLayout>(R.id.layoutTahunTanam)

                        setupSpinnerView(plantingYearLayoutView, plantingYears)
                    } else {
                        val plantingYearLayoutView = findViewById<LinearLayout>(R.id.layoutTahunTanam)
                        setupSpinnerView(plantingYearLayoutView, emptyList())
                    }
                }
                R.id.layoutTahunTanam -> {
                    val selectedTahunTanam = item.toString()
                    resetViewsBelow(R.id.layoutTahunTanam)
                    selectedTahunTanamValue = selectedTahunTanam
                    val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
                        fieldCode.BUnitCode == selectedBUnitCodeValue &&
                                fieldCode.DivisionCode == selectedDivisionCodeValue &&
                                fieldCode.PlantingYear.toString() == selectedTahunTanam // Match the selected PlantingYear
                    }

                    if (filteredFieldCodes.isNotEmpty()) {
                        // Extract the FieldName for the filtered fieldCodes
                        val fieldNames = filteredFieldCodes.map { it.FieldName }

                        val blokLayoutView = findViewById<LinearLayout>(R.id.layoutBlok)

                        setupSpinnerView(blokLayoutView, fieldNames)
                    } else {
                        val blokLayoutView = findViewById<LinearLayout>(R.id.layoutBlok)
                        setupSpinnerView(blokLayoutView, emptyList())
                    }
                }

                R.id.layoutBlok->{
                    val selectedBlok = item.toString()
                    resetViewsBelow(R.id.layoutBlok)
                    val selectedFieldCode = fieldCodeList.find { it.FieldName == selectedBlok }?.FieldCode
                    selectedFieldCodeValue = selectedFieldCode ?: run {
                        null
                    }

                    val filteredTPH = tphList?.filter { tph ->
                        tph.BUnitCode == selectedBUnitCodeValue &&
                                tph.DivisionCode == selectedDivisionCodeValue &&
                                tph.planting_year == selectedTahunTanamValue!!.toInt() &&
                                tph.FieldCode == selectedFieldCodeValue
                    }

                    if (filteredTPH != null && filteredTPH.isNotEmpty()) {
                        // Extract distinct values for 'Ancak' from the filtered TPH data
                        val ancakValues = filteredTPH.map { it.ancak }.distinct()

                        // Find the layout for 'Ancak' (assuming it's R.id.layoutAncak)
                        val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutAncak)

                        // Set up the spinner for Ancak values
                        setupSpinnerView(ancakLayoutView, ancakValues.map { it.toString() }) // Convert to String for spinner
                    } else {
                        // Handle case where no matching TPH data is found
                        Log.e("Spinner", "No matching TPH data found for the selected filters")

                        // Set an empty list to the spinner for Ancak
                        val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutAncak)
                        setupSpinnerView(ancakLayoutView, emptyList()) // Empty list when no data is found
                    }

                }
                R.id.layoutAncak->{
                    val selectedAncak = item.toString()
                    resetViewsBelow(R.id.layoutAncak)
                    // Find the matching TPH entry based on selectedAncak and other filter criteria
                    val selectedAncakCode = tphList?.find { tph ->
                                tph.BUnitCode == selectedBUnitCodeValue &&
                                tph.DivisionCode == selectedDivisionCodeValue &&
                                tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                tph.FieldCode == selectedFieldCodeValue &&
                                tph.ancak.toString() == selectedAncak // Match the selectedAncak with TPH's ancak
                    }?.ancak

                    selectedAncakValue = selectedAncakCode ?: run {
                        // If no matching TPH entry is found, return null
                        null
                    }

                    val filteredTPH = tphList?.filter { tph ->
                        tph.BUnitCode == selectedBUnitCodeValue &&
                                tph.DivisionCode == selectedDivisionCodeValue &&
                                tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                tph.FieldCode == selectedFieldCodeValue &&
                                tph.ancak == selectedAncakValue
                    }

                    if (filteredTPH != null && filteredTPH.isNotEmpty()) {
                        // Extract distinct values for 'Ancak' from the filtered TPH data
                        val tphValues = filteredTPH.map { it.tph }.distinct()

                        // Find the layout for 'Ancak' (assuming it's R.id.layoutAncak)
                        val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutNoTPH)

                        // Set up the spinner for Ancak values
                        setupSpinnerView(ancakLayoutView, tphValues.map { it.toString() }) // Convert to String for spinner
                    } else {
                        // Handle case where no matching TPH data is found
                        Log.e("Spinner", "No matching TPH data found for the selected filters")

                        // Set an empty list to the spinner for Ancak
                        val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutNoTPH)
                        setupSpinnerView(ancakLayoutView, emptyList())
                    }
                }


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
