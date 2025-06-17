package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(tableName = AppUtils.DatabaseTables.INSPEKSI_DETAIL)
data class InspectionDetailModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_inspeksi: String,
    val no_pokok: Int,
    val prioritas: Int? = null,
    val pokok_panen: Int? = null,
    val susunan_pelepah: Int? = null,
    val pelepah_sengkleh: Int? = null,
    val kondisi_pruning: Int? = null,
    val brd_tidak_dikutip: Int? = null,
    val foto: String? = null,
    val komentar: String? = null,
    val latIssue: Double,
    val lonIssue: Double,
    val status_upload: String,
    val status_uploaded_image: String,
)
