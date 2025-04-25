package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.ESTATE)
data class EstateModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_ppro: Int?,
    val abbr: String?,
    val nama: String?
)