package com.cbi.mobile_plantation.ui.view.Inspection

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
//import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity.SummaryItem
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.hideWithAnimation
import com.cbi.mobile_plantation.utils.AppUtils.showWithAnimation
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ListInspectionActivity : AppCompatActivity() {
    private var featureName = ""

    private var currentState = 0

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
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerupload: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerupload: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabDelListInspect: FloatingActionButton
    private var globalFormattedDate: String = ""
    private lateinit var inspectionViewModel: InspectionViewModel
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private val selectedPathIds = mutableListOf<String>()
    private lateinit var filterAllData: CheckBox
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

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
                inspectionViewModel.loadInspectionPaths()
            } else {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                val displayDate = ListPanenTBSActivity().formatGlobalDate(globalFormattedDate)
                dateButton.text = displayDate
                inspectionViewModel.loadInspectionPaths(globalFormattedDate)
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
                inspectionViewModel.loadInspectionPaths(todayBackendDate)
                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate
            }
        }



        currentState = 0
        lifecycleScope.launch {
            inspectionViewModel.loadInspectionPaths(globalFormattedDate)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent =
                    Intent(this@ListInspectionActivity, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        })
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
    }

    private fun initializeViews() {
        tableHeader = findViewById(R.id.tblHeaderListInspect)
        checkBoxHeader = tableHeader.findViewById(R.id.headerCheckBoxPanen)
        tvEmptyState = findViewById(R.id.tvEmptyDataListInspect)
        recyclerView = findViewById(R.id.rvTableDataListInspect)
        fabDelListInspect = findViewById(R.id.fabDelListInspect)
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

    private fun processSelectedDate(selectedDate: String) {
        loadingDialog.show()
        loadingDialog.setMessage("Sedang mengambil data...", true)


        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate


        AppLogger.d(selectedDate)
        inspectionViewModel.loadInspectionPaths(selectedDate)

        removeFilterDate.setOnClickListener {

        }

        filterDateContainer.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = ListInspectionAdapter(
            onItemClick = { inspectionPath ->
                showDetailData(inspectionPath)
            },
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListInspectionActivity)
            adapter = this@ListInspectionActivity.adapter
            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
        }

        val headers = listOf("BLOK", "TOTAL PKK", "JAM MULAI/\nJAM SELESAI", "STATUS\nUPLOAD")
        updateTableHeaders(headers)
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

    private fun formatStartDate(startDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) // Indonesian format

            val parsed = inputFormat.parse(startDate)
            outputFormat.format(parsed)

        } catch (e: Exception) {
            AppLogger.e("Error formatting start date: ${e.message}")
            // Fallback: just take first 10 characters (date part)
            startDate.take(10)
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n", "MissingInflatedId", "Recycle")
    private fun showDetailData(inspectionPath: InspectionWithDetailRelations) {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_history_inspeksi, null)
        view.background = ContextCompat.getDrawable(
            this@ListInspectionActivity,
            R.drawable.rounded_top_right_left
        )

        val dialog = BottomSheetDialog(this@ListInspectionActivity)
        dialog.setContentView(view)
        val titleDialogDetailTable = view.findViewById<TextView>(R.id.titleDialogDetailTable)
        titleDialogDetailTable.text = "Inspeksi ${formatStartDate(inspectionPath.inspeksi.created_date_start)} ${inspectionPath.tph!!.blok_kode}-${inspectionPath.tph.nomor}"
        val tphContainer = view.findViewById<LinearLayout>(R.id.tblLytTPH)
        val issueContainer = view.findViewById<LinearLayout>(R.id.tblLytIssue)

        // Populate TPH Section
        populateTPHData(tphContainer, inspectionPath.inspeksi, inspectionPath.tph, inspectionPath.panen,  view)


        val detailList = inspectionPath.detailInspeksi
        populateIssueData(issueContainer, detailList, view)

        // Handle close button
        val btnClose = view.findViewById<Button>(R.id.btnCloseDetailTable)
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)

            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val desiredHeight = (screenHeight * 0.85).toInt()

            bottomSheet?.layoutParams?.height = desiredHeight
            bottomSheet?.requestLayout()

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            behavior.peekHeight = desiredHeight
        }

        dialog.show()
    }

    private fun formatDateRange(startDate: String, endDate: String): String {
        return try {
            // Parse the input dates (assuming they're in ISO format like "2024-01-15 10:30:00")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")) // Indonesian locale

            val startParsed = inputFormat.parse(startDate)
            val endParsed = inputFormat.parse(endDate)

            val formattedStart = outputFormat.format(startParsed)
            val formattedEnd = outputFormat.format(endParsed)

            "$formattedStart s.d\n$formattedEnd"

        } catch (e: Exception) {
            AppLogger.e("Error formatting date range: ${e.message}")
            // Fallback: just show the raw dates
            "$startDate\ns.d\n$endDate"
        }
    }

    private fun formatBarisText(jenisKondisi: Int, baris1: Int, baris2: Int?): String {
        return when (jenisKondisi) {
            1 -> {
                // Jenis kondisi 1: Show baris range
                if (baris2 != null) {
                    "Baris $baris1 - $baris2"
                } else {
                    "Baris $baris1" // Fallback if baris2 is null
                }
            }
            2 -> {
                // Jenis kondisi 2: Show terasan
                "Terasan (Baris $baris1)"
            }
            else -> {
                // Unknown jenis kondisi - fallback
                "Baris $baris1"
            }
        }
    }

    private fun populateTPHData(container: LinearLayout, inspection: InspectionModel, tph: TPHNewModel, panen: PanenEntity?, parentView: View) {
        parentView.findViewById<TextView>(R.id.tvEstAfdBlok)?.text = "${tph.dept_abbr} ${tph.divisi_abbr!!.takeLast(2)} ${tph.blok_kode}"
        val jamMulaiSelesai = formatDateRange(inspection.created_date_start, inspection.created_date_end)
        parentView.findViewById<TextView>(R.id.tvJamMulaiSelesai)?.text = jamMulaiSelesai
        parentView.findViewById<TextView>(R.id.tvJalurMasuk)?.text = inspection.jalur_masuk
        val barisText = formatBarisText(inspection.jenis_kondisi, inspection.baris1, inspection.baris2)
        parentView.findViewById<TextView>(R.id.tvBaris)?.text = barisText
        parentView.findViewById<TextView>(R.id.tvTglPanen)?.text = formatToIndonesianDate(inspection.date_panen)

        parentView.findViewById<TextView>(R.id.tvKomentarTPH)?.text = inspection.komentar.toString()
        val frameLayoutFoto = parentView.findViewById<FrameLayout>(R.id.frameLayoutFoto)
        val imageView = parentView.findViewById<ImageView>(R.id.ivFoto)

        loadInspectionPhoto(frameLayoutFoto, imageView, inspection.foto)

        // Setup RecyclerView for pemanen
        val rvSelectedPemanen = parentView.findViewById<RecyclerView>(R.id.rvSelectedPemanenInspection)
        val pemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = pemanenAdapter
        rvSelectedPemanen.layoutManager = FlexboxLayoutManager(parentView.context).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Set display mode to show names without remove buttons
        pemanenAdapter.setDisplayOnly(true)

        AppLogger.d("panen $panen")
        // Check if panen exists and has worker data
        if (panen != null && (panen.karyawan_nik.isNotEmpty() || panen.karyawan_nama.isNotEmpty())) {
            rvSelectedPemanen.visibility = View.VISIBLE

            AppLogger.d("panen.karyawan_nik ${panen.karyawan_nik}")
            AppLogger.d("panen.karyawan_nama ${panen.karyawan_nama}")

            val allWorkers = mutableSetOf<Pair<String, String>>()

            val niks = panen.karyawan_nik.split(",").map { it.trim() }
            val names = panen.karyawan_nama.split(",").map { it.trim() }

            // Match each NIK with corresponding name by index position
            val maxIndex = minOf(niks.size, names.size)
            for (i in 0 until maxIndex) {
                val nik = niks[i]
                val name = names[i]
                if (nik.isNotEmpty() && name.isNotEmpty()) {
                    allWorkers.add(Pair(nik, name))
                }
            }

            // Add workers to adapter
            allWorkers.forEach { (nik, name) ->
                val formattedName = "$nik - $name"
                val worker = Worker(nik, formattedName)
                pemanenAdapter.addWorker(worker)
            }

            rvSelectedPemanen.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rvSelectedPemanen.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Style all visible items to be more compact
                    for (i in 0 until rvSelectedPemanen.childCount) {
                        val childView = rvSelectedPemanen.getChildAt(i)

                        // Make text smaller
                        val textView = childView.findViewById<TextView>(R.id.worker_name)
                        textView?.textSize = 12f

                        // Reduce container padding
                        val container = childView.findViewById<LinearLayout>(R.id.worker_container)
                        val density = parentView.context.resources.displayMetrics.density
                        container?.setPadding(
                            (8 * density).toInt(), // 8dp to pixels
                            (4 * density).toInt(), // 4dp to pixels
                            (8 * density).toInt(), // 8dp to pixels
                            (4 * density).toInt()  // 4dp to pixels
                        )
                    }
                }
            })
        } else {
            // No panen data or no worker data
            rvSelectedPemanen.visibility = View.GONE
        }

        container.removeAllViews()

        val tphData = listOf(
            SummaryItem("Jumlah Pokok Inspeksi", inspection.jml_pkk_inspeksi.toString()),
            SummaryItem("Buah Tinggal", inspection.buah_tinggal.toString()),
            SummaryItem("Brondolan Tinggal", inspection.brd_tinggal.toString()),
        )

        for (item in tphData) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(createRowTextView(item.title, Gravity.START, 2f, true))
                addView(createRowTextView(item.value, Gravity.CENTER, 1f, false))
            }
            container.addView(rowLayout)
        }
    }

    private fun populateIssueData(container: LinearLayout, detailInspeksi: List<InspectionDetailModel>, dialogView: View) {

        container.removeAllViews()

        // Update title with count
        val titleIssue = dialogView.findViewById<TextView>(R.id.titleIssue)
        titleIssue.text = "Temuan (${detailInspeksi.size} Pokok)"

        if (detailInspeksi.isEmpty()) {
            val noDataText = TextView(this).apply {
                text = "Tidak ada temuan pada inspeksi ini"
                setPadding(32, 32, 32, 32)
                setTextColor(Color.GRAY)
                textSize = 16f
                gravity = Gravity.CENTER
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            container.addView(noDataText)
            return
        }

        // Constants
        val headerHeight = 150 // EXACT height - no wrapping allowed
        val rowHeight = 120     // EXACT height - no wrapping allowed
        val frozenWidth = 150
        val columnWidth = 250

        // Define comment background color
        val commentBackgroundColor = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(this@ListInspectionActivity, R.color.graydarker),
            (0.1 * 255).toInt()
        )

        // Main container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // ==================== HEADER SECTION ====================
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                headerHeight // EXACT HEIGHT
            )
        }

        // Frozen header cell
        val frozenHeaderCell = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(frozenWidth, headerHeight).apply {
                setMargins(0, 0, 4, 0) // Right margin for separation
            }
            setBackgroundColor(ContextCompat.getColor(this@ListInspectionActivity, R.color.greenDarker))
        }

        val frozenHeaderText = TextView(this).apply {
            text = "Nomor\nPokok"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_bold)
            setPadding(8, 8, 8, 8)
        }
        frozenHeaderCell.addView(frozenHeaderText)

        // Scrollable header container
        val scrollableHeaderContainer = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                headerHeight, // EXACT HEIGHT
                1f
            )
            isHorizontalScrollBarEnabled = false
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }

        val scrollableHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                headerHeight // EXACT HEIGHTP
            )
        }

        val columnHeaders = listOf(
            "Prioritas", "Pokok di\nPanen", "Susunan\nPelepah",
            "Pelepah\nSengkleh", "Kondisi\nPruning",
            "Buah Masak\nTinggal","Buah M1", "Buah M2", "Brondolan Kutip",
            "Foto"
        )

        // Add scrollable header cells
        columnHeaders.forEachIndexed { index, headerText ->
            val headerCell = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(columnWidth, headerHeight).apply {
                    if (index < columnHeaders.size - 1) {
                        setMargins(0, 0, 4, 0) // Right margin except for last column
                    }
                }
                setBackgroundColor(ContextCompat.getColor(this@ListInspectionActivity, R.color.greenDarker))
            }

            val headerTextView = TextView(this).apply {
                text = headerText
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_bold)
                setPadding(8, 8, 8, 8)
            }

            headerCell.addView(headerTextView)
            scrollableHeaderRow.addView(headerCell)
        }

        scrollableHeaderContainer.addView(scrollableHeaderRow)
        headerContainer.addView(frozenHeaderCell)
        headerContainer.addView(scrollableHeaderContainer)

        // ==================== DATA SECTION ====================
        val dataContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Frozen column container
        val frozenDataContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(frozenWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Scrollable data container
        val scrollableDataContainer = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            isHorizontalScrollBarEnabled = false
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }

        val scrollableDataContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Sync scrolling between header and data
        scrollableHeaderContainer.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollableDataContainer.scrollTo(scrollX, scrollableDataContainer.scrollY)
        }

        scrollableDataContainer.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollableHeaderContainer.scrollTo(scrollX, scrollableHeaderContainer.scrollY)
        }

        // Add data rows
        detailInspeksi.forEachIndexed { index, detail ->
            // Check if this detail has a comment to determine background color and height
            val hasComment = !detail.komentar.isNullOrEmpty()
            val frozenCellBackgroundColor = commentBackgroundColor

            // Calculate total height for frozen cell (including comment if exists)
            var totalFrozenHeight = rowHeight
            if (hasComment) {
                // Calculate comment height
                val tempTextView = TextView(this).apply {
                    text = detail.komentar
                    textSize = 13f
                    typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                }
                val textPaint = tempTextView.paint
                val textWidth = columnWidth * columnHeaders.size - 24
                val staticLayout = android.text.StaticLayout.Builder.obtain(
                    detail.komentar ?: "",
                    0,
                    detail.komentar?.length ?: 0,
                    textPaint,
                    textWidth
                ).build()
                val commentHeight = maxOf(staticLayout.height + 32, 60)
                totalFrozenHeight += commentHeight
            }

            // Frozen cell - SPANS BOTH DATA AND COMMENT ROWS
            val frozenDataCell = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(frozenWidth, totalFrozenHeight).apply {
                    setMargins(0, 0, 4, 0) // Right margin for separation
                    if (index > 0) setMargins(0, 8, 4, 0) // Add top margin between rows
                }
                setBackgroundColor(frozenCellBackgroundColor) // Dynamic background color
            }

            val frozenDataText = TextView(this).apply {
                text = detail.no_pokok.toString()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER // This centers both horizontally and vertically across the entire height
                setTextColor(Color.BLACK)
                textSize = 12f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                setPadding(8, 8, 8, 8)
                setSingleLine(true) // FORCE SINGLE LINE
            }
            frozenDataCell.addView(frozenDataText)
            frozenDataContainer.addView(frozenDataCell)

            // Scrollable row
            val scrollableDataRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    rowHeight // EXACT HEIGHT
                ).apply {
                    if (index > 0) setMargins(0, 8, 0, 0) // Add top margin between rows
                }
            }

            val rowData = listOf(
                getPrioritasText(detail.prioritas),
                getPokokPanenText(detail.pokok_panen),
                getSusunanPelepahText(detail.susunan_pelepah),
                getPelepahSengklehText(detail.pelepah_sengkleh),
                getKondisiPruningText(detail.kondisi_pruning),
                detail.ripe?.toString() ?: "0",
                detail.buahm1?.toString() ?: "0",
                detail.buahm2?.toString() ?: "0",
                detail.brd_tidak_dikutip?.toString() ?: "0",
                "FOTO"
            )

            // Add scrollable cells
            rowData.forEachIndexed { cellIndex, cellText ->
                val dataCell = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(columnWidth, rowHeight).apply {
                        if (cellIndex < rowData.size - 1) {
                            setMargins(0, 0, 4, 0) // Right margin except for last column
                        }
                    }
                    setBackgroundColor(Color.WHITE) // Scrollable cells remain white
                }

                // Special handling for the last column (Photo)
                if (cellIndex == rowData.size - 1) {
                    // Create photo preview cell
                    createPhotoCell(dataCell, detail.foto, detail.no_pokok.toString())
                } else {
                    // Regular text cell
                    val dataCellText = TextView(this).apply {
                        text = cellText
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        gravity = Gravity.CENTER
                        setTextColor(Color.BLACK)
                        textSize = 13f
                        typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                        setPadding(8, 8, 8, 8)
                    }
                    dataCell.addView(dataCellText)
                }

                scrollableDataRow.addView(dataCell)
            }

            scrollableDataContent.addView(scrollableDataRow)

            // Add comment row if exists (NO SPACER NEEDED - frozen cell already spans both rows)
            if (!detail.komentar.isNullOrEmpty()) {
                // Create the comment text
                val commentText = TextView(this).apply {
                    text = "Komentar temuan: ${detail.komentar}"
                    setPadding(12, 8, 12, 8)
                    setTextColor(Color.DKGRAY)
                    textSize = 13f
                    typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(commentBackgroundColor)
                        cornerRadius = 6f
                    }
                }

                // Measure the text height dynamically
                val textPaint = commentText.paint
                val textWidth = columnWidth * columnHeaders.size - 24 // Subtract padding
                val staticLayout = android.text.StaticLayout.Builder.obtain(
                    detail.komentar ?: "",
                    0,
                    detail.komentar?.length ?: 0,
                    textPaint,
                    textWidth
                ).build()

                // Calculate dynamic height (text height + padding + margin)
                val dynamicHeight = staticLayout.height + 32 // 16 top + 16 bottom padding
                val finalHeight = maxOf(dynamicHeight, 60) // Minimum height of 60

                // Add comment to scrollable area with dynamic height
                val commentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        finalHeight // Use dynamic height
                    )
                    setPadding(8, 4, 8, 4)
                }

                // Update the comment text layout params
                commentText.layoutParams = LinearLayout.LayoutParams(
                    columnWidth * columnHeaders.size,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )

                commentRow.addView(commentText)
                scrollableDataContent.addView(commentRow)
            }
        }

        scrollableDataContainer.addView(scrollableDataContent)
        dataContainer.addView(frozenDataContainer)
        dataContainer.addView(scrollableDataContainer)

        // Add everything to main container
        mainContainer.addView(headerContainer)
        mainContainer.addView(dataContainer)
        container.addView(mainContainer)
    }

    private fun createPhotoCell(dataCell: FrameLayout, photoFileName: String?, noPokokText: String) {
        if (photoFileName.isNullOrEmpty()) {
            // No photo available - show placeholder
            val noPhotoText = TextView(this).apply {
                text = "No Photo"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                textSize = 10f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            dataCell.addView(noPhotoText)
            return
        }

        // Get the photo path
        val rootApp = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-${AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok}"
        ).toString()

        val fullImagePath = File(rootApp, photoFileName).absolutePath
        val file = File(fullImagePath)

        if (!file.exists()) {
            // Photo file doesn't exist - show error
            val errorText = TextView(this).apply {
                text = "Photo\nNot Found"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.RED)
                textSize = 9f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            dataCell.addView(errorText)
            return
        }

        // Create image view for photo preview
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(8, 8, 8, 8) // Add some padding around the image
            }
            scaleType = ImageView.ScaleType.CENTER_CROP

            // Add ripple effect for click feedback
            val rippleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(ColorUtils.setAlphaComponent(Color.GRAY, (0.3 * 255).toInt()))
                cornerRadius = 8f
            }
            foreground = rippleDrawable
            isClickable = true
            isFocusable = true
        }

        // Load the image
        try {
            val bitmap = BitmapFactory.decodeFile(fullImagePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                // Failed to decode - show placeholder
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            AppLogger.e("Error loading preview image: ${e.message}")
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Add click listener for full screen view
        imageView.setOnClickListener {
            showFullScreenPhoto(
                fullImagePath,
                "Temuan Pokok $noPokokText"
            )
        }

        dataCell.addView(imageView)
    }

    private fun loadInspectionPhoto(frameContainer: FrameLayout, imageView: ImageView?, photoFileName: String?) {
        // Clear any existing views in frame container
        frameContainer.removeAllViews()

        if (photoFileName.isNullOrEmpty()) {
            // No photo available - show placeholder
            val noPhotoText = TextView(this).apply {
                text = "No Photo"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                textSize = 10f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            frameContainer.addView(noPhotoText)
            return
        }

        // Get the photo path
        val rootApp = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-${AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH}"
        ).toString()

        val fullImagePath = File(rootApp, photoFileName).absolutePath
        val file = File(fullImagePath)

        if (!file.exists()) {
            // Photo file doesn't exist - show error
            val errorText = TextView(this).apply {
                text = "Photo\nNot Found"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.RED)
                textSize = 9f
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            frameContainer.addView(errorText)
            return
        }

        // Create image view for photo preview
        val photoImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(4, 4, 4, 4) // Add some padding around the image
            }
            scaleType = ImageView.ScaleType.CENTER_CROP

            // Add ripple effect for click feedback
            val rippleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(ColorUtils.setAlphaComponent(Color.GRAY, (0.3 * 255).toInt()))
                cornerRadius = 8f
            }
            foreground = rippleDrawable
            isClickable = true
            isFocusable = true
        }

        // Load the image
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Reduce memory usage for preview
                inJustDecodeBounds = false
            }
            val bitmap = BitmapFactory.decodeFile(fullImagePath, options)
            if (bitmap != null) {
                photoImageView.setImageBitmap(bitmap)
            } else {
                // Failed to decode - show placeholder
                photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            AppLogger.e("Error loading preview image: ${e.message}")
            photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Add click listener for full screen view
        photoImageView.setOnClickListener {
            showFullScreenPhoto(
                fullImagePath,
                "Foto Inspeksi TPH"
            )
        }

        frameContainer.addView(photoImageView)
    }

    private fun showFullScreenPhoto(
        imagePath: String,
        title: String,
        bottomSheetDialog: BottomSheetDialog? = null
    ) {
        AppLogger.d("showFullScreenPhoto called with: $imagePath, $title")

        // Create full screen dialog
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // Inflate your existing camera_edit layout
        val view = LayoutInflater.from(this).inflate(R.layout.camera_edit, null)
        dialog.setContentView(view)

        // Find components in the inflated layout
        val fotoZoom = view.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.fotoZoom)
        val cardCloseZoom = view.findViewById<MaterialCardView>(R.id.cardCloseZoom)
        val cardChangePhoto = view.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val cardDeletePhoto = view.findViewById<MaterialCardView>(R.id.cardDeletePhoto)
        val clZoomLayout = view.findViewById<ConstraintLayout>(R.id.clZoomLayout)

        AppLogger.d("Dialog components found: fotoZoom=${fotoZoom != null}, cardCloseZoom=${cardCloseZoom != null}")

        // Make sure the zoom layout is visible
        clZoomLayout?.visibility = View.VISIBLE

        // Load image into PhotoView
        val file = File(imagePath)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    fotoZoom?.setImageBitmap(bitmap)
                    AppLogger.d("Full screen photo loaded successfully in dialog: $title")
                } else {
                    AppLogger.e("Failed to decode bitmap for full screen")
                    return
                }
            } catch (e: Exception) {
                AppLogger.e("Error loading full screen photo: ${e.message}")
                return
            }
        } else {
            AppLogger.e("Image file not found for full screen: $imagePath")
            return
        }

        // Hide change and delete buttons for view-only mode
        cardChangePhoto?.visibility = View.GONE
        cardDeletePhoto?.visibility = View.GONE

        // Function to close full screen and show bottom sheet again
        fun closeFullScreenAndShowBottomSheet() {
            AppLogger.d("Closing full screen and showing bottom sheet again")
            dialog.dismiss()
            bottomSheetDialog?.show() // Show the bottom sheet again if provided
        }

        // Set up close button
        cardCloseZoom?.setOnClickListener {
            AppLogger.d("Close button clicked")
            closeFullScreenAndShowBottomSheet()
        }

        // Optional: Close on photo tap
        fotoZoom?.setOnClickListener {
            AppLogger.d("Photo tapped - closing dialog")
            closeFullScreenAndShowBottomSheet()
        }

        // Show dialog
        try {
            dialog.show()
            AppLogger.d("Full screen dialog shown successfully")
        } catch (e: Exception) {
            AppLogger.e("Error showing full screen dialog: ${e.message}")
        }
    }

    // Keep your existing helper functions
    private fun getSusunanPelepahText(susunan: Int?): String {
        return when (susunan) {
            1 -> "Baik"
            2 -> "Sedang"
            3 -> "Buruk"
            else -> "-"
        }
    }

    private fun getPelepahSengklehText(sengkleh: Int?): String {
        return when (sengkleh) {
            1 -> "Ada"
            0 -> "Tidak Ada"
            else -> "-"
        }
    }

    private fun getKondisiPruningText(pruning: Int?): String {
        return when (pruning) {
            1 -> "Baik"
            2 -> "Sedang"
            3 -> "Buruk"
            else -> "-"
        }
    }


    private fun populateGPSData(container: LinearLayout, inspection: InspectionModel) {
        container.removeAllViews()

        val gpsData = listOf(
            SummaryItem("Latitude TPH", String.format("%.6f", inspection.latTPH)),
            SummaryItem("Longitude TPH", String.format("%.6f", inspection.lonTPH)),
            SummaryItem("Koordinat TPH", "${String.format("%.6f", inspection.latTPH)}, ${String.format("%.6f", inspection.lonTPH)}"),
            SummaryItem("Tracking Path", if (inspection.tracking_path.isNotEmpty()) "✓ Tersedia" else "✗ Tidak ada")
        )

        for (item in gpsData) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(createRowTextView(item.title, Gravity.START, 2f, true))
                addView(createRowTextView(item.value, Gravity.CENTER, 1f, false))
            }
            container.addView(rowLayout)
        }
    }

    // Keep your existing helper functions
    private fun getJenisKondisiText(kondisi: Int): String {
        return when (kondisi) {
            1 -> "Normal"
            2 -> "Terasan"
            else -> "Tidak diketahui"
        }
    }

    private fun getPrioritasText(prioritas: Int?): String {
        return when (prioritas) {
            1 -> "Tinggi"
            2 -> "Sedang"
            3 -> "Rendah"
            else -> "-"
        }
    }

    private fun getPokokPanenText(pokokPanen: Int?): String {
        return when (pokokPanen) {
            1 -> "Ya"
            2 -> "Tidak"
            else -> "-"
        }
    }

    fun createRowTextView(
        text: String,
        gravity: Int,
        weight: Float,
        isTitle: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(32, 32, 32, 32)
            setTextColor(Color.BLACK)
            textSize = 15f
            this.gravity = gravity or Gravity.CENTER_VERTICAL
            typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_semibold)
            maxLines = Int.MAX_VALUE
            ellipsize = null
            isSingleLine = false

            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
                .apply {
                    setMargins(5, 5, 5, 5)
                }

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(
                    ColorUtils.setAlphaComponent(
                        ContextCompat.getColor(this@ListInspectionActivity, R.color.graydarker),
                        (0.2 * 255).toInt()
                    )
                )
                cornerRadii = if (isTitle) floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 20f, 20f)
                else floatArrayOf(0f, 0f, 20f, 20f, 20f, 20f, 0f, 0f)
            }
        }
    }

    // Keep your existing createRowTextView, createHeaderTextView, createDataTextView functions
    data class SummaryItem(val title: String, val value: String)

    private fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
    }

    private fun resetSelectedIds() {
        val flCheckBox = tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBox.visibility = if (currentState == 0) View.VISIBLE else View.GONE

        checkBoxHeader.setOnCheckedChangeListener(null)
        checkBoxHeader.isChecked = false
        selectedPathIds.clear()
//        adapter.toggleSelectAll(false)
//        adapter.updateCurrentState(currentState)
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        inspectionViewModel.inspectionWithDetails.observe(this) { inspectionPaths ->
            adapter.setData(inspectionPaths)
            Handler(Looper.getMainLooper()).postDelayed({
                loadingDialog.dismiss()

                lifecycleScope.launch {
                    if (inspectionPaths.isNotEmpty()) {
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    } else {
                        tvEmptyState.text = "No saved data available"
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                }
            }, 500)
        }
    }

//    private fun setupViewListeners() {
//        cardTersimpan.setOnClickListener {
//            if (currentState == 0) return@setOnClickListener
//
//            currentState = 0
//            resetSelectedIds()
//            setActiveCard(cardTersimpan)
//            loadingDialog.show()
//            loadingDialog.setMessage("Loading data tersimpan...")
////            inspectionViewModel.loadInspectionPaths(currentState)
//        }
//
//        cardTerupload.setOnClickListener {
//            if (currentState == 1) return@setOnClickListener
//
//            currentState = 1
//            resetSelectedIds()
//            setActiveCard(cardTerupload)
//            loadingDialog.show()
//            loadingDialog.setMessage("Loading data terupload...")
////            inspectionViewModel.loadInspectionPaths(currentState)
//        }
//
//        fabDelListInspect.setOnClickListener {
//            AlertDialogUtility.withTwoActions(
//                this,
//                getString(R.string.al_delete),
//                getString(R.string.confirmation_dialog_title),
//                "Apakah anda yakin ingin menghapus data terpilih?",
//                "warning.json",
//                function = {
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        try {
//                            loadingDialog.show()
//                            loadingDialog.setMessage("Menghapus data...")
//
////                            val deleteResult =
////                                inspectionViewModel.deleteInspectionDatas(selectedPathIds)
////
////                            if (deleteResult.isSuccess) {
////                                resetSelectedIds()
////
////                                withContext(Dispatchers.IO) {
////                                    inspectionViewModel.loadInspectionPaths(currentState)
////                                }
////                            } else {
////                                throw Exception("Error delete view model")
////                            }
//                        } catch (e: Exception) {
//                            AppLogger.d("Unexpected error: ${e.message}")
//                            AlertDialogUtility.withSingleAction(
//                                this@ListInspectionActivity,
//                                stringXML(R.string.al_back),
//                                stringXML(R.string.al_failed_delete),
//                                "Failed to delete data : ${e.message}",
//                                "warning.json",
//                                R.color.colorRedDark
//                            ) {}
//                        } finally {
//                            if (loadingDialog.isShowing) {
//                                loadingDialog.dismiss()
//                            }
//                        }
//                    }
//                }
//            )
//        }
//    }

    private fun setActiveCard(activeCard: MaterialCardView) {
//        cardTersimpan.apply {
//            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
//            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
//        }
//
//        cardTerupload.apply {
//            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
//            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
//        }

        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }
}