package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.BLOK)
data class BlokModel(
    @PrimaryKey val id:Int?,
    val id_ppro:Int?,
    val regional:Int?,
    val wilayah:Int?,
    val company:Int?,
    val company_ppro:Int?,
    val company_abbr:String?,
    val dept:Int?,
    val dept_ppro:Int?,
    val dept_abbr:String?,
    val divisi:Int?,
    val divisi_ppro:Int?,
    val divisi_abbr:String?,
    val nama:String?,
    val kode:String?,
    val kode_pmmp:String?,
    val ancak:Int?,
    val jumlah_tph:Int?,
    val luas_area:Int?,
    val tahun:Int?,
    val jumlah_pokok:Int?,
    val update_date:String?,
    val status:Int?
)