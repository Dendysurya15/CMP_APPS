package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class VersioningAppRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance,
){

    suspend fun getDataAppVersion(userId:Int): Response<ResponseBody> {

        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "user_app_requirements")
            put("select", JSONArray().apply {
                put("id")
                put("user_id")
                put("app_category")
                put("required_version")
                put("required_architecture")
                put("current_version")
                put("current_architecture")
                put("last_update")
                put("status")
                put("notes")
                put("auto_update")
                put("created_at")
                put("updated_at")
                put("is_exempt")
            })

            put("where", JSONObject().apply {
                put("app_category", "CMP-PLANTATION")
                put("user_id", userId)
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("User App Requirements API Request: ${jsonObject.toString()}")

        // Make the API call
        return apiService.getDataRaw(requestBody)
    }
}