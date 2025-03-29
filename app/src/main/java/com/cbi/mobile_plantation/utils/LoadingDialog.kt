package com.cbi.mobile_plantation.utils

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cbi.mobile_plantation.R


class LoadingDialog(context: Context) : Dialog(context) {
    private var loadingLogo: ImageView? = null
    private var messageTextView: TextView? = null
    private var statusMessagesContainer: LinearLayout? = null
    private var bounceAnimation: Animation? = null
    private var contentContainer: LinearLayout? = null
    private var dotsAnimator: ValueAnimator? = null
    private var baseMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_dialog)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(-1, -1)
        setCancelable(false)

        loadingLogo = findViewById<ImageView>(R.id.loading_logo)
        messageTextView = findViewById(R.id.loading_message)

        contentContainer = findViewById(R.id.content_container)
        bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
        startBouncing()
    }

    fun setMessage(message: String, isAnimate: Boolean = false) {
        // Store the base message without dots
        baseMessage = message.trimEnd('.', ' ')

        if (isAnimate) {
            updateMessageWithDots(0) // Start with no dots
            // Start the dots animation
            startDotsAnimation()
        } else {
            // Just set the message directly without animation
            dotsAnimator?.cancel()
            dotsAnimator = null
            messageTextView?.text = message
        }
    }

    private fun updateMessageWithDots(dotCount: Int) {
        val dots = when (dotCount) {
            0 -> ""
            1 -> "."
            2 -> ".."
            3 -> "..."
            4 -> "...."
            else -> "...."
        }
        messageTextView?.text = "$baseMessage$dots"
    }

    private fun startDotsAnimation() {
        // Stop any existing animation
        dotsAnimator?.cancel()

        // Create a new animator
        dotsAnimator = ValueAnimator.ofInt(0, 5).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                updateMessageWithDots(value)
            }

        start()
        }
    }

    fun addStatusMessage(message: String, status: StatusType = StatusType.INFO, showIcon: Boolean = true) {
        // Find the TextView directly from the layout
        val statusMessageText = findViewById<TextView>(R.id.status_message_text)

        statusMessageText?.let {
            // Update text and make visible
            it.text = message
            it.visibility = View.VISIBLE

            // Set icon if needed
            if (showIcon) {
                val iconDrawable = ContextCompat.getDrawable(context, when(status) {
                    StatusType.SUCCESS -> R.drawable.baseline_check_box_24
                    StatusType.ERROR -> R.drawable.baseline_close_24
                    StatusType.WARNING -> R.drawable.baseline_error_24
                    StatusType.INFO -> R.drawable.baseline_file_upload_24
                })

                iconDrawable?.let { drawable ->
                    DrawableCompat.setTint(drawable, ContextCompat.getColor(context, when(status) {
                        StatusType.SUCCESS -> R.color.greendarkerbutton
                        StatusType.ERROR -> R.color.colorRedDark
                        StatusType.WARNING -> R.color.orangeButton
                        StatusType.INFO -> R.color.white
                    }))
                    it.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                    it.compoundDrawablePadding = 5
                }
            } else {
                it.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }

    // Enum class for status types
    enum class StatusType {
        SUCCESS, ERROR, WARNING, INFO
    }

    private fun startBouncing() {
        loadingLogo?.startAnimation(bounceAnimation)
    }

    override fun show() {
        super.show()
        startBouncing()
    }

    override fun dismiss() {
        dotsAnimator?.cancel()
        dotsAnimator = null
        loadingLogo?.clearAnimation()
        super.dismiss()
    }

    // Clear all status messages
    fun clearStatusMessages() {
        statusMessagesContainer?.removeAllViews()
        statusMessagesContainer?.visibility = View.GONE
    }
}