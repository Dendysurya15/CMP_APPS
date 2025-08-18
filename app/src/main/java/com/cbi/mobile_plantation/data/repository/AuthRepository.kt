package com.cbi.mobile_plantation.data.repository

import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.model.LoginResponse
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import com.cbi.mobile_plantation.utils.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okio.IOException
import retrofit2.Response
import java.net.SocketTimeoutException

class AuthRepository {
    suspend fun login(username: String, password: String): Response<LoginResponse>? {
        return try {
            withContext(Dispatchers.IO) {
                val request = ApiService.LoginRequest(username, password)
                AppLogger.d("Login Request Body: ${Gson().toJson(request)}")

                val response = TestingAPIClient.instance.login(request)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    AppLogger.e("API Error: Code ${response.code()} - Body: $errorBody")

                    // Simple approach: Replace message for authentication errors
                    val customErrorBody = if (response.code() == 500 || response.code() == 401) {
                        "{\"success\":false,\"message\":\"Username atau Password Tidak Sesuai\"}"
                    } else {
                        errorBody ?: "{\"message\":\"Unknown server error\"}"
                    }

                    Response.error(response.code(),
                        ResponseBody.create("application/json".toMediaType(), customErrorBody))
                } else {
                    response
                }
            }
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Login Error: Request timed out")
            Response.error(408, ResponseBody.create("application/json".toMediaType(),
                "{\"message\":\"Request timed out\"}"))
        } catch (e: IOException) {
            AppLogger.e("Login Error: Network issue - ${e.message}")
            Response.error(500, ResponseBody.create("application/json".toMediaType(),
                "{\"message\":\"Network error\"}"))
        } catch (e: Exception) {
            AppLogger.e("Login Error: Unexpected issue - ${e.message}")
            Response.error(500, ResponseBody.create("application/json".toMediaType(),
                "{\"message\":\"${e.message}\"}"))
        }
    }
}

fun String.toMediaType(): MediaType {
    return (this.toMediaTypeOrNull() ?: "application/json".toMediaTypeOrNull())!!
}
