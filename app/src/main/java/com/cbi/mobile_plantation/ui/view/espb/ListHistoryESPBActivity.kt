package com.cbi.mobile_plantation.ui.view.espb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.displayHektarPanenTanggalBlok
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.ui.adapter.ESPBAdapter
import com.cbi.mobile_plantation.ui.adapter.ESPBData
import com.cbi.mobile_plantation.ui.adapter.ListHektarPanenAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel
import com.cbi.mobile_plantation.ui.viewModel.HektarPanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.MathFun
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.jaredrummler.materialspinner.MaterialSpinner
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


@Suppress("UNREACHABLE_CODE")
class ListHistoryESPBActivity : AppCompatActivity(),
    ListHektarPanenAdapter.OnLuasPanenChangeListener{
    private lateinit var recyclerView: RecyclerView
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var hektarPanenViewModel: HektarPanenViewModel
    private lateinit var adapterHektarPanen: ListHektarPanenAdapter
    private lateinit var adapterESPB: ESPBAdapter
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

    private lateinit var blokIdList: List<Int>

    // Define a variable to store clicked chips
    private var selectedBlokId: Int? = null

    private val updateRunnables = HashMap<Int, Runnable>()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvEmptyState: TextView // Add this
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private var activityInitialized = false
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        //cek tanggal otomatis
        checkDateTimeSettings()
    }

    private fun setupUI() {
        initViewModel()
        initializeApp()
        setupHeader()
        setupRecyclerView()
        initializeViews()
        findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        setupFilterAllData()
        if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
            setupObserveDataRekapESPB()
            espbViewModel.loadHistoryESPBNonScan(AppUtils.currentDate)
        } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            Log.d("testListHektarPanen", "cek")
            setupObserveDataDaftarHektarPanen()
//            hektarPanenViewModel.loadHektarPanenData(AppUtils.currentDate)
        }
    }

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up all pending runnables
        updateRunnables.forEach { (_, runnable) ->
            handler.removeCallbacks(runnable)
        }
        updateRunnables.clear()
        AppUtils.resetSelectedDate()
        SoundPlayer.releaseMediaPlayer()
        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
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
//            setupBlokFilter()
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }


    // Modify the processSelectedDate method to clear blok filters when date changes
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

        if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
            espbViewModel.loadHistoryESPBNonScan(selectedDate)
        } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
