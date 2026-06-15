package com.cbi.mobile_plantation.ui.view.faceRecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.R
import kotlin.math.max

class FaceBoundingBoxOverlay @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  data class FaceBox(
    val rect: RectF,
    val confidence: Float
  )

  init {
    setWillNotDraw(false)
    setBackgroundColor(Color.TRANSPARENT)
  }

  private val boxPaint = Paint().apply {
    style = Paint.Style.STROKE
    strokeWidth = 6f
    isAntiAlias = true
  }

  private val cornerPaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = 2f
    isAntiAlias = true
  }

  private val labelPaint = Paint().apply {
    color = Color.WHITE
    textSize = 36f
    isAntiAlias = true
    isFakeBoldText = true
  }

  private val labelBackgroundPaint = Paint().apply {
    isAntiAlias = true
  }

  private var faceBoxes: List<FaceBox> = emptyList()

  fun updateFaces(boxes: List<FaceBox>) {
    faceBoxes = boxes.filter { it.rect.width() > 2f && it.rect.height() > 2f }
    postInvalidateOnAnimation()
  }

  fun clearFaces() {
    faceBoxes = emptyList()
    postInvalidateOnAnimation()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    faceBoxes.forEach { box ->
      val confidencePercent = (box.confidence * 100).toInt()
      boxPaint.color = confidenceColor(box.confidence)

      canvas.drawRect(box.rect, boxPaint)
      canvas.drawRect(box.rect, cornerPaint)

      val label = "$confidencePercent%"
      val textWidth = labelPaint.measureText(label)
      val textHeight = labelPaint.fontMetrics.let { it.descent - it.ascent }
      val padding = 6f
      val labelLeft = box.rect.left
      val labelBottom = (box.rect.top - padding).coerceAtLeast(textHeight + padding)
      val labelTop = labelBottom - textHeight - padding
      val labelRight = labelLeft + textWidth + padding * 2

      labelBackgroundPaint.color = confidenceBackgroundColor(box.confidence)
      canvas.drawRect(labelLeft, labelTop, labelRight, labelBottom, labelBackgroundPaint)
      canvas.drawText(label, labelLeft + padding, labelBottom - padding - labelPaint.fontMetrics.descent, labelPaint)
    }
  }

  private fun confidenceColor(confidence: Float): Int {
    return when {
      confidence >= 0.70f -> ContextCompat.getColor(context, R.color.greenBorder)
      confidence >= 0.50f -> Color.parseColor("#FFC107")
      else -> Color.parseColor("#FF9800")
    }
  }

  private fun confidenceBackgroundColor(confidence: Float): Int {
    return when {
      confidence >= 0.70f -> Color.parseColor("#CC2E7D32")
      confidence >= 0.50f -> Color.parseColor("#CCF57F17")
      else -> Color.parseColor("#CCE65100")
    }
  }

  companion object {

    /**
     * Maps an ML Kit face bounding box onto the overlay.
     *
     * The face box arrives in the RAW analysis-buffer coordinate space
     * ([imageWidth] x [imageHeight], unrotated). We:
     *   1. rotate the box coordinates by [rotationDegrees] into the upright/display frame,
     *   2. FILL_CENTER scale + center onto the overlay, and
     *   3. mirror horizontally for the front camera.
     *
     * Assumes PreviewView.ScaleType.FILL_CENTER and that the overlay exactly
     * overlaps the PreviewView.
     */
    fun mapFaceToOverlay(
      faceBox: Rect,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int,
      overlayWidth: Int,
      overlayHeight: Int,
      isFrontCamera: Boolean
    ): RectF {
      if (overlayWidth == 0 || overlayHeight == 0 || imageWidth == 0 || imageHeight == 0) {
        return RectF()
      }

      val uprightRect = rotateRect(faceBox, imageWidth, imageHeight, rotationDegrees)

      val uprightWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight else imageWidth
      val uprightHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth else imageHeight

      val scale = max(
        overlayWidth / uprightWidth.toFloat(),
        overlayHeight / uprightHeight.toFloat()
      )
      val offsetX = (overlayWidth - uprightWidth * scale) / 2f
      val offsetY = (overlayHeight - uprightHeight * scale) / 2f

      var left = uprightRect.left * scale + offsetX
      var top = uprightRect.top * scale + offsetY
      var right = uprightRect.right * scale + offsetX
      var bottom = uprightRect.bottom * scale + offsetY

      if (isFrontCamera) {
        val mirroredLeft = overlayWidth - right
        val mirroredRight = overlayWidth - left
        left = mirroredLeft
        right = mirroredRight
      }

      return RectF(left, top, right, bottom)
    }

    private fun rotateRect(
      rect: Rect,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int
    ): RectF {
      val corners = listOf(
        rect.left.toFloat() to rect.top.toFloat(),
        rect.right.toFloat() to rect.top.toFloat(),
        rect.left.toFloat() to rect.bottom.toFloat(),
        rect.right.toFloat() to rect.bottom.toFloat()
      )
      val rotated = corners.map { (x, y) ->
        when (rotationDegrees) {
          90 -> (imageHeight - y) to x
          180 -> (imageWidth - x) to (imageHeight - y)
          270 -> y to (imageWidth - x)
          else -> x to y
        }
      }
      val xs = rotated.map { it.first }
      val ys = rotated.map { it.second }
      return RectF(xs.min(), ys.min(), xs.max(), ys.max())
    }
  }
}
