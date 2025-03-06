package com.cbi.cmp_project.ui.view.Absensi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.AbsensiKemandoranRelations
import com.cbi.cmp_project.ui.adapter.AbsensiAdapter
import com.cbi.cmp_project.ui.adapter.AbsensiDataRekap
import com.cbi.cmp_project.ui.adapter.ListAbsensiAdapter
import com.cbi.cmp_project.ui.adapter.UploadItem
import com.cbi.cmp_project.ui.adapter.UploadProgressAdapter
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.adapter.WeighBridgeAdapter
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.ui.viewModel.AbsensiViewModel
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
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
import com.leinardi.android.speeddial.SpeedDialView
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
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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
    private lateinit var tvEmptyStateAbsensi: TextView // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setupQRAbsensi()
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

    private fun setupQRAbsensi() {
        val btnGenerateQRAbsensi = findViewById<FloatingActionButton>(R.id.btnGenerateQRAbsensi)
        btnGenerateQRAbsensi.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
            view.background = ContextCompat.getDrawable(
                this@ListAbsensiActivity,
                R.drawable.rounded_top_right_left
            )

            val dialog = BottomSheetDialog(this@ListAbsensiActivity)
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
                            val btnConfirmScanAbsensi: MaterialButton =
                                view.findViewById(R.id.btnConfirmScanPanenTPH)

                            btnConfirmScanAbsensi.setOnClickListener {
                                AlertDialogUtility.withTwoActions(
                                    this@ListAbsensiActivity,
                                    getString(R.string.al_delete),
                                    getString(R.string.confirmation_dialog_title),
                                    "${getString(R.string.al_make_sure_scanned_qr)}  data?",
                                    "warning.json",
                                    ContextCompat.getColor(
                                        this@ListAbsensiActivity,
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
//                                                        panenViewModel.archivePanenById(id)
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

//                                        panenViewModel.loadActivePanen()
//                                        panenViewModel.loadPanenCountArchive()
                                    }
                                }
                            }
                            AppLogger.d(mappedData.toString())
                            val jsonData = formatPanenDataForQR(mappedData)
                            AppLogger.d("data json $jsonData")
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
                                    btnConfirmScanAbsensi.alpha = 0f

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

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data Absensi is empty.")
            }

            val formattedList = mappedData.map { data ->
                val idKemandoran = data["id"]?.toString()
                    ?.replace("[", "")?.replace("]", "") // Remove brackets
                    ?: throw IllegalArgumentException("Missing idKemandoran.")

                val idKaryawan = data["karyawan_msk_id"]?.toString()
                    ?: throw IllegalArgumentException("Missing idKaryawan.")

                JSONObject().apply {
                    put("idKemandoran", idKemandoran)
                    put("idKaryawan", idKaryawan)
                }.toString()
            }

            formattedList.joinToString(",")

        } catch (e: Exception) {
            throw IllegalArgumentException("formatPanenDataForQR Error: ${e.message}")
        }
    }



