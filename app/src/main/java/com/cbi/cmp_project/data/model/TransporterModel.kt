package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.TRANSPORTER)
data class TransporterModel(
    @PrimaryKey val id: Int?,
    val kode: String?,
    val nama: String?,
    val status: Int?
)
