package com.cbi.mobile_plantation.ui.view.Inspection

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
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
import java.util.Calendar

class ListInspectionActivity : AppCompatActivity() {
    private var featureName = ""

    private var currentState = 0

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

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
//                showDetailData(inspectionPath)
            },
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListInspectionActivity)
            adapter = this@ListInspectionActivity.adapter
            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
        }

        val headers = listOf("BLOK", "TOTAL PKK", "JAM MULAI/\nJAM SELESAI", "STATUS")
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
//    @SuppressLint("InflateParams", "SetTextI18n", "MissingInflatedId", "Recycle")
//    private fun showDetailData(inspectionPath: PathWithInspectionTphRelations) {
//        fun createTextView(
//            text: String,
//            gravity: Int,
//            weight: Float,
//            isTitle: Boolean = false,
//            isSemiColon: Boolean = false
//        ): TextView {
//            val textView = TextView(this)
//            textView.text = text
//
//            val paddingNumber = if (isSemiColon) 0 else 32
//            textView.setPadding(paddingNumber, paddingNumber, paddingNumber, paddingNumber)
//
//            textView.setTextColor(Color.BLACK)
//            textView.setResponsiveTextSizeWithConstraints(17F, 17F, 19F)
//            textView.gravity = gravity
//            textView.setTypeface(null, if (isTitle) Typeface.NORMAL else Typeface.BOLD)
//
//            val marginNumber = if (isSemiColon) 0 else 5
//            val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
//            params.setMargins(marginNumber, marginNumber, marginNumber, marginNumber)
//            textView.layoutParams = params
//
//            return textView
//        }
//
//        val inspectionWithTph = inspectionPath.inspections.firstOrNull()
//        val totalPokok = inspectionPath.inspections.size
//
//        var estNama = ""
//        var afdNama = ""
//        var blokNama = ""
//        var jalurMasuk = ""
//        var createdDate = ""
//        if (inspectionWithTph != null) {
//            estNama = inspectionWithTph.tph.dept_abbr ?: ""
//            afdNama = inspectionWithTph.tph.divisi_abbr ?: "-"
//            blokNama = inspectionWithTph.tph.blok_kode ?: "-"
//            jalurMasuk = inspectionWithTph.inspection.jalur_masuk
//            createdDate = formatToIndonesianDate(inspectionWithTph.inspection.created_date)
//        }
//
//        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_generate_qr_panen, null)
//        view.background = ContextCompat.getDrawable(
//            this@ListInspectionActivity,
//            R.drawable.rounded_top_right_left
//        )
//
//        val dialog = BottomSheetDialog(this@ListInspectionActivity)
//        dialog.setContentView(view)
//
//        val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
//        val tvTitleQRGenerate: TextView = view.findViewById(R.id.textTitleQRGenerate)
//        tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
//        tvTitleQRGenerate.text = "Detail Data Inspeksi"
//
//        val dashedLine: View = view.findViewById(R.id.dashedLine)
//        val loadingContainer: LinearLayout = view.findViewById(R.id.loadingDotsContainerBottomSheet)
//        val tableLayoutData: TableLayout = view.findViewById(R.id.tblLytTextView)
//        tableLayoutData.removeAllViews()
//
//        loadingLogo.visibility = View.VISIBLE
//        loadingContainer.visibility = View.VISIBLE
//
//        // Load and start bounce animation
//        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
//        loadingLogo.startAnimation(bounceAnimation)
//
//        // Setup dots animation
//        val dots = listOf(
//            loadingContainer.findViewById<View>(R.id.dot1),
//            loadingContainer.findViewById<View>(R.id.dot2),
//            loadingContainer.findViewById<View>(R.id.dot3),
//            loadingContainer.findViewById<View>(R.id.dot4)
//        )
//
//        dots.forEachIndexed { index, dot ->
//            val translateAnimation = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
//            val scaleXAnimation = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
//            val scaleYAnimation = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)
//
//            listOf(translateAnimation, scaleXAnimation, scaleYAnimation).forEach { animation ->
//                animation.duration = 500
//                animation.repeatCount = ObjectAnimator.INFINITE
//                animation.repeatMode = ObjectAnimator.REVERSE
//                animation.startDelay = (index * 100).toLong()
//                animation.start()
//            }
//        }
//
//        dialog.setOnShowListener {
//            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//            val behavior = BottomSheetBehavior.from(bottomSheet!!)
//            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        }
//
//        dialog.show()
//
//        lifecycleScope.launch {
//            try {
//                // Delay for loading effect
//                delay(1000)
//
//                // Prepare data for the table
//                val data = listOf(
//                    SummaryItem("Estate", estNama),
//                    SummaryItem("Afdeling", afdNama),
//                    SummaryItem("Blok", blokNama),
//                    SummaryItem("Jalur Masuk", jalurMasuk),
//                    SummaryItem("Total Pokok", totalPokok.toString()),
//                    SummaryItem("Tanggal", createdDate)
//                )
//
//                for (item in data) {
//                    val tableRow = TableRow(this@ListInspectionActivity)
//
//                    val titleTextView = createTextView(item.title, Gravity.START, 1f, true)
//                    tableRow.addView(titleTextView)
//
//                    val semicolonTextView = createTextView(":", Gravity.START, 0.02f, isSemiColon = true)
//                    tableRow.addView(semicolonTextView)
//
//                    val valueTextView = createTextView(item.value, Gravity.START, 2f)
//                    tableRow.addView(valueTextView)
//
//                    tableLayoutData.addView(tableRow)
//                }
//
//                // Switch to the main thread for UI updates
//                withContext(Dispatchers.Main) {
//                    try {
//                        // Create animations for transitions
//                        val fadeOut = ObjectAnimator.ofFloat(loadingLogo, "alpha", 1f, 0f).apply {
//                            duration = 250
//                        }
//                        val fadeOutDots = ObjectAnimator.ofFloat(loadingContainer, "alpha", 1f, 0f).apply {
//                            duration = 250
//                        }
//
//                        // Ensure QR code and other elements start invisible
//                        dashedLine.alpha = 0f
//                        tvTitleQRGenerate.alpha = 0f
//                        tableLayoutData.alpha = 0f
//
//                        // Create fade-in animations
//                        val fadeInDashedLine = ObjectAnimator.ofFloat(dashedLine, "alpha", 0f, 1f).apply {
//                            duration = 250
//                            startDelay = 150
//                        }
//                        val fadeInTitle = ObjectAnimator.ofFloat(tvTitleQRGenerate, "alpha", 0f, 1f).apply {
//                            duration = 250
//                            startDelay = 150
//                        }
//                        val fadeInTableLayout = ObjectAnimator.ofFloat(tableLayoutData, "alpha", 0f, 1f).apply {
//                            duration = 250
//                            startDelay = 150
//                        }
//
//                        // Run animations sequentially
//                        AnimatorSet().apply {
//                            playTogether(fadeOut, fadeOutDots)
//                            addListener(object : AnimatorListenerAdapter() {
//                                override fun onAnimationEnd(animation: Animator) {
//                                    // Hide loading elements
//                                    loadingLogo.visibility = View.GONE
//                                    loadingContainer.visibility = View.GONE
//
//                                    // Show elements
//                                    tvTitleQRGenerate.visibility = View.VISIBLE
//                                    dashedLine.visibility = View.VISIBLE
//                                    tableLayoutData.visibility = View.VISIBLE
//
//                                    // Start fade-in animations
//                                    fadeInDashedLine.start()
//                                    fadeInTitle.start()
//                                    fadeInTableLayout.start()
//                                }
//                            })
//                            start()
//                        }
//                    } catch (e: Exception) {
//                        // Handle UI-related errors on the main thread
//                        loadingLogo.animation?.cancel()
//                        loadingLogo.clearAnimation()
//                        loadingLogo.visibility = View.GONE
//                        loadingContainer.visibility = View.GONE
//                        AppLogger.e("QR Generation UI Error: ${e.message}")
//                    }
//                }
//            } catch (e: Exception) {
//                // Handle any other errors
//                withContext(Dispatchers.Main) {
//                    AppLogger.e("Error in QR process: ${e.message}")
//                }
//            } finally {
//                stopLoadingAnimation(loadingLogo, loadingContainer)
//            }
//        }
//    }

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
            AppLogger.d(inspectionPaths.toString())
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