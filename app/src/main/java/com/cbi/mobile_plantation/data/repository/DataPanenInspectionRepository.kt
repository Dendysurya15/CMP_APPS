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

class DataPanenInspectionRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance,
){

    suspend fun getDataPanen(estate: Int, afdeling: String): Response<ResponseBody> {
        // Calculate date range - from yesterday to 7 days ago (excluding today)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Yesterday (1 day ago)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = formatter.format(calendar.time)

        // 7 days ago from today (which is 6 days from yesterday)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val sevenDaysAgo = formatter.format(calendar.time)

        AppLogger.d("Date range: $sevenDaysAgo to $yesterday (inclusive, excluding today)")

        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "panen")
            put("select", JSONArray().apply {
                put("tph")
                put("tph_nomor")
                put("ancak")
                put("created_date")
                put("created_by")
                put("jjg_panen")
                put("jjg_masak")
                put("jjg_mentah")
                put("jjg_lewat_masak")
                put("jjg_kosong")
                put("jjg_abnormal")
                put("jjg_serangan_tikus")
                put("jjg_panjang")
                put("jjg_tidak_vcut")
                put("jjg_bayar")
                put("jjg_kirim")
                put("kemandoran")
                put("spb_kode")
            })

            // Build WHERE clause with multiple conditions
            put("where", JSONObject().apply {
                // Estate condition
                put("dept", estate)

                // Afdeling/divisi condition (if provided)
                if (!afdeling.isNullOrEmpty() && afdeling != "0") {
                    put("divisi", afdeling)
                }

                // Date range condition using BETWEEN
                put("tanggal", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(sevenDaysAgo)
                        put(yesterday)
                    })
                })
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        // Make the API call
        return apiService.getDataRaw(requestBody)
    }
}