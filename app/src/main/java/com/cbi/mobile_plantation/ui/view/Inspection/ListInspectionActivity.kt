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
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ListInspectionActivity : AppCompatActivity() {
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
        loadParameterInspeksi()

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

                setupObservers() // Move this here after parameter is loaded
                loadingDialog.dismiss()

            } catch (e: Exception) {
                val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"
                val fullMessage = errorMessage

                AlertDialogUtility.withSingleAction(
                    this@ListInspectionActivity,
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


        AppLogger.d(selectedDate)
        inspectionViewModel.loadInspectionPaths(selectedDate)

        removeFilterDate.setOnClickListener {

        }

        filterDateContainer.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = ListInspectionAdapter(
            "",
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

        val btnStartPasarTengah = view.findViewById<TextView>(R.id.btnStartPasarTengah)

        btnStartPasarTengah.setOnClickListener{
            AlertDialogUtility.withTwoActions(
                this,
                "Lanjutkan",
                getString(R.string.confirmation_dialog_title),
                "Inspeksi akan dilanjutkan dari Pasar Tengah dengan Nomor TPH ini. Anda masih dapat melakukan perubahan nomor TPH jika diperlukan.",
                "warning.json",
                ContextCompat.getColor(this, R.color.bluedarklight),
                function = {
                    val intent = Intent(
                        this@ListInspectionActivity,
                        FormInspectionActivity::class.java
                    )
                    intent.putExtra("FEATURE_NAME", AppUtils.ListFeatureNames.InspeksiPanen)
                    intent.putExtra("IS_FROM_PASAR_TENGAH",true)
                    intent.putExtra("DIVISI_ABBR",inspectionPath.tph.divisi_abbr)
                    intent.putExtra("DEPT_ABBR",inspectionPath.tph.dept_abbr)
                    intent.putExtra("BLOK_KODE",inspectionPath.tph.blok_kode)
                    intent.putExtra("LAST_NUMBER_POKOK",inspectionPath.inspeksi.jml_pkk_diperiksa)
                    intent.putExtra("id_inspeksi", inspectionPath.inspeksi.id)
                    startActivity(intent)
                },
                cancelFunction = { }
            )
        }

        // Populate TPH Section
        populateTPHData(tphContainer, inspectionPath.inspeksi, inspectionPath.tph, inspectionPath.panen, view, inspectionPath.detailInspeksi)

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



    private fun populateTPHData(container: LinearLayout, inspection: InspectionModel, tph: TPHNewModel, panen: PanenEntity?, parentView: View, detailInspeksi: List<InspectionDetailModel>) {
        parentView.findViewById<TextView>(R.id.tvEstAfdBlok)?.text = "${tph.dept_abbr} ${tph.divisi_abbr!!.takeLast(2)} ${tph.blok_kode}"
        val jamMulaiSelesai = formatDateRange(inspection.created_date_start, inspection.created_date_end)
        parentView.findViewById<TextView>(R.id.tvJamMulaiSelesai)?.text = jamMulaiSelesai
        parentView.findViewById<TextView>(R.id.tvJalurMasuk)?.text = inspection.jalur_masuk
        val barisText = formatBarisText(inspection.jenis_kondisi, inspection.baris)
        parentView.findViewById<TextView>(R.id.tvBaris)?.text = barisText
        val formattedDateText = try {
            val datesJson = JSONArray(inspection.date_panen)
            val datesList = mutableListOf<String>()

            for (i in 0 until datesJson.length()) {
                val dateStr = datesJson.getString(i)
                val formattedDate = formatToIndonesianDate(dateStr)
                datesList.add(formattedDate)
            }

            when {
                datesList.isEmpty() -> "Tidak ada data tanggal"
                datesList.size == 1 -> datesList.first()
                else -> {
                    val joinedDates = datesList.joinToString("\n")
                    "Total ${datesList.size} Transaksi :\n$joinedDates"
                }
            }
        } catch (e: Exception) {
            // Fallback: treat as single date string (for backward compatibility)
            AppLogger.w("Failed to parse date_panen as JSON, treating as single date: ${e.message}")
            formatToIndonesianDate(inspection.date_panen)
        }

        parentView.findViewById<TextView>(R.id.tvTglPanen)?.text = formattedDateText

        val tphKomentar = detailInspeksi
            .filter { it.no_pokok == 0 }
            .firstOrNull()
            ?.komentar

        val fotoTPH = detailInspeksi
            .filter { it.no_pokok == 0 }
            .firstOrNull()
            ?.foto

        parentView.findViewById<TextView>(R.id.tvKomentarTPH)?.text = tphKomentar ?: ""
        val frameLayoutFoto = parentView.findViewById<FrameLayout>(R.id.frameLayoutFoto)
        val imageView = parentView.findViewById<ImageView>(R.id.ivFoto)

        loadInspectionPhoto(frameLayoutFoto, imageView,fotoTPH)

        val rvSelectedPemanen = parentView.findViewById<RecyclerView>(R.id.rvSelectedPemanenInspection)
        val pemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = pemanenAdapter
        rvSelectedPemanen.layoutManager = FlexboxLayoutManager(parentView.context).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Set display mode to show names without remove buttons
        pemanenAdapter.setDisplayOnly(true)

        val uniqueWorkers = detailInspeksi
            .filter { it.no_pokok != 0 && it.nik.isNotEmpty() && it.nama.isNotEmpty() } // Exclude TPH-level records (no_pokok = 0)
            .map { "${it.nik} - ${it.nama}" } // Format as "NIK - NAMA"
            .distinct() // Remove duplicates
            .toSet()

        uniqueWorkers.forEach { worker ->
            AppLogger.d("- Worker: $worker")
        }

        if (uniqueWorkers.isNotEmpty()) {
            rvSelectedPemanen.visibility = View.VISIBLE

            // Add workers to adapter
            uniqueWorkers.forEach { formattedWorker ->
                // Extract NIK from the formatted string for the worker ID
                val nik = formattedWorker.split(" - ").firstOrNull() ?: ""
                val worker = Worker(nik, formattedWorker)
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
            // No inspection details with worker data
            rvSelectedPemanen.visibility = View.GONE
        }

        container.removeAllViews()
        val buahTinggalTPHParam = parameterInspeksi.firstOrNull {
            it.nama == AppUtils.kodeInspeksi.buahTinggalTPH
        }
        val brondolanTinggalTPHParam = parameterInspeksi.firstOrNull {
            it.nama == AppUtils.kodeInspeksi.brondolanTinggalTPH
        }

        val buahTinggalTPH = buahTinggalTPHParam?.let { param ->
            detailInspeksi.firstOrNull { it.kode_inspeksi == param.id }?.temuan_inspeksi?.toInt() ?: 0
        } ?: 0

        val brondolanTinggalTPH = brondolanTinggalTPHParam?.let { param ->
            detailInspeksi.firstOrNull { it.kode_inspeksi == param.id }?.temuan_inspeksi?.toInt() ?: 0
        } ?: 0

        val tphData = listOf(
            SummaryItem("Jumlah Pokok Inspeksi", inspection.jml_pkk_inspeksi.toString()),
            SummaryItem(AppUtils.kodeInspeksi.buahTinggalTPH, buahTinggalTPH.toString()),
            SummaryItem(AppUtils.kodeInspeksi.brondolanTinggalTPH, brondolanTinggalTPH.toString()),
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

    data class MergedInspectionDetail(
        val no_pokok: Int,
        val workers: List<String>, // List of worker names
        val pokok_panen: Int?,
        val foto: String?,
        val komentar: String?,
        val temuanByKode: Map<Int, Double>, // Combined totals from all workers
        val latIssue: Double,
        val lonIssue: Double
    )

    private fun mergeInspectionDetails(detailList: List<InspectionDetailModel>): List<MergedInspectionDetail> {
        return detailList
            .groupBy { it.no_pokok } // Group by pokok only, not by worker
            .map { (pokok, details) ->
                val firstDetail = details.first()

                // Get unique workers for this pokok
                val workers = details.map { "${it.nama} (${it.nik})" }.distinct()

                // Sum up all temuan values by kode_inspeksi from all workers
                val temuanByKode = details
                    .groupBy { it.kode_inspeksi }
                    .mapValues { (_, codeDetails) ->
                        codeDetails.sumOf { it.temuan_inspeksi }
                    }

                // Combine comments from all workers (if any)
                val allComments = details.mapNotNull { it.komentar }.filter { it.isNotEmpty() }.distinct()
                val combinedComment = if (allComments.isNotEmpty()) allComments.joinToString(" | ") else null

                MergedInspectionDetail(
                    no_pokok = firstDetail.no_pokok,
                    workers = workers,
                    pokok_panen = firstDetail.pokok_panen,
                    foto = firstDetail.foto,
                    komentar = combinedComment,
                    temuanByKode = temuanByKode,
                    latIssue = firstDetail.latIssue,
                    lonIssue = firstDetail.lonIssue
                )
            }
            .sortedBy { it.no_pokok }
    }

    // Updated populateIssueData function
    private fun populateIssueData(container: LinearLayout, detailInspeksi: List<InspectionDetailModel>, dialogView: View) {

        container.removeAllViews()

        val filteredDetailInspeksi = detailInspeksi.filter { it.no_pokok != 0 }

        val filteredParameters = parameterInspeksi.filter { param ->
            param.nama != AppUtils.kodeInspeksi.buahTinggalTPH &&
                    param.nama != AppUtils.kodeInspeksi.brondolanTinggalTPH
        }.sortedBy { it.id }

        // Merge the filtered details
        val mergedDetails = mergeInspectionDetails(filteredDetailInspeksi)

        // Update title with count
        val titleIssue = dialogView.findViewById<TextView>(R.id.titleIssue)
        titleIssue.text = "Temuan (${mergedDetails.size} Pokok)"

        if (mergedDetails.isEmpty()) {
            val noDataText = TextView(this).apply {
                text = "Tidak ada temuan pada inspeksi ini"
                setPadding(32, 32, 32, 32)
                setTextColor(Color.GRAY)
                textSize = resources.getDimension(R.dimen.m) / resources.displayMetrics.scaledDensity // Convert to sp
                gravity = Gravity.CENTER
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
            }
            container.addView(noDataText)
            return
        }

        // Constants
        val headerHeight = 210
        val rowHeight = 120
        val frozenWidth = 200
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
                headerHeight
            )
        }

        // Frozen header cell (Pokok + Worker info)
        val frozenHeaderCell = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(frozenWidth, headerHeight).apply {
                setMargins(0, 0, 4, 0)
            }
            setBackgroundColor(ContextCompat.getColor(this@ListInspectionActivity, R.color.greenDarker))
        }

        val frozenHeaderText = TextView(this).apply {
            text = "No.\nPokok"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = resources.getDimension(R.dimen.m) / resources.displayMetrics.scaledDensity // Convert to sp
            typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_bold)
            setPadding(8, 8, 8, 8)
        }
        frozenHeaderCell.addView(frozenHeaderText)

        // Scrollable header container
        val scrollableHeaderContainer = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, headerHeight, 1f)
            isHorizontalScrollBarEnabled = false
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }

        val scrollableHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                headerHeight
            )
        }

        val columnHeaders = mutableListOf<String>()

        filteredParameters.forEach { param ->
            val shortName = when (param.id) {
                1 -> AppUtils.kodeInspeksi.brondolanDigawangan
                2 -> AppUtils.kodeInspeksi.brondolanTidakDikutip
                3 -> AppUtils.kodeInspeksi.buahMasakTidakDipotong
                4 -> AppUtils.kodeInspeksi.buahTertinggalPiringan
                7 -> AppUtils.kodeInspeksi.susunanPelepahTidakSesuai
                8 -> AppUtils.kodeInspeksi.terdapatPelepahSengkleh
                9 -> AppUtils.kodeInspeksi.overPruning
                10 -> AppUtils.kodeInspeksi.underPruning
                else -> param.nama.take(20)
            }
            columnHeaders.add(shortName)
        }

        // Add standard columns
        columnHeaders.add("Pokok\nPanen")
        columnHeaders.add("Foto")

        // Add scrollable header cells
        columnHeaders.forEachIndexed { index, headerText ->
            val headerCell = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(columnWidth, headerHeight).apply {
                    if (index < columnHeaders.size - 1) {
                        setMargins(0, 0, 4, 0)
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
                textSize = resources.getDimension(R.dimen.s) / resources.displayMetrics.scaledDensity // Convert to sp
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_bold)
                setPadding(4, 4, 4, 4)
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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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

        // Add data rows for merged details
        mergedDetails.forEachIndexed { index, mergedDetail ->
            val hasComment = !mergedDetail.komentar.isNullOrEmpty()
            val frozenCellBackgroundColor = if (hasComment) commentBackgroundColor else Color.WHITE

            var totalFrozenHeight = rowHeight
            if (hasComment) {
                val tempTextView = TextView(this).apply {
                    text = mergedDetail.komentar
                    textSize = resources.getDimension(R.dimen.s) / resources.displayMetrics.scaledDensity // Convert to sp
                    typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                }
                val textPaint = tempTextView.paint
                val textWidth = columnWidth * columnHeaders.size - 24
                val staticLayout = android.text.StaticLayout.Builder.obtain(
                    mergedDetail.komentar ?: "",
                    0,
                    mergedDetail.komentar?.length ?: 0,
                    textPaint,
                    textWidth
                ).build()
                val commentHeight = maxOf(staticLayout.height + 32, 60)
                totalFrozenHeight += commentHeight
            }

            // Frozen cell - Pokok + Worker info
            val frozenDataCell = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(frozenWidth, totalFrozenHeight).apply {
                    setMargins(0, 0, 4, 0)
                    if (index > 0) setMargins(0, 8, 4, 0)
                }
                setBackgroundColor(frozenCellBackgroundColor)
            }

            val frozenDataText = TextView(this).apply {
                text = "${mergedDetail.no_pokok}"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                textSize = resources.getDimension(R.dimen.s) / resources.displayMetrics.scaledDensity // Convert to sp
                typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                setPadding(4, 4, 4, 4)
            }
            frozenDataCell.addView(frozenDataText)
            frozenDataContainer.addView(frozenDataCell)

            // Scrollable row
            val scrollableDataRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    rowHeight
                ).apply {
                    if (index > 0) setMargins(0, 8, 0, 0)
                }
            }

            // Build row data - inspection codes 1-9 + pokok_panen + foto
            val rowData = mutableListOf<String>()

            filteredParameters.forEach { param ->
                val value = mergedDetail.temuanByKode[param.id] ?: 0.0
                rowData.add(if (value > 0) value.toString() else "0")
            }

            rowData.add(getPokokPanenText(mergedDetail.pokok_panen))
            rowData.add("FOTO")

            // Add scrollable cells
            rowData.forEachIndexed { cellIndex, cellText ->
                val dataCell = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(columnWidth, rowHeight).apply {
                        if (cellIndex < rowData.size - 1) {
                            setMargins(0, 0, 4, 0)
                        }
                    }
                    setBackgroundColor(Color.WHITE)
                }

                // Special handling for the last column (Photo)
                if (cellIndex == rowData.size - 1) {
                    createPhotoCell(dataCell, mergedDetail.foto, "Pokok ${mergedDetail.no_pokok}")
                } else {
                    val dataCellText = TextView(this).apply {
                        text = cellText
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        gravity = Gravity.CENTER
                        setTextColor(Color.BLACK)
                        textSize = 12f
                        typeface = ResourcesCompat.getFont(this@ListInspectionActivity, R.font.manrope_medium)
                        setPadding(4, 4, 4, 4)
                    }
                    dataCell.addView(dataCellText)
                }

                scrollableDataRow.addView(dataCell)
            }

            scrollableDataContent.addView(scrollableDataRow)

            // Add comment row if exists
            if (!mergedDetail.komentar.isNullOrEmpty()) {
                val commentText = TextView(this).apply {
                    text = "Komentar: ${mergedDetail.komentar}"
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

                val textPaint = commentText.paint
                val textWidth = columnWidth * columnHeaders.size - 24
                val staticLayout = android.text.StaticLayout.Builder.obtain(
                    mergedDetail.komentar ?: "",
                    0,
                    mergedDetail.komentar?.length ?: 0,
                    textPaint,
                    textWidth
                ).build()

                val dynamicHeight = staticLayout.height + 32
                val finalHeight = maxOf(dynamicHeight, 60)

                val commentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        finalHeight
                    )
                    setPadding(8, 4, 8, 4)
                }

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

        mainContainer.addView(headerContainer)
        mainContainer.addView(dataContainer)
        container.addView(mainContainer)
    }

    private fun createPhotoCell(dataCell: FrameLayout, photoFileName: String?, noPokokText: String) {
        if (photoFileName.isNullOrEmpty()) {
            // No photo available - show placeholder
            val noPhotoText = TextView(this).apply {
                text = "Tidak ada foto"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                textSize = 13f
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
            val errorText = TextView(this).apply {
                text = "Foto tidak ditemukan"
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
                text = "Tidak ada Foto"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                textSize = 13f
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

    private fun formatBarisText(jenisKondisi: Int, baris: String): String {
        return when (jenisKondisi) {
            1 -> {
                // Datar - should have two values separated by comma
                val barisList = baris.split(",").map { it.trim() }
                if (barisList.size >= 2) {
                    "Baris: ${barisList[0]}, ${barisList[1]}"
                } else {
                    "Baris: $baris"
                }
            }
            2 -> {
                // Teras - should have one value
                val baris1 = baris.split(",").first().trim()
                "Baris: $baris1"
            }
            else -> "Baris: $baris"
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



}