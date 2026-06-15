package com.cbi.mobile_plantation.utils.face

import android.content.Context
import com.cbi.mobile_plantation.utils.PrefManager

object FaceEmbeddingModelManager {

  private lateinit var appContext: Context
  private var activeBackend: FaceEmbeddingBackend? = null
  private val activeType: FaceEmbeddingModelType = FaceEmbeddingModelType.ACTIVE

  fun init(context: Context) {
    if (::appContext.isInitialized && activeBackend != null) return
    appContext = context.applicationContext
    PrefManager(appContext).faceEmbeddingModelId = activeType.id
    activeBackend = MobileFaceNetEmbeddingBackend(appContext)
  }

  fun getActiveModelType(): FaceEmbeddingModelType = activeType

  fun getActiveBackend(): FaceEmbeddingBackend {
    ensureInit()
    return activeBackend ?: error("Face embedding backend not initialized")
  }

  fun computeEmbedding(alignedFace: android.graphics.Bitmap): FloatArray {
    return getActiveBackend().computeEmbedding(alignedFace)
  }

  fun embeddingToString(embedding: FloatArray): String {
    return getActiveBackend().serialize(embedding)
  }

  fun stringToEmbedding(value: String): FloatArray? {
    val parsed = FaceEmbeddingSerializer.parse(value) ?: return null
    if (parsed.first != activeType) return null
    return parsed.second
  }

  fun release() {
    activeBackend?.release()
    activeBackend = null
  }

  private fun ensureInit() {
    check(::appContext.isInitialized) { "FaceEmbeddingModelManager.init() must be called first" }
  }
}
