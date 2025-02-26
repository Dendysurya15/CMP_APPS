package com.cbi.cmp_project.ui.view.espb

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
import com.cbi.cmp_project.data.repository.AppRepository
import com.cbi.cmp_project.ui.adapter.ESPBAdapter
import com.cbi.cmp_project.ui.adapter.ESPBData
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.viewModel.ESPBViewModel
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

        espbViewModel.loadHistoryESPBNonScan()
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
                                            espbViewModel.getBlokById(idBlokList)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Blok Data: ${e.message}")
                                        null
                                    }

                                    // Even if blokData fails, use non-null fields
                                    ESPBData(
                                        time = item.created_at.ifEmpty { "-" },
                                        blok = blokJjgList.joinToString(", ") {
                                            val blokName = blokData?.find { data -> data.id == it.first }?.blok_kode ?: "Blok ${it.first}"
                                            blokName
                                        },
                                        janjang = blokJjgList.sumOf { it.second ?: 0 }.toString(),
                                        tphCount = item.tph1.split(";").filter { it.isNotEmpty() }.size.toString(),
                                        status_mekanisasi = item.status_mekanisasi,
                                        status_scan = item.status_draft // Defaults to 0 if null
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
        val headers = listOf("WAKTU", "BLOK", "JANJANG", "TPH", "STATUS ESPB")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ESPBAdapter(emptyList())
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
        val appRepository = AppRepository(application)
        val factory = ESPBViewModel.ESPBViewModelFactory(appRepository)
        espbViewModel = ViewModelProvider(this, factory)[ESPBViewModel::class.java]
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
