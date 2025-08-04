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
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // End of yesterday (23:59:59 yesterday)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = formatter.format(calendar.time)

        // Start of 7 days ago (00:00:00 seven days ago)
        calendar.add(Calendar.DAY_OF_YEAR, -6) // Go back 6 more days (total 7 days from today)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = formatter.format(calendar.time)

        AppLogger.d("Date range: $startDate to $endDate (7 days, excluding today)")

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


                // Date range condition using BETWEEN with full datetime
                put("created_date", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(startDate)
                        put(endDate)
                    })
                })
            })
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("kljasldkfjalskf j")
        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        return apiService.getDataRaw(requestBody)
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
                put("id_panen")
                put("dept")
                put("dept_ppro")
                put("dept_abbr")
                put("dept_nama")
                put("divisi")
                put("divisi_ppro")
                put("divisi_abbr")
                put("divisi_nama")
                put("blok")
                put("blok_ppro")
                put("blok_kode")
                put("blok_nama")
                put("tph_nomor")
                put("ancak")
                put("tph")
                put("tgl_inspeksi")
                put("tgl_panen")
                put("jjg_panen")
                put("inspeksi_putaran")
                put("jenis_inspeksi")
                put("rute_masuk")
                put("baris")
                put("jml_pokok_inspeksi")
                put("created_name")
                put("created_by")
                put("app_version")
                put("tracking_path")
            })

            // Build WHERE clause with multiple conditions
            put("where", JSONObject().apply {
                // Estate condition
                put("dept", estate)

                // Afdeling/divisi condition (if provided)
//                if (!afdeling.isNullOrEmpty() && afdeling != "0") {
//                    put("divisi", afdeling)
//                }

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
                        // Add WHERE condition to skip records where status_pemulihan == 1
                        put("where", JSONObject().apply {
                            put("status_pemulihan", JSONObject().apply {
                                put("!=", 1)
                            })
                        })
                    })
                })
            }
        }

        // Convert JSONObject to RequestBody
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        return apiService.getDataRaw(requestBody)
    }
}