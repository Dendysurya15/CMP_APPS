package com.cbi.mobile_plantation.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object FaceRecognitionHelper {

  private const val MAX_L2_DISTANCE = 0.72f
  private const val MIN_MATCH_MARGIN = 0.04f
  private const val ENROLL_TEST_MAX_L2_DISTANCE = 0.75f

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
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .setMinFaceSize(0.15f)
    .enableTracking()
    .build()

  private val streamFaceDetector by lazy { FaceDetection.getClient(streamDetectorOptions) }

  fun detectFacesAsync(
    image: InputImage,
    onSuccess: (List<Face>) -> Unit,
    onFailure: () -> Unit = {}
  ) {
    streamFaceDetector.process(image)
      .addOnSuccessListener(onSuccess)
      .addOnFailureListener { onFailure() }
  }

  data class FaceMatchResult(
    val karyawanId: Int,
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
      FaceEmbedder.computeEmbedding(aligned)
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

  fun matchesEnrollment(probeEmbedding: FloatArray, referenceEmbedding: FloatArray): Boolean {
    return FaceEmbedder.l2Distance(probeEmbedding, referenceEmbedding) <= ENROLL_TEST_MAX_L2_DISTANCE
  }

  suspend fun buildEmbedding(bitmap: Bitmap, rotationDegrees: Int): FloatArray? {
    val face = detectPrimaryFace(bitmap, rotationDegrees) ?: return null
    return extractEmbedding(bitmap, face, rotationDegrees)
  }

  fun embeddingToString(embedding: FloatArray): String =
    FaceEmbedder.serializeEmbedding(embedding)

  fun stringToEmbedding(value: String): FloatArray? =
    FaceEmbedder.deserializeEmbedding(value)

  fun findBestMatch(
    probeEmbedding: FloatArray,
    enrolledFaces: List<Triple<Int, String, FloatArray>>,
    metadata: Map<Int, Triple<String, String, String>>
  ): FaceMatchResult? {
    if (enrolledFaces.isEmpty()) return null

    val ranked = enrolledFaces
      .map { (karyawanId, _, storedEmbedding) ->
        karyawanId to FaceEmbedder.l2Distance(probeEmbedding, storedEmbedding)
      }
      .sortedBy { it.second }

    val (bestId, bestDistance) = ranked.first()
    if (bestDistance > MAX_L2_DISTANCE) return null

    if (ranked.size > 1) {
      val secondDistance = ranked[1].second
      val secondIsAlsoValid = secondDistance <= MAX_L2_DISTANCE
      if (secondIsAlsoValid && secondDistance - bestDistance < MIN_MATCH_MARGIN) {
        return null
      }
    }

    val info = metadata[bestId] ?: return null
    val confidence = ((MAX_L2_DISTANCE - bestDistance) / MAX_L2_DISTANCE).coerceIn(0f, 1f)

    return FaceMatchResult(
      karyawanId = bestId,
      nik = info.first,
      nama = info.second,
      kemandoranNama = info.third,
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
