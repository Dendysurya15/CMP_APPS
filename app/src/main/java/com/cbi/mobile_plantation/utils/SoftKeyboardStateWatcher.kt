package com.cbi.mobile_plantation.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

class SoftKeyboardStateWatcher(private val rootView: View, private val listener: OnSoftKeyboardStateChangedListener) {

    interface OnSoftKeyboardStateChangedListener {
        fun onSoftKeyboardOpened(keyboardHeight: Int)
        fun onSoftKeyboardClosed()
    }

    private var isKeyboardVisible = false

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        rootView.getWindowVisibleDisplayFrame(r)

        val screenHeight = rootView.rootView.height
        val keyboardHeight = screenHeight - r.bottom

        val isKeyboardNowVisible = keyboardHeight > 200

        if (isKeyboardNowVisible != isKeyboardVisible) {
            isKeyboardVisible = isKeyboardNowVisible
            if (isKeyboardNowVisible) {
                listener.onSoftKeyboardOpened(keyboardHeight)
            } else {
                listener.onSoftKeyboardClosed()
            }
        }
    }

    init {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    fun unregister() {
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }
}