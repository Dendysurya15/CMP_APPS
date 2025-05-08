package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.HEKTAR_PANEN)
data class HektarPanenEntity(
    @PrimaryKey val id: Int?,
    val nik: String,
    val pemanen_nama: String,
    val kemandoran_id: String,
    val kemandoran_nama: String,
//    val kemandoran_ppro: String,
    val kemandoran_kode: String,
    val blok: Int,
    val luas_blok: Float?,
    val luas_panen: Float =0f,
    val date_created: String,
    val created_by: String,
    val creator_info: String,
    val total_jjg_arr: String,
    val unripe_arr: String,
    val overripe_arr: String,
    val empty_bunch_arr: String,
    val abnormal_arr: String,
    val ripe_arr: String,
    val kirim_pabrik_arr: String,
    val dibayar_arr: String,
    val tph_ids: String,
    val date_created_panen: String,
    val regional: String,
    val wilayah: String,
    val company:Int?,
    val company_abbr:String?,
    val company_nama:String?,
    val dept:Int?,
    val dept_ppro:Int?,
    val dept_abbr:String?,
    val dept_nama:String?,
    val divisi:Int?,
    val divisi_ppro:Int?,
    val divisi_abbr:String?,
    val divisi_nama:String?,
    val blok_ppro:Int?,
    val blok_kode:String?,
    val blok_nama:String?
)