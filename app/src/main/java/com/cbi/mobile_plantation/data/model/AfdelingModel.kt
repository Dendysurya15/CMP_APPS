package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.AFDELING)
data class AfdelingModel(
    @PrimaryKey val id: Int,
    val id_ppro: Int?,
    val abbr: String?,
    val nama: String?,
    val estate_id: Int // This will be the foreign key to link with Estate
)