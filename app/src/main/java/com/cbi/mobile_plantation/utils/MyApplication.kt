package com.cbi.mobile_plantation.utils

import android.app.Application
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CMPApiClient.init(this)
        TestingAPIClient.init(this)
    }
}