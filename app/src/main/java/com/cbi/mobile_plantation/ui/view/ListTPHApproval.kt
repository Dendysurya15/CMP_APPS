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
//            backButton.isEnabled = false
            AlertDialogUtility.withTwoActions(
                this@ListTPHApproval,
                "KEMBALI",
                "Kembali ke Menu utama?",
                "Data scan sebelumnya akan terhapus",
                "warning.json",
                function = {
                    startActivity(Intent(this@ListTPHApproval, HomePageActivity::class.java))
                    finishAffinity()
//                    backButton.isEnabled = true
                },
                cancelFunction = {
//                    backButton.isEnabled = true
                }
            )
        }

        setupRecyclerView()
        processQRResult()
        val flCheckBoxTableHeaderLayout =
            findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        flCheckBoxTableHeaderLayout.visibility = View.GONE
        val constraintLayout = findViewById<ConstraintLayout>(R.id.clParentListPanen)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(
            R.id.filterSection, ConstraintSet.TOP,
            R.id.navbarPanenList, ConstraintSet.BOTTOM
        )
        constraintSet.applyTo(constraintLayout)
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
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)

        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )

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
                cancelFunction = {
                    // Do nothing on cancel
                }
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
        menuString = intent.getStringExtra("FEATURE_NAME").toString()
        AppLogger.d(menuString.toString())
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = menuString,
            tvFeatureName = tvFeatureName
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
                            }
                            adapter.updateList(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing QR result", e)
                // Consider showing an error message to the user
            }
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

    private suspend fun parseTphData(jsonString: String): List<TphRvData> =
        withContext(Dispatchers.IO) {
            try {
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

                        val displayName = tphInfo?.blokKode ?: "Unknown"

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
}