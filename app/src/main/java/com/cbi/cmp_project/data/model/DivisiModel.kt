package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "divisi",
    foreignKeys = [
        ForeignKey(
            entity = DeptModel::class, // Reference the RegionalModel table
            parentColumns = ["id"], // Column in RegionalModel
            childColumns = ["dept"], // Column in DeptModel
            onDelete = ForeignKey.CASCADE // Optional: Behavior on delete
        ),

    ],
    indices = [
        Index(value = ["dept"]), // Index for regional column
    ]
)
data class DivisiModel (
    @PrimaryKey val id:Int,
    val id_ppro:Int,
    val company:Int,
    val dept:Int,
    val abbr:String,
    val nama:String,
)