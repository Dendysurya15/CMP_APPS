package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.ESPB)
data class ESPBEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val blok_jjg: String, // jjg kirim pabrik X
    val created_by_id: Int, //X
    val created_at: String,//X
    val nopol: String, //X
    val driver: String, //X
    val transporter_id: Int, //X
    val pemuat_id: String, // "1,3,4" //X
    val kemandoran_id: String, // "1,3,4" //X
    val pemuat_nik: String, // "1,3,4" //X
    val mill_id: Int, //X
    val archive: Int,
    val tph0: String, // {tph_id,date_created,jjg,status_espb=0 //X
    val tph1: String,// {tph_id,date_created,jjg,status_espb=1 jadi 2 pas diupload + no espb=noESPB //X
    val update_info_sp: String = "NULL", // JSON {alasan, datetime, loc} //X
    val uploaded_by_id_wb: Int = 0, //X
    val uploaded_wb_response: String = "", //X
    val uploaded_ppro_response: String = "", //X
    val uploaded_at_wb: String = "NULL", //X
    val uploaded_at_ppro_wb: String = "", //X
    val uploaded_by_id_sp: Int = 0, //X
    val uploaded_at_sp: String = "NULL", //X
    val status_upload_cmp_sp: Int = 0,  //X
    val status_upload_cmp_wb: Int = 0, //X
    val status_upload_ppro_wb: Int = 0, //X
    val status_draft: Int = 0,
    val status_mekanisasi: Int = 0,
    val creator_info: String, // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"} //X
    val uploader_info_sp: String = "NULL", // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"} //X
    val uploader_info_wb: String = "NULL", // json {"app_version":"2.6.1","os_version":"13","device_model":"SM-A325F"} //X
    val noESPB: String, //PT-EST/AFD/TGl/BLN/THN/JAM_MENIT_Detik_mili second "SSS-SLE/OC/02/01/25/140012000" //X
    val scan_status: Int=0, // default 0
    val dataIsZipped: Int = 0,
    val ids_to_update: String = "NULL",
    val status_upload: Int = 0
)
