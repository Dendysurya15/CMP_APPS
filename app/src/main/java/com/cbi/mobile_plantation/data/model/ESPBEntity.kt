package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.ESPB)
data class ESPBEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val blok_jjg: String, // jjg kirim pabrik
    val created_by_id: Int,
    val created_at: String,
    val nopol: String,
    val driver: String,
    val transporter_id: Int,
    val pemuat_id: String, // "1,3,4"
    val mill_id: Int,
    val archive: Int,
    val tph0: String, // {tph_id,date_created,jjg,status_espb=0
    val tph1: String,// {tph_id,date_created,jjg,status_espb=1 jadi 2 pas diupload + no espb=noESPB
    val update_info_sp: String = "NULL", // JSON {alasan, datetime, loc}
    val uploaded_by_id_wb: Int = 0,
    val uploaded_at_wb: String = "NULL",
    val uploaded_by_id_sp: Int = 0,
    val uploaded_at_sp: String = "NULL",
    val status_upload_cmp_sp: Int = 0,
    val status_upload_cmp_wb: Int = 0,
    val status_upload_ppro_wb: Int = 0,
    val status_draft: Int = 0,
    val status_mekanisasi: Int = 0,
    val creator_info: String, // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"}
    val uploader_info_sp: String = "NULL", // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"}
    val uploader_info_wb: String = "NULL", // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"}
    val noESPB: String, //PT-EST/AFD/TGl/BLN/THN/JAM_MENIT_Detik_mili second "SSS-SLE/OC/02/01/25/140012000"
    val scan_status: Int=0, // default 0
    val dataIsZipped: Int = 0,
)
