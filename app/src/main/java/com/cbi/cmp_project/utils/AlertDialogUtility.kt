package com.cbi.cmp_project.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.airbnb.lottie.LottieAnimationView
import com.cbi.cmp_project.R
import com.google.android.material.button.MaterialButton

class AlertDialogUtility {
    companion object {
        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("InflateParams")
        fun alertDialog(context: Context, titleText: String, alertText: String) {
            if (context is Activity && !context.isFinishing) {
                val rootView = context.findViewById<View>(android.R.id.content)
                val parentLayout = rootView.findViewById<ConstraintLayout>(R.id.clParentAlertDialog)
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.confirmation_dialog, parentLayout)

                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.create()

                val llbuttonDialog = layoutBuilder.findViewById<LinearLayout>(R.id.llButtonDialog)
                llbuttonDialog.visibility = View.GONE

//                val viewDialog = layoutBuilder.findViewById<View>(R.id.viewDialog)
//                viewDialog.visibility = View.VISIBLE

                val tvTitleDialog = layoutBuilder.findViewById<TextView>(R.id.tvTitleDialog)
                tvTitleDialog.visibility = View.VISIBLE

                val tvDescDialog = layoutBuilder.findViewById<TextView>(R.id.tvDescDialog)
                tvDescDialog.visibility = View.VISIBLE
                tvTitleDialog.text = titleText
                tvDescDialog.text = alertText

                if (alertDialog.window != null) {
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }

                alertDialog.show()
                Handler(Looper.getMainLooper()).postDelayed({
                    alertDialog.dismiss()
                }, 2000)
            }
        }

