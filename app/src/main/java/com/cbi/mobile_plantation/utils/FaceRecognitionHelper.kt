package com.cbi.mobile_plantation.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.content.Context
import com.cbi.mobile_plantation.utils.face.FaceEmbeddingMatcher
import com.cbi.mobile_plantation.utils.face.FaceEmbeddingModelManager
import com.cbi.mobile_plantation.utils.face.FaceThresholdConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object FaceRecognitionHelper {

  private const val MAX_L2_DISTANCE = 0.72f
  private const val MIN_MATCH_MARGIN = 0.04f
  private const val ENROLL_TEST_MAX_L2_DISTANCE = 0.75f

  /** Defaults; overridden by [FaceThresholdConfig] preview sliders. */
  const val MIN_DISPLAY_CONFIDENCE = 0.30f
  const val MIN_ACTION_CONFIDENCE = 0.50f

  data class ScoredFace(
    val face: Face,
    val confidence: Float
  )

  sealed class SingleFaceValidation {
    object NoFace : SingleFaceValidation()
    data class MultipleFaces(val count: Int) : SingleFaceValidation()
    data class Valid(val face: Face) : SingleFaceValidation()
  }

  private val detectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .setMinFaceSize(0.15f)
    .enableTracking()
    .build()

  private val faceDetector by lazy { FaceDetection.getClient(detectorOptions) }

  private val streamDetectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .setMinFaceSize(0.12f)
    .build()

  private val streamFaceDetector by lazy { FaceDetection.getClient(streamDetectorOptions) }

  fun detectFacesAsync(
    context: Context,
    image: InputImage,
    imageWidth: Int,
    imageHeight: Int,
    onSuccess: (List<ScoredFace>) -> Unit,
    onFailure: () -> Unit = {}
  ) {
    streamFaceDetector.process(image)
      .addOnSuccessListener { faces ->
        onSuccess(scoreStreamFaces(context, faces, imageWidth, imageHeight))
      }
      .addOnFailureListener { onFailure() }
  }

  fun scoreStreamFaces(
    context: Context,
    faces: List<Face>,
    imageWidth: Int,
    imageHeight: Int
  ): List<ScoredFace> {
    val displayMin = FaceThresholdConfig.getPreviewThresholds(context).displayMin
    return faces
      .map { face -> ScoredFace(face, computeStreamConfidence(face, imageWidth, imageHeight)) }
      .filter { it.confidence >= displayMin }
      .sortedByDescending { it.confidence }
  }

  fun countActionableFaces(context: Context, scoredFaces: List<ScoredFace>): Int {
    val actionMin = FaceThresholdConfig.getPreviewThresholds(context).actionMin
    return scoredFaces.count { it.confidence >= actionMin }
  }

  /**
   * ML Kit does not expose a native detection confidence. This heuristic blends face size,
   * aspect ratio, landmark presence, and eye-open probabilities into a 0–1 quality score.
   */
  fun computeStreamConfidence(face: Face, imageWidth: Int, imageHeight: Int): Float {
    val box = face.boundingBox
    val faceArea = box.width() * box.height().toFloat()
    val imageArea = (imageWidth * imageHeight).toFloat().coerceAtLeast(1f)
    val areaRatio = faceArea / imageArea

    val sizeScore = ((areaRatio - 0.015f) / 0.28f).coerceIn(0f, 1f)

    val aspect = box.width().toFloat() / box.height().coerceAtLeast(1)
    val aspectScore = when {
      aspect in 0.55f..1.15f -> 1f
      aspect in 0.45f..1.35f -> 0.55f
      else -> 0.15f
    }

    var landmarkHits = 0
    if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) landmarkHits++
    if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) landmarkHits++
    if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) landmarkHits++
    val landmarkScore = landmarkHits / 3f

    val classificationScore = listOfNotNull(
      face.leftEyeOpenProbability,
      face.rightEyeOpenProbability
    ).takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0.5f

    return (sizeScore * 0.35f +
      aspectScore * 0.25f +
      landmarkScore * 0.25f +
      classificationScore * 0.15f).coerceIn(0f, 1f)
  }

  data class FaceMatchResult(
    val nik: String,
    val nama: String,
    val kemandoranNama: String,
    val confidence: Float
  )

  fun uprightBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    return rotateBitmap(bitmap, rotationDegrees)
  }

  suspend fun detectPrimaryFace(bitmap: Bitmap, rotationDegrees: Int = 0): Face? {
    val upright = uprightBitmap(bitmap, rotationDegrees)
    val image = InputImage.fromBitmap(upright, 0)
    val faces = suspendCancellableCoroutine<List<Face>> { continuation ->
      faceDetector.process(image)
        .addOnSuccessListener { result -> continuation.resume(result) }
        .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
    if (faces.isEmpty()) return null
    return faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
  }

  fun extractEmbedding(bitmap: Bitmap, face: Face, rotationDegrees: Int = 0): FloatArray? {
    return try {
      val upright = uprightBitmap(bitmap, rotationDegrees)
      val aligned = FaceEmbedder.alignAndCropFace(upright, face)
      FaceEmbeddingModelManager.computeEmbedding(aligned)
    } catch (_: Exception) {
      null
    }
  }

  suspend fun detectAllFaces(bitmap: Bitmap, rotationDegrees: Int = 0): List<Face> {
    val upright = uprightBitmap(bitmap, rotationDegrees)
    val image = InputImage.fromBitmap(upright, 0)
    return suspendCancellableCoroutine { continuation ->
      faceDetector.process(image)
        .addOnSuccessListener { result -> continuation.resume(result) }
        .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
  }

  suspend fun validateSingleFace(bitmap: Bitmap, rotationDegrees: Int = 0): SingleFaceValidation {
    val faces = detectAllFaces(bitmap, rotationDegrees)
    return when {
      faces.isEmpty() -> SingleFaceValidation.NoFace
      faces.size > 1 -> SingleFaceValidation.MultipleFaces(faces.size)
      else -> SingleFaceValidation.Valid(faces.first())
    }
  }

  suspend fun extractSingleFaceEmbedding(bitmap: Bitmap, rotationDegrees: Int): FloatArray? {
    return when (val validation = validateSingleFace(bitmap, rotationDegrees)) {
      is SingleFaceValidation.Valid ->
        extractEmbedding(bitmap, validation.face, rotationDegrees)
      else -> null
    }
  }

  fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
    if (embeddings.isEmpty()) return FloatArray(0)
    if (embeddings.size == 1) return embeddings.first()

    val size = embeddings.first().size
    val averaged = FloatArray(size)
    for (embedding in embeddings) {
      for (i in 0 until size) {
        averaged[i] += embedding[i]
      }
    }
    for (i in averaged.indices) {
      averaged[i] = averaged[i] / embeddings.size
    }
    return FaceEmbedder.l2Normalize(averaged)
  }

  fun matchesEnrollment(
    context: Context,
    probeEmbedding: FloatArray,
    referenceEmbedding: FloatArray
  ): Boolean {
    return FaceEmbeddingMatcher.isEnrollmentMatch(
      context,
      probeEmbedding,
      referenceEmbedding
    )
  }

  suspend fun buildEmbedding(bitmap: Bitmap, rotationDegrees: Int): FloatArray? {
    val face = detectPrimaryFace(bitmap, rotationDegrees) ?: return null
    return extractEmbedding(bitmap, face, rotationDegrees)
  }

  fun embeddingToString(embedding: FloatArray): String =
    FaceEmbeddingModelManager.embeddingToString(embedding)

  fun stringToEmbedding(value: String): FloatArray? =
    FaceEmbeddingModelManager.stringToEmbedding(value)

  fun findBestMatch(
    context: Context,
    probeEmbedding: FloatArray,
    enrolledFaces: List<Pair<String, FloatArray>>,
    metadata: Map<String, String>
  ): FaceMatchResult? {
    if (enrolledFaces.isEmpty()) return null

    val bestNik = FaceEmbeddingMatcher.findBestMatchKey(context, probeEmbedding, enrolledFaces)
      ?: return null
    val bestScore = FaceEmbeddingMatcher.matchScoreForCandidate(
      probeEmbedding,
      enrolledFaces.first { it.first == bestNik }.second
    )

    val nama = metadata[bestNik] ?: return null
    val confidence = FaceEmbeddingMatcher.matchConfidence(context, bestScore)

    return FaceMatchResult(
      nik = bestNik,
      nama = nama,
      kemandoranNama = "",
      confidence = confidence
    )
  }

  fun cropFace(bitmap: Bitmap, face: Face, paddingRatio: Float = 0.25f): Bitmap {
    val box = face.boundingBox
    val padX = (box.width() * paddingRatio).toInt()
    val padY = (box.height() * paddingRatio).toInt()

    val left = (box.left - padX).coerceAtLeast(0)
    val top = (box.top - padY).coerceAtLeast(0)
    val right = (box.right + padX).coerceAtMost(bitmap.width)
    val bottom = (box.bottom + padY).coerceAtMost(bitmap.height)

    val width = (right - left).coerceAtLeast(1)
    val height = (bottom - top).coerceAtLeast(1)
    return Bitmap.createBitmap(bitmap, left, top, width, height)
  }

  fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  fun close() {
    faceDetector.close()
    streamFaceDetector.close()
  }
}
