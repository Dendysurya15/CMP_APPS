package com.cbi.mobile_plantation.ui.view.faceRecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.OutputTransform
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.R
import kotlin.math.max
import kotlin.math.min

class FaceBoundingBoxOverlay @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  init {
    setWillNotDraw(false)
    setBackgroundColor(Color.TRANSPARENT)
  }

  private val boxPaint = Paint().apply {
    color = ContextCompat.getColor(context, R.color.greenBorder)
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

  private var faceRects: List<RectF> = emptyList()

  fun updateFaces(rects: List<RectF>) {
    faceRects = rects.filter { it.width() > 2f && it.height() > 2f }
    postInvalidateOnAnimation()
  }

  fun clearFaces() {
    faceRects = emptyList()
    postInvalidateOnAnimation()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    faceRects.forEach { rect ->
      canvas.drawRect(rect, boxPaint)
      canvas.drawRect(rect, cornerPaint)
    }
  }

  companion object {
    fun mapFaceToOverlay(
      faceBox: Rect,
      sourceTransform: OutputTransform?,
      previewView: PreviewView,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int,
      overlayWidth: Int,
      overlayHeight: Int
    ): RectF {
      val previewTransform = previewView.outputTransform
      if (sourceTransform != null && previewTransform != null) {
        try {
          val rect = RectF(faceBox)
          CoordinateTransform(sourceTransform, previewTransform).mapRect(rect)
          if (rect.width() > 2f && rect.height() > 2f) {
            return rect
          }
        } catch (_: Exception) {
          // Fall through to manual mapping.
        }
      }

      return mapFaceToOverlayFallback(
        faceBox = faceBox,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        rotationDegrees = rotationDegrees,
        overlayWidth = overlayWidth,
        overlayHeight = overlayHeight
      )
    }

    private fun mapFaceToOverlayFallback(
      faceBox: Rect,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int,
      overlayWidth: Int,
      overlayHeight: Int
    ): RectF {
      if (overlayWidth == 0 || overlayHeight == 0 || imageWidth == 0 || imageHeight == 0) {
        return RectF()
      }

      val uprightRect = rotateRectToUpright(faceBox, imageWidth, imageHeight, rotationDegrees)
      val uprightWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
        imageHeight
      } else {
        imageWidth
      }
      val uprightHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
        imageWidth
      } else {
        imageHeight
      }

      val scale = max(
        overlayWidth / uprightWidth.toFloat(),
        overlayHeight / uprightHeight.toFloat()
      )
      val offsetX = (overlayWidth - uprightWidth * scale) / 2f
      val offsetY = (overlayHeight - uprightHeight * scale) / 2f

      return RectF(
        uprightRect.left * scale + offsetX,
        uprightRect.top * scale + offsetY,
        uprightRect.right * scale + offsetX,
        uprightRect.bottom * scale + offsetY
      )
    }

    private fun rotateRectToUpright(
      rect: Rect,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int
    ): RectF {
      val points = listOf(
        rect.left.toFloat() to rect.top.toFloat(),
        rect.right.toFloat() to rect.top.toFloat(),
        rect.left.toFloat() to rect.bottom.toFloat(),
        rect.right.toFloat() to rect.bottom.toFloat()
      )

      val rotated = points.map { (x, y) ->
        rotatePoint(x, y, imageWidth, imageHeight, rotationDegrees)
      }

      val xs = rotated.map { it.first }
      val ys = rotated.map { it.second }
      return RectF(
        xs.min(),
        ys.min(),
        xs.max(),
        ys.max()
      )
    }

    private fun rotatePoint(
      x: Float,
      y: Float,
      imageWidth: Int,
      imageHeight: Int,
      rotationDegrees: Int
    ): Pair<Float, Float> {
      return when (rotationDegrees) {
        90 -> y to (imageWidth - x)
        180 -> (imageWidth - x) to (imageHeight - y)
        270 -> (imageHeight - y) to x
        else -> x to y
      }
    }
  }
}
