package com.cbi.cmp_project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transporter")
data class TransporterModel(
    @PrimaryKey val id: Int?,
    val kode: String?,
    val nama: String?,
    val status: Int?
)
