package com.cbi.mobile_plantation.utils

data class ScannedTPHLocation(
    val lat: Double,
    val lon: Double,
    val nomor: String,
    val blokKode: String,
    val jenisTPHId: String = "1"
)
