package com.cbi.cmp_project.data.model

data class KaryawanModel(
    val id: Int,
    val company: Int,
    val company_ppro: Int,
    val company_abbr: String,
    val company_nama: String,
    val dept: Int,
    val dept_ppro: Int,
    val dept_abbr: String,
    val dept_nama: String,
    val divisi: Int,
    val divisi_ppro: Int,
    val divisi_abbr: String,
    val divisi_nama: String,
    val nik: String,
    val nama: String,
    val jabatan: String,
    val jenis_kelamin: Int,
    val tgl_resign: String?, // Nullable if the date can be null
    val create_by: Int,
    val create_nama: String,
    val create_date: String,
    val update_by: Int,
    val update_nama: String,
    val update_date: String,
    val history: String,
    val status: Int
)
