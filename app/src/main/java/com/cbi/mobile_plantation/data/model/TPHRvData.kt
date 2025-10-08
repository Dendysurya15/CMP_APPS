package com.cbi.mobile_plantation.data.model

data class TphRvData(
    val namaBlok: String,
    val noTPH: String,
    val time: String,
    val jjg: String,
    val username: String,
    val kemandoran_id: String,
    val tipePanen: String = "NULL",
    val ancak: String = "NULL",
    val nomor_pemanen: Int = 0,
    val asistensi: Int = 1,
    val asistensi_divisi: Int? = null
)
data class TPHBlokInfo(
    val tphNomor: String,
    val blokKode: String,
    val blokId :String,
)

data class filterHektarPanenTanggalBlok(
    val nik: String,
    val luas_panen: Float,
    val blok: String,
    val luas_blok: String,
    val dibayar_arr: String
)

data class displayHektarPanenTanggalBlok(
    val nama: String,
    val luas_panen: Float,
    val blok: String,
    val luas_blok: String,
    val dibayar_arr: String,
    val nik: String,
    val id: Int
)