package com.cbi.cmp_project.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.adapter.WeighBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNREACHABLE_CODE")
class ListHistoryWeighBridgeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var weightBridgeViewModel: WeighBridgeViewModel
    private lateinit var adapter: WeighBridgeAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    private var mappedData: List<Map<String, Any>> = emptyList()

    private lateinit var tvEmptyState: TextView // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()

        weightBridgeViewModel.loadHistoryUploadeSPB()
    }

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }


    private fun setupObserveData() {
        weightBridgeViewModel.uploadedESPB.observe(this) { data ->

            if (data.isNotEmpty()) {
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Launch coroutine in lifecycleScope
                lifecycleScope.launch {
                    try {
                        val filteredData = coroutineScope {
                            data.map { item ->
                                async {
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

                                    val blokData = try {
                                        withContext(Dispatchers.IO) { // Database operation on IO thread
                                            weightBridgeViewModel.getBlokById(idBlokList)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                                        null
                                    }

                                    val groupedDeptDivisi = blokData?.groupBy { it.dept_abbr }
                                        ?.mapNotNull { (dept, blokList) ->
                                            dept?.let {
                                                val divisiList =
                                                    blokList.mapNotNull { it.divisi_abbr }
                                                        .distinct()
                                                if (divisiList.isNotEmpty()) {
                                                    "$dept ${divisiList.joinToString(" ")}"
                                                } else {
                                                    dept // If no divisions, show only the department
                                                }
                                            }
                                        } ?: listOf("-") // Default when blokData is null

                                    // Check if only one distinct department exists
                                    val distinctDeptAbbr =
                                        groupedDeptDivisi.map { it.split(" ").first() }
                                            .distinct()
                                            .joinToString("\n")

                                    val distinctDeptCount =
                                        groupedDeptDivisi.map { it.split(" ").first() }
                                            .distinct().size

                                    val distinctDivisiAbbr = if (distinctDeptCount == 1) {
                                        // Only one department → Show only divisions
                                        groupedDeptDivisi.flatMap {
                                            it.split(" ").drop(1)
                                        } // Drop department name
                                            .distinct()
                                            .joinToString(" ")
                                    } else {
                                        // Multiple departments → Show dept with divisions
                                        groupedDeptDivisi.joinToString("\n")
                                    }

                                    // Even if blokData fails, use non-null fields
                                    WBData(
                                        noSPB = item.noESPB.ifEmpty { "-" },
                                        estate = distinctDeptAbbr.ifEmpty { "-" },
                                        afdeling = distinctDivisiAbbr.ifEmpty { "-" },
                                        datetime = item.created_at.ifEmpty { "-" },
                                        status_cmp = item.status_upload_cmp,
                                        status_ppro = item.status_upload_ppro
                                            ?: 0 // Defaults to 0 if null
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
                tvEmptyState.text = "No Uploaded e-SPB data available"
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }

        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("e-SPB", "ESTATE", "AFDELING", "TGL PROSES", "STATUS UPLOAD")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WeighBridgeAdapter(emptyList())
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
    }


    private fun initViewModel() {
        val factory = WeighBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeighBridgeViewModel::class.java]
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
        featureName = intent.getStringExtra("FEATURE_NAME")
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()

        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()


    }
}