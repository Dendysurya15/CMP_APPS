package com.cbi.cmp_project.data.model

import androidx.room.Ignore

data class PanenTBSModel(
    @Ignore val id: Int,
    val user_id: Int,
    val tanggal: String,
    val name: String,
    val estate: String,
    val id_estate: Int,
    val afdeling: String,
    val id_afdeling: Int,
    val blok: String,
    val id_blok: Int,
    val tahun_tanam: Int,
    val id_tt: Int,
    val ancak: String,
    val id_ancak: Int,
    val tph: String,
    val id_tph: Int,
    val jenis_panen: String,
    val list_pemanen: String,
    val list_idpemanen: String,
    val tbs: Int,
    val tbs_mentah: Int,
    val tbs_lewatmasak: Int,
    val tks: Int,
    val abnormal: Int,
    val tikus: Int,
    val tangkai_panjang: Int,
    val vcut: Int,
    val tbs_masak: Int,
    val tbs_dibayar: Int,
    val tbs_kirim: Int,
    val latitude: String,
    val longitude: String,
    val foto: String
)
