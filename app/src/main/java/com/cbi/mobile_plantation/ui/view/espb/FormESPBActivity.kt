package com.cbi.mobile_plantation.ui.view.espb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel
import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScreenshotUtil
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
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
import kotlinx.coroutines.runBlocking
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
    private var selectedNopol = "NULL"
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var pemuatList: List<KaryawanModel> = emptyList()
    private var transporterList: List<TransporterModel> = emptyList()
    private var nopolList: List<KendaraanModel> = emptyList()

    private lateinit var inputMappings: List<Triple<LinearLayout, String, FeaturePanenTBSActivity.InputType>>
    private lateinit var viewModelFactory: ESPBViewModelFactory
    private var pemuatListId: ArrayList<String> = ArrayList()
    private lateinit var selectedPemuatAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var thp1Map: Map<Int, Int>
    private lateinit var kemandoranMap: Map<String, Int>
    private lateinit var karyawanNikMap: Map<String, String>
    private var pemuatNama = "-"

    var idsToUpdate = listOf<Int>()
    var idsToUpdateNo = listOf<Int>()
    var idsToUpdateAdd = listOf<Int>()
    var divisiAbbr = ""
    var companyAbbr = ""
    var formattedJanjangString = ""
    var tph1IdPanen = ""
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private val karyawanIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranIdMap: MutableMap<String, Int> = mutableMapOf()
    private var activityInitialized = false
    private var noESPBStr = "NULL"
    private var pemuat_id = "NULL"
    private var kemandoran_id = "NULL"
    private var pemuat_nik = "NULL"
    private var tph1NoIdPanen = ""


    private var prefManager: PrefManager? = null
    private var divisiList: List<TPHNewModel> = emptyList()
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_espbactivity)

        checkDateTimeSettings()
    }

    private fun setupUI() {
        findViewById<ConstraintLayout>(R.id.headerFormESPB).findViewById<ImageView>(R.id.statusLocation)
            .apply {
                visibility = View.GONE
            }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialogUtility.withTwoActions(
                    this@FormESPBActivity,
                    "KEMBALI",
                    "Kembali ke Menu utama?",
                    "Data scan sebelumnya akan terhapus",
                    "warning.json",
                    function = {
                        startActivity(
                            Intent(
                                this@FormESPBActivity,
                                HomePageActivity::class.java
                            )
                        )
                        finishAffinity()
                    }
                ) {
                }
            }
        })
        try {
            featureName = intent.getStringExtra("FEATURE_NAME").toString()
        } catch (e: Exception) {
            Toasty.error(
                this,
                "Terjadi Kesalahan saat mengambil FEATURE NAME $e",
                Toasty.LENGTH_LONG
            ).show()
        }
        try {
            tph0 = intent.getStringExtra("tph_0").toString()
            Log.d("FormESPBActivityTPH0", "tph0: $tph0")
        } catch (e: Exception) {
            Toasty.error(this, "Terjadi Kesalahan saat mengambil TPH 0 $e", Toasty.LENGTH_LONG)
                .show()
        }
        AppLogger.d(tph1)
        try {
            tph1 = removeRecordsWithStatus2(intent.getStringExtra("tph_1").toString())
            Log.d("FormESPBActivityTPH1", "tph1: $tph1")
        } catch (e: Exception) {
            Toasty.error(this, "Terjadi Kesalahan saat mengambil TPH 1 $e", Toasty.LENGTH_LONG)
                .show()
        }


        AppLogger.d(tph1.toString())
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

        } catch (e: Exception) {
            Toasty.error(
                this,
                "Terjadi Kesalahan saat mengambil TPH 1 ID PANEN $e",
                Toasty.LENGTH_LONG
            ).show()
        }
        try {
            ///tph1IdPanen is sometin like 1,23,4,5,2,3
            tph1NoIdPanen = intent.getStringExtra("tph_normal").toString()
            // Split the string by comma to get individual IDs
            val idStrings = tph1NoIdPanen.split(",")

            // Convert each string ID to an integer
            idsToUpdateNo = idStrings.mapNotNull {
                it.trim().toIntOrNull()
                    ?: throw NumberFormatException("Invalid integer format: $it")
            }
            Log.d("FormESPBActivityIDS", "idsToUpdateNo: $idsToUpdateNo")

        } catch (e: Exception) {
            Toasty.error(
                this,
                "Tidak terdapat data scan TPH dari driver! $e",
                Toasty.LENGTH_LONG
            ).show()
        }
        // Then inside the setupUI() method, after both idsToUpdate and idsToUpdateNo are populated:
        try {
            // Calculate idsTpUpdateAdd as the difference between idsToUpdate and idsToUpdateNo
            idsToUpdateAdd = idsToUpdate.filter { it !in idsToUpdateNo }
            Log.d("FormESPBActivityIDS", "idsTpUpdateAdd: $idsToUpdateAdd")

        } catch (e: Exception) {
            Toasty.error(
                this,
                "Tidak terdapat data scan TPH dari driver! $e",
                Toasty.LENGTH_LONG
            ).show()
        }

        initViewModel()

        setupViewModel()
        Log.d("tph1", "tph1: $tph1")
        viewModel.janjangByBlock.observe(this) { janjangMap ->
            // Log each block and its janjang sum
            janjangMap.forEach { (blockId, janjangSum) ->
                Log.d("BlockJanjang", "Block $blockId: $janjangSum janjang")
            }
            AppLogger.d("jjgMap $janjangMap")
            // Convert the map to string format INSIDE the observer
            formattedJanjangString = convertJanjangMapToString(janjangMap)
            Log.d("FormattedJanjang", "Formatted string: $formattedJanjangString")
        }
        // Process the TPH data
        viewModel.processTPHData(tph1)

        //NBM 115
        //transporter 1
