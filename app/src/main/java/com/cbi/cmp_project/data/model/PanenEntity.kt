package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "panen_table")
data class PanenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tph_id: String,
    val date_created: Int,
    val created_by: Int,
    val approved_by: Int,
    val karyawan_id: String,
    val jjg_json: String,
    val foto: String,
    val komentar:  String,
    val asistensi: Int,
    val lat: Double,
    val lon: Double,
    val jenis_panen: Int,
    val ancak: Int
)
