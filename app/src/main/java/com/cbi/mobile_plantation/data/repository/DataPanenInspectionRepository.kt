package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiProvider
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.ParameterDao
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
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

class DataPanenInspectionRepository(
    context: Context,
//    private val apiService: ApiService = ApiProvider.currentApiService
//    private val apiService: ApiService = TestingAPIClient.instance,
    private val apiService: ApiService = CMPApiClient.instance,
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

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = formatter.format(calendar.time)

        AppLogger.d("Date range: $startDate to $endDate (7 days, excluding today)")
        AppLogger.d("estate $estate")

        var useStandardFields = false

        // Step 1: Check if created_date exists (not null)
        try {
            val checkQuery = JSONObject().apply {
                put("table", "panen")
                put("select", JSONArray().apply {
                    put("created_date")
                })
                put("where", JSONObject().apply {
                    // Estate condition
                    when (estate) {
                        is Int -> put("dept", estate)
                        is String -> put("dept", estate.toIntOrNull() ?: estate)
                        is List<*> -> {
                            put("dept", JSONObject().apply {
                                put("in", JSONArray().apply {
                                    (estate as List<Int>).forEach { estateId -> put(estateId) }
                                })
                            })
                        }
                        else -> put("dept", estate as Int)
                    }

                    // Check for non-null created_date
                    put("created_date", JSONObject().apply {
                        put("!=", null)
                    })
                })
            }

            val requestBody = checkQuery.toString().toRequestBody("application/json".toMediaType())
            AppLogger.d("🔍 Check query: $checkQuery")

            val checkResponse = apiService.getDataRaw(requestBody)
            val checkBody = checkResponse.body()?.string()

            if (!checkBody.isNullOrEmpty()) {
                val checkJson = JSONObject(checkBody)
                if (checkJson.optBoolean("success")) {
                    val data = checkJson.optJSONArray("data")
                    useStandardFields = data != null && data.length() > 0
                    AppLogger.d("✅ created_date exists: $useStandardFields")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("❌ Error checking created_date: ${e.message}")
            useStandardFields = false
        }

        // Step 2: Build main query based on field availability
        val dateField = if (useStandardFields) "created_date" else "created_date_kp"
        val byField = if (useStandardFields) "created_by" else "created_by_kp"
        val nameField = if (useStandardFields) "created_name" else "created_name_kp"

        AppLogger.d("📝 Using fields: date=$dateField, by=$byField, name=$nameField")

        val jsonObject = JSONObject().apply {
            put("table", "panen")
            put("select", JSONArray().apply {
                put("id")
                put("tph")
                put("tph_nomor")
                put("asistensi")
                put("asistensi_dept")
                put("asistensi_dept_nama")
                put("asistensi_divisi")
                put("ancak")
                put("tipe")
                put("dept_abbr")
                put("jjg_kirim")
                put("jjg_masak")
                put("jjg_mentah")
                put("jjg_lewat_masak")
                put("jjg_kosong")
                put("jjg_abnormal")
                put("jjg_bayar")
                put("spb_kode")
                put("status_espb")
                put(dateField)   // created_date or created_date_kp
                put(byField)     // created_by or created_by_kp
                put(nameField)   // created_name or created_name_kp
                put("kemandoran")
            })

            put("where", JSONObject().apply {
                // Estate condition
                when (estate) {
                    is Int -> put("dept", estate)
                    is String -> put("dept", estate.toIntOrNull() ?: estate)
                    is List<*> -> {
                        put("dept", JSONObject().apply {
                            put("in", JSONArray().apply {
                                (estate as List<Int>).forEach { estateId -> put(estateId) }
                            })
                        })
                    }
                    else -> put("dept", estate as Int)
                }

                // Date range condition using the determined field
                put(dateField, JSONObject().apply {
                    put("between", JSONArray().apply {
                        put(startDate)
                        put(endDate)
                    })
                })
            })
        }

        AppLogger.d("📤 Data Panen API Request: ${jsonObject.toString()}")

        val response = apiService.getDataRaw(jsonObject.toString().toRequestBody("application/json".toMediaType()))
        val responseBody = response.body()?.string()

        AppLogger.d("📩 Data Panen API Response: $responseBody")

        if (responseBody.isNullOrEmpty()) {
            return Response.success(
                ResponseBody.create("application/json".toMediaType(),
                    """{"success":false,"message":"No data found"}""")
            )
        }

        // Step 3: Parse and remap field names to standard names
        val responseJson = JSONObject(responseBody)

        if (responseJson.optBoolean("success", false)) {
            val dataArray = responseJson.optJSONArray("data")

            if (dataArray != null && dataArray.length() > 0) {
                // Remap fields if we used _kp fields
                if (!useStandardFields) {
                    AppLogger.d("🔄 Remapping _kp fields to standard names...")
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)

                        // Rename fields from _kp to standard names
                        if (item.has("created_date_kp")) {
                            item.put("created_date", item.get("created_date_kp"))
                            item.remove("created_date_kp")
                        }
                        if (item.has("created_by_kp")) {
                            item.put("created_by", item.get("created_by_kp"))
                            item.remove("created_by_kp")
                        }
                        if (item.has("created_name_kp")) {
                            item.put("created_name", item.get("created_name_kp"))
                            item.remove("created_name_kp")
                        }
                    }
                    AppLogger.d("✅ Fields remapped successfully")
                }
            }
        }

        AppLogger.d("🎯 Final response: ${responseJson.toString(2)}")

        // Return the remapped response
        val headers = Headers.Builder()
            .add("Content-Type", "application/json")
            .build()

        val finalResponseBody = responseJson.toString().toResponseBody("application/json".toMediaType())
        return Response.success(finalResponseBody, headers)
    }

    suspend fun getDataInspeksi(
        estate: Any, // Changed from Int to Any
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
                put("kemandoran_ppro_pemuat")
                put("kemandoran_nama_pemuat")
                put("nik_pemuat")
                put("nama_pemuat")
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