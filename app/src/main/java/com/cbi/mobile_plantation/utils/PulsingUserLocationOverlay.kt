package com.cbi.mobile_plantation.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs

class PulsingUserLocationOverlay(
    private val context: Context,
    private val mapView: MapView
) : Overlay() {

    // User location and bearing
    private var userLocation: GeoPoint? = null
    private var userBearing: Float = 0f
    private var targetBearing: Float = 0f
    private var animatedBearing: Float = 0f

    // Animation properties
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var targetLat: Double = 0.0
    private var targetLon: Double = 0.0
    private var animationProgress: Float = 0f

    // Boundary circle
    private var boundaryRadiusMeters: Float = 10f

    // Pulsing animation
    private var pulsePhase = 0f
    private var pulseAnimator: ValueAnimator? = null
    private var isAnimating = false

    // Rotation animator
    private var rotationAnimator: ValueAnimator? = null

    // Position animator
    private var positionAnimator: ValueAnimator? = null

    // Paint objects
    private val boundaryPaint = Paint().apply {
        color = Color.argb(40, 33, 150, 243)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val boundaryStrokePaint = Paint().apply {
        color = Color.argb(100, 33, 150, 243)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val pulsePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val arrowPaint = Paint().apply {
        color = Color.parseColor("#FF9800") // Orange color for arrow
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val arrowStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Direction arrow bitmap
    private var arrowBitmap: Bitmap? = null
    private var rotatedArrowBitmap: Bitmap? = null

    init {
        // Create arrow bitmap programmatically instead of loading from resources
        arrowBitmap = createArrowBitmap()
    }

    private fun createArrowBitmap(): Bitmap {
        val size = 96 // 2x larger (was 48, changed from 192)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.parseColor("#FF9800") // Orange color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f // 2x larger (was 3f, changed from 12f)
            isAntiAlias = true
        }

        // Draw arrow pointing up
        val path = Path().apply {
            moveTo(size / 2f, size * 0.2f) // Top point
            lineTo(size * 0.7f, size * 0.6f) // Right point
            lineTo(size / 2f, size * 0.5f) // Center
            lineTo(size * 0.3f, size * 0.6f) // Left point
            close()
        }

        canvas.drawPath(path, strokePaint)
        canvas.drawPath(path, paint)

        return bitmap
    }

    fun setUserLocation(location: GeoPoint, animate: Boolean = true) {
        if (animate && userLocation != null) {
            // Start smooth position animation
            targetLat = location.latitude
            targetLon = location.longitude
            animatePosition()
        } else {
            // Immediate update
            userLocation = location
            currentLat = location.latitude
            currentLon = location.longitude
            targetLat = location.latitude
            targetLon = location.longitude
        }

        if (!isAnimating) {
            startPulseAnimation()
        }

        mapView.invalidate()
    }

    fun setUserBearing(bearing: Float, animate: Boolean = true) {
        // Normalize bearing to 0-360 range
        val normalizedBearing = ((bearing % 360) + 360) % 360

        if (animate) {
            // Calculate shortest rotation path
            val diff = normalizedBearing - animatedBearing
            val adjustedDiff = when {
                diff > 180 -> diff - 360
                diff < -180 -> diff + 360
                else -> diff
            }

            targetBearing = animatedBearing + adjustedDiff
            animateRotation()
        } else {
            targetBearing = normalizedBearing
            animatedBearing = normalizedBearing
            userBearing = normalizedBearing
        }

        mapView.invalidate()
    }

    private fun animatePosition() {
        positionAnimator?.cancel()

        positionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300 // 300ms for smooth position transition
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float

                // Interpolate between current and target position
                currentLat = currentLat + (targetLat - currentLat) * animationProgress
                currentLon = currentLon + (targetLon - currentLon) * animationProgress

                userLocation = GeoPoint(currentLat, currentLon)
                mapView.invalidate()
            }

            start()
        }
    }

    private fun animateRotation() {
        rotationAnimator?.cancel()

        // Only animate if the rotation difference is significant
        val rotationDiff = abs(targetBearing - animatedBearing)
        if (rotationDiff < 1f) {
            animatedBearing = targetBearing
            userBearing = targetBearing
            return
        }

        rotationAnimator = ValueAnimator.ofFloat(animatedBearing, targetBearing).apply {
            duration = 200 // 200ms for smooth rotation
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                animatedBearing = animator.animatedValue as Float
                userBearing = animatedBearing % 360

                // Recreate rotated bitmap
                updateRotatedBitmap()
                mapView.invalidate()
            }

            start()
        }
    }

    private fun updateRotatedBitmap() {
        arrowBitmap?.let { bitmap ->
            val matrix = Matrix().apply {
                postRotate(userBearing)
            }
            rotatedArrowBitmap = Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }
    }

    fun setBoundaryMeters(meters: Float) {
        boundaryRadiusMeters = meters
        mapView.invalidate()
    }

    private fun startPulseAnimation() {
        if (isAnimating) return

        isAnimating = true
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                pulsePhase = animator.animatedValue as Float
                mapView.invalidate()
            }

            start()
        }
    }

    fun stopAnimation() {
        isAnimating = false
        pulseAnimator?.cancel()
        pulseAnimator = null
        rotationAnimator?.cancel()
        rotationAnimator = null
        positionAnimator?.cancel()
        positionAnimator = null
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val location = userLocation ?: return

        // Project geo location to screen coordinates
        val projection = mapView.projection
        val point = projection.toPixels(location, null)

        // Calculate boundary radius in pixels
        // OSMDroid doesn't have metersPerPixel(), we calculate it manually
        val zoomLevel = mapView.zoomLevelDouble
        val latitude = location.latitude

        // Calculate meters per pixel at this zoom level and latitude
        // Formula: 156543.03392 * cos(latitude) / 2^zoom
        val metersPerPixel = 156543.03392 * Math.cos(latitude * Math.PI / 180.0) / Math.pow(2.0, zoomLevel)
        val boundaryRadiusPixels = (boundaryRadiusMeters / metersPerPixel).toFloat()

        // Draw pulsing circles
        drawPulsingCircles(canvas, point.x.toFloat(), point.y.toFloat(), boundaryRadiusPixels)

        // Draw boundary circle
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), boundaryRadiusPixels, boundaryPaint)
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), boundaryRadiusPixels, boundaryStrokePaint)

        // Draw user position marker (arrow or dot)
        drawUserMarker(canvas, point.x.toFloat(), point.y.toFloat())
    }

    private fun drawPulsingCircles(canvas: Canvas, x: Float, y: Float, maxRadius: Float) {
        // Draw 2 pulsing circles
        for (i in 0..1) {
            val phase = (pulsePhase + i * 0.5f) % 1f
            val radius = maxRadius * (0.3f + phase * 0.7f)
            val alpha = ((1f - phase) * 80).toInt()

            pulsePaint.color = Color.argb(alpha, 33, 150, 243)
            canvas.drawCircle(x, y, radius, pulsePaint)
        }
    }

    private fun drawUserMarker(canvas: Canvas, x: Float, y: Float) {
        // Use rotated arrow bitmap if available
        rotatedArrowBitmap?.let { bitmap ->
            val halfWidth = bitmap.width / 2f
            val halfHeight = bitmap.height / 2f
            canvas.drawBitmap(bitmap, x - halfWidth, y - halfHeight, null)
        } ?: run {
            // Fallback: draw a simple arrow using paths
            drawArrowPath(canvas, x, y, userBearing)
        }
    }

    private fun drawArrowPath(canvas: Canvas, x: Float, y: Float, bearing: Float) {
        val arrowSize = 60f // 2x larger (was 30f, changed from 120f)
        val path = Path().apply {
            // Arrow pointing up (north)
            moveTo(0f, -arrowSize)
            lineTo(arrowSize * 0.4f, arrowSize * 0.3f)
            lineTo(0f, arrowSize * 0.1f)
            lineTo(-arrowSize * 0.4f, arrowSize * 0.3f)
            close()
        }

        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(bearing)

        // Draw arrow with stroke for better visibility
        canvas.drawPath(path, arrowStrokePaint)
        canvas.drawPath(path, arrowPaint)

        canvas.restore()
    }

    override fun onDetach(mapView: MapView?) {
        stopAnimation()
        super.onDetach(mapView)
    }
}