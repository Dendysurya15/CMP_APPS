package com.cbi.mobile_plantation.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


suspend fun <T> LiveData<T>.awaitValue(): T = suspendCancellableCoroutine { continuation ->
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            removeObserver(this)
            continuation.resume(value)
        }
    }

    continuation.invokeOnCancellation {
        removeObserver(observer)
    }

    observeForever(observer)
}