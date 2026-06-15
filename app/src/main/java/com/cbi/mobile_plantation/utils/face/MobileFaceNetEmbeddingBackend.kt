package com.cbi.mobile_plantation.utils.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.cbi.mobile_plantation.utils.FaceEmbedder
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class MobileFaceNetEmbeddingBackend(
  context: Context
) : FaceEmbeddingBackend {

  override val modelType: FaceEmbeddingModelType = FaceEmbeddingModelType.ACTIVE

  private val inputSize = modelType.inputSize
  private val interpreter: Interpreter
  private val inputBuffer: ByteBuffer

  override val embeddingSize: Int

  init {
    val modelBuffer = loadModelFile(context, modelType.assetFileName)
    interpreter = Interpreter(modelBuffer, Interpreter.Options().apply { numThreads = 4 })
    embeddingSize = interpreter.getOutputTensor(0).shape().last()
    inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
      order(ByteOrder.nativeOrder())
    }
  }

  override fun computeEmbedding(alignedFace: Bitmap): FloatArray {
    val resized = if (alignedFace.width != inputSize || alignedFace.height != inputSize) {
      Bitmap.createScaledBitmap(alignedFace, inputSize, inputSize, true)
    } else {
      alignedFace
    }

    fillInputBuffer(resized)
    val output = Array(1) { FloatArray(embeddingSize) }
    interpreter.run(inputBuffer, output)
    return FaceEmbedder.l2Normalize(output[0])
  }

  private fun fillInputBuffer(bitmap: Bitmap) {
    inputBuffer.rewind()
    val pixels = IntArray(inputSize * inputSize)
    bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
    for (pixel in pixels) {
      inputBuffer.putFloat((Color.red(pixel) - 127.5f) / 128f)
      inputBuffer.putFloat((Color.green(pixel) - 127.5f) / 128f)
      inputBuffer.putFloat((Color.blue(pixel) - 127.5f) / 128f)
    }
    inputBuffer.rewind()
  }

  override fun serialize(embedding: FloatArray): String {
    return modelType.versionPrefix + embedding.joinToString(",")
  }

  override fun deserialize(value: String): FloatArray? {
    if (!value.startsWith(modelType.versionPrefix)) return null
    val payload = value.removePrefix(modelType.versionPrefix)
    if (payload.isBlank()) return null
    return try {
      val embedding = payload.split(",").map { it.toFloat() }.toFloatArray()
      if (embedding.size != embeddingSize) null else embedding
    } catch (_: Exception) {
      null
    }
  }

  override fun release() {
    interpreter.close()
  }

  companion object {
    private fun loadModelFile(context: Context, assetPath: String): MappedByteBuffer {
      context.assets.openFd(assetPath).use { assetFd ->
        FileInputStream(assetFd.fileDescriptor).use { input ->
          val channel = input.channel
          return channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength
          )
        }
      }
    }
  }
}
