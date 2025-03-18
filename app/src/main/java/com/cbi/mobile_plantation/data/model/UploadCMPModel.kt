package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.UPLOADCMP)
data class UploadCMPModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tracking_id: Int?,
    val nama_file: String?,
    val status: Int?,
    val tanggal_upload: String?,
    val table_ids: String?,
)
