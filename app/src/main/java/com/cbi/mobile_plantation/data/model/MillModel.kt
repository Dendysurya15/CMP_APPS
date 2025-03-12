package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.MILL)
data class MillModel(
    @PrimaryKey val id: Int?,
    val abbr: String?,
    val nama: String?,
    val ip:String?,
)
