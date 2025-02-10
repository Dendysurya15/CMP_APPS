package com.cbi.cmp_project.ui.view.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cbi.cmp_project.utils.AppLogger

class HomeViewModel : ViewModel() {
    private val _navigationEvent = MutableLiveData<FeatureCardEvent>()
    val navigationEvent: LiveData<FeatureCardEvent> = _navigationEvent

    private val _startSinkronisasiData = MutableLiveData<Boolean>()
    val startSinkronisasiData: LiveData<Boolean> get() = _startSinkronisasiData

    fun triggerDownload() {
        AppLogger.d("HomeViewModel: Triggering download event")
        _startSinkronisasiData.value = true
        AppLogger.d("HomeViewModel: Start download event value set")
    }

    fun onFeatureCardClicked(feature: FeatureCard, context: Context) {
        when (feature.featureName) {


            "Panen TBS" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToPanenTBS(context, feature.featureName)
                }
            }
            "List History Panen TBS" -> {
                if (feature.displayType == DisplayType.COUNT) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToListPanenTBS(context, feature.featureName)
                }
            }
            "Scan Hasil Panen" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToScanPanen(context, feature.featureName)
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
}