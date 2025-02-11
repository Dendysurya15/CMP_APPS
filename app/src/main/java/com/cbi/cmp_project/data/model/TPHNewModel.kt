package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tph",
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
    val regional:Int?,
    val company:String?,
    val company_ppro:Int?,
    val dept:Int?,
    val dept_abbr:String?,
    val divisi:Int?,
    val divisi_abbr:String?,
    val divisi_ppro:String?,
    val blok:Int?,
    val blok_kode:String?,
    val blok_nama:String?,
    val ancak:String?,
    val kode_tph:String?,
    val nomor:String?,
    val tahun:String?,
    val lat:String?,
    val lon:String?,
    val status:String?,
    val user_input:String?,
    val update_date:String?,
)