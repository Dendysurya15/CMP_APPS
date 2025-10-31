package com.cbi.mobile_plantation.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class FFBOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var bounds = Rect()
    private var boxTransparency: Float = 1.0f
    private var textRotation: Float = 0f

    private val classColors = arrayOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.parseColor("#FF6B35"),
        Color.parseColor("#7209B7"),
        Color.parseColor("#FF1744"),
        Color.parseColor("#00E676"),
        Color.parseColor("#FF6F00"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#009688"),
        Color.parseColor("#FF5722"),
        Color.parseColor("#795548"),
        Color.parseColor("#607D8B")
    )

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        invalidate()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 40f
        textBackgroundPaint.alpha = 200

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 40f
        textPaint.isAntiAlias = true

        boxPaint.strokeWidth = 6F
        boxPaint.style = Paint.Style.STROKE
        boxPaint.isAntiAlias = true
    }

    private fun getColorForClass(classIndex: Int): Int {
        return classColors[classIndex % classColors.size]
    }

    private fun applyTransparency(color: Int, transparency: Float): Int {
        val alpha = (255 * transparency).roundToInt().coerceIn(0, 255)
        return Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    fun setTransparency(transparency: Float) {
        boxTransparency = transparency.coerceIn(0.0f, 1.0f)
        invalidate()
    }

    fun setTextRotation(rotation: Float) {
        textRotation = rotation
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results.forEach { boundingBox ->
            val left = boundingBox.x1 * width
            val top = boundingBox.y1 * height
            val right = boundingBox.x2 * width
            val bottom = boundingBox.y2 * height

            val classColor = getColorForClass(boundingBox.cls)
            val transparentColor = applyTransparency(classColor, boxTransparency)
            boxPaint.color = transparentColor

            // Draw bounding box (not rotated)
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Prepare text
            val confidencePercent = (boundingBox.cnf * 100).roundToInt()
            val drawableText = "${boundingBox.clsName} ${confidencePercent}%"

            // Calculate text dimensions
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Set text background color
            val textBackgroundAlpha = (200 * boxTransparency).roundToInt().coerceIn(0, 255)
            textBackgroundPaint.color = Color.argb(
                textBackgroundAlpha,
                Color.red(classColor),
                Color.green(classColor),
                Color.blue(classColor)
            )

            // Calculate text position (center of text background)
            val textCenterX = left + textWidth / 2 + BOUNDING_RECT_TEXT_PADDING
            val textCenterY = top - textHeight / 2 - BOUNDING_RECT_TEXT_PADDING

            // Save canvas state
            canvas.save()

            // Rotate canvas around text center
            canvas.rotate(textRotation, textCenterX, textCenterY)

            // Draw text background (rotated)
            canvas.drawRect(
                left,
                top - textHeight - BOUNDING_RECT_TEXT_PADDING * 2,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING * 2,
                top,
                textBackgroundPaint
            )

            // Adjust text transparency
            val textAlpha = (255 * boxTransparency).roundToInt().coerceIn(128, 255)
            textPaint.color = Color.argb(textAlpha, 255, 255, 255)

            // Draw text (rotated)
            canvas.drawText(
                drawableText,
                left + BOUNDING_RECT_TEXT_PADDING,
                top - BOUNDING_RECT_TEXT_PADDING,
                textPaint
            )

            // Restore canvas state
            canvas.restore()
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}