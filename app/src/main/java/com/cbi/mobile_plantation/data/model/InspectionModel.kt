package com.cbi.mobile_plantation.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(
    tableName = AppUtils.DatabaseTables.INSPEKSI,
    foreignKeys = [
        ForeignKey(
            entity = InspectionPathModel::class,
            parentColumns = ["id"],
            childColumns = ["id_path"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TPHNewModel::class,
            parentColumns = ["id"],
            childColumns = ["tph_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index("id_path"),
        Index("tph_id")
    ]
)
data class InspectionModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "id_path")
    val id_path: String,
    @ColumnInfo(name = "tph_id")
    val tph_id: Int,
    val ancak: Int,
    val status_panen: Int,
    val jalur_masuk: String,
    val brd_tinggal: Int,
    val buah_tinggal: Int,
    val jenis_inspeksi: Int,
    val karyawan_id: String,
    val asistensi: Int,
    val jenis_kondisi: Int,
    val baris1: Int,
    val baris2: Int? = null,
    val no_pokok: Int,
    val jml_pokok: Int,
    val titik_kosong: Int, // This to handle inspection type (Inspeksi / AKP)
    val jjg_akp: Int? = null, // This for AKP type, and all below is for inspection type
    val prioritas: Int? = null,
    val pokok_panen: Int? = null,
    val serangan_tikus: Int? = null,
    val ganoderma: Int? = null,
    val susunan_pelepah: Int? = null,
    val pelepah_sengkleh: Int? = null,
    val kondisi_pruning: Int? = null,
    val kentosan: Int? = null,
    val buah_masak: Int? = null,
    val buah_mentah: Int? = null,
    val buah_matang: Int? = null,
    val buah_matahari: Int? = null,
    val brd_tidak_dikutip: Int? = null,
    val brd_dlm_piringan: Int? = null,
    val brd_luar_piringan: Int? = null,
    val brd_pasar_pikul: Int? = null,
    val brd_ketiak: Int? = null,
    val brd_parit: Int? = null,
    val brd_segar: Int? = null,
    val brd_busuk: Int? = null,
    val foto: String? = null,
    val komentar: String? = null,
    val info: String,
    val created_by: Int,
    val created_date: String,
    val archive: Int = 0,
)
