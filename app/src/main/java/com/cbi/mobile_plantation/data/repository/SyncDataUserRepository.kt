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
        // First, check if user has kemandoran_ppro
        val checkKemandoranQuery = JSONObject().apply {
            put("table", "sys_user")
            put("select", JSONArray().apply {
                put("kemandoran_ppro")
            })
            put("where", JSONObject().apply {
                put("id", idUser)
            })
        }
        AppLogger.d("dsadfasdfsdf")
        val checkRequestBody = checkKemandoranQuery.toString().toRequestBody("application/json".toMediaType())
        val checkResponse = apiService.getDataRaw(checkRequestBody)

        var hasKemandoranPpro = false

        try {
            val checkResponseBody = checkResponse.body()?.string()
            if (checkResponseBody != null) {
                val checkJsonObject = JSONObject(checkResponseBody)
                if (checkJsonObject.optBoolean("success", false)) {
                    val dataArray = checkJsonObject.optJSONArray("data")
                    if (dataArray != null && dataArray.length() > 0) {
                        val userData = dataArray.getJSONObject(0)
                        val kemandoranPpro = userData.optInt("kemandoran_ppro", 0)
                        hasKemandoranPpro = kemandoranPpro > 0
                        AppLogger.d("User $idUser has kemandoran_ppro: $kemandoranPpro, will include JOIN: $hasKemandoranPpro")
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error checking kemandoran_ppro: ${e.message}")
            hasKemandoranPpro = false
        }

        // Now build the main query with conditional JOIN
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
                    put("on", "sys_user.id = sys_user_org.uid")
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
                    put("on", "sys_user_org.dept = dept.id")
                })

                // Only add kemandoran JOIN if user has valid kemandoran_ppro
                if (hasKemandoranPpro) {
                    put(JSONObject().apply {
                        put("table", "kemandoran_sync")
                        put("select", JSONArray().apply {
                            put("kode")
                        })
                        put("on", "sys_user.kemandoran_ppro = kemandoran_sync.kemandoran_ppro")
                    })
                }
            })

            // Build WHERE clause
            put("where", JSONObject().apply {
                put("id", idUser)
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("User Data API Request (with kemandoran JOIN: $hasKemandoranPpro): ${jsonObject.toString()}")

        // Make the API call
        return apiService.getDataRaw(requestBody)
    }
}