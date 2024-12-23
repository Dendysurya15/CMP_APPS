package com.cbi.cmp_project.ui.view.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val _navigationEvent = MutableLiveData<FeatureCardEvent>()
    val navigationEvent: LiveData<FeatureCardEvent> = _navigationEvent

    fun onFeatureCardClicked(feature: FeatureCard, context: Context) {
        Log.d("testing", feature.featureName)
        when (feature.featureName) {
            "Panen TBS" -> {
                if (feature.displayType == DisplayType.ICON) {
                    _navigationEvent.value = FeatureCardEvent.NavigateToAddPanen(context)
                }
            }
        }
    }
}

sealed class FeatureCardEvent(val context: Context? = null) {
    class NavigateToAddPanen(context: Context) : FeatureCardEvent(context)
}