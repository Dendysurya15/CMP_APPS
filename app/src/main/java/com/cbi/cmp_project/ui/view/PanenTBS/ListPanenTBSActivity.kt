package com.cbi.cmp_project.ui.view.PanenTBS

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.ui.adapter.ListPanenTPHAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity

import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.ui.viewModel.PanenViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView

class ListPanenTBSActivity : AppCompatActivity() {
    private var featureName: String? = null
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var listAdapter: ListPanenTPHAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var currentState = 0 // 0 for tersimpan, 1 for terscan

    private var isSettingUpCheckbox = false

    // Add views for buttons and counters
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private lateinit var tvEmptyState: TextView // Add this
    private lateinit var recyclerView: RecyclerView // Add this
    private lateinit var speedDial: SpeedDialView

    private var isAscendingOrder = true // Add this property at class level

    private lateinit var searchEditText: EditText  // Add this as class property
    private lateinit var sortButton: ImageView // Add this at class level
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
        setupHeader()
        initViewModel()
        initializeViews()
        loadingDialog = LoadingDialog(this)
        setupRecyclerView()
        setupSearch()
        setupObservers()
        setupSpeedDial()
        setupCardListeners()
        initializeFilterViews()
        setupSortButton()
        setupCheckboxControl()  // Add this
        currentState = 0
        setActiveCard(cardTersimpan)
        panenViewModel.loadActivePanen()
    }

    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data tersimpan...")
            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            listAdapter.updateArchiveState(0)
            speedDial.visibility =
                if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
            panenViewModel.loadActivePanen()
        }

        cardTerscan.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data terscan...")
            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(1)
            panenViewModel.loadArchivedPanen()
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

    private fun setupObservers() {
        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        panenViewModel.activePanenList.observe(this) { panenList ->
            if (currentState == 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    if (panenList.isNotEmpty()) {
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE

                        val mappedData = panenList.map { panenWithRelations ->
                            mapOf<String, Any>(
                                "id" to (panenWithRelations.panen.id as Any),
                                "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                "tph_name" to (panenWithRelations.tphges?.nomor ?: "-") as Any,
                                "dept_name" to (panenWithRelations.department?.abbr ?: "-") as Any,  // Updated to 'department'
                                "divisi_name" to (panenWithRelations.division?.abbr ?: "-") as Any,  // Updated to 'division'
                                "blok_name" to (panenWithRelations.block?.kode ?: "-") as Any,  // Updated to 'block'
                                "date_created" to (panenWithRelations.panen.date_created as Any),
                                "created_by" to (panenWithRelations.panen.created_by as Any),
                                "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                "foto" to (panenWithRelations.panen.foto as Any),
                                "komentar" to (panenWithRelations.panen.komentar as Any),
                                "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                "lat" to (panenWithRelations.panen.lat as Any),
                                "lon" to (panenWithRelations.panen.lon as Any),
                                "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                "ancak" to (panenWithRelations.panen.ancak as Any),
                                "archive" to (panenWithRelations.panen.archive as Any)
                            )
                        }

                        listAdapter.updateData(mappedData)
                        originalData = emptyList() // Reset original data when new data is loaded
                        filterSection.visibility = View.GONE // Hide filter section for new data
                    } else {
                        tvEmptyState.text = "No saved data available"
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    counterTersimpan.text = panenList.size.toString()
                }, 500)
            }
        }


        panenViewModel.archivedPanenList.observe(this) { panenList ->
            if (currentState == 1) { // Only process if we're in terscan state
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    if (panenList.isNotEmpty()) {
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        val mappedData = panenList.map { entity ->
                            mapOf(
                                "id" to entity.id,
                                "tph_id" to entity.tph_id,
                                "date_created" to entity.date_created,
                                "created_by" to entity.created_by,
                                "karyawan_id" to entity.karyawan_id,
                                "jjg_json" to entity.jjg_json,
                                "foto" to entity.foto,
                                "komentar" to entity.komentar,
                                "asistensi" to entity.asistensi,
                                "lat" to entity.lat,
                                "lon" to entity.lon,
                                "jenis_panen" to entity.jenis_panen,
                                "ancak" to entity.ancak,
                                "archive" to entity.archive
                            )
                        }
                        listAdapter.updateData(mappedData)
                        originalData = emptyList() // Reset original data when new data is loaded
                        filterSection.visibility = View.GONE // Hide filter section for new data
                    } else {
                        tvEmptyState.text = "No scanned data available"
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

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

    private fun showEmptyDialog() {
        AlertDialogUtility.withSingleAction(
            this@ListPanenTBSActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            stringXML(R.string.al_failed_fetch_data_desc),
            "warning.json",
            R.color.colorRedDark
        ) {
            finish()
        }
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


    fun generateHighQualityQRCode(content: String, imageView: ImageView, sizePx: Int = 1000) {
        try {
            // Create encoding hints for better quality
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // Highest error correction
                put(EncodeHintType.MARGIN, 2) // Margin size
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.QR_VERSION, 8) // Larger QR version for more data and better quality
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


    private fun setupSpeedDial() {
        speedDial = findViewById(R.id.dial_tph_list)

        speedDial.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.scan_qr, R.drawable.baseline_qr_code_scanner_24)
                    .setLabel(getString(R.string.generate_qr))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListPanenTBSActivity,
                            R.color.yellowbutton
                        )
                    )
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(R.id.uploadSelected, R.drawable.baseline_file_upload_24)
                    .setLabel(getString(R.string.dial_upload_item))
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
                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)

                        view.background = ContextCompat.getDrawable(this@ListPanenTBSActivity, R.drawable.rounded_top_right_left)

                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
                        dialog.setContentView(view)
//                        view.layoutParams.height = 500.toPx()

                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                        val data = "test"
                        generateHighQualityQRCode(data, qrCodeImageView)
                        dialog.setOnShowListener {
                            val bottomSheet =
                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                        dialog.show()
                        true
                    }
//                    R.id.cancelSelection -> {
//                        listAdapter.clearSelections()
//                        true
//                    }
//                    R.id.deleteSelected -> {
//                        val selectedItems = listAdapter.getSelectedItems()
////                        handleDelete(selectedItems)
//                        true
//                    }
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
            updateFilterDisplay()
        }

        setupRemoveFilter()
    }


    private fun updateFilterDisplay() {
        filterSection.visibility = View.VISIBLE
        filterName.text = if (isAscendingOrder) "Urutan Nomor TPH Kecil - Besar" else "Urutan Nomor TPH Besar - Kecil"
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()
//        AlertDialogUtility.withTwoActions(
//            this,
//            "Simpan",
//            getString(R.string.confirmation_dialog_title),
//            getString(R.string.al_confirm_feature),
//            "warning.json"
//        ) {
        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
//        }

    }

    private fun updateSortIcon() {
        sortButton.animate()
            .scaleY(if (isAscendingOrder) 1f else -1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }

    private fun setupRecyclerView() {
        listAdapter = ListPanenTPHAdapter()
        recyclerView.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
        }
    }
}