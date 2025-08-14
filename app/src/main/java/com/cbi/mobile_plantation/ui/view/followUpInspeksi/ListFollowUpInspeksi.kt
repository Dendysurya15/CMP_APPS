package com.cbi.mobile_plantation.ui.view.followUpInspeksi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.databinding.ActivityFollowUpInspeksiBinding
import com.cbi.mobile_plantation.ui.adapter.ListInspectionAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.formatToIndonesianDate
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScreenshotUtil
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ListFollowUpInspeksi : AppCompatActivity() {
    private var featureName = ""
    private var mappedData: List<Map<String, Any?>> = emptyList()
    private var currentState = 0
    private var parameterInspeksi: List<InspectionViewModel.InspectionParameterItem> = emptyList()
    private var limit = 0
    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private var prefManager: PrefManager? = null
    private lateinit var dateButton: Button
    private lateinit var adapter: ListInspectionAdapter
    private lateinit var tableHeader: View
    private lateinit var checkBoxHeader: CheckBox
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabDelListInspect: FloatingActionButton
    private var globalFormattedDate: String = ""
    private lateinit var inspectionViewModel: InspectionViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var filterAllData: CheckBox
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var parentCardStatus: LinearLayout
    private lateinit var horizontalCardFeature: HorizontalScrollView
    private lateinit var listMenuUploadData: LinearLayout
    private val dateIndexMap = mutableMapOf<String, Int>()

    // "Tersimpan" card properties
    private lateinit var cardItemTersimpan: MaterialCardView
    private lateinit var tvCardTersimpan: TextView
    private lateinit var counterItemTersimpan: TextView

    // "Sudah Scan" card properties
    private lateinit var cardItemTerscan: MaterialCardView
    private lateinit var tvCardTerscan: TextView
    private lateinit var counterItemTerscan: TextView
    private lateinit var btnGenerateQRTPH: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_inspection)

        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)

        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        globalFormattedDate = AppUtils.currentDate

        dateButton = findViewById(R.id.calendarPicker)
        dateButton.text = AppUtils.getTodaysDate()
        selectedPemanenAdapter = SelectedWorkerAdapter()

        setupHeader()
        initViewModel()
        initializeViews()
        setupRecyclerView()

        setupObservers()

        if (featureName != AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            loadParameterInspeksi()
        } else {
            setupVisibilityStatusHorizontalCard()
            setupCardListeners()
            setupGenerateQRTransfer()
        }

        setupFilterData()

        currentState = 0
        lifecycleScope.launch {
            if (featureName != AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
            } else {
                panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, 0)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent =
                    Intent(this@ListFollowUpInspeksi, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        })
    }

    private fun setupVisibilityStatusHorizontalCard() {
        parentCardStatus.visibility = View.VISIBLE
    }

    private fun makeQRLayoutSquare(screenshotLayout: View) {
        val qrLayout = screenshotLayout.findViewById<FrameLayout>(R.id.fLayoutQR)

        // Get screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate square size (80% of screen width with padding)
        val padding = (32 * resources.displayMetrics.density).toInt() // 32dp padding
        val squareSize = screenWidth - padding

        // Set equal width and height
        val layoutParams = qrLayout.layoutParams
        layoutParams.width = squareSize
        layoutParams.height = squareSize
        qrLayout.layoutParams = layoutParams
    }


    private fun takeQRCodeScreenshot(view: View) {

        lifecycleScope.launch {
            try {
                val screenshotLayout: View =
                    if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                        layoutInflater.inflate(R.layout.layout_screenshot_qr_mandor, null)
                    } else {
                        layoutInflater.inflate(R.layout.layout_screenshot_qr_kpanen, null)
                    }

                makeQRLayoutSquare(screenshotLayout)

                // Get references to views in the custom layout
                val tvUserName = screenshotLayout.findViewById<TextView>(R.id.tvUserName)
                val qrCodeImageView = screenshotLayout.findViewById<ImageView>(R.id.qrCodeImageView)
                val tvFooter = screenshotLayout.findViewById<TextView>(R.id.tvFooter)

                // Get references to included layouts
                val infoBlokList = screenshotLayout.findViewById<View>(R.id.infoBlokList)
                val infoTotalJjg = screenshotLayout.findViewById<View>(R.id.infoTotalJjg)
                val infoTotalTransaksi =
                    screenshotLayout.findViewById<View>(R.id.infoTotalTransaksi)

                // Add references for new info views
                val infoUrutanKe = screenshotLayout.findViewById<View>(R.id.infoUrutanKe)
                val infoJamTanggal = screenshotLayout.findViewById<View>(R.id.infoJamTanggal)

                fun setInfoData(includeView: View, labelText: String, valueText: String) {
                    val tvLabel = includeView.findViewById<TextView>(R.id.tvLabel)
                    val tvValue = includeView.findViewById<TextView>(R.id.tvValue)
                    tvLabel.text = labelText
                    tvValue.text = valueText
                }

                // Get the QR code bitmap from the current view
                val currentQrImageView = view.findViewById<ImageView>(R.id.qrCodeImageView)
                val qrBitmap = currentQrImageView.drawable?.let { drawable ->
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        // Convert drawable to bitmap if not already a BitmapDrawable
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
                }

                qrCodeImageView.setImageBitmap(qrBitmap)

                // Generate current date and time for footer
                val currentDate = Date()
                val indonesianLocale = Locale("id", "ID")
                val dateFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val formattedDate = dateFormat.format(currentDate).uppercase(indonesianLocale)
                val formattedTime = timeFormat.format(currentDate)

                // Get and increment screenshot counter
                val screenshotNumber = getAndIncrementScreenshotCounter()

                val effectiveLimit = if (limit == 0) mappedData.size else limit
                val limitedData = mappedData.take(effectiveLimit)

                val processedData = AppUtils.getPanenProcessedData(limitedData, featureName)
                val capitalizedFeatureName = featureName!!.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                tvUserName.text =
                    "Hasil QR ${capitalizedFeatureName} dari ${prefManager!!.jabatanUserLogin} - ${prefManager!!.estateUserLogin}"

                if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                    val infoNoESPB = screenshotLayout.findViewById<View>(R.id.infoNoESPB)
                    val infoDriver = screenshotLayout.findViewById<View>(R.id.infoDriver)
                    val infoNopol = screenshotLayout.findViewById<View>(R.id.infoNopol)
                    val infoPemuat = screenshotLayout.findViewById<View>(R.id.infoPemuat)

                    infoNoESPB.visibility = View.VISIBLE
                    infoDriver.visibility = View.VISIBLE
                    infoNopol.visibility = View.VISIBLE
                    infoPemuat.visibility = View.VISIBLE

                    setInfoData(infoBlokList, "Blok", ": ${processedData["blokDisplay"]}")
                    setInfoData(
                        infoTotalTransaksi,
                        "Jumlah Transaksi",
                        ": ${processedData["tphCount"]}"
                    )

                    // Add new info data for DetailESPB
                    setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                    setInfoData(infoJamTanggal, "Jam & Tanggal", ": $formattedDate, $formattedTime")

                } else {
                    setInfoData(infoBlokList, "Blok", ": ${processedData["blokDisplay"]}")
                    setInfoData(
                        infoTotalJjg,
                        "Total Janjang",
                        ": ${processedData["totalJjgCount"]} jjg"
                    )
                    setInfoData(
                        infoTotalTransaksi,
                        "Jumlah Transaksi",
                        ": ${processedData["tphCount"]}"
                    )

                    // Add new info data for other features
                    setInfoData(infoUrutanKe, "Urutan Ke", ": $screenshotNumber")
                    setInfoData(infoJamTanggal, "Jam & Tanggal", ": $formattedDate, $formattedTime")
                }

                tvFooter.text =
                    "GENERATED ON $formattedDate, $formattedTime | ${stringXML(R.string.name_app)}"

                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = LinearLayout.LayoutParams.WRAP_CONTENT

                screenshotLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                screenshotLayout.layout(
                    0, 0, screenshotLayout.measuredWidth, screenshotLayout.measuredHeight
                )

                // Create a meaningful filename
                val screenshotFileName = "Transfer_Inspeksi_Panen_QR"

                val watermarkType = if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
                } else if (featureName == AppUtils.ListFeatureNames.TransferHektarPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMTransferHektarPanen
                } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                    AppUtils.WaterMarkFotoDanFolder.WMESPB
                } else if (featureName == AppUtils.ListFeatureNames.AbsensiPanen) {
                    AppUtils.WaterMarkFotoDanFolder.WMAbsensiPanen
                } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                    AppUtils.WaterMarkFotoDanFolder.WMRekapPanenDanRestan
                } else if (featureName == AppUtils.ListFeatureNames.DetailESPB) {
                    AppUtils.WaterMarkFotoDanFolder.WMESPB
                } else {
                    AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
                }

                val screenshotFile = ScreenshotUtil.takeScreenshot(
                    screenshotLayout,
                    screenshotFileName,
                    watermarkType
                )

                if (screenshotFile != null) {
                    Toasty.success(
                        this@ListFollowUpInspeksi,
                        "QR sudah tersimpan digaleri",
                        Toast.LENGTH_LONG,
                        true
                    ).show()
                }
            } catch (e: Exception) {
                AppLogger.e("Error taking QR screenshot: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ListFollowUpInspeksi,
                        "Gagal menyimpan QR Code: ${e.message}",
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

    private fun setupGenerateQRTransfer() {


        btnGenerateQRTPH.setOnClickListener {
            generateQRTPH(0)
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

                        // Get the new fields
                        val tipe = data["jenis_panen"]?.toString()
                            ?: throw IllegalArgumentException("Missing jenis_panen.")
                        val ancak = data["ancak"]?.toString()
                            ?: throw IllegalArgumentException("Missing ancak.")
                        val nikString = data["karyawan_nik"]?.toString()
                            ?: throw IllegalArgumentException("Missing karyawan_nik.")
                        val namaString = data["karyawan_nama"]?.toString()
                            ?: throw IllegalArgumentException("Missing karyawan_nama.")

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-03-28
                        val time = dateParts[1]  // 13:15:18

                        // Create pemanen object for this entry
                        val pemanenJson = JSONObject()

                        // Handle multiple NIK and nama (comma-separated)
                        val nikList = nikString.split(",").map { it.trim() }
                        val namaList = namaString.split(",").map { it.trim() }

                        nikList.forEachIndexed { index, nik ->
                            if (nik.isNotEmpty()) {
                                val nama = if (index < namaList.size) namaList[index] else ""
                                pemanenJson.put(nik, nama)
                            }
                        }

                        // Use dateIndexMap.size as the index for new dates
                        append("$tphId,${dateIndexMap.getOrPut(date) { dateIndexMap.size }},$time,$tipe,$ancak,$pemanenJson;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            // Create the tgl object with date mappings
            val tglJson = JSONObject()
            dateIndexMap.forEach { (date, index) ->
                tglJson.put(index.toString(), date)
            }

            return JSONObject().apply {
                put("tph_0", formattedData)
                put("tgl", tglJson)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

    private fun generateQRTPH(limitFun: Int) {
        limit = limitFun
        AlertDialogUtility.withTwoActions(
            this,
            "Generate QR",
            getString(R.string.confirmation_dialog_title),
            getString(R.string.al_confirm_generate_qr),
            "warning.json",
            ContextCompat.getColor(this, R.color.bluedarklight),
            function = {
                val view =
                    layoutInflater.inflate(
                        R.layout.layout_bottom_sheet_generate_qr_panen,
                        null
                    )
                val dialog = BottomSheetDialog(this@ListFollowUpInspeksi)
                dialog.setContentView(view)
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
                val confimationContainer: LinearLayout =
                    view.findViewById(R.id.confirmationContainer)
                val scrollContent: NestedScrollView = view.findViewById(R.id.scrollContent)
                scrollContent.post {
                    // Scroll to the middle to show QR code in center
                    // 300dp space + approximately half of QR view height (125dp)
                    scrollContent.smoothScrollTo(0, 600)
                }

                val btnConfirmScanPanenTPH: MaterialButton =
                    view.findViewById(R.id.btnConfirmScanPanenTPH)
                val btnPreviewFullQR: MaterialButton =
                    view.findViewById(R.id.btnPreviewFullQR)

                btnPreviewFullQR.setOnClickListener {
                    showQrCodeFullScreen(qrCodeImageView.drawable, view)
                }

                // Initially hide QR code and dashed line, show loading
                qrCodeImageView.visibility = View.GONE
                loadingLogo.visibility = View.VISIBLE
                loadingContainer.visibility = View.VISIBLE

                // Initial setup for text elements
                titleQRConfirm.setResponsiveTextSizeWithConstraints(21F, 17F, 25F)
                descQRConfirm.setResponsiveTextSizeWithConstraints(19F, 15F, 23F)

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
                    val scaleXAnimation =
                        ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
                    val scaleYAnimation =
                        ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)

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

                val maxHeight = (resources.displayMetrics.heightPixels * 0.80).toInt()

                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bottomSheet ->
                        val behavior = BottomSheetBehavior.from(bottomSheet)

                        behavior.apply {
                            this.peekHeight =
                                maxHeight  // Set the initial height when peeking
                            this.state =
                                BottomSheetBehavior.STATE_EXPANDED  // Start fully expanded
                            this.isFitToContents =
                                true  // Content will determine the height (up to maxHeight)
                            this.isDraggable =
                                false  // Prevent user from dragging the sheet
                        }

                        // Set a fixed height for the bottom sheet
                        bottomSheet.layoutParams?.height = maxHeight
                    }

                dialog.show()


                btnConfirmScanPanenTPH.setOnClickListener {
                    AlertDialogUtility.withTwoActions(
                        this@ListFollowUpInspeksi,
                        getString(R.string.al_yes),
                        getString(R.string.confirmation_dialog_title),
                        "${getString(R.string.al_make_sure_scanned_qr)}",
                        "warning.json",
                        ContextCompat.getColor(
                            this@ListFollowUpInspeksi,
                            R.color.bluedarklight
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

                                    val effectiveLimit =
                                        if (limit == 0) mappedData.size else limit
                                    takeQRCodeScreenshot(view)
                                    // Take only the required number of items
                                    val limitedData = mappedData.take(effectiveLimit)


                                    limitedData.forEach { item ->
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
                                                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                                                    panenViewModel.archivePanenById(id)
                                                } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                                                    panenViewModel.changeStatusTransferRestan(id)
                                                } else if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                                                    panenViewModel.changeStatusTransferInspeksiPanen(
                                                        id
                                                    )
                                                }

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
                                                        this@ListFollowUpInspeksi,
                                                        "Gagal mengarsipkan data",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                hasError -> {
                                                    val errorDetail =
                                                        errorMessages.joinToString("\n")
                                                    AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                    Toast.makeText(
                                                        this@ListFollowUpInspeksi,
                                                        "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                else -> {
                                                    AppLogger.d("All items archived successfully")
                                                    playSound(R.raw.berhasil_konfirmasi)
                                                    Toast.makeText(
                                                        this@ListFollowUpInspeksi,
                                                        "Semua data berhasil diarsipkan",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                }
                                            }
                                            dialog.dismiss()
                                        } catch (e: Exception) {
                                            AppLogger.e("Error in UI update: ${e.message}")
                                            Toast.makeText(
                                                this@ListFollowUpInspeksi,
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
                                                this@ListFollowUpInspeksi,
                                                "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            dialog.dismiss()
                                        } catch (dialogException: Exception) {
                                            AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                        }
                                    }
                                }

                                //refresh list
                                panenViewModel.loadDataPanenTransferInspeksi(
                                    globalFormattedDate,
                                    currentState
                                )

                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0)
                                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1)

                            }


                        }


                    ) {

                    }
                }


                lifecycleScope.launch {
                    try {
                        delay(1000)

                        val jsonData = withContext(Dispatchers.IO) {
                            try {

                                val effectiveLimit =
                                    if (limitFun == 0) mappedData.size else limitFun

                                // Take only the required number of items
                                val limitedData = mappedData.take(effectiveLimit)

                                formatPanenDataForQR(limitedData)

                            } catch (e: Exception) {
                                AppLogger.e("Error generating JSON data: ${e.message}")
                                throw e
                            }
                        }


                        AppLogger.d("jsonData $jsonData")
                        val encodedData = withContext(Dispatchers.IO) {
                            try {
                                encodeJsonToBase64ZipQR(jsonData)
                                    ?: throw Exception("Encoding failed")
                            } catch (e: Exception) {
                                AppLogger.e("Error encoding data: ${e.message}")
                                throw e
                            }
                        }


                        val effectiveLimit = if (limit == 0) mappedData.size else limit
                        val limitedData = mappedData.take(effectiveLimit)

                        AppLogger.d("limitedData $limitedData")
                        val processedData = AppUtils.getPanenProcessedData(limitedData, featureName)
                        val listBlok = view.findViewById<TextView>(R.id.listBlok)
                        val totalTPH = view.findViewById<TextView>(R.id.totalTPH)

// Hide only the totalJjgContainer (Total Jjg section)
                        val totalJjgContainer =
                            view.findViewById<LinearLayout>(R.id.totalJjgContainer)
                        totalJjgContainer.visibility = View.GONE

// Make sure the total_section is visible so totalTPH shows
                        val totalSection = view.findViewById<LinearLayout>(R.id.total_section)
                        totalSection.visibility = View.VISIBLE

                        val blokSection = view.findViewById<LinearLayout>(R.id.blok_section)

                        AppLogger.d("processedData $processedData")
                        listBlok.text = processedData["blokDisplay"].toString()
                        totalTPH.text = processedData["tphCount"].toString()
                        withContext(Dispatchers.Main) {
                            try {
                                generateHighQualityQRCode(
                                    encodedData, qrCodeImageView,
                                    this@ListFollowUpInspeksi,
                                    showLogo = false
                                )
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

                                blokSection.alpha = 0f
                                totalSection.alpha = 0f
                                qrCodeImageView.alpha = 0f
                                dashedLine.alpha = 0f
                                tvTitleQRGenerate.alpha = 0f
                                titleQRConfirm.alpha = 0f
                                confimationContainer.alpha = 0f
                                descQRConfirm.alpha = 0f
                                btnConfirmScanPanenTPH.alpha = 0f
                                btnPreviewFullQR.alpha = 0f

                                // Create fade-in animations

                                val fadeInBlokSection =
                                    ObjectAnimator.ofFloat(blokSection, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }

                                val fadeInTotalSection =
                                    ObjectAnimator.ofFloat(totalSection, "alpha", 0f, 1f)
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }
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

                                val fadeInConfirmationContainer = ObjectAnimator.ofFloat(
                                    confimationContainer,
                                    "alpha",
                                    0f,
                                    1f
                                ).apply {
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

                                val fadeInButtonPreviewBtn =
                                    ObjectAnimator.ofFloat(
                                        btnPreviewFullQR,
                                        "alpha",
                                        0f,
                                        1f
                                    )
                                        .apply {
                                            duration = 250
                                            startDelay = 150
                                        }


                                // Run animations sequentially
                                AnimatorSet().apply {
                                    playTogether(fadeOut, fadeOutDots)
                                    addListener(object : AnimatorListenerAdapter() {
                                        @SuppressLint("SuspiciousIndentation")
                                        override fun onAnimationEnd(animation: Animator) {
                                            // Hide loading elements
                                            loadingLogo.visibility = View.GONE
                                            loadingContainer.visibility = View.GONE

                                            // Show elements
                                            confimationContainer.visibility = View.VISIBLE
                                            tvTitleQRGenerate.visibility = View.VISIBLE
                                            qrCodeImageView.visibility = View.VISIBLE
                                            dashedLine.visibility = View.VISIBLE
                                            blokSection.visibility = View.VISIBLE
                                            totalSection.visibility = View.VISIBLE
                                            btnConfirmScanPanenTPH.visibility = View.VISIBLE
                                            btnPreviewFullQR.visibility = View.VISIBLE

                                            lifecycleScope.launch {
                                                delay(200)
                                                playSound(R.raw.berhasil_generate_qr)
                                                delay(300)


                                            }

                                            // Start fade-in animations
                                            fadeInBlokSection.start()
                                            fadeInTotalSection.start()
                                            fadeInQR.start()
                                            fadeInDashedLine.start()
                                            fadeInTitle.start()
                                            fadeInTitleConfirm.start()
                                            fadeInConfirmationContainer.start()
                                            fadeInDescConfirm.start()
                                            fadeInButton.start()
                                            fadeInButtonPreviewBtn.start()
                                        }
                                    })
                                    start()
                                }
                            } catch (e: Exception) {
                                // Handle UI-related errors on the main thread
                                loadingLogo.animation?.cancel()
                                loadingLogo.clearAnimation()
                                loadingLogo.visibility = View.GONE
                                loadingContainer.visibility = View.GONE
                                AppLogger.e("QR Generation UI Error: ${e.message}")
                                showErrorMessageGenerateQR(
                                    view,
                                    "Error generating QR code: ${e.message}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any other errors
                        withContext(Dispatchers.Main) {
                            AppLogger.e("Error in QR process: ${e.message}")
                            stopLoadingAnimation(loadingLogo, loadingContainer)
                            showErrorMessageGenerateQR(
                                view,
                                "Error processing QR code: ${e.message}"
                            )
                        }
                    }
                }
            },
            cancelFunction = {

            }
        )
    }


    fun showErrorMessageGenerateQR(view: View, message: String) {
        val titleLayoutText = view.findViewById<TextView>(R.id.textTitleQRGenerate)
        val dashedLine = view.findViewById<View>(R.id.dashedLine)
        dashedLine.visibility = View.VISIBLE
        titleLayoutText.visibility = View.VISIBLE
        titleLayoutText.text = "Terjadi Kesalahan Generate QR!"
        titleLayoutText.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorRedDark
            )
        )
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }

    fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
    }


    private fun setupFilterData() {
        filterAllData = findViewById(R.id.calendarCheckbox)
        filterAllData.setOnCheckedChangeListener { _, isChecked ->
            val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
            val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
            if (isChecked) {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                filterDateContainer.visibility = View.VISIBLE
                nameFilterDate.text = "Semua Data"
                dateButton.isEnabled = false
                dateButton.alpha = 0.5f

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state - pass NULL for all data
                    panenViewModel.loadDataPanenTransferInspeksi(null, currentState)
                    panenViewModel.loadCountTransferInspeksi(null, 0)
                    panenViewModel.loadCountTransferInspeksi(null, 1)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
                }
            } else {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                val displayDate = ListPanenTBSActivity().formatGlobalDate(globalFormattedDate)
                dateButton.text = displayDate

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state - pass globalFormattedDate for specific date
                    panenViewModel.loadDataPanenTransferInspeksi(globalFormattedDate, currentState)
                    panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0)
                    panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(globalFormattedDate, 1)
                }

                nameFilterDate.text = displayDate
                dateButton.isEnabled = true
                dateButton.alpha = 1f // Make the button appear darker
            }

            val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)
            removeFilterDate.setOnClickListener {
                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil data...", true)
                if (filterAllData.isChecked) {
                    filterAllData.isChecked = false
                }
                filterDateContainer.visibility = View.GONE
                val todayBackendDate = AppUtils.formatDateForBackend(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR)
                )
                AppUtils.setSelectedDate(todayBackendDate)

                // Check feature type
                if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                    // Load panen data based on current state
                    panenViewModel.loadDataPanenTransferInspeksi(todayBackendDate, currentState)
                    panenViewModel.loadCountTransferInspeksi(todayBackendDate, 0)
                    panenViewModel.loadCountTransferInspeksi(todayBackendDate, 1)
                } else {
                    // Load inspection data
                    inspectionViewModel.loadInspectionPaths(todayBackendDate, 1)
                }

                val todayDisplayDate = AppUtils.getTodaysDate()
                dateButton.text = todayDisplayDate
            }
        }
    }

    private fun setupCardListeners() {
        cardItemTersimpan.setOnClickListener {
            loadingDialog.setMessage("Loading data tersimpan", true)
            adapter.updateData(emptyList())
            currentState = 0
            setActiveCard(cardItemTersimpan)
            loadingDialog.show()

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateArchiveState(currentState)

            // Check if filterAllData is checked
            val isAllDataFiltered = filterAllData.isChecked

            if (isAllDataFiltered) {
                panenViewModel.loadDataPanenTransferInspeksi(
                    null,
                    currentState
                )
                panenViewModel.loadCountTransferInspeksi(null, 0)
                panenViewModel.loadCountTransferInspeksi(null, 1)
            } else {
                panenViewModel.loadDataPanenTransferInspeksi(
                    globalFormattedDate,
                    currentState
                ) // Pass currentState (0)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1)
            }
        }

        cardItemTerscan.setOnClickListener {
            loadingDialog.setMessage("Loading data terscan", true)
            adapter.updateData(emptyList())
            currentState = 1
            setActiveCard(cardItemTerscan)
            loadingDialog.show()

            val isAllDataFiltered = filterAllData.isChecked

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateArchiveState(currentState)

            if (isAllDataFiltered) {
                panenViewModel.loadDataPanenTransferInspeksi(
                    null,
                    currentState
                ) // Pass currentState (1)
                panenViewModel.loadCountTransferInspeksi(null, 0)
                panenViewModel.loadCountTransferInspeksi(null, 1)
            } else {
                panenViewModel.loadDataPanenTransferInspeksi(
                    globalFormattedDate,
                    currentState
                ) // Pass currentState (1)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 0)
                panenViewModel.loadCountTransferInspeksi(globalFormattedDate, 1)
            }
        }
    }


    fun generateHighQualityQRCode(
        content: String,
        imageView: ImageView,
        context: Context,
        sizePx: Int = 1000,
        foregroundColorRes: Int? = R.color.black, // Optional: use custom color or default
        backgroundColorRes: Int? = R.color.white, // Optional: use custom color or default
        showLogo: Boolean = false, // Toggle to show/hide logo (default: no logo)
        logoRes: Int = R.drawable.cbi, // Default logo resource
        logoSizeRatio: Float = 0.12f // Logo size as ratio of QR code (12% by default)
    ) {
        try {
            // Get colors from colors.xml or use defaults
            val foregroundColor = foregroundColorRes?.let {
                ContextCompat.getColor(context, it)
            } ?: Color.BLACK // Default black if not specified

            val backgroundColor = backgroundColorRes?.let {
                ContextCompat.getColor(context, it)
            } ?: Color.WHITE // Default white if not specified

            // Create encoding hints for better quality
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M) // Change from H to M
                put(EncodeHintType.MARGIN, 3) // Increase from 1 to 3
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
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

            // Fill the bitmap with colors from colors.xml
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) foregroundColor else backgroundColor)
                }
            }


            // No logo, just set the QR code
            imageView.apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardItemTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardItemTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
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

    private fun initViewModel() {
        val factory = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]

        val factory2 = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory2)[PanenViewModel::class.java]
    }

    private fun initializeViews() {
        tableHeader = findViewById(R.id.tblHeaderListInspect)
        checkBoxHeader = tableHeader.findViewById(R.id.headerCheckBoxPanen)
        tvEmptyState = findViewById(R.id.tvEmptyDataListInspect)
        recyclerView = findViewById(R.id.rvTableDataListInspect)
        fabDelListInspect = findViewById(R.id.fabDelListInspect)

        parentCardStatus = findViewById(R.id.parentCardStatus)
        horizontalCardFeature = findViewById(R.id.horizontalCardFeature)
        listMenuUploadData = findViewById(R.id.list_menu_upload_data)

        // Initialize "Tersimpan" card elements
        cardItemTersimpan = findViewById(R.id.card_item_tersimpan)
        tvCardTersimpan = findViewById(R.id.tv_card_tersimpan)
        counterItemTersimpan = findViewById(R.id.counter_item_tersimpan)

        // Initialize "Sudah Scan" card elements
        cardItemTerscan = findViewById(R.id.card_item_terscan)
        tvCardTerscan = findViewById(R.id.tv_card_terscan)
        counterItemTerscan = findViewById(R.id.counter_item_terscan)

        btnGenerateQRTPH = findViewById(R.id.btnGenerateQRTPH)
    }

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")

        // Convert stored date components back to milliseconds
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth - 1, selectedDay) // Month is 0-based in Calendar
        builder.setSelection(calendar.timeInMillis)

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Store the selected date components
            selectedDay = day
            selectedMonth = month
            selectedYear = year

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun loadParameterInspeksi() {
        lifecycleScope.launch {
            try {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)

                val estateIdStr = estateName?.trim()

                if (!estateIdStr.isNullOrEmpty()) {
                    // Direct call without async since we're already in coroutine
                    parameterInspeksi = withContext(Dispatchers.IO) {
                        try {
                            inspectionViewModel.getParameterInspeksiJson()
                        } catch (e: Exception) {
                            AppLogger.e("Parameter loading failed: ${e.message}")
                            emptyList<InspectionViewModel.InspectionParameterItem>()
                        }
                    }

                    if (parameterInspeksi.isEmpty()) {
                        throw Exception("Parameter Inspeksi kosong! Harap Untuk melakukan sinkronisasi Data")
                    }
                }


                loadingDialog.dismiss()

            } catch (e: Exception) {
                val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"
                val fullMessage = errorMessage

                AlertDialogUtility.withSingleAction(
                    this@ListFollowUpInspeksi,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_failed_fetch_data),
                    fullMessage,
                    "warning.json",
                    R.color.colorRedDark
                ) {
                    finish()
                }
            }
        }
    }

    private fun processSelectedDate(selectedDate: String) {
        loadingDialog.show()
        loadingDialog.setMessage("Sedang mengambil data...", true)

        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate

        // Check feature type and load appropriate data
        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            // Load panen data based on current state
            panenViewModel.loadDataPanenTransferInspeksi(selectedDate, currentState)
            panenViewModel.loadCountTransferInspeksi(selectedDate, 0)
            panenViewModel.loadCountTransferInspeksi(selectedDate, 1)
        } else {
            // Load inspection data
            inspectionViewModel.loadInspectionPaths(selectedDate, 1)
        }

        removeFilterDate.setOnClickListener {
            loadingDialog.show()
            loadingDialog.setMessage("Sedang mengambil data...", true)
            if (filterAllData.isChecked) {
                filterAllData.isChecked = false
            }
            filterDateContainer.visibility = View.GONE
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )
            AppUtils.setSelectedDate(todayBackendDate)

            // Check feature type
            if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
                // Load panen data based on current state
                panenViewModel.loadDataPanenTransferInspeksi(todayBackendDate, currentState)
                panenViewModel.loadCountTransferInspeksi(todayBackendDate, 0)
                panenViewModel.loadCountTransferInspeksi(todayBackendDate, 1)
            } else {
                // Load inspection data
                inspectionViewModel.loadInspectionPaths(todayBackendDate, 1)
            }

            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate
        }

        filterDateContainer.visibility = View.VISIBLE
    }


    private fun setupRecyclerView() {
        adapter = if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            ListInspectionAdapter(
                featureName = featureName,
                onInspectionItemClick = { inspectionPath ->
                    // Handle inspection click if needed for TransferInspeksiPanen
                },
                onPanenItemClick = { panenItem ->
                    // Handle panen item click
//                    handlePanenItemClick(panenItem)
                }
            )
        } else {
            ListInspectionAdapter(
                featureName = featureName,
                onInspectionItemClick = { inspectionPath ->
                    showDetailData(inspectionPath)
                }
            )
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListFollowUpInspeksi)
            adapter = this@ListFollowUpInspeksi.adapter
            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
        }


        var headers: MutableList<String> = mutableListOf()
        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            headers = mutableListOf("BLOK-TPH", "TANGGAL\nPANEN", "TIPE/\nANCAK")
        } else {
            headers = mutableListOf("BLOK-TPH", "TGL INSPEKSI", "JUMLAH POKOK TEMUAN", "STATUS")
        }
        updateTableHeaders(headers)
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
        closeCardLinearLayout.setBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.greenDarker
            )
        )

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

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        if (featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen) {
            panenViewModel.panenTransferInspeksi.observe(this) { panenData ->
                adapter.setPanenData(panenData)
                mappedData = panenData.map { panenEntityWithRelations ->
                    mapOf(
                        "id" to panenEntityWithRelations.panen.id,
                        "tph_id" to panenEntityWithRelations.panen.tph_id,
                        "date_created" to panenEntityWithRelations.panen.date_created,
                        "created_by" to panenEntityWithRelations.panen.created_by,
                        "karyawan_id" to panenEntityWithRelations.panen.karyawan_id,
                        "kemandoran_id" to panenEntityWithRelations.panen.kemandoran_id,
                        "karyawan_nik" to panenEntityWithRelations.panen.karyawan_nik,
                        "karyawan_nama" to panenEntityWithRelations.panen.karyawan_nama,
                        "jjg_json" to panenEntityWithRelations.panen.jjg_json,
                        "foto" to panenEntityWithRelations.panen.foto,
                        "komentar" to panenEntityWithRelations.panen.komentar,
                        "asistensi" to panenEntityWithRelations.panen.asistensi,
                        "lat" to panenEntityWithRelations.panen.lat,
                        "lon" to panenEntityWithRelations.panen.lon,
                        "jenis_panen" to panenEntityWithRelations.panen.jenis_panen,
                        "ancak" to panenEntityWithRelations.panen.ancak,
                        "info" to panenEntityWithRelations.panen.info,
                        "archive" to panenEntityWithRelations.panen.archive,
                        "status_banjir" to panenEntityWithRelations.panen.status_banjir,
                        "status_espb" to panenEntityWithRelations.panen.status_espb,
                        "status_transfer_restan" to panenEntityWithRelations.panen.status_transfer_restan,
                        "status_restan" to panenEntityWithRelations.panen.status_restan,
                        "scan_status" to panenEntityWithRelations.panen.scan_status,
                        "dataIsZipped" to panenEntityWithRelations.panen.dataIsZipped,
                        "no_espb" to panenEntityWithRelations.panen.no_espb,
                        "username" to panenEntityWithRelations.panen.username,
                        "status_upload" to panenEntityWithRelations.panen.status_upload,
                        "status_uploaded_image" to panenEntityWithRelations.panen.status_uploaded_image,
                        "status_pengangkutan" to panenEntityWithRelations.panen.status_pengangkutan,
                        "status_insert_mpanen" to panenEntityWithRelations.panen.status_insert_mpanen,
                        "status_scan_mpanen" to panenEntityWithRelations.panen.status_scan_mpanen,
                        "status_scan_inspeksi" to panenEntityWithRelations.panen.status_scan_inspeksi,
                        "archive_transfer_inspeksi" to panenEntityWithRelations.panen.archive_transfer_inspeksi,
                        "jumlah_pemanen" to panenEntityWithRelations.panen.jumlah_pemanen,
                        "archive_mpanen" to panenEntityWithRelations.panen.archive_mpanen,
                        "isPushedToServer" to panenEntityWithRelations.panen.isPushedToServer,
                        // Add TPH data if needed
                        "blok_name" to panenEntityWithRelations.tph?.blok_kode,
                        "nomor" to panenEntityWithRelations.tph?.nomor,
                        "company_abbr" to panenEntityWithRelations.tph?.company_abbr,
                        "dept_abbr" to panenEntityWithRelations.tph?.dept_abbr,
                        "divisi_abbr" to panenEntityWithRelations.tph?.divisi_abbr
                    )
                }

                val processedData = AppUtils.getPanenProcessedData(mappedData, featureName)
                val listBlok = findViewById<TextView>(R.id.listBlok)
                val totalTPH = findViewById<TextView>(R.id.totalTPH)

// Make sure the total_section is visible so totalTPH shows


                AppLogger.d("processedData $processedData")
                listBlok.text = processedData["blokDisplay"].toString()
                totalTPH.text = processedData["tphCount"].toString()

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (panenData.isNotEmpty()) {
                            val totalSection = findViewById<LinearLayout>(R.id.total_section)
                            totalSection.visibility = View.VISIBLE

                            val blokSection = findViewById<LinearLayout>(R.id.blok_section)
                            blokSection.visibility = View.VISIBLE
                            btnGenerateQRTPH.visibility = if (currentState == 0) View.VISIBLE else View.GONE
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                        } else {
                            val totalSection = findViewById<LinearLayout>(R.id.total_section)
                            totalSection.visibility = View.GONE

                            val blokSection = findViewById<LinearLayout>(R.id.blok_section)
                            blokSection.visibility = View.GONE
                            tvEmptyState.text = "Tidak ada data pencarian"
                            btnGenerateQRTPH.visibility = View.GONE
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                }, 500)
            }


            panenViewModel.countPanenTransferInspeksi.observe(this) { (state, count) ->
                when (state) {
                    0 -> counterItemTersimpan.text = count.toString()
                    1 -> counterItemTerscan.text = count.toString()
                }
            }

        } else {
            // Observer for inspection data (original code)
            inspectionViewModel.inspectionWithDetails.observe(this) { inspectionPaths ->
                adapter.setData(inspectionPaths)

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {
                        if (inspectionPaths.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            tvEmptyState.text = "Tidak ada data pencarian"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                }, 500)
            }
        }
    }

    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Minify JSON first
            val minifiedJson = JSONObject(jsonData).toString()
            val originalJsonSize = minifiedJson.toByteArray(StandardCharsets.UTF_8).size
            AppLogger.d("Original JSON size: $originalJsonSize bytes")

            // SIZE CHECK: If original JSON is more than 4KB, just return null
            if (originalJsonSize > 4096) { // 4KB = 4096 bytes
                AppLogger.e("❌ JSON TOO LARGE: $originalJsonSize bytes (limit: 4096 bytes)")
                AppLogger.e("❌ QR generation aborted - data exceeds size limit")
                return null // Return null to indicate failure
            }

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
                val zipSize = zipBytes.size

                // Enhanced zip size logging
                AppLogger.d("=== ZIP COMPRESSION DETAILS ===")
                AppLogger.d("ZIP SIZE: $zipSize bytes")
                AppLogger.d("ZIP SIZE: ${String.format("%.2f", zipSize / 1024.0)} KB")
                AppLogger.d(
                    "Compression ratio: ${
                        String.format(
                            "%.2f",
                            (originalJsonSize.toDouble() / zipSize.toDouble())
                        )
                    }:1"
                )
                AppLogger.d(
                    "Size reduction: ${
                        String.format(
                            "%.1f",
                            ((originalJsonSize - zipSize).toDouble() / originalJsonSize.toDouble() * 100)
                        )
                    }%"
                )
                AppLogger.d("=== END ZIP COMPRESSION DETAILS ===")

                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)
                val base64Size = base64Encoded.length
                AppLogger.d("Base64 encoded size: $base64Size characters")

                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                val finalResult = firstHalf + AppUtils.half_json_encrypted + secondHalf
                val finalSize = finalResult.length
                AppLogger.d("Final QR data size: $finalSize characters")

                // Size summary
                AppLogger.d("=== SIZE SUMMARY ===")
                AppLogger.d("Original JSON: $originalJsonSize bytes")
                AppLogger.d("Compressed ZIP: $zipSize bytes")
                AppLogger.d("Base64 encoded: $base64Size chars")
                AppLogger.d("Final QR data: $finalSize chars")

                // Additional check on final QR size
                if (finalSize > 2000) {
                    AppLogger.e("⚠️ WARNING: Final QR data is ${finalSize} characters - may be too large for scanning!")
                } else {
                    AppLogger.d("✅ QR data size is acceptable for scanning")
                }

                finalResult
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            null // Return null instead of throwing exception
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            null // Return null instead of throwing exception
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            null // Return null instead of throwing exception
        }
    }




    private fun updateTableHeaders(headerNames: List<String>) {
        val checkboxFrameLayout =
            tableHeader.findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
        checkboxFrameLayout.visibility = View.GONE
        val headerIds =
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5) // Added th5 for 5 columns

        for (i in headerNames.indices) {
            if (i < headerIds.size) {
                val textView = tableHeader.findViewById<TextView>(headerIds[i])
                textView.apply {
                    visibility = View.VISIBLE
                    text = headerNames[i]
                }
            }
        }

        for (i in headerNames.size until headerIds.size) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.visibility = View.GONE
        }
    }


    @SuppressLint("InflateParams", "SetTextI18n", "MissingInflatedId", "Recycle")
    private fun showDetailData(inspectionPath: InspectionWithDetailRelations) {
        val fullMessage =
            "Anda akan melihat detail inspeksi dari ${inspectionPath.tph!!.blok_kode} - TPH ${inspectionPath.tph.nomor} yang sudah dilakukan pada ${inspectionPath.inspeksi.created_date}"

        // Check status_upload to determine which alert dialog to show
        if (inspectionPath.inspeksi.inspeksi_putaran == 2) {
            // Show single action dialog for CHECK_ONLY mode (already uploaded)
            AlertDialogUtility.withSingleAction(
                this@ListFollowUpInspeksi,
                "Kembali",
                "Detail Hasil Follow-Up",
                "$fullMessage\n\nData ini sudah dipulihkan.",
                "warning.json",
                R.color.greenDarker
            ) {

            }
        } else {
            // Show two actions dialog for normal edit mode (not uploaded yet)
            AlertDialogUtility.withTwoActions(
                this,
                "Telusuri",
                getString(R.string.confirmation_dialog_title),
                fullMessage,
                "warning.json",
                ContextCompat.getColor(this, R.color.bluedarklight),
                function = {
                    val intent = Intent(
                        this@ListFollowUpInspeksi,
                        FormInspectionActivity::class.java
                    )
                    intent.putExtra("FEATURE_NAME", AppUtils.ListFeatureNames.FollowUpInspeksi)
                    intent.putExtra("id_inspeksi", inspectionPath.inspeksi.id)

                    AppLogger.d("Opening normal edit mode for inspection ${inspectionPath.inspeksi.id} with status_upload=${inspectionPath.inspeksi.status_upload}")
                    startActivity(intent)
                },
                cancelFunction = { }
            )
        }
    }


}