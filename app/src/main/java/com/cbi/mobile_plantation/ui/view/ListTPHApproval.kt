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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
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
    private var isValidAllBlok = true
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
    val _saveDataPanenState = MutableStateFlow<SaveDataPanenState>(SaveDataPanenState.Loading)

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private var activityInitialized = false

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

                                val result = repository.saveTPHDataList(saveData)

                                result.fold(
                                    onSuccess = { saveResult ->
                                        when (saveResult) {
                                            is SaveTPHResult.AllSuccess -> {
                                                _saveDataPanenState.value = SaveDataPanenState.Success(saveResult.savedIds)

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
        val headers = listOf("BLOK", "TPH/JJG", "JAM", "KP")
        updateTableHeaders(headers)

        recyclerView = findViewById(R.id.rvTableData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TPHRvAdapter(emptyList())
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

                                val totalSection: LinearLayout = findViewById(R.id.total_section)
                                val blokSection: LinearLayout = findViewById(R.id.blok_section)
                                val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
                                val titleTotalJjg: TextView = findViewById(R.id.titleTotalJjg)
                                val totalTphTextView: TextView = findViewById(R.id.totalTPH)
                                val listBlokTextView: TextView = findViewById(R.id.listBlok)

                                val totalJjg = data.sumOf { it.jjg }
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
                    Toast.makeText(this@ListTPHApproval, "Error processing QR: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calculateBlokSummary(data: List<TphRvData>): String {
        // Group by blok name
        val blokGroups = data.groupBy { it.namaBlok }

        // For each blok, calculate total jjg and count of unique TPH numbers
        val blokSummaries = blokGroups.map { (blokName, entries) ->
            val totalJjg = entries.sumOf { it.jjg }
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

                // Reset validation flag before processing new data
                isValidAllBlok = true

                val parsedEntries = tph0String.split(";").mapNotNull { entry ->
                    if (entry.isBlank()) return@mapNotNull null

                    val parts = entry.split(",")
                    if (parts.size != 4) {
                        Log.e(
                            TAG,
                            "Invalid entry format, expected 4 parts but got ${parts.size}: $entry"
                        )
                        return@mapNotNull null
                    }

                    try {
                        val idtph = parts[0].toInt()
                        val dateIndex = parts[1]
                        val time = parts[2]
                        val jjg = parts[3].toInt()

                        Log.d(
                            TAG,
                            "Processing idtph: $idtph, dateIndex: $dateIndex, time: $time, jjg: $jjg"
                        )

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

                        // Check if this TPH has valid blok data
                        val isValidBlok = tphInfo != null && tphInfo.blokKode != "Tidak Diketahui"

                        // If any TPH has invalid blok, set the global flag to false
                        if (!isValidBlok) {
                            isValidAllBlok = false
                            AppLogger.d("Found invalid blok for TPH ID: $idtph")
                        }

                        val displayName = tphInfo?.blokKode ?: "Tidak Diketahui"

                        // Create display data
                        val displayData = TphRvData(
                            namaBlok = displayName,
                            noTPH = try {
                                tphInfo!!.tphNomor.toInt()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing tphNomor: ${tphInfo?.tphNomor}", e)
                                0
                            },
                            time = time,  // Just show the time part for display
                            jjg = jjg,
                            username = usernameString
                        )

                        // Create save data with original values
                        val saveData = TphRvData(
                            namaBlok = parts[0], // Original ID as namaBlok
                            noTPH = idtph,
                            time = fullDateTime, // Reconstructed full datetime
                            jjg = jjg,
                            username = usernameString
                        )

                        Pair(displayData, saveData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing entry: $entry", e)
                        // Also mark as invalid if there's an exception parsing the entry
                        isValidAllBlok = false
                        null
                    }
                }

                // Log the validation status
                AppLogger.d("All bloks valid: $isValidAllBlok")

                // Separate display and save data
                data = parsedEntries.map { it.first }
                saveData = parsedEntries.map { it.second }

                return@withContext data
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON", e)
                data = emptyList()
                saveData = emptyList()
                isValidAllBlok = false // Mark as invalid on overall parse error
                return@withContext emptyList()
            }
        }
}