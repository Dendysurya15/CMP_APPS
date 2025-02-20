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
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.weightBridge.wbQRData
import com.cbi.cmp_project.ui.view.HomePageActivity

import com.cbi.cmp_project.ui.viewModel.WeightBridgeViewModel
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.formatToIndonesianDate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanWeightBridgeActivity : AppCompatActivity() {
    private lateinit var weightBridgeViewModel: WeightBridgeViewModel
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

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)
        setContentView(R.layout.activity_scan_weight_bridge)
        loadingDialog = LoadingDialog(this)
        setupHeader()
        setupInfoWb()
        initViewModel()
        processQRResult()

    }

    private fun initViewModel() {
        val factory = WeightBridgeViewModel.WeightBridgeViewModelFactory(application)
        weightBridgeViewModel = ViewModelProvider(this, factory)[WeightBridgeViewModel::class.java]
    }

    private fun setupInfoWb() {

        setInfo(findViewById(R.id.infoEstate), "Estate", "-")
        setInfo(findViewById(R.id.infoAfdeling), "Afdeling", "-")
        setInfo(findViewById(R.id.infoBlok), "Blok", "-")
        setInfo(findViewById(R.id.infoNoPol), "No Pol", "-")
        setInfo(findViewById(R.id.infoNoDriver), "Driver", "-")
        setInfo(findViewById(R.id.infoTransporter), "Transporter", "-")
        setInfo(findViewById(R.id.infoMill), "Transporter", "-")
        setInfo(findViewById(R.id.infoTotalJjg), "Total Janjang", "-")

    }

    private fun setInfo(view: View, title: String, value: String) {
        val titleView = view.findViewById<TextView>(R.id.titlInfoWb)
        val valueView = view.findViewById<TextView>(R.id.valInfoWb)

        titleView.text = title
        if (view.id == R.id.infoBlok) {
            valueView.text = value
        } else {
            valueView.text = ": $value"
        }

    }

    private fun processQRResult() {
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT).orEmpty()
        AppLogger.d(qrResult)

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)
            }
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)

                    val parsedData = Gson().fromJson(jsonStr, wbQRData::class.java)

                    // Extract only idBlok from blokJjgList and map it to pairs of (idBlok, totalJjg)
//                    val blokJjgList =  "3301,312;3303,154;3309,321;3310,215;3312,421;3315,233"
                    val blokJjgList = parsedData.espb.blokJjg
                        .split(";")
                        .mapNotNull {
                            it.split(",").takeIf { it.size == 2 }?.let { (id, jjg) ->
                                id.toIntOrNull()?.let { it to jjg.toIntOrNull() }
                            }
                        }

                    // Extract only the idBlok list
                    val idBlokList = blokJjgList.map { it.first }

                    val blokListDeferred = async {
                        try {

                            AppLogger.d(idBlokList.toString())
                            weightBridgeViewModel.getBlokById(idBlokList)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Blok Data: ${e.message}")
                            null
                        }
                    }

                    val blokData = blokListDeferred.await()
                        ?: throw Exception("Failed to fetch Blok Data! Please check the dataset.")

                    val distinctDeptAbbr = blokData.mapNotNull { it.dept_abbr }.distinct().takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "-"
                    val distinctDivisiAbbr = blokData.mapNotNull { it.divisi_abbr }.distinct().takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "-"


                    var totalJjgSum = 0

                    val formattedBlokList = blokJjgList.mapNotNull { (idBlok, totalJjg) ->
                        val blokKode = blokData.find { it.blok == idBlok }?.blok_kode
                        if (blokKode != null && totalJjg != null) { // Ensure totalJjg is not null
                            totalJjgSum += totalJjg
                            "• $blokKode ($totalJjg jjg)"
                        } else {
                            null
                        }
                    }.joinToString("\n").takeIf { it.isNotBlank() } ?: ": -"


                    val noEspb = parsedData.espb.noEspb
                    val millId = parsedData.espb.millId
                    val transporterId = parsedData.espb.transporter
                    val nopol = parsedData.espb.nopol
                    val driverName = parsedData.espb.driver
                    val createdAt = parsedData.espb.createdAt
                    val createAtFormatted = formatToIndonesianDate(createdAt)

                    val millDataDeferred = async {
                        try {
                            weightBridgeViewModel.getMillName(millId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Mill Data: ${e.message}")
                            null
                        }
                    }

                    val millData = millDataDeferred.await()
                        ?: throw Exception("Failed to fetch Mill Data! Please check the dataset.")

                    val transporterDeferred = async {
                        try {
                            weightBridgeViewModel.getTransporterName(transporterId)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching Transporter Data: ${e.message}")
                            null
                        }
                    }

                    val transporterData = transporterDeferred.await()
                        ?: throw Exception("Failed to fetch Tarnsporter Data! Please check the dataset.")

                    withContext(Dispatchers.Main) {
                        val millAbbr =
                            millData.firstOrNull()?.let { "${it.abbr} (${it.nama})" } ?: "-"
                        val transporterName = transporterData.firstOrNull()?.let { it.nama } ?: "-"
                        findViewById<TextView>(R.id.noEspbTitleScanWB).setText("Detail e-SPB $noEspb")
                        findViewById<TextView>(R.id.infoCreatedAt).setText("Tanggal e-SPB:  $createAtFormatted")
                        setInfo(findViewById(R.id.infoEstate), "Estate", distinctDeptAbbr)
                        setInfo(findViewById(R.id.infoAfdeling), "Afdeling", distinctDivisiAbbr)
                        setInfo(findViewById(R.id.infoNoPol), "No Polisi", nopol)
                        setInfo(findViewById(R.id.infoBlok), "Blok", formattedBlokList)
                        setInfo(findViewById(R.id.infoTotalJjg), "Total Janjang", "$totalJjgSum Jjg")
                        setInfo(findViewById(R.id.infoNoDriver), "Driver", driverName)
                        setInfo(findViewById(R.id.infoMill), "Mill", millAbbr)
                        setInfo(findViewById(R.id.infoTransporter), "Transporter", transporterName)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error Processing QR Result: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                }
            }
        }
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


    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent =
            Intent(this, HomePageActivity::class.java) // Change this to your desired activity
        startActivity(intent)
        finish()
    }
}