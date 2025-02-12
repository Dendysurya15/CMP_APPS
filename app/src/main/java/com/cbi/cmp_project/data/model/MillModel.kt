package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mill")
data class MillModel(
    @PrimaryKey val id: Int?,
    val abbr: String?,
    val nama: String?,

)
