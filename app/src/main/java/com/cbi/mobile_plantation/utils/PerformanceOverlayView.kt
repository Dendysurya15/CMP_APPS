package com.cbi.mobile_plantation.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

data class PerformanceMetrics(
    val fps: Float,
    val memoryUsageMB: Long,
    val memoryUsagePercent: Long,
    val inferenceTime: Long,
    val totalDetections: Int
)

class PerformanceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var fpsText: TextView
    private lateinit var memoryText: TextView
    private lateinit var inferenceText: TextView
    private lateinit var totalCountText: TextView  // Changed from detectionsText
    private lateinit var performanceCard: CardView

    private var isVisible = true

    init {
        setupView()
    }

    private fun setupView() {
        orientation = VERTICAL

        performanceCard = CardView(context).apply {
            radius = 8f
            cardElevation = 4f
            setCardBackgroundColor(Color.parseColor("#DD000000"))
            setPadding(12, 8, 12, 8)
        }

        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(8, 4, 8, 4)
        }

        fpsText = createPerformanceTextView("FPS: --")
        memoryText = createPerformanceTextView("Memory: -- MB")
        inferenceText = createPerformanceTextView("Inference: -- ms")
        totalCountText = createPerformanceTextView("Total FFB: 0")  // Updated label

        textContainer.addView(fpsText)
        textContainer.addView(memoryText)
        textContainer.addView(inferenceText)
        textContainer.addView(totalCountText)

        performanceCard.addView(textContainer)
        addView(performanceCard)

        performanceCard.setOnClickListener {
            toggleVisibility()
        }
    }

    private fun createPerformanceTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(0, 1, 0, 1)
        }
    }

    fun updateMetrics(metrics: PerformanceMetrics) {
        if (!isVisible) return

        post {
            val fpsColor = when {
                metrics.fps >= 15f -> Color.parseColor("#4CAF50")
                metrics.fps >= 8f -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#FF5722")
            }
            fpsText.text = "FPS: ${String.format("%.1f", metrics.fps)}"
            fpsText.setTextColor(fpsColor)

            val memoryColor = when {
                metrics.memoryUsagePercent <= 70 -> Color.parseColor("#4CAF50")
                metrics.memoryUsagePercent <= 85 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#FF5722")
            }
            memoryText.text = "Memory: ${metrics.memoryUsageMB}MB (${metrics.memoryUsagePercent}%)"
            memoryText.setTextColor(memoryColor)

            val inferenceColor = when {
                metrics.inferenceTime <= 50 -> Color.parseColor("#4CAF50")
                metrics.inferenceTime <= 100 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#FF5722")
            }
            inferenceText.text = "Inference: ${metrics.inferenceTime}ms"
            inferenceText.setTextColor(inferenceColor)

            // Keep the total detections updated from updateMetrics
            // but allow updateTotalDetections to override it for real-time updates
        }
    }

    fun updateTotalDetections(count: Int) {
        post {
            val countColor = when {
                count == 0 -> Color.WHITE
                count <= 5 -> Color.parseColor("#4CAF50")
                count <= 15 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#FF6B35")
            }
            totalCountText.text = "Total FFB: $count"
            totalCountText.setTextColor(countColor)
        }
    }

    private fun toggleVisibility() {
        isVisible = !isVisible

        if (isVisible) {
            fpsText.visibility = VISIBLE
            memoryText.visibility = VISIBLE
            inferenceText.visibility = VISIBLE
            totalCountText.visibility = VISIBLE
            performanceCard.setCardBackgroundColor(Color.parseColor("#DD000000"))
        } else {
            fpsText.visibility = GONE
            memoryText.visibility = GONE
            inferenceText.visibility = GONE
            totalCountText.visibility = GONE
            performanceCard.setCardBackgroundColor(Color.parseColor("#88000000"))
        }
    }

    fun clear() {
        post {
            fpsText.text = "FPS: --"
            fpsText.setTextColor(Color.WHITE)
            memoryText.text = "Memory: -- MB"
            memoryText.setTextColor(Color.WHITE)
            inferenceText.text = "Inference: -- ms"
            inferenceText.setTextColor(Color.WHITE)
            totalCountText.text = "Total FFB: 0"
            totalCountText.setTextColor(Color.WHITE)
        }
    }
}