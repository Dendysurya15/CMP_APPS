package com.cbi.mobile_plantation.utils

data class ScannedTPHLocation(
    val lat: Double,
    val lon: Double,
    val nomor: String,
    val blokKode: String,
    val divisiKode: String?=null,
    val deptKode: String?=null,
    val jmlPokokHa: Int? = null,  // Add this field
    val jenisTPHId: String = "1"
)