package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blok",
//    foreignKeys = [
//        ForeignKey(
//            entity = RegionalModel::class, // Reference the RegionalModel table
//            parentColumns = ["id"], // Column in RegionalModel
//            childColumns = ["regional"], // Column in DeptModel
//            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
//        ),
//        ForeignKey(
//            entity = DeptModel::class, // Reference the WilayahModel table
//            parentColumns = ["id"], // Column in WilayahModel
//            childColumns = ["dept"], // Column in DeptModel
//            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
//        ),
//        ForeignKey(
//            entity = DivisiModel::class, // Reference the WilayahModel table
//            parentColumns = ["id"], // Column in WilayahModel
//            childColumns = ["divisi"], // Column in DeptModel
//            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
//        )
//    ],
//    indices = [
//        Index(value = ["regional"]), // Index for regional column
//        Index(value = ["dept"]) , // Index for wilayah column
//                Index(value = ["divisi"])  // Index for wilayah column
//    ]
)
data class BlokModel (
    @PrimaryKey val id:Int,
    val regional:Int,
    val company:String,
    val company_ppro:String,
    val dept:Int,
    val dept_ppro:String,
    val dept_abbr:String,
    val divisi:Int,
    val divisi_ppro:Int,
    val nama:String,
    val kode:String,
    val ancak:String,
    val jumlah_tph:String,
    val tahun:String,
)