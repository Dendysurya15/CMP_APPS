package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.KENDARAAN)
data class KendaraanModel(
    @PrimaryKey val id: Int?,
    val no_kendaraan:String?,
    val owner: String?,
    val status: String?,
)
