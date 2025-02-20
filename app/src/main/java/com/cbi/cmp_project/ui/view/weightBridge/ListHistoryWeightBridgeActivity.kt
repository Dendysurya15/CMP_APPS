package com.cbi.cmp_project.ui.view.weightBridge

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
import com.cbi.cmp_project.ui.adapter.WeightBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.view.ListTPHApproval
import com.cbi.cmp_project.ui.viewModel.WeightBridgeViewModel
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ListHistoryWeightBridgeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var weightBridgeViewModel: WeightBridgeViewModel
    private lateinit var adapter: WeightBridgeAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_list_history_weight_bridge)
        setupHeader()
        initViewModel()
        setupRecyclerView()
        processQRResult()
    }

    private fun setupRecyclerView() {
        val headers = listOf("NO ESPB", "ESTATE", "AFDELING", "TGL PROSES", "STATUS UPLOAD")
        updateTableHeaders(headers)


        recyclerView = findViewById(R.id.wbTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WeightBridgeAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.wbTableHeader)

        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.text = headerNames[i]
        }
    }



    private fun initViewModel() {
        val factory = WeightBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeightBridgeViewModel::class.java]
    }

    private fun processQRResult() {
        val qrResult = intent.getStringExtra(ListTPHApproval.EXTRA_QR_RESULT).orEmpty()
        AppLogger.d(qrResult)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)

                    val parsedData = parseJson(jsonStr!!)

                    withContext(Dispatchers.Main) {
                        adapter.updateList(parsedData)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result $e")

            }
        }
    }

    fun parseJson(jsonStr: String): List<WBData> {
        val dataList = mutableListOf<WBData>()

        try {
            val jsonObject = JSONObject(jsonStr)
            val espb = jsonObject.getJSONObject("espb")

            val noSPB = espb.getString("no_espb")
            val createdAt = espb.getString("created_at")

            // Add to the list
            dataList.add(WBData(noSPB, 0, "", createdAt))  // estate & afdeling are empty for now
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return dataList
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