package com.cbi.mobile_plantation.ui.view.faceRecognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.view.Surface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.PemanenPanenInfo
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.IdentifyPemanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.FaceRecognitionHelper
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdentifyPemanenActivity : AppCompatActivity() {

  private lateinit var viewModel: IdentifyPemanenViewModel
  private lateinit var loadingDialog: LoadingDialog
  private lateinit var imageCapture: ImageCapture
  private lateinit var cameraExecutor: ExecutorService
  private lateinit var analysisExecutor: ExecutorService
  private lateinit var faceOverlay: FaceBoundingBoxOverlay
  private lateinit var previewView: PreviewView

  private var prefManager: PrefManager? = null
  private var featureName: String = ""
  private var useFrontCamera = false
  private var cameraProvider: ProcessCameraProvider? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsReady = false
  private var pendingPanenTts = false
  private var lastIdentifiedNik: String? = null
  private var resultBottomSheet: BottomSheetDialog? = null
  private var resultSheetView: android.view.View? = null
  private lateinit var btnSelectPanenDateMain: MaterialButton
  private lateinit var btnIdentify: MaterialButton
  private lateinit var btnEnrollFace: MaterialButton
  private lateinit var tvInstruction: TextView
  private lateinit var tvEnrollStep: TextView
  private var detectedFaceCount = 0

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      startCamera()
    } else {
      Toast.makeText(this, "Izin kamera diperlukan untuk identifikasi wajah", Toast.LENGTH_LONG).show()
      finish()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_identify_pemanen)

    prefManager = PrefManager(this)
    featureName = intent.getStringExtra("FEATURE_NAME")
      ?: AppUtils.ListFeatureNames.IdentifikasiPemanen
    loadingDialog = LoadingDialog(this)
    cameraExecutor = Executors.newSingleThreadExecutor()
    analysisExecutor = Executors.newSingleThreadExecutor()

    previewView = findViewById(R.id.previewView)
    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    faceOverlay = findViewById(R.id.faceOverlay)
    tvInstruction = findViewById(R.id.tvInstruction)
    tvEnrollStep = findViewById(R.id.tvEnrollStep)
    btnIdentify = findViewById(R.id.btnIdentify)
    btnEnrollFace = findViewById(R.id.btnEnrollFace)

    viewModel = ViewModelProvider(
      this,
      IdentifyPemanenViewModel.Factory(application)
    )[IdentifyPemanenViewModel::class.java]

    initTextToSpeech()
    setupHeader()
    setupObservers()
    viewModel.loadPemanenData()

    findViewById<ImageView>(R.id.btn_back).setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }

    btnIdentify.setOnClickListener {
      if (viewModel.isEnrollActive()) {
        if (canCaptureEnrollPhoto()) {
          captureAndProcess()
        }
      } else {
        captureAndProcess()
      }
    }

    btnEnrollFace.setOnClickListener {
      if (viewModel.isEnrollActive()) {
        cancelEnrollSession()
      } else {
        showEnrollPemanenBottomSheet()
      }
    }

    findViewById<ImageView>(R.id.btnSwitchCamera).setOnClickListener {
      useFrontCamera = !useFrontCamera
      faceOverlay.clearFaces()
      bindCameraUseCases()
    }

    btnSelectPanenDateMain = findViewById(R.id.btnSelectPanenDateMain)
    updateMainDateButton(viewModel.getYesterdayDate())
    btnSelectPanenDateMain.setOnClickListener {
      openDatePicker(viewModel.selectedPanenDate.value ?: viewModel.getYesterdayDate()) { backendDate ->
        viewModel.setSelectedPanenDate(backendDate)
        updateMainDateButton(backendDate)
        lastIdentifiedNik?.let { nik ->
          viewModel.loadPanenInfo(nik, backendDate)
        }
      }
    }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        startActivity(android.content.Intent(this@IdentifyPemanenActivity, HomePageActivity::class.java))
        finishAffinity()
      }
    })

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      startCamera()
    } else {
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  private fun initTextToSpeech() {
    textToSpeech = TextToSpeech(this) { status ->
      isTtsReady = status == TextToSpeech.SUCCESS
      if (isTtsReady) {
        textToSpeech?.language = Locale("id", "ID")
      }
    }
  }

  private fun speakPemanenName(nama: String) {
    if (!isTtsReady || nama.isBlank()) return
    textToSpeech?.speak(nama, TextToSpeech.QUEUE_FLUSH, null, "identify_pemanen_name")
  }

  private fun speakPanenInfo(info: PemanenPanenInfo) {
    if (!isTtsReady || !pendingPanenTts) return
    pendingPanenTts = false

    val janjangText = getString(R.string.tts_janjang_panen, info.totalJanjangTo)
    textToSpeech?.speak(janjangText, TextToSpeech.QUEUE_ADD, null, "identify_pemanen_janjang")

    val blokText = if (info.blokList.isEmpty()) {
      getString(R.string.blok_panen_kosong)
    } else {
      val blokSpoken = info.blokList.joinToString(", ") { it.replace(" - ", ", ") }
      getString(R.string.tts_blok_panen, blokSpoken)
    }
    textToSpeech?.speak(blokText, TextToSpeech.QUEUE_ADD, null, "identify_pemanen_blok")
  }

  private fun trySpeakPanenInfo() {
    val info = viewModel.panenInfo.value ?: return
    speakPanenInfo(info)
  }

  private fun updateMainDateButton(backendDate: String) {
    val displayDate = AppUtils.formatSelectedDateForDisplay(backendDate)
    btnSelectPanenDateMain.text = getString(R.string.data_panen_tanggal, displayDate)
  }

  private fun bindPanenInfoToSheet(sheetView: android.view.View, info: PemanenPanenInfo?) {
    val tvJanjangTo = sheetView.findViewById<TextView>(R.id.tvJanjangTo)
    val tvBlokPanen = sheetView.findViewById<TextView>(R.id.tvBlokPanen)
    val btnSelectPanenDate = sheetView.findViewById<MaterialButton>(R.id.btnSelectPanenDate)

    if (info == null) {
      tvJanjangTo.text = getString(R.string.janjang_to_label, 0)
      tvBlokPanen.text = getString(R.string.blok_panen_kosong)
      return
    }

    btnSelectPanenDate.text =
      getString(R.string.tanggal_panen_label, AppUtils.formatSelectedDateForDisplay(info.queryDate))
    tvJanjangTo.text = getString(R.string.janjang_to_label, info.totalJanjangTo)
    tvBlokPanen.text = if (info.blokList.isEmpty()) {
      getString(R.string.blok_panen_kosong)
    } else {
      info.blokList.joinToString("\n")
    }
  }

  private fun openDatePicker(
    currentBackendDate: String,
    onDateSelected: (String) -> Unit
  ) {
    val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectionMillis = backendFormat.parse(currentBackendDate)?.time
      ?: MaterialDatePicker.todayInUtcMilliseconds()

    val datePicker = MaterialDatePicker.Builder.datePicker()
      .setTitleText(getString(R.string.pilih_tanggal_panen))
      .setSelection(selectionMillis)
      .build()

    datePicker.addOnPositiveButtonClickListener { selection ->
      val calendar = Calendar.getInstance()
      calendar.timeInMillis = selection
      val backendDate = AppUtils.formatDateForBackend(
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR)
      )
      onDateSelected(backendDate)
    }
    datePicker.show(supportFragmentManager, "IDENTIFY_PEMANEN_DATE_PICKER")
  }

  private fun setupHeader() {
    val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
    val userSection = findViewById<TextView>(R.id.userSection)
    val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
    val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
    val locationSection = findViewById<LinearLayout>(R.id.locationSection)
    locationSection.visibility = android.view.View.GONE

    AppUtils.setupUserHeader(
      userName = prefManager?.nameUserLogin.orEmpty(),
      userSection = userSection,
      featureName = featureName,
      tvFeatureName = tvFeatureName,
      prefManager = prefManager,
      lastUpdateText = lastUpdateText,
      titleAppNameAndVersionText = titleAppNameAndVersion,
      context = this
    )
  }

  private fun setupObservers() {
    viewModel.enrolledCount.observe(this) { count ->
      findViewById<TextView>(R.id.tvEnrolledCount).text =
        getString(R.string.wajah_terdaftar_count, count)
    }

    viewModel.selectedPanenDate.observe(this) { date ->
      updateMainDateButton(date)
    }

    viewModel.panenInfo.observe(this) { info ->
      resultSheetView?.let { bindPanenInfoToSheet(it, info) }
      if (info != null) {
        trySpeakPanenInfo()
      }
    }

    viewModel.identifyResult.observe(this) { state ->
      when (state) {
        IdentifyPemanenViewModel.IdentifyState.Processing -> {
          loadingDialog.show()
          loadingDialog.setMessage("Mengidentifikasi wajah...", true)
        }

        is IdentifyPemanenViewModel.IdentifyState.Success -> {
          loadingDialog.dismiss()
          lastIdentifiedNik = state.result.nik
          pendingPanenTts = true
          speakPemanenName(state.result.nama)
          trySpeakPanenInfo()
          showIdentifyResultBottomSheet(
            state.result.nama,
            state.result.nik,
            state.result.kemandoranNama,
            state.result.confidence,
            true
          )
        }

        is IdentifyPemanenViewModel.IdentifyState.NotFound -> {
          pendingPanenTts = false
          loadingDialog.dismiss()
          AlertDialogUtility.withSingleAction(
            this,
            "OK",
            "Pemanen Tidak Dikenali",
            state.message,
            "warning.json",
            R.color.colorRedDark
          ) {}
        }

        is IdentifyPemanenViewModel.IdentifyState.Error -> {
          pendingPanenTts = false
          loadingDialog.dismiss()
          Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
        }

        null -> Unit
      }
    }

    viewModel.enrollSession.observe(this) { session ->
      updateEnrollUi(session)
    }

    viewModel.enrollEvent.observe(this) { event ->
      when (event) {
        IdentifyPemanenViewModel.EnrollEvent.Processing -> {
          loadingDialog.show()
          loadingDialog.setMessage("Memproses foto...", true)
        }

        is IdentifyPemanenViewModel.EnrollEvent.StepCaptured -> {
          loadingDialog.dismiss()
          val label = when (event.completedStep) {
            IdentifyPemanenViewModel.EnrollStep.FRONT -> getString(R.string.enroll_foto_depan)
            IdentifyPemanenViewModel.EnrollStep.LEFT -> getString(R.string.enroll_foto_kiri)
            IdentifyPemanenViewModel.EnrollStep.RIGHT -> getString(R.string.enroll_foto_kanan)
            IdentifyPemanenViewModel.EnrollStep.TEST -> ""
          }
          if (label.isNotEmpty()) {
            Toast.makeText(
              this,
              getString(R.string.enroll_foto_berhasil, label),
              Toast.LENGTH_SHORT
            ).show()
          }
        }

        IdentifyPemanenViewModel.EnrollEvent.TestPassed -> {
          loadingDialog.dismiss()
          Toast.makeText(this, R.string.enroll_test_berhasil, Toast.LENGTH_LONG).show()
          viewModel.loadPemanenData()
          resetInstructionUi()
        }

        is IdentifyPemanenViewModel.EnrollEvent.TestFailed -> {
          loadingDialog.dismiss()
          AlertDialogUtility.withTwoActions(
            context = this,
            actionText = getString(R.string.enroll_ulangi_test),
            titleText = getString(R.string.enroll_test_gagal),
            alertText = event.message,
            animAsset = "warning.json",
            buttonColor = R.color.colorRedDark,
            cancelText = getString(R.string.enroll_ulangi_dari_awal),
            function = {},
            cancelFunction = { restartEnrollFromBeginning() }
          )
        }

        is IdentifyPemanenViewModel.EnrollEvent.Error -> {
          loadingDialog.dismiss()
          Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
        }

        null -> Unit
      }
    }
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
      if (isDestroyed || isFinishing) return@addListener
      cameraProvider = cameraProviderFuture.get()
      if (previewView.isAttachedToWindow) {
        bindCameraUseCases()
      } else {
        previewView.post { bindCameraUseCases() }
      }
    }, ContextCompat.getMainExecutor(this))
  }

  private fun getDisplayRotation(): Int {
    previewView.display?.let { return it.rotation }
    return ContextCompat.getDisplayOrDefault(this).rotation
  }

  private fun bindCameraUseCases() {
    if (isDestroyed || isFinishing) return
    val provider = cameraProvider ?: return
    if (!previewView.isAttachedToWindow) {
      previewView.post { bindCameraUseCases() }
      return
    }

    val displayRotation = getDisplayRotation()

    val preview = Preview.Builder()
      .setTargetRotation(displayRotation)
      .build()
    preview.setSurfaceProvider(previewView.surfaceProvider)

    imageCapture = ImageCapture.Builder()
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
      .setTargetRotation(displayRotation)
      .build()

    val imageAnalysis = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .setTargetRotation(displayRotation)
      .build()

    imageAnalysis.setAnalyzer(analysisExecutor, ::analyzeFace)

    val cameraSelector = if (useFrontCamera) {
      CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
      CameraSelector.DEFAULT_BACK_CAMERA
    }

    val viewportRatio = if (previewView.width > 0 && previewView.height > 0) {
      Rational(previewView.width, previewView.height)
    } else {
      Rational(3, 4)
    }
    val viewport = ViewPort.Builder(viewportRatio, displayRotation).build()
    val useCaseGroup = UseCaseGroup.Builder()
      .addUseCase(preview)
      .addUseCase(imageCapture)
      .addUseCase(imageAnalysis)
      .setViewPort(viewport)
      .build()

    try {
      provider.unbindAll()
      provider.bindToLifecycle(this, cameraSelector, useCaseGroup)
    } catch (e: Exception) {
      Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun analyzeFace(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
      imageProxy.close()
      return
    }

    val imageWidth = imageProxy.width
    val imageHeight = imageProxy.height
    val rotation = imageProxy.imageInfo.rotationDegrees
    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
    val mainExecutor = ContextCompat.getMainExecutor(this)
    val sourceTransform = try {
      ImageProxyTransformFactory().getOutputTransform(imageProxy)
    } catch (_: Exception) {
      null
    }

    FaceRecognitionHelper.detectFacesAsync(
      image = inputImage,
      onSuccess = { faces ->
        mainExecutor.execute {
          val overlayWidth = faceOverlay.width
          val overlayHeight = faceOverlay.height
          val mappedRects = faces.map { face ->
            FaceBoundingBoxOverlay.mapFaceToOverlay(
              faceBox = face.boundingBox,
              sourceTransform = sourceTransform,
              previewView = previewView,
              imageWidth = imageWidth,
              imageHeight = imageHeight,
              rotationDegrees = rotation,
              overlayWidth = overlayWidth,
              overlayHeight = overlayHeight
            )
          }
          faceOverlay.updateFaces(mappedRects)
          detectedFaceCount = faces.size
          updateFaceCountFeedback()
          imageProxy.close()
        }
      },
      onFailure = {
        mainExecutor.execute {
          faceOverlay.clearFaces()
          detectedFaceCount = 0
          updateFaceCountFeedback()
          imageProxy.close()
        }
      }
    )
  }

  private fun captureAndProcess() {
    if (!::imageCapture.isInitialized) {
      Toast.makeText(this, "Kamera belum siap", Toast.LENGTH_SHORT).show()
      return
    }

    val photoFile = File(cacheDir, "face_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
      outputOptions,
      cameraExecutor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
          if (bitmap == null) {
            runOnUiThread {
              Toast.makeText(this@IdentifyPemanenActivity, "Gagal membaca foto", Toast.LENGTH_LONG).show()
            }
            return
          }

          val rotation = getRotationFromExif(photoFile)

          runOnUiThread {
            if (viewModel.isEnrollActive()) {
              viewModel.processEnrollCapture(bitmap, rotation)
            } else {
              viewModel.identifyFromBitmap(bitmap, rotation)
            }
          }
        }

        override fun onError(exception: ImageCaptureException) {
          runOnUiThread {
            Toast.makeText(
              this@IdentifyPemanenActivity,
              "Gagal mengambil foto: ${exception.message}",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      }
    )
  }

  private fun getRotationFromExif(file: File): Int {
    return try {
      val exif = ExifInterface(file.absolutePath)
      when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
      }
    } catch (_: Exception) {
      when (getDisplayRotation()) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
      }
    }
  }

  private fun showEnrollPemanenBottomSheet() {
    val pemanenList = viewModel.pemanenList.value.orEmpty()
    if (pemanenList.isEmpty()) {
      Toast.makeText(this, R.string.data_pemanen_kosong, Toast.LENGTH_LONG).show()
      return
    }

    val bottomSheet = BottomSheetDialog(this)
    val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_enroll_pemanen, null)
    bottomSheet.setContentView(sheetView)

    val searchField = sheetView.findViewById<EditText>(R.id.etSearchPemanen)
    val listView = sheetView.findViewById<ListView>(R.id.lvPemanen)

    var filteredList = pemanenList
    val adapter = ArrayAdapter(
      this,
      android.R.layout.simple_list_item_1,
      filteredList.map { "${it.nik.orEmpty()} - ${it.nama.orEmpty()}" }.toMutableList()
    )
    listView.adapter = adapter

    searchField.addTextChangedListener(object : android.text.TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
      override fun afterTextChanged(s: android.text.Editable?) {
        val query = s?.toString().orEmpty().trim()
        filteredList = if (query.isEmpty()) {
          pemanenList
        } else {
          pemanenList.filter {
            it.nama.orEmpty().contains(query, ignoreCase = true) ||
              it.nik.orEmpty().contains(query, ignoreCase = true)
          }
        }
        adapter.clear()
        adapter.addAll(filteredList.map { "${it.nik.orEmpty()} - ${it.nama.orEmpty()}" })
        adapter.notifyDataSetChanged()
      }
    })

    listView.setOnItemClickListener { _, _, position, _ ->
      bottomSheet.dismiss()
      viewModel.startEnrollSession(filteredList[position])
    }

    bottomSheet.show()
  }

  private fun updateEnrollUi(session: IdentifyPemanenViewModel.EnrollSession?) {
    if (session == null) {
      resetInstructionUi()
      return
    }

    val nama = session.karyawan.nama.orEmpty()
    val stepNumber = when (session.step) {
      IdentifyPemanenViewModel.EnrollStep.FRONT -> 2
      IdentifyPemanenViewModel.EnrollStep.LEFT -> 3
      IdentifyPemanenViewModel.EnrollStep.RIGHT -> 4
      IdentifyPemanenViewModel.EnrollStep.TEST -> 5
    }

    tvEnrollStep.visibility = android.view.View.VISIBLE
    tvEnrollStep.text = getString(R.string.enroll_step_label, stepNumber)

    val baseInstruction = when (session.step) {
      IdentifyPemanenViewModel.EnrollStep.FRONT ->
        getString(R.string.enroll_step_front, nama)
      IdentifyPemanenViewModel.EnrollStep.LEFT ->
        getString(R.string.enroll_step_left, nama)
      IdentifyPemanenViewModel.EnrollStep.RIGHT ->
        getString(R.string.enroll_step_right, nama)
      IdentifyPemanenViewModel.EnrollStep.TEST ->
        getString(R.string.enroll_step_test, nama)
    }
    tvInstruction.text = baseInstruction

    btnEnrollFace.text = getString(R.string.enroll_batal)
    btnIdentify.text = if (session.step == IdentifyPemanenViewModel.EnrollStep.TEST) {
      getString(R.string.enroll_uji_identifikasi)
    } else {
      getString(R.string.enroll_ambil_foto)
    }
    btnIdentify.icon = null
    updateCaptureButtonState()
    updateFaceCountFeedback()
  }

  private fun resetInstructionUi() {
    tvEnrollStep.visibility = android.view.View.GONE
    tvInstruction.text = getString(R.string.identify_pemanen_instruction)
    btnEnrollFace.text = getString(R.string.daftar_wajah_pemanen)
    btnIdentify.text = getString(R.string.identifikasi_sekarang)
    btnIdentify.setIconResource(R.drawable.baseline_camera_front_24)
    btnIdentify.isEnabled = true
    btnIdentify.alpha = 1f
  }

  private fun updateFaceCountFeedback() {
    val session = viewModel.enrollSession.value ?: return

    val nama = session.karyawan.nama.orEmpty()
    val baseInstruction = when (session.step) {
      IdentifyPemanenViewModel.EnrollStep.FRONT ->
        getString(R.string.enroll_step_front, nama)
      IdentifyPemanenViewModel.EnrollStep.LEFT ->
        getString(R.string.enroll_step_left, nama)
      IdentifyPemanenViewModel.EnrollStep.RIGHT ->
        getString(R.string.enroll_step_right, nama)
      IdentifyPemanenViewModel.EnrollStep.TEST ->
        getString(R.string.enroll_step_test, nama)
    }

    tvInstruction.text = when (detectedFaceCount) {
      0 -> getString(R.string.enroll_no_face_detected)
      1 -> baseInstruction
      else -> getString(R.string.enroll_faces_detected, detectedFaceCount)
    }
    updateCaptureButtonState()
  }

  private fun canCaptureEnrollPhoto(): Boolean {
    if (!viewModel.isEnrollActive()) return true
    if (detectedFaceCount != 1) {
      Toast.makeText(
        this,
        if (detectedFaceCount == 0) {
          getString(R.string.enroll_no_face_detected)
        } else {
          getString(R.string.enroll_one_face_required)
        },
        Toast.LENGTH_SHORT
      ).show()
      return false
    }
    return true
  }

  private fun updateCaptureButtonState() {
    if (!viewModel.isEnrollActive()) return
    val enabled = detectedFaceCount == 1
    btnIdentify.isEnabled = enabled
    btnIdentify.alpha = if (enabled) 1f else 0.5f
  }

  private fun cancelEnrollSession() {
    viewModel.cancelEnrollSession()
    resetInstructionUi()
  }

  private fun restartEnrollFromBeginning() {
    val karyawan = viewModel.enrollSession.value?.karyawan ?: return
    viewModel.startEnrollSession(karyawan)
  }

  private fun showIdentifyResultBottomSheet(
    nama: String,
    nik: String,
    kemandoran: String,
    confidence: Float,
    isSuccess: Boolean
  ) {
    resultBottomSheet?.dismiss()
    val bottomSheet = BottomSheetDialog(this)
    val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_identify_result, null)
    bottomSheet.setContentView(sheetView)
    resultBottomSheet = bottomSheet
    resultSheetView = sheetView

    sheetView.findViewById<TextView>(R.id.tvResultTitle).text =
      if (isSuccess) getString(R.string.pemanen_dikenali) else getString(R.string.pemanen_tidak_dikenali)
    sheetView.findViewById<TextView>(R.id.tvResultNama).text = nama
    sheetView.findViewById<TextView>(R.id.tvResultNik).text = getString(R.string.nik_label, nik)
    sheetView.findViewById<TextView>(R.id.tvResultKemandoran).text =
      getString(R.string.kemandoran_label, kemandoran.ifBlank { "-" })
    sheetView.findViewById<TextView>(R.id.tvResultConfidence).text =
      getString(R.string.tingkat_kecocokan, (confidence * 100).toInt())

    bindPanenInfoToSheet(sheetView, viewModel.panenInfo.value)

    sheetView.findViewById<MaterialButton>(R.id.btnSelectPanenDate).setOnClickListener {
      val date = viewModel.selectedPanenDate.value ?: viewModel.getYesterdayDate()
      openDatePicker(date) { backendDate ->
        viewModel.setSelectedPanenDate(backendDate)
        updateMainDateButton(backendDate)
        viewModel.loadPanenInfo(nik, backendDate)
      }
    }

    sheetView.findViewById<MaterialButton>(R.id.btnCloseResult).setOnClickListener {
      bottomSheet.dismiss()
      resultBottomSheet = null
      resultSheetView = null
      tvInstruction.text = getString(R.string.identify_pemanen_instruction)
    }

    bottomSheet.show()
  }

  override fun onDestroy() {
    cameraProvider?.unbindAll()
    cameraProvider = null
    resultBottomSheet?.dismiss()
    resultBottomSheet = null
    resultSheetView = null
    textToSpeech?.stop()
    textToSpeech?.shutdown()
    textToSpeech = null
    cameraExecutor.shutdown()
    analysisExecutor.shutdown()
    super.onDestroy()
  }
}
