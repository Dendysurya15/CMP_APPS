package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.HEKTAR_PANEN)
data class HektarPanenEntity(
    @PrimaryKey val id: Int?,
    val nik: String,
    val blok: Int,
    val luas_panen: Float=0f,
    val date_created:String,
    val created_by:String,
    val creator_info:String,
    val total_jjg_arr:String,
    val tph_ids:String,
    val date_created_panen:String
)
