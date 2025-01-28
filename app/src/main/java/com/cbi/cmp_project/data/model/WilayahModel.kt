package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wilayah",
//    foreignKeys = [
//        ForeignKey(
//            entity = RegionalModel::class, // The referenced table
//            parentColumns = ["id"], // The column in the referenced table
//            childColumns = ["regional"], // The column in this table
//            onDelete = ForeignKey.CASCADE // Optional: Specify behavior on deletion
//        )
//    ],
//    indices = [Index(value = ["regional"])] // Index to optimize foreign key queries
)
data class WilayahModel (
    @PrimaryKey val id:Int,
    val regional:Int,
    val abbr:String,
    val nama:String,
)