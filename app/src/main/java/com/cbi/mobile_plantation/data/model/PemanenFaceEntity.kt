package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.PEMANEN_FACE)
data class PemanenFaceEntity(
    @PrimaryKey val karyawan_id: Int,
    val nik: String,
    val nama: String,
    val kemandoran_nama: String = "",
    val embedding: String,
    val updated_at: String,
    val status_upload: Int = 0
)
