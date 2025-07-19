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
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val today = formatter.format(calendar.time)

        // 7 days ago from today (which is 6 days from yesterday)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val sevenDaysAgo = formatter.format(calendar.time)

        AppLogger.d("Date range: $sevenDaysAgo to $today (inclusive, excluding today)")

        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "panen")
            put("select", JSONArray().apply {
                put("id")
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
                        put(today)
                    })
                })
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        return TestingApiService.getDataRaw(requestBody)
    }

    suspend fun getDataInspeksi(
        estate: Int,
        afdeling: String,
        joinTable: Boolean = true
    ): Response<ResponseBody> {
        // Calculate date range with full datetime
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Today at 23:59:59 (end of day)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val today = formatter.format(calendar.time)

        // 7 days ago at 00:00:00 (start of day)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val sevenDaysAgo = formatter.format(calendar.time)

        AppLogger.d("Date range: $sevenDaysAgo to $today (inclusive, full datetime)")

        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "inspeksi")
            put("select", JSONArray().apply {
                put("id")
                put("id_panen")
                put("tph")
                put("tgl_inspeksi")
                put("tgl_panen")
                put("inspeksi_putaran")
                put("jenis_inspeksi")
                put("rute_masuk")
                put("baris")
                put("jml_pokok_inspeksi")
                put("created_name")
                put("created_by")
                put("tracking_path")
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
                put("tgl_inspeksi", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(sevenDaysAgo)
                        put(today)
                    })
                })
            })

            // Add join if requested
            if (joinTable) {
                put("join", JSONArray().apply {
                    put(JSONObject().apply {
                        put("table", "inspeksi_detail")
                        put("required", false)
                        put("select", JSONArray().apply {
                            put("id")
                            put("id_inspeksi")
                            put("no_pokok")
                            put("pokok_panen")
                            put("kode_inspeksi")
                            put("temuan_inspeksi")
                            put("status_pemulihan")
                            put("nik")
                            put("nama")
                            put("foto_pemulihan")
                            put("catatan")
                            put("created_by")
                            put("created_name")
                            put("created_date")
                            put("lat")
                            put("lon")
                        })
                    })
                })
            }
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        return TestingApiService.getDataRaw(requestBody)
    }
}