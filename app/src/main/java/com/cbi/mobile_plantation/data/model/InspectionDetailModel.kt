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
    val updated_date: String? = null,
    val updated_name: String? = null,
    val updated_by: String ? = null,
    val nik: String,
    val nama: String,
    val no_pokok: Int,
    val pokok_panen: Int? = null,
    val kode_inspeksi: Int,
    val temuan_inspeksi: Double,
    val status_pemulihan: Int,
    val foto: String? = null,
    val foto_pemulihan: String? = null,
    val komentar: String? = null,
    val komentar_pemulihan: String? = null,
    val latIssue: Double,
    val lonIssue: Double,
    val latPemulihan: Double? = null,
    val lonPemulihan: Double? = null,
    val status_upload: String,
    val status_upload_pemulihan: String? = null,
    val status_uploaded_image: String,
    val status_uploaded_image_pemulihan: String? = null,
)
