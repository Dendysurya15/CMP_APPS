package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.ABSENSI)
data class AbsensiModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val kemandoran_id: String,
    val date_absen: String,
    val created_by: Int,
    val karyawan_msk_id: String,  //"30,59,9"
    val karyawan_tdk_msk_id: String,  //"30,59,9"
    val foto: String,
    val komentar:  String,
    val asistensi: Int,
    val lat: Double,
    val lon: Double,
    val info: String,
    val status_scan:Int = 0,
    val archive: Int,
    val dataIsZipped: Int = 0,
    )

