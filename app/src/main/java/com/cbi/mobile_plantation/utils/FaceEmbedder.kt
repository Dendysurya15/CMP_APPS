package com.cbi.mobile_plantation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Aligns faces to a canonical pose and builds appearance-based embeddings.
 * Much more discriminative than raw landmark coordinates.
 */
object FaceEmbedder {

  const val EMBEDDING_VERSION = 2
  private const val OUTPUT_SIZE = 112
  private const val GRID_SIZE = 7
  private const val BINS_PER_CELL = 8
  const val EMBEDDING_SIZE = GRID_SIZE * GRID_SIZE * BINS_PER_CELL

  fun embeddingVersionPrefix(): String = "v$EMBEDDING_VERSION:"

  fun alignAndCropFace(source: Bitmap, face: Face): Bitmap {
    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

    val working = if (leftEye != null && rightEye != null) {
      val dx = rightEye.x - leftEye.x
      val dy = rightEye.y - leftEye.y
      val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
      val centerX = (leftEye.x + rightEye.x) / 2f
      val centerY = (leftEye.y + rightEye.y) / 2f
      val eyeDistance = max(sqrt(dx * dx + dy * dy), 1f)
      val targetEyeDistance = OUTPUT_SIZE * 0.38f
      val scale = targetEyeDistance / eyeDistance

      val matrix = Matrix().apply {
        postTranslate(-centerX, -centerY)
        postScale(scale, scale)
        postRotate(-angle)
        postTranslate(OUTPUT_SIZE / 2f, OUTPUT_SIZE * 0.42f)
      }

      val transformed = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
      Canvas(transformed).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
      transformed
    } else {
      val box = face.boundingBox
      val padX = (box.width() * 0.25f).toInt()
      val padY = (box.height() * 0.30f).toInt()
      val cropRect = Rect(
        (box.left - padX).coerceAtLeast(0),
        (box.top - padY).coerceAtLeast(0),
        (box.right + padX).coerceAtMost(source.width),
        (box.bottom + padY).coerceAtMost(source.height)
      )
      val cropped = Bitmap.createBitmap(
        source,
        cropRect.left,
        cropRect.top,
        cropRect.width().coerceAtLeast(1),
        cropRect.height().coerceAtLeast(1)
      )
      Bitmap.createScaledBitmap(cropped, OUTPUT_SIZE, OUTPUT_SIZE, true)
    }

    return working
  }

  fun computeEmbedding(alignedFace: Bitmap): FloatArray {
    val resized = if (alignedFace.width != OUTPUT_SIZE || alignedFace.height != OUTPUT_SIZE) {
      Bitmap.createScaledBitmap(alignedFace, OUTPUT_SIZE, OUTPUT_SIZE, true)
    } else {
      alignedFace
    }

    val gray = FloatArray(OUTPUT_SIZE * OUTPUT_SIZE)
    val pixels = IntArray(OUTPUT_SIZE * OUTPUT_SIZE)
    resized.getPixels(pixels, 0, OUTPUT_SIZE, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE)

    for (i in pixels.indices) {
      val color = pixels[i]
      val r = Color.red(color)
      val g = Color.green(color)
      val b = Color.blue(color)
      gray[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }

    val cellW = OUTPUT_SIZE / GRID_SIZE
    val cellH = OUTPUT_SIZE / GRID_SIZE
    val embedding = FloatArray(EMBEDDING_SIZE)

    for (gy in 0 until GRID_SIZE) {
      for (gx in 0 until GRID_SIZE) {
        val cellStart = (gy * GRID_SIZE + gx) * BINS_PER_CELL
        val histogram = FloatArray(BINS_PER_CELL)

        val xStart = gx * cellW
        val yStart = gy * cellH
        for (y in yStart until yStart + cellH) {
          for (x in xStart until xStart + cellW) {
            val idx = y * OUTPUT_SIZE + x
            val value = gray[idx]
            val bin = min((value * BINS_PER_CELL).toInt(), BINS_PER_CELL - 1)
            histogram[bin] += 1f
          }
        }

        val cellTotal = histogram.sum().coerceAtLeast(1f)
        for (b in histogram.indices) {
          embedding[cellStart + b] = histogram[b] / cellTotal
        }
      }
    }

    return l2Normalize(embedding)
  }

  fun l2Normalize(vector: FloatArray): FloatArray {
    var sum = 0f
    for (value in vector) {
      sum += value * value
    }
    val norm = sqrt(sum).coerceAtLeast(1e-6f)
    return FloatArray(vector.size) { index -> vector[index] / norm }
  }

  fun l2Distance(first: FloatArray, second: FloatArray): Float {
    if (first.size != second.size || first.isEmpty()) return Float.MAX_VALUE
    var sum = 0f
    for (i in first.indices) {
      val diff = first[i] - second[i]
      sum += diff * diff
    }
    return sqrt(sum)
  }

  fun serializeEmbedding(embedding: FloatArray): String {
    return embeddingVersionPrefix() + embedding.joinToString(",")
  }

  fun deserializeEmbedding(value: String): FloatArray? {
    if (!value.startsWith(embeddingVersionPrefix())) return null
    val payload = value.removePrefix(embeddingVersionPrefix())
    if (payload.isBlank()) return null
    return try {
      val embedding = payload.split(",").map { it.toFloat() }.toFloatArray()
      if (embedding.size != EMBEDDING_SIZE) null else embedding
    } catch (_: Exception) {
      null
    }
  }
}
