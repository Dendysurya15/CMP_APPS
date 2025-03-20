package com.cbi.mobile_plantation.ui.view.panenTBS

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Resources
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter
import com.cbi.mobile_plantation.ui.view.espb.FormESPBActivity
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.ScanQR
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel

import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListPanenTBSActivity : AppCompatActivity() {
    private var featureName = ""
    private var listTPHDriver = ""
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var listAdapter: ListPanenTPHAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var currentState = 0 // 0 for tersimpan, 1 for terscan
    private var prefManager: PrefManager? = null
    private var isSettingUpCheckbox = false

    // Add views for buttons and counters
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var speedDial: SpeedDialView

    private var isAscendingOrder = true

    private lateinit var searchEditText: EditText
    private lateinit var sortButton: ImageView // Add this at class level
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private lateinit var btnAddMoreTph: FloatingActionButton
    private var tph1IdPanen = ""

    private var mappedData: List<Map<String, Any>> = emptyList()

    private var espbId = 0
    private var jjg = 0
    private var noespb = "NULL"
    private var blok = "NULL"
    private var tph = 0
    private var tph0 = ""
    private var tph1 = ""

    private var blok_jjg = "NULL"
    private var nopol = "NULL"
    private var driver = "NULL"
    private var pemuat_id = "NULL"
    private var kemandoran_id = "NULL"
    private var pemuat_nik = "NULL"
    private var transporter_id = 0
    private var mill_id = 0
    private var created_by_id = 0
    private var no_espb = "NULL"
    private var tph0QR = "NULL"
    private var tph1QR = "NULL"
    private var creatorInfo = "NULL"
    private var dateTime = "NULL"

    private lateinit var ll_detail_espb: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        listTPHDriver = try {
            AppUtils.readJsonFromEncryptedBase64Zip(
                intent.getStringExtra("scannedResult").toString()
            ).toString()
        } catch (e: Exception) {
            Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        Log.d("listTPHDriver", listTPHDriver.toString())

        prefManager = PrefManager(this)
        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        // Get previous TPH data if available
        val previousTph1 = intent.getStringExtra("previous_tph_1") ?: ""
        val previousTph0 = intent.getStringExtra("previous_tph_0") ?: ""
        val previousTph1IdPanen = intent.getStringExtra("previous_tph_1_id_panen") ?: ""

        if (previousTph1.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph1 found: $previousTph1")
            tph1 = previousTph1
        }

        if (previousTph0.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph0 found: $previousTph0")
            tph0 = previousTph0
        }

        if (previousTph1IdPanen.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph1IdPanen found: $previousTph1IdPanen")
            tph1IdPanen = previousTph1IdPanen
        }

        if (listTPHDriver.isNotEmpty()) {
            // Extract TPH IDs from the current scan
            val currentScanTphIds = try {
                val tphString = listTPHDriver
                    .removePrefix("""{"tph":"""")
                    .removeSuffix(""""}""")
                tphString
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

            // If we have previous scan data stored in tph1, extract TPH IDs
            val previousScanTphIds = if (tph1.isNotEmpty()) {
                // Get the TPH IDs from tph1 string
                tph1.split(";").mapNotNull { entry ->
                    try {
                        entry.split(",").firstOrNull()
                    } catch (e: Exception) {
                        Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        null
                    }
                }.joinToString(";")
            } else ""

            // Combine previous and current scan TPH IDs
            val combinedTphIds =
                if (previousScanTphIds.isNotEmpty() && currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds;$currentScanTphIds"}"""
                } else if (currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$currentScanTphIds"}"""
                } else if (previousScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds"}"""
                } else ""

            // Update listTPHDriver with combined data
            if (combinedTphIds.isNotEmpty()) {
                listTPHDriver = combinedTphIds
                Log.d("ListPanenTBSActivity", "Combined TPH IDs: $listTPHDriver")
            }
        }

        setupHeader()
        initViewModel()
        initializeViews()
        loadingDialog = LoadingDialog(this)
        setupRecyclerView()
        setupSearch()
        setupObservers()
        if (featureName != "Buat eSPB" && featureName != "Detail eSPB") {
            setupSpeedDial()
            setupCheckboxControl()  // Add this
        }
        setupCardListeners()
        initializeFilterViews()
        setupSortButton()
        currentState = 0
        setActiveCard(cardTersimpan)
        if (featureName == "Detail eSPB") {
            // set to gone card_item_tersimpan
            cardTersimpan.visibility = View.GONE
            // set to gone card_item_terscan
            cardTerscan.visibility = View.GONE
            val app = AppRepository(application)
            val espbViewModelFactory = ESPBViewModel.ESPBViewModelFactory(app)
            espbViewModel = ViewModelProvider(this, espbViewModelFactory)[ESPBViewModel::class.java]
            espbId = try {
                intent.getStringExtra("id_espb").toString().toInt()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil id eSPB: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                0
            }
        }
        lifecycleScope.launch {
            if (featureName == "Buat eSPB") {
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {

                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                findViewById<TextView>(R.id.list_item_tersimpan).text = "Rekap TPH"
                findViewById<TextView>(R.id.list_item_terscan).text = "TPH Menjadi E-SPB"

                panenViewModel.loadTPHNonESPB(0, 0, 1)
                panenViewModel.loadCountTPHESPB(0, 1, 1)

            } else if (featureName == "Detail eSPB") {
                ll_detail_espb = findViewById<LinearLayout>(R.id.ll_detail_espb)
                ll_detail_espb.visibility = View.VISIBLE
                espbViewModel.getESPBById(espbId)
                espbViewModel.espbEntity.observe(this@ListPanenTBSActivity) { espbWithRelations ->
                    if (espbWithRelations != null) {
                        try {
                            // Extract ESPB data
                            val espb = espbWithRelations

                            // Find all included layouts
                            val tvNoEspb = findViewById<View>(R.id.tv_no_espb)
                            val tvNoPol = findViewById<View>(R.id.tv_no_pol)
                            val tvTransporter = findViewById<View>(R.id.tv_transporter)
                            val tvDriver = findViewById<View>(R.id.tv_driver)
                            val tvMill = findViewById<View>(R.id.tv_mill)
                            val tvMekanisasi = findViewById<View>(R.id.tv_mekanisasi)
//                            val tvDraft = findViewById<View>(R.id.tv_draft)

                            blok_jjg = espb.blok_jjg
                            nopol = espb.nopol
                            driver = espb.driver
                            pemuat_id = espb.pemuat_id
                            transporter_id = espb.transporter_id
                            mill_id = espb.mill_id
                            created_by_id = espb.created_by_id
                            no_espb = espb.noESPB
                            tph0QR = espb.tph0
                            tph1QR = espb.tph1
                            creatorInfo = espb.creator_info
                            dateTime = espb.created_at
                            kemandoran_id = espb.kemandoran_id
                            pemuat_nik = espb.pemuat_nik
                            tph1 = espb.tph1
                            tph0 = espb.tph0

                            // Set No eSPB
                            tvNoEspb.findViewById<TextView>(R.id.tvTitleEspb).text = "No eSPB"
                            noespb = espb.noESPB
                            panenViewModel.getAllPanenWhereESPB(noespb)
                            tvNoEspb.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.noESPB

                            // Set No Polisi
                            tvNoPol.findViewById<TextView>(R.id.tvTitleEspb).text = "No Polisi"
                            tvNoPol.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.nopol

                            // Launch coroutines to fetch transporter and mill names
                            lifecycleScope.launch {
                                try {
                                    // Set Transporter
                                    tvTransporter.findViewById<TextView>(R.id.tvTitleEspb).text =
                                        "Transporter"
                                    val transporterName = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getTransporterNameById(espb.transporter_id)
                                                ?: "Internal"
                                        } catch (e: Exception) {
                                            Log.e(
                                                "ListPanenTBSActivity",
                                                "Error fetching transporter",
                                                e
                                            )
                                            "Internal"
                                        }
                                    }
                                    tvTransporter.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        transporterName

                                    // Set Mill
                                    tvMill.findViewById<TextView>(R.id.tvTitleEspb).text = "Mill"
                                    val millAbbr = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getMillNameById(espb.mill_id) ?: "Unknown"
                                        } catch (e: Exception) {
                                            Log.e("ListPanenTBSActivity", "Error fetching mill", e)
                                            "Unknown"
                                        }
                                    }
                                    tvMill.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        millAbbr
                                } catch (e: Exception) {
                                    Log.e("ListPanenTBSActivity", "Error in coroutine", e)
                                    Toasty.error(
                                        this@ListPanenTBSActivity,
                                        "Error loading details: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            // Set Driver
                            tvDriver.findViewById<TextView>(R.id.tvTitleEspb).text = "Driver"
                            tvDriver.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.driver

                            // Set Mekanisasi status
                            tvMekanisasi.findViewById<TextView>(R.id.tvTitleEspb).text =
                                "Mekanisasi"
                            val mekanisasiStatus =
                                if (espb.status_mekanisasi == 1) "Ya" else "Tidak"
                            tvMekanisasi.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                mekanisasiStatus

//                            // Set Draft status
//                            tvDraft.findViewById<TextView>(R.id.tvTitleEspb).text = "Status"
//                            val draftStatus = if (espb.status_draft == 1) "Draft" else "Final"
//                            tvDraft.findViewById<TextView>(R.id.tvSubTitleEspb).text = draftStatus

                            // Make the layout visible now that we've populated it
                            ll_detail_espb.visibility = View.VISIBLE

                        } catch (e: Exception) {
                            Toasty.error(
                                this@ListPanenTBSActivity,
                                "Error displaying eSPB details: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ListPanenTBSActivity", "Error displaying eSPB details", e)
                        }
                    } else {
                        Toasty.error(
                            this@ListPanenTBSActivity,
                            "eSPB data not found",
                            Toast.LENGTH_LONG
                        ).show()
                        ll_detail_espb.visibility = View.GONE
                    }
                }
            } else {
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.VISIBLE
                panenViewModel.loadActivePanen()
                panenViewModel.loadPanenCountArchive() // Load archive count
            }

        }

        setupButtonGenerateQR()

        if (featureName == "Buat eSPB") {
            btnAddMoreTph = FloatingActionButton(this)
            btnAddMoreTph.id = View.generateViewId()
            btnAddMoreTph.setImageResource(R.drawable.baseline_add_24) // Make sure you have this resource, or use baseline_add_24
            btnAddMoreTph.contentDescription = "Add More TPH"

            // Set button background color to green
            btnAddMoreTph.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_dark
                )
            )

            // Set icon color to white
            btnAddMoreTph.imageTintList = ColorStateList.valueOf(Color.WHITE)

            // Add the button to the layout
            val rootLayout =
                findViewById<ConstraintLayout>(R.id.clParentListPanen) // Assuming your root layout is a ConstraintLayout
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            try {
                params.bottomToTop = R.id.btnGenerateQRTPH
            } catch (e: Exception) {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            // Convert dp to pixels for proper margin setting
            val scale = resources.displayMetrics.density
            val marginInPixels = (30 * scale + 0.5f).toInt()
            params.setMargins(
                0,
                0,
                marginInPixels,
                marginInPixels
            ) // Set right and bottom margins to 30dp
            rootLayout.addView(btnAddMoreTph, params)

            btnAddMoreTph.setOnClickListener {
                getAllDataFromList()
                val intent = Intent(this, ScanQR::class.java)
                intent.putExtra("tph_1", tph1)
                intent.putExtra("tph_0", tph0)
                intent.putExtra("tph_1_id_panen", tph1IdPanen)
                intent.putExtra("FEATURE_NAME", featureName)
                Log.d("ListPanenTBSActivityPassData", "List tph1: $tph1")
                Log.d("ListPanenTBSActivityPassData", "List tph0: $tph0")
                Log.d("ListPanenTBSActivityPassData", "List tph1IdPanen: $tph1IdPanen")
                Log.d("ListPanenTBSActivityPassData", "List FEATURE_NAME: $featureName")
                startActivity(intent)
                finishAffinity()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(
                    Intent(
                        this@ListPanenTBSActivity,
                        HomePageActivity::class.java
                    )
                )
                finishAffinity()
            }
        })
    }

    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()

            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            listAdapter.updateArchiveState(0)
            speedDial.visibility =
                if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
            if (featureName == "Buat eSPB") {
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {
                loadingDialog.setMessage("Loading data tph...")
                panenViewModel.loadTPHNonESPB(0, 0, 1)
            } else {
                loadingDialog.setMessage("Loading data tersimpan...")
                panenViewModel.loadActivePanen()
            }
        }

        cardTerscan.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()

            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(1)
            if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                loadingDialog.setMessage("Loading TPH menjadi E-SPB...")
                panenViewModel.loadTPHESPB(0, 1, 1)
            } else {
                loadingDialog.setMessage("Loading data terscan...")
                panenViewModel.loadArchivedPanen()
            }

        }
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.card_item_tersimpan)
        cardTerscan = findViewById(R.id.card_item_terscan)
        counterTersimpan = findViewById(R.id.counter_item_tersimpan)
        counterTerscan = findViewById(R.id.counter_item_terscan)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.rvTableData) // Initialize RecyclerView
    }

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {

        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }

            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val key =
                            if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"

                        val toValue = if (jjgJson.has(key)) {
                            jjgJson.getInt(key) // Throws JSONException if the key is not an int
                        } else {
                            throw IllegalArgumentException("Missing '$key' key in jjg_json: $jjgJsonString")
                        }

                        append("$tphId,$dateCreated,$toValue;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            val username = try {
                PrefManager(this).username.toString().split("@")[0].takeLast(3).uppercase()
            }catch (e: Exception){
                Toasty.error(this, "Error mengambil username: ${e.message}", Toast.LENGTH_LONG).show()
                "NULL"
            }
            return JSONObject().apply {
                put("tph_0", formattedData)
                put("username",username)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

    fun convertToFormattedString(input: String, int: Int = 1): String {
        try {
            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects.joinToString(";") { objStr ->
                // Clean up the object string
                val cleanObj = objStr.trim()
                    .removePrefix("{")
                    .removeSuffix("}")

                // Split into key-value pairs
                val map = cleanObj.split(", ").associate { pair ->
                    val (key, value) = pair.split("=", limit = 2)
                    key to value
                }

                // Extract jjg_json value
                val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                // Construct the formatted string
                "${map["tph_id"]},${map["date_created"]},${jjgJson},$int"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun convertToFormattedString(input: String, tphFilter: String): String {
        try {
            // Parse TPH filter string into a list of IDs
            val tphIds = tphFilter
                .trim()
                .removeSurrounding("{", "}")
                .substringAfter("\"tph\":\"")  // Get content after "tph":"
                .substringBefore("\"")         // Get content before the closing quote
                .split(";")
                .map { it.trim() }
                .toSet()

            Log.d("ListPanenTBSActivityESPB", "tphIds: $tphIds")

            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects
                .filter { objStr ->
                    // Extract tph_id from each object and check if it's in our filter list
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }
                    tphIds.contains(map["tph_id"])
                }
                .joinToString(";") { objStr ->
                    // Clean up the object string
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")

                    // Split into key-value pairs
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }

                    // Extract jjg_json value
                    val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                    // Construct the formatted string
                    "${map["tph_id"]},${map["date_created"]},${jjgJson},1"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    data class Entry(
        val id: String,
        val timestamp: String,
        val value: Int,
        val type: Int
    ) {
        override fun toString(): String = "$id,$timestamp,$value,$type"
    }

    fun String.toEntries(): Set<Entry> {
        if (this.isEmpty()) return emptySet()
        return split(";").map { entry ->
            val parts = entry.split(",")
            Entry(
                id = parts[0],
                timestamp = parts[1],
                value = parts[2].toInt(),
                type = parts[3].toInt()
            )
        }.toSet()
    }

    fun Set<Entry>.toString(): String {
        return if (isEmpty()) "" else joinToString(";")
    }

    private fun getAllDataFromList() {
        //get manually selected items
        val selectedItems = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems: $selectedItems")
        val tph1AD0 =
            convertToFormattedString(selectedItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD0")
        val tph1AD2 =
            convertToFormattedString(selectedItems.toString(), 1).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD2")

        //get automatically selected items
        val selectedItems2 = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems2:$selectedItems2")

        // Extract the id values from the matches and join them with commas
        val newTph1IdPanen = try {
            val pattern = Regex("\\{id=(\\d+),")
            val matches = pattern.findAll(selectedItems2.toString())
            matches.map { it.groupValues[1] }.joinToString(", ")
        } catch (e: Exception) {
            Toasty.error(this, "Error parsing panen IDs: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        // Combine with existing tph1IdPanen if it exists
        tph1IdPanen = if (tph1IdPanen.isEmpty()) {
            newTph1IdPanen
        } else {
            "$tph1IdPanen, $newTph1IdPanen"
        }

        val allItems = listAdapter.getCurrentData()
        Log.d("ListPanenTBSActivityESPB", "listTPHDriver: $listTPHDriver")
        val tph1NO = convertToFormattedString(
            selectedItems2.toString(),
            listTPHDriver
        ).replace("{\"KP\": ", "").replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsNO: $tph1NO")

        //get item which is not selected
        val tph0before =
            convertToFormattedString(allItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItems0: $tph0before")

        val set1 = tph1AD0.toEntries()
        val set2 = tph1AD2.toEntries()
        val set3 = tph1NO.toEntries()
        val set4 = tph0before.toEntries()

        // Calculate string5 = string4 - string1 - string3
        val newTph0 = (set4 - set1 - set3).toString().replace("[", "").replace("]", "")
            .replace(", ", ";")
        Log.d("ListPanenTBSActivityESPB", "New tph0: $newTph0")

        // Calculate string6 = string2 + string3
        val newTph1 =
            (set2 + set3).toString().replace("[", "").replace("]", "").replace(", ", ";")
        Log.d("ListPanenTBSActivityESPB", "New tph1: $newTph1")

        // Combine with existing data if it exists
        if (tph0.isNotEmpty() && newTph0.isNotEmpty()) {
            tph0 = "$tph0;$newTph0"
        } else if (newTph0.isNotEmpty()) {
            tph0 = newTph0
        }

        if (tph1.isNotEmpty() && newTph1.isNotEmpty()) {
            tph1 = "$tph1;$newTph1"
        } else if (newTph1.isNotEmpty()) {
            tph1 = newTph1
        }

        // Remove any duplicate entries from tph0 and tph1
        tph0 = removeDuplicateEntries(tph0)
        tph1 = removeDuplicateEntries(tph1)

        Log.d("ListPanenTBSActivityESPB", "Final tph0: $tph0")
        Log.d("ListPanenTBSActivityESPB", "Final tph1: $tph1")
        Log.d("ListPanenTBSActivityESPB", "Final tph1IdPanen: $tph1IdPanen")
    }

    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        if (featureName == "Buat eSPB") {
            btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
            btnGenerateQRTPH.setOnClickListener {
                getAllDataFromList()
                AlertDialogUtility.withTwoActions(
                    this,
                    "LANJUT",
                    "PERHATIAN!",
                    "Apakah anda ingin membuat eSPB dengan data ini?",
                    "warning.json", function = {
                        val intent = Intent(this, FormESPBActivity::class.java)
                        intent.putExtra("tph_1", tph1)
                        intent.putExtra("tph_0", tph0)
                        intent.putExtra("tph_1_id_panen", tph1IdPanen)
                        intent.putExtra("FEATURE_NAME", featureName)
                        startActivity(intent)
                        finishAffinity()
                    }
                ) {
                }
            }
        } else if (featureName == "Rekap panen dan restan") {
            btnGenerateQRTPH.visibility = View.GONE
        } else {
            btnGenerateQRTPH.setOnClickListener {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Generate QR",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_generate_qr),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {

                        val view =
                            layoutInflater.inflate(
                                R.layout.layout_bottom_sheet_generate_qr_panen,
                                null
                            )
                        view.background = ContextCompat.getDrawable(
                            this@ListPanenTBSActivity,
                            R.drawable.rounded_top_right_left
                        )

                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
                        dialog.setContentView(view)

                        // Get references to views
                        val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                        val tvTitleQRGenerate: TextView =
                            view.findViewById(R.id.textTitleQRGenerate)
                        tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
                        val dashedLine: View = view.findViewById(R.id.dashedLine)
                        val loadingContainer: LinearLayout =
                            view.findViewById(R.id.loadingDotsContainerBottomSheet)
                        val dataQR: TextView = view.findViewById(R.id.dataQR)
                        val titleQRConfirm: TextView = view.findViewById(R.id.titleAfterScanQR)
                        val descQRConfirm: TextView = view.findViewById(R.id.descAfterScanQR)
                        val btnConfirmScanPanenTPH: MaterialButton =
                            view.findViewById(R.id.btnConfirmScanPanenTPH)

                        // Initially hide QR code and dashed line, show loading
                        qrCodeImageView.visibility = View.GONE
                        loadingLogo.visibility = View.VISIBLE
                        loadingContainer.visibility = View.VISIBLE

                        // Initial setup for text elements
                        titleQRConfirm.setResponsiveTextSizeWithConstraints(17F, 17F, 19F)
                        descQRConfirm.setResponsiveTextSizeWithConstraints(17F, 15F, 19F)

                        // Load and start bounce animation
                        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
                        loadingLogo.startAnimation(bounceAnimation)

                        // Setup dots animation
                        val dots = listOf(
                            loadingContainer.findViewById<View>(R.id.dot1),
                            loadingContainer.findViewById<View>(R.id.dot2),
                            loadingContainer.findViewById<View>(R.id.dot3),
                            loadingContainer.findViewById<View>(R.id.dot4)
                        )

                        dots.forEachIndexed { index, dot ->
                            val translateAnimation =
                                ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                            val scaleXAnimation =
                                ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
                            val scaleYAnimation =
                                ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)

                            listOf(
                                translateAnimation,
                                scaleXAnimation,
                                scaleYAnimation
                            ).forEach { animation ->
                                animation.duration = 500
                                animation.repeatCount = ObjectAnimator.INFINITE
                                animation.repeatMode = ObjectAnimator.REVERSE
                                animation.startDelay = (index * 100).toLong()
                                animation.start()
                            }
                        }

                        dialog.setOnShowListener {
                            val bottomSheet =
                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        dialog.show()

                        // Set up the confirmation button click listener
                        btnConfirmScanPanenTPH.setOnClickListener {
                            AlertDialogUtility.withTwoActions(
                                this@ListPanenTBSActivity,
                                getString(R.string.al_yes),
                                getString(R.string.confirmation_dialog_title),
                                "${getString(R.string.al_make_sure_scanned_qr)}  data?",
                                "warning.json",
                                ContextCompat.getColor(
                                    this@ListPanenTBSActivity,
                                    R.color.greendarkerbutton
                                ),
                                function = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            withContext(Dispatchers.Main) {
                                                loadingDialog.show()
                                            }

                                            // Validate data first
                                            if (mappedData.isEmpty()) {
                                                throw Exception("No data to archive")
                                            }

                                            var hasError = false
                                            var successCount = 0
                                            val errorMessages = mutableListOf<String>()

                                            mappedData.forEach { item ->
                                                try {
                                                    // Null check for item
                                                    if (item == null) {
                                                        errorMessages.add("Found null item in data")
                                                        hasError = true
                                                        return@forEach
                                                    }

                                                    // ID validation
                                                    val id = when (val idValue = item["id"]) {
                                                        null -> {
                                                            errorMessages.add("ID is null")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        !is Number -> {
                                                            errorMessages.add("Invalid ID format: $idValue")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        else -> idValue.toInt()
                                                    }

                                                    if (id <= 0) {
                                                        errorMessages.add("Invalid ID value: $id")
                                                        hasError = true
                                                        return@forEach
                                                    }

                                                    try {
                                                        panenViewModel.archivePanenById(id)
                                                        successCount++
                                                    } catch (e: SQLiteException) {
                                                        errorMessages.add("Database error for ID $id: ${e.message}")
                                                        hasError = true
                                                    } catch (e: Exception) {
                                                        errorMessages.add("Error archiving ID $id: ${e.message}")
                                                        hasError = true
                                                    }

                                                } catch (e: Exception) {
                                                    errorMessages.add("Unexpected error processing item: ${e.message}")
                                                    hasError = true
                                                }
                                            }

                                            // Show results
                                            withContext(Dispatchers.Main) {
                                                try {
                                                    loadingDialog.dismiss()

                                                    when {
                                                        successCount == 0 -> {
                                                            val errorDetail =
                                                                errorMessages.joinToString("\n")
                                                            AppLogger.e("Archive failed. Errors:\n$errorDetail")
                                                            Toast.makeText(
                                                                this@ListPanenTBSActivity,
                                                                "Gagal mengarsipkan data",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        hasError -> {
                                                            val errorDetail =
                                                                errorMessages.joinToString("\n")
                                                            AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                            Toast.makeText(
                                                                this@ListPanenTBSActivity,
                                                                "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        else -> {
                                                            AppLogger.d("All items archived successfully")
                                                            Toast.makeText(
                                                                this@ListPanenTBSActivity,
                                                                "Semua data berhasil diarsipkan",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    dialog.dismiss()
                                                } catch (e: Exception) {
                                                    AppLogger.e("Error in UI update: ${e.message}")
                                                    Toast.makeText(
                                                        this@ListPanenTBSActivity,
                                                        "Terjadi kesalahan pada UI",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                        } catch (e: Exception) {
                                            AppLogger.e("Fatal error in archiving process: ${e.message}")
                                            withContext(Dispatchers.Main) {
                                                try {
                                                    loadingDialog.dismiss()
                                                    Toast.makeText(
                                                        this@ListPanenTBSActivity,
                                                        "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    dialog.dismiss()
                                                } catch (dialogException: Exception) {
                                                    AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                                }
                                            }
                                        }

                                        panenViewModel.loadActivePanen()
                                        panenViewModel.loadPanenCountArchive()
                                    }
                                }
                            ) {

                            }
                        }

                        // Generate QR code in the background
                        lifecycleScope.launch {
                            try {
                                // Delay for loading effect
                                delay(1000)

                                // Generate the JSON data and encode it in a background thread
                                // In the background processing part of setupButtonGenerateQR()
                                val jsonData = withContext(Dispatchers.IO) {
                                    try {
                                        if (featureName == "Detail eSPB") {

                                            val gson = Gson()
                                            // Create the nested ESPB object
                                            val espbObject = JsonObject().apply {
                                                addProperty("blok_jjg", blok_jjg)
                                                addProperty("nopol", nopol)
                                                addProperty("driver", driver)
                                                addProperty("pemuat_id", pemuat_id)
                                                addProperty("kemandoran_id", kemandoran_id)
                                                addProperty("pemuat_nik", pemuat_nik)
                                                addProperty("transporter_id", transporter_id)
                                                addProperty("mill_id", mill_id)
                                                addProperty("created_by_id", created_by_id)
                                                addProperty("creator_info", creatorInfo)
                                                addProperty("no_espb", no_espb)
                                                addProperty("created_at", dateTime)
                                            }

                                            // Create the root object
                                            val rootObject = JsonObject().apply {
                                                add("espb", espbObject)
                                                addProperty("tph_0", tph0)
                                                addProperty("tph_1", tph1)
                                            }

                                            gson.toJson(rootObject)
                                        } else {
                                            formatPanenDataForQR(mappedData)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error generating JSON data: ${e.message}")
                                        throw e
                                    }
                                }

                                val encodedData = withContext(Dispatchers.IO) {
                                    try {
                                        encodeJsonToBase64ZipQR(jsonData)
                                            ?: throw Exception("Encoding failed")
                                    } catch (e: Exception) {
                                        AppLogger.e("Error encoding data: ${e.message}")
                                        throw e
                                    }
                                }

                                // Switch to the main thread for UI updates
                                withContext(Dispatchers.Main) {
                                    try {
                                        // Generate and display the QR code
                                        generateHighQualityQRCode(encodedData, qrCodeImageView)

                                        // Create animations for transitions
                                        val fadeOut =
                                            ObjectAnimator.ofFloat(loadingLogo, "alpha", 1f, 0f)
                                                .apply {
                                                    duration = 250
                                                }
                                        val fadeOutDots =
                                            ObjectAnimator.ofFloat(
                                                loadingContainer,
                                                "alpha",
                                                1f,
                                                0f
                                            )
                                                .apply {
                                                    duration = 250
                                                }

                                        // Ensure QR code and other elements start invisible
                                        qrCodeImageView.alpha = 0f
                                        dashedLine.alpha = 0f
                                        tvTitleQRGenerate.alpha = 0f
                                        titleQRConfirm.alpha = 0f
                                        descQRConfirm.alpha = 0f
                                        btnConfirmScanPanenTPH.alpha = 0f
                                        if (dataQR != null) dataQR.alpha = 0f

                                        // Create fade-in animations
                                        val fadeInQR =
                                            ObjectAnimator.ofFloat(qrCodeImageView, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInDashedLine =
                                            ObjectAnimator.ofFloat(dashedLine, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInTitle =
                                            ObjectAnimator.ofFloat(
                                                tvTitleQRGenerate,
                                                "alpha",
                                                0f,
                                                1f
                                            )
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInTitleConfirm =
                                            ObjectAnimator.ofFloat(titleQRConfirm, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInDescConfirm =
                                            ObjectAnimator.ofFloat(descQRConfirm, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInButton =
                                            ObjectAnimator.ofFloat(
                                                btnConfirmScanPanenTPH,
                                                "alpha",
                                                0f,
                                                1f
                                            )
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInText = if (dataQR != null) {
                                            ObjectAnimator.ofFloat(dataQR, "alpha", 0f, 1f).apply {
                                                duration = 250
                                                startDelay = 150
                                            }
                                        } else null

                                        // Run animations sequentially
                                        AnimatorSet().apply {
                                            playTogether(fadeOut, fadeOutDots)
                                            addListener(object : AnimatorListenerAdapter() {
                                                override fun onAnimationEnd(animation: Animator) {
                                                    // Hide loading elements
                                                    loadingLogo.visibility = View.GONE
                                                    loadingContainer.visibility = View.GONE

                                                    // Show elements
                                                    tvTitleQRGenerate.visibility = View.VISIBLE
                                                    qrCodeImageView.visibility = View.VISIBLE
                                                    dashedLine.visibility = View.VISIBLE
                                                    titleQRConfirm.visibility = View.VISIBLE
                                                    descQRConfirm.visibility = View.VISIBLE
                                                    btnConfirmScanPanenTPH.visibility = View.VISIBLE
                                                    if (dataQR != null) dataQR.visibility =
                                                        View.VISIBLE

                                                    // Start fade-in animations
                                                    fadeInQR.start()
                                                    fadeInDashedLine.start()
                                                    fadeInTitle.start()
                                                    fadeInTitleConfirm.start()
                                                    fadeInDescConfirm.start()
                                                    fadeInButton.start()
                                                    fadeInText?.start()
                                                }
                                            })
                                            start()
                                        }
                                    } catch (e: Exception) {
                                        // Handle UI-related errors on the main thread
                                        loadingLogo.animation?.cancel()
                                        loadingLogo.clearAnimation()
                                        loadingLogo.visibility = View.GONE
                                        loadingContainer.visibility = View.GONE
                                        AppLogger.e("QR Generation UI Error: ${e.message}")
                                        showErrorMessageGenerateQR(
                                            view,
                                            "Error generating QR code: ${e.message}"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle any other errors
                                withContext(Dispatchers.Main) {
                                    AppLogger.e("Error in QR process: ${e.message}")
                                    stopLoadingAnimation(loadingLogo, loadingContainer)
                                    showErrorMessageGenerateQR(
                                        view,
                                        "Error processing QR code: ${e.message}"
                                    )
                                }
                            }
                        }
                    },
                    cancelFunction = {

                    }
                )

            }


        }
    }

    // Helper function to stop the loading animation and hide UI
    private fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
    }


    // Helper function to show errors
    fun showErrorMessageGenerateQR(view: View, message: String) {
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }


    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Minify JSON first
            val minifiedJson = JSONObject(jsonData).toString()

            // Reject empty JSON
            if (minifiedJson == "{}") {
                AppLogger.e("Empty JSON detected, returning null")
                throw IllegalArgumentException("Empty JSON detected")
            }

            // Create a byte array output stream to hold the zip data
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                ZipOutputStream(byteArrayOutputStream).apply {
                    setLevel(Deflater.BEST_COMPRESSION)
                }.use { zipOutputStream ->
                    val entry = ZipEntry("output.json")
                    zipOutputStream.putNextEntry(entry)
                    zipOutputStream.write(minifiedJson.toByteArray(StandardCharsets.UTF_8))
                    zipOutputStream.closeEntry()
                }

                val zipBytes = byteArrayOutputStream.toByteArray()
                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)

                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                firstHalf + "5nqHzPKdlILxS9ABpClq" + secondHalf
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            throw IllegalArgumentException(e.message.toString())
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        }
    }


    private fun setupObservers() {
        val listBlok = findViewById<TextView>(R.id.listBlok)
        val totalJjg = findViewById<TextView>(R.id.totalJjg)
        val totalTPH = findViewById<TextView>(R.id.totalTPH)
        val blokSection = findViewById<LinearLayout>(R.id.blok_section)
        val totalSection = findViewById<LinearLayout>(R.id.total_section)
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)

        blokSection.visibility = View.GONE
        totalSection.visibility = View.GONE

        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")


        if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
            panenViewModel.archivedCount.observe(this) { count ->
                counterTerscan.text = count.toString()
            }
        } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
            panenViewModel.panenCountTPHESPB.observe(this) { count ->
                counterTerscan.text = count.toString()
            }

        }

        panenViewModel.activePanenList.observe(this) { panenList ->
            if (currentState == 0) {
                listAdapter.updateData(emptyList())
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {

                        if (panenList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            mappedData = panenList.map { panenWithRelations ->

                                val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val pemuatData: List<KaryawanModel>? = withContext(Dispatchers.IO) {
                                    try {
                                        panenViewModel.getPemuatByIdList(pemuatList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                        null
                                    }
                                }

                                val rawKemandoran: List<String> = pemuatData
                                    ?.mapNotNull { it.kemandoran_id?.toString() }
                                    ?.distinct() ?: emptyList()

                                val kemandoranData: List<KemandoranModel>? =
                                    withContext(Dispatchers.IO) {
                                        try {
                                            panenViewModel.getKemandoranById(rawKemandoran)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                            null
                                        }
                                    }

                                val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString("\n") { "• $it" } ?: "-"


                                val karyawanNamas = pemuatData?.mapNotNull { it.nama }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ") ?: "-"


                                mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode
                                        ?: "Unknown"), // Handle null safely
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
//                                    "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                    "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                    "foto" to (panenWithRelations.panen.foto as Any),
                                    "komentar" to (panenWithRelations.panen.komentar as Any),
                                    "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                    "lat" to (panenWithRelations.panen.lat as Any),
                                    "lon" to (panenWithRelations.panen.lon as Any),
                                    "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                    "ancak" to (panenWithRelations.panen.ancak as Any),
                                    "archive" to (panenWithRelations.panen.archive as Any),
                                    "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                    "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                    "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                    "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                    "nama_karyawans" to karyawanNamas as Any,
                                    "nama_kemandorans" to kemandoranNamas as Any,
                                    "username" to (panenWithRelations.panen.username as Any)
                                    )
                            }


                            val distinctBlokNames = mappedData
                                .map { it["blok_name"].toString() }
                                .distinct()
                                .filter { it != "-" }
                                .sorted()
                                .joinToString(", ")

                            var totalJjgCount = 0
                            mappedData.forEach { data ->
                                try {
                                    val jjgJsonString = data["jjg_json"].toString()
                                    val jjgJson = JSONObject(jjgJsonString)
                                    val key =
                                        if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"
                                    totalJjgCount += jjgJson.optInt(key, 0)
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing jjg_json: ${e.message}")
                                }
                            }


                            // Calculate distinct TPH count
                            val distinctTphCount = mappedData
                                .mapNotNull { it["tph_id"].toString().toIntOrNull() }
                                .distinct()
                                .count()

                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            blok = distinctBlokNames.ifEmpty { "-" }
                            listBlok.text = blok
                            jjg = totalJjgCount
                            totalJjg.text = jjg.toString()
                            tph = distinctTphCount
                            totalTPH.text = tph.toString()

                            // Set Blok
                            val tvBlok = findViewById<View>(R.id.tv_blok)
                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

                            // Set jjg
                            val tvJjg = findViewById<View>(R.id.tv_jjg)
                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text = jjg.toString()

                            // Set jjg
                            val tvTph = findViewById<View>(R.id.tv_total_tph)
                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumalh TPH"
                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text = tph.toString()


                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data
                        } else {
                            tvEmptyState.text = "No saved data available"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE
                        }

                    }
                    counterTersimpan.text = panenList.size.toString()

                    if (panenList.size == 0 && featureName == "Rekap Hasil Panen") {
                        btnGenerateQRTPH.visibility = View.GONE
                    } else if (panenList.size > 0 && featureName == "Rekap Hasil Panen") {
                        btnGenerateQRTPH.visibility = View.VISIBLE
                    }
                }, 500)
            }
        }

        panenViewModel.archivedPanenList.observe(this) { panenList ->
            if (currentState == 1) {
                listAdapter.updateData(emptyList())
                btnGenerateQRTPH.visibility = View.GONE
                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBox.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({


                    loadingDialog.dismiss()
                    lifecycleScope.launch {

                        if (panenList.isNotEmpty()) {
                            AppLogger.d("kasdjflkd")
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            mappedData = panenList.map { panenWithRelations ->
                                val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val pemuatData: List<KaryawanModel>? = withContext(Dispatchers.IO) {
                                    try {
                                        panenViewModel.getPemuatByIdList(pemuatList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                        null
                                    }
                                }
                                val rawKemandoran: List<String> = pemuatData
                                    ?.mapNotNull { it.kemandoran_id?.toString() }
                                    ?.distinct() ?: emptyList()

                                val kemandoranData: List<KemandoranModel>? =
                                    withContext(Dispatchers.IO) {
                                        try {
                                            panenViewModel.getKemandoranById(rawKemandoran)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                            null
                                        }
                                    }

                                val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString("\n") { "• $it" } ?: "-"


                                val karyawanNamas = pemuatData?.mapNotNull { it.nama }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ") ?: "-"

                                mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode
                                        ?: "Unknown"), // Handle null safely
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
//                                    "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                    "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                    "foto" to (panenWithRelations.panen.foto as Any),
                                    "komentar" to (panenWithRelations.panen.komentar as Any),
                                    "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                    "lat" to (panenWithRelations.panen.lat as Any),
                                    "lon" to (panenWithRelations.panen.lon as Any),
                                    "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                    "ancak" to (panenWithRelations.panen.ancak as Any),
                                    "archive" to (panenWithRelations.panen.archive as Any),
                                    "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                    "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                    "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                    "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                    "nama_karyawans" to karyawanNamas as Any,
                                    "nama_kemandorans" to kemandoranNamas as Any,

                                    )
                            }

                            val distinctBlokNames = mappedData
                                .map { it["blok_name"]?.toString() ?: "-" }
                                .distinct()
                                .filter { it != "-" }
                                .sorted()
                                .joinToString(", ")

                            // Calculate total JJG by parsing JSON and summing TO values
                            var totalJjgCount = 0
                            mappedData.forEach { data ->
                                try {
                                    val jjgJsonString = data["jjg_json"].toString()
                                    val jjgJson = JSONObject(jjgJsonString)
                                    val key =
                                        if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"

                                    totalJjgCount += jjgJson.optInt(key, 0)
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing jjg_json: ${e.message}")
                                }
                            }

                            // Calculate distinct TPH count
                            val distinctTphCount = mappedData
                                .mapNotNull { it["tph_id"].toString().toIntOrNull() }
                                .distinct()
                                .count()

                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            listBlok.text = distinctBlokNames.ifEmpty { "-" }
                            totalJjg.text = totalJjgCount.toString()
                            totalTPH.text = distinctTphCount.toString()

                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data
                        } else {
                            tvEmptyState.text = "No scanned data available"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE

                            AppLogger.d("kasdjflkd")
                        }

                    }
                    counterTerscan.text = panenList.size.toString()
                }, 500)
            }
        }

        panenViewModel.error.observe(this) { errorMessage ->
            loadingDialog.dismiss()
            showErrorDialog(errorMessage)
        }
    }

    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
    }

    private fun setupSearch() {
        searchEditText = findViewById(R.id.search_feature)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)

        searchEditText.addTextChangedListener(object : TextWatcher {
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
                val query = s.toString().trim()
                listAdapter.filterData(query)

                // Handle empty state
                if (listAdapter.itemCount == 0) {
                    tvEmptyState.text = "Tidak ada data yang dicari"
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        })
    }


    private fun showErrorDialog(errorMessage: String) {
        AlertDialogUtility.withSingleAction(
            this@ListPanenTBSActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)} ${errorMessage}",
            "warning.json",
            R.color.colorRedDark
        ) {
            finish()
        }
    }

    private fun setupCheckboxControl() {
        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)

        headerCheckBox.apply {
            visibility = View.VISIBLE
            setOnCheckedChangeListener(null)
            setOnCheckedChangeListener { _, isChecked ->
                if (!isSettingUpCheckbox) {
                    listAdapter.selectAll(isChecked)
                    speedDial.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
            }
        }

        listAdapter.setOnSelectionChangedListener { selectedCount ->
            isSettingUpCheckbox = true
            headerCheckBox.isChecked = listAdapter.isAllSelected()

            speedDial.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            isSettingUpCheckbox = false
        }
    }


    fun generateHighQualityQRCode(
        content: String,
        imageView: ImageView,
        sizePx: Int = 1000
    ) {
        try {
            // Create encoding hints for better quality
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.M
                ) // Change to M for balance
                put(EncodeHintType.MARGIN, 1) // Smaller margin
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                // Remove fixed QR version to allow automatic scaling
            }

            // Create QR code writer with hints
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            // Create bitmap with appropriate size
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Fill the bitmap
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            // Set the bitmap to ImageView with high quality scaling
            imageView.apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark),
            function = {
                loadingDialog.show()
                loadingDialog.setMessage("Deleting items...")

                panenViewModel.deleteMultipleItems(selectedItems)

                // Observe delete result
                panenViewModel.deleteItemsResult.observe(this) { isSuccess ->
                    loadingDialog.dismiss()
                    if (isSuccess) {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload data based on current state
                        if (currentState == 0) {
                            panenViewModel.loadActivePanen()
                        } else {
                            panenViewModel.loadArchivedPanen()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_failed_delete)} data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Reset UI state
                    val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                        .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                    headerCheckBox.isChecked = false
                    listAdapter.clearSelections()
                    speedDial.visibility = View.GONE
                }

                // Observe errors
                panenViewModel.error.observe(this) { errorMessage ->
                    loadingDialog.dismiss()
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        ) {

        }
    }


    private fun setupSpeedDial() {
        speedDial = findViewById(R.id.dial_tph_list)

        speedDial.apply {
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.scan_qr, R.drawable.baseline_qr_code_scanner_24)
//                    .setLabel(getString(R.string.generate_qr))
//                    .setFabBackgroundColor(
//                        ContextCompat.getColor(
//                            this@ListPanenTBSActivity,
//                            R.color.yellowbutton
//                        )
//                    )
//                    .create()
//            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.deleteSelected,
                    R.drawable.baseline_delete_forever_24
                )
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListPanenTBSActivity,
                            R.color.colorRedDark
                        )
                    )
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.scan_qr -> {
//                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
//
//                        view.background = ContextCompat.getDrawable(this@ListPanenTBSActivity, R.drawable.rounded_top_right_left)
//
//                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
//                        dialog.setContentView(view)
////                        view.layoutParams.height = 500.toPx()
//
//                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
//                        val data = "test"
//                        generateHighQualityQRCode(data, qrCodeImageView)
//                        dialog.setOnShowListener {
//                            val bottomSheet =
//                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
//                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        }
//                        dialog.show()
                        true
                    }
//                    R.id.cancelSelection -> {
//                        listAdapter.clearSelections()
//                        true
//                    }
                    R.id.deleteSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()

//                        if (AppUtils.isInternetAvailable(this@ListPanenTBSActivity)) {
//                            handleUpload(selectedItems)
//                        } else {
//                            AlertDialogUtility.withSingleAction(
//                                this@ListPanenTBSActivity,
//                                getString(R.string.al_back),
//                                getString(R.string.al_no_internet_connection),
//                                getString(R.string.al_no_internet_connection_description),
//                                "network_error.json",
//                                R.color.colorRedDark
//                            ) {}
//                        }
                        true
                    }

                    else -> false
                }
            }
        }


    }

    fun Int.toPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun initializeFilterViews() {
        filterSection = findViewById(R.id.filterSection)
        filterName = findViewById(R.id.filterName)
        removeFilter = findViewById(R.id.removeFilter)

        // Initially hide the filter section
        filterSection.visibility = View.GONE
    }

    private fun setupSortButton() {
        sortButton = findViewById(R.id.btn_sort)
        updateSortIcon() // Set initial icon state

        sortButton.setOnClickListener {
            // Store original data order if this is the first sort
            if (originalData.isEmpty()) {
                originalData = listAdapter.getCurrentData()
            }

            isAscendingOrder = !isAscendingOrder
            updateSortIcon() // Update icon on click

            listAdapter.sortData(isAscendingOrder)
            listAdapter.sortByCheckedItems(false)
            updateFilterDisplay()
        }

        setupRemoveFilter()
    }


    private fun updateFilterDisplay() {
        filterSection.visibility = View.VISIBLE
        filterName.text =
            if (isAscendingOrder) "Urutan Nomor TPH Kecil - Besar" else "Urutan Nomor TPH Besar - Kecil"
    }


    private fun setupRemoveFilter() {
        removeFilter.setOnClickListener {
            // Get current search query
            val currentSearchQuery = searchEditText.text.toString().trim()

            // Reset sort state
            isAscendingOrder = true
            updateSortIcon()

            if (originalData.isNotEmpty()) {
                // Reset the sort but maintain the filter
                listAdapter.resetSort()
                if (currentSearchQuery.isNotEmpty()) {
                    listAdapter.filterData(currentSearchQuery)
                }
                originalData = emptyList()
            }

            // Hide filter section
            filterSection.visibility = View.GONE
        }
    }

//    @SuppressLint("MissingSuperCall")
//    override fun onBackPressed() {
//        vibrate()
////        AlertDialogUtility.withTwoActions(
////            this,
////            "Simpan",
////            getString(R.string.confirmation_dialog_title),
////            getString(R.string.al_confirm_feature),
////            "warning.json"
////        ) {
//        val intent = Intent(this, HomePageActivity::class.java)
//        startActivity(intent)
//        finishAffinity()
////        }
//
//    }

    private fun updateSortIcon() {
        sortButton.animate()
            .scaleY(if (isAscendingOrder) 1f else -1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.GONE

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

    private fun setupRecyclerView() {

        val headers = if (featureName == "Buat eSPB") {
            listOf("BLOK", "NO TPH/JJG", "JAM", "KP")
        } else {
            listOf("BLOK", "NO TPH", "TOTAL JJG", "JAM")
        }
        updateTableHeaders(headers)

        listAdapter = ListPanenTPHAdapter()
        recyclerView.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
        }
        listAdapter.setFeatureAndScanned(featureName, listTPHDriver)
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tableHeader)

        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }
    }

    // Add a helper function to remove duplicate entries
    private fun removeDuplicateEntries(entries: String): String {
        if (entries.isEmpty()) return ""

        val uniqueEntries = entries.split(";")
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(";")

        return uniqueEntries
    }
}
