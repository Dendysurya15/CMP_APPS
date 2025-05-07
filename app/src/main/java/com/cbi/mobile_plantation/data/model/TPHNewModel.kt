package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cbi.mobile_plantation.utils.AppUtils

@Entity(
    tableName = AppUtils.DatabaseTables.TPH,
//    foreignKeys = [
//        ForeignKey(
//            entity = RegionalModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["regional"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
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
//        ForeignKey(
//            entity = BlokModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["blok"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
//    ],
//    indices = [
//        Index(value = ["regional"]),
//        Index(value = ["dept"]),
//        Index(value = ["divisi"]),
//        Index(value = ["blok"])]
)

data class TPHNewModel (
    @PrimaryKey val id:Int?,
    val regional:String?,
    val company:Int?,
    val company_abbr:String?,
    val company_nama:String?,
    val wilayah:String?,
    val dept:Int?,
    val dept_ppro:Int?,
    val dept_abbr:String?,
    val dept_nama:String?,
    val divisi:Int?,
    val divisi_ppro:Int?,
    val divisi_abbr:String?,
    val divisi_nama:String?,
    val blok:Int?,
    val blok_ppro:Int?,
    val blok_kode:String?,
    val blok_nama:String?,
    val ancak:String?,
    val nomor:String?,
    val tahun:String?,
    val luas_area:String?,
    val jml_pokok:String?,
    val jml_pokok_ha:String?,
    val lat:String?,
    val lon:String?,
    val update_date:String?,
    val status:String?,
    val jenis_tph_id:String?,
)