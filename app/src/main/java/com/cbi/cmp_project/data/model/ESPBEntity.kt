package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "espb_table")
data class ESPBEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val blok_jjg: String, // jjg kirim pabrik
    val user_id: Int,
    val date_created: String,
    val nopol: String,
    val driver: String,
    val transporter_id: Int,
    val mill_id: Int,
    val archive: Int,
    val tph0: String, // {tph_id,date_created,jjg,status_espb
    val tph1: String,
    val noESPB: String //PT-EST/AFD/TGl/BLN/THN/JAM/MENIT_Detik_mili second "SSS-SLE/OC/02/01/25/140012000"
)
