package com.cbi.mobile_plantation.utils.face

import android.graphics.Bitmap

interface FaceEmbeddingBackend {
  val modelType: FaceEmbeddingModelType
  val embeddingSize: Int
  fun computeEmbedding(alignedFace: Bitmap): FloatArray
  fun serialize(embedding: FloatArray): String
  fun deserialize(value: String): FloatArray?
  fun release()
}
