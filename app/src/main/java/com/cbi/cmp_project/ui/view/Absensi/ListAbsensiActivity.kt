package com.cbi.cmp_project.ui.view.Absensi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.adapter.AbsensiAdapter
import com.cbi.cmp_project.ui.adapter.AbsensiDataRekap
import com.cbi.cmp_project.ui.adapter.ListAbsensiAdapter
import com.cbi.cmp_project.ui.adapter.UploadItem
import com.cbi.cmp_project.ui.adapter.UploadProgressAdapter
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.adapter.WeighBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.viewModel.AbsensiViewModel
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.button.MaterialButton
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.ifEmpty

@Suppress("UNREACHABLE_CODE")
class ListAbsensiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var absensiViewModel: AbsensiViewModel
    private lateinit var absensiAdapter: ListAbsensiAdapter
    private var prefManager: PrefManager? = null
    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private var infoApp: String = ""

    private var mappedData: List<Map<String, Any>> = emptyList()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var speedDial: SpeedDialView
    private lateinit var tvEmptyStateAbsensi: TextView // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_absensi)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)

        infoApp = AppUtils.getDeviceInfo(this@ListAbsensiActivity).toString()
        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()
        absensiViewModel.getAllDataAbsensi()
    }

    private fun initializeViews() {
        tvEmptyStateAbsensi = findViewById(R.id.tvEmptyStateAbsensiList)
        speedDial = findViewById(R.id.dial_listAbsensi)
    }

