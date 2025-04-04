package com.cbi.mobile_plantation.ui.view.espb

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.ui.adapter.ESPBAdapter
import com.cbi.mobile_plantation.ui.adapter.ESPBData
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


@Suppress("UNREACHABLE_CODE")
class ListHistoryESPBActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var adapter: ESPBAdapter
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
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()

        findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        setupFilterAllData()

        espbViewModel.loadHistoryESPBNonScan(AppUtils.currentDate)
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

        espbViewModel.loadHistoryESPBNonScan(selectedDate)

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
            espbViewModel.loadHistoryESPBNonScan(todayBackendDate)
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

    private fun setupObserveData() {
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

                                    // Process blok_jjg data to get janjang sum
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

                                    // Get blok data from repository
                                    val blokData = try {
                                        withContext(Dispatchers.IO) {
                                            espbViewModel.getBlokById(idBlokList)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                                        emptyList()
                                    }

                                    // Extract blok_kode values for display
                                    val blokDisplay = if (blokData.isNotEmpty()) {
                                        blokData.mapNotNull { it.blok_kode }.distinct()
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

                        adapter.updateList(filteredData)
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
        val headers = listOf("WAKTU", "BLOK", "JANJANG", "TPH", "MAIC", "SCAN")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ESPBAdapter(emptyList(), this@ListHistoryESPBActivity)
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)
        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5, R.id.th6)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }

        val th5 = tableHeader.findViewById<TextView>(R.id.th5)
        val th6 = tableHeader.findViewById<TextView>(R.id.th6)
        val layoutParamsTh5 = th5.layoutParams as LinearLayout.LayoutParams
        layoutParamsTh5.weight = 0.3f
        th5.layoutParams = layoutParamsTh5
        val layoutParamsTh6 = th6.layoutParams as LinearLayout.LayoutParams
        layoutParamsTh6.weight = 0.3f
        th6.layoutParams = layoutParamsTh6
        val flCheckBoxTableHeaderLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
    }

    private fun initViewModel() {
        val appRepository = AppRepository(application)
        val factory = ESPBViewModel.ESPBViewModelFactory(appRepository)
        espbViewModel = ViewModelProvider(this, factory)[ESPBViewModel::class.java]
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
                espbViewModel.loadHistoryESPBNonScan(null)  // Pass null to load all data
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
                espbViewModel.loadHistoryESPBNonScan(globalFormattedDate)
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
                espbViewModel.loadHistoryESPBNonScan(todayBackendDate)
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
}
