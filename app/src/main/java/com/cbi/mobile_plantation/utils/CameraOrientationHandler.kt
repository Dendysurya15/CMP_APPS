package com.cbi.mobile_plantation.utils

// Add these imports to your existing file
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface

class CameraOrientationHandler(private val context: Context) {

    private var deviceOrientation = 0
    private var orientationEventListener: OrientationEventListener? = null
    private val TAG = "CameraOrientation"
    private var lastLogTime = 0L
    private val LOG_DEBOUNCE_TIME = 500L // Only log every 500ms
    private var stableOrientation = 0
    private var orientationChangeCount = 0
    private val STABILITY_THRESHOLD = 3 // Need 3 consecutive readings before considering stable

    // Track device orientation changes
    private fun setupOrientationListener() {
        orientationEventListener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val currentTime = System.currentTimeMillis()
                val oldOrientation = deviceOrientation

                // Convert orientation to rotation degrees with wider tolerance
                val newOrientation = when {
                    orientation >= 330 || orientation < 30 -> 0      // Portrait (wider range)
                    orientation >= 60 && orientation < 120 -> 270    // Landscape Left
                    orientation >= 150 && orientation < 210 -> 180   // Portrait Upside Down
                    orientation >= 240 && orientation < 300 -> 90    // Landscape Right
                    else -> deviceOrientation // Keep current if in between ranges
                }

                // Only update if orientation actually changed and is stable
                if (newOrientation != deviceOrientation) {
                    if (newOrientation == stableOrientation) {
                        orientationChangeCount++
                    } else {
                        stableOrientation = newOrientation
                        orientationChangeCount = 1
                    }

                    // Only commit the change if it's been stable for enough readings
                    if (orientationChangeCount >= STABILITY_THRESHOLD) {
                        deviceOrientation = newOrientation
                        orientationChangeCount = 0

                        // Log with debouncing to prevent spam
                        if (currentTime - lastLogTime > LOG_DEBOUNCE_TIME) {
                            val orientationName = getOrientationName(deviceOrientation)
                            val handPreference = getHandPreferenceName()

                            Log.d(TAG, "=== STABLE ORIENTATION DETECTED ===")
                            Log.d(TAG, "Raw orientation: $orientation°")
                            Log.d(TAG, "Device orientation: $deviceOrientation° ($orientationName)")
                            Log.d(TAG, "Hand detection: $handPreference")
                            Log.d(TAG, "Is landscape: ${isLandscape()}")
                            Log.d(TAG, "==================================")

                            lastLogTime = currentTime
                        }
                    }
                }
            }
        }

        if (orientationEventListener?.canDetectOrientation() == true) {
            orientationEventListener?.enable()
            Log.d(TAG, "Orientation listener started successfully")
        } else {
            Log.w(TAG, "Cannot detect orientation - sensor not available")
        }
    }

    fun startListening() {
        Log.d(TAG, "Starting orientation detection...")
        setupOrientationListener()
    }

    fun stopListening() {
        Log.d(TAG, "Stopping orientation detection...")
        orientationEventListener?.disable()
        orientationEventListener = null
    }

    fun getCurrentOrientation(): Int = deviceOrientation

    // Helper functions for better logging
    private fun getOrientationName(orientation: Int): String {
        return when (orientation) {
            0 -> "Portrait"
            90 -> "Landscape Right"
            180 -> "Portrait Upside Down"
            270 -> "Landscape Left"
            else -> "Unknown ($orientation°)"
        }
    }

    private fun getHandPreferenceName(): String {
        return when (deviceOrientation) {
            270 -> "LEFT HAND (Landscape Left)"
            90 -> "RIGHT HAND (Landscape Right)"
            0 -> "BOTH HANDS (Portrait)"
            180 -> "BOTH HANDS (Portrait Upside Down)"
            else -> "UNKNOWN"
        }
    }

    private fun isLandscape(): Boolean {
        return deviceOrientation == 90 || deviceOrientation == 270
    }

    // Detect if user is likely holding with left hand based on orientation
    fun isLikelyLeftHanded(): Boolean {
        val isLeftHanded = when (deviceOrientation) {
            270 -> true   // Landscape left - typically left-handed grip
            90 -> false   // Landscape right - typically right-handed grip
            else -> false // Default to right-handed for portrait modes
        }

        // Only log when actually checking (during photo capture)
        Log.d(TAG, "🤚 Hand detection check: ${if (isLeftHanded) "LEFT HANDED" else "RIGHT HANDED"} (orientation: ${deviceOrientation}°)")
        return isLeftHanded
    }

    // Add a method to get current orientation without triggering detection
    fun getCurrentOrientationInfo(): String {
        return "${getOrientationName(deviceOrientation)} - ${getHandPreferenceName()}"
    }

    // Get the correct rotation needed for the image
    fun getImageRotation(cameraId: Int): Int {
        val cameraOrientation = getCameraOrientation(cameraId)

        // Calculate the rotation needed
        val rotation = when (deviceOrientation) {
            0 -> cameraOrientation // Portrait
            90 -> (cameraOrientation + 270) % 360 // Landscape right
            180 -> (cameraOrientation + 180) % 360 // Portrait upside down
            270 -> (cameraOrientation + 90) % 360 // Landscape left
            else -> cameraOrientation
        }

        Log.d(TAG, "=== ROTATION CALCULATION ===")
        Log.d(TAG, "Camera ID: $cameraId")
        Log.d(TAG, "Camera orientation: $cameraOrientation°")
        Log.d(TAG, "Device orientation: $deviceOrientation° (${getOrientationName(deviceOrientation)})")
        Log.d(TAG, "Calculated rotation: $rotation°")
        Log.d(TAG, "===========================")

        return rotation
    }

    // Make this public so we can access it from the bitmap rotation function
    fun getCameraOrientation(cameraId: Int): Int {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId.toString())
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }
}

