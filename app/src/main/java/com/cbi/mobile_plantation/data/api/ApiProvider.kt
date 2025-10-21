package com.cbi.mobile_plantation.data.api

import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient

object ApiProvider {
    var currentApiService: ApiService = CMPApiClient.instance // default to production

    fun switchToTesting() {
        currentApiService = TestingAPIClient.instance
    }

    fun switchToProduction() {
        currentApiService = CMPApiClient.instance
    }
}