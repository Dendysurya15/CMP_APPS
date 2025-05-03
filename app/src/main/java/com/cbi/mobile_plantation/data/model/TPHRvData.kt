package com.cbi.mobile_plantation.data.model

data class TphRvData(
    val namaBlok: String,
    val noTPH: String,
    val time: String,
    val jjg: String,
    val username: String
)

data class TPHBlokInfo(
    val tphNomor: String,
    val blokKode: String
)

data class filterHektarPanenTanggalBlok(
    val namaBlok: String,
    val date: String,
    val pemanen: String,
    val janjang_dibayar: String,
    val id_hektar_panen: Int
)