//        val formEspbNopol = findViewById<LinearLayout>(R.id.formEspbNopol)
//        val tvEspbNopol = formEspbNopol.findViewById<TextView>(R.id.tvTitlePaneEt)
//        val etEspbNopol = formEspbNopol.findViewById<EditText>(R.id.etPaneEt)
//        etEspbNopol.hint = "KH 2442 GF"
//        tvEspbNopol.text = "No. Polisi"

        val formEspbDriver = findViewById<LinearLayout>(R.id.formEspbDriver)
        val tvEspbDriver = formEspbDriver.findViewById<TextView>(R.id.tvTitlePaneEt)
        val etEspbDriver = formEspbDriver.findViewById<EditText>(R.id.etPaneEt)
        etEspbDriver.hint = "Fulan"
        tvEspbDriver.text = "Driver"

        val formEspbTransporter = findViewById<LinearLayout>(R.id.formEspbTransporter)
        val formEspbNopol = findViewById<LinearLayout>(R.id.formEspbNopol)

        setupSpinnerText(R.id.formEspbNopol, "No Polisi", "No Polisi")

        setupSpinnerText(R.id.formEspbMill, "Pilih Mill", "Mill")
        setupSpinnerText(R.id.formEspbKemandoran, "Pilih Kemandoran Pemuat", "Kemandoran Pemuat")
        setupSpinnerText(R.id.formEspbAfdeling, "Pilih Afdeling", "Afdeling")
        setupSpinnerText(R.id.formEspbTransporter, "Pilih Transporter", "Transporter")
        setupSpinnerText(R.id.formEspbPemuat, "Pilih Pemuat", "Pemuat")

        rvSelectedPemanen = findViewById<RecyclerView>(R.id.rvPemuat)
        selectedPemuatAdapter = SelectedWorkerAdapter()
        // Add this after initializing rvSelectedPemanen and selectedPemuatAdapter
        rvSelectedPemanen.apply {
            layoutManager = FlexboxLayoutManager(this@FormESPBActivity).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = selectedPemuatAdapter
        }

        prefManager = PrefManager(this)

        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        setupHeader()
        val idPetugas = try {
            prefManager!!.idUserLogin
        } catch (e: Exception) {
            Toasty.error(this, "Terjadi Kesalahan saat mengambil ID Petugas $e", Toasty.LENGTH_LONG)
                .show()
            0
        }
        val estatePetugas = try {
            prefManager!!.estateUserLogin
        } catch (e: Exception) {
            Toasty.error(
                this,
                "Terjadi Kesalahan saat mengambil Estate Petugas $e",
                Toasty.LENGTH_LONG
            ).show()
            "NULL"
        }
        idEstate = try {
            prefManager!!.estateIdUserLogin.toString().toInt()
        } catch (e: Exception) {
            Toasty.error(this, "Terjadi Kesalahan saat mengambil ID Estate $e", Toasty.LENGTH_LONG)
                .show()
            0
        }

