package com.cbi.mobile_plantation.ui.view.Inspection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ListInspectionActivity : AppCompatActivity() {
    private var featureName = ""

    private var currentState = 0

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    private var prefManager: PrefManager? = null

    private lateinit var listAdapter: ListInspectionAdapter
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerupload: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerupload: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var inspectionViewModel: InspectionViewModel

    private var mappedData: List<Map<String, Any>> = emptyList()

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

        setupHeader()
        initViewModel()
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupCardListeners()

        currentState = 0
        setActiveCard(cardTersimpan)

        lifecycleScope.launch {
            inspectionViewModel.loadSavedInspection()
            inspectionViewModel.loadInspectionCountUploaded()
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

    private fun initViewModel() {
        val factory = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.mcvSavedListInspect)
        cardTerupload = findViewById(R.id.mcvUploadListInspect)
        counterTersimpan = findViewById(R.id.tvTotalSavedListInspect)
        counterTerupload = findViewById(R.id.tvTotalUploadListInspect)
        tvEmptyState = findViewById(R.id.tvEmptyDataListInspect)
        recyclerView = findViewById(R.id.rvTableDataListInspect)
    }

    private fun setupRecyclerView() {
        val headers = listOf("BLOK", "TOTAL PKK", "JAM")
        updateTableHeaders(headers)

        listAdapter = ListInspectionAdapter()
        recyclerView.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@ListInspectionActivity)
        }
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tblHeaderListInspect)
        val checkBoxHeader = tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        checkBoxHeader.visibility = View.GONE

        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3)
        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE
                text = headerNames[i]
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        inspectionViewModel.uploadedCount.observe(this) { count ->
            counterTerupload.text = count.toString()
        }

        inspectionViewModel.savedInspectionList.observe(this) { list ->
            if (currentState == 0) {
                listAdapter.updateData(emptyList())
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (list.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            mappedData = list.map { item ->
                                mapOf(
                                    "path_id" to item.path.id,
                                    "tph_id" to (item.inspections.firstOrNull()?.tph_id ?: "-").toString(),
                                    "created_date" to (item.inspections.firstOrNull()?.created_date ?: "-").toString()
                                )
                            }
                            listAdapter.updateData(mappedData)
                        } else {
                            tvEmptyState.text = "No saved data available"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                    counterTersimpan.text = list.size.toString()
                }, 500)
            }
        }
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
//            listAdapter.updateArchiveState(0)
//            panenViewModel.loadActivePanen()
        }

        cardTerupload.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerupload)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data terupload...")
            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
//            listAdapter.updateArchiveState(1)
//            panenViewModel.loadArchivedPanen()
        }
    }

    private fun setActiveCard(activeCard: MaterialCardView) {
        cardTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardTerupload.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }
}