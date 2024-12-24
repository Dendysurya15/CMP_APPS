package com.cbi.cmp_project.utils

import android.content.Context
import com.cbi.cmp_project.R
import com.jaredrummler.materialspinner.BuildConfig

object AppUtils {

    /**
     * Gets the current app version from BuildConfig or string resources.
     * @param context The context used to retrieve the string resource.
     * @return The app version as a string.
     */
    fun getAppVersion(context: Context): String {
        return "Versi ${context.getString(R.string.app_version)}"
    }

    /**
     * Alternative method to fetch the version name directly from BuildConfig with a "V" prefix.
     * Use this if you don't want to rely on Gradle's `resValue`.
     */
    fun getAppVersionFromBuildConfig(): String {
        return "V${BuildConfig.VERSION_NAME}"
    }
}