//            hektarPanenViewModel.loadHektarPanenData(selectedDate)
//            setupBlokFilter()
            selectedBlokId = null
            refreshHektarPanenUI()
        }

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

            if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
                espbViewModel.loadHistoryESPBNonScan(todayBackendDate)
            } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
                refreshHektarPanenUI()
            }
        }
        filterDateContainer.visibility = View.VISIBLE
    }


    private fun refreshHektarPanenUI() {

        lifecycleScope.launch(Dispatchers.Main) {

            hektarPanenViewModel.loadHektarPanenData(
                globalFormattedDate,
                selectedBlokId
            )

            setupBlokFilter()

            updateJenisPanenIndicator()
        }
    }


    private fun checkDateTimeSettings() {
        if (!AppUtils.isDateTimeValid(this, prefManager!!)) {
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
        if (activityInitialized && AppUtils.isDateTimeValid(this, prefManager!!)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {
        super.onPause()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }


    // Modify the ChipGroup setup to be much simpler
    @SuppressLint("SetTextI18n")
    private fun setupBlokFilter() {
        if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            hektarPanenViewModel.loadHektarPanenData(globalFormattedDate, selectedBlokId)
            val cgBlok = findViewById<ChipGroup>(R.id.cgBlok)

            // Set single selection mode
            cgBlok.isSingleSelection = true // Only one chip can be selected at a time

            // Launch coroutine in lifecycleScope
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Get distinct blok parameters instead of just IDs
                    val blokParamsList =
                        hektarPanenViewModel.getDistinctBlokParamsByDate(globalFormattedDate!!)
                    Log.d("BlokParams", "Blok Params: $blokParamsList")

                    val blokDataWithCounts = blokParamsList.map { blokParams ->
                        AppLogger.d("Processing blokParams: $blokParams")

                        // Use the parameters you have - check which ones are available
                        val blok = blokParams.blok
                        val blokPpro = blokParams.blok_ppro
                        val deptAbbr = blokParams.dept_abbr  // This was null in your log
                        val divisiAbbr = blokParams.divisi_abbr  // This was null in your log

                        AppLogger.d("Calling getBlokByParams with: blok=$blok, blokPpro=$blokPpro, deptAbbr=$deptAbbr, divisiAbbr=$divisiAbbr")

                        val blokData =
                            espbViewModel.getBlokByParams(blok, blokPpro, deptAbbr, divisiAbbr)

                        AppLogger.d("Result blokData: $blokData")

                        val count0 = try {
                            hektarPanenViewModel.countWhereLuasPanenIsZeroAndDateAndBlok(
                                blokParams.blok,
                                globalFormattedDate
                            )
                        } catch (e: Exception) {
                            Log.d("BlokIds", "Error getting count: ${e.message}")
                            -1
                        }

                        Triple(blokParams, blokData, count0)
                    }

                    AppLogger.d("blokDataWithCounts $blokDataWithCounts")
                    // Switch back to main thread for UI operations
                    withContext(Dispatchers.Main) {
                        // Clear any existing chips
                        cgBlok.removeAllViews()

                        var firstChipId = -1 // Variable to store first chip ID

                        // Add chips for each blok
                        blokDataWithCounts.forEach { (blokParams, blokData, count0) ->
                            val chip = Chip(this@ListHistoryESPBActivity).apply {
                                id = View.generateViewId() // Generate unique ID for each chip

                                // Use blokData if available, otherwise use blokParams
                                val blokCode = blokData?.kode ?: blokParams.blok.toString()
                                val blokText = "${blokCode}($count0)"

                                text = blokText
                                isClickable = true
                                isCheckable = true
                                isCheckedIconVisible = true

                                // Store the blok ID as tag
                                tag = blokParams.blok // Store the actual blok ID

                                // Store first chip ID if not set yet
                                if (firstChipId == -1) {
                                    firstChipId = id
                                }

                                // Add listener to detect when chip is checked/unchecked
                                setOnCheckedChangeListener { _, isChecked ->
                                    if (isChecked) {
                                        selectedBlokId = blokParams.blok
                                        Log.d("SelectedChip", "Selected ID: $selectedBlokId")
                                    } else {
                                        selectedBlokId = null
                                        Log.d("SelectedChip", "No chip selected")
                                    }
                                }
                            }
                            cgBlok.addView(chip)
                        }

                        // Set ChipGroup listener for additional control
                        cgBlok.setOnCheckedChangeListener { group, checkedId ->
                            val selectedBlokId = getSelectedBlokId()
                            Log.d("ChipSelection", "Selected Blok ID: $selectedBlokId")

                            val ll_HektarPanenSum =
                                findViewById<LinearLayout>(R.id.ll_HektarPanenSum)
                            if (selectedBlokId != null && selectedBlokId > 0) {
                                ll_HektarPanenSum.visibility = View.VISIBLE
                                // Filter by both date and blok
//                                hektarPanenViewModel.loadHektarPanenData(
//                                    globalFormattedDate,
//                                    selectedBlokId
//                                )

                                hektarPanenViewModel.loadHektarPanenData(
                                    globalFormattedDate,
                                    selectedBlokId
                                )

                                updateJenisPanenIndicator()

//                                refreshHektarPanenUI()

                                lifecycleScope.launch(Dispatchers.IO) {
                                    val luasPanen = try {
                                        hektarPanenViewModel.getSumLuasPanen(
                                            selectedBlokId,
                                            globalFormattedDate!!
                                        )
                                    } catch (e: Exception) {
                                        Log.d("BlokIds", "Error getting sumLuasPanen: ${e.message}")
                                        0f
                                    }
                                    Log.d("BlokIds", "Sisa Luas Panen: $luasPanen")
                                    withContext(Dispatchers.Main) {
                                        val tvLuasPanen = findViewById<TextView>(R.id.tvLuasPanen)
                                        tvLuasPanen.text = "${luasPanen}ha"
                                    }
                                }
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val luasBlok = try {
                                        hektarPanenViewModel.getLuasBlokByBlok(selectedBlokId)
                                    } catch (e: Exception) {
                                        Log.d("BlokIds", "Error getting luasBlok: ${e.message}")
                                        0f
                                    }
                                    Log.d("BlokIds", "Luas Blok: $luasBlok")
                                    withContext(Dispatchers.Main) {
                                        val tvLuasBlok = findViewById<TextView>(R.id.tvLuasBlok)
                                        tvLuasBlok.text = "${luasBlok}ha"
                                    }
                                }
                            } else {
                                ll_HektarPanenSum.visibility = View.GONE
                                // Just filter by date (all bloks)
//                                hektarPanenViewModel.loadHektarPanenData(
//                                    globalFormattedDate,
//                                    selectedBlokId
//                                )

                                refreshHektarPanenUI()
                            }
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toasty.error(
                            this@ListHistoryESPBActivity,
                            "Error fetching Blok Data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
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

    // Implement the listener method to handle changes from the adapter
    override fun onLuasPanenChanged(id: Int, newValue: Float) {
        // Remove any pending updates for this id
        updateRunnables[id]?.let { runnable ->
            handler.removeCallbacks(runnable)
        }

        // Create a new runnable for this update
        val updateRunnable = Runnable {
            // Execute database update in a coroutine
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val newValueDec = MathFun().round(newValue, 2)
                    val result = hektarPanenViewModel.updateLuasPanen(id, newValueDec!!)

                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        if (result > 0) {
                            Log.d(
                                "ListHistoryESPBActivity",
                                "Database updated successfully for id: $id"
                            )
                            // Update adapter's data structure
                            adapterHektarPanen.updateItemLuasPanen(id, newValueDec)
                        } else {
                            Log.d("ListHistoryESPBActivity", "Database update failed for id: $id")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("ListHistoryESPBActivity", "Error updating database: ${e.message}")
                }
                val luasPanen = try {
                    hektarPanenViewModel.getSumLuasPanen(selectedBlokId!!, globalFormattedDate)
                } catch (e: Exception) {
                    Log.d("BlokIds", "Error getting sumLuasPanen: ${e.message}")
                    0f
                }
                withContext(Dispatchers.Main) {
                    val tvLuasPanen = findViewById<TextView>(R.id.tvLuasPanen)
                    tvLuasPanen.text = "${luasPanen}ha"
                }
            }
        }

        // Store the runnable for future reference
        updateRunnables[id] = updateRunnable

        // Schedule the update with a delay
        handler.postDelayed(updateRunnable, 4000)
    }



    private fun setupObserveDataDaftarHektarPanen() {

        hektarPanenViewModel.historyHektarPanen.observe(this) { data ->
            Log.d("HektarPanen", "Received data: ${data.size} items")

            if (data.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                return@observe
            }

            // If we have data, show it immediately with minimal processing
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            // Display with basic information first
            lifecycleScope.launch {
                val displayItems = data.map { item ->
                    // Get user details based on NIK
                    val nama = withContext(Dispatchers.IO) {
                        hektarPanenViewModel.getKaryawanByNik(item.nik.toString())
                    }

                    val estateAbbr = item.dept_abbr  // String abbreviation
                    val divisiAbbr = item.divisi_abbr  // String abbreviation
                    val blokPpro = item.blok_ppro
                    val blokId = item.blok  // Use blok_ppro if available, fallback to blok

                    // Process blok data with extracted parameters
                    val blokData = withContext(Dispatchers.IO) {
                        espbViewModel.getBlokByParams(blokId, blokPpro, estateAbbr, divisiAbbr)
                    }

                    // Get blok information - blokData is BlokModel? (nullable single object)
                    val blokInfo = if (blokData != null) {
                        blokData.kode ?: item.blok.toString()
                    } else {
                        item.blok?.toString() ?: "-"
                    }

                    // Get blok area - blokData is BlokModel? (nullable single object)
                    val luasBlok = blokData?.luas_area?.toString() ?: "-"

                    // Format dibayar array (if exists in your data)
                    val dibayarArr = item.dibayar_arr ?: "-"

                    displayHektarPanenTanggalBlok(
                        nama = nama,
                        luas_panen = item.luas_panen,
                        blok = blokInfo,
                        luas_blok = luasBlok,
                        dibayar_arr = dibayarArr,
                        nik = item.nik.toString(),
                        jenis_panen = item.jenis_panen,
                        id = item.id!!
                    )
                }
                adapterHektarPanen.updateList(displayItems)
            }
        }
    }

    private fun setupObserveDataRekapESPB() {
        espbViewModel.historyESPBNonScan.observe(this) { data ->
            if (data.isNotEmpty()) {
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Launch coroutine in lifecycleScope
                lifecycleScope.launch {
                    try {
                        val filteredData = coroutineScope {
                            data.map { item ->
                                async {

                                    // First extract TPH IDs from tph1 column to get tphCount
                                    val tphEntries = item.tph1.split(";").filter { it.isNotEmpty() }
                                    val tphCount = tphEntries.size

                                    AppLogger.d("tph Entries $tphEntries")
                                    // Process blok_jjg data to get janjang sum

                                    AppLogger.d("itemBLOK ${item.blok_jjg}")
                                    val blokJjgList = item.blok_jjg
                                        .split(";")
                                        .mapNotNull {
                                            it.split(",").takeIf { it.size == 2 }
                                                ?.let { (id, jjg) ->
                                                    id.toIntOrNull()
                                                        ?.let { idInt -> idInt to jjg.toIntOrNull() }
                                                }
                                        }

                                    val idBlokList = blokJjgList.map { it.first }


                                    AppLogger.d("idBlokList $idBlokList")
                                    // Get blok data from repository
                                    val blokData = try {
                                        withContext(Dispatchers.IO) {
                                            espbViewModel.getBlokById(idBlokList)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                                        emptyList()
                                    }

                                    AppLogger.d("blokData $blokData")
                                    // Extract blok_kode values for display
                                    val blokDisplay = if (blokData.isNotEmpty()) {
                                        blokData.mapNotNull { it.kode }.distinct()
                                            .joinToString(", ")
                                    } else {
                                        // Fallback to just listing the blok IDs if we can't get the names
                                        idBlokList.distinct().joinToString(", ") { it.toString() }
                                    }

                                    ESPBData(
                                        time = item.created_at.ifEmpty { "-" },
                                        blok = blokDisplay,
                                        janjang = blokJjgList.sumOf { it.second ?: 0 }.toString(),
                                        tphCount = tphCount.toString(),
                                        status_mekanisasi = item.status_mekanisasi,
                                        status_scan = item.status_draft,
                                        id = item.id
                                    )

                                }
                            }.map { it.await() } // Wait for all async tasks to complete
                        }

                        adapterESPB.updateList(filteredData)
                    } catch (e: Exception) {
                        AppLogger.e("Data processing error: ${e.message}")
                    }
                }
            } else {
                tvEmptyState.text = "Belum ada data eSPB"
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }

        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
            val headers = listOf("WAKTU", "BLOK", "KIRIM PABRIK", "TPH", "SCAN")
            updateTableHeaders(headers)
            adapterESPB = ESPBAdapter(emptyList(), this@ListHistoryESPBActivity)
            recyclerView.adapter = adapterESPB
        } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            val headers = listOf("NAMA", "BLOK", "DIBAYAR", "HEKTAR")
            updateTableHeaders(headers)

            // Create adapter without passing in ViewModel
            adapterHektarPanen = ListHektarPanenAdapter(emptyList(), this@ListHistoryESPBActivity)
            adapterHektarPanen.setOnLuasPanenChangeListener(this) // Set the listener
            recyclerView.adapter = adapterHektarPanen
        }
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)
        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE
                text = headerNames[i]
            }
        }
        val th5 = tableHeader.findViewById<TextView>(R.id.th5)
        val layoutParamsTh5 = th5.layoutParams as LinearLayout.LayoutParams
        layoutParamsTh5.weight = if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) 1.5f else 0.3f
        th5.layoutParams = layoutParamsTh5

        val flCheckBoxTableHeaderLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
    }

    private fun initViewModel() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val appRepository = AppRepository(application)
        val factoryEspb = ESPBViewModel.ESPBViewModelFactory(appRepository)
        espbViewModel = ViewModelProvider(this, factoryEspb)[ESPBViewModel::class.java]
        if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            val factory = HektarPanenViewModel.HektarPanenViewModelFactory(appRepository)
            hektarPanenViewModel =
                ViewModelProvider(this, factory)[HektarPanenViewModel::class.java]
        }
    }

    private fun updateJenisPanenIndicator() {

        val btnIsiJenisPanen =
            findViewById<Button>(R.id.btnIsiJenisPanen)

        lifecycleScope.launch(Dispatchers.IO) {

            val total =
                hektarPanenViewModel
                    .countTotalJenisPanen(
                        globalFormattedDate!!
                    )



            val filled =
                hektarPanenViewModel
                    .countFilledJenisPanen(
                        globalFormattedDate!!
                    )

            withContext(Dispatchers.Main) {

                if (total == 0) {

                    btnIsiJenisPanen.visibility = View.GONE

                } else {

                    btnIsiJenisPanen.visibility = View.VISIBLE

                    btnIsiJenisPanen.text =
                        "$filled/$total Jenis Panen Blok"

                    when {

                        filled == 0 -> {

                            btnIsiJenisPanen.setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@ListHistoryESPBActivity,
                                        R.color.colorRedDark
                                    )
                                )
                            )
                        }

                        filled < total -> {

                            btnIsiJenisPanen.setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@ListHistoryESPBActivity,
                                        R.color.yellowbutton
                                    )
                                )
                            )
                        }

                        else -> {

                            btnIsiJenisPanen.setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@ListHistoryESPBActivity,
                                        R.color.greenDarker
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    // Add this after your dateButton setup in setupUI() method
    private fun setupFilterAllData() {

        val btnIsiJenisPanen = findViewById<Button>(R.id.btnIsiJenisPanen)

        val filterAllData = findViewById<CheckBox>(R.id.calendarCheckbox)
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)

        if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            filterAllData.visibility = View.GONE
            val calendarCheckboxCaption = findViewById<TextView>(R.id.calendarCheckboxCaption)
            calendarCheckboxCaption.visibility = View.GONE
            btnIsiJenisPanen.visibility = View.VISIBLE

            updateJenisPanenIndicator()
        }


        btnIsiJenisPanen.setOnClickListener {
            showJenisPanenBottomSheet()
        }

        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // User wants to see all data
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"

                // Disable date picker button when viewing all data
                dateButton.isEnabled = false
                dateButton.alpha = 0.5f
                globalFormattedDate = AppUtils.currentDate

                // Load all data without date filter
                if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
                    espbViewModel.loadHistoryESPBNonScan(null)  // Pass null to load all data
                } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
                    hektarPanenViewModel.loadHektarPanenData(
                        null,
                        selectedBlokId
                    )  // Pass null to load all data
                }
            } else {
                // User wants to filter by date
                val displayDate = AppUtils.formatSelectedDateForDisplay(globalFormattedDate!!)


                // Update UI
                dateButton.text = displayDate
                nameFilterDate.text = displayDate

                // Enable date picker button
                dateButton.isEnabled = true
                dateButton.alpha = 1f

                // Load data for the selected date
                if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
                    espbViewModel.loadHistoryESPBNonScan(globalFormattedDate)
                } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
                    hektarPanenViewModel.loadHektarPanenData(
                        globalFormattedDate,
                        selectedBlokId
                    )  // Pass null to load all data
                }
            }

            // Setup remove filter button
            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)
            removeFilterDate.setOnClickListener {
                if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
                    if (filterAllData.isChecked) {
                        filterAllData.isChecked = false
                    }
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
                if (featureName == AppUtils.ListFeatureNames.RekapESPB) {
                    espbViewModel.loadHistoryESPBNonScan(todayBackendDate)

                } else if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
                    hektarPanenViewModel.loadHektarPanenData(todayBackendDate, selectedBlokId)
                }
            }
        }
    }

    private fun showJenisPanenBottomSheet() {

        val bottomSheetDialog = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.layout_bottom_sheet_edit_nama_pemanen,
            null
        )

        bottomSheetDialog.setContentView(view)

        val title =
            view.findViewById<TextView>(R.id.titleDialogDetailTable)

        title.text = "Jenis Panen Blok"

        val contentContainer =
            view.findViewById<LinearLayout>(R.id.contentContainer)

        view.findViewById<Button>(R.id.btnUpdatePemanen).text = "Simpan"

        // HIDE DEFAULT INCLUDE
        val existingLayout =
            view.findViewById<View>(R.id.layoutPemanen)

        existingLayout.visibility = View.GONE
        val changedJenisPanenMap = mutableMapOf<Int, Int>()

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val blokParamsList =
                    hektarPanenViewModel
                        .getDistinctBlokParamsByDate(globalFormattedDate!!)

                val blokDataWithCounts = blokParamsList.map { blokParams ->

                    val blokData =
                        espbViewModel.getBlokByParams(
                            blokParams.blok,
                            blokParams.blok_ppro,
                            blokParams.dept_abbr,
                            blokParams.divisi_abbr
                        )

                    Triple(blokParams, blokData, 0)
                }

                withContext(Dispatchers.Main) {

                    blokDataWithCounts.forEach { (blokParams, blokData, _) ->

                        // INFLATE NEW DROPDOWN LAYOUT
                        val spinnerLayout = layoutInflater.inflate(
                            R.layout.pertanyaan_spinner_layout,
                            contentContainer,
                            false
                        )

                        val tvTitle =
                            spinnerLayout.findViewById<TextView>(
                                R.id.tvTitleFormPanenTBS
                            )

                        val spinner =
                            spinnerLayout.findViewById<MaterialSpinner>(
                                R.id.spPanenTBS
                            )

                        // BLOK NAME
                        val blokCode =
                            blokData?.kode
                                ?: blokParams.blok.toString()

                        tvTitle.text = blokCode

                        // DROPDOWN ITEMS
                        val jenisPanenList = listOf(
                            "-- Pilih Jenis Panen --",
                            "Normal",
                            "Cut & Carry"
                        )

                        spinner.setItems(jenisPanenList)

                        lifecycleScope.launch(Dispatchers.IO) {

                            val currentJenisPanen =
                                hektarPanenViewModel
                                    .getJenisPanenByBlok(
                                        blokParams.blok
                                    )

                            withContext(Dispatchers.Main) {

                                spinner.selectedIndex =
                                    when(currentJenisPanen) {

                                        null -> 0 // Placeholder

                                        0 -> 1 // Normal

                                        1 -> 2 // Cut & Carry

                                        else -> 0
                                    }

                                AppLogger.d(
                                    "Blok $blokCode currentJenisPanen $currentJenisPanen"
                                )
                            }
                        }

                        // LISTENER
                        spinner.setOnItemSelectedListener { view, position, _, item ->

                            // Prevent selecting placeholder
                            if (position == 0) {

                                // If user tries to re-select placeholder
                                // restore previous valid value

                                val currentJenisPanen =
                                    changedJenisPanenMap[blokParams.blok]

                                spinner.selectedIndex =
                                    when(currentJenisPanen) {

                                        0 -> 1 // Normal

                                        1 -> 2 // Cut & Carry

                                        else -> 0
                                    }

                                Toast.makeText(
                                    this@ListHistoryESPBActivity,
                                    "Pilih jenis panen yang valid",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@setOnItemSelectedListener
                            }

                            val selectedValue =
                                when(position) {

                                    1 -> 0 // Normal

                                    2 -> 1 // Cut & Carry

                                    else -> null
                                }

                            if (selectedValue != null) {

                                changedJenisPanenMap[blokParams.blok] =
                                    selectedValue

                                AppLogger.d(
                                    "Changed blok $blokCode -> $selectedValue"
                                )
                            }
                        }

                        // ADD TO CONTAINER
                        contentContainer.addView(spinnerLayout)
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@ListHistoryESPBActivity,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // BUTTON
        view.findViewById<Button>(R.id.btnCancel)
            .setOnClickListener {
                bottomSheetDialog.dismiss()
            }

        view.findViewById<Button>(R.id.btnUpdatePemanen)
            .setOnClickListener {

                AlertDialogUtility.withTwoActions(
                    this,
                    getString(R.string.al_yes),
                    getString(R.string.confirmation_dialog_title),
                    "Apakah anda yakin ingin mengubah jenis panen blok?",
                    "warning.json",
                    ContextCompat.getColor(this, R.color.greenDarker),

                    function = {

                        lifecycleScope.launch(Dispatchers.IO) {

                            try {

                                hektarPanenViewModel
                                    .updateJenisPanenBulk(
                                        changedJenisPanenMap
                                    )

                                withContext(Dispatchers.Main) {

                                    Toast.makeText(
                                        this@ListHistoryESPBActivity,
                                        "Berhasil update jenis panen",
                                        Toast.LENGTH_SHORT
                                    ).show()
//                                    updateJenisPanenIndicator()

                                    refreshHektarPanenUI()
                                    bottomSheetDialog.dismiss()
                                }

                            } catch (e: Exception) {

                                withContext(Dispatchers.Main) {

                                    Toast.makeText(
                                        this@ListHistoryESPBActivity,
                                        e.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                ) {

                }
            }


        val maxHeight =
            (resources.displayMetrics.heightPixels * 0.70).toInt()

        bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.let { bottomSheet ->

            val behavior = BottomSheetBehavior.from(bottomSheet)

            behavior.apply {
                peekHeight = maxHeight
                state = BottomSheetBehavior.STATE_EXPANDED
                isFitToContents = true
            }

            bottomSheet.layoutParams.height = maxHeight
        }
        bottomSheetDialog.show()
    }


    @SuppressLint("SetTextI18n")
    private fun setupHeader() {
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
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
        setupBlokFilter()
    }

    // Function to get currently selected blok ID (single selection)
    private fun getSelectedBlokId(): Int? {
        val cgBlok = findViewById<ChipGroup>(R.id.cgBlok)

        // Get the checked chip ID from ChipGroup
        val checkedChipId = cgBlok.checkedChipId

        // If there's a checked chip, get its tag (blok ID)
        if (checkedChipId != View.NO_ID) {
            val checkedChip = cgBlok.findViewById<Chip>(checkedChipId)
            return checkedChip?.tag as? Int
        }

        return null
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()
        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    // Add this in setupUI() or initializing section
    private fun initializeApp() {
        // In your setupUI or similar initialization method
        if (featureName == AppUtils.ListFeatureNames.DaftarHektarPanen) {
            setupBlokFilter()
//            hektarPanenViewModel.loadHektarPanenData(null, null)
//
//            // Load initial data without blok filter
//            hektarPanenViewModel.loadHektarPanenData(globalFormattedDate)
        }
    }

}
