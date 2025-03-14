package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.FLAGESPB)
data class FlagESPBModel (
    @PrimaryKey val id:Int,
    val flag : String
)