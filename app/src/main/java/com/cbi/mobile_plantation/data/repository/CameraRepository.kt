package com.cbi.mobile_plantation.data.repository

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.BoundingBox
import com.cbi.mobile_plantation.utils.CameraOrientationHandler
import com.cbi.mobile_plantation.utils.DetectionMetrics
import com.cbi.mobile_plantation.utils.FFBClassCountsView
import com.cbi.mobile_plantation.utils.FFBDetector
import com.cbi.mobile_plantation.utils.FFBOverlayView
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PerformanceMetrics
import com.cbi.mobile_plantation.utils.PerformanceOverlayView
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CameraRepository(
    private val context: Context,
    private val window: Window,
    private val view: View,
    private val zoomView: View
) : FFBDetector.DetectorListener {

    enum class CameraType {
        BACK,
        FRONT
    }

    interface PhotoCallback {
        fun onPhotoTaken(
            photoFile: File,
            fname: String,
            resultCode: String,
            deletePhoto: View?,
            pageForm: Int,
            komentar: String?,
            latitude: Double?,
            longitude: Double?
        )
    }

    // FFB Detection properties
    private var ffbDetector: FFBDetector? = null
    private var overlayView: FFBOverlayView? = null
    private var classCountsView: FFBClassCountsView? = null
    private var isFFBDetectionEnabled = false
    private var useGPUDetection = false
    private var lockedDetectionResults: List<BoundingBox>? = null
    private var lockedClassCounts: Map<String, Int>? = null
    private val detectionHandler = Handler(Looper.getMainLooper())
    private var detectionRunnable: Runnable? = null

    private lateinit var orientationHandler: CameraOrientationHandler
    private var photoCallback: PhotoCallback? = null
    private var prefManager: PrefManager? = null

    private var lastCameraId = 0
    private var rotatedCam = false
    private val aspectRatio = Rational(16, 9)
    private var selectedSize: Size? = null
    private lateinit var capReq: CaptureRequest.Builder
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private lateinit var cameraManager: CameraManager
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var textureViewCam: TextureView
    private var isCameraOpen = false
    private var isFlashlightOn = false

    private var performanceOverlay: PerformanceOverlayView? = null
    private var performanceCheckCounter = 0
    private var totalDetectionCount = 0
    private var lastInferenceTime = 0L

    fun setPhotoCallback(callback: PhotoCallback) {
        this.photoCallback = callback
    }

    // FFB Detection methods
    fun enableFFBDetection(enable: Boolean, useGPU: Boolean = false) {
        isFFBDetectionEnabled = enable
        useGPUDetection = useGPU

        if (enable) {
            initializeFFBDetection()
        } else {
            stopFFBDetection()
        }
    }

    private fun initializeFFBDetection() {
        if (ffbDetector == null) {
            ffbDetector = FFBDetector(context, detectorListener = this)
            if (!ffbDetector!!.initialize(useGPUDetection)) {
                Log.e("CameraRepository", "Failed to initialize FFB detector")
                ffbDetector = null
                return
            }
        }

        // Setup overlay if not exists
        if (overlayView == null) {
            overlayView = FFBOverlayView(context, null)
            val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
            rlCamera.addView(overlayView)
        }

        startContinuousDetection()
    }

    private fun startContinuousDetection() {
        if (!isFFBDetectionEnabled || ffbDetector == null) return

        detectionRunnable = object : Runnable {
            override fun run() {
                if (isFFBDetectionEnabled && isCameraOpen && lockedDetectionResults == null) {
                    performDetection()
                    detectionHandler.postDelayed(this, 100) // 10 FPS detection
                }
            }
        }
        detectionHandler.post(detectionRunnable!!)
    }

    private fun performDetection() {
        try {
            val bitmap = textureViewCam.getBitmap() ?: return
            val detectionStartTime = System.currentTimeMillis()

            ffbDetector?.detect(bitmap)

            // Monitor performance every 30 detections (approximately every 3 seconds at 10 FPS)
            performanceCheckCounter++
            if (performanceCheckCounter >= 30) {
                monitorAndUpdatePerformance()
                performanceCheckCounter = 0
            }
        } catch (e: Exception) {
            Log.e("CameraRepository", "Error performing detection", e)
        }
    }

    private fun monitorAndUpdatePerformance() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)

            val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
            val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            val usedMemoryMB = totalMemoryMB - availableMemoryMB
            val memoryUsagePercent = (usedMemoryMB * 100) / totalMemoryMB

            // Get FPS from detector (you'll need to add this method to FFBDetector)
            val currentFps = ffbDetector?.getCurrentFps() ?: 0f

            val metrics = PerformanceMetrics(
                fps = currentFps,
                memoryUsageMB = usedMemoryMB,
                memoryUsagePercent = memoryUsagePercent,
                inferenceTime = lastInferenceTime,
                totalDetections = totalDetectionCount
            )

            // Update UI on main thread
            mainHandler.post {
                performanceOverlay?.updateMetrics(metrics)
            }

            // Log for debugging
            Log.d("Performance", "FPS: $currentFps, Memory: ${usedMemoryMB}MB (${memoryUsagePercent}%)")

        } catch (e: Exception) {
            Log.e("Performance", "Error monitoring performance", e)
        }
    }

    private fun stopFFBDetection() {
        detectionRunnable?.let { detectionHandler.removeCallbacks(it) }
        overlayView?.clear()
    }

    fun lockDetectionResults() {
        // Store current detection results
        lockedDetectionResults = overlayView?.let {
            // Get current results from overlay - this would need implementation in OverlayView
            emptyList<BoundingBox>() // Placeholder
        }
        lockedClassCounts = classCountsView?.let {
            // Get current class counts - this would need implementation
            mapOf<String, Int>()
        }
    }

    fun unlockDetectionResults() {
        lockedDetectionResults = null
        lockedClassCounts = null
        if (isFFBDetectionEnabled) {
            startContinuousDetection()
        }
    }

    fun getLockedResults(): Pair<List<BoundingBox>?, Map<String, Int>?> {
        return Pair(lockedDetectionResults, lockedClassCounts)
    }

    override fun onEmptyDetect(metrics: DetectionMetrics) {
        lastInferenceTime = metrics.inferenceTime

        mainHandler.post {
            if (lockedDetectionResults == null) {
                overlayView?.clear()
                classCountsView?.clear()
            }
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, metrics: DetectionMetrics) {
        totalDetectionCount += boundingBoxes.size
        lastInferenceTime = metrics.inferenceTime

        mainHandler.post {
            if (lockedDetectionResults == null) {
                overlayView?.setResults(boundingBoxes)
                classCountsView?.updateClassCounts(metrics.classCounts)
            }
        }
    }

    private fun rotateBitmapWithOrientation(
        photoFilePath: String?,
        cameraId: Int,
        orientationHandler: CameraOrientationHandler
    ): Bitmap {
        val TAG = "BitmapRotation"

        val originalBitmap = BitmapFactory.decodeFile(photoFilePath)

        val rotationAngle = orientationHandler.getImageRotation(cameraId)
        val isLeftHanded = orientationHandler.isLikelyLeftHanded()

        Log.d(TAG, "Original rotation from handler: ${rotationAngle}°")
        Log.d(TAG, "Hand detection: ${if (isLeftHanded) "LEFT" else "RIGHT"}")
        Log.d(TAG, "Camera ID: $cameraId")

        val correctedRotation = when {
            !isLeftHanded -> {
                when {
                    cameraId == 1 -> {
                        when (rotationAngle) {
                            180 -> 0
                            0 -> 180
                            90 -> 270
                            270 -> 90
                            else -> rotationAngle
                        }
                    }

                    else -> {
                        when (rotationAngle) {
                            180 -> 0
                            0 -> 0
                            90 -> 90
                            270 -> 270
                            else -> rotationAngle
                        }
                    }
                }
            }

            else -> {
                when {
                    cameraId == 1 -> {
                        when (rotationAngle) {
                            180 -> 0
                            0 -> 180
                            90 -> 270
                            270 -> 90
                            else -> rotationAngle
                        }
                    }

                    else -> {
                        when (rotationAngle) {
                            180 -> 180
                            0 -> 180
                            90 -> 270
                            270 -> 90
                            else -> rotationAngle
                        }
                    }
                }
            }
        }

        Log.d(
            TAG,
            "Corrected rotation for ${if (isLeftHanded) "LEFT" else "RIGHT"} hand: ${correctedRotation}°"
        )

        if (correctedRotation == 0) {
            return originalBitmap
        }

        val matrix = Matrix()
        matrix.setRotate(
            correctedRotation.toFloat(),
            originalBitmap.width / 2f,
            originalBitmap.height / 2f
        )

        return Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
    }

    private fun isInPortraitMode(orientationHandler: CameraOrientationHandler): Boolean {
        val deviceOrientation = orientationHandler.getCurrentOrientation()
        return deviceOrientation == 0 || deviceOrientation == 180
    }

    private fun addToGallery(photoFile: File) {
        val galleryIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val picUri = Uri.fromFile(photoFile)
        galleryIntent.data = picUri
        context.sendBroadcast(galleryIntent)
    }

    private fun addWatermark(bitmap: Bitmap, watermarkText: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val textPaint = Paint()
        textPaint.color = Color.YELLOW
        textPaint.textSize = if (width > height) {
            width
        } else {
            height
        } / 48f
        textPaint.textAlign = Paint.Align.RIGHT

        val backgroundPaint = Paint()
        backgroundPaint.color = Color.parseColor("#3D000000")

        val watermarkLines = watermarkText.split("\n")
        val textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top

        val x = width - width / 40f
        val y = height - (textHeight * watermarkLines.size) - height / 40f

        var maxWidth = 0f
        for (line in watermarkLines) {
            val lineWidth = textPaint.measureText(line)
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth
            }
        }

        val backgroundWidth = maxWidth
        for (i in watermarkLines.indices) {
            val line = watermarkLines[i]
            val lineY = y + (textHeight * (i + 1))
            canvas.drawRect(x - backgroundWidth, lineY - textHeight, x, lineY, backgroundPaint)
            canvas.drawText(line, x, lineY, textPaint)
        }

        return resultBitmap
    }

    private var blockingView: View? = null

    private fun isTouchOnView(view: View?, x: Float, y: Float): Boolean {
        if (view == null) return false

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val viewX = location[0]
        val viewY = location[1]

        return (x >= viewX && x <= viewX + view.width &&
                y >= viewY && y <= viewY + view.height)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun takeCameraPhotos(
        context: Context,
        resultCode: String,
        imageView: ImageView,
        pageForm: Int,
        deletePhoto: View?,
        komentar: String? = null,
        kodeFoto: String,
        featureName: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        sourceFoto: String,
        cameraType: CameraType = CameraType.BACK
    ) {
        lastCameraId = when (cameraType) {
            CameraType.BACK -> 0
            CameraType.FRONT -> 1
        }

        val shouldEnableFFB = featureName != "Mutu Buah"
        if (shouldEnableFFB) {
            enableFFBDetection(true, useGPUDetection)
        }

        orientationHandler = CameraOrientationHandler(context)
        orientationHandler.startListening()
        prefManager = PrefManager(context)
        setDefaultIconTorchButton(view)
        loadingDialog = LoadingDialog(context)

        val rootDCIM = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "CMP-$featureName"
        ).toString()

        val rootApp = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-$featureName"
        ).toString()

        AppLogger.d(rootApp)

        view.visibility = View.VISIBLE
        textureViewCam = TextureView(context)

        val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
        rlCamera.addView(textureViewCam)

        if (shouldEnableFFB) {
            setupFFBDetectionUI(rlCamera)
        }

        val captureButton = view.findViewById<FloatingActionButton>(R.id.captureCam)
        val torchButton = view.findViewById<Button>(R.id.torchButton)
        val switchButton = view.findViewById<Button>(R.id.switchButton)

        if (shouldEnableFFB) {
            setupFFBControls(view)
        }

        val blockingView = View(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)

            setOnTouchListener { _, event ->
                val x = event.rawX
                val y = event.rawY

                val isTouchOnCapture = isTouchOnView(captureButton, x, y)
                val isTouchOnTorch = isTouchOnView(torchButton, x, y)
                val isTouchOnSwitch = isTouchOnView(switchButton, x, y)

                !(isTouchOnCapture || isTouchOnTorch || isTouchOnSwitch)
            }
        }

        (view.parent as ViewGroup).addView(blockingView)
        this.blockingView = blockingView

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread!!.start()
        handler = Handler((handlerThread)!!.looper)

        textureViewCam.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                @SuppressLint("MissingPermission")
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    cameraManager.openCamera(
                        cameraManager.cameraIdList[lastCameraId],
                        object : CameraDevice.StateCallback() {
                            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                            @SuppressLint("SimpleDateFormat")
                            override fun onOpened(p0: CameraDevice) {
                                var fileName = ""
                                lateinit var file: File

                                cameraDevice = p0
                                isCameraOpen = true

                                capReq =
                                    cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                                val characteristics =
                                    cameraManager.getCameraCharacteristics(cameraDevice!!.id)
                                val streamConfigurationMap =
                                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                val outputSizes =
                                    streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)

                                for (size in outputSizes!!) {
                                    val rational = Rational(size.width, size.height)
                                    if (rational == aspectRatio) {
                                        selectedSize = size
                                        break
                                    }
                                }

                                selectedSize?.let { size ->
                                    val surfaceTexture = textureViewCam.surfaceTexture
                                    surfaceTexture!!.setDefaultBufferSize(size.width, size.height)
                                    val surface = Surface(surfaceTexture)
                                    capReq.addTarget(surface)

                                    setupTorchButton(view)
                                    setupSwitchButton(
                                        view, context, resultCode, imageView, pageForm,
                                        deletePhoto, komentar, kodeFoto, featureName,
                                        latitude, longitude, sourceFoto
                                    )

                                    setupImageReader(
                                        size = size,
                                        rootApp = rootApp,
                                        rootDCIM = rootDCIM,
                                        featureName = featureName,
                                        sourceFoto = sourceFoto,
                                        komentar = komentar,
                                        latitude = latitude,
                                        longitude = longitude,
                                        context = context,
                                        imageView = imageView,
                                        deletePhoto = deletePhoto,
                                        pageForm = pageForm,
                                        resultCode = resultCode
                                    )

                                    startCameraSession(surface)
                                }
                            }

                            override fun onDisconnected(p0: CameraDevice) {}
                            override fun onError(p0: CameraDevice, p1: Int) {}
                        },
                        handler
                    )
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}
            }

        blockingView.bringToFront()
        captureButton?.bringToFront()
        torchButton?.bringToFront()
        switchButton?.bringToFront()

        setupCaptureButton(view, context)
    }

    private fun setupFFBDetectionUI(rlCamera: RelativeLayout) {
        overlayView = FFBOverlayView(context, null).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }
        rlCamera.addView(overlayView)

        classCountsView = FFBClassCountsView(context, null).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                topMargin = 100
            }
        }
        rlCamera.addView(classCountsView)

        performanceOverlay = PerformanceOverlayView(context, null).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                bottomMargin = 120 // Above capture button
                leftMargin = 20
            }
        }
        rlCamera.addView(performanceOverlay)
    }

    private fun setupFFBControls(view: View) {
        val gpuButton = Button(context).apply {
            text = if (useGPUDetection) "GPU ON" else "GPU OFF"
            setBackgroundColor(if (useGPUDetection) Color.GREEN else Color.GRAY)
            setTextColor(Color.WHITE)
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                topMargin = 200
                rightMargin = 20
            }

            setOnClickListener {
                useGPUDetection = !useGPUDetection
                text = if (useGPUDetection) "GPU ON" else "GPU OFF"
                setBackgroundColor(if (useGPUDetection) Color.GREEN else Color.GRAY)

                ffbDetector?.close()
                ffbDetector = null
                initializeFFBDetection()
            }
        }

        val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
        rlCamera.addView(gpuButton)
    }

    private fun setupTorchButton(view: View) {
        val torchButton = view.findViewById<Button>(R.id.torchButton)
        torchButton.apply {
            setBackgroundResource(R.drawable.baseline_flash_on_24)
            backgroundTintList = ColorStateList.valueOf(Color.WHITE)

            setOnClickListener {
                isFlashlightOn = !isFlashlightOn
                if (isFlashlightOn) {
                    setBackgroundResource(R.drawable.baseline_flash_on_24)
                    backgroundTintList = ColorStateList.valueOf(Color.YELLOW)

                    capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                    capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 2)
                    capReq.set(CaptureRequest.SENSOR_SENSITIVITY, 800)
                    capReq.set(CaptureRequest.CONTROL_AE_LOCK, true)
                } else {
                    setDefaultIconTorchButton(view)
                    capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                    capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                    capReq.set(CaptureRequest.CONTROL_AE_LOCK, false)
                }

                cameraCaptureSession?.setRepeatingRequest(capReq.build(), null, null)
            }
        }
    }

    private fun setupSwitchButton(
        view: View, context: Context, resultCode: String,
        imageView: ImageView, pageForm: Int, deletePhoto: View?,
        komentar: String?, kodeFoto: String, featureName: String?,
        latitude: Double?, longitude: Double?, sourceFoto: String
    ) {
        val switchButton = view.findViewById<Button>(R.id.switchButton)
        switchButton.setOnClickListener {
            isFlashlightOn = false
            rotatedCam = true
            closeCamera()
            setDefaultIconTorchButton(view)

            lastCameraId = if (lastCameraId == 0) 1 else 0
            val newCameraType = if (lastCameraId == 0) CameraType.BACK else CameraType.FRONT

            takeCameraPhotos(
                context, resultCode, imageView, pageForm, deletePhoto,
                komentar, kodeFoto, featureName, latitude, longitude,
                sourceFoto, newCameraType
            )
        }
    }

    private fun setupImageReader(
        size: Size, rootApp: String, rootDCIM: String,
        featureName: String?, sourceFoto: String,
        komentar: String?, latitude: Double?, longitude: Double?,
        context: Context, imageView: ImageView, deletePhoto: View?,
        pageForm: Int, resultCode: String
    ) {
        imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
        imageReader!!.setOnImageAvailableListener({ p0 ->
            val image = p0?.acquireLatestImage()
            var fileDCIM: File? = null
            var actualFile: File
            var actualFileName: String

            if (image != null) {
                if (isFFBDetectionEnabled) {
                    lockDetectionResults()
                }

                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val dirApp = File(rootApp)
                if (!dirApp.exists()) dirApp.mkdirs()

                val dirDCIM = File(rootDCIM)
                if (!dirDCIM.exists()) dirDCIM.mkdirs()

                val dateTimeFormat =
                    SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().time)
                val cleanFeatureName = featureName!!.replace(" ", "_")
                val noTPH = sourceFoto?.split(" ")?.lastOrNull() ?: ""

                actualFileName =
                    "${cleanFeatureName}_${prefManager!!.idUserLogin}_${prefManager!!.estateUserLogin}_NOTPH_${noTPH}_${dateTimeFormat}.jpg"
                actualFile = File(dirApp, actualFileName)

                fileDCIM = File(dirDCIM, actualFileName)
                if (fileDCIM.exists()) fileDCIM.delete()
                addToGallery(fileDCIM)

                if (actualFile.exists()) actualFile.delete()
                addToGallery(actualFile)

                val opStream = FileOutputStream(actualFile)
                opStream.write(bytes)
                opStream.close()
                image.close()
            } else {
                Toast.makeText(
                    context,
                    "Unable to capture image. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                closeCamera()
                return@setOnImageAvailableListener
            }

            processAndWatermarkImage(
                actualFile, fileDCIM, komentar, latitude, longitude,
                featureName, sourceFoto, context, imageView,
                deletePhoto, pageForm, resultCode, actualFileName
            )

        }, handler)
    }

    private fun processAndWatermarkImage(
        file: File, fileDCIM: File?, komentar: String?,
        latitude: Double?, longitude: Double?,
        featureName: String?, sourceFoto: String,
        context: Context, imageView: ImageView,
        deletePhoto: View?, pageForm: Int,
        resultCode: String, fileName: String
    ) {
        val takenImage = rotateBitmapWithOrientation(file.path, lastCameraId, orientationHandler)
        val dateWM = SimpleDateFormat(
            "dd MMMM yyyy HH:mm:ss",
            Locale("id", "ID")
        ).format(Calendar.getInstance().time)

        var commentWm = komentar
        commentWm = commentWm?.replace("|", ",")?.replace("\n", "")
        commentWm = AppUtils.splitStringWatermark(commentWm!!, 60)

        val locationText = if (latitude != null && longitude != null) {
            "Lat: $latitude, Lon: $longitude"
        } else ""

        val userInfo = "${sourceFoto}\n${prefManager!!.nameUserLogin}"

        val detectionInfo = if (isFFBDetectionEnabled && lockedClassCounts?.isNotEmpty() == true) {
            val totalFFB = lockedClassCounts!!.values.sum()
            "\nFFB Detected: $totalFFB"
        } else ""

        val line3 = when {
            locationText.isNotEmpty() && (resultCode != "0" && commentWm.isNotEmpty()) ->
                "$locationText - $commentWm"

            locationText.isNotEmpty() -> locationText
            resultCode != "0" && commentWm.isNotEmpty() -> commentWm
            else -> "-"
        }

        val watermarkText = "CMP-$featureName\n$userInfo\n$line3$detectionInfo\n$dateWM"
        val watermarkedBitmap = addWatermark(takenImage, watermarkText)

        try {
            val targetSizeBytes = 100 * 1024
            var quality = 100
            val minQuality = 50
            val maxWidth = 1024

            val sourceWidth = watermarkedBitmap.width
            val sourceHeight = watermarkedBitmap.height
            val maxHeight = (maxWidth.toFloat() / sourceWidth.toFloat() * sourceHeight).toInt()

            var scaledBitmap: Bitmap? = null

            while (true) {
                if (quality <= minQuality || sourceWidth <= maxWidth || sourceHeight <= maxHeight) {
                    break
                }

                val aspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
                val newWidth = maxWidth.coerceAtMost((maxHeight * aspectRatio).toInt())
                scaledBitmap =
                    Bitmap.createScaledBitmap(watermarkedBitmap, newWidth, maxHeight, true)

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

                if (outputStream.size() > targetSizeBytes) {
                    quality -= 5
                } else {
                    break
                }
            }

            try {
                val out = FileOutputStream(file)
                scaledBitmap?.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        try {
            fileDCIM?.let { dcimFile ->
                val outDCIM = FileOutputStream(dcimFile)
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outDCIM)
                outDCIM.flush()
                outDCIM.close()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        mainHandler.post {
            rotatedCam = false
            closeCamera()

            Glide.with(context).load(Uri.fromFile(file))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(imageView)

            photoCallback?.onPhotoTaken(
                file,
                fileName,
                resultCode,
                deletePhoto,
                pageForm,
                komentar,
                latitude,
                longitude
            )
        }
    }

    private fun startCameraSession(surface: Surface) {
        cameraDevice!!.createCaptureSession(
            listOf(surface, imageReader!!.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(p0: CameraCaptureSession) {
                    cameraCaptureSession = p0
                    cameraCaptureSession!!.setRepeatingRequest(capReq.build(), null, null)

                    if (isFFBDetectionEnabled) {
                        startContinuousDetection()
                    }
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {}
            },
            handler
        )
    }

    private fun setupCaptureButton(view: View, context: Context) {
        val captureCam = view.findViewById<FloatingActionButton>(R.id.captureCam)
        captureCam.setOnClickListener {
            if (isInPortraitMode(orientationHandler)) {
                vibrate(context)
                Toast.makeText(context, "Mohon putar HP ke mode landscape", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            captureCam.isEnabled = false

            if (cameraDevice != null && imageReader != null && cameraCaptureSession != null) {
                capReq = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader!!.surface)

                if (isFlashlightOn) {
                    capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                    capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 2)
                    capReq.set(CaptureRequest.SENSOR_SENSITIVITY, 800)
                    capReq.set(CaptureRequest.CONTROL_AE_LOCK, true)
                }

                cameraCaptureSession?.capture(
                    capReq.build(),
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            super.onCaptureCompleted(session, request, result)
                            Handler(Looper.getMainLooper()).postDelayed({
                                captureCam.isEnabled = true
                            }, 800)
                        }

                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure
                        ) {
                            super.onCaptureFailed(session, request, failure)
                            Handler(Looper.getMainLooper()).postDelayed({
                                captureCam.isEnabled = true
                            }, 800)
                        }
                    },
                    null
                )
            } else {
                captureCam.isEnabled = true
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    fun statusCamera(): Boolean {
        rotatedCam = false
        return isCameraOpen
    }

    fun closeCamera() {
        if (isCameraOpen) {
            stopFFBDetection()

            val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
            rlCamera.removeView(textureViewCam)

            overlayView?.let { rlCamera.removeView(it) }
            classCountsView?.let { rlCamera.removeView(it) }
            performanceOverlay?.let { rlCamera.removeView(it) } // Add this line

            // Reset performance counters
            performanceCheckCounter = 0
            totalDetectionCount = 0
            lastInferenceTime = 0L

            blockingView?.let {
                (view.parent as? ViewGroup)?.removeView(it)
                blockingView = null
            }

            cameraCaptureSession?.close()
            cameraCaptureSession?.device?.close()
            cameraCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            handlerThread?.quitSafely()
            handlerThread = null
            handler = null

            isCameraOpen = false

            if (!rotatedCam) {
                view.visibility = View.GONE
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private lateinit var loadingDialog: LoadingDialog

    fun openZoomPhotos(
        file: File,
        position: String,
        onChangePhoto: () -> Unit,
        onDeletePhoto: (String) -> Unit,
        onClosePhoto: () -> Unit = {}
    ) {
        val fotoZoom = zoomView.findViewById<ImageView>(R.id.fotoZoom)
        val backgroundView = zoomView.findViewById<View>(R.id.backgroundOverlay)

        zoomView.visibility = View.VISIBLE
        backgroundView.visibility = View.VISIBLE

        Glide.with(context)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(fotoZoom)

        zoomView.findViewById<MaterialCardView>(R.id.cardCloseZoom)?.setOnClickListener {
            onClosePhoto.invoke()
            zoomView.visibility = View.GONE
            backgroundView.visibility = View.GONE
        }

        val zoomview = zoomView.findViewById<MaterialCardView>(R.id.cardDeletePhoto)
        zoomview.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                context,
                "Hapus",
                context.getString(R.string.confirmation_dialog_title),
                context.getString(R.string.al_confirm_delete_photo),
                "warning.json",
                ContextCompat.getColor(context, R.color.greenDarker),
                function = {
                    onDeletePhoto.invoke(position)
                    zoomView.visibility = View.GONE
                    backgroundView.visibility = View.GONE
                },
                cancelFunction = {}
            )
        }

        zoomView.findViewById<MaterialCardView>(R.id.cardChangePhoto)?.setOnClickListener {
            zoomView.visibility = View.GONE
            backgroundView.visibility = View.GONE
            onChangePhoto.invoke()
        }
    }

    fun isZoomViewVisible(): Boolean {
        return zoomView.visibility == View.VISIBLE
    }

    fun closeZoomView() {
        val backgroundView = zoomView.findViewById<View>(R.id.backgroundOverlay)
        zoomView.visibility = View.GONE
        backgroundView.visibility = View.GONE
    }

    fun deletePhotoSelected(fileName: String): Boolean {
        val rootDCIM = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "CMP"
        ).toString()
        val rootApp = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()

        val dirApp = File(rootApp, "CMP")
        val dirDCIM = File(rootDCIM, "CMP")

        val fileApp = File(dirApp, fileName)
        val fileDCIM = File(dirDCIM, fileName)

        var deleted = false

        if (fileApp.exists()) {
            if (fileApp.delete()) {
                deleted = true
                Toast.makeText(
                    context,
                    "Photo deleted successfully from internal storage.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Failed to delete the photo from internal storage. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Photo not found in internal storage.", Toast.LENGTH_LONG)
                .show()
        }

        if (fileDCIM.exists()) {
            if (fileDCIM.delete()) {
                deleted = true
                Toast.makeText(context, "Photo deleted successfully from DCIM.", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(
                    context,
                    "Failed to delete the photo from DCIM. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Photo not found in DCIM.", Toast.LENGTH_LONG).show()
        }

        if (!deleted) {
            Toast.makeText(context, "Photo not found in either location.", Toast.LENGTH_LONG).show()
        }

        return deleted
    }

    private fun setDefaultIconTorchButton(view: View) {
        val torchButton = view.findViewById<Button>(R.id.torchButton)
        torchButton.setBackgroundResource(R.drawable.baseline_flash_off_24)
        torchButton.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
    }

    fun cleanup() {
        ffbDetector?.close()
        ffbDetector = null
        overlayView = null
        classCountsView = null
    }
}
