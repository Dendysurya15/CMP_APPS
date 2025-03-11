package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.MILL)
data class MillModel(
    @PrimaryKey val id: Int?,
    val abbr: String?,
    val nama: String?,

)
