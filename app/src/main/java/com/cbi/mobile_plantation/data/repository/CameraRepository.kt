package com.cbi.mobile_plantation.data.repository

import android.annotation.SuppressLint
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
import android.media.ExifInterface
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
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
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class CameraRepository(
    private val context: Context,
    private val window: Window,
    private val view: View,
    private val zoomView: View
) {

    interface PhotoCallback {
        fun onPhotoTaken(
            photoFile: File,
            fname: String,
            resultCode: String,
            deletePhoto: View?,
            pageForm: Int,
            komentar: String?
        )
    }

    private var photoCallback: PhotoCallback? = null


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

    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    fun setPhotoCallback(callback: PhotoCallback) {
        this.photoCallback = callback
    }

    private fun rotateBitmapOrientation(photoFilePath: String?): Bitmap {
        // Create and configure BitmapFactory
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFilePath, bounds)
        val opts = BitmapFactory.Options()
        val bm = BitmapFactory.decodeFile(photoFilePath, opts)
        // Read EXIF Data
        var exif: ExifInterface? = null
        try {
            exif = photoFilePath?.let { ExifInterface(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientString: String? = exif?.getAttribute(ExifInterface.TAG_ORIENTATION)
        val orientation =
            orientString?.toInt() ?: ExifInterface.ORIENTATION_NORMAL
        var rotationAngle = 0
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotationAngle = 90
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotationAngle = 180
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotationAngle = 270
            }
        }
        // Rotate Bitmap
        val matrix = Matrix()
        matrix.setRotate(
            rotationAngle.toFloat(),
            bm.width.toFloat() / 2,
            bm.height.toFloat() / 2
        )
        // Return result
        return Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true)
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
        } / 32f
        textPaint.textAlign = Paint.Align.RIGHT
//        textPaint.typeface = ResourcesCompat.getFont(context, R.font.helvetica)

        val backgroundPaint = Paint()
        backgroundPaint.color = Color.parseColor("#3D000000") // Black color with 25% transparency

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

        // Check if the touch coordinates are within the view's bounds
        return (x >= viewX && x <= viewX + view.width &&
                y >= viewY && y <= viewY + view.height)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun takeCameraPhotos(
        resultCode: String,
        imageView: ImageView,
        pageForm: Int,
        deletePhoto: View?,
        komentar: String? = null,
        kodeFoto: String,
        featureName: String?,
        latitude: Double?=null,
        longitude: Double?=null
    ) {
        setDefaultIconTorchButton(view)
        loadingDialog = LoadingDialog(context)
//        val rootDCIM = File(
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
//            "CMP-$featureName" // Store under "CMP-featureName"
//        ).toString()

        val rootApp = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-$featureName" // Store under "CMP-featureName"
        ).toString()

        view.visibility = View.VISIBLE
        textureViewCam = TextureView(context)


        val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
