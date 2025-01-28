package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel

@Entity(
    tableName = "kemandoran",
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
    val kode: String?,
    val nama: String?,
    val type: String?,
    val create_by: Int?,
    val create_nama: String?,
    val create_date: String?,
    val update_by: Int?,
    val update_nama: String?,
    val update_date: String?,
    val history: String?,
    val status: Int?
)