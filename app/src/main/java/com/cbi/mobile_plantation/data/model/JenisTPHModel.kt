package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.JENIS_TPH)
data class JenisTPHModel(
    @PrimaryKey val id: Int?,
    val jenis_tph: String?,
    val limit: Int?,
    val keterangan: String?,
)
