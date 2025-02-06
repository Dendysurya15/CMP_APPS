package com.cbi.cmp_project.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData? = null
)

data class LoginData(
    val user: User?,
    val token: String?,
)

data class User(
    val id: Int,
    val username: String,
    val nama: String,
    val jabatan : String,
    val create_date: String,
    val update_date: String
)
