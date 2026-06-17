package com.cbi.mobile_plantation.utils.face

import android.graphics.RectF
import com.cbi.mobile_plantation.utils.FaceRecognitionHelper
import com.google.mlkit.vision.face.Face
import kotlin.math.sqrt

/**
 * Validates whether a detected face fits the on-screen oval guide (banking-style liveness framing).
 */
object FaceAlignmentHelper {

  enum class PoseMode {
    FRONT
  }

  enum class AlignmentState {
    NO_FACE,
    MULTIPLE_FACES,
    TOO_SMALL,
    TOO_LARGE,
    OFF_CENTER,
    TURN_MORE_LEFT,
    TURN_MORE_RIGHT,
    ALMOST,
    ALIGNED
  }

  data class Result(
    val state: AlignmentState,
    val face: Face?,
    val faceRect: RectF?
  )

  private const val MIN_FILL_RATIO_FRONT = 0.26f
  /** Back camera: require a larger face in the oval (near), reject far/small faces. */
  private const val MIN_FILL_RATIO_BACK = 0.38f
  private const val MAX_FILL_RATIO_FRONT = 0.98f
  /** Back camera: allow near faces that slightly overfill the oval. */
  private const val MAX_FILL_RATIO_BACK = 1.45f
  private const val ALIGNED_CENTER_MAX_FRONT = 0.18f
  private const val ALIGNED_CENTER_MAX_BACK = 0.42f
  private const val ALMOST_CENTER_MAX_FRONT = 0.28f
  private const val ALMOST_CENTER_MAX_BACK = 0.55f
  private const val YAW_MAX_FRONT = 25f
  private const val YAW_MAX_BACK = 40f

  fun evaluate(
    scoredFaces: List<FaceRecognitionHelper.ScoredFace>,
    faceRectsInOverlay: List<RectF>,
    guideOval: RectF,
    poseMode: PoseMode,
    actionMin: Float,
    isFrontCamera: Boolean
  ): Result {
    if (guideOval.width() <= 0f || guideOval.height() <= 0f) {
      return Result(AlignmentState.NO_FACE, null, null)
    }

    val pairs = scoredFaces.mapIndexedNotNull { index, scored ->
      faceRectsInOverlay.getOrNull(index)?.let { scored to it }
    }
    val actionable = pairs.filter { it.first.confidence >= actionMin }

    if (actionable.isEmpty()) {
      return Result(AlignmentState.NO_FACE, null, null)
    }
    if (actionable.size > 1) {
      return Result(AlignmentState.MULTIPLE_FACES, null, null)
    }

    val (scored, rect) = actionable.first()
    val face = scored.face
    val minFillRatio = if (isFrontCamera) MIN_FILL_RATIO_FRONT else MIN_FILL_RATIO_BACK
    val maxFillRatio = if (isFrontCamera) MAX_FILL_RATIO_FRONT else MAX_FILL_RATIO_BACK
    val alignedCenterMax = if (isFrontCamera) ALIGNED_CENTER_MAX_FRONT else ALIGNED_CENTER_MAX_BACK
    val almostCenterMax = if (isFrontCamera) ALMOST_CENTER_MAX_FRONT else ALMOST_CENTER_MAX_BACK

    val fillRatio = faceFillRatio(rect, guideOval)
    when {
      fillRatio < minFillRatio -> return Result(AlignmentState.TOO_SMALL, face, rect)
      fillRatio > maxFillRatio -> return Result(AlignmentState.TOO_LARGE, face, rect)
    }

    val poseState = evaluateFrontPose(face.headEulerAngleY, isFrontCamera)
    if (poseState != null) {
      return Result(poseState, face, rect)
    }

    val centerDistance = normalizedCenterDistance(rect, guideOval)
    return when {
      centerDistance <= alignedCenterMax ->
        Result(AlignmentState.ALIGNED, face, rect)

      centerDistance <= almostCenterMax ->
        Result(AlignmentState.ALMOST, face, rect)

      else ->
        Result(AlignmentState.OFF_CENTER, face, rect)
    }
  }

  fun isCaptureReady(state: AlignmentState, isFrontCamera: Boolean): Boolean {
    if (isFrontCamera) {
      return state == AlignmentState.ALIGNED || state == AlignmentState.ALMOST
    }
    return when (state) {
      AlignmentState.ALIGNED,
      AlignmentState.ALMOST,
      AlignmentState.OFF_CENTER,
      AlignmentState.TOO_LARGE -> true
      else -> false
    }
  }

  private fun faceFillRatio(faceRect: RectF, guideOval: RectF): Float {
    val faceArea = faceRect.width().coerceAtLeast(1f) * faceRect.height().coerceAtLeast(1f)
    val guideArea = guideOval.width().coerceAtLeast(1f) * guideOval.height().coerceAtLeast(1f) * 0.785f
    return faceArea / guideArea
  }

  private fun normalizedCenterDistance(faceRect: RectF, guideOval: RectF): Float {
    val dx = (faceRect.centerX() - guideOval.centerX()) / guideOval.width().coerceAtLeast(1f)
    val dy = (faceRect.centerY() - guideOval.centerY()) / guideOval.height().coerceAtLeast(1f)
    return sqrt(dx * dx + dy * dy)
  }

  private fun evaluateFrontPose(yaw: Float, isFrontCamera: Boolean): AlignmentState? {
    val yawMax = if (isFrontCamera) YAW_MAX_FRONT else YAW_MAX_BACK
    return when {
      yaw > yawMax -> AlignmentState.TURN_MORE_RIGHT
      yaw < -yawMax -> AlignmentState.TURN_MORE_LEFT
      else -> null
    }
  }
}
