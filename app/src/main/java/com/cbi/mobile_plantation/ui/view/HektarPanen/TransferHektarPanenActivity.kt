package com.cbi.mobile_plantation.ui.view.HektarPanen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.ui.adapter.TransferHektarPanenAdapter
import com.cbi.mobile_plantation.ui.adapter.TransferHektarPanenData
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


@Suppress("UNREACHABLE_CODE")
class TransferHektarPanenActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: PanenViewModel
    private lateinit var adapter: TransferHektarPanenAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private lateinit var dateButton: Button
    private var mappedData: List<Map<String, Any>> = emptyList()
    private var globalFormattedDate: String = AppUtils.currentDate
    private val dateIndexMap = mutableMapOf<String, Int>()
    private val nikIndexMap = mutableMapOf<String, Int>()
    private lateinit var loadingDialog: LoadingDialog
    private var originalMappedData: MutableList<Map<String, Any>> = mutableListOf()
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    private var jjg = 0
    private var blok = "NULL"
    private var tph = 0



    private lateinit var tvEmptyState: TextView // Add this
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private var activityInitialized = false
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    private var limit = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        //cek tanggal otomatis
        checkDateTimeSettings()
        if (featureName == AppUtils.ListFeatureNames.TransferHektarPanen) {
            setupButtonGenerateQR()
        }
        loadingDialog = LoadingDialog(this)

    }

    private fun setupUI() {
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()

        findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        setupFilterAllData()

        viewModel.getAllScanMPanenByDate(0, AppUtils.currentDate)

        viewModel.activePanenList.observe(this) { panenList ->
//                adapter.updateList(emptyList())
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    lifecycleScope.launch {
                        if (panenList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            val allWorkerData = mutableListOf<Map<String, Any>>()
                            panenList.map { panenWithRelations ->
                                val standardData = mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode ?: "Unknown"),
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
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
                                    "nama_karyawans" to "",
                                    "nama_kemandorans" to "",
                                    "username" to (panenWithRelations.panen.username as Any)
                                )

                                AppLogger.d("panenWithRelations $panenWithRelations")
                                val originalDataMapped = standardData.toMutableMap()
                                originalMappedData.add(originalDataMapped)

                                    val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    val pemuatData: List<KaryawanModel>? =
                                        withContext(Dispatchers.IO) {
                                            try {
                                                viewModel.getPemuatByIdList(pemuatList)
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
                                                viewModel.getKemandoranById(rawKemandoran)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                                null
                                            }
                                        }

                                    val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.joinToString("\n") { "• $it" } ?: "-"

                                    val karyawanNamas = pemuatData?.mapNotNull { karyawan ->
                                        karyawan.nama?.let { nama ->
                                            // Always append NIK for every worker
                                            "$nama - ${karyawan.nik ?: "N/A"}"
                                        }
                                    }?.takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ") ?: "-"

                                    // Update the original data with the fetched names
                                    originalDataMapped["nama_karyawans"] = karyawanNamas
                                    originalDataMapped["nama_kemandorans"] = kemandoranNamas

                                    val updatedStandardData = standardData.toMutableMap().apply {
                                        this["nama_karyawans"] = karyawanNamas
                                        this["nama_kemandorans"] = kemandoranNamas
                                    }

                                    allWorkerData.add(updatedStandardData)

                                    listOf(updatedStandardData)

                            }.flatten()

                            mappedData = allWorkerData

                            val processedData =
                                AppUtils.getPanenProcessedData(originalMappedData, featureName)

                            val blokNames = processedData["blokNames"]?.toString() ?: ""
                            blok = if (blokNames.isEmpty()) "-" else blokNames

                            jjg = processedData["totalJjgCount"]?.toString()!!.toInt()

                            tph = processedData["tphCount"]?.toString()!!.toInt()

//                            // Set Blok
//                            val tvBlok = findViewById<View>(R.id.tv_blok)
//                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
//                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

//                            // Set jjg
//                            val tvJjg = findViewById<View>(R.id.tv_jjg)
//                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
//                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text = jjg.toString()

//                            // Set jjg
//                            val tvTph = findViewById<View>(R.id.tv_total_tph)
//                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumlah TPH"
//                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text = tph.toString()
                            // First, convert the Map<String, Any> data to TransferHektarPanenData objects
                            val transferHektarPanenDataList = allWorkerData.map { item ->
                                val jjgStr = JSONObject(item["jjg_json"] as? String).optDouble("PA", 0.0).toInt().toString()
                                TransferHektarPanenData(
                                    time = (item["date_created"] as? String) ?: "",
                                    blok = (item["blok_name"] as? String) ?: "-",
                                    janjang = jjgStr,
                                    noTph = "${item["nomor"] ?: ""}",
                                    namaPemanen = (item["nama_karyawans"] as? String) ?: "-",
                                    status_scan = 1, // Or any appropriate default
                                    id = (item["id"] as? String)?.toIntOrNull() ?:
                                    (item["id"] as? Int) ?: 0
                                )
                            }

// Then update the adapter with the correctly typed list
                            adapter.updateList(transferHektarPanenDataList)

//                                adapter.updateData(mappedData)
//                                originalData =
//                                    emptyList() // Reset original data when new data is loaded

                        } else {
                            AppLogger.d("panenWithRelations panenList is empty")
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }

                    }
                }, 500)
            }
    }


    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            // Update global date variable
            globalFormattedDate = formattedDate
            // Keep this if AppUtils.setSelectedDate is used elsewhere in your code
            AppUtils.setSelectedDate(formattedDate)

            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun processSelectedDate(selectedDate: String) {
        val filterAllData = findViewById<CheckBox>(R.id.calendarCheckbox)
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        // If "Filter All Data" is checked, uncheck it when user selects a specific date
        if (filterAllData.isChecked) {
            filterAllData.isChecked = false
        }

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate

        viewModel.getAllScanMPanenByDate(0, selectedDate)

        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE

            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )
            AppUtils.setSelectedDate(todayBackendDate)

            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate
            viewModel.getAllScanMPanenByDate(0, todayBackendDate)
        }
        filterDateContainer.visibility = View.VISIBLE
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

        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    @SuppressLint("DefaultLocale")
    private fun setupObserveData() {
        viewModel.panenList.observe(this) { data ->

            if (data.isNotEmpty()) {
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Launch coroutine in lifecycleScope
                lifecycleScope.launch {
                    try {
                        val filteredData = coroutineScope {
                            data.map { item ->
                                async {

                                    val blok = try {
                                        viewModel.getBlokKodeByTphId(item.tph_id.toInt())
                                    } catch (e: Exception) {
                                        Log.e(
                                            "TransferHektarPanenActivity",
                                            "Error getting blok kode",
                                            e
                                        )
                                        "NULL"
                                    }

                                    val hoursAndMinutes = item.date_created.substringAfter(" ")
                                        .substringBeforeLast(":")

                                    val janjang =
                                        JSONObject(item.jjg_json).optDouble("TO", 0.0).toInt()
                                            .toString()

                                    val noTph = try {
                                        viewModel.getNomorTPHbyId(item.tph_id.toInt())
                                    } catch (e: Exception) {
                                        Log.e(
                                            "TransferHektarPanenActivity",
                                            "Error getting blok kode",
                                            e
                                        )
                                        "NULL"
                                    }

                                    val namaPemanen = try {
                                        val nikArray = item.karyawan_nik.replace(" ", "").split(",")
                                        nikArray.map { nik ->
                                            viewModel.getNamaByNik(nik)
                                        }.joinToString(", ")
                                    } catch (e: Exception) {
                                        Log.e(
                                            "TransferHektarPanenActivity",
                                            "Error getting blok kode",
                                            e
                                        )
                                        "NULL"
                                    }

                                    TransferHektarPanenData(
                                        time = hoursAndMinutes,
                                        blok = blok,
                                        janjang = janjang,
                                        noTph = noTph,
                                        status_scan = item.status_scan_mpanen,
                                        namaPemanen = namaPemanen,
                                        id = item.id
                                    )

                                }
                            }.map { it.await() } // Wait for all async tasks to complete
                        }

                        adapter.updateList(filteredData)
                    } catch (e: Exception) {
                        AppLogger.e("Data processing error: ${e.message}")
                    }
                }
            } else {
                tvEmptyState.text = "Belum ada transaksi panen"
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }

        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("WAKTU", "BLOK", "JANJANG", "PEMANEN", "NO TPH")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransferHektarPanenAdapter(emptyList(), this@TransferHektarPanenActivity)
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }

//        val th5 = tableHeader.findViewById<TextView>(R.id.th5)
//        val th6 = tableHeader.findViewById<TextView>(R.id.th6)
//        val layoutParamsTh5 = th5.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTh5.weight = 0.3f
//        th5.layoutParams = layoutParamsTh5
//        val layoutParamsTh6 = th6.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTh6.weight = 0.3f
//        th6.layoutParams = layoutParamsTh6
        val flCheckBoxTableHeaderLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
    }

    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
    }

    // Add this after your dateButton setup in setupUI() method
    private fun setupFilterAllData() {
        val filterAllData = findViewById<CheckBox>(R.id.calendarCheckbox)
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)

        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // User wants to see all data
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"

                // Disable date picker button when viewing all data
                dateButton.isEnabled = false
                dateButton.alpha = 0.5f

                // Load all data without date filter
                viewModel.getAllScanMPanenByDate(0, null)  // Pass null to load all data
            } else {
                // User wants to filter by date
                val displayDate = AppUtils.formatSelectedDateForDisplay(globalFormattedDate)

                // Update UI
                dateButton.text = displayDate
                nameFilterDate.text = displayDate

                // Enable date picker button
                dateButton.isEnabled = true
                dateButton.alpha = 1f

                // Load data for the selected date
                viewModel.getAllScanMPanenByDate(0, globalFormattedDate)
            }

            // Setup remove filter button
            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)
            removeFilterDate.setOnClickListener {
                if (filterAllData.isChecked) {
                    filterAllData.isChecked = false
                }

                filterDateContainer.visibility = View.GONE

                // Reset to today's date
                val todayBackendDate = AppUtils.formatDateForBackend(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR)
                )

                globalFormattedDate = todayBackendDate

                // Update UI
                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate

                // Load today's data
                viewModel.getAllScanMPanenByDate(0, todayBackendDate)
            }
        }
    }

    private fun setupHeader() {
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()
        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }


    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        val btnGenerateQRTPHUnl = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPHUnl)
        val tvGenQR60 = findViewById<TextView>(R.id.tvGenQR60)
        val tvGenQRFull = findViewById<TextView>(R.id.tvGenQRFull)

        btnGenerateQRTPH.visibility = View.VISIBLE
        btnGenerateQRTPHUnl.visibility = View.VISIBLE
        tvGenQR60.visibility = View.VISIBLE
        tvGenQRFull.visibility = View.VISIBLE

        btnGenerateQRTPH.setOnClickListener {
            generateQRTPH(60)
        }
        btnGenerateQRTPHUnl.setOnClickListener {
            generateQRTPH(0)
        }
    }

    private fun generateQRTPH(limitFun: Int) {
        limit = limitFun
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
                val dialog = BottomSheetDialog(this)
                dialog.setContentView(view)
                // Get references to views
                val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
//                        val qrCodeImageView: com.github.chrisbanes.photoview.PhotoView = view.findViewById(R.id.qrCodeImageView)
                val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                val tvTitleQRGenerate: TextView =
                    view.findViewById(R.id.textTitleQRGenerate)
                tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
                val dashedLine: View = view.findViewById(R.id.dashedLine)
                val loadingContainer: LinearLayout =
                    view.findViewById(R.id.loadingDotsContainerBottomSheet)
                val titleQRConfirm: TextView = view.findViewById(R.id.titleAfterScanQR)
                val descQRConfirm: TextView = view.findViewById(R.id.descAfterScanQR)
                val confimationContainer: LinearLayout =
                    view.findViewById(R.id.confirmationContainer)
                val scrollContent: NestedScrollView = view.findViewById(R.id.scrollContent)
                scrollContent.post {
                    // Scroll to the middle to show QR code in center
                    // 300dp space + approximately half of QR view height (125dp)
                    scrollContent.smoothScrollTo(0, 600)
                }

                val btnConfirmScanPanenTPH: MaterialButton =
                    view.findViewById(R.id.btnConfirmScanPanenTPH)

                // Initially hide QR code and dashed line, show loading
                qrCodeImageView.visibility = View.GONE
                loadingLogo.visibility = View.VISIBLE
                loadingContainer.visibility = View.VISIBLE

                // Initial setup for text elements
                titleQRConfirm.setResponsiveTextSizeWithConstraints(21F, 17F, 25F)
                descQRConfirm.setResponsiveTextSizeWithConstraints(19F, 15F, 23F)

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

                val maxHeight = (resources.displayMetrics.heightPixels * 0.85).toInt()

                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bottomSheet ->
                        val behavior = BottomSheetBehavior.from(bottomSheet)

                        behavior.apply {
                            this.peekHeight =
                                maxHeight  // Set the initial height when peeking
                            this.state =
                                BottomSheetBehavior.STATE_EXPANDED  // Start fully expanded
                            this.isFitToContents =
                                true  // Content will determine the height (up to maxHeight)
                            this.isDraggable =
                                false  // Prevent user from dragging the sheet
                        }

                        // Set a fixed height for the bottom sheet
                        bottomSheet.layoutParams?.height = maxHeight
                    }

                dialog.show()


                btnConfirmScanPanenTPH.setOnClickListener {
                    onBackPressed()
                }

                lifecycleScope.launch {
                    try {

                        val jsonData = withContext(Dispatchers.IO) {
                            try {
                                val effectiveLimit =
                                    if (limitFun == 0) mappedData.size else limitFun
                                // Take only the required number of items
                                val limitedData = mappedData.take(effectiveLimit)
                                formatPanenDataForQR(limitedData)
                            } catch (e: Exception) {
                                AppLogger.e("Error generating JSON data: ${e.message}")
                                throw e
                            }
                        }

                        AppLogger.d("jsonData $jsonData")
                        val encodedData = withContext(Dispatchers.IO) {
                            try {
                                ListPanenTBSActivity().encodeJsonToBase64ZipQR(jsonData)
                                    ?: throw Exception("Encoding failed")
                            } catch (e: Exception) {
                                AppLogger.e("Error encoding data: ${e.message}")
                                throw e
                            }
                        }

                        // Switch to the main thread for UI updates
                        withContext(Dispatchers.Main) {
                            try {
                                ListPanenTBSActivity().generateHighQualityQRCode(encodedData, qrCodeImageView)
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
                                confimationContainer.alpha = 0f
                                descQRConfirm.alpha = 0f
                                btnConfirmScanPanenTPH.alpha = 0f

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

                                val fadeInConfirmationContainer = ObjectAnimator.ofFloat(
                                    confimationContainer,
                                    "alpha",
                                    0f,
                                    1f
                                ).apply {
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


                                // Run animations sequentially
                                AnimatorSet().apply {
                                    playTogether(fadeOut, fadeOutDots)
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            // Hide loading elements
                                            loadingLogo.visibility = View.GONE
                                            loadingContainer.visibility = View.GONE

                                            // Show elements
                                            confimationContainer.visibility = View.VISIBLE
                                            tvTitleQRGenerate.visibility = View.VISIBLE
                                            qrCodeImageView.visibility = View.VISIBLE
                                            dashedLine.visibility = View.VISIBLE

                                            btnConfirmScanPanenTPH.visibility = View.VISIBLE

                                            lifecycleScope.launch {
                                                delay(200)
                                                playSound(R.raw.berhasil_generate_qr)
                                                delay(300)


                                            }

                                            // Start fade-in animations
                                            fadeInQR.start()
                                            fadeInDashedLine.start()
                                            fadeInTitle.start()
                                            fadeInTitleConfirm.start()
                                            fadeInConfirmationContainer.start()
                                            fadeInDescConfirm.start()
                                            fadeInButton.start()

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
                                ListPanenTBSActivity().showErrorMessageGenerateQR(
                                    view,
                                    "Error generating QR code: ${e.message}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any other errors
                        withContext(Dispatchers.Main) {
                            AppLogger.e("Error in QR process: ${e.message}")
                            ListPanenTBSActivity().stopLoadingAnimation(loadingLogo, loadingContainer)
                            ListPanenTBSActivity().showErrorMessageGenerateQR(
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
                        val nik = data["nik"]?.toString()
                            ?: throw IllegalArgumentException("Missing nik.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val keyUn = "UN"
                        val unValue = if (jjgJson.has(keyUn)) {
                            jjgJson.getInt(keyUn)
                        } else {
                            throw IllegalArgumentException("Missing '$keyUn' key in jjg_json: $jjgJsonString")
                        }

                        val keyOv = "OV"
                        val ovValue = if (jjgJson.has(keyOv)) {
                            jjgJson.getInt(keyOv)
                        } else {
                            throw IllegalArgumentException("Missing '$keyOv' key in jjg_json: $jjgJsonString")
                        }

                        val keyEm = "EM"
                        val emValue = if (jjgJson.has(keyEm)) {
                            jjgJson.getInt(keyEm)
                        } else {
                            throw IllegalArgumentException("Missing '$keyEm' key in jjg_json: $jjgJsonString")
                        }

                        val keyAb = "AB"
                        val abValue = if (jjgJson.has(keyAb)) {
                            jjgJson.getInt(keyAb)
                        } else {
                            throw IllegalArgumentException("Missing '$keyAb' key in jjg_json: $jjgJsonString")
                        }

                        val keyRi = "RI"
                        val riValue = if (jjgJson.has(keyRi)) {
                            jjgJson.getInt(keyRi)
                        } else {
                            throw IllegalArgumentException("Missing '$keyRi' key in jjg_json: $jjgJsonString")
                        }

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-03-28
                        val time = dateParts[1]  // 13:15:18

                        // Use dateIndexMap.size as the index for new dates
                        append("${dateIndexMap.getOrPut(date) {dateIndexMap.size}},$time,$tphId,${nikIndexMap.getOrPut(nik) {nikIndexMap.size}},$unValue,$ovValue,$emValue,$abValue,$riValue;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            val username = try {
                PrefManager(this).username.toString().split("@")[0].takeLast(3).uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil username: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                "NULL"
            }

            // Create the tgl object with date mappings
            val tglJson = JSONObject()
            dateIndexMap.forEach { (date, index) ->
                tglJson.put(index.toString(), date)
            }

            // Create the tgl object with date mappings
            val nikJson = JSONObject()
            nikIndexMap.forEach { (date, index) ->
                nikJson.put(index.toString(), date)
            }

            return JSONObject().apply {
                put("tph_0", formattedData)
                put("username", username)
                put("tgl", tglJson)
                put("nik", nikJson)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

}
