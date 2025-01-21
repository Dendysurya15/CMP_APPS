package com.cbi.cmp_project.data.model

data class KemandoranDetailModel(
    val id: Int,
    val kode_kemandoran: String,
    val header: Int,
    val kid: Int,
    val nik: String,
    val nama: String,
    val tgl_mulai: String,
    val tgl_akhir: String,
    val status: Int
)
