package com.cbi.mobile_plantation.utils.face

import android.content.Context
import com.cbi.mobile_plantation.utils.PrefManager

data class ModelMatchThresholds(
  val identifyPrimary: Float,
  val enrollPrimary: Float,
  val matchMargin: Float
)

object FaceThresholdConfig {

  data class PreviewThresholds(
    val displayMin: Float,
    val actionMin: Float
  )

  fun getModelThresholds(context: Context): ModelMatchThresholds {
    val p = PrefManager(context)
    return ModelMatchThresholds(
      identifyPrimary = p.getFloatPreference(KEY_IDENTIFY, DEFAULT_IDENTIFY),
      enrollPrimary = p.getFloatPreference(KEY_ENROLL, DEFAULT_ENROLL),
      matchMargin = p.getFloatPreference(KEY_MARGIN, DEFAULT_MARGIN)
    )
  }

  fun saveModelThresholds(context: Context, thresholds: ModelMatchThresholds) {
    val p = PrefManager(context)
    p.putFloatPreference(KEY_IDENTIFY, thresholds.identifyPrimary)
    p.putFloatPreference(KEY_ENROLL, thresholds.enrollPrimary)
    p.putFloatPreference(KEY_MARGIN, thresholds.matchMargin)
  }

  fun getPreviewThresholds(context: Context): PreviewThresholds {
    val p = PrefManager(context)
    return PreviewThresholds(
      displayMin = p.getFloatPreference(KEY_PREVIEW_DISPLAY, 0.30f),
      actionMin = p.getFloatPreference(KEY_PREVIEW_ACTION, 0.50f)
    )
  }

  fun savePreviewThresholds(context: Context, thresholds: PreviewThresholds) {
    val p = PrefManager(context)
    p.putFloatPreference(KEY_PREVIEW_DISPLAY, thresholds.displayMin)
    p.putFloatPreference(KEY_PREVIEW_ACTION, thresholds.actionMin)
  }

  fun resetModelThresholds(context: Context) {
    saveModelThresholds(context, defaultModelThresholds())
  }

  fun resetPreviewThresholds(context: Context) {
    savePreviewThresholds(context, PreviewThresholds(0.30f, 0.50f))
  }

  fun defaultModelThresholds(): ModelMatchThresholds {
    return ModelMatchThresholds(
      identifyPrimary = DEFAULT_IDENTIFY,
      enrollPrimary = DEFAULT_ENROLL,
      matchMargin = DEFAULT_MARGIN
    )
  }

  fun formatPrimaryValue(value: Float): String = String.format("%.0f%%", value * 100f)

  fun formatMarginValue(value: Float): String = String.format("%.2f", value)

  fun formatPreviewValue(value: Float): String = String.format("%.0f%%", value * 100f)

  fun primarySliderRange(isEnroll: Boolean): ClosedFloatingPointRange<Float> {
    return if (isEnroll) 0.30f..0.80f else 0.35f..0.85f
  }

  fun marginSliderRange(): ClosedFloatingPointRange<Float> = 0.01f..0.15f

  fun previewDisplayRange(): ClosedFloatingPointRange<Float> = 0.10f..0.60f

  fun previewActionRange(): ClosedFloatingPointRange<Float> = 0.20f..0.80f

  fun sliderToValue(sliderPosition: Float, range: ClosedFloatingPointRange<Float>): Float {
    return range.start + (range.endInclusive - range.start) * (sliderPosition / 1000f)
  }

  fun valueToSlider(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    return ((value - range.start) / (range.endInclusive - range.start) * 1000f).coerceIn(0f, 1000f)
  }

  private const val KEY_IDENTIFY = "face_threshold_mfn_identify"
  private const val KEY_ENROLL = "face_threshold_mfn_enroll"
  private const val KEY_MARGIN = "face_threshold_mfn_margin"
  private const val KEY_PREVIEW_DISPLAY = "face_threshold_preview_display"
  private const val KEY_PREVIEW_ACTION = "face_threshold_preview_action"
  private const val DEFAULT_IDENTIFY = 0.55f
  private const val DEFAULT_ENROLL = 0.52f
  private const val DEFAULT_MARGIN = 0.04f
}
