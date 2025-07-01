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

class SyncDataUserRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance,
){

    suspend fun getDataUser(idUser: Int): Response<ResponseBody> {

        val jsonObject = JSONObject().apply {
            put("table", "sys_user")

            put("select", JSONArray().apply {
                put("username")
                put("nama")
                put("jabatan")
                put("kemandoran")
                put("kemandoran_ppro")
                put("kemandoran_nama")
            })

            // JOIN configuration with select for each table
            put("join", JSONArray().apply {
                put(JSONObject().apply {
                    put("table", "sys_user_org")
                    put("select", JSONArray().apply {
                        put("dept")
                        put("divisi")
                    })
                })

                put(JSONObject().apply {
                    put("table", "dept")
                    put("select", JSONArray().apply {
                        put("regional")
                        put("wilayah")
                        put("company")
                        put("company_abbr")
                        put("company_nama")
                        put("abbr")
                        put("nama")
                    })
                })
            })

            // Build WHERE clause
            put("where", JSONObject().apply {
                put("id", idUser)
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("User Data API Request: ${jsonObject.toString()}")

        // Make the API call
        return apiService.getDataRaw(requestBody)
    }
}