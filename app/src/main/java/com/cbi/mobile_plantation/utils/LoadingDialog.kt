package com.cbi.mobile_plantation.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.cbi.mobile_plantation.R


class LoadingDialog(context: Context) : Dialog(context) {
    private var loadingLogo: ImageView? = null
    private var messageTextView: TextView? = null
    private var bounceAnimation: Animation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_dialog)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(-1, -1)
        setCancelable(false)

        loadingLogo = findViewById<ImageView>(R.id.loading_logo)
        messageTextView = findViewById(R.id.loading_message)
        bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
        startBouncing()
    }
    fun setMessage(message: String) {
        messageTextView?.text = message
    }

    private fun startBouncing() {
        loadingLogo?.startAnimation(bounceAnimation)
    }

    override fun show() {
        super.show()
        startBouncing()
    }

    override fun dismiss() {
        loadingLogo?.clearAnimation()
        super.dismiss()
    }
}