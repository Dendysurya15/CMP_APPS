package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AbsensiKemandoranRelations(
    @Embedded val absensi: AbsensiModel,

    @Relation(
        parentColumn = "kemandoran_id",
        entityColumn = "id"
    )
    val kemandoran: KemandoranModel?
)