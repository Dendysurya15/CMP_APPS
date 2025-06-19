package com.cbi.mobile_plantation.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbi.markertph.data.model.TPHNewModel

data class InspectionWithDetailRelations(
    @Embedded val inspeksi: InspectionModel,

    @Relation(
        parentColumn = "id",
        entityColumn = "id_inspeksi"
    )
    val detailInspeksi: List<InspectionDetailModel>,

    @Relation(
        parentColumn = "tph_id",
        entityColumn = "id"
    )
    val tph: TPHNewModel?
)