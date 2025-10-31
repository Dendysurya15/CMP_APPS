package com.cbi.mobile_plantation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(
    tableName = AppUtils.DatabaseTables.KEMANDORAN,
//    foreignKeys = [
//        ForeignKey(
//            entity = DeptModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["dept"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
//        ForeignKey(
//            entity = DivisiModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["divisi"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
//
//    ],
//    indices = [
//        Index(value = ["dept"]),
//        Index(value = ["divisi"]),
//    ]
)
data class KemandoranModel(
    @PrimaryKey val id: Int, // Primary key is typically non-null
    val server: Int?,
    val regional: Int?,
    val company: Int?,
    val company_ppro: Int?,
    val company_abbr: String?,
    val company_nama: String?,
    val dept: Int?,
    val dept_ppro: Int?,
    val dept_abbr: String?,
    val dept_nama: String?,
    val divisi: Int?,
    val divisi_ppro: Int?,
    val divisi_abbr: String?,
    val divisi_nama: String?,
    val kemandoran_ppro: String?,
    val kode: String?,
    val nama: String?,
    val type: String?,
    val asistensi: Int?,
    val foto: String?,
    val komentar:  String?,
    val lat: Double?,
    val lon: Double?,
    val date_absen: String?,
    val status_absen: String?,
    val status: Int?
)