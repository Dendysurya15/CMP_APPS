package com.cbi.mobile_plantation.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Centralized state management for feature availability
 * Handles app version checks and feature disable states
 */
object FeatureStateManager {

    private val _isUpdateRequired = MutableLiveData<Boolean>(false)
    val isUpdateRequired: LiveData<Boolean> = _isUpdateRequired

    private val _featuresDisabled = MutableLiveData<Set<String>>(emptySet())
    val featuresDisabled: LiveData<Set<String>> = _featuresDisabled

    /**
     * Check app version and update state if needed
     * This will automatically notify all observers
     */
    fun checkAndUpdateAppVersion(context: Context, prefManager: PrefManager) {
        val needsUpdate = AppUtils.checkAppVersionUpdate(context, prefManager)
        val currentState = _isUpdateRequired.value ?: false

        if (currentState != needsUpdate) {
            _isUpdateRequired.postValue(needsUpdate)
            AppUtils.isAppUpdateRequired = needsUpdate

//            AppLogger.d("FeatureStateManager: App update required changed to: $needsUpdate")
//
//            if (needsUpdate) {
//                AppLogger.d("FeatureStateManager: All features will be disabled due to update requirement")
//            } else {
//                AppLogger.d("FeatureStateManager: Features enabled - app version is current")
//            }
        }
    }

    /**
     * Manually disable/enable a specific feature
     * Useful for server-side feature flags or temporary disables
     */
    fun updateFeatureDisabledState(featureName: String, isDisabled: Boolean) {
        val currentSet = _featuresDisabled.value ?: emptySet()
        val newSet = if (isDisabled) {
            currentSet + featureName
        } else {
            currentSet - featureName
        }

        if (currentSet != newSet) {
            _featuresDisabled.postValue(newSet)
//            AppLogger.d("FeatureStateManager: Feature '$featureName' disabled state changed to: $isDisabled")
        }
    }

    /**
     * Check if a specific feature should be disabled
     * Combines multiple disable conditions but respects exempt features
     */
    fun isFeatureDisabled(featureName: String): Boolean {
        // Check if this feature is exempt from being disabled
        if (AppUtils.ExemptFeatures.ALWAYS_ENABLED.contains(featureName)) {
//            AppLogger.d("FeatureStateManager: Feature '$featureName' is EXEMPT - always enabled")
            return false
        }

        val updateRequired = _isUpdateRequired.value ?: false
        val manuallyDisabled = _featuresDisabled.value?.contains(featureName) ?: false
        val appUtilsDisabled = AppUtils.isFeatureDisabled(featureName)

        val isDisabled = updateRequired || manuallyDisabled || appUtilsDisabled

        if (isDisabled) {
//            AppLogger.d("FeatureStateManager: Feature '$featureName' is disabled. UpdateRequired: $updateRequired, ManuallyDisabled: $manuallyDisabled, AppUtilsDisabled: $appUtilsDisabled")
        }

        return isDisabled
    }

    /**
     * Get current update requirement status
     */
    fun getCurrentUpdateRequiredState(): Boolean = _isUpdateRequired.value ?: false

    /**
     * Force refresh all feature states
     * Useful for debugging or manual refresh scenarios
     */
    fun forceRefreshStates(context: Context, prefManager: PrefManager) {
        AppLogger.d("FeatureStateManager: Force refreshing all states")
        checkAndUpdateAppVersion(context, prefManager)

        // Trigger observers by setting the same value
        _featuresDisabled.postValue(_featuresDisabled.value ?: emptySet())
    }
}