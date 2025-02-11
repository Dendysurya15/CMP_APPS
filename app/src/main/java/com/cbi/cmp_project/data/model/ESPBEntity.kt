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
    val info: String, // versi_app , os , device_name
    val archive: Int
)