//    private fun handleUpload(selectedItems: List<Map<String, Any>>) {
//
//        val uploadItems = selectedItems.map { item ->
//            UploadItem(
//                id = item["id"] as Int,
//                deptPpro = (item["dept_ppro"] as Number).toInt(),
//                divisiPpro = (item["divisi_ppro"] as Number).toInt(),
//                commodity = (item["commodity"] as Number).toInt(),
//                blokJjg = item["blok_jjg"] as String,
//                nopol = item["nopol"] as String,
//                driver = item["driver"] as String,
//                pemuatId = item["pemuat_id"].toString(),
//                transporterId = (item["transporter_id"] as Number).toInt(),
//                millId = (item["mill_id"] as Number).toInt(),
//                createdById = (item["created_by_id"] as Number).toInt(),
//                createdAt = item["created_at"] as String,
//                no_espb = item["no_espb"] as String,
//                uploader_info = infoApp,
//                uploaded_at = SimpleDateFormat(
//                    "yyyy-MM-dd HH:mm:ss",
//                    Locale.getDefault()
//                ).format(Date()),
//                uploaded_by_id = prefManager!!.idUserLogin!!.toInt()
//            )
//        }
//
//        // Merge all items into one combined UploadItem
////        val mergedItem = UploadItem(
////            id = -1, // Indicating merged data
////            deptPpro = 0, // Use logic if needed
////            divisiPpro = 0,
////            commodity = 0,
////            blokJjg = uploadItems.joinToString("; ") { it.blokJjg },
////            nopol = uploadItems.joinToString(", ") { it.nopol }.trim(),
////            driver = uploadItems.joinToString(", ") { it.driver }.trim(),
////            pemuatId = uploadItems.joinToString(", ") { it.pemuatId }.trim(),
////            transporterId = 0, // Use logic if necessary
////            millId = 0,
////            createdById = 0,
////            createdAt = uploadItems.maxByOrNull { it.createdAt }?.createdAt ?: "",
////            noEspb = uploadItems.joinToString(" | ") { it.noEspb }
////        )
//
//        // Add merged item to the list
////        val allUploadItems = uploadItems + mergedItem
//
//        val allUploadItems = uploadItems
//
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)
//
//        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
//        titleTV.text = "Progress Upload..."
//        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
//        counterTV.text = "0/${allUploadItems.size}"
//        val cancelDownloadDataset =
//            dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
//        val containerDownloadDataset =
//            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)
//
//        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = UploadProgressAdapter(uploadItems, weightBridgeViewModel)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//        dialog.show()
//
//        cancelDownloadDataset.setOnClickListener {
//            speedDial.close()
//            weightBridgeViewModel.loadHistoryUploadeSPB()
//            dialog.dismiss()
//        }
//
//        weightBridgeViewModel.uploadStatusMap.observe(this) { statusMap ->
//            val completedCount = statusMap.count { it.value == "Success" || it.value == "Failed" }
//            counterTV.text = "$completedCount/${allUploadItems.size}"
//
//            if (completedCount == allUploadItems.size) {
//                containerDownloadDataset.visibility = View.VISIBLE
//                cancelDownloadDataset.visibility = View.VISIBLE
//            }
//        }
//
//        weightBridgeViewModel.uploadESPBStagingKraniTimbang(
//            uploadItems.map { uploadItem ->
//                mapOf(
//                    "id" to uploadItem.id,
//                    "dept_ppro" to uploadItem.deptPpro,
//                    "divisi_ppro" to uploadItem.divisiPpro,
//                    "commodity" to uploadItem.commodity,
//                    "blok_jjg" to uploadItem.blokJjg,
//                    "nopol" to uploadItem.nopol,
//                    "driver" to uploadItem.driver,
//                    "pemuat_id" to uploadItem.pemuatId,
//                    "transporter_id" to uploadItem.transporterId,
//                    "mill_id" to uploadItem.millId,
//                    "created_by_id" to uploadItem.createdById,
//                    "created_at" to uploadItem.createdAt,
//                    "no_espb" to uploadItem.no_espb,
//                    "uploader_info" to uploadItem.uploader_info,
//                    "uploaded_at" to uploadItem.uploaded_at,
//                    "uploaded_by_id" to uploadItem.uploaded_by_id
//                )
//            }
//        )
//
//
//
//    }

    @SuppressLint("SetTextI18n")
    private fun setupObserveData() {
        absensiViewModel.savedDataAbsensiList.observe(this) { data ->

            if (data.isNotEmpty()) {
                speedDial.visibility = View.VISIBLE
                tvEmptyStateAbsensi.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Launch coroutine in lifecycleScope
                lifecycleScope.launch {
                    try {
                        val filteredData = coroutineScope {
                            data.map { item ->
                                async {

                                    AppLogger.d(item.toString())

                                    AbsensiDataRekap(
                                        //data untuk upload staging
                                        id = item.id,
//                                        estate = deptPPRO,
//                                        afdeling = divisiPPRO,
//                                        datetime = 0,
//                                        karyawan_msk_id = item.blok_jjg,
//                                        karyawan_tdk_msk_id = item.nopol,
//                                        driver = item.driver,
//                                        pemuat_id = item.pemuat_id,
//                                        transporter_id = item.transporter_id,
//                                        mill_id = item.mill_id,
//                                        created_by_id = item.created_by_id,
//                                        created_at = item.created_at,
//                                        noSPB = item.noESPB.ifEmpty { "-" },
                                        //untuk table
                                        kemandoranId = item.kemandoran_id,
                                        datetime = item.date_absen,
                                        karyawan_msk_id = item.karyawan_msk_id,
                                        karyawan_tdk_msk_id = item.karyawan_tdk_msk_id
                                    )

                                }
                            }.map { it.await() } // Wait for all async tasks to complete
                        }

                        absensiAdapter.updateList(filteredData)
                    } catch (e: Exception) {
                        AppLogger.e("Data processing error: ${e.message}")
                    }
                }
            } else {
                tvEmptyStateAbsensi.text = "No Uploaded e-SPB data available"
                tvEmptyStateAbsensi.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }

        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("TANGGAL", "LOKASI", "TOTAL KEHADIRAN")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.rvTableDataAbsensiList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        absensiAdapter = ListAbsensiAdapter(emptyList())
        recyclerView.adapter = absensiAdapter
    }



//    private fun setupRecyclerView() {
//        val headers = listOf("TANGGAL", "LOKASI", "TOTAL KEHADIRAN", "AKSI")
//        updateTableHeaders(headers)
//
//        recyclerView = findViewById(R.id.wbTableData)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        absensiAdapter = AbsensiAdapter(emptyList())
//        recyclerView.adapter = absensiAdapter
//    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tableHeaderAbsensi)
        val headerIds = listOf(R.id.th1ListAbsensi, R.id.th2ListAbsensi, R.id.th3ListAbsensi)
//edit headerIds
        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }
    }

    private fun initViewModel() {
        val factory = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel = ViewModelProvider(this, factory)[AbsensiViewModel::class.java]
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