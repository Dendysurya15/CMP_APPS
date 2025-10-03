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


    private val outerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.orange) and 0x80FFFFFF.toInt() // Yellow with transparency
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

    // Load your arrow image
    private val arrowBitmap: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.arrow_direction  // Replace with your drawable name
    )

    private var pulseRadius = 0f
    private val minRadius = 30f
    private val maxRadius = 80f

    private val animator = ValueAnimator.ofFloat(minRadius, maxRadius).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animation ->
            pulseRadius = animation.animatedValue as Float
            mapView.invalidate()
        }
    }

    init {
        animator.start()
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

        // Draw pulsing outer circle
        outerCirclePaint.alpha = ((1 - (pulseRadius - minRadius) / (maxRadius - minRadius)) * 180).toInt()
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

        // Draw white outline - thicker version
        val offsetPositions = listOf(
            // Inner ring
            Pair(-1f, -1f), Pair(0f, -1f), Pair(1f, -1f),
            Pair(-1f, 0f), Pair(1f, 0f),
            Pair(-1f, 1f), Pair(0f, 1f), Pair(1f, 1f),
            // Outer ring for more thickness
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

        canvas.drawBitmap(
            arrowBitmap,
            null,
            rect,
            null
        )

        canvas.restore()
    }

    fun stopAnimation() {
        animator.cancel()
    }
}