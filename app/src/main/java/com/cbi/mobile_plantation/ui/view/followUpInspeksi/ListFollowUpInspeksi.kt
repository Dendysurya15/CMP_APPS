package com.cbi.mobile_plantation.ui.view.followUpInspeksi

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.databinding.ActivityFollowUpInspeksiBinding
import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ListFollowUpInspeksi : AppCompatActivity() {
    private var featureName = ""

    private var currentState = 0
    private var parameterInspeksi: List<InspectionViewModel.InspectionParameterItem> = emptyList()

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private var prefManager: PrefManager? = null
    private lateinit var dateButton: Button
    private lateinit var adapter: ListInspectionAdapter
    private lateinit var tableHeader: View
    private lateinit var checkBoxHeader: CheckBox
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabDelListInspect: FloatingActionButton
    private var globalFormattedDate: String = ""
    private lateinit var inspectionViewModel: InspectionViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var filterAllData: CheckBox
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var parentCardStatus: LinearLayout
    private lateinit var horizontalCardFeature: HorizontalScrollView
    private lateinit var listMenuUploadData: LinearLayout

    // "Tersimpan" card properties
    private lateinit var cardItemTersimpan: MaterialCardView
    private lateinit var tvCardTersimpan: TextView
    private lateinit var counterItemTersimpan: TextView

    // "Sudah Scan" card properties
    private lateinit var cardItemTerscan: MaterialCardView
    private lateinit var tvCardTerscan: TextView
    private lateinit var counterItemTerscan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_inspection)

        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)

        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        globalFormattedDate = AppUtils.currentDate

        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        selectedPemanenAdapter = SelectedWorkerAdapter()

        setupHeader()
        initViewModel()
        initializeViews()
        setupRecyclerView()

        setupObservers()

        if (featureName != AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            loadParameterInspeksi()
        } else {
            setupVisibilityStatusHorizontalCard()
            setupCardListeners()
        }

        filterAllData = findViewById(R.id.calendarCheckbox)
        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
            val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
            if (isChecked) {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"
                dateButton.isEnabled = false
                dateButton.alpha = 0.5f

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state - pass NULL for all data
                    panenViewModel.loadDataPanenTransferInspeksi(null, currentState)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
                }
            } else {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                val displayDate = ListPanenTBSActivity().formatGlobalDate(globalFormattedDate)
                dateButton.text = displayDate

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state - pass globalFormattedDate for specific date
                    panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, currentState)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
                }

                nameFilterDate.text = displayDate
                dateButton.isEnabled = true
                dateButton.alpha = 1f // Make the button appear darker
            }

            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)
            removeFilterDate.setOnClickListener {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                if (filterAllData.isChecked) {
                    filterAllData.isChecked = false
                }
                filterDateContainer.visibility = View.GONE
                val todayBackendDate = AppUtils.formatDateForBackend(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR)
                )
                AppUtils.setSelectedDate(todayBackendDate)

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state
                    panenViewModel.loadDataPanenTransferInspeksi(todayBackendDate, currentState)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(todayBackendDate, 1)
                }

                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate
            }
        }

        currentState = 0
        lifecycleScope.launch {
            if (featureName != AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
            } else {
                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, 0)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent =
                    Intent(this@ListFollowUpInspeksi, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        })
    }

    private fun setupVisibilityStatusHorizontalCard() {
        parentCardStatus.visibility = View.VISIBLE
    }

    private fun setupCardListeners() {
        cardItemTersimpan.setOnClickListener {
            loadingDialog.setMessage("Loading data tersimpan", true)
            adapter.updateData(emptyList())
            currentState = 0
            setActiveCard(cardItemTersimpan)
            loadingDialog.show()

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateArchiveState(currentState)

            // Check if filterAllData is checked
            val isAllDataFiltered = filterAllData.isChecked

            if(isAllDataFiltered){
                panenViewModel.loadDataPanenTransferInspeksi(null, currentState) // Pass currentState (0)
            }else{
                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, currentState) // Pass currentState (0)
            }
        }

        cardItemTerscan.setOnClickListener {
            loadingDialog.setMessage("Loading data terscan", true)
            adapter.updateData(emptyList())
            currentState = 1
            setActiveCard(cardItemTerscan)
            loadingDialog.show()

            val isAllDataFiltered = filterAllData.isChecked

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateArchiveState(currentState)

            if(isAllDataFiltered){
                panenViewModel.loadDataPanenTransferInspeksi(null, currentState) // Pass currentState (1)
            }else{
                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, currentState) // Pass currentState (1)
            }
        }
    }

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardItemTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardItemTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
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

    private fun initViewModel() {
        val factory = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]

        val factory2 = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory2)[PanenViewModel::class.java]
    }

    private fun initializeViews() {
        tableHeader = findViewById(R.id.tblHeaderListInspect)
        checkBoxHeader = tableHeader.findViewById(R.id.headerCheckBoxPanen)
        tvEmptyState = findViewById(R.id.tvEmptyDataListInspect)
        recyclerView = findViewById(R.id.rvTableDataListInspect)
        fabDelListInspect = findViewById(R.id.fabDelListInspect)

        parentCardStatus = findViewById(R.id.parentCardStatus)
        horizontalCardFeature = findViewById(R.id.horizontalCardFeature)
        listMenuUploadData = findViewById(R.id.list_menu_upload_data)

        // Initialize "Tersimpan" card elements
        cardItemTersimpan = findViewById(R.id.card_item_tersimpan)
        tvCardTersimpan = findViewById(R.id.tv_card_tersimpan)
        counterItemTersimpan = findViewById(R.id.counter_item_tersimpan)

        // Initialize "Sudah Scan" card elements
        cardItemTerscan = findViewById(R.id.card_item_terscan)
        tvCardTerscan = findViewById(R.id.tv_card_terscan)
        counterItemTerscan = findViewById(R.id.counter_item_terscan)
    }

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")

        // Convert stored date components back to milliseconds
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth - 1, selectedDay) // Month is 0-based in Calendar
        builder.setSelection(calendar.timeInMillis)

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Store the selected date components
            selectedDay = day
            selectedMonth = month
            selectedYear = year

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun loadParameterInspeksi() {
        lifecycleScope.launch {
            try {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)

                val estateIdStr = estateName?.trim()

                if (!estateIdStr.isNullOrEmpty()) {
                    // Direct call without async since we're already in coroutine
                    parameterInspeksi = withContext(Dispatchers.IO) {
                        try {
                            inspectionViewModel.getParameterInspeksiJson()
                        } catch (e: Exception) {
                            AppLogger.e("Parameter loading failed: ${e.message}")
                            emptyList<InspectionViewModel.InspectionParameterItem>()
                        }
                    }

                    if (parameterInspeksi.isEmpty()) {
                        throw Exception("Parameter Inspeksi kosong! Harap Untuk melakukan sinkronisasi Data")
                    }
                }


                loadingDialog.dismiss()

            } catch (e: Exception) {
                val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"
                val fullMessage = errorMessage

                AlertDialogUtility.withSingleAction(
                    this@ListFollowUpInspeksi,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_failed_fetch_data),
                    fullMessage,
                    "warning.json",
                    R.color.colorRedDark
                ) {
                    finish()
                }
            }
        }
    }

    private fun processSelectedDate(selectedDate: String) {
        loadingDialog.show()
        loadingDialog.setMessage("Sedang mengambil data...", true)

        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate

        // Check feature type and load appropriate data
        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            // Load panen data based on current state
            panenViewModel.loadDataPanenTransferInspeksi(selectedDate, currentState)
        } else {
            // Load inspection data
            inspectionViewModel.loadInspectionPaths(selectedDate, 1)
        }

        removeFilterDate.setOnClickListener {
            loadingDialog.show()
            loadingDialog.setMessage("Sedang mengambil data...", true)
            if (filterAllData.isChecked) {
                filterAllData.isChecked = false
            }
            filterDateContainer.visibility = View.GONE
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )
            AppUtils.setSelectedDate(todayBackendDate)

            // Check feature type
            if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                // Load panen data based on current state
                panenViewModel.loadDataPanenTransferInspeksi(todayBackendDate, currentState)
            } else {
                // Load inspection data
                inspectionViewModel.loadInspectionPaths(todayBackendDate, 1)
            }

            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate
        }

        filterDateContainer.visibility = View.VISIBLE
    }


    private fun setupRecyclerView() {
        adapter = if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            ListInspectionAdapter(
                featureName = featureName,
                onInspectionItemClick = { inspectionPath ->
                    // Handle inspection click if needed for TransferInspeksiPanen
                },
                onPanenItemClick = { panenItem ->
                    // Handle panen item click
//                    handlePanenItemClick(panenItem)
                }
            )
        } else {
            ListInspectionAdapter(
                featureName = featureName,
                onInspectionItemClick = { inspectionPath ->
                    showDetailData(inspectionPath)
                }
            )
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListFollowUpInspeksi)
            adapter = this@ListFollowUpInspeksi.adapter
            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
        }


        var headers: MutableList<String> = mutableListOf()
        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            headers = mutableListOf("BLOK-TPH", "TANGGAL", "TIPE/\nANCAK", "KIRIM PABRIK")
        } else {
            headers = mutableListOf("BLOK-TPH", "TGL INSPEKSI", "JUMLAH POKOK TEMUAN", "STATUS")
        }
        updateTableHeaders(headers)
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            panenViewModel.panenTransferInspeksi.observe(this) { panenData ->
                adapter.setPanenData(panenData)
                updateCounters() // Update card counters

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (panenData.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            tvEmptyState.text = "Tidak ada data pencarian"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                }, 500)
            }


        } else {
            // Observer for inspection data (original code)
            inspectionViewModel.inspectionWithDetails.observe(this) { inspectionPaths ->
                adapter.setData(inspectionPaths)

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (inspectionPaths.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            tvEmptyState.text = "Tidak ada data pencarian"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                }, 500)
            }
        }
    }

    private fun updateCounters() {
        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            // Update the counter values based on your panen data
            // You'll need to implement these methods in your ViewModel

//            lifecycleScope.launch {
//                val savedCount = panenViewModel.getSavedPanenCount() // Implement this
//                val scannedCount = panenViewModel.getScannedPanenCount() // Implement this
//
//                counterItemTersimpan.text = savedCount.toString()
//                counterItemTerscan.text = scannedCount.toString()
//            }
        }
    }


    private fun updateTableHeaders(headerNames: List<String>) {
        val checkboxFrameLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        checkboxFrameLayout.visibility = View.GONE
        val headerIds =
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5) // Added th5 for 5 columns

        for (i in headerNames.indices) {
            if (i < headerIds.size) {
                val textView = tableHeader.findViewById<TextView>(headerIds[i])
                textView.apply {
                    visibility = View.VISIBLE
                    text = headerNames[i]
                }
            }
        }

        for (i in headerNames.size until headerIds.size) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.visibility = View.GONE
        }
    }


    @SuppressLint("InflateParams", "SetTextI18n", "MissingInflatedId", "Recycle")
    private fun showDetailData(inspectionPath: InspectionWithDetailRelations) {
        val fullMessage =
            "Anda akan melihat detail inspeksi dari ${inspectionPath.tph!!.blok_kode} - TPH ${inspectionPath.tph.nomor} yang sudah dilakukan pada ${inspectionPath.inspeksi.created_date}"

        // Check status_upload to determine which alert dialog to show
        if (inspectionPath.inspeksi.inspeksi_putaran == 2) {
            // Show single action dialog for CHECK_ONLY mode (already uploaded)
            AlertDialogUtility.withSingleAction(
                this@ListFollowUpInspeksi,
                "Kembali",
                "Detail Hasil Follow-Up",
                "$fullMessage\n\nData ini sudah dipulihkan.",
                "warning.json",
                R.color.greenDarker
            ) {

            }
        } else {
            // Show two actions dialog for normal edit mode (not uploaded yet)
            AlertDialogUtility.withTwoActions(
                this,
                "Telusuri",
                getString(R.string.confirmation_dialog_title),
                fullMessage,
                "warning.json",
                ContextCompat.getColor(this, R.color.bluedarklight),
                function = {
                    val intent = Intent(
                        this@ListFollowUpInspeksi,
                        FormInspectionActivity::class.java
                    )
                    intent.putExtra("FEATURE_NAME", AppUtils.ListFeatureNames.FollowUpInspeksi)
                    intent.putExtra("id_inspeksi", inspectionPath.inspeksi.id)

                    AppLogger.d("Opening normal edit mode for inspection ${inspectionPath.inspeksi.id} with status_upload=${inspectionPath.inspeksi.status_upload}")
                    startActivity(intent)
                },
                cancelFunction = { }
            )
        }
    }


}