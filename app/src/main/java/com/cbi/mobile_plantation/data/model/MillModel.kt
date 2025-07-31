package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.MILL)
data class MillModel(
    @PrimaryKey val id: Int?,
    val id_ppro:Int?,
    val abbr: String?,
    val nama: String?,
    val ip_address:String?,
    val ip_client:String?,
    val sinkronisasi_pks:String?,
)
