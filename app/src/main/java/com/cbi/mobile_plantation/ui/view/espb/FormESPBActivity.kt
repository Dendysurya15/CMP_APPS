package com.cbi.mobile_plantation.ui.view.espb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.data.model.TPHNewModel
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
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
    private lateinit var backButton: ImageView
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
    private val karyawanNamaMap = mutableMapOf<String, String>()
    private var activityInitialized = false
    private var noESPBStr = "NULL"
    private var tph1NoIdPanen = ""
    private var selectedTransporterName = ""
    private lateinit var warningText: TextView
    private lateinit var warningCard: CardView


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

    private fun setupWarningCard() {
        warningCard = findViewById(R.id.warning_card)
        warningText = warningCard.findViewById(R.id.warningText)
        updateWarningText()
        warningCard.findViewById<ImageButton>(R.id.btnCloseWarning).setOnClickListener {
            warningCard.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWarningText() {
        warningText.text = "Pastikan semua form terisi dengan benar. Jika QR Code sudah tampil, lakukan scan dan konfirmasi dengan menekan tombol yang tersedia."
        warningText.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun createScannedResultFromTPH1(): String {
        if (tph1.isEmpty()) return ""

        val panenIds = tph1IdPanen.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return "[${panenIds.joinToString(", ")}]" // Format as list of IDs for restoration
    }

    private fun navigateBackToListTPH() {
        val intent = Intent(this, ListPanenTBSActivity::class.java)

        // Pass the preserved scan data back
        intent.putExtra("FEATURE_NAME", featureName)
        intent.putExtra("scannedResult", createScannedResultFromTPH1())
        intent.putExtra("previous_tph_1", tph1)
        intent.putExtra("previous_tph_0", tph0)
        intent.putExtra("previous_tph_1_id_panen", tph1IdPanen)
        intent.putExtra("RESTORE_CHECKBOX_STATE", true) // Flag to restore checkbox states

        AppLogger.d("Navigating back to ListPanenTBSActivity with data:")
        AppLogger.d("tph1: $tph1")
        AppLogger.d("tph0: $tph0")
        AppLogger.d("tph1IdPanen: $tph1IdPanen")
        AppLogger.d("createScannedResultFromTPH1(): ${createScannedResultFromTPH1()}")

        startActivity(intent)
        finishAffinity()
    }

    private fun setupUI() {
        backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener {
            onBackPressed()
        }
        setupWarningCard()
        findViewById<ConstraintLayout>(R.id.headerFormESPB).findViewById<ImageView>(R.id.statusLocation)
            .apply {
                visibility = View.GONE
            }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialogUtility.withTwoActions(
                    this@FormESPBActivity,
                    "Kembali",
                    "Kembali ke List TPH?",
                    "Data yang sudah diisi akan hilang",
                    "warning.json",
                    function = {
                        navigateBackToListTPH()
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


        AppLogger.d("tph1 $tph1")
        try {
            ///tph1IdPanen is sometin like 1,23,4,5,2,3
            AppLogger.d("tph_1_id_panen ${intent.getStringExtra("tph_1_id_panen")}")
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

            AppLogger.d("tph_normal ${intent.getStringExtra("tph_normal")}")
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
                val nameDivisi: List<String> = divisiList.sortedBy { it.divisi_abbr }.map { it.divisi_abbr.toString() }
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
                        transporterList.find { it.nama == selectedTransporter }?.kode!!.toInt()
                    } catch (e: Exception) {
                        AppLogger.e("Error finding selectedTransporterId: ${e.message}")
                        0
                    }
                    Log.d(
                        "FormESPBActivityTransporter",
                        "selectedTransporterId: $selectedTransporterId"
                    )

                    AppLogger.d("select4edTransposertId $selectedTransporterId")
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
            btnGenerateQRESPB.isEnabled = false
            btnGenerateQRESPB.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.graytextdark))



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

                enableButtonAndSpinners()
                return@setOnClickListener
            }

            val driver = try {
                etEspbDriver.text.toString().replace(" ", "").uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Terjadi Kesalahan saat mengambil Driver $e", Toasty.LENGTH_LONG)
                    .show()
                enableButtonAndSpinners()  // Re-enable if there's an error
                return@setOnClickListener
            }

            val espbDate: String = try {
                getFormattedDateTime().toString()
            } catch (e: Exception) {
                Toasty.error(
                    this,
                    "Terjadi Kesalahan saat mengambil Tanggal ESPB $e",
                    Toasty.LENGTH_LONG
                ).show()
                enableButtonAndSpinners()
                return@setOnClickListener
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

            AppLogger.d("transporter_id $transporter_id")
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
                enableButtonAndSpinners()  // Re-enable if there's an error
                return@setOnClickListener
            }

            val selectedPemuat = selectedPemuatAdapter.getSelectedWorkers()
            AppLogger.d("selectedPemuat: $selectedPemuat")

            AppLogger.d("karyawanIdMap $karyawanIdMap")

            val idKaryawanList = selectedPemuat.mapNotNull { worker ->
                // First try to get ID using the full name (with NIK if present)
                var id = karyawanIdMap[worker.name]

                // If that fails and the name contains a NIK separator, try with just the base name
                if (id == null && worker.name.contains(" - ")) {
                    val baseName = worker.name.substringBefore(" - ").trim()
                    id = karyawanIdMap[baseName]
                }

                // If that still fails and we don't have a NIK separator, try all possible matches
                // that start with this name (handles case where map has "NAME - NIK" but worker just has "NAME")
                if (id == null && !worker.name.contains(" - ")) {
                    // Find any key in the map that starts with this worker's name followed by " - "
                    val possibleKey =
                        karyawanIdMap.keys.find { it.startsWith("${worker.name} - ") }
                    if (possibleKey != null) {
                        id = karyawanIdMap[possibleKey]
                    }
                }

                id
            }

            val kemandoranIdList = selectedPemuat.mapNotNull { worker ->
                var id = kemandoranIdMap[worker.name]

                // If that fails and the name contains a NIK separator, try with just the base name
                if (id == null && worker.name.contains(" - ")) {
                    val baseName = worker.name.substringBefore(" - ").trim()
                    id = kemandoranIdMap[baseName]
                }

                // If that still fails and we don't have a NIK separator, try all possible matches
                if (id == null && !worker.name.contains(" - ")) {
                    val possibleKey =
                        kemandoranIdMap.keys.find { it.startsWith("${worker.name} - ") }
                    if (possibleKey != null) {
                        id = kemandoranIdMap[possibleKey]
                    }
                }

                id
            }

// If you need to maintain karyawanNamaMap logic like in original code
            val selectedNamaPemuatList = selectedPemuat.mapNotNull { worker ->
                // First try to get name using the full worker name (with NIK if present)
                var nama = karyawanNamaMap[worker.name]

                // If that fails and the name contains a NIK separator, try with just the base name
                if (nama == null && worker.name.contains(" - ")) {
                    val baseName = worker.name.substringBefore(" - ").trim()
                    nama = karyawanNamaMap[baseName]
                }

                // If that still fails and we don't have a NIK separator, try all possible matches
                if (nama == null && !worker.name.contains(" - ")) {
                    // Find any key in the map that starts with this worker's name followed by " - "
                    val possibleKey = karyawanNamaMap.keys.find { it.startsWith("${worker.name} - ") }
                    if (possibleKey != null) {
                        nama = karyawanNamaMap[possibleKey]
                    } else {
                        // If all else fails, use the worker name itself
                        nama = worker.name
                    }
                }

                nama
            }

            val selectedNikPemuatIds = selectedPemuat.mapNotNull { worker ->
                if (worker.name.contains(" - ")) {
                    // Find the last occurrence of " - " to extract only the NIK
                    val lastDashIndex = worker.name.lastIndexOf(" - ")
                    if (lastDashIndex != -1) {
                        worker.name.substring(lastDashIndex + 3).trim()
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

// Final joining of all collected data
            val uniqueNamaPemuat = selectedNamaPemuatList.joinToString(",")

            val uniqueNikPemanen = selectedNikPemuatIds.joinToString(",")

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
                            val result =
                                weightBridgeViewModel.getPemuatByIdList(idKaryawanStringList)
                            result?.mapNotNull { "${it.nama} - ${it.nik}" }
                                ?.takeIf { it.isNotEmpty() }
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

            val blokDisplay = getFormattedBlokDisplay(tph1)
            AppLogger.d(blokDisplay)

            if (selectedNopol == "NULL" || selectedNopol == "") {
                Toasty.error(
                    this,
                    "Mohon lengkapi data No Polisi terlebih dahulu",
                    Toasty.LENGTH_LONG
                ).show()
                enableButtonAndSpinners()  // Re-enable if validation fails
                return@setOnClickListener
            }
            if (driver == "NULL" || driver == "") {
                Toasty.error(this, "Mohon lengkapi data Driver terlebih dahulu", Toasty.LENGTH_LONG)
                    .show()
                enableButtonAndSpinners()  // Re-enable if validation fails
                return@setOnClickListener
            }
            if (selectedTransporterId == 0 && !cbFormEspbTransporter.isChecked) {
                Toasty.error(
                    this,
                    "Mohon lengkapi data Transporter terlebih dahulu",
                    Toasty.LENGTH_LONG
                ).show()
                enableButtonAndSpinners()  // Re-enable if validation fails
                return@setOnClickListener
            }
            if (selectedMillId == 0) {
                Toasty.error(this, "Mohon lengkapi data Mill terlebih dahulu", Toasty.LENGTH_LONG)
                    .show()
                enableButtonAndSpinners()  // Re-enable if validation fails
                return@setOnClickListener
            }

            val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageViewESPB)

            val btnPreviewFullQR: MaterialButton = findViewById(R.id.btnPreviewFullQR)


            AlertDialogUtility.Companion.withTwoActions(
                this,
                "SIMPAN",
                "KONFIRMASI BUAT QR ESPB?",
                "Pastikan seluruh data sudah valid!",
                "warning.json",
                function = {

                    disableAllSpinners()
                    val statusDraft = if (mekanisasi == 0) {
                        1
                    } else {
                        0
                    }

                    if (mekanisasi == 0) {

                        val json = constructESPBJson(
                            blok_jjg = blok_jjg,
                            nopol = selectedNopol,
                            driver = driver,
                            pemuat_id = uniqueIdKaryawan,
                            transporter_id = transporter_id,
                            mill_id = selectedMillId,
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

                        AppLogger.d("json $json")

                        val encodedData = ListPanenTBSActivity().encodeJsonToBase64ZipQR(json)



                        if (encodedData != null) {
                            // Data is valid size, proceed with QR generation
                            AppLogger.d("Encoded data valid: ${encodedData.take(50)}...")

                            ListPanenTBSActivity().generateHighQualityQRCode(
                                encodedData,
                                qrCodeImageView,
                                this@FormESPBActivity,
                                showLogo = false
                            )
                            setMaxBrightness(this, true)
                            playSound(R.raw.berhasil_generate_qr)

                            takeQRCodeScreenshot(
                                qrCodeImageView,
                                pemuatNama,
                                driver,
                                blokDisplay
                            )

                            saveESPB(
                                blok_jjg = blok_jjg,
                                nopol = selectedNopol,
                                driver = driver,
                                pemuat_id = uniqueIdKaryawan,
                                transporter_id = transporter_id,
                                mill_id = selectedMillId,
                                created_by_id = idPetugas,
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

                            btnPreviewFullQR.visibility = View.VISIBLE
                            btnPreviewFullQR.setOnClickListener {
                                showQrCodeFullScreen(qrCodeImageView.drawable)
                            }

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
                                        playSound(R.raw.berhasil_konfirmasi)
                                        showSuccessAndNavigate()

                                    },
                                    cancelFunction = {
                                        // Don't re-enable since we're still in the outer dialog
                                    }
                                )
                            }
                        } else {
                            // Data is too large or encoding failed
                            AppLogger.e("QR generation failed - data too large or encoding error")

                            Toast.makeText(this, "Tidak dapat membuat QR Code - data terlalu besar", Toast.LENGTH_LONG).show()

                            enableButtonAndSpinners()
                        }

                    }
                },
                cancelFunction = {
                    // Re-enable the button if the user cancels the operation
                    enableButtonAndSpinners()
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

    private fun disableAllSpinners() {
        // Disable all spinners
        disableSpinner(R.id.formEspbNopol)
        disableSpinner(R.id.formEspbMill)
        disableSpinner(R.id.formEspbKemandoran)
        disableSpinner(R.id.formEspbAfdeling)
        disableSpinner(R.id.formEspbTransporter)
        disableSpinner(R.id.formEspbPemuat)

        // Also disable the driver input field
        val formEspbDriver = findViewById<LinearLayout>(R.id.formEspbDriver)
        val etEspbDriver = formEspbDriver.findViewById<EditText>(R.id.etPaneEt)
        etEspbDriver.isEnabled = false
        etEspbDriver.alpha = 0.5f

        val cbFormEspbTransporter = findViewById<MaterialCheckBox>(R.id.cbFormEspbTransporter)
        cbFormEspbTransporter.isEnabled = false
        cbFormEspbTransporter.alpha = 0.5f

        disableRecyclerViewItems(rvSelectedPemanen)
    }


    private fun disableRecyclerViewItems(recyclerView: RecyclerView) {
        // First, make the RecyclerView non-interactive
        recyclerView.alpha = 0.8f

        // Loop through all visible child views to disable remove buttons
        for (i in 0 until recyclerView.childCount) {
            val itemView = recyclerView.getChildAt(i)

            // Find and disable the remove button in each item
            val removeButton = itemView.findViewById<ImageView>(R.id.remove_worker)
            removeButton?.let {
                it.isEnabled = false
                it.alpha = 0.5f

                // Prevent click events by setting a dummy listener that does nothing
                it.setOnClickListener(null)
            }
        }

        // Store original OnItemTouchListener(s) and replace with one that blocks interactions
        // We'll re-enable interactions in enableRecyclerViewItems
        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Intercept all touch events to prevent interactions
                return true
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // Do nothing with the touch events
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // No implementation needed
            }
        })
    }

    /**
     * Enables the button and all spinners
     */
    private fun enableButtonAndSpinners() {
        // Re-enable the button with original color
        val btnGenerateQRESPB = findViewById<FloatingActionButton>(R.id.btnGenerateQRESPB)
        btnGenerateQRESPB.isEnabled = true
        btnGenerateQRESPB.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.greendarkerbutton))

        // Re-enable all spinners
        enableSpinner(R.id.formEspbNopol)
        enableSpinner(R.id.formEspbMill)
        enableSpinner(R.id.formEspbKemandoran)
        enableSpinner(R.id.formEspbAfdeling)
        enableSpinner(R.id.formEspbTransporter)
        enableSpinner(R.id.formEspbPemuat)

        // Re-enable the driver input field
        val formEspbDriver = findViewById<LinearLayout>(R.id.formEspbDriver)
        val etEspbDriver = formEspbDriver.findViewById<EditText>(R.id.etPaneEt)
        etEspbDriver.isEnabled = true
        etEspbDriver.alpha = 1.0f
    }

    /**
     * Disables a specific spinner
     */
    private fun disableSpinner(spinnerId: Int) {
        val spinnerLayout = findViewById<LinearLayout>(spinnerId)
        val spinner = spinnerLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner.isEnabled = false
        spinner.alpha = 0.5f // Reduce opacity to visually indicate disabled state

        // Also disable the TextView title for a consistent look
        val tvTitle = spinnerLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        tvTitle.alpha = 0.5f
    }

    /**
     * Enables a specific spinner
     */
    private fun enableSpinner(spinnerId: Int) {
        val spinnerLayout = findViewById<LinearLayout>(spinnerId)
        val spinner = spinnerLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        spinner.isEnabled = true
        spinner.alpha = 1.0f // Restore full opacity

        // Also restore the TextView title
        val tvTitle = spinnerLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        tvTitle.alpha = 1.0f
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

    private fun makeQRLayoutSquare(screenshotLayout: View) {
        val qrLayout = screenshotLayout.findViewById<FrameLayout>(R.id.fLayoutQR)

        // Get screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate square size (80% of screen width with padding)
        val padding = (32 * resources.displayMetrics.density).toInt() // 32dp padding
        val squareSize = screenWidth - padding

        // Set equal width and height
        val layoutParams = qrLayout.layoutParams
        layoutParams.width = squareSize
        layoutParams.height = squareSize
        qrLayout.layoutParams = layoutParams
    }

    private fun takeQRCodeScreenshot(
        sourceQrImageView: ImageView,
        pemuatNama: String,
        driver: String,
        blokDisplay: String
    ) {

        lifecycleScope.launch {
            try {
                // Inflate custom screenshot layout
                val screenshotLayout =
                    layoutInflater.inflate(R.layout.layout_screenshot_qr_mandor, null)
                makeQRLayoutSquare(screenshotLayout)
                // Get references to views in the custom layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView = screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
                val tvFooter = screenshotLayout.findViewById<TextView>(R.id.tvFooter)

                // Get references to included layouts
                val infoBlokList = screenshotLayout.findViewById<View>(R.id.infoBlokList)
                val infoTotalJjg = screenshotLayout.findViewById<View>(R.id.infoTotalJjg)
                val infoTotalTransaksi = screenshotLayout.findViewById<View>(R.id.infoTotalTransaksi)
                val infoNoESPB = screenshotLayout.findViewById<View>(R.id.infoNoESPB)
                val infoTransporter = screenshotLayout.findViewById<View>(R.id.infoTransporter)
                val infoDriver = screenshotLayout.findViewById<View>(R.id.infoDriver)
                val infoNopol = screenshotLayout.findViewById<View>(R.id.infoNopol)
                val infoPemuat = screenshotLayout.findViewById<View>(R.id.infoPemuat)

                // Add references for new info views
                val infoUrutanKe = screenshotLayout.findViewById<View>(R.id.infoUrutanKe)
                val infoJamTanggal = screenshotLayout.findViewById<View>(R.id.infoJamTanggal)

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

                val formattedDate = dateFormat.format(currentDate).uppercase(indonesianLocale)
                val formattedTime = timeFormat.format(currentDate)

                // Get and increment screenshot counter
                val screenshotNumber = getAndIncrementScreenshotCounter()

                val capitalizedFeatureName = featureName!!.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                // Set data for eSPB
                tvUserName.text = "Hasil QR ${capitalizedFeatureName} dari ${prefManager!!.jabatanUserLogin}"
                setInfoData(infoBlokList, "Blok", ": $blokDisplay")
                setInfoData(infoTotalJjg, "Total Janjang", ": $totalJjg")
                setInfoData(infoTotalTransaksi, "Jumlah Transaksi", ": $tphCount")
                setInfoData(infoNoESPB, "E-SPB", ": $noESPBStr")
                if (selectedTransporterId == 0) {
                    selectedTransporterName = "Internal"
                }
                setInfoData(infoTransporter, "Transporter", ": $selectedTransporterName")
                setInfoData(infoDriver, "Driver", ": $driver")
                setInfoData(infoNopol, "Nomor Polisi", ": $selectedNopol")
                setInfoData(infoPemuat, "Pemuat", ": $pemuatNama")

                // Add new info data
                setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                setInfoData(infoJamTanggal, "Jam & Tanggal", ": $formattedDate, $formattedTime")

                tvFooter.text =
                    "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

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
                    Toasty.success(
                        this@FormESPBActivity,
                        "QR sudah tersimpan digaleri",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
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


    private fun getAndIncrementScreenshotCounter(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefManager!!.getScreenshotDate(featureName!!)
        val currentCounter = prefManager!!.getScreenshotCounter(featureName!!)

        return if (lastDate != today) {
            // Reset counter for new day
            prefManager!!.setScreenshotDate(featureName!!, today)
            prefManager!!.setScreenshotCounter(featureName!!, 1)
            1
        } else {
            // Increment counter for same day
            val newCounter = currentCounter + 1
            prefManager!!.setScreenshotCounter(featureName!!, newCounter)
            newCounter
        }
    }

    /**
     * Shows the QR code in fullscreen mode without relying on a bottom sheet
     */
    private fun showQrCodeFullScreen(qrDrawable: Drawable?) {
        if (qrDrawable == null) return

        // Create a dialog to display the QR code
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Make dialog dismissible with back button
        dialog.setCancelable(true)

        // Inflate the camera_edit layout
        val fullscreenView = layoutInflater.inflate(R.layout.camera_edit, null)
        dialog.setContentView(fullscreenView)

        // Find views within the dialog layout
        val fotoLayout = fullscreenView.findViewById<ConstraintLayout>(R.id.clZoomLayout)
        val photoView = fullscreenView.findViewById<PhotoView>(R.id.fotoZoom)
        val closeZoomCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardCloseZoom)
        val changePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val deletePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardDeletePhoto)

        // Find the TextView and ImageView for color changes
        val tvCardCloseButton = fullscreenView.findViewById<TextView>(R.id.tvCardCloseButton)
        val closeZoomIcon = fullscreenView.findViewById<ImageView>(R.id.closeZoom)

        // Set the image to the PhotoView
        photoView.setImageDrawable(qrDrawable)

        // Hide edit options
        changePhotoCard.visibility = View.GONE
        deletePhotoCard.visibility = View.GONE

        // Set background color of the layout to white
        fotoLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

        // Set close button background color to green
        val closeCardLinearLayout = closeZoomCard.getChildAt(0) as LinearLayout

// Set the LinearLayout background to green instead of the card
        closeCardLinearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.greenDarker))

        // Change the text color to white
        tvCardCloseButton.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Change the close icon tint to white
        closeZoomIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))

        // Set up close button
        closeZoomCard.setOnClickListener {
            dialog.dismiss()
        }

        // Make dialog display properly
        dialog.window?.apply {
            // Set window background to white
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@FormESPBActivity, R.color.white)))
            setDimAmount(0f) // Remove dimming since we have a white background

            // Set fullscreen and proper layout
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // This helps ensure it appears on top
            setGravity(Gravity.CENTER)

            // Set maximum brightness for better QR code scanning
            attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }

        dialog.show()

        // Set phone to maximum brightness for better QR visibility
        setMaxBrightness(this, true)

        // When dialog is dismissed, restore original brightness
        dialog.setOnDismissListener {
            setMaxBrightness(this, false)
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
                        val pemuatNames = pemuatList.map { "${it.nama} - ${it.nik ?: "N/A"}" }
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
                    transporterList.find { it.nama == selectedItem }?.kode!!.toInt()
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedTransporterId: ${e.message}")
                    0
                }
                selectedTransporterName = try {
                    transporterList.find { it.nama == selectedItem }?.nama!!
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedTransporterName: ${e.message}")
                    ""
                }
                Log.d(
                    "FormESPBActivityTransporter",
                    "selectedTransporterId: $selectedTransporterId"
                )
            }
            R.id.formEspbPemuat -> {
                val selectedPemuat = selectedItem.toString()
                AppLogger.d("Selected Pemuat: $selectedPemuat")

                // Extract NIK from the selection
                val lastDashIndex = selectedPemuat.lastIndexOf(" - ")
                val selectedNik =
                    if (lastDashIndex != -1 && lastDashIndex < selectedPemuat.length - 3) {
                        val potentialNik = selectedPemuat.substring(lastDashIndex + 3).trim()
                        if (potentialNik.all { it.isDigit() }) potentialNik else ""
                    } else ""

                AppLogger.d("Extracted NIK: $selectedNik")

                // Find the selected employee in pemuatList
                var selectedEmployee = pemuatList.firstOrNull {
                    it.nik == selectedNik || it.nama?.trim()
                        ?.equals(selectedPemuat.trim(), ignoreCase = true) == true
                }

                // If not found by exact match, try partial match on name
                if (selectedEmployee == null && lastDashIndex != -1) {
                    val nameWithoutNik = selectedPemuat.substring(0, lastDashIndex).trim()
                    selectedEmployee = pemuatList.firstOrNull {
                        it.nama?.trim()?.equals(nameWithoutNik, ignoreCase = true) == true
                    }
                }

                if (selectedEmployee == null) {
                    selectedEmployee = pemuatList.firstOrNull {
                        it.nama?.contains(
                            selectedPemuat.split(" - ")[0],
                            ignoreCase = true
                        ) == true
                    }
                }

                // Process the pemuatList to build the maps
                val nameCounts = mutableMapOf<String, Int>()
                pemuatList.forEach {
                    it.nama?.trim()?.let { nama -> nameCounts[nama] = (nameCounts[nama] ?: 0) + 1 }
                }

                // Building the maps based on existing list
                pemuatList.forEach {
                    it.nama?.trim()?.let { nama ->
                        val key = if (nameCounts[nama]!! > 1) {
                            "$nama - ${it.nik}"
                        } else {
                            nama
                        }
                        karyawanIdMap[key] = it.id!!
                        kemandoranIdMap[key] = it.kemandoran_id!!
                        // If you need to store names separately as in original code
                        karyawanNamaMap[key] = nama
                    }
                }

                // Explicitly ensure the selected employee is in the maps
                if (selectedEmployee != null) {
                    // Get clean name of selected employee
                    val employeeName = selectedEmployee.nama?.trim() ?: ""
                    // Get name without NIK from selection
                    val selectionName = if (lastDashIndex != -1) {
                        selectedPemuat.substring(0, lastDashIndex).trim()
                    } else {
                        selectedPemuat.trim()
                    }

                    // Make sure employee is in maps by both full selection name and clean name
                    karyawanIdMap[selectedPemuat] = selectedEmployee.id!!
                    kemandoranIdMap[selectedPemuat] = selectedEmployee.kemandoran_id!!

                    karyawanIdMap[selectionName] = selectedEmployee.id!!
                    kemandoranIdMap[selectionName] = selectedEmployee.kemandoran_id!!

                    karyawanIdMap[employeeName] = selectedEmployee.id!!
                    kemandoranIdMap[employeeName] = selectedEmployee.kemandoran_id!!
                }

                if (selectedEmployee != null) {
                    val worker = Worker(selectedEmployee.id.toString(), selectedPemuat)
                    selectedPemuatAdapter.addWorker(worker)
                    // If you're tracking selected worker IDs as in your new code
                    pemuatListId.add(selectedEmployee.id.toString())

                    val availableWorkers = selectedPemuatAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinner(
                            R.id.formEspbPemuat,
                            availableWorkers.map { it.name }
                        )
                    }

                    AppLogger.d("Selected Worker: $selectedPemuat, ID: ${selectedEmployee.id}")
                } else {
                    AppLogger.d("Error: Could not find worker with name $selectedPemuat or NIK $selectedNik")
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

        val nikList = pemuat_nik

        // Extract unique dates from tph1 and prepare optimized string
        val dateMap = JsonObject()
        val optimizedTph1 = StringBuilder()

        // Process tph1 if not empty
        if (tph1.isNotEmpty()) {
            val entries = tph1.split(";")
            entries.forEachIndexed { index, entry ->
                if (entry.isNotEmpty()) {
                    val parts = entry.split(",")
                    if (parts.size >= 2) {
                        // Extract date and time
                        val dateTime = parts[1]
                        val dateParts = dateTime.split(" ")
                        if (dateParts.size >= 2) {
                            val date = dateParts[0]
                            val time = dateParts[1]

                            // Add date to dateMap with index 0 (instead of 1)
                            if (!dateMap.has("0")) {
                                dateMap.addProperty("0", date)
                            }

                            // Create optimized entry: ID,0,TIME,VALUE1,VALUE2...
                            // Note: using 0 instead of 1 for the index
                            val newEntry = StringBuilder("${parts[0]},0,${time}")

                            // Add remaining values (starting from index 2)
                            for (i in 2 until parts.size) {
                                newEntry.append(",${parts[i]}")
                            }

                            if (index > 0) {
                                optimizedTph1.append(";")
                            }
                            optimizedTph1.append(newEntry)
                        }
                    }
                }
            }
        }

        // Create the nested ESPB object
        val espbObject = JsonObject().apply {
            addProperty("blok_jjg", blok_jjg)
            addProperty("nopol", nopol)
            addProperty("driver", driver)
            addProperty("pemuat_id", pemuat_id)
            addProperty("transporter_id", transporter_id)
            addProperty("mill_id", mill_id)
            addProperty("kemandoran_id", kemandoran_id)
            addProperty("pemuat_nik", nikList.toString()) // Use the extracted NIKs only
            addProperty("created_by_id", created_by_id)
            add("creator_info", createCreatorInfo(appVersion, osVersion, phoneModel))
            addProperty("no_espb", no_espb)
            addProperty("created_at", getCurrentDateTime())
        }

        // Create the root object
        val rootObject = JsonObject().apply {
            add("espb", espbObject)
            addProperty("tph_0", tph0)
            addProperty("tph_1", optimizedTph1.toString())
            add("tgl", dateMap)
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
                    ids_to_update = idsToUpdate.joinToString(","),
                    date_scan = ""
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

//                    // Update UI on main thread
//                    withContext(Dispatchers.Main) {
//
//                    }
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