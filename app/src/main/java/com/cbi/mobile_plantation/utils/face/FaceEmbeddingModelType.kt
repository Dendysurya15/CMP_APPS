package com.cbi.mobile_plantation.utils.face

enum class FaceEmbeddingModelType(
  val id: String,
  val assetFileName: String,
  val inputSize: Int,
  val expectedEmbeddingSize: Int
) {
  MOBILE_FACE_NET(
    id = "mfn",
    assetFileName = "face_models/mobilefacenet.tflite",
    inputSize = 112,
    expectedEmbeddingSize = 192
  );

  val versionPrefix: String get() = "$id:"

  companion object {
    val ACTIVE: FaceEmbeddingModelType = MOBILE_FACE_NET

    fun fromStoredEmbedding(value: String): FaceEmbeddingModelType? {
      return if (value.startsWith(ACTIVE.versionPrefix)) ACTIVE else null
    }
  }
}
