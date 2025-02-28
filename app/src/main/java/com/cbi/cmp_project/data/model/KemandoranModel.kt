package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cbi.cmp_project.utils.AppUtils

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
    val company: Int?,
    val dept: Int?,
    val dept_abbr: String?,
    val divisi: Int?,
    val divisi_abbr: String?,
    val kode: String?,
    val nama: String?,
    val type: String?,
    val status: Int?
)