        @SuppressLint("InflateParams")
        fun alertDialogAction(context: Context, titleText: String, alertText: String, animAsset: String,  delayMs: Long = 3000,  function: () -> Unit) {
            if (context is Activity && !context.isFinishing) {
                val rootView = context.findViewById<View>(android.R.id.content)
                val parentLayout = rootView.findViewById<ConstraintLayout>(R.id.clParentAlertDialog)
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.confirmation_dialog, parentLayout)

                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.create()

                val llbuttonDialog = layoutBuilder.findViewById<LinearLayout>(R.id.llButtonDialog)
                llbuttonDialog.visibility = View.GONE
//                val viewDialog = layoutBuilder.findViewById<View>(R.id.viewDialog)
//                viewDialog.visibility = View.VISIBLE

                val lottieAnim = layoutBuilder.findViewById<LottieAnimationView>(R.id.lottie_anim)
                lottieAnim.setAnimation(animAsset)
                lottieAnim.loop(true)
                lottieAnim.playAnimation()

                val tvTitleDialog = layoutBuilder.findViewById<TextView>(R.id.tvTitleDialog)
                tvTitleDialog.visibility = View.VISIBLE

                val tvDescDialog = layoutBuilder.findViewById<TextView>(R.id.tvDescDialog)
                tvDescDialog.visibility = View.VISIBLE
                tvTitleDialog.text = titleText
                tvDescDialog.text = alertText

                if (alertDialog.window != null) {
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }

                alertDialog.show()
                Handler(Looper.getMainLooper()).postDelayed({
                    alertDialog.dismiss()
                    function()
                }, delayMs)
            }
        }

        @SuppressLint("InflateParams")
        fun withTwoActions(context: Context, actionText: String, titleText: String, alertText: String,animAsset: String,  buttonColor: Int? = null, function: () -> Unit) {
            if (context is Activity && !context.isFinishing) {
                val rootView = context.findViewById<View>(android.R.id.content)
                rootView.foreground = ColorDrawable(Color.parseColor("#F0000000"))
                val parentLayout = rootView.findViewById<ConstraintLayout>(R.id.clParentAlertDialog)
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.confirmation_dialog, parentLayout)

                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder).setCancelable(false)
                val alertDialog: AlertDialog = builder.create()


                val tvTitleDialog = layoutBuilder.findViewById<TextView>(R.id.tvTitleDialog)
                tvTitleDialog.visibility = View.VISIBLE

                val tvDescDialog = layoutBuilder.findViewById<TextView>(R.id.tvDescDialog)
                tvDescDialog.visibility = View.VISIBLE
                tvTitleDialog.text = titleText
                tvDescDialog.text = alertText

                val mbSuccessDialog = layoutBuilder.findViewById<MaterialButton>(R.id.mbSuccessDialog)
                mbSuccessDialog.text = actionText
                val lottieAnim = layoutBuilder.findViewById<LottieAnimationView>(R.id.lottie_anim)
                lottieAnim.setAnimation(animAsset)
                lottieAnim.loop(true)
                lottieAnim.playAnimation()
                if (buttonColor != null) {
                    val colorStateList = ColorStateList.valueOf(buttonColor)
                    mbSuccessDialog.backgroundTintList = colorStateList
                    mbSuccessDialog.rippleColor =  ColorStateList.valueOf(Color.argb(70, 255, 255, 255))
                } else {
                    val defaultColorStateList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    )
                    mbSuccessDialog.backgroundTintList = defaultColorStateList
                    mbSuccessDialog.rippleColor = ColorStateList.valueOf(Color.argb(70, 255, 255, 255))
                }

                mbSuccessDialog.setOnClickListener {
                    alertDialog.dismiss()
                    function()
                }
                val mbCancelDialog = layoutBuilder.findViewById<MaterialButton>(R.id.mbCancelDialog)

                mbCancelDialog.setOnClickListener {
                    alertDialog.dismiss()
                }

                if (alertDialog.window != null) {
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }

                alertDialog.show()
            }
        }

        @SuppressLint("InflateParams")
        fun withSingleAction(context: Context, actionText: String, titleText: String, alertText: String, animAsset: String, color: Int = R.color.greendarkerbutton, function: () -> Unit) {
            if (context is Activity && !context.isFinishing) {
                val rootView = context.findViewById<View>(android.R.id.content)
                val parentLayout = rootView.findViewById<ConstraintLayout>(R.id.clParentAlertDialog)
                val layoutBuilder = LayoutInflater.from(context).inflate(R.layout.confirmation_dialog, parentLayout)

                val builder: AlertDialog.Builder = AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.create()

                // Disable dismiss on outside touch and back press
                alertDialog.setCancelable(false)
                alertDialog.setCanceledOnTouchOutside(false)

                val mbCancelDialog = layoutBuilder.findViewById<MaterialButton>(R.id.mbCancelDialog)
                mbCancelDialog.visibility = View.GONE

                val tvTitleDialog = layoutBuilder.findViewById<TextView>(R.id.tvTitleDialog)
                tvTitleDialog.visibility = View.VISIBLE

                val tvDescDialog = layoutBuilder.findViewById<TextView>(R.id.tvDescDialog)
                val scrollView = layoutBuilder.findViewById<NestedScrollView>(R.id.scrollView)
                tvDescDialog.visibility = View.VISIBLE
                tvTitleDialog.text = titleText
                tvDescDialog.text = alertText

                // Handle dynamic height
                tvDescDialog.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val maxHeight = context.resources.getDimensionPixelSize(R.dimen.max_dialog_height) // Make sure to add this dimension (150dp)
                        val params = scrollView.layoutParams

                        if (tvDescDialog.height > maxHeight) {
                            params.height = maxHeight
                        } else {
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }

                        scrollView.layoutParams = params
                        tvDescDialog.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })

                val mbSuccessDialog = layoutBuilder.findViewById<MaterialButton>(R.id.mbSuccessDialog)
                val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
                mbSuccessDialog.backgroundTintList = colorStateList
                mbSuccessDialog.text = actionText

                val lottieAnim = layoutBuilder.findViewById<LottieAnimationView>(R.id.lottie_anim)
                lottieAnim.setAnimation(animAsset)
                lottieAnim.loop(true)
                lottieAnim.playAnimation()

                mbSuccessDialog.setOnClickListener {
                    alertDialog.dismiss()
                    function()
                }

                if (alertDialog.window != null) {
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }

                alertDialog.show()
            }
        }
    }
}