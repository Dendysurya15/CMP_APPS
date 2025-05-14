package com.cbi.mobile_plantation.ui.view

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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.mobile_plantation.data.repository.SaveTPHResult
import com.cbi.mobile_plantation.ui.adapter.TPHRvAdapter
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


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

    private var featureName: String? = null

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
        private const val TAG = "ListTPHApproval"
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
                                } else {
                                    repository.saveScanMPanen(
                                        saveDataMPanenList,
                                        createdBy,
                                        creatorInfo,
                                        this@ListTPHApproval
                                    )
                                }

                            result.fold(
                                onSuccess = { saveResult ->
                                    when (saveResult) {
                                        is SaveTPHResult.AllSuccess -> {
                                            _saveDataPanenState.value =
                                                SaveDataPanenState.Success(saveResult.savedIds)

                                            playSound(R.raw.berhasil_simpan)
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
                                                _saveDataPanenState.value = SaveDataPanenState.PartialSuccess(
                                                    savedIds = saveResult.savedIds,
                                                    duplicateCount = saveResult.duplicateCount,
                                                    duplicateInfo = saveResult.duplicateInfo
                                                )

                                                // Play success sound but show partial success message
                                                playSound(R.raw.berhasil_simpan)

                                                AlertDialogUtility.withSingleAction(
                                                    this@ListTPHApproval,
                                                    "OK",
                                                    "Sebagian data berhasil disimpan",
                                                    "${saveResult.savedIds.size} data disimpan, ${saveResult.duplicateCount} data duplikat dilewati.",
                                                    "warning.json"
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
                                        if (exception.message?.contains("All data is duplicate") == true) {
                                            AlertDialogUtility.withSingleAction(
                                                this@ListTPHApproval,
                                                "OK",
                                                "Data duplikat, anda telah melakukan scan untuk data panen ini!",
                                                "Error: ${exception.message}",
                                                "warning.json"
                                            ) {
                                                // Stay on the same screen
                                            }
                                        } else {
                                            Toasty.error(
                                                this@ListTPHApproval,
                                                "Error: ${exception.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
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
        val headers = if (featureName == AppUtils.ListFeatureNames.ScanPanenMPanen) {
            listOf("BLOK-TPH", "PEMANEN", "JAM", "JJG")
        } else {
            listOf("BLOK", "TPH/JJG", "JAM", "KP")
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

    private fun processQRResult() {
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT).orEmpty()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)

                    jsonStr?.let {
                        data = parseTphData(it)
                        withContext(Dispatchers.Main) {
                            if (data.isNotEmpty()) {
                                playSound(R.raw.berhasil_scan)

                                val totalSection: LinearLayout =
                                    findViewById(R.id.total_section)
                                val blokSection: LinearLayout =
                                    findViewById(R.id.blok_section)
                                val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
                                val titleTotalJjg: TextView = findViewById(R.id.titleTotalJjg)
                                val totalTphTextView: TextView = findViewById(R.id.totalTPH)
                                val listBlokTextView: TextView = findViewById(R.id.listBlok)

                                val totalJjg = data.sumOf { it.jjg.toInt() }
                                val totalTphCount = data.size

                                val blokSummary = calculateBlokSummary(data)


                                totalSection.visibility = View.VISIBLE
                                blokSection.visibility = View.VISIBLE
                                titleTotalJjg.text = "Kirim Pabrik: "
                                totalJjgTextView.text = totalJjg.toString()
                                totalTphTextView.text = totalTphCount.toString()
                                listBlokTextView.text = blokSummary
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

    private fun calculateBlokSummary(data: List<TphRvData>): String {
        // Group by normalized blok name (without trailing -xx)
        val blokGroups = data.groupBy { it.namaBlok.substringBeforeLast("-") }

        // For each blok, calculate total jjg and count of unique TPH numbers
        val blokSummaries = blokGroups.map { (blokName, entries) ->
            val totalJjg = entries.sumOf { it.jjg.toInt() }
            val uniqueTphCount = entries.distinctBy { it.noTPH }.size
            "$blokName($totalJjg/$uniqueTphCount)"
        }

        // Join all summaries with comma
        return blokSummaries.joinToString(", ")
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

    private suspend fun parseTphData(jsonString: String): List<TphRvData> =
        withContext(Dispatchers.IO) {
            try {
                AppLogger.d(jsonString.toString())
                val jsonObject = JSONObject(jsonString)
                val tph0String = jsonObject.getString("tph_0")
                val usernameString = try {
                    jsonObject.getString("username")
                } catch (e: Exception) {
                    AppLogger.d("Username tidak ditemukan: $e")
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

                    val parts = entry.split(",")
                    if (parts.size != 4 && featureName == AppUtils.ListFeatureNames.ScanHasilPanen) {
                        Log.e(
                            TAG,
                            "Invalid entry format, expected 4 parts but got ${parts.size}: $entry"
                        )
                        return@mapNotNull null
                    } else if (parts.size != 9 && featureName == AppUtils.ListFeatureNames.ScanPanenMPanen) {
                        Log.e(
                            TAG,
                            "Invalid entry format, expected 9 parts but got ${parts.size}: $entry"
                        )
                        return@mapNotNull null
                    }

                    try {
                        var idtph = 0
                        var dateIndex = "NULL"
                        var time = "NULL"
                        var jjg = 0

                        Log.d(
                            TAG,
                            "Processing idtph: $idtph, dateIndex: $dateIndex, time: $time, jjg: $jjg"
                        )

                        var displayData = TphRvData("NULL", "NULL", "NULL", "NULL", "NULL")
                        var saveDataHasilPanen = TphRvData("NULL", "NULL", "NULL", "NULL", "NULL")

                        if (featureName == AppUtils.ListFeatureNames.ScanHasilPanen) {
                            idtph = parts[0].toInt()
                            dateIndex = parts[1]
                            time = parts[2]
                            jjg = parts[3].toInt()
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
                                username = usernameString
                            )
                            // Create save data with original values
                            saveDataHasilPanen = TphRvData(
                                namaBlok = parts[0], // Original ID as namaBlok
                                noTPH = idtph.toString(),
                                time = fullDateTime, // Reconstructed full datetime
                                jjg = jjg.toString(),
                                username = usernameString
                            )
                        } else if (featureName == AppUtils.ListFeatureNames.ScanPanenMPanen) {
                            // Extract the values from parts first
                            idtph = parts[2].toInt()         // TPH ID
                            dateIndex = parts[0]             // Date index
                            time = parts[1]                  // Time
                            jjg =
                                parts[4].toInt() + parts[5].toInt() + parts[6].toInt() + parts[7].toInt() + parts[8].toInt()

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
                                jjg = jjg.toString(),
                                username = usernameString
                            )
                            // 2. Create separate PanenEntity for each NIK
                            val kP = parts[4].toInt() + parts[5].toInt() + parts[7].toInt() + parts[8].toInt()
                            val pA = parts[5].toInt() + parts[7].toInt() + parts[8].toInt()

                            val panenEntity = PanenEntity(
                                tph_id = idtph.toString(),
                                date_created = fullDateTime,
                                karyawan_nik = nik,
                                jjg_json = "{\"TO\":$jjg,\"UN\":${parts[4]},\"OV\":${parts[5]},\"EM\":${parts[6]},\"AB\":${parts[7]},\"RI\":${parts[8]},\"KP\":$kP,\"PA\":$pA}",
                                foto = "NULL",
                                komentar = "NULL",
                                asistensi = 0,
                                lat = 0.0,
                                lon = 0.0,
                                jenis_panen = 0,
                                ancak = 0,
                                info = "NULL",
                                scan_status = 0,
                                dataIsZipped = 0,
                                created_by = 0,
                                karyawan_id = "NULL",
                                kemandoran_id = "NULL",
                                karyawan_nama = "NULL",
                                jumlah_pemanen = if (nik.contains(",")) nik.split(",").size else 1
                            )
                            saveDataMPanenList.add(panenEntity)
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
}