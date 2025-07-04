package com.cbi.mobile_plantation.data.model

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
    val regional : String,
    val company : String,
    val company_abbr : String,
    val company_nama : String,
    val wilayah : String,
    val dept_id : String,
    val dept_abbr : String,
    val dept_nama : String,
    val divisi : String,
    val create_date: String,
    val update_date: String,
    val kemandoran: String,
    val kemandoran_ppro: String,
    val kemandoran_nama: String,
    val kemandoran_kode: String,
)
