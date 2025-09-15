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
    private lateinit var detectionsText: TextView
    private lateinit var performanceCard: CardView

    private var isVisible = true

    init {
        setupView()
    }

    private fun setupView() {
        orientation = VERTICAL

        // Create card container
        performanceCard = CardView(context).apply {
            radius = 8f
            cardElevation = 4f
            setCardBackgroundColor(Color.parseColor("#DD000000")) // Semi-transparent black
            setPadding(12, 8, 12, 8)
        }

        // Create container for text views
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(8, 4, 8, 4)
        }

        // Create text views
        fpsText = createPerformanceTextView("FPS: --")
        memoryText = createPerformanceTextView("Memory: -- MB")
        inferenceText = createPerformanceTextView("Inference: -- ms")
        detectionsText = createPerformanceTextView("Detections: 0")

        // Add text views to container
        textContainer.addView(fpsText)
        textContainer.addView(memoryText)
        textContainer.addView(inferenceText)
        textContainer.addView(detectionsText)

        // Add container to card
        performanceCard.addView(textContainer)

        // Add card to main view
        addView(performanceCard)

        // Setup click listener to toggle visibility
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
            // Update FPS with color coding
            val fpsColor = when {
                metrics.fps >= 15f -> Color.parseColor("#4CAF50") // Green
                metrics.fps >= 8f -> Color.parseColor("#FFC107")  // Yellow
                else -> Color.parseColor("#FF5722")               // Red
            }
            fpsText.text = "FPS: ${String.format("%.1f", metrics.fps)}"
            fpsText.setTextColor(fpsColor)

            // Update Memory with color coding
            val memoryColor = when {
                metrics.memoryUsagePercent <= 70 -> Color.parseColor("#4CAF50") // Green
                metrics.memoryUsagePercent <= 85 -> Color.parseColor("#FFC107")  // Yellow
                else -> Color.parseColor("#FF5722")                              // Red
            }
            memoryText.text = "Memory: ${metrics.memoryUsageMB}MB (${metrics.memoryUsagePercent}%)"
            memoryText.setTextColor(memoryColor)

            // Update Inference time with color coding
            val inferenceColor = when {
                metrics.inferenceTime <= 50 -> Color.parseColor("#4CAF50")  // Green
                metrics.inferenceTime <= 100 -> Color.parseColor("#FFC107") // Yellow
                else -> Color.parseColor("#FF5722")                         // Red
            }
            inferenceText.text = "Inference: ${metrics.inferenceTime}ms"
            inferenceText.setTextColor(inferenceColor)

            // Update detections count
            detectionsText.text = "Detections: ${metrics.totalDetections}"
            detectionsText.setTextColor(Color.WHITE)
        }
    }

    private fun toggleVisibility() {
        isVisible = !isVisible

        if (isVisible) {
            fpsText.visibility = VISIBLE
            memoryText.visibility = VISIBLE
            inferenceText.visibility = VISIBLE
            detectionsText.visibility = VISIBLE
            performanceCard.setCardBackgroundColor(Color.parseColor("#DD000000"))
        } else {
            fpsText.visibility = GONE
            memoryText.visibility = GONE
            inferenceText.visibility = GONE
            detectionsText.visibility = GONE
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
            detectionsText.text = "Detections: 0"
            detectionsText.setTextColor(Color.WHITE)
        }
    }
}