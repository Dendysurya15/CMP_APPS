package com.cbi.mobile_plantation.ui.view.Absensi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
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
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.adapter.AbsensiDataRekap
import com.cbi.mobile_plantation.ui.adapter.ListAbsensiAdapter
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScreenshotUtil
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("UNREACHABLE_CODE")
class ListAbsensiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var absensiViewModel: AbsensiViewModel
    private lateinit var absensiAdapter: ListAbsensiAdapter
    private var isSettingUpCheckbox = false
    private lateinit var filterAllData: CheckBox
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
    private var globalFormattedDate: String = ""

    // Flag to track if we're currently in the process of deleting
    private var isDeleting = false

    private var originalData: List<Map<String, Any>> = emptyList()

    private lateinit var filterSection: LinearLayout

    // Add views for buttons and counters
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private var currentState = 0 // 0 for tersimpan, 1 for terscan
    private var mappedData: List<Map<String, Any>> = emptyList()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var speedDial: SpeedDialView
    private lateinit var rvListAbsensi: RecyclerView
    private var tvTitlePage: TextView? = null
    private lateinit var tvEmptyStateAbsensi: TextView // Add this
    private var activityInitialized = false
    private lateinit var dateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_absensi)
    }

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    private fun setupUI() {
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        infoApp = AppUtils.getDeviceInfo(this@ListAbsensiActivity).toString()

        globalFormattedDate = AppUtils.currentDate
        // Initialize views
        recyclerView = findViewById(R.id.rvTableDataAbsensiList)
        speedDial = findViewById(R.id.dial_listAbsensi)
        tvTitlePage = findViewById(R.id.tvEmptyStateAbsensiList) // This might be null if you don't have it

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener {
            onBackPressed()
        }

        findViewById<LinearLayout>(R.id.calendarContainerAbsensi).visibility = View.VISIBLE
        dateButton = findViewById(R.id.calendarPickerAbsensi)
        dateButton.text = AppUtils.getTodaysDate()

//        filterAllData = findViewById(R.id.calendarCheckboxAbsensi)
//
//        filterAllData.setOnCheckedChangeListener { _, isChecked ->
//            val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainerAbsensi)
//            val nameFilterDate = findViewById<TextView>(R.id.name_filter_dateAbsensi)
//            if (isChecked) {
//                filterDateContainer.visibility = View.VISIBLE
//                nameFilterDate.text = "Semua Data"
//
//                dateButton.isEnabled = false
//                dateButton.alpha = 0.5f
//
//                if (currentState == 0) {
//                    absensiViewModel.loadHistoryRekapAbsensi(archive = 0)
//                } else if (currentState == 1) {
//                    absensiViewModel.loadHistoryRekapAbsensi(archive = 1)
//                }
//            } else {
//                // For line 136 (use date from date picker)
//                val displayDate = formatGlobalDate(globalFormattedDate)
//
//                nameFilterDate.text = displayDate
//                dateButton.isEnabled = true
//                dateButton.alpha = 1f // Make the button appear darker
//                Log.d("FilterAllData", "Checkbox is UNCHECKED. Button enabled.")
//                if (currentState == 0) {
//                    absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 0)
//                } else if (currentState == 1) {
//                    absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 1)
//                }
//            }
//
//            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_dateAbsensi)
//
//            removeFilterDate.setOnClickListener {
//                if (filterAllData.isChecked) {
//                    filterAllData.isChecked = false
//                }
//
//                filterDateContainer.visibility = View.GONE
//
//                val todayBackendDate = AppUtils.formatDateForBackend(
//                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
//                    Calendar.getInstance().get(Calendar.MONTH) + 1,
//                    Calendar.getInstance().get(Calendar.YEAR)
//                )
//                // Reset the selected date in your utils
//                AppUtils.setSelectedDate(todayBackendDate)
//
//                // Update the dateButton to show today's date
//                val todayDisplayDate = AppUtils.getTodaysDate()
//                dateButton.text = todayDisplayDate
//
//            }
//        }

        setupHeader()
        initViewModel()
        setupRecyclerView()
        initializeViews()
        setupObserveData()
        setupSpeedDial()
        if (prefManager!!.jabatanUserLogin == "Kerani Panen") {
            absensiViewModel.getAllDataAbsensi(1)
        } else {
            absensiViewModel.getAllDataAbsensi(0)
        }
        if (prefManager!!.jabatanUserLogin == "Kerani Panen") {
            absensiViewModel.loadAbsensiCountArchive(1)
        } else {
            absensiViewModel.loadAbsensiCountArchive(0)
        }
        setupQRAbsensi()
        setupCardListeners()
        setActiveCard(cardTersimpan)
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

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun startPeriodicDateTimeChecking() {
        dateTimeCheckHandler.postDelayed(dateTimeCheckRunnable, AppUtils.DATE_TIME_INITIAL_DELAY)

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

        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    fun getGeneratedDate(): String {
        val sharedPreferences = getSharedPreferences("QR_PREFS", MODE_PRIVATE)
        val savedDate = sharedPreferences.getString("generated_date", "")

        AppLogger.d("📥 Fetched Generated Date: '$savedDate'")

        if (savedDate.isNullOrEmpty()) {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            saveGeneratedDate(todayDate) // Simpan langsung
            return todayDate
        }
        return savedDate
    }


    fun saveGeneratedDate(date: String) {
        val sharedPreferences = getSharedPreferences("QR_PREFS", MODE_PRIVATE)
        val success = sharedPreferences.edit().putString("generated_date", date).commit()
        AppLogger.d("✅ Date Saved: '$date', Success: $success")
    }


    fun isSameDate(): Boolean {
        val sharedPreferences = getSharedPreferences("QR_PREFS", MODE_PRIVATE)
        val savedDate = sharedPreferences.getString("generated_date", "") ?: ""
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        AppLogger.d("🔍 Debug: savedDate = '$savedDate', todayDate = '$todayDate'")

        return savedDate == todayDate
    }


    fun generateData() {
        if (isSameDate()) {
            // Proses generate bisa dilakukan
            AppLogger.d("✅ Data generated for today")
            // Lakukan proses generate di sini...
        } else {
            AppLogger.d("❌ Data cannot be generated, different date")
            // Tampilkan pesan ke pengguna bahwa tidak bisa generate data
            Toast.makeText(
                this,
                "Generate hanya bisa dilakukan untuk tanggal yang sama!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupQRAbsensi() {
        val btnGenerateQRAbsensi = findViewById<FloatingActionButton>(R.id.btnGenerateQRAbsensi)
        btnGenerateQRAbsensi.setOnClickListener {
            // First dialog: Check if employee status has been filled
            AlertDialogUtility.withSingleAction(
                this,
                "OKE",
                "Peringatan",
                "Pastikan sudah mengisi status karyawan yang tidak hadir",
                "warning.json",
                R.color.colorRedDark,
                function = {
                    // After confirming the status check, show the QR generation dialog
                    AlertDialogUtility.withTwoActions(
                        this,
                        "Generate QR",
                        getString(R.string.confirmation_dialog_title),
                        getString(R.string.al_confirm_generate_qr),
                        "warning.json",
                        ContextCompat.getColor(this, R.color.bluedarklight),
                        function = {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val todayDate = dateFormat.format(Date())  // Ambil tanggal hari ini
                            val generatedDate = getGeneratedDate() ?: "" // Ambil tanggal yang tersimpan (default kosong jika null)

                            // Log tanggal
                            AppLogger.d("Generated Date: '$generatedDate'")
                            AppLogger.d("Today Date: '$todayDate'")

                            if (generatedDate.isEmpty() || generatedDate != todayDate) {
                                // Jika belum pernah generate atau tanggal berbeda dari yang tersimpan
                                // Berarti ini adalah generate baru untuk hari ini
                                playSound(R.raw.berhasil_generate_qr)
                                saveGeneratedDate(todayDate)  // Simpan tanggal hari ini
                                generateData()  // Generate data baru
                            } else {
                                // Jika sudah pernah generate di hari yang sama
                                // Tampilkan QR yang sudah ada
                                showBottomSheetQR()
                            }
                        }
                    )
                }
            )
        }
    }

    private fun showBottomSheetQR() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_generate_qr_absen, null)
        view.background = ContextCompat.getDrawable(
            this@ListAbsensiActivity,
            R.drawable.rounded_top_right_left
        )

        val dialog = BottomSheetDialog(this@ListAbsensiActivity)
        dialog.setContentView(view)

        // Get references to views
        val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
        val tvTitleQRGenerate: TextView =
            view.findViewById(R.id.textTitleQRGenerate)
        tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
        val capitalizedFeatureName = featureName!!.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        tvTitleQRGenerate.text = "Hasil QR $capitalizedFeatureName"
        val dashedLine: View = view.findViewById(R.id.dashedLine)
        val loadingContainer: LinearLayout =
            view.findViewById(R.id.loadingDotsContainerBottomSheet)
        val titleQRConfirm: TextView = view.findViewById(R.id.titleAfterScanQR)
        val descQRConfirm: TextView = view.findViewById(R.id.descAfterScanQR)
        // Initial setup for text elements
        titleQRConfirm.setResponsiveTextSizeWithConstraints(21F, 17F, 25F)
        descQRConfirm.setResponsiveTextSizeWithConstraints(19F, 15F, 23F)

        val btnPreviewFullQR: MaterialButton =
            view.findViewById(R.id.btnPreviewFullQR)

        btnPreviewFullQR.setOnClickListener {
            showQrCodeFullScreen(qrCodeImageView.drawable, view)
        }

        // Initially hide QR code and dashed line, show loading
        qrCodeImageView.visibility = View.GONE

        loadingLogo.visibility = View.VISIBLE
        loadingContainer.visibility = View.VISIBLE

        // Load and start bounce animation
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        loadingLogo.startAnimation(bounceAnimation)

        // Setup dots animation
        val dots = listOf(
            loadingContainer.findViewById<View>(R.id.dot1),
            loadingContainer.findViewById<View>(R.id.dot2),
            loadingContainer.findViewById<View>(R.id.dot3),
            loadingContainer.findViewById<View>(R.id.dot4)
        )

        dots.forEachIndexed { index, dot ->
            val translateAnimation =
                ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
            val scaleXAnimation = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
            val scaleYAnimation = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)

            listOf(
                translateAnimation,
                scaleXAnimation,
                scaleYAnimation
            ).forEach { animation ->
                animation.duration = 500
                animation.repeatCount = ObjectAnimator.INFINITE
                animation.repeatMode = ObjectAnimator.REVERSE
                animation.startDelay = (index * 100).toLong()
                animation.start()
            }
        }

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        dialog.show()

        lifecycleScope.launch(Dispatchers.Default) {
            delay(1000)
            try {
                lifecycleScope.launch(Dispatchers.Default) {
                    delay(1000)
                    try {
                        val dataQR: TextView? = view.findViewById(R.id.dataQR)
                        val titleQRConfirm: TextView =
                            view.findViewById(R.id.titleAfterScanQR)
                        val descQRConfirm: TextView =
                            view.findViewById(R.id.descAfterScanQR)
                        val btnConfirmScanAbsensi: MaterialButton =
                            view.findViewById(R.id.btnConfirmScanPanenTPH)

                        btnConfirmScanAbsensi.setOnClickListener {
                            AlertDialogUtility.withTwoActions(
                                this@ListAbsensiActivity,
                                getString(R.string.al_yes),
                                getString(R.string.confirmation_dialog_title),
                                getString(R.string.al_make_sure_scanned_qr),
                                "warning.json",
                                ContextCompat.getColor(
                                    this@ListAbsensiActivity,
                                    R.color.greendarkerbutton
                                ),
                                function = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            withContext(Dispatchers.Main) {
                                                loadingDialog.show()
                                            }

                                            // Validate data first
                                            if (mappedData.isEmpty()) {
                                                throw Exception("No data to archive")
                                            }

                                            var hasError = false
                                            var successCount = 0
                                            val errorMessages = mutableListOf<String>()
                                            takeQRCodeScreenshot(view)

                                            mappedData.forEach { item ->
                                                try {
                                                    // Null check for item
                                                    if (item == null) {
                                                        errorMessages.add("Found null item in data")
                                                        hasError = true
                                                        return@forEach
                                                    }

                                                    // ID validation
                                                    val id = when (val idValue = item["id"]) {
                                                        null -> {
                                                            errorMessages.add("ID is null")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        !is Number -> {
                                                            errorMessages.add("Invalid ID format: $idValue")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        else -> idValue.toInt()
                                                    }

                                                    AppLogger.d(id.toString())
                                                    if (id <= 0) {
                                                        errorMessages.add("Invalid ID value: $id")
                                                        hasError = true
                                                        return@forEach
                                                    }

                                                    try {
                                                        absensiViewModel.archiveAbsensiById(id)
                                                        successCount++
                                                    } catch (e: SQLiteException) {
                                                        errorMessages.add("Database error for ID $id: ${e.message}")
                                                        hasError = true
                                                    } catch (e: Exception) {
                                                        errorMessages.add("Error archiving ID $id: ${e.message}")
                                                        hasError = true
                                                    }

                                                } catch (e: Exception) {
                                                    errorMessages.add("Unexpected error processing item: ${e.message}")
                                                    hasError = true
                                                }
                                            }

                                            // Show results
                                            withContext(Dispatchers.Main) {
                                                try {
                                                    loadingDialog.dismiss()

                                                    when {
                                                        successCount == 0 -> {
                                                            val errorDetail =
                                                                errorMessages.joinToString("\n")
                                                            AppLogger.e("Archive failed. Errors:\n$errorDetail")
                                                            Toast.makeText(
                                                                this@ListAbsensiActivity,
                                                                "Gagal mengarsipkan data",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        hasError -> {
                                                            val errorDetail =
                                                                errorMessages.joinToString("\n")
                                                            AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                            Toast.makeText(
                                                                this@ListAbsensiActivity,
                                                                "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        else -> {
                                                            AppLogger.d("All items archived successfully")
                                                            Toast.makeText(
                                                                this@ListAbsensiActivity,
                                                                "Semua data berhasil diarsipkan",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    dialog.dismiss()
                                                } catch (e: Exception) {
                                                    AppLogger.e("Error in UI update: ${e.message}")
                                                    Toast.makeText(
                                                        this@ListAbsensiActivity,
                                                        "Terjadi kesalahan pada UI",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                        } catch (e: Exception) {
                                            AppLogger.e("Fatal error in archiving process: ${e.message}")
                                            withContext(Dispatchers.Main) {
                                                try {
                                                    loadingDialog.dismiss()
                                                    Toast.makeText(
                                                        this@ListAbsensiActivity,
                                                        "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    dialog.dismiss()
                                                } catch (dialogException: Exception) {
                                                    AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                                }
                                            }
                                        }

                                        absensiViewModel.loadActiveAbsensi()
                                        absensiViewModel.loadAbsensiCountArchive(1)
                                    }
                                },
                                cancelFunction = {}
                            )
                        }
                        AppLogger.d("test $mappedData")
                        val jsonData = formatPanenDataForQR(mappedData)
                        AppLogger.d("data json $jsonData")
                        val encodedData =
                            encodeJsonToBase64ZipQR(jsonData)
                                ?: throw Exception("Encoding failed")
                        AppLogger
                        withContext(Dispatchers.Main) {
                            try {

                                ListPanenTBSActivity().generateHighQualityQRCode(encodedData!!, qrCodeImageView,
                                    this@ListAbsensiActivity,
                                    showLogo = false)
                                // Fade-out the loading elements
                                val fadeOut =
                                    ObjectAnimator.ofFloat(loadingLogo, "alpha", 1f, 0f)
                                        .apply {
                                            duration = 250
                                        }
                                val fadeOutDots =
                                    ObjectAnimator.ofFloat(
                                        loadingContainer,
                                        "alpha",
                                        1f,
                                        0f
                                    )
                                        .apply {
                                            duration = 250
                                        }

                                // Ensure QR code, text, and button start fully invisible
                                qrCodeImageView.alpha = 0f
                                dashedLine.alpha = 0f
                                tvTitleQRGenerate.alpha = 0f
                                titleQRConfirm.alpha = 0f
                                descQRConfirm.alpha = 0f
                                btnConfirmScanAbsensi.alpha = 0f
                                btnPreviewFullQR.alpha = 0f

                                // Fade-in animations
                                val fadeInQR =
                                    ObjectAnimator.ofFloat(qrCodeImageView, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInDashedLine =
                                    ObjectAnimator.ofFloat(dashedLine, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInTitle =
                                    ObjectAnimator.ofFloat(
                                        tvTitleQRGenerate,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInButtonPreviewBtn =
                                    ObjectAnimator.ofFloat(
                                        btnPreviewFullQR,
                                        "alpha",
                                        0f,
                                        1f
                                    ).apply {
                                        duration = 250
                                        startDelay = 150
                                    }
                                // Create fade-in animation for QR code and dashed line
                                val fadeIn =
                                    ObjectAnimator.ofFloat(qrCodeImageView, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                tvTitleQRGenerate.alpha =
                                    0f  // Ensure title starts invisible

                                // Create fade-in for the dataQR text as well
                                val fadeInText =
                                    ObjectAnimator.ofFloat(dataQR, "alpha", 0f, 1f).apply {
                                        duration = 250
                                        startDelay = 150
                                    }
                                val fadeInTitleConfirm =
                                    ObjectAnimator.ofFloat(titleQRConfirm, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInDescConfirm =
                                    ObjectAnimator.ofFloat(descQRConfirm, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                val fadeInButton =
                                    ObjectAnimator.ofFloat(
                                        btnConfirmScanAbsensi,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                // Run animations together
                                AnimatorSet().apply {
                                    playTogether(fadeOut, fadeOutDots)
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            // Hide loading elements
                                            loadingLogo.visibility = View.GONE
                                            loadingContainer.visibility = View.GONE

                                            // Show elements and start fade-in
                                            tvTitleQRGenerate.visibility = View.VISIBLE
                                            qrCodeImageView.visibility = View.VISIBLE
                                            dashedLine.visibility = View.VISIBLE
                                            titleQRConfirm.visibility = View.VISIBLE
                                            descQRConfirm.visibility = View.VISIBLE
                                            btnConfirmScanAbsensi.visibility = View.VISIBLE
                                            btnPreviewFullQR.visibility = View.VISIBLE

                                            fadeInQR.start()
                                            fadeInDashedLine.start()
                                            fadeInTitle.start()
                                            fadeInText.start()
                                            fadeInTitleConfirm.start()
                                            fadeInDescConfirm.start()
                                            fadeInButton.start()
                                            fadeInButtonPreviewBtn.start()
                                        }
                                    })
                                    start()
                                }

                            } catch (e: Exception) {
                                loadingLogo.animation?.cancel()
                                loadingLogo.clearAnimation()
                                loadingLogo.visibility = View.GONE
                                loadingContainer.visibility = View.GONE
                                AppLogger.e("QR Generation Error: ${e.message}")
                                showErrorMessageGenerateQR(
                                    view,
                                    "Error Generating QR code: ${e.message}"
                                )
                            }
                        }


                    } catch (e: Exception) {
                        AppLogger.e("Error in QR process: ${e.message}")
                        withContext(Dispatchers.Main) {
                            stopLoadingAnimation(loadingLogo, loadingContainer)
                            showErrorMessageGenerateQR(
                                view,
                                "Error Processing QR code: ${e.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error in QR process: ${e.message}")
            }
        }
    }

    fun showErrorMessageGenerateQR(view: View, message: String) {
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }

    private fun takeQRCodeScreenshot(view: View) {
        lifecycleScope.launch {
            try {
                // First check if we have data
                if (mappedData.isEmpty()) {
                    AppLogger.e("mappedData is empty")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ListAbsensiActivity,
                            "Tidak ada data untuk tangkapan layar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                AppLogger.d("mappedData $mappedData")

                // Inflate the correct screenshot layout
                val screenshotLayout: View =
                    layoutInflater.inflate(R.layout.layout_screenshot_qr_absensi, null)

                AppLogger.d("Inflated layout_screenshot_qr_absensi layout")

                // Get QR code from the bottom sheet view
                val originalQrImageView = view.findViewById<ImageView>(R.id.qrCodeImageView)
                if (originalQrImageView == null || originalQrImageView.drawable == null) {
                    AppLogger.e("Original QR ImageView or drawable is null")
                    return@launch
                }

                // Get references to views in the screenshot layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView = screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
                val tvFooter = screenshotLayout.findViewById<TextView>(R.id.tvFooter)


                // Check that we found the essential views
                if (tvUserName == null) AppLogger.e("tvUserName not found")
                if (qrCodeImageView == null) AppLogger.e("qrCodeImageView not found")
                if (tvFooter == null) AppLogger.e("tvFooter not found")

                if (tvUserName == null || qrCodeImageView == null || tvFooter == null) {
                    AppLogger.e("Essential views not found in screenshot layout")
                    return@launch
                }

                // Find the included layout views - log each ID for debugging
                AppLogger.d("Looking for info layouts in screenshot_qr_absensi.xml")

                // Let's print the resource IDs to make sure we're looking for the right ones
                val infoKemandoranId = resources.getIdentifier("infoKemandoran", "id", packageName)
                val infoTotalTransaksiId = resources.getIdentifier("infoTotalTransaksi", "id", packageName)

                // Try to find the included layout views using resource IDs
                val infoKemandoran = screenshotLayout.findViewById<View>(infoKemandoranId)
                    ?: screenshotLayout.findViewById<View>(R.id.infoKemandoran) // Fallback ID

                val infoTotalTransaksi = screenshotLayout.findViewById<View>(infoTotalTransaksiId)
                    ?: screenshotLayout.findViewById<View>(R.id.infoKehadiran) // Fallback ID

                // Add references for new info views
                val infoUrutanKe = screenshotLayout.findViewById<View>(R.id.infoUrutanKe)
                val infoJamTanggal = screenshotLayout.findViewById<View>(R.id.infoJamTanggal)

                // Log whether we found the views
                AppLogger.d("infoKemandoran found: ${infoKemandoran != null}")
                AppLogger.d("infoTotalTransaksi found: ${infoTotalTransaksi != null}")
                AppLogger.d("infoUrutanKe found: ${infoUrutanKe != null}")
                AppLogger.d("infoJamTanggal found: ${infoJamTanggal != null}")

                if (infoKemandoran == null || infoTotalTransaksi == null) {
                    AppLogger.e("One or more info layout views not found - falling back to direct search")

                    // Last ditch effort - search for any view that has tvLabel and tvValue
                    val infoViews = findViewsWithLabels(screenshotLayout as ViewGroup)
                    AppLogger.d("Found ${infoViews.size} potential info views with tvLabel and tvValue")

                    if (infoViews.size >= 3) {
                        AppLogger.d("Using dynamically found info views instead")
                        // We'll use these views instead
                        processScreenshotWithFoundViews(
                            screenshotLayout,
                            tvUserName,
                            qrCodeImageView,
                            tvFooter,
                            infoViews[0],
                            infoViews[1],
                            infoViews[2],
                            originalQrImageView
                        )
                        return@launch
                    } else {
                        AppLogger.e("Not enough info views found automatically - screenshot may be incomplete")
                        // Continue with what we have - some views might be missing
                    }
                }

                // Helper function to set info data
                fun setInfoData(includeView: View?, labelText: String, valueText: String) {
                    if (includeView == null) {
                        AppLogger.e("Cannot set data for null includeView: $labelText")
                        return
                    }

                    val tvLabel = includeView.findViewById<TextView>(R.id.tvLabel)
                    val tvValue = includeView.findViewById<TextView>(R.id.tvValue)

                    if (tvLabel == null) AppLogger.e("tvLabel not found in includeView for $labelText")
                    if (tvValue == null) AppLogger.e("tvValue not found in includeView for $labelText")

                    if (tvLabel != null && tvValue != null) {
                        tvLabel.text = labelText
                        tvValue.text = valueText
                    }
                }

                // Convert QR drawable to bitmap
                val qrBitmap = if (originalQrImageView.drawable is BitmapDrawable) {
                    (originalQrImageView.drawable as BitmapDrawable).bitmap
                } else {
                    try {
                        val drawable = originalQrImageView.drawable
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    } catch (e: Exception) {
                        AppLogger.e("Error converting drawable to bitmap: ${e.message}")
                        return@launch
                    }
                }

                // Set the QR bitmap in our screenshot layout
                qrCodeImageView.setImageBitmap(qrBitmap)

                // Process the data for display
                val estateAfdeling = if (mappedData.isNotEmpty()) {
                    val firstData = mappedData[0]
                    val estate = firstData["dept_abbr"]?.toString() ?: "Unknown"
                    val afdeling = firstData["divisi_abbr"]?.toString() ?: "Unknown"
                    "$estate/$afdeling"
                } else {
                    "Unknown/Unknown"
                }

                var kemandoranValue = ""
                if (mappedData.isNotEmpty() && mappedData[0]["nama_kemandoran"] != null) {
                    val namaKemandoran = mappedData[0]["nama_kemandoran"]

                    // Convert to list of strings regardless of original type
                    val kemandoranList = when (namaKemandoran) {
                        is List<*> -> namaKemandoran.filterNotNull().map { it.toString() }
                        is Array<*> -> namaKemandoran.filterNotNull().map { it.toString() }
                        is String -> {
                            if (namaKemandoran.contains(",")) {
                                namaKemandoran.split(",").filter { it.isNotEmpty() }
                            } else {
                                listOf(namaKemandoran)
                            }
                        }
                        else -> listOf(namaKemandoran.toString())
                    }

                    // Format based on number of items
                    kemandoranValue = if (kemandoranList.size > 1) {
                        val bulletList = kemandoranList.joinToString("\n") { "• $it" }
                        ":\n$bulletList"
                    } else if (kemandoranList.size == 1) {
                        ": ${kemandoranList[0]}"
                    } else {
                        ": -"
                    }
                } else {
                    kemandoranValue = ": -"
                }

                // Calculate attendance
                var totalMasuk = 0
                var totalTidakMasuk = 0

                for (data in mappedData) {
                    val karyawanMskNik = data["karyawan_msk_nik"]?.toString() ?: ""
                    val karyawanTdkMskNik = data["karyawan_tdk_msk_nik"]?.toString() ?: ""

                    totalMasuk += karyawanMskNik.split(",").filter { it.isNotEmpty() }.size
                    totalTidakMasuk += karyawanTdkMskNik.split(",").filter { it.isNotEmpty() }.size
                }

                val totalKaryawan = totalMasuk + totalTidakMasuk
                val totalAttendance = "$totalMasuk / $totalKaryawan"
                AppLogger.d("Total attendance: $totalAttendance")

                // Generate current date and time for footer and counter
                val currentDate = Date()
                val indonesianLocale = Locale("id", "ID")
                val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val formattedDate = dateFormat.format(currentDate).uppercase(indonesianLocale)
                val formattedTime = timeFormat.format(currentDate)

                // Get and increment screenshot counter for this specific feature
                val screenshotNumber = getAndIncrementScreenshotCounter()

                // Set user info
                tvUserName.text =
                    "Hasil QR ${featureName} dari ${prefManager?.jabatanUserLogin ?: "Unknown"} - ${estateAfdeling}"

                // Set footer with date/time
                tvFooter.text =
                    "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

                // Set info data
                setInfoData(infoKemandoran, "Kemandoran", kemandoranValue)
                setInfoData(infoTotalTransaksi, "Kehadiran", ": $totalAttendance")

                // Add new info data with counter and timestamp
                setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                setInfoData(infoJamTanggal, "Jam & Tanggal", ": $formattedTime, $formattedDate")

                // Take the screenshot
                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels

                screenshotLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                screenshotLayout.layout(
                    0, 0, screenshotLayout.measuredWidth, screenshotLayout.measuredHeight
                )

                val screenshotFileName = "Absensi_QR"
                val watermarkType = AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen

                AppLogger.d("Taking screenshot now")
                val screenshotFile = ScreenshotUtil.takeScreenshot(
                    screenshotLayout,
                    screenshotFileName,
                    watermarkType
                )

                if (screenshotFile != null) {
                    AppLogger.d("Screenshot saved successfully: ${screenshotFile.absolutePath}")
                    withContext(Dispatchers.Main) {
                        Toasty.success(
                            this@ListAbsensiActivity,
                            "QR sudah tersimpan digaleri",
                            Toast.LENGTH_LONG,
                            true
                        ).show()
                    }
                } else {
                    AppLogger.e("Screenshot file is null")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ListAbsensiActivity,
                            "Gagal menyimpan QR Code: Screenshot file is null",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error taking QR screenshot: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ListAbsensiActivity,
                        "Gagal menyimpan QR Code: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getAndIncrementScreenshotCounter(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefManager!!.getScreenshotDate(featureName!!)
        val currentCounter = prefManager!!.getScreenshotCounter(featureName!!)

        return if (lastDate != today) {
            // Reset counter for new day
            prefManager!!.setScreenshotDate(featureName!!, today)
            prefManager!!.setScreenshotCounter(featureName!!, 1)
            1
        } else {
            // Increment counter for same day
            val newCounter = currentCounter + 1
            prefManager!!.setScreenshotCounter(featureName!!, newCounter)
            newCounter
        }
    }

    // Helper method to process screenshot with dynamically found views
    private suspend fun processScreenshotWithFoundViews(
        screenshotLayout: View,
        tvUserName: TextView,
        qrCodeImageView: ImageView,
        tvFooter: TextView,
        infoView1: View,
        infoView2: View,
        infoView3: View,
        originalQrImageView: ImageView
    ) {
        try {
            // Convert QR drawable to bitmap
            val qrBitmap = if (originalQrImageView.drawable is BitmapDrawable) {
                (originalQrImageView.drawable as BitmapDrawable).bitmap
            } else {
                val drawable = originalQrImageView.drawable
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            // Set the QR bitmap in our screenshot layout
            qrCodeImageView.setImageBitmap(qrBitmap)

            // Helper function to set info data
            fun setInfoData(view: View, labelText: String, valueText: String) {
                val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
                val tvValue = view.findViewById<TextView>(R.id.tvValue)

                if (tvLabel != null && tvValue != null) {
                    tvLabel.text = labelText
                    tvValue.text = valueText
                }
            }

            // Process the data for display
            val estateAfdeling = mappedData.joinToString("\n") {
                "${it["estate"] ?: "Unknown"}/${it["afdeling"] ?: "Unknown"}"
            }

            // Extract kemandoran list
            val kemandoranIds = mutableListOf<String>()
            for (data in mappedData) {
                val idKemandoran = data["id_kemandoran"]
                when (idKemandoran) {
                    is List<*> -> {
                        idKemandoran.forEach { id ->
                            if (id != null) kemandoranIds.add(id.toString())
                        }
                    }

                    is Array<*> -> {
                        idKemandoran.forEach { id ->
                            if (id != null) kemandoranIds.add(id.toString())
                        }
                    }

                    is String -> {
                        // If it's a string, it might be a single ID or a comma-separated list
                        if (idKemandoran.contains(",")) {
                            kemandoranIds.addAll(idKemandoran.split(",").filter { it.isNotEmpty() })
                        } else {
                            kemandoranIds.add(idKemandoran)
                        }
                    }

                    else -> {
                        if (idKemandoran != null) kemandoranIds.add(idKemandoran.toString())
                    }
                }
            }

            val kemandoranList = kemandoranIds.distinct().joinToString("\n")

            // Calculate attendance
            var totalMasuk = 0
            var totalTidakMasuk = 0

            for (data in mappedData) {
                val karyawanMskId = data["karyawan_msk_id"]?.toString() ?: ""
                val karyawanTdkMskId = data["karyawan_tdk_msk_id"]?.toString() ?: ""

                totalMasuk += karyawanMskId.split(",").filter { it.isNotEmpty() }.size
                totalTidakMasuk += karyawanTdkMskId.split(",").filter { it.isNotEmpty() }.size
            }

            val totalAttendance = "$totalMasuk / $totalTidakMasuk"

            // Set user info
            tvUserName.text =
                "Hasil QR dari ${prefManager?.jabatanUserLogin ?: "Unknown"} - ${prefManager?.estateUserLogin ?: "Unknown"}"

            // Set footer with date/time
            val currentDate = Date()
            val indonesianLocale = Locale("id", "ID")
            val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val formattedDate = dateFormat.format(currentDate).toUpperCase(indonesianLocale)
            val formattedTime = timeFormat.format(currentDate)

            tvFooter.text =
                "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

            // Set the data in the info views
            setInfoData(infoView1, "Est/Afd", ": $estateAfdeling")
            setInfoData(infoView2, "Kemandoran", ": $kemandoranList")
            setInfoData(infoView3, "Total Hadir / Tidak Hadir", ": $totalAttendance")

            // Take the screenshot
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels

            screenshotLayout.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            screenshotLayout.layout(
                0, 0, screenshotLayout.measuredWidth, screenshotLayout.measuredHeight
            )

            // Create a filename and take the screenshot
            val screenshotFileName = "Absensi_QR"

            val watermarkType = if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen)
                AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
            else if (featureName == AppUtils.ListFeatureNames.RekapAbsensiPanen) {
                AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
            } else
                AppUtils.WaterMarkFotoDanFolder.WMESPB

            AppLogger.d("Taking screenshot now")
            val screenshotFile = ScreenshotUtil.takeScreenshot(
                screenshotLayout,
                screenshotFileName,
                watermarkType
            )

            if (screenshotFile != null) {
                AppLogger.d("Screenshot saved successfully: ${screenshotFile.absolutePath}")
                withContext(Dispatchers.Main) {
                    Toasty.success(
                        this@ListAbsensiActivity,
                        "QR sudah tersimpan digaleri",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                }
            } else {
                AppLogger.e("Screenshot file is null")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ListAbsensiActivity,
                        "Gagal menyimpan QR Code: Screenshot file is null",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error processing with found views: ${e.message}")
            e.printStackTrace()
        }
    }

    // Helper function to find views that have both tvLabel and tvValue TextViews
    private fun findViewsWithLabels(viewGroup: ViewGroup): List<View> {
        val result = mutableListOf<View>()

        // Check if this ViewGroup has both tvLabel and tvValue
        val hasLabel = viewGroup.findViewById<TextView>(R.id.tvLabel) != null
        val hasValue = viewGroup.findViewById<TextView>(R.id.tvValue) != null

        if (hasLabel && hasValue) {
            result.add(viewGroup)
        }

        // Check all children recursively
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                result.addAll(findViewsWithLabels(child))
            }
        }

        return result
    }

    private fun showQrCodeFullScreen(qrDrawable: Drawable?, bottomSheetView: View) {
        if (qrDrawable == null) return

        // Get the bottom sheet behavior to control it
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView.parent as View)

        // Save current state to restore later
        val previousState = bottomSheetBehavior.state

        // Expand bottom sheet fully first
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Create a dialog to display the QR code
        val context = bottomSheetView.context
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Make dialog dismissible with back button
        dialog.setCancelable(true)
        dialog.setOnCancelListener {
            // Restore previous bottom sheet state when dismissed with back button
            bottomSheetBehavior.state = previousState
        }

        // Inflate the camera_edit layout
        val fullscreenView = layoutInflater.inflate(R.layout.camera_edit, null)
        dialog.setContentView(fullscreenView)

        // Find views within the dialog layout
        val fotoLayout = fullscreenView.findViewById<ConstraintLayout>(R.id.clZoomLayout)
        val photoView = fullscreenView.findViewById<PhotoView>(R.id.fotoZoom)
        val closeZoomCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardCloseZoom)
        val changePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardChangePhoto)
        val deletePhotoCard = fullscreenView.findViewById<MaterialCardView>(R.id.cardDeletePhoto)

        // Find the TextView and ImageView for color changes
        val tvCardCloseButton = fullscreenView.findViewById<TextView>(R.id.tvCardCloseButton)
        val closeZoomIcon = fullscreenView.findViewById<ImageView>(R.id.closeZoom)

        // Set the image to the PhotoView
        photoView.setImageDrawable(qrDrawable)

        // Hide edit options
        changePhotoCard.visibility = View.GONE
        deletePhotoCard.visibility = View.GONE

        // Set background color of the layout to white using your color resource
        fotoLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white))

        val closeCardLinearLayout = closeZoomCard.getChildAt(0) as LinearLayout
        closeCardLinearLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.greenDarker))

        // Change the text color to white
        tvCardCloseButton.setTextColor(ContextCompat.getColor(context, R.color.white))

        // Change the close icon tint to white
        closeZoomIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))

        // Set up close button to restore previous bottom sheet state
        closeZoomCard.setOnClickListener {
            dialog.dismiss()
            // Restore previous bottom sheet state
            bottomSheetBehavior.state = previousState
        }

        // Make dialog display properly
        dialog.window?.apply {
            // Set window background to white using your color resource
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.white)))
            setDimAmount(0f) // Remove dimming since we have a white background

            // This is important - use TYPE_APPLICATION to ensure it appears above the bottom sheet
            attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION

            // Make sure to set the layout flags properly
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // Add FLAG_NOT_TOUCH_MODAL to make sure it gets all touch events
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

            // This helps ensure it appears on top
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Check if the input is a JSON array or object and handle accordingly
            val minifiedJson = if (jsonData.trim().startsWith("[")) {
                // It's a JSON array
                JSONArray(jsonData).toString()
            } else {
                // It's a JSON object
                JSONObject(jsonData).toString()
            }

            // Reject empty JSON
            if (minifiedJson == "{}" || minifiedJson == "[]") {
                AppLogger.e("Empty JSON detected, returning null")
                throw IllegalArgumentException("Empty JSON detected")
            }

            // Create a byte array output stream to hold the zip data
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                ZipOutputStream(byteArrayOutputStream).apply {
                    setLevel(Deflater.BEST_COMPRESSION)
                }.use { zipOutputStream ->
                    val entry = ZipEntry("output.json")
                    zipOutputStream.putNextEntry(entry)
                    zipOutputStream.write(minifiedJson.toByteArray(StandardCharsets.UTF_8))
                    zipOutputStream.closeEntry()
                }

                val zipBytes = byteArrayOutputStream.toByteArray()
                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)
                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                firstHalf + "5nqHzPKdlILxS9ABpClq" + secondHalf
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            throw IllegalArgumentException(e.message.toString())
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        }
    }

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data Absensi is empty.")
            }

            val allKemandoran = mutableSetOf<String>()
            val deptSet = mutableSetOf<String>()
            val deptAbbrSet = mutableSetOf<String>()
            val divisiSet = mutableSetOf<String>()
            val divisiAbbrSet = mutableSetOf<String>()
            val createdBySet = mutableSetOf<String>()
            val datetimeSet = mutableSetOf<String>()

            // Changed to use JSONObject for proper structure
            val mergedKaryawanMskNik = JSONObject()
            val mergedKaryawanTdkMskNik = JSONObject()
            val mergedKaryawanMskId = JSONObject()
            val mergedKaryawanTdkMskId = JSONObject()
            val mergedKaryawanMskNama = JSONObject()
            val mergedKaryawanTdkMskNama = JSONObject()

            val infoSet = mutableSetOf<String>()

            mappedData.forEach { data ->
                val idKemandoran = data["id_kemandoran"]?.toString()
                    ?: throw IllegalArgumentException("Missing id_kemandoran.")

                val deptAbbr = data["dept_abbr"]?.toString()?.trim() ?: ""
                val divisiAbbr = data["divisi_abbr"]?.toString()?.trim() ?: ""
                val dept = data["dept"]?.toString()?.trim() ?: deptAbbr
                val divisi = data["divisi"]?.toString()?.trim() ?: divisiAbbr
                val createdBy = data["created_by"]?.toString() ?: ""
                val dateAbsen = data["datetime"]?.toString() ?: ""
                val info = data["info"]?.toString() ?: ""

                // Get JSON strings for karyawan data
                val karyawanMskNik = data["karyawan_msk_nik"]?.toString() ?: ""
                val karyawanTdkMskNik = data["karyawan_tdk_msk_nik"]?.toString() ?: ""
                val karyawanMskId = data["karyawan_msk_id"]?.toString() ?: ""
                val karyawanTdkMskId = data["karyawan_tdk_msk_id"]?.toString() ?: ""
                val karyawanMskNama = data["karyawan_msk_nama"]?.toString() ?: ""
                val karyawanTdkMskNama = data["karyawan_tdk_msk_nama"]?.toString() ?: ""

                // Process id_kemandoran
                idKemandoran.removeSurrounding("[", "]").split(",")
                    .forEach { allKemandoran.add(it.trim()) }

                // Merge JSON objects properly using the correct function
                mergeJsonObjects(karyawanMskNik, mergedKaryawanMskNik)
                mergeJsonObjects(karyawanTdkMskNik, mergedKaryawanTdkMskNik)
                mergeJsonObjects(karyawanMskId, mergedKaryawanMskId)
                mergeJsonObjects(karyawanTdkMskId, mergedKaryawanTdkMskId)
                mergeJsonObjects(karyawanMskNama, mergedKaryawanMskNama)
                mergeJsonObjects(karyawanTdkMskNama, mergedKaryawanTdkMskNama)

                // Add other data
                if (dept.isNotEmpty()) deptSet.add(dept)
                if (deptAbbr.isNotEmpty()) deptAbbrSet.add(deptAbbr)
                if (dateAbsen.isNotEmpty()) datetimeSet.add(dateAbsen)

                if (divisi.isNotEmpty()) {
                    divisi.split("\n").forEach { divisiSet.add(it.trim()) }
                }
                if (divisiAbbr.isNotEmpty()) {
                    divisiAbbr.split("\n").forEach { divisiAbbrSet.add(it.trim()) }
                }

                if (createdBy.isNotEmpty()) createdBySet.add(createdBy)
                if (info.isNotEmpty()) infoSet.add(info)
            }

            // Final JSON with proper structure
            val jsonObject = JSONObject().apply {
                put("id_kemandoran", JSONArray(allKemandoran))
                put("datetime", JSONArray(datetimeSet))
                put("dept", JSONArray(deptSet))
                put("dept_abbr", JSONArray(deptAbbrSet))
                put("divisi", JSONArray(divisiSet))
                put("divisi_abbr", JSONArray(divisiAbbrSet))
                put("created_by", JSONArray(createdBySet))

                // Store as JSON objects, not arrays
                put("karyawan_msk_nik", mergedKaryawanMskNik)
                put("karyawan_tdk_msk_nik", mergedKaryawanTdkMskNik)
                put("karyawan_msk_id", mergedKaryawanMskId)
                put("karyawan_tdk_msk_id", mergedKaryawanTdkMskId)
                put("karyawan_msk_nama", mergedKaryawanMskNama)
                put("karyawan_tdk_msk_nama", mergedKaryawanTdkMskNama)

                put("info", JSONArray(infoSet))
            }

            AppLogger.d("cek json object: $jsonObject")
            jsonObject.toString()

        } catch (e: Exception) {
            throw IllegalArgumentException("formatPanenDataForQR Error: ${e.message}")
        }
    }

    // Helper function to merge JSON objects
    private fun mergeJsonObjects(jsonString: String, targetObject: JSONObject) {
        if (jsonString.isNotEmpty()) {
            try {
                val sourceJson = JSONObject(jsonString)
                val statusKeys = sourceJson.keys()

                while (statusKeys.hasNext()) {
                    val statusKey = statusKeys.next()
                    val statusValue = sourceJson.opt(statusKey)

                    when (statusValue) {
                        is JSONObject -> {
                            // Handle nested structure: {"status": {"kemandoran": "values"}}
                            if (!targetObject.has(statusKey)) {
                                targetObject.put(statusKey, JSONObject())
                            }

                            val targetStatusObject = targetObject.getJSONObject(statusKey)
                            val kemandoranKeys = statusValue.keys()

                            while (kemandoranKeys.hasNext()) {
                                val kemandoranKey = kemandoranKeys.next()
                                val kemandoranValue = statusValue.getString(kemandoranKey)

                                if (targetStatusObject.has(kemandoranKey)) {
                                    // Merge values if key already exists
                                    val existingValue = targetStatusObject.getString(kemandoranKey)
                                    val mergedValue = "$existingValue,$kemandoranValue"
                                    targetStatusObject.put(kemandoranKey, mergedValue)
                                } else {
                                    targetStatusObject.put(kemandoranKey, kemandoranValue)
                                }
                            }
                        }
                        is String -> {
                            // Handle flat structure: {"kemandoran": "values"}
                            if (targetObject.has(statusKey)) {
                                val existingValue = targetObject.getString(statusKey)
                                val mergedValue = "$existingValue,$statusValue"
                                targetObject.put(statusKey, mergedValue)
                            } else {
                                targetObject.put(statusKey, statusValue)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error merging JSON objects: ${e.message}")
            }
        }
    }

    // Helper function to extract values from JSON string and add to set
    private fun extractAndAddFromJson(jsonString: String, targetSet: MutableSet<String>) {
        try {
            if (jsonString.isNotEmpty() && jsonString.startsWith("{")) {
                val jsonObj = JSONObject(jsonString)

                // Handle nested JSON structure: {"status": {"kemandoran": "values"}}
                jsonObj.keys().forEach { statusKey ->
                    val statusValue = jsonObj.opt(statusKey)

                    when (statusValue) {
                        is JSONObject -> {
                            // This is a nested object (status -> kemandoran -> values)
                            statusValue.keys().forEach { kemandoranKey ->
                                val kemandoranValue = statusValue.optString(kemandoranKey, "")
                                if (kemandoranValue.isNotEmpty()) {
                                    kemandoranValue.split(",").filter { it.trim().isNotEmpty() }
                                        .forEach { targetSet.add(it.trim()) }
                                }
                            }
                        }
                        is String -> {
                            // This is a direct string value (old format)
                            if (statusValue.isNotEmpty()) {
                                statusValue.split(",").filter { it.trim().isNotEmpty() }
                                    .forEach { targetSet.add(it.trim()) }
                            }
                        }
                    }
                }
            } else {
                // Handle old format (comma-separated values)
                if (jsonString.isNotEmpty()) {
                    jsonString.split(",").filter { it.trim().isNotEmpty() }
                        .forEach { targetSet.add(it.trim()) }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error extracting from JSON: ${e.message}")
            // Fallback to treat as comma-separated string
            if (jsonString.isNotEmpty()) {
                jsonString.split(",").filter { it.trim().isNotEmpty() }
                    .forEach { targetSet.add(it.trim()) }
            }
        }
    }


    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data tersimpan...")
            // Reset visibility states before loading new data
            tvEmptyStateAbsensi.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            absensiAdapter.updateArchiveState(currentState)
            if (prefManager!!.jabatanUserLogin == "Kerani Panen") {
                absensiViewModel.getAllDataAbsensi(1)
            } else {
                absensiViewModel.getAllDataAbsensi(0)
            }

        }

        cardTerscan.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data terscan...")
            // Reset visibility states before loading new data
            tvEmptyStateAbsensi.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            absensiAdapter.updateArchiveState(currentState)
            absensiViewModel.loadArchivedAbsensi()
        }
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.card_item_tersimpanAbsensi)
        cardTerscan = findViewById(R.id.card_item_terscanAbsensi)
        counterTersimpan = findViewById(R.id.counter_item_tersimpanAbsensi)
        counterTerscan = findViewById(R.id.counter_item_terscanAbsensi)
        tvEmptyStateAbsensi = findViewById(R.id.tvEmptyStateAbsensiList)
        speedDial = findViewById(R.id.dial_listAbsensi)
    }

    private fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
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

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }

    fun formatToIndonesianDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr) ?: return dateStr
            val outputFormat =
                SimpleDateFormat("d MMMM yyyy", Locale("id")) // Example: 6 Maret 2025
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr // Return original string in case of error
        }
    }

    fun formatGlobalDate(dateString: String): String {
        // Parse the date string in format "YYYY-MM-DD"
        val parts = dateString.split("-")
        if (parts.size != 3) return dateString // Return original if format doesn't match

        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        // Return formatted date string using getMonthFormat
        return "${AppUtils.getMonthFormat(month)} $day $year"
    }

    @SuppressLint("SetTextI18n")
    private fun setupObserveData() {
        val listTgl = findViewById<TextView>(R.id.listTglAbsensi)
        val listLokasi = findViewById<TextView>(R.id.lokasiKerja)
        val totalKehadiran = findViewById<TextView>(R.id.totalKehadiranAbsensi)
        val dialUploadAbsensi = findViewById<SpeedDialView>(R.id.dial_listAbsensi)
        val tglAbsensi = findViewById<LinearLayout>(R.id.ll_tglRekapAbsensi)
        val tglSection = findViewById<LinearLayout>(R.id.tglAbsensi)
        val totakKehadiranSection = findViewById<LinearLayout>(R.id.total_sectionAbsensi)
        val btnGenerateQRAbsensi = findViewById<FloatingActionButton>(R.id.btnGenerateQRAbsensi)

        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        absensiViewModel.archivedCount.observe(this) { count ->
            counterTerscan.text = count.toString()
        }

        absensiViewModel.savedDataAbsensiList.observe(this) { data ->
            if (currentState == 0 || currentState == 1) {
                absensiAdapter.updateList(emptyList())

                if (data.isNotEmpty()) {
                    val dateAbsen =
                        data.first().absensi.date_absen // Get date_absen from first item
                    val formattedDate =
                        formatToIndonesianDate(dateAbsen) // Format it to Indonesian date
                    val tglAbsensi = findViewById<TextView>(R.id.tvTglAbsensi)
                    tglAbsensi.text = formattedDate

                    speedDial.visibility = View.VISIBLE
                    tvEmptyStateAbsensi.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    lifecycleScope.launch {
                        try {
                            val filteredData = withContext(Dispatchers.IO) {
                                try {
                                    AppLogger.d("Processing absensi data: ${data.size} items")

                                    // Use a more straightforward approach with fewer nested coroutines
                                    data.map { absensiWithRelations ->
                                        try {
                                            // Extract kemandoran IDs
                                            val rawKemandoran =
                                                absensiWithRelations.absensi.kemandoran_id
                                                    .split(",")
                                                    .map { it.trim() }
                                                    .filter { it.isNotEmpty() }
                                                    .takeIf { it.isNotEmpty() } ?: emptyList()

                                            AppLogger.d("Processing item ID ${absensiWithRelations.absensi.id} with kemandoran IDs: $rawKemandoran")

                                            // Get kemandoran data safely
                                            val kemandoranData = try {
                                                absensiViewModel.getKemandoranById(rawKemandoran)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching kemandoran data for IDs $rawKemandoran: ${e.message}")
                                                null
                                            }

                                            AppLogger.d("Kemandoran data for ID ${absensiWithRelations.absensi.id}: ${kemandoranData?.map { it.id }}")
                                            AppLogger.d("kemandoranData $kemandoranData")
                                            // Process kemandoran names
                                            val kemandoranKode = if (kemandoranData != null && kemandoranData.isNotEmpty()) {
                                                val kodes = kemandoranData.mapNotNull { it.kode }
                                                if (kodes.isNotEmpty()) {
                                                    kodes.joinToString("\n")
                                                } else {
                                                    "-"
                                                }
                                            } else {
                                                "-"
                                            }

                                            val kemandoranNama = if (kemandoranData != null && kemandoranData.isNotEmpty()) {
                                                val kodes = kemandoranData.mapNotNull { it.nama }
                                                if (kodes.isNotEmpty()) {
                                                    kodes.joinToString("\n")
                                                } else {
                                                    "-"
                                                }
                                            } else {
                                                "-"
                                            }

                                            AppLogger.d("Final kemandoranNama using alternative method: $kemandoranNama")

                                            // Add to mappedData (safely handling nulls)
                                            mappedData = mappedData + mapOf(
                                                "id_kemandoran" to (rawKemandoran.takeIf { it.isNotEmpty() }
                                                    ?: listOf("-")),
                                                "id" to absensiWithRelations.absensi.id,
                                                "nama_kemandoran" to kemandoranNama,
                                                "divisi" to absensiWithRelations.absensi.divisi,
                                                "divisi_abbr" to absensiWithRelations.absensi.divisi_abbr,
                                                "dept" to absensiWithRelations.absensi.dept,
                                                "dept_abbr" to absensiWithRelations.absensi.dept_abbr,
                                                "datetime" to (absensiWithRelations.absensi.date_absen
                                                    ?: "-"),
                                                "created_by" to (absensiWithRelations.absensi.created_by
                                                    ?: "-"),
                                                "info" to (absensiWithRelations.absensi.info
                                                    ?: "-"),
                                                "karyawan_msk_nik" to (absensiWithRelations.absensi.karyawan_msk_nik
                                                    ?: ""),
                                                "karyawan_tdk_msk_nik" to (absensiWithRelations.absensi.karyawan_tdk_msk_nik
                                                    ?: ""),
                                                "karyawan_msk_id" to (absensiWithRelations.absensi.karyawan_msk_id
                                                    ?: ""),
                                                "karyawan_tdk_msk_id" to (absensiWithRelations.absensi.karyawan_tdk_msk_id
                                                    ?: "")
                                            )

                                            // Return the AbsensiDataRekap object
                                            AbsensiDataRekap(
                                                id = absensiWithRelations.absensi.id,
                                                afdeling = absensiWithRelations.absensi.divisi_abbr
                                                    ?: "-",
                                                datetime = absensiWithRelations.absensi.date_absen
                                                    ?: "-",
                                                kemandoran_kode = absensiWithRelations.absensi.kemandoran_id,
                                                kemandoran = kemandoranKode,
                                                karyawan_msk_id = absensiWithRelations.absensi.karyawan_msk_id
                                                    ?: "",
                                                karyawan_tdk_msk_id = absensiWithRelations.absensi.karyawan_tdk_msk_id
                                                    ?: "",
                                                karyawan_msk_nama = absensiWithRelations.absensi.karyawan_msk_nama
                                                    ?: "",
                                                karyawan_tdk_msk_nama = absensiWithRelations.absensi.karyawan_tdk_msk_nama
                                                    ?: "",
                                                karyawan_msk_nik = absensiWithRelations.absensi.karyawan_msk_nik
                                                    ?: "",
                                                karyawan_tdk_msk_nik = absensiWithRelations.absensi.karyawan_tdk_msk_nik
                                                    ?: "",
                                                status_upload = absensiWithRelations.absensi.status_upload
                                            )
                                        } catch (e: Exception) {
                                            AppLogger.e("Error processing item ${absensiWithRelations.absensi.id}: ${e.message}")
                                            e.printStackTrace()

                                            // Return a default AbsensiDataRekap object with the ID to avoid losing data
                                            AbsensiDataRekap(
                                                id = absensiWithRelations.absensi.id,
                                                afdeling = "-",
                                                datetime = "-",
                                                kemandoran_kode =  "-",
                                                kemandoran = "-",
                                                karyawan_msk_id = "",
                                                karyawan_tdk_msk_id = "",
                                                karyawan_msk_nama = "",
                                                karyawan_tdk_msk_nama = "",
                                                karyawan_msk_nik = "",
                                                karyawan_tdk_msk_nik = "",
                                                status_upload = 0
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("Error processing data list: ${e.message}")
                                    e.printStackTrace()
                                    emptyList() // Return empty list on error
                                }
                            }

                            AppLogger.d("Data processing complete. Mapped data: ${mappedData.size} items")

                            // Update the adapter with the results
                            absensiAdapter.updateList(filteredData)

                        } catch (e: Exception) {
                            AppLogger.e("Data processing error: ${e.message}")
                            e.printStackTrace()

                            // Show an error message to the user if needed
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ListAbsensiActivity,
                                    "Error loading data: ${e.message ?: "Unknown error"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    tvEmptyStateAbsensi.text = "Tidak ada data kehadiran yang tercatat"
                    tvEmptyStateAbsensi.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    tglSection.visibility = View.GONE
                    totakKehadiranSection.visibility = View.GONE
                }
                counterTersimpan.text = data.size.toString()

                if (data.size == 0) {
                    btnGenerateQRAbsensi.visibility = View.GONE
                    dialUploadAbsensi.visibility = View.GONE
                    tglAbsensi.visibility = View.GONE
                } else {
                    btnGenerateQRAbsensi.visibility = View.VISIBLE
                    dialUploadAbsensi.visibility = View.GONE
                    tglAbsensi.visibility = View.VISIBLE
                }
                loadingDialog.dismiss()
            }
        }

        absensiViewModel.archivedAbsensiList.observe(this) { data ->
            btnGenerateQRAbsensi.visibility = View.GONE
            tglAbsensi.visibility = View.GONE

            if (currentState == 1) {
                absensiAdapter.updateList(emptyList())
                if (data.isNotEmpty()) {
                    speedDial.visibility = View.GONE
                    tvEmptyStateAbsensi.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    lifecycleScope.launch {
                        try {
                            // Process the data on IO thread
                            val filteredData = withContext(Dispatchers.IO) {
                                AppLogger.d("Processing ${data.size} items")

                                data.map { absensiWithRelations ->
                                    try {
                                        // Extract kemandoran IDs
                                        val rawKemandoran =
                                            absensiWithRelations.absensi.kemandoran_id
                                                .split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() } ?: emptyList()

                                        AppLogger.d("Processing ID ${absensiWithRelations.absensi.id} with kemandoran: $rawKemandoran")

                                        // Get kemandoran data
                                        val kemandoranData = try {
                                            absensiViewModel.getKemandoranById(rawKemandoran)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching kemandoran data: ${e.message}")
                                            null
                                        }

                                        AppLogger.d("Kemandoran data for ID ${absensiWithRelations.absensi.id}: ${kemandoranData?.map { it.id }}")
                                        AppLogger.d("kemandoranData $kemandoranData")
                                        // Process kemandoran kode
                                        val kemandoranNama =
                                            if (kemandoranData != null && kemandoranData.isNotEmpty()) {
                                                val kodes = kemandoranData.mapNotNull { it.kode }
                                                if (kodes.isNotEmpty()) {
                                                    kodes.joinToString("\n")
                                                } else {
                                                    "-"
                                                }
                                            } else {
                                                "-"
                                            }

                                        // Update mappedData
                                        mappedData = mappedData + mapOf(
                                            "id_kemandoran" to (rawKemandoran.takeIf { it.isNotEmpty() }
                                                ?: listOf("-")),
                                            "id" to absensiWithRelations.absensi.id,
                                            "nama_kemandoran" to kemandoranNama,
                                            "divisi" to absensiWithRelations.absensi.divisi,
                                            "divisi_abbr" to absensiWithRelations.absensi.divisi_abbr,
                                            "dept" to absensiWithRelations.absensi.dept,
                                            "dept_abbr" to absensiWithRelations.absensi.dept_abbr,
                                            "datetime" to (absensiWithRelations.absensi.date_absen
                                                ?: "-"),
                                            "created_by" to (absensiWithRelations.absensi.created_by
                                                ?: "-"),
                                            "info" to (absensiWithRelations.absensi.info
                                                ?: "-"),
                                            "karyawan_msk_nik" to (absensiWithRelations.absensi.karyawan_msk_nik
                                                ?: ""),
                                            "karyawan_tdk_msk_nik" to (absensiWithRelations.absensi.karyawan_tdk_msk_nik
                                                ?: ""),
                                            "karyawan_msk_id" to (absensiWithRelations.absensi.karyawan_msk_id
                                                ?: ""),
                                            "karyawan_tdk_msk_id" to (absensiWithRelations.absensi.karyawan_tdk_msk_id
                                                ?: "")
                                        )

                                        // Return the data object
                                        AbsensiDataRekap(
                                            id = absensiWithRelations.absensi.id,
                                            afdeling = absensiWithRelations.absensi.divisi_abbr
                                                ?: "-",
                                            datetime = absensiWithRelations.absensi.date_absen
                                                ?: "-",
                                            kemandoran_kode =  absensiWithRelations.absensi.kemandoran_id,
                                            kemandoran = kemandoranNama,
                                            karyawan_msk_id = absensiWithRelations.absensi.karyawan_msk_id
                                                ?: "",
                                            karyawan_tdk_msk_id = absensiWithRelations.absensi.karyawan_tdk_msk_id
                                                ?: "",
                                            karyawan_msk_nama = absensiWithRelations.absensi.karyawan_msk_nama
                                                ?: "",
                                            karyawan_tdk_msk_nama = absensiWithRelations.absensi.karyawan_tdk_msk_nama
                                                ?: "",
                                            karyawan_msk_nik = absensiWithRelations.absensi.karyawan_msk_nik
                                                ?: "",
                                            karyawan_tdk_msk_nik = absensiWithRelations.absensi.karyawan_tdk_msk_nik
                                                ?: "",
                                            status_upload = absensiWithRelations.absensi.status_upload
                                        )
                                    } catch (e: Exception) {
                                        AppLogger.e("Error processing item ${absensiWithRelations.absensi.id}: ${e.message}")

                                        // Return a default object on error
                                        AbsensiDataRekap(
                                            id = absensiWithRelations.absensi.id,
                                            afdeling = "-",
                                            datetime = "-",
                                            kemandoran_kode = "-",
                                            kemandoran = "-",
                                            karyawan_msk_id = "",
                                            karyawan_tdk_msk_id = "",
                                            karyawan_msk_nama = "",
                                            karyawan_tdk_msk_nama = "",
                                            karyawan_msk_nik = "",
                                            karyawan_tdk_msk_nik = "",
                                            status_upload = 0
                                        )
                                    }
                                }
                            }

                            AppLogger.d("Data processing complete: ${mappedData.size} items")
                            absensiAdapter.updateList(filteredData)

                        } catch (e: Exception) {
                            AppLogger.e("Data processing error: ${e.message}")
                            e.printStackTrace()

                            // Show error message
                            Toast.makeText(
                                this@ListAbsensiActivity,
                                "Error loading data: ${e.message ?: "Unknown error"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    tvEmptyStateAbsensi.text = "Tidak ada data kehadiran yang tercatat"
                    tvEmptyStateAbsensi.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    tglSection.visibility = View.GONE
                    totakKehadiranSection.visibility = View.GONE
                }
                counterTerscan.text = data.size.toString()
                loadingDialog.dismiss()
            }
        }
        absensiViewModel.error.observe(this) { errorMessage ->
            loadingDialog.dismiss()
            showErrorDialog(errorMessage)
        }

    }

    private fun showErrorDialog(errorMessage: String) {
        AlertDialogUtility.withSingleAction(
            this@ListAbsensiActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)} ${errorMessage}",
            "warning.json",
            R.color.colorRedDark
        ) {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("LOKASI", "KEMANDORAN", "TOTAL KEHADIRAN")
        updateTableHeaders(headers)

        absensiAdapter = ListAbsensiAdapter(this, listOf())
        recyclerView.adapter = absensiAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

//        recyclerView = findViewById(R.id.rvTableDataAbsensiList)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        absensiAdapter = ListAbsensiAdapter(emptyList())
//        recyclerView.adapter = absensiAdapter

        // Handle selection changes
        absensiAdapter.setOnSelectionChangeListener { selectedCount ->
            if (selectedCount > 0) {
                // Show speed dial when items are selected
                speedDial.visibility = View.VISIBLE
                // Update title or show counter
                tvTitlePage?.text = "$selectedCount item selected"
            } else {
                // Hide speed dial when no items are selected
                speedDial.visibility = View.GONE
                // Reset title
                tvTitlePage?.text = "Daftar Absensi"
                // Disable selection mode if there are no selections
                absensiAdapter.disableSelectionMode()
            }
        }
    }

    private fun setupSpeedDial() {
        speedDial.apply {
            // Add actions to the speed dial
            addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_delete, R.drawable.baseline_delete_forever_24)
                    .setLabel("Delete")
                    .setFabBackgroundColor(ContextCompat.getColor(this@ListAbsensiActivity, R.color.colorRed))
                    .setLabelBackgroundColor(ContextCompat.getColor(this@ListAbsensiActivity, R.color.white))
                    .create()
            )

            // Handle speed dial clicks
            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.fab_delete -> {
                        // Show confirmation dialog before deleting
                        showDeleteConfirmationDialog()
                        return@setOnActionSelectedListener true
                    }
                    R.id.fab_select_all -> {
                        // Toggle select all
                        absensiAdapter.toggleSelectAll()
                        return@setOnActionSelectedListener true
                    }
                }
                false
            }

            // Handle main FAB click (optional)
            setOnChangeListener(object : SpeedDialView.OnChangeListener {
                override fun onMainActionSelected(): Boolean {
                    // If you want to do something when the main FAB is clicked
                    return false
                }

                override fun onToggleChanged(isOpen: Boolean) {
                    // Optional: Do something when the speed dial is opened/closed
                }
            })
        }
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = absensiAdapter.getSelectedItems().size

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apa Anda yakin ingin menghapus $selectedCount item?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteSelectedItems()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteSelectedItems() {
        val selectedIds = absensiAdapter.getSelectedItemsIdLocal()
        absensiViewModel.deleteMultipleItems(selectedIds)
        absensiViewModel.deleteItemsResult.observe(this) { isSuccess ->
            loadingDialog.dismiss()
            val selectedCount = absensiAdapter.getSelectedItemsIdLocal().size
            if (isSuccess) {
                absensiViewModel.getAllDataAbsensi(0)
                playSound(R.raw.data_terhapus)
                Toast.makeText(
                    this,
                    "${getString(R.string.al_success_delete)} $selectedCount data",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "${getString(R.string.al_failed_delete)} data",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Reset UI state
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeaderAbsensi)
                .findViewById<CheckBox>(R.id.headerCheckBoxAbsensi)
            headerCheckBox.isChecked = false
            absensiAdapter.clearSelections()
            speedDial.visibility = View.GONE
        }
        AppLogger.d("testId ${selectedIds}")

        Toast.makeText(this, "${selectedIds.size} items deleted", Toast.LENGTH_SHORT).show()

        // After deletion, refresh your data and reset selection mode
        absensiAdapter.disableSelectionMode()
        absensiViewModel.getAllDataAbsensi(0)
    }

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

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun processSelectedDate(selectedDate: String) {

        AppLogger.d("cek tgl: ${globalFormattedDate}")
        AppLogger.d("cek stst: ${currentState}")
        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainerAbsensi)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_dateAbsensi)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_dateAbsensi)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate
        if (currentState == 0) {
            absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 0)
        } else if (currentState == 1) {
            absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 1)
        }

        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE
//            loadingDialog.show()
//            loadingDialog.setMessage("Sedang mengambil data...", true)
            // Get today's date in backend format
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )

            // Reset the selected date in your utils
            AppUtils.setSelectedDate(todayBackendDate)

            // Update the dateButton to show today's date
            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate

            if (currentState == 0) {
                absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 0)
            } else if (currentState == 1) {
                absensiViewModel.loadHistoryRekapAbsensi(globalFormattedDate, 1)
            }

        }

        filterDateContainer.visibility = View.VISIBLE
    }

    private fun initViewModel() {
        val factory = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel = ViewModelProvider(this, factory)[AbsensiViewModel::class.java]
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        vibrate()
        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}