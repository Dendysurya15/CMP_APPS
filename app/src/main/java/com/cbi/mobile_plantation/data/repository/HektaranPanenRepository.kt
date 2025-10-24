package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiProvider
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

class HektaranPanenRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance
) {

    suspend fun getDataHektaranHektarDetail(estateId: Int, afdelingId:Int): Response<ResponseBody> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val twoDaysAgo = dateFormat.format(calendar.time)

        // Build JSON request
        val jsonObject = JSONObject().apply {
            put("table", "hektaran")
            put("select", JSONArray().apply {
                put("id")
                put("tanggal")
                put("created_date")
                put("created_by")
                put("blok_kode")
                put("dept_abbr")
                put("dept")
            })

            put("join", JSONArray().apply {
                put(JSONObject().apply {
                    put("table", "hektaran_detail")
                    put("select", JSONArray().apply {
                        put("id")
                        put("header")
                        put("date_panen")
                        put("kemandoran_ppro")
                        put("kemandoran_nama")
                        put("pemanen_nik")
                        put("pemanen_nama")
                        put("jjg_panen")
                        put("jjg_masak")
                        put("jjg_mentah")
                        put("jjg_lewat_masak")
                        put("jjg_kosong")
                        put("jjg_abnormal")
                        put("jjg_bayar")
                        put("jjg_kirim")
                    })
                    put("on", "hektaran.id = hektaran_detail.header")
                })
            })

            put("where", JSONObject().apply {
                put("tanggal", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(twoDaysAgo)
                        put(today)
                    })
                })
                put("dept", estateId)
                put("divisi", afdelingId)
            })
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        return apiService.getDataRaw(requestBody)
    }
}
