package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.ParameterDao
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

    suspend fun getDataPanen(estate: Any): Response<ResponseBody> {
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
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = formatter.format(calendar.time)

        AppLogger.d("Date range: $startDate to $endDate (7 days, excluding today)")

        AppLogger.d("estate $estate")
        // Create the JSON request using JSONObject
        val jsonObject = JSONObject().apply {
            put("table", "panen")
            put("select", JSONArray().apply {
                put("id")
                put("tph")
                put("tph_nomor")
                put("ancak")
                put("tipe")
                put("dept_abbr")
                put("jjg_kirim")
                put("spb_kode")
                put("status_espb")
                put("created_date")
                put("created_by")
                put("created_name")
                put("kemandoran")
            })

            // Build WHERE clause with multiple conditions
            put("where", JSONObject().apply {
                // Estate condition - handle both Int and List<Int>
                when (estate) {
                    is Int -> {
                        put("dept", estate)
                    }
                    is String -> {
                        // Convert string to int, or handle as string depending on your API
                        put("dept", estate.toIntOrNull() ?: estate)
                        // OR if your API expects string IDs:
                        // put("dept", estate)
                    }
                    is List<*> -> {
                        put("dept", JSONObject().apply {
                            put("in", JSONArray().apply {
                                (estate as List<Int>).forEach { estateId ->
                                    put(estateId)
                                }
                            })
                        })
                    }
                    else -> {
                        // Fallback to Int
                        put("dept", estate as Int)
                    }
                }

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

        AppLogger.d("jsonObject $jsonObject")
        AppLogger.d("Data Panen Inspeksi API Request: ${jsonObject.toString()}")

        return apiService.getDataRaw(requestBody)
    }

    suspend fun getDataInspeksi(
        estate: Any, // Changed from Int to Any
        afdeling: String,
        joinTable: Boolean = true,
        parameterDao: ParameterDao
    ): Response<ResponseBody> {

        // Get parameter JSON and extract status_ppro = 1 IDs
        val validKodeInspeksiIds = try {
            val parameterJson = parameterDao.getParameterInspeksiJson()
            if (parameterJson != null) {
                val jsonArray = JSONArray(parameterJson)
                val validIds = mutableListOf<Int>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val statusPpro = item.optInt("status_ppro", 0)
                    if (statusPpro == 1) {
                        val id = item.optInt("id", 0)
                        if (id > 0) {
                            validIds.add(id)
                        }
                    }
                }

                //tambahkan kode untuk TPH agar bisa di download
                validIds.add(5)
                validIds.add(6)

                AppLogger.d("Valid kode_inspeksi IDs (status_ppro=1): $validIds")
                validIds
            } else {
                AppLogger.d("No parameter JSON found, will not filter by kode_inspeksi")
                emptyList<Int>()
            }
        } catch (e: Exception) {
            AppLogger.e("Error parsing parameter JSON: ${e.message}")
            emptyList<Int>()
        }

        // Calculate date range with full datetime
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Today at 23:59:59 (end of day)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val today = formatter.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = formatter.format(calendar.time)

        AppLogger.d("Date range: $startDate to $today (inclusive, full datetime)")



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
                // Estate condition - handle both Int and List<Int>
                when (estate) {
                    is Int -> {
                        put("dept", estate)
                    }
                    is String ->{
                        put("dept", estate.toIntOrNull() ?: estate)
                    }
                    is List<*> -> {
                        // Multiple estates: "dept": {"in": [112, 134, 145, 129]}
                        put("dept", JSONObject().apply {
                            put("in", JSONArray().apply {
                                (estate as List<Int>).forEach { estateId ->
                                    put(estateId)
                                }
                            })
                        })
                    }
                    else -> {
                        // Fallback to Int
                        put("dept", estate as Int)
                    }
                }

                // Date range condition using BETWEEN
                put("tgl_inspeksi", JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(startDate)
                        put(today)
                    })
                })

                // ✅ NEW: Skip records where inspeksi_putaran == 2
                put("inspeksi_putaran", JSONObject().apply {
                    put("!=", 2)
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

                        // Build WHERE condition for inspeksi_detail
                        put("where", JSONObject().apply {
                            // Skip records where status_pemulihan == 1
                            put("status_pemulihan", JSONObject().apply {
                                put("!=", 1)
                            })

                            // ✅ Filter by kode_inspeksi if we have valid IDs
                            if (validKodeInspeksiIds.isNotEmpty()) {
                                put("kode_inspeksi", JSONObject().apply {
                                    put("in", JSONArray().apply {
                                        validKodeInspeksiIds.forEach { id ->
                                            put(id)
                                        }
                                    })
                                })
                                AppLogger.d("Added kode_inspeksi filter: $validKodeInspeksiIds")
                            } else {
                                AppLogger.d("No kode_inspeksi filter applied")
                            }
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