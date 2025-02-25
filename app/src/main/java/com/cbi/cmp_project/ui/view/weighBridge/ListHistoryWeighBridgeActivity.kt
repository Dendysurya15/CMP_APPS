package com.cbi.cmp_project.ui.view.weighBridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.WeighBridgeRepository
import com.cbi.cmp_project.ui.adapter.UploadItem
import com.cbi.cmp_project.ui.adapter.UploadProgressAdapter
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.adapter.WeighBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var speedDial: SpeedDialView

    private lateinit var tvEmptyState: TextView // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        setContentView(R.layout.activity_list_history_weigh_bridge)
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupSpeedDial()
        setupObserveData()

        weightBridgeViewModel.loadHistoryUploadeSPB()
    }

    private fun initializeViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState)
        speedDial = findViewById(R.id.dial_tph_list_krani_timbang_espb)
    }


    private fun handleUpload(selectedItems: List<Map<String, Any>>) {
        // Map all selected items individually
        val uploadItems = selectedItems.map { item ->
            UploadItem(
                id = item["id"] as Int,
                deptPpro = (item["dept_ppro"] as Number).toInt(),
                divisiPpro = (item["divisi_ppro"] as Number).toInt(),
                commodity = (item["commodity"] as Number).toInt(),
                blokJjg = item["blok_jjg"] as String,
                nopol = item["nopol"] as String,
                driver = item["driver"] as String,
                pemuatId = item["pemuat_id"].toString(),
                transporterId = (item["transporter_id"] as Number).toInt(),
                millId = (item["mill_id"] as Number).toInt(),
                createdById = (item["created_by_id"] as Number).toInt(),
                createdAt = item["created_at"] as String,
                noEspb = item["no_espb"] as String
            )
        }

        // Merge all items into one combined UploadItem
//        val mergedItem = UploadItem(
//            id = -1, // Indicating merged data
//            deptPpro = 0, // Use logic if needed
//            divisiPpro = 0,
//            commodity = 0,
//            blokJjg = uploadItems.joinToString("; ") { it.blokJjg },
//            nopol = uploadItems.joinToString(", ") { it.nopol }.trim(),
//            driver = uploadItems.joinToString(", ") { it.driver }.trim(),
//            pemuatId = uploadItems.joinToString(", ") { it.pemuatId }.trim(),
//            transporterId = 0, // Use logic if necessary
//            millId = 0,
//            createdById = 0,
//            createdAt = uploadItems.maxByOrNull { it.createdAt }?.createdAt ?: "",
//            noEspb = uploadItems.joinToString(" | ") { it.noEspb }
//        )

        // Add merged item to the list
//        val allUploadItems = uploadItems + mergedItem

        val allUploadItems = uploadItems
        // Setup the upload dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)

        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Progress Upload..."
        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
        counterTV.text = "0/${allUploadItems.size}"
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UploadProgressAdapter(allUploadItems)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()
        lifecycleScope.launch {
            loadingDialog.show()
            delay(500) // Delay for 500 milliseconds

            loadingDialog.dismiss()
        }
    }


    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark)
        ) {
            loadingDialog.show()
            loadingDialog.setMessage("Deleting items...")

            weightBridgeViewModel.deleteMultipleItems(selectedItems)

            weightBridgeViewModel.deleteItemsResult.observe(this) { isSuccess ->
                loadingDialog.dismiss()
                if (isSuccess) {
                    Toast.makeText(
                        this,
                        "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reload data based on current state
                    weightBridgeViewModel.loadHistoryUploadeSPB()
                } else {
                    Toast.makeText(
                        this,
                        "${getString(R.string.al_failed_delete)} data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                speedDial.visibility = View.GONE
            }

            weightBridgeViewModel.error.observe(this) { errorMessage ->
                loadingDialog.dismiss()
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpeedDial() {

        speedDial.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.uploadSelected, R.drawable.baseline_file_upload_24)
                    .setLabel(getString(R.string.dial_upload_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListHistoryWeighBridgeActivity,
                            R.color.bluedarklight
                        )
                    )
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.deleteSelected,
                    R.drawable.baseline_delete_forever_24
                )
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListHistoryWeighBridgeActivity,
                            R.color.colorRedDark
                        )
                    )
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.scan_qr -> {
//                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
//
//                        view.background = ContextCompat.getDrawable(this@ListPanenTBSActivity, R.drawable.rounded_top_right_left)
//
//                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
//                        dialog.setContentView(view)
////                        view.layoutParams.height = 500.toPx()
//
//                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
//                        val data = "test"
//                        generateHighQualityQRCode(data, qrCodeImageView)
//                        dialog.setOnShowListener {
//                            val bottomSheet =
//                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
//                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        }
//                        dialog.show()
                        true
                    }

                    R.id.deleteSelected -> {
                        val selectedItems = adapter.getSelectedItemsIdLocal()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = adapter.getSelectedItemsForUpload()

                        if (AppUtils.isNetworkAvailable(this@ListHistoryWeighBridgeActivity)) {

                            handleUpload(selectedItems)
                        } else {
                            AlertDialogUtility.withSingleAction(
                                this@ListHistoryWeighBridgeActivity,
                                getString(R.string.al_back),
                                getString(R.string.al_no_internet_connection),
                                getString(R.string.al_no_internet_connection_description_upload_espb_krani),
                                "network_error.json",
                                R.color.colorRedDark
                            ) {}
                        }
                        true
                    }

                    else -> false
                }
            }
        }


    }

    private fun setupObserveData() {
        weightBridgeViewModel.savedESPBByKrani.observe(this) { data ->

            if (data.isNotEmpty()) {
                speedDial.visibility = View.VISIBLE
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

                                    val deptAbbr = blokData?.firstOrNull()?.dept_abbr
                                        ?: "-"

                                    val deptPPRO = blokData?.firstOrNull()?.dept_ppro
                                        ?: 0

                                    val divisiAbbr = blokData?.firstOrNull()?.divisi_abbr ?: "-"

                                    val divisiPPRO = blokData?.firstOrNull()?.dept_ppro
                                        ?: 0

                                    WBData(
                                        //data untuk upload staging
                                        id = item.id,
                                        dept_ppro = deptPPRO,
                                        divisi_ppro = divisiPPRO,
                                        commodity = 0,
                                        blok_jjg = item.blok_jjg,
                                        nopol = item.nopol,
                                        driver = item.driver,
                                        pemuat_id = item.pemuat_id,
                                        transporter_id = item.transporter_id,
                                        mill_id = item.mill_id,
                                        created_by_id = item.created_by_id,
                                        created_at = item.created_at,
                                        noSPB = item.noESPB.ifEmpty { "-" },
                                        //untuk table
                                        estate = deptAbbr.ifEmpty { "-" },
                                        afdeling = divisiAbbr.ifEmpty { "-" },
                                        datetime = item.created_at.ifEmpty { "-" },
                                        status_cmp = item.status_upload_cmp,
                                        status_ppro = item.status_upload_ppro
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