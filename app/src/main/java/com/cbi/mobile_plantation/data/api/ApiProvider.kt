package com.cbi.mobile_plantation.data.api

import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger

object ApiProvider {
    var currentApiService: ApiService = CMPApiClient.instance // default to production

    fun switchToTesting() {
        currentApiService = TestingAPIClient.instance
        AppLogger.d("✅ Switched to Testing API: http://10.9.116.125:3005/")
    }

    fun switchToProduction() {
        currentApiService = CMPApiClient.instance
        AppLogger.d("✅ Switched to Production API: https://api.yourproductionserver.com/")
    }
}