package com.cbi.mobile_plantation.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.mobile_plantation.data.repository.SaveTPHResult
import com.cbi.mobile_plantation.ui.adapter.ReceiveDataBTHektaranAdapter
import com.cbi.mobile_plantation.ui.adapter.TPHRvAdapter
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID


// Update the SaveDataPanenState to handle the new result types
sealed class SaveDataPanenState {
    object Loading : SaveDataPanenState()
    data class Success(val savedIds: List<Long>) : SaveDataPanenState()
    data class PartialSuccess(
        val savedIds: List<Long>,
        val duplicateCount: Int,
        val duplicateInfo: String
    ) : SaveDataPanenState()

    data class Error(val message: String) : SaveDataPanenState()
}

class ListTPHApproval : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TPHRvAdapter
    private val repository by lazy { AppRepository(this) }
    private var bluetoothReceiveDialog: BottomSheetDialog? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var receivedDataList = mutableListOf<BluetoothDataItem>()
    private lateinit var receivedDataAdapter: ReceiveDataBTHektaranAdapter

    // Add this to your companion object
    private lateinit var loadingDialog: LoadingDialog

    // Data class for received Bluetooth data
    data class BluetoothDataItem(
        val senderName: String,
        val jsonData: String,
        val timestamp: String
    )

    private var featureName: String? = null
    private val saveDataTransferInspeksiList = mutableListOf<PanenEntity>()

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
        private const val TAG = "ListTPHApproval"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_ENABLE_BT_RECEIVE = 2
    }

    var menuString = ""
    private var prefManager: PrefManager? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    private lateinit var data: List<TphRvData>
    private lateinit var saveData: List<TphRvData>
    private lateinit var saveDataMpanen: List<PanenEntity>

    val _saveDataPanenState = MutableStateFlow<SaveDataPanenState>(SaveDataPanenState.Loading)

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private var activityInitialized = false
    private val saveDataMPanenList = mutableListOf<PanenEntity>()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        //cek tanggal otomatis
        checkDateTimeSettings()
    }

    private fun checkDateTimeSettings() {
        if (!AppUtils.isDateTimeValid(this)) {
            dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
            AppUtils.showDateTimeNetworkWarning(this)
        } else if (!activityInitialized) {
            initializeActivity()
            startPeriodicDateTimeChecking()
        }
    }

    private fun startPeriodicDateTimeChecking() {
        dateTimeCheckHandler.postDelayed(dateTimeCheckRunnable, AppUtils.DATE_TIME_INITIAL_DELAY)

    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun setupUI() {

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialogUtility.withTwoActions(
                    this@ListTPHApproval,
                    "KEMBALI",
                    "Kembali ke Menu utama?",
                    "Data scan sebelumnya akan terhapus",
                    "warning.json",
                    function = {
                        startActivity(
                            Intent(
                                this@ListTPHApproval,
                                HomePageActivity::class.java
                            )
                        )
                        finishAffinity()
                    },
                    cancelFunction = {

                    }
                )
            }
        })
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        setupHeader()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        val calendarContainer = findViewById<LinearLayout>(R.id.calendarContainer)
        calendarContainer.visibility = View.GONE
        backButton.setOnClickListener {

            AlertDialogUtility.withTwoActions(
                this@ListTPHApproval,
                "KEMBALI",
                "Kembali ke Menu utama?",
                "Data scan sebelumnya akan terhapus",
                "warning.json",
                function = {
                    startActivity(Intent(this@ListTPHApproval, HomePageActivity::class.java))
                    finishAffinity()

                },
                cancelFunction = {

                }
            )
        }

        setupRecyclerView()
        processQRResult()
        val flCheckBoxTableHeaderLayout =
            findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE

        val list_menu_upload_data = findViewById<LinearLayout>(R.id.list_menu_upload_data)
        list_menu_upload_data.visibility = View.GONE

        val btnGenerateQRTPH: FloatingActionButton = findViewById(R.id.btnGenerateQRTPH)
        btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
        btnGenerateQRTPH.imageTintList = ColorStateList.valueOf(Color.WHITE)
        // Convert 20dp to pixels
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics
        ).toInt()
        // Get the existing layout params and cast it to MarginLayoutParams
        val params = btnGenerateQRTPH.layoutParams as ViewGroup.MarginLayoutParams
        // Set margins (left, top, right, bottom)
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
        // Apply the updated params
        btnGenerateQRTPH.layoutParams = params

        featureName = intent.getStringExtra("FEATURE_NAME")


        val isTransferBluetooth = intent.getBooleanExtra("IS_TRANSFER_BLUETOOTH", false)
        if (isTransferBluetooth) {
            // Show Bluetooth receive bottom sheet
            checkBluetoothAndShowReceiveDialog()
        }

        btnGenerateQRTPH.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                this,
                "Simpan",
                "Apakah anda ingin menyimpan data ini?",
                getString(R.string.confirmation_dialog_description),
                "warning.json",
                function = {
                    lifecycleScope.launch {
                        try {
                            _saveDataPanenState.value = SaveDataPanenState.Loading

                            // Get device info
                            val appVersion: String = try {
                                this@ListTPHApproval.packageManager.getPackageInfo(
                                    this@ListTPHApproval.packageName, 0
                                ).versionName
                            } catch (e: Exception) {
                                Log.e("DeviceInfo", "Failed to get app version", e)
                                "Unknown"
                            }

                            val osVersion: String = try {
                                Build.VERSION.RELEASE
                            } catch (e: Exception) {
                                Log.e("DeviceInfo", "Failed to get OS version", e)
                                "Unknown"
                            }

                            val phoneModel: String = try {
                                "${Build.MANUFACTURER} ${Build.MODEL}"
                            } catch (e: Exception) {
                                Log.e("DeviceInfo", "Failed to get phone model", e)
                                "Unknown"
                            }

                            // Create creator info JSON
                            val creatorInfo =
                                createCreatorInfo(appVersion, osVersion, phoneModel).toString()
                            val createdBy = prefManager!!.idUserLogin.toString()


                            val result =
                                if (featureName == AppUtils.ListFeatureNames.ScanHasilPanen) {
                                    repository.saveTPHDataList(saveData)
                                } else if (featureName == AppUtils.ListFeatureNames.ScanTransferInspeksiPanen) {
                                    repository.saveTransferInspeksi(
                                        saveDataTransferInspeksiList,
                                        createdBy,
                                        creatorInfo,
                                        this@ListTPHApproval
                                    )
                                } else {
                                    repository.saveScanMPanen(
                                        saveDataMPanenList,
                                        createdBy,
                                        creatorInfo,
                                        this@ListTPHApproval
                                    )
                                }

                            AppLogger.d("result nya bro $result")

                            result.fold(
                                onSuccess = { saveResult ->
                                    when (saveResult) {
                                        is SaveTPHResult.AllSuccess -> {
                                            _saveDataPanenState.value =
                                                SaveDataPanenState.Success(saveResult.savedIds)

                                            playSound(R.raw.berhasil_simpan)
                                            delay(300)
                                            Toasty.success(
                                                this@ListTPHApproval,
                                                "Data berhasil disimpan",
                                                Toast.LENGTH_LONG,
                                                true
                                            ).show()
                                            startActivity(
                                                Intent(
                                                    this@ListTPHApproval,
                                                    HomePageActivity::class.java
                                                )
                                            )
                                            finish()
                                        }

                                        is SaveTPHResult.PartialSuccess -> {
                                            _saveDataPanenState.value =
                                                SaveDataPanenState.PartialSuccess(
                                                    savedIds = saveResult.savedIds,
                                                    duplicateCount = saveResult.duplicateCount,
                                                    duplicateInfo = saveResult.duplicateInfo
                                                )

                                            // Play success sound
                                            playSound(R.raw.berhasil_simpan)
                                            delay(300)
                                            // Format duplicate info for user display
                                            val duplicateDetails =
                                                saveResult.duplicateInfo.replace("TPH ID:", "TPH:")
                                                    .replace("Date:", "Tanggal:")

                                            // Show alert for partial success with duplicates
                                            AlertDialogUtility.withSingleAction(
                                                this@ListTPHApproval,
                                                "OK",
                                                "Beberapa data yang tidak terduplikat sudah tersimpan",
                                                "${saveResult.savedIds.size} data disimpan, ${saveResult.duplicateCount} data duplikat dilewati.\n\nData duplikat:\n$duplicateDetails",
                                                "warning.json",
                                                color = R.color.orange
                                            ) {
                                                startActivity(
                                                    Intent(
                                                        this@ListTPHApproval,
                                                        HomePageActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                        }

                                        is SaveTPHResult.AllDuplicate -> {
                                            _saveDataPanenState.value =
                                                SaveDataPanenState.Error("Semua data duplikat:")

                                            // Format duplicate info for user display
                                            val duplicateDetails =
                                                saveResult.duplicateInfo.replace("TPH ID:", "TPH:")
                                                    .replace("Date:", "Tanggal:")

                                            // Show alert for all duplicate
                                            AlertDialogUtility.withSingleAction(
                                                this@ListTPHApproval,
                                                "OK",
                                                "Data duplikat, anda telah melakukan scan untuk data panen ini!",
                                                "Semua ${saveResult.duplicateCount} data yang dipilih sudah ada di database.\n\nData duplikat:\n$duplicateDetails",
                                                "warning.json",
                                                color = R.color.colorRedDark
                                            ) {
                                                startActivity(
                                                    Intent(
                                                        this@ListTPHApproval,
                                                        HomePageActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                        }
                                    }
                                },
                                onFailure = { exception ->
                                    _saveDataPanenState.value = SaveDataPanenState.Error(
                                        exception.message ?: "Unknown error occurred"
                                    )
                                    Toasty.error(
                                        this@ListTPHApproval,
                                        "Error: ${exception.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } catch (e: Exception) {
                            _saveDataPanenState.value = SaveDataPanenState.Error(
                                e.message ?: "Unknown error occurred"
                            )
                        }
                    }
                },
                cancelFunction = {}

            )


        }
    }

    private fun setupRecyclerView() {
        // Set headers based on featureName
        val headers = when (featureName) {
            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                listOf("BLOK-TPH", "PEMANEN", "JAM", "JJG")
            }

            AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                listOf("BLOK-TPH", "TANGGAL", "TIPE PANEN/\nANCAK") // Only 3 headers
            }

            else -> {
                listOf("BLOK", "TPH/JJG", "JAM", "KP")
            }
        }

        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.rvTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TPHRvAdapter(emptyList(), featureName) // Pass featureName here
        recyclerView.adapter = adapter
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tableHeader)

        val headerIds = listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4)

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }
    }

    private fun createCreatorInfo(
        appVersion: String,
        osVersion: String,
        phoneModel: String
    ): JsonObject {
        return JsonObject().apply {
            addProperty("app_version", appVersion)
            addProperty("os_version", osVersion)
            addProperty("device_model", phoneModel)
        }
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

    private fun processQRResult(base64Data: String? = null) {
        val qrResult = base64Data ?: intent.getStringExtra(EXTRA_QR_RESULT).orEmpty()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)

                    AppLogger.d(jsonStr.toString())
                    AppLogger.d("laksdjflkjafdl kj $jsonStr")
                    jsonStr?.let {
                        data = parseTphData(it)
                        withContext(Dispatchers.Main) {
                            if (data.isNotEmpty()) {
                                val isTransferBluetooth =
                                    intent.getBooleanExtra("IS_TRANSFER_BLUETOOTH", false)

                                if (!isTransferBluetooth) {
                                    playSound(R.raw.berhasil_scan)
                                }


                                val totalSection: LinearLayout =
                                    findViewById(R.id.total_section)
                                val blokSection: LinearLayout =
                                    findViewById(R.id.blok_section)
                                val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
                                val titleTotalJjg: TextView = findViewById(R.id.titleTotalJjg)
                                val totalTphTextView: TextView = findViewById(R.id.totalTPH)
                                val listBlokTextView: TextView = findViewById(R.id.listBlok)
                                val totalJjgSection: LinearLayout =
                                    findViewById(R.id.totalJjgSection)

                                // Handle different calculations based on feature
                                when (featureName) {
                                    AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                                        val blokSummary = calculateBlokSummary(
                                            data,
                                            featureName
                                        ) // Pass featureName
                                        val totalTransaksi = data.size

                                        totalSection.visibility = View.VISIBLE
                                        totalJjgSection.visibility = View.GONE
                                        blokSection.visibility = View.VISIBLE
                                        totalTphTextView.text = totalTransaksi.toString()
                                        listBlokTextView.text = blokSummary
                                    }

                                    else -> {
                                        // For other features (ScanHasilPanen, ScanPanenMPanen)
                                        val totalJjg = data.sumOf {
                                            try {
                                                it.jjg.toInt()
                                            } catch (e: NumberFormatException) {
                                                0 // Return 0 if jjg is "NULL" or invalid
                                            }
                                        }
                                        val totalTphCount = data.size
                                        val blokSummary = calculateBlokSummary(data, featureName)

                                        totalSection.visibility = View.VISIBLE
                                        blokSection.visibility = View.VISIBLE
                                        titleTotalJjg.text =
                                            if (featureName == AppUtils.ListFeatureNames.ScanHasilPanen) "Kirim Pabrik: " else "Jjg Bayar: "
                                        totalJjgTextView.text = totalJjg.toString()
                                        totalTphTextView.text = totalTphCount.toString()
                                        listBlokTextView.text = blokSummary
                                    }
                                }
                            }

                            adapter.updateList(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing QR result", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ListTPHApproval,
                        "Error processing QR: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun calculateBlokSummary(data: List<TphRvData>, featureName: String? = null): String {
        // Group by normalized blok name (without trailing -xx)
        val blokGroups = data.groupBy { it.namaBlok.substringBeforeLast("-") }

        if (featureName == AppUtils.ListFeatureNames.ScanTransferInspeksiPanen) {
            // For ScanTransferInspeksiPanen, just return the block names
            val blokNames = blokGroups.keys.toList()
            return blokNames.joinToString(", ")
        } else {
            // For other features, calculate total jjg and count of unique TPH numbers
            val blokSummaries = blokGroups.map { (blokName, entries) ->
                val totalJjg = entries.sumOf {
                    try {
                        it.jjg.toInt()
                    } catch (e: NumberFormatException) {
                        0 // Return 0 if jjg is "NULL" or invalid
                    }
                }
                val uniqueTphCount = entries.distinctBy { it.noTPH }.size
                "$blokName($totalJjg/$uniqueTphCount)"
            }

            // Join all summaries with comma
            return blokSummaries.joinToString(", ")
        }
    }

    override fun onResume() {
        super.onResume()
        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {
        super.onPause()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        SoundPlayer.releaseMediaPlayer()
        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }


    // COMPLETE FUNCTION:
    private suspend fun parseTphData(jsonString: String): List<TphRvData> =
        withContext(Dispatchers.IO) {
            try {
                AppLogger.d(jsonString.toString())
                val jsonObject = JSONObject(jsonString)
                AppLogger.d("jsonObject $jsonObject")

                AppUtils.writeJsonToFile(this@ListTPHApproval, jsonObject, "tph_parsed_data")
                val tph0String = jsonObject.getString("tph_0")

                val usernameString = if (jsonObject.has("username")) {
                    jsonObject.getString("username")
                } else {
                    AppLogger.d("Username key not found in JSON")
                    "NULL"
                }

                val kemandoranId = if (jsonObject.has("kemandoran_id")) {
                    jsonObject.getString("kemandoran_id")
                } else {
                    AppLogger.d("kemandoran_id key not found in JSON")
                    "NULL"
                }

                // Parse the date mapping object
                val tglObject = try {
                    jsonObject.getJSONObject("tgl")
                } catch (e: Exception) {
                    AppLogger.d("tgl object tidak ditemukan: $e")
                    null
                }

                // Create a map of date indices to actual dates
                val dateMap = mutableMapOf<String, String>()
                if (tglObject != null) {
                    val keys = tglObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        dateMap[key] = tglObject.getString(key)
                    }
                }

                val nikMap = mutableMapOf<String, String>()
                if (featureName == AppUtils.ListFeatureNames.ScanPanenMPanen) {
                    // Parse the date mapping object
                    val nikObject = try {
                        jsonObject.getJSONObject("nik")
                    } catch (e: Exception) {
                        AppLogger.d("nik object tidak ditemukan: $e")
                        null
                    }
                    // Create a map of date indices to actual dates
                    if (nikObject != null) {
                        val keys = nikObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            nikMap[key] = nikObject.getString(key)
                        }
                    }
                }

                val parsedEntries = tph0String.split(";").mapNotNull { entry ->
                    if (entry.isBlank()) return@mapNotNull null

//                    AppLogger.d("Processing entry: $entry")

                    try {
                        var idtph = 0
                        var dateIndex = "NULL"
                        var time = "NULL"
                        var jjg = 0
                        var nomor_pemanen = 0
                        var asistensi = 1

                        var displayData = TphRvData(
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL"
                        )
                        var saveDataHasilPanen = TphRvData(
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            "NULL",
                            0,
                            asistensi = 1
                        )

                        if (featureName == AppUtils.ListFeatureNames.ScanHasilPanen) {
                            val parts = entry.split(",")
//                            if (parts.size != 4) {
//                                Log.e(
//                                    TAG,
//                                    "Invalid entry format, expected 4 parts but got ${parts.size}: $entry"
//                                )
//                                return@mapNotNull null
//                            }

                            idtph = parts[0].toInt()
                            dateIndex = parts[1]
                            time = parts[2]
                            jjg = parts[3].toInt()
                            nomor_pemanen = parts[4].toInt()
                            asistensi = parts[5].toInt()

                            AppLogger.d("asistensi $asistensi")
                            AppLogger.d("nomorPemanen $nomor_pemanen")
                            // Get the full date from the date map
                            val fullDate = dateMap[dateIndex] ?: "Unknown Date"
                            val fullDateTime = "$fullDate $time"

                            Log.d(TAG, "Full datetime: $fullDateTime")

                            val tphInfo = try {
                                repository.getTPHAndBlokInfo(idtph)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting TPH info for idtph $idtph", e)
                                null
                            }

                            val noTph = try {
                                tphInfo!!.tphNomor.toInt()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing tphNomor: ${tphInfo?.tphNomor}", e)
                                0
                            }

                            val displayName = tphInfo?.blokKode ?: "Tidak Diketahui"
                            // Create display data
                            displayData = TphRvData(
                                namaBlok = displayName,
                                noTPH = noTph.toString(),
                                time = time,  // Just show the time part for display
                                jjg = jjg.toString(),
                                username = usernameString,
                                kemandoran_id = "",
                                tipePanen = "NULL",
                                ancak = "NULL"
                            )
                            // Create save data with original values
                            saveDataHasilPanen = TphRvData(
                                namaBlok = parts[0], // Original ID as namaBlok
                                noTPH = idtph.toString(),
                                time = fullDateTime, // Reconstructed full datetime
                                jjg = jjg.toString(),
                                username = usernameString,
                                kemandoran_id = "",
                                tipePanen = "NULL",
                                ancak = "NULL",
                                nomor_pemanen = nomor_pemanen,
                                asistensi = asistensi,
                            )
                        } else if (featureName == AppUtils.ListFeatureNames.ScanPanenMPanen) {
                            val parts = entry.split(",")
                            if (parts.size != 9) {
                                Log.e(
                                    TAG,
                                    "Invalid entry format, expected 9 parts but got ${parts.size}: $entry"
                                )
                                return@mapNotNull null
                            }

                            // Extract the values from parts first
                            idtph = parts[2].toInt()         // TPH ID
                            dateIndex = parts[0]             // Date index
                            time = parts[1]                  // Time
                            val un = parts[4].toInt() // UN = buah mentah
                            val ov = parts[5].toInt() // OV = overripe
                            val em = parts[6].toInt() // EM = empty bunches
                            val ab = parts[7].toInt() // AB = abnormal
                            val ri = parts[8].toInt() // RI = ripe

                            val tbsDibayar = ov + ab + ri

                            val jjg = ov + ab + ri + un + em

                            // Get the full date from the date map
                            val fullDate = dateMap[dateIndex] ?: "Unknown Date"
                            val fullDateTime = "$fullDate $time"

                            Log.d(
                                TAG,
                                "Processing idtph: $idtph, dateIndex: $dateIndex, time: $time, jjg: $jjg"
                            )
                            Log.d(TAG, "Full datetime: $fullDateTime")

                            // Now get the TPH info with the proper idtph value
                            val tphInfo = try {
                                repository.getTPHAndBlokInfo(idtph)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting TPH info for idtph $idtph", e)
                                null
                            }

                            val displayName = tphInfo?.blokKode ?: "Unknown"

                            val noTph = try {
                                tphInfo?.tphNomor?.toInt() ?: 0
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing tphNomor: ${tphInfo?.tphNomor}", e)
                                0
                            }

                            // Use the fourth value (index 3) as the NIK key
                            val nikKey = parts[3]
                            val nik = nikMap[nikKey] ?: "NULL"

                            Log.d(TAG, "Using NIK key: $nikKey, got NIK: $nik")

                            // Parse names for display
                            val karyawan = try {
                                if (nik.contains(",")) {
                                    // Multiple NIKs, split and retrieve name for each
                                    val niks = nik.split(",").map { it.trim() }
                                    val names = niks.map { singleNik ->
                                        val name = repository.getNamaByNik(singleNik)
                                        name ?: singleNik // Fallback to NIK if name can't be found
                                    }
                                    names.joinToString(", ")
                                } else {
                                    // Single NIK
                                    repository.getNamaByNik(nik) ?: nik
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting karyawan info for NIK $nik", e)
                                nik // Fallback to NIK if name can't be found
                            }

                            displayData = TphRvData(
                                namaBlok = "$displayName-$noTph",
                                noTPH = karyawan, // Use karyawan name for display
                                time = time,
                                jjg = tbsDibayar.toString(),
                                username = usernameString,
                                kemandoran_id = "",
                                tipePanen = "NULL",
                                ancak = "NULL"
                            )
                            // 2. Create separate PanenEntity for each NIK
                            val kP =
                                parts[4].toInt() + parts[5].toInt() + parts[7].toInt() + parts[8].toInt()
                            val pA = parts[5].toInt() + parts[7].toInt() + parts[8].toInt()

                            val panenEntity = PanenEntity(
                                tph_id = idtph.toString(),
                                date_created = fullDateTime,
                                karyawan_nik = nik,
                                jjg_json = "{\"TO\":$jjg,\"UN\":${parts[4]},\"OV\":${parts[5]},\"EM\":${parts[6]},\"AB\":${parts[7]},\"RI\":${parts[8]},\"KP\":$kP,\"PA\":$pA}",
                                foto = "NULL",
                                komentar = "NULL",
                                asistensi = asistensi,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = 0,
                                ancak = 0,
                                info = "NULL",
                                scan_status = 0,
                                nomor_pemanen = 0,
                                dataIsZipped = 0,
                                created_by = 0,
                                karyawan_id = "NULL",
                                kemandoran_id = kemandoranId,
                                karyawan_nama = "NULL",
                                jumlah_pemanen = if (nik.contains(",")) nik.split(",").size else 1,
                                status_scan_mpanen = 1
                            )

//                            AppLogger.d("panenEntity $panenEntity")
                            saveDataMPanenList.add(panenEntity)
                        } else if (featureName == AppUtils.ListFeatureNames.ScanTransferInspeksiPanen) {
                            // Parse ScanTransferInspeksiPanen with JSON handling
                            // Format: 222593,0,07:20:48,1,16,{"1232500026":"DANIEL DAWU BORA","1232100169":"ARSYAD"}

                            var idtph: Int
                            var dateIndex: String
                            var time: String
                            var tipePanen: Int
                            var ancak: Int
                            var jsonPart = ""

                            // Check if entry contains JSON (has curly braces)
                            if (entry.contains("{") && entry.contains("}")) {
                                // Find the JSON part
                                val jsonStartIndex = entry.indexOf("{")
                                val jsonEndIndex = entry.lastIndexOf("}") + 1
                                jsonPart = entry.substring(jsonStartIndex, jsonEndIndex)

                                // Get the part before JSON and split by comma
                                val beforeJson = entry.substring(0, jsonStartIndex).trimEnd(',')
                                val parts = beforeJson.split(",")

                                if (parts.size < 5) {
                                    Log.e(
                                        TAG,
                                        "Invalid entry format for ScanTransferInspeksiPanen with JSON, expected at least 5 parts before JSON but got ${parts.size}: $entry"
                                    )
                                    return@mapNotNull null
                                }

                                idtph = parts[0].toInt()
                                dateIndex = parts[1]
                                time = parts[2]
                                tipePanen = parts[3].toInt()
                                ancak = parts[4].toInt()

                            } else {
                                // No JSON, just split by comma normally
                                val parts = entry.split(",")

                                if (parts.size < 5) {
                                    Log.e(
                                        TAG,
                                        "Invalid entry format for ScanTransferInspeksiPanen without JSON, expected at least 5 parts but got ${parts.size}: $entry"
                                    )
                                    return@mapNotNull null
                                }

                                idtph = parts[0].toInt()
                                dateIndex = parts[1]
                                time = parts[2]
                                tipePanen = parts[3].toInt()
                                ancak = parts[4].toInt()
                            }

                            // Get the full date from the date map
                            val fullDate = dateMap[dateIndex] ?: "Unknown Date"
                            val fullDateTime = "$fullDate $time"

                            Log.d(
                                TAG,
                                "Processing ScanTransferInspeksiPanen - idtph: $idtph, dateIndex: $dateIndex, time: $time, tipePanen: $tipePanen, ancak: $ancak"
                            )
                            Log.d(TAG, "Full datetime: $fullDateTime")
                            Log.d(TAG, "JSON part: $jsonPart")

                            // Parse worker information from JSON if exists
                            val nikNames = mutableListOf<String>()
                            val nikNumbers = mutableListOf<String>()

                            if (jsonPart.isNotEmpty()) {
                                try {
                                    val nikJsonObject = JSONObject(jsonPart)
                                    val nikKeys = nikJsonObject.keys()
                                    while (nikKeys.hasNext()) {
                                        val nik = nikKeys.next()
                                        val name = nikJsonObject.getString(nik)
                                        nikNumbers.add(nik)
                                        nikNames.add(name)
                                    }
                                } catch (e: Exception) {
                                    AppLogger.d("Error parsing NIK JSON: $e")
                                    AppLogger.d("JSON part was: $jsonPart")
                                }
                            }

                            // Join NIKs and names with commas
                            val karyawanNik =
                                if (nikNumbers.isNotEmpty()) nikNumbers.joinToString(",") else "NULL"
                            val karyawanNama =
                                if (nikNames.isNotEmpty()) nikNames.joinToString(",") else "NULL"

                            Log.d(TAG, "Parsed NIKs: $karyawanNik")
                            Log.d(TAG, "Parsed Names: $karyawanNama")

                            // Get karyawan data from database
                            val karyawanList = try {
                                if (nikNumbers.isNotEmpty()) {
                                    repository.getKemandoranByNik(nikNumbers)
                                } else {
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                AppLogger.d("Error getting karyawan data: $e")
                                emptyList()
                            }

                            // Extract IDs and kemandoran_ids
                            val karyawanIds = if (karyawanList.isNotEmpty()) {
                                karyawanList.map { it.id.toString() }.joinToString(",")
                            } else {
                                "NULL"
                            }

                            val kemandoranIds = if (karyawanList.isNotEmpty()) {
                                karyawanList.map { it.kemandoran_id.toString() }.joinToString(",")
                            } else {
                                kemandoranId // Fallback to original kemandoranId if no workers found
                            }

                            Log.d(TAG, "Karyawan IDs: $karyawanIds")
                            Log.d(TAG, "Kemandoran IDs: $kemandoranIds")

                            // Get TPH info
                            val tphInfo = try {
                                repository.getTPHAndBlokInfo(idtph)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting TPH info for idtph $idtph", e)
                                null
                            }

                            val displayName = tphInfo?.blokKode ?: "Unknown"
                            val noTph = try {
                                tphInfo?.tphNomor?.toInt() ?: 0
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing tphNomor: ${tphInfo?.tphNomor}", e)
                                0
                            }

                            // Convert tipe panen to readable format
                            val tipePanenText = if (tipePanen == 0) "NORMAL" else "CUT & CARRY"

                            // Create display data
                            displayData = TphRvData(
                                namaBlok = displayName,               // BLOK only (E019A)
                                noTPH = noTph.toString(),             // TPH number only (70)
                                time = fullDateTime,                  // TANGGAL (full date + time)
                                jjg = "0",                           // Not used for this feature
                                username = usernameString,
                                kemandoran_id = kemandoranId,
                                tipePanen = tipePanenText,           // TIPE_PANEN (NORMAL or CUT & CARRY)
                                ancak = ancak.toString()             // ANCAK (16)
                            )

                            // CREATE AND STORE TO GLOBAL VARIABLE
                            val transferInspeksiEntity = PanenEntity(
                                tph_id = idtph.toString(),
                                date_created = fullDateTime,
                                karyawan_nik = karyawanNik,           // Comma-separated NIKs
                                karyawan_id = karyawanIds,            // Comma-separated IDs from database
                                karyawan_nama = karyawanNama,         // Comma-separated names
                                kemandoran_id = kemandoranIds,
                                jjg_json = "", // Store tipe_panen and ancak if needed
                                foto = "NULL",
                                komentar = "NULL",
                                asistensi = 0,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = tipePanen,              // Store tipe panen as jenis_panen
                                ancak = ancak,                        // Store ancak
                                info = "NULL",
                                scan_status = 0,
                                nomor_pemanen = 0,
                                dataIsZipped = 0,
                                created_by = 0,
                                jumlah_pemanen = nikNumbers.size,     // Number of workers
                                status_scan_mpanen = 0,  // Different from MPanen
                                status_scan_inspeksi = 1
                            )

                            AppLogger.d("transferInspeksiEntity $transferInspeksiEntity")
                            saveDataTransferInspeksiList.add(transferInspeksiEntity) // ADD TO GLOBAL LIST!

                            // Create save data (keeping original structure for compatibility)
                            saveDataHasilPanen = TphRvData(
                                namaBlok = idtph.toString(), // Use idtph instead of parts[0]
                                noTPH = idtph.toString(),
                                time = fullDateTime,
                                jjg = ancak.toString(),
                                username = usernameString,
                                kemandoran_id = kemandoranId,
                                tipePanen = tipePanenText,
                                ancak = ancak.toString()
                            )
                        }
                        Pair(displayData, saveDataHasilPanen)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing entry: $entry", e)
                        null
                    }
                }

                // Separate display and save data
                data = parsedEntries.map { it.first }
                saveData = parsedEntries.map { it.second }

                return@withContext data
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON", e)
                data = emptyList()
                saveData = emptyList()
                return@withContext emptyList()
            }
        }

    // 1. Create a sealed class to represent different types of save data
    sealed class SaveDataType {
        data class TPHData(val data: TphRvData) : SaveDataType()
        data class PanenData(val data: PanenEntity) : SaveDataType()
    }


    @SuppressLint("MissingPermission")
    private fun checkBluetoothAndShowReceiveDialog() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        when {
            bluetoothAdapter == null -> {
                Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT)
                    .show()
            }

            !bluetoothAdapter!!.isEnabled -> {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Aktifkan",
                    "Bluetooth Nonaktif",
                    "Aktifkan Bluetooth untuk menerima data",
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_RECEIVE)
                    },
                    cancelFunction = {
                        // User cancelled enabling Bluetooth
                    }
                )
            }

            else -> {
                // Bluetooth is enabled, show receive dialog
                showBluetoothReceiveDialog()
            }
        }
    }

    private fun showBluetoothReceiveDialog() {
        bluetoothReceiveDialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.layout_bluetooth_receive, null)
        bluetoothReceiveDialog?.setContentView(dialogView)

        // Use the EXACT same pattern as your working code
        val maxHeight = (resources.displayMetrics.heightPixels * 0.55).toInt()

        bluetoothReceiveDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    this.peekHeight = maxHeight
                    this.state = BottomSheetBehavior.STATE_EXPANDED
                    this.isFitToContents = true
                    this.isDraggable = false
                    this.isHideable = false  // ✅ Prevent hiding by swiping
                }

                bottomSheet.layoutParams?.height = maxHeight
            }

        // ✅ PREVENT OUTSIDE TOUCH DISMISSAL
        bluetoothReceiveDialog?.setCanceledOnTouchOutside(false)

        // ✅ PREVENT BACK BUTTON DISMISSAL (optional)
        bluetoothReceiveDialog?.setCancelable(false)

        // Get views and set content
        val tvBluetoothStatus = dialogView.findViewById<TextView>(R.id.tvBluetoothStatus)
        val tvReceiveStatus = dialogView.findViewById<TextView>(R.id.tvReceiveStatus)
        val btnCloseReceive = dialogView.findViewById<Button>(R.id.btnCloseReceive)
        val titleDialogBluetoothReceive = dialogView.findViewById<TextView>(R.id.titleDialogBluetoothReceive)

        titleDialogBluetoothReceive.text = when (featureName) {
            AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> "Data Transfer Inspeksi Bluetooth"
            AppUtils.ListFeatureNames.ScanHasilPanen -> "Data Hasil Panen Bluetooth"
            else -> "Data Transfer Hektaran Bluetooth" // or AppUtils.ListFeatureNames.ScanPanenMPanen
        }

        tvBluetoothStatus.text = "Status Bluetooth: Aktif - Mendengarkan koneksi"
        tvReceiveStatus.text = "Menunggu data dari perangkat pengirim..."

        // Setup RecyclerView
        receivedDataAdapter = ReceiveDataBTHektaranAdapter(receivedDataList)

        btnCloseReceive.setOnClickListener {
            stopBluetoothServer()
            bluetoothReceiveDialog?.dismiss()
            startActivity(Intent(this@ListTPHApproval, HomePageActivity::class.java))
            finishAffinity()
        }

        bluetoothReceiveDialog?.setOnDismissListener {
            stopBluetoothServer()
        }

        bluetoothReceiveDialog?.show()
        startBluetoothServer(dialogView)
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothServer(dialogView: View) {
        val tvReceiveStatus = dialogView.findViewById<TextView>(R.id.tvReceiveStatus)
        val tvBluetoothStatus = dialogView.findViewById<TextView>(R.id.tvBluetoothStatus)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarReceive)

        tvReceiveStatus.text = "Menunggu koneksi dari perangkat pengirim..."
        tvBluetoothStatus.text = "Status Bluetooth: Aktif - Mendengarkan koneksi"
        progressBar.visibility = View.VISIBLE

        Thread {
            try {
                val uuid =
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
                bluetoothServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    "TPHTransferService", uuid
                )

                runOnUiThread {
                    tvReceiveStatus.text = "Server Bluetooth aktif. Menunggu koneksi..."
                }

                while (true) {
                    try {
                        val socket = bluetoothServerSocket?.accept()
                        socket?.let { bluetoothSocket ->
                            handleBluetoothConnection(bluetoothSocket, dialogView)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Bluetooth server error: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to start Bluetooth server: ${e.message}")
                runOnUiThread {
                    tvReceiveStatus.text = "Gagal memulai server Bluetooth $e"
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }

    // Updated auto-save function that returns the saved data for feedback
    // Enhanced SaveResult to include duplicate count
    data class SaveResult(
        val success: Boolean,
        val savedCount: Int = 0,
        val duplicateCount: Int = 0, // Add duplicate count
        val errorMessage: String = "",
        val savedData: List<Map<String, Any>> = emptyList(),
        val duplicateData: List<Map<String, Any>> = emptyList()
    )

    // Updated autoSaveBluetoothData to return duplicate count
    private suspend fun autoSaveBluetoothData(): SaveResult = withContext(Dispatchers.IO) {
        try {
            // Check if we have saveData populated from processQRResult -> parseTphData
            if (saveData.isEmpty() && saveDataMPanenList.isEmpty() && saveDataTransferInspeksiList.isEmpty()) {
                return@withContext SaveResult(
                    false,
                    0,
                    0,
                    "No data to save - saveData lists are empty"
                )
            }

            AppLogger.d("Starting auto-save with:")
            AppLogger.d("- saveData size: ${saveData.size}")
            AppLogger.d("- saveDataMPanenList size: ${saveDataMPanenList.size}")
            AppLogger.d("- saveDataTransferInspeksiList size: ${saveDataTransferInspeksiList.size}")
            AppLogger.d("- featureName: $featureName")

            // Get device info (same as manual save)
            val appVersion: String = try {
                this@ListTPHApproval.packageManager.getPackageInfo(
                    this@ListTPHApproval.packageName, 0
                ).versionName
            } catch (e: Exception) {
                AppLogger.e("Failed to get app version: ${e.message}")
                "Unknown"
            }

            val osVersion: String = try {
                Build.VERSION.RELEASE
            } catch (e: Exception) {
                AppLogger.e("Failed to get OS version: ${e.message}")
                "Unknown"
            }

            val phoneModel: String = try {
                "${Build.MANUFACTURER} ${Build.MODEL}"
            } catch (e: Exception) {
                AppLogger.e("Failed to get phone model: ${e.message}")
                "Unknown"
            }

            // Create creator info JSON (same as manual save)
            val creatorInfo = createCreatorInfo(appVersion, osVersion, phoneModel).toString()
            val createdBy = prefManager!!.idUserLogin.toString()

            val result = when (featureName) {
                AppUtils.ListFeatureNames.ScanHasilPanen -> {
                    AppLogger.d("Saving ScanHasilPanen data...")
                    repository.saveTPHDataList(saveData)
                }

                AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                    AppLogger.d("Saving ScanTransferInspeksiPanen data...")
                    repository.saveTransferInspeksi(
                        saveDataTransferInspeksiList,
                        createdBy,
                        creatorInfo,
                        this@ListTPHApproval
                    )
                }

                else -> { // ScanPanenMPanen
                    AppLogger.d("Saving ScanPanenMPanen data...")
                    repository.saveScanMPanen(
                        saveDataMPanenList,
                        createdBy,
                        creatorInfo,
                        this@ListTPHApproval
                    )
                }
            }

            AppLogger.d("Repository save result: $result")

            // Handle the result and create feedback data
            return@withContext result.fold(
                onSuccess = { saveResult ->
                    when (saveResult) {
                        is SaveTPHResult.AllSuccess -> {
                            AppLogger.d("All data saved successfully: ${saveResult.savedIds.size} items")
                            val savedDataList = createSavedDataList(featureName!!)
                            SaveResult(
                                success = true,
                                savedCount = saveResult.savedIds.size,
                                duplicateCount = 0, // No duplicates
                                errorMessage = "All data saved successfully",
                                savedData = savedDataList,
                                duplicateData = emptyList()
                            )
                        }

                        is SaveTPHResult.PartialSuccess -> {
                            AppLogger.d("Partial success: ${saveResult.savedIds.size} saved, ${saveResult.duplicateCount} duplicates")

                            val savedEntities =
                                getSavedEntitiesFromIds(saveResult.savedIds, featureName!!)
                            val savedDataList = createSavedDataList(featureName!!, savedEntities)
                            val duplicateEntities =
                                getDuplicateEntities(savedEntities, featureName!!)
                            val duplicateDataList =
                                createSavedDataList(featureName!!, duplicateEntities)

                            SaveResult(
                                success = true,
                                savedCount = saveResult.savedIds.size,
                                duplicateCount = saveResult.duplicateCount, // Include duplicate count
                                errorMessage = "${saveResult.savedIds.size} data disimpan, ${saveResult.duplicateCount} data duplikat dilewati",
                                savedData = savedDataList,
                                duplicateData = duplicateDataList
                            )
                        }

                        is SaveTPHResult.AllDuplicate -> {
                            val duplicateDataList = createSavedDataList(featureName!!)
                            SaveResult(
                                success = false, // Set to false for all duplicates
                                savedCount = 0,
                                duplicateCount = saveResult.duplicateCount, // Include duplicate count
                                errorMessage = "Semua ${saveResult.duplicateCount} data sudah ada di database (duplikat)",
                                savedData = emptyList(),
                                duplicateData = duplicateDataList
                            )
                        }
                    }
                },
                onFailure = { exception ->
                    AppLogger.e("Save failed with exception: ${exception.message}")
                    SaveResult(false, 0, 0, exception.message ?: "Unknown error occurred")
                }
            )

        } catch (e: Exception) {
            AppLogger.e("Fatal error in auto-save process: ${e.message}")
            SaveResult(false, 0, 0, e.message ?: "Unknown error")
        }
    }

    private fun createStatusMessage(saveResult: SaveResult): String {
        return when {
            saveResult.savedCount > 0 && saveResult.duplicateCount > 0 -> {
                // Partial success: some saved, some duplicates
                "Data berhasil disimpan! (${saveResult.savedCount} tersimpan, ${saveResult.duplicateCount} duplikat)"
            }

            saveResult.savedCount > 0 && saveResult.duplicateCount == 0 -> {
                // All success: all saved, no duplicates
                "Data berhasil disimpan! (${saveResult.savedCount} item)"
            }

            saveResult.savedCount == 0 && saveResult.duplicateCount > 0 -> {
                // All duplicates: nothing saved, all duplicates
                "Semua data duplikat! (${saveResult.duplicateCount} duplikat)"
            }

            else -> {
                // Fallback
                saveResult.errorMessage
            }
        }
    }

    private fun getSavedEntitiesFromIds(
        savedIds: List<Long>,
        featureName: String
    ): List<PanenEntity> {
        return when (featureName) {
            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                // Filter saveDataMPanenList to get only the entities that were successfully saved
                // This is a simple approach - you might need to adjust based on your actual data structure
                saveDataMPanenList.take(savedIds.size) // Simplified - assumes saved IDs correspond to first N entities
            }

            else -> emptyList()
        }
    }

    // Helper function to get duplicate entities
    private fun getDuplicateEntities(
        savedEntities: List<PanenEntity>,
        featureName: String
    ): List<PanenEntity> {
        return when (featureName) {
            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                // Get entities that were not saved (duplicates)
                saveDataMPanenList.drop(savedEntities.size) // Simplified approach
            }

            else -> emptyList()
        }
    }

    // Helper function to create saved data list for feedback
    private fun createSavedDataList(
        featureName: String,
        savedEntities: List<PanenEntity> = emptyList()
    ): List<Map<String, Any>> {
        return when (featureName) {
            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                // Use savedEntities if provided, otherwise fall back to original list
                val entitiesToProcess =
                    if (savedEntities.isNotEmpty()) savedEntities else saveDataMPanenList

                // For MPanen, extract only the required fields: tph_id, date_created, jjg_json, nik
                entitiesToProcess.map { panenEntity ->
                    mapOf(
                        "tph_id" to panenEntity.tph_id,
                        "date_created" to panenEntity.date_created,
                        "jjg_json" to panenEntity.jjg_json,
                        "karyawan_nik" to panenEntity.karyawan_nik,
                    )
                }
            }

            AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                // Use savedEntities if provided, otherwise fall back to original list
                val entitiesToProcess =
                    if (savedEntities.isNotEmpty()) savedEntities else saveDataTransferInspeksiList

                // For MPanen, extract only the required fields: tph_id, date_created, jjg_json, nik
                entitiesToProcess.map { panenEntity ->
                    mapOf(
                        "tph_id" to panenEntity.tph_id,
                        "date_created" to panenEntity.date_created,
                        "karyawan_nama" to panenEntity.karyawan_nama,
                        "karyawan_nik" to panenEntity.karyawan_nik,
                    )
                }
            }

            else -> emptyList()
        }
    }


    // Updated success feedback to include saved data and duplicates
    private fun createSuccessFeedback(
        savedCount: Int,
        duplicateCount: Int = 0,
        savedData: List<Map<String, Any>> = emptyList(),
        duplicateData: List<Map<String, Any>> = emptyList()
    ): String {
        val feedbackData = mapOf(
            "status" to "success",
            "message" to "Data berhasil diproses",
            "savedCount" to savedCount,
            "duplicateCount" to duplicateCount, // Add duplicate count to feedback
            "timestamp" to System.currentTimeMillis(),
            "savedData" to savedData,
            "duplicateData" to duplicateData
        )

        val feedbackJson = Gson().toJson(feedbackData)
        AppLogger.d("Creating success feedback:")
        AppLogger.d("  savedCount: $savedCount")
        AppLogger.d("  duplicateCount: $duplicateCount")
        AppLogger.d("  savedData size: ${savedData.size}")
        AppLogger.d("  duplicateData size: ${duplicateData.size}")
        AppLogger.d("  Full feedback JSON: $feedbackJson")

        return feedbackJson
    }

    // Error feedback remains the same
    private fun createErrorFeedback(errorMessage: String): String {
        val feedbackData = mapOf(
            "status" to "error",
            "message" to "Gagal menyimpan data",
            "error" to errorMessage,
            "timestamp" to System.currentTimeMillis()
        )

        val feedbackJson = Gson().toJson(feedbackData)
        AppLogger.d("Creating error feedback:")
        AppLogger.d("  errorMessage: $errorMessage")
        AppLogger.d("  Full feedback JSON: $feedbackJson")

        return feedbackJson
    }


    // Updated startCountdownAndProcess to pass saved data to feedback
    private fun startCountdownAndProcess(
        tvReceiveStatus: TextView,
        senderName: String,
        btnCloseReceive: Button,
        socket: BluetoothSocket? = null
    ) {
        saveDataMPanenList.clear()
        saveDataTransferInspeksiList.clear()

        tvReceiveStatus.setTextColor(
            ContextCompat.getColor(
                this@ListTPHApproval,
                R.color.greenDarker
            )
        )
        btnCloseReceive.isEnabled = false
        btnCloseReceive.alpha = 0.5f

        lifecycleScope.launch {
            for (i in 2 downTo 1) {
                tvReceiveStatus.text = "Konversi data JSON dalam $i detik..."
                delay(1000)
            }

            tvReceiveStatus.text = "Memproses data..."

            try {
                processReceivedBluetoothData()
                AppLogger.d("Data processing completed:")
                AppLogger.d("- data size: ${data.size}")
                AppLogger.d("- saveData size: ${saveData.size}")
                AppLogger.d("- saveDataMPanenList size: ${saveDataMPanenList.size}")
                AppLogger.d("- saveDataTransferInspeksiList size: ${saveDataTransferInspeksiList.size}")

            } catch (e: Exception) {
                AppLogger.e("Error processing Bluetooth data: ${e.message}")
                tvReceiveStatus.text = "Error memproses data: ${e.message}"
                tvReceiveStatus.setTextColor(
                    ContextCompat.getColor(
                        this@ListTPHApproval,
                        R.color.colorRedDark
                    )
                )
                btnCloseReceive.isEnabled = true
                btnCloseReceive.alpha = 1.0f
                return@launch
            }

            tvReceiveStatus.text = "Data berhasil diproses dari $senderName"
            delay(1000)

            tvReceiveStatus.text = "Sedang simpan data..."
            loadingDialog.show()
            loadingDialog.setMessage("Menyimpan data...", true)
            delay(2000)

            try {
                val saveResult = autoSaveBluetoothData()
                loadingDialog.dismiss()

                // Handle different save scenarios

                AppLogger.d("saveResult $saveResult")
                when {
                    saveResult.success -> {
                        // Success or partial success
                        val statusMessage = createStatusMessage(saveResult)
                        tvReceiveStatus.text = statusMessage
                        tvReceiveStatus.setTextColor(
                            ContextCompat.getColor(
                                this@ListTPHApproval,
                                R.color.greenDarker
                            )
                        )
                        playSound(R.raw.berhasil_simpan)

                        // Send success feedback
                        sendFeedbackToSender(
                            socket,
                            createSuccessFeedback(
                                saveResult.savedCount,
                                saveResult.duplicateCount,
                                saveResult.savedData,
                                saveResult.duplicateData
                            )
                        )

                        btnCloseReceive.isEnabled = true
                        btnCloseReceive.alpha = 1.0f
                        btnCloseReceive.text = "Tutup"

                        delay(3000)
                        bluetoothReceiveDialog?.dismiss()

                        // Create final dialog message
                        val finalMessage = if (saveResult.duplicateCount > 0) {
                            "Data berhasil diproses:\n• ${saveResult.savedCount} data baru tersimpan\n• ${saveResult.duplicateCount} data duplikat dilewati"
                        } else {
                            "Data ${featureName} berhasil disimpan (${saveResult.savedCount} item)"
                        }

                        // ADD THIS NEW CODE HERE:
                        val dialogColor = if (saveResult.duplicateCount > 0) {
                            R.color.orange // Orange for partial success with duplicates
                        } else {
                            R.color.greenDarker // Green for all success
                        }


                        AlertDialogUtility.withSingleAction(
                            this@ListTPHApproval,
                            "OK",
                            "Transfer Data Selesai",
                            finalMessage,
                            "success.json",
                            dialogColor
                        ) {
                            startActivity(Intent(this@ListTPHApproval, HomePageActivity::class.java))
                            finish()
                        }

                    }

                    else -> {
                        // All duplicates or error
                        val statusMessage = createStatusMessage(saveResult)
                        tvReceiveStatus.text = statusMessage

                        // Use orange color for all duplicates, red for errors
                        val textColor = if (saveResult.duplicateCount > 0) {
                            R.color.orange // You might need to define this color
                        } else {
                            R.color.colorRedDark
                        }
                        tvReceiveStatus.setTextColor(
                            ContextCompat.getColor(
                                this@ListTPHApproval,
                                textColor
                            )
                        )

                        // Send appropriate feedback
                        if (saveResult.duplicateCount > 0) {
                            // All duplicates - still "successful" transfer but no new data
                            sendFeedbackToSender(
                                socket,
                                createSuccessFeedback(
                                    saveResult.savedCount,
                                    saveResult.duplicateCount,
                                    saveResult.savedData,
                                    saveResult.duplicateData
                                )
                            )
                        } else {
                            // Actual error
                            sendFeedbackToSender(
                                socket,
                                createErrorFeedback(saveResult.errorMessage)
                            )
                        }

                        btnCloseReceive.isEnabled = true
                        btnCloseReceive.alpha = 1.0f
                    }
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                AppLogger.e("Error auto-saving data: ${e.message}")
                tvReceiveStatus.text = "Error menyimpan data: ${e.message}"
                tvReceiveStatus.setTextColor(
                    ContextCompat.getColor(
                        this@ListTPHApproval,
                        R.color.colorRedDark
                    )
                )

                sendFeedbackToSender(socket, createErrorFeedback(e.message ?: "Unknown error"))

                btnCloseReceive.isEnabled = true
                btnCloseReceive.alpha = 1.0f
            }
        }
    }

    // Send feedback to sender
    @SuppressLint("MissingPermission")
    private fun sendFeedbackToSender(socket: BluetoothSocket?, feedbackJson: String) {
        socket?.let { bluetoothSocket ->
            Thread {
                try {
                    val outputStream = bluetoothSocket.outputStream
                    val feedbackData = "FEEDBACK_START\n$feedbackJson\nFEEDBACK_END"
                    val feedbackBytes = feedbackData.toByteArray(Charsets.UTF_8)

                    outputStream.write(feedbackBytes)
                    outputStream.flush()

                    AppLogger.d("Feedback sent to sender: $feedbackJson")

                    // Close the socket after sending feedback
//                    delay(1000)
                    bluetoothSocket.close()

                } catch (e: Exception) {
                    AppLogger.e("Error sending feedback: ${e.message}")
                    try {
                        bluetoothSocket.close()
                    } catch (closeError: Exception) {
                        AppLogger.e("Error closing socket: ${closeError.message}")
                    }
                }
            }.start()
        }
    }

    // Updated handleBluetoothConnection to pass socket to startCountdownAndProcess
    @SuppressLint("MissingPermission")
    private fun handleBluetoothConnection(socket: BluetoothSocket, dialogView: View) {
        val tvReceiveStatus = dialogView.findViewById<TextView>(R.id.tvReceiveStatus)
        val btnCloseReceive = dialogView.findViewById<Button>(R.id.btnCloseReceive)
        Thread {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(2048)
                val stringBuilder = StringBuilder()
                var totalBytesReceived = 0

                runOnUiThread {
                    tvReceiveStatus.setTextColor(
                        ContextCompat.getColor(
                            this@ListTPHApproval,
                            R.color.greenDarker
                        )
                    )
                    tvReceiveStatus.text =
                        "Terhubung! Menerima data dari ${socket.remoteDevice.name ?: "Unknown Device"}..."
                    btnCloseReceive.isEnabled = false
                    btnCloseReceive.alpha = 0.5f
                }

                var isDataStarted = false
                var consecutiveEmptyReads = 0
                val maxEmptyReads = 10

                while (consecutiveEmptyReads < maxEmptyReads) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            consecutiveEmptyReads = 0
                            totalBytesReceived += bytes
                            val receivedData = String(buffer, 0, bytes, Charsets.UTF_8)
                            stringBuilder.append(receivedData)

                            val currentData = stringBuilder.toString()

                            runOnUiThread {
                                tvReceiveStatus.text =
                                    "Menerima data... ${totalBytesReceived} bytes"
                            }

                            // Check for start marker
                            if (!isDataStarted && currentData.contains("START_DATA")) {
                                isDataStarted = true
                                AppLogger.d("Data transmission started")
                            }

                            // Check for end marker and complete data
                            if (isDataStarted && currentData.contains("END_DATA")) {
                                AppLogger.d("Data transmission completed")

                                // Extract base64 data between markers
                                val startIndex =
                                    currentData.indexOf("START_DATA") + "START_DATA".length
                                val endIndex = currentData.indexOf("END_DATA")

                                if (startIndex > 0 && endIndex > startIndex) {
                                    val base64Data =
                                        currentData.substring(startIndex, endIndex).trim()

                                    if (base64Data.isNotBlank()) {
                                        val senderName =
                                            socket.remoteDevice.name ?: "Unknown Device"
                                        val timestamp = java.text.SimpleDateFormat(
                                            "dd/MM/yyyy HH:mm:ss",
                                            java.util.Locale.getDefault()
                                        )
                                            .format(java.util.Date())

                                        val dataItem =
                                            BluetoothDataItem(senderName, base64Data, timestamp)

                                        runOnUiThread {
                                            receivedDataList.add(dataItem)
                                            tvReceiveStatus.text =
                                                "Data berhasil diterima dari $senderName (${totalBytesReceived} bytes)"

                                            // Start countdown and auto-process (pass socket for feedback)
                                            startCountdownAndProcess(
                                                tvReceiveStatus,
                                                senderName,
                                                btnCloseReceive,
                                                socket
                                            )
                                        }

                                        AppLogger.d("Successfully received base64 data: ${base64Data.length} characters")
                                        break
                                    } else {
                                        AppLogger.e("Received empty data")
                                        runOnUiThread {
                                            tvReceiveStatus.text = "Error: Data kosong"
                                            tvReceiveStatus.setTextColor(
                                                ContextCompat.getColor(
                                                    this@ListTPHApproval,
                                                    R.color.colorRedDark
                                                )
                                            )
                                            // FIXED: Re-enable button on empty data error
                                            btnCloseReceive.isEnabled = true
                                            btnCloseReceive.alpha = 1.0f
                                        }
                                        break
                                    }
                                }
                            }

                        } else {
                            consecutiveEmptyReads++
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error reading data: ${e.message}")
                        runOnUiThread {
                            tvReceiveStatus.text = "Error membaca data: ${e.message}"
                            tvReceiveStatus.setTextColor(
                                ContextCompat.getColor(
                                    this@ListTPHApproval,
                                    R.color.colorRedDark
                                )
                            )
                            // FIXED: Re-enable button on reading error (like Bluetooth turned off)
                            btnCloseReceive.isEnabled = true
                            btnCloseReceive.alpha = 1.0f
                        }
                        break
                    }
                }

                if (consecutiveEmptyReads >= maxEmptyReads) {
                    runOnUiThread {
                        tvReceiveStatus.text = "Koneksi timeout - tidak ada data lebih lanjut"
                        tvReceiveStatus.setTextColor(
                            ContextCompat.getColor(
                                this@ListTPHApproval,
                                R.color.colorRedDark
                            )
                        )
                        // FIXED: Re-enable button on timeout
                        btnCloseReceive.isEnabled = true
                        btnCloseReceive.alpha = 1.0f
                    }
                }

                // Don't close socket here - let sendFeedbackToSender handle it

            } catch (e: Exception) {
                AppLogger.e("Error handling Bluetooth connection: ${e.message}")
                runOnUiThread {
                    tvReceiveStatus.text = "Error menerima data: ${e.message}"
                    tvReceiveStatus.setTextColor(
                        ContextCompat.getColor(
                            this@ListTPHApproval,
                            R.color.colorRedDark
                        )
                    )
                    // FIXED: Re-enable button on main catch block (connection errors)
                    btnCloseReceive.isEnabled = true
                    btnCloseReceive.alpha = 1.0f
                }
                try {
                    socket.close()
                } catch (closeError: Exception) {
                    AppLogger.e("Error closing socket: ${closeError.message}")
                }
            }
        }.start()
    }


    @SuppressLint("MissingPermission")
    private fun stopBluetoothServer() {
        try {
            bluetoothServerSocket?.close()
            bluetoothServerSocket = null
        } catch (e: Exception) {
            AppLogger.e("Error stopping Bluetooth server: ${e.message}")
        }
    }


    private suspend fun processReceivedBluetoothData() {
        if (receivedDataList.isNotEmpty()) {
            val latestData = receivedDataList.last()
            try {
                AppLogger.d("Processing Bluetooth received base64 data...")

                // Call processQRResult synchronously (remove lifecycleScope.launch)
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(latestData.jsonData)

                    AppLogger.d(jsonStr.toString())
                    jsonStr?.let {
                        data = parseTphData(it)
                        withContext(Dispatchers.Main) {
                            if (data.isNotEmpty()) {
                                val isTransferBluetooth =
                                    intent.getBooleanExtra("IS_TRANSFER_BLUETOOTH", false)

                                if (!isTransferBluetooth) {
                                    playSound(R.raw.berhasil_scan)
                                }


                                val totalSection: LinearLayout =
                                    findViewById(R.id.total_section)
                                val blokSection: LinearLayout =
                                    findViewById(R.id.blok_section)
                                val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
                                val titleTotalJjg: TextView = findViewById(R.id.titleTotalJjg)
                                val totalTphTextView: TextView = findViewById(R.id.totalTPH)
                                val listBlokTextView: TextView = findViewById(R.id.listBlok)
                                val totalJjgSection: LinearLayout =
                                    findViewById(R.id.totalJjgSection)

                                // Handle different calculations based on feature
                                when (featureName) {
                                    AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                                        val blokSummary = calculateBlokSummary(
                                            data,
                                            featureName
                                        ) // Pass featureName
                                        val totalTransaksi = data.size

                                        totalSection.visibility = View.VISIBLE
                                        totalJjgSection.visibility = View.GONE
                                        blokSection.visibility = View.VISIBLE
                                        totalTphTextView.text = totalTransaksi.toString()
                                        listBlokTextView.text = blokSummary
                                    }

                                    else -> {
                                        // For other features (ScanHasilPanen, ScanPanenMPanen)
                                        val totalJjg = data.sumOf {
                                            try {
                                                it.jjg.toInt()
                                            } catch (e: NumberFormatException) {
                                                0 // Return 0 if jjg is "NULL" or invalid
                                            }
                                        }
                                        val totalTphCount = data.size
                                        val blokSummary = calculateBlokSummary(data, featureName)

                                        totalSection.visibility = View.VISIBLE
                                        blokSection.visibility = View.VISIBLE
                                        titleTotalJjg.text =
                                            if (featureName == AppUtils.ListFeatureNames.ScanHasilPanen) "Kirim Pabrik: " else "Jjg Bayar: "
                                        totalJjgTextView.text = totalJjg.toString()
                                        totalTphTextView.text = totalTphCount.toString()
                                        listBlokTextView.text = blokSummary
                                    }
                                }


                            }
                            adapter.updateList(data)
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Error memproses data: ${e.message}", Toast.LENGTH_LONG).show()
                AppLogger.e("Error processing received data: ${e.message}")
                throw e // Re-throw so caller can handle
            }
        }
    }

    // Update your existing onActivityResult method
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(
                        this,
                        "Bluetooth diaktifkan. Menyiapkan data...",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Your existing generateJsonAndShowBluetoothDialog() call
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth diperlukan untuk transfer data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            REQUEST_ENABLE_BT_RECEIVE -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(
                        this,
                        "Bluetooth diaktifkan. Memulai penerima...",
                        Toast.LENGTH_SHORT
                    ).show()
                    showBluetoothReceiveDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth diperlukan untuk menerima data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}