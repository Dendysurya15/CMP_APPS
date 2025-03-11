package com.cbi.cmp_project.ui.view.espb

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.ui.adapter.SelectedWorkerAdapter
import com.cbi.cmp_project.ui.adapter.Worker
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.view.ListTPHApproval
import com.cbi.cmp_project.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.ui.view.weighBridge.ScanWeighBridgeActivity
import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.ESPBViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.setMaxBrightness
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jaredrummler.materialspinner.MaterialSpinner
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormESPBActivity : AppCompatActivity() {
    var featureName = ""
    var tph0 = ""
    var tph1 = ""
    var idEstate = 0
    var mekanisasi = 0
    var selectedKemandoranId = 0
    var selectedTransporterId = 0
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var viewModel: ESPBViewModel
    private var selectedMillId = 0
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var pemuatList: List<KaryawanModel> = emptyList()
    private var transporterList: List<TransporterModel> = emptyList()

    private lateinit var inputMappings: List<Triple<LinearLayout, String, FeaturePanenTBSActivity.InputType>>
    private lateinit var viewModelFactory: ESPBViewModelFactory
    private var pemuatListId: ArrayList<Int> = ArrayList()
    private lateinit var selectedPemuatAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var thp1Map: Map<Int, Int>
    var idsToUpdate = listOf<Int>()
    var divisiAbbr = ""
    var companyAbbr = ""
    var formattedJanjangString = ""
    var tph1IdPanen = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_espbactivity)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialogUtility.withTwoActions(
                    this@FormESPBActivity,
                    "KEMBALI",
                    "Kembali ke Menu utama?",
                    "Data scan sebelumnya akan terhapus",
                    "warning.json"
                ) {
                    startActivity(
                        Intent(
                            this@FormESPBActivity,
                            HomePageActivity::class.java
                        ))
                    finishAffinity()
                }
            }
        })
        try {
            featureName = intent.getStringExtra("FEATURE_NAME").toString()
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil FEATURE NAME $e", Toasty.LENGTH_LONG).show()
        }
        try {
            tph0 = intent.getStringExtra("tph_0").toString()
            Log.d("FormESPBActivityTPH0", "tph0: $tph0")
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil TPH 0 $e", Toasty.LENGTH_LONG).show()
        }
        try {
            tph1 = intent.getStringExtra("tph_1").toString()
            Log.d("FormESPBActivityTPH1", "tph1: $tph1")
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil TPH 1 $e", Toasty.LENGTH_LONG).show()
        }
        try {
            ///tph1IdPanen is sometin like 1,23,4,5,2,3
            tph1IdPanen = intent.getStringExtra("tph_1_id_panen").toString()
            // Split the string by comma to get individual IDs
            val idStrings = tph1IdPanen.split(",")

            // Convert each string ID to an integer
            idsToUpdate = idStrings.mapNotNull {
                it.trim().toIntOrNull()
                    ?: throw NumberFormatException("Invalid integer format: $it")
            }
            Log.d("FormESPBActivityIDS", "idsToUpdate: $idsToUpdate")

        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil TPH 1 ID PANEN $e", Toasty.LENGTH_LONG).show()
        }

        initViewModel()
        setupHeader()
        setupViewModel()
        Log.d("tph1", "tph1: $tph1")
        viewModel.janjangByBlock.observe(this) { janjangMap ->
            // Log each block and its janjang sum
            janjangMap.forEach { (blockId, janjangSum) ->
                Log.d("BlockJanjang", "Block $blockId: $janjangSum janjang")
            }

            // Convert the map to string format INSIDE the observer
            formattedJanjangString = convertJanjangMapToString(janjangMap)
            Log.d("FormattedJanjang", "Formatted string: $formattedJanjangString")
        }
        // Process the TPH data
        viewModel.processTPHData(tph1)

        //NBM 115
        //transporter 1
        val formEspbNopol = findViewById<LinearLayout>(R.id.formEspbNopol)
        val tvEspbNopol = formEspbNopol.findViewById<TextView>(R.id.tvTitlePaneEt)
        val etEspbNopol = formEspbNopol.findViewById<EditText>(R.id.etPaneEt)
        etEspbNopol.hint = "KH 2442 GF"
        tvEspbNopol.text = "No. Polisi"

        val formEspbDriver = findViewById<LinearLayout>(R.id.formEspbDriver)
        val tvEspbDriver = formEspbDriver.findViewById<TextView>(R.id.tvTitlePaneEt)
        val etEspbDriver = formEspbDriver.findViewById<EditText>(R.id.etPaneEt)
        etEspbDriver.hint = "Fulan"
        tvEspbDriver.text = "Driver"

        val formEspbTransporter = findViewById<LinearLayout>(R.id.formEspbTransporter)

        setupSpinnerText(R.id.formEspbMill,"Pilih Mill", "Mill")
        setupSpinnerText(R.id.formEspbKemandoran,"Pilih Kemandoran", "Kemandoran")
        setupSpinnerText(R.id.formEspbTransporter,"Pilih Transporter", "Transporter")
        setupSpinnerText(R.id.formEspbPemuat,"Pilih Pemuat", "Pemuat")

        rvSelectedPemanen = findViewById<RecyclerView>(R.id.rvPemuat)
        selectedPemuatAdapter = SelectedWorkerAdapter()
        // Add this after initializing rvSelectedPemanen and selectedPemuatAdapter
        rvSelectedPemanen.apply {
            layoutManager = FlexboxLayoutManager(this@FormESPBActivity).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = selectedPemuatAdapter
        }

        val prefManager = PrefManager(this)
        val idPetugas = try {
            prefManager.idUserLogin
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil ID Petugas $e", Toasty.LENGTH_LONG).show()
            0
        }
        val estatePetugas = try {
            prefManager.estateUserLogin
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil Estate Petugas $e", Toasty.LENGTH_LONG).show()
            "NULL"
        }
        idEstate = try {
            prefManager.estateIdUserLogin.toString().toInt()
        }catch (e: Exception){
            Toasty.error(this, "Terjadi Kesalahan saat mengambil ID Estate $e", Toasty.LENGTH_LONG).show()
            0
        }

        val cbFormEspbMekanisasi = findViewById<MaterialCheckBox>(R.id.cbFormEspbMekanisasi)
        cbFormEspbMekanisasi.setOnCheckedChangeListener {
            _, isChecked ->
            if (isChecked) {
//                formEspbTransporter.visibility = View.GONE
//                formEspbDriver.visibility = View.GONE
//                formEspbNopol.visibility = View.GONE
                mekanisasi = 1
            }else{
                formEspbTransporter.visibility = View.VISIBLE
                formEspbDriver.visibility = View.VISIBLE
                formEspbNopol.visibility = View.VISIBLE
                mekanisasi = 0
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val kemandoranDeferred = async {
                    try {
                        datasetViewModel.getKemandoranEstate(idEstate)
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching kemandoranList: ${e.message}")
                        emptyList()
                    }
                }
                kemandoranList = kemandoranDeferred.await()
                val nameKemandoran: List<String> = kemandoranList.map { it.nama.toString() }
                Log.d("FormESPBActivityKemandoran", "nameKemandoran: $nameKemandoran")
                withContext(Dispatchers.Main) {
                    setupSpinner(R.id.formEspbKemandoran, nameKemandoran)
                }
                val formEspbKemandoran = findViewById<LinearLayout>(R.id.formEspbKemandoran)
                val spEspbKemandoran= formEspbKemandoran.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                spEspbKemandoran.setOnItemSelectedListener { view, position, id, item ->
                    val selectedKemandoran = item.toString()

                    selectedKemandoranId = try {
                        kemandoranList.find { it.nama == selectedKemandoran }?.id!!
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
                            Log.d("FormESPBActivityKemandoran", "pemuatList: $pemuatList")
                            val pemuatNames = pemuatList.map { it.nama.toString() }
                            Log.d("FormESPBActivityKemandoran", "pemuatNames: $pemuatNames")
                            withContext(Dispatchers.Main) {
                                setupSpinner(R.id.formEspbPemuat, pemuatNames)
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching kemandoran data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FormESPBActivity,
                                    "Error loading kemandoran data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {

                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error fetching kemandoran data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FormESPBActivity,
                        "Error loading kemandoran data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {

                }
            }

            try {
                val transporterDeffered = async {
                    try {
                        datasetViewModel.getAllTransporter()
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching transporterList: ${e.message}")
                        emptyList()
                    }
                }
                transporterList = transporterDeffered.await()
                val nameTransporter: List<String> = transporterList.map { it.nama.toString() }
                Log.d("FormESPBActivityTransporter", "nameTransporter: $nameTransporter")
                withContext(Dispatchers.Main) {
                    setupSpinner(R.id.formEspbTransporter, nameTransporter)
                }
                val spEspbTransporter= formEspbTransporter.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                spEspbTransporter.setOnItemSelectedListener { view, position, id, item ->
                    val selectedTransporter = item.toString()

                    selectedTransporterId = try {
                        transporterList.find { it.nama == selectedTransporter }?.id!!
                    } catch (e: Exception) {
                        AppLogger.e("Error finding selectedTransporterId: ${e.message}")
                        0
                    }
                    Log.d("FormESPBActivityTransporter", "selectedTransporterId: $selectedTransporterId")
                }
            } catch (e: Exception) {
                AppLogger.e("Error fetching transporter data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FormESPBActivity,
                        "Error loading transporter data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {

                }
            }
        }

        lifecycleScope.launch {
            var firstTphId = 0
            // Split by semicolon to get each record
            // Split the first record by comma and get the first part (ID)
            try {
                // Split by semicolon to get each record
                val firstTphRecord = tph1.split(";")[0]
                // Split the first record by comma and get the first part (ID)
                firstTphId = firstTphRecord.split(",")[0].toInt()
                Log.d("FormESPBActivityDivisiAbbr", "firstTphId: $firstTphId")
            }catch (e: Exception){
                Toasty.error(
                    this@FormESPBActivity,
                    "Terjadi Kesalahan saat mengambil firstTphId $e",
                    Toasty.LENGTH_LONG
                ).show()
            }
            try {
                //use getDivisiAbbrByTphId FROM REPO
                divisiAbbr = viewModel.getDivisiAbbrByTphId(firstTphId)
                Log.d("FormESPBActivityDivisiAbbr", "divisiAbbr: $divisiAbbr")
            } catch (e: Exception) {
                Toasty.error(
                    this@FormESPBActivity,
                    "Terjadi Kesalahan saat mengambil divisiAbbr $e",
                    Toasty.LENGTH_LONG
                ).show()
            }
            try {
                //use getDivisiAbbrByTphId FROM REPO
                companyAbbr = viewModel.getCompanyAbbrByTphId(firstTphId)
                Log.d("FormESPBActivityDivisiAbbr", "companyAbbr: $companyAbbr")
            } catch (e: Exception) {
                Toasty.error(
                    this@FormESPBActivity,
                    "Terjadi Kesalahan saat mengambil companyAbbr $e",
                    Toasty.LENGTH_LONG
                ).show()
            }
        }

        val cbFormEspbTransporter = findViewById<MaterialCheckBox>(R.id.cbFormEspbTransporter)
        cbFormEspbTransporter.setOnCheckedChangeListener {
            _, isChecked ->
            if (isChecked) {
                formEspbTransporter.visibility = View.GONE
                selectedTransporterId = 0
            }else{
                formEspbTransporter.visibility = View.VISIBLE
            }
        }

        val btnGenerateQRESPB = findViewById<FloatingActionButton>(R.id.btnGenerateQRESPB)
        btnGenerateQRESPB.setOnClickListener {
            val nopol = try {
                etEspbNopol.text.toString().replace(" ","").uppercase()
            }catch (e: Exception){
                Toasty.error(this, "Terjadi Kesalahan saat mengambil No Polisi $e", Toasty.LENGTH_LONG).show()
                "NULL"
            }

            val driver = try {
                etEspbDriver.text.toString().replace(" ","").uppercase()
            }catch (e: Exception){
                Toasty.error(this, "Terjadi Kesalahan saat mengambil Driver $e", Toasty.LENGTH_LONG).show()
                "NULL"
            }

            val espbDate: String = try {
                getFormattedDateTime().toString()
            }catch (e: Exception){
                Toasty.error(this, "Terjadi Kesalahan saat mengambil Tanggal ESPB $e", Toasty.LENGTH_LONG).show()
                "NULL"
            }

            val appVersion: String = try {
                this.packageManager.getPackageInfo(this.packageName, 0).versionName
            } catch (e: Exception) {
                Log.e("DeviceInfo", "Failed to get app version", e)
                "Unknown"
            }

            val osVersion: String = try {
                Build.VERSION.RELEASE
            } catch (e: Exception) {
                Log.e("DeviceInfo", "Failed to get OS version", e)
                "Unknown"
            }

            val phoneModel: String = try {
                "${Build.MANUFACTURER} ${Build.MODEL}"
            } catch (e: Exception) {
                Log.e("DeviceInfo", "Failed to get phone model", e)
                "Unknown"
            }
            val noESPBStr = "$companyAbbr-$estatePetugas/$divisiAbbr/$espbDate"
            var transporter_id = 0
            transporter_id = if (cbFormEspbTransporter.isChecked) {
                0
            }else{
                selectedTransporterId
            }
            val creatorInfo = createCreatorInfo(
                appVersion = appVersion,
                osVersion = osVersion,
                phoneModel = phoneModel
            )
            val blok_jjg = try {
                formattedJanjangString
            }catch (e: Exception){
                Toasty.error(this, "Terjadi Kesalahan saat mengambil Janjang Blok $e", Toasty.LENGTH_LONG).show()
                "NULL"
            }
            val selectedPemanen = selectedPemuatAdapter.getSelectedWorkers()
            val pemuatListId = selectedPemanen.map { it.id }
            if (nopol == "NULL" || nopol == ""){
                Toasty.error(this, "Mohon lengkapi data No Polisi terlebih dahulu", Toasty.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (driver == "NULL" || driver == ""){
                Toasty.error(this, "Mohon lengkapi data Driver terlebih dahulu", Toasty.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (selectedTransporterId == 0 && !cbFormEspbTransporter.isChecked){
                Toasty.error(this, "Mohon lengkapi data Transporter terlebih dahulu", Toasty.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (selectedMillId == 0){
                Toasty.error(this, "Mohon lengkapi data Mill terlebih dahulu", Toasty.LENGTH_LONG).show()
                return@setOnClickListener
            }
            AlertDialogUtility.Companion.withTwoActions(this, "SIMPAN", "KONFIRMASI BUAT QR ESPB?", "Pastikan seluruh data sudah valid!", "warning.json"){
                val btKonfirmScanESPB = findViewById<MaterialButton>(R.id.btKonfirmScanESPB)
                btKonfirmScanESPB.visibility = View.VISIBLE
                btKonfirmScanESPB.setOnClickListener {
                    val statusDraft = if (mekanisasi == 0){
                        1
                    }else{
                        0
                    }
                    saveESPB(
                        blok_jjg = blok_jjg,
                        nopol = nopol,
                        driver = driver,
                        pemuat_id = pemuatListId.joinToString(","),
                        transporter_id = transporter_id,
                        mill_id = selectedMillId!!,
                        created_by_id = idPetugas!!,
                        creator_info = creatorInfo.toString(),
                        noESPB = noESPBStr,
                        created_at = getCurrentDateTime(),
                        tph0 = tph0,
                        tph1 = tph1,
                        status_draft = statusDraft,
                        status_mekanisasi = mekanisasi
                    )
                }
                if (mekanisasi == 0) {
                    val json = constructESPBJson(
                        blok_jjg = blok_jjg,
                        nopol = nopol,
                        driver = driver,
                        pemuat_id = pemuatListId.joinToString(","),
                        transporter_id = transporter_id,
                        mill_id = selectedMillId!!,
                        created_by_id = idPetugas!!,
                        no_espb = noESPBStr,
                        tph0 = tph0,
                        tph1 = tph1,
                        appVersion = appVersion,
                        osVersion = osVersion,
                        phoneModel = phoneModel
                    )
                    val encodedData = ListPanenTBSActivity().encodeJsonToBase64ZipQR(json)
                    val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageViewESPB)
                    ListPanenTBSActivity().generateHighQualityQRCode(encodedData!!, qrCodeImageView)
                    setMaxBrightness(this, true)
                }
            }
        }

        val formEspbMill = findViewById<LinearLayout>(R.id.formEspbMill)
        val spEspbMill = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spEspbMill.setOnItemSelectedListener { view, position, id, item ->
            val selectedMill = viewModel.millList.value?.get(position)
            selectedMillId = try {
                selectedMill?.id!!
            }catch (e: Exception){
                Toasty.error(this, "Terjadi Kesalahan saat mengambil ID Mill $e", Toasty.LENGTH_LONG).show()
                0
            }
        }
    }

    private fun setupViewModel() {
        val repository = AppRepository(this) // Get millDao from your database
        viewModelFactory = ESPBViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ESPBViewModel::class.java]
        Log.d("FormESPBActivityMill", "setupViewModel: $viewModel")
        viewModel.millList.observe(this) { mills ->
            updateSpinner(mills)
        }
    }

    private fun initViewModel() {

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinner(idSpinner: Int, list: List<String>):Int{
        var selectedID = 0
        val formEspbMill = findViewById<LinearLayout>(idSpinner)
        val editText = findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spinner = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner.setItems(list)
        spinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showPopupSearchDropdown(spinner, list, editText, formEspbMill) { selectedItem, position ->
                    spinner.text = selectedItem // Update spinner UI
                }
            }
            true // Consume event, preventing default behavior
        }
        return selectedID
    }

    fun getFormattedDateTime(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yy/HHmmssSSS", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    private fun setupSpinnerText(idSpinner: Int, desc: String, title: String) {
        val formEspbMill = findViewById<LinearLayout>(idSpinner)
        val spinner = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner.hint = desc
        formEspbMill.findViewById<TextView>(R.id.tvTitleFormPanenTBS).text = title
    }

    private fun updateSpinner(mills: List<MillModel>) {
        val formEspbMill = findViewById<LinearLayout>(R.id.formEspbMill)
        val spinner = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val loadingDots = formEspbMill.findViewById<LinearLayout>(R.id.loadingDotsContainer)

        if (mills.isEmpty()) {
            spinner.visibility = View.GONE
            loadingDots.visibility = View.VISIBLE
        } else {
            loadingDots.visibility = View.GONE
            spinner.visibility = View.VISIBLE

            // Convert mill list to display strings (abbreviations)
            val millNames = mills.map { it.abbr ?: "Unknown" }
            spinner.setItems(millNames)
        }
    }
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun setupHeader() {
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }

    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
        onItemSelected: (String, Int) -> Unit
    ) {
        val popupView = LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
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
                        val scrollAmount = spinnerLocation[1] - 400 // Scroll to show dropdown with extra space
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
        val adapter = object : ArrayAdapter<String>(spinner.context, android.R.layout.simple_list_item_1, filteredData) {
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
        val imm = spinner.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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

    private fun handleItemSelection(linearLayout: LinearLayout, position: Int, selectedItem: String) {
        when (linearLayout.id) {
            R.id.formEspbKemandoran -> {
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
                        Log.d("FormESPBActivityKemandoran", "pemuatList: $pemuatList")
                        val pemuatNames = pemuatList.map { it.nama.toString() }
                        Log.d("FormESPBActivityKemandoran", "pemuatNames: $pemuatNames")
                        withContext(Dispatchers.Main) {
                            setupSpinner(R.id.formEspbPemuat, pemuatNames)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching afdeling data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FormESPBActivity,
                                "Error loading afdeling data: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {

                        }
                    }
                }
            }
            R.id.formEspbTransporter -> {
                selectedTransporterId = try {
                    transporterList.find { it.nama == selectedItem }?.id!!
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedTransporterId: ${e.message}")
                    0
                }
                Log.d("FormESPBActivityTransporter", "selectedTransporterId: $selectedTransporterId")
            }
            R.id.formEspbPemuat -> {
                val karyawanMap = pemuatList.associateBy({ it.nama }, { it.id })
                Log.d("karyawanMap", "karyawanMap: $karyawanMap")
                val selectedPemanenId = karyawanMap[selectedItem]
                Log.d("karyawanMap", "selectedPemanenId: $selectedPemanenId")

                if (selectedPemanenId != null) {
                    val worker = Worker(selectedPemanenId.toString(), selectedItem)
                    selectedPemuatAdapter.addWorker(worker)
                    pemuatListId.add(selectedPemanenId) // Add the ID to your list

                    // Update available workers in adapter
                    selectedPemuatAdapter.setAvailableWorkers(pemuatList.map {
                        Worker(it.id.toString(), it.nama.toString())
                    })

                    // Get updated available workers for spinner
                    val availableWorkers = selectedPemuatAdapter.getAvailableWorkers()

                    Log.d("FormESPBActivityPemuat", "availableWorkers: $availableWorkers")
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinner(R.id.formEspbPemuat, availableWorkers.map { it.name })
                    }

                    AppLogger.d("Selected Worker: $selectedItem, ID: $selectedPemanenId")
                }
            }
        }
    }

    fun createCreatorInfo(appVersion: String, osVersion: String, phoneModel: String): JsonObject {
        return JsonObject().apply {
            addProperty("app_version", appVersion)
            addProperty("os_version", osVersion)
            addProperty("device_model", phoneModel)
        }
    }

    // Main JSON construction function
    fun constructESPBJson(
        blok_jjg: String,
        nopol: String,
        driver: String,
        pemuat_id: String,
        transporter_id: Int,
        mill_id: Int,
        created_by_id: Int,
        no_espb: String,
        tph0: String,
        tph1: String,
        appVersion: String,
        osVersion: String,
        phoneModel: String
    ): String {
        val gson = Gson()

        // Create the nested ESPB object
        val espbObject = JsonObject().apply {
            addProperty("blok_jjg", blok_jjg)
            addProperty("nopol", nopol)
            addProperty("driver", driver)
            addProperty("pemuat_id", pemuat_id)
            addProperty("transporter_id", transporter_id)
            addProperty("mill_id", mill_id)
            addProperty("created_by_id", created_by_id)
            add("creator_info", createCreatorInfo(appVersion, osVersion, phoneModel))
            addProperty("no_espb", no_espb)
            addProperty("created_at", getCurrentDateTime())
        }

        // Create the root object
        val rootObject = JsonObject().apply {
            add("espb", espbObject)
            addProperty("tph_0", tph0)
            addProperty("tph_1", tph1)
        }

        return gson.toJson(rootObject)
    }


    class ESPBViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ESPBViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ESPBViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private fun saveESPB(
        blok_jjg: String,
        created_by_id: Int,
        created_at: String,
        nopol: String,
        driver: String,
        transporter_id:Int,
        pemuat_id: String,
        mill_id: Int,
        tph0: String,
        tph1: String,
        creator_info: String,
        noESPB: String,
        status_draft: Int,
        status_mekanisasi: Int
        ){
        val vM = ViewModelProvider(this)[ESPBViewModel::class.java]

        // Example: Create ESPB data
        val espbList = listOf(
            ESPBEntity(
                blok_jjg = blok_jjg,
                created_by_id = created_by_id,
                created_at = created_at,
                nopol = nopol,
                driver = driver,
                transporter_id = transporter_id,
                pemuat_id = pemuat_id,
                mill_id = mill_id,
                archive = 0,
                tph0 = tph0,
                tph1 = tph1,
                creator_info = creator_info,
                noESPB = noESPB,
                status_draft = status_draft,
                status_mekanisasi = status_mekanisasi
            )
        )

        // Insert ESPB data
        try {
            vM.insertESPB(espbList)
            Toasty.success(this, "ESPB data inserted successfully", Toasty.LENGTH_LONG).show()
            viewModel.updateESPBStatus(idsToUpdate, 1)
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }catch (e: Exception){
            AppLogger.e("Error inserting ESPB data: ${e.message}")
            Toasty.error(this, "Error inserting ESPB data: ${e.message}", Toasty.LENGTH_LONG).show()
        }
    }

    private fun convertJanjangMapToString(janjangByBlock: Map<Int, Int>): String {
        return janjangByBlock.entries
            .joinToString(";") { (blockId, janjangSum) ->
                "$blockId,$janjangSum"
            }
    }


}