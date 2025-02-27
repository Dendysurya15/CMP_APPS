package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_cmp")
data class UploadCMPModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tracking_id: Int?,
    val nama_file: String?,
    val status: Int?,
    val tanggal_upload: String?,
    val table_ids: String?,
)
