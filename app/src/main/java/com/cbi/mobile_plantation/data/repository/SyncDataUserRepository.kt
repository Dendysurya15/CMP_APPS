package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
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
    private val ApiService: ApiService = ApiProvider.currentApiService
){

    suspend fun getDataUser(idUser: Int): Response<ResponseBody> {
        var hasKemandoranPpro = false
        var isGMOrRH = false

        try {
            // Step 1: Check jabatan + kemandoran_ppro first
            val checkUserQuery = JSONObject().apply {
                put("table", "sys_user")
                put("select", JSONArray().apply {
                    put("jabatan")
                    put("kemandoran_ppro")
                })
                put("where", JSONObject().apply {
                    put("id", idUser)
                })
            }

            val checkRequestBody = checkUserQuery.toString().toRequestBody("application/json".toMediaType())
            val checkResponse = ApiService.getDataRaw(checkRequestBody)

            val checkResponseBody = checkResponse.body()?.string()
            if (checkResponseBody != null) {
                val checkJsonObject = JSONObject(checkResponseBody)
                if (checkJsonObject.optBoolean("success", false)) {
                    val dataArray = checkJsonObject.optJSONArray("data")
                    if (dataArray != null && dataArray.length() > 0) {
                        val userData = dataArray.getJSONObject(0)
                        val jabatan = userData.optString("jabatan", "")
                        val kemandoranPpro = userData.optInt("kemandoran_ppro", 0)

                        hasKemandoranPpro = kemandoranPpro > 0
                        isGMOrRH = jabatan.contains(AppUtils.ListFeatureByRoleUser.GM, ignoreCase = true) ||
                                jabatan.contains(AppUtils.ListFeatureByRoleUser.RH, ignoreCase = true)

                        AppLogger.d("User $idUser jabatan=$jabatan | GM/RH=$isGMOrRH | kemandoran_ppro=$kemandoranPpro")
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error checking user data: ${e.message}")
        }

        // Step 2: Build the main query dynamically
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

            put("join", JSONArray().apply {
                // Join sys_user_org
                put(JSONObject().apply {
                    put("table", "sys_user_org")
                    put("select", JSONArray().apply {
                        put("dept")
                        put("divisi")
                    })
                    put("on", "sys_user.id = sys_user_org.uid")
                })

                // Join dept
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

                // Only join kemandoran if NOT GM/RH and has valid ppro
                if (!isGMOrRH && hasKemandoranPpro) {
                    put(JSONObject().apply {
                        put("table", "kemandoran_sync")
                        put("select", JSONArray().apply {
                            put("kode")
                        })
                        put("on", "sys_user.kemandoran_ppro = kemandoran_sync.kemandoran_ppro")
                    })
                }
            })

            put("where", JSONObject().apply {
                put("id", idUser)
            })
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("✅ User Data API Request (GM/RH=$isGMOrRH | Kemandoran JOIN=${!isGMOrRH && hasKemandoranPpro}): $jsonObject")

        return ApiService.getDataRaw(requestBody)
    }

}