package com.cbi.cmp_project.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cbi.cmp_project.R


class LoadingDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_loading_dialog)
        // Make dialog background transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Make dialog full screen
        window?.setLayout(-1, -1)

        // Set cancelable
        setCancelable(false)

        // Start bouncing animation
        val loadingLogo = findViewById<ImageView>(R.id.loading_logo)
        val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
        loadingLogo.startAnimation(bounceAnimation)
    }
}