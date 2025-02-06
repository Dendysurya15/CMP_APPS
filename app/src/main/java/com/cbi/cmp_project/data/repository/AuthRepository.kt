package com.cbi.cmp_project.data.repository

import com.cbi.cmp_project.data.api.ApiService
import com.cbi.cmp_project.data.model.LoginResponse
import com.cbi.cmp_project.data.network.CMPApiClient
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
                CMPApiClient.instance.login(ApiService.LoginRequest(username, password))
            }
        } catch (e: SocketTimeoutException) {
            Response.error(408, ResponseBody.create("application/json".toMediaType(), "{\"message\":\"Request timed out\"}"))
        } catch (e: IOException) {
            Response.error(500, ResponseBody.create("application/json".toMediaType(), "{\"message\":\"Network error\"}"))
        } catch (e: Exception) {
            Response.error(500, ResponseBody.create("application/json".toMediaType(), "{\"message\":\"Unexpected error occurred\"}"))
        }
    }
}

fun String.toMediaType(): MediaType {
    return (this.toMediaTypeOrNull() ?: "application/json".toMediaTypeOrNull())!!
}
