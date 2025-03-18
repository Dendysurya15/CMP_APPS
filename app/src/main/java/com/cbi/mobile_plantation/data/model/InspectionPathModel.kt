package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.INSPEKSI_PATH)
data class InspectionPathModel(
    @PrimaryKey val id: String,
    val tracking_path: String,
)
