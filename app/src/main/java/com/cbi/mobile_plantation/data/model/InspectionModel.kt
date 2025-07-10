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
    val tph_id:Int,
    val id_panen:Int,
    val date_panen:String,
    val jalur_masuk: String,
    val jenis_kondisi: Int,
    val baris: String,
    val jml_pkk_inspeksi: Int,
    val tracking_path: String,
    val foto: String? = null,
    val komentar: String? = null,
    val latTPH: Double,
    val lonTPH: Double,
    val dataIsZipped: Int = 0,
    val app_version: String,
    val status_upload: String,
    val status_uploaded_image: String,
)
