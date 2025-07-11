package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.INSPEKSI_DETAIL)
data class InspectionDetailModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_inspeksi: String,
    val created_date: String,
    val created_name: String,
    val created_by: String,
    val nik: String,
    val nama: String,
    val no_pokok: Int,
    val pokok_panen: Int? = null,
    val kode_inspeksi: Int,
    val temuan_inspeksi: Double,
    val status_pemulihan: Double,
    val foto: String? = null,
    val foto_pemulihan: String? = null,
    val komentar: String? = null,
    val latIssue: Double,
    val lonIssue: Double,
    val status_upload: String,
    val status_uploaded_image: String,
)
