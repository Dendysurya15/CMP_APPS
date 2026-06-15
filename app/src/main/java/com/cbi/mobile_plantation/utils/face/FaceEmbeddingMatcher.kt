package com.cbi.mobile_plantation.utils.face

import android.content.Context

object FaceEmbeddingMatcher {

  fun isEnrollmentMatch(
    context: Context,
    probe: FloatArray,
    reference: FloatArray
  ): Boolean {
    val thresholds = FaceThresholdConfig.getModelThresholds(context)
    return cosineSimilarity(probe, reference) >= thresholds.enrollPrimary
  }

  fun findBestMatchId(
    context: Context,
    probeEmbedding: FloatArray,
    candidates: List<Pair<Int, FloatArray>>
  ): Int? {
    if (candidates.isEmpty()) return null
    val thresholds = FaceThresholdConfig.getModelThresholds(context)

    val ranked = candidates
      .map { (id, embedding) -> id to cosineSimilarity(probeEmbedding, embedding) }
      .sortedByDescending { it.second }

    val (bestId, bestScore) = ranked.first()
    if (bestScore < thresholds.identifyPrimary) return null

    if (ranked.size > 1) {
      val secondScore = ranked[1].second
      if (secondScore >= thresholds.identifyPrimary) {
        if (bestScore - secondScore < thresholds.matchMargin) return null
      }
    }

    return bestId
  }

  fun matchConfidence(context: Context, score: Float): Float {
    val min = FaceThresholdConfig.getModelThresholds(context).identifyPrimary
    return ((score - min) / (1f - min).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
  }

  fun matchScoreForCandidate(
    probeEmbedding: FloatArray,
    candidateEmbedding: FloatArray
  ): Float = cosineSimilarity(probeEmbedding, candidateEmbedding)

  private fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
    if (first.size != second.size || first.isEmpty()) return -1f
    var dot = 0f
    for (i in first.indices) {
      dot += first[i] * second[i]
    }
    return dot
  }
}
