package com.cbi.cmp_project.utils

import android.util.Log

object AppLogger {
    private const val TAG = "AppLogger"

    fun d(message: String, tag: String = TAG) = Log.d(tag, message)
    fun e(message: String, tag: String = TAG) = Log.e(tag, message)
    fun i(message: String, tag: String = TAG) = Log.i(tag, message)
    fun v(message: String, tag: String = TAG) = Log.v(tag, message)
    fun w(message: String, tag: String = TAG) = Log.w(tag, message)
}
