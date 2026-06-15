package com.cbi.mobile_plantation.ui.view.faceRecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.face.FaceAlignmentHelper

/**
 * Banking-style face framing overlay: dimmed background with a central oval guide.
 */
class FaceAlignmentGuideOverlay @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#B3000000")
    style = Paint.Style.FILL
  }

  private val ovalBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 6f
  }

  private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 34f
    textAlign = Paint.Align.CENTER
    isFakeBoldText = true
  }

  private val clipPath = Path()
  private val guideOval = RectF()
  private var alignmentState = FaceAlignmentHelper.AlignmentState.NO_FACE

  init {
    setWillNotDraw(false)
    setBackgroundColor(Color.TRANSPARENT)
  }

  fun getGuideOval(): RectF = RectF(guideOval)

  fun setAlignmentState(state: FaceAlignmentHelper.AlignmentState) {
    if (alignmentState != state) {
      alignmentState = state
      ovalBorderPaint.color = borderColor(state)
      postInvalidateOnAnimation()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (w == 0 || h == 0) return

    // Portrait face oval (height/width ≈ 1.45), kept vertical even on short preview areas.
    val aspect = 1.45f
    val maxHeight = h * 0.62f
    val maxWidth = w * 0.62f

    var ovalHeight = maxHeight
    var ovalWidth = ovalHeight / aspect
    if (ovalWidth > maxWidth) {
      ovalWidth = maxWidth
      ovalHeight = ovalWidth * aspect
    }

    val centerX = w / 2f
    val centerY = h * 0.44f

    guideOval.set(
      centerX - ovalWidth / 2f,
      centerY - ovalHeight / 2f,
      centerX + ovalWidth / 2f,
      centerY + ovalHeight / 2f
    )
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (width == 0 || height == 0 || guideOval.isEmpty) return

    clipPath.reset()
    clipPath.fillType = Path.FillType.EVEN_ODD
    clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
    clipPath.addOval(guideOval, Path.Direction.CCW)
    canvas.drawPath(clipPath, dimPaint)

    ovalBorderPaint.color = borderColor(alignmentState)
    canvas.drawOval(guideOval, ovalBorderPaint)

    if (alignmentState == FaceAlignmentHelper.AlignmentState.ALIGNED) {
      hintPaint.alpha = 220
      canvas.drawText(
        "✓",
        guideOval.centerX(),
        guideOval.centerY() + hintPaint.textSize * 0.35f,
        hintPaint
      )
    }
  }

  private fun borderColor(state: FaceAlignmentHelper.AlignmentState): Int {
    return when (state) {
      FaceAlignmentHelper.AlignmentState.ALIGNED ->
        ContextCompat.getColor(context, R.color.greenBorder)

      FaceAlignmentHelper.AlignmentState.ALMOST ->
        Color.parseColor("#FFC107")

      FaceAlignmentHelper.AlignmentState.NO_FACE,
      FaceAlignmentHelper.AlignmentState.MULTIPLE_FACES ->
        Color.parseColor("#CCFFFFFF")

      else ->
        Color.parseColor("#FF9800")
    }
  }
}
