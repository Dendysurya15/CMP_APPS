package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.PANEN)
data class PanenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tph_id: String,
    val date_created: String,
    val created_by: Int,
    val karyawan_id: String,
    val kemandoran_id: String,
    val karyawan_nik: String,
    val karyawan_nama: String,
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
    val status_banjir: Int=0,
    val status_espb: Int=0, // 0 = belum jadi espb , 1 = sudah jadi espb , 2 = espb sudah sampai pabrik
    //    val status_scan_mpanen: Int=0, //0 = belum scan , 1 = sudah scan //
    val status_restan: Int=0,
    val scan_status: Int=0,
    val dataIsZipped: Int = 0,
    val no_espb: String = "NULL",
    val username: String = "NULL",
    val status_upload: Int = 0,
    val status_uploaded_image: String = "0",
    val status_pengangkutan: Int = 0, //1 diangkut unit, 0 belum diangkut, 2 status angkut manual
    val status_insert_mpanen: Int = 0,
    val status_scan_mpanen: Int = 0,
    val jumlah_pemanen: Int = 1,
    val archive_mpanen: Int=0,
    val isPushedToServer: Int = 0
    )
