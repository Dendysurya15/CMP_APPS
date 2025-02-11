package com.cbi.cmp_project.ui.view.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cbi.cmp_project.ui.viewModel.SingleLiveEvent
import com.cbi.cmp_project.utils.AppLogger

class EventWrapper<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}



class HomeViewModel : ViewModel() {
    private val _navigationEvent = MutableLiveData<FeatureCardEvent>()
    val navigationEvent: LiveData<FeatureCardEvent> = _navigationEvent

    private val _startSinkronisasiData = MutableLiveData<Boolean>()
    val startSinkronisasiData: LiveData<Boolean> get() = _startSinkronisasiData

    private var isTriggered = false // Prevent re-triggering

    fun triggerDownload() {
        if (!isTriggered) {
            isTriggered = true
            _startSinkronisasiData.value = true
            AppLogger.d("HomeViewModel: Download triggered")
        }
    }

    fun resetTrigger() {
        isTriggered = false // Allow triggering again when needed
    }

//    fun clearDownloadTrigger() {
//
//
//
//        _startSinkronisasiData.value = false // Reset event after handling
//
//        AppLogger.d( _startSinkronisasiData.value.toString())
//    }


    fun onFeatureCardClicked(feature: FeatureCard, context: Context) {
        when (feature.featureName) {


            "Panen TBS" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToPanenTBS(context, feature.featureName)
                }
            }
            "Rekap Hasil Panen" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToListPanenTBS(context, feature.featureName)
                }
            }
            "Scan Hasil Panen" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToScanPanen(context, feature.featureName)
                }
            }
            "Buat eSPB" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToBuatESPB(context, feature.featureName)
                }
            }
            "Rekap panen dan restan" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToRekapPanen(context, feature.featureName)
                }
            }
            "Sinkronisasi Data" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.SinkronisasiData(context, feature.featureName)
                }
            }
        }
    }
}

sealed class FeatureCardEvent(val context: Context? = null, val featureName: String? = null) {
    class NavigateToPanenTBS(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
    class NavigateToListPanenTBS(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
    class NavigateToScanPanen(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
    class SinkronisasiData(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
    class NavigateToBuatESPB(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
    class NavigateToRekapPanen(context: Context, featureName: String) : FeatureCardEvent(context, featureName)
}