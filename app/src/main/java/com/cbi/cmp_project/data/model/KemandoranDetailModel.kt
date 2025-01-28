package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel

@Entity(
    tableName = "kemandoran_detail",
//    foreignKeys = [
//        ForeignKey(
//            entity = KemandoranModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["header"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
//        ForeignKey(
//            entity = KaryawanModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["kid"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        ),
//
//    ],
//    indices = [
//        Index(value = ["header"]),
//        Index(value = ["kid"]),
//    ]
)
data class KemandoranDetailModel(
    @PrimaryKey val id: Int, // Primary key is typically non-null
    val kode_kemandoran: String?,
    val header: Int?,
    val kid: Int?,
    val nik: String?,
    val nama: String?,
    val tgl_mulai: String?,
    val tgl_akhir: String?,
    val status: Int?
)