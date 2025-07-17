package com.cbi.mobile_plantation.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(
    tableName = AppUtils.DatabaseTables.INSPEKSI,
)
data class InspectionModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val created_date_start: String,
    val created_date_end: String,
    val created_by: String,
    val updated_date_start: String? = null,
    val updated_date_end: String? = null,
    val updated_by: String? = null,
    val tph_id:Int,
    val id_panen:Int,
    val date_panen:String,
    val jalur_masuk: String,
    val jenis_kondisi: Int,
    val inspeksi_putaran: Int? = 1,
    val baris: String,
    val jml_pkk_inspeksi: Int,
    val tracking_path: String,
    val tracking_path_pemulihan: String? = null,
    val foto: String? = null,
    val foto_pemulihan: String? = null,
    val komentar: String,
    val komentar_pemulihan: String? = null,
    val latTPH: Double,
    val lonTPH: Double,
    val latTPHPemulihan: Double?= null,
    val lonTPHPemulihan: Double? = null,
    val dataIsZipped: Int = 0,
    val app_version: String,
    val app_version_pemulihan: String? = null,
    val status_upload: String,
    val status_upload_pemulihan: String? = "0",
    val status_uploaded_image: String,
    val status_uploaded_image_pemulihan: String? = "0",
)
