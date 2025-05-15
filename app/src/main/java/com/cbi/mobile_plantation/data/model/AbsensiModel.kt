package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.ABSENSI)
data class AbsensiModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dept: String = "",
    val dept_abbr: String = "",
    val divisi: String = "",
    val divisi_abbr: String = "",
    val kemandoran_id: String,
    val date_absen: String,
    val created_by: Int,
    val karyawan_msk_id: String,  //"30,59,9"
    val karyawan_tdk_msk_id: String,  //"30,59,9"
    val karyawan_msk_nik: String = "",
    val karyawan_tdk_msk_nik: String = "",
    val karyawan_msk_nama: String = "",
    val karyawan_tdk_msk_nama: String = "",
    val foto: String,
    val komentar: String,
    val asistensi: Int,
    val lat: Double,
    val lon: Double,
    val info: String,
    val status_scan: Int = 0,
    val archive: Int,
    val dataIsZipped: Int = 0,
    val status_upload: Int = 0,
    val status_uploaded_image: String = "0",
)