//        val cbFormEspbMekanisasi = findViewById<MaterialCheckBox>(R.id.cbFormEspbMekanisasi)
//        cbFormEspbMekanisasi.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
////                formEspbTransporter.visibility = View.GONE
////                formEspbDriver.visibility = View.GONE
////                formEspbNopol.visibility = View.GONE
//                mekanisasi = 1
//            } else {
//                formEspbTransporter.visibility = View.VISIBLE
//                formEspbDriver.visibility = View.VISIBLE
//                formEspbNopol.visibility = View.VISIBLE
//                mekanisasi = 0
//            }
//        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val divisiDeferred = async {
                    try {
                        datasetViewModel.getDivisiList(estateId!!.toInt())
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching divisiList: ${e.message}")
                        emptyList() // Return an empty list to prevent crash
                    }
                }
                divisiList = divisiDeferred.await()

                // You can add more code here to handle the divisiList
                val nameDivisi: List<String> = divisiList.map { it.divisi_abbr.toString() }
                Log.d("FormESPBActivityDivisi", "nameDivisi: $nameDivisi")
                withContext(Dispatchers.Main) {
                    setupSpinner(R.id.formEspbAfdeling, nameDivisi)
                }

            } catch (e: Exception) {
                AppLogger.e("Error fetching divisi data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FormESPBActivity,
                        "Error loading divisi data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
                val spEspbTransporter =
                    formEspbTransporter.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                spEspbTransporter.setOnItemSelectedListener { view, position, id, item ->
                    val selectedTransporter = item.toString()

                    selectedTransporterId = try {
                        transporterList.find { it.nama == selectedTransporter }?.id!!
                    } catch (e: Exception) {
                        AppLogger.e("Error finding selectedTransporterId: ${e.message}")
                        0
                    }
                    Log.d(
                        "FormESPBActivityTransporter",
                        "selectedTransporterId: $selectedTransporterId"
                    )
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

            try {
                val nopolDeffered = async {
                    try {
                        datasetViewModel.getAllNopol()
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching Nopol List: ${e.message}")
                        emptyList()
                    }
                }
                nopolList = nopolDeffered.await()
                val nopol: List<String> = nopolList.map { it.no_kendaraan.toString() }
                Log.d("FormESPBActivityNopol", "Available nopol: $nopol")
                withContext(Dispatchers.Main) {
                    setupSpinner(R.id.formEspbNopol, nopol)

                    // If we have license plates available, set the first one as default
                    if (nopol.isNotEmpty()) {
                        selectedNopol = nopol[0]
                        Log.d("FormESPBActivityNopol", "Default selectedNopol: $selectedNopol")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error fetching nopol data: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FormESPBActivity,
                        "Error loading nopol data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
            } catch (e: Exception) {
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
        cbFormEspbTransporter.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                formEspbTransporter.visibility = View.GONE
                selectedTransporterId = 0
            } else {
                formEspbTransporter.visibility = View.VISIBLE
            }
        }

        val btnGenerateQRESPB = findViewById<FloatingActionButton>(R.id.btnGenerateQRESPB)
        btnGenerateQRESPB.setOnClickListener {

            Log.d("FormESPBActivity", "Generate QR clicked, selectedNopol = $selectedNopol")

            // Check if selectedNopol is valid
            if (selectedNopol.isNullOrBlank() || selectedNopol == "NULL") {
                Toasty.error(
                    this,
                    "Mohon lengkapi data No Polisi terlebih dahulu",
                    Toasty.LENGTH_LONG
                ).show()

                // Debug info for troubleshooting
                Log.e("FormESPBActivity", "Nopol validation failed: '$selectedNopol'")
                Log.e("FormESPBActivity", "Available nopols: ${nopolList.map { it.no_kendaraan }}")

                return@setOnClickListener
            }

            val driver = try {
                etEspbDriver.text.toString().replace(" ", "").uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Terjadi Kesalahan saat mengambil Driver $e", Toasty.LENGTH_LONG)
                    .show()
                "NULL"
            }

            val espbDate: String = try {
                getFormattedDateTime().toString()
            } catch (e: Exception) {
                Toasty.error(
                    this,
                    "Terjadi Kesalahan saat mengambil Tanggal ESPB $e",
                    Toasty.LENGTH_LONG
                ).show()
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
            noESPBStr = "$companyAbbr-$estatePetugas/$divisiAbbr/$espbDate"
            var transporter_id = 0
            transporter_id = if (cbFormEspbTransporter.isChecked) {
                0
            } else {
                selectedTransporterId
            }
            val creatorInfo = createCreatorInfo(
                appVersion = appVersion,
                osVersion = osVersion,
                phoneModel = phoneModel
            )
            val blok_jjg = try {
                formattedJanjangString
            } catch (e: Exception) {
                Toasty.error(
                    this,
                    "Terjadi Kesalahan saat mengambil Janjang Blok $e",
                    Toasty.LENGTH_LONG
                ).show()
                "NULL"
            }

            AppLogger.d("blok jjg $blok_jjg")
            val selectedPemanen = selectedPemuatAdapter.getSelectedWorkers()
            AppLogger.d(selectedPemanen.toString())

// Get a map of names to counts to determine which names have duplicates
            val workerNameCounts = mutableMapOf<String, Int>()
            selectedPemanen.forEach { worker ->
                val baseName = worker.name.substringBefore(" - ").trim()
                workerNameCounts[baseName] = (workerNameCounts[baseName] ?: 0) + 1
            }

// For worker ID lookup, handle both duplicate and non-duplicate cases
            val idKaryawanList = selectedPemanen.mapNotNull { worker ->
                val baseName = worker.name.substringBefore(" - ").trim()

                if (worker.name.contains(" - ")) {
                    // If name contains NIK, try with the full name first
                    karyawanIdMap[worker.name] ?: karyawanIdMap[baseName]
                } else {
                    // For names without NIK, just use the base name
                    karyawanIdMap[baseName]
                }
            }

            val kemandoranIdList = selectedPemanen.mapNotNull { worker ->
                val baseName = worker.name.substringBefore(" - ").trim()

                if (worker.name.contains(" - ")) {
                    // If name contains NIK, try with the full name first
                    kemandoranIdMap[worker.name] ?: kemandoranIdMap[baseName]
                } else {
                    // For names without NIK, just use the base name
                    kemandoranIdMap[baseName]
                }
            }

            val selectedNikPemanenIds = selectedPemanen.mapNotNull { worker ->
                if (worker.name.contains(" - ")) {
                    worker.name.substringAfter(" - ").trim()
                } else {
                    worker.id // Fallback to worker.id if NIK not in the name
                }
            }

            val uniqueNikPemanen = selectedNikPemanenIds
                .joinToString(",")

            val uniqueIdKaryawan = idKaryawanList
                .map { it.toString() }
                .joinToString(",")

            val uniqueKemandoranId = kemandoranIdList
                .map { it.toString() }
                .joinToString(",")

            runBlocking {
                try {
                    val idKaryawanStringList = idKaryawanList.map { it.toString() }

                    val job = lifecycleScope.async(Dispatchers.IO) {
                        try {
                            val result = weightBridgeViewModel.getPemuatByIdList(idKaryawanStringList)
                            result?.mapNotNull { "${it.nama} - ${it.nik}" }?.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") ?: "-"
                        } catch (e: Exception) {
                            AppLogger.e("Gagal mendapatkan data pemuat: ${e.message}")
                            "-"
                        }
                    }

                    // Wait for the job to complete and get the result
                    pemuatNama = job.await()

                } catch (e: Exception) {
                    AppLogger.e("Error getting pemuat data: ${e.message}")
                    pemuatNama = "-"

                    withContext(Dispatchers.Main) {
                        Toasty.error(
                            this@FormESPBActivity,
                            "Terjadi kesalahan saat mengambil data pemuat",
                            Toasty.LENGTH_LONG
                        ).show()
                    }
                }
            }


            AppLogger.d(tph1)
            val blokDisplay = getFormattedBlokDisplay(tph1)

            AppLogger.d(blokDisplay)

            if (selectedNopol == "NULL" || selectedNopol == "") {
                Toasty.error(
                    this,
                    "Mohon lengkapi data No Polisi terlebih dahulu",
                    Toasty.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (driver == "NULL" || driver == "") {
                Toasty.error(this, "Mohon lengkapi data Driver terlebih dahulu", Toasty.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            if (selectedTransporterId == 0 && !cbFormEspbTransporter.isChecked) {
                Toasty.error(
                    this,
                    "Mohon lengkapi data Transporter terlebih dahulu",
                    Toasty.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (selectedMillId == 0) {
                Toasty.error(this, "Mohon lengkapi data Mill terlebih dahulu", Toasty.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageViewESPB)
            AlertDialogUtility.Companion.withTwoActions(
                this,
                "SIMPAN",
                "KONFIRMASI BUAT QR ESPB?",
                "Pastikan seluruh data sudah valid!",
                "warning.json",
                function = {

                    val btKonfirmScanESPB = findViewById<MaterialButton>(R.id.btKonfirmScanESPB)
                    btKonfirmScanESPB.visibility = View.VISIBLE
                    btKonfirmScanESPB.setOnClickListener {
                        AlertDialogUtility.withTwoActions(
                            this,
                            getString(R.string.al_yes),
                            getString(R.string.confirmation_dialog_title),
                            "${getString(R.string.al_make_sure_scanned_qr)}",
                            "warning.json",
                            ContextCompat.getColor(
                                this,
                                R.color.bluedarklight
                            ),
                            function = {
                                val statusDraft = if (mekanisasi == 0) {
                                    1
                                } else {
                                    0
                                }
                                takeQRCodeScreenshot(qrCodeImageView, pemuatNama, driver, blokDisplay)
                                saveESPB(
                                    blok_jjg = blok_jjg,
                                    nopol = selectedNopol,
                                    driver = driver,
                                    pemuat_id = uniqueIdKaryawan,
                                    transporter_id = transporter_id,
                                    mill_id = selectedMillId!!,
                                    created_by_id = idPetugas!!,
                                    creator_info = creatorInfo.toString(),
                                    noESPB = noESPBStr,
                                    created_at = getCurrentDateTime(),
                                    tph0 = "",
                                    tph1 = tph1,
                                    status_draft = statusDraft,
                                    status_mekanisasi = mekanisasi,
                                    pemuat_nik = uniqueNikPemanen,
                                    kemandoran_id = uniqueKemandoranId
                                )
                            },
                            cancelFunction = {
                            }
                        )

                    }
                    if (mekanisasi == 0) {
                        val json = constructESPBJson(
                            blok_jjg = blok_jjg,
                            nopol = selectedNopol,
                            driver = driver,
                            pemuat_id = uniqueIdKaryawan,
                            transporter_id = transporter_id,
                            mill_id = selectedMillId!!,
                            created_by_id = idPetugas!!,
                            no_espb = noESPBStr,
                            tph0 = "",
                            tph1 = tph1,
                            appVersion = appVersion,
                            osVersion = osVersion,
                            phoneModel = phoneModel,
                            pemuat_nik = uniqueNikPemanen,
                            kemandoran_id = uniqueKemandoranId
                        )

                        val encodedData = ListPanenTBSActivity().encodeJsonToBase64ZipQR(json)

                        ListPanenTBSActivity().generateHighQualityQRCode(
                            encodedData!!,
                            qrCodeImageView
                        )
                        setMaxBrightness(this, true)
                        playSound(R.raw.berhasil_generate_qr)
//                        btKonfirmScanESPB.isEnabled = true


                    }
//                    btnGenerateQRESPB.isEnabled = true
                },
                cancelFunction = {
//                    btnGenerateQRESPB.isEnabled = true
                }
            )
        }

        val formEspbMill = findViewById<LinearLayout>(R.id.formEspbMill)
        val spEspbMill = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spEspbMill.setOnItemSelectedListener { view, position, id, item ->
            val selectedMill = viewModel.millList.value?.get(position)
            selectedMillId = try {
                selectedMill?.id!!
            } catch (e: Exception) {
                Toasty.error(
                    this,
                    "Terjadi Kesalahan saat mengambil ID Mill $e",
                    Toasty.LENGTH_LONG
                ).show()
                0
            }
        }

//        val formNopol = findViewById<LinearLayout>(R.id.formEspbNopol)
//        val spEspbNopol = formNopol.findViewById<MaterialSpinner>(R.id.spPanenTBS)
//        spEspbNopol.setOnItemSelectedListener { view, position, id, item ->
//            val selectedMill = viewModel.nopolList.value?.get(position)
//            selectedtNopol = try {
//                selectedMill?.no_kendaraan!!
//            } catch (e: Exception) {
//                Toasty.error(
//                    this,
//                    "Terjadi Kesalahan saat mengambil ID Mill $e",
//                    Toasty.LENGTH_LONG
//                ).show()
//                "NULL"
//            }
//        }
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
        val factory2 = WeighBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory2)[WeighBridgeViewModel::class.java]
    }

    private fun takeQRCodeScreenshot(sourceQrImageView: ImageView, pemuatNama :String, driver:String, blokDisplay:String) {

        lifecycleScope.launch {
            try {
                // Inflate custom screenshot layout
                val screenshotLayout = layoutInflater.inflate(R.layout.layout_screenshot_qr_mandor, null)

                // Get references to views in the custom layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView = screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
                val tvFooter = screenshotLayout.findViewById<TextView>(R.id.tvFooter)

                // Get references to included layouts
                val infoBlokList = screenshotLayout.findViewById<View>(R.id.infoBlokList)
                val infoTotalJjg = screenshotLayout.findViewById<View>(R.id.infoTotalJjg)
                val infoTotalTransaksi = screenshotLayout.findViewById<View>(R.id.infoTotalTransaksi)
                val infoNoESPB = screenshotLayout.findViewById<View>(R.id.infoNoESPB)
                val infoDriver = screenshotLayout.findViewById<View>(R.id.infoDriver)
                val infoNopol = screenshotLayout.findViewById<View>(R.id.infoNopol)
                val infoPemuat = screenshotLayout.findViewById<View>(R.id.infoPemuat)

                // Helper function to set label and value for included layouts
                fun setInfoData(includeView: View, labelText: String, valueText: String) {
                    val tvLabel = includeView.findViewById<TextView>(R.id.tvLabel)
                    val tvValue = includeView.findViewById<TextView>(R.id.tvValue)
                    tvLabel.text = labelText
                    tvValue.text = valueText
                }

                val tphRecords = tph1.split(";")

                val tphIds = tphRecords.mapNotNull { record ->
                    val parts = record.split(",")
                    if (parts.isNotEmpty()) {
                        parts[0].toIntOrNull()  // Extract the TPH ID (first part)
                    } else {
                        null
                    }
                }

                val tphCount = tphIds.size

                val blokJjgList = tphRecords.mapNotNull { record ->
                    val parts = record.split(",")
                    if (parts.size >= 3) {  // Make sure there are at least 3 parts
                        val id = parts[0].toIntOrNull()
                        val jjg = parts[2].toIntOrNull()  // The jjg value is at index 2
                        if (id != null && jjg != null) {
                            id to jjg
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                val totalJjg = blokJjgList.sumOf { it.second }

                // Get the QR code bitmap from the passed ImageView
                val qrBitmap = sourceQrImageView.drawable?.let { drawable ->
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        // Convert drawable to bitmap if not already a BitmapDrawable
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    }
                }

                // Set QR code image
                qrCodeImageView.setImageBitmap(qrBitmap)

                // Generate current date and time for footer
                val currentDate = Date()
                val indonesianLocale = Locale("id", "ID")
                val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val formattedDate = dateFormat.format(currentDate).toUpperCase(indonesianLocale)
                val formattedTime = timeFormat.format(currentDate)

                // Set data for eSPB
                tvUserName.text = "Hasil QR dari ${prefManager!!.nameUserLogin}"
                setInfoData(infoBlokList, "Blok", ": $blokDisplay")
                setInfoData(infoTotalJjg, "Total Janjang", ": $totalJjg")
                setInfoData(infoTotalTransaksi, "Jumlah Transaksi", ": $tphCount")
                setInfoData(infoNoESPB, "E-SPB", ": $noESPBStr")
                setInfoData(infoDriver, "Driver", ": $driver")
                setInfoData(infoNopol, "Nomor Polisi", ": $selectedNopol")
                setInfoData(infoPemuat, "Pemuat", ": $pemuatNama")


                tvFooter.text = "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

                // Measure and layout the screenshot view
                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = LinearLayout.LayoutParams.WRAP_CONTENT

                screenshotLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                screenshotLayout.layout(
                    0, 0, screenshotLayout.measuredWidth, screenshotLayout.measuredHeight
                )

                // Create a meaningful filename
                val screenshotFileName = "eSPB_QR_${noESPBStr.replace("/", "_")}"

                // Take the screenshot of the custom layout
                val screenshotFile = ScreenshotUtil.takeScreenshot(
                    screenshotLayout,
                    screenshotFileName,
                    AppUtils.WaterMarkFotoDanFolder.WMESPB
                )

                if (screenshotFile != null) {
                    Toasty.success(this@FormESPBActivity, "QR sudah tersimpan digaleri", Toast.LENGTH_LONG, true).show()
                }
            } catch (e: Exception) {
                AppLogger.e("Error taking QR screenshot: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FormESPBActivity,
                        "Gagal menyimpan QR Code: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinner(idSpinner: Int, list: List<String>): Int {
        var selectedID = 0
        val formEspbMill = findViewById<LinearLayout>(idSpinner)
        val editText = findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spinner = formEspbMill.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner.setItems(list)
        spinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showPopupSearchDropdown(
                    spinner,
                    list,
                    editText,
                    formEspbMill
                ) { selectedItem, position ->
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
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.GONE

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

                    // Normalize search input by removing spaces
                    val normalizedSearch = s.toString().replace(" ", "").lowercase()

                    // Filter using normalized comparison
                    data.filter { item ->
                        // Normalize item by removing spaces for comparison
                        val normalizedItem = item.replace(" ", "").lowercase()
                        normalizedItem.contains(normalizedSearch)
                    }
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

    private fun handleItemSelection(
        linearLayout: LinearLayout,
        position: Int,
        selectedItem: String
    ) {
        when (linearLayout.id) {
            R.id.formEspbAfdeling -> {

                val selectedDivisiId = try {
                    divisiList.find { it.divisi_abbr == selectedItem }?.divisi
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedDivisiId: ${e.message}")
                    null
                }
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

                    try {

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
                        kemandoranList = kemandoranDeferred.await()

                        withContext(Dispatchers.Main) {
                            try {
                                val kemandoranNames = kemandoranList.map { it.nama }
                                setupSpinner(
                                    R.id.formEspbKemandoran,
                                    kemandoranNames as List<String>
                                )
                            } catch (e: Exception) {
                                AppLogger.e("Error updating UI: ${e.message}")
                            }
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
                    }
                }
            }

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
                Log.d(
                    "FormESPBActivityTransporter",
                    "selectedTransporterId: $selectedTransporterId"
                )
            }

            R.id.formEspbPemuat -> {
                val selectedItem = selectedItem.toString()

                // Check if selectedItem contains a NIK (for duplicate names)
                val hasNik = selectedItem.contains(" - ")
                val selectedName = selectedItem.substringBefore(" - ").trim()
                val selectedNik = if (hasNik) selectedItem.substringAfter(" - ").trim() else null

                // Create a NIK to Employee map for lookup by NIK
                val nikToEmployeeMap = pemuatList.filter { it.nik != null }
                    .associateBy { it.nik!! }

                // Create a map to count occurrences of each name
                val nameCounts = mutableMapOf<String, Int>()
                pemuatList.forEach {
                    it.nama?.trim()?.let { nama ->
                        nameCounts[nama] = (nameCounts[nama] ?: 0) + 1
                    }
                }

                // Update map keys to include NIK for duplicate names
                pemuatList.forEach {
                    it.nama?.trim()?.let { nama ->
                        val key = if (nameCounts[nama]!! > 1) {
                            "$nama - ${it.nik}"
                        } else {
                            nama
                        }
                        karyawanIdMap[key] = it.id!!
                        kemandoranIdMap[key] = it.kemandoran_id!!
                    }
                }

                // Find the selected employee either by NIK or name
                val selectedEmployee = if (selectedNik != null) {
                    nikToEmployeeMap[selectedNik]
                } else {
                    pemuatList.find { it.nama?.trim() == selectedName }
                }

                if (selectedEmployee != null) {
                    val worker = Worker(selectedEmployee.toString(), selectedItem)
                    selectedPemuatAdapter.addWorker(worker)
                    pemuatListId.add(selectedEmployee.toString())

                    // Update available workers with proper name formatting
                    selectedPemuatAdapter.setAvailableWorkers(pemuatList.map {
                        val workerName = if (nameCounts[it.nama?.trim()] ?: 0 > 1) {
                            "${it.nama?.trim()} - ${it.nik}"
                        } else {
                            it.nama?.trim() ?: ""
                        }
                        Worker(it.id.toString(), workerName)
                    })

                    // Get updated available workers for spinner
                    val availableWorkers = selectedPemuatAdapter.getAvailableWorkers()

                    Log.d("FormESPBActivityPemuat", "availableWorkers: $availableWorkers")
                    if (availableWorkers.isNotEmpty()) {
                        setupSpinner(R.id.formEspbPemuat, availableWorkers.map { it.name })
                    }

                    AppLogger.d("Selected Worker: $selectedItem, ID: ${selectedEmployee.id}")
                }
            }

            R.id.formEspbNopol -> {
                selectedNopol = selectedItem
                Log.d("FormESPBActivityNopol", "Selected nopol: $selectedNopol")
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
        phoneModel: String,
        kemandoran_id: String,
        pemuat_nik: String
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
            addProperty("kemandoran_id", kemandoran_id)
            addProperty("pemuat_nik", pemuat_nik)
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

    private fun getFormattedBlokDisplay(tph1: String): String {
        var formattedBlokDisplay = "-"

        runBlocking {
            try {
                // Parse TPH records
                val tphRecords = tph1.split(";")

                // Extract TPH IDs
                val tphIds = tphRecords.mapNotNull { record ->
                    val parts = record.split(",")
                    if (parts.isNotEmpty()) {
                        parts[0].toIntOrNull()
                    } else {
                        null
                    }
                }

                // Extract JJG data with TPH ID mapping
                val tphJjgMap = tphRecords.mapNotNull { record ->
                    val parts = record.split(",")
                    if (parts.size >= 3) {
                        val id = parts[0].toIntOrNull()
                        val jjg = parts[2].toIntOrNull()
                        if (id != null && jjg != null) {
                            id to jjg
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }.toMap()

                // Fetch TPH data from database
                val tphList = withContext(Dispatchers.IO) {
                    datasetViewModel.getTPHsByIds(tphIds)
                }

                // Create a lookup map of id to TPH
                val tphLookupMap = tphList.associateBy { it.id }

                // Process each record separately
                val blokJjgMap = mutableMapOf<String, Int>()
                val blokCountMap = mutableMapOf<String, Int>()

                tphRecords.forEach { record ->
                    val parts = record.split(",")
                    if (parts.size >= 3) {
                        val tphId = parts[0].toIntOrNull()
                        val jjg = parts[2].toIntOrNull() ?: 0

                        if (tphId != null) {
                            val tph = tphLookupMap[tphId]
                            val blokKode = tph?.blok_kode ?: "-"

                            // Increment JJG sum for this block
                            blokJjgMap[blokKode] = (blokJjgMap[blokKode] ?: 0) + jjg

                            // Increment record count for this block
                            blokCountMap[blokKode] = (blokCountMap[blokKode] ?: 0) + 1
                        }
                    }
                }

                // Build the formatted display string
                formattedBlokDisplay = blokJjgMap.keys
                    .sortedBy { it }
                    .mapNotNull { blokKode ->
                        val jjgSum = blokJjgMap[blokKode] ?: 0
                        val recordCount = blokCountMap[blokKode] ?: 0

                        if (recordCount > 0) {
                            "$blokKode($jjgSum/$recordCount)"
                        } else {
                            null
                        }
                    }
                    .joinToString(", ")

                if (formattedBlokDisplay.isEmpty()) {
                    formattedBlokDisplay = "-"
                }

            } catch (e: Exception) {
                AppLogger.e("Error formatting blok display: ${e.message}")
                formattedBlokDisplay = "-"
            }
        }

        return formattedBlokDisplay
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

    override fun onResume() {
        super.onResume()
        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {
        super.onPause()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        SoundPlayer.releaseMediaPlayer()
        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun saveESPB(
        blok_jjg: String,
        created_by_id: Int,
        created_at: String,
        nopol: String,
        driver: String,
        transporter_id: Int,
        pemuat_id: String,
        kemandoran_id: String,
        pemuat_nik: String,
        mill_id: Int,
        tph0: String,
        tph1: String,
        creator_info: String,
        noESPB: String,
        status_draft: Int,
        status_mekanisasi: Int,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Create ESPB entity
                val espbEntity = ESPBEntity(
                    blok_jjg = blok_jjg,
                    created_by_id = created_by_id,
                    created_at = created_at,
                    nopol = nopol,
                    driver = driver,
                    transporter_id = transporter_id,
                    pemuat_id = pemuat_id,
                    mill_id = mill_id,
                    archive = 0,
                    tph0 = "",
                    tph1 = tph1,
                    creator_info = creator_info,
                    noESPB = noESPB,
                    status_draft = status_draft,
                    status_mekanisasi = status_mekanisasi,
                    kemandoran_id = kemandoran_id,
                    pemuat_nik = pemuat_nik,
                    ids_to_update = idsToUpdate.joinToString(",")
                )

                // Insert ESPB and get the ID
                val espbId = viewModel.insertESPBAndGetId(espbEntity)

                // Update related records with the ESPB reference
                if (espbId > 0) {
                    viewModel.updateESPBStatus(idsToUpdate, 1, noESPB)
                    viewModel.panenUpdateStatusAngkut(idsToUpdateNo, 1)
                    viewModel.panenUpdateStatusAngkut(idsToUpdateAdd, 2)

                    // Log successful operation
                    AppLogger.i("ESPB saved successfully with ID: $espbId")

                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        playSound(R.raw.berhasil_konfirmasi)
                        showSuccessAndNavigate()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toasty.error(
                            this@FormESPBActivity,
                            "Gagal menyimpan ESPB: ID tidak valid",
                            Toasty.LENGTH_LONG
                        ).show()
                    }
                }
                try {
                    viewModel.deleteESPBById(intent.getIntExtra("id_espb", 0))
                } catch (e: Exception) {
                    Log.e("FormESPBActivity", "Error parsing id_espb: ${e.message}")
                }
            } catch (e: Exception) {
                AppLogger.e("Error saving ESPB data", e.toString())

                withContext(Dispatchers.Main) {

                    // Show specific error message based on exception type
                    val errorMessage = when {
                        e.message?.contains("UNIQUE constraint failed") == true ->
                            "ESPB dengan nomor tersebut sudah ada"

                        e.message?.contains("foreign key constraint") == true ->
                            "Referensi data tidak valid"

                        else -> "Gagal menyimpan data ESPB: ${e.message}"
                    }

                    Toasty.error(this@FormESPBActivity, errorMessage, Toasty.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showSuccessAndNavigate() {
        // Use a more descriptive success message
        AlertDialogUtility.withSingleAction(
            this,
            "OK",
            "eSPB Telah Disimpan!",
            "Data ESPB berhasil disimpan dengan nomor: $noESPBStr",
            "success.json"
        ) {
            // Navigate back when dialog is dismissed
            val intent = Intent(this, HomePageActivity::class.java)
            intent.putExtra("REFRESH_ESPB_LIST", true)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun convertJanjangMapToString(janjangByBlock: Map<Int, Int>): String {
        return janjangByBlock.entries
            .joinToString(";") { (blockId, janjangSum) ->
                "$blockId,$janjangSum"
            }
    }

    private fun removeRecordsWithStatus2(dataString: String): String {
        // Parse the string into individual records
        val records = dataString.split(";")

        // Filter out records where status_espb = 2
        val filteredRecords = records.filter { record ->
            val fields = record.split(",")
            if (fields.size >= 4) {
                val statusEspb = fields[3].trim()
                statusEspb != "2"
            } else {
                true // Keep records that don't match our expected format
            }
        }

        // Join the filtered records back into a string
        return filteredRecords.joinToString(";")
    }

}