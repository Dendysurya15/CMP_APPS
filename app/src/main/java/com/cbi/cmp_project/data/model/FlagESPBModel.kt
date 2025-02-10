package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flag_espb")
data class FlagESPBModel (
    @PrimaryKey val id:Int,
    val flag : String
)