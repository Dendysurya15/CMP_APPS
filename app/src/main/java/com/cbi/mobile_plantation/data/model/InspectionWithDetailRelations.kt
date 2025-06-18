package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class InspectionWithDetailRelations(
    @Embedded val inspeksi: InspectionModel,

    @Relation(
        parentColumn = "id",
        entityColumn = "id_inspeksi"
    )
    val detailInspeksi: InspectionDetailModel?
)