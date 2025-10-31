import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.util.GeoPoint

class PulsingUserLocationOverlay(
    private val context: Context,
    private val mapView: MapView
) : Overlay() {
    private var userLocation: GeoPoint? = null
    private var userBearing: Float = 0f
    private var boundaryMeters: Float = 15f // Default 15 meters

    private val outerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.orange) and 0x80FFFFFF.toInt()
    }

    private val innerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.orange)
    }

    private val whiteBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val arrowBitmap: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.arrow_direction
    )

    private var pulseRadius = 0f
    private val minRadiusMultiplier = 0.3f // 30% of max radius
    private var maxRadiusPixels = 80f // Will be calculated based on meters

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            pulseRadius = maxRadiusPixels * minRadiusMultiplier +
                    (maxRadiusPixels * (1 - minRadiusMultiplier) * progress)
            mapView.invalidate()
        }
    }

    init {
        animator.start()
    }

    // Set boundary distance in meters
    fun setBoundaryMeters(meters: Float) {
        boundaryMeters = meters
    }

    // Convert meters to pixels based on current map zoom
    private fun metersToPixels(meters: Float): Float {
        val location = userLocation ?: return 80f

        // Calculate meters per pixel at current zoom level
        val projection = mapView.projection
        val centerPoint = projection.toPixels(location, null)

        // Create a point 'meters' away from user location (latitude offset)
        val metersPerDegree = 111320.0 // meters per degree of latitude
        val latOffset = meters / metersPerDegree
        val offsetLocation = GeoPoint(location.latitude + latOffset, location.longitude)
        val offsetPoint = projection.toPixels(offsetLocation, null)

        // Calculate pixel distance
        val dx = (offsetPoint.x - centerPoint.x).toFloat()
        val dy = (offsetPoint.y - centerPoint.y).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun setUserLocation(location: GeoPoint) {
        userLocation = location
    }

    fun setUserBearing(bearing: Float) {
        userBearing = bearing
        mapView.invalidate()
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow || canvas == null || mapView == null) return

        val location = userLocation ?: return
        val point = mapView.projection.toPixels(location, null)

        // Calculate max radius based on boundary meters
        maxRadiusPixels = metersToPixels(boundaryMeters)

        // Draw pulsing outer circle
        val progress = (pulseRadius - (maxRadiusPixels * minRadiusMultiplier)) /
                (maxRadiusPixels * (1 - minRadiusMultiplier))
        outerCirclePaint.alpha = ((1 - progress) * 180).toInt()
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), pulseRadius, outerCirclePaint)

        // Draw white border
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 22f, whiteBorderPaint)

        // Draw inner yellow dot
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 18f, innerCirclePaint)

        // Draw arrow image with white outline
        canvas.save()
        canvas.rotate(userBearing, point.x.toFloat(), point.y.toFloat())

        val arrowSize = 40f
        val left = point.x - arrowSize / 2
        val top = point.y - arrowSize - 30f
        val right = left + arrowSize
        val bottom = top + arrowSize

        val rect = RectF(left, top, right, bottom)

        val offsetPositions = listOf(
            Pair(-1f, -1f), Pair(0f, -1f), Pair(1f, -1f),
            Pair(-1f, 0f), Pair(1f, 0f),
            Pair(-1f, 1f), Pair(0f, 1f), Pair(1f, 1f),
            Pair(-2f, -2f), Pair(-1f, -2f), Pair(0f, -2f), Pair(1f, -2f), Pair(2f, -2f),
            Pair(-2f, -1f), Pair(2f, -1f),
            Pair(-2f, 0f), Pair(2f, 0f),
            Pair(-2f, 1f), Pair(2f, 1f),
            Pair(-2f, 2f), Pair(-1f, 2f), Pair(0f, 2f), Pair(1f, 2f), Pair(2f, 2f)
        )

        offsetPositions.forEach { (xOffset, yOffset) ->
            canvas.drawBitmap(
                arrowBitmap,
                null,
                RectF(left + xOffset, top + yOffset, right + xOffset, bottom + yOffset),
                Paint().apply {
                    colorFilter = android.graphics.PorterDuffColorFilter(
                        Color.WHITE,
                        android.graphics.PorterDuff.Mode.SRC_ATOP
                    )
                }
            )
        }

        canvas.drawBitmap(arrowBitmap, null, rect, null)
        canvas.restore()
    }

    fun stopAnimation() {
        animator.cancel()
    }
}