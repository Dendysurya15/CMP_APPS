package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils


@Entity(tableName = AppUtils.DatabaseTables.PARAMETER)
data class ParameterModel(
    @PrimaryKey
    val id: String,
    val isjson: Int?,
    val param_val: String?,
    val keterangan: String?
)