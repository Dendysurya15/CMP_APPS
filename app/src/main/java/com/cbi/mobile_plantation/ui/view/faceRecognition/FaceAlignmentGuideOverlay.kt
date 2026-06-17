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
    color = Color.parseColor("#99000000")
    style = Paint.Style.FILL
  }

  private val ovalBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 10f
    strokeCap = Paint.Cap.ROUND
  }

  private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 18f
    alpha = 90
  }

  private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 42f
    textAlign = Paint.Align.CENTER
    isFakeBoldText = true
    setShadowLayer(6f, 0f, 2f, Color.parseColor("#CC000000"))
  }

  private val clipPath = Path()
  private val guideOval = RectF()
  private var alignmentState = FaceAlignmentHelper.AlignmentState.NO_FACE
  private var lockedOvalWidth = 0f
  private var lockedOvalHeight = 0f
  private var isOvalSizeLocked = false

  companion object {
    private const val OVAL_ASPECT = 1.45f
    private const val MIN_LOCK_HEIGHT_PX = 240
  }

  init {
    setWillNotDraw(false)
    setBackgroundColor(Color.TRANSPARENT)
  }

  fun getGuideOval(): RectF = RectF(guideOval)

  fun setAlignmentState(state: FaceAlignmentHelper.AlignmentState) {
    if (alignmentState != state) {
      alignmentState = state
      postInvalidateOnAnimation()
    }
  }

  fun resetOvalLock() {
    isOvalSizeLocked = false
    lockedOvalWidth = 0f
    lockedOvalHeight = 0f
    if (width > 0 && height > 0) {
      updateGuideOval(width, height)
      postInvalidateOnAnimation()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (w == 0 || h == 0) return
    updateGuideOval(w, h)
  }

  private fun updateGuideOval(w: Int, h: Int) {
    if (!isOvalSizeLocked) {
      if (h < MIN_LOCK_HEIGHT_PX) return
      val computed = computeOvalSize(w, h)
      lockedOvalWidth = computed.width()
      lockedOvalHeight = computed.height()
      isOvalSizeLocked = lockedOvalWidth > 0f && lockedOvalHeight > 0f
    }

    if (!isOvalSizeLocked) return

    val centerX = w / 2f
    val centerY = h / 2f
    guideOval.set(
      centerX - lockedOvalWidth / 2f,
      centerY - lockedOvalHeight / 2f,
      centerX + lockedOvalWidth / 2f,
      centerY + lockedOvalHeight / 2f
    )
  }

  private fun computeOvalSize(w: Int, h: Int): RectF {
    val verticalMargin = h * 0.08f
    val maxHeight = (h - verticalMargin * 2f).coerceAtMost(h * 0.78f)
    val maxWidth = w * 0.72f

    var ovalHeight = maxHeight
    var ovalWidth = ovalHeight / OVAL_ASPECT
    if (ovalWidth > maxWidth) {
      ovalWidth = maxWidth
      ovalHeight = ovalWidth * OVAL_ASPECT
    }

    return RectF(0f, 0f, ovalWidth, ovalHeight)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (width == 0 || height == 0 || guideOval.isEmpty) return

    clipPath.reset()
    clipPath.fillType = Path.FillType.EVEN_ODD
    clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
    clipPath.addOval(guideOval, Path.Direction.CCW)
    canvas.drawPath(clipPath, dimPaint)

    val borderColor = borderColor(alignmentState)
    ovalBorderPaint.color = borderColor

    if (alignmentState == FaceAlignmentHelper.AlignmentState.ALIGNED) {
      glowPaint.color = borderColor
      canvas.drawOval(guideOval, glowPaint)
    }

    canvas.drawOval(guideOval, ovalBorderPaint)

    if (alignmentState == FaceAlignmentHelper.AlignmentState.ALIGNED) {
      hintPaint.alpha = 255
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
        ContextCompat.getColor(context, R.color.face_status_ready)

      FaceAlignmentHelper.AlignmentState.ALMOST ->
        ContextCompat.getColor(context, R.color.face_status_almost)

      FaceAlignmentHelper.AlignmentState.MULTIPLE_FACES ->
        ContextCompat.getColor(context, R.color.face_status_error)

      FaceAlignmentHelper.AlignmentState.NO_FACE ->
        Color.WHITE

      else ->
        ContextCompat.getColor(context, R.color.face_status_warning)
    }
  }
}
