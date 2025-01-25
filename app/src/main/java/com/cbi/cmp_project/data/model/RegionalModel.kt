package com.cbi.markertph.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regional")
data class RegionalModel(
    @PrimaryKey val id: Int,
    val abbr: String,
    val nama: String
)