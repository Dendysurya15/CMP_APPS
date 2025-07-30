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
    val created_date: String,
    val created_by: String,
    val created_name: String,
    val updated_date_start: String? = null,
    val updated_date_end: String? = null,
    val updated_by: String? = null,
    val updated_name: String? = null,
    val tph_id:Int,
    val id_panen:String,
    val date_panen:String,
    val dept:Int? = null,
    val dept_ppro:Int? = null,
    val dept_abbr:String? = null,
    val dept_nama:String? = null,
    val divisi:Int? = null,
    val divisi_ppro:Int? = null,
    val divisi_abbr:String? = null,
    val divisi_nama:String? = null,
    val blok:Int? = null,
    val blok_ppro:Int? = null,
    val blok_kode:String? = null,
    val blok_nama:String? = null,
    val tph_nomor:Int? = null,
    val ancak:String? = null,
    val jalur_masuk: String,
    val jenis_kondisi: Int,
    val inspeksi_putaran: Int? = 1,
    val baris: String,
    val foto_user:String,
    val foto_user_pemulihan:String?=null,
    val jml_pkk_inspeksi: Int,
    val tracking_path: String,
    val tracking_path_pemulihan: String? = null,
    val dataIsZipped: Int = 0,
    val app_version: String,
    val app_version_pemulihan: String? = null,
    val status_upload: String,
    val status_upload_pemulihan: String? = "0",
    val status_uploaded_image: String,
    val status_uploaded_image_pemulihan: String? = "0",
    val isPushedToServer: Int = 0
)
