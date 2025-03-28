package com.cbi.mobile_plantation.data.model

data class TphRvData(
    val namaBlok: String,
    val noTPH: Int,
    val time: String,
    val jjg: Int,
    val username: String
)

data class TPHBlokInfo(
    val tphNomor: String,
    val blokKode: String
)