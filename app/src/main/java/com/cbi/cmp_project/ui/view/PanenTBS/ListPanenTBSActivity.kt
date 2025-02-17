package com.cbi.cmp_project.ui.view.PanenTBS

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.adapter.ListPanenTPHAdapter
import com.cbi.cmp_project.ui.view.FormESPBActivity
import com.cbi.cmp_project.ui.view.HomePageActivity

import com.cbi.cmp_project.ui.viewModel.PanenViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

class ListPanenTBSActivity : AppCompatActivity() {
    private var featureName = ""
    private var listTPHDriver = ""
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var listAdapter: ListPanenTPHAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var currentState = 0 // 0 for tersimpan, 1 for terscan
    private var prefManager: PrefManager? = null
    private var isSettingUpCheckbox = false

    // Add views for buttons and counters
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private lateinit var tvEmptyState: TextView // Add this
    private lateinit var recyclerView: RecyclerView // Add this
    private lateinit var speedDial: SpeedDialView

    private var isAscendingOrder = true // Add this property at class level

    private lateinit var searchEditText: EditText  // Add this as class property
    private lateinit var sortButton: ImageView // Add this at class level
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    private var mappedData: List<Map<String, Any>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }
        listTPHDriver = try {
            AppUtils.readJsonFromEncryptedBase64Zip(
                intent.getStringExtra("scannedResult").toString()
            )
                .toString()


        } catch (e: Exception) {
            Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        Log.d("testing", listTPHDriver.toString())
        prefManager = PrefManager(this)
        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin
        setupHeader()
        initViewModel()
        initializeViews()
        loadingDialog = LoadingDialog(this)
        setupRecyclerView()
        setupSearch()
        setupObservers()
        setupSpeedDial()
        setupCardListeners()
        initializeFilterViews()
        setupSortButton()
        setupCheckboxControl()  // Add this
        currentState = 0
        setActiveCard(cardTersimpan)
        lifecycleScope.launch {
            if (featureName == "Buat eSPB") {
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {
                panenViewModel.loadActivePanenRestan()
            } else {
                panenViewModel.loadActivePanen()
                panenViewModel.loadPanenCountArchive() // Load archive count
            }

        }

        setupButtonGenerateQR()
    }

    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data tersimpan...")
            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            listAdapter.updateArchiveState(0)
            speedDial.visibility =
                if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
            if (featureName == "Buat eSPB") {
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {
                panenViewModel.loadActivePanenRestan()
            } else {
                panenViewModel.loadActivePanen()
            }
        }

        cardTerscan.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()
            loadingDialog.setMessage("Loading data terscan...")
            // Reset visibility states before loading new data
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(1)
            panenViewModel.loadArchivedPanen()
        }
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.card_item_tersimpan)
        cardTerscan = findViewById(R.id.card_item_terscan)
        counterTersimpan = findViewById(R.id.counter_item_tersimpan)
        counterTerscan = findViewById(R.id.counter_item_terscan)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.rvTableData) // Initialize RecyclerView
    }

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

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {

        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }

            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val toValue = if (jjgJson.has("TO")) {
                            jjgJson.getInt("TO") // Throws JSONException if "TO" is not an int
                        } else {
                            throw IllegalArgumentException("Missing 'TO' key in jjg_json: $jjgJsonString")
                        }

                        append("$tphId,$dateCreated,$toValue;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            return JSONObject().apply {
                put("tph_0", formattedData)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

    fun convertToFormattedString(input: String, int: Int = 1): String {
        try {
            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects.joinToString(";") { objStr ->
                // Clean up the object string
                val cleanObj = objStr.trim()
                    .removePrefix("{")
                    .removeSuffix("}")

                // Split into key-value pairs
                val map = cleanObj.split(", ").associate { pair ->
                    val (key, value) = pair.split("=", limit = 2)
                    key to value
                }

                // Extract jjg_json value
                val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                // Construct the formatted string
                "${map["tph_id"]},${map["date_created"]},${jjgJson},$int"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun convertToFormattedString(input: String, tphFilter: String): String {
        try {
            // Parse TPH filter string into a list of IDs
            val tphIds = tphFilter
                .trim()
                .removeSurrounding("{", "}")
                .substringAfter("\"tph\":\"")  // Get content after "tph":"
                .substringBefore("\"")         // Get content before the closing quote
                .split(";")
                .map { it.trim() }
                .toSet()

            Log.d("ListPanenTBSActivityESPB", "tphIds: $tphIds")

            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects
                .filter { objStr ->
                    // Extract tph_id from each object and check if it's in our filter list
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }
                    tphIds.contains(map["tph_id"])
                }
                .joinToString(";") { objStr ->
                    // Clean up the object string
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")

                    // Split into key-value pairs
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }

                    // Extract jjg_json value
                    val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                    // Construct the formatted string
                    "${map["tph_id"]},${map["date_created"]},${jjgJson},1"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    data class Entry(
        val id: String,
        val timestamp: String,
        val value: Int,
        val type: Int
    ) {
        override fun toString(): String = "$id,$timestamp,$value,$type"
    }

    fun String.toEntries(): Set<Entry> {
        if (this.isEmpty()) return emptySet()
        return split(";").map { entry ->
            val parts = entry.split(",")
            Entry(
                id = parts[0],
                timestamp = parts[1],
                value = parts[2].toInt(),
                type = parts[3].toInt()
            )
        }.toSet()
    }

    fun Set<Entry>.toString(): String {
        return if (isEmpty()) "" else joinToString(";")
    }

    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        if (featureName == "Buat eSPB") {
            btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
            btnGenerateQRTPH.setOnClickListener {
                //get manually selected items
                val selectedItems = listAdapter.getSelectedItems()
                Log.d("ListPanenTBSActivityESPB", "selectedItems: $selectedItems")
                val tph1AD0 =
                    convertToFormattedString(selectedItems.toString(), 0).replace("{\"KP\": ", "")
                        .replace("},", ",")
                Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD0")
                val tph1AD2 =
                    convertToFormattedString(selectedItems.toString(), 2).replace("{\"KP\": ", "")
                        .replace("},", ",")
                Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD2")

                //get automatically selected items
                val selectedItems2 = listAdapter.getCurrentData()
                Log.d("ListPanenTBSActivityESPB", selectedItems2.toString())
                Log.d("ListPanenTBSActivityESPB", listTPHDriver)
                val tph1NO = convertToFormattedString(
                    selectedItems2.toString(),
                    listTPHDriver
                ).replace("{\"KP\": ", "").replace("},", ",")
                Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsNO: $tph1NO")

                //get item which is not selected
                val tph0before =
                    convertToFormattedString(selectedItems2.toString(), 0).replace("{\"KP\": ", "")
                        .replace("},", ",")
                Log.d("ListPanenTBSActivityESPB", "formatted selectedItems0: $tph0before")

                val set1 = tph1AD0.toEntries()
                val set2 = tph1AD2.toEntries()
                val set3 = tph1NO.toEntries()
                val set4 = tph0before.toEntries()

                // Calculate string5 = string4 - string1 - string3
                val tph0 = (set4 - set1 - set3).toString().replace("[", "").replace("]", "")
                    .replace(", ", ";")
                Log.d("ListPanenTBSActivityESPB", "tph0: $tph0")

                // Calculate string6 = string2 + string3
                val tph1 =
                    (set2 + set3).toString().replace("[", "").replace("]", "").replace(", ", ";")
                Log.d("ListPanenTBSActivityESPB", "tph1: $tph1")
                AlertDialogUtility.withTwoActions(
                    this,
                    "LANJUT",
                    "PERHATIAN!",
                    "Apakah anda ingin membuat eSPB dengan data ini?",
                    "warning.json"
                ) {
                    val intent = Intent(this, FormESPBActivity::class.java)
                    intent.putExtra("tph_1", tph1)
                    intent.putExtra("tph_0", tph0)
                    intent.putExtra("FEATURE_NAME", featureName)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        } else {
            btnGenerateQRTPH.setOnClickListener {
                val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
                view.background = ContextCompat.getDrawable(
                    this@ListPanenTBSActivity,
                    R.drawable.rounded_top_right_left
                )

                val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
                dialog.setContentView(view)

                // Get references to views
                val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
                val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
                val tvTitleQRGenerate: TextView = view.findViewById(R.id.textTitleQRGenerate)
                val dashedLine: View = view.findViewById(R.id.dashedLine)
                val loadingContainer: LinearLayout =
                    view.findViewById(R.id.loadingDotsContainerBottomSheet)

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
                        val dataQR: TextView? = view.findViewById(R.id.dataQR)
                        val dataQRTitle: TextView? = view.findViewById(R.id.dataQRTitle)
                        lifecycleScope.launch(Dispatchers.Default) {
                            delay(1000)
                            try {
                                val dataQR: TextView? = view.findViewById(R.id.dataQR)
                                val titleQRConfirm: TextView =
                                    view.findViewById(R.id.titleAfterScanQR)
                                val descQRConfirm: TextView =
                                    view.findViewById(R.id.descAfterScanQR)
                                val btnConfirmScanPanenTPH: MaterialButton =
                                    view.findViewById(R.id.btnConfirmScanPanenTPH)

                                btnConfirmScanPanenTPH.setOnClickListener {

                                    AlertDialogUtility.withTwoActions(
                                        this@ListPanenTBSActivity,
                                        getString(R.string.al_delete),
                                        getString(R.string.confirmation_dialog_title),
                                        "${getString(R.string.al_make_sure_scanned_qr)}  data?",
                                        "warning.json",
                                        ContextCompat.getColor(
                                            this@ListPanenTBSActivity,
                                            R.color.greendarkerbutton
                                        )
                                    ) {
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

                                                        if (id <= 0) {
                                                            errorMessages.add("Invalid ID value: $id")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        try {
                                                            panenViewModel.archivePanenById(id)
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
                                                                    this@ListPanenTBSActivity,
                                                                    "Gagal mengarsipkan data",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            hasError -> {
                                                                val errorDetail =
                                                                    errorMessages.joinToString("\n")
                                                                AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                                Toast.makeText(
                                                                    this@ListPanenTBSActivity,
                                                                    "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            else -> {
                                                                AppLogger.d("All items archived successfully")
                                                                Toast.makeText(
                                                                    this@ListPanenTBSActivity,
                                                                    "Semua data berhasil diarsipkan",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                        dialog.dismiss()
                                                    } catch (e: Exception) {
                                                        AppLogger.e("Error in UI update: ${e.message}")
                                                        Toast.makeText(
                                                            this@ListPanenTBSActivity,
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
                                                            this@ListPanenTBSActivity,
                                                            "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        dialog.dismiss()
                                                    } catch (dialogException: Exception) {
                                                        AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                                    }
                                                }
                                            }

                                            panenViewModel.loadActivePanen()
                                            panenViewModel.loadPanenCountArchive()
                                        }
                                    }
                                }

                                val jsonData = formatPanenDataForQR(mappedData)
                                val encodedData =
                                    encodeJsonToBase64ZipQR(jsonData)
                                        ?: throw Exception("Encoding failed")

                                withContext(Dispatchers.Main) {
                                    try {

                                        generateHighQualityQRCode(encodedData, qrCodeImageView)
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
                                        btnConfirmScanPanenTPH.alpha = 0f

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
                                                btnConfirmScanPanenTPH,
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
                                                    btnConfirmScanPanenTPH.visibility = View.VISIBLE

                                                    fadeInQR.start()
                                                    fadeInDashedLine.start()
                                                    fadeInTitle.start()
                                                    fadeInText.start()
                                                    fadeInTitleConfirm.start()
                                                    fadeInDescConfirm.start()
                                                    fadeInButton.start()
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
        }
    }

            // Helper function to stop the loading animation and hide UI
            private fun stopLoadingAnimation(
                loadingLogo: ImageView,
                loadingContainer: LinearLayout
            ) {
                loadingLogo.animation?.cancel()
                loadingLogo.clearAnimation()
                loadingLogo.visibility = View.GONE
                loadingContainer.visibility = View.GONE
            }


            // Helper function to show errors
            fun showErrorMessageGenerateQR(view: View, message: String) {
                val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
                val errorText = view.findViewById<TextView>(R.id.errorText)
                errorText.text = message
                errorCard.visibility = View.VISIBLE
            }


            fun encodeJsonToBase64ZipQR(jsonData: String): String? {
                return try {
                    if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

                    // Minify JSON first
                    val minifiedJson = JSONObject(jsonData).toString()

                    // Reject empty JSON
                    if (minifiedJson == "{}") {
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


            private fun setupObservers() {
                val listBlok = findViewById<TextView>(R.id.listBlok)
                val totalJjg = findViewById<TextView>(R.id.totalJjg)
                val totalTPH = findViewById<TextView>(R.id.totalTPH)
                val blokSection = findViewById<LinearLayout>(R.id.blok_section)
                val totalSection = findViewById<LinearLayout>(R.id.total_section)
                val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)

                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")


                panenViewModel.archivedCount.observe(this) { count ->
                    counterTerscan.text = count.toString()
                }

                panenViewModel.activePanenList.observe(this) { panenList ->
                    if (currentState == 0) {
                        listAdapter.updateData(emptyList())
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadingDialog.dismiss()
                            if (panenList.isNotEmpty()) {
                                tvEmptyState.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE

                                mappedData = panenList.map { panenWithRelations ->
                                    mapOf<String, Any>(
                                        "id" to (panenWithRelations.panen.id as Any),
                                        "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                        "date_created" to (panenWithRelations.panen.date_created as Any),
                                        "blok_name" to (panenWithRelations.tph?.blok_kode
                                            ?: "Unknown"), // Handle null safely
                                        "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                        "created_by" to (panenWithRelations.panen.created_by as Any),
                                        "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                        "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                        "foto" to (panenWithRelations.panen.foto as Any),
                                        "komentar" to (panenWithRelations.panen.komentar as Any),
                                        "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                        "lat" to (panenWithRelations.panen.lat as Any),
                                        "lon" to (panenWithRelations.panen.lon as Any),
                                        "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                        "ancak" to (panenWithRelations.panen.ancak as Any),
                                        "archive" to (panenWithRelations.panen.archive as Any)
                                    )
                                }


                                val distinctBlokNames = mappedData
                                    .map { it["blok_name"].toString() }
                                    .distinct()
                                    .filter { it != "-" }
                                    .sorted()
                                    .joinToString(", ")

                                var totalJjgCount = 0
                                mappedData.forEach { data ->
                                    try {
                                        val jjgJsonString = data["jjg_json"].toString()
                                        val jjgJson = JSONObject(jjgJsonString)
                                        totalJjgCount += jjgJson.optInt("TO", 0)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error parsing jjg_json: ${e.message}")
                                    }
                                }

                                // Calculate distinct TPH count
                                val distinctTphCount = mappedData
                                    .mapNotNull { it["tph_id"].toString().toIntOrNull() }
                                    .distinct()
                                    .count()

                                // Update TextViews

                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                                listBlok.text = distinctBlokNames.ifEmpty { "-" }
                                totalJjg.text = totalJjgCount.toString()
                                totalTPH.text = distinctTphCount.toString()

                                listAdapter.updateData(mappedData)
                                originalData =
                                    emptyList() // Reset original data when new data is loaded
                                filterSection.visibility =
                                    View.GONE // Hide filter section for new data
                            } else {
                                tvEmptyState.text = "No saved data available"
                                tvEmptyState.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                                blokSection.visibility = View.GONE
                                totalSection.visibility = View.GONE
                            }
                            counterTersimpan.text = panenList.size.toString()

                            if (panenList.size == 0) {
                                btnGenerateQRTPH.visibility = View.GONE
                            } else {
                                btnGenerateQRTPH.visibility = View.VISIBLE
                            }
                        }, 500)
                    }
                }


                panenViewModel.archivedPanenList.observe(this) { panenList ->
                    if (currentState == 1) { // Only process if we're in terscan state
                        listAdapter.updateData(emptyList())
                        btnGenerateQRTPH.visibility = View.GONE
                        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                        headerCheckBox.visibility = View.GONE
                        Handler(Looper.getMainLooper()).postDelayed({


                            loadingDialog.dismiss()
                            if (panenList.isNotEmpty()) {
                                tvEmptyState.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                mappedData = panenList.map { panenWithRelations ->
                                    mapOf<String, Any>(
                                        "id" to (panenWithRelations.panen.id as Any),
                                        "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                        "blok_name" to (panenWithRelations.tph?.blok_kode
                                            ?: "-"),  // Handle null
                                        "nomor" to (panenWithRelations.tph?.nomor
                                            ?: "-"),  // Handle null
                                        "date_created" to (panenWithRelations.panen.date_created as Any),
                                        "created_by" to (panenWithRelations.panen.created_by as Any),
                                        "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                        "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                        "foto" to (panenWithRelations.panen.foto as Any),
                                        "komentar" to (panenWithRelations.panen.komentar as Any),
                                        "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                        "lat" to (panenWithRelations.panen.lat as Any),
                                        "lon" to (panenWithRelations.panen.lon as Any),
                                        "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                        "ancak" to (panenWithRelations.panen.ancak as Any),
                                        "archive" to (panenWithRelations.panen.archive as Any)
                                    )
                                }

                                val distinctBlokNames = mappedData
                                    .map { it["blok_name"]?.toString() ?: "-" }
                                    .distinct()
                                    .filter { it != "-" }
                                    .sorted()
                                    .joinToString(", ")

                                // Calculate total JJG by parsing JSON and summing TO values
                                var totalJjgCount = 0
                                mappedData.forEach { data ->
                                    try {
                                        val jjgJsonString = data["jjg_json"].toString()
                                        val jjgJson = JSONObject(jjgJsonString)
                                        totalJjgCount += jjgJson.optInt("TO", 0)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error parsing jjg_json: ${e.message}")
                                    }
                                }

                                // Calculate distinct TPH count
                                val distinctTphCount = mappedData
                                    .mapNotNull { it["tph_id"].toString().toIntOrNull() }
                                    .distinct()
                                    .count()

                                // Update TextViews
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                                listBlok.text = distinctBlokNames.ifEmpty { "-" }
                                totalJjg.text = totalJjgCount.toString()
                                totalTPH.text = distinctTphCount.toString()

                                listAdapter.updateData(mappedData)
                                originalData =
                                    emptyList() // Reset original data when new data is loaded
                                filterSection.visibility =
                                    View.GONE // Hide filter section for new data
                            } else {
                                tvEmptyState.text = "No scanned data available"
                                tvEmptyState.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                                blokSection.visibility = View.GONE
                                totalSection.visibility = View.GONE
                            }
                            counterTerscan.text = panenList.size.toString()
                        }, 500)
                    }
                }

                panenViewModel.error.observe(this) { errorMessage ->
                    loadingDialog.dismiss()
                    showErrorDialog(errorMessage)
                }
            }

            private fun initViewModel() {
                val factory = PanenViewModel.PanenViewModelFactory(application)
                panenViewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
            }

            private fun setupSearch() {
                searchEditText = findViewById(R.id.search_feature)
                val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)

                searchEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        val query = s.toString().trim()
                        listAdapter.filterData(query)

                        // Handle empty state
                        if (listAdapter.itemCount == 0) {
                            tvEmptyState.text = "Tidak ada data yang dicari"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }
                    }
                })
            }


            private fun showErrorDialog(errorMessage: String) {
                AlertDialogUtility.withSingleAction(
                    this@ListPanenTBSActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_failed_fetch_data),
                    "${stringXML(R.string.al_failed_fetch_data_desc)} ${errorMessage}",
                    "warning.json",
                    R.color.colorRedDark
                ) {
                    finish()
                }
            }

            private fun setupCheckboxControl() {
                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)

                headerCheckBox.apply {
                    visibility = View.VISIBLE
                    setOnCheckedChangeListener(null)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (!isSettingUpCheckbox) {
                            listAdapter.selectAll(isChecked)
                            speedDial.visibility = if (isChecked) View.VISIBLE else View.GONE
                        }
                    }
                }

                listAdapter.setOnSelectionChangedListener { selectedCount ->
                    isSettingUpCheckbox = true
                    headerCheckBox.isChecked = listAdapter.isAllSelected()

                    speedDial.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
                    isSettingUpCheckbox = false
                }
            }


            fun generateHighQualityQRCode(
                content: String,
                imageView: ImageView,
                sizePx: Int = 1000
            ) {
                try {
                    // Create encoding hints for better quality
                    val hints = hashMapOf<EncodeHintType, Any>().apply {
                        put(
                            EncodeHintType.ERROR_CORRECTION,
                            ErrorCorrectionLevel.M
                        ) // Change to M for balance
                        put(EncodeHintType.MARGIN, 1) // Smaller margin
                        put(EncodeHintType.CHARACTER_SET, "UTF-8")
                        // Remove fixed QR version to allow automatic scaling
                    }

                    // Create QR code writer with hints
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(
                        content,
                        BarcodeFormat.QR_CODE,
                        sizePx,
                        sizePx,
                        hints
                    )

                    // Create bitmap with appropriate size
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    // Fill the bitmap
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                        }
                    }

                    // Set the bitmap to ImageView with high quality scaling
                    imageView.apply {
                        setImageBitmap(bitmap)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
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

                    panenViewModel.deleteMultipleItems(selectedItems)

                    // Observe delete result
                    panenViewModel.deleteItemsResult.observe(this) { isSuccess ->
                        loadingDialog.dismiss()
                        if (isSuccess) {
                            Toast.makeText(
                                this,
                                "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Reload data based on current state
                            if (currentState == 0) {
                                panenViewModel.loadActivePanen()
                            } else {
                                panenViewModel.loadArchivedPanen()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "${getString(R.string.al_failed_delete)} data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Reset UI state
                        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                        headerCheckBox.isChecked = false
                        listAdapter.clearSelections()
                        speedDial.visibility = View.GONE
                    }

                    // Observe errors
                    panenViewModel.error.observe(this) { errorMessage ->
                        loadingDialog.dismiss()
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }


            private fun setupSpeedDial() {
                speedDial = findViewById(R.id.dial_tph_list)

                speedDial.apply {
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.scan_qr, R.drawable.baseline_qr_code_scanner_24)
//                    .setLabel(getString(R.string.generate_qr))
//                    .setFabBackgroundColor(
//                        ContextCompat.getColor(
//                            this@ListPanenTBSActivity,
//                            R.color.yellowbutton
//                        )
//                    )
//                    .create()
//            )

                    addActionItem(
                        SpeedDialActionItem.Builder(
                            R.id.deleteSelected,
                            R.drawable.baseline_delete_forever_24
                        )
                            .setLabel(getString(R.string.dial_delete_item))
                            .setFabBackgroundColor(
                                ContextCompat.getColor(
                                    this@ListPanenTBSActivity,
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
//                    R.id.cancelSelection -> {
//                        listAdapter.clearSelections()
//                        true
//                    }
                            R.id.deleteSelected -> {
                                val selectedItems = listAdapter.getSelectedItems()
                                handleDelete(selectedItems)
                                true
                            }

                            R.id.uploadSelected -> {
                                val selectedItems = listAdapter.getSelectedItems()

//                        if (AppUtils.isInternetAvailable(this@ListPanenTBSActivity)) {
//                            handleUpload(selectedItems)
//                        } else {
//                            AlertDialogUtility.withSingleAction(
//                                this@ListPanenTBSActivity,
//                                getString(R.string.al_back),
//                                getString(R.string.al_no_internet_connection),
//                                getString(R.string.al_no_internet_connection_description),
//                                "network_error.json",
//                                R.color.colorRedDark
//                            ) {}
//                        }
                                true
                            }

                            else -> false
                        }
                    }
                }


            }

            fun Int.toPx(): Int {
                return (this * Resources.getSystem().displayMetrics.density).toInt()
            }

            private fun initializeFilterViews() {
                filterSection = findViewById(R.id.filterSection)
                filterName = findViewById(R.id.filterName)
                removeFilter = findViewById(R.id.removeFilter)

                // Initially hide the filter section
                filterSection.visibility = View.GONE
            }

            private fun setupSortButton() {
                sortButton = findViewById(R.id.btn_sort)
                updateSortIcon() // Set initial icon state

                sortButton.setOnClickListener {
                    // Store original data order if this is the first sort
                    if (originalData.isEmpty()) {
                        originalData = listAdapter.getCurrentData()
                    }

                    isAscendingOrder = !isAscendingOrder
                    updateSortIcon() // Update icon on click

                    listAdapter.sortData(isAscendingOrder)
                    listAdapter.sortByCheckedItems(false)
                    updateFilterDisplay()
                }

                setupRemoveFilter()
            }


            private fun updateFilterDisplay() {
                filterSection.visibility = View.VISIBLE
                filterName.text =
                    if (isAscendingOrder) "Urutan Nomor TPH Kecil - Besar" else "Urutan Nomor TPH Besar - Kecil"
            }


            private fun setupRemoveFilter() {
                removeFilter.setOnClickListener {
                    // Get current search query
                    val currentSearchQuery = searchEditText.text.toString().trim()

                    // Reset sort state
                    isAscendingOrder = true
                    updateSortIcon()

                    if (originalData.isNotEmpty()) {
                        // Reset the sort but maintain the filter
                        listAdapter.resetSort()
                        if (currentSearchQuery.isNotEmpty()) {
                            listAdapter.filterData(currentSearchQuery)
                        }
                        originalData = emptyList()
                    }

                    // Hide filter section
                    filterSection.visibility = View.GONE
                }
            }

            @SuppressLint("MissingSuperCall")
            override fun onBackPressed() {
                vibrate()
//        AlertDialogUtility.withTwoActions(
//            this,
//            "Simpan",
//            getString(R.string.confirmation_dialog_title),
//            getString(R.string.al_confirm_feature),
//            "warning.json"
//        ) {
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
//        }

            }

            private fun updateSortIcon() {
                sortButton.animate()
                    .scaleY(if (isAscendingOrder) 1f else -1f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }


            private fun setupHeader() {
                featureName = intent.getStringExtra("FEATURE_NAME").toString()
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

            private fun setupRecyclerView() {
                listAdapter = ListPanenTPHAdapter()
                recyclerView.apply {
                    adapter = listAdapter
                    layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
                }
                listAdapter.setFeatureAndScanned(featureName, listTPHDriver)
            }


}
