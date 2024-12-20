package com.cbi.cmp_project.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cbi.cmp_project.R


class LoadingDialog(context: Context) : Dialog(context) {
    private var loadingLogo: ImageView? = null
    private var bounceAnimation: Animation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_dialog)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(-1, -1)
        setCancelable(false)

        loadingLogo = findViewById<ImageView>(R.id.loading_logo)
        bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
        startBouncing()
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