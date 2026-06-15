package com.cbi.mobile_plantation.utils.face

import android.graphics.RectF
import com.cbi.mobile_plantation.utils.FaceRecognitionHelper
import com.google.mlkit.vision.face.Face
import kotlin.math.abs
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

  private const val MIN_FILL_RATIO = 0.26f
  private const val MAX_FILL_RATIO = 0.98f
  private const val ALIGNED_CENTER_MAX = 0.18f
  private const val ALMOST_CENTER_MAX = 0.28f
  private const val FRONT_YAW_MAX = 25f

  fun evaluate(
    scoredFaces: List<FaceRecognitionHelper.ScoredFace>,
    faceRectsInOverlay: List<RectF>,
    guideOval: RectF,
    poseMode: PoseMode,
    actionMin: Float
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

    val fillRatio = faceFillRatio(rect, guideOval)
    when {
      fillRatio < MIN_FILL_RATIO -> return Result(AlignmentState.TOO_SMALL, face, rect)
      fillRatio > MAX_FILL_RATIO -> return Result(AlignmentState.TOO_LARGE, face, rect)
    }

    val poseState = evaluateFrontPose(face.headEulerAngleY)
    if (poseState != null) {
      return Result(poseState, face, rect)
    }

    val centerDistance = normalizedCenterDistance(rect, guideOval)
    return when {
      centerDistance <= ALIGNED_CENTER_MAX ->
        Result(AlignmentState.ALIGNED, face, rect)

      centerDistance <= ALMOST_CENTER_MAX ->
        Result(AlignmentState.ALMOST, face, rect)

      else ->
        Result(AlignmentState.OFF_CENTER, face, rect)
    }
  }

  fun isCaptureReady(state: AlignmentState): Boolean =
    state == AlignmentState.ALIGNED || state == AlignmentState.ALMOST

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

  private fun evaluateFrontPose(yaw: Float): AlignmentState? {
    return when {
      yaw > FRONT_YAW_MAX -> AlignmentState.TURN_MORE_RIGHT
      yaw < -FRONT_YAW_MAX -> AlignmentState.TURN_MORE_LEFT
      else -> null
    }
  }
}
