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

class RestanRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance,
){

    suspend fun getDataRestan(estate: Int, afdeling: String): Response<ResponseBody> {
        // Calculate date range - from today 23:59:59 to 7 days ago 00:00:00
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Today at 23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val today = dateTimeFormatter.format(calendar.time)

        // 7 days ago at 00:00:00
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val sevenDaysAgo = dateTimeFormatter.format(calendar.time)

        AppLogger.d("Date range: $sevenDaysAgo to $today (inclusive)")

        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "panen")
            put("select", JSONArray().apply {
                put("tph")
                put("created_date")
                put("created_name")
                put("jjg_kirim")
                put("spb_kode")
                put("status_espb")
            })

            // Build WHERE clause with multiple conditions
            put("where", JSONObject().apply {
                // Estate condition
                put("dept", estate)

                // Afdeling/divisi condition (if provided)
                if (!afdeling.isNullOrEmpty() && afdeling != "0") {
                    put("divisi", afdeling)
                }

                // Date range condition using BETWEEN with full datetime
                put("created_date", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(sevenDaysAgo)
                        put(today)
                    })
                })
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("Restan API Request: ${jsonObject.toString()}")

        // Make the API call
        return TestingApiService.getDataRaw(requestBody)
    }
}