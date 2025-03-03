package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.PANEN)
data class PanenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tph_id: String,
    val date_created: String,
    val created_by: Int,
    val karyawan_id: String,
    val jjg_json: String,
    val foto: String,
    val komentar:  String,
    val asistensi: Int,
    val lat: Double,
    val lon: Double,
    val jenis_panen: Int, //normal = 0, Cut& carry =1, MAIC = 2
    val ancak: Int,
    val info: String,
    val archive: Int=0,
    val status_espb: Int=0, // default 0
    val status_restan: Int=0, // default 0
    val scan_status: Int=0 // default 0
)
