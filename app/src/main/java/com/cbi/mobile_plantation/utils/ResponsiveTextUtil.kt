package com.cbi.mobile_plantation.utils

import android.util.TypedValue
import android.widget.TextView

object ResponsiveTextUtil {

    /**
     * Adjusts TextView size based on screen width
     * @param textView The TextView to adjust
     * @param baseTextSize Base text size in SP
     */
    fun setResponsiveTextSize(textView: TextView, baseTextSize: Float) {
        val context = textView.context
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        // Scale factor based on screen width
        val scaleFactor = when {
            screenWidthDp < 360 -> 0.85f  // Smaller phones (5.4")
            screenWidthDp > 480 -> 1.25f  // Larger phones (6.7")
            else -> 1.0f                  // Medium phones
        }

        // Apply the calculated text size
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseTextSize * scaleFactor)
    }

    /**
     * Adjusts TextView size with min/max constraints
     * @param textView The TextView to adjust
     * @param baseTextSize Base text size in SP
     * @param minTextSize Minimum text size in SP
     * @param maxTextSize Maximum text size in SP
     */
    fun setResponsiveTextSizeWithConstraints(
        textView: TextView,
        baseTextSize: Float,
        minTextSize: Float,
        maxTextSize: Float
    ) {
        val context = textView.context
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        // Scale factor based on screen width
        val scaleFactor = when {
            screenWidthDp < 360 -> 0.85f  // Smaller phones (5.4")
            screenWidthDp > 480 -> 1.25f  // Larger phones (6.7")
            else -> 1.0f                  // Medium phones
        }

        // Apply the calculated text size with constraints
        val calculatedSize = baseTextSize * scaleFactor
        val constrainedSize = calculatedSize.coerceIn(minTextSize, maxTextSize)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, constrainedSize)
    }
}

/**
 * Extension function to set text size responsively based on screen width
 * @param baseSizeSp Base text size in SP
 */
fun TextView.setResponsiveTextSize(baseSizeSp: Float) {
    ResponsiveTextUtil.setResponsiveTextSize(this, baseSizeSp)
}

/**
 * Extension function to set text size responsively with min/max constraints
 * @param baseSizeSp Base text size in SP
 * @param minSizeSp Minimum text size in SP
 * @param maxSizeSp Maximum text size in SP
 */
fun TextView.setResponsiveTextSizeWithConstraints(
    baseSizeSp: Float,
    minSizeSp: Float,
    maxSizeSp: Float
) {
    ResponsiveTextUtil.setResponsiveTextSizeWithConstraints(this, baseSizeSp, minSizeSp, maxSizeSp)
}