//    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
//
//        return try {
//            if (mappedData.isEmpty()) {
//                throw IllegalArgumentException("Data Absensi is empty.")
//            }
//
//            val formattedData = buildString {
//                mappedData.forEach { data ->
//                    try {
//                        val id = data["id"]?.toString()
//                            ?: throw IllegalArgumentException("Missing id.")
//                        val dateCreated = data["datetime"]?.toString()
//                            ?: throw IllegalArgumentException("Missing date_created.")
//
//                        val kemandoranId = data["jjg_json"]?.toString()
//                            ?: throw IllegalArgumentException("Missing jjg_json.")
//                        val jjgJson = try {
//                            JSONObject(kemandoranId)
//                        } catch (e: JSONException) {
//                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $kemandoranId")
//                        }
//
//                        append("$id,$dateCreated;")
//                    } catch (e: Exception) {
//                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
//                    }
//                }
//            }
//
//            return JSONObject().apply {
//                put("kemandoran_0", formattedData)
//            }.toString()
//        } catch (e: Exception) {
//            AppLogger.e("formatAbsensiDataForQR Error: ${e.message}")
//            throw e
//        }
//    }

    private fun initializeViews() {
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

    fun formatToIndonesianDateTime(dateTimeStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr) ?: return dateTimeStr
            val outputFormat = SimpleDateFormat("d MMM yy\nHH:mm", Locale("id"))
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateTimeStr
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObserveData() {
        val listTgl = findViewById<TextView>(R.id.tvTglAbsensi)
        val listLokasi = findViewById<TextView>(R.id.lokasiKerja)
        val totalKehadiran = findViewById<TextView>(R.id.totalKehadiranAbsensi)
        val tglSection = findViewById<LinearLayout>(R.id.tglAbsensi)
        val totakKehadiranSection = findViewById<LinearLayout>(R.id.total_sectionAbsensi)
        val btnGenerateQRAbsensi = findViewById<FloatingActionButton>(R.id.btnGenerateQRAbsensi)

//        listTgl.text = formatToIndonesianDateTime(datetime)
//        loadingDialog.show()
//        loadingDialog.setMessage("Loading data...")
//
//        absensiViewModel.archivedCount.observe(this) { count ->
//            counterTerscan.text = count.toString()
//        }

        absensiViewModel.savedDataAbsensiList.observe(this) { data ->
            if (currentState == 0 ) {
                absensiAdapter.updateList(emptyList())

                if (data.isNotEmpty()) {
                    speedDial.visibility = View.VISIBLE
                    tvEmptyStateAbsensi.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

//                    mappedData = data.map { absensiWithRelations ->
//                        mapOf<String, Any>(
//                            "id" to (absensiWithRelations.absensi.id as Any),
//                            "kemandoran_id" to (absensiWithRelations.kemandoran?.kemandoran_id as Any),
//                            "datetime" to (absensiWithRelations.absensi.date_absen as Any),
//                            "karyawan_msk_id" to (absensiWithRelations.absensi.karyawan_msk_id as Any),
//                            "karyawan_tdk_msk_id" to (absensiWithRelations.absensi.karyawan_tdk_msk_id as Any)
//                        )
//                    }
//
//                    listTgl.visibility = View.VISIBLE
//                    listLokasi.visibility = View.VISIBLE
////                    totalKehadiran.text = View.VISIBLE
////                    totalJjg.text = totalJjgCount.toString()
////                    totalTPH.text = distinctTphCount.toString()
//
//                    absensiAdapter.updateList(mappedData)
//                    originalData =
//                        emptyList() // Reset original data when new data is loaded
//                    filterSection.visibility =
//                        View.GONE // Hide filter section for new data

//                // Launch coroutine in lifecycleScope
                    lifecycleScope.launch {
                        try {
                            val filteredData = coroutineScope {
                                AppLogger.d(data.toString())

                                data.map { absensiWithRelations ->
//                                val kemandoran = absensiWithRelations.kemandoran
//                                AppLogger.d("${kemandoran?.id}, Divisi Abbr: ${kemandoran?.divisi_abbr}, Nama: ${kemandoran?.nama}")


                                    val rawKemandoran = absensiWithRelations.absensi.kemandoran_id
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() } ?: emptyList()

                                    val kemandoranDeferred = async {
                                        try {
                                            absensiViewModel.getKemandoranById(rawKemandoran)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                            null
                                        }
                                    }

                                    val kemandoranData = try {
                                        kemandoranDeferred.await()
                                    } catch (e: Exception) {
                                        AppLogger.e("Failed to fetch Pemuat Data: ${e.message}")
                                        null
                                    }
                                    AppLogger.d(kemandoranData.toString())
                                    val kemandoranNama = kemandoranData?.mapNotNull { it.divisi_abbr }?.takeIf { it.isNotEmpty() }
                                        ?.joinToString("\n") ?: "-"

//                                    val kemandoranId = kemandoranData?.mapNotNull { it.divisi_abbr }?.takeIf { it.isNotEmpty() }
//                                        ?.joinToString("\n") ?: "-"

                                    async {
//                                    val allAfdeling = data.mapNotNull { it.kemandoran?.divisi_abbr }
//                                        .distinct() // Remove duplicates
//                                        .joinToString(", ") // Combine into a single string

                                        mappedData = mappedData + mapOf(
                                            "id" to rawKemandoran,
                                            "afdeling" to kemandoranNama,
                                            "datetime" to absensiWithRelations.absensi.date_absen,
                                            "karyawan_msk_id" to absensiWithRelations.absensi.karyawan_msk_id,
                                            "karyawan_tdk_msk_id" to absensiWithRelations.absensi.karyawan_tdk_msk_id
                                        )

                                        AbsensiDataRekap(
                                            //data untuk upload staging
                                            id = absensiWithRelations.absensi.id,
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
                                            afdeling = kemandoranNama,
                                            datetime = absensiWithRelations.absensi.date_absen,
                                            kemandoran = absensiWithRelations.kemandoran?.kode.toString(),
                                            karyawan_msk_id = absensiWithRelations.absensi.karyawan_msk_id,
                                            karyawan_tdk_msk_id = absensiWithRelations.absensi.karyawan_tdk_msk_id
                                        )

                                    }
                                }.map { it.await() } // Wait for all async tasks to complete
                            }
                            AppLogger.d("cek data ${mappedData.toString()}")
                            absensiAdapter.updateList(filteredData)
                        } catch (e: Exception) {
                            AppLogger.e("Data processing error: ${e.message}")
                        }
                    }
                } else {
                    tvEmptyStateAbsensi.text = "No Uploaded e-SPB data available"
                    tvEmptyStateAbsensi.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    tglSection.visibility = View.GONE
                    totakKehadiranSection.visibility = View.GONE
                }
//                counterTersimpan.text = data.size.toString()

//                if (data.size == 0) {
//                    btnGenerateQRAbsensi.visibility = View.GONE
//                } else {
//                    btnGenerateQRAbsensi.visibility = View.VISIBLE
//                }
            }
        }
    }

    private fun setupRecyclerView() {
        val headers = listOf("LOKASI", "KEMANDORAN", "TOTAL KEHADIRAN")
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