package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.TRANSPORTER)
data class TransporterModel(
    @PrimaryKey val id: Int?,
    val kode: String?,
    val nama: String?,
    val status: Int?
)
