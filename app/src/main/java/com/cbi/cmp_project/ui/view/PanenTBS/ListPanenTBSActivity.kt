package com.cbi.cmp_project.ui.view.PanenTBS

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.android.material.card.MaterialCardView
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
        setupObservers()
        setupSpeedDial()
        setupCardListeners()
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
            speedDial.visibility = if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
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
            if (currentState == 0) { // Only process if we're in tersimpan state
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
                    }else {
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
                    }else {
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

    private fun setupSpeedDial() {
        speedDial = findViewById(R.id.dial_tph_list)

        speedDial.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.cancelSelection, R.drawable.baseline_check_24)
                    .setLabel(getString(R.string.dial_unselect_item))
                    .setFabBackgroundColor(ContextCompat.getColor(this@ListPanenTBSActivity, R.color.yellowbutton))
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(R.id.deleteSelected, R.drawable.baseline_delete_forever_24)
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(ContextCompat.getColor(this@ListPanenTBSActivity, R.color.colorRedDark))
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.cancelSelection -> {
                        listAdapter.clearSelections()
                        true
                    }
                    R.id.deleteSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()
//                        handleDelete(selectedItems)
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