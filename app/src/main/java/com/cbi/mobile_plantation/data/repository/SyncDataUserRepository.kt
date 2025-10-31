package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import okhttp3.Headers
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
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

        // Step 1: Check if user has kemandoran_ppro
        try {
            val checkQuery = JSONObject().apply {
                put("table", "sys_user")
                put("select", JSONArray().apply { put("kemandoran_ppro") })
                put("where", JSONObject().apply { put("id", idUser) })
            }
            val checkResponse = ApiService.getDataRaw(checkQuery.toString().toRequestBody("application/json".toMediaType()))
            val checkBody = checkResponse.body()?.string()

            if (!checkBody.isNullOrEmpty()) {
                val checkJson = JSONObject(checkBody)
                if (checkJson.optBoolean("success")) {
                    val data = checkJson.optJSONArray("data")?.optJSONObject(0)
                    val kemandoran = data?.optInt("kemandoran_ppro", 0) ?: 0
                    hasKemandoranPpro = kemandoran > 0
                    AppLogger.d("User $idUser has kemandoran_ppro=$kemandoran (join=$hasKemandoranPpro)")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error checking kemandoran_ppro: ${e.message}")
        }

        // Step 2: Get main user + sys_user_org
        val mainQuery = JSONObject().apply {
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
                put(JSONObject().apply {
                    put("table", "sys_user_org")
                    put("select", JSONArray().apply {
                        put("dept")
                        put("divisi")
                    })
                    put("on", "sys_user.id = sys_user_org.uid")
                })
                if (hasKemandoranPpro) {
                    put(JSONObject().apply {
                        put("table", "kemandoran_sync")
                        put("select", JSONArray().apply { put("kode") })
                        put("on", "sys_user.kemandoran_ppro = kemandoran_sync.kemandoran_ppro")
                    })
                }
            })
            put("where", JSONObject().apply { put("id", idUser) })
        }

        val mainResponse = ApiService.getDataRaw(mainQuery.toString().toRequestBody("application/json".toMediaType()))
        val mainBody = mainResponse.body()?.string()
        AppLogger.d("📩 Main user response: ${mainBody ?: "null"}")

        if (mainBody.isNullOrEmpty()) {
            return Response.success(ResponseBody.create("application/json".toMediaType(), """{"success":false,"message":"No data found"}"""))
        }

        val mainJson = JSONObject(mainBody)
        if (!mainJson.optBoolean("success", false)) {
            return Response.success(ResponseBody.create("application/json".toMediaType(), """{"success":false,"message":"Invalid user"}"""))
        }

        val dataArray = mainJson.optJSONArray("data")
        if (dataArray == null || dataArray.length() == 0) {
            return Response.success(ResponseBody.create("application/json".toMediaType(), """{"success":false,"message":"Empty user data"}"""))
        }

        val mainUserData = dataArray.getJSONObject(0)
        val userOrgArray = mainUserData.optJSONArray("userOrg")
        val firstOrg = userOrgArray?.optJSONObject(0)
        val deptValue = firstOrg?.optString("dept", "") ?: ""

        if (deptValue.isEmpty()) {
            AppLogger.w("⚠️ No dept value found in userOrg, returning base user only")
            val finalData = JSONObject().apply {
                put("success", true)
                put("data", dataArray)
                put("count", dataArray.length())
            }
            return Response.success(finalData.toString().toResponseBody("application/json".toMediaType()))
        }

        // Step 3: Split and query dept table using IN clause
        val estateIds = deptValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val deptQuery = JSONObject().apply {
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
            put("where", JSONObject().apply {
                put("id", JSONObject().apply {
                    put("in", JSONArray(estateIds))
                })
            })
        }

        AppLogger.d("📤 Dept query for all estates: $deptQuery")

        val deptResponse = ApiService.getDataRaw(deptQuery.toString().toRequestBody("application/json".toMediaType()))
        val deptBody = deptResponse.body()?.string()
        AppLogger.d("📩 Dept response: ${deptBody ?: "null"}")

        val deptJson = JSONObject(deptBody ?: "{}")
        val deptsData = if (deptJson.optBoolean("success", false)) deptJson.optJSONArray("data") else JSONArray()

        // Step 4: Merge Depts into user
        // Step 4: Merge Depts into user with SysUserOrg inside
        val sysUserOrg = firstOrg ?: JSONObject() // from earlier (dept + divisi + uid + etc.)
        val deptsFullArray = JSONArray()

        for (i in 0 until (deptsData?.length() ?: 0)) {
            val deptObj = deptsData!!.getJSONObject(i)

            // Deep copy SysUserOrg data into each dept
            val sysUserOrgObj = JSONObject().apply {
                put("id", sysUserOrg.optInt("id", 0))
                put("uid", idUser)
                put("company", sysUserOrg.optString("company", ""))
                put("dept", sysUserOrg.optString("dept", ""))
                put("users", JSONObject.NULL)
                put("divisi", sysUserOrg.optString("divisi", ""))
            }

            deptObj.put("SysUserOrg", sysUserOrgObj)
            deptsFullArray.put(deptObj)
        }

        mainUserData.put("Depts", deptsFullArray)
        mainJson.put("data", JSONArray().apply { put(mainUserData) })
        mainJson.put("count", 1)


        AppLogger.d("🎯 Final merged user data:\n${mainJson.toString(2)}")

        // Step 5: Return
        val headers = Headers.Builder()
            .add("Content-Type", "application/json")
            .build()

        val responseBody = mainJson.toString().toResponseBody("application/json".toMediaType())
        return Response.success(responseBody, headers)

    }


}