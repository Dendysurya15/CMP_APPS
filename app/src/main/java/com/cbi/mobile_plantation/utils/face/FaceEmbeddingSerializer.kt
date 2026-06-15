package com.cbi.mobile_plantation.utils.face

object FaceEmbeddingSerializer {

  fun parse(value: String): Pair<FaceEmbeddingModelType, FloatArray>? {
    val type = FaceEmbeddingModelType.fromStoredEmbedding(value) ?: return null
    val payload = value.removePrefix(type.versionPrefix)
    if (payload.isBlank()) return null
    return try {
      val embedding = payload.split(",").map { it.toFloat() }.toFloatArray()
      if (embedding.size != type.expectedEmbeddingSize) null else type to embedding
    } catch (_: Exception) {
      null
    }
  }
}