// Add the texture view for the camera
        rlCamera.addView(textureViewCam)

        // Get references to camera control buttons
        val captureButton = view.findViewById<FloatingActionButton>(R.id.captureCam)
        val torchButton = view.findViewById<Button>(R.id.torchButton)
        val switchButton = view.findViewById<Button>(R.id.switchButton)

        // Create a transparent blocking view that covers the whole screen EXCEPT camera controls
        val blockingView = View(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)

            // This is the key part - consume touch events EXCEPT for camera controls
            setOnTouchListener { _, event ->
                // Get the touch coordinates
                val x = event.rawX
                val y = event.rawY

                // Check if touch is within any of the camera controls
                val isTouchOnCapture = isTouchOnView(captureButton, x, y)
                val isTouchOnTorch = isTouchOnView(torchButton, x, y)
                val isTouchOnSwitch = isTouchOnView(switchButton, x, y)

                // If touch is on a camera control, don't consume the event
                // Otherwise, consume it to block interaction with underlying UI
                !(isTouchOnCapture || isTouchOnTorch || isTouchOnSwitch)
            }
        }

        // Add the blocking view
        (view.parent as ViewGroup).addView(blockingView)

        // Store the blocking view reference so we can remove it later
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
                                val outputSizes = streamConfigurationMap?.getOutputSizes(
                                    ImageFormat.JPEG
                                )

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

                                    val torchButton = view.findViewById<Button>(R.id.torchButton)
                                    torchButton.apply {
                                        // Make sure it has the default icon when first created
                                        setBackgroundResource(R.drawable.baseline_flash_on_24)
                                        backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                                    }
                                    torchButton.apply {
                                        setOnClickListener {
                                            isFlashlightOn = !isFlashlightOn
                                            if (isFlashlightOn) {
                                                torchButton.setBackgroundResource(R.drawable.baseline_flash_on_24)
                                                torchButton.backgroundTintList = ColorStateList.valueOf(Color.YELLOW)

                                                // Use TORCH mode for consistent brightness
                                                capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)

                                                // Prevent auto-exposure from dimming the preview
                                                capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                                                // Increase exposure compensation to prevent dimming (values typically range from -3 to +3)
                                                capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 2)

                                                // Set a higher ISO value to increase sensor sensitivity (typical range 100-1600)
                                                capReq.set(CaptureRequest.SENSOR_SENSITIVITY, 800)

                                                // Lock auto-exposure to prevent the camera from adjusting brightness automatically
                                                capReq.set(CaptureRequest.CONTROL_AE_LOCK, true)
                                            } else {
                                                setDefaultIconTorchButton(view)
                                                // Reset all settings when turning flash off
                                                capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                                capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                                capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                                                capReq.set(CaptureRequest.CONTROL_AE_LOCK, false)
                                                // Let the camera determine ISO automatically
                                                capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                            }

                                            // Apply the changes
                                            cameraCaptureSession!!.setRepeatingRequest(
                                                capReq.build(),
                                                null,
                                                null
                                            )
                                        }
                                    }

                                    val switchButton = view.findViewById<Button>(R.id.switchButton)

                                    switchButton.apply {
                                        setOnClickListener {
                                            isFlashlightOn = false
                                            rotatedCam = true
                                            closeCamera()
                                            setDefaultIconTorchButton(view)

                                            lastCameraId = if (lastCameraId == 0) {
                                                1
                                            } else {
                                                0
                                            }

                                            takeCameraPhotos(
                                                resultCode,
                                                imageView,
                                                pageForm,
                                                deletePhoto,
                                                komentar,
                                                kodeFoto,
                                                featureName,
                                                latitude,
                                                longitude
                                            )
                                        }
                                    }

                                    imageReader =
                                        ImageReader.newInstance(
                                            size.width,
                                            size.height,
                                            ImageFormat.JPEG,
                                            1
                                        )
                                    imageReader!!.setOnImageAvailableListener({ p0 ->
                                        val image = p0?.acquireLatestImage()
                                        var fileDCIM: File? = null
                                        if (image != null) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)

                                            val dirApp = File(rootApp)
                                            if (!dirApp.exists()) dirApp.mkdirs()

//                                            val dirDCIM = File(rootDCIM)
//                                            if (!dirDCIM.exists()) dirDCIM.mkdirs()

                                            val dateFormat =
                                                SimpleDateFormat("yyyyMdd_HHmmss").format(Calendar.getInstance().time)
                                            fileName =
                                                "${featureName}_${kodeFoto}_${dateFormat}.jpg"
                                            file = File(dirApp, fileName)

//                                            fileDCIM = File(dirDCIM, fileName)
//                                            if (fileDCIM.exists()) fileDCIM.delete()
//                                            addToGallery(fileDCIM)

                                            if (file.exists()) file.delete()
                                            addToGallery(file)

                                            val opStream = FileOutputStream(file)
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
                                        }

                                        val takenImage = rotateBitmapOrientation(file.path)
                                        val dateWM = SimpleDateFormat(
                                            "dd MMMM yyyy HH:mm:ss",
                                            Locale("id", "ID")
                                        ).format(Calendar.getInstance().time)

                                        var commentWm = komentar
                                        commentWm = commentWm?.replace("|", ",")?.replace("\n", "")
                                        commentWm = AppUtils.splitStringWatermark(commentWm!!, 60)

                                        // Build the location string if coordinates are available
                                        val locationText =
                                            if (latitude != null && longitude != null) {
                                                "Lat: $latitude, Lon: $longitude"
                                            } else {
                                                ""
                                            }

                                        val watermarkText = when {
                                            resultCode == "0" || commentWm.isEmpty() -> {
                                                if (locationText.isNotEmpty()) {
                                                    "CMP-$featureName\n$locationText\n${dateWM}"
                                                } else {
                                                    "CMP-$featureName\n${dateWM}"
                                                }
                                            }
                                            else -> {
                                                if (locationText.isNotEmpty()) {
                                                    "CMP-$featureName\n$locationText\n${commentWm}\n${dateWM}"
                                                } else {
                                                    "CMP-$featureName\n${commentWm}\n${dateWM}"
                                                }
                                            }
                                        }

                                        val watermarkedBitmap =
                                            addWatermark(takenImage, watermarkText)
                                        try {
                                            val targetSizeBytes = 100 * 1024
                                            var quality = 100
                                            val minQuality = 50
                                            val maxWidth = 1024

                                            val sourceWidth = watermarkedBitmap.width
                                            val sourceHeight = watermarkedBitmap.height

                                            val maxHeight =
                                                (maxWidth.toFloat() / sourceWidth.toFloat() * sourceHeight).toInt()

                                            var scaledBitmap: Bitmap? = null

                                            while (true) {
                                                if (quality <= minQuality || sourceWidth <= maxWidth || sourceHeight <= maxHeight) {
                                                    break
                                                }

                                                val aspectRatio =
                                                    sourceWidth.toFloat() / sourceHeight.toFloat()
                                                val newWidth =
                                                    maxWidth.coerceAtMost((maxHeight * aspectRatio).toInt())
                                                scaledBitmap = Bitmap.createScaledBitmap(
                                                    watermarkedBitmap,
                                                    newWidth,
                                                    maxHeight,
                                                    true
                                                )

                                                val outputStream = ByteArrayOutputStream()
                                                scaledBitmap.compress(
                                                    Bitmap.CompressFormat.JPEG,
                                                    quality,
                                                    outputStream
                                                )

                                                if (outputStream.size() > targetSizeBytes) {
                                                    quality -= 5
                                                } else {
                                                    break
                                                }
                                            }

                                            try {
                                                val out = FileOutputStream(file)
                                                scaledBitmap?.compress(
                                                    Bitmap.CompressFormat.JPEG,
                                                    quality,
                                                    out
                                                )
                                                out.flush()
                                                out.close()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }

                                        try {
                                            val outDCIM = FileOutputStream(fileDCIM)
                                            watermarkedBitmap.compress(
                                                Bitmap.CompressFormat.JPEG,
                                                100,
                                                outDCIM
                                            )
                                            outDCIM.flush()
                                            outDCIM.close()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }


                                        mainHandler.post {
                                            rotatedCam = false
                                            closeCamera()

                                            Glide.with(context).load(Uri.fromFile(file))
                                                .diskCacheStrategy(
                                                    DiskCacheStrategy.NONE
                                                ).skipMemoryCache(true).centerCrop()
                                                .into(imageView)



                                            photoCallback?.onPhotoTaken(
                                                file,
                                                fileName,
                                                resultCode,
                                                deletePhoto,
                                                pageForm,
                                                komentar
                                            )
                                        }
                                    }, handler)

                                    cameraDevice!!.createCaptureSession(
                                        listOf(surface, imageReader!!.surface),
                                        object : CameraCaptureSession.StateCallback() {
                                            override fun onConfigured(p0: CameraCaptureSession) {
                                                cameraCaptureSession = p0
                                                cameraCaptureSession!!.setRepeatingRequest(
                                                    capReq.build(),
                                                    null,
                                                    null
                                                )
                                            }

                                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                                            }
                                        },
                                        handler
                                    )
                                }
                            }

                            override fun onDisconnected(p0: CameraDevice) {

                            }

                            override fun onError(p0: CameraDevice, p1: Int) {

                            }
                        },
                        handler
                    )
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

                }

            }

        blockingView.bringToFront()

        captureButton?.bringToFront()
        torchButton?.bringToFront()
        switchButton?.bringToFront()

        val captureCam = view.findViewById<FloatingActionButton>(R.id.captureCam)
        captureCam.apply {
            setOnClickListener {
                isEnabled = false

                loadingDialog.show()
                loadingDialog.setMessage("Sedang mengambil foto...", isAnimate = true)

                if (cameraDevice != null && imageReader != null && cameraCaptureSession != null) {
                    capReq = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    capReq.addTarget(imageReader!!.surface)

                    // Apply the same flash settings for the actual photo capture
                    if (isFlashlightOn) {
                        capReq.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                        capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        capReq.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 2)
                        capReq.set(CaptureRequest.SENSOR_SENSITIVITY, 800)
                        capReq.set(CaptureRequest.CONTROL_AE_LOCK, true)
                    }


                    cameraCaptureSession?.capture(capReq.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                            super.onCaptureCompleted(session, request, result)

                            // Simulate processing time
                            Handler(Looper.getMainLooper()).postDelayed({
                                // Dismiss loading dialog
                                loadingDialog.dismiss()

                                // Re-enable button
                                isEnabled = true
                            }, 800)
                        }

                        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                            super.onCaptureFailed(session, request, failure)

                            // Dismiss after a short delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                loadingDialog.dismiss()
                                isEnabled = true
                            }, 800)
                        }
                    }, null)
                } else {
                    // Show error and dismiss dialog if camera components are null
                    loadingDialog.addStatusMessage("Camera error", LoadingDialog.StatusType.ERROR)
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        isEnabled = true
                    }, 800)
                    Log.e("CameraError", "CameraDevice or ImageReader is null")
                }
            }
        }

    }

    fun statusCamera(): Boolean {
        rotatedCam = false
        return isCameraOpen
    }

    fun closeCamera() {
        if (isCameraOpen) {

            val rlCamera = view.findViewById<RelativeLayout>(R.id.rlCamera)
            rlCamera.removeView(textureViewCam)


            blockingView?.let {
                (view.parent as? ViewGroup)?.removeView(it)
                blockingView = null
            }

            cameraCaptureSession!!.close()
            cameraCaptureSession!!.device.close()
            cameraCaptureSession = null

            cameraDevice!!.close()
            cameraDevice = null

            imageReader!!.close()
            imageReader = null

            handlerThread!!.quitSafely()
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

    // Initialize the loading dialog in your onCreate or init method
    private fun initLoadingDialog() {
        loadingDialog = LoadingDialog(context)
    }


    fun openZoomPhotos(
        file: File,
        position: String,
        onChangePhoto: () -> Unit,
        onDeletePhoto: (String) -> Unit,
        onClosePhoto: () -> Unit = {}
    ) {
        val fotoZoom = zoomView.findViewById<ImageView>(R.id.fotoZoom)
        val backgroundView =
            zoomView.findViewById<View>(R.id.backgroundOverlay) // Add this to your layout

        // Make both visible
        zoomView.visibility = View.VISIBLE
        backgroundView.visibility = View.VISIBLE


        Glide.with(context)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(fotoZoom)

        // Your existing click listeners...
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
                cancelFunction = {

                }
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
            Toast.makeText(
                context,
                "Photo not found in internal storage.",
                Toast.LENGTH_LONG
            ).show()
        }

        if (fileDCIM.exists()) {
            if (fileDCIM.delete()) {
                deleted = true

                Toast.makeText(
                    context,
                    "Photo deleted successfully from DCIM.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Failed to delete the photo from DCIM. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Photo not found in DCIM.",
                Toast.LENGTH_LONG
            ).show()
        }

        if (!deleted) {
            Toast.makeText(
                context,
                "Photo not found in either location.",
                Toast.LENGTH_LONG
            ).show()

        }

        return deleted
    }

    private fun setDefaultIconTorchButton(view: View) {
        val torchButton = view.findViewById<Button>(R.id.torchButton)
        torchButton.setBackgroundResource(R.drawable.baseline_flash_off_24)
        torchButton.backgroundTintList =
            ColorStateList.valueOf(Color.WHITE)
    }
}