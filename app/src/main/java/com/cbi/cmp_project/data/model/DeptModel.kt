package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dept",
//    foreignKeys = [
//        ForeignKey(
//            entity = RegionalModel::class, // Reference the RegionalModel table
//            parentColumns = ["id"], // Column in RegionalModel
//            childColumns = ["regional"], // Column in DeptModel
//            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
//        ),
//        ForeignKey(
//            entity = WilayahModel::class, // Reference the WilayahModel table
//            parentColumns = ["id"], // Column in WilayahModel
//            childColumns = ["wilayah"], // Column in DeptModel
//            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
//        )
//    ],
//    indices = [
//        Index(value = ["regional"]), // Index for regional column
//        Index(value = ["wilayah"])  // Index for wilayah column
//    ]
)
data class DeptModel(
    @PrimaryKey val id: Int,
    val id_ppro: String,
    val regional: Int, // Foreign key referencing RegionalModel
    val wilayah: Int,  // Foreign key referencing WilayahModel
    val company: Int,
    val abbr: String,
    val nama: String
)
