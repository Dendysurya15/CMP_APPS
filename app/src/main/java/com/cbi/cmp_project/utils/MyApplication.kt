package com.cbi.cmp_project.utils

import android.app.Application
import com.cbi.cmp_project.data.network.CMPApiClient
import com.cbi.cmp_project.data.network.TestingAPIClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CMPApiClient.init(this)
        TestingAPIClient.init(this)
    }
}